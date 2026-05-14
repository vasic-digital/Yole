/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 8: top-level syntax highlighting API. Glue between the
 * platform TokenizerEngine and the Compose AnnotatedString consumed by
 * the editor / preview. All concerns above the API surface
 * (engine lifecycle, scope mapping, theme lookup, rendering) live in
 * this single class so callers pass a string + lang and get back a
 * styled AnnotatedString.
 *
 * Graceful degradation (spec §4 error table):
 *   - lang disabled via EnabledFormatGate → return unstyled AnnotatedString.
 *   - engine throws → return unstyled AnnotatedString. Higher layers may
 *     log / surface the engine error; SyntaxHighlighter never produces
 *     fake tokens (CONST-035).
 *
 *########################################################*/
package digital.vasic.yole.syntax

import androidx.compose.ui.text.AnnotatedString
import digital.vasic.yole.syntax.render.AnnotatedStringBuilder
import digital.vasic.yole.syntax.theme.Theme

/**
 * Top-level syntax-highlighting API.
 *
 * @param engine the platform tokenizer engine. Must have had
 *   [TokenizerEngine.initialize] called successfully before any
 *   [highlight] / [tokens] call.
 * @param theme function returning the currently active [Theme]. Passed
 *   as a function (not a value) so each highlight call observes the
 *   latest theme without the consumer having to rebuild the highlighter
 *   on every theme change.
 */
class SyntaxHighlighter(
    private val engine: TokenizerEngine,
    private val theme: () -> Theme,
) {
    /**
     * Tokenize [text] in the [lang] grammar and produce a Compose
     * [AnnotatedString] colored by the current theme.
     *
     * Returns an unstyled `AnnotatedString(text)` when:
     *  - [lang] is gated off in [EnabledFormatGate], OR
     *  - [TokenizerEngine.tokenize] throws (engine load failure,
     *    grammar not found, etc.).
     *
     * Never throws for typical caller usage — graceful degradation is
     * preferred over an exception because the editor must always render
     * the document, even if syntax highlighting is unavailable.
     */
    suspend fun highlight(text: String, lang: String): AnnotatedString {
        if (!EnabledFormatGate.isEnabled(lang)) {
            return AnnotatedString(text)
        }
        val toks = try {
            engine.tokenize(text, lang)
        } catch (e: Throwable) {
            // CONST-035: silent degradation, never fake tokens. Higher
            // layers are responsible for surfacing the engine error if
            // they care; the editor must always render the text.
            return AnnotatedString(text)
        }
        return AnnotatedStringBuilder.build(text, toks, theme())
    }

    /**
     * Lower-level escape hatch: return the raw token list without
     * building an AnnotatedString. Returns the empty list if [lang] is
     * disabled (gracefully — no exception).
     */
    suspend fun tokens(text: String, lang: String): List<Token> =
        if (EnabledFormatGate.isEnabled(lang)) engine.tokenize(text, lang) else emptyList()
}
