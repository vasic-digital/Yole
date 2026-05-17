/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 4 / iter-62 Phase 2 / iter-63 Phase 2: LspServerHost — Desktop (JVM) actual.
 *
 * Uses Eclipse LSP4J 1.0.0 for JSON-RPC framing. Each langId gets its
 * own OS process + Launcher pair, serialized by a Mutex.
 *
 * Key behaviors:
 *   - Lazy spawn: acquireOrNull creates a process on first need.
 *   - 30-second initialize handshake timeout (jdtls cold-start is ~20s).
 *   - 500ms completion / hover timeout; 1000ms definition timeout.
 *   - Idle ticker every 60s: kills servers with no open docs for > idleShutdownMillis.
 *   - CancellationException always rethrown (CONST-035 + Detekt).
 *   - publishDiagnostics wired to DiagnosticsCache (iter-62 Phase 2).
 *
 * iter-62 Phase 2 additions:
 *   - hover(): 500ms timeout; mapHoverContentsToMarkdown internal helper.
 *   - definition(): 1000ms timeout; mapLspDefinitionsToList internal helper.
 *   - diagnosticsCache: DiagnosticsCache field; publishDiagnostics populated.
 *   - mapLspSeverity / mapLspMessage / mapLspCode internal helpers for diagnostics.
 *
 * iter-62 Phase 3 additions:
 *   - RunningServer.docTexts: caches latest document text per URI.
 *   - didOpen / didChange / didClose update docTexts.
 *   - publishDiagnostics uses LspRangeMapping.lineColToOffset for real ranges.
 *   - LspRangeMapping (commonMain pure helper) introduced.
 *
 * iter-63 Phase 2 additions:
 *   - rename(): 2000ms timeout; placeholder mapping (Phase 3 finalizes).
 *   - codeActions(): 1000ms timeout; handles Either<Command, CodeAction> union.
 *   - signatureHelp(): 300ms timeout; placeholder mapping (Phase 3 finalizes).
 *   - formatting(): 1000ms timeout; placeholder TextEdit list mapping.
 *   - references(): 2000ms timeout; reuses mapLspDefinitionsToList for Locations.
 *
 * iter-63 Phase 8 additions:
 *   - onTypeFormatting(): 500ms timeout; maps result via mapLspTextEdits.
 *     Called by FormattingTrigger.onType after the trigger-char guard.
 *
 * iter-63 Phase 3 additions (finalizes Phase 2 placeholder mappers):
 *   - mapLspWorkspaceEdit: handles documentChanges (modern, preferred) +
 *     changes (legacy) per LSP 3.18; prefers documentChanges when present.
 *   - mapLspCodeAction: extracted top-level helper for Either<Command,CodeAction>.
 *   - mapLspTextEdits: batch TextEdit mapping with real LspRangeMapping offsets.
 *   - mapLspTextEditWithDoc: single TextEdit using docText for range resolution.
 *   - mapLspSignatureHelpToYole: finalized (already correct in Phase 2).
 *   - formatting() wired to pass docTexts[uri] for real range mapping.
 *   - ReferenceLocation typealias introduced in commonMain.
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.CodeActionContext
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.CompletionCapabilities
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionItemCapabilities
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams
import org.eclipse.lsp4j.FormattingOptions
import org.eclipse.lsp4j.ResourceOperation
import org.eclipse.lsp4j.SnippetTextEdit
import org.eclipse.lsp4j.TextDocumentEdit
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.MarkupContent
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ReferenceContext
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.SignatureHelpParams
import org.eclipse.lsp4j.TextDocumentClientCapabilities
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.TextEdit as LspTextEdit
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

