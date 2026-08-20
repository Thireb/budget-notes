package com.budgetnotes.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedCardDao {
    @Query("SELECT * FROM saved_cards ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SavedCard>>

    @Query("SELECT * FROM saved_cards ORDER BY updatedAt DESC")
    suspend fun getAllOnce(): List<SavedCard>

    @Query("SELECT * FROM saved_cards WHERE id = :id")
    fun observeById(id: Long): Flow<SavedCard?>

    @Query("SELECT * FROM saved_cards WHERE id = :id")
    suspend fun getById(id: Long): SavedCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(card: SavedCard): Long

    @Update
    suspend fun update(card: SavedCard)

    @Query("DELETE FROM saved_cards WHERE id = :id")
    suspend fun deleteById(id: Long)
}
