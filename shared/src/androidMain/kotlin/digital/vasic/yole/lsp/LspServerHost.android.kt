/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 4 / iter-62 Phase 2 / iter-63 Phase 2: LspServerHost — Android (JVM) actual.
 *
 * Body is identical to the Desktop actual. Both targets run on the JVM
 * so LSP4J's ProcessBuilder-based Launcher works on both.
 *
 * iter-62 Phase 2 additions (same as Desktop):
 *   - hover(): 500ms timeout; delegates to mapHoverContentsToMarkdown.
 *   - definition(): 1000ms timeout; delegates to mapLspDefinitionsToList.
 *   - diagnosticsCache: DiagnosticsCache field; publishDiagnostics populated.
 *
 * Note: LspServerInstaller returns InstallError.NotInstalled on Android
 * until Phase 8 adds SplitInstallManager-aware extraction. The acquireOrNull
 * guard surfaces this honestly: getOrElse { return@withLock null } means
 * complete()/hover()/definition() return their honest-degradation values —
 * non-LSP completions (token + snippet providers) remain unaffected.
 *
 * iter-62 Phase 3 additions (mirror of Desktop):
 *   - RunningServer.docTexts: caches latest document text per URI.
 *   - didOpen / didChange / didClose update docTexts.
 *   - publishDiagnostics uses LspRangeMapping.lineColToOffset for real ranges.
 *
 * iter-63 Phase 2 additions (mirror of Desktop):
 *   - rename(): 2000ms timeout; placeholder mapping (Phase 3 finalizes).
 *   - codeActions(): 1000ms timeout; handles Either<Command, CodeAction> union.
 *   - signatureHelp(): 300ms timeout; placeholder mapping (Phase 3 finalizes).
 *   - formatting(): 1000ms timeout; placeholder TextEdit list mapping.
 *   - references(): 2000ms timeout; reuses Location→DefinitionLocation mapping.
 *
 * iter-63 Phase 8 additions (mirror of Desktop):
 *   - onTypeFormatting(): 500ms timeout; delegates to mapLspTextEditsAndroid.
 *
 * iter-63 Phase 3 additions (mirror of Desktop):
 *   - mapLspWorkspaceEditToYoleAndroid: finalized with documentChanges support.
 *   - mapLspCodeActionAndroid: extracted helper for Either<Command, CodeAction>.
 *   - mapLspTextEditsAndroid: batch TextEdit mapping with real LspRangeMapping offsets.
 *   - mapLspTextEditWithDocAndroid: single TextEdit using docText for range resolution.
 *   - mapLspSignatureHelpToYoleAndroid: finalized (unchanged from Phase 2 logic).
 *   - formatting() wired to pass docTexts[uri] for real range mapping.
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
                raw?.let { mapLspWorkspaceEditToYoleAndroid(it) }
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
                val raw = server.languageServer.textDocumentService.codeAction(params)
                    .get(1000, TimeUnit.MILLISECONDS)
                // Phase 3: delegate to extracted mapLspCodeActionAndroid helper (CONST-035).
                raw.orEmpty().map { either -> mapLspCodeActionAndroid(either) }
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
                raw?.let { mapLspSignatureHelpToYoleAndroid(it) }
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
                val docText = server.docTexts[uri] ?: ""
                mapLspTextEditsAndroid(raw.orEmpty(), docText)
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
                mapLspTextEditsAndroid(raw.orEmpty(), docText)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: TimeoutException) {
            emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
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
                ls.initialize(initParams).get(30, TimeUnit.SECONDS)
                ls.initialized(InitializedParams())
                val server = RunningServer(process, ls, lastActivity = System.currentTimeMillis())
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
    )
}

// ---------------------------------------------------------------------------
// Internal helpers — mirror of Desktop actual; top-level for testability.
// ---------------------------------------------------------------------------

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

internal fun mapLspSeverity(severity: DiagnosticSeverity?): Severity = when (severity) {
    DiagnosticSeverity.Error -> Severity.Error
    DiagnosticSeverity.Warning -> Severity.Warning
    DiagnosticSeverity.Information -> Severity.Information
    DiagnosticSeverity.Hint -> Severity.Hint
    null -> Severity.Information
}

