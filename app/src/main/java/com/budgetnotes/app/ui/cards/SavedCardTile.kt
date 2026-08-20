package com.budgetnotes.app.ui.cards

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budgetnotes.app.data.CardImageStore
import com.budgetnotes.app.data.CardType
import com.budgetnotes.app.data.SavedCard
import com.budgetnotes.app.ocr.PaymentCardParser
import com.budgetnotes.app.ui.theme.RedNegative
import com.budgetnotes.app.util.CardExpiry
import com.budgetnotes.app.util.ExpiryStatus

private val ExpiringSoonAmber = Color(0xFFE65100)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedCardTile(
    card: SavedCard,
    imageStore: CardImageStore,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val expiryStatus = CardExpiry.statusFor(card)
    val chip = CardExpiry.chipLabel(expiryStatus)
    val borderColor = when (expiryStatus) {
        ExpiryStatus.EXPIRED -> RedNegative
        ExpiryStatus.EXPIRING_SOON -> ExpiringSoonAmber
        else -> null
    }

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
    val shape = RoundedCornerShape(12.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (borderColor != null) {
                    Modifier.border(2.dp, borderColor, shape)
                } else {
                    Modifier
                },
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val frontPath = card.frontImagePath
            if (frontPath != null) {
                EncryptedCardImage(
                    relativePath = frontPath,
                    imageStore = imageStore,
                    contentDescription = "Card front",
                    contentScale = ContentScale.Crop,
                    sizePx = thumbPx,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                        .clip(RoundedCornerShape(8.dp)),
                )
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
            if (chip != null && borderColor != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = borderColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.labelMedium,
                        color = borderColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
