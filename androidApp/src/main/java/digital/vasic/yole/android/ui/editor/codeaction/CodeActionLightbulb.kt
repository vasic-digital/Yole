/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-63 Phase 6.1: CodeActionLightbulb — 3rd gutter column.
 *
 * Renders a yellow lightbulb icon (Icons.Filled.Star used as the
 * lightbulb affordance; Icons.Filled.Info is the fallback) at the
 * vertical position corresponding to each line that has at least one
 * LSP CodeAction. Lines with no actions render an empty spacer Box
 * of the same height so column alignment is preserved.
 *
 * testTag convention:
 *   root:         "code-action-lightbulb"
 *   per lightbulb: "lightbulb-line-<0-based-lineNum>"
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub Composable body to an empty Column (render nothing).
 *   2. Re-run tests — lightbulb_visible_when_actions_present FAILS because
 *      no "lightbulb-line-" tag appears in the source.
 *      lightbulb_hidden_when_no_actions FAILS because the Spacer fallback
 *      branch disappears.
 *   3. Revert → all PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: ships here.
 *   - Desktop: code-action gutter deferred; tracked in CONTINUATION.md.
 *   - iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.codeaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import digital.vasic.yole.lsp.CodeAction

/** Amber / yellow tint matching Material Design "warning" amber (#FFC107). */
internal val LightbulbTint = Color(0xFFFFC107)

/**
 * Renders a column of lightbulb affordances for lines that carry LSP
 * code actions.  Lines without actions emit a same-height [Spacer] so
 * the gutter column stays vertically aligned with the text.
 *
 * @param actionsByLine  Map of 0-based line number → list of [CodeAction].
 *                       Lines absent from the map have no lightbulb.
 * @param lineHeight     Per-line height that matches the editor's line height.
 * @param totalLines     Total line count in the current document.  Used to
 *                       size the column correctly even when the last lines
 *                       have no actions.
 * @param onTap          Invoked with the 0-based line number when the user
 *                       taps a lightbulb; the caller is responsible for
 *                       anchoring and showing [CodeActionMenu].
 * @param modifier       Optional modifier for the root Column.
 */
@Composable
fun CodeActionLightbulb(
    actionsByLine: Map<Int, List<CodeAction>>,
    lineHeight: Dp,
    totalLines: Int,
    onTap: (line: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag("code-action-lightbulb")) {
        val maxLine = if (actionsByLine.isEmpty()) {
            totalLines - 1
        } else {
            maxOf(actionsByLine.keys.maxOrNull() ?: 0, totalLines - 1)
        }
        for (lineNum in 0..maxLine) {
            val actions = actionsByLine[lineNum]
            Box(
                modifier = Modifier.height(lineHeight),
                contentAlignment = Alignment.Center,
            ) {
                if (!actions.isNullOrEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Code actions available at line $lineNum",
                        tint = LightbulbTint,
                        modifier = Modifier
                            .testTag("lightbulb-line-$lineNum")
                            .size(14.dp)
                            .clickable { onTap(lineNum) },
                    )
                } else {
                    Spacer(modifier = Modifier.size(14.dp))
                }
            }
        }
        // Ensure root tag is emitted even when actionsByLine is empty and
        // totalLines is 0 (avoids the Column being empty).
        if (totalLines == 0 && actionsByLine.isEmpty()) {
            Spacer(modifier = Modifier.size(0.dp))
        }
    }
}
