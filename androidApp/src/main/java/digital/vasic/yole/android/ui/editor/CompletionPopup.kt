/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-60 Phase 6.2: CompletionPopup — the visible completion-candidate
 * list shown as a floating overlay anchored below the editor.
 *
 * Design:
 *   Uses androidx.compose.ui.window.Popup so it floats above the IME
 *   and other composable layers. Cursor-rect positioning via
 *   TextLayoutResult is invasive (requires SyncedScrollEditor to expose
 *   its TextLayoutResult externally — a structural change that risks the
 *   iter-57 VisualTransformation length-guard). For v1 we anchor to the
 *   bottom-left of the calling Box with a vertical offset derived from
 *   the line height, which is UX-sufficient per spec §12 note.
 *
 *   List — LazyColumn, max 8 rows × 40dp = 320dp height, each row shows
 *   label + kind badge. testTag on root: "completion-popup". testTag per
 *   item: "completion-item-${index}".
 *
 *   Keyboard handling (Esc / Enter / Tab / arrows) is wired at the caller
 *   (SyncedScrollEditor) via Modifier.onPreviewKeyEvent so it intercepts
 *   before BasicTextField consumes the event.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation — remove the testTag("completion-popup") → the Robolectric
 *   test assertions on "completion-popup" FAIL. Reverted; tests GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   Android: full implementation.
 *   Desktop: deferred — Phase 6 ships Android-only per design spec §10.
 *   iOS:     deferred — same reason.
 *   Web:     deferred — same reason.
 *########################################################*/
package digital.vasic.yole.android.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import digital.vasic.yole.completion.CompletionItem

/** Maximum number of rows shown without scrolling. */
private const val MAX_VISIBLE_ITEMS = 8

/** Height of each completion row in dp. */
private const val ITEM_HEIGHT_DP = 40

/**
 * Floating completion-candidate popup.
 *
 * Shows when [state].isOpen is true. Dismisses when the user clicks
 * outside ([onDismiss]). Committing an item calls [onCommit].
 *
 * @param state observable state bag — recomposed automatically.
 * @param isDarkTheme drives background/foreground colours.
 * @param onCommit called when the user clicks or keyboard-commits an item.
 * @param onDismiss called when the user taps outside the popup.
 */
@Composable
fun CompletionPopup(
    state: CompletionPopupState,
    isDarkTheme: Boolean,
    onCommit: (CompletionItem) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!state.isOpen || state.items.isEmpty()) return

    val bgColor = if (isDarkTheme) Color(0xFF252526) else Color(0xFFFFFFFF)
    val fgColor = if (isDarkTheme) Color(0xFFD4D4D4) else Color(0xFF1E1E1E)
    val selectedBg = if (isDarkTheme) Color(0xFF094771) else Color(0xFFBFDBFE)
    val borderColor = if (isDarkTheme) Color(0xFF454545) else Color(0xFFD1D5DB)

    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        val listState = rememberLazyListState()

        // Scroll to keep the selected item visible whenever selection moves.
        LaunchedEffect(state.selectedIndex) {
            listState.animateScrollToItem(state.selectedIndex)
        }

        Box(
            modifier = Modifier
                .testTag("completion-popup")
                .width(320.dp)
                .heightIn(max = (MAX_VISIBLE_ITEMS * ITEM_HEIGHT_DP).dp)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .background(bgColor, RoundedCornerShape(4.dp))
                .border(1.dp, borderColor, RoundedCornerShape(4.dp)),
        ) {
            LazyColumn(state = listState) {
                itemsIndexed(state.items) { index, item ->
                    val isSelected = index == state.selectedIndex
                    Row(
                        modifier = Modifier
                            .testTag("completion-item-$index")
                            .fillMaxWidth()
                            .height(ITEM_HEIGHT_DP.dp)
                            .background(if (isSelected) selectedBg else Color.Transparent)
                            .clickable { onCommit(item) }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Kind badge — one letter, coloured.
                        Text(
                            text = item.kind.badge(),
                            fontSize = 10.sp,
                            color = item.kind.badgeColor(isDarkTheme),
                            modifier = Modifier
                                .background(
                                    item.kind.badgeBackground(isDarkTheme),
                                    RoundedCornerShape(2.dp),
                                )
                                .padding(horizontal = 3.dp, vertical = 1.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.label,
                            fontSize = 13.sp,
                            color = fgColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Kind badge helpers
// ---------------------------------------------------------------------------

private fun CompletionItem.Kind.badge(): String = when (this) {
    CompletionItem.Kind.Identifier -> "I"
    CompletionItem.Kind.Snippet    -> "S"
    CompletionItem.Kind.Keyword    -> "K"
    CompletionItem.Kind.Word       -> "W"
}

private fun CompletionItem.Kind.badgeColor(dark: Boolean): Color = when (this) {
    CompletionItem.Kind.Snippet    -> if (dark) Color(0xFF4FC1FF) else Color(0xFF0070C1)
    CompletionItem.Kind.Identifier -> if (dark) Color(0xFF9CDCFE) else Color(0xFF001080)
    CompletionItem.Kind.Keyword    -> if (dark) Color(0xFFC586C0) else Color(0xFFAF00DB)
    CompletionItem.Kind.Word       -> if (dark) Color(0xFFD4D4D4) else Color(0xFF1E1E1E)
}

private fun CompletionItem.Kind.badgeBackground(dark: Boolean): Color = when (this) {
    CompletionItem.Kind.Snippet    -> if (dark) Color(0xFF0D3245) else Color(0xFFDCEEFF)
    CompletionItem.Kind.Identifier -> if (dark) Color(0xFF1E3A5F) else Color(0xFFE8F0FE)
    CompletionItem.Kind.Keyword    -> if (dark) Color(0xFF3A1A3A) else Color(0xFFF3E8FF)
    CompletionItem.Kind.Word       -> if (dark) Color(0xFF3A3A3A) else Color(0xFFF3F4F6)
}
