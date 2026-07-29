package com.budgetnotes.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddItemInput(
    initialAmount: String = "",
    initialDescription: String = "",
    onConfirm: (amount: String, description: String) -> Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var amount by remember { mutableStateOf(initialAmount) }
    var description by remember { mutableStateOf(initialDescription) }
    var amountError by remember { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        amountFocus.requestFocus()
    }

    fun submit() {
        val ok = onConfirm(amount, description)
        amountError = !ok
        if (ok) {
            amount = ""
            description = ""
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = amount,
            onValueChange = {
                amount = it
                amountError = false
            },
            modifier = Modifier
                .widthIn(min = 88.dp, max = 120.dp)
                .focusRequester(amountFocus),
            label = { Text("Amount") },
            isError = amountError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.weight(1f),
            label = { Text("Description") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
        IconButton(onClick = { submit() }) {
            Icon(Icons.Default.Check, contentDescription = "Confirm")
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, contentDescription = "Cancel")
        }
    }
}
