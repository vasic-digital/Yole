/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-75 (#iter-63-desktop-signature-help-popup-deferred):
 * Desktop signature-help popup — floating Compose Popup anchored near the
 * cursor showing the active signature label with active parameter bolded,
 * plus optional parameter documentation.
 *
 * Design:
 *   Mirrors the Android SignatureHelpPopup (Phase 7.2) but uses the
 *   Compose for Desktop Popup window.  The pure span-resolution logic lives
 *   in commonMain (SignatureHelpSpanResolver.kt) and is shared with Android.
 *
 *   Active-param highlighting uses SpanStyle(fontWeight = FontWeight.Bold)
 *   on the token located by resolveActiveParamSpan().
 *
 *   Max width: 480 dp. Max height: 200 dp before scrolling.
 *
 * testTag: "signature-popup"  (same tags as Android surface for parity)
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub Composable body to an empty Box → ALL assertions in
 *      DesktopSignatureHelpPopupLogicTest FAIL (structural markers vanish).
 *   2. Stub resolveActiveParamSpan to always return null →
 *      activeParam_isHighlighted test FAILS (span never applied).
 *   3. Remove testTag("signature-popup") →
 *      popupHasTestTag FAILS.
 *   4. Remove the paramDoc Text block →
 *      paramDocRenderedWhenPresent FAILS.
 *   5. Revert all → GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop: this file — full implementation.
 *   - Android: SignatureHelpPopup.kt (Phase 7.2) — identical parity.
 *   - iOS/Web:  N/A this phase; helper available in commonMain for future use.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.desktop.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import digital.vasic.yole.lsp.resolveActiveParamSpan

/** Maximum popup width. */
private val DESKTOP_SIG_POPUP_MAX_WIDTH = 480.dp

/** Maximum popup height before scrolling. */
private val DESKTOP_SIG_POPUP_MAX_HEIGHT = 200.dp

/**
 * Floating signature-help popup for the Desktop editor, anchored near the
 * cursor via [anchorOffset].
 *
 * Shows the active signature label with the active parameter token highlighted
 * in bold, plus optional parameter documentation below a divider. Dismisses
 * via [onDismiss].
 *
 * @param info         The [SignatureHelp] result. Nothing rendered when null
 *                     or signatures list is empty.
 * @param anchorOffset Pixel offset (parent coordinates) for popup anchoring.
 * @param onDismiss    Invoked when the popup should close.
 * @param modifier     Applied to the content Column inside the Popup.
 */
@Composable
fun DesktopSignatureHelpPopup(
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
        alignment = Alignment.TopStart,
        offset = anchorOffset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        Column(
            modifier = modifier
                .testTag("signature-popup")
                .widthIn(max = DESKTOP_SIG_POPUP_MAX_WIDTH)
                .heightIn(max = DESKTOP_SIG_POPUP_MAX_HEIGHT)
                .shadow(6.dp, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                .background(Color(0xFF2D2D2D), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF454545), androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
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
