/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-63 Phase 6.2: CodeActionMenu — DropdownMenu anchored to the
 * lightbulb tap point.
 *
 * One DropdownMenuItem is rendered per CodeAction. Tapping an item
 * invokes onSelected(action) and dismisses the menu via onDismissRequest.
 *
 * testTag convention:
 *   root menu:    "code-action-menu"
 *   per item:     "code-action-item-<0-based-index>"
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub Composable body to an empty Box (no items rendered).
 *   2. Re-run tests — menu_renders_all_actions FAILS because no
 *      "code-action-item-" tags appear in the source.
 *      menu_item_click_invokes_callback FAILS because no onClick wiring exists.
 *   3. Revert → all PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: ships here.
 *   - Desktop: code-action menu deferred; tracked in CONTINUATION.md.
 *   - iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.codeaction

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import digital.vasic.yole.lsp.CodeAction

/**
 * A [DropdownMenu] that lists all available [CodeAction]s for a line.
 *
 * @param actions            The list of code actions to display.  Must be
 *                           non-empty when [expanded] is true (caller ensures this).
 * @param expanded           Whether the menu is currently visible.
 * @param onDismissRequest   Invoked when the user taps outside the menu or
 *                           presses Back — caller sets [expanded] to false.
 * @param onSelected         Invoked with the chosen [CodeAction]; the caller
 *                           should then close the menu and invoke the action.
 */
@Composable
fun CodeActionMenu(
    actions: List<CodeAction>,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSelected: (CodeAction) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("code-action-menu"),
    ) {
        actions.forEachIndexed { index, action ->
            DropdownMenuItem(
                text = { Text(text = action.title) },
                onClick = {
                    onSelected(action)
                    onDismissRequest()
                },
                modifier = Modifier.testTag("code-action-item-$index"),
            )
        }
    }
}
