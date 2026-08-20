package com.budgetnotes.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_cards")
data class SavedCard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: CardType,
    val label: String = "",
    // Payment fields
    val cardholderName: String = "",
    val cardNumber: String = "",
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val cvv: String = "",
    val brand: String = "",
    // ID fields
    val fullName: String = "",
    val documentNumber: String = "",
    val issuer: String = "",
    val expiryDate: String = "",
    // Relative paths under files/cards/{id}/
    val frontImagePath: String? = null,
    val backImagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
