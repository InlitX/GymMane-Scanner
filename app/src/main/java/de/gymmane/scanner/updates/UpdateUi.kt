package de.gymmane.scanner.updates

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import de.gymmane.scanner.BuildConfig
import de.gymmane.scanner.R
import de.gymmane.scanner.ui.*
import de.gymmane.scanner.ui.theme.*
import kotlinx.coroutines.flow.*

fun apkInstallIntent(context: android.content.Context, apk: java.io.File): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
    return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).apply { clipData = ClipData.newRawUri("update", uri) }
}

@Composable
fun UpdateInstallEffects(viewModel: UpdateViewModel) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.packageManager.canRequestPackageInstalls()) viewModel.resumePermission()
        else viewModel.installFailed(permission = true)
    }
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.state.map { it.installReady }.distinctUntilChanged().filter { it }.collect {
                val apk = viewModel.consumeInstall() ?: return@collect
                try {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        if (viewModel.awaitingPermission) viewModel.installFailed(permission = true)
                        else {
                            viewModel.awaitPermission()
                            permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                        }
                    } else {
                        context.startActivity(apkInstallIntent(context, apk))
                        viewModel.installerOpened()
                    }
                } catch (_: Exception) { viewModel.installFailed() }
            }
        }
    }
}

@Composable
fun UpdateSettings(viewModel: UpdateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HorizontalDivider(color = Outline)
    SectionLabel(stringResource(R.string.update_section))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.update_auto), Modifier.weight(1f))
        Switch(state.automatic, viewModel::automatic)
    }
    TextButton(onClick = { viewModel.check(true) }, enabled = !state.checking && !state.downloading) {
        Text(stringResource(if (state.checking) R.string.update_checking else R.string.update_check))
    }
    state.status?.let { Text(stringResource(it), color = Muted, style = MaterialTheme.typography.bodySmall) }
}

@Composable
fun UpdateOffer(viewModel: UpdateViewModel, allowed: Boolean) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MotionDialog(state.offer?.takeIf { allowed }) { release, motion ->
        AlertDialog(modifier = motion, onDismissRequest = viewModel::later,
            title = { Text(stringResource(R.string.update_available, release.tag)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel(stringResource(R.string.update_whats_new))
                    Text(release.notes.ifBlank { stringResource(R.string.update_no_notes) }.take(30_000),
                        Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()))
                    if (state.downloading) {
                        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.update_downloading), style = MaterialTheme.typography.bodySmall)
                    }
                    state.downloadError?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(viewModel::update, enabled = !state.downloading) { Text(stringResource(R.string.update_action)) } },
            dismissButton = { TextButton(viewModel::later) { Text(stringResource(R.string.update_later)) } })
    }
}

@Composable
fun WhatsNewScreen(state: UpdateState, onContinue: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = Background, contentColor = Ink) {
        ScreenEntrance("whats-new") {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
                Text(stringResource(R.string.update_whats_new), style = MaterialTheme.typography.headlineLarge)
                Text(BuildConfig.VERSION_NAME, color = Apricot, modifier = Modifier.padding(top = 8.dp, bottom = 24.dp))
                Text(state.notes?.takeIf { it.isNotBlank() }?.take(30_000) ?: stringResource(R.string.update_local_notes),
                    Modifier.weight(1f).verticalScroll(rememberScrollState()))
                PrimaryButton(stringResource(R.string.update_done), onContinue, Modifier.fillMaxWidth().padding(top = 20.dp))
            }
        }
    }
}
