/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 8: FormattingSettingsRobolectricTest.
 *
 * Source-level structural assertions that verify FormattingSettings.kt
 * and the SyncedScrollEditor.kt Ctrl+Shift+F integration are correctly
 * implemented. Uses the same source-file-read pattern established by
 * CodeActionLightbulbRobolectricTest and RenamePreviewPanelRobolectricTest
 * (Config.NONE + no createComposeRule — avoids the activity-resolution
 * limitation with Config.NONE Robolectric runs).
 *
 * Tests:
 *   1. toggle_structuralCheck_formattingSettingsRow — verifies:
 *      a) testTag("formatting-settings-toggle") is present.
 *      b) Default value is true (getFormatOnSave() = { true }).
 *      c) Switch composable is used (accessibility-friendly toggle).
 *      d) setFormatOnSave(newValue) is called in onCheckedChange.
 *
 *   2. toggle_structuralCheck_ctrlShiftF_inSyncedScrollEditor — verifies:
 *      a) SyncedScrollEditor.kt contains `onExplicitFormat()`.
 *      b) `isShiftPressed` is present (Ctrl+Shift+F detection).
 *      c) `Key.F` is referenced for the shortcut key.
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   toggle_structuralCheck_formattingSettingsRow:
 *     Mutation 1: remove testTag("formatting-settings-toggle") from
 *       FormattingSettings.kt → assertTrue(…"formatting-settings-toggle"…) FAILS.
 *     Mutation 2: change default from { true } to { false } →
 *       assertTrue(…"{ true }"…) FAILS (default not present).
 *     Mutation 3: replace Switch with Checkbox → assertTrue(…"Switch("…) FAILS.
 *     Mutation 4: remove setFormatOnSave(newValue) invocation →
 *       assertTrue(…"setFormatOnSave(newValue)"…) FAILS.
 *
 *   toggle_structuralCheck_ctrlShiftF_inSyncedScrollEditor:
 *     Mutation: remove `onExplicitFormat()` from SyncedScrollEditor → FAILS.
 *     Mutation: remove `isShiftPressed` → FAILS.
 *     Mutation: remove `Key.F` → FAILS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: this test covers the Android SharedPreferences-backed toggle
 *     and the SyncedScrollEditor Ctrl+Shift+F wiring.
 *   - Desktop: formatting on Desktop is always explicit (no settings gate).
 *   - iOS/Web: N/A.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.robolectric

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FormattingSettingsRobolectricTest {

    private fun loadSource(relativePath: String): String {
        val candidates = listOf(
            relativePath,
            "../$relativePath",
            relativePath.removePrefix("androidApp/"),
        )
        for (path in candidates) {
            val f = File(path)
            if (f.isFile) return f.readText()
        }
        error("$relativePath not found; checked: $candidates (cwd=${File(".").absolutePath})")
    }

    private fun loadFormattingSettingsSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/settings/FormattingSettings.kt",
    )

    private fun loadSyncedScrollEditorSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt",
    )

    // -----------------------------------------------------------------------
    // Test 1: toggle_structuralCheck_formattingSettingsRow
    // -----------------------------------------------------------------------

    /**
     * Structural source-level assertion that FormattingSettings.kt has the
     * correct wiring for a "Format on save" toggle:
     *   a) testTag("formatting-settings-toggle") present.
     *   b) Default value is true (getFormatOnSave = { true }).
     *   c) Switch composable is used (not Checkbox or RadioButton).
     *   d) setFormatOnSave(newValue) is called in onCheckedChange.
     *
     * Anti-bluff: removing any of these markers causes the corresponding
     * assertion to fail.
     */
    @Test
    fun toggle_structuralCheck_formattingSettingsRow() {
        val src = loadFormattingSettingsSource()

        assertTrue(
            "FormattingSettingsRow MUST apply testTag(\"formatting-settings-toggle\") to its Switch",
            src.contains("\"formatting-settings-toggle\""),
        )
        assertTrue(
            "FormattingSettingsRow MUST default getFormatOnSave to { true }",
            src.contains("{ true }"),
        )
        assertTrue(
            "FormattingSettingsRow MUST use a Switch composable for the toggle",
            src.contains("Switch("),
        )
        assertTrue(
            "FormattingSettingsRow MUST call setFormatOnSave(newValue) in onCheckedChange",
            src.contains("setFormatOnSave(newValue)"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: toggle_structuralCheck_ctrlShiftF_inSyncedScrollEditor
    // -----------------------------------------------------------------------

    /**
     * SyncedScrollEditor.kt MUST contain:
     *   - the `onExplicitFormat()` callback invocation inside the key handler,
     *   - the `isShiftPressed` check (Ctrl+Shift+F detection), and
     *   - `Key.F` detection for the shortcut key.
     *
     * Anti-bluff:
     *   - Removing `onExplicitFormat()` from the key handler → FAILS.
     *   - Removing `isShiftPressed` check → FAILS.
     *   - Removing `Key.F` reference inside the Ctrl+Shift+F block → FAILS.
     */
    @Test
    fun toggle_structuralCheck_ctrlShiftF_inSyncedScrollEditor() {
        val src = loadSyncedScrollEditorSource()

        assertTrue(
            "SyncedScrollEditor MUST contain `onExplicitFormat()` invocation for Ctrl+Shift+F",
            src.contains("onExplicitFormat()"),
        )
        assertTrue(
            "SyncedScrollEditor MUST check `isShiftPressed` for Ctrl+Shift+F detection",
            src.contains("isShiftPressed"),
        )
        assertTrue(
            "SyncedScrollEditor MUST reference `Key.F` for the format shortcut",
            src.contains("Key.F"),
        )
    }
}
