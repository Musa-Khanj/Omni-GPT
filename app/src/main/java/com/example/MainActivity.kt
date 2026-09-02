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

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
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

