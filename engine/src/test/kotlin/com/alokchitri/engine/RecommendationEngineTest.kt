package com.alokchitri.engine

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RecommendationEngineTest {

    /** A typical mid-range phone: f/1.8, ISO 50-6400, 1/4000 s to 30 s, three lenses, RAW, no OIS. */
    private val phone = DeviceLimits(
        isoMin = 50,
        isoMax = 6400,
        shutterMinSeconds = 1.0 / 4000,
        shutterMaxSeconds = 30.0,
        aperture = 1.8,
        lenses = setOf(LensType.MAIN, LensType.ULTRA_WIDE, LensType.TELE),
        rawSupported = true,
        hasOis = false,
    )

    private fun run(
        goal: ShotGoal,
        ev: Double,
        device: DeviceLimits = phone,
        colorTempK: Int? = null,
        allowRaw: Boolean = true,
    ): Recommendation {
        val result = RecommendationEngine.recommend(goal, LightReading(ev, colorTempK), device, allowRaw)
        return assertIs<RecommendationResult.Success>(result).recommendation
    }

    // ---- light meter -------------------------------------------------------------------------

    @Test
    fun `ev100 from auto exposure settings`() {
        // f/1.8, 1/60 s, ISO 800: log2(3.24 * 60) - log2(8) = 7.60 - 3 = 4.60
        assertEquals(4.6, LightMeter.ev100(1.8, 1.0 / 60, 800.0), 0.05)
        // "Sunny 16": f/16, 1/100 s, ISO 100 is about EV 14.6
        assertEquals(14.6, LightMeter.ev100(16.0, 1.0 / 100, 100.0), 0.05)
    }

    @Test
    fun `ev100 from nanoseconds matches seconds`() {
        assertEquals(
            LightMeter.ev100(1.8, 1.0 / 60, 800.0),
            LightMeter.ev100FromNanos(1.8, 16_666_667L, 800),
            0.001,
        )
    }

    @Test
    fun `light levels map to the five gauge bars`() {
        assertEquals(1, LightMeter.levelOf(2.0).gauge)
        assertEquals(2, LightMeter.levelOf(5.0).gauge)
        assertEquals(3, LightMeter.levelOf(7.0).gauge)
        assertEquals(4, LightMeter.levelOf(11.0).gauge)
        assertEquals(5, LightMeter.levelOf(14.0).gauge)
    }

    @Test
    fun `smoother starts at the first reading and moves toward new ones`() {
        val s = LightMeter.Smoother(alpha = 0.5)
        assertEquals(10.0, s.add(10.0), 1e-9)
        assertEquals(8.0, s.add(6.0), 1e-9)
        s.reset()
        assertEquals(3.0, s.add(3.0), 1e-9)
    }

    @Test
    fun `shutter labels`() {
        assertEquals("1/60", formatShutter(1.0 / 60))
        assertEquals("1/4000", formatShutter(1.0 / 4000))
        assertEquals("1/2", formatShutter(0.5))
        assertEquals("2 s", formatShutter(2.0))
        assertEquals("1.5 s", formatShutter(1.5))
    }

    // ---- the design's worked example ---------------------------------------------------------

    @Test
    fun `indoor low light matches the recommendation screen`() {
        val r = run(ShotGoal.INDOOR_LOW_LIGHT, ev = 5.0)
        assertEquals(800, r.iso)
        assertEquals("1/60", r.shutterLabel)
        assertEquals(4200, r.whiteBalanceK)
        assertEquals(0.3, r.evBias, 1e-9)
        assertEquals(LensType.MAIN, r.lens)
        assertEquals(FocusMode.AF_SINGLE, r.focus)
        assertEquals(CaptureFormat.RAW, r.format)
        assertEquals(Stability.HANDHELD_OK, r.stability)
        assertContains(r.advice, Advice.PREFER_NIGHT_MODE)
    }

    // ---- landscape / sunset ------------------------------------------------------------------

    @Test
    fun `sunset in good light uses base ISO and a fast shutter`() {
        val r = run(ShotGoal.LANDSCAPE_SUNSET, ev = 11.0)
        assertEquals(100, r.iso)
        assertEquals("1/1000", r.shutterLabel)
        assertEquals(5600, r.whiteBalanceK)
        assertEquals(LensType.ULTRA_WIDE, r.lens)
        assertEquals(CaptureFormat.RAW, r.format)
        assertTrue(r.advice.isEmpty())
    }

    @Test
    fun `sunset white balance ignores a measured color temperature`() {
        assertEquals(5600, run(ShotGoal.LANDSCAPE_SUNSET, ev = 11.0, colorTempK = 3200).whiteBalanceK)
    }

    @Test
    fun `scene brighter than the phone can handle clamps and warns`() {
        val r = run(ShotGoal.LANDSCAPE_SUNSET, ev = 15.0)
        assertEquals(50, r.iso)
        assertEquals("1/4000", r.shutterLabel)
        assertContains(r.advice, Advice.OVEREXPOSURE_RISK)
        assertContains(r.advice, Advice.PREFER_AUTO)
    }

    // ---- motion ------------------------------------------------------------------------------

    @Test
    fun `motion in dim indoor light keeps 1 over 500`() {
        val r = run(ShotGoal.MOTION, ev = 6.0)
        assertEquals("1/500", r.shutterLabel)
        assertEquals(2500, r.iso)
        assertEquals(FocusMode.AF_CONTINUOUS, r.focus)
        assertFalse(Advice.PREFER_NIGHT_MODE in r.advice)
    }

    @Test
    fun `motion in dark light pins 1 over 250 and pays with noise`() {
        val r = run(ShotGoal.MOTION, ev = 4.0)
        assertEquals("1/250", r.shutterLabel)
        assertEquals(5000, r.iso)
        assertContains(r.advice, Advice.HIGH_NOISE)
        assertFalse(Advice.MOTION_BLUR_RISK in r.advice)
    }

    @Test
    fun `motion in near darkness admits it cannot freeze`() {
        val r = run(ShotGoal.MOTION, ev = 0.0)
        assertContains(r.advice, Advice.MOTION_BLUR_RISK)
        assertTrue(r.shutterSeconds > 1.0 / 250)
        assertTrue(r.iso <= phone.isoMax)
    }

    // ---- night -------------------------------------------------------------------------------

    @Test
    fun `dark night street needs a tripod and a slow shutter`() {
        val r = run(ShotGoal.NIGHT_CITY, ev = 0.0)
        assertEquals(Stability.TRIPOD_RECOMMENDED, r.stability)
        assertTrue(r.iso <= 1600)
        assertTrue(r.shutterSeconds >= 1.0 / 8)
        assertEquals(CaptureFormat.RAW, r.format)
        assertContains(r.advice, Advice.PREFER_NIGHT_MODE)
    }

    @Test
    fun `optical stabilisation extends the handheld limit by a stop`() {
        val noOis = run(ShotGoal.NIGHT_CITY, ev = 2.5)
        val withOis = run(ShotGoal.NIGHT_CITY, ev = 2.5, device = phone.copy(hasOis = true))
        assertEquals("1/30", noOis.shutterLabel)
        assertEquals(Stability.BRACE, noOis.stability)
        assertEquals(Stability.HANDHELD_OK, withOis.stability)
    }

    // ---- lens, format, white balance ----------------------------------------------------------

    @Test
    fun `portrait uses the telephoto only when there is light`() {
        assertEquals(LensType.TELE, run(ShotGoal.PORTRAIT, ev = 11.0).lens)
        assertEquals(LensType.MAIN, run(ShotGoal.PORTRAIT, ev = 6.0).lens)
        assertEquals(LensType.MAIN, run(ShotGoal.PORTRAIT, ev = 11.0, device = phone.copy(lenses = setOf(LensType.MAIN))).lens)
    }

    @Test
    fun `phones without RAW, or users who opt out, get JPEG`() {
        assertEquals(CaptureFormat.JPEG, run(ShotGoal.NIGHT_CITY, ev = 1.0, device = phone.copy(rawSupported = false)).format)
        assertEquals(CaptureFormat.JPEG, run(ShotGoal.NIGHT_CITY, ev = 1.0, allowRaw = false).format)
    }

    @Test
    fun `everyday portrait in bright light stays JPEG`() {
        assertEquals(CaptureFormat.JPEG, run(ShotGoal.PORTRAIT, ev = 12.0).format)
    }

    @Test
    fun `measured color temperature is rounded to 100 K and clamped`() {
        assertEquals(3500, run(ShotGoal.FOOD_PRODUCT, ev = 7.0, colorTempK = 3480).whiteBalanceK)
        assertEquals(8000, run(ShotGoal.FOOD_PRODUCT, ev = 7.0, colorTempK = 9300).whiteBalanceK)
        assertEquals(2500, run(ShotGoal.FOOD_PRODUCT, ev = 7.0, colorTempK = 1800).whiteBalanceK)
    }

    @Test
    fun `bright daylight tells the user Auto is fine`() {
        assertContains(run(ShotGoal.PORTRAIT, ev = 14.0).advice, Advice.PREFER_AUTO)
    }

    // ---- device limits ------------------------------------------------------------------------

    @Test
    fun `phones without manual control get a clear refusal`() {
        val result = RecommendationEngine.recommend(
            ShotGoal.PORTRAIT, LightReading(8.0), phone.copy(manualSupported = false),
        )
        assertEquals(RecommendationResult.ManualUnsupported, result)
    }

    @Test
    fun `a phone with a narrow shutter range never gets an impossible shutter`() {
        val narrow = phone.copy(shutterMinSeconds = 1.0 / 1000, shutterMaxSeconds = 1.0 / 4)
        for (ev in listOf(-2.0, 3.0, 8.0, 14.0)) {
            for (goal in ShotGoal.entries) {
                val r = run(goal, ev, device = narrow)
                assertTrue(r.shutterSeconds >= 1.0 / 1000 * 0.999, "$goal ev=$ev shutter ${r.shutterSeconds}")
                assertTrue(r.shutterSeconds <= 1.0 / 4 * 1.001, "$goal ev=$ev shutter ${r.shutterSeconds}")
            }
        }
    }

    @Test
    fun `fromCamera2 converts nanoseconds and always includes the main lens`() {
        val d = DeviceLimits.fromCamera2(
            isoMin = 100, isoMax = 3200, exposureMinNs = 125_000, exposureMaxNs = 10_000_000_000L,
            aperture = null, lenses = setOf(LensType.ULTRA_WIDE), rawSupported = true, hasOis = true, manualSupported = true,
        )
        assertEquals(1.25e-4, d.shutterMinSeconds, 1e-12)
        assertEquals(10.0, d.shutterMaxSeconds, 1e-9)
        assertEquals(DeviceLimits.DEFAULT_APERTURE, d.aperture, 1e-9)
        assertContains(d.lenses, LensType.MAIN)
    }

    // ---- invariants over the whole input space ------------------------------------------------

    @Test
    fun `every goal and light level yields settings inside the phone's limits on real stops`() {
        val isoStops = setOf(50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400)
        var ev = -3.0
        while (ev <= 17.0) {
            for (goal in ShotGoal.entries) {
                val r = run(goal, ev)
                assertTrue(r.iso in phone.isoMin..phone.isoMax, "$goal ev=$ev iso=${r.iso}")
                assertTrue(r.iso in isoStops, "$goal ev=$ev iso=${r.iso} is not a standard ISO step")
                assertTrue(r.shutterSeconds >= phone.shutterMinSeconds * 0.999, "$goal ev=$ev shutter=${r.shutterSeconds}")
                assertTrue(r.shutterSeconds <= phone.shutterMaxSeconds * 1.001, "$goal ev=$ev shutter=${r.shutterSeconds}")
                assertTrue(r.whiteBalanceK in 2500..8000, "$goal ev=$ev wb=${r.whiteBalanceK}")
            }
            ev += 0.5
        }
    }

    @Test
    fun `darker scenes never get a faster exposure than brighter ones for the same goal`() {
        // exposure "amount" = shutter * iso. It must not go down as the scene gets darker (except where clamped, flagged by advice).
        for (goal in listOf(ShotGoal.PORTRAIT, ShotGoal.FOOD_PRODUCT, ShotGoal.INDOOR_LOW_LIGHT)) {
            var previous = Double.NEGATIVE_INFINITY
            var ev = 12.0
            while (ev >= 2.0) {
                val r = run(goal, ev)
                val amount = r.shutterSeconds * r.iso
                assertTrue(amount >= previous * 0.5, "$goal ev=$ev amount=$amount previous=$previous")
                previous = amount
                ev -= 1.0
            }
        }
    }
}
