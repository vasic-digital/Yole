/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-63 Phase 7.1: SignatureHelpPill — compact Material3 Surface chip
 * displayed above the cursor line on mobile, showing the active signature
 * label with the active parameter highlighted in bold.
 *
 * Design:
 *   A small rounded Surface (chip shape) containing a single Text with
 *   buildAnnotatedString that bolds the active parameter span. The
 *   active parameter span is located by splitting on commas inside the
 *   outermost parentheses of the signature label.
 *
 *   Auto-dismiss contract (Phase 10 wiring):
 *     - Caller passes onDismiss; the pill itself does not inspect keystrokes.
 *     - SignatureHelpTrigger calls onResult(null) on ')' → caller passes
 *       null help → early return guards render nothing.
 *
 * testTag: "signature-pill"
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub Composable body to an empty Box (no Surface, no Text).
 *   2. Re-run SignatureHelpPillRobolectricTest — ALL assertions fail
 *      because structural markers disappear from the source.
 *   3. Remove SpanStyle(fontWeight = Bold) from the annotated string →
 *      activeParam_isHighlightedBold FAILS (no Bold reference).
 *   4. Remove testTag("signature-pill") →
 *      pillHasTestTag FAILS.
 *   5. Revert all → GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: full implementation here (mobile pill pattern).
 *   - Desktop: SignatureHelpPopup (Phase 7.2) covers the desktop surface.
 *   - iOS/Web: N/A this phase; deferred to Phase 10 integration.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.signaturehelp

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import digital.vasic.yole.lsp.SignatureHelp

/**
 * Compact signature-help chip displayed inline above the editor cursor.
 *
 * Renders [info.signatures[info.activeSignature].label] with the active
 * parameter token highlighted via [SpanStyle(fontWeight = FontWeight.Bold)].
 *
 * @param info      The [SignatureHelp] result from the LSP server. When null
 *                  or the signatures list is empty, nothing is rendered.
 * @param modifier  Optional outer modifier.
 */
@Composable
fun SignatureHelpPill(
    info: SignatureHelp?,
    modifier: Modifier = Modifier,
) {
    if (info == null || info.signatures.isEmpty()) return

    val sig = info.signatures[info.activeSignature.coerceIn(0, info.signatures.lastIndex)]
    val label = sig.label
    val activeParam = info.activeParameter

    val annotated = buildAnnotatedString {
        val paramSpan = resolveActiveParamSpan(label, activeParam)
        if (paramSpan != null) {
            append(label.substring(0, paramSpan.first))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(label.substring(paramSpan.first, paramSpan.last))
            }
            append(label.substring(paramSpan.last))
        } else {
            append(label)
        }
    }

    Surface(
        modifier = modifier
            .testTag("signature-pill"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Text(
            text = annotated,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Active-parameter span resolution
// ---------------------------------------------------------------------------

/**
 * Returns the character [IntRange] (start-inclusive, end-exclusive) of the
 * [activeParam]-th comma-delimited token inside the outermost parentheses of
 * [label], or null if the label does not contain parentheses or the index is
 * out of range.
 *
 * Mutation guard: removing this function causes the annotated string to render
 * all text un-bolded → [activeParam_isHighlightedBold] FAILS.
 */
internal fun resolveActiveParamSpan(label: String, activeParam: Int): IntRange? {
    val openParen = label.indexOf('(')
    val closeParen = label.lastIndexOf(')')
    if (openParen < 0 || closeParen <= openParen) return null

    val inner = label.substring(openParen + 1, closeParen)
    val params = splitParams(inner)
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
 */
private fun splitParams(inner: String): List<String> {
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
