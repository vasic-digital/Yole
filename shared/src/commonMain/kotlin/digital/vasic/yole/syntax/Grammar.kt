/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 8: language-grammar metadata used by GrammarRegistry to
 * detect a document's grammar from its filename / MIME type. One Grammar
 * per supported language. v1 only bundles Markdown; additional grammars
 * land in Feature 2 of the broader 5-feature plan.
 *
 *########################################################*/
package digital.vasic.yole.syntax

/**
 * Metadata describing a single language grammar.
 *
 * @property id stable identifier used by [TokenizerEngine.loadGrammar] /
 *   [EnabledFormatGate.isEnabled] — e.g., `"markdown"`, `"kotlin"`.
 * @property displayName human-readable name shown in UI (Settings,
 *   format badges) — e.g., `"Markdown"`.
 * @property extensions lowercase filename suffixes that map to this
 *   grammar — e.g., `listOf(".md", ".markdown")`. Match is
 *   case-insensitive via [GrammarRegistry.detectByFilename].
 * @property mimeTypes IANA / common MIME types associated with this
 *   grammar — e.g., `listOf("text/markdown")`.
 */
data class Grammar(
    val id: String,
    val displayName: String,
    val extensions: List<String>,
    val mimeTypes: List<String>,
)
