/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 5: LspCompletionProvider — Desktop (JVM) actual.
 *
 * Delegates to LspServerHost.complete(), converting LspCompletionLine
 * results into CompletionItem values. The two helper functions
 * (lspCursorCharToLineCol, mapLspKindToItemKind) are internal top-level
 * functions so LspCompletionProviderTest can call them directly.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop: this file — full LspServerHost delegation.
 *   - Android: separate actual (androidMain) — identical body.
 *   - iOS:     honest stub returning emptyList (iosMain).
 *   - Web/Wasm: honest stub returning emptyList (wasmJsMain).
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.completion.providers

import digital.vasic.yole.completion.CompletionContext
import digital.vasic.yole.completion.CompletionItem
import digital.vasic.yole.completion.CompletionProvider
import digital.vasic.yole.lsp.LspCompletionLine
import digital.vasic.yole.lsp.LspServerHost

actual class LspCompletionProvider actual constructor(
    private val host: LspServerHost,
) : CompletionProvider {

    actual override val id: String = "lsp"

    actual override suspend fun complete(ctx: CompletionContext): List<CompletionItem> {
        val langId = ctx.langId ?: return emptyList()
        val uri = ctx.documentUri ?: "untitled:scratch-$langId"
        val workspaceRoot = ctx.workspaceRoot ?: System.getProperty("java.io.tmpdir") ?: "/tmp"
        val (line, character) = lspCursorCharToLineCol(ctx.text, ctx.cursorChar)
        val result = host.complete(
            langId = langId,
            documentUri = uri,
            documentText = ctx.text,
            documentVersion = 0,
            line = line,
            character = character,
            workspaceRoot = workspaceRoot,
        )
        return result.items
            .filter { it.label.startsWith(ctx.prefix) || it.insertText.startsWith(ctx.prefix) }
            .map { it.toCompletionItem(ctx.prefixRange) }
    }

    private fun LspCompletionLine.toCompletionItem(range: IntRange): CompletionItem =
        CompletionItem(
            label = label,
            insertText = insertText,
            kind = mapLspKindToItemKind(kind),
            score = mapSortTextToScore(sortText),
            range = range,
        )

    private fun mapSortTextToScore(sortText: String?): Double {
        if (sortText == null) return 1.0
        // LSP sortText is lexically ordered — earlier sortText = higher priority.
        // Map the first 4 chars to a [1.0, 2.0] range by inverting lexical position:
        // lower sortText → higher score → LSP items naturally outrank token-frequency.
        val prefix = sortText.take(4).padEnd(4, 'z')
        val codepointSum = prefix.sumOf { it.code }.toDouble()
        val maxSum = 4.0 * 'z'.code
        return 1.0 + (1.0 - codepointSum / maxSum)
    }
}

/**
 * Convert a char-index cursor position to (line, column) for an LSP Position.
 * Internal so [LspCompletionProviderTest] can exercise it without reflection.
 *
 * @param text the full document text.
 * @param cursorChar char index of the cursor within [text].
 * @return Pair(zero-based line index, zero-based column index).
 */
internal fun lspCursorCharToLineCol(text: String, cursorChar: Int): Pair<Int, Int> {
    val safe = cursorChar.coerceIn(0, text.length)
    var line = 0
    var lineStart = 0
    for (i in 0 until safe) {
        if (text[i] == '\n') {
            line++
            lineStart = i + 1
        }
    }
    return line to (safe - lineStart)
}

/**
 * Map an LSP CompletionItemKind name to a [CompletionItem.Kind].
 * Internal so [LspCompletionProviderTest] can exercise it without reflection.
 *
 * LSP kind names follow the enum in the LSP spec §3.18.1. CompletionItemKind
 * ordinals are transmitted as integers; the JVM actual receives them already
 * decoded to the `.name` string by [LspCompletionLine.kind].
 */
internal fun mapLspKindToItemKind(lspKind: String): CompletionItem.Kind = when (lspKind) {
    "Snippet" -> CompletionItem.Kind.Snippet
    "Keyword" -> CompletionItem.Kind.Keyword
    "Function", "Method", "Constructor", "Class", "Interface", "Module",
    "Property", "Field", "Variable", "Constant", "Enum", "EnumMember",
    "Struct", "TypeParameter", "Event", "Operator",
    -> CompletionItem.Kind.Identifier
    else -> CompletionItem.Kind.Word
}
