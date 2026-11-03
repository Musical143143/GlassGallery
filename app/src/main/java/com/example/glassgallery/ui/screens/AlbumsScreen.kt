package com.example.glassgallery.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.glassgallery.data.Album
import com.example.glassgallery.data.MediaItem
import com.example.glassgallery.data.buildDaySections
import com.example.glassgallery.ui.GalleryViewModel
import com.example.glassgallery.ui.components.PhotoGrid
import com.example.glassgallery.ui.glass.GlassTopBar

@Composable
fun AlbumsScreen(
    vm: GalleryViewModel,
    onAlbum: (Long) -> Unit,
) {
    val albums by vm.albums.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 88.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 110.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(albums, key = { it.bucketId }, contentType = { "album" }) { album ->
                AlbumCard(album = album, onClick = { onAlbum(album.bucketId) })
            }
        }

        GlassTopBar(
            title = "Albums",
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
fun AlbumDetailScreen(
    vm: GalleryViewModel,
    bucketId: Long,
    onBack: () -> Unit,
    onOpen: (List<MediaItem>, String) -> Unit,
) {
    val media by vm.media.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()

    val items = remember(media, bucketId) { media.filter { it.bucketId == bucketId } }
    val sections = remember(items) { buildDaySections(items) }

    Box(Modifier.fillMaxSize()) {
        PhotoGrid(
            sections = sections,
            selection = emptySet(),
            favorites = favorites,
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 88.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
            ),
            onClick = { item -> onOpen(items, item.key) },
            onLongClick = {},
        )

        GlassTopBar(
            title = items.firstOrNull()?.bucketName ?: "Album",
            modifier = Modifier.align(Alignment.TopCenter),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(album.cover.uri)
                .size(480)
                .crossfade(true)
                .build(),
            contentDescription = album.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${album.count} items",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
