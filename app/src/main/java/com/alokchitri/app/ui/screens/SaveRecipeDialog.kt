package com.alokchitri.app.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.alokchitri.app.R

/** Names a recipe before saving it from the Recommendation screen. Starts with the goal's name. */
@Composable
fun SaveRecipeDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rec_save)) },
        text = {
            OutlinedTextField(
                name, { if (it.length <= 40) name = it },
                label = { Text(stringResource(R.string.recipes_name_label)) },
                supportingText = { Text(stringResource(R.string.recipes_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.picker_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.perm_cancel)) } },
    )
}
