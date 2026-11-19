package de.gymmane.scanner

import de.gymmane.scanner.data.*
import de.gymmane.scanner.domain.*
import org.junit.Assert.*
import org.junit.Test

class ProductTests {
    private fun response(fields: String) = """{"status":1,"product":{$fields}}"""

    @Test fun `valid EAN and UPC normalize without losing leading zeroes`() {
        assertEquals("4006381333931", Barcode.normalize("4006381333931"))
        assertEquals("0036000291452", Barcode.normalize("036000291452"))
        assertEquals("96385074", Barcode.normalize("96385074"))
        assertNull(Barcode.normalize("4006381333932"))
        assertNull(Barcode.normalize("ABCDEFGHIJKLM"))
        assertNull(Barcode.normalize("12345"))
    }

    @Test fun `UPC E expands using the encoded number system and check digit`() {
        assertEquals("0042100005264", Barcode.expandUpcE("04252614"))
        assertNull(Barcode.expandUpcE("04252615"))
        assertNull(Barcode.expandUpcE("24252614"))
    }

    @Test fun `missing nutrients remain unknown but actual zero survives`() {
        val p = ProductJson.fromApi(response(""""product_name":"Fixture","nutriments":{"fat_100g":0,"proteins_100g":null,"carbohydrates_100g":-5}"""), "123")
        assertEquals(0.0, p.per100.fat!!, 0.0)
        assertNull(p.per100.protein)
        assertNull(p.per100.carbs)
        assertNull(p.per100.kcal)
    }

    @Test fun `energy in kJ converts to kcal and sodium remains grams`() {
        val p = ProductJson.fromApi(response(""""nutriments":{"energy_100g":418.4,"sodium_100g":0.04}"""), "123")
        assertEquals(100.0, p.per100.kcal!!, .0001)
        assertEquals(.04, p.per100.sodium!!, .000001)
    }

    @Test fun `explicit kcal takes precedence over kJ`() {
        val p = ProductJson.fromApi(response(""""nutriments":{"energy-kcal_100g":92,"energy_100g":418.4}"""), "123")
        assertEquals(92.0, p.per100.kcal!!, .0001)
    }

    @Test fun `known portion scales all nutrients and changing it uses the new amount`() {
        val p = ProductJson.fromApi(response(""""serving_size":"1 Becher (150 g)","serving_quantity":150,"nutriments":{"energy-kcal_100g":60,"proteins_100g":4,"fat_100g":0,"sodium_100g":0.1}"""), "123")
        assertEquals(150.0, p.servingAmount!!, .001)
        assertEquals(90.0, p.nutrients(true).kcal!!, .001)
        assertEquals(6.0, p.nutrients(true).protein!!, .001)
        assertEquals(.15, p.nutrients(true).sodium!!, .0001)
        assertEquals(120.0, p.nutrients(true, 200.0).kcal!!, .001)
        assertNull(p.nutrients(true).fiber)
    }

    @Test fun `unknown serving weight does not invent a conversion`() {
        val p = ProductJson.fromApi(response(""""serving_size":"1 Stück","nutriments":{"energy-kcal_100g":100}"""), "123")
        assertNull(p.servingAmount)
        assertNull(p.nutrients(true).kcal)
    }

    @Test fun `explicit per serving data usable without known weight`() {
        val p = ProductJson.fromApi(response(""""serving_size":"1 Stück","nutriments":{"energy-kcal_serving":85}"""), "123")
        assertEquals(85.0, p.nutrients(true).kcal!!, .001)
        assertNull(p.per100.kcal)
        assertNull(p.nutrients(true, 50.0).kcal)
    }

    @Test fun `per serving data can derive missing base values when weight known`() {
        val p = ProductJson.fromApi(response(""""serving_size":"30 g","nutriments":{"proteins_serving":12}"""), "123")
        assertEquals(40.0, p.per100.protein!!, .001)
    }

    @Test fun `liquid portions stay in milliliters`() {
        val p = ProductJson.fromApi(response(""""product_quantity_unit":"ml","serving_quantity":250,"serving_quantity_unit":"ml","nutriments":{"energy-kcal_100g":40}"""), "123")
        assertEquals(QuantityUnit.ML, p.unit)
        assertEquals(100.0, p.nutrients(true).kcal!!, .001)
    }

    @Test fun `milliliter product does not infer gram portions`() {
        val p = ProductJson.fromApi(response(""""product_quantity_unit":"ml","serving_size":"30 g","nutriments":{"energy-kcal_100g":40}"""), "123")
        assertNull(p.servingAmount)
    }

    @Test(expected = ProductNotFound::class) fun `not found has a dedicated result`() { ProductJson.fromApi("""{"status":0}""", "123") }
    @Test(expected = InvalidProductResponse::class) fun `malformed response not silently accepted`() { ProductJson.fromApi("{}", "123") }

    @Test fun `cache serialization preserves unknown values image path source and units`() {
        val p = Product("manual:1", name = "Testeintrag", localImage = "/local/image", unit = QuantityUnit.ML,
            per100 = Nutrients(kcal = 0.0, sodium = .012), manual = true)
        assertEquals(p, ProductJson.decode(ProductJson.encode(p)))
    }

    @Test fun `clipboard numbers have decimal comma no grouping and no unit`() {
        assertEquals("1234,5679", formatNumber(1234.56789))
        assertEquals("0,0428", formatNumber(.0428))
        assertEquals("0", formatNumber(0.0))
        assertEquals("2,5", formatNumber(2.5))
        assertEquals(2.5, parseNumber("2,5")!!, 0.0)
        assertNull(parseNumber("NaN")); assertNull(parseNumber("-1")); assertNull(parseNumber(""))
    }

    @Test fun `manual editor requires real values and validates optional values`() {
        assertNotNull(ManualEntry.validate("", "", "", List(7) { "0" }))
        assertNotNull(ManualEntry.validate("A", "", "", List(7) { "" }))
        assertNotNull(ManualEntry.validate("A", "", "0", List(7) { "0" }))
        assertNotNull(ManualEntry.validate("A", "", "", listOf("900", "0", "0", "101", "", "", "")))
        assertNull(ManualEntry.validate("A", "", "30", listOf("100", "2,5", "5", "0", "", "", "0,012")))
    }
}
