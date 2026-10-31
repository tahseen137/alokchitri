package com.alokchitri.engine

import kotlin.math.ln
import kotlin.math.roundToInt

private val LN2 = ln(2.0)
private fun log2(x: Double) = ln(x) / LN2

/**
 * Turns the phone's own auto-exposure result into a scene brightness number.
 *
 * Auto exposure aims for a mid-grey scene, so the settings it picks tell us how bright the scene is:
 * EV at ISO 100 = log2(N^2 / t) - log2(ISO / 100), where N is the f-number and t the exposure time in seconds.
 * Read N, t and ISO from the same frame's CaptureResult (LENS_APERTURE, SENSOR_EXPOSURE_TIME, SENSOR_SENSITIVITY),
 * with exposure compensation at 0.
 */
object LightMeter {
    fun ev100(aperture: Double, exposureSeconds: Double, iso: Double): Double {
        require(aperture > 0 && exposureSeconds > 0 && iso > 0) { "aperture, exposure and ISO must be positive" }
        return log2(aperture * aperture / exposureSeconds) - log2(iso / 100.0)
    }

    fun ev100FromNanos(aperture: Double, exposureNanos: Long, iso: Int): Double =
        ev100(aperture, exposureNanos / 1e9, iso.toDouble())

    fun levelOf(ev100: Double): LightLevel = when {
        ev100 < 3.0 -> LightLevel.VERY_DARK
        ev100 < 6.0 -> LightLevel.LOW
        ev100 < 9.0 -> LightLevel.MEDIUM
        ev100 < 13.0 -> LightLevel.BRIGHT
        else -> LightLevel.SUNNY
    }

    /** Smooths the live stream of readings so the gauge does not flicker. alpha 0.2 is about a 5-frame memory. */
    class Smoother(private val alpha: Double = 0.2) {
        private var value: Double? = null

        fun add(ev100: Double): Double {
            val next = value?.let { it + alpha * (ev100 - it) } ?: ev100
            value = next
            return next
        }

        fun reset() {
            value = null
        }
    }
}

/** "1/60", "1/4000", "2 s", "0.5 s" is shown as "1/2". */
fun formatShutter(seconds: Double): String = when {
    seconds >= 1.0 -> {
        val whole = seconds.roundToInt()
        if (kotlin.math.abs(seconds - whole) < 0.05) "$whole s" else "${(seconds * 10).roundToInt() / 10.0} s"
    }
    else -> "1/${(1.0 / seconds).roundToInt()}"
}
