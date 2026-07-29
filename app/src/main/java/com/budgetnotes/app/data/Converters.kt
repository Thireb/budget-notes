package com.budgetnotes.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromBudgetItemType(value: BudgetItemType): String = value.name

    @TypeConverter
    fun toBudgetItemType(value: String): BudgetItemType = BudgetItemType.valueOf(value)
}
