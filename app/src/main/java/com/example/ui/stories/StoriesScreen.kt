package com.example.ui.stories

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.Story
import com.example.model.StoryType
import com.example.ui.ConnectViewModel
import com.example.ui.common.ConnectAvatar
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary
import com.example.ui.theme.ConnectCyanAccent

@Composable
fun StoriesScreen(
  viewModel: ConnectViewModel,
  modifier: Modifier = Modifier
) {
  val stories by viewModel.stories.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val activeStory by viewModel.activeStory.collectAsState()
  val showCreateDialog by viewModel.showCreateStoryDialog.collectAsState()

  val myStories = stories.filter { it.userId == currentUser?.id }
  val friendStories = stories.filter { it.userId != currentUser?.id }
  val recentStories = friendStories.filter { !it.isViewed }
  val viewedStories = friendStories.filter { it.isViewed }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Header
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Stories & Status",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Status updates vanish automatically after 24 hours",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // My Status Item
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                if (myStories.isNotEmpty()) viewModel.openStory(myStories.first())
                else viewModel.setShowCreateStoryDialog(true)
              }
              .testTag("my_status_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
              ) {
                ConnectAvatar(
                  avatarUrl = currentUser?.avatarUrl,
                  name = currentUser?.name ?: "Me",
                  size = 50.dp
                )
                Box(
                  modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(ConnectBluePrimary)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(14.dp))
                }
              }

              Spacer(modifier = Modifier.width(14.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "My Status",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = if (myStories.isNotEmpty()) "Tap to view updates (${myStories.size})" else "Tap to add story update",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              IconButton(
                onClick = { viewModel.setShowCreateStoryDialog(true) },
                modifier = Modifier
                  .size(40.dp)
                  .background(ConnectBlueLight, CircleShape)
              ) {
                Icon(Icons.Default.Edit, contentDescription = "New Status", tint = ConnectBluePrimary, modifier = Modifier.size(20.dp))
              }
            }
          }
        }

        // Recent Updates
        if (recentStories.isNotEmpty()) {
          item {
            Text(
              text = "Recent Updates",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 8.dp)
            )
          }

          items(recentStories, key = { it.id }) { story ->
            StoryCard(story = story, onClick = { viewModel.openStory(story) })
          }
        }

        // Viewed Updates
        if (viewedStories.isNotEmpty()) {
          item {
            Text(
              text = "Viewed Updates",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 8.dp)
            )
          }

          items(viewedStories, key = { it.id }) { story ->
            StoryCard(story = story, onClick = { viewModel.openStory(story) })
          }
        }
      }
    }

    // Floating Button to Add Story
    FloatingActionButton(
      onClick = { viewModel.setShowCreateStoryDialog(true) },
      containerColor = ConnectBluePrimary,
      contentColor = Color.White,
      shape = CircleShape,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(bottom = 80.dp, end = 20.dp)
        .testTag("add_story_fab")
    ) {
      Icon(Icons.Default.CameraAlt, contentDescription = "Add Status")
    }
  }

  // Fullscreen Story Viewer Dialog
  if (activeStory != null) {
    StoryViewerDialog(
      story = activeStory!!,
      onDismiss = { viewModel.closeStory() }
    )
  }

  // Create Story Dialog
  if (showCreateDialog) {
    CreateStoryDialog(
      onDismiss = { viewModel.setShowCreateStoryDialog(false) },
      onPost = { caption, type, mediaUrl, hex ->
        viewModel.postStory(caption, type, mediaUrl, hex)
      }
    )
  }
}

