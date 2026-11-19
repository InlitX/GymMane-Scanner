package de.gymmane.scanner.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class QuantityUnit(val symbol: String) { G("g"), ML("ml") }

data class Nutrients(
    val kcal: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodium: Double? = null,
    val salt: Double? = null,
    val saturated: Double? = null,
    val trans: Double? = null,
    val monounsaturated: Double? = null,
    val polyunsaturated: Double? = null,
    val cholesterol: Double? = null,
) {
    fun scaled(factor: Double) = Nutrients(
        kcal?.times(factor), protein?.times(factor), carbs?.times(factor),
        fat?.times(factor), fiber?.times(factor), sugar?.times(factor), sodium?.times(factor),
        salt?.times(factor), saturated?.times(factor), trans?.times(factor),
        monounsaturated?.times(factor), polyunsaturated?.times(factor), cholesterol?.times(factor),
    )
    fun withFallback(other: Nutrients) = Nutrients(
        kcal ?: other.kcal, protein ?: other.protein, carbs ?: other.carbs,
        fat ?: other.fat, fiber ?: other.fiber, sugar ?: other.sugar, sodium ?: other.sodium,
        salt ?: other.salt, saturated ?: other.saturated, trans ?: other.trans,
        monounsaturated ?: other.monounsaturated, polyunsaturated ?: other.polyunsaturated,
        cholesterol ?: other.cholesterol,
    )
    val hasValues: Boolean get() = listOf(kcal, protein, carbs, fat, fiber, sugar, sodium,
        salt, saturated, trans, monounsaturated, polyunsaturated, cholesterol).any { it != null }
}

data class Product(
    val id: String,
    val barcode: String? = null,
    val name: String,
    val brand: String? = null,
    val category: String? = null,
    val imageUrl: String? = null,
    val localImage: String? = null,
    val servingLabel: String? = null,
    val servingAmount: Double? = null,
    val unit: QuantityUnit = QuantityUnit.G,
    val per100: Nutrients = Nutrients(),
    val perServing: Nutrients = Nutrients(),
    val manual: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis(),
    val nameKnown: Boolean = true,
) {
    fun nutrients(portion: Boolean, amount: Double? = servingAmount): Nutrients {
        if (!portion) return per100
        if (amount == null) return perServing
        val scaled = per100.scaled(amount / 100.0)
        return if (amount == servingAmount) scaled.withFallback(perServing) else scaled
    }
}

/** Numeric clipboard values deliberately omit units and grouping separators. */
fun formatNumber(value: Double): String = BigDecimal.valueOf(value)
    .setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',')

fun parseNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()
    ?.takeIf { it.isFinite() && it >= 0 }
