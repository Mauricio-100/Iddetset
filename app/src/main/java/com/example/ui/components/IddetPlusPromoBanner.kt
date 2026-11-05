package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IddetPlusPromoBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPremium: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "promo_shimmer")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientOffset"
    )

    val backgroundBrush = if (isPremium) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E1B4B),
                Color(0xFF312E81),
                Color(0xFF4338CA)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E1B4B),
                Color(0xFF2E1065)
            )
        )
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFD700),
            Color(0xFFFF8008),
            Color(0xFFFFC837),
            Color(0xFFFFD700)
        )
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp), spotColor = Color(0xFFFF9800)),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
                .border(width = 1.5.dp, brush = borderBrush, shape = RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFFFD700), Color(0xFFFF8F00))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPremium) Icons.Default.Diamond else Icons.Default.AutoAwesome,
                            contentDescription = "Iddet Plus",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "IDDET PLUS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isPremium) Color(0xFF10B981) else Color(0xFFFF9800),
                                contentColor = Color.Black
                            ) {
                                Text(
                                    text = if (isPremium) "VIP ACTIF" else "-50% PROMO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = if (isPremium) Color.White else Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = if (isPremium)
                                "Gérez votre statut, 500 crédits/mois et vos styles de carte"
                            else
                                "Débloquez les styles Neon & Gold, 500 crédits & badge vérifié",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFD700),
                    contentColor = Color.Black,
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isPremium) "VIP" else "2800 FC",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
