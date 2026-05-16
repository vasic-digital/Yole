/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 4 / iter-62 Phase 2 / iter-63 Phase 2: LspServerHost — Web/Wasm honest stub.
 *
 * Native binaries cannot run inside a browser Wasm sandbox. All LSP
 * methods degrade honestly:
 *   - complete()       → LspCompletionResult(emptyList())
 *   - hover()          → null
 *   - definition()     → emptyList()
 *   - rename()         → null
 *   - codeActions()    → emptyList()
 *   - signatureHelp()  → null
 *   - formatting()          → emptyList()
 *   - references()          → emptyList()
 *   - onTypeFormatting()    → emptyList()
 * per CONST-035 honest-degradation policy.
 *
 * diagnosticsCache stays empty (no publishDiagnostics source in Wasm).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

actual class LspServerHost actual constructor(
    registry: LspServerRegistry,
    idleShutdownMillis: Long,
    maxCrashRetries: Int,
) {
    actual val states: SharedFlow<Map<String, ServerState>> =
        MutableSharedFlow<Map<String, ServerState>>().asSharedFlow()

    actual val diagnosticsCache: DiagnosticsCache = DiagnosticsCache()

    actual suspend fun complete(
        langId: String,
        documentUri: String,
        documentText: String,
        documentVersion: Int,
        line: Int,
        character: Int,
        workspaceRoot: String,
    ): LspCompletionResult = LspCompletionResult(emptyList())

    actual suspend fun hover(
        langId: String,
        documentUri: String,
        line: Int,
        character: Int,
    ): HoverInfo? = null

    actual suspend fun definition(
        langId: String,
        documentUri: String,
        line: Int,
        character: Int,
    ): List<DefinitionLocation> = emptyList()

    actual suspend fun rename(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        newName: String,
    ): WorkspaceEdit? = null

    actual suspend fun codeActions(
        langId: String,
        uri: String,
        range: IntRange,
    ): List<CodeAction> = emptyList()

    actual suspend fun signatureHelp(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
    ): SignatureHelp? = null

    actual suspend fun formatting(
        langId: String,
        uri: String,
        indentSize: Int,
        useSpaces: Boolean,
    ): List<TextEdit> = emptyList()

    actual suspend fun references(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        includeDeclaration: Boolean,
    ): List<DefinitionLocation> = emptyList()

    actual suspend fun onTypeFormatting(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        triggerChar: Char,
    ): List<TextEdit> = emptyList()

    actual suspend fun didOpen(langId: String, uri: String, text: String, version: Int) {}
    actual suspend fun didChange(langId: String, uri: String, version: Int, fullText: String) {}
    actual suspend fun didClose(langId: String, uri: String) {}
    actual suspend fun shutdownAll() {}
}
