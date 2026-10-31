package com.alokchitri.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.components.AlokCard
import com.alokchitri.app.ui.components.SecondaryButton
import com.alokchitri.app.ui.components.ValueChip
import com.alokchitri.app.ui.localizedNumber
import com.alokchitri.app.ui.theme.AlokColors

/** What the AI said. `whatHappened` / `nextTime` come back from the Edge Function in the user's language. */
data class FeedbackResult(val whatHappened: String, val nextTime: String)

/** A photo's saved EXIF settings, formatted for chips ("ISO 3200", "1/30", ...). */
data class PhotoMeta(val chips: List<String>)

sealed interface FeedbackUiState {
    /** No photo chosen yet. */
    data object Empty : FeedbackUiState
    data class Loading(val meta: PhotoMeta) : FeedbackUiState
    data class Ready(val meta: PhotoMeta, val result: FeedbackResult) : FeedbackUiState
    /** Daily quota used up: offer the ad, the top-up pack and (if not bought) the unlock. */
    data class OutOfQuota(val meta: PhotoMeta?) : FeedbackUiState
    data class Error(val meta: PhotoMeta?) : FeedbackUiState
}

/** Screen 9 · Photo feedback. Bottom bar comes from the scaffold. Photo comes from the system photo picker. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen(
    state: FeedbackUiState,
    freeLeft: Int,
    onPickPhoto: () -> Unit,
    onWatchAd: () -> Unit,
    onTopUp: () -> Unit,
) {
    val meta = when (state) {
        is FeedbackUiState.Loading -> state.meta
        is FeedbackUiState.Ready -> state.meta
        is FeedbackUiState.OutOfQuota -> state.meta
        is FeedbackUiState.Error -> state.meta
        FeedbackUiState.Empty -> null
    }
    Column(
        Modifier.fillMaxSize().background(AlokColors.Ground).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.feedback_title), style = MaterialTheme.typography.headlineSmall, color = AlokColors.Text)
            Box(
                Modifier.height(36.dp).clip(CircleShape).background(AlokColors.Surface).border(1.dp, AlokColors.Outline, CircleShape).padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.feedback_left_fmt, localizedNumber(freeLeft)), style = MaterialTheme.typography.labelMedium, color = AlokColors.Green) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // TODO: show the chosen photo here (Coil AsyncImage). 140 x 176 dp, 20 dp corners.
            Box(
                Modifier.size(width = 140.dp, height = 176.dp).clip(MaterialTheme.shapes.medium).background(AlokColors.SurfaceRaised)
                    .border(1.dp, AlokColors.OutlineStrong, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.feedback_photo), style = MaterialTheme.typography.labelSmall, color = AlokColors.TextMuted, textAlign = TextAlign.Center) }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                Text(stringResource(R.string.feedback_exif), style = MaterialTheme.typography.labelSmall, color = AlokColors.TextMuted)
                if (meta != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        meta.chips.forEach { ValueChip(it, container = AlokColors.Surface) }
                    }
                }
                SecondaryButton(stringResource(R.string.feedback_pick_other), onPickPhoto, Modifier.fillMaxWidth())
            }
        }

        if (state is FeedbackUiState.Ready) {
            AlokCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = AlokColors.Amber, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.feedback_ai), style = MaterialTheme.typography.labelMedium, color = AlokColors.Amber)
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.feedback_what), style = MaterialTheme.typography.bodySmall, color = AlokColors.TextMuted)
                Text(state.result.whatHappened, style = MaterialTheme.typography.bodyMedium, color = AlokColors.Text)
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = AlokColors.Outline)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.feedback_next), style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = AlokColors.Green)
                Text(state.result.nextTime, style = MaterialTheme.typography.bodyMedium, color = AlokColors.Text)
            }
        }
        // TODO: Loading = skeleton card; Error = retry banner; OutOfQuota = highlight the two buttons below.

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(stringResource(R.string.feedback_watch_ad), onWatchAd, Modifier.weight(1f), accent = true, icon = Icons.Outlined.PlayCircle)
            SecondaryButton(stringResource(R.string.feedback_topup), onTopUp, Modifier.weight(1f))
        }
    }
}
