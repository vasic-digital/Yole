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
import digital.vasic.yole.lsp.resolveActiveParamSpan

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

// resolveActiveParamSpan is now in commonMain:
// digital.vasic.yole.lsp.resolveActiveParamSpan (SignatureHelpSpanResolver.kt)
