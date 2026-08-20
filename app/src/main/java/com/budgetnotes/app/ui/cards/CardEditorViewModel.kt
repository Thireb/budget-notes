package com.budgetnotes.app.ui.cards

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.budgetnotes.app.data.CardCustomField
import com.budgetnotes.app.data.CardImageStore
import com.budgetnotes.app.ocr.CardOcrHelper
import com.budgetnotes.app.ocr.ParsedPaymentFields
import com.budgetnotes.app.ocr.PaymentCardParser
import com.budgetnotes.app.repository.CardRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CardEditorUiState(
    val cardType: com.budgetnotes.app.data.CardType? = null,
    val frontImagePath: String? = null,
    val backImagePath: String? = null,
    val customFields: List<CardCustomField> = emptyList(),
    val labelDraft: String = "",
    val cardholderName: String = "",
    val cardNumber: String = "",
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val cvv: String = "",
    val brand: String = "",
    val fullName: String = "",
    val documentNumber: String = "",
    val issuer: String = "",
    val expiryDate: String = "",
    val revealSecrets: Boolean = false,
    val isOcrRunning: Boolean = false,
    val loaded: Boolean = false,
    val missing: Boolean = false,
)

sealed interface CardEditorEvent {
    data object CardDeleted : CardEditorEvent
    data class Copied(val label: String) : CardEditorEvent
    data class Message(val text: String) : CardEditorEvent
}

