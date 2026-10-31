package com.alokchitri.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.data.SavedRecipe
import com.alokchitri.app.ui.chips
import com.alokchitri.app.ui.components.AlokCard
import com.alokchitri.app.ui.components.LogoMark
import com.alokchitri.app.ui.localizedNumber
import com.alokchitri.app.ui.theme.AlokColors
import com.alokchitri.app.ui.theme.AlokShapes

/** Screen 3 · Home. Bottom bar is supplied by the scaffold in AlokApp. */
@Composable
fun HomeScreen(
    freeLeft: Int,
    freeTotal: Int,
    lastRecipe: SavedRecipe?,
    onNewShot: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(AlokColors.Ground).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LogoMark(30.dp)
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = AlokColors.Text)
            }
            IconButton(
                onClick = onSettings,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(AlokColors.Surface),
            ) { Icon(Icons.Outlined.Settings, stringResource(R.string.cd_settings), tint = AlokColors.Text) }
        }

        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium, color = AlokColors.Text)

        // Hero: amber card, whole surface is the button.
        Surface(onClick = onNewShot, shape = AlokShapes.Hero, color = AlokColors.Amber, contentColor = AlokColors.OnAmber) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(CircleShape).background(AlokColors.OnAmber).padding(horizontal = 12.dp, vertical = 3.dp)) {
                        Text(stringResource(R.string.home_cta_tag), style = MaterialTheme.typography.labelSmall, color = AlokColors.Amber)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(28.dp))
                }
                Text(stringResource(R.string.home_cta_title), style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize))
                Text(stringResource(R.string.home_cta_sub), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AlokCard(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_free_feedback), style = MaterialTheme.typography.labelSmall, color = AlokColors.TextMuted)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.home_left_fmt, localizedNumber(freeLeft), localizedNumber(freeTotal)),
                    style = MaterialTheme.typography.titleLarge,
                    color = AlokColors.Green,
                )
            }
            AlokCard(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_last_recipe), style = MaterialTheme.typography.labelSmall, color = AlokColors.TextMuted)
                Spacer(Modifier.height(6.dp))
                Text(lastRecipe?.name ?: "—", style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize), color = AlokColors.Text)
                if (lastRecipe != null) {
                    Text(lastRecipe.recommendation.chips().take(2).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = AlokColors.TextMuted)
                }
            }
        }

        AlokCard(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(44.dp).clip(AlokShapes.Tile).background(AlokColors.SurfaceRaised), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LightMode, null, tint = AlokColors.Amber, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(stringResource(R.string.home_tip_label), style = MaterialTheme.typography.labelSmall, color = AlokColors.TextMuted)
                    Text(stringResource(R.string.home_tip_body), style = MaterialTheme.typography.bodyMedium, color = AlokColors.Text)
                }
            }
        }
    }
}
