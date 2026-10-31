package com.alokchitri.engine

import java.util.EnumSet
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The rules that turn "what I want" + "how bright it is" + "what my phone can do" into Pro mode settings.
 * Pure Kotlin, no Android types, so it runs offline, costs nothing per use and is easy to unit test.
 *
 * The exposure maths: for a correctly exposed frame, t = 100 * N^2 / (ISO * 2^EV), where EV is the scene's
 * EV at ISO 100. Each goal picks a shutter speed first (what the picture needs), then derives the lowest ISO
 * that exposes it. If that ISO is too noisy, ISO is capped and the shutter gets slower, which may mean
 * advising a tripod. If the scene is so bright that even base ISO is too much, the shutter gets faster.
 */
object RecommendationEngine {

    private const val BASE_ISO = 100

    /** Full stops of shutter speed, seconds. Filtered to what the phone allows. */
    private val SHUTTER_STOPS: List<Double> = listOf(
        1.0 / 8000, 1.0 / 4000, 1.0 / 2000, 1.0 / 1000, 1.0 / 500, 1.0 / 250, 1.0 / 125, 1.0 / 60,
        1.0 / 30, 1.0 / 15, 1.0 / 8, 1.0 / 4, 1.0 / 2, 1.0, 2.0, 4.0, 8.0, 15.0, 30.0,
    )

    /** Third-stop ISO steps that Pro mode sliders typically show. */
    private val ISO_STOPS: List<Int> = listOf(
        50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500,
        3200, 4000, 5000, 6400, 8000, 10000, 12800,
    )

    private enum class Snap { NEAREST, SLOWER }

    fun recommend(
        goal: ShotGoal,
        reading: LightReading,
        device: DeviceLimits,
        allowRaw: Boolean = true,
    ): RecommendationResult {
        if (!device.manualSupported) return RecommendationResult.ManualUnsupported

        val profile = GoalProfile.of(goal)
        val level = LightMeter.levelOf(reading.ev100)
        val advice: MutableSet<Advice> = EnumSet.noneOf(Advice::class.java)

        val n2 = device.aperture * device.aperture
        val exposureFactor = 2.0.pow(reading.ev100 - profile.evBias) // 2^EV, with the goal's bias folded in
        fun isoFor(t: Double) = 100.0 * n2 / (t * exposureFactor)
        fun shutterFor(iso: Double) = 100.0 * n2 / (iso * exposureFactor)

        val isoFloor = max(BASE_ISO, device.isoMin).toDouble()
        val deviceIsoMax = device.isoMax.toDouble()
        val isoCeiling = max(min(profile.isoCeiling, device.isoMax), isoFloor.toInt()).toDouble()
        val maxShutter = min(profile.maxShutter, device.shutterMaxSeconds)

        var isoLimit = isoCeiling
        var snap = Snap.NEAREST
        var shutter = min(profile.preferredShutter, maxShutter)
        var iso = isoFor(shutter)

        if (iso < isoFloor) {
            // Plenty of light: stay at base ISO and let the shutter get faster.
            iso = isoFloor
            shutter = shutterFor(iso)
        } else if (iso > isoCeiling) {
            // Too dark for the preferred shutter at a clean ISO: cap ISO, slow the shutter down.
            iso = isoCeiling
            shutter = shutterFor(iso)
            snap = Snap.SLOWER
        }

        var allowPastMaxShutter = false
        if (shutter > maxShutter) {
            // Even a long shutter is not enough at the ISO ceiling: pin the shutter, push ISO as far as the phone allows.
            shutter = maxShutter
            snap = Snap.NEAREST
            val needed = isoFor(shutter)
            iso = min(needed, deviceIsoMax)
            isoLimit = deviceIsoMax
            if (iso > isoCeiling) advice += Advice.HIGH_NOISE
            if (needed > deviceIsoMax) {
                if (profile.freezesMotion) {
                    // Freezing motion is the point of this goal, so we say honestly that we cannot.
                    shutter = min(shutterFor(iso), device.shutterMaxSeconds)
                    snap = Snap.SLOWER
                    allowPastMaxShutter = true
                    advice += Advice.MOTION_BLUR_RISK
                } else {
                    advice += Advice.UNDEREXPOSURE_RISK
                }
            }
        }

        var overexposed = false
        if (shutter < device.shutterMinSeconds) {
            shutter = device.shutterMinSeconds
            overexposed = true
            advice += Advice.OVEREXPOSURE_RISK
        }

        // Snap to the steps a Pro mode slider offers.
        val shutterStops = SHUTTER_STOPS.filter {
            it >= device.shutterMinSeconds * 0.999 && it <= device.shutterMaxSeconds * 1.001
        }
        var finalShutter = when {
            shutterStops.isEmpty() -> shutter
            snap == Snap.SLOWER -> shutterStops.firstOrNull { it >= shutter * 0.999 } ?: shutterStops.last()
            else -> nearest(shutter, shutterStops)
        }
        if (!allowPastMaxShutter && finalShutter > maxShutter * 1.001) {
            finalShutter = shutterStops.lastOrNull { it <= maxShutter * 1.001 } ?: maxShutter
        }

        val isoLow = if (overexposed) device.isoMin.toDouble() else isoFloor
        val rawIso = isoFor(finalShutter).coerceIn(isoLow, max(isoLimit, isoLow))
        val isoStops = ISO_STOPS.filter { it >= isoLow * 0.999 && it <= max(isoLimit, isoLow) * 1.001 }
        val finalIso = if (isoStops.isEmpty()) rawIso.roundToInt() else nearest(rawIso, isoStops.map { it.toDouble() }).roundToInt()

        val limit = if (device.hasOis) 1.0 / 30 else 1.0 / 60
        val ratio = finalShutter / limit
        val stability = when {
            ratio <= 1.0001 -> Stability.HANDHELD_OK
            ratio <= 2.0001 -> Stability.BRACE
            else -> Stability.TRIPOD_RECOMMENDED
        }

        if (level <= LightLevel.LOW && !profile.freezesMotion) advice += Advice.PREFER_NIGHT_MODE
        if (level == LightLevel.SUNNY && !profile.freezesMotion) advice += Advice.PREFER_AUTO

        return RecommendationResult.Success(
            Recommendation(
                iso = finalIso,
                shutterSeconds = finalShutter,
                whiteBalanceK = whiteBalance(goal, reading, level),
                focus = if (goal == ShotGoal.MOTION) FocusMode.AF_CONTINUOUS else FocusMode.AF_SINGLE,
                evBias = profile.evBias,
                lens = lens(goal, reading.ev100, device),
                format = format(goal, level, device, allowRaw),
                stability = stability,
                advice = advice,
                lightLevel = level,
            ),
        )
    }

