package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ChatMessage
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MessageBubble(
    message: ChatMessage,
    isStreaming: Boolean = false,
    isSpeaking: Boolean = false,
    onSpeakToggle: () -> Unit = {},
    onRegenerate: () -> Unit = {},
    onExecuteCode: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(if (isUser) "user_message_bubble" else "assistant_message_bubble"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // Immersive AI Avatar with glowing indigo border
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFF4338CA), Color(0xFF6366F1))
                        )
                    )
                    .border(1.5.dp, Color(0x66818CF8), CircleShape)
                    .padding(5.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "ChatGPT",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isUser) {
                // Attached Image preview with immersive border
                if (!message.imageBase64.isNullOrEmpty()) {
                    val bitmap = remember(message.imageBase64) {
                        try {
                            val bytes = Base64.decode(message.imageBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (_: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "User attached image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .size(180.dp, 120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0x4D6366F1), RoundedCornerShape(12.dp))
                        )
                    }
                } else if (!message.imageUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = message.imageUri,
                        contentDescription = "User image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .size(180.dp, 120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0x4D6366F1), RoundedCornerShape(12.dp))
                    )
                }

                // User message: Immersive rich indigo bubble with subtle glow
                Surface(
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                    color = Color(0xE64F46E5), // bg-indigo-600/90
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33818CF8)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            color = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            } else {
                // Assistant Message Container with Immersive glass card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                ) {
                    // AI Assistant Subtitle Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF818CF8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Assistant",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
                        color = Color(0xFF0F101A), // Glass obsidian surface
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24FFFFFF)), // 1px solid rgba(255, 255, 255, 0.08)
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            if (message.content.isEmpty() && isStreaming) {
                                // Thinking animation indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    PulsingCursor()
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Omni-GPT is analyzing...",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF94A3B8),
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    )
                                }
                            } else {
                                // Formatted markdown content with real-time cursor
                                MarkdownContent(
                                    content = message.content,
                                    onExecuteCode = onExecuteCode
                                )

                                if (isStreaming) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        PulsingCursor()
                                    }
                                }
                            }
                        }
                    }

                    // Assistant Action Toolbar (Copy, Read Aloud, Regenerate)
                    if (message.content.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Copy button
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ChatGPT Response", message.content)
                                    clipboard.setPrimaryClip(clip)
                                    isCopied = true
                                    Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        delay(2000)
                                        isCopied = false
                                    }
                                },
                                modifier = Modifier.size(32.dp).testTag("copy_response_button")
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy message",
                                    tint = if (isCopied) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Read Aloud / TTS button
                            IconButton(
                                onClick = onSpeakToggle,
                                modifier = Modifier.size(32.dp).testTag("tts_speak_button")
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isSpeaking) "Stop voice" else "Read aloud",
                                    tint = if (isSpeaking) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            if (isSpeaking) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Speaking waves",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Regenerate button
                            IconButton(
                                onClick = onRegenerate,
                                modifier = Modifier.size(32.dp).testTag("regenerate_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate answer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User Avatar
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PulsingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp, 16.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(EmeraldPrimary.copy(alpha = alpha))
    )
}
