package de.gymmane.scanner.ui

import de.gymmane.scanner.i18n.T

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.gymmane.scanner.domain.*
import de.gymmane.scanner.ui.theme.*
import java.util.UUID

@Composable
fun ManualScreen(editing: Product?, initialBarcode: String, loading: Boolean, error: String?, onSave: (Product) -> Unit, onCancel: () -> Unit, forSending: Boolean = false) {
    val key = editing?.id ?: "new"
    var showDetails by rememberSaveable(key) { mutableStateOf(false) }
    var name by rememberSaveable(key) { mutableStateOf(editing?.takeIf { it.nameKnown }?.let { if (forSending) GymManeImport.foodName(it) else it.name }.orEmpty()) }
    var category by rememberSaveable(key) { mutableStateOf(editing?.category.orEmpty()) }
    var barcode by rememberSaveable(key) { mutableStateOf(editing?.barcode ?: initialBarcode) }
    var serving by rememberSaveable(key) { mutableStateOf(editing?.servingLabel.orEmpty()) }
    var amount by rememberSaveable(key) { mutableStateOf(editing?.servingAmount?.let(::editableNumber) ?: if (forSending) "100" else "") }
    var ml by rememberSaveable(key) { mutableStateOf(editing?.unit == QuantityUnit.ML) }
    val original = editing?.per100 ?: Nutrients()
    var kcal by rememberSaveable(key) { mutableStateOf(original.kcal?.let(::editableNumber).orEmpty()) }
    var protein by rememberSaveable(key) { mutableStateOf(original.protein?.let(::editableNumber).orEmpty()) }
    var carbs by rememberSaveable(key) { mutableStateOf(original.carbs?.let(::editableNumber).orEmpty()) }
    var fat by rememberSaveable(key) { mutableStateOf(original.fat?.let(::editableNumber).orEmpty()) }
    var fiber by rememberSaveable(key) { mutableStateOf(original.fiber?.let(::editableNumber).orEmpty()) }
    var sugar by rememberSaveable(key) { mutableStateOf(original.sugar?.let(::editableNumber).orEmpty()) }
    var sodium by rememberSaveable(key) { mutableStateOf(original.sodium?.let(::editableNumber).orEmpty()) }
    var formError by rememberSaveable(key) { mutableStateOf<String?>(null) }
    val errorVisibility = rememberMotionVisibility(formError != null || error != null)
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "editor_1") { EditorField(T.text("Name *"), name, { name = it.take(200) }, modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) }
        item(key = "editor_2") {
            SectionLabel(T.text("Angaben vom Etikett"), color = Apricot)
            Spacer(Modifier.height(8.dp))
            PillSwitch(listOf(T.text("je 100 g"), T.text("je 100 ml")), if (ml) 1 else 0, { ml = it == 1 }, Modifier.fillMaxWidth())
        }
        item(key = "editor_3") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EditorField(T.text("Kalorien *"), kcal, { kcal = it.take(12) }, true, "kcal")
                EditorField(T.text("Eiweiß *"), protein, { protein = it.take(12) }, true, "g", Apricot)
                EditorField(T.text("Kohlenhydrate *"), carbs, { carbs = it.take(12) }, true, "g", Gold)
                EditorField(T.text("Fett *"), fat, { fat = it.take(12) }, true, "g", Sage)
            }
        }
        item(key = "editor_4") { TextButton(onClick = { showDetails = !showDetails }) { Text(if (showDetails) T.text("Weniger Angaben") else T.text("Optionale Angaben")) } }
        if (showDetails) {
        item(key = "editor_5") { EditorField(T.text("Kategorie (optional)"), category, { category = it.take(100) }, modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) }
        item(key = "editor_6") { EditorField(T.text("Barcode (optional)"), barcode, { barcode = it.filter(Char::isDigit).take(13) }, numeric = true, enabled = editing == null, modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) }
        item(key = "editor_7") { SectionLabel(T.text("Portion (optional)"), modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) }
        item(key = "editor_8") { EditorField(T.text("Bezeichnung, z. B. 1 Messlöffel"), serving, { serving = it.take(100) }, modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) }
        item(key = "editor_9") { EditorField(T.text("Portionsgröße"), amount, { amount = it.take(12) }, true, if (ml) "ml" else "g", modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) }
        item(key = "editor_10") { SectionLabel(T.text("Weitere Nährwerte (optional)"), color = Apricot, modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) }
        item(key = "editor_11") { EditorField(T.text("Ballaststoffe"), fiber, { fiber = it.take(12) }, true, "g", modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) }
        item(key = "editor_12") { EditorField(T.text("Zucker"), sugar, { sugar = it.take(12) }, true, "g", modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) }
        item(key = "editor_13") { EditorField(T.text("Natrium"), sodium, { sodium = it.take(12) }, true, "g", modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true))) }
        item(key = "editor_14") { Text(T.text("Auch die optionalen Werte gelten je 100 ${if (ml) "ml" else "g"}. Natrium wird in Gramm erfasst, nicht in Milligramm. Leere optionale Felder bleiben unbekannt."), color = Muted, style = MaterialTheme.typography.bodySmall) }
        }
        if (errorVisibility.currentState || errorVisibility.targetState) item(key = "editor_15") { MotionError((formError ?: error)?.let(T::text), errorVisibility) }
        item(key = "editor_16") {
            PrimaryButton(if (loading) T.text("Wird gespeichert …") else if (forSending) T.text("Speichern und an GymMane senden") else T.text("Lebensmittel speichern"), {
                val values = listOf(kcal, protein, carbs, fat, fiber, sugar, sodium)
                formError = ManualEntry.validate(name, barcode, amount, values)
                if (formError == null) {
                    val normalized = Barcode.normalize(barcode)
                    val base = editing ?: Product(id = normalized ?: "manual:${UUID.randomUUID()}", name = name.trim())
                    onSave(base.copy(
                        barcode = normalized, name = name.trim(), category = category.trim().ifEmpty { null },
                        servingLabel = serving.trim().ifEmpty { null }, servingAmount = parseNumber(amount),
                        unit = if (ml) QuantityUnit.ML else QuantityUnit.G,
                        per100 = original.copy(kcal = parseNumber(kcal), protein = parseNumber(protein), carbs = parseNumber(carbs), fat = parseNumber(fat),
                            fiber = parseNumber(fiber), sugar = parseNumber(sugar), sodium = parseNumber(sodium)),
                        perServing = Nutrients(), manual = true, nameKnown = true, fetchedAt = System.currentTimeMillis()))
                }
            }, Modifier.fillMaxWidth().animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true)), enabled = !loading, icon = Icons.Outlined.Check)
        }
        item(key = "editor_17") { TextButton(onClick = onCancel, modifier = Modifier.animateItem(fadeInSpec = Motion.spec(), placementSpec = Motion.spec(Motion.Layout), fadeOutSpec = Motion.spec(Motion.Exit, true)), enabled = !loading) { Text(T.text("Abbrechen")) } }
    }
}

@Composable
private fun EditorField(label: String, value: String, onValueChange: (String) -> Unit, numeric: Boolean = false, unit: String? = null, accent: androidx.compose.ui.graphics.Color = Muted, enabled: Boolean = true, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onValueChange, label = { Text(label, color = accent) }, singleLine = true, enabled = enabled,
        suffix = unit?.let { { Text(it, color = Muted) } }, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Field, focusedContainerColor = Field, unfocusedBorderColor = Outline))
}

private fun editableNumber(value: Double): String = if (value.isFinite() && value >= 0)
    java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', if (T.isEnglish) '.' else ',') else ""
