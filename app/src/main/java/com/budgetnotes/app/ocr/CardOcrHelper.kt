package com.budgetnotes.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CardOcrHelper(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        result.text.orEmpty()
    }

    suspend fun recognizeText(uri: Uri, maxEdgePx: Int = 1280): String = withContext(Dispatchers.IO) {
        val bitmap = loadBitmapDownsampled(uri, maxEdgePx) ?: return@withContext ""
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            result.text.orEmpty()
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    suspend fun parsePaymentFromUri(uri: Uri): ParsedPaymentFields {
        val text = recognizeText(uri)
        return PaymentCardParser.parse(text)
    }

    suspend fun parsePaymentFromBitmap(bitmap: Bitmap): ParsedPaymentFields {
        val text = recognizeText(bitmap)
        return PaymentCardParser.parse(text)
    }

    private fun loadBitmapDownsampled(uri: Uri, maxEdgePx: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            } ?: return null
            var sample = 1
            var w = bounds.outWidth
            var h = bounds.outHeight
            while (max(w, h) / 2 >= maxEdgePx) {
                sample *= 2
                w /= 2
                h /= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, opts)
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Do not close when shared via AppContainer. */
    fun close() {
        recognizer.close()
    }
}
