package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ConnectRepository
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
}
