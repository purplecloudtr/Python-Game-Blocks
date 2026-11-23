package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Python Yol Aç", appName)
    }

    @Test
    fun `main screen displays board, code editor, action buttons, and hint`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("level_tab_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("level_tab_2").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("level_tab_3").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("game_board_grid").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("board_text_summary").performScrollTo().assertIsDisplayed()

        // Scroll outer vertical list to action buttons so the code editor above them is also brought on-screen
        composeTestRule.onNodeWithTag("run_code_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("python_code_editor").assertExists()
        composeTestRule.onNodeWithTag("reset_level_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("hint_button").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("hint_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("hint_card").performScrollTo().assertIsDisplayed()
    }
}
