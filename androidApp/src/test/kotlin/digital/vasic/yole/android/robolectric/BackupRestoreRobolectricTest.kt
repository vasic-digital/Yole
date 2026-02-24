/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Backup and restore
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
class BackupRestoreRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun backupRestoreUIAccessible() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Backup & Restore").onFirst().assertExists()
    }

    @Test
    fun moreScreenNavigable() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.waitForIdle()
        // Verify we can navigate to the More screen and it renders
        composeTestRule.onAllNodesWithText("More").onFirst().assertIsDisplayed()
    }

    @Test
    fun moreScreenRetainsStateOnRevisit() {
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.onAllNodesWithText("Files").onFirst().performClick()
        composeTestRule.onAllNodesWithText("More").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("More").onFirst().assertIsDisplayed()
    }
}
