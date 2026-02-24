/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: App launch without crash
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AppLaunchRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun appLaunchesWithoutCrash() {
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
    }

    @Test
    fun appShowsMainNavigationTabs() {
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
        composeTestRule.onNodeWithText("To-Do").assertIsDisplayed()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
        composeTestRule.onNodeWithText("More").assertIsDisplayed()
    }

    @Test
    fun appInitializesThemeWithoutCrash() {
        // This specifically tests the Theme.kt fix
        // On first launch, custom_seed_color is empty string
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
    }

    @Test
    fun appSurvivesActivityRecreation() {
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
    }

    @Test
    fun appHandlesConfigurationChange() {
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
        composeTestRule.onNodeWithText("To-Do").assertIsDisplayed()
    }
}
