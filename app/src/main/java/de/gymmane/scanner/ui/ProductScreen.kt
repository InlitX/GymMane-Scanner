package de.gymmane.scanner.ui

import de.gymmane.scanner.i18n.T

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import de.gymmane.scanner.domain.*
import de.gymmane.scanner.ui.theme.*
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun ProductImage(product: Product, modifier: Modifier = Modifier) {
    val source = product.localImage?.let(::File)?.takeIf { it.exists() } ?: product.imageUrl
    val context = LocalContext.current
    val reducedMotion = LocalReducedMotion.current
    val request = remember(source, context, reducedMotion) {
        ImageRequest.Builder(context).data(source).apply {
            if (reducedMotion) crossfade(false) else crossfade(Motion.Enter)
        }.build()
    }
    Surface(modifier, color = Field, shape = RoundedCornerShape(24.dp)) {
        if (source != null) SubcomposeAsyncImage(model = request, contentDescription = T.format("Produktbild: %1\$s", product.name),
            contentScale = ContentScale.Fit, modifier = Modifier.padding(12.dp),
            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) } },
            error = { MissingImage() })
        else MissingImage()
    }
}

@Composable
private fun MissingImage() {
    Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.Restaurant, null, tint = Apricot, modifier = Modifier.size(30.dp))
        Text(T.text("Kein Bild"), color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ProductScreen(product: Product, cached: Boolean, loading: Boolean, error: String?, onCopy: (String, String) -> Unit, onRefresh: () -> Unit, onScan: () -> Unit, onEdit: () -> Unit, onSend: ((Product) -> Unit)? = null) {
    var portion by rememberSaveable(product.id) { mutableStateOf(false) }
    var amountText by rememberSaveable(product.id, product.fetchedAt) { mutableStateOf(product.servingAmount?.let(T::number).orEmpty()) }
    val originalServing = T.text(Serving.name(product))
    var servingText by rememberSaveable(product.id, product.fetchedAt) { mutableStateOf(originalServing) }
    var details by rememberSaveable(product.id) { mutableStateOf(false) }
    var expanded by rememberSaveable(product.id) { mutableStateOf(false) }
    val amount = parseNumber(amountText)?.takeIf { it > 0 }
    val invalid = amountText.isNotBlank() && amount == null
    val canPortion = !invalid && (amount != null || product.perServing.hasValues)
    val errorVisibility = rememberMotionVisibility(error != null)
    val n = if (portion && !canPortion) Nutrients() else product.nutrients(portion, amount)
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProductImage(product, Modifier.size(72.dp))
                    Column(Modifier.weight(1f)) {
                        product.brand?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
                        Text(product.name, style = MaterialTheme.typography.headlineSmall)
                        if (cached) Text(T.text("Offline gespeichert"), color = Sage, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { onCopy(T.text("Name"), product.name) }) { Icon(Icons.Outlined.ContentCopy, T.text("Name kopieren"), tint = Muted, modifier = Modifier.size(18.dp)) }
                }
            }
            if (errorVisibility.currentState || errorVisibility.targetState) item { MotionError(error, errorVisibility) }
            item { PillSwitch(listOf(T.text("pro Portion"), "100 ${product.unit.symbol}"), if (portion) 0 else 1,
                { portion = it == 0 }, Modifier.fillMaxWidth(), { it != 0 || canPortion }) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CopyField("kcal", n.kcal?.let(T::number), onCopy, Modifier.weight(1f), unit = "kcal", large = true)
                        CopyField(T.text("Eiweiß"), n.protein?.let(T::number), onCopy, Modifier.weight(1f), color = Apricot, unit = "g", large = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CopyField(T.text("Kohlenhydrate"), n.carbs?.let(T::number), onCopy, Modifier.weight(1f), color = Gold, unit = "g", large = true)
                        CopyField(T.text("Fett"), n.fat?.let(T::number), onCopy, Modifier.weight(1f), color = Sage, unit = "g", large = true)
                    }
                }
            }
            item { TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) T.text("Weniger Nährwerte") else T.text("Weitere Nährwerte")) } }
            item {
                AnimatedVisibility(expanded, enter = Motion.enter(), exit = Motion.exit()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CopyField(T.text("Ballaststoffe"), n.fiber?.let(T::number), onCopy, Modifier.fillMaxWidth(), unit = "g")
                    CopyField(T.text("Zucker"), n.sugar?.let(T::number), onCopy, Modifier.fillMaxWidth(), unit = "g")
                    CopyField(T.text("Natrium"), n.sodium?.let(T::number), onCopy, Modifier.fillMaxWidth(), unit = "g")
                }
            }
            }
            item { TextButton(onClick = { details = !details }) { Text(if (details) T.text("Details schließen") else T.text("Portion & Produktdetails")) } }
            item {
                AnimatedVisibility(details, enter = Motion.enter(), exit = Motion.exit()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(servingText, { servingText = it.take(100) },
                        label = { Text(T.text("Bezeichnung, z. B. 1 Messlöffel")) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(amountText, {
                        amountText = it.take(12)
                        if (parseNumber(amountText) != product.servingAmount && servingText == originalServing) {
                            servingText = T.text(Serving.DEFAULT_NAME)
                        }
                    }, label = { Text(T.format("Portionsgröße in %1\$s", product.unit.symbol)) },
                        singleLine = true, isError = invalid, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    if (amount == null) Text(T.text("Ohne Portionsmenge wird eine Bezugsportion von 100 g bzw. 100 ml geteilt."),
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                    CopyField(T.text("Portion"), if (amount == null) T.text(Serving.DEFAULT_NAME)
                        else Serving.label(servingText) ?: T.text(Serving.DEFAULT_NAME), onCopy, Modifier.fillMaxWidth())
                    CopyField(if (product.unit == QuantityUnit.G) T.text("Gramm") else T.text("Milliliter"), amount?.let(T::number), onCopy, Modifier.fillMaxWidth())
                    CopyField(T.text("Kategorie"), product.category, onCopy, Modifier.fillMaxWidth())
                    CopyField("Barcode", product.barcode, onCopy, Modifier.fillMaxWidth())
                    if (product.manual) TextButton(onClick = onEdit) { Text(T.text("Eintrag bearbeiten")) }
                    else TextButton(onClick = onRefresh, enabled = !loading) { Text(if (loading) T.text("Wird aktualisiert …") else T.text("Daten aktualisieren")) }
                }
            }
            }
            item { Text(if (product.manual) T.text("Manueller Eintrag") else "Open Food Facts · ODbL", color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
        if (onSend != null) Surface(color = Background) {
            PrimaryButton(T.text("An GymMane senden"), {
                val selected = product.copy(
                    servingAmount = if (amountText == product.servingAmount?.let(T::number).orEmpty()) product.servingAmount else amount,
                    servingLabel = Serving.label(servingText))
                onSend(selected)
            }, Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp), enabled = !loading && !invalid, icon = Icons.AutoMirrored.Outlined.Send)
        }
    }
}
