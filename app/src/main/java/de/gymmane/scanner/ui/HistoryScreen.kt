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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import de.gymmane.scanner.domain.Product
import de.gymmane.scanner.domain.formatNumber
import de.gymmane.scanner.ui.theme.*

@Composable
fun HistoryScreen(state: UiState, onOpen: (Product) -> Unit, onDelete: (Product) -> Unit, onScan: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Product?>(null) }
    val errorVisibility = rememberMotionVisibility(state.error != null)
    val filtered = state.history.filter { query.isBlank() || "${it.name} ${it.brand.orEmpty()} ${it.barcode.orEmpty()} ${it.category.orEmpty()}".contains(query, ignoreCase = true) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            TextField(query, { query = it.take(100) }, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text(T.text("Name oder Barcode suchen"), style = MaterialTheme.typography.bodyLarge, color = Muted) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = Muted, modifier = Modifier.size(20.dp)) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, T.text("Suche leeren"), tint = Muted, modifier = Modifier.size(18.dp)) } },
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(unfocusedContainerColor = Surface, focusedContainerColor = Surface,
                    unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, cursorColor = Apricot))
        }
        if (state.history.isNotEmpty()) item {
            SectionLabel(T.format("%1\$s gespeicherte Lebensmittel", state.history.size), Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp))
        }
        if (errorVisibility.currentState || errorVisibility.targetState) item { MotionError(state.error, errorVisibility) }
        if (filtered.isEmpty()) item(key = "history_empty") {
            EmptyCard(Icons.Outlined.History, if (query.isEmpty()) T.text("Dein Verlauf wartet auf dich") else T.text("Kein Treffer"), if (query.isEmpty()) T.text("Scanne dein erstes Produkt oder trage ein Lebensmittel selbst ein.") else T.text("Versuche einen anderen Namen oder Barcode."), modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) {
                if (query.isEmpty()) PrimaryButton(T.text("Erstes Produkt scannen"), onScan) else TextButton(onClick = { query = "" }) { Text(T.text("Suche zurücksetzen")) }
            }
        }
        items(filtered, key = { it.id }) { product ->
            val interaction = remember(product.id) { MutableInteractionSource() }
            Surface(modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true)).motionPress(interaction), interactionSource = interaction, onClick = { onOpen(product) }, color = Surface, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProductImage(product, Modifier.size(64.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(product.brand ?: product.category ?: if (product.manual) T.text("Manuell eingetragen") else "Open Food Facts", style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val kcal = product.per100.kcal
                        Text(buildAnnotatedString {
                            if (kcal != null) {
                                withStyle(SpanStyle(color = Ink, fontWeight = FontWeight.ExtraBold)) { append(T.number(kcal)) }
                                append(" kcal · 100 ${product.unit.symbol}")
                            } else append(T.text("Kalorien nicht angegeben"))
                        }, style = MaterialTheme.typography.bodySmall, color = Apricot, modifier = Modifier.padding(top = 2.dp))
                    }
                    IconButton(onClick = { deleting = product }) { Icon(Icons.Outlined.DeleteOutline, T.format("%1\$s aus Verlauf löschen", product.name), tint = Muted, modifier = Modifier.size(20.dp)) }
                }
            }
        }
        if (state.history.isNotEmpty()) item {
            Text(T.text("Die letzten 100 gescannten Produkte und alle manuellen Einträge werden lokal gespeichert. Tippe auf ein Produkt, um seine Werte zu kopieren."),
                color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp))
        }
    }
    MotionDialog(deleting) { product, motion ->
        AlertDialog(modifier = motion, containerColor = Surface, textContentColor = Muted, onDismissRequest = { deleting = null }, title = { Text(T.text("Eintrag löschen?")) },
            text = { Text(T.text(if (product.manual) "„${product.name}“ wird mit deinen manuell eingegebenen Werten von diesem Gerät entfernt." else "„${product.name}“ wird aus dem Offline-Verlauf entfernt. Du kannst es später erneut scannen.")) },
            confirmButton = { DialogConfirm(T.text("Löschen"), { onDelete(product); deleting = null }) },
            dismissButton = { DialogDismiss(T.text("Behalten")) { deleting = null } })
    }
}
