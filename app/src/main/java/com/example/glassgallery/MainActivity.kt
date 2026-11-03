package com.example.glassgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.glassgallery.ui.GlassGalleryApp
import com.example.glassgallery.ui.theme.GlassGalleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GlassGalleryTheme {
                GlassGalleryApp()
            }
        }
    }
}
