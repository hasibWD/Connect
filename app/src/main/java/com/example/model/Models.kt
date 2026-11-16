package com.example.model

enum class ChatType {
  DIRECT,
  GROUP
}

enum class MessageType {
  TEXT,
  IMAGE,
  AUDIO,
  DOCUMENT,
  VIDEO
}

enum class MessageStatus {
  SENDING,
  SENT,
  DELIVERED,
  READ
}

enum class StoryType {
  TEXT,
  IMAGE
}

enum class CallType {
  VOICE,
  VIDEO
}

enum class CallDirection {
  INCOMING,
  OUTGOING,
  MISSED
}

data class User(
  val id: String,
  val name: String,
  val username: String,
  val email: String,
  val avatarUrl: String,
  val bio: String = "Hey there! I am using CONNECT.",
  val isOnline: Boolean = false,
  val lastSeenText: String = "Just now",
  val role: String = "user", // "user" or "admin"
  val isBlocked: Boolean = false
)

data class ReplySnippet(
  val id: String,
  val senderName: String,
  val content: String,
  val type: MessageType = MessageType.TEXT
)

data class Message(
  val id: String,
  val chatId: String,
  val senderId: String,
  val senderName: String,
  val senderAvatar: String,
  val content: String,
  val type: MessageType = MessageType.TEXT,
  val mediaUrl: String? = null,
  val audioDurationSec: Int = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val timeString: String = "10:30 AM",
  val status: MessageStatus = MessageStatus.SENT,
  val reactions: Map<String, Int> = emptyMap(),
  val replyTo: ReplySnippet? = null,
  val isEdited: Boolean = false,
  val isDeleted: Boolean = false
)

data class Chat(
  val id: String,
  val type: ChatType = ChatType.DIRECT,
  val name: String,
  val avatarUrl: String,
  val lastMessage: String,
  val lastMessageTime: String,
  val unreadCount: Int = 0,
  val isPinned: Boolean = false,
  val participants: List<User> = emptyList(),
  val groupDescription: String = "",
  val adminId: String = "",
  val lastMessageStatus: MessageStatus? = null,
  val lastMessageSenderId: String? = null
)

data class Story(
  val id: String,
  val userId: String,
  val userName: String,
  val userAvatar: String,
  val type: StoryType = StoryType.TEXT,
  val caption: String = "",
  val mediaUrl: String? = null,
  val bgGradientHex: Long = 0xFF0066FF,
  val timestamp: Long = System.currentTimeMillis(),
  val timeAgo: String = "Just now",
  val viewerCount: Int = 0,
  val isViewed: Boolean = false,
  val viewers: List<String> = emptyList()
)

data class CallItem(
  val id: String,
  val userId: String,
  val userName: String,
  val userAvatar: String,
  val type: CallType = CallType.VOICE,
  val direction: CallDirection = CallDirection.OUTGOING,
  val timestamp: Long = System.currentTimeMillis(),
  val timeString: String = "Today, 11:24 AM",
  val durationText: String = "3m 42s"
)

data class ActiveCallState(
  val callId: String,
  val contactId: String,
  val contactName: String,
  val contactAvatar: String,
  val type: CallType,
  val isIncoming: Boolean = false,
  val durationSeconds: Int = 0,
  val isMuted: Boolean = false,
  val isSpeakerOn: Boolean = false,
  val isVideoEnabled: Boolean = true,
  val isConnected: Boolean = true
)

data class ReportItem(
  val id: String,
  val reporterId: String,
  val reportedUserId: String,
  val reportedUserName: String,
  val reason: String,
  val details: String,
  val status: String = "Pending", // "Pending", "Action Taken", "Dismissed"
  val timestamp: Long = System.currentTimeMillis()
)

data class InsForgeConfig(
  val url: String = "https://app.insforge.app",
  val anonKey: String = "",
  val isConnected: Boolean = false,
  val lastChecked: String? = null,
  val statusMessage: String = "Ready to connect"
)
