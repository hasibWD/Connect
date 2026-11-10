package com.example.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CallDirection
import com.example.model.CallItem
import com.example.model.CallType
import com.example.ui.ConnectViewModel
import com.example.ui.common.ConnectAvatar
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary
import com.example.ui.theme.ConnectGreenOnline
import com.example.ui.theme.ConnectRedError

@Composable
fun CallsScreen(
  viewModel: ConnectViewModel,
  modifier: Modifier = Modifier
) {
  val callHistory by viewModel.callHistory.collectAsState()
  val allUsers by viewModel.allUsers.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()

  var showNewCallDialog by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Header
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Calls",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Voice and high-definition video calls",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (callHistory.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Call,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No recent calls",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Tap the phone icon below to start a call",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(callHistory, key = { it.id }) { call ->
            CallHistoryItem(
              call = call,
              onRedial = {
                viewModel.startCall(
                  contactId = call.userId,
                  contactName = call.userName,
                  contactAvatar = call.userAvatar,
                  type = call.type
                )
              }
            )
          }
        }
      }
    }

    // Floating Button to start Call
    FloatingActionButton(
      onClick = { showNewCallDialog = true },
      containerColor = ConnectBluePrimary,
      contentColor = Color.White,
      shape = CircleShape,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(bottom = 80.dp, end = 20.dp)
        .testTag("start_call_fab")
    ) {
      Icon(Icons.Default.Call, contentDescription = "New Call")
    }
  }

  // New Call Contact Selector Dialog
  if (showNewCallDialog) {
    val contacts = allUsers.filter { it.id != currentUser?.id && !it.isBlocked }

    AlertDialog(
      onDismissRequest = { showNewCallDialog = false },
      title = { Text("Start a Call", fontWeight = FontWeight.Bold) },
      text = {
        LazyColumn(modifier = Modifier.fillMaxWidth().height(240.dp)) {
          items(contacts) { user ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              ConnectAvatar(avatarUrl = user.avatarUrl, name = user.name, size = 42.dp)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("@${user.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              IconButton(onClick = {
                showNewCallDialog = false
                viewModel.startCall(user.id, user.name, user.avatarUrl, CallType.VOICE)
              }) {
                Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = ConnectBluePrimary)
              }
              IconButton(onClick = {
                showNewCallDialog = false
                viewModel.startCall(user.id, user.name, user.avatarUrl, CallType.VIDEO)
              }) {
                Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = ConnectBluePrimary)
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showNewCallDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun CallHistoryItem(
  call: CallItem,
  onRedial: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("call_item_${call.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      ConnectAvatar(
        avatarUrl = call.userAvatar,
        name = call.userName,
        size = 46.dp
      )

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = call.userName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = if (call.direction == CallDirection.MISSED) ConnectRedError else MaterialTheme.colorScheme.onSurface
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          val (icon, tint) = when (call.direction) {
            CallDirection.INCOMING -> Pair(Icons.AutoMirrored.Filled.CallReceived, ConnectGreenOnline)
            CallDirection.OUTGOING -> Pair(Icons.AutoMirrored.Filled.CallMade, ConnectBluePrimary)
            CallDirection.MISSED -> Pair(Icons.AutoMirrored.Filled.CallMissed, ConnectRedError)
          }

          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${call.timeString} • ${call.durationText}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      IconButton(
        onClick = onRedial,
        modifier = Modifier
          .size(40.dp)
          .background(ConnectBlueLight, CircleShape)
      ) {
        Icon(
          imageVector = if (call.type == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
          contentDescription = "Redial",
          tint = ConnectBluePrimary,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}
