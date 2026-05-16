/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-62 Phase 5.2: DiagnosticsGutter — severity dots in the editor
 * left gutter, one dot per line (highest severity wins per-line).
 *
 * Behavior:
 *   1. For each Diagnostic, offsetToLine(text, offset) converts
 *      range.first to a 0-based line number.
 *   2. Diagnostics on the same line are grouped; highest severity
 *      (Error > Warning > Information > Hint) wins.
 *   3. A colored 8dp dot is drawn at the vertical position corresponding
 *      to (lineNumber * lineHeight) via a fixed-height Column.
 *
 * testTag convention:
 *   root:        "diagnostics-gutter"
 *   per dot:     "diag-line-<0-based-lineNum>"
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   Stub to render nothing (empty Column) → the
 *   "twoDotsForThreeDiagnosticsOnTwoLines" structural test FAILS
 *   because it checks that the source emits both testTag strings.
 *   Revert → PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: ships here.
 *   - Desktop: diagnostics gutter deferred; tracked in CONTINUATION.md.
 *   - iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import digital.vasic.yole.lsp.Diagnostic
import digital.vasic.yole.lsp.Severity

/**
 * Converts a character [offset] in [text] to a 0-based line number by
 * counting newline characters before [offset].
 *
 * Clamps [offset] to [0, text.length]. Returns 0 for any offset in the
 * first line.
 */
internal fun offsetToLine(text: String, offset: Int): Int {
    val clamped = offset.coerceIn(0, text.length)
    var line = 0
    for (i in 0 until clamped) {
        if (text[i] == '\n') line++
    }
    return line
}

/**
 * Severity ordinal for priority comparison: lower ordinal = higher priority
 * in the enum declaration order (Error=0, Warning=1, Information=2, Hint=3).
 * We want Error to "win" so we compare by ordinal ascending.
 */
private fun Severity.priority(): Int = ordinal

/**
 * Renders colored dot indicators in the gutter for each line that has at
 * least one LSP diagnostic. When multiple diagnostics share a line, only
 * the highest-severity dot is shown (Error > Warning > Information > Hint).
 *
 * @param diagnostics        The list of diagnostics for the current document.
 * @param textForLineNumberMapping The document text used to map
 *                           [Diagnostic.range.first] to line numbers.
 * @param lineHeight         Height per text line (must match the editor's
 *                           line height so dots align with the text).
 * @param modifier           Optional modifier for the root Column.
 */
@Composable
fun DiagnosticsGutter(
    diagnostics: List<Diagnostic>,
    textForLineNumberMapping: String,
    lineHeight: Dp,
    modifier: Modifier = Modifier,
) {
    // Group diagnostics by line; pick the highest severity per line.
    val dotsByLine: Map<Int, Diagnostic> = buildMap {
        for (diag in diagnostics) {
            val line = offsetToLine(textForLineNumberMapping, diag.range.first)
            val existing = get(line)
            if (existing == null || diag.severity.priority() < existing.severity.priority()) {
                put(line, diag)
            }
        }
    }

    Column(
        modifier = modifier.testTag("diagnostics-gutter"),
    ) {
        val maxLine = dotsByLine.keys.maxOrNull() ?: -1
        for (lineNum in 0..maxLine) {
            val diag = dotsByLine[lineNum]
            Box(modifier = Modifier.height(lineHeight)) {
                if (diag != null) {
                    val visuals = severityVisuals(diag.severity, isDark = false)
                    Box(
                        modifier = Modifier
                            .testTag("diag-line-$lineNum")
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(visuals.color),
                    )
                }
            }
        }
        // Ensure empty-diagnostics case still emits the root tag.
        if (dotsByLine.isEmpty()) {
            Spacer(modifier = Modifier.size(0.dp))
        }
    }
}
