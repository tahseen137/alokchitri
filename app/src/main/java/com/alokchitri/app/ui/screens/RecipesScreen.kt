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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.data.SampleData
import com.alokchitri.app.ui.components.AlokCard
import com.alokchitri.app.ui.components.ValueChip
import com.alokchitri.app.ui.theme.AlokColors

/**
 * Screen 10 · Saved recipes (a lifetime-unlock feature). If the user has not unlocked, show the same list
 * read-only with the first recipe free and the "+" opening the Upgrade screen (open question in the spec).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipesScreen(
    recipes: List<SampleData.Recipe>,
    onNew: () -> Unit,
    onUse: (SampleData.Recipe) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(AlokColors.Ground).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.recipes_title), style = MaterialTheme.typography.headlineSmall, color = AlokColors.Text)
            IconButton(
                onClick = onNew,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(AlokColors.Amber),
            ) { Icon(Icons.Filled.Add, stringResource(R.string.recipes_new_cd), tint = AlokColors.OnAmber) }
        }
        Text(stringResource(R.string.recipes_sub), style = MaterialTheme.typography.bodyMedium, color = AlokColors.TextMuted)

        recipes.forEach { recipe ->
            AlokCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(recipe.name, style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize), color = AlokColors.Text)
                        Text(stringResource(recipe.goalLabelRes), style = MaterialTheme.typography.bodySmall, color = AlokColors.TextMuted)
                    }
                    Icon(Icons.Filled.Bookmark, null, tint = AlokColors.Amber, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recipe.chips.forEach { ValueChip(it) }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onUse(recipe) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = AlokColors.AmberTint, contentColor = AlokColors.Amber),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AlokColors.Amber),
                ) { Text(stringResource(R.string.recipes_use), style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) }
            }
        }

        // Dashed "how to add one" hint, as in the design.
        Row(
            Modifier.fillMaxWidth().dashedBorder().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(AlokColors.Surface), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, null, tint = AlokColors.TextMuted, modifier = Modifier.size(22.dp))
            }
            Text(stringResource(R.string.recipes_empty), style = MaterialTheme.typography.bodyMedium, color = AlokColors.TextMuted)
        }
    }
}

private fun Modifier.dashedBorder(): Modifier = drawBehind {
    drawRoundRect(
        color = AlokColors.OutlineStrong,
        cornerRadius = CornerRadius(24.dp.toPx()),
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
    )
}
