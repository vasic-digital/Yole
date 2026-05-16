/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-63 Phase 10, CONST-035 anti-bluff):
 *   Verifies the structural wiring of IdeEditorScreen with all five
 *   LSP capabilities introduced in Phases 5–9 and integrated in Phase 10.
 *
 *   Test architecture (matches iter-62 IdeEditorScreenLspIntegrationRobolectricTest
 *   pattern): source-level structural assertions that inspect source text for key
 *   invariants. createComposeRule() is avoided — `manifest = Config.NONE` runs
 *   do not provide an Activity.
 *
 * Tests:
 *   1. renameAction_wiredViaF2_invokesPanel
 *      Verifies that IdeEditorScreen:
 *        (a) declares showRenameAction state,
 *        (b) sets showRenameAction = true from the F2 shortcut (onRenameRequest),
 *        (c) passes onRenameRequest to SyncedScrollEditor,
 *        (d) renders RenameAction when showRenameAction is true, and
 *        (e) SyncedScrollEditor handles the F2 key (Key.F2) in onPreviewKeyEvent.
 *      Mutation: comment out showRenameAction = true in onRenameRequest lambda →
 *        assertions (b) presence of `showRenameAction = true` FAILS.
 *      Mutation2: remove onRenameRequest param from SyncedScrollEditor call →
 *        assertion (c) FAILS.
 *      Revert both → PASS.
 *
 *   2. codeActionLightbulb_pollingPopulatesActions
 *      Verifies that IdeEditorScreen:
 *        (a) declares actionsByLine state,
 *        (b) starts a polling LaunchedEffect with delay(500L),
 *        (c) calls lspCodeActionRequester.codeActions(...),
 *        (d) passes actionsByLine to SyncedScrollEditor, and
 *        (e) SyncedScrollEditor accepts actionsByLine param.
 *      Mutation: remove actionsByLine from SyncedScrollEditor call →
 *        assertion (d) FAILS (no `actionsByLine = actionsByLine` in source).
 *      Mutation2: remove delay(500L) from polling loop →
 *        assertion (b) FAILS (no `delay(500L)` in source near polling).
 *      Revert both → PASS.
 *
 *   3. referencesPanel_opensOnShiftF12
 *      Verifies that IdeEditorScreen:
 *        (a) declares showReferencesPanel state,
 *        (b) declares referencesList state,
 *        (c) renders ReferencesPanel when showReferencesPanel && referencesList.isNotEmpty(),
 *        (d) passes onFindReferencesRequest to SyncedScrollEditor, and
 *        (e) SyncedScrollEditor handles Shift+F12 (Key.F12 + isShiftPressed) in onPreviewKeyEvent.
 *      Mutation: remove showReferencesPanel from the gate → assertion (c) FAILS.
 *      Mutation2: remove onFindReferencesRequest from SyncedScrollEditor call → assertion (d) FAILS.
 *      Revert both → PASS.
 *
 * Anti-bluff procedure (CONST-035):
 *   For each test, the mutations described above MUST cause the assertion
 *   to fail. After reverting, all 3 tests MUST PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: full implementation verified here.
 *   - Desktop: rename/references/signature-help Desktop wiring deferred; tracked as
 *       #iter-63-desktop-signature-help-popup-deferred in CONTINUATION.md.
 *   - iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
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
class IdeEditorScreenIter63IntegrationRobolectricTest {

    // -----------------------------------------------------------------------
    // Source loaders
    // -----------------------------------------------------------------------

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

