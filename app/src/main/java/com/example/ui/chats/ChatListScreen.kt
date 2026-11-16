package com.example.ui.chats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Chat
import com.example.model.ChatType
import com.example.model.MessageStatus
import com.example.model.Story
import com.example.model.User
import com.example.ui.ConnectViewModel
import com.example.ui.MainTab
import com.example.ui.common.ConnectAvatar
import com.example.ui.common.InsForgeStatusBadge
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary
import com.example.ui.theme.ConnectCyanAccent
import com.example.ui.theme.ConnectRedError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
  viewModel: ConnectViewModel,
  modifier: Modifier = Modifier
) {
  val chats by viewModel.filteredChats.collectAsState()
  val stories by viewModel.stories.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val insForgeConfig by viewModel.insForgeConfig.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()

  var isSearching by remember { mutableStateOf(false) }
  var showFabMenu by remember { mutableStateOf(false) }
  var selectedChatForProfile by remember { mutableStateOf<Chat?>(null) }
  var selectedChatForActions by remember { mutableStateOf<Chat?>(null) }
  var chatToDelete by remember { mutableStateOf<Chat?>(null) }
  var userToBlock by remember { mutableStateOf<User?>(null) }

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Header Bar
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(ConnectBluePrimary)
                  .padding(4.dp),
                contentAlignment = Alignment.Center
              ) {
                Image(
                  painter = painterResource(id = R.drawable.ic_connect_logo),
                  contentDescription = "Logo",
                  modifier = Modifier.size(28.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "CONNECT",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.ExtraBold,
                  color = ConnectBluePrimary
                )
                Text(
                  text = "Connect people, everywhere.",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { viewModel.setShowNewChatDialog(true) },
                modifier = Modifier.testTag("top_bar_new_chat_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "New Conversation",
                  tint = ConnectBluePrimary
                )
              }

              IconButton(onClick = {
                isSearching = !isSearching
                if (!isSearching) viewModel.setSearchQuery("")
              }) {
                Icon(
                  imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                  contentDescription = "Search",
                  tint = MaterialTheme.colorScheme.onSurface
                )
              }

              InsForgeStatusBadge(
                config = insForgeConfig,
                onClick = { viewModel.setShowInsForgeDialog(true) }
              )
            }
          }

          // Expandable Search Field
          AnimatedVisibility(visible = isSearching) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search conversations and messages...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ConnectBluePrimary) },
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("chat_search_input"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  focusedContainerColor = MaterialTheme.colorScheme.surface
                )
              )
            }
          }
        }
      }

      // Stories & Online Friends Reel
      StoriesReel(
        stories = stories,
        onAddStory = { viewModel.setShowCreateStoryDialog(true) },
        onOpenStory = { viewModel.openStory(it) }
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

      // Conversation List
      if (chats.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Chat,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "No conversations match \"$searchQuery\"" else "No conversations yet",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "Tap + to start chatting with friends",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(bottom = 88.dp)
        ) {
          items(chats, key = { it.id }) { chat ->
            ChatItem(
              chat = chat,
              currentUserId = currentUser?.id ?: "user_me",
              onClick = { viewModel.openChat(chat.id) },
              onLongClick = { selectedChatForActions = chat },
              onAvatarClick = { selectedChatForProfile = chat }
            )
            HorizontalDivider(
              modifier = Modifier.padding(start = 76.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
          }
        }
      }
    }

    // Floating Action Button for New Chat / Group
    Column(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(bottom = 80.dp, end = 20.dp),
      horizontalAlignment = Alignment.End
    ) {
      AnimatedVisibility(visible = showFabMenu) {
        Column(
          horizontalAlignment = Alignment.End,
          modifier = Modifier.padding(bottom = 12.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier
              .clickable {
                showFabMenu = false
                viewModel.setShowCreateGroupDialog(true)
              }
              .testTag("create_group_option")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
              Text("New Group", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.width(8.dp))
              Icon(Icons.Default.GroupAdd, contentDescription = null, tint = ConnectBluePrimary)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
            modifier = Modifier
              .clickable {
                showFabMenu = false
                viewModel.setShowNewChatDialog(true)
              }
              .testTag("new_chat_option")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
              Text("New Chat", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
              Spacer(modifier = Modifier.width(8.dp))
              Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = ConnectBluePrimary)
            }
          }
        }
      }

      FloatingActionButton(
        onClick = { showFabMenu = !showFabMenu },
        containerColor = ConnectBluePrimary,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier.testTag("new_chat_fab")
      ) {
        Icon(
          imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
          contentDescription = "New Conversation"
        )
      }
    }
  }

  // Messenger Chat Profile Bottom Sheet
  selectedChatForProfile?.let { chat ->
    ChatProfileSheet(
      chat = chat,
      viewModel = viewModel,
      onDismiss = { selectedChatForProfile = null }
    )
  }

  // Messenger Long-Press / More Actions Sheet
  selectedChatForActions?.let { chat ->
    val otherParticipant = chat.participants.firstOrNull { it.id != currentUser?.id }
    ModalBottomSheet(
      onDismissRequest = { selectedChatForActions = null },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
          .padding(bottom = 28.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
          ConnectAvatar(
            avatarUrl = chat.avatarUrl,
            name = chat.name,
            size = 48.dp,
            isOnline = chat.participants.any { it.isOnline },
            showOnlineBadge = chat.type == ChatType.DIRECT
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = chat.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = if (chat.type == ChatType.GROUP) "${chat.participants.size} members" else "@${otherParticipant?.username ?: "connect_user"}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              selectedChatForActions = null
              selectedChatForProfile = chat
            }
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag("action_view_chat_profile"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Person, contentDescription = null, tint = ConnectBluePrimary)
          Spacer(modifier = Modifier.width(14.dp))
          Text("View Profile & Chat Info", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }

        if (chat.unreadCount > 0) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                viewModel.markChatAsRead(chat.id)
                selectedChatForActions = null
              }
              .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.DoneAll, contentDescription = null, tint = ConnectBluePrimary)
            Spacer(modifier = Modifier.width(14.dp))
            Text("Mark as Read", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
          }
        }

        if (chat.type == ChatType.DIRECT && otherParticipant != null) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                selectedChatForActions = null
                userToBlock = otherParticipant
              }
              .padding(horizontal = 12.dp, vertical = 14.dp)
              .testTag("action_block_user"),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Block, contentDescription = null, tint = ConnectRedError)
            Spacer(modifier = Modifier.width(14.dp))
            Text("Block ${otherParticipant.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = ConnectRedError)
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              selectedChatForActions = null
              chatToDelete = chat
            }
            .padding(horizontal = 12.dp, vertical = 14.dp)
            .testTag("action_delete_chat"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, tint = ConnectRedError)
          Spacer(modifier = Modifier.width(14.dp))
          Text("Delete Chat", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = ConnectRedError)
        }
      }
    }
  }

  // Delete Chat Confirmation Dialog
  chatToDelete?.let { chat ->
    AlertDialog(
      onDismissRequest = { chatToDelete = null },
      title = { Text("Delete Chat?", fontWeight = FontWeight.Bold) },
      text = {
        Text("Are you sure you want to delete your conversation with ${chat.name}? This cannot be undone.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteChat(chat.id)
            chatToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ConnectRedError)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { chatToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Block User Confirmation Dialog
  userToBlock?.let { target ->
    AlertDialog(
      onDismissRequest = { userToBlock = null },
      title = { Text("Block ${target.name}?", fontWeight = FontWeight.Bold) },
      text = {
        Text("${target.name} won't be able to message or call you on CONNECT. This chat will be removed from your inbox.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.blockUser(target.id)
            userToBlock = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ConnectRedError)
        ) {
          Text("Block")
        }
      },
      dismissButton = {
        TextButton(onClick = { userToBlock = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun StoriesReel(
  stories: List<Story>,
  onAddStory: () -> Unit,
  onOpenStory: (Story) -> Unit
) {
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp),
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // My Story Add Item
    item {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .clickable { onAddStory() }
          .testTag("add_story_reel_item")
      ) {
        Box(
          modifier = Modifier.size(56.dp),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(ConnectBlueLight),
            contentAlignment = Alignment.Center
          ) {
            Text("Me", color = ConnectBluePrimary, fontWeight = FontWeight.Bold)
          }
          Box(
            modifier = Modifier
              .size(20.dp)
              .align(Alignment.BottomEnd)
              .clip(CircleShape)
              .background(ConnectBluePrimary)
              .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Add, contentDescription = "Add Story", tint = Color.White, modifier = Modifier.size(14.dp))
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "My Status",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }

    // Friends' Stories
    items(stories, key = { it.id }) { story ->
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .clickable { onOpenStory(story) }
          .testTag("story_reel_item_${story.id}")
      ) {
        Box(
          modifier = Modifier
            .size(58.dp)
            .border(
              width = 2.5.dp,
              brush = if (story.isViewed) Brush.linearGradient(listOf(Color.Gray, Color.LightGray))
              else Brush.linearGradient(listOf(ConnectBluePrimary, ConnectCyanAccent)),
              shape = CircleShape
            )
            .padding(3.dp),
          contentAlignment = Alignment.Center
        ) {
          ConnectAvatar(
            avatarUrl = story.userAvatar,
            name = story.userName,
            size = 48.dp
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = story.userName.split(" ").firstOrNull() ?: story.userName,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItem(
  chat: Chat,
  currentUserId: String = "user_me",
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  onAvatarClick: () -> Unit
) {
  val isOnline = chat.participants.any { it.isOnline }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag("chat_item_${chat.id}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .clip(CircleShape)
        .clickable { onAvatarClick() }
        .testTag("chat_avatar_profile_${chat.id}")
    ) {
      ConnectAvatar(
        avatarUrl = chat.avatarUrl,
        name = chat.name,
        size = 52.dp,
        isOnline = isOnline,
        showOnlineBadge = chat.type == ChatType.DIRECT
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = chat.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false)
        )

        Text(
          text = chat.lastMessageTime,
          style = MaterialTheme.typography.labelSmall,
          color = if (chat.unreadCount > 0) ConnectBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (chat.lastMessageSenderId == currentUserId && chat.lastMessageStatus != null) {
            val (statusIcon, statusTint, statusLabel) = when (chat.lastMessageStatus) {
              MessageStatus.READ -> Triple(Icons.Default.DoneAll, ConnectBluePrimary, "Read")
              MessageStatus.DELIVERED -> Triple(Icons.Default.DoneAll, MaterialTheme.colorScheme.onSurfaceVariant, "Delivered")
              MessageStatus.SENT, MessageStatus.SENDING -> Triple(Icons.Default.Done, MaterialTheme.colorScheme.onSurfaceVariant, "Sent")
            }
            Icon(
              imageVector = statusIcon,
              contentDescription = statusLabel,
              tint = statusTint,
              modifier = Modifier
                .size(15.dp)
                .testTag("chat_list_receipt_${chat.id}")
            )
            Spacer(modifier = Modifier.width(4.dp))
          }

          Text(
            text = chat.lastMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = if (chat.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (chat.isPinned) {
            Icon(
              imageVector = Icons.Default.PushPin,
              contentDescription = "Pinned",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
          }

          if (chat.unreadCount > 0) {
            Box(
              modifier = Modifier
                .clip(CircleShape)
                .background(ConnectBluePrimary)
                .padding(horizontal = 6.dp, vertical = 2.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = chat.unreadCount.toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    IconButton(
      onClick = onLongClick,
      modifier = Modifier
        .size(48.dp)
        .testTag("chat_item_more_${chat.id}")
    ) {
      Icon(
        imageVector = Icons.Default.MoreVert,
        contentDescription = "Chat Options",
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
