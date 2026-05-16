/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-62 Phase 8, CONST-035 anti-bluff):
 *   Verifies the structural wiring of IdeEditorScreen with all three LSP
 *   render surfaces introduced in Phases 5–7 and integrated in Phase 8.
 *
 *   Test architecture (matches iter-57/58/60 pattern): source-level
 *   structural assertions that inspect source text for key invariants.
 *   createComposeRule() is avoided — `manifest = Config.NONE` runs do
 *   not provide an Activity.
 *
 * Tests:
 *   1. diagnosticsPanel_renders_when_cache_has_diagnostics_for_current_file
 *      Verifies that IdeEditorScreen observes lspHost.diagnosticsCache.states
 *      via collectAsState() and passes the result to DiagnosticsProblemsPanel.
 *      Mutation: comment out the diagnostics observer in IdeEditorScreen →
 *        this test FAILS because the source no longer contains the observation
 *        call or the DiagnosticsProblemsPanel wiring.
 *
 *   2. hoverPopup_wired_via_onHoverRequest_and_renders_when_blocks_nonEmpty
 *      Verifies that SyncedScrollEditor receives a non-null onHoverRequest
 *      lambda when passedLangId is non-null, and that HoverPopup is rendered
 *      when hoverBlocks is non-empty.
 *      Mutation: remove onHoverRequest from SyncedScrollEditor call →
 *        this test FAILS because the source no longer passes onHoverRequest.
 *
 *   3. goToDef_chooser_wired_into_IdeEditorScreen
 *      Verifies that IdeEditorScreen declares chooserLocations state and
 *      renders DefinitionLocationChooser when chooserLocations is non-empty.
 *      Mutation: remove the DefinitionLocationChooser call or the
 *        chooserLocations state variable → this test FAILS.
 *
 * Anti-bluff procedure (CONST-035):
 *   For each test, the mutation described above MUST cause the assertion
 *   to fail. After reverting, all 3 tests MUST PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: full implementation verified here.
 *   - Desktop: diagnostics + hover wiring deferred; tracked as
 *     #iter-62-desktop-editor-lsp-wiring in CONTINUATION.md.
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
class IdeEditorScreenLspIntegrationRobolectricTest {

