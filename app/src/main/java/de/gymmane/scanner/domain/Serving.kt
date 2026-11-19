package de.gymmane.scanner.domain

import kotlin.math.abs

/** A portion's name is independent of its weight or volume. */
object Serving {
    const val DEFAULT_NAME = "1 Portion"

    data class Quantity(val amount: Double, val unit: QuantityUnit)

    private val quantity = Regex("(?i)(?<![\\p{L}\\d.,-])(\\d+(?:[.,]\\d+)?)\\s*(kg|mg|ml|cl|dl|g|l)\\b")

    fun unit(value: String?): QuantityUnit? = when (value?.lowercase(java.util.Locale.ROOT)) {
        "g", "kg", "mg" -> QuantityUnit.G
        "ml", "cl", "dl", "l" -> QuantityUnit.ML
        else -> null
    }

    fun quantity(amount: Double?, symbol: String?): Quantity? {
        val unit = unit(symbol) ?: return null
        val factor = when (symbol?.lowercase(java.util.Locale.ROOT)) {
            "kg", "l" -> 1000.0
            "mg" -> .001
            "cl" -> 10.0
            "dl" -> 100.0
            else -> 1.0
        }
        val normalized = amount?.times(factor)?.takeIf { it.isFinite() && it > 0 } ?: return null
        return Quantity(normalized, unit)
    }

    /** Only one explicit quantity is safe; multipacks need their own interpretation. */
    fun parseQuantity(text: String?): Quantity? {
        val matches = quantity.findAll(text.orEmpty()).toList()
        if (matches.size != 1 || Regex("(?i)\\d\\s*[x×*]").containsMatchIn(text.orEmpty())) return null
        val match = matches.single()
        return quantity(parseNumber(match.groupValues[1]), match.groupValues[2])
    }

    /** Also cleans old cached labels such as `1 Becher (150 g)` and `500 ml`. */
    fun label(text: String?): String? {
        if (Regex("(?i)^\\s*\\d+\\s*[x×*]").containsMatchIn(text.orEmpty())) return null
        val cleaned = quantity.replace(text.orEmpty(), "")
            .replace(Regex("\\(\\s*\\)|\\[\\s*\\]"), "")
            .replace(Regex("\\s+"), " ").trim(' ', '·', '-', '/', ',', ';', ':', '=', '(', ')', '[', ']')
        if (cleaned.isEmpty() || !Regex("\\p{L}").containsMatchIn(cleaned) ||
            Regex("(?i)^[\\d\\s.,x×*()+/\\-]+$").matches(cleaned)) return null
        return cleaned
    }

    fun name(product: Product): String = if (product.servingAmount == null) DEFAULT_NAME
        else label(product.servingLabel) ?: DEFAULT_NAME

    fun matches(first: Quantity, second: Quantity): Boolean = first.unit == second.unit &&
        abs(first.amount - second.amount) <= .001
}
