package com.budgetnotes.app.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budgetnotes.app.data.CardType
import com.budgetnotes.app.data.SavedCard
import com.budgetnotes.app.repository.CardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CardsHomeViewModel(
    private val repository: CardRepository,
) : ViewModel() {

    val cards: StateFlow<List<SavedCard>> = repository
        .observeCards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createCard(type: CardType, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createCard(type)
            onCreated(id)
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    class Factory(
        private val repository: CardRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CardsHomeViewModel(repository) as T
        }
    }
}
