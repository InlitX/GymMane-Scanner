package de.gymmane.scanner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.gymmane.scanner.domain.*
import de.gymmane.scanner.ui.ProductScreen
import de.gymmane.scanner.ui.theme.GymManeTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ProductUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun copiesExactNumbersAndRecalculatesPortion() {
        var copied = ""
        val fixture = Product("test", name = "UI-Testeintrag", servingAmount = 30.0, servingLabel = "1 Portion · 30 g",
            per100 = Nutrients(kcal = 400.0, protein = 80.0, carbs = 10.0, fat = 5.0))
        compose.setContent { GymManeTheme { ProductScreen(fixture, true, false, null, { _, value -> copied = value }, {}, {}, {}) } }
        compose.onNodeWithContentDescription("Eiweiß kopieren").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("80", copied) }
        compose.onNodeWithText("pro Portion").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Eiweiß kopieren").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("24", copied) }
        compose.onNodeWithContentDescription("kcal kopieren").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("120", copied) }
    }

    @Test fun missingValuesCannotBeCopied() {
        val fixture = Product("test", name = "Unvollständiger Testeintrag", per100 = Nutrients(kcal = 0.0))
        compose.setContent { GymManeTheme { ProductScreen(fixture, true, false, null, { _, _ -> }, {}, {}, {}) } }
        compose.onNodeWithContentDescription("Eiweiß kopieren").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("kcal kopieren").performScrollTo().assertIsEnabled()
    }
}
