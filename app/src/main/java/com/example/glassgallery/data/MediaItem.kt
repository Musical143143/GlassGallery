package com.example.glassgallery.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val isVideo: Boolean,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val dateTaken: Long,
    val durationMs: Long,
    val bucketId: Long,
    val bucketName: String,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
) {
    /** Stable key: images and videos can share the same MediaStore id. */
    val key: String get() = (if (isVideo) "v" else "i") + id
}

data class Album(
    val bucketId: Long,
    val name: String,
    val cover: MediaItem,
    val count: Int,
)
