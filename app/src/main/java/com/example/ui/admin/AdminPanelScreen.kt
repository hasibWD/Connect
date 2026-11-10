package com.example.ui.admin

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReportItem
import com.example.ui.ConnectViewModel
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary
import com.example.ui.theme.ConnectGreenOnline
import com.example.ui.theme.ConnectOrangeWarning
import com.example.ui.theme.ConnectRedError

@Composable
fun AdminPanelScreen(
  viewModel: ConnectViewModel,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.setShowAdminPanel(false)
  }

  val context = LocalContext.current
  val users by viewModel.allUsers.collectAsState()
  val chats by viewModel.rawChats.collectAsState()
  val reports by viewModel.reports.collectAsState()

  val activeUsersCount = users.count { it.isOnline }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shadowElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = { viewModel.setShowAdminPanel(false) }) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier = Modifier
            .size(34.dp)
            .background(ConnectBlueLight, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = ConnectBluePrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "CONNECT Admin Panel",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Platform Moderation & Metrics",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Metrics Overview Cards
      item {
        Text("Platform Metrics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MetricCard(
            title = "Total Users",
            count = users.size.toString(),
            icon = Icons.Default.People,
            color = ConnectBluePrimary,
            modifier = Modifier.weight(1f)
          )
          MetricCard(
            title = "Active Now",
            count = activeUsersCount.toString(),
            icon = Icons.Default.CheckCircle,
            color = ConnectGreenOnline,
            modifier = Modifier.weight(1f)
          )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MetricCard(
            title = "Total Chats",
            count = chats.size.toString(),
            icon = Icons.AutoMirrored.Filled.Message,
            color = Color(0xFF8B5CF6),
            modifier = Modifier.weight(1f)
          )
          MetricCard(
            title = "User Reports",
            count = reports.count { it.status == "Pending" }.toString(),
            icon = Icons.Default.ReportProblem,
            color = ConnectRedError,
            modifier = Modifier.weight(1f)
          )
        }
      }

      // Moderation Reports Section
      item {
        Text("User Moderation Reports", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      }

      if (reports.isEmpty()) {
        item {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("No pending moderation reports", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      } else {
        items(reports, key = { it.id }) { report ->
          ReportAdminCard(
            report = report,
            onAction = { action ->
              viewModel.resolveReport(report.id, action)
              Toast.makeText(context, "Report marked as: $action", Toast.LENGTH_SHORT).show()
            }
          )
        }
      }
    }
  }
}

@Composable
fun MetricCard(
  title: String,
  count: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  color: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(count, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
    }
  }
}

@Composable
fun ReportAdminCard(
  report: ReportItem,
  onAction: (String) -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Reported: ${report.reportedUserName}",
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleMedium
        )
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = when (report.status) {
            "Pending" -> ConnectOrangeWarning.copy(alpha = 0.15f)
            "Suspended", "Banned" -> ConnectRedError.copy(alpha = 0.15f)
            else -> ConnectGreenOnline.copy(alpha = 0.15f)
          }
        ) {
          Text(
            text = report.status,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when (report.status) {
              "Pending" -> ConnectOrangeWarning
              "Suspended", "Banned" -> ConnectRedError
              else -> ConnectGreenOnline
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Reason: ${report.reason}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = ConnectRedError
      )

      if (report.details.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = report.details,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = { onAction("Dismissed") },
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Dismiss")
        }

        Button(
          onClick = { onAction("Suspended") },
          colors = ButtonDefaults.buttonColors(containerColor = ConnectRedError),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Suspend User")
        }
      }
    }
  }
}
