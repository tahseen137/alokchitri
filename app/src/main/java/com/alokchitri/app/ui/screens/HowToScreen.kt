package com.alokchitri.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.components.AlokCard
import com.alokchitri.app.ui.components.BackBar
import com.alokchitri.app.ui.components.InfoBanner
import com.alokchitri.app.ui.components.PrimaryButton
import com.alokchitri.app.ui.localizedNumber
import com.alokchitri.app.ui.theme.AlokColors

enum class PhoneBrand(val displayName: String, private val aliases: List<String>) {
    XIAOMI("Xiaomi", listOf("xiaomi", "redmi", "poco")),
    SAMSUNG("Samsung", listOf("samsung", "galaxy")),
    REALME("Realme", listOf("realme"));

    companion object {
        /** Brand with a how-to guide whose name or sub-brand appears in [phoneName]; null if none. */
        fun guess(phoneName: String): PhoneBrand? {
            val lower = phoneName.lowercase()
            return entries.firstOrNull { b -> b.aliases.any { it in lower } }
        }
    }
}

/**
 * Screen 8 · Brand how-to. v1 shows three generic steps per brand; the real steps and screenshots come from
 * the remote brand-guide JSON (menu paths per brand and model), so this screen takes a list of steps.
 */
data class HowToStep(val text: String, val screenshotUrl: String? = null)

@Composable
fun HowToScreen(
    initialBrand: PhoneBrand,
    stepsFor: (PhoneBrand) -> List<HowToStep>,
    onBack: () -> Unit,
    onStartCard: () -> Unit,
) {
    var brand by remember { mutableStateOf(initialBrand) }
    Column(Modifier.fillMaxSize().background(AlokColors.Ground).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 24.dp)) {
        BackBar(onBack, title = stringResource(R.string.howto_title), titleBold = true)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PhoneBrand.entries.forEach { b ->
                val selected = b == brand
                Surface(
                    onClick = { brand = b },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = CircleShape,
                    color = if (selected) AlokColors.Amber else AlokColors.Surface,
                    border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, AlokColors.Outline),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(b.displayName, style = MaterialTheme.typography.labelMedium, color = if (selected) AlokColors.OnAmber else AlokColors.Text)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            stepsFor(brand).forEachIndexed { index, step ->
                AlokCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ScreenshotBox(index + 1, step.screenshotUrl)
                        Column {
                            Text(
                                stringResource(R.string.howto_step_fmt, localizedNumber(index + 1)),
                                style = MaterialTheme.typography.labelSmall.copy(color = AlokColors.Amber),
                            )
                            Text(step.text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = AlokColors.Text)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        InfoBanner(Icons.Outlined.Info, AlokColors.Green, stringResource(R.string.howto_note))
        Spacer(Modifier.height(12.dp))
        PrimaryButton(stringResource(R.string.howto_cta), onStartCard)
    }
}

/**
 * 84 x 112 dp. With a [url], load the screenshot (add Coil: io.coil-kt:coil-compose and use AsyncImage);
 * without one, show the dashed placeholder from the design.
 */
@Composable
private fun ScreenshotBox(number: Int, url: String?) {
    Box(
        Modifier.size(width = 84.dp, height = 112.dp).clip(MaterialTheme.shapes.small)
            .background(AlokColors.SurfaceRaised)
            .border(1.dp, AlokColors.OutlineStrong, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        // TODO(coil): if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop)
        Text(
            stringResource(R.string.howto_screenshot_fmt, localizedNumber(number)),
            style = MaterialTheme.typography.labelSmall,
            color = AlokColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(6.dp),
        )
    }
}
