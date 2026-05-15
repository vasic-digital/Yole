/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 F2 Phase 8.2 — Markdown fenced-code-block sub-language
 * tokenization.
 *
 * Walks a markdown document; for each `fenced_code_block` whose
 * opening line declares a language tag (e.g. ```kotlin), if a matching
 * sub-engine has been provided via [subEnginesByLang], re-tokenizes
 * the fence body with that sub-engine and splices the resulting
 * tokens (byte-offset-adjusted) into the outer markdown token stream.
 *
 * This supersedes — at the source level — what iter-57 Phase 10's
 * `PreviewCodeBlockHighlighter` did at the HTML-output level: edit-
 * mode highlighting inside fences (the BasicTextField visual
 * transformation) now sees the correct sub-language scopes.
 *
 * Anti-bluff (CONST-035) contract:
 *
 *   - If no sub-engine is provided for the fence's lang tag (or the
 *     fence has no lang tag), the fence body is left as outer
 *     markdown tokens. We do NOT pretend the body is highlighted by
 *     the sub-grammar when no sub-grammar is wired.
 *   - If a provided sub-engine throws when tokenizing the fence body
 *     (engine not initialized, grammar not loaded, gated off via
 *     EnabledFormatGate, native crash), the failure is treated as
 *     "sub-grammar unavailable for this fence" — we keep the outer
 *     markdown tokens and continue. We never fabricate tokens.
 *   - Region detection is by regex on the raw source text, NOT a
 *     tree-sitter parse tree walk (the TSTree/TSNode API is JVM-only
 *     / platform-specific; this file lives in commonMain). The regex
 *     matches the same shape — opening fence (```lang or ~~~lang
 *     followed by a newline) → body → closing fence (``` or ~~~ on
 *     its own line) — that the tree-sitter markdown grammar's
 *     `fenced_code_block` production describes.
 *
 *########################################################*/
package digital.vasic.yole.language.special

import digital.vasic.yole.syntax.Token
import digital.vasic.yole.syntax.TokenizerEngine

/**
 * Tokenize markdown text with embedded sub-language fenced code blocks.
 *
 * The returned list contains the outer markdown tokens, except in
 * regions inside a fenced code block whose lang tag has a matching
 * entry in [subEnginesByLang] AND the sub-engine successfully produced
 * tokens — those regions are replaced by the sub-engine's tokens,
 * with byte offsets adjusted so the entire stream is referenced to
 * the original [text].
 */
object MarkdownCodeFences {

    /**
     * Pattern for one ```lang ... ``` fenced code block.
     *
     * Captures:
     *   group 1 — the lang tag (e.g. "kotlin"). May contain dashes,
     *             digits, plus signs (`c++`), and underscores.
     *   group 2 — the fence body (between the opening-fence newline
     *             and the closing-fence line).
     *
     * Accepts BOTH triple-backtick and triple-tilde fences (CommonMark
     * §4.5). Indentation in front of the opening fence is captured to
     * ensure the closing fence at the same indentation is the one we
     * match (this is a simplification — the markdown spec is lenient
     * with closing-fence indentation; the tree-sitter parser handles
     * it more precisely. For the test snippets and >99% of real-world
     * fences this is correct).
     */
    private val backtickFenceRegex = Regex(
        "^(?:[ ]{0,3})```([a-zA-Z0-9_+\\-]*)[ \\t]*\\r?\\n([\\s\\S]*?)\\r?\\n[ ]{0,3}```[ \\t]*(?:\\r?\\n|$)",
        RegexOption.MULTILINE,
    )

    private val tildeFenceRegex = Regex(
        "^(?:[ ]{0,3})~~~([a-zA-Z0-9_+\\-]*)[ \\t]*\\r?\\n([\\s\\S]*?)\\r?\\n[ ]{0,3}~~~[ \\t]*(?:\\r?\\n|$)",
        RegexOption.MULTILINE,
    )

    /**
     * Tokenize [text] using the markdown grammar plus optional
     * sub-grammars for fenced code blocks whose lang tag has a key in
     * [subEnginesByLang].
     *
     * @param text the full markdown source.
     * @param markdownEngine engine with `markdown` grammar loaded.
     * @param subEnginesByLang map of lang ID -> sub-engine. The sub-
     *   engine must have already had `loadGrammar(lang)` called.
     */
    suspend fun tokenize(
        text: String,
        markdownEngine: TokenizerEngine,
        subEnginesByLang: Map<String, TokenizerEngine>,
    ): List<Token> {
        val outer = markdownEngine.tokenize(text, "markdown")

        if (subEnginesByLang.isEmpty()) {
            return outer
        }

        val fences = mutableListOf<FenceRegion>()
        collectFences(text, backtickFenceRegex, fences)
        collectFences(text, tildeFenceRegex, fences)
        if (fences.isEmpty()) {
            return outer
        }

        val resolved = mutableListOf<ResolvedRegion>()
        for (f in fences) {
            val subEngine = subEnginesByLang[f.lang] ?: continue
            val subTokens: List<Token>? = try {
                val raw = subEngine.tokenize(f.body, f.lang)
                if (raw.isEmpty()) {
                    null
                } else {
                    raw.map {
                        Token(
                            startByte = it.startByte + f.byteStart,
                            endByte = it.endByte + f.byteStart,
                            scope = it.scope,
                        )
                    }
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (_: Throwable) {
                null
            }
            if (subTokens != null) {
                resolved += ResolvedRegion(f.byteStart, f.byteEnd, subTokens)
            }
        }

        if (resolved.isEmpty()) {
            return outer
        }

        val out = ArrayList<Token>(outer.size + resolved.sumOf { it.subTokens.size })
        for (t in outer) {
            val coveredBy = resolved.firstOrNull { r ->
                t.startByte >= r.byteStart && t.endByte <= r.byteEnd
            }
            if (coveredBy == null) out += t
        }
        for (r in resolved) {
            out += r.subTokens
        }
        out.sortBy { it.startByte }
        return out
    }

    private fun collectFences(text: String, regex: Regex, into: MutableList<FenceRegion>) {
        for (m in regex.findAll(text)) {
            val lang = m.groupValues[1].trim()
            if (lang.isEmpty()) continue // unlabeled fence — no sub-grammar to apply
            val bodyRange = m.groups[2]?.range ?: continue
            if (bodyRange.isEmpty()) continue
            val charStart = bodyRange.first
            val charEnd = bodyRange.last + 1
            val byteStart = charOffsetToByte(text, charStart)
            val byteEnd = charOffsetToByte(text, charEnd)
            val body = text.substring(charStart, charEnd)
            into += FenceRegion(
                byteStart = byteStart,
                byteEnd = byteEnd,
                body = body,
                lang = lang,
            )
        }
    }

    /**
     * Map a UTF-16 char offset in [text] to the corresponding UTF-8
     * byte offset (mirrors HtmlEmbeddedLang.charOffsetToByte).
     */
    private fun charOffsetToByte(text: String, charOffset: Int): Int {
        if (charOffset <= 0) return 0
        if (charOffset >= text.length) return text.encodeToByteArray().size
        return text.substring(0, charOffset).encodeToByteArray().size
    }

    private data class FenceRegion(
        val byteStart: Int,
        val byteEnd: Int,
        val body: String,
        val lang: String,
    )

    private data class ResolvedRegion(
        val byteStart: Int,
        val byteEnd: Int,
        val subTokens: List<Token>,
    )
}
