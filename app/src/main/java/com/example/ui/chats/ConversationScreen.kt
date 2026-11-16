package com.example.ui.chats

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CallType
import com.example.model.Chat
import com.example.model.ChatType
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.model.MessageType
import com.example.ui.ConnectViewModel
import com.example.ui.common.ConnectAvatar
import com.example.ui.theme.BubbleReceiverDark
import com.example.ui.theme.BubbleReceiverLight
import com.example.ui.theme.BubbleSenderLight
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary
import com.example.ui.theme.ConnectGreenOnline
import com.example.ui.theme.ConnectRedError
import com.example.ui.theme.TextReceiverDark
import com.example.ui.theme.TextReceiverLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
  chatId: String,
  viewModel: ConnectViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.closeChat()
  }

  val context = LocalContext.current
  val chats by viewModel.rawChats.collectAsState()
  val allMessagesMap by viewModel.rawMessages.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val replyingTo by viewModel.replyingTo.collectAsState()
  val readReceiptsEnabled by viewModel.readReceiptsEnabled.collectAsState()
  val typingParticipants by viewModel.typingParticipants.collectAsState()
  val typingSender = typingParticipants[chatId]

  val chat = chats.find { it.id == chatId } ?: run {
    viewModel.closeChat()
    return
  }

  val messages = allMessagesMap[chatId] ?: emptyList()
  val listState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()

  var inputText by remember { mutableStateOf("") }
  var showEmojiDrawer by remember { mutableStateOf(false) }
  var showAttachmentSheet by remember { mutableStateOf(false) }
  var showChatProfileSheet by remember { mutableStateOf(false) }
  var selectedMessageForAction by remember { mutableStateOf<Message?>(null) }
  var messageToEdit by remember { mutableStateOf<Message?>(null) }
  var showMoreMenu by remember { mutableStateOf(false) }
  var showReportDialog by remember { mutableStateOf(false) }
  var showDeleteChatDialog by remember { mutableStateOf(false) }
  var showBlockUserDialog by remember { mutableStateOf(false) }

  LaunchedEffect(chatId) {
    viewModel.markChatAsRead(chatId)
  }

  // Auto scroll to bottom on new message
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { viewModel.closeChat() },
          modifier = Modifier.testTag("conversation_back_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        // Messenger Clickable Contact Profile Header
        Row(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showChatProfileSheet = true }
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag("conversation_header_profile_click"),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ConnectAvatar(
            avatarUrl = chat.avatarUrl,
            name = chat.name,
            size = 40.dp,
            isOnline = chat.participants.any { it.isOnline },
            showOnlineBadge = chat.type == ChatType.DIRECT
          )

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = chat.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (typingSender != null) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("chat_header_typing_indicator")
              ) {
                TypingDotsIndicator(color = ConnectBluePrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = if (chat.type == ChatType.GROUP) "$typingSender is typing..." else "typing...",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = ConnectBluePrimary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            } else {
              val subtitle = if (chat.type == ChatType.GROUP) {
                "${chat.participants.size} members"
              } else {
                val contact = chat.participants.firstOrNull { it.id != currentUser?.id }
                if (contact?.isOnline == true) "Online" else contact?.lastSeenText ?: "Offline"
              }
              Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (subtitle == "Online") ConnectGreenOnline else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Voice Call Button
        IconButton(
          onClick = {
            val contact = chat.participants.firstOrNull { it.id != currentUser?.id } ?: chat.participants.firstOrNull()
            viewModel.startCall(
              contactId = contact?.id ?: "unknown",
              contactName = chat.name,
              contactAvatar = chat.avatarUrl,
              type = CallType.VOICE
            )
          },
          modifier = Modifier.testTag("conversation_voice_call_button")
        ) {
          Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = ConnectBluePrimary)
        }

        // Video Call Button
        IconButton(
          onClick = {
            val contact = chat.participants.firstOrNull { it.id != currentUser?.id } ?: chat.participants.firstOrNull()
            viewModel.startCall(
              contactId = contact?.id ?: "unknown",
              contactName = chat.name,
              contactAvatar = chat.avatarUrl,
              type = CallType.VIDEO
            )
          },
          modifier = Modifier.testTag("conversation_video_call_button")
        ) {
          Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = ConnectBluePrimary)
        }

        // More Options Menu
        Box {
          IconButton(onClick = { showMoreMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
          }
          DropdownMenu(
            expanded = showMoreMenu,
            onDismissRequest = { showMoreMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Chat Profile & Info") },
              onClick = {
                showMoreMenu = false
                showChatProfileSheet = true
              }
            )
            DropdownMenuItem(
              text = { Text("Clear Chat History") },
              onClick = {
                showMoreMenu = false
                viewModel.clearChatHistory(chat.id)
                Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
              }
            )
            val otherParticipant = chat.participants.firstOrNull { it.id != currentUser?.id }
            if (chat.type == ChatType.DIRECT && otherParticipant != null) {
              DropdownMenuItem(
                text = { Text("Block ${otherParticipant.name}", color = ConnectRedError) },
                onClick = {
                  showMoreMenu = false
                  showBlockUserDialog = true
                }
              )
            }
            DropdownMenuItem(
              text = { Text("Delete Chat", color = ConnectRedError) },
              onClick = {
                showMoreMenu = false
                showDeleteChatDialog = true
              }
            )
            DropdownMenuItem(
              text = { Text("Report Chat") },
              onClick = {
                showMoreMenu = false
                showReportDialog = true
              }
            )
          }
        }
      }
    }

    // Message List
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 14.dp),
      contentPadding = PaddingValues(vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(messages, key = { it.id }) { message ->
        val isMe = message.senderId == (currentUser?.id ?: "user_me")
        MessageBubble(
          message = message,
          isMe = isMe,
          showReadReceipts = readReceiptsEnabled,
          onLongClick = { selectedMessageForAction = message },
          onReplyClick = { viewModel.setReplyingTo(message) }
        )
      }
    }

    // Quoted Reply Banner
    AnimatedVisibility(visible = replyingTo != null) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .width(4.dp)
              .height(36.dp)
              .background(ConnectBluePrimary, RoundedCornerShape(2.dp))
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Replying to ${replyingTo?.senderName}",
              style = MaterialTheme.typography.labelSmall,
              color = ConnectBluePrimary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = replyingTo?.content ?: "",
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          IconButton(onClick = { viewModel.setReplyingTo(null) }) {
            Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(18.dp))
          }
        }
      }
    }

    // Bottom Input Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 4.dp,
      modifier = Modifier
        .fillMaxWidth()
        .imePadding()
        .navigationBarsPadding()
    ) {
      Column {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Emoji Button
          IconButton(
            onClick = { showEmojiDrawer = !showEmojiDrawer },
            modifier = Modifier.testTag("emoji_picker_toggle")
          ) {
            Icon(
              imageVector = Icons.Default.EmojiEmotions,
              contentDescription = "Emoji",
              tint = if (showEmojiDrawer) ConnectBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Attachment Button
          IconButton(
            onClick = { showAttachmentSheet = true },
            modifier = Modifier.testTag("attachment_button")
          ) {
            Icon(
              imageVector = Icons.Default.AttachFile,
              contentDescription = "Attach",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Input Text Field
          OutlinedTextField(
            value = inputText,
            onValueChange = {
              inputText = it
              viewModel.onMessageInputChanged(chatId, it)
            },
            placeholder = { Text("Type a message...") },
            maxLines = 4,
            modifier = Modifier
              .weight(1f)
              .testTag("chat_message_input"),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              focusedContainerColor = MaterialTheme.colorScheme.surface
            )
          )

          Spacer(modifier = Modifier.width(6.dp))

          // Voice / Send Action Button
          if (inputText.isNotBlank()) {
            IconButton(
              onClick = {
                val toSend = inputText.trim()
                inputText = ""
                showEmojiDrawer = false
                viewModel.sendMessage(content = toSend, type = MessageType.TEXT)
              },
              modifier = Modifier
                .size(44.dp)
                .background(ConnectBluePrimary, CircleShape)
                .testTag("send_message_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          } else {
            // Voice Message Trigger Button
            IconButton(
              onClick = {
                // Simulate recording voice note
                Toast.makeText(context, "Voice note recorded and sent! 🎤", Toast.LENGTH_SHORT).show()
                viewModel.sendMessage(
                  content = "Voice message",
                  type = MessageType.AUDIO,
                  audioDurationSec = (5..25).random()
                )
              },
              modifier = Modifier
                .size(44.dp)
                .background(ConnectBluePrimary, CircleShape)
                .testTag("voice_message_button")
            ) {
              Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Message",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
          }
        }

        // Emoji Drawer
        AnimatedVisibility(visible = showEmojiDrawer) {
          EmojiDrawer(
            onEmojiSelected = { emoji ->
              inputText += emoji
              viewModel.onMessageInputChanged(chatId, inputText)
            }
          )
        }
      }
    }
  }

  // Message Action Bottom Sheet / Long-press dialog
  if (selectedMessageForAction != null) {
    val msg = selectedMessageForAction!!
    val isMine = msg.senderId == (currentUser?.id ?: "user_me")

    AlertDialog(
      onDismissRequest = { selectedMessageForAction = null },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          listOf("❤️", "😂", "👍", "🔥", "🎉", "😮").forEach { emoji ->
            Text(
              text = emoji,
              fontSize = 24.sp,
              modifier = Modifier
                .clip(CircleShape)
                .clickable {
                  viewModel.addReaction(msg.id, emoji)
                  selectedMessageForAction = null
                }
                .padding(4.dp)
            )
          }
        }
      },
      text = {
        Column {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                viewModel.setReplyingTo(msg)
                selectedMessageForAction = null
              }
              .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = ConnectBluePrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Reply")
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Message", msg.content))
                Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                selectedMessageForAction = null
              }
              .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ConnectBluePrimary)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Copy Text")
          }

          if (isMine && !msg.isDeleted) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  messageToEdit = msg
                  selectedMessageForAction = null
                }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, tint = ConnectBluePrimary)
              Spacer(modifier = Modifier.width(12.dp))
              Text("Edit Message")
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.deleteMessage(msg.id)
                  selectedMessageForAction = null
                }
                .padding(vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Delete, contentDescription = null, tint = ConnectRedError)
              Spacer(modifier = Modifier.width(12.dp))
              Text("Delete Message", color = ConnectRedError)
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { selectedMessageForAction = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Edit Message Dialog
  if (messageToEdit != null) {
    var editText by remember { mutableStateOf(messageToEdit!!.content) }
    AlertDialog(
      onDismissRequest = { messageToEdit = null },
      title = { Text("Edit Message") },
      text = {
        OutlinedTextField(
          value = editText,
          onValueChange = { editText = it },
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (editText.isNotBlank()) {
              viewModel.editMessage(messageToEdit!!.id, editText)
            }
            messageToEdit = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ConnectBluePrimary)
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { messageToEdit = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // Attachment Options Modal Bottom Sheet
  if (showAttachmentSheet) {
    ModalBottomSheet(
      onDismissRequest = { showAttachmentSheet = false },
      sheetState = rememberModalBottomSheetState()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp)
      ) {
        Text(
          text = "Share Content",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          AttachmentActionItem(
            icon = Icons.Default.Image,
            label = "Gallery Photo",
            color = ConnectBluePrimary,
            onClick = {
              showAttachmentSheet = false
              viewModel.sendMessage(
                content = "Check out this photo!",
                type = MessageType.IMAGE,
                mediaUrl = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600"
              )
            }
          )

          AttachmentActionItem(
            icon = Icons.Default.Description,
            label = "Document",
            color = Color(0xFF8B5CF6),
            onClick = {
              showAttachmentSheet = false
              viewModel.sendMessage(
                content = "Project_Specs_v2.pdf",
                type = MessageType.DOCUMENT
              )
            }
          )

          AttachmentActionItem(
            icon = Icons.Default.Mic,
            label = "Audio File",
            color = Color(0xFF10B981),
            onClick = {
              showAttachmentSheet = false
              viewModel.sendMessage(
                content = "Audio Recording",
                type = MessageType.AUDIO,
                audioDurationSec = 18
              )
            }
          )
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // Report Dialog
  if (showReportDialog) {
    var reportReason by remember { mutableStateOf("Inappropriate Content") }
    var reportDetails by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showReportDialog = false },
      title = { Text("Report User or Chat", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          Text(
            "Help keep CONNECT safe. Reports are reviewed by platform administrators.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = reportReason,
            onValueChange = { reportReason = it },
            label = { Text("Reason") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = reportDetails,
            onValueChange = { reportDetails = it },
            label = { Text("Additional details") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.reportUser(
              userId = chat.id,
              userName = chat.name,
              reason = reportReason,
              details = reportDetails
            )
            showReportDialog = false
            Toast.makeText(context, "Report submitted to admin team", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ConnectRedError)
        ) {
          Text("Submit Report")
        }
      },
      dismissButton = {
        TextButton(onClick = { showReportDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Messenger Chat Profile Bottom Sheet
  if (showChatProfileSheet) {
    ChatProfileSheet(
      chat = chat,
      viewModel = viewModel,
      onDismiss = { showChatProfileSheet = false }
    )
  }

  // Delete Chat Confirmation Dialog
  if (showDeleteChatDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteChatDialog = false },
      title = { Text("Delete Entire Chat?", fontWeight = FontWeight.Bold) },
      text = {
        Text("Once you delete your copy of this conversation, it cannot be undone. All messages and shared media in this chat will be removed.")
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteChatDialog = false
            viewModel.deleteChat(chat.id)
            Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ConnectRedError)
        ) {
          Text("Delete Chat")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteChatDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Block User Confirmation Dialog
  if (showBlockUserDialog) {
    val targetUser = chat.participants.firstOrNull { it.id != currentUser?.id }
    AlertDialog(
      onDismissRequest = { showBlockUserDialog = false },
      title = { Text("Block ${targetUser?.name ?: "User"}?", fontWeight = FontWeight.Bold) },
      text = {
        Text("${targetUser?.name ?: "They"} won't be able to send you messages or view your stories and online status on CONNECT. The conversation will be removed from your chat list.")
      },
      confirmButton = {
        Button(
          onClick = {
            showBlockUserDialog = false
            if (targetUser != null) {
              viewModel.blockUser(targetUser.id)
            }
            viewModel.deleteChat(chat.id)
            Toast.makeText(context, "${targetUser?.name ?: "User"} blocked", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ConnectRedError)
        ) {
          Text("Block")
        }
      },
      dismissButton = {
        TextButton(onClick = { showBlockUserDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
  message: Message,
  isMe: Boolean,
  showReadReceipts: Boolean = true,
  onLongClick: () -> Unit,
  onReplyClick: () -> Unit
) {
  var isAudioPlaying by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
  ) {
    Surface(
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isMe) 16.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 16.dp
      ),
      color = if (isMe) BubbleSenderLight else MaterialTheme.colorScheme.surfaceVariant,
      modifier = Modifier
        .widthIn(max = 300.dp)
        .clip(RoundedCornerShape(16.dp))
        .combinedClickable(
          onClick = { /* normal tap */ },
          onLongClick = onLongClick
        )
        .testTag("message_bubble_${message.id}")
    ) {
      Column(modifier = Modifier.padding(10.dp)) {
        // Quoted Reply Preview
        if (message.replyTo != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isMe) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 6.dp)
          ) {
            Row(modifier = Modifier.padding(6.dp)) {
              Box(
                modifier = Modifier
                  .width(3.dp)
                  .height(28.dp)
                  .background(if (isMe) Color.White else ConnectBluePrimary, RoundedCornerShape(2.dp))
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text(
                  text = message.replyTo.senderName,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isMe) Color.White else ConnectBluePrimary
                )
                Text(
                  text = message.replyTo.content,
                  style = MaterialTheme.typography.bodySmall,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  color = if (isMe) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }

        // Image Attachment
        if (message.type == MessageType.IMAGE && message.mediaUrl != null) {
          AsyncImage(
            model = message.mediaUrl,
            contentDescription = "Shared photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp)
              .clip(RoundedCornerShape(10.dp))
          )
          Spacer(modifier = Modifier.height(6.dp))
        }

        // Audio Message Player
        if (message.type == MessageType.AUDIO) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            IconButton(
              onClick = { isAudioPlaying = !isAudioPlaying },
              modifier = Modifier
                .size(36.dp)
                .background(if (isMe) Color.White else ConnectBluePrimary, CircleShape)
            ) {
              Icon(
                imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isAudioPlaying) "Pause" else "Play",
                tint = if (isMe) ConnectBluePrimary else Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Waveform bars simulation
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              listOf(10, 18, 24, 14, 28, 20, 16, 26, 12, 22, 30, 16, 10).forEach { barHeight ->
                Box(
                  modifier = Modifier
                    .width(3.dp)
                    .height(barHeight.dp)
                    .background(
                      if (isMe) Color.White.copy(alpha = 0.85f) else ConnectBluePrimary.copy(alpha = 0.8f),
                      RoundedCornerShape(1.dp)
                    )
                )
              }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${message.audioDurationSec}s",
              style = MaterialTheme.typography.labelSmall,
              color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Text Message Content
        if (message.content.isNotBlank() && message.type != MessageType.AUDIO) {
          Text(
            text = message.content,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
          )
        }

        // Metadata: Timestamp & Status
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          modifier = Modifier.align(Alignment.End),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (message.isEdited) {
            Text(
              text = "(edited) ",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.sp,
              color = if (isMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Text(
            text = message.timeString,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = if (isMe) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (isMe) {
            Spacer(modifier = Modifier.width(4.dp))
            val effectiveStatus = if (!showReadReceipts && message.status == MessageStatus.READ) {
              MessageStatus.DELIVERED
            } else {
              message.status
            }
            when (effectiveStatus) {
              MessageStatus.READ -> {
                Box(
                  modifier = Modifier
                    .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                    .testTag("read_receipt_${message.id}"),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Read",
                    tint = ConnectBluePrimary,
                    modifier = Modifier.size(13.dp)
                  )
                }
              }
              MessageStatus.DELIVERED -> {
                Icon(
                  imageVector = Icons.Default.DoneAll,
                  contentDescription = "Delivered",
                  tint = Color.White,
                  modifier = Modifier
                    .size(14.dp)
                    .testTag("message_status_${message.id}")
                )
              }
              MessageStatus.SENT -> {
                Icon(
                  imageVector = Icons.Default.Done,
                  contentDescription = "Sent",
                  tint = Color.White.copy(alpha = 0.85f),
                  modifier = Modifier
                    .size(14.dp)
                    .testTag("message_status_${message.id}")
                )
              }
              MessageStatus.SENDING -> {
                Icon(
                  imageVector = Icons.Default.Done,
                  contentDescription = "Sending",
                  tint = Color.White.copy(alpha = 0.5f),
                  modifier = Modifier
                    .size(14.dp)
                    .testTag("message_status_${message.id}")
                )
              }
            }
          }
        }
      }
    }

    // Reaction Chips below Bubble
    if (message.reactions.isNotEmpty()) {
      LazyRow(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        items(message.reactions.entries.toList()) { (emoji, count) ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
          ) {
            Text(
              text = "$emoji $count",
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun AttachmentActionItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  color: Color,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clickable { onClick() }
      .padding(8.dp)
  ) {
    Box(
      modifier = Modifier
        .size(56.dp)
        .background(color.copy(alpha = 0.15f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
  }
}

@Composable
fun EmojiDrawer(
  onEmojiSelected: (String) -> Unit
) {
  val emojis = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
    "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
    "😋", "😛", "😜", "🤪", "😝", "🤗", "🤭", "🤫", "🤔", "🤐",
    "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙", "👏", "🙌",
    "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
    "🔥", "✨", "🎉", "🎊", "🚀", "⭐", "💯", "🎈", "🎂", "🍕"
  )

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    modifier = Modifier
      .fillMaxWidth()
      .height(180.dp)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)
    ) {
      val chunked = emojis.chunked(8)
      items(chunked) { rowEmojis ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          rowEmojis.forEach { emoji ->
            Text(
              text = emoji,
              fontSize = 24.sp,
              modifier = Modifier
                .clickable { onEmojiSelected(emoji) }
                .padding(6.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun TypingDotsIndicator(
  color: Color,
  modifier: Modifier = Modifier
) {
  val transition = rememberInfiniteTransition(label = "typingDots")
  val dot1 by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(450, delayMillis = 0), RepeatMode.Reverse),
    label = "dot1"
  )
  val dot2 by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(450, delayMillis = 150), RepeatMode.Reverse),
    label = "dot2"
  )
  val dot3 by transition.animateFloat(
    initialValue = 0.25f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(450, delayMillis = 300), RepeatMode.Reverse),
    label = "dot3"
  )

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    listOf(dot1, dot2, dot3).forEach { alphaValue ->
      Box(
        modifier = Modifier
          .size(5.dp)
          .alpha(alphaValue)
          .background(color, CircleShape)
      )
    }
  }
}
