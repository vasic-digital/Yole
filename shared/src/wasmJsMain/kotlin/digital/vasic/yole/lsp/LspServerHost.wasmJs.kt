/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 4: LspServerHost — Web/Wasm honest stub.
 *
 * Native binaries cannot run inside a browser Wasm sandbox. Returns
 * emptyList() from complete() per CONST-035 honest-degradation policy.
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

    actual suspend fun complete(
        langId: String,
        documentUri: String,
        documentText: String,
        documentVersion: Int,
        line: Int,
        character: Int,
        workspaceRoot: String,
    ): LspCompletionResult = LspCompletionResult(emptyList())

    actual suspend fun didOpen(langId: String, uri: String, text: String, version: Int) {}
    actual suspend fun didChange(langId: String, uri: String, version: Int, fullText: String) {}
    actual suspend fun didClose(langId: String, uri: String) {}
    actual suspend fun shutdownAll() {}
}
