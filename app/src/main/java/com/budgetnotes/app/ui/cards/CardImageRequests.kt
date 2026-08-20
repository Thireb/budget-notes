package com.budgetnotes.app.ui.cards

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

/** Sized Coil request so list/editor tiles do not decode full camera JPEGs. */
@Composable
fun rememberCardImageRequest(
    file: java.io.File?,
    sizePx: Int,
): ImageRequest? {
    val context = LocalContext.current
    return remember(file?.path, sizePx) {
        if (file == null || !file.exists()) return@remember null
        ImageRequest.Builder(context)
            .data(file)
            .size(sizePx)
            .crossfade(false)
            .build()
    }
}

fun cardImageRequest(context: Context, file: java.io.File, sizePx: Int): ImageRequest {
    return ImageRequest.Builder(context)
        .data(file)
        .size(sizePx)
        .crossfade(false)
        .build()
}
