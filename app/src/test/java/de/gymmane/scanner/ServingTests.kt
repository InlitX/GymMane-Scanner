package de.gymmane.scanner

import de.gymmane.scanner.data.ProductJson
import de.gymmane.scanner.domain.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ServingTests {
    private fun product(fields: String, nutrients: String = "") = ProductJson.fromApi(
        """{"status":1,"product":{"product_name":"Fixture",$fields,"nutriments":{
            "energy-kcal_100g":2,"proteins_100g":0,"carbohydrates_100g":0.9,"fat_100g":0$nutrients}}}""", "123")
    private fun food(product: Product) = JSONObject(GymManeImport.serialize(product)).getJSONObject("food")

    @Test fun `metric serving text becomes a neutral name with its actual volume`() {
        val p = product(""""serving_size":"500 ml","serving_quantity":500,"product_quantity_unit":"ml"""")
        val f = food(p)
        assertEquals("1 Portion", f.getString("serving_name"))
        assertEquals(500.0, f.getDouble("grams"), 0.0)
        assertTrue(f.getBoolean("liquid"))
        assertEquals(2.0, f.getJSONObject("per100").getDouble("kcal"), 0.0)
    }

    @Test fun `a matching can is named separately from its 500 milliliters`() {
        val p = product(""""serving_size":"500 ml","serving_quantity":500,"product_quantity":500,
            "product_quantity_unit":"ml","packagings":[{"shape":"en:can","number_of_units":1}]""")
        assertEquals("1 Dose", food(p).getString("serving_name"))
        assertEquals(500.0, food(p).getDouble("grams"), 0.0)
        assertEquals("1 Dose", ProductJson.decode(ProductJson.encode(p)).servingLabel)
    }

    @Test fun `a half can is not mislabeled as a whole can`() {
        val p = product(""""serving_size":"250 ml","product_quantity":500,"product_quantity_unit":"ml",
            "packagings":[{"shape":"en:can","number_of_units":1}]""")
        assertEquals("1 Portion", food(p).getString("serving_name"))
        assertEquals(250.0, food(p).getDouble("grams"), 0.0)
    }

    @Test fun `OFF taxonomy objects and structured quantities are read as returned by the API`() {
        val p = product(""""serving_size":"250 ml","product_quantity_unit":"ml","packagings":[{
            "shape":{"id":"en:bottle","lc_name":"Flasche"},"number_of_units":6,
            "quantity_per_unit_value":25,"quantity_per_unit_unit":"cl","weight_measured":32}]""")
        assertEquals("1 Flasche", food(p).getString("serving_name"))
        assertEquals(250.0, p.servingAmount!!, 0.0)
    }

    @Test fun `structured container count divides the whole multipack quantity`() {
        val p = product(""""quantity":"6 x 500 ml","product_quantity":3000,"product_quantity_unit":"ml",
            "packagings":[{"shape":{"id":"en:can"},"number_of_units":6}]""")
        assertEquals("1 Dose", p.servingLabel)
        assertEquals(500.0, p.servingAmount!!, 0.0)
    }

    @Test fun `a named serving loses only its metric suffix and preserves its count`() {
        val becher = product(""""serving_size":"1 Becher (150 g)"""")
        assertEquals("1 Becher", becher.servingLabel)
        assertEquals(150.0, becher.servingAmount!!, 0.0)
        val pieces = product(""""serving_size":"2 Stück (30 g)"""")
        assertEquals("2 Stück", pieces.servingLabel)
        assertEquals(30.0, food(pieces).getDouble("grams"), 0.0)
    }

    @Test fun `explicit serving description takes precedence over a container`() {
        val p = product(""""serving_size":"1 Glas (250 ml)","product_quantity_unit":"ml",
            "packagings":[{"shape":"en:bottle","quantity_per_unit":"250 ml"}]""")
        assertEquals("1 Glas", food(p).getString("serving_name"))
    }

    @Test fun `container quantity can supply a portion when no serving is given`() {
        val p = product(""""quantity":"6 x 500 ml","product_quantity":3000,"product_quantity_unit":"ml",
            "packagings":[{"shape":"en:bottle","number_of_units":6,"quantity_per_unit":"0.5 l"},
            {"shape":"en:cap","number_of_units":6,"weight_measured":2}]""")
        assertEquals("1 Flasche", p.servingLabel)
        assertEquals(500.0, p.servingAmount!!, 0.0)
        assertEquals(QuantityUnit.ML, p.unit)
    }

    @Test fun `multiple possible containers are not guessed`() {
        val p = product(""""product_quantity_unit":"ml","packagings":[
            {"shape":"en:bottle","quantity_per_unit":"500 ml"},
            {"shape":"en:can","quantity_per_unit":"330 ml"}]""")
        assertNull(p.servingAmount)
        assertEquals("1 Portion", food(p).getString("serving_name"))
    }

    @Test fun `multipack total is never interpreted as one item without container information`() {
        val p = product(""""quantity":"2 x 60 g","product_quantity":120,"serving_size":"2 x 60 g"""")
        assertNull(p.servingAmount)
        assertEquals("1 Portion", food(p).getString("serving_name"))
        assertEquals(100.0, food(p).getDouble("grams"), 0.0)
        assertNull(Serving.label("6x100 g"))
        assertNull(Serving.parseQuantity("6x100 g"))
        assertNull(Serving.parseQuantity("-500 ml"))
    }

    @Test fun `normalized numeric serving quantity does not get converted twice`() {
        val p = product(""""serving_size":"1 Flasche (0,5 l)","serving_quantity":500,"product_quantity_unit":"ml"""")
        assertEquals(500.0, p.servingAmount!!, 0.0)
        assertEquals("1 Flasche", p.servingLabel)
        assertEquals(500.0, Serving.parseQuantity("0,5 l")!!.amount, 0.0)
        assertEquals(250.0, Serving.parseQuantity("25 cl")!!.amount, 0.0)
        assertEquals(250.0, Serving.parseQuantity("0.25 kg")!!.amount, 0.0)
    }

    @Test fun `unknown serving weight cannot be replaced with packaging weight or pack size`() {
        val p = product(""""serving_size":"1 Stück","product_quantity":500,"product_quantity_unit":"g",
            "packagings":[{"shape":"en:pot","number_of_units":1,"weight_measured":10}]""")
        assertNull(p.servingAmount)
        assertEquals("1 Portion", food(p).getString("serving_name"))
        assertEquals(100.0, food(p).getDouble("grams"), 0.0)
    }

    @Test fun `container inference never invents the basis for per serving nutrition`() {
        val p = ProductJson.fromApi("""{"status":1,"product":{"product_name":"Fixture",
            "product_quantity":500,"product_quantity_unit":"ml",
            "packagings":[{"shape":"en:can","number_of_units":1}],
            "nutriments":{"energy-kcal_serving":40}}}""", "123")
        assertNull(p.servingAmount)
        assertNull(p.per100.kcal)
        assertEquals(40.0, p.perServing.kcal!!, 0.0)
    }

    @Test fun `legacy cache and direct export clean metric labels without requiring network`() {
        val legacy = Product("legacy", name = "Fixture", servingLabel = "500 ml", servingAmount = 500.0,
            unit = QuantityUnit.ML, per100 = Nutrients(2.0, 0.0, .9, 0.0))
        val cached = ProductJson.decode(ProductJson.encode(legacy))
        assertNull(cached.servingLabel)
        assertEquals("1 Portion", food(cached).getString("serving_name"))
        assertEquals("1 Portion", food(legacy).getString("serving_name"))
        assertEquals("1 Becher", food(legacy.copy(servingLabel = "1 Becher (500 ml)")).getString("serving_name"))
        assertEquals(500.0, food(cached).getDouble("grams"), 0.0)
    }

    @Test fun `missing amount uses a neutral reference portion even when a piece name exists`() {
        val p = product(""""serving_size":"1 Stück","product_quantity_unit":"ml"""")
        assertEquals("1 Portion", food(p).getString("serving_name"))
        assertEquals(100.0, food(p).getDouble("grams"), 0.0)
        assertTrue(food(p).getBoolean("liquid"))
    }

    @Test fun `known per serving nutrition still derives base values using serving weight`() {
        val p = ProductJson.fromApi("""{"status":1,"product":{"product_name":"Fixture",
            "serving_size":"1 Becher (150 g)","nutriments":{
            "energy-kcal_serving":90,"proteins_serving":6,"carbohydrates_serving":12,"fat_serving":3}}}""", "123")
        val f = food(p)
        assertEquals(60.0, f.getJSONObject("per100").getDouble("kcal"), .00001)
        assertEquals("1 Becher", f.getString("serving_name"))
        assertEquals(150.0, f.getDouble("grams"), 0.0)
    }
}