class CardEditorViewModel(
    private val cardId: Long,
    private val repository: CardRepository,
    private val imageStore: CardImageStore,
    private val ocrHelper: CardOcrHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardEditorUiState())
    val uiState: StateFlow<CardEditorUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null
    private var customFieldSaveJob: Job? = null

    private val _events = MutableSharedFlow<CardEditorEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    /** Observed separately so field typing does not recompose from Room echo. */
    val customFields: StateFlow<List<CardCustomField>> = repository
        .observeCustomFields(cardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            val card = withContext(Dispatchers.IO) { repository.getCard(cardId) }
            if (card == null) {
                _uiState.value = CardEditorUiState(loaded = true, missing = true)
                return@launch
            }
            _uiState.value = CardEditorUiState(
                cardType = card.type,
                frontImagePath = card.frontImagePath,
                backImagePath = card.backImagePath,
                labelDraft = card.label,
                cardholderName = card.cardholderName,
                cardNumber = card.cardNumber.filter { it.isDigit() },
                expiryMonth = card.expiryMonth,
                expiryYear = card.expiryYear,
                cvv = card.cvv,
                brand = card.brand,
                fullName = card.fullName,
                documentNumber = card.documentNumber,
                issuer = card.issuer,
                expiryDate = card.expiryDate,
                loaded = true,
            )
        }
    }

    private fun edit(transform: (CardEditorUiState) -> CardEditorUiState) {
        _uiState.update(transform)
        scheduleSave()
    }

    fun onLabelChange(value: String) = edit { it.copy(labelDraft = value) }
    fun onCardholderNameChange(value: String) = edit { it.copy(cardholderName = value) }
    fun onCardNumberChange(value: String) = edit {
        val digits = value.filter { it.isDigit() }.take(19)
        it.copy(
            cardNumber = digits,
            brand = PaymentCardParser.detectBrand(digits).ifBlank { it.brand },
        )
    }

    fun onExpiryMonthChange(value: String) = edit {
        it.copy(expiryMonth = value.filter { ch -> ch.isDigit() }.take(2))
    }

    fun onExpiryYearChange(value: String) = edit {
        it.copy(expiryYear = value.filter { ch -> ch.isDigit() }.take(4))
    }

    fun onCvvChange(value: String) = edit {
        it.copy(cvv = value.filter { ch -> ch.isDigit() }.take(4))
    }

    fun onBrandChange(value: String) = edit { it.copy(brand = value) }
    fun onFullNameChange(value: String) = edit { it.copy(fullName = value) }
    fun onDocumentNumberChange(value: String) = edit { it.copy(documentNumber = value) }
    fun onIssuerChange(value: String) = edit { it.copy(issuer = value) }
    fun onExpiryDateChange(value: String) = edit { it.copy(expiryDate = value) }

    fun toggleRevealSecrets() {
        _uiState.update { it.copy(revealSecrets = !it.revealSecrets) }
    }

    fun notifyCopied(label: String, value: String) {
        if (value.isBlank()) {
            _events.tryEmit(CardEditorEvent.Message("Nothing to copy"))
        } else {
            _events.tryEmit(CardEditorEvent.Copied(label))
        }
    }

    fun addCustomField(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { repository.addCustomField(cardId, name) }
    }

    fun updateCustomField(field: CardCustomField, name: String, value: String) {
        customFieldSaveJob?.cancel()
        customFieldSaveJob = viewModelScope.launch {
            delay(300)
            withContext(Dispatchers.IO) {
                repository.updateCustomField(field.copy(name = name, value = value))
            }
        }
    }

    fun deleteCustomField(fieldId: Long) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteCustomField(fieldId) }
    }

    fun deleteCard() {
        viewModelScope.launch {
            saveJob?.cancel()
            withContext(Dispatchers.IO) { repository.deleteCard(cardId) }
            _events.emit(CardEditorEvent.CardDeleted)
        }
    }

    fun prepareCameraFile(side: CardImageStore.ImageSide): File {
        return imageStore.createCameraTempFile(cardId, side)
    }

    fun onImagePicked(side: CardImageStore.ImageSide, uri: Uri, runOcr: Boolean) {
        viewModelScope.launch {
            try {
                val path = imageStore.saveFromUri(cardId, side, uri)
                applyImagePath(side, path)
                if (runOcr) runOcrOnUri(uri, side)
            } catch (_: Exception) {
                _events.emit(CardEditorEvent.Message("Could not save image"))
            }
        }
    }

    fun onCameraCaptured(side: CardImageStore.ImageSide, tempFile: File, runOcr: Boolean) {
        viewModelScope.launch {
            try {
                val path = imageStore.finalizeCameraCapture(cardId, side, tempFile)
                applyImagePath(side, path)
                if (runOcr) {
                    _uiState.update { it.copy(isOcrRunning = true) }
                    val bitmap = imageStore.loadBitmap(path)
                    if (bitmap != null) {
                        try {
                            val parsed = ocrHelper.parsePaymentFromBitmap(bitmap)
                            applyParsedFields(parsed, preferCvv = side == CardImageStore.ImageSide.BACK)
                        } finally {
                            if (!bitmap.isRecycled) bitmap.recycle()
                        }
                    }
                    _uiState.update { it.copy(isOcrRunning = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isOcrRunning = false) }
                _events.emit(CardEditorEvent.Message("Could not save photo"))
            }
        }
    }

    private suspend fun applyImagePath(side: CardImageStore.ImageSide, path: String) {
        withContext(Dispatchers.IO) {
            val existing = repository.getCard(cardId) ?: return@withContext
            val updated = when (side) {
                CardImageStore.ImageSide.FRONT -> existing.copy(frontImagePath = path)
                CardImageStore.ImageSide.BACK -> existing.copy(backImagePath = path)
            }
            repository.updateCard(updated)
        }
        _uiState.update {
            when (side) {
                CardImageStore.ImageSide.FRONT -> it.copy(frontImagePath = path)
                CardImageStore.ImageSide.BACK -> it.copy(backImagePath = path)
            }
        }
    }

    private suspend fun runOcrOnUri(uri: Uri, side: CardImageStore.ImageSide) {
        _uiState.update { it.copy(isOcrRunning = true) }
        try {
            val parsed = ocrHelper.parsePaymentFromUri(uri)
            applyParsedFields(parsed, preferCvv = side == CardImageStore.ImageSide.BACK)
        } catch (_: Exception) {
            _events.emit(CardEditorEvent.Message("OCR failed — enter fields manually"))
        } finally {
            _uiState.update { it.copy(isOcrRunning = false) }
        }
    }

    private fun applyParsedFields(parsed: ParsedPaymentFields, preferCvv: Boolean) {
        _uiState.update { current ->
            current.copy(
                cardNumber = current.cardNumber.ifBlank {
                    parsed.cardNumber.filter { it.isDigit() }
                },
                cardholderName = current.cardholderName.ifBlank { parsed.cardholderName },
                expiryMonth = current.expiryMonth.ifBlank { parsed.expiryMonth },
                expiryYear = current.expiryYear.ifBlank { parsed.expiryYear },
                brand = current.brand.ifBlank { parsed.brand },
                cvv = when {
                    current.cvv.isNotBlank() -> current.cvv
                    preferCvv && parsed.cvv.isNotBlank() -> parsed.cvv
                    parsed.cvv.isNotBlank() -> parsed.cvv
                    else -> current.cvv
                },
                fullName = current.fullName.ifBlank { parsed.cardholderName },
            )
        }
        scheduleSave()
        val filled = listOf(
            parsed.cardNumber.isNotBlank(),
            parsed.expiryMonth.isNotBlank(),
            parsed.cardholderName.isNotBlank(),
            parsed.cvv.isNotBlank(),
        ).count { it }
        _events.tryEmit(
            CardEditorEvent.Message(
                if (filled > 0) "Filled $filled field(s) from scan" else "No card fields detected",
            ),
        )
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(350)
            persist()
        }
    }

    private suspend fun persist() {
        withContext(Dispatchers.IO) {
            val existing = repository.getCard(cardId) ?: return@withContext
            val d = _uiState.value
            repository.updateCard(
                existing.copy(
                    label = d.labelDraft,
                    cardholderName = d.cardholderName,
                    cardNumber = d.cardNumber.filter { it.isDigit() },
                    expiryMonth = d.expiryMonth,
                    expiryYear = d.expiryYear,
                    cvv = d.cvv,
                    brand = d.brand,
                    fullName = d.fullName,
                    documentNumber = d.documentNumber,
                    issuer = d.issuer,
                    expiryDate = d.expiryDate,
                    frontImagePath = d.frontImagePath,
                    backImagePath = d.backImagePath,
                ),
            )
        }
    }

    class Factory(
        private val cardId: Long,
        private val repository: CardRepository,
        private val imageStore: CardImageStore,
        private val ocrHelper: CardOcrHelper,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CardEditorViewModel(
                cardId = cardId,
                repository = repository,
                imageStore = imageStore,
                ocrHelper = ocrHelper,
            ) as T
        }
    }
}
