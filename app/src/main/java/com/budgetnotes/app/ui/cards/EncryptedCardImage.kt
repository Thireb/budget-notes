package com.budgetnotes.app.ui.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.budgetnotes.app.data.CardImageStore

@Composable
fun EncryptedCardImage(
    relativePath: String?,
    imageStore: CardImageStore,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    sizePx: Int = 512,
) {
    val context = LocalContext.current
    var bytes by remember(relativePath) { mutableStateOf<ByteArray?>(null) }
    var failed by remember(relativePath) { mutableStateOf(false) }

    LaunchedEffect(relativePath, sizePx) {
        failed = false
        bytes = null
        if (relativePath == null) {
            failed = true
            return@LaunchedEffect
        }
        bytes = imageStore.loadDecryptedBytes(relativePath)
        if (bytes == null) failed = true
    }

    when {
        bytes != null -> {
            val request = remember(bytes, sizePx) {
                ImageRequest.Builder(context)
                    .data(bytes)
                    .size(sizePx)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier,
            )
        }
        failed -> Box(modifier = modifier)
        else -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}
