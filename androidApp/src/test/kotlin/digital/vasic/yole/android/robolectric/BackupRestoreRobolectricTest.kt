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
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
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

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun backupRestoreUIAccessible() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Backup & Restore").assertIsDisplayed()
    }

    @Test
    fun backupButtonDisplayed() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Backup & Restore").assertIsDisplayed()
        composeTestRule.onNodeWithText("Backup").assertIsDisplayed()
    }

    @Test
    fun restoreButtonDisplayed() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Backup & Restore").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restore").assertIsDisplayed()
    }
}
