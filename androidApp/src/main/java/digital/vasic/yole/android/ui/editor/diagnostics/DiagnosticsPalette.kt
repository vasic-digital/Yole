/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-62 Phase 5.1: DiagnosticsPalette — severity → (color, icon) mapping.
 *
 * Pure helper; no @Composable annotation needed. Called from all 3
 * render surfaces (DiagnosticsGutter, DiagnosticsInlineUnderline,
 * DiagnosticsProblemsPanel).
 *
 * Color choices mirror VS Code default diagnostics palette:
 *   Error:       red   (light #D32F2F, dark #EF5350)
 *   Warning:     amber (light #F57C00, dark #FFA726)
 *   Information: blue  (light #1976D2, dark #42A5F5)
 *   Hint:        gray  (light #757575, dark #BDBDBD)
 *
 * Icon choices are limited to the standard material-icons subset
 * already present in the classpath (no material-icons-extended dep):
 *   Error:       Icons.Filled.Close   (✕ mark)
 *   Warning:     Icons.Filled.Warning (triangle !)
 *   Information: Icons.Filled.Info    (i circle)
 *   Hint:        Icons.Filled.Info    (same; no Lightbulb in core set)
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   Stub severityVisuals to always return (Color.Red, Icons.Filled.Close).
 *   → Warning/Information/Hint color tests FAIL (3 of 4 cases fail
 *     because Warning, Information, Hint each expect non-red colors).
 *   Revert → 4/4 PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: this file ships here.
 *   - Desktop: diagnostics rendering deferred; Desktop editor wiring
 *     tracked in docs/CONTINUATION.md.
 *   - iOS: N/A — Android-only UI in this phase.
 *   - Web: N/A — same.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.ui.editor.diagnostics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import digital.vasic.yole.lsp.Severity

/**
 * Holds the resolved color and icon for a given diagnostic severity.
 *
 * @param color Severity-appropriate foreground color for the current theme.
 * @param icon  Severity-appropriate Material icon.
 */
data class SeverityVisuals(
    val color: Color,
    val icon: ImageVector,
)

/**
 * Returns the [SeverityVisuals] (color + icon) appropriate for [severity]
 * in the current theme mode ([isDark]).
 *
 * All colors are hardcoded VS Code–inspired values rather than being
 * sourced from MaterialTheme because this helper is also consumed from
 * non-composable callsites (e.g. DiagnosticsInlineUnderline.filter()).
 *
 * Anti-bluff: stubbing to always return `(Color.Red, Icons.Filled.Close)`
 * causes the Warning, Information, and Hint test cases to FAIL because
 * each expects a different color.
 */
fun severityVisuals(severity: Severity, isDark: Boolean): SeverityVisuals = when (severity) {
    Severity.Error -> SeverityVisuals(
        color = if (isDark) Color(0xFFEF5350) else Color(0xFFD32F2F),
        icon = Icons.Filled.Close,
    )
    Severity.Warning -> SeverityVisuals(
        color = if (isDark) Color(0xFFFFA726) else Color(0xFFF57C00),
        icon = Icons.Filled.Warning,
    )
    Severity.Information -> SeverityVisuals(
        color = if (isDark) Color(0xFF42A5F5) else Color(0xFF1976D2),
        icon = Icons.Filled.Info,
    )
    Severity.Hint -> SeverityVisuals(
        color = if (isDark) Color(0xFFBDBDBD) else Color(0xFF757575),
        icon = Icons.Filled.Info,
    )
}
