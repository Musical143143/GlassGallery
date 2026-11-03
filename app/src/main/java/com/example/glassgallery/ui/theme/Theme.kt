package com.example.glassgallery.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Base tint used by glass surfaces; light frost in light mode, smoky graphite in dark. */
val LocalGlassTint = staticCompositionLocalOf { Color.White }

private val IosBlue = Color(0xFF0A84FF)

private val LightScheme = lightColorScheme(
    primary = IosBlue,
    secondary = Color(0xFF5E5CE6),
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF64B5FF),
    secondary = Color(0xFF8E8CFF),
    background = Color.Black,
    surface = Color(0xFF1C1C1E),
)

private val GlassShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun GlassGalleryTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkScheme
        else -> LightScheme
    }
    CompositionLocalProvider(
        LocalGlassTint provides (if (dark) Color(0xFF1C1C1E) else Color.White),
    ) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = GlassShapes,
            content = content,
        )
    }
}
