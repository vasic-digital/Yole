/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-58 F2 Phase 8.1 — HTML embedded sub-language tokenization.
 *
 * Walks an HTML document and re-tokenizes the bodies of `<style>` blocks
 * with the CSS grammar and `<script>` blocks with the JavaScript grammar
 * (when those sub-grammars are available). The result is a merged list
 * of [Token]s whose byte offsets are all referenced to the original
 * HTML source — sub-tokens are offset by the start byte of the embedded
 * region so consumers downstream ([SyntaxHighlighter] /
 * [PreviewCodeBlockHighlighter]) can treat them as if they came from a
 * single tokenize pass.
 *
 * Anti-bluff (CONST-035) contract:
 *
 *   - If a sub-grammar is NOT provided (`cssEngine` or `jsEngine` is
 *     null), the corresponding embedded region is left as outer HTML
 *     tokens only. We do NOT synthesise CSS/JS scopes from thin air.
 *   - If a provided sub-engine throws when tokenizing an embedded
 *     region (engine not initialized, grammar not loaded, gated off via
 *     EnabledFormatGate, native crash), the failure is treated as
 *     "sub-grammar unavailable for this region" — we keep the outer
 *     HTML tokens for that region and continue. We never emit fabricated
 *     tokens labeled as if they were real CSS/JS output.
 *   - Region detection is done by regex on the raw source text, NOT by
 *     walking the tree-sitter parse tree. The tree-sitter `TSTree` /
 *     `TSNode` API surface is JVM-only (and Kotlin/Native-specific on
 *     iOS, JS-specific on Wasm), so a tree walk cannot live in
 *     commonMain. The regex matches the same node shape — opening tag
 *     (`<style …>` or `<script …>`) → body → closing tag (`</style>`
 *     or `</script>`) — that the tree-sitter HTML grammar's
 *     `style_element` / `script_element` productions describe.
 *
 *   The regex approach is sound for well-formed HTML; it does NOT
 *   handle pathological inputs where `</style>` or `</script>` appears
 *   inside a string literal (CSS doesn't have `</style>` literals; JS
 *   shouldn't either when CDATA-wrapped). Anything beyond that is a
 *   user-input edge case we degrade gracefully on — never bluff.
 *
 *########################################################*/
package digital.vasic.yole.language.special

import digital.vasic.yole.syntax.Token
import digital.vasic.yole.syntax.TokenizerEngine

/**
 * Tokenize HTML text with embedded CSS in `<style>` and JavaScript in
 * `<script>` sub-grammars.
 *
 * The returned list contains the outer HTML tokens, except in regions
 * inside a `<style>` or `<script>` body where the corresponding
 * sub-engine successfully produced tokens — those regions are replaced
 * by the sub-engine's tokens, with byte offsets adjusted so the entire
 * stream is referenced to the original [text].
 */
object HtmlEmbeddedLang {

    /**
     * Pattern for one `<style ...>...</style>` region. Captures:
     *   group 1 — the body bytes between the closing `>` of the opening
     *             tag and the `<` of the closing tag.
     *
     * `[\s\S]` (not `.`) is used inside the body so newlines match.
     * `[^>]*` on the opening tag accepts any attribute payload.
     */
    private val styleRegex = Regex(
        "<style\\b[^>]*>([\\s\\S]*?)</style>",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Pattern for one `<script ...>...</script>` region.
     */
    private val scriptRegex = Regex(
        "<script\\b[^>]*>([\\s\\S]*?)</script>",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Tokenize [text] using the HTML grammar plus optional CSS/JS
     * sub-grammars for embedded regions.
     *
     * @param text the full HTML source.
     * @param htmlEngine the engine that has already had `loadGrammar("html")`
     *   called successfully on it.
     * @param cssEngine engine with `css` grammar loaded — if null, `<style>`
     *   regions are left as outer HTML tokens (no fabrication).
     * @param jsEngine engine with `javascript` grammar loaded — if null,
     *   `<script>` regions are left as outer HTML tokens (no fabrication).
     *
     * The same `TokenizerEngine` instance can play multiple roles
     * (i.e. callers commonly pass `engine, engine, engine`); each call
     * is honored independently.
     */
    suspend fun tokenize(
        text: String,
        htmlEngine: TokenizerEngine,
        cssEngine: TokenizerEngine?,
        jsEngine: TokenizerEngine?,
    ): List<Token> {
        val outer = htmlEngine.tokenize(text, "html")
        // substring(int, int) on a Kotlin String operates on char indices;
        // tree-sitter's Token byte offsets are UTF-8 byte indices. For pure-
        // ASCII inputs the two coincide; otherwise we translate char offsets
        // -> UTF-8 byte offsets via [charOffsetToByte] when slicing.
        val embeddedRegions = mutableListOf<EmbeddedRegion>()
        if (cssEngine != null) {
            for (m in styleRegex.findAll(text)) {
                val bodyRange = m.groups[1]?.range ?: continue
                if (bodyRange.isEmpty()) continue
                val charStart = bodyRange.first
                val charEnd = bodyRange.last + 1
                val byteStart = charOffsetToByte(text, charStart)
                val byteEnd = charOffsetToByte(text, charEnd)
                val body = text.substring(charStart, charEnd)
                embeddedRegions += EmbeddedRegion(
                    byteStart = byteStart,
                    byteEnd = byteEnd,
                    body = body,
                    subLang = "css",
                    subEngine = cssEngine,
                )
            }
        }
        if (jsEngine != null) {
            for (m in scriptRegex.findAll(text)) {
                val bodyRange = m.groups[1]?.range ?: continue
                if (bodyRange.isEmpty()) continue
                val charStart = bodyRange.first
                val charEnd = bodyRange.last + 1
                val byteStart = charOffsetToByte(text, charStart)
                val byteEnd = charOffsetToByte(text, charEnd)
                val body = text.substring(charStart, charEnd)
                embeddedRegions += EmbeddedRegion(
                    byteStart = byteStart,
                    byteEnd = byteEnd,
                    body = body,
                    subLang = "javascript",
                    subEngine = jsEngine,
                )
            }
        }

        if (embeddedRegions.isEmpty()) {
            return outer
        }

        // Re-tokenize each embedded region with its sub-engine. On any
        // failure (gated off, grammar not loaded, native error) we honor
        // CONST-035 and leave that region as outer HTML tokens — no
        // fabricated sub-tokens.
        val resolved = mutableListOf<ResolvedRegion>()
        for (r in embeddedRegions) {
            val subTokens: List<Token>? = try {
                val raw = r.subEngine.tokenize(r.body, r.subLang)
                if (raw.isEmpty()) {
                    null
                } else {
                    raw.map {
                        Token(
                            startByte = it.startByte + r.byteStart,
                            endByte = it.endByte + r.byteStart,
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
                resolved += ResolvedRegion(r.byteStart, r.byteEnd, subTokens)
            }
        }

        if (resolved.isEmpty()) {
            return outer
        }

        // Merge: drop outer tokens that fall inside any resolved region,
        // splice in the sub-tokens, return sorted by startByte.
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

    /**
     * Map a UTF-16 char offset in [text] to the corresponding UTF-8 byte
     * offset, mirroring how tree-sitter reports byte positions on its
     * tokens. For pure-ASCII inputs the two are identical; for inputs
     * containing supplementary or multibyte code points the difference
     * matters.
     */
    private fun charOffsetToByte(text: String, charOffset: Int): Int {
        if (charOffset <= 0) return 0
        if (charOffset >= text.length) return text.encodeToByteArray().size
        return text.substring(0, charOffset).encodeToByteArray().size
    }

    private data class EmbeddedRegion(
        val byteStart: Int,
        val byteEnd: Int,
        val body: String,
        val subLang: String,
        val subEngine: TokenizerEngine,
    )

    private data class ResolvedRegion(
        val byteStart: Int,
        val byteEnd: Int,
        val subTokens: List<Token>,
    )
}
