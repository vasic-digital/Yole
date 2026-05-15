/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 4: LspServerHost — expect class declaration.
 *
 * Central owner of LSP server processes. Lazy spawn per langId,
 * 5-min idle shutdown, restart-on-crash with backoff. Uses Eclipse
 * LSP4J 1.0.0 for JSON-RPC framing on JVM targets.
 *
 * Mutation procedure (CONST-035):
 *   1. In the JVM actual, stub complete() to always return
 *      LspCompletionResult(listOf(LspCompletionLine("__stub__","__stub__","Text",null,null))).
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.LspServerHostTest"
 *   3. Expect: noSpec_complete_returnsEmptyList FAILS (stub returns non-empty).
 *   4. Revert; confirm all LspServerHostTest tests PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop:  JVM actual — LSP4J ProcessBuilder wiring (this phase).
 *   - Android:  JVM actual — identical body to Desktop (this phase).
 *   - iOS:      Honest stub — returns emptyList. App Store sandbox
 *               prohibits spawning subprocesses.
 *   - Web/Wasm: Honest stub — returns emptyList. Native binaries
 *               cannot run inside a browser Wasm sandbox.
 *
 * Submodules: not touched (CONST-038). LSP4J consumed as Maven artifact.
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.flow.SharedFlow

/** Lifecycle state of a single per-langId LSP server process. */
sealed class ServerState {
    data object Stopped : ServerState()
    data object Spawning : ServerState()
    data object Ready : ServerState()
    data class BackingOff(val retryCount: Int, val nextAttemptMillis: Long) : ServerState()
    data class Dead(val cause: Throwable) : ServerState()
}

/** Completion result returned by [LspServerHost.complete]. */
data class LspCompletionResult(
    val items: List<LspCompletionLine>,
)

/** Single completion candidate surfaced from an LSP server. */
data class LspCompletionLine(
    val label: String,
    val insertText: String,
    val kind: String,
    val sortText: String?,
    val detail: String?,
)

/**
 * Central owner of LSP server processes.
 *
 * One [LspServerHost] instance per editor session manages a per-langId
 * Map<langId, RunningServer>. Servers are spawned lazily on first [complete]
 * / [didOpen] call and shut down after [idleShutdownMillis] of inactivity
 * (no open documents).
 *
 * @param registry      Authority for resolving langId → [LspServerSpec].
 * @param idleShutdownMillis After how many ms idle a server is shut down (default 5 min).
 * @param maxCrashRetries   Reserved for Phase 4b restart-with-backoff (not yet used in v1).
 */
expect class LspServerHost(
    registry: LspServerRegistry,
    idleShutdownMillis: Long = 5 * 60 * 1000L,
    maxCrashRetries: Int = 5,
) {
    /** Emits the current per-langId state map on every state change. Replay = 1. */
    val states: SharedFlow<Map<String, ServerState>>

    /**
     * Request completion at the given position. Returns [LspCompletionResult] with
     * an empty list on timeout (500 ms), no spec, or any error — honest degradation
     * per CONST-035. The caller's popup still shows non-LSP completions.
     */
    suspend fun complete(
        langId: String,
        documentUri: String,
        documentText: String,
        documentVersion: Int,
        line: Int,
        character: Int,
        workspaceRoot: String,
    ): LspCompletionResult

    /** Notify the server that a document was opened. No-op if no spec or server down. */
    suspend fun didOpen(langId: String, uri: String, text: String, version: Int)

    /** Notify the server of a full-document change. No-op if no server running. */
    suspend fun didChange(langId: String, uri: String, version: Int, fullText: String)

    /** Notify the server that a document was closed. No-op if no server running. */
    suspend fun didClose(langId: String, uri: String)

    /** Gracefully shutdown and forcibly kill all running servers. Idempotent. */
    suspend fun shutdownAll()
}
