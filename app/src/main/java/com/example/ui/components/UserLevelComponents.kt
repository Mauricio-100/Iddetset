package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LevelTableItem
import com.example.data.UserLevelResponse

fun getRankColor(levelIndex: Int): Color = when (levelIndex) {
    0 -> Color(0xFF64748B) // Débutant
    1 -> Color(0xFFCD7F32) // Bronze
    2 -> Color(0xFF94A3B8) // Argent
    3 -> Color(0xFFF59E0B) // Or
    4 -> Color(0xFF06B6D4) // Platine
    5 -> Color(0xFF3B82F6) // Diamant
    6 -> Color(0xFF8B5CF6) // Légende
    else -> Color(0xFFF59E0B)
}

/**
 * Compact Pill Badge displaying the user's current rank and level.
 */
@Composable
fun UserLevelBadge(
    level: UserLevelResponse?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val levelIndex = level?.level_index ?: 0
    val levelName = level?.level_name ?: "Débutant"
    val rankColor = getRankColor(levelIndex)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = rankColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, rankColor.copy(alpha = 0.4f)),
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .testTag("user_level_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.MilitaryTech,
                contentDescription = null,
                tint = rankColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "$levelName • Niv. ${levelIndex + 1}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = rankColor
            )
        }
    }
}

/**
 * Full Gamified Level & Progress Card used in personal & public profiles, and drawer.
 */
@Composable
fun UserLevelCard(
    level: UserLevelResponse?,
    onOpenLadder: () -> Unit,
    modifier: Modifier = Modifier,
    titlePrefix: String = "Statut & Niveau",
    subtitle: String = "Score calculé sur les likes et commentaires"
) {
    val levelIndex = level?.level_index ?: 0
    val levelNum = levelIndex + 1
    val rankName = level?.level_name ?: "Débutant"
    val rankColor = getRankColor(levelIndex)
    val score = level?.score ?: 0
    val progress = level?.progress ?: 0f

    Card(
        onClick = onOpenLadder,
        modifier = modifier
            .fillMaxWidth()
            .testTag("user_level_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, rankColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = rankColor.copy(alpha = 0.18f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.MilitaryTech,
                                contentDescription = null,
                                tint = rankColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = rankName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = rankColor,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "Niv. $levelNum",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$score pts d'expérience",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = rankColor
                        )
                    }
                }

                // Palier / Ladder button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Paliers",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score description
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Objective and Next Tier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (level?.next_level_name != null) {
                        "Prochain : ${level.next_level_name} (${level.points_to_next} pts restants)"
                    } else {
                        "Rang maximal atteint !"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (level?.next_level_score != null) {
                        "$score / ${level.next_level_score} pts"
                    } else {
                        "$score pts"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = rankColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = rankColor,
                trackColor = rankColor.copy(alpha = 0.15f)
            )
        }
    }
}

/**
 * Dialog presenting the complete hierarchy of Ranks & Levels fetched from GET /api/levels.
 */
@Composable
fun LevelsLadderDialog(
    currentLevel: UserLevelResponse?,
    table: List<LevelTableItem>,
    onDismiss: () -> Unit
) {
    val displayTable = if (table.isNotEmpty()) table else listOf(
        LevelTableItem("Débutant", 0),
        LevelTableItem("Bronze", 100),
        LevelTableItem("Argent", 500),
        LevelTableItem("Or", 2000),
        LevelTableItem("Platine", 10000),
        LevelTableItem("Diamant", 50000),
        LevelTableItem("Légende", 200000)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rangs & Niveaux IDDET", fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (currentLevel != null) {
                    val userLvlIndex = currentLevel.level_index
                    val rankColor = getRankColor(userLvlIndex)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = rankColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, rankColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Votre Rang Actuel :",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.MilitaryTech,
                                    contentDescription = null,
                                    tint = rankColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Niveau ${currentLevel.level_index + 1} - ${currentLevel.level_name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = rankColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Score : ${currentLevel.score} pts • Progression : ${(currentLevel.progress * 100).toInt()}%" +
                                        (if (currentLevel.next_level_name != null) " • Prochain palier : ${currentLevel.next_level_name} (${currentLevel.points_to_next} pts restants)" else " • Rang Maximum atteint !"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Text(
                    text = "Votre score est calculé automatiquement à partir des likes et des commentaires reçus sur vos publications, de Débutant à Légende !",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(displayTable.size) { idx ->
                        val tier = displayTable[idx]
                        val tierLevel = idx + 1
                        val userLvl = (currentLevel?.level_index ?: 0) + 1
                        val isCurrent = userLvl == tierLevel
                        val isUnlocked = userLvl >= tierLevel
                        val rankColor = getRankColor(idx)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isCurrent -> rankColor.copy(alpha = 0.18f)
                                isUnlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            },
                            border = BorderStroke(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) rankColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MilitaryTech,
                                    contentDescription = null,
                                    tint = rankColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Niv. $tierLevel - ${tier.name}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isCurrent) rankColor else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = rankColor,
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    text = "ACTUEL",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${tier.min_score} points requis",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isUnlocked && !isCurrent) {
                                    Text("✓", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                } else if (!isUnlocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Verrouillé",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", fontWeight = FontWeight.Bold)
            }
        }
    )
}

