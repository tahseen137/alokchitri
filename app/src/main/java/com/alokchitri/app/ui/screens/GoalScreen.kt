package com.alokchitri.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.components.BackBar
import com.alokchitri.app.ui.components.GoalTile
import com.alokchitri.app.ui.components.PrimaryButton
import com.alokchitri.app.ui.icon
import com.alokchitri.app.ui.theme.AlokColors
import com.alokchitri.app.ui.titleRes
import com.alokchitri.engine.ShotGoal

/** Screen 4 · Shot goal. Two columns of six tiles; the order matches the spec. */
@Composable
fun GoalScreen(
    selected: ShotGoal,
    onSelect: (ShotGoal) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val rows = listOf(
        ShotGoal.PORTRAIT to ShotGoal.FOOD_PRODUCT,
        ShotGoal.INDOOR_LOW_LIGHT to ShotGoal.MOTION,
        ShotGoal.NIGHT_CITY to ShotGoal.LANDSCAPE_SUNSET,
    )
    Column(Modifier.fillMaxSize().background(AlokColors.Ground).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 28.dp)) {
        BackBar(onBack, title = stringResource(R.string.goal_step))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.goal_title), style = MaterialTheme.typography.headlineMedium, color = AlokColors.Text)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.goal_sub), style = MaterialTheme.typography.bodyMedium, color = AlokColors.TextMuted)
        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for ((left, right) in rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (goal in listOf(left, right)) {
                        GoalTile(
                            title = stringResource(goal.titleRes()),
                            subtitle = if (goal == ShotGoal.MOTION) stringResource(R.string.goal_motion_sub) else null,
                            icon = goal.icon(),
                            selected = goal == selected,
                            onClick = { onSelect(goal) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.goal_cta), onNext)
    }
}
