/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57: immutable VS Code theme value object.
 *   - uiColor(key) looks up VS Code `colors.*` keys.
 *   - tokenColor(scope) looks up `tokenColors[].scope` with scope-hierarchy fallback.
 *
 *########################################################*/
package digital.vasic.yole.syntax.theme

/**
 * Immutable VS Code theme.
 *
 * @property name human-readable name from JSON `name` field.
 * @property type "dark" or "light" from JSON `type` field.
 * @property uiColors map of VS Code colors.* keys → ARGB int (0xFFRRGGBB).
 * @property tokenColors map of token scope → ARGB int. Lookup falls back through
 *           dot-separated scope hierarchy (foo.bar.baz → foo.bar → foo).
 */
data class Theme(
    val name: String,
    val type: String,
    val uiColors: Map<String, Int>,
    val tokenColors: Map<String, Int>,
) {
    /** Look up a VS Code UI color (e.g., "editor.background"). Returns null if absent. */
    fun uiColor(key: String): Int? = uiColors[key]

    /** Look up a token-scope color with VS Code hierarchical fallback. */
    fun tokenColor(scope: String): Int? {
        // Exact match first.
        tokenColors[scope]?.let { return it }
        // Fall back: "foo.bar.baz" → "foo.bar" → "foo".
        var s = scope
        while (s.contains('.')) {
            s = s.substringBeforeLast('.')
            tokenColors[s]?.let { return it }
        }
        return null
    }
}
