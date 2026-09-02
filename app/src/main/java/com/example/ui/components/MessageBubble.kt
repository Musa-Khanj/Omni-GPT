package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
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
import com.example.data.model.GroundingSource
import com.example.data.model.VideoScene
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

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
                                // If assistant generated an image
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
                                            contentDescription = "AI Generated Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .padding(bottom = 10.dp)
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0x33818CF8), RoundedCornerShape(12.dp))
                                        )
                                    }
                                }

                                // Fact check verdict banner if present
                                if (!message.factCheckVerdict.isNullOrBlank()) {
                                    FactCheckVerdictChip(verdict = message.factCheckVerdict)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Formatted markdown content with real-time cursor
                                MarkdownContent(
                                    content = message.content,
                                    onExecuteCode = onExecuteCode
                                )

                                // Video scenes storyboard preview if present
                                if (!message.videoScenesJson.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    VideoScenesCard(
                                        scenesJson = message.videoScenesJson,
                                        aspectRatio = message.generatedVideoAspect ?: "16:9"
                                    )
                                }

                                // Grounding sources if present
                                if (!message.groundingSourcesJson.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    GroundingSourcesSection(
                                        sourcesJson = message.groundingSourcesJson,
                                        queriesJson = message.searchQueriesJson
                                    )
                                }

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

@Composable
fun FactCheckVerdictChip(verdict: String) {
    val isTrue = verdict.contains("True", ignoreCase = true)
    val isFalse = verdict.contains("False", ignoreCase = true) || verdict.contains("Misleading", ignoreCase = true)

    val bgColor = when {
        isTrue -> Color(0x2E10B981)
        isFalse -> Color(0x2EEF4444)
        else -> Color(0x2EF59E0B)
    }
    val textColor = when {
        isTrue -> Color(0xFF34D399)
        isFalse -> Color(0xFFF87171)
        else -> Color(0xFFFBBF24)
    }
    val icon = when {
        isTrue -> Icons.Default.Verified
        isFalse -> Icons.Default.Warning
        else -> Icons.Default.Search
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.4f)),
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Fact-Check Verdict: $verdict",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroundingSourcesSection(
    sourcesJson: String,
    queriesJson: String?
) {
    val context = LocalContext.current
    val sources = remember(sourcesJson) {
        val list = mutableListOf<GroundingSource>()
        try {
            val arr = JSONArray(sourcesJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(GroundingSource(obj.optString("title", "Source"), obj.optString("url", "")))
            }
        } catch (_: Exception) {}
        list
    }

    val queries = remember(queriesJson) {
        val list = mutableListOf<String>()
        if (!queriesJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(queriesJson)
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
            } catch (_: Exception) {}
        }
        list
    }

    if (sources.isEmpty() && queries.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF131422))
            .border(1.dp, Color(0x33818CF8), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = Color(0xFF818CF8),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Grounded with Google Search",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFC7D2FE),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            )
        }

        if (queries.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                queries.forEach { q ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x26FFFFFF)
                    ) {
                        Text(
                            text = "🔍 $q",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        if (sources.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sources.take(6).forEach { src ->
                    val domain = remember(src.url) {
                        try {
                            Uri.parse(src.url).host?.removePrefix("www.") ?: "web source"
                        } catch (_: Exception) {
                            "web source"
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E1F30),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33A5B4FC)),
                        modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(src.url))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = domain,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFA5B4FC),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open link",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoScenesCard(
    scenesJson: String,
    aspectRatio: String
) {
    val scenes = remember(scenesJson) {
        val list = mutableListOf<VideoScene>()
        try {
            val arr = JSONArray(scenesJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    VideoScene(
                        sceneNumber = obj.optInt("sceneNumber", i + 1),
                        title = obj.optString("title", "Scene ${i + 1}"),
                        visualPrompt = obj.optString("visualPrompt", ""),
                        narration = obj.optString("narration", ""),
                        durationSec = obj.optInt("durationSec", 4)
                    )
                )
            }
        } catch (_: Exception) {}
        list
    }

    if (scenes.isEmpty()) return

    var isPlaying by remember { mutableStateOf(false) }
    var activeSceneIndex by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F101F))
            .border(1.dp, Color(0x4D6366F1), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Veo 3 Video Storyboard",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0x336366F1)
            ) {
                Text(
                    text = aspectRatio,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFA5B4FC),
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active scene display box
        val currentScene = scenes.getOrNull(activeSceneIndex) ?: scenes.first()
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF191A2A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x26818CF8)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Scene ${currentScene.sceneNumber}: ${currentScene.title}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "${currentScene.durationSec}s",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🎥 ${currentScene.visualPrompt}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                )
                if (currentScene.narration.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "🎙 \"${currentScene.narration}\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFA5B4FC),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scene timeline navigation & playback
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                scenes.forEachIndexed { index, s ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (activeSceneIndex == index) Color(0xFF6366F1) else Color(0xFF1E2033),
                        modifier = Modifier.clickable {
                            activeSceneIndex = index
                            isPlaying = false
                        }
                    ) {
                        Text(
                            text = "S${s.sceneNumber}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (activeSceneIndex == index) Color.White else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = {
                    if (isPlaying) {
                        isPlaying = false
                    } else {
                        isPlaying = true
                        coroutineScope.launch {
                            for (i in 0 until scenes.size) {
                                if (!isPlaying) break
                                activeSceneIndex = i
                                delay((scenes[i].durationSec * 1000L).coerceAtLeast(2000L))
                            }
                            isPlaying = false
                            activeSceneIndex = 0
                        }
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Playing preview" else "Play preview",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

