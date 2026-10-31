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
 */
class DeviceProbe(private val context: Context) {

    data class Result(val phoneName: String, val limits: DeviceLimits)

    companion object {
        /** Name Android reports for this phone, e.g. "Xiaomi 23129RAA4G". Often a model code, hence "not my phone". */
        fun buildName(): String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
    }

    fun probe(): Result? {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val back = backCameras(manager)
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
        val main = byLens[LensType.MAIN] ?: back.first()

        val caps = main.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toSet().orEmpty()
        val iso = main.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val exposure = main.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val manual = CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR in caps && iso != null && exposure != null
        val raw = CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW in caps
        val ois = main.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            ?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) == true
        val aperture = main.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull()?.toDouble()

        val limits = DeviceLimits.fromCamera2(
            isoMin = iso?.lower ?: 100,
            isoMax = iso?.upper ?: 800,
            exposureMinNs = exposure?.lower ?: 100_000L,
            exposureMaxNs = exposure?.upper ?: 1_000_000_000L,
            aperture = aperture,
            lenses = byLens.keys,
            rawSupported = raw,
            hasOis = ois,
            manualSupported = manual,
        )
        return Result(buildName(), limits)
    }

    private fun backCameras(manager: CameraManager): List<CameraCharacteristics> {
        val out = mutableListOf<CameraCharacteristics>()
        for (id in manager.cameraIdList) {
            val c = runCatching { manager.getCameraCharacteristics(id) }.getOrNull() ?: continue
            if (c.get(CameraCharacteristics.LENS_FACING) != CameraCharacteristics.LENS_FACING_BACK) continue
            out += c
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
