/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 11: maps a filename to a theme-tinted Color for the
 * FILES tab badge chip. Disabled formats / unknown files return null,
 * letting the caller draw a generic gray (or omit the badge entirely).
 *
 *########################################################*/
package digital.vasic.yole.syntax.render

import digital.vasic.yole.syntax.grammar.GrammarRegistry
import digital.vasic.yole.syntax.theme.Theme

/**
 * Computes the per-file badge tint and language id for the FILES tab.
 *
 * iter-57 Phase 11. Consumers (the Android FILES tab, Desktop file
 * browser, future iOS/Web equivalents) call [tintFor] with the active
 * theme and a filename to get the ARGB int to paint the chip
 * background, and [langIdFor] for the 2-letter chip label. Both
 * return `null` when the format is unknown OR disabled via
 * [digital.vasic.yole.syntax.EnabledFormatGate]; callers then either
 * omit the badge or fall back to a generic gray.
 *
 * Lookup order in [tintFor]: theme's `badge.background.<langId>`
 * per-language override first, then the generic `badge.background`
 * fallback. This mirrors VS Code's hierarchical UI-color convention.
 */
object BadgeTinter {
    /**
     * Returns ARGB int (compose-Color-ready) for the filename's badge
     * tint using the active theme. Returns null when:
     *  - filename has no recognized extension, or
     *  - the recognized format is disabled in EnabledFormatGate
     *    (GrammarRegistry.detectByFilename returns null in that case).
     *
     * Lookup order: `theme.uiColor("badge.background.<langId>")` first,
     * falling back to `theme.uiColor("badge.background")`.
     */
    fun tintFor(filename: String, theme: Theme): Int? {
        val grammar = GrammarRegistry.detectByFilename(filename) ?: return null
        val perLang = theme.uiColor("badge.background.${grammar.id}")
        if (perLang != null) return perLang
        return theme.uiColor("badge.background")
    }

    /**
     * Returns the language id for the filename (used as the 2-letter
     * chip label and the testTag suffix in the Android UI), or null
     * if the format is unknown or disabled via EnabledFormatGate.
     */
    fun langIdFor(filename: String): String? =
        GrammarRegistry.detectByFilename(filename)?.id
}
