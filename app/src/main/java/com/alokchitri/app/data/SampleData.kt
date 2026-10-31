package com.alokchitri.app.data

import com.alokchitri.engine.DeviceLimits
import com.alokchitri.engine.LensType

/** Placeholder values used until Camera2 probing and Room/DataStore are wired up. They are not real user data. */
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

    data class Recipe(val name: String, val goalLabelRes: Int, val chips: List<String>)

    val recipes = listOf(
        Recipe("আমার ঘর, রাত", com.alokchitri.app.R.string.goal_indoor, listOf("ISO 800", "1/60", "4200K", "+0.3")),
        Recipe("ছাদের সূর্যাস্ত", com.alokchitri.app.R.string.goal_landscape, listOf("ISO 100", "1/250", "5600K", "-0.7")),
    )

    const val FREE_FEEDBACKS_PER_DAY = 3
    const val FREE_FEEDBACKS_LEFT = 2
}