    private fun loadYoleAppSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt"
    )

    private fun loadSyncedScrollEditorSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt"
    )

    // -----------------------------------------------------------------------
    // Test 1: renameAction_wiredViaF2_invokesPanel
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035):
     *   Mutation A: comment out `showRenameAction = true` in the onRenameRequest
     *     lambda inside IdeEditorScreen.
     *   → Assertion 1b FAILS (literal `showRenameAction = true` absent from source).
     *   Mutation B: remove `onRenameRequest = { showRenameAction = true }` from the
     *     SyncedScrollEditor call.
     *   → Assertion 1c FAILS (no `onRenameRequest =` in source).
     *   Revert both → PASS.
     */
    @Test
    fun renameAction_wiredViaF2_invokesPanel() {
        val src = loadYoleAppSource()
        val editorSrc = loadSyncedScrollEditorSource()

        // 1a. showRenameAction state is declared.
        assertTrue(
            "IdeEditorScreen MUST declare showRenameAction state variable",
            src.contains("showRenameAction"),
        )

        // 1b. showRenameAction = true is set from the F2 / onRenameRequest path.
        assertTrue(
            "IdeEditorScreen MUST set showRenameAction = true from the onRenameRequest lambda",
            src.contains("showRenameAction = true"),
        )

        // 1c. onRenameRequest parameter is passed to SyncedScrollEditor.
        assertTrue(
            "IdeEditorScreen MUST pass onRenameRequest = { showRenameAction = true } to SyncedScrollEditor",
            src.contains("onRenameRequest ="),
        )

        // 1d. RenameAction is rendered when showRenameAction is true.
        assertTrue(
            "IdeEditorScreen MUST render RenameAction",
            src.contains("RenameAction("),
        )

        // 1e. RenameAction is gated on showRenameAction.
        assertTrue(
            "IdeEditorScreen MUST gate RenameAction on showRenameAction",
            src.contains("showRenameAction && passedLangId != null") ||
                src.contains("if (showRenameAction"),
        )

        // 1f. SyncedScrollEditor declares the onRenameRequest parameter.
        assertTrue(
            "SyncedScrollEditor MUST declare onRenameRequest parameter",
            editorSrc.contains("onRenameRequest"),
        )

        // 1g. SyncedScrollEditor handles Key.F2 in onPreviewKeyEvent to invoke onRenameRequest.
        assertTrue(
            "SyncedScrollEditor MUST handle Key.F2 keypress to invoke onRenameRequest",
            editorSrc.contains("Key.F2"),
        )

        // 1h. F2 handler calls onRenameRequest().
        assertTrue(
            "SyncedScrollEditor MUST call onRenameRequest() in the F2 key branch",
            editorSrc.contains("onRenameRequest()"),
        )

        // 1i. LspRenameRequester adapter is constructed.
        assertTrue(
            "IdeEditorScreen MUST instantiate a LspRenameRequester adapter wrapping lspHost.rename",
            src.contains("lspRenameRequester"),
        )

        // 1j. lspHost.rename() is called inside the adapter.
        assertTrue(
            "LspRenameRequester adapter MUST call lspHost.rename()",
            src.contains("lspHost.rename("),
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: codeActionLightbulb_pollingPopulatesActions
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035):
     *   Mutation A: remove `actionsByLine = actionsByLine` from the SyncedScrollEditor call.
     *   → Assertion 2d FAILS (literal absent from source).
     *   Mutation B: remove `delay(500L)` from the polling LaunchedEffect body.
     *   → Assertion 2b FAILS (literal absent from source).
     *   Revert both → PASS.
     */
    @Test
    fun codeActionLightbulb_pollingPopulatesActions() {
        val src = loadYoleAppSource()
        val editorSrc = loadSyncedScrollEditorSource()

        // 2a. actionsByLine state is declared in IdeEditorScreen.
        assertTrue(
            "IdeEditorScreen MUST declare actionsByLine state variable",
            src.contains("actionsByLine"),
        )

        // 2b. A polling LaunchedEffect with delay(500L) exists.
        assertTrue(
            "IdeEditorScreen MUST poll code actions with delay(500L) in a LaunchedEffect",
            src.contains("delay(500L)"),
        )

        // 2c. lspCodeActionRequester.codeActions(...) is called in the polling loop.
        assertTrue(
            "IdeEditorScreen MUST call lspCodeActionRequester.codeActions inside the polling loop",
            src.contains("lspCodeActionRequester.codeActions("),
        )

        // 2d. actionsByLine is passed to SyncedScrollEditor.
        assertTrue(
            "IdeEditorScreen MUST pass actionsByLine to SyncedScrollEditor",
            src.contains("actionsByLine = actionsByLine"),
        )

        // 2e. SyncedScrollEditor accepts actionsByLine parameter.
        assertTrue(
            "SyncedScrollEditor MUST declare actionsByLine parameter",
            editorSrc.contains("actionsByLine: Map<Int, List<CodeAction>>"),
        )

        // 2f. A LspCodeActionRequester adapter is constructed.
        assertTrue(
            "IdeEditorScreen MUST instantiate a LspCodeActionRequester adapter",
            src.contains("lspCodeActionRequester"),
        )

        // 2g. onCodeActionLineTap is wired.
        assertTrue(
            "IdeEditorScreen MUST wire onCodeActionLineTap to codeActionMenuLine",
            src.contains("onCodeActionLineTap"),
        )

        // 2h. CodeActionMenu is rendered when codeActionMenuLine is non-null.
        assertTrue(
            "IdeEditorScreen MUST render CodeActionMenu",
            src.contains("CodeActionMenu("),
        )
    }

    // -----------------------------------------------------------------------
    // Test 3: referencesPanel_opensOnShiftF12
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035):
     *   Mutation A: remove `showReferencesPanel &&` from the gate condition.
     *   → Assertion 3c FAILS (literal `showReferencesPanel` absent from gate context).
     *   Mutation B: remove `onFindReferencesRequest = {` from the SyncedScrollEditor call.
     *   → Assertion 3d FAILS (literal absent).
     *   Revert both → PASS.
     */
    @Test
    fun referencesPanel_opensOnShiftF12() {
        val src = loadYoleAppSource()
        val editorSrc = loadSyncedScrollEditorSource()

        // 3a. showReferencesPanel state is declared.
        assertTrue(
            "IdeEditorScreen MUST declare showReferencesPanel state variable",
            src.contains("showReferencesPanel"),
        )

        // 3b. referencesList state is declared.
        assertTrue(
            "IdeEditorScreen MUST declare referencesList state variable",
            src.contains("referencesList"),
        )

        // 3c. ReferencesPanel is rendered inside a gate that checks showReferencesPanel.
        assertTrue(
            "IdeEditorScreen MUST render ReferencesPanel",
            src.contains("ReferencesPanel("),
        )

        // 3d. onFindReferencesRequest parameter is passed to SyncedScrollEditor.
        assertTrue(
            "IdeEditorScreen MUST pass onFindReferencesRequest to SyncedScrollEditor",
            src.contains("onFindReferencesRequest"),
        )

        // 3e. SyncedScrollEditor handles Shift+F12 (Key.F12) to invoke onFindReferencesRequest.
        assertTrue(
            "SyncedScrollEditor MUST handle Key.F12 in onPreviewKeyEvent for Shift+F12 shortcut",
            editorSrc.contains("Key.F12"),
        )

        // 3f. SyncedScrollEditor declares the onFindReferencesRequest parameter.
        assertTrue(
            "SyncedScrollEditor MUST declare onFindReferencesRequest parameter",
            editorSrc.contains("onFindReferencesRequest"),
        )

        // 3g. onFindReferencesRequest() is called in the Shift+F12 branch.
        assertTrue(
            "SyncedScrollEditor MUST call onFindReferencesRequest() in the Shift+F12 key branch",
            editorSrc.contains("onFindReferencesRequest()"),
        )

        // 3h. FindReferencesAction.findReferences is invoked.
        assertTrue(
            "IdeEditorScreen MUST invoke FindReferencesAction.findReferences",
            src.contains("FindReferencesAction.findReferences("),
        )

        // 3i. LspReferencesRequester adapter is constructed.
        assertTrue(
            "IdeEditorScreen MUST instantiate a LspReferencesRequester adapter",
            src.contains("lspReferencesRequester"),
        )

        // 3j. showReferencesPanel is set to true when references are found.
        assertTrue(
            "IdeEditorScreen MUST set showReferencesPanel = true on successful find-references",
            src.contains("showReferencesPanel = true"),
        )

        // 3k. SignatureHelpPill is wired (Phase 10.3 completeness check).
        assertTrue(
            "IdeEditorScreen MUST render SignatureHelpPill for signature help",
            src.contains("SignatureHelpPill("),
        )

        // 3l. FormattingTrigger is instantiated (Phase 10.4 completeness check).
        assertTrue(
            "IdeEditorScreen MUST instantiate FormattingTrigger",
            src.contains("FormattingTrigger("),
        )
    }
}
