package com.alokchitri.engine

import com.alokchitri.engine.ProCapture.WbPreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProCaptureTest {

    private val phone = DeviceLimits(isoMin = 50, isoMax = 6400, shutterMinSeconds = 1.0 / 4000, shutterMaxSeconds = 30.0)

    private fun rec(iso: Int, shutter: Double, lens: LensType = LensType.MAIN) = Recommendation(
        iso = iso,
        shutterSeconds = shutter,
        whiteBalanceK = 5500,
        focus = FocusMode.AF_SINGLE,
        evBias = 0.0,
        lens = lens,
        format = CaptureFormat.JPEG,
        stability = Stability.HANDHELD_OK,
        advice = emptySet(),
        lightLevel = LightLevel.MEDIUM,
    )

    @Test
    fun `white balance picks the closest preset`() {
        assertEquals(WbPreset.INCANDESCENT, ProCapture.wbPresetFor(2600))
        assertEquals(WbPreset.WARM_FLUORESCENT, ProCapture.wbPresetFor(3200))
        assertEquals(WbPreset.DAYLIGHT, ProCapture.wbPresetFor(5600))
        assertEquals(WbPreset.SHADE, ProCapture.wbPresetFor(9000))
    }

    @Test
    fun `white balance uses only the presets the camera offers`() {
        assertEquals(WbPreset.DAYLIGHT, ProCapture.wbPresetFor(3000, setOf(WbPreset.DAYLIGHT, WbPreset.CLOUDY_DAYLIGHT)))
        assertNull(ProCapture.wbPresetFor(3000, emptySet()))
    }

    @Test
    fun `still exposure is the recommendation in nanoseconds`() {
        val e = ProCapture.stillExposure(rec(800, 1.0 / 60), phone)
        assertEquals(800, e.iso)
        assertEquals(16_666_666L, e.exposureNs)
    }

    @Test
    fun `still exposure stays inside the sensor range`() {
        val small = phone.copy(isoMax = 3200, shutterMaxSeconds = 0.5)
        val e = ProCapture.stillExposure(rec(6400, 2.0), small)
        assertEquals(3200, e.iso)
        assertEquals(500_000_000L, e.exposureNs)
    }

    @Test
    fun `fast shutter preview matches the photo`() {
        val r = rec(200, 1.0 / 250)
        assertEquals(ProCapture.stillExposure(r, phone), ProCapture.previewExposure(r, phone))
    }

    @Test
    fun `slow shutter preview trades time for ISO`() {
        // 1/4 s at ISO 400 -> 1/30 s needs 7.5x the gain: ISO 3000.
        val e = ProCapture.previewExposure(rec(400, 0.25), phone)
        assertEquals(33_333_333L, e.exposureNs)
        assertEquals(3000, e.iso)
    }

    @Test
    fun `preview ISO never passes the sensor maximum`() {
        val e = ProCapture.previewExposure(rec(3200, 1.0), phone)
        assertEquals(6400, e.iso)
    }

    @Test
    fun `zoom ratio for each lens`() {
        assertEquals(1.0, ProCapture.zoomRatioFor(LensType.MAIN, 0.6, 10.0))
        assertEquals(0.6, ProCapture.zoomRatioFor(LensType.ULTRA_WIDE, 0.6, 10.0))
        assertEquals(1.0, ProCapture.zoomRatioFor(LensType.ULTRA_WIDE, 1.0, 10.0))
        assertEquals(2.0, ProCapture.zoomRatioFor(LensType.TELE, 0.6, 10.0))
        assertEquals(1.5, ProCapture.zoomRatioFor(LensType.TELE, 1.0, 1.5))
        assertEquals(1.0, ProCapture.zoomRatioFor(LensType.TELE, 1.0, 1.0))
    }
}
