package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Pre-initialize cache directories to prevent Chromium WebView simple_file_enumerator and simple_index_file warnings
    try {
      val defaultHttpCache = File(cacheDir, "WebView/Default/HTTP Cache")
      defaultHttpCache.mkdirs()
      val indexDir = File(defaultHttpCache, "index-dir")
      indexDir.mkdirs()
      val theRealIndex = File(indexDir, "the-real-index")
      if (!theRealIndex.exists()) {
        theRealIndex.createNewFile()
      }
      val webViewJsCache = File(defaultHttpCache, "Code Cache/js")
      webViewJsCache.mkdirs()
      val webViewWasmCache = File(defaultHttpCache, "Code Cache/wasm")
      webViewWasmCache.mkdirs()
    } catch (_: Exception) {}

    setContent {
      val chatViewModel: ChatViewModel = viewModel()
      val preferences by chatViewModel.userPreferences.collectAsState()

      MyApplicationTheme(darkTheme = preferences.isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
          ChatScreen(viewModel = chatViewModel)
        }
      }
    }
  }
}

