package com.example.glassgallery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.glassgallery.data.DaySection
import com.example.glassgallery.data.MediaItem
import com.example.glassgallery.ui.formatDuration

/**
 * Virtualised photo grid. Stable keys and content types let LazyVerticalGrid
 * recycle and reuse cells, which keeps scrolling smooth on long libraries.
 */
@Composable
fun PhotoGrid(
    sections: List<DaySection>,
    selection: Set<String>,
    favorites: Set<String>,
    contentPadding: PaddingValues,
    onClick: (MediaItem) -> Unit,
    onLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        sections.forEach { section ->
            item(
                key = "h-${section.id}",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "header",
            ) {
                DayHeader(section.title)
            }
            items(
                items = section.items,
                key = { it.key },
                contentType = { "photo" },
            ) { item ->
                PhotoTile(
                    item = item,
                    selected = item.key in selection,
                    favorite = item.key in favorites,
                    onClick = { onClick(item) },
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick(item)
                    },
                )
            }
        }
    }
}

@Composable
private fun DayHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 16.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoTile(
    item: MediaItem,
    selected: Boolean,
    favorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val scale by animateFloatAsState(
        targetValue = if (selected) 0.9f else 1f,
        label = "tileScale",
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .size(320)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        if (selected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            )
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(Color.White, CircleShape),
            )
        }

        if (favorite) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorite",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(16.dp),
            )
        }

        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatDuration(item.durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
