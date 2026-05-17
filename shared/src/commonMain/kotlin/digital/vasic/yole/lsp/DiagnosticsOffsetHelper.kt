/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-75 (#iter-62-desktop-editor-lsp-wiring):
 * Pure-logic helper: character-offset → 0-based line number conversion.
 *
 * Previously each platform (Android: DiagnosticsGutter.kt, Desktop:
 * DesktopLspSurfaces.kt) had a local copy. Extracted to commonMain so
 * both consume the same implementation and tests run in commonTest.
 *
 * Cross-platform impact (CONST-037):
 *   - Common: pure Kotlin, no platform APIs. Available on all targets.
 *   - Android: DiagnosticsGutter.kt delegates to this (old local copy removed).
 *   - Desktop: DesktopLspSurfaces.kt delegates to this.
 *   - iOS/Web: not called (no diagnostics surface yet) but available.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Convert a character [offset] in [text] to a 0-based line number by
 * counting newline characters before [offset].
 *
 * Clamps [offset] to [0, text.length]. Returns 0 for any offset in the
 * first line or when [text] is empty.
 *
 * Used by DiagnosticsGutter (Android) and DesktopDiagnosticsGutter (Desktop)
 * to translate [Diagnostic.range.first] into a screen row number.
 *
 * Mutation procedure (CONST-035 — verified in DiagnosticsOffsetHelperTest):
 *   Return 0 always → multiLine test FAILS (line 2 expected, got 0).
 *   Remove coerceIn → beyondEnd test throws or returns wrong value → FAIL.
 */
fun diagnosticOffsetToLine(text: String, offset: Int): Int {
    val clamped = offset.coerceIn(0, text.length)
    var line = 0
    for (i in 0 until clamped) {
        if (text[i] == '\n') line++
    }
    return line
}
