package com.budgetnotes.app.util

import com.budgetnotes.app.data.BudgetItem
import com.budgetnotes.app.data.BudgetItemType

object Totals {
    fun compute(items: List<BudgetItem>): Long {
        var add = 0L
        var deduct = 0L
        for (item in items) {
            when (item.type) {
                BudgetItemType.ADD -> add += item.amountMinor
                BudgetItemType.DEDUCT -> deduct += item.amountMinor
            }
        }
        return add - deduct
    }

    fun addTotal(items: List<BudgetItem>): Long =
        items.filter { it.type == BudgetItemType.ADD }.sumOf { it.amountMinor }

    fun deductTotal(items: List<BudgetItem>): Long =
        items.filter { it.type == BudgetItemType.DEDUCT }.sumOf { it.amountMinor }
}
