package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.Notification
import com.example.ui.IddetViewModel
import com.example.ui.components.VerificationBadge
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: IddetViewModel, navController: NavController) {
    val notifications by viewModel.notifications.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                        Text("Tout lire")
                    }
                }
            )
        }
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Aucune notification",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        viewModel = viewModel,
                        onClick = {
                            viewModel.markNotificationAsRead(notification.id)
                            
                            val route = when (notification.type) {
                                "like", "comment" -> "discussion/${notification.targetId}"
                                "message" -> "chat/${notification.fromUserId}"
                                "follow" -> "profile/${notification.fromUserId}"
                                else -> null
                            }
                            if (route != null) {
                                navController.navigate(route)
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp), 
                        thickness = 0.5.dp, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    viewModel: IddetViewModel,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(notification.createdAt))

    // Load the sender's profile dynamically
    val senderUser by viewModel.getUserProfile(notification.fromUserId).collectAsStateWithLifecycle(initialValue = null)
    val isFollowing by viewModel.isFollowing(notification.fromUserId).collectAsStateWithLifecycle(initialValue = false)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.isRead) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded background icon with category emoji indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                val iconEmoji = when (notification.type) {
                    "like" -> "💖"
                    "comment" -> "💬"
                    "follow" -> "🎉"
                    "message" -> "✉️"
                    else -> "🔔"
                }
                Text(iconEmoji, fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        // Beautiful Facebook/Whatsapp style Mini Profile Card at the bottom of the notification
        Spacer(modifier = Modifier.height(10.dp))
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Click specifically on the card goes to user profile
                    onClick()
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sender Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = senderUser?.avatarUrl ?: notification.fromAvatar
                    if (!avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = avatarUrl?.let { com.example.utils.UrlHelper.fixCloudinaryUrl(it) },
                            contentDescription = "Avatar de ${notification.fromUsername}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = notification.fromUsername.firstOrNull()?.toString()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // User details
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = notification.fromUsername,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        VerificationBadge(userName = notification.fromUsername, isVerified = senderUser?.isVerified == true)
                    }
                    Text(
                        text = senderUser?.bio ?: "Membre actif",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Follow back button
                Button(
                    onClick = {
                        if (isFollowing) {
                            viewModel.unfollowUser(notification.fromUserId)
                        } else {
                            viewModel.followUser(notification.fromUserId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) MaterialTheme.colorScheme.outline.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    if (isFollowing) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Suivi",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Suivi", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Suivre",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Suivre", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
