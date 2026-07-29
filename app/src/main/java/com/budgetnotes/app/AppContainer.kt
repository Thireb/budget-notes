package com.budgetnotes.app

import com.budgetnotes.app.data.AppDatabase
import com.budgetnotes.app.repository.BudgetNoteRepository

class AppContainer(application: BudgetNotesApplication) {
    private val database = AppDatabase.getInstance(application)
    val repository = BudgetNoteRepository(
        noteDao = database.budgetNoteDao(),
        itemDao = database.budgetItemDao(),
    )
}
