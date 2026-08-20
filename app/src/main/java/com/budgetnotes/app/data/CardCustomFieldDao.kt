package com.budgetnotes.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CardCustomFieldDao {
    @Query(
        "SELECT * FROM card_custom_fields WHERE cardId = :cardId ORDER BY position ASC, id ASC",
    )
    fun observeForCard(cardId: Long): Flow<List<CardCustomField>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM card_custom_fields WHERE cardId = :cardId")
    suspend fun maxPosition(cardId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(field: CardCustomField): Long

    @Update
    suspend fun update(field: CardCustomField)

    @Delete
    suspend fun delete(field: CardCustomField)

    @Query("SELECT * FROM card_custom_fields WHERE id = :id")
    suspend fun getById(id: Long): CardCustomField?

    @Query(
        "SELECT * FROM card_custom_fields WHERE cardId = :cardId ORDER BY position ASC, id ASC",
    )
    suspend fun getForCardOnce(cardId: Long): List<CardCustomField>
}
