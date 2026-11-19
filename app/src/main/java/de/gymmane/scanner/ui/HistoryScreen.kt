package de.gymmane.scanner.ui

import de.gymmane.scanner.i18n.T

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.gymmane.scanner.domain.Product
import de.gymmane.scanner.domain.formatNumber
import de.gymmane.scanner.ui.theme.*

@Composable
fun HistoryScreen(state: UiState, onOpen: (Product) -> Unit, onDelete: (Product) -> Unit, onScan: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Product?>(null) }
    val errorVisibility = rememberMotionVisibility(state.error != null)
    val filtered = state.history.filter { query.isBlank() || "${it.name} ${it.brand.orEmpty()} ${it.barcode.orEmpty()} ${it.category.orEmpty()}".contains(query, ignoreCase = true) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(T.format("%1\$s gespeicherte Lebensmittel", state.history.size), color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        item {
            OutlinedTextField(query, { query = it.take(100) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text(T.text("Name oder Barcode suchen")) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, T.text("Suche leeren")) } },
                shape = RoundedCornerShape(50), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Surface, focusedContainerColor = Surface, unfocusedBorderColor = Outline))
        }
        if (errorVisibility.currentState || errorVisibility.targetState) item { MotionError(state.error, errorVisibility) }
        if (filtered.isEmpty()) item(key = "history_empty") {
            EmptyCard(Icons.Outlined.History, if (query.isEmpty()) T.text("Dein Verlauf wartet auf dich") else T.text("Kein Treffer"), if (query.isEmpty()) T.text("Scanne dein erstes Produkt oder trage ein Lebensmittel selbst ein.") else T.text("Versuche einen anderen Namen oder Barcode."), modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) {
                if (query.isEmpty()) PrimaryButton(T.text("Erstes Produkt scannen"), onScan) else TextButton(onClick = { query = "" }) { Text(T.text("Suche zurücksetzen")) }
            }
        }
        items(filtered, key = { it.id }) { product ->
            val interaction = remember(product.id) { MutableInteractionSource() }
            Surface(modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true)).motionPress(interaction), interactionSource = interaction, onClick = { onOpen(product) }, color = Surface, shape = RoundedCornerShape(25.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProductImage(product, Modifier.size(68.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(product.name, style = MaterialTheme.typography.titleMedium)
                        Text(product.brand ?: product.category ?: if (product.manual) T.text("Manuell eingetragen") else "Open Food Facts", style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1)
                        Text("${product.per100.kcal?.let { "${T.number(it)} kcal" } ?: T.text("Kalorien nicht angegeben")} · 100 ${product.unit.symbol}", style = MaterialTheme.typography.bodySmall, color = Apricot)
                    }
                    IconButton(onClick = { deleting = product }) { Icon(Icons.Outlined.DeleteOutline, T.format("%1\$s aus Verlauf löschen", product.name), tint = Muted, modifier = Modifier.size(20.dp)) }
                }
            }
        }
        if (state.history.isNotEmpty()) item { Text("Die letzten 100 gescannten Produkte und alle manuellen Einträge werden lokal gespeichert. Tippe auf ein Produkt, um seine Werte zu kopieren.", color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
    MotionDialog(deleting) { product, motion ->
        AlertDialog(modifier = motion,onDismissRequest = { deleting = null }, title = { Text(T.text("Eintrag löschen?")) },
            text = { Text(T.text(if (product.manual) "„${product.name}“ wird mit deinen manuell eingegebenen Werten von diesem Gerät entfernt." else "„${product.name}“ wird aus dem Offline-Verlauf entfernt. Du kannst es später erneut scannen.")) },
            confirmButton = { TextButton(onClick = { onDelete(product); deleting = null }) { Text(T.text("Löschen")) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(T.text("Behalten")) } })
    }
}
