package de.gymmane.scanner.ui

import de.gymmane.scanner.updates.*
import de.gymmane.scanner.i18n.T
import de.gymmane.scanner.data.ScannerSettings
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import de.gymmane.scanner.integration.GymManeSender
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.filterNotNull
import de.gymmane.scanner.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ScannerApp(viewModel: ScannerViewModel, updates: UpdateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val updateState by updates.state.collectAsStateWithLifecycle()
    UpdateInstallEffects(updates)
    if (updateState.whatsNew) {
        LaunchedEffect(Unit) { updates.markWhatsNewSeen() }
        WhatsNewScreen(updateState, updates::closeWhatsNew)
        return
    }
    LaunchedEffect(updateState.status) {
        if (state.screen != Screen.SETTINGS) updateState.status?.let { snackbar.showSnackbar(context.getString(it)) }
    }
    val preferences = remember { ScannerSettings(context) }
    var onboarding by rememberSaveable { mutableStateOf(!preferences.onboardingComplete) }
    var previousScreen by rememberSaveable { mutableStateOf(Screen.SCAN) }
    fun language(code: String) {
        if (code == preferences.language) return
        preferences.language = code
        T.configure(context, code)
        (context as? android.app.Activity)?.recreate()
    }
    if (onboarding) {
        OnboardingScreen(preferences.language, ::language) {
            preferences.onboardingComplete = true
            onboarding = false
        }
        return
    }
    val owner = LocalLifecycleOwner.current
    val sender = remember(context) { GymManeSender(context) }
    LaunchedEffect(viewModel, owner, sender) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.state.map { it.pendingSend }.filterNotNull().collect { request ->
                viewModel.consumeSend(request.id)?.let {
                    val result = sender.send(it.json)
                    result.error?.let(viewModel::sendFailed)
                }
            }
        }
    }
    val copy: (String, String) -> Unit = { label, value ->
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText(label, value))
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(T.format("%1\$s kopiert: %2\$s", label, value), duration = SnackbarDuration.Short) }
    }
    BackHandler(enabled = state.screen != Screen.SCAN || state.loading) { viewModel.navigate(if (state.screen == Screen.SETTINGS) previousScreen else Screen.SCAN) }
    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (state.screen == Screen.PRODUCT || state.screen == Screen.MANUAL || state.screen == Screen.SETTINGS) {
                    FilledIconButton(onClick = { viewModel.navigate(if (state.screen == Screen.SETTINGS) previousScreen else Screen.SCAN) }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Surface)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, T.text("Zurück zum Scanner"), Modifier.size(20.dp))
                    }
                } else {
                    Box(Modifier.size(44.dp).background(Surface, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.CenterFocusStrong, null, tint = Apricot, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(T.text(when (state.screen) { Screen.SETTINGS -> T.text("Einstellungen"); Screen.PRODUCT -> T.text("Lebensmittel"); Screen.HISTORY -> T.text("Deine Lebensmittel"); Screen.MANUAL -> if (state.editing == null) T.text("Neues Lebensmittel") else T.text("Lebensmittel bearbeiten"); else -> "GymMane" }), style = MaterialTheme.typography.titleLarge)
                    
                }
                if (state.screen != Screen.SETTINGS) IconButton(onClick = { previousScreen = state.screen; viewModel.navigate(Screen.SETTINGS) }) { Icon(Icons.Outlined.Settings, T.text("Einstellungen"), tint = Muted, modifier = Modifier.size(22.dp)) }
            }
        },
        bottomBar = {
            if (state.screen == Screen.SCAN || state.screen == Screen.HISTORY) {
                Row(Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 36.dp, vertical = 12.dp)
                    .background(Surface, RoundedCornerShape(40.dp)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Screen.SCAN to T.text("Scannen"), Screen.HISTORY to T.text("Verlauf")).forEach { (screen, label) ->
                        val selected = state.screen == screen
                        val tabColor by animateColorAsState(if (selected) Ink else Muted, Motion.spec(), label = "tab")
                        FilledTonalButton(onClick = { viewModel.navigate(screen) }, modifier = Modifier.weight(1f).height(49.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Surface, contentColor = tabColor)) {
                            Icon(if (screen == Screen.SCAN) Icons.Outlined.QrCodeScanner else Icons.Outlined.History, null, Modifier.size(19.dp))
                            Spacer(Modifier.width(8.dp)); Text(T.text(label))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val screen = state.screen
            ScreenEntrance(screen, enabled = screen != Screen.SCAN) {
            when (screen) {
                Screen.SETTINGS -> SettingsScreen(preferences.language, state.autoSend, ::language, viewModel::setAutoSend,
                    { onboarding = true },
                    { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(de.gymmane.scanner.BuildConfig.REPOSITORY_URL))) },
                    { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://world.openfoodfacts.org/terms-of-use"))) }, updates = { UpdateSettings(updates) })
                Screen.SCAN -> ScanScreen(state, onScan = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.lookup(it)
                }, onManual = { viewModel.manual() }, onRetry = { viewModel.lookup(state.lastBarcode) }, onDismiss = viewModel::clearError, active = updateState.offer == null)
                Screen.HISTORY -> HistoryScreen(state, viewModel::open, viewModel::delete, { viewModel.navigate(if (state.screen == Screen.SETTINGS) previousScreen else Screen.SCAN) })
                Screen.PRODUCT -> state.product?.let { product ->
                    ProductScreen(product, state.cached, state.loading, state.error, copy,
                        { product.barcode?.let { viewModel.lookup(it, refresh = true) } },
                        { viewModel.navigate(if (state.screen == Screen.SETTINGS) previousScreen else Screen.SCAN) }, { viewModel.manual(product) }, onSend = viewModel::send)
                }
                Screen.MANUAL -> ManualScreen(state.editing, state.lastBarcode, state.loading, state.error, viewModel::save,
                    { viewModel.navigate(if (state.screen == Screen.SETTINGS) previousScreen else Screen.SCAN) }, forSending = state.editingForSend)
            }
            }
        }
    }
    UpdateOffer(updates, allowed = !state.loading && state.pendingSend == null && state.screen != Screen.MANUAL && state.correction == null)
    MotionDialog(state.correction?.let { it to state.correctionError }) { correction, motion -> AlertDialog(modifier = motion,onDismissRequest = viewModel::dismissCorrection,
        title = { Text(T.text("Angaben für GymMane ergänzen")) },
        text = { Text(T.text(correction.second.orEmpty())) },
        confirmButton = { TextButton(onClick = viewModel::editForSending) { Text(T.text("Werte bearbeiten")) } },
        dismissButton = { TextButton(onClick = viewModel::dismissCorrection) { Text(T.text("Abbrechen")) } })
    }
}
