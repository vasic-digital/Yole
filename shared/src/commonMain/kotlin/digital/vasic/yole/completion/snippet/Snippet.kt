/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 2: Snippet data class.
 *
 * A parsed VS Code snippet entry. The VS Code snippet bundle schema is
 * documented authoritatively in
 * docs/features/auto-complete/research-report.md §2 and at
 * https://code.visualstudio.com/api/language-extensions/snippet-guide.
 *#######################################################*/
package digital.vasic.yole.completion.snippet

/**
 * A single snippet entry parsed from a VS Code snippets.json bundle.
 *
 * @property prefix what the user types to trigger this snippet.
 * @property body the inserted text, possibly with `${N:placeholder}` markers.
 * @property description shown in the popup tooltip.
 * @property scope optional VS Code scope filter; empty = any scope.
 */
data class Snippet(
    val prefix: String,
    val body: String,
    val description: String? = null,
    val scope: String = "",
)
