package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.IddetViewModel

import androidx.compose.material.icons.filled.Clear

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersistentSearchBar(
    viewModel: IddetViewModel,
    modifier: Modifier = Modifier,
    placeholder: String = "Search actfiles and users...",
    onSearch: ((String) -> Unit)? = null
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showFilterDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            TextField(
                value = query,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { 
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke(query) }),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            IconButton(onClick = { showFilterDialog = true }) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showFilterDialog) {
        SearchFilterDialog(
            viewModel = viewModel,
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
fun SearchFilterDialog(
    viewModel: IddetViewModel,
    onDismiss: () -> Unit
) {
    val inContent by viewModel.searchInContent.collectAsStateWithLifecycle()
    val inTags by viewModel.searchInTags.collectAsStateWithLifecycle()
    val inUsers by viewModel.searchInUsers.collectAsStateWithLifecycle()
    val sortOrder by viewModel.searchSortOrder.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Advanced Filters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Search in:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                FilterOptionRow(
                    label = "Content Keywords",
                    selected = inContent,
                    onToggle = { viewModel.setSearchOptions(!inContent, inTags, inUsers) }
                )
                FilterOptionRow(
                    label = "Tags",
                    selected = inTags,
                    onToggle = { viewModel.setSearchOptions(inContent, !inTags, inUsers) }
                )
                FilterOptionRow(
                    label = "Usernames",
                    selected = inUsers,
                    onToggle = { viewModel.setSearchOptions(inContent, inTags, !inUsers) }
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    "Sort by:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = sortOrder == "Recent",
                        onClick = { viewModel.setSearchSortOrder("Recent") }
                    )
                    Text("Most Recent", modifier = Modifier.clickable { viewModel.setSearchSortOrder("Recent") })
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    RadioButton(
                        selected = sortOrder == "Popular",
                        onClick = { viewModel.setSearchSortOrder("Popular") }
                    )
                    Text("Most Popular", modifier = Modifier.clickable { viewModel.setSearchSortOrder("Popular") })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun FilterOptionRow(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
