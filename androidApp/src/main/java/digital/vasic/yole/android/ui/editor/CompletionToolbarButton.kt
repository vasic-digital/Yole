/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-60 Phase 6.3: CompletionToolbarButton — mobile toolbar button
 * that fires an explicit completion trigger (Ctrl+Space equivalent on
 * touchscreen devices).
 *
 * testTag: "completion-suggest-button" — matches
 * MobileSuggestButtonRobolectricTest.
 *
 * Anti-bluff anchor (CONST-035):
 *   Removing testTag("completion-suggest-button") → the Robolectric test
 *   cannot find the node → FAILS. Reverted; tests GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   Android: full implementation.
 *   Desktop: deferred — desktop has Ctrl+Space; touchscreen button
 *     not needed. Tracked in docs/CONTINUATION.md.
 *   iOS:     deferred — same reason as Desktop.
 *   Web:     deferred — same reason.
 *########################################################*/
package digital.vasic.yole.android.ui.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Toolbar [IconButton] that fires an explicit completion request when tapped.
 *
 * Intended to be placed in [IdeEditorTopBar] / the IDE toolbar Row so
 * that touchscreen users can invoke the popup without typing Ctrl+Space.
 *
 * @param onTrigger callback called on tap — should call
 *   [CompletionTrigger.onExplicitTrigger].
 * @param modifier optional extra modifiers.
 *
 * Anti-bluff anchor (CONST-035):
 *   testTag("completion-suggest-button") MUST be present.
 *   Removing it causes MobileSuggestButtonRobolectricTest to FAIL.
 */
@Composable
fun CompletionToolbarButton(
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onTrigger,
        modifier = modifier
            .testTag("completion-suggest-button")
            .semantics { contentDescription = "Suggest completions" },
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Suggest",
        )
    }
}
