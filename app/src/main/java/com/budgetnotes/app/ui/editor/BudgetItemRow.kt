package com.budgetnotes.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.budgetnotes.app.data.BudgetItem
import com.budgetnotes.app.util.MoneyFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetItemRow(
    item: BudgetItem,
    onToggleChecked: () -> Unit,
    onEditConfirm: (amount: String, description: String) -> Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember(item.id) { mutableStateOf(false) }

    if (editing) {
        AddItemInput(
            initialAmount = MoneyFormat.formatMinor(item.amountMinor)
                .replace("\u00A0", "")
                .replace(",", "")
                .replace(" ", ""),
            initialDescription = item.description,
            onConfirm = { amount, description ->
                val ok = onEditConfirm(amount, description)
                if (ok) editing = false
                ok
            },
            onCancel = { editing = false },
            modifier = modifier.padding(vertical = 4.dp),
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        content = {
            ItemContent(
                item = item,
                onToggleChecked = onToggleChecked,
                onEdit = { editing = true },
            )
        },
    )
}

@Composable
private fun ItemContent(
    item: BudgetItem,
    onToggleChecked: () -> Unit,
    onEdit: () -> Unit,
) {
    val struck = item.isChecked
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .alpha(if (struck) 0.55f else 1f)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onToggleChecked() },
        )
        Text(
            text = "${MoneyFormat.formatMinor(item.amountMinor)} : ${item.description}",
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (struck) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit)
                .padding(end = 8.dp),
        )
    }
}
