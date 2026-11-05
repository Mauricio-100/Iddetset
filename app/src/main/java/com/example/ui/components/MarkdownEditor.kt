package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Write your actfile here...",
    onPublish: (() -> Unit)? = null
) {
    var isPreviewMode by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    
    // Sync external value to internal state if needed
    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = value)
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Toolbar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        EditorToolbarButton(
                            icon = Icons.Default.Edit,
                            isSelected = !isPreviewMode,
                            onClick = { isPreviewMode = false },
                            label = "Write"
                        )
                        EditorToolbarButton(
                            icon = Icons.Default.Visibility,
                            isSelected = isPreviewMode,
                            onClick = { isPreviewMode = true },
                            label = "Preview"
                        )
                    }
                    
                    if (!isPreviewMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { applyFormatting(textFieldValue, "**", "**") { textFieldValue = it; onValueChange(it.text) } }) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Bold", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { applyFormatting(textFieldValue, "_", "_") { textFieldValue = it; onValueChange(it.text) } }) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { applyFormatting(textFieldValue, "[", "](url)") { textFieldValue = it; onValueChange(it.text) } }) {
                                Icon(Icons.Default.Link, contentDescription = "Link", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { applyFormatting(textFieldValue, "### ", "") { textFieldValue = it; onValueChange(it.text) } }) {
                                Icon(Icons.Default.Title, contentDescription = "Heading", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { applyFormatting(textFieldValue, "```\n", "\n```") { textFieldValue = it; onValueChange(it.text) } }) {
                                Icon(Icons.Default.Code, contentDescription = "Code Block", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Content
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isPreviewMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        if (value.isBlank()) {
                            Text(
                                "Nothing to preview yet.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            MarkdownActfile(content = value)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            onValueChange(it.text)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        placeholder = { Text(placeholder) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
                    )
                }
            }
            
            // Footer (optional)
            if (onPublish != null) {
                Button(
                    onClick = onPublish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Publish Actfile")
                }
            }
        }
    }
}

@Composable
private fun EditorToolbarButton(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

private fun applyFormatting(
    currentValue: TextFieldValue,
    prefix: String,
    suffix: String,
    onUpdate: (TextFieldValue) -> Unit
) {
    val selection = currentValue.selection
    val text = currentValue.text
    
    val selectedText = text.substring(selection.start, selection.end)
    val newText = StringBuilder(text)
        .replace(selection.start, selection.end, "$prefix$selectedText$suffix")
        .toString()
    
    val newSelectionStart = selection.start + prefix.length
    val newSelectionEnd = selection.end + prefix.length
    
    onUpdate(
        currentValue.copy(
            text = newText,
            selection = androidx.compose.ui.text.TextRange(newSelectionStart, newSelectionEnd)
        )
    )
}
