/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 8: translation layer between Tree-Sitter / vscode-textmate
 * native scope names and VS Code TextMate scope names that VS Code theme
 * JSON files key on. Tree-Sitter emits short hierarchical scopes
 * ("comment", "keyword.control", "function.builtin"); VS Code themes
 * key on TextMate-style scopes ("comment.line.double-slash",
 * "keyword.control", "support.function.builtin"). This object bridges
 * the two so a single Theme can colorize tokens produced by any engine.
 *
 * Mapping table sourced from:
 *   - research-report.md §1.5 (Tree-Sitter scope conventions)
 *   - syntax-highlighting spec §3 (scope mapping table)
 *   - VS Code TextMate scope reference
 *
 *########################################################*/
package digital.vasic.yole.syntax.grammar

/**
 * Maps Tree-Sitter / vscode-textmate scope names to VS Code TextMate
 * scope names used as keys in VS Code theme JSON `tokenColors[].scope`.
 *
 * Lookup is exact first, then hierarchical: if `keyword.control.return`
 * is not a key, try `keyword.control`, then `keyword`. If no mapping
 * matches at any level, the input is returned unchanged so callers can
 * still rely on [digital.vasic.yole.syntax.theme.Theme]'s own
 * hierarchical fallback inside `tokenColor()`.
 */
object ScopeMapper {
    private val map: Map<String, String> = mapOf(
        // Comments
        "comment" to "comment",
        "comment.line" to "comment.line.double-slash",
        "comment.block" to "comment.block",
        // Keywords
        "keyword" to "keyword",
        "keyword.control" to "keyword.control",
        "keyword.operator" to "keyword.operator",
        "operator" to "keyword.operator",
        // Storage
        "storage" to "storage",
        "storage.type" to "storage.type",
        "storage.modifier" to "storage.modifier",
        // Strings
        "string" to "string",
        "string.quoted" to "string.quoted",
        "string.escape" to "constant.character.escape",
        "escape" to "constant.character.escape",
        // Constants
        "constant" to "constant",
        "constant.builtin" to "constant.language",
        "constant.numeric" to "constant.numeric",
        "number" to "constant.numeric",
        "boolean" to "constant.language.boolean",
        // Functions
        "function" to "entity.name.function",
        "function.builtin" to "support.function.builtin",
        "function.macro" to "entity.name.function.macro",
        "method" to "entity.name.function",
        // Types
        "type" to "entity.name.type",
        "type.builtin" to "support.type.builtin",
        "class" to "entity.name.class",
        // Variables
        "variable" to "variable",
        "variable.builtin" to "variable.language",
        "variable.parameter" to "variable.parameter",
        "parameter" to "variable.parameter",
        // Punctuation
        "punctuation" to "punctuation",
        "punctuation.bracket" to "punctuation.section.bracket",
        "punctuation.delimiter" to "punctuation.separator",
        // Tag / markup
        "tag" to "entity.name.tag",
        "attribute" to "entity.other.attribute-name",
        // Markdown-specific
        "heading" to "markup.heading",
        "heading.1" to "markup.heading.1",
        "heading.2" to "markup.heading.2",
        "heading.3" to "markup.heading.3",
        "link" to "markup.underline.link",
        "emphasis" to "markup.italic",
        "strong" to "markup.bold",
    )

    /**
     * Translate a Tree-Sitter / vscode-textmate scope name to a VS Code
     * TextMate scope name. Returns the input unchanged if no mapping
     * exists at any hierarchy level (the consuming
     * [digital.vasic.yole.syntax.theme.Theme] still applies its own
     * hierarchical fallback during `tokenColor()` lookup).
     */
    fun treeSitterToVsCode(tsScope: String): String {
        // Exact match first.
        map[tsScope]?.let { return it }
        // Walk dot-segments shortest-first.
        var s = tsScope
        while (s.contains('.')) {
            s = s.substringBeforeLast('.')
            map[s]?.let { return it }
        }
        return tsScope
    }
}
