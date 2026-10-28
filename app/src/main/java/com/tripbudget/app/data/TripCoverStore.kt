package com.tripbudget.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/** Copies a picked photo into app storage, and caches the shared hero downloaded from the cloud. */
object TripCoverStore {
    private const val MAX_EDGE = 1600

    fun save(context: Context, source: Uri): String? {
        val bitmap = decode(context, source) ?: return null
        val dir = File(context.filesDir, "trip-covers").apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        return try {
            FileOutputStream(dest).use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)) {
                    dest.delete()
                    return null
                }
            }
            dest.absolutePath
        } catch (_: Exception) {
            dest.delete()
            null
        } finally {
            bitmap.recycle()
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    /** Writes a downloaded hero JPEG keyed by the trip's cloud id. */
    fun saveDownloaded(context: Context, remoteId: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val dir = File(context.filesDir, "trip-covers").apply { mkdirs() }
        val dest = File(dir, "$remoteId.jpg")
        return try {
            dest.writeBytes(bytes)
            dest.absolutePath
        } catch (_: Exception) {
            dest.delete()
            null
        }
    }

    private fun decode(context: Context, source: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        val decoded = resolver.openInputStream(source)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return null
        val rotation = resolver.openInputStream(source)?.use { stream ->
            rotationDegrees(ExifInterface(stream))
        } ?: 0
        if (rotation == 0) return decoded
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (rotated != decoded) decoded.recycle()
        return rotated
    }

    private fun rotationDegrees(exif: ExifInterface): Int {
        return when (
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        var longest = max(width, height)
        while (longest / sample > MAX_EDGE) sample *= 2
        return sample
    }
}
