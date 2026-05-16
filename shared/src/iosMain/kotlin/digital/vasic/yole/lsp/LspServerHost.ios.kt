/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 4 / iter-62 Phase 2: LspServerHost — iOS honest stub.
 *
 * App Store sandbox prohibits spawning subprocesses; LSP on iOS is
 * architecturally excluded. All LSP methods degrade honestly:
 *   - complete()    → LspCompletionResult(emptyList())
 *   - hover()       → null
 *   - definition()  → emptyList()
 * per CONST-035 honest-degradation policy.
 *
 * diagnosticsCache stays empty (no publishDiagnostics source on iOS).
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

    actual suspend fun didOpen(langId: String, uri: String, text: String, version: Int) {}
    actual suspend fun didChange(langId: String, uri: String, version: Int, fullText: String) {}
    actual suspend fun didClose(langId: String, uri: String) {}
    actual suspend fun shutdownAll() {}
}
