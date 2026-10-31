package com.alokchitri.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.theme.AlokColors

/**
 * "Not my phone" on Setup: pick a brand and type the model as printed on the box or in Settings > About phone.
 * Only the displayed name changes; ISO, shutter and lens limits still come from Camera2.
 */
@Composable
fun PhonePickerDialog(
    initialBrand: PhoneBrand?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val other = stringResource(R.string.picker_other_brand)
    var brand by rememberSaveable { mutableStateOf(initialBrand?.displayName) }
    var otherBrand by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    val brandName = brand ?: otherBrand.trim()
    val canSave = brandName.isNotEmpty() && model.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.selectableGroup()) {
                    (PhoneBrand.entries.map { it.displayName } + other).forEach { name ->
                        val isOther = name == other
                        val selected = if (isOther) brand == null else brand == name
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .selectable(selected, role = Role.RadioButton) { brand = if (isOther) null else name },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = AlokColors.Amber))
                            Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
                if (brand == null) {
                    OutlinedTextField(
                        otherBrand, { otherBrand = it },
                        label = { Text(stringResource(R.string.picker_brand_label)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    model, { model = it },
                    label = { Text(stringResource(R.string.picker_model_label)) },
                    supportingText = { Text(stringResource(R.string.picker_model_hint)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave("$brandName ${model.trim()}") }, enabled = canSave) { Text(stringResource(R.string.picker_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.perm_cancel)) } },
    )
}
