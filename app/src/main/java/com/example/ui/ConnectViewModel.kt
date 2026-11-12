package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ConnectRepository
import com.example.model.ActiveCallState
import com.example.model.CallItem
import com.example.model.CallType
import com.example.model.Chat
import com.example.model.InsForgeConfig
import com.example.model.Message
import com.example.model.MessageType
import com.example.model.ReplySnippet
import com.example.model.ReportItem
import com.example.model.Story
import com.example.model.StoryType
import com.example.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
  CHATS,
  CONTACTS,
  STORIES,
  CALLS,
  PROFILE
}

class ConnectViewModel(application: Application) : AndroidViewModel(application) {

  val repository = ConnectRepository(application)

  val currentUser: StateFlow<User?> = repository.currentUser
  val insForgeConfig: StateFlow<InsForgeConfig> = repository.insForgeConfig
  val allUsers: StateFlow<List<User>> = repository.allUsers
  val rawChats: StateFlow<List<Chat>> = repository.chats
  val rawMessages: StateFlow<Map<String, List<Message>>> = repository.messages
  val stories: StateFlow<List<Story>> = repository.stories
  val callHistory: StateFlow<List<CallItem>> = repository.callHistory
  val activeCall: StateFlow<ActiveCallState?> = repository.activeCall
  val reports: StateFlow<List<ReportItem>> = repository.reports

  // UI Navigation & Dialog states
  private val _currentTab = MutableStateFlow(MainTab.CHATS)
  val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

  private val _activeChatId = MutableStateFlow<String?>(null)
  val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

  private val _activeStory = MutableStateFlow<Story?>(null)
  val activeStory: StateFlow<Story?> = _activeStory.asStateFlow()

  private val _showInsForgeDialog = MutableStateFlow(false)
  val showInsForgeDialog: StateFlow<Boolean> = _showInsForgeDialog.asStateFlow()

  private val _showCreateGroupDialog = MutableStateFlow(false)
  val showCreateGroupDialog: StateFlow<Boolean> = _showCreateGroupDialog.asStateFlow()

  private val _showCreateStoryDialog = MutableStateFlow(false)
  val showCreateStoryDialog: StateFlow<Boolean> = _showCreateStoryDialog.asStateFlow()

  private val _showNewChatDialog = MutableStateFlow(false)
  val showNewChatDialog: StateFlow<Boolean> = _showNewChatDialog.asStateFlow()

  private val _showAdminPanel = MutableStateFlow(false)
  val showAdminPanel: StateFlow<Boolean> = _showAdminPanel.asStateFlow()

  private val _isDarkMode = MutableStateFlow(false)
  val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

