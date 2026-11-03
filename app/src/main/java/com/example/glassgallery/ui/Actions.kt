package com.example.glassgallery.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.glassgallery.data.MediaItem
import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun shareMedia(context: Context, items: List<MediaItem>) {
    if (items.isEmpty()) return
    val uris: List<Uri> = items.map { it.uri }

    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = items.first().mimeType.ifBlank { "*/*" }
            putExtra(Intent.EXTRA_STREAM, uris.first())
        }
    } else {
        val allImages = items.all { it.mimeType.startsWith("image/") }
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = if (allImages) "image/*" else "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }

    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.clipData = ClipData.newUri(context.contentResolver, "Photos", uris.first()).apply {
        uris.drop(1).forEach { addItem(ClipData.Item(it)) }
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

fun openExternally(context: Context, item: MediaItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(item.uri, item.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to play this video", Toast.LENGTH_SHORT).show()
    }
}

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.1f GB".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.1f MB".format(bytes / (1L shl 20).toDouble())
    else -> "%.0f KB".format(bytes / 1024.0)
}

fun formatDateTime(ms: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
        .format(Date(ms))