internal fun mapLspMessageEither(message: Either<String, MarkupContent>?): String = when {
    message == null -> ""
    message.isLeft -> message.left ?: ""
    else -> message.right?.value ?: ""
}

internal fun mapLspCodeEither(code: Either<String, Int>?): String? = when {
    code == null -> null
    code.isLeft -> code.left
    else -> code.right?.toString()
}

// ---------------------------------------------------------------------------
// iter-63 Phase 3 mapping helpers (Android mirror of Desktop variants).
// Suffixed -Android to avoid duplicate-function conflict if both source sets
// are merged in a single classpath (e.g. during shared unit-test compilation).
// All are internal top-level for androidUnitTest testability (CONST-035).
// ---------------------------------------------------------------------------

/**
 * Map an LSP4J [org.eclipse.lsp4j.WorkspaceEdit] to Yole's [WorkspaceEdit].
 *
 * Prefers `documentChanges` (modern, LSP 3.18) over `changes` (legacy)
 * when both fields are present. Only [TextDocumentEdit] entries are mapped;
 * [ResourceOperation] entries (file-level ops) are skipped.
 *
 * Mutation: swap preference → documentChanges_preferredOver_changes FAILS.
 */
internal fun mapLspWorkspaceEditToYoleAndroid(lspEdit: org.eclipse.lsp4j.WorkspaceEdit): WorkspaceEdit {
    val docChanges = lspEdit.documentChanges
    if (!docChanges.isNullOrEmpty()) {
        val mapped = mutableMapOf<String, List<TextEdit>>()
        for (either in docChanges) {
            if (either.isLeft) {
                // TextDocumentEdit — carries uri + list of Either<TextEdit, SnippetTextEdit>
                val tde: TextDocumentEdit = either.left
                val uri = tde.textDocument?.uri ?: continue
                // LSP4J 1.0.0: edits is List<Either<TextEdit, SnippetTextEdit>>.
                val edits = tde.edits.orEmpty().map { editEither ->
                    if (editEither.isLeft) {
                        TextEdit(range = 0..0, newText = editEither.left.newText ?: "")
                    } else {
                        val snip = editEither.right
                        TextEdit(range = 0..0, newText = snip.snippet?.value ?: "")
                    }
                }
                mapped[uri] = edits
            }
        }
        return WorkspaceEdit(changes = mapped)
    }
    val rawChanges = lspEdit.changes.orEmpty()
    val mapped = rawChanges.mapValues { (_, edits) ->
        edits.orEmpty().map { TextEdit(range = 0..0, newText = it.newText ?: "") }
    }
    return WorkspaceEdit(changes = mapped)
}

/**
 * Map an LSP4J `Either<Command, CodeAction>` entry to Yole's [CodeAction].
 *
 * Mutation: return CodeAction("WRONG",null,null,null) → commandEither FAILS.
 */
internal fun mapLspCodeActionAndroid(
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
            edit = lspAction.edit?.let { mapLspWorkspaceEditToYoleAndroid(it) },
            command = lspAction.command?.command,
        )
    }
}

/**
 * Map a single LSP4J [org.eclipse.lsp4j.TextEdit] with real offset resolution.
 *
 * Mutation: return 0..0 → real-offset tests FAIL.
 */
internal fun mapLspTextEditWithDocAndroid(lspEdit: LspTextEdit, docText: String): TextEdit {
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
 * Map a batch of LSP4J [org.eclipse.lsp4j.TextEdit]s with real offset resolution.
 *
 * Mutation: return emptyList() → single-edit and multi-edit tests FAIL.
 */
internal fun mapLspTextEditsAndroid(lspEdits: List<LspTextEdit>, docText: String): List<TextEdit> =
    lspEdits.map { mapLspTextEditWithDocAndroid(it, docText) }

/**
 * Map an LSP4J [org.eclipse.lsp4j.SignatureHelp] to Yole's [SignatureHelp].
 *
 * [org.eclipse.lsp4j.SignatureInformation.documentation] is
 * `Either<String, MarkupContent>` — plain string (left) or markdown (right).
 *
 * Mutation: return null always → stringDocumentation_isExtracted FAILS.
 */
internal fun mapLspSignatureHelpToYoleAndroid(lspHelp: org.eclipse.lsp4j.SignatureHelp): SignatureHelp? {
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
