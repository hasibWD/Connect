package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.calls.CallsScreen
import com.example.ui.chats.ChatListScreen
import com.example.ui.chats.GroupCreateDialog
import com.example.ui.contacts.ContactsScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.settings.InsForgeSetupDialog
import com.example.ui.stories.StoriesScreen
import com.example.ui.theme.ConnectBluePrimary

data class NavTabItem(
  val tab: MainTab,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
)

@Composable
fun MainScreen(
  viewModel: ConnectViewModel,
  modifier: Modifier = Modifier
) {
  val currentTab by viewModel.currentTab.collectAsState()
  val chats by viewModel.rawChats.collectAsState()
  val showInsForgeDialog by viewModel.showInsForgeDialog.collectAsState()
  val showCreateGroupDialog by viewModel.showCreateGroupDialog.collectAsState()

  val totalUnread = chats.sumOf { it.unreadCount }

  val navItems = listOf(
    NavTabItem(MainTab.CHATS, "Chats", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
    NavTabItem(MainTab.CONTACTS, "Contacts", Icons.Filled.People, Icons.Outlined.People),
    NavTabItem(MainTab.STORIES, "Stories", Icons.Filled.DonutLarge, Icons.Outlined.DonutLarge),
    NavTabItem(MainTab.CALLS, "Calls", Icons.Filled.Call, Icons.Outlined.Call),
    NavTabItem(MainTab.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
  )

  Scaffold(
    modifier = modifier.fillMaxSize(),
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("main_bottom_nav")
      ) {
        navItems.forEach { item ->
          val isSelected = currentTab == item.tab
          NavigationBarItem(
            selected = isSelected,
            onClick = { viewModel.setTab(item.tab) },
            icon = {
              if (item.tab == MainTab.CHATS && totalUnread > 0) {
                BadgedBox(
                  badge = {
                    Badge(containerColor = ConnectBluePrimary) {
                      Text(
                        text = totalUnread.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                ) {
                  Icon(
                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label
                  )
                }
              } else {
                Icon(
                  imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = item.label
                )
              }
            },
            label = {
              Text(
                text = item.label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = ConnectBluePrimary,
              selectedTextColor = ConnectBluePrimary,
              indicatorColor = ConnectBluePrimary.copy(alpha = 0.12f),
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_${item.label.lowercase()}")
          )
        }
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      when (currentTab) {
        MainTab.CHATS -> ChatListScreen(viewModel = viewModel)
        MainTab.CONTACTS -> ContactsScreen(viewModel = viewModel)
        MainTab.STORIES -> StoriesScreen(viewModel = viewModel)
        MainTab.CALLS -> CallsScreen(viewModel = viewModel)
        MainTab.PROFILE -> ProfileScreen(viewModel = viewModel)
      }
    }
  }

  // InsForge Setup Modal
  if (showInsForgeDialog) {
    InsForgeSetupDialog(
      viewModel = viewModel,
      onDismiss = { viewModel.setShowInsForgeDialog(false) }
    )
  }

  // Create Group Modal
  if (showCreateGroupDialog) {
    GroupCreateDialog(
      viewModel = viewModel,
      onDismiss = { viewModel.setShowCreateGroupDialog(false) }
    )
  }
}
