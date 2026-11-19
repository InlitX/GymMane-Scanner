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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.gymmane.scanner.ui.theme.*

@Composable
fun ScanScreen(state: UiState, onScan: (String) -> Unit, onManual: () -> Unit, onRetry: () -> Unit, onDismiss: () -> Unit, active: Boolean = true) {
    var entering by rememberSaveable { mutableStateOf(false) }
    var barcode by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(T.text("Barcode scannen"), style = MaterialTheme.typography.headlineSmall)
        Text(T.text("Barcode in den Rahmen halten"), color = Muted, style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            CameraScanner(enabled = active && state.screen == Screen.SCAN && !state.loading && state.error == null && !entering,
                onBarcode = onScan, modifier = Modifier.fillMaxSize())
            androidx.compose.animation.AnimatedVisibility(state.loading, modifier = Modifier.matchParentSize(), enter = fadeIn(Motion.spec()), exit = fadeOut(Motion.spec(Motion.Exit, true))) { Surface(Modifier.fillMaxSize(), color = Background.copy(alpha = .85f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.height(12.dp)); Text(T.text("Produkt wird gesucht …"))
                }
            }
        }
        }
        MotionError(state.error, resize = false) {
            if (state.lastBarcode.isNotEmpty()) TextButton(onClick = onRetry) { Text(T.text("Erneut suchen")) }
            TextButton(onClick = onDismiss) { Text(T.text("Weiter scannen")) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { entering = true }, enabled = !state.loading) { Text(T.text("Barcode eintippen")) }
            TextButton(onClick = onManual, enabled = !state.loading) { Text(T.text("Manuell eintragen")) }
        }
    }
    MotionDialog(if (entering) Unit else null) { _, motion -> AlertDialog(modifier = motion,onDismissRequest = { entering = false }, title = { Text(T.text("Barcode eintippen")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(T.text("Die 8, 12 oder 13 Ziffern unter dem Barcode eingeben."), color = Muted)
            OutlinedTextField(value = barcode, onValueChange = { barcode = it.filter(Char::isDigit).take(13) },
                label = { Text("EAN / UPC-A") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { entering = false; onScan(barcode) }, enabled = barcode.length in listOf(8, 12, 13)) { Text(T.text("Produkt suchen")) } },
        dismissButton = { TextButton(onClick = { entering = false }) { Text(T.text("Abbrechen")) } })
    }
}