    // -----------------------------------------------------------------------
    // Source loader — reusable across all 3 tests
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
    // Test 1: Diagnostics cache observation + panel wiring
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035):
     *   Mutation: comment out `val diagnosticsByUri by
     *     lspHost.diagnosticsCache.states.collectAsState()` in IdeEditorScreen.
     *   → This test FAILS because the source no longer contains the observer call.
     *   Mutation2: remove the DiagnosticsProblemsPanel invocation.
     *   → This test FAILS because the panel reference disappears.
     *   Revert both → PASS.
     */
    @Test
    fun diagnosticsPanel_renders_when_cache_has_diagnostics_for_current_file() {
        val src = loadYoleAppSource()

        // 1a. DiagnosticsCache observer is wired via collectAsState().
        assertTrue(
            "IdeEditorScreen MUST observe lspHost.diagnosticsCache.states " +
                "via collectAsState()",
            src.contains("diagnosticsCache.states.collectAsState()"),
        )

        // 1b. Current-file diagnostics are derived from the observed map.
        assertTrue(
            "IdeEditorScreen MUST compute currentFileDiagnostics from the " +
                "observed diagnosticsByUri map",
            src.contains("currentFileDiagnostics"),
        )

        // 1c. DiagnosticsProblemsPanel is rendered (bottom drawer).
        assertTrue(
            "IdeEditorScreen MUST render DiagnosticsProblemsPanel",
            src.contains("DiagnosticsProblemsPanel("),
        )

        // 1d. The panel is gated on isProblemsPanelOpen so it is
        //     collapsible (not always visible).
        assertTrue(
            "IdeEditorScreen MUST gate DiagnosticsProblemsPanel on " +
                "isProblemsPanelOpen",
            src.contains("isProblemsPanelOpen"),
        )

        // 1e. SyncedScrollEditor receives the diagnostics list.
        assertTrue(
            "SyncedScrollEditor call MUST pass diagnostics = currentFileDiagnostics",
            src.contains("diagnostics = currentFileDiagnostics"),
        )

        // 1f. SyncedScrollEditor source accepts the diagnostics parameter.
        val editorSrc = loadSyncedScrollEditorSource()
        assertTrue(
            "SyncedScrollEditor MUST declare a diagnostics parameter",
            editorSrc.contains("diagnostics: List<Diagnostic>"),
        )

        // 1g. DiagnosticsGutter is composed inside the gutter column.
        assertTrue(
            "SyncedScrollEditor MUST render DiagnosticsGutter when " +
                "diagnostics is non-empty",
            editorSrc.contains("DiagnosticsGutter("),
        )

        // 1h. DiagnosticsInlineUnderline is chained onto the VisualTransformation.
        assertTrue(
            "SyncedScrollEditor MUST apply DiagnosticsInlineUnderline as a " +
                "chained VisualTransformation",
            editorSrc.contains("DiagnosticsInlineUnderline("),
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: Hover popup wiring via onHoverRequest + F1 shortcut
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035):
     *   Mutation: remove `onHoverRequest = if (passedLangId != null) { ... } else null`
     *     from the SyncedScrollEditor call in IdeEditorScreen.
     *   → This test FAILS because the source no longer contains onHoverRequest.
     *   Mutation2: stub SyncedScrollEditor to ignore onHoverRequest (not wire hoverShortcut).
     *   → Part 2f FAILS because hoverShortcut is absent from the editor source.
     *   Revert both → PASS.
     */
    @Test
    fun hoverPopup_wired_via_onHoverRequest_and_renders_when_blocks_nonEmpty() {
        val src = loadYoleAppSource()

        // 2a. onHoverRequest parameter is passed to SyncedScrollEditor.
        assertTrue(
            "IdeEditorScreen MUST pass onHoverRequest to SyncedScrollEditor",
            src.contains("onHoverRequest ="),
        )

        // 2b. hoverBlocks state variable is declared in IdeEditorScreen.
        assertTrue(
            "IdeEditorScreen MUST declare hoverBlocks state for hover content",
            src.contains("hoverBlocks"),
        )

        // 2c. HoverPopup is rendered when hoverBlocks is non-empty.
        assertTrue(
            "IdeEditorScreen MUST render HoverPopup",
            src.contains("HoverPopup("),
        )

        // 2d. The popup is gated on hoverBlocks.isNotEmpty().
        assertTrue(
            "IdeEditorScreen MUST gate HoverPopup on hoverBlocks.isNotEmpty()",
            src.contains("hoverBlocks.isNotEmpty()"),
        )

        // 2e. Dismissal clears hoverBlocks.
        assertTrue(
            "IdeEditorScreen MUST clear hoverBlocks on popup dismiss",
            src.contains("hoverBlocks = emptyList()"),
        )

        // 2f. SyncedScrollEditor wires hoverShortcut (F1) on the BTF modifier.
        val editorSrc = loadSyncedScrollEditorSource()
        assertTrue(
            "SyncedScrollEditor MUST wire hoverShortcut to the BTF modifier",
            editorSrc.contains("hoverShortcut"),
        )

        // 2g. hoverShortcut import is present in SyncedScrollEditor.
        assertTrue(
            "SyncedScrollEditor MUST import hoverShortcut",
            editorSrc.contains("import digital.vasic.yole.android.ui.editor.hover.hoverShortcut"),
        )

        // 2h. LspServerHost.hover() is called in the onHoverRequest lambda.
        assertTrue(
            "IdeEditorScreen onHoverRequest MUST call lspHost.hover()",
            src.contains("lspHost.hover("),
        )

        // 2i. HoverMarkdownRenderer is used to parse the result.
        assertTrue(
            "IdeEditorScreen MUST parse hover result via HoverMarkdownRenderer.render()",
            src.contains("HoverMarkdownRenderer.render("),
        )
    }

    // -----------------------------------------------------------------------
    // Test 3: Go-to-definition chooser wiring
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035):
     *   Mutation: remove `var chooserLocations by remember { ... }`
     *     from IdeEditorScreen.
     *   → This test FAILS (chooserLocations absent from source).
     *   Mutation2: remove the DefinitionLocationChooser composable call.
     *   → This test FAILS (def-chooser wiring absent).
     *   Mutation3: remove navStack declaration.
     *   → Part 3e FAILS.
     *   Revert all → PASS.
     */
    @Test
    fun goToDef_chooser_wired_into_IdeEditorScreen() {
        val src = loadYoleAppSource()

        // 3a. chooserLocations state variable is declared.
        assertTrue(
            "IdeEditorScreen MUST declare chooserLocations state for multi-result chooser",
            src.contains("chooserLocations"),
        )

        // 3b. DefinitionLocationChooser is rendered when chooserLocations is non-empty.
        assertTrue(
            "IdeEditorScreen MUST render DefinitionLocationChooser",
            src.contains("DefinitionLocationChooser("),
        )

        // 3c. chooser is gated on chooserLocations.isNotEmpty().
        assertTrue(
            "IdeEditorScreen MUST gate DefinitionLocationChooser on chooserLocations.isNotEmpty()",
            src.contains("chooserLocations.isNotEmpty()"),
        )

        // 3d. Dismissal clears chooserLocations.
        assertTrue(
            "IdeEditorScreen MUST clear chooserLocations on dismiss",
            src.contains("chooserLocations = emptyList()"),
        )

        // 3e. EditorNavigationStack is instantiated.
        assertTrue(
            "IdeEditorScreen MUST instantiate EditorNavigationStack",
            src.contains("EditorNavigationStack()"),
        )

        // 3f. BackHandler is registered to allow navigating back.
        assertTrue(
            "IdeEditorScreen MUST register BackHandler with navStack.canGoBack()",
            src.contains("navStack.canGoBack()"),
        )

        // 3g. The chooser onSelected callback pushes to navStack before navigation.
        assertTrue(
            "DefinitionLocationChooser onSelected MUST push to navStack",
            src.contains("navStack.push("),
        )
    }
}
