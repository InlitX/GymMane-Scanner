package de.gymmane.scanner.ui

import de.gymmane.scanner.i18n.T

import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.gymmane.scanner.ui.theme.*

@Composable
fun ScanScreen(state: UiState, onScan: (String) -> Unit, onManual: () -> Unit, onRetry: () -> Unit, onDismiss: () -> Unit, active: Boolean = true) {
    var entering by rememberSaveable { mutableStateOf(false) }
    var barcode by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            CameraScanner(enabled = active && state.screen == Screen.SCAN && !state.loading && state.error == null && !entering,
                onBarcode = onScan, modifier = Modifier.fillMaxSize())
            androidx.compose.animation.AnimatedVisibility(state.loading, modifier = Modifier.matchParentSize(), enter = fadeIn(Motion.spec()), exit = fadeOut(Motion.spec(Motion.Exit, true))) { Surface(Modifier.fillMaxSize(), color = Background.copy(alpha = .85f), shape = RoundedCornerShape(28.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 2.5.dp, color = Apricot)
                    Spacer(Modifier.height(14.dp)); Text(T.text("Produkt wird gesucht …"), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        }
        MotionError(state.error, resize = false) {
            if (state.lastBarcode.isNotEmpty()) TextButton(onClick = onRetry) { Text(T.text("Erneut suchen")) }
            TextButton(onClick = onDismiss) { Text(T.text("Weiter scannen")) }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ScanAction(T.text("Barcode eintippen"), Icons.Outlined.Keyboard, !state.loading, Modifier.weight(1f)) { entering = true }
            ScanAction(T.text("Manuell eintragen"), Icons.Outlined.EditNote, !state.loading, Modifier.weight(1f), onManual)
        }
    }
    MotionDialog(if (entering) Unit else null) { _, motion -> AlertDialog(modifier = motion, containerColor = Surface, onDismissRequest = { entering = false }, title = { Text(T.text("Barcode eintippen")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(T.text("Die 8, 12 oder 13 Ziffern unter dem Barcode eingeben."), color = Muted, style = MaterialTheme.typography.bodyMedium)
            AppTextField(barcode, { barcode = it.filter(Char::isDigit).take(13) }, "EAN / UPC-A", numeric = true)
        }
    }, confirmButton = { DialogConfirm(T.text("Produkt suchen"), { entering = false; onScan(barcode) }, enabled = barcode.length in listOf(8, 12, 13)) },
        dismissButton = { DialogDismiss(T.text("Abbrechen")) { entering = false } })
    }
}

@Composable
private fun ScanAction(label: String, icon: ImageVector, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    FilledTonalButton(onClick, modifier.height(52.dp).motionPress(interactions, enabled), enabled = enabled, interactionSource = interactions,
        shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Surface, contentColor = Ink)) {
        Icon(icon, null, Modifier.size(18.dp), tint = Apricot)
        Spacer(Modifier.width(8.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
    }
}
