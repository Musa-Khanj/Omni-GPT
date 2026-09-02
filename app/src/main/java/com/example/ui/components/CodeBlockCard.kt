package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CodeDarkBg
import com.example.ui.theme.CodeHeaderDark
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CodeExecutor
import com.example.util.ExecutionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    onExecute: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }
    var isExecuting by remember { mutableStateOf(false) }
    var executionResult by remember { mutableStateOf<ExecutionResult?>(null) }
    var showTerminal by remember { mutableStateOf(false) }

    val codeExecutor = remember { CodeExecutor(context) }
    DisposableEffect(Unit) {
        onDispose {
            codeExecutor.destroy()
        }
    }
    val displayLang = if (language.isBlank() || language == "code") "code" else language.lowercase()
    val displayFileName = when (displayLang) {
        "python", "py" -> "python_executor.py"
        "javascript", "js" -> "script_runner.js"
        "kotlin", "kt" -> "Main.kt"
        "json" -> "payload.json"
        "html" -> "index.html"
        else -> "${displayLang}_snippet"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x4D6366F1), RoundedCornerShape(12.dp)) // border-indigo-500/30
            .testTag("code_block_$displayLang"),
        color = Color(0xCC05060A) // bg-black/80
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0E18))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (executionResult?.isSuccess == true) Color(0xFF4ADE80)
                                else Color(0xFF818CF8)
                            )
                    )
                    Spacer(modifier = Modifier.width(7.dp))

                    Text(
                        text = displayFileName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF818CF8), // text-indigo-400 font-mono
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    if (executionResult != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (executionResult?.isSuccess == true) "● Success" else "● Error",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (executionResult?.isSuccess == true) Color(0xFF4ADE80) else Color(0xFFEF4444),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Execute button
                    IconButton(
                        onClick = {
                            if (isExecuting) return@IconButton
                            isExecuting = true
                            showTerminal = true
                            coroutineScope.launch {
                                val result = codeExecutor.executeCode(displayLang, code)
                                executionResult = result
                                isExecuting = false
                                onExecute?.invoke(displayLang, result.output)
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("run_code_button")
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldPrimary
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x334F46E5))
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Run code",
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Run",
                                    color = Color(0xFF818CF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Copy button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Code snippet", code)
                            clipboard.setPrimaryClip(clip)
                            isCopied = true
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                            coroutineScope.launch {
                                delay(2000)
                                isCopied = false
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("copy_code_button")
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (isCopied) EmeraldPrimary else Color(0xFF8B949E),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Code Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE6EDF3),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                )
            }

            // Terminal output window
            AnimatedVisibility(
                visible = showTerminal,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF040D14))
                        .border(1.dp, Color(0xFF1B2A38))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Terminal Output",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Console Output",
                                color = Color(0xFF58A6FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            executionResult?.let { res ->
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${res.executionTimeMs}ms)",
                                    color = Color(0xFF8B949E),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = "Close",
                            color = Color(0xFF8B949E),
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp)
                                .clickable { showTerminal = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isExecuting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Executing in sandbox...",
                                color = Color(0xFF8B949E),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else if (executionResult != null) {
                        val res = executionResult!!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = res.output,
                                color = if (res.isSuccess) Color(0xFF7EE787) else Color(0xFFFFA198),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
