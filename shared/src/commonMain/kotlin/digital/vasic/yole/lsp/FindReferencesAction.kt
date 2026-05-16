/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 9: FindReferencesAction — pure orchestration object.
 *
 * Orchestrates the find-references user flow:
 *   0 results → show toast ("No references found").
 *   N results → hand off to onShow(list); stack push happens per-row
 *               inside ReferencesPanel when the user taps a row.
 *
 * Design: mirrors [GoToDefinitionAction] (iter-62 Phase 7) — accepts
 * [LspReferencesRequester] instead of a concrete [LspServerHost] so the
 * object stays platform-neutral and trivially testable in commonTest
 * without requiring MockK.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub requester.references to always return emptyList().
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.FindReferencesActionTests"
 *   3. Expect:
 *      - nonEmpty_callsOnShow FAILS (onShow never called).
 *      - empty_callsOnToast FAILS? No — emptyList() now triggers onToast,
 *        but the 2nd test asserts onToast is called for empty input so
 *        that one would still PASS; the first test FAILS.
 *   4. Revert; confirm all 3 tests PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Common:  no platform-specific imports — runs on all targets.
 *   - Android: consumed by IdeEditorScreen / long-press "Find references" (Phase 10).
 *   - Desktop: accessible; Phase 10 Desktop wiring deferred.
 *   - iOS:     accessible; Phase 10 iOS wiring deferred.
 *   - Web:     accessible; Phase 10 Web wiring deferred.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Stateless orchestrator for the find-references editor command.
 *
 * All side-effects (panel presentation, toast) are delegated via callbacks
 * so the object stays platform-neutral and trivially testable.
 */
object FindReferencesAction {

    /**
     * Execute a find-references request.
     *
     * @param requester    Thin interface over [LspServerHost.references].
     * @param langId       Language identifier of the active document (e.g. "kotlin").
     * @param currentUri   Document URI of the file currently open in the editor.
     * @param currentCursor Character offset of the cursor in the current document
     *                     (reserved for stack push on per-row jump in [ReferencesPanel]).
     * @param line         Zero-based line index for the LSP request.
     * @param character    Zero-based character index for the LSP request.
     * @param stack        Navigation history stack (Phase 10 wires per-row push via ReferencesPanel).
     * @param onShow       Called with the full result list when references are found.
     *                     Stack push happens inside [ReferencesPanel] on each row tap.
     * @param onToast      Called with a human-readable message when there are no results.
     */
    suspend fun findReferences(
        requester: LspReferencesRequester,
        langId: String,
        currentUri: String,
        currentCursor: Int,
        line: Int,
        character: Int,
        stack: EditorNavigationStack,
        onShow: (List<ReferenceLocation>) -> Unit,
        onToast: (String) -> Unit,
    ) {
        val results = requester.references(langId, currentUri, line, character)
        if (results.isEmpty()) {
            onToast("No references found")
        } else {
            onShow(results)
        }
    }
}
