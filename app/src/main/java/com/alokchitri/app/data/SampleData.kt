package com.alokchitri.app.data

import com.alokchitri.engine.DeviceLimits
import com.alokchitri.engine.LensType

/** Placeholder values used until the real sources are wired up (device fallback until Camera2 answers). They are not real user data. */
object SampleData {
    const val PHONE_NAME = "Xiaomi Redmi Note 13"

    val device = DeviceLimits(
        isoMin = 50,
        isoMax = 6400,
        shutterMinSeconds = 1.0 / 4000,
        shutterMaxSeconds = 30.0,
        aperture = 1.8,
        lenses = setOf(LensType.MAIN, LensType.ULTRA_WIDE),
        rawSupported = true,
        hasOis = false,
    )
}
