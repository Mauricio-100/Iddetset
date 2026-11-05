package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoleBadge(role: String?) {
    if (role == null || role == "member") return

    val (bgColor, contentColor, icon, text) = when (role) {
        "admin" -> listOf(
            Color(0xFFFFD700).copy(alpha = 0.15f), // Gold background
            Color(0xFFB8860B), // Dark gold text/icon
            Icons.Default.Star, // Star/Crown equivalent
            "Admin"
        )
        "moderator" -> listOf(
            Color(0xFF10B981).copy(alpha = 0.15f), // Emerald background
            Color(0xFF059669), // Dark emerald text/icon
            Icons.Default.Shield,
            "Mod"
        )
        else -> return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(bgColor as Color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon as androidx.compose.ui.graphics.vector.ImageVector,
            contentDescription = text as String,
            tint = contentColor as Color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
    }
}
