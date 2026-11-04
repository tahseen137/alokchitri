package com.alokchitri.engine

import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Writes the engine's answer for a grid of inputs to engine/golden/recommendations.txt and checks it has not changed.
 * The iPhone app's Swift port (ios/AlokEngine) reads the same file, so both apps give the same advice.
 *
 * After an intended rule change: run `UPDATE_GOLDEN=1 ./gradlew :engine:test`, port the change to Swift, commit both.
 * If the file is missing it is generated and the test passes.
 */
class GoldenVectorsTest {

    @Test
    fun `engine output matches the golden file shared with iOS`() {
        val actual = render()
        val file = File("golden/recommendations.txt")
        if (!file.exists() || System.getenv("UPDATE_GOLDEN") == "1") {
            file.parentFile.mkdirs()
            file.writeText(actual)
            println("Wrote ${file.absolutePath}")
            return
        }
        assertEquals(file.readText(), actual, "Engine output changed. If intended, run with UPDATE_GOLDEN=1 and update the Swift port.")
    }

    companion object {
        /** Keep this list identical to GoldenVectorsTests.swift. */
        val devices: List<Pair<String, DeviceLimits>> = listOf(
            "phone" to DeviceLimits(50, 6400, 1.0 / 4000, 30.0, 1.8, setOf(LensType.MAIN, LensType.ULTRA_WIDE, LensType.TELE), rawSupported = true, hasOis = false),
            "narrow" to DeviceLimits(100, 3200, 1.0 / 1000, 1.0 / 4, 2.0, setOf(LensType.MAIN), rawSupported = false, hasOis = true),
            "iphone" to DeviceLimits(32, 3072, 1.0 / 24000, 1.0 / 3, 1.78, setOf(LensType.MAIN, LensType.ULTRA_WIDE, LensType.TELE), rawSupported = true, hasOis = true),
        )

        fun render(): String = buildString {
            for ((name, device) in devices) {
                for (goal in ShotGoal.entries) {
                    for (step in -6..34) { // EV -3.0 to 17.0 in half stops
                        val ev = step / 2.0
                        line(name, device, goal, ev, null, true)
                    }
                    line(name, device, goal, 7.0, 3480, true)
                    line(name, device, goal, 1.0, null, false)
                }
            }
        }

        private fun StringBuilder.line(name: String, device: DeviceLimits, goal: ShotGoal, ev: Double, ct: Int?, allowRaw: Boolean) {
            append(name).append(' ').append(goal.name)
            append(" ev=").append(String.format(Locale.ROOT, "%.1f", ev))
            append(" ct=").append(ct?.toString() ?: "-")
            append(" raw=").append(if (allowRaw) 1 else 0)
            append(" -> ")
            when (val r = RecommendationEngine.recommend(goal, LightReading(ev, ct), device, allowRaw)) {
                RecommendationResult.ManualUnsupported -> append("MANUAL_UNSUPPORTED")
                is RecommendationResult.Success -> with(r.recommendation) {
                    append("iso=").append(iso)
                    append(" shutter=").append(String.format(Locale.ROOT, "%.6e", shutterSeconds))
                    append(" label=").append(shutterLabel.replace(' ', '_'))
                    append(" wb=").append(whiteBalanceK)
                    append(" bias=").append(String.format(Locale.ROOT, "%.1f", evBias))
                    append(" focus=").append(focus.name)
                    append(" lens=").append(lens.name)
                    append(" format=").append(format.name)
                    append(" stability=").append(stability.name)
                    append(" level=").append(lightLevel.name)
                    append(" advice=").append(Advice.entries.filter { it in advice }.joinToString(",").ifEmpty { "-" })
                }
            }
            append('\n')
        }
    }
}
