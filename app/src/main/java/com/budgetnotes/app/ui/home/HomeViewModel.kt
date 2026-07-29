package com.budgetnotes.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budgetnotes.app.repository.BudgetNoteRepository
import com.budgetnotes.app.repository.NoteWithPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: BudgetNoteRepository,
) : ViewModel() {

    val notes: StateFlow<List<NoteWithPreview>> = repository
        .observeHomeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createNote(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createNote()
            onCreated(id)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    class Factory(
        private val repository: BudgetNoteRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
