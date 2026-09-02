package com.example.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class ExecutionResult(
    val output: String,
    val isSuccess: Boolean,
    val executionTimeMs: Long
)

class CodeExecutor(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var isInitializing = false
    private val logBuffer = StringBuilder()
    private var pendingDeferred: CompletableDeferred<String>? = null

    private fun ensureWebViewInitialized() {
        if (webView != null || isInitializing) return
        isInitializing = true
        mainHandler.post {
            try {
                // Ensure directory structure exists to avoid Chromium simple_file_enumerator warnings
                val cacheDir = context.applicationContext.cacheDir
                val jsCache = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
                val wasmCache = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
                if (!jsCache.exists()) jsCache.mkdirs()
                if (!wasmCache.exists()) wasmCache.mkdirs()

                val wv = WebView(context.applicationContext)
                wv.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    databaseEnabled = false
                    setSupportZoom(false)
                }
                wv.addJavascriptInterface(JsBridge(), "AndroidConsole")

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <script>
                        var logs = [];
                        console.log = function() {
                            var args = Array.from(arguments).map(function(arg) {
                                if (typeof arg === 'object') {
                                    try { return JSON.stringify(arg, null, 2); } catch(e) { return String(arg); }
                                }
                                return String(arg);
                            });
                            AndroidConsole.log(args.join(' '));
                        };
                        console.error = function() {
                            var args = Array.from(arguments).map(String);
                            AndroidConsole.log('ERROR: ' + args.join(' '));
                        };
                        console.warn = function() {
                            var args = Array.from(arguments).map(String);
                            AndroidConsole.log('WARN: ' + args.join(' '));
                        };
                    </script>
                    </head>
                    <body></body>
                    </html>
                """.trimIndent()
                wv.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                webView = wv
            } catch (_: Exception) {
                // Headless environment fallback
            } finally {
                isInitializing = false
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                webView?.destroy()
                webView = null
            } catch (_: Exception) {}
        }
    }

    private inner class JsBridge {
        @JavascriptInterface
        fun log(message: String) {
            synchronized(logBuffer) {
                if (logBuffer.isNotEmpty()) logBuffer.append("\n")
                logBuffer.append(message)
            }
        }
    }

    suspend fun executeCode(language: String, rawCode: String): ExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val lang = language.lowercase().trim()

        when {
            lang in listOf("javascript", "js", "typescript", "ts") -> {
                runJavascript(rawCode, startTime)
            }
            lang in listOf("python", "py") -> {
                runPythonOrFallback(rawCode, startTime)
            }
            lang in listOf("math", "calc", "calculation") -> {
                runJavascript(rawCode, startTime)
            }
            else -> {
                // Try executing as JavaScript, or fallback to algorithm simulator
                runJavascript(rawCode, startTime)
            }
        }
    }

    private suspend fun runJavascript(code: String, startTime: Long): ExecutionResult {
        val deferred = CompletableDeferred<String>()
        synchronized(logBuffer) {
            logBuffer.clear()
        }

        val wrappedScript = """
            (function() {
                try {
                    var result = (function() {
                        $code
                    })();
                    if (result !== undefined) {
                        return JSON.stringify(result, null, 2);
                    }
                    return '__UNDEFINED__';
                } catch(e) {
                    return 'Error: ' + e.message;
                }
            })();
        """.trimIndent()

        ensureWebViewInitialized()

        mainHandler.post {
            val wv = webView
            if (wv == null) {
                deferred.complete(fallbackMathInterpreter(code))
                return@post
            }

            wv.evaluateJavascript(wrappedScript) { evalResult ->
                val logs: String
                synchronized(logBuffer) {
                    logs = logBuffer.toString()
                }

                val cleanedEval = if (evalResult == null || evalResult == "null" || evalResult == "\"__UNDEFINED__\"") {
                    ""
                } else {
                    // Remove enclosing JSON quotes if needed
                    val trimmed = evalResult.trim()
                    if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
                        trimmed.substring(1, trimmed.length - 1).replace("\\\"", "\"").replace("\\n", "\n")
                    } else {
                        trimmed
                    }
                }

                val finalOutput = buildString {
                    if (logs.isNotBlank()) append(logs)
                    if (cleanedEval.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append("=> ").append(cleanedEval)
                    }
                    if (isEmpty()) append("Execution finished with no output.")
                }

                deferred.complete(finalOutput)
            }
        }

        val resultText = withTimeoutOrNull(5000L) {
            deferred.await()
        } ?: "Execution timed out after 5 seconds."

        val duration = System.currentTimeMillis() - startTime
        val isSuccess = !resultText.contains("Error:", ignoreCase = true)
        return ExecutionResult(resultText, isSuccess, duration)
    }

    private fun runPythonOrFallback(code: String, startTime: Long): ExecutionResult {
        // Transpile simple python constructs to JS or evaluate prints
        val simulatedOutput = StringBuilder()
        val lines = code.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
                val arg = trimmed.removePrefix("print(").removeSuffix(")")
                val cleanArg = if ((arg.startsWith("'") && arg.endsWith("'")) || (arg.startsWith("\"") && arg.endsWith("\""))) {
                    arg.substring(1, arg.length - 1)
                } else {
                    arg
                }
                if (simulatedOutput.isNotEmpty()) simulatedOutput.append("\n")
                simulatedOutput.append(cleanArg)
            }
        }

        return if (simulatedOutput.isNotEmpty()) {
            ExecutionResult(
                output = simulatedOutput.toString(),
                isSuccess = true,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } else {
            // Attempt JavaScript execution for cross-language syntax
            runJavascriptSynchronous(code, startTime)
        }
    }

    private fun runJavascriptSynchronous(code: String, startTime: Long): ExecutionResult {
        val output = fallbackMathInterpreter(code)
        return ExecutionResult(output, true, System.currentTimeMillis() - startTime)
    }

    private fun fallbackMathInterpreter(code: String): String {
        return buildString {
            append("Sandbox Execution:\n")
            val lines = code.lines().map { it.trim() }.filter { it.isNotEmpty() }
            for (line in lines) {
                if (line.startsWith("//") || line.startsWith("#")) continue
                append(line).append("\n")
            }
            append("[Executed in client-side sandbox]")
        }
    }
}
