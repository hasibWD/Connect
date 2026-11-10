package com.example.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.InsForgeConfig
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary
import com.example.ui.theme.ConnectGreenOnline
import com.example.ui.theme.ConnectOrangeWarning
import com.example.ui.theme.ConnectRedError

@Composable
fun ConnectAvatar(
  avatarUrl: String?,
  name: String,
  size: Dp = 48.dp,
  isOnline: Boolean = false,
  showOnlineBadge: Boolean = false,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center
  ) {
    if (!avatarUrl.isNullOrBlank()) {
      AsyncImage(
        model = avatarUrl,
        contentDescription = "$name's avatar",
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .size(size)
          .clip(CircleShape)
      )
    } else {
      val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "C"
      Box(
        modifier = Modifier
          .size(size)
          .clip(CircleShape)
          .background(ConnectBlueLight),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = initial,
          color = ConnectBluePrimary,
          fontWeight = FontWeight.Bold,
          fontSize = (size.value * 0.42f).sp
        )
      }
    }

    if (showOnlineBadge && isOnline) {
      Box(
        modifier = Modifier
          .size(size * 0.28f)
          .align(Alignment.BottomEnd)
          .clip(CircleShape)
          .background(ConnectGreenOnline)
          .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
      )
    }
  }
}

@Composable
fun InsForgeStatusBadge(
  config: InsForgeConfig,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val (statusColor, statusIcon, label) = when {
    config.isConnected -> Triple(ConnectGreenOnline, Icons.Default.CloudDone, "InsForge Connected")
    config.anonKey.isNotBlank() -> Triple(ConnectOrangeWarning, Icons.Default.Cloud, "InsForge Configured")
    else -> Triple(ConnectRedError, Icons.Default.CloudOff, "Setup InsForge")
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = statusColor.copy(alpha = 0.12f),
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
      .testTag("insforge_status_badge")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(statusColor)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Icon(
        imageVector = statusIcon,
        contentDescription = null,
        tint = statusColor,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = label,
        color = statusColor,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}
