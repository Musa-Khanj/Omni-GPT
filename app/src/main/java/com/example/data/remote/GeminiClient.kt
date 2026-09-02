package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class GeminiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "GeminiClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    }

    data class MessageItem(
        val role: String, // "user" or "model"
        val text: String,
        val imageBase64: String? = null
    )

    /**
     * Streams content in real-time from Gemini using Server-Sent Events (SSE).
     */
    fun streamGenerateContent(
        messages: List<MessageItem>,
        model: String = "gemini-3.5-flash",
        systemInstruction: String? = null,
        temperature: Float = 0.7f,
        thinkingLevel: String = "none",
        apiKeyOverride: String? = null,
        enableCodeExecution: Boolean = true
    ): Flow<String> = flow {
        val apiKey = if (!apiKeyOverride.isNullOrBlank()) apiKeyOverride else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            emit("⚠️ **Gemini API Key Missing**\nPlease configure your `GEMINI_API_KEY` in the AI Studio Secrets panel or enter it in the app's Dashboard Settings.")
            return@flow
        }

        val requestJson = buildRequestBody(
            messages = messages,
            systemInstruction = systemInstruction,
            temperature = temperature,
            thinkingLevel = thinkingLevel,
            enableCodeExecution = enableCodeExecution
        )

        val url = "$BASE_URL$model:streamGenerateContent?alt=sse&key=$apiKey"
        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                val errorMsg = parseErrorMessage(errorBody, response.code)
                emit("\n\n❌ **Error ${response.code}**: $errorMsg")
                return@flow
            }

            val responseBody = response.body ?: throw IllegalStateException("Empty response body")
            val reader = BufferedReader(InputStreamReader(responseBody.byteStream(), Charsets.UTF_8))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.startsWith("data: ")) {
                    val jsonStr = currentLine.removePrefix("data: ").trim()
                    if (jsonStr.isNotEmpty()) {
                        try {
                            val chunkObj = JSONObject(jsonStr)
                            val chunkText = extractTextFromChunk(chunkObj)
                            if (chunkText.isNotEmpty()) {
                                emit(chunkText)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parsing SSE chunk: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Streaming request failed", e)
            emit("\n\n❌ **Connection Error**: ${e.localizedMessage ?: "Failed to connect to AI server"}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Single-shot generation for tasks like image description or one-off prompts.
     */
    suspend fun generateContent(
        messages: List<MessageItem>,
        model: String = "gemini-3.5-flash",
        systemInstruction: String? = null,
        temperature: Float = 0.7f,
        apiKeyOverride: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (!apiKeyOverride.isNullOrBlank()) apiKeyOverride else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Please configure your GEMINI_API_KEY in the Secrets panel or Dashboard Settings."
        }

        val requestJson = buildRequestBody(
            messages = messages,
            systemInstruction = systemInstruction,
            temperature = temperature,
            thinkingLevel = "none",
            enableCodeExecution = false
        )

        val url = "$BASE_URL$model:generateContent?key=$apiKey"
        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext "Error ${response.code}: ${parseErrorMessage(bodyString, response.code)}"
            }
            val jsonObj = JSONObject(bodyString)
            extractTextFromChunk(jsonObj).ifEmpty { "No response generated." }
        } catch (e: Exception) {
            "Network error: ${e.message}"
        }
    }

    private fun buildRequestBody(
        messages: List<MessageItem>,
        systemInstruction: String?,
        temperature: Float,
        thinkingLevel: String,
        enableCodeExecution: Boolean
    ): JSONObject {
        val root = JSONObject()

        // 1. System instruction
        if (!systemInstruction.isNullOrBlank()) {
            val sysObj = JSONObject()
            val sysParts = JSONArray().put(JSONObject().put("text", systemInstruction))
            sysObj.put("parts", sysParts)
            root.put("systemInstruction", sysObj)
        }

        // 2. Contents
        val contentsArray = JSONArray()
        for (msg in messages) {
            val contentObj = JSONObject()
            contentObj.put("role", if (msg.role == "assistant" || msg.role == "model") "model" else "user")
            val partsArray = JSONArray()

            if (msg.text.isNotEmpty()) {
                partsArray.put(JSONObject().put("text", msg.text))
            }

            if (!msg.imageBase64.isNullOrEmpty()) {
                val inlineDataObj = JSONObject()
                inlineDataObj.put("mimeType", "image/jpeg")
                inlineDataObj.put("data", msg.imageBase64)
                partsArray.put(JSONObject().put("inlineData", inlineDataObj))
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }
        root.put("contents", contentsArray)

        // 3. Generation config
        val genConfig = JSONObject()
        genConfig.put("temperature", temperature.toDouble())
        if (thinkingLevel != "none") {
            val thinkingConfig = JSONObject()
            thinkingConfig.put("thinkingLevel", thinkingLevel)
            genConfig.put("thinkingConfig", thinkingConfig)
        }
        root.put("generationConfig", genConfig)

        // 4. Tools (Code execution tool)
        if (enableCodeExecution) {
            val toolsArray = JSONArray()
            val codeExecTool = JSONObject()
            codeExecTool.put("codeExecution", JSONObject())
            toolsArray.put(codeExecTool)
            root.put("tools", toolsArray)
        }

        return root
    }

    private fun extractTextFromChunk(chunkObj: JSONObject): String {
        val builder = StringBuilder()
        val candidates = chunkObj.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""

        val candidate = candidates.optJSONObject(0) ?: return ""
        val content = candidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""

        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue

            // Standard text
            if (part.has("text")) {
                builder.append(part.getString("text"))
            }

            // Executable code block generated by tool
            if (part.has("executableCode")) {
                val execCode = part.getJSONObject("executableCode")
                val lang = execCode.optString("language", "python").lowercase()
                val code = execCode.optString("code", "")
                builder.append("\n```$lang\n$code\n```\n")
            }

            // Code execution result output from tool
            if (part.has("codeExecutionResult")) {
                val execResult = part.getJSONObject("codeExecutionResult")
                val outcome = execResult.optString("outcome", "")
                val output = execResult.optString("output", "")
                builder.append("\n> **Code Execution Result ($outcome)**:\n```console\n$output\n```\n")
            }
        }
        return builder.toString()
    }

    private fun parseErrorMessage(errorBody: String, statusCode: Int): String {
        return try {
            val json = JSONObject(errorBody)
            val error = json.optJSONObject("error")
            error?.optString("message", "Request failed with HTTP $statusCode") ?: "Request failed with HTTP $statusCode"
        } catch (_: Exception) {
            "HTTP $statusCode: $errorBody"
        }
    }
}
