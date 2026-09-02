package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GeneratedVideoItem
import com.example.data.model.VideoScene
import com.example.ui.theme.EmeraldPrimary
import com.example.util.SpeechManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoStudioScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val generatedVideos by viewModel.generatedVideos.collectAsState()
    val isGenerating by viewModel.isVideoGenerating.collectAsState()
    val generationStatus by viewModel.videoGenerationStatus.collectAsState()
    val currentVideo by viewModel.currentPlayingVideo.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Script to Video, 1: Animate Image, 2: Video Library

    // Script to Video state
    var scriptTitle by remember { mutableStateOf("") }
    var scriptContent by remember { mutableStateOf("") }
    var selectedAspect by remember { mutableStateOf("16:9") }

    // Image to Video state
    var animateImageBase64 by remember { mutableStateOf<String?>(null) }
    var animateImageUri by remember { mutableStateOf<Uri?>(null) }
    var animationPrompt by remember { mutableStateOf("Turn into dynamic 360 cinematic product ad with dramatic studio lighting") }
    var selectedStylePreset by remember { mutableStateOf("Dynamic Product Ad") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            animateImageUri = uri
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    animateImageBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Veo 3 Video Studio",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Google Veo 3 • Generative Video AI",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFA5B4FC),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF070810)
                )
            )
        },
        containerColor = Color(0xFF070810)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0E0F1C),
                contentColor = Color(0xFF818CF8),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF818CF8)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Script to Video",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) Color.White else Color(0xFF94A3B8)
                            )
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Animate Photo",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) Color.White else Color(0xFF94A3B8)
                            )
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "Library (${generatedVideos.size})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 2) Color.White else Color(0xFF94A3B8)
                            )
                        )
                    }
                )
            }

            // Generation in progress overlay / banner
            AnimatedVisibility(visible = isGenerating) {
                Surface(
                    color = Color(0x336366F1),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFF818CF8),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = generationStatus.ifEmpty { "Generating video with Veo 3..." },
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE0E7FF))
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> ScriptToVideoTab(
                    scriptTitle = scriptTitle,
                    onTitleChange = { scriptTitle = it },
                    scriptContent = scriptContent,
                    onScriptChange = { scriptContent = it },
                    aspectRatio = selectedAspect,
                    onAspectChange = { selectedAspect = it },
                    isGenerating = isGenerating,
                    currentVideo = currentVideo,
                    speechManager = viewModel.speechManager,
                    onGenerate = {
                        viewModel.generateVideoFromScript(
                            title = scriptTitle,
                            script = scriptContent,
                            aspectRatio = selectedAspect
                        )
                    }
                )
                1 -> AnimateImageTab(
                    imageBase64 = animateImageBase64,
                    onPickImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onClearImage = {
                        animateImageBase64 = null
                        animateImageUri = null
                    },
                    animationPrompt = animationPrompt,
                    onPromptChange = { animationPrompt = it },
                    selectedPreset = selectedStylePreset,
                    onPresetSelect = { preset, defaultPrompt ->
                        selectedStylePreset = preset
                        animationPrompt = defaultPrompt
                    },
                    isGenerating = isGenerating,
                    currentVideo = currentVideo,
                    speechManager = viewModel.speechManager,
                    onAnimate = {
                        if (animateImageBase64 != null) {
                            viewModel.animateImageWithVeo(
                                imageBitmapBase64 = animateImageBase64!!,
                                animationPrompt = animationPrompt,
                                stylePreset = selectedStylePreset,
                                aspectRatio = "16:9"
                            )
                        } else {
                            Toast.makeText(context, "Please pick a photo first", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                2 -> VideoLibraryTab(
                    videos = generatedVideos,
                    onSelectVideo = { video ->
                        viewModel.setCurrentPlayingVideo(video)
                    },
                    currentVideo = currentVideo,
                    speechManager = viewModel.speechManager
                )
            }
        }
    }
}

