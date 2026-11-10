package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.User
import com.example.ui.ConnectViewModel
import com.example.ui.common.ConnectAvatar
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary
import com.example.ui.theme.ConnectGreenOnline
import com.example.ui.theme.ConnectRedError

@Composable
fun ProfileScreen(
  viewModel: ConnectViewModel,
  modifier: Modifier = Modifier
) {
  val currentUser by viewModel.currentUser.collectAsState()
  val isDarkMode by viewModel.isDarkMode.collectAsState()
  val insForgeConfig by viewModel.insForgeConfig.collectAsState()
  val chats by viewModel.rawChats.collectAsState()
  val users by viewModel.allUsers.collectAsState()

  var showEditProfileDialog by remember { mutableStateOf(false) }

  val user = currentUser ?: User(
    id = "me",
    name = "Alex Rivera",
    username = "alexrivera",
    email = "alex@connect.app",
    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(bottom = 88.dp)
  ) {
    // Header
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 1.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(contentAlignment = Alignment.BottomEnd) {
          ConnectAvatar(
            avatarUrl = user.avatarUrl,
            name = user.name,
            size = 88.dp,
            isOnline = user.isOnline,
            showOnlineBadge = true
          )
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(ConnectBluePrimary)
              .clickable { showEditProfileDialog = true }
              .padding(4.dp),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Avatar", tint = Color.White, modifier = Modifier.size(16.dp))
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = user.name,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )

        Text(
          text = "@${user.username}",
          style = MaterialTheme.typography.bodyMedium,
          color = ConnectBluePrimary,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = user.bio,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stats Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          StatItem(count = chats.size.toString(), label = "Chats")
          StatItem(count = users.size.toString(), label = "Contacts")
          StatItem(count = "24h", label = "Stories")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
      // Online Status Toggle
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Online Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              if (user.isOnline) "Visible as Online to contacts" else "Invisible (Appear Offline)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = user.isOnline,
            onCheckedChange = { viewModel.toggleOnlineStatus(it) },
            colors = SwitchDefaults.colors(checkedThumbColor = ConnectGreenOnline, checkedTrackColor = ConnectGreenOnline.copy(alpha = 0.5f))
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // InsForge Backend Hub Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { viewModel.setShowInsForgeDialog(true) }
          .testTag("profile_insforge_hub_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .background(ConnectBlueLight, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Cloud, contentDescription = null, tint = ConnectBluePrimary)
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text("InsForge Backend Hub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              text = if (insForgeConfig.isConnected) "Status: Connected to PostgreSQL" else "Configure URL & API Keys",
              style = MaterialTheme.typography.bodySmall,
              color = if (insForgeConfig.isConnected) ConnectGreenOnline else ConnectBluePrimary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Settings List
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
          // Edit Profile
          SettingsRowItem(
            icon = Icons.Default.Edit,
            title = "Edit Profile Info",
            subtitle = "Update display name and bio",
            onClick = { showEditProfileDialog = true }
          )

          HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // Dark Theme Toggle
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { viewModel.toggleDarkMode() }
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.DarkMode, contentDescription = null, tint = ConnectBluePrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text("Dark Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
              Text("Toggle dark & light appearance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = isDarkMode,
              onCheckedChange = { viewModel.toggleDarkMode() },
              colors = SwitchDefaults.colors(checkedThumbColor = ConnectBluePrimary, checkedTrackColor = ConnectBluePrimary.copy(alpha = 0.5f))
            )
          }

          HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // Admin Panel (Inspect / Moderation)
          SettingsRowItem(
            icon = Icons.Default.AdminPanelSettings,
            title = "Admin Panel",
            subtitle = "Platform metrics, user reports & moderation",
            onClick = { viewModel.setShowAdminPanel(true) }
          )

          HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          // Logout
          SettingsRowItem(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = "Log Out",
            subtitle = "Sign out from this device",
            titleColor = ConnectRedError,
            onClick = { viewModel.logout() }
          )
        }
      }
    }
  }

  // Edit Profile Dialog
  if (showEditProfileDialog) {
    var editName by remember { mutableStateOf(user.name) }
    var editBio by remember { mutableStateOf(user.bio) }

    AlertDialog(
      onDismissRequest = { showEditProfileDialog = false },
      title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
      text = {
        Column {
          OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = editBio,
            onValueChange = { editBio = it },
            label = { Text("Bio") },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.updateProfile(editName, editBio, user.avatarUrl)
            showEditProfileDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = ConnectBluePrimary)
        ) {
          Text("Save Changes")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditProfileDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun StatItem(count: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = count,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = ConnectBluePrimary
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun SettingsRowItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  titleColor: Color = Color.Unspecified,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = if (titleColor != Color.Unspecified) titleColor else ConnectBluePrimary, modifier = Modifier.size(24.dp))
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
