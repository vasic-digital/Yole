/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 8: builds a Compose AnnotatedString from raw text +
 * a list of Token + the active Theme. Each Token is translated through
 * ScopeMapper into a VS Code TextMate scope, then resolved against the
 * Theme. If the theme has no color for the resolved scope (even after
 * the theme's own hierarchical fallback), the token is left unstyled —
 * NEVER injected with a placeholder color (CONST-035: no bluff styling).
 *
 *########################################################*/
package digital.vasic.yole.syntax.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import digital.vasic.yole.syntax.Token
import digital.vasic.yole.syntax.grammar.ScopeMapper
import digital.vasic.yole.syntax.theme.Theme

/**
 * Compose AnnotatedString builder for tokenized text.
 *
 * Token byte offsets are treated as char offsets (correct for ASCII;
 * approximate for multibyte UTF-8 content). Full codepoint-correct
 * mapping is deferred to a v1.1 enhancement; the v1 contract is
 * documented on [build] below.
 */
object AnnotatedStringBuilder {
    /**
     * Build a Compose [AnnotatedString] from [text] + [tokens] + [theme].
     *
     * Implementation notes:
     * - Tokens whose mapped scope has no theme color (after ScopeMapper
     *   AND Theme hierarchical fallback) are dropped — the text appears
     *   in the default editor color, NOT a placeholder.
     * - Token byte offsets are clamped to `[0, text.length]` defensively
     *   so a malformed engine cannot crash the renderer.
     * - For ASCII content, byte offsets == char offsets exactly. For
     *   multibyte UTF-8 content (e.g., CJK), v1 approximates by using
     *   the byte offset as the char offset; this may visually shift
     *   styling within multibyte glyphs but never panics.
     */
    fun build(text: String, tokens: List<Token>, theme: Theme): AnnotatedString =
        buildAnnotatedString {
            append(text)
            val len = text.length
            for (tok in tokens) {
                val vsScope = ScopeMapper.treeSitterToVsCode(tok.scope)
                val argb = theme.tokenColor(vsScope) ?: continue
                val safeStart = tok.startByte.coerceIn(0, len)
                val safeEnd = tok.endByte.coerceIn(safeStart, len)
                if (safeStart < safeEnd) {
                    addStyle(SpanStyle(color = Color(argb)), safeStart, safeEnd)
                }
            }
        }
}
