package com.budgetnotes.app.repository

import com.budgetnotes.app.data.CardCustomField
import com.budgetnotes.app.data.CardCustomFieldDao
import com.budgetnotes.app.data.CardImageStore
import com.budgetnotes.app.data.CardType
import com.budgetnotes.app.data.SavedCard
import com.budgetnotes.app.data.SavedCardDao
import kotlinx.coroutines.flow.Flow

class CardRepository(
    private val cardDao: SavedCardDao,
    private val fieldDao: CardCustomFieldDao,
    private val imageStore: CardImageStore,
) {
    fun observeCards(): Flow<List<SavedCard>> = cardDao.observeAll()

    fun observeCard(cardId: Long): Flow<SavedCard?> = cardDao.observeById(cardId)

    fun observeCustomFields(cardId: Long): Flow<List<CardCustomField>> =
        fieldDao.observeForCard(cardId)

    suspend fun createCard(type: CardType, label: String = ""): Long {
        val now = System.currentTimeMillis()
        val defaultLabel = label.ifBlank {
            when (type) {
                CardType.PAYMENT -> "Payment card"
                CardType.ID -> "ID card"
            }
        }
        return cardDao.insert(
            SavedCard(
                type = type,
                label = defaultLabel,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun updateCard(card: SavedCard) {
        cardDao.update(card.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCard(cardId: Long) {
        imageStore.deleteCardImages(cardId)
        cardDao.deleteById(cardId)
    }

    suspend fun getCard(cardId: Long): SavedCard? = cardDao.getById(cardId)

    suspend fun addCustomField(cardId: Long, name: String, value: String = ""): Long {
        val nextPos = fieldDao.maxPosition(cardId) + 1
        val id = fieldDao.insert(
            CardCustomField(
                cardId = cardId,
                name = name.trim(),
                value = value,
                position = nextPos,
            ),
        )
        touch(cardId)
        return id
    }

    suspend fun updateCustomField(field: CardCustomField) {
        fieldDao.update(field)
        touch(field.cardId)
    }

    suspend fun deleteCustomField(fieldId: Long) {
        val field = fieldDao.getById(fieldId) ?: return
        fieldDao.delete(field)
        touch(field.cardId)
    }

    private suspend fun touch(cardId: Long) {
        val existing = cardDao.getById(cardId) ?: return
        cardDao.update(existing.copy(updatedAt = System.currentTimeMillis()))
    }
}