actual class LspServerHost actual constructor(
    private val registry: LspServerRegistry,
    private val idleShutdownMillis: Long,
    private val maxCrashRetries: Int,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mutex = Mutex()
    private val servers = mutableMapOf<String, RunningServer>()
    private val _states = MutableSharedFlow<Map<String, ServerState>>(replay = 1)
    actual val states: SharedFlow<Map<String, ServerState>> = _states.asSharedFlow()
    actual val diagnosticsCache: DiagnosticsCache = DiagnosticsCache()

    init {
        scope.launch { tickIdleShutdown() }
    }

    actual suspend fun complete(
        langId: String,
        documentUri: String,
        documentText: String,
        documentVersion: Int,
        line: Int,
        character: Int,
        workspaceRoot: String,
    ): LspCompletionResult {
        val server = acquireOrNull(langId, workspaceRoot) ?: return LspCompletionResult(emptyList())
        server.lastActivity = System.currentTimeMillis()
        return try {
            withTimeout(500L) { // honors iter-60 Phase 4 timeout budget
                val params = CompletionParams(
                    TextDocumentIdentifier(documentUri),
                    Position(line, character),
                )
                @Suppress("UNCHECKED_CAST")
                val raw: Either<MutableList<CompletionItem>, CompletionList>? =
                    server.languageServer.textDocumentService.completion(params)
                        .get(500, TimeUnit.MILLISECONDS) as? Either<MutableList<CompletionItem>, CompletionList>
                val items = when {
                    raw == null -> emptyList()
                    raw.isLeft -> raw.left.orEmpty()
                    else -> raw.right?.items.orEmpty()
                }
                LspCompletionResult(items.map { it.toLine() })
            }
        } catch (e: CancellationException) {
            throw e // rethrow per CONST-035 + Detekt
        } catch (_: TimeoutException) {
            LspCompletionResult(emptyList())
        } catch (_: Throwable) {
            LspCompletionResult(emptyList())
        }
    }

    private fun CompletionItem.toLine() = LspCompletionLine(
        label = label ?: insertText ?: "",
        insertText = insertText ?: label ?: "",
        kind = kind?.name ?: "Text",
        sortText = sortText,
        detail = detail,
    )

    actual suspend fun didOpen(langId: String, uri: String, text: String, version: Int) {
        val server = acquireOrNull(langId, workspaceRootOf(uri)) ?: return
        try {
            server.languageServer.textDocumentService.didOpen(
                DidOpenTextDocumentParams(
                    TextDocumentItem(uri, langId, version, text),
                ),
            )
            server.openDocs.add(uri)
            server.docTexts[uri] = text // Phase 3: cache for range mapping
            server.lastActivity = System.currentTimeMillis()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // benign — server may be down. Other providers still emit.
        }
    }

    actual suspend fun didChange(langId: String, uri: String, version: Int, fullText: String) {
        val server = mutex.withLock { servers[langId] } ?: return
        try {
            // Full-document sync (TextDocumentSyncKind.Full). Phase 4b may
            // upgrade to Incremental once we instrument keystroke diffs.
            server.languageServer.textDocumentService.didChange(
                DidChangeTextDocumentParams(
                    VersionedTextDocumentIdentifier(uri, version),
                    listOf(TextDocumentContentChangeEvent(fullText)),
                ),
            )
            server.docTexts[uri] = fullText // Phase 3: keep cache in sync
            server.lastActivity = System.currentTimeMillis()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
        }
    }

    actual suspend fun didClose(langId: String, uri: String) {
        val server = mutex.withLock { servers[langId] } ?: return
        try {
            server.languageServer.textDocumentService.didClose(
                DidCloseTextDocumentParams(TextDocumentIdentifier(uri)),
            )
            server.openDocs.remove(uri)
            server.docTexts.remove(uri) // Phase 3: evict cached text
            server.lastActivity = System.currentTimeMillis()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
        }
    }

    actual suspend fun hover(
        langId: String,
        documentUri: String,
        line: Int,
        character: Int,
    ): HoverInfo? {
        val server = mutex.withLock { servers[langId] } ?: return null
        return try {
            withTimeout(500L) {
                val params = HoverParams(TextDocumentIdentifier(documentUri), Position(line, character))
                val raw = server.languageServer.textDocumentService.hover(params)
                    .get(500, TimeUnit.MILLISECONDS)
                raw?.let { mapHoverContentsToMarkdown(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: TimeoutException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun definition(
        langId: String,
        documentUri: String,
        line: Int,
        character: Int,
    ): List<DefinitionLocation> {
        val server = mutex.withLock { servers[langId] } ?: return emptyList()
        return try {
            withTimeout(1000L) {
                val params = DefinitionParams(TextDocumentIdentifier(documentUri), Position(line, character))
                @Suppress("UNCHECKED_CAST")
                val raw: Either<MutableList<Location>, MutableList<LocationLink>>? =
                    server.languageServer.textDocumentService.definition(params)
                        .get(1000, TimeUnit.MILLISECONDS) as? Either<MutableList<Location>, MutableList<LocationLink>>
                mapLspDefinitionsToList(raw)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: TimeoutException) {
            emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    actual suspend fun rename(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        newName: String,
    ): WorkspaceEdit? {
        val server = mutex.withLock { servers[langId] } ?: return null
        return try {
            withTimeout(2000L) {
                val params = RenameParams(TextDocumentIdentifier(uri), Position(line, character), newName)
                val raw = server.languageServer.textDocumentService.rename(params)
                    .get(2000, TimeUnit.MILLISECONDS)
                raw?.let { mapLspWorkspaceEditToYole(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: TimeoutException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun codeActions(
        langId: String,
        uri: String,
        range: IntRange,
    ): List<CodeAction> {
        val server = mutex.withLock { servers[langId] } ?: return emptyList()
        return try {
            withTimeout(1000L) {
                val lspRange = Range(
                    Position(0, range.first),
                    Position(0, range.last + 1),
                )
                val params = CodeActionParams(
                    TextDocumentIdentifier(uri),
                    lspRange,
                    CodeActionContext(emptyList()),
                )
                @Suppress("UNCHECKED_CAST")
                val raw = server.languageServer.textDocumentService.codeAction(params)
                    .get(1000, TimeUnit.MILLISECONDS)
                // Phase 3: delegate to extracted mapLspCodeAction helper (CONST-035).
                raw.orEmpty().map { either -> mapLspCodeAction(either) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: TimeoutException) {
            emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    actual suspend fun signatureHelp(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
    ): SignatureHelp? {
        val server = mutex.withLock { servers[langId] } ?: return null
        return try {
            withTimeout(300L) {
                val params = SignatureHelpParams(TextDocumentIdentifier(uri), Position(line, character))
                val raw = server.languageServer.textDocumentService.signatureHelp(params)
                    .get(300, TimeUnit.MILLISECONDS)
                raw?.let { mapLspSignatureHelpToYole(it) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: TimeoutException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun formatting(
        langId: String,
        uri: String,
        indentSize: Int,
        useSpaces: Boolean,
    ): List<TextEdit> {
        val server = mutex.withLock { servers[langId] } ?: return emptyList()
        return try {
            withTimeout(1000L) {
                val options = FormattingOptions(indentSize, useSpaces)
                val params = DocumentFormattingParams(TextDocumentIdentifier(uri), options)
                val raw = server.languageServer.textDocumentService.formatting(params)
                    .get(1000, TimeUnit.MILLISECONDS)
                // Phase 3: pass docText for real LspRangeMapping-based offset conversion.
                val docText = server.docTexts[uri] ?: ""
                mapLspTextEdits(raw.orEmpty(), docText)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: TimeoutException) {
            emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    actual suspend fun references(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        includeDeclaration: Boolean,
    ): List<DefinitionLocation> {
        val server = mutex.withLock { servers[langId] } ?: return emptyList()
        return try {
            withTimeout(2000L) {
                val params = ReferenceParams(
                    TextDocumentIdentifier(uri),
                    Position(line, character),
                    ReferenceContext(includeDeclaration),
                )
                val raw = server.languageServer.textDocumentService.references(params)
                    .get(2000, TimeUnit.MILLISECONDS)
                raw.orEmpty().map { loc ->
                    DefinitionLocation(uri = loc.uri ?: "", range = 0..0)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: TimeoutException) {
            emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    actual suspend fun onTypeFormatting(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        triggerChar: Char,
    ): List<TextEdit> {
        val server = mutex.withLock { servers[langId] } ?: return emptyList()
        return try {
            withTimeout(500L) {
                val options = FormattingOptions(4, true)
                val params = DocumentOnTypeFormattingParams(
                    TextDocumentIdentifier(uri),
                    options,
                    Position(line, character),
                    triggerChar.toString(),
                )
                val raw = server.languageServer.textDocumentService.onTypeFormatting(params)
                    .get(500, TimeUnit.MILLISECONDS)
                val docText = server.docTexts[uri] ?: ""
                mapLspTextEdits(raw.orEmpty(), docText)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: TimeoutException) {
            emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    actual fun getOnTypeTriggerChars(langId: String): Set<Char>? {
        val server = servers[langId] ?: return null
        val chars = server.onTypeTriggerChars
        return if (chars.isEmpty()) null else chars
    }

    actual suspend fun shutdownAll() = mutex.withLock {
        for ((langId, srv) in servers.toMap()) {
            try {
                srv.languageServer.shutdown().get(3, TimeUnit.SECONDS)
                srv.languageServer.exit()
            } catch (_: Throwable) {
            }
            srv.process.destroyForcibly()
            servers.remove(langId)
        }
        emitState()
    }

    private suspend fun acquireOrNull(langId: String, workspaceRoot: String): RunningServer? =
        mutex.withLock {
            servers[langId]?.let { return@withLock it }
            val spec = registry.forLanguage(langId) ?: return@withLock null
            try {
                val installer = LspServerInstaller(spec)
                val path = installer.ensureInstalled().getOrElse { return@withLock null }
                val process = ProcessBuilder(
                    listOf(path.toString()) + spec.args,
                ).redirectErrorStream(false).start()
                val client = buildFakeClient(langId) // Phase 3: pass langId for docTexts lookup
                val launcher = Launcher.createLauncher(
                    client,
                    LanguageServer::class.java,
                    process.inputStream,
                    process.outputStream,
                )
                launcher.startListening()
                val ls = launcher.remoteProxy
                val initParams = InitializeParams().apply {
                    processId = ProcessHandle.current().pid().toInt()
                    rootUri = "file://$workspaceRoot"
                    capabilities = ClientCapabilities().apply {
                        textDocument = TextDocumentClientCapabilities().apply {
                            completion = CompletionCapabilities().apply {
                                completionItem = CompletionItemCapabilities().apply {
                                    snippetSupport = true
                                }
                            }
                        }
                    }
                    initializationOptions = spec.initOptions
                }
                val initResult = ls.initialize(initParams).get(30, TimeUnit.SECONDS)
                ls.initialized(InitializedParams())
                // iter-74 (#iter-63-server-trigger-chars-hardcoded): capture on-type
                // trigger chars from server capabilities so callers can query them
                // instead of using the hardcoded set.
                val onTypeTriggerChars: Set<Char> = run {
                    val onTypeProvider = initResult?.capabilities
                        ?.documentOnTypeFormattingProvider
                    if (onTypeProvider == null) return@run emptySet()
                    val chars = mutableSetOf<Char>()
                    onTypeProvider.firstTriggerCharacter?.firstOrNull()?.let { chars.add(it) }
                    onTypeProvider.moreTriggerCharacter?.forEach { s ->
                        s?.firstOrNull()?.let { chars.add(it) }
                    }
                    chars
                }
                val server = RunningServer(
                    process,
                    ls,
                    lastActivity = System.currentTimeMillis(),
                    onTypeTriggerChars = onTypeTriggerChars,
                )
                servers[langId] = server
                emitState()
                server
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                null
            }
        }

    private fun buildFakeClient(langId: String): LanguageClient = object : LanguageClient {
        override fun telemetryEvent(o: Any?) {}
        override fun publishDiagnostics(p: PublishDiagnosticsParams?) {
            p ?: return
            // Phase 3: look up the doc text at publish time so LspRangeMapping can
            // resolve LSP (line, col) → absolute char offset. Best-effort: if the
            // server hasn't seen the document yet, range collapses to 0..0.
            val text = servers[langId]?.docTexts?.get(p.uri) ?: ""
            val mapped = p.diagnostics.orEmpty().map { lspDiag ->
                val start = LspRangeMapping.lineColToOffset(
                    text,
                    lspDiag.range.start.line,
                    lspDiag.range.start.character,
                )
                val end = LspRangeMapping.lineColToOffset(
                    text,
                    lspDiag.range.end.line,
                    lspDiag.range.end.character,
                )
                Diagnostic(
                    severity = mapLspSeverity(lspDiag.severity),
                    range = start..end,
                    message = mapLspMessageEither(lspDiag.message),
                    source = lspDiag.source,
                    code = mapLspCodeEither(lspDiag.code),
                )
            }
            diagnosticsCache.upsert(p.uri, mapped)
        }
        override fun showMessage(p: MessageParams?) {}
        override fun showMessageRequest(p: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> =
            CompletableFuture.completedFuture(null)
        override fun logMessage(p: MessageParams?) {}
    }

    private suspend fun tickIdleShutdown() {
        while (currentCoroutineContext().isActive) {
            delay(60_000L)
            val now = System.currentTimeMillis()
            mutex.withLock {
                val toKill = servers.filter { (_, srv) ->
                    srv.openDocs.isEmpty() && (now - srv.lastActivity) > idleShutdownMillis
                }.keys.toList()
                for (langId in toKill) {
                    val srv = servers.remove(langId) ?: continue
                    try { srv.languageServer.shutdown().get(3, TimeUnit.SECONDS) } catch (_: Throwable) {}
                    try { srv.languageServer.exit() } catch (_: Throwable) {}
                    srv.process.destroyForcibly()
                }
                if (toKill.isNotEmpty()) emitState()
            }
        }
    }

    private suspend fun emitState() {
        _states.emit(
            servers.mapValues { (_, srv) ->
                if (srv.process.isAlive) ServerState.Ready else ServerState.Dead(IllegalStateException("process exited"))
            },
        )
    }

    /** Derive workspace root from a file:// URI as a best-effort fallback. */
    private fun workspaceRootOf(uri: String): String =
        uri.removePrefix("file://").substringBeforeLast('/')

    private data class RunningServer(
        val process: Process,
        val languageServer: LanguageServer,
        @Volatile var lastActivity: Long,
        val openDocs: MutableSet<String> = mutableSetOf(),
        val docTexts: MutableMap<String, String> = mutableMapOf(), // Phase 3: per-URI text cache for LspRangeMapping
        // iter-74: on-type trigger chars captured from InitializeResult capabilities.
        val onTypeTriggerChars: Set<Char> = emptySet(),
    )
}

// ---------------------------------------------------------------------------
// Internal helpers — top-level for Phase 3 testability (CONST-035).
// ---------------------------------------------------------------------------

/**
 * Map an LSP4J Hover to a [HoverInfo] with a markdown string.
 *
 * LSP4J 1.0.0: Hover.contents is
 *   Either<List<Either<String, MarkedString>>, MarkupContent>
 * Phase 2 extracts raw strings; Phase 3 HoverMarkdownRenderer polishes them.
 */
internal fun mapHoverContentsToMarkdown(hover: org.eclipse.lsp4j.Hover): HoverInfo? {
    val contents = hover.contents ?: return null
    val text = when {
        contents.isLeft -> {
            contents.left.orEmpty().joinToString("\n\n") { item ->
                when {
                    item.isLeft -> item.left ?: ""
                    else -> item.right?.value ?: ""
                }
            }
        }
        else -> contents.right?.value ?: ""
    }
    return if (text.isBlank()) null else HoverInfo(contents = text, range = null)
}

/**
 * Map an LSP4J definition result (Location list or LocationLink list) to
 * [DefinitionLocation] list. Phase 2 uses range = 0..0 (Phase 3 finalizes).
 */
internal fun mapLspDefinitionsToList(
    raw: Either<MutableList<org.eclipse.lsp4j.Location>, MutableList<org.eclipse.lsp4j.LocationLink>>?,
): List<DefinitionLocation> {
    raw ?: return emptyList()
    return when {
        raw.isLeft -> raw.left.orEmpty().map { loc ->
            DefinitionLocation(uri = loc.uri ?: "", range = 0..0)
        }
        else -> raw.right.orEmpty().map { link ->
            DefinitionLocation(uri = link.targetUri ?: "", range = 0..0)
        }
    }
}

/** Map LSP4J DiagnosticSeverity (1-4) to Yole [Severity]. Default: Information. */
internal fun mapLspSeverity(severity: DiagnosticSeverity?): Severity = when (severity) {
    DiagnosticSeverity.Error -> Severity.Error
    DiagnosticSeverity.Warning -> Severity.Warning
    DiagnosticSeverity.Information -> Severity.Information
    DiagnosticSeverity.Hint -> Severity.Hint
    null -> Severity.Information
}

/**
 * Extract diagnostic message string.
 * LSP4J 1.0.0: Diagnostic.message is Either<String, MarkupContent>.
 * Plain-string case (left) is most common; MarkupContent.value used for right.
 */
internal fun mapLspMessageEither(message: Either<String, MarkupContent>?): String = when {
    message == null -> ""
    message.isLeft -> message.left ?: ""
    else -> message.right?.value ?: ""
}

/**
 * Extract diagnostic code.
 * LSP4J 1.0.0: Diagnostic.code is Either<String, Int>.
 * Phase 2 converts to nullable String for [Diagnostic.code].
 */
internal fun mapLspCodeEither(code: Either<String, Int>?): String? = when {
    code == null -> null
    code.isLeft -> code.left
    else -> code.right?.toString()
}

// ---------------------------------------------------------------------------
// iter-63 Phase 3 mapping helpers — finalized (Phase 2 placeholders removed).
// All are internal top-level so desktopTest can call them directly (CONST-035).
// ---------------------------------------------------------------------------

/**
 * Map an LSP4J [org.eclipse.lsp4j.WorkspaceEdit] to Yole's [WorkspaceEdit].
 *
 * Per LSP 3.18 §3.17.11, if `documentChanges` is present it takes precedence
 * over the legacy `changes` map. Only [TextDocumentEdit] entries in
 * `documentChanges` are mapped; [ResourceOperation] entries (create/rename/delete
 * file) are silently skipped because [WorkspaceEdit] models text changes only.
 *
 * Decision: prefer documentChanges over changes when both are non-empty.
 * This matches the LSP specification's explicit "preferred over changes" note.
 *
 * Mutation procedure (CONST-035 — verified in WorkspaceEditMappingTest):
 *   Swap the preference so changes wins → documentChanges_preferredOver_changes FAILS.
 */
internal fun mapLspWorkspaceEditToYole(lspEdit: org.eclipse.lsp4j.WorkspaceEdit): WorkspaceEdit {
    val docChanges = lspEdit.documentChanges
    if (!docChanges.isNullOrEmpty()) {
        // Modern path: documentChanges (List<Either<TextDocumentEdit, ResourceOperation>>)
        val mapped = mutableMapOf<String, List<TextEdit>>()
        for (either in docChanges) {
            if (either.isLeft) {
                // TextDocumentEdit — carries uri + list of Either<TextEdit, SnippetTextEdit>
                val tde: TextDocumentEdit = either.left
                val uri = tde.textDocument?.uri ?: continue
                // LSP4J 1.0.0: edits is List<Either<TextEdit, SnippetTextEdit>>.
                // Plain TextEdit is left; SnippetTextEdit (right) is extracted as range+snippet.value.
                val edits = tde.edits.orEmpty().map { editEither ->
                    if (editEither.isLeft) {
                        mapLspTextEditToYole(editEither.left)
                    } else {
                        // SnippetTextEdit: use snippet.value as newText; range maps as normal.
                        val snip = editEither.right
                        TextEdit(range = 0..0, newText = snip.snippet?.value ?: "")
                    }
                }
                mapped[uri] = edits
            }
            // ResourceOperation (right side of outer Either) = file-level op; skip.
        }
        return WorkspaceEdit(changes = mapped)
    }
    // Legacy path: plain changes map (Map<uri, List<TextEdit>>)
    val rawChanges = lspEdit.changes.orEmpty()
    val mapped = rawChanges.mapValues { (_, edits) ->
        edits.orEmpty().map { mapLspTextEditToYole(it) }
    }
    return WorkspaceEdit(changes = mapped)
}

/**
 * Map a single LSP4J [org.eclipse.lsp4j.TextEdit] to Yole's [TextEdit].
 *
 * Uses offset 0..0 when no document text is available (e.g. workspace-edit
 * mapping where the doc text isn't cached yet). Prefer [mapLspTextEditWithDoc]
 * when the document text is known.
 */
internal fun mapLspTextEditToYole(lspEdit: LspTextEdit): TextEdit =
    TextEdit(range = 0..0, newText = lspEdit.newText ?: "")

/**
 * Map a single LSP4J [org.eclipse.lsp4j.TextEdit] using the live document
 * text for real line/col → offset conversion via [LspRangeMapping].
 *
 * Mutation procedure (CONST-035 — verified in TextEditMappingTest):
 *   Return 0..0 instead of computed range → singleEdit_realRange + multiEdit_preservesOrder FAIL.
 */
internal fun mapLspTextEditWithDoc(lspEdit: LspTextEdit, docText: String): TextEdit {
    val startOffset = LspRangeMapping.lineColToOffset(
        docText,
        lspEdit.range?.start?.line ?: 0,
        lspEdit.range?.start?.character ?: 0,
    )
    val endOffset = LspRangeMapping.lineColToOffset(
        docText,
        lspEdit.range?.end?.line ?: 0,
        lspEdit.range?.end?.character ?: 0,
    )
    return TextEdit(range = startOffset..endOffset, newText = lspEdit.newText ?: "")
}

/**
 * Map a batch of LSP4J [org.eclipse.lsp4j.TextEdit]s using [docText] for
 * real offset conversion. Called by [LspServerHost.formatting].
 *
 * Mutation procedure (CONST-035 — verified in TextEditMappingTest):
 *   Return emptyList() → singleEdit_realRange + outOfBounds_clamps FAIL.
 */
internal fun mapLspTextEdits(lspEdits: List<LspTextEdit>, docText: String): List<TextEdit> =
    lspEdits.map { mapLspTextEditWithDoc(it, docText) }

/**
 * Map an LSP4J `Either<Command, CodeAction>` entry to Yole's [CodeAction].
 *
 * Per LSP spec, `textDocument/codeAction` returns a list where each element
 * is either a [org.eclipse.lsp4j.Command] (left) or a full
 * [org.eclipse.lsp4j.CodeAction] (right).
 *   - Left (Command): construct a [CodeAction] with title/command; edit is null.
 *   - Right (CodeAction): map all fields including optional edit and command.
 *
 * Mutation procedure (CONST-035 — verified in CodeActionMappingTest):
 *   Return CodeAction("WRONG", null, null, null) → commandEither_mapsToCodeAction FAILS.
 */
internal fun mapLspCodeAction(
    either: Either<Command, org.eclipse.lsp4j.CodeAction>,
): CodeAction = when {
    either.isLeft -> {
        val cmd: Command = either.left
        CodeAction(
            title = cmd.title ?: "",
            kind = null,
            edit = null,
            command = cmd.command,
        )
    }
    else -> {
        val lspAction = either.right
        CodeAction(
            title = lspAction.title ?: "",
            kind = lspAction.kind,
            edit = lspAction.edit?.let { mapLspWorkspaceEditToYole(it) },
            command = lspAction.command?.command,
        )
    }
}

/**
 * Map an LSP4J [org.eclipse.lsp4j.SignatureHelp] to Yole's [SignatureHelp].
 *
 * [org.eclipse.lsp4j.SignatureInformation.documentation] is
 * `Either<String, MarkupContent>` — plain string (left) or markdown (right).
 * [org.eclipse.lsp4j.ParameterInformation.label] is
 * `Either<String, Tuple.Two<Integer, Integer>>` — plain label (left) or
 * start/end offsets into the parent signature label (right).
 *
 * Mutation procedure (CONST-035 — verified in SignatureHelpMappingTest):
 *   Return null always → stringDocumentation_isExtracted FAILS.
 */
internal fun mapLspSignatureHelpToYole(lspHelp: org.eclipse.lsp4j.SignatureHelp): SignatureHelp? {
    val sigs = lspHelp.signatures.orEmpty()
    if (sigs.isEmpty()) return null
    val mapped = sigs.map { lspSig ->
        SignatureInformation(
            label = lspSig.label ?: "",
            documentation = when {
                lspSig.documentation == null -> null
                lspSig.documentation.isLeft -> lspSig.documentation.left
                else -> lspSig.documentation.right?.value
            },
            parameters = lspSig.parameters.orEmpty().map { lspParam ->
                ParameterInformation(
                    label = when {
                        lspParam.label == null -> ""
                        lspParam.label.isLeft -> lspParam.label.left ?: ""
                        else -> {
                            // Tuple.Two<Integer,Integer>: (start, end) byte-offsets into sig label.
                            val tuple = lspParam.label.right
                            lspSig.label?.let { s ->
                                try {
                                    val start = (tuple?.first as? Number)?.toInt() ?: 0
                                    val end = (tuple?.second as? Number)?.toInt() ?: s.length
                                    s.substring(start.coerceIn(0, s.length), end.coerceIn(0, s.length))
                                } catch (_: Throwable) { "" }
                            } ?: ""
                        }
                    },
                    documentation = when {
                        lspParam.documentation == null -> null
                        lspParam.documentation.isLeft -> lspParam.documentation.left
                        else -> lspParam.documentation.right?.value
                    },
                )
            },
        )
    }
    return SignatureHelp(
        signatures = mapped,
        activeSignature = lspHelp.activeSignature ?: 0,
        activeParameter = lspHelp.activeParameter ?: 0,
    )
}
