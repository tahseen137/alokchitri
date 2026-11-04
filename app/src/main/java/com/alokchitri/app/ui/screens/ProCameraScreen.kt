package com.alokchitri.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.alokchitri.app.R
import com.alokchitri.app.camera.ProCameraControl
import com.alokchitri.app.camera.ProCameraHost
import com.alokchitri.app.ui.components.BackBar
import com.alokchitri.app.ui.components.InfoBanner
import com.alokchitri.app.ui.label
import com.alokchitri.app.ui.theme.AlokColors
import com.alokchitri.app.ui.theme.AlokShapes
import com.alokchitri.app.ui.titleRes
import com.alokchitri.engine.DeviceLimits
import com.alokchitri.engine.Recommendation
import com.alokchitri.engine.ShotGoal
import com.alokchitri.engine.Stability

/**
 * The one-tap Pro setup: Alokchitri's own camera, opened with the recommended ISO, shutter, white balance,
 * focus and lens already set. The user frames the shot and presses the shutter; the photo goes to the gallery.
 */
@Composable
fun ProCameraScreen(
    goal: ShotGoal,
    rec: Recommendation,
    device: DeviceLimits,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val control = remember { ProCameraControl() }
    var live by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var lastPhoto by remember { mutableStateOf<Uri?>(null) }
    val saved = stringResource(R.string.pro_saved)
    val failed = stringResource(R.string.pro_failed)
    val storageNeeded = stringResource(R.string.pro_storage_needed)

    val shoot: () -> Unit = {
        busy = true
        control.capture { uri ->
            busy = false
            if (uri != null) lastPhoto = uri
            Toast.makeText(context, if (uri != null) saved else failed, Toast.LENGTH_SHORT).show()
        }
    }
    // Android 8-9 only: MediaStore writes need the old storage permission there.
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) shoot() else Toast.makeText(context, storageNeeded, Toast.LENGTH_SHORT).show()
    }
    val onShutter: () -> Unit = {
        val needsStorage = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsStorage) storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else shoot()
    }

    Box(Modifier.fillMaxSize().background(AlokColors.ViewfinderDeep)) {
        ProCameraHost(rec, device, control, onReady = { live = it }, onManual = { manual = it })

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.clip(CircleShape).background(AlokColors.Ground)) { BackBar(onBack) }
                Box(
                    Modifier.clip(CircleShape).background(AlokColors.AmberTint).padding(horizontal = 16.dp, vertical = 8.dp),
                ) { Text(stringResource(goal.titleRes()), style = MaterialTheme.typography.labelMedium, color = AlokColors.Text) }
            }
            AppliedChips(rec, manual)
            if (!manual) {
                InfoBanner(
                    Icons.Outlined.Info,
                    AlokColors.Amber,
                    stringResource(R.string.pro_auto_exposure),
                    Modifier.padding(horizontal = 20.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            Column(
                Modifier.fillMaxWidth().clip(AlokShapes.Sheet).background(AlokColors.Surface)
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(if (rec.stability == Stability.HANDHELD_OK) R.string.pro_title else R.string.pro_hold_still),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (rec.stability == Stability.HANDHELD_OK) AlokColors.Green else AlokColors.Amber,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.size(16.dp))
                Box(Modifier.fillMaxWidth()) {
                    lastPhoto?.let { uri -> LastPhoto(uri, Modifier.align(Alignment.CenterStart)) }
                    ShutterButton(enabled = live && !busy, busy = busy, onClick = onShutter, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

/** The values that were applied, as chips over the viewfinder. ISO and shutter only when the phone took them. */
@Composable
private fun AppliedChips(rec: Recommendation, manual: Boolean) {
    val chips = buildList {
        if (manual) {
            add("ISO ${rec.iso}")
            add(rec.shutterLabel)
        }
        add("${rec.whiteBalanceK}K")
        add(rec.focus.label())
        add(rec.lens.label())
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            Box(Modifier.clip(CircleShape).background(AlokColors.Ground).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(chip, style = MaterialTheme.typography.labelSmall, color = AlokColors.Amber)
            }
        }
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cd = stringResource(R.string.pro_shutter_cd)
    Box(
        modifier.size(76.dp).clip(CircleShape).border(4.dp, if (enabled) AlokColors.Text else AlokColors.OutlineStrong, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(color = AlokColors.Amber, modifier = Modifier.size(40.dp))
        } else {
            Box(Modifier.size(58.dp).clip(CircleShape).background(if (enabled) AlokColors.Amber else AlokColors.OutlineStrong))
        }
    }
}

/** Thumbnail of the last photo; tap opens it in the phone's gallery app. */
@Composable
private fun LastPhoto(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cd = stringResource(R.string.pro_view_cd)
    AsyncImage(
        model = uri,
        contentDescription = cd,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(52.dp).clip(MaterialTheme.shapes.small).border(1.dp, AlokColors.Outline, MaterialTheme.shapes.small)
            .clickable {
                val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/jpeg").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                runCatching { context.startActivity(view) }
            },
    )
}
