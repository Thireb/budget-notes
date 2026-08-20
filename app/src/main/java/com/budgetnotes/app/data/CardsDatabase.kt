package com.budgetnotes.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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
        const val NAME = "cards.db"

        @Volatile
        private var instance: CardsDatabase? = null

        fun getInstance(context: Context): CardsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CardsDatabase::class.java,
                    NAME,
                ).build().also { instance = it }
            }
        }
    }
}
