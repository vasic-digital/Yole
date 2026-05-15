/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60: auto-complete context snapshot.
 *#######################################################*/
package digital.vasic.yole.completion

/**
 * Snapshot of editor state for a single completion request.
 *
 * @property text full document text.
 * @property cursorChar cursor position as a char index in [text].
 * @property langId lang id from LanguageRegistry; null = plaintext.
 * @property prefix the partial word the user has already typed
 *   (chars [a-zA-Z0-9_] walking back from cursor until whitespace).
 * @property prefixRange char range of [prefix] inside [text].
 * @property surroundingScope Tree-Sitter node-type at cursor, or null
 *   when Tree-Sitter is unavailable (graceful degradation).
 */
data class CompletionContext(
    val text: String,
    val cursorChar: Int,
    val langId: String?,
    val prefix: String,
    val prefixRange: IntRange,
    val surroundingScope: String? = null,
) {
    companion object {
        /**
         * Build a context with the prefix derived from text + cursor.
         * surroundingScope is filled by the engine via Tree-Sitter (not here).
         */
        fun of(text: String, cursorChar: Int, langId: String?): CompletionContext {
            val safe = cursorChar.coerceIn(0, text.length)
            var start = safe
            while (start > 0 && text[start - 1].isWordChar()) start--
            val prefix = text.substring(start, safe)
            return CompletionContext(
                text = text,
                cursorChar = safe,
                langId = langId,
                prefix = prefix,
                prefixRange = start..safe,
            )
        }

        private fun Char.isWordChar(): Boolean =
            this.isLetterOrDigit() || this == '_'
    }
}
