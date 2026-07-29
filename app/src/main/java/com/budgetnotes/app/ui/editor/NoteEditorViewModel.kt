package com.budgetnotes.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budgetnotes.app.data.BudgetItem
import com.budgetnotes.app.data.BudgetItemType
import com.budgetnotes.app.data.BudgetNote
import com.budgetnotes.app.repository.BudgetNoteRepository
import com.budgetnotes.app.util.MoneyFormat
import com.budgetnotes.app.util.Totals
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditorUiState(
    val note: BudgetNote? = null,
    val addItems: List<BudgetItem> = emptyList(),
    val deductItems: List<BudgetItem> = emptyList(),
    val totalMinor: Long = 0L,
    val titleDraft: String = "",
    val isLoading: Boolean = true,
)

sealed interface EditorEvent {
    data class ItemDeleted(val item: BudgetItem) : EditorEvent
    data object NoteDeleted : EditorEvent
}

@OptIn(FlowPreview::class)
class NoteEditorViewModel(
    private val noteId: Long,
    private val repository: BudgetNoteRepository,
) : ViewModel() {

    private val titleDraft = MutableStateFlow<String?>(null)
    private val _events = MutableSharedFlow<EditorEvent>()
    val events = _events.asSharedFlow()

    private val noteAndItems = combine(
        repository.observeNote(noteId),
        repository.observeItems(noteId),
    ) { note, items ->
        note to items
    }

    val uiState: StateFlow<EditorUiState> = combine(noteAndItems, titleDraft) { pair, draft ->
        val (note, items) = pair
        EditorUiState(
            note = note,
            addItems = items.filter { it.type == BudgetItemType.ADD },
            deductItems = items.filter { it.type == BudgetItemType.DEDUCT },
            totalMinor = Totals.compute(items),
            titleDraft = draft ?: note?.title.orEmpty(),
            isLoading = note == null && draft == null,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EditorUiState(),
    )

    init {
        viewModelScope.launch {
            titleDraft
                .filter { it != null }
                .debounce(300)
                .distinctUntilChanged()
                .collect { title ->
                    repository.updateTitle(noteId, title.orEmpty())
                }
        }
    }

    fun onTitleChange(value: String) {
        titleDraft.value = value
    }

    fun toggleChecked(item: BudgetItem) {
        viewModelScope.launch {
            repository.setItemChecked(item.id, !item.isChecked)
        }
    }

    fun addItem(type: BudgetItemType, amountText: String, description: String): Boolean {
        val amount = MoneyFormat.parseToMinor(amountText) ?: return false
        viewModelScope.launch {
            repository.addItem(noteId, amount, description, type)
        }
        return true
    }

    fun updateItem(item: BudgetItem, amountText: String, description: String): Boolean {
        val amount = MoneyFormat.parseToMinor(amountText) ?: return false
        viewModelScope.launch {
            repository.updateItem(
                item.copy(
                    amountMinor = amount,
                    description = description.trim(),
                ),
            )
        }
        return true
    }

    fun deleteItem(item: BudgetItem) {
        viewModelScope.launch {
            val deleted = repository.deleteItem(item.id) ?: return@launch
            _events.emit(EditorEvent.ItemDeleted(deleted))
        }
    }

    fun undoDelete(item: BudgetItem) {
        viewModelScope.launch {
            repository.restoreItem(item)
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            repository.deleteNote(noteId)
            _events.emit(EditorEvent.NoteDeleted)
        }
    }

    class Factory(
        private val noteId: Long,
        private val repository: BudgetNoteRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NoteEditorViewModel(noteId, repository) as T
        }
    }
}