  // Search Query
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  // Filtered Chats
  val filteredChats: StateFlow<List<Chat>> = combine(rawChats, _searchQuery) { chats, query ->
    if (query.isBlank()) chats
    else chats.filter { it.name.contains(query, ignoreCase = true) || it.lastMessage.contains(query, ignoreCase = true) }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Filtered Users
  val filteredUsers: StateFlow<List<User>> = combine(allUsers, _searchQuery) { users, query ->
    if (query.isBlank()) users
    else users.filter { it.name.contains(query, ignoreCase = true) || it.username.contains(query, ignoreCase = true) }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Reply snippet state
  private val _replyingTo = MutableStateFlow<ReplySnippet?>(null)
  val replyingTo: StateFlow<ReplySnippet?> = _replyingTo.asStateFlow()

  // Call timer job
  private var callTimerJob: Job? = null

  // Auth form states
  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  private val _authSuccessMessage = MutableStateFlow<String?>(null)
  val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

  private val _authLoading = MutableStateFlow(false)
  val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

  fun setTab(tab: MainTab) {
    _currentTab.value = tab
    _searchQuery.value = ""
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun openChat(chatId: String) {
    _activeChatId.value = chatId
    repository.markChatAsRead(chatId)
  }

  fun closeChat() {
    _activeChatId.value = null
    _replyingTo.value = null
  }

  fun openStory(story: Story) {
    _activeStory.value = story
    repository.viewStory(story.id)
  }

  fun closeStory() {
    _activeStory.value = null
  }

  fun setReplyingTo(message: Message?) {
    if (message == null) {
      _replyingTo.value = null
    } else {
      _replyingTo.value = ReplySnippet(
        id = message.id,
        senderName = message.senderName,
        content = message.content,
        type = message.type
      )
    }
  }

  fun setShowInsForgeDialog(show: Boolean) {
    _showInsForgeDialog.value = show
  }

  fun setShowCreateGroupDialog(show: Boolean) {
    _showCreateGroupDialog.value = show
  }

  fun setShowCreateStoryDialog(show: Boolean) {
    _showCreateStoryDialog.value = show
  }

  fun setShowAdminPanel(show: Boolean) {
    _showAdminPanel.value = show
  }

  fun toggleDarkMode() {
    _isDarkMode.value = !_isDarkMode.value
  }

  // Auth Operations
  fun login(email: String, pass: String) {
    if (email.isBlank() || pass.isBlank()) {
      _authError.value = "Please enter both email and password."
      return
    }
    _authLoading.value = true
    _authError.value = null
    viewModelScope.launch {
      val res = repository.login(email, pass)
      _authLoading.value = false
      res.onFailure {
        _authError.value = it.localizedMessage ?: "Login failed"
      }
    }
  }

  fun signUp(fullName: String, username: String, email: String, pass: String, confirmPass: String, avatarUrl: String) {
    if (fullName.isBlank() || username.isBlank() || email.isBlank() || pass.isBlank()) {
      _authError.value = "All fields are required."
      return
    }
    if (pass != confirmPass) {
      _authError.value = "Passwords do not match."
      return
    }
    if (pass.length < 6) {
      _authError.value = "Password must be at least 6 characters."
      return
    }
    _authLoading.value = true
    _authError.value = null
    viewModelScope.launch {
      val res = repository.signUp(fullName, username, email, pass, avatarUrl)
      _authLoading.value = false
      res.onFailure {
        _authError.value = it.localizedMessage ?: "Signup failed"
      }
    }
  }

  fun recoverPassword(email: String) {
    if (email.isBlank()) {
      _authError.value = "Please enter your email address."
      return
    }
    viewModelScope.launch {
      val res = repository.recoverPassword(email)
      _authSuccessMessage.value = res.getOrDefault("Recovery link sent.")
    }
  }

  fun clearAuthMessages() {
    _authError.value = null
    _authSuccessMessage.value = null
  }

  fun logout() {
    repository.logout()
    closeChat()
    closeStory()
    _showAdminPanel.value = false
  }

  // Messaging
  fun sendMessage(content: String, type: MessageType = MessageType.TEXT, mediaUrl: String? = null, audioDurationSec: Int = 0) {
    val chatId = _activeChatId.value ?: return
    if (content.isBlank() && mediaUrl == null && type == MessageType.TEXT) return
    repository.sendMessage(
      chatId = chatId,
      content = content,
      type = type,
      mediaUrl = mediaUrl,
      audioDurationSec = audioDurationSec,
      replyTo = _replyingTo.value
    )
    _replyingTo.value = null
  }

  fun addReaction(messageId: String, emoji: String) {
    val chatId = _activeChatId.value ?: return
    repository.addReaction(chatId, messageId, emoji)
  }

  fun editMessage(messageId: String, newContent: String) {
    val chatId = _activeChatId.value ?: return
    repository.editMessage(chatId, messageId, newContent)
  }

  fun deleteMessage(messageId: String) {
    val chatId = _activeChatId.value ?: return
    repository.deleteMessage(chatId, messageId)
  }

  fun createGroup(name: String, description: String, avatarUrl: String, memberIds: List<String>) {
    if (name.isBlank() || memberIds.isEmpty()) return
    val newGroupId = repository.createGroupChat(name, description, avatarUrl, memberIds)
    _showCreateGroupDialog.value = false
    openChat(newGroupId)
  }

  // Stories
  fun postStory(caption: String, type: StoryType, mediaUrl: String? = null, bgGradientHex: Long = 0xFF0066FF) {
    repository.postStory(caption, type, mediaUrl, bgGradientHex)
    _showCreateStoryDialog.value = false
  }

  // Calling
  fun startCall(contactId: String, contactName: String, contactAvatar: String, type: CallType) {
    repository.startCall(contactId, contactName, contactAvatar, type)
    startCallTimer()
  }

  private fun startCallTimer() {
    callTimerJob?.cancel()
    callTimerJob = viewModelScope.launch {
      while (true) {
        delay(1000)
        if (repository.activeCall.value != null) {
          repository.incrementCallDuration()
        } else {
          break
        }
      }
    }
  }

  fun endCall() {
    callTimerJob?.cancel()
    repository.endCall()
  }

  fun toggleMute() = repository.toggleMute()
  fun toggleSpeaker() = repository.toggleSpeaker()
  fun toggleVideo() = repository.toggleVideo()

  // Profile & Moderation
  fun updateProfile(name: String, bio: String, avatarUrl: String) {
    repository.updateProfile(name, bio, avatarUrl)
  }

  fun toggleOnlineStatus(isOnline: Boolean) {
    repository.toggleOnlineStatus(isOnline)
  }

  fun addFriend(userId: String) {
    repository.addFriend(userId)
  }

  fun blockUser(userId: String) {
    repository.blockUser(userId)
  }

  fun unblockUser(userId: String) {
    repository.unblockUser(userId)
  }

  fun reportUser(userId: String, userName: String, reason: String, details: String) {
    repository.reportUser(userId, userName, reason, details)
  }

  fun resolveReport(reportId: String, action: String) {
    repository.resolveReport(reportId, action)
  }

  fun setShowNewChatDialog(show: Boolean) {
    _showNewChatDialog.value = show
  }

  fun deleteChat(chatId: String) {
    if (_activeChatId.value == chatId) {
      closeChat()
    }
    repository.deleteChat(chatId)
  }

  fun clearChatHistory(chatId: String) {
    repository.clearChatHistory(chatId)
  }

  fun startDirectChat(userId: String) {
    val chatId = repository.startDirectChat(userId)
    if (chatId.isNotBlank()) {
      _showNewChatDialog.value = false
      openChat(chatId)
    }
  }

  // InsForge Config
  fun updateInsForge(url: String, anonKey: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val res = repository.updateInsForgeConfig(url, anonKey)
      onResult(res.first, res.second)
    }
  }

  fun testInsForge(onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val res = repository.testInsForgeConnection()
      onResult(res.first, res.second)
    }
  }
}
