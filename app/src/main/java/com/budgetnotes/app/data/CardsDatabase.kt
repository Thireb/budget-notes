package com.budgetnotes.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteOpenHelper

@Database(
    entities = [SavedCard::class, CardCustomField::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CardsDatabase : RoomDatabase() {
    abstract fun savedCardDao(): SavedCardDao
    abstract fun cardCustomFieldDao(): CardCustomFieldDao

    companion object {
        const val NAME = "cards_secure.db"
        const val LEGACY_PLAIN_NAME = "cards.db"

        @Volatile
        private var instance: CardsDatabase? = null

        fun getInstance(): CardsDatabase =
            instance ?: error("Cards database not unlocked")

        fun isOpen(): Boolean = instance != null

        fun openEncrypted(context: Context, factory: SupportSQLiteOpenHelper.Factory): CardsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CardsDatabase::class.java,
                    NAME,
                )
                    .openHelperFactory(factory)
                    .build()
                    .also { instance = it }
            }
        }

        fun buildPlainLegacy(context: Context): CardsDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CardsDatabase::class.java,
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
