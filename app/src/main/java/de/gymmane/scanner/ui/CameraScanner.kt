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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode as MlBarcode
import com.google.mlkit.vision.common.InputImage
import de.gymmane.scanner.domain.Barcode
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

    Box(modifier.clip(RoundedCornerShape(28.dp)).background(Surface)) {
        if (permission && cameraError == null) {
            key(retry) {
                CameraPreview(enabled, { onBarcode(it) }, { camera = it }, { cameraError = it }, Modifier.matchParentSize())
            }
            Canvas(Modifier.matchParentSize()) {
                val x = size.width * .12f
                val y = size.height * .30f
                val right = size.width - x
                val bottom = size.height * .62f
                val length = 28.dp.toPx()
                val stroke = 3.5.dp.toPx()
                drawRect(Color.Black.copy(alpha = .22f))
                for ((corner, dx, dy) in listOf(
                    Triple(Offset(x, y), 1, 1), Triple(Offset(right, y), -1, 1),
                    Triple(Offset(x, bottom), 1, -1), Triple(Offset(right, bottom), -1, -1))) {
                    drawLine(Apricot, corner, corner + Offset(length * dx, 0f), stroke, cap = StrokeCap.Round)
                    drawLine(Apricot, corner, corner + Offset(0f, length * dy), stroke, cap = StrokeCap.Round)
                }
            }
            Row(Modifier.align(Alignment.TopStart).padding(14.dp).height(36.dp).background(Background.copy(alpha = .72f), RoundedCornerShape(50)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                val live by animateColorAsState(if (camera != null && enabled) Sage else Muted, Motion.spec(), label = "scannerDot")
                Box(Modifier.size(7.dp).background(live, CircleShape))
                Spacer(Modifier.width(8.dp))
                Crossfade(if (camera == null) T.text("KAMERA WIRD GESTARTET") else if (enabled) T.text("BEREIT ZUM SCANNEN") else T.text("SCAN PAUSIERT"), animationSpec = Motion.spec(), label = "scannerStatus") {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = Ink)
                }
            }
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                FilledIconToggleButton(checked = torch, onCheckedChange = { torch = it }, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(44.dp),
                    colors = IconButtonDefaults.filledIconToggleButtonColors(containerColor = Background.copy(alpha = .72f), contentColor = Ink, checkedContainerColor = Apricot, checkedContentColor = Background)) {
                    Icon(if (torch) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff, if (torch) T.text("Licht ausschalten") else T.text("Licht einschalten"), Modifier.size(20.dp))
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Background.copy(alpha = .88f))))
                .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(T.text("Barcode ausrichten · zum Fokussieren tippen"), style = MaterialTheme.typography.bodySmall, color = Ink.copy(alpha = .85f), textAlign = TextAlign.Center)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ZoomOut, null, tint = Muted, modifier = Modifier.size(18.dp))
                    Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 0f..1f,
                        modifier = Modifier.weight(1f).height(36.dp).padding(horizontal = 8.dp), enabled = camera != null,
                        thumb = { Box(Modifier.size(16.dp).background(Apricot, CircleShape)) },
                        track = { SliderDefaults.Track(sliderState = it, modifier = Modifier.height(4.dp), thumbTrackGapSize = 0.dp, drawStopIndicator = null,
                            colors = SliderDefaults.colors(activeTrackColor = Apricot, inactiveTrackColor = Ink.copy(alpha = .18f))) })
                    Icon(Icons.Outlined.ZoomIn, null, tint = Muted, modifier = Modifier.size(18.dp))
                }
            }
        } else {
            Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(76.dp).background(Field, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CameraAlt, null, tint = Apricot, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(20.dp))
                Text(if (!permission) T.text("Kamera freigeben") else T.text("Kamera nicht verfügbar"), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(cameraError ?: T.text("Die Kamera wird nur zum Lesen von Barcodes verwendet. Es werden keine Kamerabilder gespeichert oder hochgeladen."),
                    color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(22.dp))
                PrimaryButton(if (permission) T.text("Erneut versuchen") else T.text("Kamera erlauben"), {
                    if (permission) { cameraError = null; retry++ }
                    else launcher.launch(Manifest.permission.CAMERA)
                }, Modifier.fillMaxWidth(.8f), icon = if (permission) Icons.Outlined.Refresh else Icons.Outlined.CameraAlt)
                if (!permission) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }) { Text(T.text("App-Einstellungen öffnen"), style = MaterialTheme.typography.labelLarge, color = Muted) }
                }
            }
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
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
        val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(
            MlBarcode.FORMAT_EAN_8, MlBarcode.FORMAT_EAN_13, MlBarcode.FORMAT_UPC_A, MlBarcode.FORMAT_UPC_E).build())
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setResolutionSelector(ResolutionSelector.Builder().setResolutionStrategy(
                ResolutionStrategy(android.util.Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)).build()).build()
        var provider: ProcessCameraProvider? = null
        var failures = 0
        analysis.setAnalyzer(executor) { proxy ->
            val image = proxy.image
            if (disposed.get() || !latestEnabled || delivered.get() || image == null) { proxy.close() }
            else {
                try {
                    scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener(main) { barcodes ->
                            if (!disposed.get() && latestEnabled) {
                                failures = 0
                                val code = barcodes.mapNotNull { barcode ->
                                    barcode.rawValue?.let { if (barcode.format == MlBarcode.FORMAT_UPC_E) Barcode.expandUpcE(it) else Barcode.normalize(it) }
                                }.firstOrNull()
                                if (code != null && delivered.compareAndSet(false, true)) latestBarcode(code)
                            }
                        }.addOnFailureListener(main) {
                            if (!disposed.get() && ++failures >= 5) latestError(T.text("Die Barcode-Erkennung konnte nicht gestartet werden. Bitte Kamera erneut öffnen oder den Code eintippen."))
                        }.addOnCompleteListener { proxy.close() }
                } catch (_: Exception) { proxy.close() }
            }
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
            scanner.close()
            executor.shutdown()
        }
    }
    LaunchedEffect(enabled) {
        if (enabled) delivered.set(false)
    }
}
