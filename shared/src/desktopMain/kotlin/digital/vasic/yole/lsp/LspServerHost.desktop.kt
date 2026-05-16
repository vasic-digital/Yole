/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 4 / iter-62 Phase 2: LspServerHost — Desktop (JVM) actual.
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
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.TextDocumentClientCapabilities
import org.eclipse.lsp4j.TextDocumentContentChangeEvent
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
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
                val client = buildFakeClient()
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

    private fun buildFakeClient(): LanguageClient = object : LanguageClient {
        override fun telemetryEvent(o: Any?) {}
        override fun publishDiagnostics(p: PublishDiagnosticsParams?) {
            p ?: return
            val mapped = p.diagnostics.orEmpty().map { lspDiag ->
                Diagnostic(
                    severity = mapLspSeverity(lspDiag.severity),
                    range = 0..0, // Phase 3 finalizes via LspRangeMapping + cached docTexts
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
