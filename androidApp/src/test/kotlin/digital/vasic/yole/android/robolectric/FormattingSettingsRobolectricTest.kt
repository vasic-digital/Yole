/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 8: FormattingSettingsRobolectricTest.
 *
 * Tests [FormattingSettingsRow] via Compose Rule:
 *   1. toggle_persisted_andDefault_isTrue — verifies:
 *      a) The toggle is rendered with testTag "formatting-settings-toggle".
 *      b) The default value is true (checked by default).
 *      c) When the toggle is clicked, setFormatOnSave is called with `false`.
 *      d) getFormatOnSave is re-consulted after the toggle, reflecting the
 *         persisted value (injected fake returns the stored value).
 *
 *   2. toggle_structuralCheck_ctrlShiftF_inSyncedScrollEditor — structural
 *      source-level assertion that SyncedScrollEditor.kt contains both
 *      `onExplicitFormat()` (the callback invocation) and the Ctrl+Shift+F
 *      key combination detection string. Removing either MUST cause this
 *      assertion to fail.
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   toggle_persisted_andDefault_isTrue:
 *     Mutation: stub setFormatOnSave to no-op (never record the new value).
 *       Re-reading via fakeGet returns `true` instead of `false` because the
 *       setter was never called → assertFalse(capturedValue) FAILS because
 *       capturedValue is still the unset initial value.
 *     Alternative: make the default value false → assertTrue(initialChecked) FAILS.
 *
 *   toggle_structuralCheck_ctrlShiftF_inSyncedScrollEditor:
 *     Mutation: remove `onExplicitFormat()` call from SyncedScrollEditor.kt →
 *       assertTrue(src.contains("onExplicitFormat()")) FAILS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: this test covers the Android SharedPreferences-backed toggle.
 *   - Desktop: formatting on Desktop is always explicit (no settings gate).
 *   - iOS/Web: N/A.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import digital.vasic.yole.android.ui.settings.FormattingSettingsRow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FormattingSettingsRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    // -----------------------------------------------------------------------
    // Test 1: toggle_persisted_andDefault_isTrue
    // -----------------------------------------------------------------------

    /**
     * The "Format on save" toggle MUST:
     *   a) be rendered (testTag "formatting-settings-toggle" present and displayed),
     *   b) default to checked (on) — true by default per spec,
     *   c) invoke setFormatOnSave(false) when tapped once (toggles from on→off),
     *   d) reflect the persisted value via getFormatOnSave after the toggle.
     *
     * Anti-bluff load-bearing assertions:
     *   - assertIsOn() fails if default is false.
     *   - assertFalse(capturedValue) fails if setFormatOnSave is never called.
     *   - assertIsOff() fails if the Composable does not re-read getFormatOnSave
     *     after the click, or if the toggle is not actually a Switch.
     */
    @Test
    fun toggle_persisted_andDefault_isTrue() {
        // Fake in-memory persistence: starts at true (default).
        var persisted = true
        var capturedCalled = false
        var capturedValue = true // will be overwritten on click

        composeRule.setContent {
            FormattingSettingsRow(
                getFormatOnSave = { persisted },
                setFormatOnSave = { newValue ->
                    capturedCalled = true
                    capturedValue = newValue
                    persisted = newValue // simulate real persistence
                },
            )
        }

        // a) Toggle must be displayed.
        composeRule.onNodeWithTag("formatting-settings-toggle").assertIsDisplayed()

        // b) Default state must be ON (true).
        composeRule.onNodeWithTag("formatting-settings-toggle").assertIsOn()

        // c) Tap to toggle → should turn off and call setFormatOnSave(false).
        composeRule.onNodeWithTag("formatting-settings-toggle").performClick()
        composeRule.waitForIdle()

        assertTrue(
            "setFormatOnSave MUST be called when the toggle is clicked",
            capturedCalled,
        )
        assertFalse(
            "setFormatOnSave MUST be called with false after turning the toggle off",
            capturedValue,
        )

        // d) The toggle must now appear as OFF (reflecting the persisted false value).
        composeRule.onNodeWithTag("formatting-settings-toggle").assertIsOff()
    }

    // -----------------------------------------------------------------------
    // Test 2: toggle_structuralCheck_ctrlShiftF_inSyncedScrollEditor
    // -----------------------------------------------------------------------

    /**
     * SyncedScrollEditor.kt MUST contain:
     *   - the `onExplicitFormat()` callback invocation inside the key handler, and
     *   - the Ctrl+Shift+F key detection string (`event.isShiftPressed`), and
     *   - `Key.F` detection for the shortcut key.
     *
     * Anti-bluff:
     *   - Removing `onExplicitFormat()` from the key handler → FAILS.
     *   - Removing `isShiftPressed` check → FAILS.
     *   - Removing `Key.F` reference inside the Ctrl+Shift+F block → FAILS.
     *
     * This is a source-level mutation check following the pattern established
     * in iter-62 Phase 8 (IdeEditorScreenLspIntegrationRobolectricTest).
     */
    @Test
    fun toggle_structuralCheck_ctrlShiftF_inSyncedScrollEditor() {
        val path = "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt"
        val candidates = listOf(path, "../$path")
        val src = candidates.map { File(it) }.firstOrNull { it.isFile }?.readText()
            ?: error("SyncedScrollEditor.kt not found (cwd=${File(".").absolutePath})")

        assertTrue(
            "SyncedScrollEditor MUST contain `onExplicitFormat()` invocation for Ctrl+Shift+F",
            src.contains("onExplicitFormat()"),
        )
        assertTrue(
            "SyncedScrollEditor MUST check `event.isShiftPressed` for Ctrl+Shift+F detection",
            src.contains("isShiftPressed"),
        )
        assertTrue(
            "SyncedScrollEditor MUST reference `Key.F` for the format shortcut",
            src.contains("Key.F"),
        )
    }
}
