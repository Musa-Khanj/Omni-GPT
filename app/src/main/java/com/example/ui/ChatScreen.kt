package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.ChatDrawer
import com.example.ui.components.DashboardView
import com.example.ui.components.MessageBubble
import com.example.ui.components.SupportedModels
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.launch

enum class AppScreen {
    CHAT,
    VIDEO_STUDIO,
    IMAGE_STUDIO,
    SEARCH_FACT_CHECK,
    DASHBOARD
}

val QuickPrompts = listOf(
    "Write a Python script to sort and analyze data",
    "Explain quantum computing like I'm five",
    "Create an interactive JavaScript game loop",
    "Give me 5 creative ideas for an Android app"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val preferences by viewModel.userPreferences.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val attachedImageUri by viewModel.attachedImageUri.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val currentSpeakingId by viewModel.currentSpeakingId.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }
    var showModelMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto scroll to bottom when messages update
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.attachImage(uri)
        }
    }

    // Audio recording permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.speechManager.startListening { dictatedText ->
                inputPrompt = if (inputPrompt.isBlank()) dictatedText else "$inputPrompt $dictatedText"
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    when (currentScreen) {
        AppScreen.DASHBOARD -> {
            DashboardView(
                preferences = preferences,
                stats = stats,
                onPreferencesChanged = { viewModel.updatePreferences(it) },
                onTestVoice = {
                    viewModel.speechManager.speak(
                        text = "Hello! I am ChatGPT, your multimodal AI assistant.",
                        messageId = "test_voice",
                        speed = preferences.speechRate,
                        pitch = preferences.speechPitch
                    )
                },
                onExportHistory = {
                    val markdown = viewModel.exportHistoryAsMarkdown()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Chat History", markdown)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "History copied to clipboard as Markdown", Toast.LENGTH_SHORT).show()
                },
                onClearAllHistory = {
                    viewModel.clearAllHistory()
                    currentScreen = AppScreen.CHAT
                },
                onBack = { currentScreen = AppScreen.CHAT }
            )
            return
        }
        AppScreen.VIDEO_STUDIO -> {
            VideoStudioScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.CHAT }
            )
            return
        }
        AppScreen.IMAGE_STUDIO -> {
            ImageStudioScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.CHAT },
                onNavigateToVideoWithImage = { base64 ->
                    viewModel.animateImageWithVeo(
                        imageBitmapBase64 = base64,
                        animationPrompt = "Dynamic cinematic 360 camera motion",
                        stylePreset = "Dynamic Product Ad"
                    )
                    currentScreen = AppScreen.VIDEO_STUDIO
                }
            )
            return
        }
        AppScreen.SEARCH_FACT_CHECK -> {
            SearchFactCheckScreen(
                viewModel = viewModel,
                onBack = { currentScreen = AppScreen.CHAT },
                onOpenChat = { currentScreen = AppScreen.CHAT }
            )
            return
        }
        AppScreen.CHAT -> { /* Continue to chat Scaffold */ }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ChatDrawer(
                sessions = sessions,
                currentSessionId = currentSessionId,
                onSessionSelected = { session ->
                    viewModel.selectSession(session)
                    coroutineScope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.createNewChat()
                    coroutineScope.launch { drawerState.close() }
                },
                onRenameSession = { id, title -> viewModel.renameSession(id, title) },
                onDeleteSession = { id -> viewModel.deleteSession(id) },
                onClearAll = {
                    viewModel.clearAllHistory()
                    coroutineScope.launch { drawerState.close() }
                },
                onOpenDashboard = {
                    coroutineScope.launch { drawerState.close() }
                    currentScreen = AppScreen.DASHBOARD
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                // Immersive UI Glowing Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0x40673AB7), // glow-bg
                                    Color(0x1C4F46E5),
                                    Color(0xFF050505)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Identity Block
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { coroutineScope.launch { drawerState.open() } }
                        ) {
                            // Glowing avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4F46E5))
                                    .border(1.5.dp, Color(0x80818CF8), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "Open menu",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Omni-GPT",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 17.sp,
                                            color = Color.White
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Compact model dropdown pill
                                    Box {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0x22FFFFFF))
                                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                                .clickable { showModelMenu = true }
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .testTag("model_selector_pill")
                                        ) {
                                            val currentModelLabel = when (preferences.model) {
                                                "gemini-3.1-pro-preview" -> "o1 Pro"
                                                "gemini-2.5-flash-image" -> "Vision"
                                                else -> "4o"
                                            }
                                            Text(
                                                text = currentModelLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFFA5B4FC),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            )
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Select Model",
                                                tint = Color(0xFFA5B4FC),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showModelMenu,
                                            onDismissRequest = { showModelMenu = false }
                                        ) {
                                            SupportedModels.forEach { (modelKey, label) ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = label,
                                                            fontWeight = if (preferences.model == modelKey) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (preferences.model == modelKey) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    onClick = {
                                                        viewModel.updatePreferences(preferences.copy(model = modelKey))
                                                        showModelMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "MULTIMODAL ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        letterSpacing = 1.2.sp,
                                        color = Color(0xFF818CF8),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // Right Glass Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Google Search Grounding Quick Toggle Pill
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (preferences.enableGoogleSearch) Color(0x330EA5E9) else Color(0x14FFFFFF),
                                border = BorderStroke(1.dp, if (preferences.enableGoogleSearch) Color(0xFF38BDF8) else Color(0x22FFFFFF)),
                                modifier = Modifier
                                    .clickable { viewModel.toggleGoogleSearch() }
                                    .testTag("appbar_search_grounding_toggle")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Search Grounding",
                                        tint = if (preferences.enableGoogleSearch) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (preferences.enableGoogleSearch) "Search ON" else "Search",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (preferences.enableGoogleSearch) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = if (preferences.enableGoogleSearch) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }

                            // New Chat button
                            IconButton(
                                onClick = { viewModel.createNewChat() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1FFFFFFF))
                                    .border(1.dp, Color(0x28FFFFFF), CircleShape)
                                    .testTag("appbar_new_chat_button")
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "New Chat",
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // History Drawer button
                            IconButton(
                                onClick = { coroutineScope.launch { drawerState.open() } },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x14FFFFFF))
                                    .border(1.dp, Color(0x1FFFFFFF), CircleShape)
                                    .testTag("drawer_toggle_button")
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "History",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Dashboard / Tune button
                            IconButton(
                                onClick = { currentScreen = AppScreen.DASHBOARD },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x14FFFFFF))
                                    .border(1.dp, Color(0x1FFFFFFF), CircleShape)
                                    .testTag("appbar_dashboard_button")
                            ) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "Tune Dashboard",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Messages List or Empty Welcome State
                if (messages.isEmpty()) {
                    EmptyChatWelcome(
                        onPromptSelected = { prompt ->
                            inputPrompt = prompt
                            viewModel.sendMessage(prompt)
                            inputPrompt = ""
                        },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            val isThisSpeaking = isSpeaking && currentSpeakingId == message.id
                            val isThisStreaming = isGenerating && message.id == messages.lastOrNull()?.id && message.role == "model"
                            MessageBubble(
                                message = message,
                                isStreaming = isThisStreaming,
                                isSpeaking = isThisSpeaking,
                                onSpeakToggle = {
                                    viewModel.toggleSpeakMessage(message.id, message.content)
                                },
                                onRegenerate = { viewModel.regenerateLastResponse() },
                                onExecuteCode = { _, _ -> viewModel.recordCodeExecution() },
                                onSpeakText = { text ->
                                    viewModel.toggleSpeakMessage("scene_${message.id}", text)
                                }
                            )
                        }
                    }
                }

                // Attached image preview banner
                AnimatedVisibility(
                    visible = attachedImageUri != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    attachedImageUri?.let { uri ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(60.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Attached Image",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .clickable { viewModel.removeAttachedImage() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Image attached (Multimodal prompt)",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                // Action Pills (Horizontally scrollable glass pills)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val pills = listOf(
                        "🎬 Veo 3 Video Studio" to { currentScreen = AppScreen.VIDEO_STUDIO },
                        "🎨 Flash Image Studio" to { currentScreen = AppScreen.IMAGE_STUDIO },
                        "🔍 Fact-Check Claim" to { currentScreen = AppScreen.SEARCH_FACT_CHECK },
                        (if (preferences.enableGoogleSearch) "🌐 Live Search: ON" else "🌐 Live Search: OFF") to { viewModel.toggleGoogleSearch() },
                        "📁 Attach Image" to {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        "✨ Creative Script" to { inputPrompt = "Write a creative short video script about " },
                        "⚡ ChatGPT 4o" to {
                            viewModel.updatePreferences(preferences.copy(model = "gemini-3.5-flash"))
                        },
                        "🧠 ChatGPT o1 Pro" to {
                            viewModel.updatePreferences(preferences.copy(model = "gemini-3.1-pro-preview"))
                        }
                    )

                    pills.forEach { (label, action) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x1AFFFFFF),
                            border = BorderStroke(1.dp, Color(0x2EFFFFFF)),
                            modifier = Modifier.clickable { action() }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Chat Input Bar - Immersive Glass Capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xCC0E0F1A), // glass obsidian
                        border = BorderStroke(1.dp, Color(0x33818CF8)), // subtle indigo glow border
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Photo Picker Button
                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("attach_image_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Attach image",
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // Text Field
                            OutlinedTextField(
                                value = inputPrompt,
                                onValueChange = { inputPrompt = it },
                                placeholder = {
                                    Text(
                                        text = if (isListening) "Listening..." else "Ask Omni anything...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp
                                        )
                                    )
                                },
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    cursorColor = Color(0xFF818CF8)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .testTag("message_input_field")
                            )

                            // Voice Dictation Button
                            IconButton(
                                onClick = {
                                    if (isListening) {
                                        viewModel.speechManager.stopListening()
                                    } else {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPermission) {
                                            viewModel.speechManager.startListening { text ->
                                                inputPrompt = if (inputPrompt.isBlank()) text else "$inputPrompt $text"
                                            }
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("voice_input_button")
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Dictation",
                                    tint = if (isListening) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Send / Stop Button with Indigo Shadow Glow
                            IconButton(
                                onClick = {
                                    if (isGenerating) {
                                        viewModel.cancelGeneration()
                                    } else {
                                        if (inputPrompt.isNotBlank() || attachedImageUri != null) {
                                            val text = inputPrompt
                                            inputPrompt = ""
                                            viewModel.sendMessage(text)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isGenerating) Color(0xFFEF4444)
                                        else if (inputPrompt.isNotBlank() || attachedImageUri != null) Color(0xFF4F46E5)
                                        else Color(0x334F46E5)
                                    )
                                    .border(1.dp, Color(0x4D818CF8), CircleShape)
                                    .testTag("send_message_button")
                            ) {
                                if (isGenerating) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Send",
                                        tint = if (inputPrompt.isNotBlank() || attachedImageUri != null) Color.White
                                        else Color(0xFF818CF8).copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Android-Style Bottom Navigation (Immersive UI)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF0A0A0A),
                    border = BorderStroke(1.dp, Color(0x14FFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Chat Tab (Active)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { currentScreen = AppScreen.CHAT }
                                .testTag("bottom_nav_chat")
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(26.dp)
                                    .width(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x336366F1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubble,
                                    contentDescription = "Chat",
                                    tint = Color(0xFFA5B4FC),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "CHAT",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA5B4FC),
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        // 2. Veo 3 Video Studio Tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { currentScreen = AppScreen.VIDEO_STUDIO }
                                .testTag("bottom_nav_veo")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Veo 3",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "VEO 3",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF818CF8),
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        // 3. Image Studio Tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { currentScreen = AppScreen.IMAGE_STUDIO }
                                .testTag("bottom_nav_image")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brush,
                                contentDescription = "Image Studio",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "IMAGE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF34D399),
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        // 4. Live Search & Fact-Check Tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { currentScreen = AppScreen.SEARCH_FACT_CHECK }
                                .testTag("bottom_nav_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Search Agent",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SEARCH",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF38BDF8),
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        // 5. Space / Dashboard Tab
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { currentScreen = AppScreen.DASHBOARD }
                                .testTag("bottom_nav_dashboard")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = "Space",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SPACE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8),
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatWelcome(
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing Hero Avatar
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF4338CA), Color(0xFF6366F1))
                    )
                )
                .border(2.dp, Color(0x80818CF8), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Omni Logo",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Omni-GPT",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = Color.White
            )
        )

        Text(
            text = "Human-Like AI • Real-Time Code Execution • Multimodal Vision",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF818CF8),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Quick Suggestion Cards with Immersive Glassmorphism
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickPrompts.forEach { prompt ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPromptSelected(prompt) },
                    color = Color(0x14FFFFFF),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.5.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
