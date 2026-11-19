package de.gymmane.scanner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.gymmane.scanner.domain.Product
import de.gymmane.scanner.domain.Nutrients
import de.gymmane.scanner.domain.QuantityUnit
import de.gymmane.scanner.ui.ManualScreen
import de.gymmane.scanner.ui.theme.GymManeTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ManualUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun manualFormBuildsProductFromExplicitInputs() {
        var saved: Product? = null
        compose.setContent { GymManeTheme { ManualScreen(null, "", false, null, { saved = it }, {}) } }
        compose.onNodeWithText("Name *").performScrollTo().performTextInput("Manueller UI-Test")
        compose.onNodeWithText("Kalorien *").performScrollTo().performTextInput("100")
        compose.onNodeWithText("Eiweiß *").performScrollTo().performTextInput("2,5")
        compose.onNodeWithText("Kohlenhydrate *").performScrollTo().performTextInput("10")
        compose.onNodeWithText("Fett *").performScrollTo().performTextInput("0")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Lebensmittel speichern"))
        compose.onNodeWithText("Lebensmittel speichern").performClick()
        compose.runOnIdle {
            assertNotNull(saved)
            assertEquals("Manueller UI-Test", saved!!.name)
            assertTrue(saved!!.manual)
            assertEquals(2.5, saved!!.per100.protein!!, .001)
            assertEquals(0.0, saved!!.per100.fat!!, 0.0)
            assertNull(saved!!.per100.sodium)
            assertNull(saved!!.barcode)
        }
    }

    @Test fun editingServingAmountDoesNotKeepAnOldWholeCanName() {
        var saved: Product? = null
        val product = Product("can", name = "Drink", servingLabel = "1 Dose", servingAmount = 500.0,
            unit = QuantityUnit.ML, manual = true, per100 = Nutrients(2.0, 0.0, .9, 0.0))
        compose.setContent { GymManeTheme { ManualScreen(product, "", false, null, { saved = it }, {}) } }
        compose.onNodeWithText("Optionale Angaben").performScrollTo().performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Portionsgröße"))
        compose.onNodeWithText("Portionsgröße").performTextReplacement("250")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Lebensmittel speichern"))
        compose.onNodeWithText("Lebensmittel speichern").performClick()
        compose.runOnIdle {
            assertEquals("1 Portion", saved!!.servingLabel)
            assertEquals(250.0, saved!!.servingAmount!!, 0.0)
            assertEquals(product.per100, saved!!.per100)
        }
    }
}
