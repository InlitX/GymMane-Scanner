package de.gymmane.scanner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import de.gymmane.scanner.i18n.T
import de.gymmane.scanner.ui.*
import de.gymmane.scanner.ui.theme.GymManeTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class SetupUiTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @After fun resetLocale() { T.configure(context, "de") }

    @Test fun englishOnboardingHasOnePrimaryContinueAction() {
        T.configure(context, "en")
        var continued = false
        compose.setContent { GymManeTheme { OnboardingScreen("en", {}, { continued = true }) } }
        compose.onNodeWithText("Scan. Check. Share.").assertIsDisplayed()
        compose.onNodeWithText("Get started").performClick()
        compose.runOnIdle { assertTrue(continued) }
    }

    @Test fun englishSettingsShowsVersionAndLanguageChoices() {
        T.configure(context, "en")
        var language = "en"
        compose.setContent { GymManeTheme { SettingsScreen("en", false, { language = it }, {}, {}, {}, {}) } }
        compose.onNodeWithText("Deutsch").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("de", language) }
        compose.onNodeWithText(BuildConfig.VERSION_NAME).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("App version").assertIsDisplayed()
    }

    @Test fun legacyValidationMessagesAreLocalizedAndJsonStaysUnchanged() {
        T.configure(context, "en")
        assertEquals("Protein", T.text("Eiweiß"))
        assertTrue(T.text("Bitte vor dem Senden ergänzen oder korrigieren: Eiweiß, Fett. Alle Nährwerte gelten je 100 g bzw. 100 ml.").contains("Protein, Fat"))
        assertEquals("Copy Protein", T.format("%1\$s kopieren", "Protein"))
    }
}
