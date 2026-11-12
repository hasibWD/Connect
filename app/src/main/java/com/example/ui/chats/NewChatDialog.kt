package com.example.ui.chats

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ConnectViewModel
import com.example.ui.common.ConnectAvatar
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatDialog(
  viewModel: ConnectViewModel,
  onDismiss: () -> Unit
) {
  val allUsers by viewModel.allUsers.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var searchFilter by remember { mutableStateOf("") }

  val contacts = allUsers.filter {
    it.id != currentUser?.id && !it.isBlocked &&
      (searchFilter.isBlank() || it.name.contains(searchFilter, ignoreCase = true) || it.username.contains(searchFilter, ignoreCase = true))
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("new_chat_dialog")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .padding(bottom = 32.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "New Message",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Search Field "To: ..."
      OutlinedTextField(
        value = searchFilter,
        onValueChange = { searchFilter = it },
        placeholder = { Text("To: Type a name or @username...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ConnectBluePrimary) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("new_chat_search_input"),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          focusedContainerColor = MaterialTheme.colorScheme.surface
        )
      )

      Spacer(modifier = Modifier.height(14.dp))

      // "Create a new group" shortcut item
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onDismiss()
            viewModel.setShowCreateGroupDialog(true)
          }
          .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(ConnectBlueLight),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.GroupAdd, contentDescription = null, tint = ConnectBluePrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
          Text(
            text = "Create a new group",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ConnectBluePrimary
          )
          Text(
            text = "Chat with multiple friends together",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))

      Text(
        text = "Suggested Contacts",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 6.dp)
      )

      // Contact List
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(300.dp)
      ) {
        items(contacts, key = { it.id }) { user ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                viewModel.startDirectChat(user.id)
                onDismiss()
              }
              .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            ConnectAvatar(
              avatarUrl = user.avatarUrl,
              name = user.name,
              size = 46.dp,
              isOnline = user.isOnline,
              showOnlineBadge = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = user.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "@${user.username} • ${if (user.isOnline) "Active now" else user.lastSeenText}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
