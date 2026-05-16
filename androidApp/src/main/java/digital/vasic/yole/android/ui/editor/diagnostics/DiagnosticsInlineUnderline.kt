/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-62 Phase 5.3: DiagnosticsInlineUnderline — VisualTransformation
 * that overlays straight colored underlines on diagnostic ranges inside
 * BasicTextField.
 *
 * Compose 1.7 has NO wavy underline support. The fallback is
 * TextDecoration.Underline + severity color from DiagnosticsPalette.
 * The severity color is the primary visual signal.
 *
 * Behavior:
 *   - Empty diagnostics list → identity transform (original text unchanged).
 *   - For each Diagnostic, applies SpanStyle(textDecoration=Underline,
 *     color=severityColor) over [range.first, range.last + 1).
 *   - Range is clamped to [0, text.length] to avoid IndexOutOfBounds.
 *   - OffsetMapping is always Identity (displayed chars == source chars,
 *     because underlines do not insert/remove characters).
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   Stub filter() to always return TransformedText(text, OffsetMapping.Identity)
 *   (skip the span application loop).
 *   → singleDiagnostic_appliesUnderlineSpan FAILS (span count stays 0).
 *   → multipleDiagnostics_layerSpans FAILS (span count stays 0).
 *   Revert → 4/4 PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: ships here (androidx.compose.ui.text.input API).
 *   - Desktop: diagnostics inline underline deferred (tracked CONTINUATION.md).
 *   - iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.diagnostics

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import digital.vasic.yole.lsp.Diagnostic

/**
 * A [VisualTransformation] that applies a straight colored underline for
 * each [Diagnostic] in [diagnostics]. The color is determined by
 * [severityVisuals] using [isDark] to select light/dark palette values.
 *
 * Passes through [OffsetMapping.Identity] because no characters are
 * added or removed — only styling is applied.
 *
 * @param diagnostics The diagnostics to render as inline underlines.
 * @param isDark      Whether the editor is in dark mode (selects palette colors).
 */
class DiagnosticsInlineUnderline(
    private val diagnostics: List<Diagnostic>,
    private val isDark: Boolean,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (diagnostics.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val builder = AnnotatedString.Builder(text)
        for (diag in diagnostics) {
            val (color, _) = severityVisuals(diag.severity, isDark)
            val start = diag.range.first.coerceIn(0, text.length)
            val end = (diag.range.last + 1).coerceIn(start, text.length)
            if (start < end) {
                builder.addStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline, color = color),
                    start,
                    end,
                )
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