    private fun whiteBalance(goal: ShotGoal, reading: LightReading, level: LightLevel): Int {
        // Sunsets keep their warmth under daylight white balance, so never "correct" them.
        if (goal == ShotGoal.LANDSCAPE_SUNSET) return 5600
        reading.colorTempK?.let { return ((it / 100.0).roundToInt() * 100).coerceIn(2500, 8000) }
        return when (level) {
            LightLevel.VERY_DARK -> 3800
            LightLevel.LOW -> 4200
            LightLevel.MEDIUM -> 4800
            LightLevel.BRIGHT -> 5500
            LightLevel.SUNNY -> 5600
        }
    }

    private fun lens(goal: ShotGoal, ev100: Double, device: DeviceLimits): LensType = when {
        // Telephoto sensors are small and dim: only use them when there is enough light.
        goal == ShotGoal.PORTRAIT && LensType.TELE in device.lenses && ev100 >= 9.0 -> LensType.TELE
        goal == ShotGoal.LANDSCAPE_SUNSET && LensType.ULTRA_WIDE in device.lenses && ev100 >= 9.0 -> LensType.ULTRA_WIDE
        else -> LensType.MAIN
    }

    /** RAW only where it pays: very high contrast (sunset) or very low light, and only if the user allows it. */
    private fun format(goal: ShotGoal, level: LightLevel, device: DeviceLimits, allowRaw: Boolean): CaptureFormat =
        if (allowRaw && device.rawSupported &&
            (goal == ShotGoal.LANDSCAPE_SUNSET || level <= LightLevel.LOW)
        ) CaptureFormat.RAW else CaptureFormat.JPEG

    /** Closest value in log space, which is how stops work. */
    private fun nearest(value: Double, stops: List<Double>): Double =
        stops.minByOrNull { abs(ln(value / it)) } ?: value
}

internal data class GoalProfile(
    /** The shutter this kind of picture wants, seconds. */
    val preferredShutter: Double,
    /** Exposure bias in stops: + brighter, - darker. */
    val evBias: Double,
    /** The noisiest ISO we are happy to recommend. */
    val isoCeiling: Int,
    /** The slowest shutter we will recommend. Slower than this, ISO goes up instead. */
    val maxShutter: Double,
    val freezesMotion: Boolean = false,
) {
    companion object {
        fun of(goal: ShotGoal): GoalProfile = when (goal) {
            ShotGoal.PORTRAIT -> GoalProfile(1.0 / 125, +0.3, 1600, 1.0 / 15)
            ShotGoal.FOOD_PRODUCT -> GoalProfile(1.0 / 60, +0.3, 1600, 1.0 / 8)
            ShotGoal.INDOOR_LOW_LIGHT -> GoalProfile(1.0 / 60, +0.3, 1600, 1.0 / 8)
            ShotGoal.MOTION -> GoalProfile(1.0 / 500, 0.0, 3200, 1.0 / 250, freezesMotion = true)
            ShotGoal.NIGHT_CITY -> GoalProfile(1.0 / 30, -0.3, 1600, 8.0)
            ShotGoal.LANDSCAPE_SUNSET -> GoalProfile(1.0 / 125, -0.7, 400, 2.0)
        }
    }
}
