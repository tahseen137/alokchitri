package com.alokchitri.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.components.BackBar
import com.alokchitri.app.ui.components.LightGauge
import com.alokchitri.app.ui.components.PrimaryButton
import com.alokchitri.app.ui.label
import com.alokchitri.app.ui.localizedNumber
import com.alokchitri.app.ui.theme.AlokColors
import com.alokchitri.app.ui.theme.AlokShapes
import com.alokchitri.app.ui.titleRes
import com.alokchitri.engine.LightLevel
import com.alokchitri.engine.ShotGoal

/**
 * Screen 5 · Meter the light. The top 620 dp is the live camera preview; the sheet below shows the 5-bar gauge.
 * Pass the real Camera2 preview as [preview]; the default is the placeholder drawn in the design.
 */
@Composable
fun MeterScreen(
    goal: ShotGoal,
    level: LightLevel,
    onBack: () -> Unit,
    onDone: () -> Unit,
    preview: @Composable () -> Unit = { PreviewPlaceholder() },
) {
    Box(Modifier.fillMaxSize().background(AlokColors.ViewfinderDeep)) {
        Box(Modifier.fillMaxWidth().height(620.dp)) { preview() }

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.clip(CircleShape).background(AlokColors.Ground)) { BackBar(onBack) }
                Box(
                    Modifier.clip(CircleShape).background(AlokColors.AmberTint).padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text(stringResource(goal.titleRes()), style = MaterialTheme.typography.labelMedium, color = AlokColors.Text) }
            }
            Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).clip(AlokShapes.Banner).background(AlokColors.Ground).padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(stringResource(R.string.meter_hint), style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize), color = AlokColors.Text, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.weight(1f))

            Column(
                Modifier.fillMaxWidth().clip(AlokShapes.Sheet).background(AlokColors.Surface).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(stringResource(R.string.meter_light_now), style = MaterialTheme.typography.bodyMedium, color = AlokColors.TextMuted)
                    Text(level.label(), style = MaterialTheme.typography.headlineSmall, color = AlokColors.Amber)
                }
                LightGauge(level.gauge, stringResource(R.string.meter_gauge_cd, localizedNumber(level.gauge)))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.meter_dark_end), style = MaterialTheme.typography.labelSmall, color = AlokColors.TextMuted)
                    Text(stringResource(R.string.meter_bright_end), style = MaterialTheme.typography.labelSmall, color = AlokColors.TextMuted)
                }
                PrimaryButton(stringResource(R.string.meter_cta), onDone)
            }
        }
    }
}

/** Rule-of-thirds grid and the amber spot-meter reticle from the design. */
@Composable
fun PreviewPlaceholder() {
    Box(Modifier.fillMaxSize().background(AlokColors.Viewfinder)) {
        Canvas(Modifier.fillMaxSize()) {
            val grid = AlokColors.Text.copy(alpha = 0.12f)
            drawLine(grid, Offset(size.width / 3, 0f), Offset(size.width / 3, size.height))
            drawLine(grid, Offset(size.width * 2 / 3, 0f), Offset(size.width * 2 / 3, size.height))
            drawLine(grid, Offset(0f, size.height / 3), Offset(size.width, size.height / 3))
            drawLine(grid, Offset(0f, size.height * 2 / 3), Offset(size.width, size.height * 2 / 3))
            val c = Offset(size.width / 2, size.height / 2)
            val r = 64.dp.toPx()
            drawCircle(AlokColors.Amber, r, c, style = Stroke(2.5.dp.toPx()))
            drawCircle(AlokColors.Amber, 5.dp.toPx(), c)
            val cap = StrokeCap.Round
            val w = 2.5.dp.toPx()
            drawLine(AlokColors.Amber, Offset(c.x, c.y - r), Offset(c.x, c.y - r + 14.dp.toPx()), w, cap)
            drawLine(AlokColors.Amber, Offset(c.x, c.y + r), Offset(c.x, c.y + r - 14.dp.toPx()), w, cap)
            drawLine(AlokColors.Amber, Offset(c.x - r, c.y), Offset(c.x - r + 14.dp.toPx(), c.y), w, cap)
            drawLine(AlokColors.Amber, Offset(c.x + r, c.y), Offset(c.x + r - 14.dp.toPx(), c.y), w, cap)
        }
        Text(
            stringResource(R.string.meter_preview),
            modifier = Modifier.align(Alignment.Center).padding(top = 150.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
            color = AlokColors.TextMuted,
        )
    }
}
