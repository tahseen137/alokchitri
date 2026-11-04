package com.alokchitri.engine

import kotlin.math.roundToInt

/**
 * Turns a [Recommendation] into the values the in-app Pro camera sends to Camera2, so the user does not type
 * them into the phone's own camera. Kept here (no Android types) so it can be unit tested.
 */
object ProCapture {

    /** Camera2's white-balance presets. Most phones offer these even when they don't allow a Kelvin value. */
    enum class WbPreset(val kelvin: Int) {
        INCANDESCENT(2700),
        WARM_FLUORESCENT(3000),
        FLUORESCENT(4200),
        DAYLIGHT(5500),
        CLOUDY_DAYLIGHT(6500),
        SHADE(7500),
    }

    /** The preset closest to [kelvin] among the ones this camera offers; null if it offers none (use auto). */
    fun wbPresetFor(kelvin: Int, available: Set<WbPreset> = WbPreset.entries.toSet()): WbPreset? =
        available.minByOrNull { kotlin.math.abs(it.kelvin - kelvin) }

    /** Exposure for one frame. Shutter in nanoseconds, as Camera2 wants it. */
    data class Exposure(val iso: Int, val exposureNs: Long)

    /** The photo itself: exactly what the engine recommended, clamped to what the sensor allows. */
    fun stillExposure(rec: Recommendation, device: DeviceLimits): Exposure = Exposure(
        iso = rec.iso.coerceIn(device.isoMin, device.isoMax),
        exposureNs = (rec.shutterSeconds.coerceIn(device.shutterMinSeconds, device.shutterMaxSeconds) * 1e9).toLong(),
    )

    /**
     * The live preview. A 1/4 s shutter would make the viewfinder crawl at 4 frames a second, so the preview
     * caps the shutter at [maxPreviewSeconds] and raises ISO by the same amount, keeping the brightness the
     * photo will have (as far as the sensor's ISO range allows).
     */
    fun previewExposure(rec: Recommendation, device: DeviceLimits, maxPreviewSeconds: Double = 1.0 / 30): Exposure {
        val still = stillExposure(rec, device)
        val stillSeconds = still.exposureNs / 1e9
        if (stillSeconds <= maxPreviewSeconds) return still
        val gain = stillSeconds / maxPreviewSeconds
        return Exposure(
            iso = (still.iso * gain).roundToInt().coerceIn(device.isoMin, device.isoMax),
            exposureNs = (maxPreviewSeconds * 1e9).toLong(),
        )
    }

    /**
     * Zoom ratio for the recommended lens on a logical multi-camera (Camera2 CONTROL_ZOOM_RATIO, Android 11+),
     * given the range the phone reports. 1.0 when the lens is the main one or the phone can't reach it.
     */
    fun zoomRatioFor(lens: LensType, zoomMin: Double, zoomMax: Double): Double = when (lens) {
        LensType.MAIN -> 1.0
        LensType.ULTRA_WIDE -> if (zoomMin < 1.0) zoomMin else 1.0
        LensType.TELE -> if (zoomMax > 1.0) minOf(TELE_ZOOM, zoomMax) else 1.0
    }

    private const val TELE_ZOOM = 2.0
}
