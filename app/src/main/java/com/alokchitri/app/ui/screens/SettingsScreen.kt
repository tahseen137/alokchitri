package com.alokchitri.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.components.AlokCard
import com.alokchitri.app.ui.components.BackBar
import com.alokchitri.app.ui.theme.AlokColors

/** Settings, opened from the gear on Home. Each row is one tap; nothing here needs a connection except Restore. */
@Composable
fun SettingsScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    phoneName: String,
    unlocked: Boolean,
    version: String,
    onBack: () -> Unit,
    onChangePhone: () -> Unit,
    onUpgrade: () -> Unit,
    onRestorePurchases: () -> Unit,
    onPaymentGuide: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(AlokColors.Ground).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp),
    ) {
        BackBar(onBack, title = stringResource(R.string.settings_title), titleBold = true)
        Spacer(Modifier.height(12.dp))

        AlokCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge, color = AlokColors.Text)
                LanguageToggle(language, onLanguageChange)
            }
            HorizontalDivider(color = AlokColors.Outline)
            LinkRow(stringResource(R.string.settings_phone), phoneName, onChangePhone)
        }

        Spacer(Modifier.height(16.dp))
        AlokCard(Modifier.fillMaxWidth(), padding = 0.dp) {
            LinkRow(
                stringResource(R.string.settings_unlock),
                stringResource(if (unlocked) R.string.upgrade_unlocked else R.string.settings_not_unlocked),
                onUpgrade,
                valueColor = if (unlocked) AlokColors.Green else AlokColors.Amber,
            )
            HorizontalDivider(color = AlokColors.Outline)
            LinkRow(stringResource(R.string.settings_restore), null, onRestorePurchases)
            HorizontalDivider(color = AlokColors.Outline)
            LinkRow(stringResource(R.string.settings_payment_guide), null, onPaymentGuide)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.settings_version_fmt, version),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = AlokColors.TextMuted,
        )
    }
}

@Composable
private fun LinkRow(label: String, value: String?, onClick: () -> Unit, valueColor: androidx.compose.ui.graphics.Color = AlokColors.TextMuted) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick)
            .padding(start = 18.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = AlokColors.Text)
        if (value != null) {
            Text(value, Modifier.widthIn(max = 180.dp), style = MaterialTheme.typography.bodyMedium, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = AlokColors.TextMuted, modifier = Modifier.size(24.dp))
    }
}
