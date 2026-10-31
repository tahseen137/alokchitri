package com.alokchitri.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.alokchitri.app.MainActivity
import com.alokchitri.app.R
import com.alokchitri.app.ui.screens.CardItem
import com.alokchitri.app.ui.screens.FloatingCard
import com.alokchitri.app.ui.theme.AlokTheme

/**
 * Shows the recommended settings as a draggable card on top of the phone's own camera app.
 * Needs the user to grant "Display over other apps" once (see [canDrawOverlays] / [permissionIntent]).
 *
 * Start it from the Recommend screen with [start]; labels and values are passed as plain strings because
 * the Service has no Activity and must not depend on UI state.
 *
 * Calling [start] again while the card is up replaces its rows in the same window (no duplicates).
 * Still to verify on real Xiaomi / Samsung / Realme phones: some skins restrict overlays while another
 * app's camera is in the foreground.
 */
class FloatingCardService : Service() {

    private lateinit var windowManager: WindowManager
    private val owner = OverlayLifecycleOwner()
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var title by mutableStateOf("")
    private var items by mutableStateOf(emptyList<CardItem>())
    private var checked by mutableStateOf(emptySet<Int>())
    private var minimized by mutableStateOf(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        owner.start()
        val channelId = "floating_card"
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channelId, getString(R.string.overlay_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val close = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingCardService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(getString(R.string.overlay_notification))
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.card_close), close)
            .setOngoing(true)
            .build()
        // The specialUse type exists from Android 14 (API 34); older versions must not be passed it.
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        val labels = intent?.getStringArrayExtra(EXTRA_LABELS) ?: emptyArray()
        val values = intent?.getStringArrayExtra(EXTRA_VALUES) ?: emptyArray()
        items = labels.zip(values).map { (l, v) -> CardItem(l, v) }
        checked = emptySet()
        if (composeView == null) attach()
        return START_NOT_STICKY
    }

    private fun attach() {
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                AlokTheme {
                    FloatingCard(
                        title = title,
                        items = items,
                        checked = checked,
                        minimized = minimized,
                        onToggle = { i -> checked = if (i in checked) checked - i else checked + i },
                        onMinimizedChange = { minimized = it },
                        onDrag = { dx, dy -> moveBy(dx, dy) },
                        onClose = { stopSelf() },
                    )
                }
            }
        }
        val density = resources.displayMetrics.density
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Not focusable: the camera app keeps the keyboard/focus. Not touch-modal: touches outside the card reach the camera.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (24 * density).toInt()
            y = (56 * density).toInt()
        }
        windowManager.addView(view, lp)
        composeView = view
        layoutParams = lp
    }

    /** Moves the card, keeping at least its header on screen so it can always be dragged back. */
    private fun moveBy(dx: Float, dy: Float) {
        val view = composeView ?: return
        val lp = layoutParams ?: return
        val screen = resources.displayMetrics
        val keep = (48 * screen.density).toInt()
        lp.x = (lp.x + dx.toInt()).coerceIn(keep - view.width, screen.widthPixels - keep)
        lp.y = (lp.y + dy.toInt()).coerceIn(0, screen.heightPixels - keep)
        windowManager.updateViewLayout(view, lp)
    }

    override fun onDestroy() {
        composeView?.let { runCatching { windowManager.removeView(it) } }
        composeView = null
        owner.stop()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.alokchitri.app.STOP_CARD"
        const val EXTRA_TITLE = "title"
        const val EXTRA_LABELS = "labels"
        const val EXTRA_VALUES = "values"

        fun canDrawOverlays(context: Context) = Settings.canDrawOverlays(context)

        /** Opens the system screen where the user grants "Display over other apps" to this app. */
        fun permissionIntent(context: Context) = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )

        fun start(context: Context, title: String, items: List<CardItem>) {
            val intent = Intent(context, FloatingCardService::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_LABELS, items.map { it.label }.toTypedArray())
                .putExtra(EXTRA_VALUES, items.map { it.value }.toTypedArray())
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingCardService::class.java))
        }
    }
}
