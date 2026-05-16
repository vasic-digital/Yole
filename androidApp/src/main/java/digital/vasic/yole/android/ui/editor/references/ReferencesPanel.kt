/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-63 Phase 9: ReferencesPanel — persistent bottom drawer showing all
 * reference locations for a symbol. Mirrors iter-62 DiagnosticsProblemsPanel
 * in layout style but is used for find-references results.
 *
 * Behavior:
 *   - Persistent (non-dismissing) LazyColumn bottom drawer.
 *   - Each row: filename (last path segment) + "line:col" label +
 *     context-line-preview extracted from [sources] map.
 *   - Tapping a row calls onJump(uri, range.first); does NOT dismiss panel.
 *   - Empty list renders root container with no rows.
 *
 * testTag convention:
 *   root:    "references-panel"
 *   per row: "references-row-<0-based-index>"
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub onClick to no-op (remove onJump call from clickable).
 *      → clickRow_jumps FAILS (callback never fires).
 *   2. Remove testTag("references-row-$index").
 *      → rendersAllRefs FAILS (row nodes unreachable by tag lookup).
 *   3. Revert both → PASS.
 *
 * Persistent-vs-chooser distinction (panel_persistent_across_navigation):
 *   Unlike DefinitionLocationChooser (ModalBottomSheet — dismissed by swipe),
 *   this panel is a persistent scrollable list that stays visible. There is no
 *   onDismiss callback and no ModalBottomSheet wrapper.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: ships here (Compose Multiplatform, androidMain).
 *   - Desktop:  references panel deferred; tracked in CONTINUATION.md.
 *   - iOS:      N/A this phase.
 *   - Web:      N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.references

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import digital.vasic.yole.lsp.ReferenceLocation

/**
 * Persistent scrollable panel listing all [references] for a symbol.
 *
 * Each row shows the filename, a "line:col" hint, and a one-line preview
 * of the source context extracted from [sources]. Tapping a row calls
 * [onJump] with the reference URI and character offset; the panel itself
 * stays visible (persistent drawer — not a dismissing bottom sheet).
 *
 * @param references  Locations returned by the LSP find-references call.
 * @param sources     Map of URI → full document text; used to extract context
 *                    line previews. Missing URIs produce an empty preview.
 * @param onJump      Called with (uri, characterOffset) when a row is tapped.
 * @param modifier    Optional modifier for the root [LazyColumn].
 */
@Composable
fun ReferencesPanel(
    references: List<ReferenceLocation>,
    sources: Map<String, String>,
    onJump: (uri: String, offset: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag("references-panel"),
    ) {
        itemsIndexed(references) { index, ref ->
            val filename = ref.uri.trimEnd('/').substringAfterLast('/').ifBlank { ref.uri }
            val contextLine = remember(ref.uri, ref.range.first) {
                extractContextLine(sources[ref.uri].orEmpty(), ref.range.first)
            }
            val lineCol = remember(ref.uri, ref.range.first) {
                offsetToLineCol(sources[ref.uri].orEmpty(), ref.range.first)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onJump(ref.uri, ref.range.first) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("references-row-$index"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = filename,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = lineCol,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (contextLine.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = contextLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            if (index < references.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

/**
 * Extract the trimmed text of the line that contains [offset] from [text].
 * Returns an empty string if [text] is empty or [offset] is out of range.
 */
internal fun extractContextLine(text: String, offset: Int): String {
    if (text.isEmpty() || offset < 0 || offset > text.length) return ""
    val safeOffset = offset.coerceIn(0, text.length)
    val lineStart = text.lastIndexOf('\n', safeOffset - 1) + 1
    val lineEnd = text.indexOf('\n', safeOffset).let { if (it == -1) text.length else it }
    return text.substring(lineStart, lineEnd).trim()
}

/**
 * Convert a character [offset] within [text] to a "line:col" display string
 * (both 1-based). Returns "1:1" for empty text.
 */
internal fun offsetToLineCol(text: String, offset: Int): String {
    if (text.isEmpty() || offset <= 0) return "1:1"
    val safeOffset = offset.coerceIn(0, text.length)
    var line = 1
    var lastNewline = -1
    for (i in 0 until safeOffset) {
        if (text[i] == '\n') {
            line++
            lastNewline = i
        }
    }
    val col = safeOffset - lastNewline
    return "$line:$col"
}
