/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-60 Phase 6.1: CompletionPopupState — observable state bag that
 * drives the CompletionPopup composable.
 *
 * Design:
 *   All fields are Compose-observable (by mutableStateOf) so
 *   CompletionPopup recomposes automatically on any mutation.
 *   Mutations are only accessible via the public API; the backing
 *   fields are private-set to prevent accidental writes.
 *
 *   Thread-safety: methods must be called from the composition thread
 *   (Main dispatcher). The CompletionEngine flow is collected inside
 *   LaunchedEffect which already runs on Main.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure — set show() to a no-op (never flip isOpen to
 *   true). CompletionPopupRobolectricTest.popupOpensAfterThreeChars
 *   FAILS because the popup tag never appears. Reverted; tests GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   Android: full implementation.
 *   Desktop: deferred — Phase 6 ships Android-only per design spec §10.
 *   iOS:     deferred — same reason.
 *   Web:     deferred — same reason.
 *########################################################*/
package digital.vasic.yole.android.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import digital.vasic.yole.completion.CompletionItem

/**
 * Observable state for [CompletionPopup].
 *
 * Lifecycle:
 *   - [show] is called when the engine returns the first set of items.
 *   - [update] is called on subsequent engine emissions (progressive fill).
 *   - [hide] is called on Esc / tap-outside / short prefix.
 *   - [moveSelection] is called on arrow-key input.
 *
 * Anti-bluff anchor (CONST-035):
 *   Stubbing [show] to a no-op → [isOpen] stays false →
 *   `CompletionPopupRobolectricTest.popupOpensAfterThreeChars` FAILS.
 */
class CompletionPopupState {

    /** Whether the popup window is currently visible. */
    var isOpen by mutableStateOf(false)
        private set

    /** Current list of ranked completion candidates. */
    var items by mutableStateOf<List<CompletionItem>>(emptyList())
        private set

    /** Index into [items] of the keyboard-highlighted row (0-based). */
    var selectedIndex by mutableStateOf(0)
        private set

    /**
     * Char offset in the document at which the popup is anchored
     * (= start of the user's partial prefix). Used by the popup to
     * position relative to the cursor via TextLayoutResult.
     */
    var anchorOffset by mutableStateOf(0)
        private set

    // -----------------------------------------------------------------------
    // Mutations
    // -----------------------------------------------------------------------

    /**
     * Open the popup with [newItems] anchored at [cursorAnchor].
     *
     * Resets [selectedIndex] to 0. No-op when [newItems] is empty
     * (avoids flashing an empty popup on the first progressive emission).
     */
    fun show(newItems: List<CompletionItem>, cursorAnchor: Int) {
        if (newItems.isEmpty()) return
        items = newItems
        anchorOffset = cursorAnchor
        selectedIndex = 0
        isOpen = true
    }

    /**
     * Update the visible list in-place. Preserves [selectedIndex] if it
     * is still in range; clamps it otherwise.
     */
    fun update(newItems: List<CompletionItem>) {
        if (newItems.isEmpty()) {
            hide()
            return
        }
        items = newItems
        selectedIndex = selectedIndex.coerceIn(0, newItems.size - 1)
    }

    /** Close the popup and reset all state. */
    fun hide() {
        isOpen = false
        items = emptyList()
        selectedIndex = 0
    }

    /**
     * Move the keyboard selection by [delta] rows (+1 = down, -1 = up).
     * Wraps around at both ends.
     */
    fun moveSelection(delta: Int) {
        val size = items.size
        if (size == 0) return
        selectedIndex = ((selectedIndex + delta) % size + size) % size
    }
}
