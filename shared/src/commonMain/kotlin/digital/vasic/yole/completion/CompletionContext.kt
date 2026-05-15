/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60: auto-complete context snapshot.
 * iter-61 Phase 5 deviation: added documentUri + workspaceRoot optional
 * fields (default null). Plan did not anticipate document-URI threading;
 * deviation tracked in docs/CONTINUATION.md. All existing call sites
 * keep working — defaults preserve backward compatibility.
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
 * @property documentUri file:// URI of the open document, or null when
 *   the editor does not know the path (e.g. unsaved scratch buffer).
 *   Used by [LspCompletionProvider] to pass the correct URI to the LSP
 *   server. Phase 6 of iter-61 will set this from IdeEditorScreen.
 * @property workspaceRoot absolute path of the project root, or null to
 *   let [LspCompletionProvider] fall back to the system tmp dir. Phase 6
 *   of iter-61 will resolve this via [LspWorkspaceResolver].
 */
data class CompletionContext(
    val text: String,
    val cursorChar: Int,
    val langId: String?,
    val prefix: String,
    val prefixRange: IntRange,
    val surroundingScope: String? = null,
    val documentUri: String? = null,
    val workspaceRoot: String? = null,
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
