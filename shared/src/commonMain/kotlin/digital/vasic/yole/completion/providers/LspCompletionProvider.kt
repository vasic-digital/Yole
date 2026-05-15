/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 5: LspCompletionProvider — expect class.
 *
 * 4th CompletionProvider that delegates to LspServerHost. Wires LSP
 * completions into the iter-60 CompletionEngine pipeline.
 *
 * Platform disposition (CONST-037):
 *   - Desktop: JVM actual — full LspServerHost delegation.
 *   - Android: JVM actual — identical body; LspServerInstaller returns
 *               NotInstalled until Phase 8 adds SplitInstall extraction,
 *               so complete() returns emptyList() honestly.
 *   - iOS:     Honest stub — App Store sandbox prohibits subprocesses.
 *   - Web/Wasm: Honest stub — browser Wasm cannot run native binaries.
 *
 * Submodules: not touched (CONST-038).
 *
 * Mutation procedure (CONST-035):
 *   1. In the JVM actual, stub mapKind() to always return
 *      CompletionItem.Kind.Word.
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.completion.providers.LspCompletionProviderTest"
 *   3. Expect: mapKind_FunctionMapsToIdentifier FAILS (stub returns Word).
 *              mapKind_SnippetMapsToSnippet FAILS (stub returns Word).
 *   4. Revert; confirm all tests PASS.
 *#######################################################*/
package digital.vasic.yole.completion.providers

import digital.vasic.yole.completion.CompletionContext
import digital.vasic.yole.completion.CompletionItem
import digital.vasic.yole.completion.CompletionProvider
import digital.vasic.yole.lsp.LspServerHost

/**
 * Completion provider backed by an LSP server managed by [LspServerHost].
 *
 * On Desktop and Android the provider delegates the completion call to
 * [LspServerHost.complete], converting [LspCompletionLine] results into
 * [CompletionItem] values. On iOS and Web the class is an honest stub
 * that returns [emptyList] without throwing.
 *
 * The [CompletionContext.documentUri] and [CompletionContext.workspaceRoot]
 * fields introduced in Phase 5 are consumed here; Phase 6 wiring in
 * IdeEditorScreen will populate them from the open file path.
 */
expect class LspCompletionProvider(
    host: LspServerHost,
) : CompletionProvider {
    override val id: String
    override suspend fun complete(ctx: CompletionContext): List<CompletionItem>
}
