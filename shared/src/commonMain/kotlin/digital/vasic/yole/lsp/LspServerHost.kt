/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 4 / iter-62 Phase 2: LspServerHost — expect class.
 *
 * Central owner of LSP server processes. Lazy spawn per langId,
 * 5-min idle shutdown, restart-on-crash with backoff. Uses Eclipse
 * LSP4J 1.0.0 for JSON-RPC framing on JVM targets.
 *
 * iter-62 Phase 2 additions:
 *   - hover(): returns HoverInfo? (null on timeout/error/no-spec).
 *   - definition(): returns List<DefinitionLocation> (empty on timeout/error/no-spec).
 *   - diagnosticsCache: DiagnosticsCache populated by publishDiagnostics.
 *
 * iter-63 Phase 2 additions:
 *   - rename(): returns WorkspaceEdit? (null on timeout/error/no-spec). 2s timeout.
 *   - codeActions(): returns List<CodeAction> (empty on timeout/error/no-spec). 1s timeout.
 *   - signatureHelp(): returns SignatureHelp? (null on timeout/error/no-spec). 300ms timeout.
 *   - formatting(): returns List<TextEdit> (empty on timeout/error/no-spec). 1s timeout.
 *   - references(): returns List<DefinitionLocation> (empty on timeout/error/no-spec). 2s timeout.
 *
 * iter-63 Phase 8 additions:
 *   - onTypeFormatting(): returns List<TextEdit> (empty on timeout/error/no-spec). 500ms timeout.
 *     Called by FormattingTrigger.onType for trigger-char-gated on-type formatting.
 *
 * iter-75 additions (#iter-62-jdt-uri-scheme-unsupported):
 *   - jdtClassFileContents(): sends custom LSP request `java/classFileContents` to jdtls
 *     for jdt:// URIs. Returns decompiled source on success, null otherwise. 3s timeout.
 *     JVM actuals implement via launcher.remoteEndpoint.request(). iOS/Wasm stubs return null.
 *
 * Mutation procedure (CONST-035):
 *   1. In the JVM actual, stub complete() to always return
 *      LspCompletionResult(listOf(LspCompletionLine("__stub__","__stub__","Text",null,null))).
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.LspServerHostTest"
 *   3. Expect: noSpec_complete_returnsEmptyList FAILS (stub returns non-empty).
 *   4. Revert; confirm all LspServerHostTest tests PASS.
 *   5. Stub hover() to return HoverInfo("fake", null) → noSpec_hover_returnsNull FAILS.
 *   6. Stub definition() to return listOf(DefinitionLocation("x",0..0)) → noSpec_definition_returnsEmpty FAILS.
 *   7. Stub rename() to return WorkspaceEdit() → noSpec_rename_returnsNull FAILS.
 *   8. Stub codeActions() to return listOf(CodeAction("x",null,null,null)) → noSpec_codeActions_returnsEmpty FAILS.
 *   9. Stub signatureHelp() to return SignatureHelp(emptyList(),0,0) → noSpec_signatureHelp_returnsNull FAILS.
 *  10. Stub formatting() to return listOf(TextEdit(0..0,"x")) → noSpec_formatting_returnsEmpty FAILS.
 *  11. Stub references() to return listOf(DefinitionLocation("x",0..0)) → noSpec_references_returnsEmpty FAILS.
 *  12. Stub onTypeFormatting() to return listOf(TextEdit(0..0,"x")) → noSpec_onTypeFormatting_returnsEmpty FAILS.
 *  13. Stub getOnTypeTriggerChars() to return setOf(';') when server has no spec
 *      → noSpec_getOnTypeTriggerChars_returnsNull FAILS.
 *  14. Stub jdtClassFileContents() to return "" → noSpec_jdtClassFileContents_returnsNull FAILS.
 *  15. Revert; confirm all tests PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop:  JVM actual — LSP4J ProcessBuilder wiring (this phase).
 *   - Android:  JVM actual — identical body to Desktop (this phase).
 *   - iOS:      Honest stub — all 5 new methods return null/emptyList.
 *               App Store sandbox prohibits spawning subprocesses.
 *   - Web/Wasm: Honest stub — all 5 new methods return null/emptyList.
 *               Native binaries cannot run inside a browser Wasm sandbox.
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

    /**
     * Request hover information at the given position.
     *
     * Returns [HoverInfo] with LSP-provided markdown on success, or `null` on
     * timeout (500 ms), no spec, server down, or any error — honest degradation
     * per CONST-035.
     */
    suspend fun hover(
        langId: String,
        documentUri: String,
        line: Int,
        character: Int,
    ): HoverInfo?

    /**
     * Request go-to-definition locations for the symbol at the given position.
     *
     * Returns a list of [DefinitionLocation] on success, or an empty list on
     * timeout (1000 ms), no spec, server down, or any error — honest degradation
     * per CONST-035.
     */
    suspend fun definition(
        langId: String,
        documentUri: String,
        line: Int,
        character: Int,
    ): List<DefinitionLocation>

    /**
     * Request a workspace-wide rename of the symbol at the given position.
     *
     * Returns a [WorkspaceEdit] on success, or `null` on timeout (2000 ms),
     * no spec, server down, or any error — honest degradation per CONST-035.
     *
     * @param newName Replacement identifier the user supplied.
     */
    suspend fun rename(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        newName: String,
    ): WorkspaceEdit?

    /**
     * Request code actions available at the given document range.
     *
     * Returns a list of [CodeAction] on success, or an empty list on
     * timeout (1000 ms), no spec, server down, or any error — honest degradation
     * per CONST-035.
     */
    suspend fun codeActions(
        langId: String,
        uri: String,
        range: IntRange,
    ): List<CodeAction>

    /**
     * Request signature help at the given position (called on '(' or ',').
     *
     * Returns [SignatureHelp] on success, or `null` on timeout (300 ms),
     * no spec, server down, or any error — honest degradation per CONST-035.
     */
    suspend fun signatureHelp(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
    ): SignatureHelp?

    /**
     * Request full-document formatting edits.
     *
     * Returns a list of [TextEdit] on success, or an empty list on timeout
     * (1000 ms), no spec, server down, or any error — honest degradation
     * per CONST-035.
     *
     * @param indentSize Number of spaces (or tab-width) per indent level.
     * @param useSpaces  True → spaces, false → tabs.
     */
    suspend fun formatting(
        langId: String,
        uri: String,
        indentSize: Int = 4,
        useSpaces: Boolean = true,
    ): List<TextEdit>

    /**
     * Request all reference locations for the symbol at the given position.
     *
     * Returns a list of [DefinitionLocation] on success, or an empty list on
     * timeout (2000 ms), no spec, server down, or any error — honest degradation
     * per CONST-035.
     *
     * @param includeDeclaration Whether to include the symbol's own declaration.
     */
    suspend fun references(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        includeDeclaration: Boolean = true,
    ): List<DefinitionLocation>

    /**
     * Request on-type formatting edits for the character just typed.
     *
     * Returns a list of [TextEdit] on success, or an empty list on timeout
     * (500 ms), no spec, server down, or any error — honest degradation
     * per CONST-035.
     *
     * Called by [FormattingTrigger.onType] after confirming [triggerChar] is
     * in the server's declared trigger-character set. The caller is responsible
     * for the trigger-character guard; this method always attempts the LSP call.
     *
     * Per Phase 0 §3: rust-analyzer supports 8 on-type trigger chars,
     * clangd supports `}` only, gopls has no on-type support.
     *
     * @param langId      Language identifier for the document.
     * @param uri         Document URI.
     * @param line        0-based cursor line after the keystroke.
     * @param character   0-based cursor character offset after the keystroke.
     * @param triggerChar The character that was typed (first trigger char per LSP spec).
     */
    suspend fun onTypeFormatting(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        triggerChar: Char,
    ): List<TextEdit>

    /**
     * Returns the on-type trigger characters declared by the LSP server for [langId],
     * or null when the server has not yet initialised, has no on-type formatting spec,
     * or any error occurred.
     *
     * iter-74 (#iter-63-server-trigger-chars-hardcoded): replaces the hardcoded
     * `setOf(';', '}', '\n')` with real server-capability query. Callers fall back
     * to the default set when this returns null.
     *
     * JVM actuals extract `initializeResult.capabilities
     *   .documentOnTypeFormattingProvider?.firstTriggerCharacter` plus
     * `moreTriggerCharacter` entries. iOS/Wasm stubs return null.
     */
    fun getOnTypeTriggerChars(langId: String): Set<Char>?

    /**
     * Fetch decompiled source for a `jdt://` URI via the jdtls custom request
     * `java/classFileContents`.
     *
     * Only meaningful for the Eclipse JDT Language Server (langId = "java").
     * Returns the decompiled class file source as a [String] on success, or
     * `null` on timeout (3000 ms), no server running, server error, or on
     * platforms where process-based LSP servers cannot run (iOS, Wasm).
     *
     * Honest degradation per CONST-035: callers MUST handle null gracefully
     * (e.g. show a toast "Cannot open JDT URI: server unavailable").
     *
     * @param langId   Language identifier; only "java" with jdtls is expected.
     * @param jdtUri   The full `jdt://` URI returned by jdtls go-to-definition.
     *
     * Cross-platform impact (CONST-037):
     *   - Desktop/Android: JVM actual sends `java/classFileContents` via remoteEndpoint.
     *   - iOS/Wasm: stub returns null (no subprocess support).
     *
     * (#iter-62-jdt-uri-scheme-unsupported, iter-75)
     */
    suspend fun jdtClassFileContents(langId: String, jdtUri: String): String?

    /**
     * Single source of truth for LSP-emitted diagnostics. Populated by
     * the internal YoleLanguageClient.publishDiagnostics callback wired
     * in Phase 2. Consumed by the 3 render surfaces in Phase 5.
     *
     * iOS/Wasm stubs instantiate an empty [DiagnosticsCache] that stays empty.
     */
    val diagnosticsCache: DiagnosticsCache

    /** Gracefully shutdown and forcibly kill all running servers. Idempotent. */
    suspend fun shutdownAll()
}
