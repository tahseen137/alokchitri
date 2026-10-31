package com.alokchitri.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.getValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coil3.compose.AsyncImage
import com.alokchitri.app.ui.components.InfoBanner
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

/** What the AI said. `whatHappened` / `nextTime` come back from server/api/photo-feedback in the user's language. */
data class FeedbackResult(val whatHappened: String, val nextTime: String)

/** The chosen photo and its saved EXIF settings, formatted for chips ("ISO 3200", "1/30", ...). */
data class PhotoMeta(val chips: List<String>, val photoUri: String? = null)

sealed interface FeedbackUiState {
    /** No photo chosen yet. */
    data object Empty : FeedbackUiState
    data class Loading(val meta: PhotoMeta) : FeedbackUiState
    data class Ready(val meta: PhotoMeta, val result: FeedbackResult) : FeedbackUiState
    /** Daily quota used up: offer the ad, the top-up pack and (if not bought) the unlock. */
    data class OutOfQuota(val meta: PhotoMeta?) : FeedbackUiState
    /** Network or server failure (retry), or [unreadable] when the picked file could not be opened as a photo. */
    data class Error(val meta: PhotoMeta?, val unreadable: Boolean = false) : FeedbackUiState
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
    onRetry: () -> Unit = {},
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
            Box(
                Modifier.size(width = 140.dp, height = 176.dp).clip(MaterialTheme.shapes.medium).background(AlokColors.SurfaceRaised)
                    .border(1.dp, AlokColors.OutlineStrong, MaterialTheme.shapes.medium)
                    .clickable(onClickLabel = stringResource(R.string.feedback_pick), onClick = onPickPhoto),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.feedback_photo), style = MaterialTheme.typography.labelSmall, color = AlokColors.TextMuted, textAlign = TextAlign.Center)
                meta?.photoUri?.let {
                    AsyncImage(model = it, contentDescription = stringResource(R.string.feedback_photo_cd), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
                Text(stringResource(R.string.feedback_exif), style = MaterialTheme.typography.labelSmall, color = AlokColors.TextMuted)
                if (meta != null && meta.chips.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        meta.chips.forEach { ValueChip(it, container = AlokColors.Surface) }
                    }
                } else if (meta != null) {
                    Text(stringResource(R.string.feedback_no_exif), style = MaterialTheme.typography.bodySmall, color = AlokColors.TextMuted)
                }
                SecondaryButton(
                    stringResource(if (meta == null) R.string.feedback_pick else R.string.feedback_pick_other),
                    onPickPhoto,
                    Modifier.fillMaxWidth(),
                    accent = meta == null,
                )
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
        when (state) {
            is FeedbackUiState.Loading -> SkeletonCard()
            is FeedbackUiState.Error -> InfoBanner(
                Icons.Outlined.ErrorOutline,
                AlokColors.Amber,
                stringResource(if (state.unreadable) R.string.feedback_unreadable else R.string.feedback_error),
            )
            is FeedbackUiState.OutOfQuota -> InfoBanner(Icons.Outlined.HourglassEmpty, AlokColors.Amber, stringResource(R.string.feedback_out))
            else -> Unit
        }
        if (state is FeedbackUiState.Error && !state.unreadable) {
            SecondaryButton(stringResource(R.string.meter_retry), onRetry, Modifier.fillMaxWidth(), accent = true, icon = Icons.Outlined.Refresh)
        }

        // Out of quota: these two are the way forward, so they get the strong outline.
        val highlight = state is FeedbackUiState.OutOfQuota
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).then(
                if (highlight) Modifier.border(2.dp, AlokColors.Amber, MaterialTheme.shapes.large).padding(8.dp) else Modifier,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Same height side by side, even when only one label wraps.
            SecondaryButton(stringResource(R.string.feedback_watch_ad), onWatchAd, Modifier.weight(1f).fillMaxHeight(), accent = true, icon = Icons.Outlined.PlayCircle)
            SecondaryButton(stringResource(R.string.feedback_topup), onTopUp, Modifier.weight(1f).fillMaxHeight(), accent = highlight)
        }
    }
}

/** Grey placeholder in the AI card's place while the server answers. Pulses so it reads as working. */
@Composable
private fun SkeletonCard() {
    val pulse = rememberInfiniteTransition(label = "skeleton")
    val alpha by pulse.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")
    val loading = stringResource(R.string.feedback_loading)
    AlokCard(Modifier.fillMaxWidth().semantics { contentDescription = loading }) {
        listOf(0.4f, 1f, 0.85f, 0f, 0.3f, 1f, 0.7f).forEach { width ->
            if (width == 0f) Spacer(Modifier.height(12.dp))
            else Box(
                Modifier.padding(vertical = 4.dp).fillMaxWidth(width).height(12.dp)
                    .clip(CircleShape).background(AlokColors.SurfaceRaised.copy(alpha = alpha)),
            )
        }
    }
}
