package com.budgetnotes.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetItemDao {
    @Query("SELECT * FROM budget_items WHERE noteId = :noteId ORDER BY position ASC, createdAt ASC")
    fun observeForNote(noteId: Long): Flow<List<BudgetItem>>

    @Query("SELECT * FROM budget_items WHERE noteId IN (:noteIds) ORDER BY position ASC, createdAt ASC")
    fun observeForNotes(noteIds: List<Long>): Flow<List<BudgetItem>>

    @Query("SELECT * FROM budget_items WHERE noteId = :noteId ORDER BY position ASC, createdAt ASC")
    suspend fun getForNote(noteId: Long): List<BudgetItem>

    @Query("SELECT COALESCE(MAX(position), -1) FROM budget_items WHERE noteId = :noteId AND type = :type")
    suspend fun maxPosition(noteId: Long, type: BudgetItemType): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BudgetItem): Long

    @Update
    suspend fun update(item: BudgetItem)

    @Delete
    suspend fun delete(item: BudgetItem)

    @Query("DELETE FROM budget_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM budget_items WHERE id = :id")
    suspend fun getById(id: Long): BudgetItem?
}
