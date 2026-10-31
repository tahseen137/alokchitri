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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.components.AlokCard
import com.alokchitri.app.ui.components.BackBar
import com.alokchitri.app.ui.components.InfoBanner
import com.alokchitri.app.ui.components.PrimaryButton
import com.alokchitri.app.ui.components.SecondaryButton
import com.alokchitri.app.ui.components.SettingRow
import com.alokchitri.app.ui.formatEv
import com.alokchitri.app.ui.label
import com.alokchitri.app.ui.text
import com.alokchitri.app.ui.theme.AlokColors
import com.alokchitri.app.ui.titleRes
import com.alokchitri.engine.Advice
import com.alokchitri.engine.Recommendation
import com.alokchitri.engine.RecommendationResult
import com.alokchitri.engine.ShotGoal
import com.alokchitri.engine.Stability

/** Screen 6 · Recommended settings. Handles both outcomes of the engine. */
@Composable
fun RecommendScreen(
    goal: ShotGoal,
    result: RecommendationResult,
    onBack: () -> Unit,
    onStartCard: () -> Unit,
    onSaveRecipe: () -> Unit,
    onWhereToFind: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(AlokColors.Ground).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BackBar(onBack)
            Box(
                Modifier.clip(CircleShape).background(AlokColors.AmberTint).padding(horizontal = 14.dp, vertical = 6.dp),
            ) { Text(stringResource(goal.titleRes()), style = MaterialTheme.typography.labelSmall, color = AlokColors.Text) }
        }

        when (result) {
            is RecommendationResult.ManualUnsupported -> ManualUnsupported()
            is RecommendationResult.Success -> {
                val rec = result.recommendation
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.rec_title), style = MaterialTheme.typography.headlineMedium.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize), color = AlokColors.Text)
                    Spacer(Modifier.height(12.dp))
                    rec.advice.forEach { advice ->
                        InfoBanner(advice.icon(), advice.tint(), advice.text())
                        Spacer(Modifier.height(12.dp))
                    }
                    SettingsTable(rec)
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(stringResource(R.string.rec_cta_card), onStartCard, icon = Icons.Outlined.PictureInPictureAlt)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(stringResource(R.string.rec_save), onSaveRecipe, Modifier.weight(1f))
                    SecondaryButton(stringResource(R.string.rec_where), onWhereToFind, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SettingsTable(rec: Recommendation) {
    AlokCard(Modifier.fillMaxWidth(), padding = 0.dp) {
        SettingRow(stringResource(R.string.rec_iso), rec.iso.toString())
        SettingRow(stringResource(R.string.rec_shutter), rec.shutterLabel)
        SettingRow(stringResource(R.string.rec_wb), "${rec.whiteBalanceK}K")
        SettingRow(stringResource(R.string.rec_focus), rec.focus.label())
        SettingRow(stringResource(R.string.rec_ev), formatEv(rec.evBias))
        SettingRow(stringResource(R.string.rec_lens), rec.lens.label())
        SettingRow(stringResource(R.string.rec_format), rec.format.label())
        SettingRow(
            stringResource(R.string.rec_stability),
            rec.stability.label(),
            valueColor = if (rec.stability == Stability.HANDHELD_OK) AlokColors.Green else AlokColors.Amber,
            divider = false,
        )
    }
}

@Composable
private fun ManualUnsupported() {
    Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.manual_title), style = MaterialTheme.typography.headlineSmall, color = AlokColors.Text)
        Text(stringResource(R.string.manual_body), style = MaterialTheme.typography.bodyLarge, color = AlokColors.TextMuted)
    }
}

private fun Advice.icon(): ImageVector = when (this) {
    Advice.PREFER_NIGHT_MODE -> Icons.Outlined.NightsStay
    Advice.PREFER_AUTO -> Icons.Outlined.Info
    else -> Icons.Outlined.WarningAmber
}

/** Green = friendly alternative ("use Night mode"), amber = a limitation to know about. */
private fun Advice.tint() = when (this) {
    Advice.PREFER_NIGHT_MODE, Advice.PREFER_AUTO -> AlokColors.Green
    else -> AlokColors.Amber
}
