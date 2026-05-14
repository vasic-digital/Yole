/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 8: catalogue of all known Grammar values.
 *   v1 only ships Markdown (per spec §3.7 and Phase 8.1 plan).
 *   Feature 2 of the broader 5-feature plan adds Kotlin / Java /
 *   Python / Rust / etc. — new entries get appended to `all` and the
 *   matching Tree-Sitter grammars get registered in the
 *   TokenizerEngine implementations.
 *
 *########################################################*/
package digital.vasic.yole.syntax

/**
 * Static catalogue of all language grammars known to Yole at compile
 * time. [GrammarRegistry] queries this set when detecting the grammar
 * for a given filename.
 *
 * Adding a new grammar:
 *  1. Add a `val foo = Grammar(...)` entry below.
 *  2. Append `foo` to [all].
 *  3. Register the Tree-Sitter (or vscode-textmate) parser in the
 *     platform actual of [TokenizerEngine].
 *  4. Ensure the format id appears in
 *     `FormatRegistry.defaultEnabledFormatIds()` or is opt-in via
 *     Settings → Formats.
 */
object GrammarMetadata {
    val markdown: Grammar = Grammar(
        id = "markdown",
        displayName = "Markdown",
        extensions = listOf(".md", ".markdown", ".mdown", ".mkd"),
        mimeTypes = listOf("text/markdown", "text/x-markdown"),
    )

    /** All grammars known to this build of Yole. */
    val all: List<Grammar> = listOf(markdown)
}
