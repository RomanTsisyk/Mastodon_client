package io.github.romantsisyk.mastodon.ui.views

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.colors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {},
    onClearSearch: () -> Unit = {},
    placeholderText: String = "Search timeline...",
    isOffline: Boolean = false
) {
    var isActive by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val effectivePlaceholder = if (isOffline) "Search cached items..." else placeholderText

    SearchBarDefaults.inputFieldColors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = {
                    if (query.isNotEmpty()) {
                        onSearch(query)
                        isActive = false
                    }
                },
                placeholder = {
                    Text(
                        text = effectivePlaceholder,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isOffline)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isOffline)
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = onClearSearch,
                            content = {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                },
                interactionSource = interactionSource,
                expanded = false,
                onExpandedChange = {}
            )
        },
        expanded = isActive,
        onExpandedChange = { isActive = it },
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = colors(
            containerColor = MaterialTheme.colorScheme.surface,
            dividerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
        ),
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        windowInsets = SearchBarDefaults.windowInsets
    ) { }
}