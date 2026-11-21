package de.gymmane.scanner.ui

import de.gymmane.scanner.i18n.T

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.gymmane.scanner.domain.BarcodeDecoder
import de.gymmane.scanner.ui.theme.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CameraScanner(enabled: Boolean, onBarcode: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    fun granted() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    var permission by remember { mutableStateOf(granted()) }
    var requested by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    LaunchedEffect(Unit) {
        if (!permission && !requested) { requested = true; launcher.launch(Manifest.permission.CAMERA) }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) permission = granted() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var torch by rememberSaveable { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(0f) }
    DisposableEffect(camera, lifecycleOwner) {
        val info = camera?.cameraInfo
        val observer = Observer<CameraState> { state ->
            if (state.error != null) cameraError = T.text("Die Kamera ist gerade nicht verfügbar. Schließe andere Kamera-Apps und versuche es erneut.")
        }
        info?.cameraState?.observe(lifecycleOwner, observer)
        onDispose { info?.cameraState?.removeObserver(observer) }
    }
    LaunchedEffect(camera, torch) { camera?.cameraControl?.enableTorch(torch) }
    LaunchedEffect(camera, zoom) { camera?.cameraControl?.setLinearZoom(zoom) }

    Box(modifier.clip(RoundedCornerShape(30.dp)).background(Surface)) {
        if (permission && cameraError == null) {
            key(retry) {
                CameraPreview(enabled, { onBarcode(it) }, { camera = it }, { cameraError = it }, Modifier.matchParentSize())
            }
            Canvas(Modifier.matchParentSize()) {
                val x = size.width * .10f
                val y = size.height * .27f
                val right = size.width - x
                val bottom = size.height * .68f
                val length = 25.dp.toPx()
                val stroke = 3.dp.toPx()
                drawRect(Color.Black.copy(alpha = .18f))
                for ((corner, dx, dy) in listOf(
                    Triple(Offset(x, y), 1, 1), Triple(Offset(right, y), -1, 1),
                    Triple(Offset(x, bottom), 1, -1), Triple(Offset(right, bottom), -1, -1))) {
                    drawLine(Apricot, corner, corner + Offset(length * dx, 0f), stroke)
                    drawLine(Apricot, corner, corner + Offset(0f, length * dy), stroke)
                }
            }
            Row(Modifier.align(Alignment.TopStart).padding(18.dp).background(Background.copy(alpha = .75f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CenterFocusStrong, null, tint = Sage, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Crossfade(if (camera == null) T.text("KAMERA WIRD GESTARTET") else if (enabled) T.text("BEREIT ZUM SCANNEN") else T.text("SCAN PAUSIERT"), animationSpec = Motion.spec(), label = "scannerStatus") {
                    Text(it, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                FilledIconToggleButton(checked = torch, onCheckedChange = { torch = it }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp), colors = IconButtonDefaults.filledIconToggleButtonColors(containerColor = Background.copy(alpha = .75f), checkedContainerColor = Apricot)) {
                    Icon(if (torch) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff, if (torch) T.text("Licht ausschalten") else T.text("Licht einschalten"))
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Background.copy(alpha = .7f)).padding(horizontal = 24.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(T.text("Barcode ausrichten · zum Fokussieren tippen"), style = MaterialTheme.typography.bodySmall)
                Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 0f..1f,
                    modifier = Modifier.height(32.dp), enabled = camera != null,
                    thumb = { Box(Modifier.size(14.dp).background(Apricot, CircleShape)) },
                    track = { SliderDefaults.Track(sliderState = it, modifier = Modifier.height(4.dp)) })
            }
        } else {
            Column(Modifier.align(Alignment.Center).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.CameraAlt, null, tint = Apricot, modifier = Modifier.size(38.dp))
                Text(if (!permission) T.text("Kamera freigeben") else T.text("Kamera nicht verfügbar"), style = MaterialTheme.typography.titleMedium)
                Text(cameraError ?: T.text("Die Kamera wird nur zum Lesen von Barcodes verwendet. Es werden keine Kamerabilder gespeichert oder hochgeladen."), color = Muted, style = MaterialTheme.typography.bodyMedium)
                Button(onClick = {
                    if (permission) { cameraError = null; retry++ }
                    else launcher.launch(Manifest.permission.CAMERA)
                }) { Text(if (permission) T.text("Erneut versuchen") else T.text("Kamera erlauben")) }
                if (!permission) TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }) { Text(T.text("App-Einstellungen öffnen")) }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    enabled: Boolean,
    onBarcode: (String) -> Unit,
    onCamera: (Camera?) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latestEnabled by rememberUpdatedState(enabled)
    val latestBarcode by rememberUpdatedState(onBarcode)
    val latestError by rememberUpdatedState(onError)
    val delivered = remember { AtomicBoolean(false) }
    val previewView = remember { PreviewView(context).apply { implementationMode = PreviewView.ImplementationMode.PERFORMANCE; scaleType = PreviewView.ScaleType.FILL_CENTER } }
    AndroidView(factory = { previewView }, modifier = modifier)
    DisposableEffect(owner, previewView) {
        val disposed = AtomicBoolean(false)
        val executor = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        val decoder = BarcodeDecoder()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
                ResolutionStrategy(android.util.Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build()).build()
        var provider: ProcessCameraProvider? = null
        analysis.setAnalyzer(executor) { proxy ->
            try {
                if (!disposed.get() && latestEnabled && !delivered.get()) {
                    val code = decoder.decode(proxy.luminance(), proxy.width, proxy.height, proxy.imageInfo.rotationDegrees)
                    if (code != null) main.execute {
                        if (!disposed.get() && latestEnabled && delivered.compareAndSet(false, true)) latestBarcode(code)
                    }
                }
            } catch (_: Exception) {
            } finally { proxy.close() }
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!disposed.get()) {
                try {
                    provider = future.get()
                    val camera = provider!!.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                    onCamera(camera)
                    previewView.setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            view.performClick()
                            val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                            camera.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                        }
                        true
                    }
                } catch (_: Exception) { latestError(T.text("Die Kamera konnte nicht geöffnet werden. Prüfe, ob eine andere App sie verwendet.")) }
            }
        }, main)
        onDispose {
            disposed.set(true)
            previewView.setOnTouchListener(null)
            analysis.clearAnalyzer()
            provider?.unbind(preview, analysis)
            onCamera(null)
            executor.shutdown()
        }
    }
    LaunchedEffect(enabled) {
        if (enabled) delivered.set(false)
    }
}

/** Copies the Y (brightness) plane into a tight width x height array, dropping row padding. */
private fun ImageProxy.luminance(): ByteArray {
    val plane = planes[0]
    val buffer = plane.buffer.duplicate().apply { rewind() }
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val out = ByteArray(width * height)
    if (pixelStride == 1 && rowStride == width) {
        buffer.get(out, 0, out.size)
        return out
    }
    val row = ByteArray(rowStride)
    for (y in 0 until height) {
        buffer.position(y * rowStride)
        buffer.get(row, 0, minOf(rowStride, buffer.remaining()))
        for (x in 0 until width) out[y * width + x] = row[x * pixelStride]
    }
    return out
}
