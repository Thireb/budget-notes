package com.budgetnotes.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetNoteDao {
    @Query("SELECT * FROM budget_notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<BudgetNote>>

    @Query("SELECT * FROM budget_notes WHERE id = :id")
    fun observeById(id: Long): Flow<BudgetNote?>

    @Query("SELECT * FROM budget_notes WHERE id = :id")
    suspend fun getById(id: Long): BudgetNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: BudgetNote): Long

    @Update
    suspend fun update(note: BudgetNote)

    @Delete
    suspend fun delete(note: BudgetNote)

    @Query("DELETE FROM budget_notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
