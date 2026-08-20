package com.budgetnotes.app.security

import android.content.Context
import com.budgetnotes.app.data.AppDatabase
import com.budgetnotes.app.data.CardsDatabase
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object SecureDatabaseFactory {
    init {
        System.loadLibrary("sqlcipher")
    }

    fun openNotes(context: Context, key: ByteArray): AppDatabase {
        val factory = SupportOpenHelperFactory(key.copyOf())
        return AppDatabase.openEncrypted(context, factory)
    }

    fun openCards(context: Context, key: ByteArray): CardsDatabase {
        val factory = SupportOpenHelperFactory(key.copyOf())
        return CardsDatabase.openEncrypted(context, factory)
    }
}

/**
 * One-time copy from legacy plaintext DB filenames into SQLCipher secure DBs.
 */
object VaultMigration {

    suspend fun migrateIfNeeded(context: Context, key: ByteArray) = withContext(Dispatchers.IO) {
        migrateNotes(context, key)
        migrateCards(context, key)
    }

    private suspend fun migrateNotes(context: Context, key: ByteArray) {
        val flag = File(context.noBackupFilesDir, "migrated_notes_v1")
        if (flag.exists()) return
        val plainFile = context.getDatabasePath(AppDatabase.LEGACY_PLAIN_NAME)
        if (!plainFile.exists()) {
            flag.writeText("ok")
            return
        }
        val plainDb = AppDatabase.buildPlainLegacy(context)
        try {
            val notes = plainDb.budgetNoteDao().getAllOnce()
            val items = notes.flatMap { plainDb.budgetItemDao().getForNote(it.id) }
            // Ensure secure DB is created/open with key, then insert
            AppDatabase.closeAndClear()
            val encDb = SecureDatabaseFactory.openNotes(context, key)
            notes.forEach { encDb.budgetNoteDao().insert(it) }
            items.forEach { encDb.budgetItemDao().insert(it) }
        } finally {
            plainDb.close()
        }
        context.deleteDatabase(AppDatabase.LEGACY_PLAIN_NAME)
        File(plainFile.path + "-shm").delete()
        File(plainFile.path + "-wal").delete()
        flag.writeText("ok")
    }

    private suspend fun migrateCards(context: Context, key: ByteArray) {
        val flag = File(context.noBackupFilesDir, "migrated_cards_v1")
        if (flag.exists()) return
        val plainFile = context.getDatabasePath(CardsDatabase.LEGACY_PLAIN_NAME)
        if (!plainFile.exists()) {
            flag.writeText("ok")
            return
        }
        val plainDb = CardsDatabase.buildPlainLegacy(context)
        try {
            val cards = plainDb.savedCardDao().getAllOnce()
            val fields = cards.flatMap { plainDb.cardCustomFieldDao().getForCardOnce(it.id) }
            CardsDatabase.closeAndClear()
            val encDb = SecureDatabaseFactory.openCards(context, key)
            cards.forEach { encDb.savedCardDao().insert(it) }
            fields.forEach { encDb.cardCustomFieldDao().insert(it) }
        } finally {
            plainDb.close()
        }
        context.deleteDatabase(CardsDatabase.LEGACY_PLAIN_NAME)
        File(plainFile.path + "-shm").delete()
        File(plainFile.path + "-wal").delete()
        flag.writeText("ok")
    }
}
