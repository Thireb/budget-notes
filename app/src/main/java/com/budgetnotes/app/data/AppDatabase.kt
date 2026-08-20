package com.budgetnotes.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteOpenHelper

@Database(
    entities = [BudgetNote::class, BudgetItem::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun budgetNoteDao(): BudgetNoteDao
    abstract fun budgetItemDao(): BudgetItemDao

    companion object {
        const val NAME = "budget_notes_secure.db"
        const val LEGACY_PLAIN_NAME = "budget_notes.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(): AppDatabase =
            instance ?: error("Database not unlocked")

        fun isOpen(): Boolean = instance != null

        fun openEncrypted(context: Context, factory: SupportSQLiteOpenHelper.Factory): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NAME,
                )
                    .openHelperFactory(factory)
                    .build()
                    .also { instance = it }
            }
        }

        fun buildPlainLegacy(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                LEGACY_PLAIN_NAME,
            ).build()
        }

        fun closeAndClear() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
