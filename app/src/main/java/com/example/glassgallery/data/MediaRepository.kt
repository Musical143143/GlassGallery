package com.example.glassgallery.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    suspend fun loadMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        (queryCollection(isVideo = false) + queryCollection(isVideo = true))
            .sortedByDescending { it.dateTaken }
    }

    private fun queryCollection(isVideo: Boolean): List<MediaItem> {
        val collection: Uri = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE,
            MediaStore.Video.VideoColumns.DURATION,
        )

        val out = ArrayList<MediaItem>()
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val bucketIdCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val widthCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            // Returns -1 for images, where the column is not part of the projection.
            val durationCol = c.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val taken = c.getLong(takenCol).takeIf { it > 0L } ?: (c.getLong(addedCol) * 1000L)
                out += MediaItem(
                    id = id,
                    isVideo = isVideo,
                    uri = ContentUris.withAppendedId(collection, id),
                    name = c.getString(nameCol).orEmpty(),
                    mimeType = c.getString(mimeCol).orEmpty(),
                    dateTaken = taken,
                    durationMs = if (durationCol >= 0) c.getLong(durationCol) else 0L,
                    bucketId = c.getLong(bucketIdCol),
                    bucketName = c.getString(bucketNameCol) ?: "Unknown",
                    width = c.getInt(widthCol),
                    height = c.getInt(heightCol),
                    sizeBytes = c.getLong(sizeCol),
                )
            }
        }
        return out
    }
}
