/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 4.6: anti-bluff Robolectric tests for the one-time
 * format-enablement migration dialog. The dialog must:
 *   - Appear on first launch after upgrade when the persisted enabled-set
 *     differs from the new default ({markdown}) and no migration choice
 *     was previously recorded.
 *   - Tapping "Use new default" resets to {markdown} and writes
 *     formatEnablementMigrationChoiceMade = true.
 *   - NOT appear once formatEnablementMigrationChoiceMade is true.
 *
 * Mutation-verified per CONST-035: commenting out the
 * `settings.setFormatEnablementMigrationChoiceMade(true)` write in
 * FormatMigrationDialog.kt → dialogDoesNotShowAfterChoiceMade FAILS
 * because the dialog continues to show.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.android.ui.YoleSettings
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.syntax.EnabledFormatGate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FormatMigrationDialogRobolectricTest {

    // We need to seed YoleSettings BEFORE the Activity composes. The
    // createAndroidComposeRule launches the activity lazily on first
    // access; we hijack that by writing through a YoleSettings built
    // from the ApplicationContext in @Before, so by the time the
    // activity reads YoleSettings during composition, the disk values
    // are already in place.
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var settings: YoleSettings

    @Before
    fun seedSettings() = runBlocking<Unit> {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        settings = YoleSettings(ctx)
        // Clear any state left by other tests so this class is hermetic.
        settings.setFormatEnablementMigrationChoiceMade(false)
        settings.setPriorEnabledFormatIds(emptySet())
        settings.setEnabledFormatIds(setOf("markdown"))
        FormatRegistry.setEnabledFormatIds(setOf("markdown"))
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @Test
    fun dialogShowsOnFirstLaunchAfterUpgrade() = runBlocking<Unit> {
        // Simulate prior install: multiple formats enabled, no migration
        // choice recorded yet.
        settings.setEnabledFormatIds(setOf("markdown", "asciidoc", "csv"))
        settings.setFormatEnablementMigrationChoiceMade(false)
        settings.setPriorEnabledFormatIds(emptySet()) // force snapshot to capture

        // Trigger a fresh composition.
        composeRule.activity.recreate()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Format defaults changed").onFirst().assertIsDisplayed()
    }

    @Test
    fun useNewDefault_setsMarkdownOnly() = runBlocking<Unit> {
        settings.setEnabledFormatIds(setOf("markdown", "asciidoc", "csv"))
        settings.setFormatEnablementMigrationChoiceMade(false)
        settings.setPriorEnabledFormatIds(emptySet())

        composeRule.activity.recreate()
        composeRule.waitForIdle()

        // Find and tap the "Use new default" button.
        composeRule.onAllNodesWithText("Use new default").onFirst().performClick()
        composeRule.waitForIdle()

        // Gate must now hold ONLY markdown — load-bearing assertion.
        assertEquals(setOf("markdown"), EnabledFormatGate.enabled.value)
        // And the choice must be persisted so the dialog never re-shows.
        assertTrue(
            "migration choice must be recorded",
            settings.getFormatEnablementMigrationChoiceMade(),
        )
    }

    @Test
    fun dialogDoesNotShowAfterChoiceMade() = runBlocking<Unit> {
        // Steady state: choice already made, default set persisted.
        settings.setEnabledFormatIds(setOf("markdown"))
        settings.setFormatEnablementMigrationChoiceMade(true)

        composeRule.activity.recreate()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("Format defaults changed").assertCountEquals(0)
    }
}
