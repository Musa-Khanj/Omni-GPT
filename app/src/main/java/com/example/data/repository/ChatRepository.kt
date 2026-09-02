package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    private val geminiClient: GeminiClient = GeminiClient()
) {
    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    fun getSessionById(sessionId: String): Flow<ChatSession?> {
        return chatDao.getSessionById(sessionId)
    }

    suspend fun createSession(title: String = "New chat", model: String = "gemini-3.5-flash"): ChatSession {
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            model = model
        )
        chatDao.insertSession(session)
        return session
    }

    suspend fun updateSession(session: ChatSession) {
        chatDao.updateSession(session)
    }

    suspend fun updateSessionTitle(sessionId: String, newTitle: String) {
        val now = System.currentTimeMillis()
        val session = ChatSession(
            id = sessionId,
            title = newTitle,
            updatedAt = now
        )
        chatDao.updateSession(session)
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSessionById(sessionId)
    }

    suspend fun clearAllHistory() {
        chatDao.clearAllSessions()
    }

    suspend fun saveMessage(message: ChatMessage) {
        chatDao.insertMessage(message)
    }

    suspend fun updateMessage(message: ChatMessage) {
        chatDao.updateMessage(message)
    }

    fun streamAiResponse(
        messages: List<GeminiClient.MessageItem>,
        model: String,
        systemInstruction: String?,
        temperature: Float,
        thinkingLevel: String,
        apiKeyOverride: String?,
        enableCodeExecution: Boolean,
        enableGoogleSearch: Boolean = false,
        onGroundingFound: ((sources: List<com.example.data.model.GroundingSource>, queries: List<String>) -> Unit)? = null
    ): Flow<String> {
        return geminiClient.streamGenerateContent(
            messages = messages,
            model = model,
            systemInstruction = systemInstruction,
            temperature = temperature,
            thinkingLevel = thinkingLevel,
            apiKeyOverride = apiKeyOverride,
            enableCodeExecution = enableCodeExecution,
            enableGoogleSearch = enableGoogleSearch,
            onGroundingFound = onGroundingFound
        )
    }

    suspend fun generateImage(
        prompt: String,
        inputImageBase64: String? = null,
        aspectRatio: String = "1:1",
        apiKeyOverride: String? = null
    ): Result<String> {
        return geminiClient.generateImage(
            prompt = prompt,
            inputImageBase64 = inputImageBase64,
            aspectRatio = aspectRatio,
            apiKeyOverride = apiKeyOverride
        )
    }

    suspend fun generateVideo(
        prompt: String,
        imageBase64: String? = null,
        resolution: String = "720p",
        aspectRatio: String = "16:9",
        apiKeyOverride: String? = null
    ): Result<String> {
        return geminiClient.generateVideo(
            prompt = prompt,
            imageBase64 = imageBase64,
            resolution = resolution,
            aspectRatio = aspectRatio,
            apiKeyOverride = apiKeyOverride
        )
    }

    suspend fun decomposeScriptToScenes(
        script: String,
        apiKeyOverride: String? = null
    ): List<com.example.data.model.VideoScene> {
        return geminiClient.decomposeScriptToScenes(script, apiKeyOverride)
    }

    suspend fun factCheckClaim(
        claim: String,
        apiKeyOverride: String? = null
    ): com.example.data.model.FactCheckResult {
        return geminiClient.factCheckClaim(claim, apiKeyOverride)
    }

    val sessionsCount: Flow<Int> = chatDao.getSessionsCount()
    val messagesCount: Flow<Int> = chatDao.getMessagesCount()

    suspend fun getAllMessagesOnce(): List<ChatMessage> {
        return chatDao.getAllMessagesOnce()
    }
}
