package com.alokchitri.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.components.LogoMark
import com.alokchitri.app.ui.components.PrimaryButton
import com.alokchitri.app.ui.components.ValueChip
import com.alokchitri.app.ui.theme.AlokColors

enum class AppLanguage { BN, EN }

/** Screen 1 · Welcome. Layout: header (logo, language toggle) / illustration / headline + body / dots / CTA. */
@Composable
fun WelcomeScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(AlokColors.Ground).padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LogoMark(32.dp)
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize), color = AlokColors.Text)
            }
            LanguageToggle(language, onLanguageChange)
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            PhoneIllustration()
        }

        Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.displayMedium, color = AlokColors.Text)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.welcome_body), style = MaterialTheme.typography.bodyLarge, color = AlokColors.TextMuted)

        Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            Box(Modifier.size(width = 24.dp, height = 8.dp).clip(CircleShape).background(AlokColors.Amber))
            Box(Modifier.size(8.dp).clip(CircleShape).background(AlokColors.Outline))
            Box(Modifier.size(8.dp).clip(CircleShape).background(AlokColors.Outline))
        }

        PrimaryButton(stringResource(R.string.welcome_cta), onStart)
    }
}

/** Two-segment pill: selected segment is amber. Each segment is at least 48 dp wide for touch. */
@Composable
fun LanguageToggle(language: AppLanguage, onChange: (AppLanguage) -> Unit) {
    Row(
        Modifier.clip(CircleShape).border(1.dp, AlokColors.Outline, CircleShape).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for ((lang, labelRes) in listOf(AppLanguage.BN to R.string.lang_bn, AppLanguage.EN to R.string.lang_en)) {
            val selected = lang == language
            TextButton(
                onClick = { onChange(lang) },
                modifier = Modifier.width(52.dp).height(40.dp),
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Box(
                    Modifier.fillMaxSize().clip(CircleShape).background(if (selected) AlokColors.Amber else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) AlokColors.OnAmber else AlokColors.TextMuted,
                    )
                }
            }
        }
    }
}

/** Phone outline with a lens dial and three sliders, plus three floating value chips (320 x 300 dp). */
@Composable
private fun PhoneIllustration() {
    val description = stringResource(R.string.welcome_illustration_cd)
    Box(Modifier.size(width = 320.dp, height = 300.dp).semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxSize()) {
            val s = density
            fun dp(v: Float) = v * s
            // phone body
            drawRoundRect(AlokColors.Surface, Offset(dp(95f), dp(10f)), Size(dp(130f), dp(270f)), CornerRadius(dp(26f)))
            drawRoundRect(AlokColors.Text.copy(alpha = 0.45f), Offset(dp(95f), dp(10f)), Size(dp(130f), dp(270f)), CornerRadius(dp(26f)), style = Stroke(dp(2f)))
            // dial
            drawCircle(AlokColors.Amber, dp(36f), Offset(dp(160f), dp(86f)), style = Stroke(dp(3f)))
            drawCircle(AlokColors.Amber, dp(14f), Offset(dp(160f), dp(86f)))
            for ((a, b) in listOf(Offset(160f, 36f) to Offset(160f, 46f), Offset(160f, 126f) to Offset(160f, 136f), Offset(110f, 86f) to Offset(120f, 86f), Offset(200f, 86f) to Offset(210f, 86f))) {
                drawLine(AlokColors.Amber, Offset(dp(a.x), dp(a.y)), Offset(dp(b.x), dp(b.y)), dp(3f), StrokeCap.Round)
            }
            // sliders: y, filled-to, fill color
            for ((y, to, color) in listOf(Triple(170f, 169f, AlokColors.Amber), Triple(205f, 145f, AlokColors.Green), Triple(240f, 187f, AlokColors.Amber))) {
                drawLine(AlokColors.Outline, Offset(dp(115f), dp(y)), Offset(dp(205f), dp(y)), dp(4f), StrokeCap.Round)
                drawLine(color, Offset(dp(115f), dp(y)), Offset(dp(to), dp(y)), dp(4f), StrokeCap.Round)
                drawCircle(AlokColors.Text, dp(8f), Offset(dp(to), dp(y)))
            }
        }
        ValueChip("ISO 800", Modifier.offset(x = 14.dp, y = 120.dp).height(40.dp), AlokColors.SurfaceRaised)
        ValueChip("1/60", Modifier.offset(x = 232.dp, y = 170.dp).height(40.dp), AlokColors.SurfaceRaised)
        ValueChip("+0.3 EV", Modifier.offset(x = 8.dp, y = 222.dp).height(40.dp), AlokColors.SurfaceRaised)
    }
}
