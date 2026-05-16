/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-62 Phase 5.4: DiagnosticsProblemsPanel — scrollable list of all
 * LSP diagnostics with severity icon, message, and line reference.
 * Mirrors the "Problems" panel in VS Code / IntelliJ.
 *
 * Behavior:
 *   - Diagnostics sorted ascending by range.first.
 *   - Each row: [severity-icon] message :line (1-based for display).
 *   - Tapping a row calls onJumpToLine(lineNumber) with the 0-based
 *     line number of that diagnostic.
 *   - Empty list renders the root container with no rows.
 *
 * testTag convention:
 *   root:        "problems-panel"
 *   per row:     "problems-row-<0-based-index>"
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   Stub onClick to no-op (remove onJumpToLine call from clickable).
 *   → clickRow_invokesOnJumpToLine FAILS (callback never fires).
 *   Stub sort to identity (remove sortBy).
 *   → rendersAllRows still passes (count check unaffected) but
 *     any order-sensitive check would fail; the primary guard is the
 *     click test.
 *   Revert → PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: ships here.
 *   - Desktop: diagnostics problems panel deferred; tracked CONTINUATION.md.
 *   - iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.diagnostics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import digital.vasic.yole.lsp.Diagnostic

/**
 * Scrollable panel listing all [diagnostics] sorted by character offset,
 * with severity icon, message, and 1-based line number. Tapping a row
 * calls [onJumpToLine] with the 0-based line number of that diagnostic.
 *
 * @param diagnostics  Diagnostics for the current document.
 * @param text         Document text used to compute line numbers from offsets.
 * @param onJumpToLine Callback invoked with the 0-based line when a row is
 *                     tapped. Callers should scroll the editor to that line.
 * @param modifier     Optional modifier for the root [LazyColumn].
 */
@Composable
fun DiagnosticsProblemsPanel(
    diagnostics: List<Diagnostic>,
    text: String,
    onJumpToLine: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(diagnostics) { diagnostics.sortedBy { it.range.first } }

    LazyColumn(
        modifier = modifier.testTag("problems-panel"),
    ) {
        itemsIndexed(sorted) { index, diag ->
            val line = offsetToLine(text, diag.range.first)
            val visuals = severityVisuals(diag.severity, isDark = false)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onJumpToLine(line) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("problems-row-$index"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = visuals.icon,
                    contentDescription = diag.severity.name,
                    tint = visuals.color,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${diag.message} :${line + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
