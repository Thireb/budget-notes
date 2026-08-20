package com.budgetnotes.app

import com.budgetnotes.app.data.AppDatabase
import com.budgetnotes.app.data.CardImageStore
import com.budgetnotes.app.data.CardsDatabase
import com.budgetnotes.app.ocr.CardOcrHelper
import com.budgetnotes.app.repository.BudgetNoteRepository
import com.budgetnotes.app.repository.CardRepository

class AppContainer(application: BudgetNotesApplication) {
    private val database = AppDatabase.getInstance(application)
    private val cardsDatabase = CardsDatabase.getInstance(application)
    val imageStore = CardImageStore(application)
    /** Shared OCR client — creating TextRecognizer per editor open is expensive. */
    val ocrHelper = CardOcrHelper(application)

    val repository = BudgetNoteRepository(
        noteDao = database.budgetNoteDao(),
        itemDao = database.budgetItemDao(),
    )

    val cardRepository = CardRepository(
        cardDao = cardsDatabase.savedCardDao(),
        fieldDao = cardsDatabase.cardCustomFieldDao(),
        imageStore = imageStore,
    )
}
