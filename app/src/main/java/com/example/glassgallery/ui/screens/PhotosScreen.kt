package com.example.glassgallery.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.glassgallery.data.MediaItem
import com.example.glassgallery.ui.GalleryViewModel
import com.example.glassgallery.ui.MediaFilter
import com.example.glassgallery.ui.components.PhotoGrid
import com.example.glassgallery.ui.glass.GlassSurface
import com.example.glassgallery.ui.glass.GlassTopBar
import com.example.glassgallery.ui.shareMedia

@Composable
fun PhotosScreen(
    vm: GalleryViewModel,
    onOpen: (List<MediaItem>, String) -> Unit,
) {
    val media by vm.media.collectAsStateWithLifecycle()
    val sections by vm.sections.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val contentPadding = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 210.dp,
        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 110.dp,
    )

    Box(Modifier.fillMaxSize()) {
        PhotoGrid(
            sections = sections,
            selection = selection,
            favorites = favorites,
            contentPadding = contentPadding,
            onClick = { item ->
                if (selection.isEmpty()) {
                    onOpen(sections.flatMap { it.items }, item.key)
                } else {
                    vm.toggleSelection(item.key)
                }
            },
            onLongClick = { item -> vm.toggleSelection(item.key) },
        )

        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
            if (selection.isEmpty()) {
                GlassTopBar(
                    title = "Gallery",
                    actions = {
                        Text(
                            text = "${media.size}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    },
                )
                SearchAndFilters(
                    query = query,
                    filter = filter,
                    onQuery = vm::setQuery,
                    onFilter = vm::setFilter,
                )
            } else {
                GlassTopBar(
                    title = "${selection.size} selected",
                    navigationIcon = {
                        IconButton(onClick = vm::clearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = vm::favoriteSelection) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Favorite selected")
                        }
                        IconButton(onClick = { shareMedia(context, media.filter { it.key in selection }) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share selected")
                        }
                    },
                )
            }
        }

        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            sections.isEmpty() -> Text(
                text = "Nothing here yet",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun SearchAndFilters(
    query: String,
    filter: MediaFilter,
    onQuery: (String) -> Unit,
    onFilter: (MediaFilter) -> Unit,
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Search photos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    },
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MediaFilter.entries.forEach { mode ->
                    FilterChip(
                        selected = filter == mode,
                        onClick = { onFilter(mode) },
                        label = { Text(mode.label) },
                    )
                }
            }
        }
    }
}