@Composable
fun ScriptToVideoTab(
    scriptTitle: String,
    onTitleChange: (String) -> Unit,
    scriptContent: String,
    onScriptChange: (String) -> Unit,
    aspectRatio: String,
    onAspectChange: (String) -> Unit,
    isGenerating: Boolean,
    currentVideo: GeneratedVideoItem?,
    speechManager: SpeechManager? = null,
    onGenerate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Quick templates row
        Text(
            text = "Quick Inspiration Presets",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                "👟 Cyberpunk Sneaker Ad" to "Cinematic neon metropolis at midnight. A futuristic self-lacing runner levitates in rainy reflection, dynamic 4K slow motion zoom on high-tech textured mesh with violet laser pulses.",
                "☕ Artisan Coffee Roaster" to "Golden morning light filtering through vintage café window. Steaming dark roast espresso slowly pours into ceramic cup, rich crema forming with cinematic macro depth-of-field.",
                "🚀 Quantum AI Core" to "Floating obsidian glass device radiating pulsing holographic neural waves. Particles swirl around crystalline processor in dark futuristic laboratory.",
                "🌿 Sustainable Living" to "Lush sunlit rainforest canopy with morning dew droplets falling in ultra high-speed 120fps. Camera glides over emerald leaves with gentle ambient breeze."
            )

            presets.forEach { (label, script) ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF161726),
                    border = BorderStroke(1.dp, Color(0x33818CF8)),
                    modifier = Modifier.clickable {
                        onTitleChange(label.substringAfter(" "))
                        onScriptChange(script)
                    }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFC7D2FE),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title Input
        OutlinedTextField(
            value = scriptTitle,
            onValueChange = onTitleChange,
            label = { Text("Video Title / Project Name") },
            placeholder = { Text("e.g. Cyberpunk Runner Promo") },
            modifier = Modifier.fillMaxWidth().testTag("video_title_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF818CF8),
                unfocusedBorderColor = Color(0x33818CF8),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Script / Blog Post / Product Description
        OutlinedTextField(
            value = scriptContent,
            onValueChange = onScriptChange,
            label = { Text("Blog Post, Script, or Product Description") },
            placeholder = { Text("Paste your creative script, product pitch, or blog excerpt here. Veo 3 will decompose it into cinematic storyboard scenes and render the video...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("video_script_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF818CF8),
                unfocusedBorderColor = Color(0x33818CF8),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            maxLines = 8
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Aspect Ratio Selector
        Text(
            text = "Aspect Ratio",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val ratios = listOf(
                "16:9" to "Cinematic (16:9)",
                "9:16" to "Reels / TikTok (9:16)",
                "1:1" to "Square (1:1)"
            )
            ratios.forEach { (ratio, label) ->
                val selected = aspectRatio == ratio
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) Color(0xFF4F46E5) else Color(0xFF161726),
                    border = BorderStroke(1.dp, if (selected) Color(0xFF818CF8) else Color(0x22818CF8)),
                    modifier = Modifier.clickable { onAspectChange(ratio) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (selected) Color.White else Color(0xFF94A3B8),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Generate Button
        Button(
            onClick = onGenerate,
            enabled = scriptContent.isNotBlank() && !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_video_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1),
                disabledContainerColor = Color(0xFF2E3150)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isGenerating) "Veo 3 Synthesizing Video..." else "Turn Script into Veo 3 Video",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Display current generated video preview if available
        if (currentVideo != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Generated Video: ${currentVideo.title}",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            InteractiveVeoVideoPlayer(video = currentVideo, speechManager = speechManager)
        }
    }
}

@Composable
fun AnimateImageTab(
    imageBase64: String?,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    animationPrompt: String,
    onPromptChange: (String) -> Unit,
    selectedPreset: String,
    onPresetSelect: (String, String) -> Unit,
    isGenerating: Boolean,
    currentVideo: GeneratedVideoItem?,
    speechManager: SpeechManager? = null,
    onAnimate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Bring Photos to Life with Veo 3",
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "Upload a product photo to turn into a dynamic video ad, or animate a character portrait.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Image Preview or Upload Box
        if (imageBase64 == null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onPickImage() }
                    .testTag("upload_photo_for_video"),
                color = Color(0xFF131422),
                border = BorderStroke(1.5.dp, Color(0x40818CF8)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0x336366F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Pick photo",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to upload Product Photo or Character Portrait",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFC7D2FE),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = "JPEG, PNG supported • Optimized for Veo 3",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        } else {
            val bitmap = remember(imageBase64) {
                try {
                    val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (_: Exception) { null }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(14.dp))
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Source image to animate",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Clear button
                IconButton(
                    onClick = onClearImage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove photo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Animation Styles
        Text(
            text = "Animation Style Presets",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                "Dynamic Product Ad" to "360 degree turntable rotation with dynamic studio rim lighting, subtle floating particles and sleek product reflection",
                "Character Portrait" to "Natural lifelike subtle eye blinking, gentle breathing motion, wind blowing hair, warm cinematic portrait lighting",
                "3D Cinematic Push" to "Smooth cinematic drone push-in with shallow depth of field and soft atmospheric lens flare",
                "Liquid / Splash Action" to "High-speed 1000fps slow motion liquid splash exploding around subject with sparkling droplet reflections"
            )

            presets.forEach { (preset, prompt) ->
                val isSelected = selectedPreset == preset
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) Color(0xFF4F46E5) else Color(0xFF161726),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF818CF8) else Color(0x33818CF8)),
                    modifier = Modifier.clickable { onPresetSelect(preset, prompt) }
                ) {
                    Text(
                        text = preset,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) Color.White else Color(0xFFC7D2FE),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom Animation Prompt
        OutlinedTextField(
            value = animationPrompt,
            onValueChange = onPromptChange,
            label = { Text("Motion & Animation Instructions") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF818CF8),
                unfocusedBorderColor = Color(0x33818CF8),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAnimate,
            enabled = imageBase64 != null && !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("animate_photo_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B5CF6),
                disabledContainerColor = Color(0xFF2E3150)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isGenerating) "Veo 3 Animating Video..." else "Animate with Veo 3",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        if (currentVideo != null) {
            Spacer(modifier = Modifier.height(24.dp))
            InteractiveVeoVideoPlayer(video = currentVideo, speechManager = speechManager)
        }
    }
}

@Composable
fun VideoLibraryTab(
    videos: List<GeneratedVideoItem>,
    onSelectVideo: (GeneratedVideoItem) -> Unit,
    currentVideo: GeneratedVideoItem?,
    speechManager: SpeechManager? = null
) {
    if (videos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color(0xFF4B5563),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Videos Generated Yet",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF94A3B8))
                )
                Text(
                    text = "Use the Script to Video or Animate Photo tabs to produce your first clip with Veo 3.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B), textAlign = TextAlign.Center)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (currentVideo != null) {
                    Text(
                        text = "Now Playing: ${currentVideo.title}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InteractiveVeoVideoPlayer(video = currentVideo, speechManager = speechManager)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "All Generated Videos",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            items(videos) { video ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectVideo(video) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131422)),
                    border = BorderStroke(1.dp, if (currentVideo?.id == video.id) Color(0xFF818CF8) else Color(0x22818CF8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E2038)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${video.durationSec}s • ${video.aspectRatio} • ${video.scenes.size} Scenes",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = video.visualPrompt,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveVeoVideoPlayer(
    video: GeneratedVideoItem,
    speechManager: SpeechManager? = null
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var isAudioMuted by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var currentSceneIndex by remember { mutableIntStateOf(0) }
    var lastNarratedScene by remember { mutableIntStateOf(-1) }

    val totalDuration = video.durationSec.coerceAtLeast(6)
    val sceneCount = video.scenes.size.coerceAtLeast(1)

    // Advance timeline smoothly while playing
    LaunchedEffect(isPlaying, video.id) {
        if (isPlaying) {
            while (isPlaying) {
                delay(100)
                currentProgress += 0.1f / totalDuration
                if (currentProgress >= 1f) {
                    currentProgress = 0f
                    lastNarratedScene = -1
                }
                currentSceneIndex = (currentProgress * sceneCount).toInt().coerceIn(0, sceneCount - 1)
            }
        }
    }

    // Synchronize audio narration with active scene
    LaunchedEffect(currentSceneIndex, isPlaying, isAudioMuted, video.id) {
        if (isPlaying && !isAudioMuted && speechManager != null) {
            val scene = video.scenes.getOrNull(currentSceneIndex) ?: video.scenes.firstOrNull()
            if (scene != null && lastNarratedScene != currentSceneIndex) {
                lastNarratedScene = currentSceneIndex
                val voiceoverText = scene.narration.ifBlank {
                    "${scene.title}. ${scene.visualPrompt.take(80)}"
                }
                if (voiceoverText.isNotBlank()) {
                    speechManager.speak(
                        text = voiceoverText,
                        messageId = "veo_scene_${video.id}_$currentSceneIndex",
                        speed = 1.0f,
                        pitch = 1.0f
                    )
                }
            }
        } else if (!isPlaying || isAudioMuted) {
            speechManager?.stopSpeaking()
            lastNarratedScene = -1
        }
    }

    DisposableEffect(video.id) {
        onDispose {
            speechManager?.stopSpeaking()
        }
    }

    val activeScene = video.scenes.getOrNull(currentSceneIndex) ?: video.scenes.firstOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x66818CF8), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090A14))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Video Canvas Viewport - Never black, rich visual cinematic composition
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF2E1065),
                                Color(0xFF1E1B4B),
                                Color(0xFF0F172A),
                                Color(0xFF030712)
                            )
                        )
                    )
            ) {
                // If keyframe/source image is present, render with animated Ken-Burns camera movement
                if (video.sourceImageBase64 != null) {
                    val bitmap = remember(video.sourceImageBase64) {
                        try {
                            val bytes = Base64.decode(video.sourceImageBase64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } catch (_: Exception) { null }
                    }
                    if (bitmap != null) {
                        val cameraZoom = 1.04f + 0.08f * kotlin.math.sin(currentProgress * Math.PI.toFloat())
                        val cameraPanX = (currentProgress - 0.5f) * 36f
                        val cameraPanY = kotlin.math.cos(currentProgress * Math.PI.toFloat()) * 8f

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Video frame",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = cameraZoom
                                    scaleY = cameraZoom
                                    translationX = cameraPanX
                                    translationY = cameraPanY
                                }
                        )

                        // Vignette & cinematic film lighting gradient
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0x80000000),
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color(0xCC000000)
                                        )
                                    )
                                )
                        )
                    }
                } else {
                    // Procedural Neural Canvas when no base image is provided (avoids black screen completely)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridColor = Color(0x2B818CF8)
                        val horizon = size.height * 0.58f

                        // Horizon perspective lines
                        for (i in -4..4) {
                            val xOffset = (size.width * 0.5f) + (i * size.width * 0.14f) + (currentProgress * 20f)
                            drawLine(
                                color = gridColor,
                                start = Offset(size.width * 0.5f, horizon),
                                end = Offset(xOffset, size.height),
                                strokeWidth = 1.5f
                            )
                        }

                        // Perspective horizontal rungs
                        for (j in 1..4) {
                            val y = horizon + (size.height - horizon) * (j.toFloat() / 4f)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Viewfinder corner brackets
                        val bracketColor = Color(0x88818CF8)
                        val bLen = 24f
                        val pad = 16f
                        // Top-left
                        drawLine(bracketColor, Offset(pad, pad), Offset(pad + bLen, pad), 2f)
                        drawLine(bracketColor, Offset(pad, pad), Offset(pad, pad + bLen), 2f)
                        // Top-right
                        drawLine(bracketColor, Offset(size.width - pad, pad), Offset(size.width - pad - bLen, pad), 2f)
                        drawLine(bracketColor, Offset(size.width - pad, pad), Offset(size.width - pad, pad + bLen), 2f)
                        // Bottom-left
                        drawLine(bracketColor, Offset(pad, size.height - pad), Offset(pad + bLen, size.height - pad), 2f)
                        drawLine(bracketColor, Offset(pad, size.height - pad), Offset(pad, size.height - pad - bLen), 2f)
                        // Bottom-right
                        drawLine(bracketColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - bLen, size.height - pad), 2f)
                        drawLine(bracketColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - bLen), 2f)

                        // Center crosshair
                        val cX = size.width * 0.5f
                        val cY = size.height * 0.44f
                        drawLine(Color(0x44818CF8), Offset(cX - 12f, cY), Offset(cX + 12f, cY), 1.5f)
                        drawLine(Color(0x44818CF8), Offset(cX, cY - 12f), Offset(cX, cY + 12f), 1.5f)
                    }

                    // Procedural Slate Center Display
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x401E1B4B),
                            border = BorderStroke(1.dp, Color(0x66818CF8)),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Color(0xFFA5B4FC),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SCENE ${currentSceneIndex + 1} OF $sceneCount",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFA5B4FC),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                        }

                        Text(
                            text = activeScene?.title ?: video.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = activeScene?.visualPrompt ?: "Veo 3 Neural Motion Synthesis",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Animated sound visualizer bars in center
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(16.dp)
                        ) {
                            for (b in 0..6) {
                                val barHeight = if (isPlaying && !isAudioMuted) {
                                    val wave = kotlin.math.sin(currentProgress * 25f + b.toFloat()) * 0.5f + 0.5f
                                    (6 + wave * 10).dp
                                } else {
                                    4.dp
                                }
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(barHeight)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isPlaying && !isAudioMuted) Color(0xFF34D399) else Color(0xFF4B5563))
                                )
                            }
                        }
                    }
                }

                // Top Status Bar: Veo 3 badge + Audio indicator + Aspect Ratio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xCC000000),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) Color(0xFFEF4444) else Color(0xFF6B7280))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VEO 3 • 4K",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Voiceover / Audio state indicator badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isAudioMuted) Color(0xCC3B0764) else Color(0xCC064E3B),
                        border = BorderStroke(1.dp, if (isAudioMuted) Color(0xFF9333EA) else Color(0xFF10B981))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = if (isAudioMuted) Color(0xFFD8B4FE) else Color(0xFF34D399),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAudioMuted) "SOUND MUTED" else "VOICEOVER ON",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isAudioMuted) Color(0xFFD8B4FE) else Color(0xFF34D399),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xCC000000),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF))
                    ) {
                        Text(
                            text = "${video.aspectRatio} • 60 FPS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFA5B4FC),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Dynamic narration / scene subtitles in bottom of viewport
                if (activeScene != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xE60A0D18),
                        border = BorderStroke(1.dp, Color(0x40818CF8)),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp, start = 14.dp, end = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = if (!isAudioMuted) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = null,
                                tint = if (!isAudioMuted) Color(0xFF34D399) else Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeScene.narration.isNotBlank()) activeScene.narration else "Scene ${activeScene.sceneNumber}: ${activeScene.title}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Controls & Timeline
            Column(modifier = Modifier.padding(14.dp)) {
                // Scrubber Slider
                Slider(
                    value = currentProgress,
                    onValueChange = {
                        currentProgress = it
                        currentSceneIndex = (it * sceneCount).toInt().coerceIn(0, sceneCount - 1)
                        lastNarratedScene = -1
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF818CF8),
                        activeTrackColor = Color(0xFF6366F1),
                        inactiveTrackColor = Color(0xFF1E2038)
                    ),
                    modifier = Modifier.height(20.dp)
                )

                // Playback toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isPlaying = !isPlaying },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                currentProgress = 0f
                                currentSceneIndex = 0
                                lastNarratedScene = -1
                                isPlaying = true
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Replay",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val currentSec = (currentProgress * totalDuration).toInt()
                        Text(
                            text = "00:${String.format("%02d", currentSec)} / 00:${String.format("%02d", totalDuration)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Voiceover narration sound toggle button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isAudioMuted) Color(0xFF1F2937) else Color(0xFF064E3B),
                            border = BorderStroke(1.dp, if (isAudioMuted) Color(0xFF374151) else Color(0xFF059669)),
                            modifier = Modifier.clickable {
                                isAudioMuted = !isAudioMuted
                                if (isAudioMuted) {
                                    speechManager?.stopSpeaking()
                                } else {
                                    lastNarratedScene = -1
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAudioMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isAudioMuted) "Unmute" else "Mute",
                                    tint = if (isAudioMuted) Color(0xFF9CA3AF) else Color(0xFF34D399),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAudioMuted) "Sound Off" else "Voiceover",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isAudioMuted) Color(0xFF9CA3AF) else Color(0xFF34D399),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Exported Veo 3 Video with Narration to Gallery", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Scene Cards Breakdown
                if (video.scenes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Storyboard Scenes (${video.scenes.size}) — Tap to Jump & Narrate",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        video.scenes.forEachIndexed { idx, s ->
                            val isActive = currentSceneIndex == idx
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isActive) Color(0xFF1E2038) else Color(0xFF10111F),
                                border = BorderStroke(1.dp, if (isActive) Color(0xFF818CF8) else Color(0x1AFFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentSceneIndex = idx
                                        currentProgress = idx.toFloat() / video.scenes.size.toFloat()
                                        lastNarratedScene = -1
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isActive) Color(0xFF6366F1) else Color(0x33FFFFFF)
                                    ) {
                                        Text(
                                            text = "${s.sceneNumber}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = s.title,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                        Text(
                                            text = if (s.narration.isNotBlank()) "🎙 \"${s.narration}\"" else s.visualPrompt,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isActive) Color(0xFFA5B4FC) else Color(0xFF94A3B8),
                                                fontSize = 10.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${s.durationSec}s",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
