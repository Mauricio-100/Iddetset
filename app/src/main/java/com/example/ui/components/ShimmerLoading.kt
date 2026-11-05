package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerActfileCard() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = ""
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(
                        modifier = Modifier
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .fillMaxWidth(0.5f)
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Spacer(
                        modifier = Modifier
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .fillMaxWidth(0.3f)
                            .background(brush)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(
                modifier = Modifier
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .fillMaxWidth(0.9f)
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Spacer(
                modifier = Modifier
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .fillMaxWidth(0.7f)
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Spacer(
                modifier = Modifier
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .fillMaxWidth(0.4f)
                    .background(brush)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(brush)
                    )
                    Spacer(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(brush)
                    )
                    Spacer(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(brush)
                    )
                }
            }
        }
    }
}
