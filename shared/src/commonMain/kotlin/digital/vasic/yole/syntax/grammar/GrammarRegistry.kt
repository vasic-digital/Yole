/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 8: filename-to-grammar resolution. Consumers (FILES tab
 * badges, the editor pre-flight, the preview code-block highlighter) ask
 * "what grammar should I use for this file?" and get back either a
 * Grammar or null (when the user has disabled that format via
 * EnabledFormatGate). Falls back to the "plaintext" id when no grammar
 * matches.
 *
 *########################################################*/
package digital.vasic.yole.syntax.grammar

import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.Grammar
import digital.vasic.yole.syntax.GrammarMetadata

/**
 * Resolves a filename to its language grammar by walking
 * [GrammarMetadata.all] and honoring [EnabledFormatGate]. v1 only
 * detects by filename extension; future enhancements may add
 * MIME-type / content-sniffing detection.
 */
object GrammarRegistry {
    /**
     * Returns the [Grammar] whose extensions include the given filename
     * (case-insensitive). Returns `null` if no grammar matches OR if the
     * matching grammar is currently disabled via [EnabledFormatGate].
     */
    fun detectByFilename(filename: String): Grammar? {
        val lowered = filename.lowercase()
        for (g in GrammarMetadata.all) {
            for (ext in g.extensions) {
                if (lowered.endsWith(ext.lowercase())) {
                    return if (EnabledFormatGate.isEnabled(g.id)) g else null
                }
            }
        }
        return null
    }

    /**
     * Returns the language id (e.g., `"markdown"`) for the given filename
     * or `"plaintext"` if no enabled grammar matches. Useful for callers
     * that need a non-null string id (e.g., to pass to
     * [digital.vasic.yole.syntax.TokenizerEngine.tokenize]).
     */
    fun detectLangId(filename: String): String =
        detectByFilename(filename)?.id ?: "plaintext"
}
