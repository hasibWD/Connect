package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.ConnectViewModel
import com.example.ui.common.InsForgeStatusBadge
import com.example.ui.theme.ConnectBlueLight
import com.example.ui.theme.ConnectBluePrimary

@Composable
fun AuthContainer(
  viewModel: ConnectViewModel,
  modifier: Modifier = Modifier
) {
  var isSignUp by remember { mutableStateOf(false) }
  var showForgotDialog by remember { mutableStateOf(false) }
  var showGoogleNoticeDialog by remember { mutableStateOf(false) }

  val authError by viewModel.authError.collectAsState()
  val authSuccess by viewModel.authSuccessMessage.collectAsState()
  val authLoading by viewModel.authLoading.collectAsState()
  val insForgeConfig by viewModel.insForgeConfig.collectAsState()

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Top Status Badge
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 48.dp, start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.End
      ) {
        InsForgeStatusBadge(
          config = insForgeConfig,
          onClick = { viewModel.setShowInsForgeDialog(true) }
        )
      }

      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp)
          .padding(top = 80.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // App Logo & Header
        Box(
          modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(ConnectBluePrimary)
            .padding(10.dp),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_connect_logo),
            contentDescription = "CONNECT Logo",
            modifier = Modifier.size(60.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "CONNECT",
          style = MaterialTheme.typography.displayLarge,
          color = ConnectBluePrimary,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 1.sp
        )

        Text(
          text = "Connect people, everywhere.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Error / Success Banners
        AnimatedVisibility(visible = authError != null) {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
          ) {
            Text(
              text = authError ?: "",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        AnimatedVisibility(visible = authSuccess != null) {
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp)
          ) {
            Text(
              text = authSuccess ?: "",
              color = Color(0xFF10B981),
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        if (isSignUp) {
          SignUpContent(
            isLoading = authLoading,
            onSignUp = { name, username, email, pass, confirm, avatar ->
              viewModel.signUp(name, username, email, pass, confirm, avatar)
            },
            onSwitchToLogin = {
              viewModel.clearAuthMessages()
              isSignUp = false
            },
            onGoogleClick = { showGoogleNoticeDialog = true }
          )
        } else {
          LoginContent(
            isLoading = authLoading,
            onLogin = { email, pass ->
              viewModel.login(email, pass)
            },
            onSwitchToSignUp = {
              viewModel.clearAuthMessages()
              isSignUp = true
            },
            onForgotPassword = { showForgotDialog = true },
            onGoogleClick = { showGoogleNoticeDialog = true }
          )
        }
      }
    }
  }

  // Forgot Password Dialog
  if (showForgotDialog) {
    ForgotPasswordDialog(
      onDismiss = { showForgotDialog = false },
      onSubmit = { email ->
        viewModel.recoverPassword(email)
        showForgotDialog = false
      }
    )
  }

  // Google OAuth Informational Dialog
  if (showGoogleNoticeDialog) {
    AlertDialog(
      onDismissRequest = { showGoogleNoticeDialog = false },
      title = { Text("InsForge Google OAuth Setup") },
      text = {
        Text(
          "To enable Google Sign-In with your InsForge backend:\n\n" +
            "1. Open your InsForge Dashboard -> Authentication -> Providers.\n" +
            "2. Enable Google Provider and enter your Google OAuth Client ID and Secret.\n" +
            "3. Add your InsForge callback URL to the authorized redirect URIs in Google Cloud Console.\n\n" +
            "For now, you can sign in directly with email & password."
        )
      },
      confirmButton = {
        Button(onClick = { showGoogleNoticeDialog = false }) {
          Text("Understood")
        }
      }
    )
  }
}

@Composable
fun LoginContent(
  isLoading: Boolean,
  onLogin: (String, String) -> Unit,
  onSwitchToSignUp: () -> Unit,
  onForgotPassword: () -> Unit,
  onGoogleClick: () -> Unit
) {
  var email by remember { mutableStateOf("alex@connect.app") }
  var password by remember { mutableStateOf("password123") }
  var showPassword by remember { mutableStateOf(false) }
  var rememberMe by remember { mutableStateOf(true) }

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      Text(
        text = "Welcome Back",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Sign in to continue connecting",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(20.dp))

      OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email Address") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ConnectBluePrimary) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_email_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ConnectBluePrimary) },
        trailingIcon = {
          IconButton(onClick = { showPassword = !showPassword }) {
            Icon(
              imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
              contentDescription = if (showPassword) "Hide password" else "Show password"
            )
          }
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("login_password_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = rememberMe,
            onCheckedChange = { rememberMe = it },
            colors = CheckboxDefaults.colors(checkedColor = ConnectBluePrimary)
          )
          Text(
            text = "Remember me",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        TextButton(onClick = onForgotPassword) {
          Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.bodySmall,
            color = ConnectBluePrimary,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = { onLogin(email, password) },
        enabled = !isLoading,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("login_submit_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ConnectBluePrimary)
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
          Text(
            text = "Login",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedButton(
        onClick = onGoogleClick,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("google_login_button"),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text(
          text = "Continue with Google",
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Don't have an account? ",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Sign Up",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = ConnectBluePrimary,
          modifier = Modifier
            .clickable { onSwitchToSignUp() }
            .testTag("switch_to_signup_button")
        )
      }
    }
  }
}

@Composable
fun SignUpContent(
  isLoading: Boolean,
  onSignUp: (String, String, String, String, String, String) -> Unit,
  onSwitchToLogin: () -> Unit,
  onGoogleClick: () -> Unit
) {
  var fullName by remember { mutableStateOf("") }
  var username by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var agreedToTerms by remember { mutableStateOf(false) }
  var selectedAvatarIndex by remember { mutableStateOf(0) }

  val presetAvatars = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150"
  )

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp)
    ) {
      Text(
        text = "Create Account",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Join CONNECT and start chatting",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Avatar Selector
      Text(
        text = "Select Profile Picture",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
      ) {
        presetAvatars.forEachIndexed { index, url ->
          val isSelected = selectedAvatarIndex == index
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) ConnectBluePrimary else Color.Transparent,
                shape = CircleShape
              )
              .clickable { selectedAvatarIndex = index }
          ) {
            AsyncImage(
              model = url,
              contentDescription = "Avatar $index",
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = fullName,
        onValueChange = { fullName = it },
        label = { Text("Full Name") },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ConnectBluePrimary) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("signup_name_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = username,
        onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase() },
        label = { Text("Unique Username") },
        leadingIcon = { Text("@", style = MaterialTheme.typography.titleMedium, color = ConnectBluePrimary, modifier = Modifier.padding(start = 12.dp)) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("signup_username_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email Address") },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ConnectBluePrimary) },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("signup_email_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password (min 6 chars)") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ConnectBluePrimary) },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("signup_password_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = confirmPassword,
        onValueChange = { confirmPassword = it },
        label = { Text("Confirm Password") },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ConnectBluePrimary) },
        visualTransformation = PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("signup_confirm_password_input"),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Checkbox(
          checked = agreedToTerms,
          onCheckedChange = { agreedToTerms = it },
          colors = CheckboxDefaults.colors(checkedColor = ConnectBluePrimary)
        )
        Text(
          text = "I agree to CONNECT Terms of Service & Privacy Policy",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          onSignUp(
            fullName,
            username,
            email,
            password,
            confirmPassword,
            presetAvatars[selectedAvatarIndex]
          )
        },
        enabled = !isLoading && agreedToTerms,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("signup_submit_button"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = ConnectBluePrimary)
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
          Text(
            text = "Create Account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Already have an account? ",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Login",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = ConnectBluePrimary,
          modifier = Modifier
            .clickable { onSwitchToLogin() }
            .testTag("switch_to_login_button")
        )
      }
    }
  }
}

@Composable
fun ForgotPasswordDialog(
  onDismiss: () -> Unit,
  onSubmit: (String) -> Unit
) {
  var email by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Reset Password", fontWeight = FontWeight.Bold)
    },
    text = {
      Column {
        Text(
          "Enter your registered email address and we'll send you password recovery instructions through InsForge Authentication.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email Address") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmit(email) },
        colors = ButtonDefaults.buttonColors(containerColor = ConnectBluePrimary)
      ) {
        Text("Send Recovery Email")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
