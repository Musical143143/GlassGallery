package com.example.glassgallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.video.VideoFrameDecoder

class GlassGalleryApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
