package com.example.ui.contacts

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.User
import com.example.ui.ConnectViewModel
import com.example.ui.common.ConnectAvatar
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary
import com.example.ui.theme.ConnectGreenOnline
import com.example.ui.theme.ConnectRedError

@Composable
fun ContactsScreen(
  viewModel: ConnectViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val users by viewModel.filteredUsers.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) }
  var userToReport by remember { mutableStateOf<User?>(null) }

  val myContacts = users.filter { it.id != currentUser?.id && !it.isBlocked }
  val onlineContacts = myContacts.filter { it.isOnline }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Header
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 1.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Contacts & Discovery",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Find people and start conversations",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Field
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { viewModel.setSearchQuery(it) },
          placeholder = { Text("Search by name or @username...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ConnectBluePrimary) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("contacts_search_input"),
          shape = RoundedCornerShape(24.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            focusedContainerColor = MaterialTheme.colorScheme.surface
          )
        )
      }
    }

    // Tabs: All Contacts / Online Now
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = ConnectBluePrimary,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = ConnectBluePrimary
        )
      }
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("All People (${myContacts.size})", fontWeight = FontWeight.SemiBold) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Online Now (${onlineContacts.size})", fontWeight = FontWeight.SemiBold) }
      )
    }

    val displayList = if (selectedTab == 0) myContacts else onlineContacts

    if (displayList.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "No contacts found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(displayList, key = { it.id }) { user ->
          ContactCard(
            user = user,
            onChatClick = {
              viewModel.startDirectChat(user.id)
            },
            onBlockClick = {
              viewModel.blockUser(user.id)
              Toast.makeText(context, "${user.name} blocked", Toast.LENGTH_SHORT).show()
            },
            onReportClick = { userToReport = user }
          )
        }
      }
    }
  }

  // Report Dialog
  if (userToReport != null) {
    val u = userToReport!!
    var reason by remember { mutableStateOf("Harassment / Spam") }
    var details by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { userToReport = null },
      title = { Text("Report @${u.username}", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Reason") },
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = details,
            onValueChange = { details = it },
            label = { Text("Details (optional)") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.reportUser(u.id, u.name, reason, details)
            userToReport = null
            Toast.makeText(context, "Report submitted", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ConnectRedError)
        ) {
          Text("Submit Report")
        }
      },
      dismissButton = {
        TextButton(onClick = { userToReport = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun ContactCard(
  user: User,
  onChatClick: () -> Unit,
  onBlockClick: () -> Unit,
  onReportClick: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("contact_card_${user.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      ConnectAvatar(
        avatarUrl = user.avatarUrl,
        name = user.name,
        size = 50.dp,
        isOnline = user.isOnline,
        showOnlineBadge = true
      )

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = user.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Text(
          text = "@${user.username}",
          style = MaterialTheme.typography.labelSmall,
          color = ConnectBluePrimary
        )
        Text(
          text = user.bio,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Chat Button
      Button(
        onClick = onChatClick,
        colors = ButtonDefaults.buttonColors(containerColor = ConnectBluePrimary),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        modifier = Modifier.testTag("chat_with_${user.id}_button")
      ) {
        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Chat", style = MaterialTheme.typography.labelMedium)
      }

      // Dropdown Menu for options
      Box {
        IconButton(onClick = { showMenu = true }) {
          Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
          DropdownMenuItem(
            text = { Text("Report User") },
            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = ConnectRedError) },
            onClick = {
              showMenu = false
              onReportClick()
            }
          )
          DropdownMenuItem(
            text = { Text("Block User") },
            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
            onClick = {
              showMenu = false
              onBlockClick()
            }
          )
        }
      }
    }
  }
}
