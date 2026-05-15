/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 5: LspCompletionProvider — Android (JVM) actual.
 *
 * Body mirrors Desktop actual. LspServerInstaller.ensureInstalled()
 * returns InstallError.NotInstalled on Android until Phase 8 adds
 * SplitInstallManager-aware extraction; complete() therefore returns
 * emptyList() honestly — other providers (token + snippet) remain
 * unaffected.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: this file — JVM actual, same delegation path as Desktop.
 *   - Desktop: see LspCompletionProvider.desktop.kt.
 *   - iOS:     honest stub (iosMain).
 *   - Web/Wasm: honest stub (wasmJsMain).
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
        val (line, character) = cursorCharToLineCol(ctx.text, ctx.cursorChar)
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
            kind = mapKind(kind),
            score = mapSortTextToScore(sortText),
            range = range,
        )

    private fun mapKind(lspKind: String): CompletionItem.Kind = when (lspKind) {
        "Snippet" -> CompletionItem.Kind.Snippet
        "Keyword" -> CompletionItem.Kind.Keyword
        "Function", "Method", "Constructor", "Class", "Interface", "Module",
        "Property", "Field", "Variable", "Constant", "Enum", "EnumMember",
        "Struct", "TypeParameter", "Event", "Operator",
        -> CompletionItem.Kind.Identifier
        else -> CompletionItem.Kind.Word
    }

    private fun mapSortTextToScore(sortText: String?): Double {
        if (sortText == null) return 1.0
        val prefix = sortText.take(4).padEnd(4, 'z')
        val codepointSum = prefix.sumOf { it.code }.toDouble()
        val maxSum = 4.0 * 'z'.code
        return 1.0 + (1.0 - codepointSum / maxSum)
    }

    private fun cursorCharToLineCol(text: String, cursorChar: Int): Pair<Int, Int> {
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
}
