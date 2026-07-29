package com.budgetnotes.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.budgetnotes.app.repository.NoteWithPreview
import com.budgetnotes.app.ui.theme.GreenPositive
import com.budgetnotes.app.ui.theme.NeutralZero
import com.budgetnotes.app.ui.theme.RedNegative
import com.budgetnotes.app.util.MoneyFormat

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    notePreview: NoteWithPreview,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    cardModifier: Modifier = Modifier,
) {
    val title = notePreview.note.title.ifBlank { "Untitled" }
    val totalColor = when {
        notePreview.totalMinor > 0 -> GreenPositive
        notePreview.totalMinor < 0 -> RedNegative
        else -> NeutralZero
    }

    Card(
        modifier = cardModifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = MoneyFormat.formatMinor(notePreview.totalMinor),
                style = MaterialTheme.typography.headlineMedium,
                color = totalColor,
                fontWeight = FontWeight.Bold,
            )
            if (notePreview.previewItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    notePreview.previewItems.forEach { item ->
                        Text(
                            text = "${MoneyFormat.formatMinor(item.amountMinor)} : ${item.description}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (item.isChecked) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            },
                        )
                    }
                }
            }
        }
    }
}
