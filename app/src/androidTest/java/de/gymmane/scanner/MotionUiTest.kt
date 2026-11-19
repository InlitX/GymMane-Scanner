package de.gymmane.scanner

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.gymmane.scanner.ui.*
import de.gymmane.scanner.ui.theme.GymManeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MotionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun reducedMotionUsesZeroDuration() {
        var duration = -1
        compose.setContent {
            GymManeTheme { CompositionLocalProvider(LocalReducedMotion provides true) {
                duration = Motion.spec<Float>(Motion.Screen).durationMillis
            } }
        }
        compose.runOnIdle { assertEquals(0, duration) }
    }

    @Test fun screenChangesKeepOnlyOneContentTree() {
        var screen by mutableStateOf("first")
        compose.mainClock.autoAdvance = false
        compose.setContent { GymManeTheme { ScreenEntrance(screen) { Text(screen, Modifier.testTag("screen")) } } }
        compose.runOnUiThread { screen = "second" }
        compose.mainClock.advanceTimeByFrame()
        compose.onAllNodesWithTag("screen").assertCountEquals(1)
        compose.onNodeWithText("second").assertExists()
    }

    @Test fun dialogPayloadIsRemovedAfterExitAndReducedMotionSkipsExit() {
        var value by mutableStateOf<String?>("payload")
        compose.setContent { GymManeTheme { CompositionLocalProvider(LocalReducedMotion provides true) {
            MotionDialog(value) { text, modifier -> Text(text, modifier.testTag("dialog")) }
        } } }
        compose.onNodeWithTag("dialog").assertExists()
        compose.runOnIdle { value = null }
        compose.onNodeWithTag("dialog").assertDoesNotExist()
    }
}
