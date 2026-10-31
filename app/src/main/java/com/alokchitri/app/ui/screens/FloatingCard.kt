package com.alokchitri.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.components.LogoMark
import com.alokchitri.app.ui.localizedNumber
import com.alokchitri.app.ui.theme.AlokColors
import com.alokchitri.app.ui.theme.AlokShapes

/** One checklist row on the floating card. Strings are resolved before the card is shown (the overlay is a Service). */
data class CardItem(val label: String, val value: String)

private const val VISIBLE_ROWS = 5

/**
 * Screen 7 · The floating checklist card that sits over the phone's own camera app.
 * Rendered inside a WindowManager overlay by overlay/FloatingCardService; also usable in previews.
 * Width 300 dp; drag the header to move; the minus button collapses it to a 48 dp pill that shows progress.
 */
@Composable
fun FloatingCard(
    title: String,
    items: List<CardItem>,
    checked: Set<Int>,
    minimized: Boolean,
    onToggle: (Int) -> Unit,
    onMinimizedChange: (Boolean) -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
) {
    val progress = stringResource(R.string.card_progress_fmt, localizedNumber(checked.size), localizedNumber(items.size))

    if (minimized) {
        Surface(
            onClick = { onMinimizedChange(false) },
            shape = CircleShape,
            color = AlokColors.Amber,
            contentColor = AlokColors.OnAmber,
            modifier = Modifier.height(48.dp).shadow(8.dp, CircleShape)
                .pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); onDrag(drag.x, drag.y) } },
        ) {
            Row(Modifier.padding(start = 14.dp, end = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LogoMark(22.dp, AlokColors.OnAmber)
                Text(progress, style = MaterialTheme.typography.labelMedium.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize))
            }
        }
        return
    }

    Surface(
        modifier = Modifier.width(300.dp).shadow(12.dp, AlokShapes.Card),
        shape = AlokShapes.Card,
        color = AlokColors.Surface,
        border = BorderStroke(1.dp, AlokColors.Amber),
    ) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 14.dp)) {
            Row(
                Modifier.fillMaxWidth().pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); onDrag(drag.x, drag.y) } },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.DragIndicator, null, tint = AlokColors.TextMuted, modifier = Modifier.size(20.dp))
                    Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = AlokColors.Text)
                    Text(progress, style = MaterialTheme.typography.labelSmall, color = AlokColors.Green)
                }
                IconButton(onClick = { onMinimizedChange(true) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Remove, stringResource(R.string.card_minimize), tint = AlokColors.Text)
                }
            }

            Column(Modifier.heightIn(max = (44 * VISIBLE_ROWS).dp).verticalScroll(rememberScrollState())) {
                items.forEachIndexed { index, item ->
                    val done = index in checked
                    Row(
                        Modifier.fillMaxWidth().height(44.dp)
                            .toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle(index) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CheckBox(done)
                        Text(
                            item.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (done) AlokColors.TextMuted else AlokColors.Text,
                            textDecoration = if (done) TextDecoration.LineThrough else null,
                        )
                        Text(
                            item.value,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                            color = if (done) AlokColors.TextMuted else AlokColors.Amber,
                        )
                    }
                }
            }
            if (items.size > VISIBLE_ROWS) {
                Text(
                    stringResource(R.string.card_more_fmt, localizedNumber(items.size - VISIBLE_ROWS)),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = AlokColors.TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CheckBox(done: Boolean) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        Modifier.size(24.dp).clip(shape)
            .then(if (done) Modifier.background(AlokColors.Green) else Modifier.border(2.dp, AlokColors.OutlineStrong, shape)),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Outlined.Check, stringResource(R.string.card_done_cd), tint = AlokColors.Ground, modifier = Modifier.size(16.dp))
    }
}
