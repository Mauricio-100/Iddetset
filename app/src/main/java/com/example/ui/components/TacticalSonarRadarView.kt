package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.ChillSpot
import com.example.data.FriendLocation
import kotlin.math.*

@Composable
fun TacticalSonarRadarView(
    friends: List<FriendLocation>,
    chillSpots: List<ChillSpot>,
    userLat: Double,
    userLng: Double,
    selectedFriend: FriendLocation?,
    onSelectFriend: (FriendLocation) -> Unit,
    onSelectSpot: (ChillSpot) -> Unit,
    onTriggerScan: () -> Unit,
    onRandomMatch: () -> Unit,
    scanRadiusKm: Double,
    onRadiusChange: (Double) -> Unit,
    serverSpeedKmh: Double = 0.0,
    isServerSynced: Boolean = false,
    trafficJamAlerts: List<com.example.ui.IddetViewModel.TrafficJamAlert> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPinging by remember { mutableStateOf(false) }

    // Rotating Radar Sweep Angle
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    // Pulse expanding wave on ping
    val pulseWave = remember { Animatable(0f) }
    val pulseAlpha = remember { Animatable(0f) }

    LaunchedEffect(isPinging) {
        if (isPinging) {
            pulseWave.snapTo(0f)
            pulseAlpha.snapTo(1f)
            pulseWave.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
            pulseAlpha.animateTo(0f, animationSpec = tween(600))
            isPinging = false
        }
    }

    // Filter friends and spots within the selected radius
    val inRangeFriends = remember(friends, scanRadiusKm) {
        friends.filter { it.distanceKm <= scanRadiusKm }
    }
    val inRangeSpots = remember(chillSpots, scanRadiusKm) {
        chillSpots.filter { it.distanceKm <= scanRadiusKm }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712),
                        Color(0xFF0F172A),
                        Color(0xFF051B1F)
                    )
                )
            )
    ) {
        // Radar Canvas & Grid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 140.dp, top = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = min(size.width, size.height) / 2f * 0.92f

                // Draw background grid lines (Crosshairs)
                drawLine(
                    color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                    start = Offset(center.x - maxRadius, center.y),
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawLine(
                    color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                    start = Offset(center.x, center.y - maxRadius),
                    end = Offset(center.x, center.y + maxRadius),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Diagonal crosshairs
                val diagOffset = maxRadius * 0.7071f
                drawLine(
                    color = Color(0xFF06B6D4).copy(alpha = 0.12f),
                    start = Offset(center.x - diagOffset, center.y - diagOffset),
                    end = Offset(center.x + diagOffset, center.y + diagOffset),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0xFF06B6D4).copy(alpha = 0.12f),
                    start = Offset(center.x - diagOffset, center.y + diagOffset),
                    end = Offset(center.x + diagOffset, center.y - diagOffset),
                    strokeWidth = 1f
                )

                // Concentric distance circles
                val rings = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                rings.forEachIndexed { idx, frac ->
                    val r = maxRadius * frac
                    drawCircle(
                        color = if (idx == rings.lastIndex) Color(0xFF06B6D4).copy(alpha = 0.5f) else Color(0xFF06B6D4).copy(alpha = 0.22f),
                        radius = r,
                        center = center,
                        style = Stroke(width = if (idx == rings.lastIndex) 2f else 1.2f)
                    )
                }

                // Expanding ping wave if triggered
                if (pulseWave.value > 0f) {
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = pulseAlpha.value * 0.7f),
                        radius = maxRadius * pulseWave.value,
                        center = center,
                        style = Stroke(width = 3.5f)
                    )
                }

                // Rotating Sonar Beam with Sweep Gradient
                drawSonarSweep(
                    center = center,
                    radius = maxRadius,
                    angle = sweepAngle,
                    color = Color(0xFF06B6D4)
                )

                // Central User Beacon
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7).copy(alpha = 0.4f), Color.Transparent),
                        center = center,
                        radius = 24f
                    ),
                    radius = 24f,
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = center
                )
            }

            // Interactive Blips Layer (Placed over Canvas for touch precision)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val cX = maxWidth / 2
                val cY = maxHeight / 2
                val maxR = min(maxWidth.value, maxHeight.value) / 2f * 0.92f

                // Render Friends on Radar
                inRangeFriends.forEach { friend ->
                    // Calculate angle & distance ratio
                    val deltaLat = (friend.latitude - userLat)
                    val deltaLng = (friend.longitude - userLng)
                    var angleRad = atan2(deltaLat, deltaLng)
                    if (angleRad.isNaN()) angleRad = 0.0

                    val distRatio = (friend.distanceKm / scanRadiusKm).coerceIn(0.12, 0.95)
                    val blipRadius = (maxR * distRatio).dp

                    val xOffset = cX + (blipRadius.value * cos(angleRad).toFloat()).dp - 20.dp
                    val yOffset = cY - (blipRadius.value * sin(angleRad).toFloat()).dp - 20.dp

                    val isSelected = selectedFriend?.id == friend.id

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .size(40.dp)
                            .clickable { onSelectFriend(friend) },
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulse glow for active/selected friend
                        if (isSelected || friend.isOnline) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.35f)
                                        else Color(0xFF06B6D4).copy(alpha = 0.25f)
                                    )
                            )
                        }

                        // Avatar or icon bubble
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFFF59E0B) else Color(0xFF0F172A),
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (isSelected) Color.White else if (friend.isOnline) Color(0xFF10B981) else Color(0xFF64748B)
                            ),
                            modifier = Modifier.size(30.dp),
                            shadowElevation = 6.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (friend.avatarUrl != null) {
                                    AsyncImage(
                                        model = friend.avatarUrl,
                                        contentDescription = friend.displayName,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(friend.statusEmoji, fontSize = 14.sp)
                                }
                            }
                        }

                        // Mini Online Status indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (friend.isOnline) Color(0xFF10B981) else Color(0xFF64748B))
                                .border(1.dp, Color.Black, CircleShape)
                        )
                    }
                }

                // Render Chill Spots on Radar
                inRangeSpots.forEach { spot ->
                    val deltaLat = (spot.latitude - userLat)
                    val deltaLng = (spot.longitude - userLng)
                    var angleRad = atan2(deltaLat, deltaLng)
                    if (angleRad.isNaN()) angleRad = 0.0

                    val distRatio = (spot.distanceKm / scanRadiusKm).coerceIn(0.15, 0.92)
                    val blipRadius = (maxR * distRatio).dp

                    val xOffset = cX + (blipRadius.value * cos(angleRad).toFloat()).dp - 18.dp
                    val yOffset = cY - (blipRadius.value * sin(angleRad).toFloat()).dp - 18.dp

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .size(36.dp)
                            .clickable { onSelectSpot(spot) },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFC4B5FD)),
                            modifier = Modifier.size(28.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(spot.emoji, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Render Traffic Jam Hazard Blips on Radar
                trafficJamAlerts.forEach { alert ->
                    val deltaLat = (alert.latitude - userLat)
                    val deltaLng = (alert.longitude - userLng)
                    var angleRad = atan2(deltaLat, deltaLng)
                    if (angleRad.isNaN()) angleRad = 0.0

                    val dist = com.example.data.RealLocationProvider.calculateDistanceKm(userLat, userLng, alert.latitude, alert.longitude)
                    val distRatio = (dist / scanRadiusKm).coerceIn(0.2, 0.95)
                    val blipRadius = (maxR * distRatio).dp

                    val xOffset = cX + (blipRadius.value * cos(angleRad).toFloat()).dp - 18.dp
                    val yOffset = cY - (blipRadius.value * sin(angleRad).toFloat()).dp - 18.dp

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .size(36.dp)
                            .clickable {
                                android.widget.Toast.makeText(context, alert.message, android.widget.Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.3f))
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEF4444),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                            modifier = Modifier.size(26.dp),
                            shadowElevation = 6.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🚗", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Top HUD Telemetry Panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isServerSynced) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isServerSynced) "RADAR SYNCHRONISÉ SERVEUR" else "RADAR ACTIF 360°",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = "${inRangeFriends.size} utilisateurs • ${inRangeSpots.size} spots détectés",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Radius Filter Selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1.0, 3.0, 5.0, 10.0).forEach { radius ->
                            val isSelected = scanRadiusKm == radius
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF06B6D4) else Color(0xFF1E293B),
                                modifier = Modifier.clickable { onRadiusChange(radius) }
                            ) {
                                Text(
                                    text = "${radius.toInt()}km",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                // Live Speed & Movement Status Telemetry
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val movementLabel = when {
                        serverSpeedKmh > 25.0 -> "🚗 Véhicule (${String.format(java.util.Locale.US, "%.1f", serverSpeedKmh)} km/h)"
                        serverSpeedKmh > 8.0 -> "🚲 En déplacement (${String.format(java.util.Locale.US, "%.1f", serverSpeedKmh)} km/h)"
                        serverSpeedKmh > 1.0 -> "🚶 Marche (${String.format(java.util.Locale.US, "%.1f", serverSpeedKmh)} km/h)"
                        else -> "📍 Stationnaire (0 km/h)"
                    }
                    Text(
                        text = "Vitesse estimée : $movementLabel",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.SemiBold
                    )

                    if (trafficJamAlerts.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚠️ ${trafficJamAlerts.size} ralenti(s)", fontSize = 11.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Bottom Radar Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Quick Action Buttons (Pinger Radar & Match Chill Aléatoire)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ping Echo Button
                Button(
                    onClick = {
                        isPinging = true
                        onTriggerScan()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("radar_ping_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pinger la zone", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }

                // Random Chill Matcher Button
                Button(
                    onClick = onRandomMatch,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("radar_roulette_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Text("🎲", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Roulette Chill", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// Draw Sonar Sweep Beam via Canvas
private fun DrawScope.drawSonarSweep(
    center: Offset,
    radius: Float,
    angle: Float,
    color: Color
) {
    val sweepRad = Math.toRadians(angle.toDouble()).toFloat()
    val endX = center.x + radius * cos(sweepRad)
    val endY = center.y + radius * sin(sweepRad)

    // Leading line
    drawLine(
        color = color.copy(alpha = 0.9f),
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 2.5f
    )

    // Tail sector gradient
    val path = Path().apply {
        moveTo(center.x, center.y)
        for (i in 0..45 step 3) {
            val tailRad = Math.toRadians((angle - i).toDouble()).toFloat()
            val x = center.x + radius * cos(tailRad)
            val y = center.y + radius * sin(tailRad)
            lineTo(x, y)
        }
        close()
    }

    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.28f),
                color.copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        )
    )
}
