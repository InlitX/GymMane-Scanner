package de.gymmane.scanner.domain

/** Pure validation shared by the editor and unit tests. Empty is never zero. */
object ManualEntry {
    fun validate(name: String, barcode: String, amount: String, values: List<String>): String? {
        if (name.isBlank()) return "Bitte gib einen Namen ein."
        if (barcode.isNotBlank() && Barcode.normalize(barcode) == null) return "Der Barcode ist ungültig. Prüfe Ziffern und Prüfziffer oder lasse das Feld leer."
        if (amount.isNotBlank() && (parseNumber(amount)?.let { it > 0 } != true)) return "Die Portionsgröße muss größer als 0 sein."
        if (values.size != 7 || values.take(4).any { parseNumber(it) == null }) return "Bitte Kalorien, Eiweiß, Kohlenhydrate und Fett als gültige Zahlen eintragen. 0 ist erlaubt."
        if (values.drop(4).any { it.isNotBlank() && parseNumber(it) == null }) return "Bitte optionale Nährwerte als gültige Zahlen eintragen oder leer lassen."
        val numbers = values.map(::parseNumber)
        if (numbers.first()!! > 1000 || numbers.drop(1).any { it != null && it > 100 }) return "Bitte die Werte je 100 g / ml prüfen: höchstens 1000 kcal und 100 g je Nährstoff."
        return null
    }
}
