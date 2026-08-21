package com.budgetnotes.app

import com.budgetnotes.app.data.AppDatabase
import com.budgetnotes.app.data.CardImageStore
import com.budgetnotes.app.data.CardsDatabase
import com.budgetnotes.app.repository.BudgetNoteRepository
import com.budgetnotes.app.repository.CardRepository
import com.budgetnotes.app.security.SecureClipboard
import com.budgetnotes.app.security.SecureDatabaseFactory
import com.budgetnotes.app.security.VaultCrypto
import com.budgetnotes.app.security.VaultLockManager
import com.budgetnotes.app.security.VaultMigration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppContainer(private val application: BudgetNotesApplication) {
    val lockManager = VaultLockManager(application)
    val clipboard = SecureClipboard(application)

    @Volatile
    private var unlocked = false

    lateinit var imageStore: CardImageStore
        private set
    lateinit var repository: BudgetNoteRepository
        private set
    lateinit var cardRepository: CardRepository
        private set

    val isUnlocked: Boolean get() = unlocked && lockManager.isUnlocked

    suspend fun unlockWithSessionKey() = withContext(Dispatchers.IO) {
        val key = lockManager.requireSessionKey()
        VaultMigration.migrateIfNeeded(application, key)
        SecureDatabaseFactory.openNotes(application, key)
        SecureDatabaseFactory.openCards(application, key)
        val imgKey = VaultCrypto.imageKey(key)
        imageStore = CardImageStore(application, imgKey)
        imageStore.encryptExistingPlaintextImages()
        repository = BudgetNoteRepository(
            noteDao = AppDatabase.getInstance().budgetNoteDao(),
            itemDao = AppDatabase.getInstance().budgetItemDao(),
        )
        cardRepository = CardRepository(
            cardDao = CardsDatabase.getInstance().savedCardDao(),
            fieldDao = CardsDatabase.getInstance().cardCustomFieldDao(),
            imageStore = imageStore,
        )
        unlocked = true
    }

    fun lock() {
        unlocked = false
        lockManager.lock()
        AppDatabase.closeAndClear()
        CardsDatabase.closeAndClear()
    }
}
