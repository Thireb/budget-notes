package com.budgetnotes.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.budgetnotes.app.security.VaultCrypto
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Card photos in app-private storage, encrypted at rest with AES-GCM.
 * Never writes to the device gallery / MediaStore.
 */
class CardImageStore(
    private val context: Context,
    private val imageKey: ByteArray,
) {

    fun cardDir(cardId: Long): File {
        return File(context.filesDir, "cards/$cardId").also { it.mkdirs() }
    }

    fun absoluteFile(relativePath: String): File {
        return File(context.filesDir, relativePath)
    }

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
            writeEncryptedJpeg(cardId, side, bitmap, quality)
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    suspend fun finalizeCameraCapture(
        cardId: Long,
        side: ImageSide,
        tempFile: File,
        maxEdgePx: Int = 1600,
        quality: Int = 85,
    ): String = withContext(Dispatchers.IO) {
        val decoded = decodeFileDownsampled(tempFile, maxEdgePx)
        tempFile.delete()
        if (decoded == null) error("Unable to read camera capture")
        try {
            writeEncryptedJpeg(cardId, side, decoded, quality)
        } finally {
            if (!decoded.isRecycled) decoded.recycle()
        }
    }

    fun createCameraTempFile(cardId: Long, side: ImageSide): File {
        val dir = File(context.cacheDir, "card_capture/$cardId").also { it.mkdirs() }
        return File(dir, "${side.fileName}.tmp").also {
            if (it.exists()) it.delete()
            it.createNewFile()
        }
    }

    suspend fun deleteCardImages(cardId: Long) = withContext(Dispatchers.IO) {
        cardDir(cardId).deleteRecursively()
    }

    suspend fun loadBitmap(
        relativePath: String?,
        maxEdgePx: Int = 1280,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val bytes = loadDecryptedBytes(relativePath) ?: return@withContext null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxEdgePx)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    /** Decrypted JPEG bytes for Coil display. */
    suspend fun loadDecryptedBytes(relativePath: String?): ByteArray? = withContext(Dispatchers.IO) {
        if (relativePath.isNullOrBlank()) return@withContext null
        val file = absoluteFile(relativePath)
        if (!file.exists()) return@withContext null
        val blob = file.readBytes()
        try {
            VaultCrypto.decryptAesGcm(imageKey, blob)
        } catch (_: Exception) {
            // Legacy plaintext JPEG from before encryption — migrate in place.
            if (looksLikeJpeg(blob)) {
                val enc = VaultCrypto.encryptAesGcm(imageKey, blob)
                file.writeBytes(enc)
                blob
            } else {
                null
            }
        }
    }

    /** Encrypt any leftover plaintext JPEGs under files/cards/. */
    suspend fun encryptExistingPlaintextImages() = withContext(Dispatchers.IO) {
        val root = File(context.filesDir, "cards")
        if (!root.isDirectory) return@withContext
        root.walkTopDown().filter { it.isFile && it.name.endsWith(".jpg") }.forEach { file ->
            val bytes = file.readBytes()
            if (looksLikeJpeg(bytes)) {
                file.writeBytes(VaultCrypto.encryptAesGcm(imageKey, bytes))
            }
        }
    }

    private fun writeEncryptedJpeg(
        cardId: Long,
        side: ImageSide,
        bitmap: Bitmap,
        quality: Int,
    ): String {
        val relative = relativePath(cardId, side)
        val dest = absoluteFile(relative)
        dest.parentFile?.mkdirs()
        val jpeg = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
        val encrypted = VaultCrypto.encryptAesGcm(imageKey, jpeg)
        FileOutputStream(dest).use { it.write(encrypted) }
        // Remove legacy plaintext sibling if present
        File(dest.parentFile, side.legacyFileName).delete()
        return relative
    }

    private fun looksLikeJpeg(bytes: ByteArray): Boolean =
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()

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

    enum class ImageSide(val fileName: String, val legacyFileName: String) {
        FRONT("front.jpg.enc", "front.jpg"),
        BACK("back.jpg.enc", "back.jpg"),
    }
}
