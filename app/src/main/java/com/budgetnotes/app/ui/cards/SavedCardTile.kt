package com.budgetnotes.app.ui.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.budgetnotes.app.data.CardImageStore
import com.budgetnotes.app.data.CardType
import com.budgetnotes.app.data.SavedCard
import com.budgetnotes.app.ocr.PaymentCardParser

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedCardTile(
    card: SavedCard,
    imageStore: CardImageStore,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val subtitle = when (card.type) {
        CardType.PAYMENT -> {
            val masked = PaymentCardParser.maskPan(card.cardNumber)
            listOfNotNull(
                card.brand.takeIf { it.isNotBlank() },
                masked.takeIf { card.cardNumber.isNotBlank() },
            ).joinToString(" · ").ifBlank { "Payment card" }
        }
        CardType.ID -> {
            card.documentNumber.ifBlank { "ID card" }
        }
    }

    val density = LocalDensity.current
    val thumbPx = with(density) { 220.dp.roundToPx() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val frontPath = card.frontImagePath
            if (frontPath != null) {
                val request = rememberCardImageRequest(
                    file = imageStore.absoluteFile(frontPath),
                    sizePx = thumbPx,
                )
                if (request != null) {
                    AsyncImage(
                        model = request,
                        contentDescription = "Card front",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.6f)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (card.type) {
                            CardType.PAYMENT -> Icons.Default.CreditCard
                            CardType.ID -> Icons.Default.Badge
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = card.label.ifBlank {
                    when (card.type) {
                        CardType.PAYMENT -> "Payment card"
                        CardType.ID -> "ID card"
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
