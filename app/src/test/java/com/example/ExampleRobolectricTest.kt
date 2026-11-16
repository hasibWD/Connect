package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ConnectRepository
import com.example.model.MessageStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

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
}
