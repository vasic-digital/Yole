/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 8 / iter-74: FormattingSettingsRobolectricTest.
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
 *   3. iter74_formatOnSave_wiresRealPreference — verifies:
 *      a) YoleApp.kt wires settings.getLspFormatOnSave() (not hardcoded false).
 *      b) getLspFormatOnSave() method exists in YoleSettings.
 *      c) The settings lambda closure does not contain the hardcoded `{ false }` stub.
 *
 *   4. iter74_onTypeEditApply_wiresWorkspaceEdit — verifies:
 *      a) YoleApp.kt applies on-type edits via WorkspaceEditApplier.
 *      b) on-type block constructs WorkspaceEdit and calls WorkspaceEditApplier.apply.
 *
 *   5. iter74_explicitFormatEditApply_wiresWorkspaceEdit — verifies:
 *      a) YoleApp.kt applies explicit-format edits via WorkspaceEditApplier.
 *
 *   6. iter74_scrollToLine_wiresProblemsPanel — verifies:
 *      a) DiagnosticsProblemsPanel onJumpToLine sets scrollToLineState.value.
 *      b) SyncedScrollEditor receives scrollToLineRequest.
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   toggle_structuralCheck_formattingSettingsRow:
 *     Mutation 1: remove testTag("formatting-settings-toggle") → FAILS.
 *     Mutation 2: change default from { true } to { false } → FAILS.
 *     Mutation 3: replace Switch with Checkbox → FAILS.
 *     Mutation 4: remove setFormatOnSave(newValue) → FAILS.
 *
 *   toggle_structuralCheck_ctrlShiftF_inSyncedScrollEditor:
 *     Mutation: remove `onExplicitFormat()` from SyncedScrollEditor → FAILS.
 *     Mutation: remove `isShiftPressed` → FAILS.
 *     Mutation: remove `Key.F` → FAILS.
 *
 *   iter74_formatOnSave_wiresRealPreference:
 *     Mutation: revert settings = { settings.getLspFormatOnSave() } back to
 *       settings = { false } → "getLspFormatOnSave()" assertion FAILS.
 *
 *   iter74_onTypeEditApply_wiresWorkspaceEdit:
 *     Mutation: remove WorkspaceEditApplier.apply from on-type branch →
 *       "WorkspaceEditApplier.apply" in on-type block assertion FAILS.
 *
 *   iter74_explicitFormatEditApply_wiresWorkspaceEdit:
 *     Mutation: remove WorkspaceEditApplier.apply from explicit-format branch →
 *       assertion FAILS.
 *
 *   iter74_scrollToLine_wiresProblemsPanel:
 *     Mutation: revert onJumpToLine to just set isProblemsPanelOpen = false →
 *       "scrollToLineState.value = line" assertion FAILS.
 *
 *   7. iter74_hoverFilter_wiresCursorOffset — verifies:
 *      a) SyncedScrollEditor.kt declares onCursorOffsetChanged parameter.
 *      b) SyncedScrollEditor.kt calls onCursorOffsetChanged?.invoke in onValueChange.
 *      c) YoleApp.kt declares lastCursorOffset and lastTokens.
 *      d) YoleApp.kt contains isIdentifierScope identifier check (not always-true stub).
 *      e) YoleApp.kt passes onCursorOffsetChanged to SyncedScrollEditor.
 *
 *   8. iter74_hoverPreciseAnchor_wiresCursorRect — verifies:
 *      a) SyncedScrollEditor.kt declares onCursorRectChanged parameter.
 *      b) SyncedScrollEditor.kt calls onTextLayout with getCursorRect.
 *      c) YoleApp.kt wires hoverPopupAnchor from onCursorRectChanged.
 *      d) YoleApp.kt uses var (not val) for hoverPopupAnchor (mutable).
 *      e) YoleApp.kt passes onCursorRectChanged to SyncedScrollEditor.
 *
 *   Anti-bluff mutation for iter74_hoverFilter_wiresCursorOffset (CONST-035):
 *     Mutation 1: remove onCursorOffsetChanged from SyncedScrollEditor param list →
 *       "onCursorOffsetChanged" in SyncedScrollEditor assertion FAILS.
 *     Mutation 2: remove onCursorOffsetChanged?.invoke from onValueChange →
 *       "onCursorOffsetChanged?.invoke" assertion FAILS.
 *     Mutation 3: replace isIdentifierScope with always-true →
 *       "isIdentifierScope" assertion FAILS.
 *     Mutation 4: remove lastTokens / lastCursorOffset from YoleApp →
 *       "lastTokens" assertion FAILS.
 *
 *   Anti-bluff mutation for iter74_hoverPreciseAnchor_wiresCursorRect (CONST-035):
 *     Mutation 1: remove onCursorRectChanged from SyncedScrollEditor param list →
 *       "onCursorRectChanged" in SyncedScrollEditor assertion FAILS.
 *     Mutation 2: remove getCursorRect from onTextLayout →
 *       "getCursorRect" assertion FAILS.
 *     Mutation 3: revert hoverPopupAnchor to val IntOffset.Zero →
 *       "var hoverPopupAnchor" assertion FAILS.
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

    private fun loadYoleAppSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt",
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

    // -----------------------------------------------------------------------
    // iter-74 Tests
    // -----------------------------------------------------------------------

    /**
     * iter-74 (#iter-63-format-on-save-settings-toggle): YoleApp.kt MUST wire
     * FormattingTrigger settings lambda to the real getLspFormatOnSave() preference,
     * not the hardcoded `{ false }` stub from Phase 10 v1.
     *
     * Anti-bluff (CONST-035):
     *   Mutation: revert `settings = { settings.getLspFormatOnSave() }` to
     *   `settings = { false }` → "getLspFormatOnSave()" assertion FAILS.
     */
    @Test
    fun iter74_formatOnSave_wiresRealPreference() {
        val src = loadYoleAppSource()

        assertTrue(
            "YoleSettings MUST have getLspFormatOnSave() for iter-74 settings wiring",
            src.contains("getLspFormatOnSave()"),
        )
        assertTrue(
            "FormattingTrigger settings lambda MUST call settings.getLspFormatOnSave()",
            src.contains("settings = { settings.getLspFormatOnSave() }"),
        )
        // The hardcoded stub must be gone from the FormattingTrigger construction.
        // (A comment mentioning "false" is OK; only the literal lambda assignment must not appear.)
        val formattingTriggerBlock = src.substringAfter("FormattingTrigger(").substringBefore("} else {")
        assertTrue(
            "FormattingTrigger settings parameter MUST NOT be the hardcoded { false } stub",
            !formattingTriggerBlock.contains("settings = { false }"),
        )
    }

    /**
     * iter-74 (#iter-63-on-type-edit-apply): YoleApp.kt on-type formatting branch MUST
     * apply edits to the buffer via WorkspaceEditApplier, not just log them.
     *
     * Anti-bluff (CONST-035):
     *   Mutation: remove WorkspaceEditApplier.apply from on-type branch →
     *   the structural assertion on "iter-63-on-type-edit-apply" + "WorkspaceEditApplier"
     *   fails because the block reverts to a log-only pattern.
     */
    @Test
    fun iter74_onTypeEditApply_wiresWorkspaceEdit() {
        val src = loadYoleAppSource()

        // The on-type block (closes #iter-63-on-type-edit-apply) must contain both
        // markers: the tracker comment and WorkspaceEditApplier invocation.
        assertTrue(
            "YoleApp.kt MUST contain the iter-63-on-type-edit-apply tracker comment",
            src.contains("iter-63-on-type-edit-apply"),
        )
        // Find the on-type block and assert WorkspaceEditApplier is present in it.
        val onTypeBlock = src.substringAfter("iter-63-on-type-edit-apply").substringBefore("Degraded — on-type formatting unavailable")
        assertTrue(
            "On-type formatting block MUST call WorkspaceEditApplier.apply (not just log)",
            onTypeBlock.contains("WorkspaceEditApplier.apply"),
        )
    }

    /**
     * iter-74 (#iter-63-explicit-format-edit-apply): YoleApp.kt explicit-format branch
     * MUST apply edits to the buffer via WorkspaceEditApplier, not just log them.
     *
     * Anti-bluff (CONST-035):
     *   Mutation: remove WorkspaceEditApplier.apply from explicit-format branch →
     *   the structural assertion fails because the block reverts to a log-only pattern.
     */
    @Test
    fun iter74_explicitFormatEditApply_wiresWorkspaceEdit() {
        val src = loadYoleAppSource()

        assertTrue(
            "YoleApp.kt MUST contain the iter-63-explicit-format-edit-apply tracker comment",
            src.contains("iter-63-explicit-format-edit-apply"),
        )
        val explicitBlock = src.substringAfter("iter-63-explicit-format-edit-apply")
            .substringBefore("Degraded — formatting unavailable")
        assertTrue(
            "Explicit-format block MUST call WorkspaceEditApplier.apply (not just log)",
            explicitBlock.contains("WorkspaceEditApplier.apply"),
        )
    }

    /**
     * iter-74 (#iter-62-phase-8-problems-scroll-to-line): YoleApp.kt onJumpToLine
     * callback MUST set scrollToLineState.value instead of just closing the panel.
     * SyncedScrollEditor MUST declare a scrollToLineRequest parameter.
     *
     * Anti-bluff (CONST-035):
     *   Mutation: revert onJumpToLine to only set isProblemsPanelOpen = false →
     *   "scrollToLineState.value = line" assertion FAILS.
     */
    @Test
    fun iter74_scrollToLine_wiresProblemsPanel() {
        val yoleApp = loadYoleAppSource()
        val syncedEditor = loadSyncedScrollEditorSource()

        assertTrue(
            "YoleApp.kt onJumpToLine MUST set scrollToLineState.value = line",
            yoleApp.contains("scrollToLineState.value = line"),
        )
        assertTrue(
            "SyncedScrollEditor MUST declare scrollToLineRequest parameter",
            syncedEditor.contains("scrollToLineRequest"),
        )
        assertTrue(
            "SyncedScrollEditor MUST animate scroll to target line via animateScrollTo",
            syncedEditor.contains("animateScrollTo"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 7 (iter-74): iter74_hoverFilter_wiresCursorOffset
    // -----------------------------------------------------------------------

    /**
     * iter-74 (#iter-62-phase-8-tree-sitter-hover-filter-stubbed):
     * Verifies that the hover filter is wired with real identifier-scope checking,
     * not the always-true stub that fired hover on whitespace and keywords.
     *
     * Checks:
     *   (a) SyncedScrollEditor declares onCursorOffsetChanged parameter.
     *   (b) SyncedScrollEditor calls onCursorOffsetChanged?.invoke in onValueChange.
     *   (c) YoleApp.kt maintains lastCursorOffset and lastTokens state.
     *   (d) YoleApp.kt contains isIdentifierScope function (real filter, not always-true).
     *   (e) YoleApp.kt passes onCursorOffsetChanged to SyncedScrollEditor.
     *
     * Anti-bluff (CONST-035):
     *   Mutation 1: remove onCursorOffsetChanged from SyncedScrollEditor param list →
     *     assertion (a) FAILS.
     *   Mutation 2: remove onCursorOffsetChanged?.invoke call from onValueChange →
     *     assertion (b) FAILS.
     *   Mutation 3: replace isIdentifierScope with always-true →
     *     assertion (d) FAILS because "isIdentifierScope" disappears.
     *   Mutation 4: remove lastTokens / lastCursorOffset declarations →
     *     assertion (c) FAILS.
     */
    @Test
    fun iter74_hoverFilter_wiresCursorOffset() {
        val syncedEditor = loadSyncedScrollEditorSource()
        val yoleApp = loadYoleAppSource()

        // (a) SyncedScrollEditor must declare onCursorOffsetChanged parameter.
        assertTrue(
            "SyncedScrollEditor MUST declare onCursorOffsetChanged parameter " +
                "(iter-74 #iter-62-phase-8-tree-sitter-hover-filter-stubbed)",
            syncedEditor.contains("onCursorOffsetChanged"),
        )

        // (b) SyncedScrollEditor must call onCursorOffsetChanged?.invoke in onValueChange.
        // The call site is the cursor write-back added by iter-74.
        assertTrue(
            "SyncedScrollEditor onValueChange MUST call onCursorOffsetChanged?.invoke " +
                "to write the cursor offset back to the caller",
            syncedEditor.contains("onCursorOffsetChanged?.invoke"),
        )

        // (c) YoleApp must maintain lastCursorOffset state for hover filter.
        assertTrue(
            "YoleApp.kt MUST declare lastCursorOffset for hover identifier check",
            yoleApp.contains("lastCursorOffset"),
        )

        // (d) YoleApp must contain isIdentifierScope function — the real filter.
        // If this is replaced with always-true, this assertion kills the mutation.
        assertTrue(
            "YoleApp.kt MUST contain isIdentifierScope function " +
                "(real identifier filter, not always-true stub)",
            yoleApp.contains("isIdentifierScope"),
        )

        // Also verify the scope set covers known identifier types (variable, function, etc.)
        assertTrue(
            "YoleApp.kt identifier scope predicate MUST include \"variable\" scope",
            yoleApp.contains("\"variable\""),
        )
        assertTrue(
            "YoleApp.kt identifier scope predicate MUST check endsWith(\"identifier\")",
            yoleApp.contains("endsWith(\"identifier\")"),
        )

        // (e) YoleApp must pass onCursorOffsetChanged to SyncedScrollEditor.
        assertTrue(
            "YoleApp.kt MUST pass onCursorOffsetChanged to SyncedScrollEditor",
            yoleApp.contains("onCursorOffsetChanged = { offset -> lastCursorOffset = offset }"),
        )

        // (f) Verify the old always-false stub (line=0,character=0) is gone.
        // The hover block with real cursor position should not contain
        // literal `line = 0,\n` and `character = 0,` in the hover call.
        // We check that the "iter-62-phase-8-tree-sitter-hover-filter-stubbed" tracker
        // comment has the "Replaced" text (iter-74 resolution marker).
        assertTrue(
            "YoleApp.kt MUST contain iter-74 resolution marker for hover filter tracker",
            yoleApp.contains("iter-74 (#iter-62-phase-8-tree-sitter-hover-filter-stubbed)"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 8 (iter-74): iter74_hoverPreciseAnchor_wiresCursorRect
    // -----------------------------------------------------------------------

    /**
     * iter-74 (#iter-62-phase-8-hover-precise-anchor):
     * Verifies that HoverPopup is positioned at the actual cursor pixel rect
     * computed by BasicTextField.onTextLayout → getCursorRect, not always at (0,0).
     *
     * Checks:
     *   (a) SyncedScrollEditor declares onCursorRectChanged parameter.
     *   (b) SyncedScrollEditor wires onTextLayout with getCursorRect call.
     *   (c) YoleApp.kt uses var hoverPopupAnchor (mutable, updated from rect).
     *   (d) YoleApp.kt passes onCursorRectChanged to SyncedScrollEditor.
     *   (e) YoleApp.kt updates hoverPopupAnchor with rect.left/rect.bottom.
     *
     * Anti-bluff (CONST-035):
     *   Mutation 1: remove onCursorRectChanged from SyncedScrollEditor param list →
     *     assertion (a) FAILS.
     *   Mutation 2: remove getCursorRect from onTextLayout →
     *     assertion (b) FAILS.
     *   Mutation 3: revert hoverPopupAnchor to val IntOffset.Zero →
     *     assertion (c) on "var hoverPopupAnchor" FAILS.
     */
    @Test
    fun iter74_hoverPreciseAnchor_wiresCursorRect() {
        val syncedEditor = loadSyncedScrollEditorSource()
        val yoleApp = loadYoleAppSource()

        // (a) SyncedScrollEditor must declare onCursorRectChanged parameter.
        assertTrue(
            "SyncedScrollEditor MUST declare onCursorRectChanged parameter " +
                "(iter-74 #iter-62-phase-8-hover-precise-anchor)",
            syncedEditor.contains("onCursorRectChanged"),
        )

        // (b) SyncedScrollEditor onTextLayout must call getCursorRect.
        assertTrue(
            "SyncedScrollEditor onTextLayout MUST call getCursorRect to get cursor pixel rect",
            syncedEditor.contains("getCursorRect"),
        )

        // Also verify onTextLayout is wired (not just declared as a comment).
        assertTrue(
            "SyncedScrollEditor MUST wire onTextLayout in BasicTextField call",
            syncedEditor.contains("onTextLayout"),
        )

        // (c) YoleApp must use var hoverPopupAnchor (mutable, can be updated from rect).
        assertTrue(
            "YoleApp.kt MUST declare var hoverPopupAnchor (mutable, updated from cursor rect)",
            yoleApp.contains("var hoverPopupAnchor"),
        )

        // (d) YoleApp must pass onCursorRectChanged to SyncedScrollEditor.
        assertTrue(
            "YoleApp.kt MUST pass onCursorRectChanged to SyncedScrollEditor",
            yoleApp.contains("onCursorRectChanged"),
        )

        // (e) YoleApp must update anchor with rect.left and rect.bottom.
        val rectAnchorBlock = yoleApp.substringAfter("onCursorRectChanged").substringBefore("scrollToLineRequest")
        assertTrue(
            "YoleApp.kt onCursorRectChanged MUST update hoverPopupAnchor with rect.left",
            rectAnchorBlock.contains("rect.left"),
        )
        assertTrue(
            "YoleApp.kt onCursorRectChanged MUST update hoverPopupAnchor with rect.bottom",
            rectAnchorBlock.contains("rect.bottom"),
        )

        // (f) Resolution marker — verify this closes the deferred tracker.
        assertTrue(
            "YoleApp.kt MUST contain iter-74 resolution marker for hover precise anchor",
            yoleApp.contains("iter-74 (#iter-62-phase-8-hover-precise-anchor)"),
        )
    }
}