@Composable
fun StoryCard(
  story: Story,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("story_card_${story.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .border(
            width = 2.5.dp,
            brush = if (story.isViewed) Brush.linearGradient(listOf(Color.Gray, Color.LightGray))
            else Brush.linearGradient(listOf(ConnectBluePrimary, ConnectCyanAccent)),
            shape = CircleShape
          )
          .padding(3.dp),
        contentAlignment = Alignment.Center
      ) {
        ConnectAvatar(
          avatarUrl = story.userAvatar,
          name = story.userName,
          size = 46.dp
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = story.userName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = story.timeAgo,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
fun StoryViewerDialog(
  story: Story,
  onDismiss: () -> Unit
) {
  var progress by remember { mutableFloatStateOf(0f) }
  var isPaused by remember { mutableStateOf(false) }

  // 6 second story display with progress
  LaunchedEffect(isPaused) {
    if (!isPaused) {
      while (progress < 1f) {
        delay(50)
        progress += 0.050f / 6f
      }
      onDismiss()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(story.bgGradientHex))
        .clickable {
          isPaused = !isPaused
        }
        .testTag("story_viewer_fullscreen")
    ) {
      // Photo media if present
      if (story.type == StoryType.IMAGE && story.mediaUrl != null) {
        AsyncImage(
          model = story.mediaUrl,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        // Dark overlay for legibility
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
        )
      }

      // Top Progress & User Header
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 40.dp, start = 16.dp, end = 16.dp)
      ) {
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
          color = Color.White,
          trackColor = Color.White.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ConnectAvatar(
            avatarUrl = story.userAvatar,
            name = story.userName,
            size = 40.dp
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = story.userName,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = story.timeAgo + " • Expires in 24h",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.8f)
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }
      }

      // Center Caption
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = story.caption,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          textAlign = TextAlign.Center,
          lineHeight = 36.sp
        )
      }

      // Bottom Viewers Info
      Row(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 36.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Visibility, contentDescription = "Views", tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "${story.viewerCount} views",
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
fun CreateStoryDialog(
  onDismiss: () -> Unit,
  onPost: (String, StoryType, String?, Long) -> Unit
) {
  var caption by remember { mutableStateOf("") }
  var selectedType by remember { mutableStateOf(StoryType.TEXT) }
  var selectedBgHex by remember { mutableStateOf(0xFF0066FF) }

  val bgColors = listOf(
    0xFF0066FF, // Electric Blue
    0xFF8B5CF6, // Purple
    0xFFEC4899, // Pink
    0xFF10B981, // Emerald
    0xFF0F172A  // Midnight
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add to Story", fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceAround
        ) {
          Button(
            onClick = { selectedType = StoryType.TEXT },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (selectedType == StoryType.TEXT) ConnectBluePrimary else MaterialTheme.colorScheme.surfaceVariant
            )
          ) {
            Text("Text Status", color = if (selectedType == StoryType.TEXT) Color.White else MaterialTheme.colorScheme.onSurface)
          }

          Button(
            onClick = { selectedType = StoryType.IMAGE },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (selectedType == StoryType.IMAGE) ConnectBluePrimary else MaterialTheme.colorScheme.surfaceVariant
            )
          ) {
            Text("Photo Status", color = if (selectedType == StoryType.IMAGE) Color.White else MaterialTheme.colorScheme.onSurface)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedType == StoryType.TEXT) {
          Text("Select Background Color:", style = MaterialTheme.typography.labelSmall)
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            bgColors.forEach { hex ->
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(hex))
                  .border(
                    width = if (selectedBgHex == hex) 3.dp else 1.dp,
                    color = if (selectedBgHex == hex) Color.White else Color.Transparent,
                    shape = CircleShape
                  )
                  .clickable { selectedBgHex = hex }
              )
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
        }

        OutlinedTextField(
          value = caption,
          onValueChange = { caption = it },
          label = { Text(if (selectedType == StoryType.TEXT) "What's on your mind?" else "Add a caption...") },
          maxLines = 4,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_story_input"),
          shape = RoundedCornerShape(12.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (caption.isNotBlank()) {
            val photoUrl = if (selectedType == StoryType.IMAGE) "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600" else null
            onPost(caption, selectedType, photoUrl, selectedBgHex)
          }
        },
        enabled = caption.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = ConnectBluePrimary),
        modifier = Modifier.testTag("post_story_button")
      ) {
        Text("Share Status")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
