package com.example.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.ConnectViewModel
import com.example.ui.common.ConnectAvatar
import com.example.ui.theme.ConnectBluePrimary

@Composable
fun GroupCreateDialog(
  viewModel: ConnectViewModel,
  onDismiss: () -> Unit
) {
  val allUsers by viewModel.allUsers.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()

  val availableContacts = allUsers.filter { it.id != currentUser?.id && !it.isBlocked }

  var groupName by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  val selectedMembers = remember { mutableStateListOf<String>() }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Create New Group", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = groupName,
          onValueChange = { groupName = it },
          label = { Text("Group Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("group_name_input"),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Group Description") },
          maxLines = 2,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("group_desc_input"),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Select Members (${selectedMembers.size})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
        ) {
          items(availableContacts) { user ->
            val isChecked = selectedMembers.contains(user.id)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (isChecked) selectedMembers.remove(user.id)
                  else selectedMembers.add(user.id)
                }
                .padding(vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Checkbox(
                checked = isChecked,
                onCheckedChange = {
                  if (it) selectedMembers.add(user.id)
                  else selectedMembers.remove(user.id)
                },
                colors = CheckboxDefaults.colors(checkedColor = ConnectBluePrimary)
              )
              ConnectAvatar(
                avatarUrl = user.avatarUrl,
                name = user.name,
                size = 36.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(user.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("@${user.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (groupName.isNotBlank() && selectedMembers.isNotEmpty()) {
            viewModel.createGroup(
              name = groupName,
              description = description,
              avatarUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150",
              memberIds = selectedMembers.toList()
            )
          }
        },
        enabled = groupName.isNotBlank() && selectedMembers.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(containerColor = ConnectBluePrimary),
        modifier = Modifier.testTag("create_group_confirm_button")
      ) {
        Text("Create")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
