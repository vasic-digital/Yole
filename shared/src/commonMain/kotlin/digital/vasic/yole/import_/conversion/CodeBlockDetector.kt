/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 2: CodeBlockDetector — monospace font whitelist.
 *#######################################################*/
package digital.vasic.yole.import_.conversion

/**
 * Determines whether a font name belongs to the well-known monospace
 * (code) fonts that should trigger a Markdown fenced code block.
 *
 * Matching is case-insensitive and uses `contains` semantics so that
 * variants such as "Courier New" still match the "Courier" entry.
 */
object CodeBlockDetector {

    /**
     * Canonical monospace font name fragments.
     * A run whose font name contains any of these (case-insensitive) is
     * considered monospace.
     */
    private val MONOSPACE_FRAGMENTS = listOf(
        "Courier",
        "Consolas",
        "Menlo",
        "Monaco",
        "Fira Code",
        "Roboto Mono",
        "Source Code Pro",
        "JetBrains Mono",
        "Inconsolata",
        "DejaVu Sans Mono",
        "Andale Mono",
    )

    /**
     * Returns `true` when [fontName] is a recognised monospace font.
     *
     * @param fontName The font name as reported by the source document.
     */
    fun isMonospaceRun(fontName: String): Boolean {
        val lower = fontName.lowercase()
        return MONOSPACE_FRAGMENTS.any { fragment -> lower.contains(fragment.lowercase()) }
    }
}
