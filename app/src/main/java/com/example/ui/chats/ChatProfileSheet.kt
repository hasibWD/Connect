package com.example.ui.chats

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallType
import com.example.model.Chat
import com.example.model.ChatType
import com.example.ui.ConnectViewModel
import com.example.ui.common.ConnectAvatar
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary
import com.example.ui.theme.ConnectGreenOnline
import com.example.ui.theme.ConnectRedError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatProfileSheet(
  chat: Chat,
  viewModel: ConnectViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var showDeleteDialog by remember { mutableStateOf(false) }
  var showBlockDialog by remember { mutableStateOf(false) }

  val targetContact = chat.participants.firstOrNull { it.id != viewModel.currentUser.value?.id }
  val isOnline = chat.participants.any { it.isOnline }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("chat_profile_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 36.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Messenger-Style Avatar & Header
      ConnectAvatar(
        avatarUrl = chat.avatarUrl,
        name = chat.name,
        size = 96.dp,
        isOnline = isOnline,
        showOnlineBadge = true
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = chat.name,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
      )

      val subtitle = if (chat.type == ChatType.GROUP) {
        "${chat.participants.size} members • Group"
      } else {
        "@${targetContact?.username ?: "connect_user"} • ${if (isOnline) "Active now" else targetContact?.lastSeenText ?: "Offline"}"
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = if (isOnline && chat.type == ChatType.DIRECT) ConnectGreenOnline else MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(20.dp))

      // Messenger Action Quick Buttons Row (Audio, Video, Profile, Mute)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        MessengerActionButton(
          icon = Icons.Default.Call,
          label = "Audio",
          onClick = {
            onDismiss()
            viewModel.startCall(
              contactId = targetContact?.id ?: chat.id,
              contactName = chat.name,
              contactAvatar = chat.avatarUrl,
              type = CallType.VOICE
            )
          }
        )

        MessengerActionButton(
          icon = Icons.Default.Videocam,
          label = "Video",
          onClick = {
            onDismiss()
            viewModel.startCall(
              contactId = targetContact?.id ?: chat.id,
              contactName = chat.name,
              contactAvatar = chat.avatarUrl,
              type = CallType.VIDEO
            )
          }
        )

        MessengerActionButton(
          icon = Icons.Default.Person,
          label = "Profile",
          onClick = {
            Toast.makeText(context, "${chat.name}'s CONNECT Profile", Toast.LENGTH_SHORT).show()
          }
        )

        MessengerActionButton(
          icon = Icons.Default.NotificationsOff,
          label = "Mute",
          onClick = {
            Toast.makeText(context, "Notifications muted for ${chat.name}", Toast.LENGTH_SHORT).show()
          }
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Customization Section
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
          MessengerSettingItem(
            icon = Icons.Default.ColorLens,
            title = "Theme",
            subtitle = "CONNECT Electric Blue",
            onClick = { Toast.makeText(context, "Theme set to Electric Blue", Toast.LENGTH_SHORT).show() }
          )
          HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          MessengerSettingItem(
            icon = Icons.Default.EmojiEmotions,
            title = "Quick Emoji",
            subtitle = "👍 (Thumbs Up)",
            onClick = { Toast.makeText(context, "Quick emoji updated", Toast.LENGTH_SHORT).show() }
          )
          HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          MessengerSettingItem(
            icon = Icons.Default.Image,
            title = "View Shared Media & Files",
            subtitle = "Photos, audio notes and documents",
            onClick = { Toast.makeText(context, "Showing shared photos and files", Toast.LENGTH_SHORT).show() }
          )
          HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          MessengerSettingItem(
            icon = Icons.Default.Search,
            title = "Search in Conversation",
            subtitle = "Find messages and links",
            onClick = { onDismiss() }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Privacy & Destructive Actions Section
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
          MessengerSettingItem(
            icon = Icons.Default.Flag,
            title = "Report Conversation",
            subtitle = "Report spam, abuse, or inappropriate content",
            onClick = {
              onDismiss()
              viewModel.reportUser(chat.id, chat.name, "Inappropriate Content", "Reported from chat profile")
              Toast.makeText(context, "Report submitted to admin team", Toast.LENGTH_SHORT).show()
            }
          )

          HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          MessengerSettingItem(
            icon = Icons.Default.Block,
            title = "Block ${chat.name}",
            subtitle = "They won't be able to message or call you",
            titleColor = ConnectRedError,
            onClick = { showBlockDialog = true }
          )

          HorizontalDivider(modifier = Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          MessengerSettingItem(
            icon = Icons.Default.Delete,
            title = "Delete Chat",
            subtitle = "Permanently remove conversation and messages",
            titleColor = ConnectRedError,
            onClick = { showDeleteDialog = true }
          )
        }
      }
    }
  }

  // Delete Chat Confirmation Dialog
  if (showDeleteDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteDialog = false },
      title = { Text("Delete Entire Chat?", fontWeight = FontWeight.Bold) },
      text = {
        Text("Once you delete your copy of this conversation, it cannot be undone. All messages and shared media in this chat will be removed.")
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteDialog = false
            onDismiss()
            viewModel.deleteChat(chat.id)
            Toast.makeText(context, "Conversation deleted", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ConnectRedError)
        ) {
          Text("Delete Chat")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Block User Confirmation Dialog
  if (showBlockDialog) {
    AlertDialog(
      onDismissRequest = { showBlockDialog = false },
      title = { Text("Block ${chat.name}?", fontWeight = FontWeight.Bold) },
      text = {
        Text("${chat.name} will not be able to message or call you on CONNECT. You can unblock them anytime from your Profile settings.")
      },
      confirmButton = {
        Button(
          onClick = {
            showBlockDialog = false
            onDismiss()
            if (targetContact != null) {
              viewModel.blockUser(targetContact.id)
            }
            viewModel.deleteChat(chat.id)
            Toast.makeText(context, "${chat.name} has been blocked", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ConnectRedError)
        ) {
          Text("Block & Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { showBlockDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun MessengerActionButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable { onClick() }
  ) {
    Box(
      modifier = Modifier
        .size(46.dp)
        .clip(CircleShape)
        .background(ConnectBlueLight),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = label, tint = ConnectBluePrimary, modifier = Modifier.size(22.dp))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun MessengerSettingItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  titleColor: Color = Color.Unspecified,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = if (titleColor != Color.Unspecified) titleColor else ConnectBluePrimary,
      modifier = Modifier.size(22.dp)
    )
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
