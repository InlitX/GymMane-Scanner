package de.gymmane.scanner.data

import de.gymmane.scanner.domain.*
import org.json.JSONObject

class InvalidProductResponse : Exception("Die Produktdaten konnten nicht gelesen werden.")
class ProductNotFound : Exception("Dieses Produkt ist bei Open Food Facts noch nicht hinterlegt.")

/** Explicit mapping: absent, invalid and negative values never turn into zero. */
object ProductJson {
    private fun JSONObject.text(key: String) = optString(key, "").trim().takeIf { it.isNotBlank() && it != "null" }
    private fun JSONObject.number(key: String): Double? = if (!has(key) || isNull(key)) null
        else opt(key)?.toString()?.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }

    private fun nutrients(json: JSONObject, suffix: String): Nutrients {
        val energyUnit = json.text("energy_unit")?.lowercase()
        val kcal = json.number("energy-kcal_$suffix")
            ?: json.number("energy-kj_$suffix")?.div(4.184)
            ?: json.number("energy_$suffix")?.let { if (energyUnit == "kcal") it else it / 4.184 }
        // OFF normalizes nutrient quantities to grams, including sodium.
        return Nutrients(kcal, json.number("proteins_$suffix"), json.number("carbohydrates_$suffix"),
            json.number("fat_$suffix"), json.number("fiber_$suffix"), json.number("sugars_$suffix"),
            json.number("sodium_$suffix"), json.number("salt_$suffix"),
            json.number("saturated-fat_$suffix"), json.number("trans-fat_$suffix"),
            json.number("monounsaturated-fat_$suffix"), json.number("polyunsaturated-fat_$suffix"),
            json.number("cholesterol_$suffix"))
    }

    fun fromApi(body: String, barcode: String, language: String = "de"): Product {
        val root = try { JSONObject(body) } catch (_: Exception) { throw InvalidProductResponse() }
        if (!root.has("status")) throw InvalidProductResponse()
        if (root.optInt("status") == 0) throw ProductNotFound()
        val p = root.optJSONObject("product") ?: throw InvalidProductResponse()
        val n = p.optJSONObject("nutriments") ?: JSONObject()
        val perServing = nutrients(n, "serving")
        val serving = OffServing.read(p, perServing.hasValues)
        val base = nutrients(n, "100g")
        val per100 = if (serving.nutritionAmount != null) {
            base.withFallback(perServing.scaled(100 / serving.nutritionAmount))
        } else base
        return Product(
            id = barcode, barcode = barcode,
            name = p.text("product_name_$language") ?: p.text("product_name_de") ?: p.text("product_name") ?: "Produkt $barcode",
            nameKnown = p.text("product_name_$language") != null || p.text("product_name_de") != null || p.text("product_name") != null,
            brand = p.text("brands"),
            category = listOf(language, "de").distinct().firstNotNullOfOrNull { lang ->
                p.optJSONArray("categories_tags_$lang")?.let { tags ->
                    (0 until tags.length()).map { tags.optString(it) }.lastOrNull {
                        it.isNotBlank() && !Regex("^[a-z]{2}:").containsMatchIn(it)
                    }
                }
            } ?: listOf(language, "de").distinct().firstNotNullOfOrNull { lang ->
                p.optJSONArray("categories_hierarchy")?.let { tags ->
                    (0 until tags.length()).map { tags.optString(it) }.lastOrNull { it.startsWith("$lang:") }
                        ?.removePrefix("$lang:")?.replace('-', ' ')?.replaceFirstChar { it.titlecase() }
                }
            } ?: p.text("categories")?.split(',')?.lastOrNull()?.trim(),
            imageUrl = (p.text("image_front_url") ?: p.text("image_url"))?.takeIf { it.startsWith("https://") },
            servingLabel = serving.label, servingAmount = serving.amount,
            unit = serving.unit, per100 = per100, perServing = perServing,
        )
    }

    private fun encodeNutrients(n: Nutrients) = JSONObject().apply {
        put("kcal", n.kcal); put("protein", n.protein); put("carbs", n.carbs); put("fat", n.fat)
        put("fiber", n.fiber); put("sugar", n.sugar); put("sodium", n.sodium)
        put("salt", n.salt); put("saturated", n.saturated); put("trans", n.trans)
        put("monounsaturated", n.monounsaturated); put("polyunsaturated", n.polyunsaturated)
        put("cholesterol", n.cholesterol)
    }
    private fun decodeNutrients(n: JSONObject) = Nutrients(n.number("kcal"), n.number("protein"),
        n.number("carbs"), n.number("fat"), n.number("fiber"), n.number("sugar"), n.number("sodium"),
        n.number("salt"), n.number("saturated"), n.number("trans"), n.number("monounsaturated"),
        n.number("polyunsaturated"), n.number("cholesterol"))

    fun encode(p: Product): String = JSONObject().apply {
        put("id", p.id); put("barcode", p.barcode); put("name", p.name); put("brand", p.brand)
        put("category", p.category); put("imageUrl", p.imageUrl); put("localImage", p.localImage)
        put("servingLabel", p.servingLabel); put("servingAmount", p.servingAmount)
        put("unit", p.unit.name); put("per100", encodeNutrients(p.per100))
        put("perServing", encodeNutrients(p.perServing)); put("manual", p.manual); put("fetchedAt", p.fetchedAt)
        put("nameKnown", p.nameKnown)
    }.toString()

    fun decode(body: String): Product = JSONObject(body).let {
        Product(id = it.getString("id"), barcode = it.text("barcode"), name = it.getString("name"),
            brand = it.text("brand"), category = it.text("category"), imageUrl = it.text("imageUrl"),
            localImage = it.text("localImage"), servingLabel = Serving.label(it.text("servingLabel")),
            servingAmount = it.number("servingAmount"), unit = QuantityUnit.valueOf(it.getString("unit")),
            per100 = decodeNutrients(it.getJSONObject("per100")), perServing = decodeNutrients(it.getJSONObject("perServing")),
            manual = it.optBoolean("manual"), fetchedAt = it.getLong("fetchedAt"),
            nameKnown = it.optBoolean("nameKnown", it.getString("name") != "Produkt ${it.text("barcode")}"))
    }
}
