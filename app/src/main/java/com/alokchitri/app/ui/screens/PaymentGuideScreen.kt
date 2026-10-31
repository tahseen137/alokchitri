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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.components.AlokCard
import com.alokchitri.app.ui.components.BackBar
import com.alokchitri.app.ui.components.InfoBanner
import com.alokchitri.app.ui.components.PrimaryButton
import com.alokchitri.app.ui.localizedNumber
import com.alokchitri.app.ui.theme.AlokColors

/**
 * "No card?" from the Upgrade screen: how to pay on Google Play without a bank card. Only steps that hold
 * for every account are shown; which methods appear (bKash, mobile balance, gift code) is decided by Play
 * for the user's country and account, so the screen points to where Play lists them.
 */
@Composable
fun PaymentGuideScreen(onBack: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AlokColors.Ground).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp)) {
        BackBar(onBack, title = stringResource(R.string.payguide_title), titleBold = true)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.payguide_sub), style = MaterialTheme.typography.bodyMedium, color = AlokColors.TextMuted)
            listOf(R.string.payguide_1, R.string.payguide_2, R.string.payguide_3, R.string.payguide_4).forEachIndexed { i, res ->
                AlokCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(AlokColors.AmberTint), contentAlignment = Alignment.Center) {
                            Text(localizedNumber(i + 1), style = MaterialTheme.typography.labelLarge, color = AlokColors.Amber)
                        }
                        Text(stringResource(res), style = MaterialTheme.typography.bodyLarge, color = AlokColors.Text)
                    }
                }
            }
            InfoBanner(Icons.Outlined.Info, AlokColors.Green, stringResource(R.string.payguide_note))
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(stringResource(R.string.payguide_cta), onDone)
    }
}
