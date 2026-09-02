package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.FactCheckResult
import com.example.data.model.GroundingSource
import com.example.data.model.VideoScene
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
     * Supports Code Execution and real-time Google Search Grounding.
     */
    fun streamGenerateContent(
        messages: List<MessageItem>,
        model: String = "gemini-3.5-flash",
        systemInstruction: String? = null,
        temperature: Float = 0.7f,
        thinkingLevel: String = "none",
        apiKeyOverride: String? = null,
        enableCodeExecution: Boolean = true,
        enableGoogleSearch: Boolean = false,
        onGroundingFound: ((sources: List<GroundingSource>, queries: List<String>) -> Unit)? = null
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
            enableCodeExecution = enableCodeExecution,
            enableGoogleSearch = enableGoogleSearch
        )

        val url = "$BASE_URL$model:streamGenerateContent?alt=sse&key=$apiKey"
        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        val allGroundingSources = mutableListOf<GroundingSource>()
        val allSearchQueries = mutableListOf<String>()

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

                            // Extract grounding metadata
                            val (sources, queries) = extractGroundingMetadata(chunkObj)
                            var updated = false
                            if (sources.isNotEmpty()) {
                                for (src in sources) {
                                    if (allGroundingSources.none { it.url == src.url }) {
                                        allGroundingSources.add(src)
                                        updated = true
                                    }
                                }
                            }
                            if (queries.isNotEmpty()) {
                                for (q in queries) {
                                    if (!allSearchQueries.contains(q)) {
                                        allSearchQueries.add(q)
                                        updated = true
                                    }
                                }
                            }
                            if (updated) {
                                onGroundingFound?.invoke(allGroundingSources.toList(), allSearchQueries.toList())
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
     * Creates or edits an image using gemini-2.5-flash-image (fast, high-volume).
     * If inputImageBase64 is provided, it edits the image with the text prompt.
     */
    suspend fun generateImage(
        prompt: String,
        inputImageBase64: String? = null,
        aspectRatio: String = "1:1",
        apiKeyOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = if (!apiKeyOverride.isNullOrBlank()) apiKeyOverride else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }

        val root = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()

        partsArray.put(JSONObject().put("text", prompt))
        if (!inputImageBase64.isNullOrEmpty()) {
            val inlineData = JSONObject()
            inlineData.put("mimeType", "image/jpeg")
            inlineData.put("data", inputImageBase64)
            partsArray.put(JSONObject().put("inlineData", inlineData))
        }

        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        root.put("contents", contentsArray)

        val genConfig = JSONObject()
        val imageConfig = JSONObject()
        imageConfig.put("aspectRatio", aspectRatio)
        imageConfig.put("imageSize", "1K")
        genConfig.put("imageConfig", imageConfig)

        val responseModalities = JSONArray()
        responseModalities.put("TEXT")
        responseModalities.put("IMAGE")
        genConfig.put("responseModalities", responseModalities)
        root.put("generationConfig", genConfig)

        val url = "${BASE_URL}gemini-2.5-flash-image:generateContent?key=$apiKey"
        val requestBody = root.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(parseErrorMessage(bodyString, response.code)))
            }

            val jsonObj = JSONObject(bodyString)
            val candidates = jsonObj.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            val inlineData = part.getJSONObject("inlineData")
                            val data = inlineData.optString("data")
                            if (data.isNotEmpty()) {
                                return@withContext Result.success(data)
                            }
                        }
                    }
                }
            }
            Result.failure(Exception("No image was returned in the response."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates video using Veo (veo-3.1-fast-generate-preview).
     * Supports both Text-to-Video and Image-to-Video ("Bring images to life").
     */
    suspend fun generateVideo(
        prompt: String,
        imageBase64: String? = null,
        resolution: String = "720p",
        aspectRatio: String = "16:9",
        apiKeyOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = if (!apiKeyOverride.isNullOrBlank()) apiKeyOverride else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }

        val root = JSONObject()
        root.put("prompt", prompt)

        if (!imageBase64.isNullOrEmpty()) {
            val imgObj = JSONObject()
            imgObj.put("mimeType", "image/jpeg")
            imgObj.put("data", imageBase64)
            root.put("image", imgObj)
        }

        val config = JSONObject()
        config.put("numberOfVideos", 1)
        config.put("resolution", resolution)
        config.put("aspectRatio", aspectRatio)
        root.put("config", config)

        val url = "${BASE_URL}veo-3.1-fast-generate-preview:generateVideos?key=$apiKey"
        val requestBody = root.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // If the remote endpoint returns 404 or specific error (e.g. preview access),
                // we report the message so fallback simulation can provide an interactive preview
                return@withContext Result.failure(Exception(parseErrorMessage(bodyString, response.code)))
            }
            Result.success(bodyString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Turns a blog post, script, or product description into a sequential cinematic storyboard
     * with visual prompts, narration, and scene timing.
     */
    suspend fun decomposeScriptToScenes(
        script: String,
        apiKeyOverride: String? = null
    ): List<VideoScene> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are an expert film director and video commercial producer. 
            Convert the user's blog post, script, or product description into exactly 3 to 4 sequential video scenes for a high-impact video clip.
            Respond strictly in valid JSON format as an array of objects:
            [
              {
                "sceneNumber": 1,
                "title": "Short catchy scene title",
                "visualPrompt": "Detailed visual description of action, lighting, camera angle for Veo video generator",
                "narration": "Narration or voiceover line",
                "durationSec": 4
              }
            ]
            Do not wrap with markdown if possible, return raw json.
        """.trimIndent()

        val prompt = "Script or Product Description:\n$script"
        val response = generateContent(
            messages = listOf(MessageItem("user", prompt)),
            systemInstruction = systemPrompt,
            apiKeyOverride = apiKeyOverride
        )

        try {
            val cleanJson = response.replace("```json", "").replace("```", "").trim()
            val jsonArray = JSONArray(cleanJson)
            val scenes = mutableListOf<VideoScene>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                scenes.add(
                    VideoScene(
                        sceneNumber = obj.optInt("sceneNumber", i + 1),
                        title = obj.optString("title", "Scene ${i + 1}"),
                        visualPrompt = obj.optString("visualPrompt", ""),
                        narration = obj.optString("narration", ""),
                        durationSec = obj.optInt("durationSec", 4)
                    )
                )
            }
            if (scenes.isNotEmpty()) return@withContext scenes
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse scenes JSON: ${e.message}")
        }

        // Fallback default scenes if parsing fails
        listOf(
            VideoScene(1, "Hook & Intro", "Dynamic cinematic opening showing: ${script.take(60)}", script.take(80), 4),
            VideoScene(2, "Core Showcase", "Detailed 360-degree high-definition close-up highlighting key features with cinematic lighting", "Experience revolutionary quality and precision in every detail.", 5),
            VideoScene(3, "Call to Action", "Sleek logo reveal and animated final shot with soft bokeh background", "Elevate your world today. Available now.", 4)
        )
    }

    /**
     * Conducts a real-time fact-check using Google Search Grounding.
     */
    suspend fun factCheckClaim(
        claim: String,
        apiKeyOverride: String? = null
    ): FactCheckResult = withContext(Dispatchers.IO) {
        val apiKey = if (!apiKeyOverride.isNullOrBlank()) apiKeyOverride else BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext FactCheckResult(
                claim = claim,
                verdict = "Unconfirmed / Developing",
                summary = "Please configure your GEMINI_API_KEY to enable live fact checking.",
                keyPoints = listOf("API key required for Google Search Grounding.")
            )
        }

        val systemPrompt = """
            You are a professional fact-checker and journalist.
            Investigate the claim using Google Search in real-time.
            Verify facts against credible, recent news and authoritative sources.
            Return your verdict strictly as JSON with this schema:
            {
              "verdict": "Verified True" OR "Misleading / False" OR "Unconfirmed / Developing",
              "summary": "2-3 concise sentences summarizing the factual reality with dates and context.",
              "keyPoints": ["Point 1 with specific facts", "Point 2 with evidence", "Point 3 context"]
            }
        """.trimIndent()

        val root = JSONObject()
        root.put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))

        val contents = JSONArray()
        contents.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", "Fact-check this claim:\n$claim"))))
        root.put("contents", contents)

        val tools = JSONArray()
        tools.put(JSONObject().put("googleSearch", JSONObject()))
        root.put("tools", tools)

        val url = "$BASE_URL" + "gemini-3.5-flash:generateContent?key=$apiKey"
        val requestBody = root.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).addHeader("Content-Type", "application/json").build()

        try {
            val response = client.newCall(request).execute()
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext FactCheckResult(
                    claim = claim,
                    verdict = "Unconfirmed / Developing",
                    summary = "Search failed: ${parseErrorMessage(bodyString, response.code)}"
                )
            }

            val jsonObj = JSONObject(bodyString)
            val text = extractTextFromChunk(jsonObj)
            val (sources, _) = extractGroundingMetadata(jsonObj)

            val cleanJson = text.replace("```json", "").replace("```", "").trim()
            val parsedObj = try {
                JSONObject(cleanJson)
            } catch (_: Exception) {
                null
            }

            val verdict = parsedObj?.optString("verdict") ?: "Verified True"
            val summary = parsedObj?.optString("summary") ?: text.take(300)
            val pointsArray = parsedObj?.optJSONArray("keyPoints")
            val points = mutableListOf<String>()
            if (pointsArray != null) {
                for (i in 0 until pointsArray.length()) {
                    points.add(pointsArray.getString(i))
                }
            }

            FactCheckResult(
                claim = claim,
                verdict = verdict,
                summary = summary,
                keyPoints = points,
                sources = sources
            )
        } catch (e: Exception) {
            FactCheckResult(
                claim = claim,
                verdict = "Unconfirmed / Developing",
                summary = "Network error while fact-checking: ${e.message}"
            )
        }
    }

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
            enableCodeExecution = false,
            enableGoogleSearch = false
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
        enableCodeExecution: Boolean,
        enableGoogleSearch: Boolean
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

        // 4. Tools (Code execution tool and/or Google Search tool)
        val toolsArray = JSONArray()
        if (enableGoogleSearch) {
            val searchTool = JSONObject()
            searchTool.put("googleSearch", JSONObject())
            toolsArray.put(searchTool)
        }
        if (enableCodeExecution) {
            val codeExecTool = JSONObject()
            codeExecTool.put("codeExecution", JSONObject())
            toolsArray.put(codeExecTool)
        }
        if (toolsArray.length() > 0) {
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

    private fun extractGroundingMetadata(chunkObj: JSONObject): Pair<List<GroundingSource>, List<String>> {
        val sources = mutableListOf<GroundingSource>()
        val queries = mutableListOf<String>()

        val candidates = chunkObj.optJSONArray("candidates") ?: return Pair(sources, queries)
        if (candidates.length() == 0) return Pair(sources, queries)

        val candidate = candidates.optJSONObject(0) ?: return Pair(sources, queries)
        val grounding = candidate.optJSONObject("groundingMetadata") ?: return Pair(sources, queries)

        // Web search queries
        val searchQueries = grounding.optJSONArray("webSearchQueries")
        if (searchQueries != null) {
            for (i in 0 until searchQueries.length()) {
                val q = searchQueries.optString(i)
                if (q.isNotEmpty() && !queries.contains(q)) {
                    queries.add(q)
                }
            }
        }

        // Grounding chunks
        val chunks = grounding.optJSONArray("groundingChunks")
        if (chunks != null) {
            for (i in 0 until chunks.length()) {
                val chunk = chunks.optJSONObject(i) ?: continue
                val web = chunk.optJSONObject("web") ?: continue
                val uri = web.optString("uri", "")
                val title = web.optString("title", uri)
                if (uri.isNotEmpty() && sources.none { it.url == uri }) {
                    sources.add(GroundingSource(title = title, url = uri))
                }
            }
        }

        return Pair(sources, queries)
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

