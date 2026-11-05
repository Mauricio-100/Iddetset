package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

fun colorForTag(tagName: String): Color {
    val palette = listOf(
        Color(0xFFFFE3E3), Color(0xFFFFEDD5), Color(0xFFFEF9C3),
        Color(0xFFD9F99D), Color(0xFFBBF7D0), Color(0xFFA7F3D0),
        Color(0xFFBAE6FD), Color(0xFFDDD6FE), Color(0xFFFBCFE8)
    )
    val index = abs(tagName.hashCode()) % palette.size
    return palette[index]
}

@Composable
fun TagChip(tagName: String, onClick: () -> Unit) {
    val tagColor = colorForTag(tagName)
    Text(
        text = "#$tagName",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
        color = Color.Black.copy(alpha = 0.7f),
        modifier = Modifier
            .clip(CircleShape)
            .background(tagColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
