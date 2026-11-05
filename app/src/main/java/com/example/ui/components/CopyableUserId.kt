package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Universal copyable ID badge placed right under usernames/bot names.
 * Supports human users, system accounts, and bots.
 * One tap immediately copies the ID to clipboard and shows a toast.
 */
@Composable
fun CopyableUserId(
    id: String,
    modifier: Modifier = Modifier,
    isBot: Boolean = false,
    fontSize: TextUnit = 10.sp,
    iconSize: Dp = 10.dp,
    showPrefix: Boolean = true,
    customLabel: String? = null
) {
    if (id.isBlank()) return
    val context = LocalContext.current

    val displayText = when {
        customLabel != null -> customLabel
        isBot -> "bot_id: ${id.take(12)}"
        showPrefix -> "id: ${id.take(10)}"
        else -> id.take(10)
    }

    val containerBg = if (isBot) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    val contentColor = if (isBot) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerBg)
            .clickable {
                copyIdToClipboard(context, id, isBot)
            }
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("copyable_id_$id"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (isBot) {
            Icon(
                imageVector = Icons.Outlined.SmartToy,
                contentDescription = "Bot",
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
        Text(
            text = displayText,
            fontSize = fontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            letterSpacing = 0.2.sp
        )
        Icon(
            imageVector = Icons.Outlined.ContentCopy,
            contentDescription = "Copier l'identifiant",
            tint = contentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun copyIdToClipboard(context: Context, id: String, isBot: Boolean) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val label = if (isBot) "ID du Bot" else "ID Utilisateur"
        val clip = ClipData.newPlainText(label, id)
        clipboard.setPrimaryClip(clip)
        val msg = if (isBot) "ID du bot copié : $id" else "ID copié : $id"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "ID : $id", Toast.LENGTH_SHORT).show()
    }
}
