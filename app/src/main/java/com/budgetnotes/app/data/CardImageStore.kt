package com.budgetnotes.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Stores card front/back JPEGs under app-private [Context.getFilesDir]/cards/{cardId}/.
 * Heavy I/O and decode work runs on [Dispatchers.IO].
 */
class CardImageStore(private val context: Context) {

    fun cardDir(cardId: Long): File {
        return File(context.filesDir, "cards/$cardId").also { it.mkdirs() }
    }

    fun absoluteFile(relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

    /**
     * Decodes [uri], downsamples to [maxEdgePx], writes JPEG, returns path relative to filesDir.
     */
    suspend fun saveFromUri(
        cardId: Long,
        side: ImageSide,
        uri: Uri,
        maxEdgePx: Int = 1600,
        quality: Int = 85,
    ): String = withContext(Dispatchers.IO) {
        val bitmap = decodeUriDownsampled(uri, maxEdgePx)
            ?: error("Unable to read image")
        try {
            writeJpeg(cardId, side, bitmap, quality)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    suspend fun saveBitmap(
        cardId: Long,
        side: ImageSide,
        bitmap: Bitmap,
        quality: Int = 85,
    ): String = withContext(Dispatchers.IO) {
        writeJpeg(cardId, side, bitmap, quality)
    }

    fun createCameraTempFile(cardId: Long, side: ImageSide): File {
        // App cache only — never MediaStore / DCIM / gallery.
        val dir = File(context.cacheDir, "card_capture/$cardId").also { it.mkdirs() }
        return File(dir, "${side.fileName}.tmp").also {
            if (it.exists()) it.delete()
            it.createNewFile()
        }
    }

    suspend fun finalizeCameraCapture(
        cardId: Long,
        side: ImageSide,
        tempFile: File,
        maxEdgePx: Int = 1600,
        quality: Int = 85,
    ): String = withContext(Dispatchers.IO) {
        val relative = relativePath(cardId, side)
        val dest = absoluteFile(relative)
        dest.parentFile?.mkdirs()
        // Re-encode downsampled so gallery/camera full-res shots don't stay huge on disk.
        val decoded = decodeFileDownsampled(tempFile, maxEdgePx)
        if (decoded != null) {
            try {
                FileOutputStream(dest).use { out ->
                    decoded.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
            } finally {
                if (!decoded.isRecycled) decoded.recycle()
            }
            tempFile.delete()
        } else {
            if (dest.exists()) dest.delete()
            if (!tempFile.renameTo(dest)) {
                tempFile.copyTo(dest, overwrite = true)
                tempFile.delete()
            }
        }
        relative
    }

    suspend fun deleteCardImages(cardId: Long) = withContext(Dispatchers.IO) {
        cardDir(cardId).deleteRecursively()
    }

    /** Downsampled decode for OCR / preview; never loads full camera resolution. */
    suspend fun loadBitmap(
        relativePath: String?,
        maxEdgePx: Int = 1280,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (relativePath.isNullOrBlank()) return@withContext null
        val file = absoluteFile(relativePath)
        if (!file.exists()) return@withContext null
        decodeFileDownsampled(file, maxEdgePx)
    }

    private fun writeJpeg(
        cardId: Long,
        side: ImageSide,
        bitmap: Bitmap,
        quality: Int,
    ): String {
        val relative = relativePath(cardId, side)
        val dest = absoluteFile(relative)
        dest.parentFile?.mkdirs()
        FileOutputStream(dest).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return relative
    }

    private fun decodeUriDownsampled(uri: Uri, maxEdgePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        } ?: return null
        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxEdgePx)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, opts)
        }
    }

    private fun decodeFileDownsampled(file: File, maxEdgePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxEdgePx)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdgePx: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        var w = width
        var h = height
        while (max(w, h) / 2 >= maxEdgePx) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample.coerceAtLeast(1)
    }

    private fun relativePath(cardId: Long, side: ImageSide): String {
        return "cards/$cardId/${side.fileName}"
    }

    enum class ImageSide(val fileName: String) {
        FRONT("front.jpg"),
        BACK("back.jpg"),
    }
}
