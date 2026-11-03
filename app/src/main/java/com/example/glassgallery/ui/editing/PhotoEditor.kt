package com.example.glassgallery.ui.editing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface

enum class CropAspect(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f),
}

data class EditState(
    val rotationQuarterTurns: Int = 0,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val aspect: CropAspect = CropAspect.ORIGINAL,
    val brightness: Float = 0f, // -1..1
    val contrast: Float = 1f,   // 0..2
    val saturation: Float = 1f, // 0..2
    val warmth: Float = 0f,     // -1..1
) {
    /** Rotation normalised to 0..3 quarter turns. */
    val quarterTurns: Int get() = Math.floorMod(rotationQuarterTurns, 4)
}

/**
 * 4x5 colour matrix (row-major). Offsets use a 0..255 scale to match
 * android.graphics.ColorMatrix and Compose's ColorMatrix.
 */
fun EditState.colorMatrix(offsetScale: Float = 255f): FloatArray {
    val m = FloatArray(20)
    val s = saturation
    val lr = 0.2126f
    val lg = 0.7152f
    val lb = 0.0722f

    m[0] = (1 - s) * lr + s
    m[1] = (1 - s) * lg
    m[2] = (1 - s) * lb
    m[5] = (1 - s) * lr
    m[6] = (1 - s) * lg + s
    m[7] = (1 - s) * lb
    m[10] = (1 - s) * lr
    m[11] = (1 - s) * lg
    m[12] = (1 - s) * lb + s
    m[18] = 1f

    // Contrast pivots around mid-grey; brightness shifts every channel; warmth moves red vs blue.
    val c = contrast
    for (row in 0..2) for (col in 0..2) m[row * 5 + col] *= c
    val shift = ((1 - c) * 0.5f + brightness) * offsetScale
    val warm = warmth * 0.15f * offsetScale
    m[4] = shift + warm
    m[9] = shift
    m[14] = shift - warm
    return m
}

private fun exifRotation(context: Context, uri: Uri): Int =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
    }.getOrDefault(0)

private fun decode(context: Context, uri: Uri, maxSide: Int?): Bitmap {
    val opts = BitmapFactory.Options()
    if (maxSide != null) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longest / (sample * 2) >= maxSide) sample *= 2
        opts.inSampleSize = sample
    }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        ?: error("Cannot open image")
}

/**
 * Applies rotation, flips, centre crop and colour edits. Pass [previewMaxSide]
 * for a fast, downscaled preview; leave it null for the full-size save.
 */
fun renderEdited(context: Context, source: Uri, edits: EditState, previewMaxSide: Int? = null): Bitmap {
    val src = decode(context, source, previewMaxSide)

    val matrix = Matrix().apply {
        postRotate((exifRotation(context, source) + edits.quarterTurns * 90).toFloat())
        if (edits.flipH) postScale(-1f, 1f)
        if (edits.flipV) postScale(1f, -1f)
    }
    val oriented = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)

    val ow = oriented.width
    val oh = oriented.height
    val ratio = edits.aspect.ratio
    val (cw, ch) = if (ratio == null) {
        ow to oh
    } else if (ow.toFloat() / oh > ratio) {
        (oh * ratio).toInt() to oh
    } else {
        ow to (ow / ratio).toInt()
    }
    val cropped = Bitmap.createBitmap(oriented, (ow - cw) / 2, (oh - ch) / 2, cw, ch)

    val result = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix(edits.colorMatrix(255f)))
    }
    Canvas(result).drawBitmap(cropped, 0f, 0f, paint)
    return result
}

/** Saves a JPEG into Pictures/GlassGallery and returns its URI, or null on failure. */
fun saveToGallery(context: Context, bitmap: Bitmap, displayName: String): Uri? {
    val resolver = context.contentResolver
    val isQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (isQ) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GlassGallery")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

    val written = resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) } ?: false
    if (!written) {
        resolver.delete(uri, null, null)
        return null
    }
    if (isQ) {
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    }
    return uri
}
