package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Distinct, elegant Iddet Plus premium badge.
 */
@Composable
fun IddetPlusBadge(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    showLabel: Boolean = false
) {
    val goldGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFD700), // Rich Gold
            Color(0xFFF59E0B), // Amber 500
            Color(0xFFD97706)  // Amber 600
        )
    )

    if (showLabel) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .background(goldGradient)
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("iddet_plus_badge_full")
                .semantics {
                    contentDescription = "Abonné Iddet Plus"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = "PLUS",
                color = Color.Black,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.6.sp
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(size / 3))
                .background(goldGradient)
                .testTag("iddet_plus_badge_icon")
                .semantics {
                    contentDescription = "Abonné Iddet Plus"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(size * 0.7f)
            )
        }
    }
}
