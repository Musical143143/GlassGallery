package com.example.glassgallery.ui.screens

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.glassgallery.data.MediaItem
import com.example.glassgallery.ui.GalleryViewModel
import com.example.glassgallery.ui.deleteRequestSender
import com.example.glassgallery.ui.formatBytes
import com.example.glassgallery.ui.formatDateTime
import com.example.glassgallery.ui.formatDuration
import com.example.glassgallery.ui.glass.GlassSurface
import com.example.glassgallery.ui.glass.GlassTopBar
import com.example.glassgallery.ui.openExternally
import com.example.glassgallery.ui.readExif
import com.example.glassgallery.ui.shareMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Zoom and pan for one photo page. Plain state values, so every change
 * redraws straight away. Gestures and buttons share the same clamped transform.
 */
class ZoomState {
    var scale by mutableFloatStateOf(1f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
    var viewport: Size = Size.Zero

    val isZoomed: Boolean get() = scale > 1.01f

    private fun clamp() {
        val maxX = max(0f, viewport.width * (scale - 1f) / 2f)
        val maxY = max(0f, viewport.height * (scale - 1f) / 2f)
        offsetX = offsetX.coerceIn(-maxX, maxX)
        offsetY = offsetY.coerceIn(-maxY, maxY)
    }

    /** Called while fingers are on the screen. */
    fun applyGesture(zoomChange: Float, pan: Offset) {
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offsetX += pan.x
        offsetY += pan.y
        clamp()
    }

    /** Animates to [target], keeping [focus] (relative to the viewport centre) in place. */
    suspend fun animateTo(target: Float, focus: Offset = Offset.Zero) {
        val t = target.coerceIn(1f, 5f)
        val startScale = scale
        val startX = offsetX
        val startY = offsetY
        val endX = -focus.x * (t - 1f)
        val endY = -focus.y * (t - 1f)
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        ) { p, _ ->
            scale = startScale + (t - startScale) * p
            offsetX = startX + (endX - startX) * p
            offsetY = startY + (endY - startY) * p
        }
        clamp()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    vm: GalleryViewModel,
    startKey: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val items = vm.viewerItems
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(items.isEmpty()) {
        if (items.isEmpty()) onBack()
    }
    if (items.isEmpty()) return

    val startPage = remember(startKey) {
        items.indexOfFirst { it.key == startKey }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { items.size })
    val zoomStates = remember { HashMap<String, ZoomState>() }
    var chromeVisible by remember { mutableStateOf(true) }
    var showInfo by remember { mutableStateOf(false) }
    var slideshow by remember { mutableStateOf(false) }
    var pendingDeleteKey by remember { mutableStateOf<String?>(null) }

    val current = items[pagerState.currentPage.coerceIn(0, items.lastIndex)]
    val currentZoom = zoomStates.getOrPut(current.key) { ZoomState() }

    // Advances to the next photo after a pause; restarts the timer on every page change.
    LaunchedEffect(slideshow, pagerState.currentPage) {
        if (slideshow) {
            delay(3500)
            pagerState.animateScrollToPage((pagerState.currentPage + 1) % items.size)
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingDeleteKey?.let { vm.markDeleted(setOf(it)) }
            onBack()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            // Swiping between photos is paused while zoomed, so panning stays put.
            userScrollEnabled = !currentZoom.isZoomed,
            key = { items[it].key },
        ) { page ->
            val item = items[page]
            if (item.isVideo) {
                VideoPage(
                    item = item,
                    onPlay = { openExternally(context, item) },
                    onTap = { chromeVisible = !chromeVisible },
                )
            } else {
                ZoomablePhoto(
                    item = item,
                    zoom = zoomStates.getOrPut(item.key) { ZoomState() },
                    onTap = { chromeVisible = !chromeVisible },
                )
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
        ) {
            val isFavorite = current.key in favorites
            GlassTopBar(
                title = current.name,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleFavorite(current.key) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                        )
                    }
                    IconButton(onClick = { shareMedia(context, listOf(current)) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = formatDateTime(current.dateTaken),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { showInfo = true }) {
                            Icon(Icons.Filled.Info, contentDescription = "Info")
                        }
                        if (!current.isVideo) {
                            IconButton(onClick = { onEdit(current.key) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit")
                            }
                        }
                        IconButton(onClick = { scope.launch { currentZoom.animateTo(currentZoom.scale / 1.6f) } }) {
                            Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom out")
                        }
                        IconButton(onClick = { scope.launch { currentZoom.animateTo(currentZoom.scale * 1.6f) } }) {
                            Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom in")
                        }
                        IconButton(onClick = { slideshow = !slideshow }) {
                            Icon(
                                imageVector = if (slideshow) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (slideshow) "Stop slideshow" else "Start slideshow",
                            )
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            IconButton(onClick = {
                                pendingDeleteKey = current.key
                                deleteRequestSender(context, listOf(current.uri))?.let {
                                    deleteLauncher.launch(IntentSenderRequest.Builder(it).build())
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        ModalBottomSheet(onDismissRequest = { showInfo = false }) {
            InfoSheet(current)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZoomablePhoto(item: MediaItem, zoom: ZoomState, onTap: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        zoom.applyGesture(zoomChange, panChange)
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { zoom.viewport = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(item.key) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tap ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        scope.launch {
                            if (zoom.isZoomed) zoom.animateTo(1f) else zoom.animateTo(2.5f, tap - center)
                        }
                    },
                )
            }
            // Only claim pan gestures while zoomed in, so the pager still swipes at 1x.
            .transformable(state = transformState, canPan = { zoom.isZoomed }),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offsetX
                    translationY = zoom.offsetY
                },
        )
    }
}

@Composable
private fun VideoPage(item: MediaItem, onPlay: () -> Unit, onTap: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(item.key) {
                detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(onClick = onPlay, modifier = Modifier.size(84.dp)) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = "Play video",
                tint = Color.White,
                modifier = Modifier.size(84.dp),
            )
        }
    }
}

@Composable
private fun InfoSheet(item: MediaItem) {
    val context = LocalContext.current
    val exif by produceState<List<Pair<String, String>>>(emptyList(), item.key) {
        value = withContext(Dispatchers.IO) { readExif(context, item.uri) }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        InfoRow("Taken", formatDateTime(item.dateTaken))
        InfoRow("Size", formatBytes(item.sizeBytes))
        InfoRow("Dimensions", "${item.width} × ${item.height}")
        if (item.isVideo) InfoRow("Duration", formatDuration(item.durationMs))
        exif.forEach { (label, value) -> InfoRow(label, value) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp).weight(1f),
        )
    }
}
