package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.insforge.InsForgeSchema
import com.example.ui.ConnectViewModel
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary
import com.example.ui.theme.ConnectGreenOnline
import com.example.ui.theme.ConnectOrangeWarning
import com.example.ui.theme.ConnectRedError

@Composable
fun InsForgeSetupDialog(
  viewModel: ConnectViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val config by viewModel.insForgeConfig.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) }
  var inputUrl by remember { mutableStateOf(config.url) }
  var inputAnonKey by remember { mutableStateOf(config.anonKey) }
  var isTesting by remember { mutableStateOf(false) }
  var testResultText by remember { mutableStateOf<String?>(null) }
  var testResultSuccess by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 24.dp)
        .testTag("insforge_setup_dialog"),
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Dialog Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(ConnectBlueLight, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Cloud, contentDescription = null, tint = ConnectBluePrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "InsForge Backend Hub",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "PostgreSQL • Auth • Realtime • Storage",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        // Tabs
        ScrollableTabRow(
          selectedTabIndex = selectedTab,
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = ConnectBluePrimary,
          edgePadding = 16.dp,
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
            text = { Text("Connection", fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Database Schema & RLS", fontWeight = FontWeight.SemiBold) }
          )
          Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("Setup Guide", fontWeight = FontWeight.SemiBold) }
          )
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
          when (selectedTab) {
            0 -> ConnectionTabContent(
              config = config,
              inputUrl = inputUrl,
              onUrlChange = { inputUrl = it },
              inputAnonKey = inputAnonKey,
              onAnonKeyChange = { inputAnonKey = it },
              isTesting = isTesting,
              testResultText = testResultText,
              testResultSuccess = testResultSuccess,
              onTest = {
                isTesting = true
                viewModel.updateInsForge(inputUrl, inputAnonKey) { success, msg ->
                  isTesting = false
                  testResultSuccess = success
                  testResultText = msg
                }
              },
              onSave = {
                viewModel.updateInsForge(inputUrl, inputAnonKey) { success, msg ->
                  Toast.makeText(context, if (success) "Connected to InsForge!" else msg, Toast.LENGTH_LONG).show()
                }
              }
            )

            1 -> SchemaTabContent(
              onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("InsForge SQL Schema", InsForgeSchema.SQL_SCHEMA))
                Toast.makeText(context, "SQL Schema copied to clipboard!", Toast.LENGTH_SHORT).show()
              }
            )

            2 -> GuideTabContent()
          }
        }
      }
    }
  }
}

@Composable
fun ConnectionTabContent(
  config: com.example.model.InsForgeConfig,
  inputUrl: String,
  onUrlChange: (String) -> Unit,
  inputAnonKey: String,
  onAnonKeyChange: (String) -> Unit,
  isTesting: Boolean,
  testResultText: String?,
  testResultSuccess: Boolean,
  onTest: () -> Unit,
  onSave: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(18.dp)
  ) {
    // Current Status Card
    val (statusBg, statusFg, statusLabel) = when {
      config.isConnected -> Triple(ConnectGreenOnline.copy(alpha = 0.12f), ConnectGreenOnline, "InsForge Connected")
      config.anonKey.isNotBlank() -> Triple(ConnectOrangeWarning.copy(alpha = 0.12f), ConnectOrangeWarning, "Configured (Not Verified)")
      else -> Triple(ConnectRedError.copy(alpha = 0.12f), ConnectRedError, "Setup Required")
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = statusBg),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = if (config.isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
          contentDescription = null,
          tint = statusFg,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(statusLabel, fontWeight = FontWeight.Bold, color = statusFg)
          Text(
            text = config.statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
      text = "InsForge Project URL",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = inputUrl,
      onValueChange = onUrlChange,
      placeholder = { Text("https://your-project.insforge.app") },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("insforge_url_input"),
      shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "InsForge Public Anon Key",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = inputAnonKey,
      onValueChange = onAnonKeyChange,
      placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("insforge_anon_key_input"),
      shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Test Results Display
    if (testResultText != null) {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = if (testResultSuccess) ConnectGreenOnline.copy(alpha = 0.1f) else ConnectRedError.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 14.dp)
      ) {
        Text(
          text = testResultText,
          color = if (testResultSuccess) ConnectGreenOnline else ConnectRedError,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(12.dp)
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      OutlinedButton(
        onClick = onTest,
        enabled = !isTesting,
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("insforge_test_button"),
        shape = RoundedCornerShape(12.dp)
      ) {
        if (isTesting) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
          Text("Test Connection")
        }
      }

      Button(
        onClick = onSave,
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("insforge_save_button"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ConnectBluePrimary)
      ) {
        Text("Save & Connect")
      }
    }
  }
}

@Composable
fun SchemaTabContent(
  onCopy: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("PostgreSQL DDL & RLS Policies", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text("Ready to run in InsForge SQL Editor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }

      Button(
        onClick = onCopy,
        colors = ButtonDefaults.buttonColors(containerColor = ConnectBluePrimary),
        shape = RoundedCornerShape(16.dp)
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Copy SQL")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
      color = Color(0xFF0F172A),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(14.dp)
      ) {
        Text(
          text = InsForgeSchema.SQL_SCHEMA,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          color = Color(0xFF38BDF8),
          lineHeight = 16.sp
        )
      }
    }
  }
}

@Composable
fun GuideTabContent() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(18.dp)
  ) {
    Text(
      text = "InsForge Backend Integration Guide",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    GuideStep(
      number = "1",
      title = "Create an InsForge Project",
      description = "Go to your InsForge Dashboard and spin up a new project named 'CONNECT'. InsForge provisions managed PostgreSQL, Auth, Realtime, and S3 Storage."
    )

    GuideStep(
      number = "2",
      title = "Apply Database Schema",
      description = "Navigate to the SQL Editor in InsForge. Switch to the 'Database Schema & RLS' tab in this dialog, copy the SQL, and execute it to create all tables (profiles, chats, messages, stories, calls, etc.) and Row Level Security."
    )

    GuideStep(
      number = "3",
      title = "Create Storage Buckets",
      description = "Under Storage, create the public buckets:\n• 'avatars' (Profile photos)\n• 'chat-media' (Images and attachments)\n• 'voice-messages' (Audio recordings)\n• 'stories' (24-hour status media)"
    )

    GuideStep(
      number = "4",
      title = "Copy API Keys",
      description = "From Project Settings -> API, copy your Project URL and anon public key. Paste them in the Connection tab and click 'Save & Connect'."
    )
  }
}

@Composable
fun GuideStep(number: String, title: String, description: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.Top
  ) {
    Box(
      modifier = Modifier
        .size(28.dp)
        .background(ConnectBlueLight, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text(number, color = ConnectBluePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
      Spacer(modifier = Modifier.height(2.dp))
      Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
