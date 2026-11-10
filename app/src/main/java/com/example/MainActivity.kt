package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.ConnectViewModel
import com.example.ui.MainScreen
import com.example.ui.admin.AdminPanelScreen
import com.example.ui.auth.AuthContainer
import com.example.ui.calls.ActiveCallScreen
import com.example.ui.chats.ConversationScreen
import com.example.ui.theme.ConnectTheme

class MainActivity : ComponentActivity() {

  private val viewModel: ConnectViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val isDarkMode by viewModel.isDarkMode.collectAsState()
      val currentUser by viewModel.currentUser.collectAsState()
      val activeCall by viewModel.activeCall.collectAsState()
      val showAdminPanel by viewModel.showAdminPanel.collectAsState()
      val activeChatId by viewModel.activeChatId.collectAsState()

      ConnectTheme(darkTheme = isDarkMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
          when {
            currentUser == null -> {
              AuthContainer(viewModel = viewModel)
            }
            activeCall != null -> {
              ActiveCallScreen(callState = activeCall!!, viewModel = viewModel)
            }
            showAdminPanel -> {
              AdminPanelScreen(viewModel = viewModel)
            }
            activeChatId != null -> {
              ConversationScreen(chatId = activeChatId!!, viewModel = viewModel)
            }
            else -> {
              MainScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}
