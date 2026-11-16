package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.ConnectRepository
import com.example.model.Message
import com.example.model.MessageStatus
import com.example.ui.ConnectViewModel
import com.example.ui.chats.MessageBubble
import com.example.ui.profile.ProfileScreen
import com.example.ui.theme.ConnectTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CONNECT", appName)
  }

  @Test
  fun `messenger new chat delete chat and block user work`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ConnectRepository(context)
    repo.login("alex@connect.app", "password123")

    val newChatId = repo.startDirectChat("u_marcus")
    assertTrue(newChatId.isNotBlank())
    assertNotNull(repo.chats.value.find { it.id == newChatId })

    repo.clearChatHistory(newChatId)
    assertEquals(0, repo.messages.value[newChatId]?.size ?: -1)

    repo.blockUser("u_marcus")
    assertTrue(repo.allUsers.value.first { it.id == "u_marcus" }.isBlocked)
    assertFalse(repo.chats.value.any { it.id == newChatId })

    repo.unblockUser("u_marcus")
    assertFalse(repo.allUsers.value.first { it.id == "u_marcus" }.isBlocked)

    val sarahChat = repo.chats.value.firstOrNull { it.participants.any { p -> p.id == "u_sarah" } }
    assertNotNull(sarahChat)
    repo.deleteChat(sarahChat!!.id)
    assertFalse(repo.chats.value.any { it.id == sarahChat.id })
  }

  @Test
  fun `read receipts transition to READ when other participant views conversation and respect toggle`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ConnectRepository(context)
    repo.login("alex@connect.app", "password123")

    // Seed message md2 in chat_david starts as DELIVERED
    val initialDavidMsg = repo.messages.value["chat_david"]?.find { it.id == "md2" }
    assertNotNull(initialDavidMsg)
    assertEquals(MessageStatus.DELIVERED, initialDavidMsg!!.status)

    // When read receipts disabled, status stays DELIVERED
    repo.setReadReceiptsEnabled(false)
    assertFalse(repo.readReceiptsEnabled.value)
    repo.markMessagesAsReadByOtherParticipant("chat_david")
    assertEquals(
      MessageStatus.DELIVERED,
      repo.messages.value["chat_david"]?.find { it.id == "md2" }?.status
    )

    // Re-enable read receipts and mark conversation viewed by other participant -> READ
    repo.setReadReceiptsEnabled(true)
    assertTrue(repo.readReceiptsEnabled.value)
    repo.markMessagesAsReadByOtherParticipant("chat_david")
    assertEquals(
      MessageStatus.READ,
      repo.messages.value["chat_david"]?.find { it.id == "md2" }?.status
    )
    assertEquals(
      MessageStatus.READ,
      repo.chats.value.find { it.id == "chat_david" }?.lastMessageStatus
    )

    // markChatAsRead clears unreadCount and marks incoming messages READ
    repo.markChatAsRead("chat_sarah")
    assertEquals(0, repo.chats.value.find { it.id == "chat_sarah" }?.unreadCount)
    assertTrue(repo.messages.value["chat_sarah"]?.all { it.status == MessageStatus.READ } == true)
  }

  @Test
  fun `typing indicator appears and clears for other participant`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = ConnectRepository(context)
    repo.login("alex@connect.app", "password123")

    assertFalse(repo.typingParticipants.value.containsKey("chat_sarah"))

    repo.setOtherParticipantTyping("chat_sarah", true, "Sarah Jenkins")
    assertEquals("Sarah Jenkins", repo.typingParticipants.value["chat_sarah"])

    repo.setOtherParticipantTyping("chat_sarah", false)
    assertFalse(repo.typingParticipants.value.containsKey("chat_sarah"))
  }

  @Test
  fun `message bubble hides blue double checkmarks when read receipts disabled`() {
    val msg = Message(
      id = "msg_test_1",
      chatId = "chat_sarah",
      senderId = "user_me",
      senderName = "Alex",
      senderAvatar = "",
      content = "Hello!",
      status = MessageStatus.READ
    )

    composeTestRule.setContent {
      ConnectTheme {
        MessageBubble(
          message = msg,
          isMe = true,
          showReadReceipts = false,
          onLongClick = {},
          onReplyClick = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("read_receipt_msg_test_1", useUnmergedTree = true).assertDoesNotExist()
    composeTestRule.onNodeWithTag("message_status_msg_test_1", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Delivered", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun `message bubble shows blue double checkmarks when read receipts enabled`() {
    val msg = Message(
      id = "msg_test_2",
      chatId = "chat_sarah",
      senderId = "user_me",
      senderName = "Alex",
      senderAvatar = "",
      content = "Hello again!",
      status = MessageStatus.READ
    )

    composeTestRule.setContent {
      ConnectTheme {
        MessageBubble(
          message = msg,
          isMe = true,
          showReadReceipts = true,
          onLongClick = {},
          onReplyClick = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("read_receipt_msg_test_2", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Read", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun `profile settings read receipts switch toggles viewModel state`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = ConnectViewModel(app)
    viewModel.toggleReadReceipts(true)

    composeTestRule.setContent {
      ConnectTheme {
        ProfileScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    assertTrue(viewModel.readReceiptsEnabled.value)

    composeTestRule.onNodeWithTag("read_receipts_switch").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertFalse(viewModel.readReceiptsEnabled.value)

    composeTestRule.onNodeWithTag("read_receipts_switch").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(viewModel.readReceiptsEnabled.value)
  }
}
