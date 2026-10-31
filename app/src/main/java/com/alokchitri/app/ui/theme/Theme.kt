package com.alokchitri.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AlokColorScheme = darkColorScheme(
    primary = AlokColors.Amber,
    onPrimary = AlokColors.OnAmber,
    primaryContainer = AlokColors.AmberTint,
    onPrimaryContainer = AlokColors.Amber,
    secondary = AlokColors.Green,
    onSecondary = AlokColors.Ground,
    background = AlokColors.Ground,
    onBackground = AlokColors.Text,
    surface = AlokColors.Surface,
    onSurface = AlokColors.Text,
    surfaceVariant = AlokColors.SurfaceRaised,
    onSurfaceVariant = AlokColors.TextMuted,
    outline = AlokColors.Outline,
    outlineVariant = AlokColors.OutlineStrong,
)

/** Card 24, hero 28, sheet 32. Buttons and chips are pills (CircleShape / 50%). */
object AlokShapes {
    val Card = RoundedCornerShape(24.dp)
    val Hero = RoundedCornerShape(28.dp)
    val Sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val Banner = RoundedCornerShape(20.dp)
    val Tile = RoundedCornerShape(16.dp)
}

object AlokSpacing {
    val ScreenPadding = 20.dp
    val CardGap = 12.dp
    val MinTouch = 48.dp
}

@Composable
fun AlokTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AlokColorScheme,
        typography = AlokTypography,
        shapes = Shapes(
            small = AlokShapes.Tile,
            medium = AlokShapes.Banner,
            large = AlokShapes.Card,
            extraLarge = AlokShapes.Hero,
        ),
        content = content,
    )
}
