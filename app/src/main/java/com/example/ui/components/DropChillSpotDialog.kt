package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DropChillSpotDialog(
    userLat: Double,
    userLng: Double,
    onConfirm: (title: String, category: String, emoji: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Café & Co-Working") }
    var selectedEmoji by remember { mutableStateOf("☕") }

    val categories = listOf(
        Pair("Café & Co-Working", "☕"),
        Pair("Détente & Plein Air", "🌿"),
        Pair("Gaming & Arcade", "🎮"),
        Pair("Musique & Lofi Lounge", "🎧"),
        Pair("Terrasse & Vue", "🌅")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Créer un Chill Spot 📍",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Posé sur votre position GPS actuelle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nom du spot") },
                    placeholder = { Text("Ex: Terrasse Café Lofi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector chips
                Text(
                    text = "Type d'ambiance",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { (cat, emoji) ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                selectedCategory = cat
                                selectedEmoji = emoji
                            }
                        ) {
                            Text(
                                text = "$emoji $cat",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Vibe") },
                    placeholder = { Text("Spot calme avec prises et bonne musique...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, selectedCategory, selectedEmoji, description.ifBlank { "Lieu de rencontre et détente IDDET" })
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Poser le spot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
