package com.budgetnotes.app.repository

import com.budgetnotes.app.data.BudgetItem
import com.budgetnotes.app.data.BudgetItemDao
import com.budgetnotes.app.data.BudgetItemType
import com.budgetnotes.app.data.BudgetNote
import com.budgetnotes.app.data.BudgetNoteDao
import com.budgetnotes.app.util.Totals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class NoteWithPreview(
    val note: BudgetNote,
    val totalMinor: Long,
    val previewItems: List<BudgetItem>,
)

class BudgetNoteRepository(
    private val noteDao: BudgetNoteDao,
    private val itemDao: BudgetItemDao,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeHomeNotes(previewLimit: Int = 3): Flow<List<NoteWithPreview>> {
        return noteDao.observeAll().flatMapLatest { notes ->
            val ids = notes.map { it.id }
            if (ids.isEmpty()) {
                flowOf(emptyList())
            } else {
                itemDao.observeForNotes(ids).map { items ->
                    val byNote = items.groupBy { it.noteId }
                    notes.map { note ->
                        val noteItems = byNote[note.id].orEmpty()
                        NoteWithPreview(
                            note = note,
                            totalMinor = Totals.compute(noteItems),
                            previewItems = noteItems.take(previewLimit),
                        )
                    }
                }
            }
        }
    }

    fun observeNote(noteId: Long): Flow<BudgetNote?> = noteDao.observeById(noteId)

    fun observeItems(noteId: Long): Flow<List<BudgetItem>> = itemDao.observeForNote(noteId)

    suspend fun createNote(title: String = ""): Long {
        val now = System.currentTimeMillis()
        return noteDao.insert(
            BudgetNote(title = title, createdAt = now, updatedAt = now),
        )
    }

    suspend fun updateTitle(noteId: Long, title: String) {
        val existing = noteDao.getById(noteId) ?: return
        noteDao.update(
            existing.copy(title = title, updatedAt = System.currentTimeMillis()),
        )
    }

    suspend fun touchNote(noteId: Long) {
        val existing = noteDao.getById(noteId) ?: return
        noteDao.update(existing.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(noteId: Long) {
        noteDao.deleteById(noteId)
    }

    suspend fun addItem(
        noteId: Long,
        amountMinor: Long,
        description: String,
        type: BudgetItemType,
    ): Long {
        val nextPos = itemDao.maxPosition(noteId, type) + 1
        val id = itemDao.insert(
            BudgetItem(
                noteId = noteId,
                amountMinor = amountMinor,
                description = description.trim(),
                type = type,
                position = nextPos,
            ),
        )
        touchNote(noteId)
        return id
    }

    suspend fun updateItem(item: BudgetItem) {
        itemDao.update(item)
        touchNote(item.noteId)
    }

    suspend fun setItemChecked(itemId: Long, checked: Boolean) {
        val item = itemDao.getById(itemId) ?: return
        itemDao.update(item.copy(isChecked = checked))
        touchNote(item.noteId)
    }

    suspend fun deleteItem(itemId: Long): BudgetItem? {
        val item = itemDao.getById(itemId) ?: return null
        itemDao.delete(item)
        touchNote(item.noteId)
        return item
    }

    suspend fun restoreItem(item: BudgetItem): Long {
        val id = itemDao.insert(item.copy(id = 0))
        touchNote(item.noteId)
        return id
    }
}
