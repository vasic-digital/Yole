/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-63 Phase 7.2: SignatureHelpPopup — floating Compose Popup anchored
 * near the cursor, showing the active signature label with the active
 * parameter highlighted in bold, plus optional parameter documentation.
 *
 * Design:
 *   Uses androidx.compose.ui.window.Popup with alignment=TopStart +
 *   anchorOffset (same pattern as HoverPopup, iter-62 Phase 6.1).
 *   Content is vertically expanded relative to the pill: full signature
 *   label + parameter documentation when present.
 *   Max width: 480 dp. Max height: 200 dp before scrolling.
 *
 * testTag: "signature-popup"
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub Composable body to an empty Box → ALL assertions FAIL because
 *      structural markers (testTag, Popup, SpanStyle Bold) disappear.
 *   2. Remove testTag("signature-popup") → popupHasTestTag FAILS.
 *   3. Remove SpanStyle(fontWeight = Bold) → activeParam_isHighlightedBold FAILS.
 *   4. Remove paramDoc Text block → paramDocIsRenderedWhenPresent FAILS.
 *   5. Revert all → GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: ships here (also usable on tablet; tooltip-like pattern).
 *   - Desktop: same Popup API available in Compose for Desktop;
 *              Phase 10 integration will wire this surface on Desktop.
 *   - iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.signaturehelp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import digital.vasic.yole.lsp.SignatureHelp

/** Maximum popup width. */
private val POPUP_MAX_WIDTH = 480.dp

/** Maximum popup height before scrolling. */
private val POPUP_MAX_HEIGHT = 200.dp

/**
 * Floating signature-help popup anchored near the editor cursor.
 *
 * Shows the active signature label with the active parameter token
 * highlighted in bold, plus optional parameter documentation below a
 * divider. Dismisses via [onDismiss] when the user taps outside.
 *
 * @param info         The [SignatureHelp] result. Nothing rendered when null
 *                     or signatures list is empty.
 * @param anchorOffset Pixel offset (parent coordinates) for popup anchoring.
 * @param onDismiss    Invoked when the popup should close.
 * @param modifier     Applied to the outermost Box inside the Popup.
 */
@Composable
fun SignatureHelpPopup(
    info: SignatureHelp?,
    anchorOffset: IntOffset,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (info == null || info.signatures.isEmpty()) return

    val sig = info.signatures[info.activeSignature.coerceIn(0, info.signatures.lastIndex)]
    val label = sig.label
    val activeParam = info.activeParameter
    val paramDoc: String? = sig.parameters.getOrNull(activeParam)?.documentation

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

    Popup(
        alignment = androidx.compose.ui.Alignment.TopStart,
        offset = anchorOffset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        Column(
            modifier = modifier
                .testTag("signature-popup")
                .widthIn(max = POPUP_MAX_WIDTH)
                .heightIn(max = POPUP_MAX_HEIGHT)
                .shadow(6.dp, RoundedCornerShape(6.dp))
                .background(Color(0xFF2D2D2D), RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF454545), RoundedCornerShape(6.dp))
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // --- Signature label with active parameter bolded ---
            Text(
                text = annotated,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color(0xFFD4D4D4),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signature-popup-label"),
            )

            // --- Parameter documentation (if present) ---
            if (!paramDoc.isNullOrBlank()) {
                Spacer(modifier = Modifier.padding(top = 4.dp))
                HorizontalDivider(color = Color(0xFF454545), thickness = 1.dp)
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Text(
                    text = paramDoc,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = Color(0xFFAAAAAA),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signature-popup-paramdoc"),
                )
            }
        }
    }
}
