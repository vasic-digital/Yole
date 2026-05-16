/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-63 Phase 5, CONST-035 anti-bluff):
 *   Verifies that RenamePreviewPanel.kt has the correct structural wiring
 *   for rendering a WorkspaceEdit preview inside a Material3 ModalBottomSheet.
 *
 *   Test architecture (mirrors iter-62 Phase 7 DefinitionLocationChooser
 *   pattern): source-level structural assertions.
 *   createComposeRule() is avoided — `manifest = Config.NONE` runs do not
 *   provide an Activity.
 *
 *   Anti-bluff mutation guards (CONST-035):
 *     1. Stub composable body to empty (no LazyColumn, no testTag, no
 *        buttons) → ALL FOUR tests FAIL because every structural marker
 *        they assert disappears from the source text.
 *     2. Remove testTag("rename-preview-panel") → rendersAllFilesAsRows FAILS.
 *     3. Remove testTag("rename-file-<uri>") → rendersAllFilesAsRows FAILS.
 *     4. Stub onApply invocation to no-op (remove onApply(edit)) →
 *        applyButton_invokesCallback FAILS.
 *     5. Stub onDismiss invocation in Cancel path to no-op →
 *        cancelButton_invokesCallback FAILS.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class RenamePreviewPanelRobolectricTest {

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

    private fun loadPanelSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/rename/RenamePreviewPanel.kt"
    )

    private fun loadActionSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/rename/RenameAction.kt"
    )

    // -----------------------------------------------------------------------
    // Test 1: 2-file WorkspaceEdit → 2 rows rendered (structural anchors)
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing testTag("rename-preview-panel"), testTag("rename-file-\$uri"),
     * or the LazyColumn means UI instrumentation and future HelixQA tests cannot
     * locate rows or the root → this check FAILS.
     *
     * Mutation: stub Composable body to empty → FAIL.
     */
    @Test
    fun rendersAllFilesAsRows() {
        val src = loadPanelSource()

        assertTrue(
            "RenamePreviewPanel MUST apply testTag(\"rename-preview-panel\") to its content root",
            src.contains("""testTag("rename-preview-panel")"""),
        )
        assertTrue(
            "RenamePreviewPanel MUST apply testTag(\"rename-file-\$uri\") to each file row",
            src.contains("""testTag("rename-file-${"$"}uri")"""),
        )
        assertTrue(
            "RenamePreviewPanel MUST use LazyColumn for the list of files",
            src.contains("LazyColumn"),
        )
        assertTrue(
            "RenamePreviewPanel MUST iterate edit.changes entries",
            src.contains("edit.changes"),
        )
        assertTrue(
            "RenamePreviewPanel MUST use ModalBottomSheet for the mobile sheet UX",
            src.contains("ModalBottomSheet("),
        )
        assertTrue(
            "RenamePreviewPanel MUST use @OptIn(ExperimentalMaterial3Api::class)",
            src.contains("ExperimentalMaterial3Api"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: Expansion — tap row toggles diff display
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing the isExpanded state and the conditional diff block
     * means tapping a file row never shows TextEdit details → this structural
     * check FAILS because "isExpanded" and the per-edit rendering vanish.
     *
     * Mutation: remove expansion logic → FAIL.
     */
    @Test
    fun expansion_revealsEdits() {
        val src = loadPanelSource()

        assertTrue(
            "RenamePreviewPanel MUST maintain per-URI expanded state (mutableStateMapOf)",
            src.contains("mutableStateMapOf"),
        )
        assertTrue(
            "RenamePreviewPanel file row MUST toggle expanded state on click (onToggle)",
            src.contains("onToggle"),
        )
        assertTrue(
            "RenamePreviewPanel MUST render edit details when isExpanded == true",
            src.contains("isExpanded"),
        )
        assertTrue(
            "RenamePreviewPanel MUST show textEdit.range for each expanded edit",
            src.contains("textEdit.range"),
        )
        assertTrue(
            "RenamePreviewPanel MUST show textEdit.newText for each expanded edit",
            src.contains("textEdit.newText"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 3: Apply button invokes onApply callback with WorkspaceEdit
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: stubbing the Apply button's onClick to a no-op (removing
     * onApply(edit)) means the callback is never fired → this structural
     * check FAILS because the "onApply(edit)" call-site disappears.
     *
     * Mutation: replace onApply(edit) with Unit → FAIL.
     */
    @Test
    fun applyButton_invokesCallback() {
        val src = loadPanelSource()

        assertTrue(
            "RenamePreviewPanel MUST apply testTag(\"rename-apply\") to the Apply button",
            src.contains("""testTag("rename-apply")"""),
        )
        assertTrue(
            "RenamePreviewPanel Apply button onClick MUST call onApply(edit)",
            src.contains("onApply(edit)"),
        )
        assertTrue(
            "RenamePreviewPanel MUST use Button for the Apply action",
            src.contains("Button("),
        )
    }

    // -----------------------------------------------------------------------
    // Test 4: Cancel button invokes onDismiss callback
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: stubbing the Cancel button's onClick to a no-op (removing
     * the onDismiss call) means the callback is never fired → this structural
     * check FAILS because the "onDismiss" call-site in the Cancel path disappears.
     *
     * Mutation: replace onDismiss in Cancel onClick with Unit → FAIL.
     */
    @Test
    fun cancelButton_invokesCallback() {
        val src = loadPanelSource()

        assertTrue(
            "RenamePreviewPanel MUST apply testTag(\"rename-cancel\") to the Cancel button",
            src.contains("""testTag("rename-cancel")"""),
        )
        assertTrue(
            "RenamePreviewPanel Cancel button MUST call onDismiss",
            src.contains("OutlinedButton("),
        )
        assertTrue(
            "RenamePreviewPanel MUST supply onDismissRequest = onDismiss to ModalBottomSheet",
            src.contains("onDismissRequest = onDismiss"),
        )
        // Verify the action source correctly wires rename requester → preview panel.
        val actionSrc = loadActionSource()
        assertTrue(
            "RenameAction MUST invoke requester.rename(...)",
            actionSrc.contains("requester.rename("),
        )
        assertTrue(
            "RenameAction MUST show RenamePreviewPanel on non-empty WorkspaceEdit",
            actionSrc.contains("RenamePreviewPanel("),
        )
        assertTrue(
            "RenameAction MUST show Toast when edit is null or empty",
            actionSrc.contains("No rename changes available."),
        )
        assertTrue(
            "RenameAction MUST call onApplyEdit with the confirmed WorkspaceEdit",
            actionSrc.contains("onApplyEdit("),
        )
    }
}
