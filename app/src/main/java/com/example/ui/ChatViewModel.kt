package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.DashboardStats
import com.example.data.model.UserPreferences
import com.example.data.remote.GeminiClient
import com.example.data.repository.ChatRepository
import com.example.util.CodeExecutor
import com.example.util.SpeechManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = ChatRepository(db.chatDao())
    val speechManager = SpeechManager(application)
    val codeExecutor = CodeExecutor(application)

    val sessions: StateFlow<List<ChatSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    val currentMessages: StateFlow<List<ChatMessage>> = _currentSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMessagesForSession(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _attachedImageBase64 = MutableStateFlow<String?>(null)
    val attachedImageBase64: StateFlow<String?> = _attachedImageBase64.asStateFlow()

    private val _attachedImageUri = MutableStateFlow<String?>(null)
    val attachedImageUri: StateFlow<String?> = _attachedImageUri.asStateFlow()

    private val _userPreferences = MutableStateFlow(UserPreferences())
    val userPreferences: StateFlow<UserPreferences> = _userPreferences.asStateFlow()

    private val _codeExecutionsCount = MutableStateFlow(0)

    val dashboardStats: StateFlow<DashboardStats> = combine(
        repository.sessionsCount,
        repository.messagesCount,
        _codeExecutionsCount,
        _userPreferences
    ) { sessionsCount, messagesCount, codeCount, prefs ->
        DashboardStats(
            totalSessions = sessionsCount,
            totalMessages = messagesCount,
            totalCodeExecutions = codeCount,
            estimatedWordsGenerated = messagesCount * 85,
            activePersona = prefs.personaName
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    val isListening: StateFlow<Boolean> = speechManager.isListening
    val isSpeaking: StateFlow<Boolean> = speechManager.isSpeaking
    val currentSpeakingId: StateFlow<String?> = speechManager.currentSpeakingId

    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            // Load or initialize default session
            val initialSessions = repository.allSessions.first()
            if (initialSessions.isNotEmpty()) {
                _currentSessionId.value = initialSessions.first().id
            } else {
                createNewChat()
            }
        }
    }

    fun createNewChat(initialTitle: String = "New chat") {
        viewModelScope.launch {
            cancelGeneration()
            val newSession = repository.createSession(
                title = initialTitle,
                model = _userPreferences.value.model
            )
            _currentSessionId.value = newSession.id
            _attachedImageBase64.value = null
            _attachedImageUri.value = null
        }
    }

    fun selectSession(session: ChatSession) {
        if (_currentSessionId.value == session.id) return
        cancelGeneration()
        _currentSessionId.value = session.id
        _attachedImageBase64.value = null
        _attachedImageUri.value = null
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            val session = sessions.value.find { it.id == sessionId } ?: return@launch
            repository.updateSession(session.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                val remaining = sessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    _currentSessionId.value = remaining.first().id
                } else {
                    createNewChat()
                }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            cancelGeneration()
            repository.clearAllHistory()
            createNewChat()
        }
    }

    fun attachImage(uri: Uri) {
        viewModelScope.launch {
            try {
                _attachedImageUri.value = uri.toString()
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    // Downscale bitmap if too large to fit in memory
                    val scaledBitmap = scaleBitmap(bitmap, 1024)
                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    _attachedImageBase64.value = base64
                }
            } catch (e: Exception) {
                _attachedImageBase64.value = null
                _attachedImageUri.value = null
            }
        }
    }

    fun removeAttachedImage() {
        _attachedImageBase64.value = null
        _attachedImageUri.value = null
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDim
            newHeight = (maxDim / ratio).toInt()
        } else {
            newHeight = maxDim
            newWidth = (maxDim * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun sendMessage(promptText: String) {
        val trimmedPrompt = promptText.trim()
        if (trimmedPrompt.isEmpty() && _attachedImageBase64.value == null) return

        val sessionId = _currentSessionId.value ?: return
        val currentPrefs = _userPreferences.value
        val imageBase64 = _attachedImageBase64.value
        val imageUri = _attachedImageUri.value

        // Clear attachment
        _attachedImageBase64.value = null
        _attachedImageUri.value = null

        viewModelScope.launch {
            // 1. Create and save user message
            val userMsg = ChatMessage(
                sessionId = sessionId,
                role = "user",
                content = trimmedPrompt,
                imageBase64 = imageBase64,
                imageUri = imageUri,
                timestamp = System.currentTimeMillis()
            )
            repository.saveMessage(userMsg)

            // Auto-rename session if it is still "New chat"
            val session = sessions.value.find { it.id == sessionId }
            if (session != null && (session.title == "New chat" || session.title.isBlank())) {
                val autoTitle = if (trimmedPrompt.isNotEmpty()) {
                    if (trimmedPrompt.length > 28) trimmedPrompt.take(28) + "…" else trimmedPrompt
                } else "Image Analysis"
                repository.updateSession(session.copy(title = autoTitle, updatedAt = System.currentTimeMillis()))
            }

            // 2. Prepare assistant placeholder
            val assistantMsgId = UUID.randomUUID().toString()
            val placeholderMsg = ChatMessage(
                id = assistantMsgId,
                sessionId = sessionId,
                role = "model",
                content = "",
                timestamp = System.currentTimeMillis()
            )
            repository.saveMessage(placeholderMsg)

            // 3. Build message history for Gemini
            val previousMessages = currentMessages.value + userMsg
            val historyItems = previousMessages.map { msg ->
                GeminiClient.MessageItem(
                    role = msg.role,
                    text = msg.content,
                    imageBase64 = msg.imageBase64
                )
            }

            _isGenerating.value = true
            _streamingText.value = ""

            generationJob = launch {
                val fullResponseBuilder = StringBuilder()
                try {
                    repository.streamAiResponse(
                        messages = historyItems,
                        model = currentPrefs.model,
                        systemInstruction = currentPrefs.systemInstruction,
                        temperature = currentPrefs.temperature,
                        thinkingLevel = currentPrefs.thinkingLevel,
                        apiKeyOverride = currentPrefs.customApiKey.ifBlank { null },
                        enableCodeExecution = true
                    ).collect { chunk ->
                        fullResponseBuilder.append(chunk)
                        _streamingText.value = fullResponseBuilder.toString()
                        repository.updateMessage(placeholderMsg.copy(content = fullResponseBuilder.toString()))
                    }
                } catch (e: Exception) {
                    val errorText = fullResponseBuilder.toString() + "\n\n❌ Error: ${e.localizedMessage}"
                    repository.updateMessage(placeholderMsg.copy(content = errorText, isError = true))
                } finally {
                    _isGenerating.value = false
                    _streamingText.value = ""
                }
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _isGenerating.value = false
        _streamingText.value = ""
    }

    fun regenerateLastResponse() {
        val messages = currentMessages.value
        val lastAssistant = messages.lastOrNull { it.role == "model" }
        val lastUser = messages.lastOrNull { it.role == "user" }
        if (lastUser != null) {
            sendMessage(lastUser.content)
        }
    }

    fun toggleSpeakMessage(messageId: String, content: String) {
        val prefs = _userPreferences.value
        speechManager.speak(content, messageId, speed = prefs.speechRate, pitch = prefs.speechPitch)
    }

    fun recordCodeExecution() {
        _codeExecutionsCount.value += 1
    }

    fun updatePreferences(newPrefs: UserPreferences) {
        _userPreferences.value = newPrefs
    }

    fun exportHistoryAsMarkdown(): String {
        val allSess = sessions.value
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        return buildString {
            append("# ChatGPT AI - Exported History\n")
            append("Exported on: $dateStr\n\n")
            for (s in allSess) {
                append("## Conversation: ${s.title}\n")
                append("Model: ${s.model}\n\n")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
