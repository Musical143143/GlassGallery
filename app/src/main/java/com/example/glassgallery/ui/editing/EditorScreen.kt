package com.example.glassgallery.ui.editing

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.glassgallery.ui.GalleryViewModel
import com.example.glassgallery.ui.glass.GlassSurface
import com.example.glassgallery.ui.glass.GlassTopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditorTab(val label: String) {
    ADJUST("Adjust"),
    FILTERS("Filters"),
    CROP("Crop"),
    ROTATE("Rotate"),
}

private data class Preset(
    val label: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val warmth: Float,
)

private val presets = listOf(
    Preset("Original", 0f, 1f, 1f, 0f),
    Preset("Vivid", 0.03f, 1.15f, 1.5f, 0f),
    Preset("Warm", 0.02f, 1f, 1.05f, 0.4f),
    Preset("Cool", 0f, 1.05f, 0.95f, -0.4f),
    Preset("Mono", 0f, 1.1f, 0f, 0f),
    Preset("Fade", 0.06f, 0.85f, 0.7f, 0.05f),
)

@Composable
fun EditorScreen(
    vm: GalleryViewModel,
    startKey: String,
    onDone: () -> Unit,
) {
    val media by vm.media.collectAsStateWithLifecycle()
    val item = remember(media, startKey) { media.firstOrNull { it.key == startKey } }
    if (item == null || item.isVideo) {
        LaunchedEffect(Unit) { onDone() }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var edits by remember { mutableStateOf(EditState()) }
    var tab by remember { mutableStateOf(EditorTab.ADJUST) }
    var saving by remember { mutableStateOf(false) }
    var showOriginal by remember { mutableStateOf(false) }

    // Animated so rotations and flips feel physical instead of snapping.
    val angle by animateFloatAsState(
        targetValue = edits.rotationQuarterTurns * 90f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "angle",
    )
    val flipX by animateFloatAsState(if (edits.flipH) -1f else 1f, label = "flipX")
    val flipY by animateFloatAsState(if (edits.flipV) -1f else 1f, label = "flipY")

    val onSave: () -> Unit = {
        if (!saving) {
            saving = true
            scope.launch {
                val uri = withContext(Dispatchers.IO) {
                    runCatching {
                        saveToGallery(
                            context,
                            renderEdited(context, item.uri, edits),
                            "GlassGallery_${System.currentTimeMillis()}.jpg",
                        )
                    }.getOrNull()
                }
                saving = false
                Toast.makeText(
                    context,
                    if (uri != null) "Saved a copy to Pictures/GlassGallery" else "Could not save the edited photo",
                    Toast.LENGTH_SHORT,
                ).show()
                if (uri != null) onDone()
            }
        }
    }

    val ratio = edits.aspect.ratio
    val sideways = Math.floorMod(edits.rotationQuarterTurns, 2) == 1
    val boxRatio = ratio?.let { if (sideways) 1f / it else it }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(item.uri).build(),
            contentDescription = item.name,
            contentScale = if (boxRatio == null) ContentScale.Fit else ContentScale.Crop,
            colorFilter = if (showOriginal) null else ColorFilter.colorMatrix(ColorMatrix(edits.colorMatrix(255f))),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f)
                .then(if (boxRatio != null) Modifier.aspectRatio(boxRatio) else Modifier.fillMaxHeight(0.6f))
                .pointerInput(Unit) {
                    // Press and hold to see the original without edits.
                    detectTapGestures(
                        onPress = {
                            showOriginal = true
                            tryAwaitRelease()
                            showOriginal = false
                        },
                    )
                }
                .graphicsLayer {
                    rotationZ = angle
                    scaleX = flipX
                    scaleY = flipY
                },
        )

        GlassTopBar(
            title = "Edit",
            modifier = Modifier.align(Alignment.TopCenter),
            navigationIcon = {
                IconButton(onClick = onDone) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel")
                }
            },
            actions = {
                if (saving) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = onSave) { Text("Save") }
                }
            },
        )

        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Hold the photo to compare with the original",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EditorTab.entries.forEach { t ->
                        FilterChip(
                            selected = tab == t,
                            onClick = { tab = t },
                            label = { Text(t.label) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                AnimatedContent(
                    targetState = tab,
                    label = "editorTab",
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                ) { current ->
                    when (current) {
                        EditorTab.ADJUST -> AdjustControls(edits) { edits = it }
                        EditorTab.FILTERS -> FilterControls(edits) { edits = it }
                        EditorTab.CROP -> CropControls(edits) { edits = it }
                        EditorTab.ROTATE -> RotateControls(edits) { edits = it }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustControls(edits: EditState, onChange: (EditState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SliderRow("Brightness", edits.brightness, -1f..1f) { onChange(edits.copy(brightness = it)) }
        SliderRow("Contrast", edits.contrast, 0f..2f) { onChange(edits.copy(contrast = it)) }
        SliderRow("Saturation", edits.saturation, 0f..2f) { onChange(edits.copy(saturation = it)) }
        SliderRow("Warmth", edits.warmth, -1f..1f) { onChange(edits.copy(warmth = it)) }
        TextButton(onClick = {
            onChange(edits.copy(brightness = 0f, contrast = 1f, saturation = 1f, warmth = 0f))
        }) {
            Text("Reset adjustments")
        }
    }
}

@Composable
private fun FilterControls(edits: EditState, onChange: (EditState) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { p ->
            val selected = edits.brightness == p.brightness &&
                edits.contrast == p.contrast &&
                edits.saturation == p.saturation &&
                edits.warmth == p.warmth
            FilterChip(
                selected = selected,
                onClick = {
                    onChange(
                        edits.copy(
                            brightness = p.brightness,
                            contrast = p.contrast,
                            saturation = p.saturation,
                            warmth = p.warmth,
                        ),
                    )
                },
                label = { Text(p.label) },
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun CropControls(edits: EditState, onChange: (EditState) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CropAspect.entries.forEach { a ->
            FilterChip(
                selected = edits.aspect == a,
                onClick = { onChange(edits.copy(aspect = a)) },
                label = { Text(a.label) },
            )
        }
    }
}

@Composable
private fun RotateControls(edits: EditState, onChange: (EditState) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(onClick = { onChange(edits.copy(rotationQuarterTurns = edits.rotationQuarterTurns - 1)) }) {
            Icon(Icons.Filled.RotateLeft, contentDescription = "Rotate left")
        }
        IconButton(onClick = { onChange(edits.copy(rotationQuarterTurns = edits.rotationQuarterTurns + 1)) }) {
            Icon(Icons.Filled.RotateRight, contentDescription = "Rotate right")
        }
        IconButton(onClick = { onChange(edits.copy(flipH = !edits.flipH)) }) {
            Icon(Icons.Filled.Flip, contentDescription = "Flip horizontally")
        }
        IconButton(onClick = { onChange(edits.copy(flipV = !edits.flipV)) }) {
            Icon(Icons.Filled.SwapVert, contentDescription = "Flip vertically")
        }
    }
}
