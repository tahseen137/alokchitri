package com.alokchitri.app.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.alokchitri.app.ui.screens.CameraProblem
import com.alokchitri.app.ui.screens.CameraUnavailable
import com.alokchitri.app.ui.screens.MeterReticle
import com.alokchitri.app.ui.screens.PreviewPlaceholder

/**
 * Screen 5's live preview: asks for CAMERA, opens the main back camera into a TextureView and feeds every
 * settled auto-exposure result to [onFrameEv] through [AeMeterCallback]. [onCameraReady] tells the screen
 * whether a reading is coming, so it can hide the gauge while the camera is unavailable.
 */
@Composable
fun Camera2PreviewHost(
    onFrameEv: (Double) -> Unit,
    onCameraReady: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onEv by rememberUpdatedState(onFrameEv)
    val onReady by rememberUpdatedState(onCameraReady)

    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    var asked by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<CameraProblem?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        asked = true
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    // Coming back from the system settings page: the user may have granted it there.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasCameraPermission(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val live = granted && problem == null
    SideEffect { onReady(live) }

    when {
        !granted && !asked -> PreviewPlaceholder()
        !granted -> {
            CameraUnavailable(CameraProblem.PERMISSION_DENIED) { context.startActivity(appSettingsIntent(context)) }
        }
        problem != null -> {
            CameraUnavailable(problem!!) { problem = null; attempt++ }
        }
        else -> {
            val session = remember(attempt) {
                MeterSession(context, onEv = { onEv(it) }, onProblem = { problem = it })
            }
            DisposableEffect(session, lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> session.resume()
                        Lifecycle.Event.ON_PAUSE -> session.pause()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    session.release()
                }
            }
            Box(Modifier.fillMaxSize()) {
                AndroidView(factory = { session.textureView }, modifier = Modifier.fillMaxSize())
                MeterReticle()
            }
        }
    }
}

private fun hasCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun appSettingsIntent(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/**
 * One open camera. Opens when both the surface exists and the screen is resumed, closes on pause so the
 * user's own camera app can take the camera while the floating card is up.
 */
private class MeterSession(
    private val context: Context,
    private val onEv: (Double) -> Unit,
    private val onProblem: (CameraProblem) -> Unit,
) {
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val thread = HandlerThread("alok-meter").apply { start() }
    private val handler = Handler(thread.looper)
    private val main = Handler(context.mainLooper)

    private var resumed = false
    private var surface: SurfaceTexture? = null
    @Volatile private var device: CameraDevice? = null
    @Volatile private var capture: CameraCaptureSession? = null
    @Volatile private var previewSize: Size? = null

    val textureView: TextureView = TextureView(context).apply {
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) { surface = st; openIfReady() }
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = applyCenterCrop()
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { surface = null; close(); return true }
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
        }
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> applyCenterCrop() }
    }

    fun resume() { resumed = true; openIfReady() }
    fun pause() { resumed = false; close() }
    fun release() { pause(); thread.quitSafely() }

    @SuppressLint("MissingPermission") // only constructed once CAMERA is granted
    private fun openIfReady() {
        if (!resumed || surface == null || device != null) return
        val id = mainBackCameraId() ?: return report(CameraProblem.IN_USE)
        val chars = manager.getCameraCharacteristics(id)
        try {
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    if (!resumed) { camera.close(); return }
                    device = camera
                    startPreview(camera, chars)
                }
                override fun onDisconnected(camera: CameraDevice) { camera.close(); device = null; report(CameraProblem.IN_USE) }
                override fun onError(camera: CameraDevice, error: Int) { camera.close(); device = null; report(CameraProblem.IN_USE) }
            }, handler)
        } catch (e: CameraAccessException) {
            report(CameraProblem.IN_USE)
        } catch (e: SecurityException) {
            report(CameraProblem.PERMISSION_DENIED)
        }
    }

    private fun startPreview(camera: CameraDevice, chars: CameraCharacteristics) {
        val st = surface ?: return
        val size = choosePreviewSize(chars)
        previewSize = size
        st.setDefaultBufferSize(size.width, size.height)
        main.post { applyCenterCrop() }
        val target = Surface(st)
        @Suppress("DEPRECATION") // the SessionConfiguration overload needs API 28; minSdk is 26
        camera.createCaptureSession(listOf(target), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                if (device == null) { session.close(); return }
                capture = session
                val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(target)
                    set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    // The reading is only a measure of the scene at zero compensation.
                    set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)
                    spotRegion(chars)?.let { set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(it)) }
                }.build()
                val aperture = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull()?.toDouble()
                val callback = if (aperture != null) AeMeterCallback(aperture) { ev -> main.post { onEv(ev) } }
                else AeMeterCallback { ev -> main.post { onEv(ev) } }
                runCatching { session.setRepeatingRequest(request, callback, handler) }
                    .onFailure { report(CameraProblem.IN_USE) }
            }
            override fun onConfigureFailed(session: CameraCaptureSession) = report(CameraProblem.IN_USE)
        }, handler)
    }

    private fun close() {
        runCatching { capture?.close() }
        runCatching { device?.close() }
        capture = null
        device = null
    }

    private fun report(problem: CameraProblem) {
        close()
        main.post { onProblem(problem) }
    }

    /** The logical back camera; on multi-camera phones this is the one that switches lenses for us. */
    private fun mainBackCameraId(): String? = runCatching {
        manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }.getOrNull()

    /** Largest 4:3 or 16:9 output up to 1920 px wide: plenty for a viewfinder, cheap for low-end phones. */
    private fun choosePreviewSize(chars: CameraCharacteristics): Size {
        val sizes = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(SurfaceTexture::class.java).orEmpty()
        return sizes.filter { it.width <= 1920 && it.height <= 1440 }
            .maxByOrNull { it.width.toLong() * it.height }
            ?: sizes.minByOrNull { it.width.toLong() * it.height }
            ?: Size(1280, 720)
    }

    /** Centre quarter of the sensor, matching the reticle drawn over the preview. */
    private fun spotRegion(chars: CameraCharacteristics): MeteringRectangle? {
        val maxRegions = chars.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 0
        val area = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return null
        if (maxRegions < 1) return null
        val w = area.width() / 4
        val h = area.height() / 4
        val cx = area.width() / 2
        val cy = area.height() / 2
        return MeteringRectangle(Rect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2), MeteringRectangle.METERING_WEIGHT_MAX)
    }

    /**
     * TextureView stretches the buffer to fill the view. The sensor buffer is landscape and shown rotated in
     * this portrait UI, so scale it back to its own aspect ratio and crop the overflow from the centre.
     */
    private fun applyCenterCrop() {
        val size = previewSize ?: return
        val vw = textureView.width.toFloat()
        val vh = textureView.height.toFloat()
        if (vw == 0f || vh == 0f) return
        val contentAspect = size.height.toFloat() / size.width // width / height once rotated to portrait
        val matrix = Matrix()
        if (vw / vh > contentAspect) matrix.setScale(1f, (vw / contentAspect) / vh, vw / 2, vh / 2)
        else matrix.setScale((vh * contentAspect) / vw, 1f, vw / 2, vh / 2)
        textureView.setTransform(matrix)
    }
}
