package de.gymmane.scanner.data

import de.gymmane.scanner.domain.QuantityUnit
import de.gymmane.scanner.domain.Serving
import org.json.JSONObject

/** OFF's serving_size is free text; packaging quantities describe individual containers. */
internal object OffServing {
    data class Result(val label: String?, val amount: Double?, val unit: QuantityUnit,
        val nutritionAmount: Double?)
    private data class Container(val name: String, val quantity: Serving.Quantity)

    private fun JSONObject.text(key: String) = optString(key, "").trim().takeIf { it.isNotBlank() && it != "null" }
    private fun JSONObject.positive(key: String) = opt(key)?.toString()?.toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 }

    private val shapes = mapOf(
        "en:can" to "1 Dose", "en:drink-can" to "1 Dose", "en:beverage-can" to "1 Dose",
        "en:bottle" to "1 Flasche", "en:jar" to "1 Glas", "en:pot" to "1 Becher",
        "en:tub" to "1 Becher", "en:cup" to "1 Becher", "en:brick" to "1 Packung",
        "en:carton" to "1 Packung",
    )

    fun read(p: JSONObject, hasServingNutrients: Boolean): Result {
        val raw = p.text("serving_size")
        val parsed = Serving.parseQuantity(raw)
        val productUnit = Serving.unit(p.text("product_quantity_unit"))
            ?: Serving.parseQuantity(p.text("quantity"))?.unit
            ?: QuantityUnit.ML.takeIf { p.text("nutrition_data_per") == "100ml" }
        val servingSymbol = p.text("serving_quantity_unit")
            ?: parsed?.unit?.symbol ?: productUnit?.symbol ?: "g"
        val serving = Serving.quantity(p.positive("serving_quantity"), servingSymbol) ?: parsed
        val unit = productUnit ?: serving?.unit ?: QuantityUnit.G
        val nutritionAmount = serving?.takeIf { it.unit == unit }?.amount
        val label = Serving.label(raw)
        val containers = containers(p, productUnit)
        val matched = nutritionAmount?.let { amount ->
            containers.filter { Serving.matches(it.quantity, Serving.Quantity(amount, unit)) }
                .map { it.name }.distinct().singleOrNull()
        }
        // A container is a portion only when the serving is the same size. Do not turn
        // an unspecified serving or unknown per-serving nutrition into the whole pack.
        if (nutritionAmount != null) return Result(label ?: matched ?: Serving.DEFAULT_NAME,
            nutritionAmount, unit, nutritionAmount)
        if (raw == null && !hasServingNutrients) {
            val container = containers.distinct().singleOrNull()
            if (container != null && (productUnit == null || container.quantity.unit == productUnit)) {
                return Result(container.name, container.quantity.amount, container.quantity.unit, null)
            }
        }
        return Result(label, null, unit, null)
    }

    private fun containers(p: JSONObject, productUnit: QuantityUnit?): List<Container> {
        val packaging = p.optJSONArray("packagings") ?: return emptyList()
        val total = Serving.quantity(p.positive("product_quantity"), productUnit?.symbol)
            ?: Serving.parseQuantity(p.text("quantity"))
        return buildList {
            for (i in 0 until packaging.length()) {
                val part = packaging.optJSONObject(i) ?: continue
                val shape = part.optJSONObject("shape")?.text("id") ?: part.text("shape")
                val name = shapes[shape] ?: continue
                val count = part.positive("number_of_units")?.takeIf { it % 1.0 == 0.0 }
                val amount = Serving.quantity(part.positive("quantity_per_unit_value"), part.text("quantity_per_unit_unit"))
                    ?: Serving.parseQuantity(part.text("quantity_per_unit"))
                    ?: total?.let { quantity -> count?.let { Serving.Quantity(quantity.amount / it, quantity.unit) } }
                    ?: continue
                add(Container(name, amount))
            }
        }
    }
}
