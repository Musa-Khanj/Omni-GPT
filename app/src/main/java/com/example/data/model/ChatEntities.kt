package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val model: String = "gemini-3.5-flash",
    val isPinned: Boolean = false
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessage(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: String, // "user", "model", "system"
    val content: String,
    val imageBase64: String? = null,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val codeExecutionOutput: String? = null,
    val isError: Boolean = false,
    val generatedVideoPrompt: String? = null,
    val generatedVideoAspect: String? = null,
    val videoScenesJson: String? = null,
    val groundingSourcesJson: String? = null,
    val searchQueriesJson: String? = null,
    val factCheckVerdict: String? = null
)

data class GroundingSource(
    val title: String,
    val url: String
)

data class VideoScene(
    val sceneNumber: Int,
    val title: String,
    val visualPrompt: String,
    val narration: String,
    val durationSec: Int = 4
)

data class FactCheckResult(
    val claim: String,
    val verdict: String, // "Verified True", "Misleading / False", "Unconfirmed / Developing"
    val summary: String,
    val keyPoints: List<String> = emptyList(),
    val sources: List<GroundingSource> = emptyList()
)

data class GeneratedImageItem(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val base64: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeneratedVideoItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val script: String,
    val visualPrompt: String,
    val sourceImageBase64: String? = null,
    val aspectRatio: String = "16:9",
    val durationSec: Int = 6,
    val scenes: List<VideoScene> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class UserPreferences(
    val personaName: String = "ChatGPT Standard",
    val systemInstruction: String = "You are ChatGPT, a helpful, witty, versatile, and precise AI assistant. Provide human-like text generation, clear code examples, and insightful explanations.",
    val model: String = "gemini-3.5-flash",
    val temperature: Float = 0.7f,
    val thinkingLevel: String = "none", // "none", "low", "medium", "high"
    val autoRunCode: Boolean = false,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val isDarkTheme: Boolean = true,
    val customApiKey: String = "",
    val enableGoogleSearch: Boolean = false
)

data class DashboardStats(
    val totalSessions: Int = 0,
    val totalMessages: Int = 0,
    val totalCodeExecutions: Int = 0,
    val estimatedWordsGenerated: Int = 0,
    val activePersona: String = "ChatGPT Standard"
)
