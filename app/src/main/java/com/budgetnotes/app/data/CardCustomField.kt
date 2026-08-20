package com.budgetnotes.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "card_custom_fields",
    foreignKeys = [
        ForeignKey(
            entity = SavedCard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("cardId")],
)
data class CardCustomField(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val name: String,
    val value: String = "",
    val position: Int = 0,
)
