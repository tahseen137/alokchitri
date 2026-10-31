package com.alokchitri.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.components.AlokCard
import com.alokchitri.app.ui.components.BackBar
import com.alokchitri.app.ui.components.PrimaryButton
import com.alokchitri.app.ui.components.SettingRow
import com.alokchitri.app.ui.label
import com.alokchitri.app.ui.theme.AlokColors
import com.alokchitri.app.ui.theme.AlokShapes
import com.alokchitri.engine.DeviceLimits
import com.alokchitri.engine.formatShutter

/** Screen 2 · Phone setup. Shows what Camera2 reported about this phone. */
@Composable
fun SetupScreen(
    phoneName: String,
    device: DeviceLimits,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onNotMyPhone: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AlokColors.Ground).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 28.dp)) {
        BackBar(onBack, title = stringResource(R.string.setup_step))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium, color = AlokColors.Text)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.setup_sub), style = MaterialTheme.typography.bodyMedium, color = AlokColors.TextMuted)
        Spacer(Modifier.height(20.dp))

        AlokCard(Modifier.fillMaxWidth(), padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(56.dp).clip(AlokShapes.Tile).background(AlokColors.SurfaceRaised), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.PhoneAndroid, null, tint = AlokColors.Amber, modifier = Modifier.size(28.dp))
                }
                Column {
                    Text(stringResource(R.string.setup_your_phone), style = MaterialTheme.typography.labelSmall, color = AlokColors.TextMuted)
                    Text(phoneName, style = MaterialTheme.typography.titleLarge, color = AlokColors.Text)
                    Text(
                        stringResource(if (device.manualSupported) R.string.setup_pro_found else R.string.setup_pro_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (device.manualSupported) AlokColors.Green else AlokColors.Amber,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        AlokCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            SettingRow(stringResource(R.string.setup_iso_range), "${device.isoMin} – ${device.isoMax}", valueColor = AlokColors.Text)
            SettingRow(
                stringResource(R.string.setup_shutter_range),
                "${formatShutter(device.shutterMinSeconds)} – ${formatShutter(device.shutterMaxSeconds)}",
                valueColor = AlokColors.Text,
            )
            val lensNames = device.lenses.sorted().map { it.label(withZoom = false) }.joinToString(" · ")
            SettingRow(stringResource(R.string.setup_lenses), lensNames, valueColor = AlokColors.Text)
            SettingRow(
                stringResource(R.string.setup_manual),
                stringResource(if (device.manualSupported) R.string.setup_available else R.string.setup_unavailable),
                valueColor = if (device.manualSupported) AlokColors.Green else AlokColors.Amber,
                divider = false,
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Info, null, tint = AlokColors.TextMuted, modifier = Modifier.padding(top = 3.dp).size(22.dp))
            Text(stringResource(R.string.setup_note), style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = AlokColors.TextMuted)
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.setup_cta), onConfirm)
        TextButton(onClick = onNotMyPhone, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(stringResource(R.string.setup_wrong), style = MaterialTheme.typography.labelMedium, color = AlokColors.TextMuted)
        }
    }
}
