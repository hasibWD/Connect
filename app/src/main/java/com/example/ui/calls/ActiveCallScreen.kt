package com.example.ui.calls

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveCallState
import com.example.model.CallType
import com.example.ui.ConnectViewModel
import com.example.ui.common.ConnectAvatar
import com.example.ui.theme.ConnectBluePrimary
import com.example.ui.theme.ConnectNavyDark
import com.example.ui.theme.ConnectRedError

@Composable
fun ActiveCallScreen(
  callState: ActiveCallState,
  viewModel: ConnectViewModel,
  modifier: Modifier = Modifier
) {
  val minutes = callState.durationSeconds / 60
  val seconds = callState.durationSeconds % 60
  val formattedDuration = String.format("%02d:%02d", minutes, seconds)

  val infiniteTransition = rememberInfiniteTransition()
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.18f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(ConnectNavyDark)
      .testTag("active_call_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = 80.dp, bottom = 48.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top Contact Info
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = if (callState.type == CallType.VIDEO) "CONNECT Video Call" else "CONNECT Voice Call",
          style = MaterialTheme.typography.labelLarge,
          color = Color.White.copy(alpha = 0.7f),
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = callState.contactName,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = if (callState.isConnected) formattedDuration else "Connecting...",
          style = MaterialTheme.typography.titleMedium,
          color = Color.White.copy(alpha = 0.9f)
        )
      }

      // Center Animated Avatar
      Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
      ) {
        // Outer pulsing wave
        Box(
          modifier = Modifier
            .size(170.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(ConnectBluePrimary.copy(alpha = 0.25f))
        )

        // Middle wave
        Box(
          modifier = Modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(ConnectBluePrimary.copy(alpha = 0.45f))
        )

        ConnectAvatar(
          avatarUrl = callState.contactAvatar,
          name = callState.contactName,
          size = 110.dp
        )
      }

      // Video Preview Badge if Video Call
      if (callState.type == CallType.VIDEO) {
        Box(
          modifier = Modifier
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
          Text(
            text = if (callState.isVideoEnabled) "📷 HD Camera Active" else "Camera Muted",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
          )
        }
      }

      // Bottom Control Buttons
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Mute Button
          IconButton(
            onClick = { viewModel.toggleMute() },
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(if (callState.isMuted) Color.White else Color.White.copy(alpha = 0.2f))
              .testTag("call_mute_button")
          ) {
            Icon(
              imageVector = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
              contentDescription = "Mute",
              tint = if (callState.isMuted) Color.Black else Color.White
            )
          }

          // Speaker Button
          IconButton(
            onClick = { viewModel.toggleSpeaker() },
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(if (callState.isSpeakerOn) Color.White else Color.White.copy(alpha = 0.2f))
              .testTag("call_speaker_button")
          ) {
            Icon(
              imageVector = if (callState.isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
              contentDescription = "Speaker",
              tint = if (callState.isSpeakerOn) Color.Black else Color.White
            )
          }

          // Video Toggle (if Video Call)
          if (callState.type == CallType.VIDEO) {
            IconButton(
              onClick = { viewModel.toggleVideo() },
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (!callState.isVideoEnabled) Color.White else Color.White.copy(alpha = 0.2f))
                .testTag("call_video_toggle_button")
            ) {
              Icon(
                imageVector = if (callState.isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                contentDescription = "Video",
                tint = if (!callState.isVideoEnabled) Color.Black else Color.White
              )
            }
          }

          // End Call Button
          IconButton(
            onClick = { viewModel.endCall() },
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(ConnectRedError)
              .testTag("end_call_button")
          ) {
            Icon(
              imageVector = Icons.Default.CallEnd,
              contentDescription = "End Call",
              tint = Color.White,
              modifier = Modifier.size(32.dp)
            )
          }
        }
      }
    }
  }
}
