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

    @Test
    fun appLaunchesWithoutCrash() {
        composeTestRule.onAllNodesWithText("Files").onFirst().assertIsDisplayed()
    }

    @Test
    fun appShowsMainNavigationTabs() {
        composeTestRule.onAllNodesWithText("Files").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("To-Do").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("QuickNote").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("More").onFirst().assertIsDisplayed()
    }

    @Test
    fun appInitializesThemeWithoutCrash() {
        // This specifically tests the Theme.kt fix
        // On first launch, custom_seed_color is empty string
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Files").onFirst().assertIsDisplayed()
    }

    @Test
    fun appSurvivesActivityRecreation() {
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onAllNodesWithText("Files").onFirst().assertIsDisplayed()
    }

    @Test
    fun appHandlesConfigurationChange() {
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Files").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("To-Do").onFirst().assertIsDisplayed()
    }
}
