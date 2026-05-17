/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-75 (#iter-63-desktop-signature-help-popup-deferred):
 * Pure-logic helper: resolve the character range of the active parameter
 * within a signature label string.
 *
 * Previously defined in Android's SignatureHelpPill.kt (internal).
 * Extracted to commonMain so both Android and Desktop signature-help
 * surfaces share the same implementation.
 *
 * Cross-platform impact (CONST-037):
 *   - Common: pure Kotlin, no platform APIs. Available on all targets.
 *   - Android: SignatureHelpPill.kt and SignatureHelpPopup.kt now delegate
 *              to this (old internal copies remain with a call to this).
 *   - Desktop: DesktopSignatureHelpPopup.kt uses this directly.
 *   - iOS/Web: not called (no signature surface yet) but available.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Returns the character [IntRange] (start-inclusive, end-exclusive) of the
 * [activeParam]-th comma-delimited token inside the outermost parentheses of
 * [label], or null if the label does not contain parentheses or the index is
 * out of range.
 *
 * Used by SignatureHelpPill (Android), SignatureHelpPopup (Android/Desktop)
 * to locate the active parameter token for bold highlighting.
 *
 * Mutation procedure (CONST-035 — verified in SignatureHelpSpanResolverTest):
 *   Return null always → multiParam test FAILS (span expected, got null).
 *   Remove splitParams nesting guard → Map<K,V> param test FAILS (wrong span).
 */
fun resolveActiveParamSpan(label: String, activeParam: Int): IntRange? {
    val openParen = label.indexOf('(')
    val closeParen = label.lastIndexOf(')')
    if (openParen < 0 || closeParen <= openParen) return null

    val inner = label.substring(openParen + 1, closeParen)
    val params = splitSignatureParams(inner)
    if (activeParam < 0 || activeParam >= params.size) return null

    // Walk the params list to compute absolute positions in [label].
    var cursor = openParen + 1
    for (i in 0 until activeParam) {
        cursor += params[i].length + 1 // +1 for the comma separator
    }
    // Skip any leading whitespace inserted by comma separation.
    val rawToken = params[activeParam]
    val leadingSpaces = rawToken.length - rawToken.trimStart().length
    val tokenStart = cursor + leadingSpaces
    // Return an inclusive..exclusive range compatible with String.substring(start, end).
    // The caller uses substring(span.first, span.last) so span.last must be exclusive.
    val tokenEnd = cursor + rawToken.length
    return tokenStart..tokenEnd
}

/**
 * Splits [inner] (text between the outermost parens) on commas, respecting
 * nested angle-brackets and parentheses so generic types like `Map<K, V>`
 * are not split mid-type.
 *
 * Mutation guard: removing the depth guard causes generic params to be
 * split erroneously → [resolveActiveParamSpan] returns the wrong span.
 */
internal fun splitSignatureParams(inner: String): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    val current = StringBuilder()
    for (ch in inner) {
        when {
            ch == ',' && depth == 0 -> {
                result += current.toString()
                current.clear()
            }
            ch == '(' || ch == '<' -> {
                depth++
                current.append(ch)
            }
            ch == ')' || ch == '>' -> {
                depth--
                current.append(ch)
            }
            else -> current.append(ch)
        }
    }
    result += current.toString()
    return result
}
