package com.alokchitri.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alokchitri.app.R
import com.alokchitri.app.ui.theme.AlokColors
import com.alokchitri.app.ui.theme.AlokShapes

/** Amber pill, at least 56 dp tall: long (Bangla) labels wrap and the button grows instead of clipping. One per screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = AlokColors.Amber, contentColor = AlokColors.OnAmber),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Outlined pill, at least 48 dp tall, growing for wrapped labels. [accent] = amber outline and text (used for "watch an ad"). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    icon: ImageVector? = null,
) {
    val tint = if (accent) AlokColors.Amber else AlokColors.Text
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, if (accent) AlokColors.Amber else AlokColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}

@Composable
fun AlokCard(
    modifier: Modifier = Modifier,
    containerColor: Color = AlokColors.Surface,
    borderColor: Color = AlokColors.Outline,
    shape: Shape = AlokShapes.Card,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier, shape = shape, color = containerColor, border = BorderStroke(1.dp, borderColor)) {
        Column(Modifier.padding(padding), content = content)
    }
}

/** Back arrow (48 dp touch target) with an optional caption, as on Setup, Goal, Recommend, How-to. */
@Composable
fun BackBar(onBack: () -> Unit, modifier: Modifier = Modifier, title: String? = null, titleBold: Boolean = false) {
    Row(modifier.height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = AlokColors.Text)
        }
        if (title != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                title,
                style = if (titleBold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelMedium,
                color = if (titleBold) AlokColors.Text else AlokColors.TextMuted,
            )
        }
    }
}

/** One line of the settings table: muted label left, amber value right. 48 dp tall. */
@Composable
fun SettingRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = AlokColors.Amber,
    divider: Boolean = true,
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = AlokColors.TextMuted)
            Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, textAlign = TextAlign.End)
        }
        if (divider) HorizontalDivider(color = AlokColors.Outline)
    }
}

@Composable
fun ValueChip(text: String, modifier: Modifier = Modifier, container: Color = AlokColors.SurfaceRaised) {
    Box(
        modifier.height(30.dp).clip(CircleShape).background(container).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = AlokColors.Text)
    }
}

/** Raised inset banner with a leading icon: used for tips, honest "use Night mode" notes and help text. */
@Composable
fun InfoBanner(icon: ImageVector, tint: Color, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(AlokShapes.Banner).background(AlokColors.SurfaceRaised).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = AlokColors.Text)
    }
}

/** Five bars; [level] of them are lit (see LightLevel.gauge). */
@Composable
fun LightGauge(level: Int, description: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (i in 1..5) {
            Box(
                Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp))
                    .background(if (i <= level) AlokColors.Amber else AlokColors.Outline),
            )
        }
    }
}

/** The aperture mark used in the app header. */
@Composable
fun LogoMark(size: Dp = 30.dp, color: Color = AlokColors.Amber) {
    Canvas(Modifier.size(size)) {
        val c = Offset(this.size.width / 2, this.size.height / 2)
        val r = this.size.width * 0.40f
        val stroke = this.size.width * 0.08f
        drawCircle(color, radius = r, center = c, style = Stroke(width = stroke))
        drawCircle(color, radius = this.size.width * 0.16f, center = c)
        val cap = StrokeCap.Round
        val w = this.size.width
        drawLine(color, Offset(c.x, w * 0.09f), Offset(c.x, w * 0.28f), stroke, cap)
        drawLine(color, Offset(c.x, w * 0.72f), Offset(c.x, w * 0.91f), stroke, cap)
        drawLine(color, Offset(w * 0.09f, c.y), Offset(w * 0.28f, c.y), stroke, cap)
        drawLine(color, Offset(w * 0.72f, c.y), Offset(w * 0.91f, c.y), stroke, cap)
    }
}

/** Selectable tile on the Goal screen: 136 dp tall, amber outline + tint when selected. */
@Composable
fun GoalTile(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(136.dp),
        shape = AlokShapes.Card,
        color = if (selected) AlokColors.AmberTint else AlokColors.Surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) AlokColors.Amber else AlokColors.Outline),
    ) {
        Box(Modifier.padding(16.dp)) {
            Column(Modifier.fillMaxWidth().height(104.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, contentDescription = null, tint = if (selected) AlokColors.Amber else AlokColors.TextMuted, modifier = Modifier.size(32.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize), color = AlokColors.Text)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AlokColors.TextMuted)
                }
            }
        }
    }
}

enum class AlokTab { HOME, METER, FEEDBACK, RECIPES }

/** The four-tab Material 3 navigation bar (80 dp). Shown on Home, Feedback and Recipes. */
@Composable
fun AlokNavBar(selected: AlokTab, onSelect: (AlokTab) -> Unit) {
    NavigationBar(containerColor = AlokColors.Surface, contentColor = AlokColors.Text, tonalElevation = 0.dp) {
        val colors = NavigationBarItemDefaults.colors(
            indicatorColor = AlokColors.AmberTint,
            selectedIconColor = AlokColors.Amber,
            selectedTextColor = AlokColors.Text,
            unselectedIconColor = AlokColors.TextMuted,
            unselectedTextColor = AlokColors.TextMuted,
        )
        NavigationBarItem(
            selected = selected == AlokTab.HOME, onClick = { onSelect(AlokTab.HOME) },
            icon = { Icon(Icons.Outlined.Home, null) },
            label = { Text(stringResource(R.string.nav_home), style = MaterialTheme.typography.labelSmall) }, colors = colors,
        )
        NavigationBarItem(
            selected = selected == AlokTab.METER, onClick = { onSelect(AlokTab.METER) },
            icon = { Icon(Icons.Outlined.Camera, null) },
            label = { Text(stringResource(R.string.nav_meter), style = MaterialTheme.typography.labelSmall) }, colors = colors,
        )
        NavigationBarItem(
            selected = selected == AlokTab.FEEDBACK, onClick = { onSelect(AlokTab.FEEDBACK) },
            icon = { Icon(Icons.Outlined.Image, null) },
            label = { Text(stringResource(R.string.nav_feedback), style = MaterialTheme.typography.labelSmall) }, colors = colors,
        )
        NavigationBarItem(
            selected = selected == AlokTab.RECIPES, onClick = { onSelect(AlokTab.RECIPES) },
            icon = { Icon(Icons.Outlined.BookmarkBorder, null) },
            label = { Text(stringResource(R.string.nav_recipes), style = MaterialTheme.typography.labelSmall) }, colors = colors,
        )
    }
}
