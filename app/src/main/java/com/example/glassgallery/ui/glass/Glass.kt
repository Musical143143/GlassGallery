package com.example.glassgallery.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.glassgallery.ui.theme.LocalGlassTint

/**
 * Frosted glass panel: a translucent tint with a top-to-bottom sheen, a light
 * rim that catches the edge, and a soft shadow. The look is an approximation
 * of iOS material, built from gradients rather than a live backdrop blur.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable () -> Unit,
) {
    val tint = LocalGlassTint.current
    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.18f),
                spotColor = Color.Black.copy(alpha = 0.22f),
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = 0.78f), tint.copy(alpha = 0.50f)),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0.08f)),
                ),
                shape = shape,
            ),
    ) {
        content()
    }
}

@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationIcon?.invoke()
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (navigationIcon == null) 12.dp else 4.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, content = actions)
            }
        }
    }
}

enum class GlassTab(val label: String, val icon: ImageVector) {
    PHOTOS("Photos", Icons.Filled.PhotoLibrary),
    ALBUMS("Albums", Icons.Filled.PhotoAlbum),
}

@Composable
fun GlassBottomBar(
    selected: GlassTab,
    onSelect: (GlassTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 48.dp, vertical = 12.dp),
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(34.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassTab.entries.forEach { tab ->
                    TabItem(
                        tab = tab,
                        selected = tab == selected,
                        onClick = { onSelect(tab) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(tab: GlassTab, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
        label = "tabBackground",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        label = "tabForeground",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(28.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(tab.icon, contentDescription = tab.label, tint = foreground)
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = foreground)
    }
}
