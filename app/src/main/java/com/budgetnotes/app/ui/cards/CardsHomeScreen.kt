package com.budgetnotes.app.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetnotes.app.data.CardImageStore
import com.budgetnotes.app.data.CardType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsHomeScreen(
    viewModel: CardsHomeViewModel,
    imageStore: CardImageStore,
    onOpenCard: (Long) -> Unit,
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    var showTypeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Saved Cards") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showTypeSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "New card")
            }
        },
    ) { padding ->
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No cards yet",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Tap + to save a payment or ID card.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(cards, key = { it.id }) { card ->
                    SavedCardTile(
                        card = card,
                        imageStore = imageStore,
                        onClick = { onOpenCard(card.id) },
                        onLongClick = { pendingDeleteId = card.id },
                    )
                }
            }
        }
    }

    if (showTypeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTypeSheet = false },
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = "Add card",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                TextButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            showTypeSheet = false
                            viewModel.createCard(CardType.PAYMENT, onOpenCard)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null)
                    Text(
                        text = "  Payment card (credit / debit)",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            showTypeSheet = false
                            viewModel.createCard(CardType.ID, onOpenCard)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null)
                    Text(
                        text = "  Identification card",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }

    pendingDeleteId?.let { cardId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete card?") },
            text = { Text("This permanently deletes the card, photos, and custom fields.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCard(cardId)
                        pendingDeleteId = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}
