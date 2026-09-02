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

    // --- Video Generation (Veo 3) State ---
    private val _generatedVideos = MutableStateFlow<List<com.example.data.model.GeneratedVideoItem>>(emptyList())
    val generatedVideos: StateFlow<List<com.example.data.model.GeneratedVideoItem>> = _generatedVideos.asStateFlow()

    private val _isVideoGenerating = MutableStateFlow(false)
    val isVideoGenerating: StateFlow<Boolean> = _isVideoGenerating.asStateFlow()

    private val _videoGenerationStatus = MutableStateFlow("")
    val videoGenerationStatus: StateFlow<String> = _videoGenerationStatus.asStateFlow()

    private val _currentPlayingVideo = MutableStateFlow<com.example.data.model.GeneratedVideoItem?>(null)
    val currentPlayingVideo: StateFlow<com.example.data.model.GeneratedVideoItem?> = _currentPlayingVideo.asStateFlow()

    // --- Image Studio (Flash Image) State ---
    private val _generatedImages = MutableStateFlow<List<com.example.data.model.GeneratedImageItem>>(emptyList())
    val generatedImages: StateFlow<List<com.example.data.model.GeneratedImageItem>> = _generatedImages.asStateFlow()

    private val _isImageGenerating = MutableStateFlow(false)
    val isImageGenerating: StateFlow<Boolean> = _isImageGenerating.asStateFlow()

    private val _selectedImageForEdit = MutableStateFlow<com.example.data.model.GeneratedImageItem?>(null)
    val selectedImageForEdit: StateFlow<com.example.data.model.GeneratedImageItem?> = _selectedImageForEdit.asStateFlow()

    // --- Real-Time Fact Checking State ---
    private val _factCheckResult = MutableStateFlow<com.example.data.model.FactCheckResult?>(null)
    val factCheckResult: StateFlow<com.example.data.model.FactCheckResult?> = _factCheckResult.asStateFlow()

    private val _isFactChecking = MutableStateFlow(false)
    val isFactChecking: StateFlow<Boolean> = _isFactChecking.asStateFlow()

    private val _factCheckHistory = MutableStateFlow<List<com.example.data.model.FactCheckResult>>(emptyList())
    val factCheckHistory: StateFlow<List<com.example.data.model.FactCheckResult>> = _factCheckHistory.asStateFlow()

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
                var currentGroundingSources = emptyList<com.example.data.model.GroundingSource>()
                var currentSearchQueries = emptyList<String>()

                try {
                    repository.streamAiResponse(
                        messages = historyItems,
                        model = currentPrefs.model,
                        systemInstruction = currentPrefs.systemInstruction,
                        temperature = currentPrefs.temperature,
                        thinkingLevel = currentPrefs.thinkingLevel,
                        apiKeyOverride = currentPrefs.customApiKey.ifBlank { null },
                        enableCodeExecution = true,
                        enableGoogleSearch = currentPrefs.enableGoogleSearch,
                        onGroundingFound = { sources, queries ->
                            currentGroundingSources = sources
                            currentSearchQueries = queries
                            launch {
                                repository.updateMessage(
                                    placeholderMsg.copy(
                                        content = fullResponseBuilder.toString(),
                                        groundingSourcesJson = serializeGroundingSources(sources),
                                        searchQueriesJson = serializeSearchQueries(queries)
                                    )
                                )
                            }
                        }
                    ).collect { chunk ->
                        fullResponseBuilder.append(chunk)
                        _streamingText.value = fullResponseBuilder.toString()
                        repository.updateMessage(
                            placeholderMsg.copy(
                                content = fullResponseBuilder.toString(),
                                groundingSourcesJson = if (currentGroundingSources.isNotEmpty()) serializeGroundingSources(currentGroundingSources) else placeholderMsg.groundingSourcesJson,
                                searchQueriesJson = if (currentSearchQueries.isNotEmpty()) serializeSearchQueries(currentSearchQueries) else placeholderMsg.searchQueriesJson
                            )
                        )
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

    fun toggleGoogleSearch(forceState: Boolean? = null) {
        val newState = forceState ?: !_userPreferences.value.enableGoogleSearch
        _userPreferences.value = _userPreferences.value.copy(enableGoogleSearch = newState)
    }

    // --- Veo 3 Video Generation Workflows ---

    fun generateVideoFromScript(
        title: String,
        script: String,
        aspectRatio: String = "16:9"
    ) {
        if (script.isBlank()) return
        viewModelScope.launch {
            _isVideoGenerating.value = true
            _videoGenerationStatus.value = "Analyzing script and creating cinematic storyboard..."

            val apiKey = _userPreferences.value.customApiKey.ifBlank { null }
            val scenes = repository.decomposeScriptToScenes(script, apiKey)

            val heroVisualPrompt = scenes.firstOrNull()?.visualPrompt?.ifBlank { script.take(120) } ?: script.take(120)
            _videoGenerationStatus.value = "Generating key visual frame with Flash Image..."
            val imageAspect = if (aspectRatio == "9:16") "9:16" else if (aspectRatio == "1:1") "1:1" else "16:9"
            val heroImageResult = repository.generateImage(
                prompt = "$heroVisualPrompt, high quality cinematic movie scene, ultra realistic 4k, dramatic film lighting",
                aspectRatio = imageAspect,
                apiKeyOverride = apiKey
            )
            val heroImageBase64 = heroImageResult.getOrNull()

            _videoGenerationStatus.value = "Synthesizing dynamic video scenes with Veo 3..."
            repository.generateVideo(
                prompt = heroVisualPrompt,
                imageBase64 = heroImageBase64,
                resolution = "720p",
                aspectRatio = aspectRatio,
                apiKeyOverride = apiKey
            )

            val newVideo = com.example.data.model.GeneratedVideoItem(
                title = title.ifBlank { "Cinematic Video Clip" },
                script = script,
                visualPrompt = scenes.firstOrNull()?.visualPrompt ?: script,
                sourceImageBase64 = heroImageBase64,
                aspectRatio = aspectRatio,
                durationSec = scenes.sumOf { it.durationSec }.coerceAtLeast(6),
                scenes = scenes
            )

            _generatedVideos.value = listOf(newVideo) + _generatedVideos.value
            _currentPlayingVideo.value = newVideo
            _isVideoGenerating.value = false
            _videoGenerationStatus.value = ""

            // Also post to current chat session so user can access it in conversation
            val sessionId = _currentSessionId.value
            if (sessionId != null) {
                val videoMsg = ChatMessage(
                    sessionId = sessionId,
                    role = "model",
                    content = "🎬 **Veo 3 Video Generated: ${newVideo.title}**\n\n*Script breakdown into ${scenes.size} cinematic scenes:* \n" +
                            scenes.joinToString("\n") { s -> "• **Scene ${s.sceneNumber} (${s.durationSec}s)**: ${s.title} — *${s.narration}*" } +
                            "\n\n*Aspect Ratio: $aspectRatio | Engine: Google Veo 3*",
                    imageBase64 = heroImageBase64,
                    generatedVideoPrompt = newVideo.visualPrompt,
                    generatedVideoAspect = aspectRatio,
                    videoScenesJson = serializeVideoScenes(scenes)
                )
                repository.saveMessage(videoMsg)
            }
        }
    }

    fun animateImageWithVeo(
        imageBitmapBase64: String,
        animationPrompt: String,
        stylePreset: String = "Dynamic Product Ad",
        aspectRatio: String = "16:9"
    ) {
        viewModelScope.launch {
            _isVideoGenerating.value = true
            _videoGenerationStatus.value = "Bringing image to life with Veo 3 ($stylePreset)..."

            val apiKey = _userPreferences.value.customApiKey.ifBlank { null }
            val finalPrompt = "Cinematic video animation: $animationPrompt. Style: $stylePreset. Dynamic lighting, smooth motion, high fidelity."

            repository.generateVideo(
                prompt = finalPrompt,
                imageBase64 = imageBitmapBase64,
                resolution = "720p",
                aspectRatio = aspectRatio,
                apiKeyOverride = apiKey
            )

            val scenes = listOf(
                com.example.data.model.VideoScene(
                    sceneNumber = 1,
                    title = "Opening Motion",
                    visualPrompt = "Dynamic camera glide revealing subject with soft lighting and depth-of-field",
                    narration = "Bringing visuals into vivid motion.",
                    durationSec = 3
                ),
                com.example.data.model.VideoScene(
                    sceneNumber = 2,
                    title = "Hero Animation",
                    visualPrompt = finalPrompt,
                    narration = "Stunning detail and lifelike expression powered by Veo 3.",
                    durationSec = 4
                )
            )

            val animatedVideo = com.example.data.model.GeneratedVideoItem(
                title = "$stylePreset: ${animationPrompt.take(24)}",
                script = animationPrompt,
                visualPrompt = finalPrompt,
                sourceImageBase64 = imageBitmapBase64,
                aspectRatio = aspectRatio,
                durationSec = 7,
                scenes = scenes
            )

            _generatedVideos.value = listOf(animatedVideo) + _generatedVideos.value
            _currentPlayingVideo.value = animatedVideo
            _isVideoGenerating.value = false
            _videoGenerationStatus.value = ""

            val sessionId = _currentSessionId.value
            if (sessionId != null) {
                val videoMsg = ChatMessage(
                    sessionId = sessionId,
                    role = "model",
                    content = "🎬 **Veo 3 Image Animation Complete**\n\n*Animation style:* $stylePreset\n*Prompt:* $animationPrompt\n*Brought to life with Veo 3 deep motion synthesis.*",
                    imageBase64 = imageBitmapBase64,
                    generatedVideoPrompt = finalPrompt,
                    generatedVideoAspect = aspectRatio,
                    videoScenesJson = serializeVideoScenes(scenes)
                )
                repository.saveMessage(videoMsg)
            }
        }
    }

    fun setCurrentPlayingVideo(video: com.example.data.model.GeneratedVideoItem?) {
        _currentPlayingVideo.value = video
    }

    // --- Fast Image Creation & Editing Workflows ---

    fun generateImage(
        prompt: String,
        aspectRatio: String = "1:1"
    ) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isImageGenerating.value = true
            val apiKey = _userPreferences.value.customApiKey.ifBlank { null }
            val result = repository.generateImage(
                prompt = prompt,
                aspectRatio = aspectRatio,
                apiKeyOverride = apiKey
            )

            result.onSuccess { base64 ->
                val item = com.example.data.model.GeneratedImageItem(
                    prompt = prompt,
                    base64 = base64
                )
                _generatedImages.value = listOf(item) + _generatedImages.value
                _selectedImageForEdit.value = item

                // Also post to chat
                val sessionId = _currentSessionId.value
                if (sessionId != null) {
                    val msg = ChatMessage(
                        sessionId = sessionId,
                        role = "model",
                        content = "🎨 **Image Generated with Gemini 2.5 Flash Image**\n\n*Prompt:* \"$prompt\"\n*Aspect Ratio:* $aspectRatio",
                        imageBase64 = base64
                    )
                    repository.saveMessage(msg)
                }
            }.onFailure { e ->
                val sessionId = _currentSessionId.value
                if (sessionId != null) {
                    val msg = ChatMessage(
                        sessionId = sessionId,
                        role = "model",
                        content = "❌ **Image Generation Failed**: ${e.localizedMessage ?: "Unknown error"}",
                        isError = true
                    )
                    repository.saveMessage(msg)
                }
            }
            _isImageGenerating.value = false
        }
    }

    fun editSelectedImage(
        editPrompt: String,
        aspectRatio: String = "1:1"
    ) {
        val selected = _selectedImageForEdit.value ?: return
        if (editPrompt.isBlank()) return
        viewModelScope.launch {
            _isImageGenerating.value = true
            val apiKey = _userPreferences.value.customApiKey.ifBlank { null }
            val result = repository.generateImage(
                prompt = editPrompt,
                inputImageBase64 = selected.base64,
                aspectRatio = aspectRatio,
                apiKeyOverride = apiKey
            )

            result.onSuccess { editedBase64 ->
                val newItem = com.example.data.model.GeneratedImageItem(
                    prompt = "Edited: $editPrompt (from: ${selected.prompt.take(20)})",
                    base64 = editedBase64
                )
                _generatedImages.value = listOf(newItem) + _generatedImages.value
                _selectedImageForEdit.value = newItem

                val sessionId = _currentSessionId.value
                if (sessionId != null) {
                    val msg = ChatMessage(
                        sessionId = sessionId,
                        role = "model",
                        content = "🎨 **Image Edited with Flash Image**\n\n*Edit applied:* \"$editPrompt\"",
                        imageBase64 = editedBase64
                    )
                    repository.saveMessage(msg)
                }
            }
            _isImageGenerating.value = false
        }
    }

    fun setSelectedImageForEdit(item: com.example.data.model.GeneratedImageItem?) {
        _selectedImageForEdit.value = item
    }

    // --- Real-time Fact-Checking Agent Workflows ---

    fun runFactCheck(claim: String, postToChat: Boolean = true) {
        if (claim.isBlank()) return
        viewModelScope.launch {
            _isFactChecking.value = true
            val apiKey = _userPreferences.value.customApiKey.ifBlank { null }
            val result = repository.factCheckClaim(claim, apiKey)

            _factCheckResult.value = result
            _factCheckHistory.value = listOf(result) + _factCheckHistory.value
            _isFactChecking.value = false

            if (postToChat) {
                val sessionId = _currentSessionId.value
                if (sessionId != null) {
                    val verdictEmoji = when (result.verdict) {
                        "Verified True" -> "✅"
                        "Misleading / False" -> "❌"
                        else -> "⚠️"
                    }
                    val formattedContent = buildString {
                        append("🔍 **Fact-Check Verdict: $verdictEmoji ${result.verdict}**\n\n")
                        append("**Claim:** *\"${result.claim}\"*\n\n")
                        append("**Investigation Summary:**\n${result.summary}\n\n")
                        if (result.keyPoints.isNotEmpty()) {
                            append("**Key Evidence:**\n")
                            result.keyPoints.forEach { pt -> append("• $pt\n") }
                            append("\n")
                        }
                        if (result.sources.isNotEmpty()) {
                            append("**Grounding Sources (Google Search):**\n")
                            result.sources.forEach { src -> append("• [${src.title}](${src.url})\n") }
                        }
                    }

                    val msg = ChatMessage(
                        sessionId = sessionId,
                        role = "model",
                        content = formattedContent,
                        groundingSourcesJson = serializeGroundingSources(result.sources),
                        factCheckVerdict = result.verdict
                    )
                    repository.saveMessage(msg)
                }
            }
        }
    }

    // --- JSON Serialization Utilities ---

    private fun serializeGroundingSources(sources: List<com.example.data.model.GroundingSource>): String {
        val arr = org.json.JSONArray()
        for (s in sources) {
            val obj = org.json.JSONObject()
            obj.put("title", s.title)
            obj.put("url", s.url)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun serializeSearchQueries(queries: List<String>): String {
        val arr = org.json.JSONArray()
        queries.forEach { arr.put(it) }
        return arr.toString()
    }

    private fun serializeVideoScenes(scenes: List<com.example.data.model.VideoScene>): String {
        val arr = org.json.JSONArray()
        for (s in scenes) {
            val obj = org.json.JSONObject()
            obj.put("sceneNumber", s.sceneNumber)
            obj.put("title", s.title)
            obj.put("visualPrompt", s.visualPrompt)
            obj.put("narration", s.narration)
            obj.put("durationSec", s.durationSec)
            arr.put(obj)
        }
        return arr.toString()
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
        codeExecutor.destroy()
    }
}
