package com.alokchitri.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.alokchitri.app.R
import com.alokchitri.engine.Advice
import com.alokchitri.engine.CaptureFormat
import com.alokchitri.engine.FocusMode
import com.alokchitri.engine.LensType
import com.alokchitri.engine.LightLevel
import com.alokchitri.engine.Recommendation
import com.alokchitri.engine.ShotGoal
import com.alokchitri.engine.Stability
import java.text.NumberFormat
import java.util.Locale

/** Engine enums -> strings. The engine stays free of Android, so all wording lives here and in strings.xml. */

@Composable
fun LensType.label(withZoom: Boolean = true): String = stringResource(
    when (this) {
        LensType.MAIN -> if (withZoom) R.string.lens_main_x else R.string.lens_main
        LensType.ULTRA_WIDE -> if (withZoom) R.string.lens_ultra_wide_x else R.string.lens_ultra_wide
        LensType.TELE -> if (withZoom) R.string.lens_tele_x else R.string.lens_tele
    },
)

@Composable
fun LightLevel.label(): String = stringResource(
    when (this) {
        LightLevel.VERY_DARK -> R.string.level_very_dark
        LightLevel.LOW -> R.string.level_low
        LightLevel.MEDIUM -> R.string.level_medium
        LightLevel.BRIGHT -> R.string.level_bright
        LightLevel.SUNNY -> R.string.level_sunny
    },
)

@Composable
fun Stability.label(): String = stringResource(
    when (this) {
        Stability.HANDHELD_OK -> R.string.stab_ok
        Stability.BRACE -> R.string.stab_brace
        Stability.TRIPOD_RECOMMENDED -> R.string.stab_tripod
    },
)

@Composable
fun FocusMode.label(): String = stringResource(
    when (this) {
        FocusMode.AF_SINGLE -> R.string.focus_afs
        FocusMode.AF_CONTINUOUS -> R.string.focus_afc
    },
)

@Composable
fun CaptureFormat.label(): String = stringResource(
    when (this) {
        CaptureFormat.RAW -> R.string.format_raw
        CaptureFormat.JPEG -> R.string.format_jpeg
    },
)

@Composable
fun Advice.text(): String = stringResource(
    when (this) {
        Advice.PREFER_NIGHT_MODE -> R.string.advice_night
        Advice.PREFER_AUTO -> R.string.advice_auto
        Advice.OVEREXPOSURE_RISK -> R.string.advice_over
        Advice.UNDEREXPOSURE_RISK -> R.string.advice_under
        Advice.HIGH_NOISE -> R.string.advice_noise
        Advice.MOTION_BLUR_RISK -> R.string.advice_blur
    },
)

fun ShotGoal.titleRes(): Int = when (this) {
    ShotGoal.PORTRAIT -> R.string.goal_portrait
    ShotGoal.FOOD_PRODUCT -> R.string.goal_food
    ShotGoal.INDOOR_LOW_LIGHT -> R.string.goal_indoor
    ShotGoal.MOTION -> R.string.goal_motion
    ShotGoal.NIGHT_CITY -> R.string.goal_night
    ShotGoal.LANDSCAPE_SUNSET -> R.string.goal_landscape
}

fun ShotGoal.icon(): ImageVector = when (this) {
    ShotGoal.PORTRAIT -> Icons.Outlined.Person
    ShotGoal.FOOD_PRODUCT -> Icons.Outlined.Restaurant
    ShotGoal.INDOOR_LOW_LIGHT -> Icons.Outlined.Lightbulb
    ShotGoal.MOTION -> Icons.AutoMirrored.Outlined.DirectionsRun
    ShotGoal.NIGHT_CITY -> Icons.Outlined.LocationCity
    ShotGoal.LANDSCAPE_SUNSET -> Icons.Outlined.Landscape
}

/** "+0.3", "0.0", "-0.7" */
fun formatEv(bias: Double): String = when {
    bias > 0.001 -> "+%.1f".format(Locale.ROOT, bias)
    bias < -0.001 -> "%.1f".format(Locale.ROOT, bias)
    else -> "0.0"
}

/** Digits follow the current locale: ২ in Bangla, 2 in English. */
@Composable
fun localizedNumber(n: Int): String {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return NumberFormat.getInstance(locale).format(n)
}

/** The four values shown as chips on a saved recipe, in the order of the floating card. */
fun Recommendation.chips(): List<String> = listOf("ISO $iso", shutterLabel, "${whiteBalanceK}K", formatEv(evBias))
