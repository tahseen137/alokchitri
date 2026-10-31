package com.alokchitri.engine

/** What the user wants from the shot. Matches the six tiles on the Goal screen. */
enum class ShotGoal { PORTRAIT, FOOD_PRODUCT, INDOOR_LOW_LIGHT, MOTION, NIGHT_CITY, LANDSCAPE_SUNSET }

enum class LensType { MAIN, ULTRA_WIDE, TELE }

/** AF_SINGLE = "AF-S" (focus once), AF_CONTINUOUS = "AF-C" (keeps tracking). */
enum class FocusMode { AF_SINGLE, AF_CONTINUOUS }

enum class CaptureFormat { JPEG, RAW }

enum class Stability { HANDHELD_OK, BRACE, TRIPOD_RECOMMENDED }

/**
 * Notes the UI can turn into a Bangla sentence. The enum order is the order the UI should show them.
 * PREFER_* are the "be honest" cases from the spec: sometimes the phone's own mode will do better.
 */
enum class Advice {
    PREFER_NIGHT_MODE,
    PREFER_AUTO,
    OVEREXPOSURE_RISK,
    UNDEREXPOSURE_RISK,
    HIGH_NOISE,
    MOTION_BLUR_RISK,
}

/** gauge = how many of the 5 bars on the Meter screen are lit. */
enum class LightLevel(val gauge: Int) {
    VERY_DARK(1), LOW(2), MEDIUM(3), BRIGHT(4), SUNNY(5)
}

/**
 * Scene brightness. [ev100] is the exposure value at ISO 100 (see [LightMeter.ev100]).
 * [colorTempK] is optional: an estimate of the light's color temperature, e.g. from the auto white balance gains.
 */
data class LightReading(val ev100: Double, val colorTempK: Int? = null)

/**
 * What this phone's camera can do, read from Android's Camera2 characteristics.
 * Seconds, not nanoseconds, to keep the engine free of Android types.
 */
data class DeviceLimits(
    val isoMin: Int,
    val isoMax: Int,
    val shutterMinSeconds: Double,
    val shutterMaxSeconds: Double,
    val aperture: Double = DEFAULT_APERTURE,
    val lenses: Set<LensType> = setOf(LensType.MAIN),
    val rawSupported: Boolean = false,
    val hasOis: Boolean = false,
    val manualSupported: Boolean = true,
) {
    companion object {
        const val DEFAULT_APERTURE = 1.8

        /** Convenience for values straight from Camera2 (exposure range is in nanoseconds). */
        fun fromCamera2(
            isoMin: Int,
            isoMax: Int,
            exposureMinNs: Long,
            exposureMaxNs: Long,
            aperture: Double?,
            lenses: Set<LensType>,
            rawSupported: Boolean,
            hasOis: Boolean,
            manualSupported: Boolean,
        ) = DeviceLimits(
            isoMin = isoMin,
            isoMax = isoMax,
            shutterMinSeconds = exposureMinNs / 1e9,
            shutterMaxSeconds = exposureMaxNs / 1e9,
            aperture = aperture ?: DEFAULT_APERTURE,
            lenses = lenses + LensType.MAIN,
            rawSupported = rawSupported,
            hasOis = hasOis,
            manualSupported = manualSupported,
        )
    }
}

data class Recommendation(
    val iso: Int,
    val shutterSeconds: Double,
    val whiteBalanceK: Int,
    val focus: FocusMode,
    /**
     * The exposure bias the goal wants (e.g. +0.3 for food). It is already baked into [iso] and
     * [shutterSeconds]; show it only as a hint, or as the EV slider value on phones that keep that slider active.
     */
    val evBias: Double,
    val lens: LensType,
    val format: CaptureFormat,
    val stability: Stability,
    val advice: Set<Advice>,
    val lightLevel: LightLevel,
) {
    val shutterLabel: String get() = formatShutter(shutterSeconds)
}

sealed interface RecommendationResult {
    data class Success(val recommendation: Recommendation) : RecommendationResult

    /** The phone does not allow manual ISO/shutter. The UI should say so and suggest Auto or Night mode. */
    data object ManualUnsupported : RecommendationResult
}
