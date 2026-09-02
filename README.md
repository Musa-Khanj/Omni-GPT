# ChatGPT AI Studio

An advanced multimodal Android application built with **Jetpack Compose** and the **Gemini API**. It integrates next-generation creative AI tooling including **Veo 3 Video Generation**, **Flash Image Creation & Editing**, and **Real-Time Google Search Grounding & Fact-Checking**.

---

## 🌟 Key Features

### 1. 🎬 Veo 3 Video Generation Studio
- **Script & Blog to Video**: Transform long-form blog posts, video scripts, and product concepts into sequenced, multi-scene video storyboards complete with camera motions, visual framing prompts, voiceovers, and duration timing.
- **Bring Photos to Life**: Animate still product photos and character portraits into cinematic ads, 360° product turntable spins, and atmospheric motion clips.
- **Interactive Storyboard Player**: Preview scene sequences, inspect visual camera directions, and control playback pacing.

### 2. 🎨 Fast Image Creation & Editing (Flash Image)
- **Rapid High-Volume Generation**: Powered by `gemini-2.5-flash-image` with configurable aspect ratios (`1:1` Square, `16:9` Landscape, `9:16` Portrait).
- **Curated Style Presets**: Instant styling for photorealistic studio renders, cyberpunk neon aesthetics, 3D Octane clay renders, clean minimalist vectors, and textured oil paintings.
- **Natural Language Image Restyling**: Select any image to add accessories, modify backdrops, or alter lighting using conversational instructions.
- **Veo 3 Animation Handoff**: Transition any created or edited still image directly into a Veo 3 video workflow with a single tap.

### 3. 🌐 Real-Time Google Search & Fact-Checking Agent
- **Live Search Grounding**: Connect conversation directly to Google Search for up-to-the-minute answers on breaking news, live events, and scientific breakthroughs.
- **Fact-Check Claim Analyzer**: Verify rumors, quotes, and viral headlines with confidence verdicts (*Verified True*, *Misleading/False*, *Developing/Unconfirmed*), key evidence breakdowns, and clickable Google Search source links.
- **Current Events Hub**: Curated entry points to discuss the latest developments in space exploration, artificial intelligence, and global milestones.

### 4. 💬 Multimodal Conversational Interface
- **Model Switching**: Toggle between `gemini-3.5-flash` for blazing response speeds and `gemini-3.1-pro-preview` for deep reasoning.
- **Text-to-Speech & Voice Recognition**: Hands-free conversation with adjustable pitch and playback speed.
- **Local Persistence**: Built on Android Room Database for offline message caching and session history management.
- **Markdown Export**: One-tap export of entire conversation threads formatted in clean Markdown.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern + Clean Architecture
- **Language**: Kotlin & Kotlin Coroutines / Flow
- **Persistence**: Android Jetpack [Room Database](https://developer.android.com/training/data-storage/room) with KSP
- **Networking**: REST API communication with Google Gemini endpoints (Search Grounding, Multimodal Input, Image Generation)
- **Media**: Android Photo Picker API (`ActivityResultContracts.PickVisualMedia`)
- **Testing**: Robolectric, Roborazzi screenshot testing, and JUnit 4

---

## 📱 Navigation Structure

| Tab / Screen | Description |
| :--- | :--- |
| **Chat** | Primary multimodal assistant screen with live search toggle, voice controls, and quick action pills. |
| **Veo 3** | Video storyboard generation from text/blog posts and image-to-video animation studio. |
| **Image** | Gemini Flash Image studio with aspect ratio selection, style presets, editing, and gallery. |
| **Search** | Real-time fact-checking agent and current events discussion explorer grounded in Google Search. |
| **Space** | Model settings, TTS configuration, usage statistics, and chat history management. |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17+
- Android SDK 35 (compileSdk 35, minSdk 26)

### Running the Project
1. Clone or export the repository.
2. Open the project in Android Studio.
3. Provide your Gemini API key in the environment configuration or AI Studio Secrets panel.
4. Build and run on an Android device or emulator:
   ```bash
   gradle :app:assembleDebug
   ```

### Running Tests
Execute JVM unit tests and Robolectric test suites:
```bash
gradle :app:testDebugUnitTest
```
