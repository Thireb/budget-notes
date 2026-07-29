package com.budgetnotes.app.util

import com.budgetnotes.app.data.BudgetItem
import com.budgetnotes.app.data.BudgetItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TotalsTest {
    @Test
    fun compute_sumsAddMinusDeduct_includingChecked() {
        val items = listOf(
            item(1000, BudgetItemType.ADD, checked = false),
            item(250, BudgetItemType.ADD, checked = true),
            item(400, BudgetItemType.DEDUCT, checked = false),
            item(50, BudgetItemType.DEDUCT, checked = true),
        )
        assertEquals(800L, Totals.compute(items))
    }

    @Test
    fun compute_canBeNegative() {
        val items = listOf(
            item(100, BudgetItemType.ADD),
            item(500, BudgetItemType.DEDUCT),
        )
        assertEquals(-400L, Totals.compute(items))
    }

    private fun item(
        amount: Long,
        type: BudgetItemType,
        checked: Boolean = false,
    ) = BudgetItem(
        id = 0,
        noteId = 1,
        amountMinor = amount,
        description = "x",
        type = type,
        isChecked = checked,
    )
}

class MoneyFormatTest {
    @Test
    fun parseToMinor_acceptsDecimals() {
        assertEquals(1250L, MoneyFormat.parseToMinor("12.50"))
        assertEquals(100L, MoneyFormat.parseToMinor("1"))
        assertEquals(105L, MoneyFormat.parseToMinor("1.05"))
    }

    @Test
    fun parseToMinor_rejectsNonPositive() {
        assertNull(MoneyFormat.parseToMinor("0"))
        assertNull(MoneyFormat.parseToMinor("-5"))
        assertNull(MoneyFormat.parseToMinor(""))
        assertNull(MoneyFormat.parseToMinor("abc"))
    }

    @Test
    fun formatMinor_twoDecimals() {
        val formatted = MoneyFormat.formatMinor(1234)
        // Locale-dependent separators; assert contains digits and ends with fractional part concept
        assertEquals("12.34", formatted.replace(",", "").replace("\u00A0", "").replace(" ", ""))
    }
}
