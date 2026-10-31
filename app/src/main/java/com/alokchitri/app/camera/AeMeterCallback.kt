package com.alokchitri.app.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import com.alokchitri.engine.DeviceLimits
import com.alokchitri.engine.LightMeter

/**
 * Turns each preview frame's auto-exposure result into a scene brightness (EV at ISO 100) and hands it to
 * [onEv100], e.g. FlowViewModel::onLightReading.
 *
 * Plumbing still to write (the part that needs a real phone to debug):
 *   1. Ask for CAMERA permission, open the back camera with CameraManager.openCamera.
 *   2. Create a preview Surface (TextureView / SurfaceView inside an AndroidView in MeterScreen's `preview` slot).
 *   3. Build a TEMPLATE_PREVIEW request with CONTROL_AE_MODE_ON and CONTROL_AE_EXPOSURE_COMPENSATION = 0
 *      (the reading is only meaningful at zero compensation), then setRepeatingRequest(request, AeMeterCallback(...), handler).
 *   4. Close the session when the Meter screen leaves composition.
 */
class AeMeterCallback(
    private val fallbackAperture: Double = DeviceLimits.DEFAULT_APERTURE,
    private val onEv100: (ev100: Double) -> Unit,
) : CameraCaptureSession.CaptureCallback() {

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult,
    ) {
        // While AE is still searching its numbers are not a stable reading of the scene.
        if (result.get(CaptureResult.CONTROL_AE_STATE) == CaptureResult.CONTROL_AE_STATE_SEARCHING) return
        val exposureNs = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: return
        val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: return
        val aperture = result.get(CaptureResult.LENS_APERTURE)?.toDouble() ?: fallbackAperture
        if (exposureNs <= 0L || iso <= 0) return
        onEv100(LightMeter.ev100FromNanos(aperture, exposureNs, iso))
    }
}
