package com.alokchitri.app.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.alokchitri.engine.DeviceLimits
import com.alokchitri.engine.LensType
import kotlin.math.hypot

/**
 * Reads what this phone's cameras can do from Camera2 characteristics (no camera permission needed for this).
 *
 * Caveat to verify on real phones: many devices expose secondary lenses only as "physical" cameras inside a
 * logical multi-camera, and some OEMs hide them from third-party apps. We use what Android reports and fall
 * back to the main camera alone.
 *
 * "manualSupported" here only means Camera2 lets *other apps* set ISO and shutter. Many phones (OnePlus, OPPO,
 * realme and others) hide that from third-party apps while their own camera app still has Pro mode, which is
 * where our settings get used. FlowViewModel therefore lets the user confirm Pro mode by hand.
 */
class DeviceProbe(private val context: Context) {

    data class Result(val phoneName: String, val limits: DeviceLimits)

    companion object {
        /** Name Android reports for this phone, e.g. "Xiaomi 23129RAA4G". Often a model code, hence "not my phone". */
        fun buildName(): String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }

    fun probe(): Result? {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val logical = backCameras(manager, includePhysical = false)
        val back = backCameras(manager, includePhysical = true)
        if (back.isEmpty()) return null

        // Lens classification by 35 mm-equivalent focal length.
        val byLens = mutableMapOf<LensType, CameraCharacteristics>()
        for (c in back) {
            val eq = equivalentFocalLength(c) ?: continue
            val lens = when {
                eq < 20.0 -> LensType.ULTRA_WIDE
                eq < 40.0 -> LensType.MAIN
                eq >= 55.0 -> LensType.TELE
                else -> null
            } ?: continue
            byLens.putIfAbsent(lens, c)
        }
        // The first logical back camera is the one the meter opens and the one the phone's own camera app shoots
        // with. Picking "main" by focal length instead can land on a 2 MP depth or macro sensor on budget phones
        // ("main" by focal length also matches those), and they never report manual control.
        val main = logical.firstOrNull() ?: byLens[LensType.MAIN] ?: back.first()

        // OEMs often advertise MANUAL_SENSOR on only one of the logical/physical ids, so ask all back cameras.
        val manualCamera = (listOf(main) + back).firstOrNull { reportsManual(it) }
        val manual = manualCamera != null
        val ranges = manualCamera ?: main
        val iso = ranges.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposure = ranges.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val raw = back.any { CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW in it.capabilities() }
        val ois = main.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            ?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true
        val aperture = main.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull()?.toDouble()

        val limits = DeviceLimits.fromCamera2(
            // Fallbacks for phones that hide the ranges; typical of a stock Pro mode, which the user may confirm.
            isoMin = iso?.lower ?: 100,
            isoMax = iso?.upper ?: 3200,
            exposureMinNs = exposure?.lower ?: 125_000L,
            exposureMaxNs = exposure?.upper ?: 1_000_000_000L,
            aperture = aperture,
            lenses = byLens.keys,
            rawSupported = raw,
            hasOis = ois,
            manualSupported = manual,
        )
        return Result(buildName(), limits)
    }

    private fun CameraCharacteristics.capabilities(): Set<Int> =
        get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toSet().orEmpty()

    private fun reportsManual(c: CameraCharacteristics): Boolean =
        CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR in c.capabilities() &&
            c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) != null &&
            c.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) != null

    private fun backCameras(manager: CameraManager, includePhysical: Boolean): List<CameraCharacteristics> {
        val out = mutableListOf<CameraCharacteristics>()
        for (id in manager.cameraIdList) {
            val c = runCatching { manager.getCameraCharacteristics(id) }.getOrNull() ?: continue
            if (c.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK) continue
            out += c
            if (includePhysical && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                for (physical in c.physicalCameraIds) {
                    runCatching { manager.getCameraCharacteristics(physical) }.getOrNull()?.let { out += it }
                }
            }
        }
        return out
    }

    /** focal length x (full-frame diagonal 43.27 mm / sensor diagonal). */
    private fun equivalentFocalLength(c: CameraCharacteristics): Double? {
        val focal = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() ?: return null
        val size = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: return null
        val diagonal = hypot(size.width.toDouble(), size.height.toDouble())
        return if (diagonal > 0) focal * 43.27 / diagonal else null
    }
}
