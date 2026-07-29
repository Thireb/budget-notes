package com.budgetnotes.app.ui.editor

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetnotes.app.data.BudgetItemType
import com.budgetnotes.app.ui.components.SectionDivider
import com.budgetnotes.app.ui.theme.GreenPositive
import com.budgetnotes.app.ui.theme.NeutralZero
import com.budgetnotes.app.ui.theme.RedNegative
import com.budgetnotes.app.ui.theme.SectionMuted
import com.budgetnotes.app.util.MoneyFormat
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    var addingType by remember { mutableStateOf<BudgetItemType?>(null) }
    var titleFocused by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is EditorEvent.ItemDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Item deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.item)
                    }
                }
                EditorEvent.NoteDeleted -> onBack()
            }
        }
    }

    val totalColor = when {
        state.totalMinor > 0 -> GreenPositive
        state.totalMinor < 0 -> RedNegative
        else -> NeutralZero
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    BasicTextField(
                        value = state.titleDraft,
                        onValueChange = viewModel::onTitleChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { titleFocused = it.isFocused },
                        decorationBox = { inner ->
                            if (state.titleDraft.isEmpty() && !titleFocused) {
                                Text(
                                    text = "Title",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete note") },
                            onClick = {
                                menuExpanded = false
                                viewModel.deleteNote()
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Text(
                    text = MoneyFormat.formatMinor(state.totalMinor),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = totalColor,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item { SectionDivider() }

            item {
                Text(
                    text = "ADD",
                    style = MaterialTheme.typography.labelSmall,
                    color = SectionMuted,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
            }

            items(state.addItems, key = { it.id }) { item ->
                BudgetItemRow(
                    item = item,
                    onToggleChecked = { viewModel.toggleChecked(item) },
                    onEditConfirm = { amount, description ->
                        viewModel.updateItem(item, amount, description)
                    },
                    onDelete = { viewModel.deleteItem(item) },
                )
            }

            item {
                when (addingType) {
                    BudgetItemType.ADD -> AddItemInput(
                        onConfirm = { amount, description ->
                            val ok = viewModel.addItem(BudgetItemType.ADD, amount, description)
                            if (ok) addingType = null
                            ok
                        },
                        onCancel = { addingType = null },
                    )
                    else -> TextButton(onClick = { addingType = BudgetItemType.ADD }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add item", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            item { SectionDivider() }

            item {
                Text(
                    text = "DEDUCT",
                    style = MaterialTheme.typography.labelSmall,
                    color = SectionMuted,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
            }

            items(state.deductItems, key = { it.id }) { item ->
                BudgetItemRow(
                    item = item,
                    onToggleChecked = { viewModel.toggleChecked(item) },
                    onEditConfirm = { amount, description ->
                        viewModel.updateItem(item, amount, description)
                    },
                    onDelete = { viewModel.deleteItem(item) },
                )
            }

            item {
                when (addingType) {
                    BudgetItemType.DEDUCT -> AddItemInput(
                        onConfirm = { amount, description ->
                            val ok = viewModel.addItem(BudgetItemType.DEDUCT, amount, description)
                            if (ok) addingType = null
                            ok
                        },
                        onCancel = { addingType = null },
                    )
                    else -> TextButton(onClick = { addingType = BudgetItemType.DEDUCT }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Add item", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }
    }
}
