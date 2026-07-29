package com.budgetnotes.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_notes")
data class BudgetNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
