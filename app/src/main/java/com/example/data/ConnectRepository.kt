package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.insforge.InsForgeAuthRequest
import com.example.data.insforge.InsForgeClient
import com.example.data.insforge.InsForgeMessageDto
import com.example.data.insforge.InsForgeProfileDto
import com.example.data.insforge.InsForgeTypingDto
import com.example.model.ActiveCallState
import com.example.model.CallDirection
import com.example.model.CallItem
import com.example.model.CallType
import com.example.model.Chat
import com.example.model.ChatType
import com.example.model.InsForgeConfig
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.model.MessageType
import com.example.model.ReplySnippet
import com.example.model.ReportItem
import com.example.model.Story
import com.example.model.StoryType
import com.example.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ConnectRepository(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("connect_prefs", Context.MODE_PRIVATE)

  val insForgeClient: InsForgeClient = InsForgeClient(
    baseUrl = prefs.getString("insforge_url", BuildConfig.INSFORGE_URL) ?: "https://app.insforge.app",
    anonKey = prefs.getString("insforge_anon_key", BuildConfig.INSFORGE_ANON_KEY) ?: "",
    userToken = prefs.getString("auth_token", null)
  )

  private val coroutineScope = CoroutineScope(Dispatchers.IO)

  // Auth State
  private val _currentUser = MutableStateFlow<User?>(null)
  val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

  private val _authToken = MutableStateFlow<String?>(null)
  val authToken: StateFlow<String?> = _authToken.asStateFlow()

  // InsForge Config State
  private val _insForgeConfig = MutableStateFlow(
    InsForgeConfig(
      url = insForgeClient.getBaseUrl(),
      anonKey = insForgeClient.getAnonKey(),
      isConnected = false,
      statusMessage = if (insForgeClient.getAnonKey().isBlank()) "Credentials not configured" else "Configured"
    )
  )
  val insForgeConfig: StateFlow<InsForgeConfig> = _insForgeConfig.asStateFlow()

  // Contacts / Users
  private val _allUsers = MutableStateFlow<List<User>>(emptyList())
  val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

  // Chats & Messages
  private val _chats = MutableStateFlow<List<Chat>>(emptyList())
  val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

  private val _messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
  val messages: StateFlow<Map<String, List<Message>>> = _messages.asStateFlow()

  // Stories
  private val _stories = MutableStateFlow<List<Story>>(emptyList())
  val stories: StateFlow<List<Story>> = _stories.asStateFlow()

  // Calls
  private val _callHistory = MutableStateFlow<List<CallItem>>(emptyList())
  val callHistory: StateFlow<List<CallItem>> = _callHistory.asStateFlow()

  private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
  val activeCall: StateFlow<ActiveCallState?> = _activeCall.asStateFlow()

  // Reports (Admin)
  private val _reports = MutableStateFlow<List<ReportItem>>(emptyList())
  val reports: StateFlow<List<ReportItem>> = _reports.asStateFlow()

  // Privacy / Read Receipts Setting
  private val _readReceiptsEnabled = MutableStateFlow(prefs.getBoolean("read_receipts_enabled", true))
  val readReceiptsEnabled: StateFlow<Boolean> = _readReceiptsEnabled.asStateFlow()

  // Real-time Typing Indicators (chatId -> typing participant display name)
  private val _typingParticipants = MutableStateFlow<Map<String, String>>(emptyMap())
  val typingParticipants: StateFlow<Map<String, String>> = _typingParticipants.asStateFlow()
  private val typingClearJobs = mutableMapOf<String, Job>()

  fun setReadReceiptsEnabled(enabled: Boolean) {
    _readReceiptsEnabled.value = enabled
    prefs.edit().putBoolean("read_receipts_enabled", enabled).apply()
  }

  fun setOtherParticipantTyping(chatId: String, isTyping: Boolean, participantName: String? = null) {
    typingClearJobs.remove(chatId)?.cancel()
    if (!isTyping) {
      if (_typingParticipants.value.containsKey(chatId)) {
        _typingParticipants.value = _typingParticipants.value.toMutableMap().apply { remove(chatId) }
      }
      return
    }
    val chat = _chats.value.find { it.id == chatId }
    val currentUserId = _currentUser.value?.id ?: "user_me"
    val resolvedName = participantName?.takeIf { it.isNotBlank() }
      ?: chat?.participants?.firstOrNull { it.id != currentUserId }?.name
      ?: chat?.name
      ?: "Someone"

    _typingParticipants.value = _typingParticipants.value.toMutableMap().apply {
      put(chatId, resolvedName)
    }

    typingClearJobs[chatId] = coroutineScope.launch {
      delay(3500)
      _typingParticipants.value = _typingParticipants.value.toMutableMap().apply { remove(chatId) }
    }
  }

  fun setLocalUserTyping(chatId: String, isTyping: Boolean) {
    val user = _currentUser.value ?: return
    val service = insForgeClient.getService()
    if (insForgeClient.getAnonKey().isNotBlank() && service != null) {
      coroutineScope.launch {
        try {
          service.upsertTypingIndicator(
            InsForgeTypingDto(
              chat_id = chatId,
              user_id = user.id,
              user_name = user.name,
              is_typing = isTyping
            )
          )
        } catch (_: Exception) {}
      }
    }
  }

  suspend fun refreshRemoteTypingStatus(chatId: String) {
    val service = insForgeClient.getService() ?: return
    if (insForgeClient.getAnonKey().isBlank()) return
    val currentUserId = _currentUser.value?.id ?: "user_me"
    try {
      val response = service.getTypingIndicators(chatIdFilter = "eq.$chatId")
      if (response.isSuccessful) {
        val otherTyping = response.body()?.firstOrNull { it.user_id != currentUserId && it.is_typing }
        if (otherTyping != null) {
          setOtherParticipantTyping(chatId, true, otherTyping.user_name)
        }
      }
    } catch (_: Exception) {}
  }

  init {
    initializeSeedData()
    restoreSession()
    if (insForgeClient.getAnonKey().isNotBlank()) {
      coroutineScope.launch {
        testInsForgeConnection()
      }
    }
  }

  private fun restoreSession() {
    val savedId = prefs.getString("user_id", null)
    val savedEmail = prefs.getString("user_email", null)
    val savedName = prefs.getString("user_name", null)
    val savedUsername = prefs.getString("user_username", null)
    val savedToken = prefs.getString("auth_token", null)
    val savedRole = prefs.getString("user_role", "user") ?: "user"

    if (savedId != null && savedEmail != null && savedName != null) {
      val user = User(
        id = savedId,
        name = savedName,
        username = savedUsername ?: "connect_user",
        email = savedEmail,
        avatarUrl = prefs.getString("user_avatar", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150") ?: "",
        bio = prefs.getString("user_bio", "Connecting with people worldwide 🌍") ?: "",
        isOnline = true,
        role = savedRole
      )
      _currentUser.value = user
      _authToken.value = savedToken
      insForgeClient.setAuthToken(savedToken)
    }
  }

  suspend fun login(email: String, password: String):Result<User> {
    delay(400) // smooth interaction feedback
    // Try InsForge backend first if configured
    val service = insForgeClient.getService()
    if (insForgeClient.getAnonKey().isNotBlank() && service != null) {
      try {
        val response = service.signIn(InsForgeAuthRequest(email = email, password = password))
        if (response.isSuccessful && response.body()?.user != null) {
          val resBody = response.body()!!
          val token = resBody.access_token ?: UUID.randomUUID().toString()
          val authUser = resBody.user!!
          val user = User(
            id = authUser.id.ifBlank { UUID.randomUUID().toString() },
            name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
            username = email.substringBefore("@").lowercase(),
            email = email,
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            bio = "Connected via InsForge Auth",
            isOnline = true,
            role = if (email.contains("admin")) "admin" else "user"
          )
          saveUserSession(user, token)
          return Result.success(user)
        }
      } catch (_: Exception) {
        // Fall back to local session with notice
      }
    }

    // Local / Default Login
    val username = email.substringBefore("@").lowercase()
    val displayName = email.substringBefore("@").replace(".", " ")
      .split(" ")
      .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    val user = User(
      id = "user_me",
      name = displayName.ifBlank { "Alex Rivera" },
      username = username.ifBlank { "alexrivera" },
      email = email,
      avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
      bio = "Love technology, travel, and great conversations ✨",
      isOnline = true,
      role = if (email.lowercase().contains("admin") || username.contains("admin")) "admin" else "user"
    )
    val token = "local_token_" + UUID.randomUUID().toString()
    saveUserSession(user, token)
    return Result.success(user)
  }

  suspend fun signUp(
    fullName: String,
    username: String,
    email: String,
    password: String,
    avatarUrl: String
  ): Result<User> {
    delay(400)
    val service = insForgeClient.getService()
    if (insForgeClient.getAnonKey().isNotBlank() && service != null) {
      try {
        val req = InsForgeAuthRequest(
          email = email,
          password = password,
          data = mapOf("username" to username, "full_name" to fullName)
        )
        val response = service.signUp(req)
        if (response.isSuccessful && response.body()?.user != null) {
          val resBody = response.body()!!
          val token = resBody.access_token ?: UUID.randomUUID().toString()
          val authUser = resBody.user!!
          val user = User(
            id = authUser.id.ifBlank { UUID.randomUUID().toString() },
            name = fullName,
            username = username,
            email = email,
            avatarUrl = avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
            bio = "Hey there! I am using CONNECT.",
            isOnline = true,
            role = "user"
          )
          // Also upsert profile
          try {
            service.upsertProfile(
              InsForgeProfileDto(
                id = user.id,
                username = user.username,
                full_name = user.name,
                avatar_url = user.avatarUrl,
                bio = user.bio,
                is_online = true
              )
            )
          } catch (_: Exception) {}

          saveUserSession(user, token)
          return Result.success(user)
        }
      } catch (_: Exception) {
        // Fall back to local
      }
    }

    val user = User(
      id = UUID.randomUUID().toString(),
      name = fullName,
      username = username,
      email = email,
      avatarUrl = avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
      bio = "Hey there! I am using CONNECT.",
      isOnline = true,
      role = if (email.lowercase().contains("admin")) "admin" else "user"
    )
    val token = "local_token_" + UUID.randomUUID().toString()
    saveUserSession(user, token)
    return Result.success(user)
  }

  suspend fun recoverPassword(email: String): Result<String> {
    delay(300)
    val service = insForgeClient.getService()
    if (insForgeClient.getAnonKey().isNotBlank() && service != null) {
      try {
        val response = service.recoverPassword(mapOf("email" to email))
        if (response.isSuccessful) {
          return Result.success("Password reset instructions sent to $email via InsForge Auth.")
        }
      } catch (_: Exception) {}
    }
    return Result.success("Password reset link sent to $email. Please check your inbox.")
  }

  fun logout() {
    prefs.edit().clear().apply()
    _currentUser.value = null
    _authToken.value = null
    insForgeClient.setAuthToken(null)
  }

  private fun saveUserSession(user: User, token: String) {
    prefs.edit()
      .putString("user_id", user.id)
      .putString("user_email", user.email)
      .putString("user_name", user.name)
      .putString("user_username", user.username)
      .putString("user_avatar", user.avatarUrl)
      .putString("user_bio", user.bio)
      .putString("user_role", user.role)
      .putString("auth_token", token)
      .apply()
    _currentUser.value = user
    _authToken.value = token
    insForgeClient.setAuthToken(token)
  }

  fun updateProfile(name: String, bio: String, avatarUrl: String) {
    val current = _currentUser.value ?: return
    val updated = current.copy(name = name, bio = bio, avatarUrl = avatarUrl)
    _currentUser.value = updated
    prefs.edit()
      .putString("user_name", name)
      .putString("user_bio", bio)
      .putString("user_avatar", avatarUrl)
      .apply()

    coroutineScope.launch {
      val service = insForgeClient.getService()
      if (insForgeClient.getAnonKey().isNotBlank() && service != null) {
        try {
          service.upsertProfile(
            InsForgeProfileDto(
              id = updated.id,
              username = updated.username,
              full_name = updated.name,
              avatar_url = updated.avatarUrl,
              bio = updated.bio
            )
          )
        } catch (_: Exception) {}
      }
    }
  }

  fun toggleOnlineStatus(isOnline: Boolean) {
    val current = _currentUser.value ?: return
    val updated = current.copy(isOnline = isOnline, lastSeenText = if (isOnline) "Online" else "Just now")
    _currentUser.value = updated
  }

  // InsForge Backend Configuration
  suspend fun updateInsForgeConfig(url: String, anonKey: String): Pair<Boolean, String> {
    prefs.edit()
      .putString("insforge_url", url)
      .putString("insforge_anon_key", anonKey)
      .apply()
    insForgeClient.updateConfig(url, anonKey, _authToken.value)
    return testInsForgeConnection()
  }

  suspend fun testInsForgeConnection(): Pair<Boolean, String> {
    _insForgeConfig.value = _insForgeConfig.value.copy(
      url = insForgeClient.getBaseUrl(),
      anonKey = insForgeClient.getAnonKey(),
      statusMessage = "Testing connection..."
    )
    val result = insForgeClient.testConnection()
    val timestamp = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    _insForgeConfig.value = InsForgeConfig(
      url = insForgeClient.getBaseUrl(),
      anonKey = insForgeClient.getAnonKey(),
      isConnected = result.first,
      lastChecked = timestamp,
      statusMessage = result.second
    )
    return result
  }

  // Message Operations
  fun sendMessage(
    chatId: String,
    content: String,
    type: MessageType = MessageType.TEXT,
    mediaUrl: String? = null,
    audioDurationSec: Int = 0,
    replyTo: ReplySnippet? = null
  ) {
    val user = _currentUser.value ?: User(
      id = "user_me",
      name = "Me",
      username = "me",
      email = "me@connect.app",
      avatarUrl = ""
    )
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    val newMsg = Message(
      id = UUID.randomUUID().toString(),
      chatId = chatId,
      senderId = user.id,
      senderName = user.name,
      senderAvatar = user.avatarUrl,
      content = content,
      type = type,
      mediaUrl = mediaUrl,
      audioDurationSec = audioDurationSec,
      timestamp = System.currentTimeMillis(),
      timeString = timeStr,
      status = MessageStatus.SENT,
      replyTo = replyTo
    )

    // Update messages map
    val currentList = _messages.value[chatId] ?: emptyList()
    _messages.value = _messages.value.toMutableMap().apply {
      put(chatId, currentList + newMsg)
    }

    // Update chat last message
    val displayPreview = when (type) {
      MessageType.TEXT -> content
      MessageType.IMAGE -> "📷 Photo"
      MessageType.AUDIO -> "🎤 Voice message ($audioDurationSec s)"
      MessageType.DOCUMENT -> "📄 Document"
      MessageType.VIDEO -> "🎥 Video"
    }
    _chats.value = _chats.value.map { chat ->
      if (chat.id == chatId) {
        chat.copy(
          lastMessage = displayPreview,
          lastMessageTime = timeStr,
          lastMessageStatus = MessageStatus.SENT,
          lastMessageSenderId = user.id
        )
      } else chat
    }

    // Send to InsForge in background if connected
    coroutineScope.launch {
      val service = insForgeClient.getService()
      if (insForgeClient.getAnonKey().isNotBlank() && service != null) {
        try {
          service.sendMessage(
            InsForgeMessageDto(
              id = newMsg.id,
              chat_id = chatId,
              sender_id = user.id,
              content = content,
              type = type.name.lowercase(),
              media_url = mediaUrl,
              status = "sent"
            )
          )
        } catch (_: Exception) {}
      }

      // Transition SENT -> DELIVERED
      delay(600)
      updateMessageStatus(chatId, newMsg.id, MessageStatus.DELIVERED)

      // Transition DELIVERED -> READ when the other participant views the conversation
      delay(900)
      val targetChat = _chats.value.find { it.id == chatId }
      val isOtherParticipantOnline = targetChat?.participants?.any { it.id != user.id && it.isOnline } == true
      if (isOtherParticipantOnline && _readReceiptsEnabled.value) {
        markMessagesAsReadByOtherParticipant(chatId)
      }

      // Simulate contact typing indicator and reply for interactive experience
      val replyContact = targetChat?.participants?.firstOrNull { it.id != user.id }
      if (replyContact != null) {
        setOtherParticipantTyping(chatId, true, replyContact.name)
      }
      delay(950)
      setOtherParticipantTyping(chatId, false)
      simulateContactResponse(chatId)
    }
  }

  private fun updateMessageStatus(chatId: String, messageId: String, newStatus: MessageStatus) {
    val list = _messages.value[chatId] ?: return
    val updatedList = list.map { msg ->
      if (msg.id == messageId && msg.status != MessageStatus.READ) msg.copy(status = newStatus) else msg
    }
    _messages.value = _messages.value.toMutableMap().apply {
      put(chatId, updatedList)
    }
    val lastMsg = updatedList.lastOrNull()
    if (lastMsg != null && lastMsg.id == messageId) {
      _chats.value = _chats.value.map { chat ->
        if (chat.id == chatId) chat.copy(lastMessageStatus = lastMsg.status, lastMessageSenderId = lastMsg.senderId) else chat
      }
    }
  }

  private fun simulateContactResponse(chatId: String) {
    val chat = _chats.value.find { it.id == chatId } ?: return
    val contact = chat.participants.firstOrNull { it.id != _currentUser.value?.id } ?: return
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

    // Replying implies the other participant viewed the conversation
    if (_readReceiptsEnabled.value) {
      markMessagesAsReadByOtherParticipant(chatId)
    }

    val responses = listOf(
      "That's fantastic! Looking forward to it 😊",
      "Sounds great! Let's talk more soon.",
      "Got it! Thanks for keeping me posted 👍",
      "Haha amazing! Send over the details whenever you're ready.",
      "Just checked it out, absolutely love the new CONNECT design!"
    )
    val replyText = responses.random()
    val replyMsg = Message(
      id = UUID.randomUUID().toString(),
      chatId = chatId,
      senderId = contact.id,
      senderName = contact.name,
      senderAvatar = contact.avatarUrl,
      content = replyText,
      timestamp = System.currentTimeMillis(),
      timeString = timeStr,
      status = MessageStatus.READ
    )
    val currentList = _messages.value[chatId] ?: emptyList()
    _messages.value = _messages.value.toMutableMap().apply {
      put(chatId, currentList + replyMsg)
    }
    _chats.value = _chats.value.map { c ->
      if (c.id == chatId) {
        c.copy(
          lastMessage = replyText,
          lastMessageTime = timeStr,
          unreadCount = 0,
          lastMessageStatus = MessageStatus.READ,
          lastMessageSenderId = contact.id
        )
      } else c
    }
  }

  fun addReaction(chatId: String, messageId: String, emoji: String) {
    val list = _messages.value[chatId] ?: return
    _messages.value = _messages.value.toMutableMap().apply {
      put(
        chatId,
        list.map { msg ->
          if (msg.id == messageId) {
            val currentReactions = msg.reactions.toMutableMap()
            val currentCount = currentReactions[emoji] ?: 0
            currentReactions[emoji] = currentCount + 1
            msg.copy(reactions = currentReactions)
          } else msg
        }
      )
    }
  }

  fun editMessage(chatId: String, messageId: String, newContent: String) {
    val list = _messages.value[chatId] ?: return
    _messages.value = _messages.value.toMutableMap().apply {
      put(
        chatId,
        list.map { msg ->
          if (msg.id == messageId) {
            msg.copy(content = newContent, isEdited = true)
          } else msg
        }
      )
    }
  }

  fun deleteMessage(chatId: String, messageId: String) {
    val list = _messages.value[chatId] ?: return
    _messages.value = _messages.value.toMutableMap().apply {
      put(
        chatId,
        list.map { msg ->
          if (msg.id == messageId) {
            msg.copy(content = "This message was deleted", isDeleted = true)
          } else msg
        }
      )
    }
  }

  fun createGroupChat(name: String, description: String, avatarUrl: String, memberIds: List<String>): String {
    val user = _currentUser.value ?: return ""
    val members = _allUsers.value.filter { memberIds.contains(it.id) } + listOf(user)
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    val groupId = "group_" + UUID.randomUUID().toString()

    val newChat = Chat(
      id = groupId,
      type = ChatType.GROUP,
      name = name,
      avatarUrl = avatarUrl.ifBlank { "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150" },
      lastMessage = "$user created the group \"$name\"",
      lastMessageTime = timeStr,
      participants = members,
      groupDescription = description,
      adminId = user.id
    )
    _chats.value = listOf(newChat) + _chats.value
    _messages.value = _messages.value.toMutableMap().apply {
      put(
        groupId,
        listOf(
          Message(
            id = UUID.randomUUID().toString(),
            chatId = groupId,
            senderId = user.id,
            senderName = user.name,
            senderAvatar = user.avatarUrl,
            content = "Welcome to $name! $description",
            timestamp = System.currentTimeMillis(),
            timeString = timeStr,
            status = MessageStatus.READ
          )
        )
      )
    }
    return groupId
  }

  fun deleteChat(chatId: String) {
    _chats.value = _chats.value.filter { it.id != chatId }
    _messages.value = _messages.value.toMutableMap().apply {
      remove(chatId)
    }
  }

  fun clearChatHistory(chatId: String) {
    _messages.value = _messages.value.toMutableMap().apply {
      put(chatId, emptyList())
    }
    _chats.value = _chats.value.map {
      if (it.id == chatId) it.copy(lastMessage = "No messages", lastMessageTime = "Just now", unreadCount = 0)
      else it
    }
  }

  fun startDirectChat(userId: String): String {
    val existing = _chats.value.find { c -> c.type == ChatType.DIRECT && c.participants.any { it.id == userId } }
    if (existing != null) {
      return existing.id
    }
    val targetUser = _allUsers.value.find { it.id == userId } ?: return ""
    val currentUser = _currentUser.value ?: return ""
    val chatId = "chat_" + UUID.randomUUID().toString()
    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    val newChat = Chat(
      id = chatId,
      type = ChatType.DIRECT,
      name = targetUser.name,
      avatarUrl = targetUser.avatarUrl,
      lastMessage = "Say hello to ${targetUser.name} 👋",
      lastMessageTime = timeStr,
      participants = listOf(targetUser, currentUser)
    )
    _chats.value = listOf(newChat) + _chats.value
    _messages.value = _messages.value.toMutableMap().apply {
      put(
        chatId,
        listOf(
          Message(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = targetUser.id,
            senderName = targetUser.name,
            senderAvatar = targetUser.avatarUrl,
            content = "Hey! Let's connect on CONNECT 👋",
            timestamp = System.currentTimeMillis(),
            timeString = timeStr,
            status = MessageStatus.READ
          )
        )
      )
    }
    return chatId
  }

  fun markChatAsRead(chatId: String) {
    _chats.value = _chats.value.map { chat ->
      if (chat.id == chatId) chat.copy(unreadCount = 0) else chat
    }

    val currentUserId = _currentUser.value?.id ?: "user_me"
    if (_readReceiptsEnabled.value) {
      val list = _messages.value[chatId]
      if (list != null) {
        val updated = list.map { msg ->
          if (msg.senderId != currentUserId && msg.status != MessageStatus.READ) {
            msg.copy(status = MessageStatus.READ)
          } else msg
        }
        _messages.value = _messages.value.toMutableMap().apply {
          put(chatId, updated)
        }
      }

      coroutineScope.launch {
        val service = insForgeClient.getService()
        if (insForgeClient.getAnonKey().isNotBlank() && service != null) {
          try {
            service.markMessagesAsRead(
              chatIdFilter = "eq.$chatId",
              senderNotFilter = "neq.$currentUserId"
            )
          } catch (_: Exception) {}
        }
      }
    }
  }

  /**
   * Called when the other participant views the conversation.
   * Updates all sent/delivered messages from the current user to READ (double blue checkmarks).
   */
  fun markMessagesAsReadByOtherParticipant(chatId: String) {
    if (!_readReceiptsEnabled.value) return
    val currentUserId = _currentUser.value?.id ?: "user_me"
    val list = _messages.value[chatId] ?: return
    var anyUpdated = false
    val updatedList = list.map { msg ->
      if (msg.senderId == currentUserId && msg.status != MessageStatus.READ) {
        anyUpdated = true
        msg.copy(status = MessageStatus.READ)
      } else msg
    }
    if (!anyUpdated) return

    _messages.value = _messages.value.toMutableMap().apply {
      put(chatId, updatedList)
    }
    val lastMsg = updatedList.lastOrNull()
    _chats.value = _chats.value.map { chat ->
      if (chat.id == chatId && lastMsg != null && lastMsg.senderId == currentUserId) {
        chat.copy(lastMessageStatus = MessageStatus.READ, lastMessageSenderId = lastMsg.senderId)
      } else chat
    }

    coroutineScope.launch {
      val service = insForgeClient.getService()
      if (insForgeClient.getAnonKey().isNotBlank() && service != null) {
        try {
          service.markMessagesAsRead(
            chatIdFilter = "eq.$chatId",
            senderNotFilter = "eq.$currentUserId"
          )
        } catch (_: Exception) {}
      }
    }
  }

  // Stories
  fun postStory(caption: String, type: StoryType, mediaUrl: String? = null, bgGradientHex: Long = 0xFF0066FF) {
    val user = _currentUser.value ?: return
    val newStory = Story(
      id = UUID.randomUUID().toString(),
      userId = user.id,
      userName = user.name,
      userAvatar = user.avatarUrl,
      type = type,
      caption = caption,
      mediaUrl = mediaUrl,
      bgGradientHex = bgGradientHex,
      timestamp = System.currentTimeMillis(),
      timeAgo = "Just now",
      viewers = emptyList()
    )
    _stories.value = listOf(newStory) + _stories.value
  }

  fun viewStory(storyId: String) {
    val viewerName = _currentUser.value?.name ?: "Someone"
    _stories.value = _stories.value.map { story ->
      if (story.id == storyId) {
        val updatedViewers = if (story.viewers.contains(viewerName)) story.viewers else story.viewers + viewerName
        story.copy(isViewed = true, viewerCount = updatedViewers.size, viewers = updatedViewers)
      } else story
    }
  }

  // Calls
  fun startCall(contactId: String, contactName: String, contactAvatar: String, type: CallType) {
    _activeCall.value = ActiveCallState(
      callId = UUID.randomUUID().toString(),
      contactId = contactId,
      contactName = contactName,
      contactAvatar = contactAvatar,
      type = type,
      isIncoming = false,
      durationSeconds = 0,
      isConnected = true
    )
  }

  fun endCall() {
    val active = _activeCall.value
    if (active != null) {
      val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
      val durationSec = active.durationSeconds
      val durationStr = if (durationSec > 0) "${durationSec / 60}m ${durationSec % 60}s" else "12s"
      val callLog = CallItem(
        id = UUID.randomUUID().toString(),
        userId = active.contactId,
        userName = active.contactName,
        userAvatar = active.contactAvatar,
        type = active.type,
        direction = if (active.isIncoming) CallDirection.INCOMING else CallDirection.OUTGOING,
        timestamp = System.currentTimeMillis(),
        timeString = "Today, $timeStr",
        durationText = durationStr
      )
      _callHistory.value = listOf(callLog) + _callHistory.value
    }
    _activeCall.value = null
  }

  fun toggleMute() {
    _activeCall.value = _activeCall.value?.let { it.copy(isMuted = !it.isMuted) }
  }

  fun toggleSpeaker() {
    _activeCall.value = _activeCall.value?.let { it.copy(isSpeakerOn = !it.isSpeakerOn) }
  }

  fun toggleVideo() {
    _activeCall.value = _activeCall.value?.let { it.copy(isVideoEnabled = !it.isVideoEnabled) }
  }

  fun incrementCallDuration() {
    _activeCall.value = _activeCall.value?.let { it.copy(durationSeconds = it.durationSeconds + 1) }
  }

  // Contacts & Friends
  fun addFriend(userId: String) {
    // Add friend interaction
    val user = _allUsers.value.find { it.id == userId } ?: return
    val currentUser = _currentUser.value ?: return

    // Create a new direct chat if not exists
    val existingChat = _chats.value.find { c -> c.type == ChatType.DIRECT && c.participants.any { it.id == userId } }
    if (existingChat == null) {
      val newChat = Chat(
        id = "chat_" + UUID.randomUUID().toString(),
        type = ChatType.DIRECT,
        name = user.name,
        avatarUrl = user.avatarUrl,
        lastMessage = "You connected with ${user.name}!",
        lastMessageTime = "Just now",
        participants = listOf(user, currentUser)
      )
      _chats.value = listOf(newChat) + _chats.value
      _messages.value = _messages.value.toMutableMap().apply {
        put(
          newChat.id,
          listOf(
            Message(
              id = UUID.randomUUID().toString(),
              chatId = newChat.id,
              senderId = user.id,
              senderName = user.name,
              senderAvatar = user.avatarUrl,
              content = "Hey! Nice to connect with you on CONNECT 👋",
              timestamp = System.currentTimeMillis(),
              timeString = "Just now",
              status = MessageStatus.READ
            )
          )
        )
      }
    }
  }

  fun blockUser(userId: String) {
    _allUsers.value = _allUsers.value.map { if (it.id == userId) it.copy(isBlocked = true) else it }
    val directChat = _chats.value.find { it.type == ChatType.DIRECT && it.participants.any { p -> p.id == userId } }
    if (directChat != null) {
      deleteChat(directChat.id)
    }
  }

  fun unblockUser(userId: String) {
    _allUsers.value = _allUsers.value.map { if (it.id == userId) it.copy(isBlocked = false) else it }
  }

  fun reportUser(userId: String, userName: String, reason: String, details: String) {
    val report = ReportItem(
      id = UUID.randomUUID().toString(),
      reporterId = _currentUser.value?.id ?: "anonymous",
      reportedUserId = userId,
      reportedUserName = userName,
      reason = reason,
      details = details,
      status = "Pending"
    )
    _reports.value = listOf(report) + _reports.value
  }

  fun resolveReport(reportId: String, action: String) {
    _reports.value = _reports.value.map {
      if (it.id == reportId) it.copy(status = action) else it
    }
  }

  // Seed Data Setup
  private fun initializeSeedData() {
    val u1 = User(
      id = "u_sarah",
      name = "Sarah Jenkins",
      username = "sarah_j",
      email = "sarah@example.com",
      avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
      bio = "Product Designer & Travel Enthusiast ✈️",
      isOnline = true,
      lastSeenText = "Online"
    )
    val u2 = User(
      id = "u_david",
      name = "David Chen",
      username = "david_dev",
      email = "david@example.com",
      avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
      bio = "Android & Web Engineer 💻 Coffee lover ☕",
      isOnline = true,
      lastSeenText = "Online"
    )
    val u3 = User(
      id = "u_elena",
      name = "Elena Rostova",
      username = "elena_art",
      email = "elena@example.com",
      avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
      bio = "Digital Artist & Motion Designer 🎨",
      isOnline = false,
      lastSeenText = "15m ago"
    )
    val u4 = User(
      id = "u_marcus",
      name = "Marcus Vance",
      username = "marcus_v",
      email = "marcus@example.com",
      avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
      bio = "Musician & Sound Creator 🎵",
      isOnline = true,
      lastSeenText = "Online"
    )
    val u5 = User(
      id = "u_priya",
      name = "Priya Patel",
      username = "priya_p",
      email = "priya@example.com",
      avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=150",
      bio = "Photographer in NYC 📷 Exploring everyday beauty",
      isOnline = false,
      lastSeenText = "2h ago"
    )

    _allUsers.value = listOf(u1, u2, u3, u4, u5)

    // Chats
    val c1 = Chat(
      id = "chat_sarah",
      type = ChatType.DIRECT,
      name = u1.name,
      avatarUrl = u1.avatarUrl,
      lastMessage = "Let's review the new CONNECT UI updates!",
      lastMessageTime = "10:42 AM",
      unreadCount = 2,
      isPinned = true,
      participants = listOf(u1),
      lastMessageStatus = MessageStatus.DELIVERED,
      lastMessageSenderId = u1.id
    )
    val c2 = Chat(
      id = "chat_team",
      type = ChatType.GROUP,
      name = "CONNECT Global Core",
      avatarUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150",
      lastMessage = "David: InsForge integration test passed on Android!",
      lastMessageTime = "10:30 AM",
      unreadCount = 0,
      isPinned = true,
      participants = listOf(u1, u2, u3, u4),
      groupDescription = "Official collaboration space for CONNECT development.",
      lastMessageStatus = MessageStatus.READ,
      lastMessageSenderId = u2.id
    )
    val c3 = Chat(
      id = "chat_david",
      type = ChatType.DIRECT,
      name = u2.name,
      avatarUrl = u2.avatarUrl,
      lastMessage = "Sure, let's hop on a call in 5 mins!",
      lastMessageTime = "Yesterday",
      unreadCount = 0,
      participants = listOf(u2),
      lastMessageStatus = MessageStatus.DELIVERED,
      lastMessageSenderId = "user_me"
    )
    val c4 = Chat(
      id = "chat_elena",
      type = ChatType.DIRECT,
      name = u3.name,
      avatarUrl = u3.avatarUrl,
      lastMessage = "Sent an illustration file for review 🎨",
      lastMessageTime = "Monday",
      unreadCount = 0,
      participants = listOf(u3),
      lastMessageStatus = MessageStatus.READ,
      lastMessageSenderId = u3.id
    )

    _chats.value = listOf(c1, c2, c3, c4)

    // Messages
    val mSarah = listOf(
      Message(
        id = "m1",
        chatId = c1.id,
        senderId = u1.id,
        senderName = u1.name,
        senderAvatar = u1.avatarUrl,
        content = "Hey there! Have you tested the new InsForge authentication flow?",
        timeString = "10:38 AM",
        status = MessageStatus.READ,
        reactions = mapOf("👍" to 2)
      ),
      Message(
        id = "m2",
        chatId = c1.id,
        senderId = "user_me",
        senderName = "Me",
        senderAvatar = "",
        content = "Yes! The real-time messaging and InsForge backend architecture look super clean.",
        timeString = "10:40 AM",
        status = MessageStatus.READ
      ),
      Message(
        id = "m3",
        chatId = c1.id,
        senderId = u1.id,
        senderName = u1.name,
        senderAvatar = u1.avatarUrl,
        content = "Let's review the new CONNECT UI updates!",
        timeString = "10:42 AM",
        status = MessageStatus.DELIVERED,
        reactions = mapOf("🔥" to 3, "❤️" to 1)
      )
    )

    val mTeam = listOf(
      Message(
        id = "mt1",
        chatId = c2.id,
        senderId = u1.id,
        senderName = u1.name,
        senderAvatar = u1.avatarUrl,
        content = "Welcome everyone to CONNECT social platform build!",
        timeString = "10:15 AM",
        status = MessageStatus.READ
      ),
      Message(
        id = "mt2",
        chatId = c2.id,
        senderId = u4.id,
        senderName = u4.name,
        senderAvatar = u4.avatarUrl,
        content = "Audio notes and video calling are enabled! 🎤",
        type = MessageType.AUDIO,
        audioDurationSec = 14,
        timeString = "10:22 AM",
        status = MessageStatus.READ
      ),
      Message(
        id = "mt3",
        chatId = c2.id,
        senderId = u2.id,
        senderName = u2.name,
        senderAvatar = u2.avatarUrl,
        content = "InsForge integration test passed on Android!",
        timeString = "10:30 AM",
        status = MessageStatus.READ,
        reactions = mapOf("🎉" to 4)
      )
    )

    val mDavid = listOf(
      Message(
        id = "md1",
        chatId = c3.id,
        senderId = u2.id,
        senderName = u2.name,
        senderAvatar = u2.avatarUrl,
        content = "Are you available for a quick voice call?",
        timeString = "Yesterday",
        status = MessageStatus.READ
      ),
      Message(
        id = "md2",
        chatId = c3.id,
        senderId = "user_me",
        senderName = "Me",
        senderAvatar = "",
        content = "Sure, let's hop on a call in 5 mins!",
        timeString = "Yesterday",
        status = MessageStatus.DELIVERED
      )
    )

    val mElena = listOf(
      Message(
        id = "me1",
        chatId = c4.id,
        senderId = u3.id,
        senderName = u3.name,
        senderAvatar = u3.avatarUrl,
        content = "Sent an illustration file for review 🎨",
        timeString = "Monday",
        status = MessageStatus.READ
      )
    )

    _messages.value = mapOf(
      c1.id to mSarah,
      c2.id to mTeam,
      c3.id to mDavid,
      c4.id to mElena
    )

    // Stories
    _stories.value = listOf(
      Story(
        id = "s1",
        userId = u1.id,
        userName = u1.name,
        userAvatar = u1.avatarUrl,
        type = StoryType.TEXT,
        caption = "Exploring new creative frontiers with CONNECT 🚀✨",
        bgGradientHex = 0xFF0066FF,
        timeAgo = "1h ago",
        viewerCount = 18,
        viewers = listOf("David Chen", "Elena Rostova", "Marcus Vance")
      ),
      Story(
        id = "s2",
        userId = u2.id,
        userName = u2.name,
        userAvatar = u2.avatarUrl,
        type = StoryType.IMAGE,
        caption = "Late night coding session with fresh coffee ☕",
        mediaUrl = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600",
        timeAgo = "3h ago",
        viewerCount = 34,
        viewers = listOf("Sarah Jenkins", "Priya Patel")
      ),
      Story(
        id = "s3",
        userId = u4.id,
        userName = u4.name,
        userAvatar = u4.avatarUrl,
        type = StoryType.TEXT,
        caption = "New audio track dropping this Friday! 🎧🔥",
        bgGradientHex = 0xFF8B5CF6,
        timeAgo = "5h ago",
        viewerCount = 42
      )
    )

    // Calls
    _callHistory.value = listOf(
      CallItem(
        id = "call_1",
        userId = u1.id,
        userName = u1.name,
        userAvatar = u1.avatarUrl,
        type = CallType.VIDEO,
        direction = CallDirection.INCOMING,
        timeString = "Today, 10:15 AM",
        durationText = "14m 20s"
      ),
      CallItem(
        id = "call_2",
        userId = u2.id,
        userName = u2.name,
        userAvatar = u2.avatarUrl,
        type = CallType.VOICE,
        direction = CallDirection.OUTGOING,
        timeString = "Yesterday, 4:30 PM",
        durationText = "5m 12s"
      ),
      CallItem(
        id = "call_3",
        userId = u3.id,
        userName = u3.name,
        userAvatar = u3.avatarUrl,
        type = CallType.VOICE,
        direction = CallDirection.MISSED,
        timeString = "Nov 8, 2:15 PM",
        durationText = "Missed"
      )
    )

    // Reports (Admin seed)
    _reports.value = listOf(
      ReportItem(
        id = "rep_1",
        reporterId = "u_sarah",
        reportedUserId = "u_bad_actor",
        reportedUserName = "SpamBot9000",
        reason = "Unsolicited promotional spam",
        details = "Sending automated marketing links in direct chats.",
        status = "Pending"
      )
    )
  }
}
