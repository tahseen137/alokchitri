package com.alokchitri.app.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alokchitri.app.ui.screens.CameraProblem
import com.alokchitri.app.ui.screens.CameraUnavailable
import com.alokchitri.app.ui.screens.PreviewPlaceholder
import com.alokchitri.engine.DeviceLimits
import com.alokchitri.engine.FocusMode
import com.alokchitri.engine.ProCapture
import com.alokchitri.engine.ProCapture.WbPreset
import com.alokchitri.engine.Recommendation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.roundToInt

/** What the screen needs to drive the camera: the shutter, and whether manual exposure really took. */
class ProCameraControl {
    internal var session: ProCameraSession? = null

    /** Takes one photo with the recommended settings; [onDone] gets the gallery Uri, or null if it failed. */
    fun capture(onDone: (Uri?) -> Unit) {
        val s = session
        if (s == null) onDone(null) else s.capture(onDone)
    }
}

/**
 * The in-app Pro camera: opens the back camera with the recommendation already applied (ISO, shutter, white
 * balance, focus, lens), so the user only has to press the shutter. [onReady] reports whether the camera is
 * live, and [onManual] whether this phone accepted manual ISO/shutter (false: auto exposure with the EV bias).
 */
@Composable
fun ProCameraHost(
    rec: Recommendation,
    device: DeviceLimits,
    control: ProCameraControl,
    onReady: (Boolean) -> Unit,
    onManual: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ready by rememberUpdatedState(onReady)
    val manual by rememberUpdatedState(onManual)

    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    var asked by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<CameraProblem?>(null) }
    var attempt by remember { mutableIntStateOf(0) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        asked = true
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = hasCameraPermission(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val live = granted && problem == null
    LaunchedEffect(live) { ready(live) }

    when {
        !granted && !asked -> PreviewPlaceholder()
        !granted -> CameraUnavailable(CameraProblem.PERMISSION_DENIED) { context.startActivity(appSettingsIntent(context)) }
        problem != null -> CameraUnavailable(problem!!) { problem = null; attempt++ }
        else -> {
            val session = remember(attempt, rec, device) {
                ProCameraSession(context, rec, device, onProblem = { problem = it }, onManual = { manual(it) })
            }
            DisposableEffect(session, lifecycleOwner) {
                control.session = session
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> session.resume()
                        Lifecycle.Event.ON_PAUSE -> session.pause()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                // Already resumed when this screen is navigated to: the observer replays ON_RESUME on add.
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    if (control.session === session) control.session = null
                    session.release()
                }
            }
            Box(Modifier.fillMaxSize()) {
                AndroidView(factory = { session.textureView }, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** One open camera with a live manual preview and a JPEG output for the shutter. */
internal class ProCameraSession(
    private val context: Context,
    private val rec: Recommendation,
    private val device: DeviceLimits,
    private val onProblem: (CameraProblem) -> Unit,
    private val onManual: (Boolean) -> Unit,
) {
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val thread = HandlerThread("alok-pro").apply { start() }
    private val handler = Handler(thread.looper)
    private val main = Handler(context.mainLooper)
    private val io = Executors.newSingleThreadExecutor()

    private var resumed = false
    private var surface: SurfaceTexture? = null
    @Volatile private var camera: CameraDevice? = null
    @Volatile private var capture: CameraCaptureSession? = null
    @Volatile private var reader: ImageReader? = null
    @Volatile private var previewSurface: Surface? = null
    @Volatile private var chars: CameraCharacteristics? = null
    @Volatile private var previewSize: Size? = null
    @Volatile private var manualOk = false
    @Volatile private var pending: ((Uri?) -> Unit)? = null

    val textureView: TextureView = TextureView(context).apply {
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) { surface = st; openIfReady() }
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = centerCrop(previewSize)
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { surface = null; close(); return true }
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
        }
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> centerCrop(previewSize) }
    }

    fun resume() { resumed = true; openIfReady() }
    fun pause() { resumed = false; close() }
    fun release() { pause(); thread.quitSafely(); io.shutdown() }

    @SuppressLint("MissingPermission") // only constructed once CAMERA is granted
    private fun openIfReady() {
        if (!resumed || surface == null || camera != null) return
        val id = backCameraId() ?: return report(CameraProblem.IN_USE)
        val c = manager.getCameraCharacteristics(id)
        chars = c
        val caps = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toSet().orEmpty()
        manualOk = CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR in caps
        main.post { onManual(manualOk) }
        try {
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(cam: CameraDevice) {
                    if (!resumed) { cam.close(); return }
                    camera = cam
                    startSession(cam, c)
                }
                override fun onDisconnected(cam: CameraDevice) { cam.close(); camera = null; report(CameraProblem.IN_USE) }
                override fun onError(cam: CameraDevice, error: Int) { cam.close(); camera = null; report(CameraProblem.IN_USE) }
            }, handler)
        } catch (e: CameraAccessException) {
            report(CameraProblem.IN_USE)
        } catch (e: SecurityException) {
            report(CameraProblem.PERMISSION_DENIED)
        }
    }

    private fun startSession(cam: CameraDevice, c: CameraCharacteristics) {
        val st = surface ?: return
        val map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return report(CameraProblem.IN_USE)
        val jpegSize = map.getOutputSizes(ImageFormat.JPEG).orEmpty().maxByOrNull { it.width.toLong() * it.height }
            ?: return report(CameraProblem.IN_USE)
        val size = choosePreviewSize(map.getOutputSizes(SurfaceTexture::class.java).orEmpty(), jpegSize)
        previewSize = size
        st.setDefaultBufferSize(size.width, size.height)
        main.post { textureView.centerCrop(size) }

        val preview = Surface(st).also { previewSurface = it }
        val jpeg = ImageReader.newInstance(jpegSize.width, jpegSize.height, ImageFormat.JPEG, 2).also { reader = it }
        jpeg.setOnImageAvailableListener({ r -> onJpeg(r) }, handler)

        @Suppress("DEPRECATION") // the SessionConfiguration overload needs API 28; minSdk is 26
        cam.createCaptureSession(listOf(preview, jpeg.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                if (camera == null) { session.close(); return }
                capture = session
                val request = cam.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(preview)
                    applySettings(this, c, ProCapture.previewExposure(rec, device))
                }.build()
                runCatching { session.setRepeatingRequest(request, null, handler) }
                    .onFailure { report(CameraProblem.IN_USE) }
            }
            override fun onConfigureFailed(session: CameraCaptureSession) = report(CameraProblem.IN_USE)
        }, handler)
    }

    fun capture(onDone: (Uri?) -> Unit) {
        val cam = camera
        val session = capture
        val jpeg = reader
        val c = chars
        if (cam == null || session == null || jpeg == null || c == null || pending != null) return onDone(null)
        pending = onDone
        val request = runCatching {
            cam.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(jpeg.surface)
                previewSurface?.let { addTarget(it) }
                applySettings(this, c, ProCapture.stillExposure(rec, device))
                set(CaptureRequest.JPEG_QUALITY, 95.toByte())
                set(CaptureRequest.JPEG_ORIENTATION, c.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90)
            }.build()
        }.getOrNull()
        if (request == null) return finish(null)
        runCatching {
            session.capture(request, object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) = finish(null)
            }, handler)
        }.onFailure { finish(null) }
    }

    /** The settings from the recommendation. Exposure is manual when the phone allows it, else AE with the EV bias. */
    private fun applySettings(b: CaptureRequest.Builder, c: CameraCharacteristics, exposure: ProCapture.Exposure) {
        b.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        if (manualOk) {
            b.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            b.set(CaptureRequest.SENSOR_SENSITIVITY, exposure.iso)
            b.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposure.exposureNs)
            b.set(CaptureRequest.SENSOR_FRAME_DURATION, exposure.exposureNs)
        } else {
            b.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            evSteps(c)?.let { b.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, it) }
        }

        val awbModes = c.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.toSet().orEmpty()
        val presets = WB_MODES.filterValues { it in awbModes }.keys
        val preset = ProCapture.wbPresetFor(rec.whiteBalanceK, presets)
        b.set(CaptureRequest.CONTROL_AWB_MODE, preset?.let { WB_MODES.getValue(it) } ?: CaptureRequest.CONTROL_AWB_MODE_AUTO)

        val afModes = c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.toSet().orEmpty()
        val af = when (rec.focus) {
            FocusMode.AF_CONTINUOUS -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            // Still subjects: the same continuous focus, which settles and holds on a still scene.
            FocusMode.AF_SINGLE -> CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        }
        if (af in afModes) b.set(CaptureRequest.CONTROL_AF_MODE, af)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            c.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)?.let { range ->
                val zoom = ProCapture.zoomRatioFor(rec.lens, range.lower.toDouble(), range.upper.toDouble())
                if (zoom != 1.0) b.set(CaptureRequest.CONTROL_ZOOM_RATIO, zoom.toFloat())
            }
        }
        val ois = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.toSet().orEmpty()
        if (CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON in ois) {
            b.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON)
        }
    }

    /** The recommendation's EV bias in this camera's compensation steps, clamped to its range. */
    private fun evSteps(c: CameraCharacteristics): Int? {
        val step = c.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)?.toDouble() ?: return null
        val range = c.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) ?: return null
        if (step <= 0.0) return null
        return (rec.evBias / step).roundToInt().coerceIn(range.lower, range.upper)
    }

    private fun onJpeg(r: ImageReader) {
        val image = runCatching { r.acquireLatestImage() }.getOrNull() ?: return
        val bytes = try {
            val buffer = image.planes[0].buffer
            ByteArray(buffer.remaining()).also { buffer.get(it) }
        } finally {
            image.close()
        }
        io.execute { finish(runCatching { saveToGallery(context, bytes) }.getOrNull()) }
    }

    private fun finish(uri: Uri?) {
        val cb = pending ?: return
        pending = null
        main.post { cb(uri) }
    }

    private fun close() {
        runCatching { capture?.close() }
        runCatching { camera?.close() }
        runCatching { reader?.close() }
        runCatching { previewSurface?.release() }
        capture = null
        camera = null
        reader = null
        previewSurface = null
        finish(null)
    }

    private fun report(problem: CameraProblem) {
        close()
        main.post { onProblem(problem) }
    }

    /** The logical back camera: on multi-camera phones it switches lenses itself when the zoom ratio changes. */
    private fun backCameraId(): String? = runCatching {
        manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }.getOrNull()

    /** Largest preview size up to 1920 x 1440 with the photo's aspect ratio, so the viewfinder frames the photo. */
    private fun choosePreviewSize(sizes: Array<out Size>, photo: Size): Size {
        val aspect = photo.width.toDouble() / photo.height
        val fitting = sizes.filter { it.width <= 1920 && it.height <= 1440 }
        return fitting.filter { abs(it.width.toDouble() / it.height - aspect) < 0.02 }.maxByOrNull { it.width.toLong() * it.height }
            ?: fitting.maxByOrNull { it.width.toLong() * it.height }
            ?: sizes.minByOrNull { it.width.toLong() * it.height }
            ?: Size(1280, 720)
    }

    private companion object {
        val WB_MODES = mapOf(
            WbPreset.INCANDESCENT to CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT,
            WbPreset.WARM_FLUORESCENT to CaptureRequest.CONTROL_AWB_MODE_WARM_FLUORESCENT,
            WbPreset.FLUORESCENT to CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT,
            WbPreset.DAYLIGHT to CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
            WbPreset.CLOUDY_DAYLIGHT to CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
            WbPreset.SHADE to CaptureRequest.CONTROL_AWB_MODE_SHADE,
        )
    }
}

/**
 * Saves a JPEG to Pictures/Alokchitri. Android 10+ needs no permission for this; Android 8-9 needs
 * WRITE_EXTERNAL_STORAGE, which the screen asks for before the shutter (the manifest caps it at API 28).
 */
private fun saveToGallery(context: Context, jpeg: ByteArray): Uri? {
    val name = "Alokchitri_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date()) + ".jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Alokchitri")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    try {
        resolver.openOutputStream(uri)?.use { it.write(jpeg) } ?: error("no output stream")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        }
        return uri
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        return null
    }
}
