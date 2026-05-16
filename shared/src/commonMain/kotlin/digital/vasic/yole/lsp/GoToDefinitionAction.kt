/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 7: GoToDefinitionAction — pure orchestration object.
 *
 * Orchestrates the go-to-definition user flow:
 *   0 results → show toast ("No definition found at cursor").
 *   1 result  → push current location onto back-stack; open the target.
 *   N results → hand off to a chooser (the UI layer picks the target).
 *
 * Plan deviation (CONST-035 / testability):
 *   Original plan used LspServerHost directly. Because LspServerHost is an
 *   expect class that cannot be subclassed or mocked in commonTest, this
 *   object instead accepts [LspDefinitionRequester] — a thin interface
 *   wrapping the definition() call. Production callers (IdeEditorScreen,
 *   Phase 8) create an anonymous object delegating to their real host.
 *   This is a clean, intentional deviation that improves testability without
 *   changing observable behaviour.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub requester.definition to always return emptyList().
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.GoToDefinitionActionTests"
 *   3. Expect: one_result_pushes_and_opens FAILS (onOpenFileAt never called).
 *              multi_results_invokes_chooser FAILS (onChoose never called).
 *   4. Revert; confirm all 3 tests PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Common:  this object has no platform-specific imports.
 *   - Android: consumed by IdeEditorScreen wrapper in Phase 8.
 *   - Desktop: consumed by IdeEditorScreen wrapper in Phase 8.
 *   - iOS:     accessible; Phase 8 iOS wiring deferred.
 *   - Web:     accessible; Phase 8 Web wiring deferred.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Stateless orchestrator for the go-to-definition editor command.
 *
 * All side-effects (navigation, chooser presentation, toast) are delegated
 * via callbacks so the object stays platform-neutral and trivially testable.
 */
object GoToDefinitionAction {

    /**
     * Execute a go-to-definition request.
     *
     * @param requester      Thin wrapper over [LspServerHost.definition] (see [LspDefinitionRequester]).
     * @param langId         Language identifier of the active document (e.g. "kotlin", "python").
     * @param currentUri     Document URI of the file currently open in the editor.
     * @param currentCursor  Character offset of the cursor BEFORE the jump (saved to [stack]).
     * @param line           Zero-based line index of the cursor position for the LSP request.
     * @param character      Zero-based character index of the cursor position for the LSP request.
     * @param stack          Navigation history stack; current location pushed on a single-result jump.
     * @param onOpenFileAt   Called with (targetUri, cursorOffset) when exactly one result is found.
     * @param onChoose       Called with the full result list when more than one result is found.
     *                       Stack push happens AFTER the user selects from the chooser (Phase 8 wires that).
     * @param onToast        Called with a human-readable message when there are no results.
     */
    suspend fun goToDefinition(
        requester: LspDefinitionRequester,
        langId: String,
        currentUri: String,
        currentCursor: Int,
        line: Int,
        character: Int,
        stack: EditorNavigationStack,
        onOpenFileAt: (uri: String, cursorOffset: Int) -> Unit,
        onChoose: (List<DefinitionLocation>) -> Unit,
        onToast: (String) -> Unit,
    ) {
        val results = requester.definition(langId, currentUri, line, character)
        when {
            results.isEmpty() -> onToast("No definition found at cursor")
            results.size == 1 -> {
                stack.push(NavEntry(currentUri, currentCursor))
                onOpenFileAt(results[0].uri, results[0].range.first)
            }
            else -> onChoose(results)
        }
    }
}
