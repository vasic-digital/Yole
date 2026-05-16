/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 3: LspRangeMapping — pure LSP line/col → offset helper.
 *
 * Converts LSP (line, character) pairs to absolute byte offsets in a
 * plain-text string. Pure function, no platform dependencies.
 *
 * Used by:
 *   - Desktop JVM actual: publishDiagnostics + hover range wiring.
 *   - Android JVM actual: same.
 *   - Definition target ranges: intentionally NOT wired here (Phase 7
 *     openFileAt resolves the target URI's text on demand; see KDoc below).
 *
 * Cross-platform impact (CONST-037):
 *   - commonMain: tested in commonTest (no WASM coroutines-test dep needed).
 *   - Android: consumed via androidMain actual; same logic, JVM path.
 *   - Desktop: consumed via desktopMain actual; same logic, JVM path.
 *   - iOS/Wasm: this file compiles on all targets but is called only from
 *               JVM actuals; no-op on iOS/Wasm because hover() returns null.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * Maps LSP (line, character) positions to absolute character offsets in
 * a document text string.
 *
 * LSP positions are 0-indexed (line 0 = first line, character 0 = first
 * column). This helper iterates the text character-by-character, counting
 * newline characters to locate the target line, then adds the column offset.
 *
 * **Definition target caveats:** For `go-to-definition` responses, the
 * target file may differ from the currently cached document. This helper
 * is intentionally NOT called for definition [DefinitionLocation.range]
 * because the target URI's text is not available until Phase 7's
 * `openFileAt` loads it. Definition ranges remain `0..0` as documented
 * placeholders; Phase 7 resolves them post-file-open.
 */
object LspRangeMapping {

    /**
     * Returns the 0-based absolute character offset for the given LSP
     * [line]/[col] position inside [text].
     *
     * - If [line] exceeds the number of lines in [text], returns [text].length.
     * - If [col] would go past the end of the target line (or past [text].length),
     *   the result is clamped to [text].length.
     *
     * @param text Document text (any line endings; this counts `\n` only,
     *             consistent with LSP spec §3.17 which normalises to LF).
     * @param line 0-based LSP line index.
     * @param col  0-based LSP character offset within [line].
     * @return     Absolute character offset in [text], clamped to [0, text.length].
     */
    fun lineColToOffset(text: String, line: Int, col: Int): Int {
        var currentLine = 0
        var lineStart = 0
        for (i in text.indices) {
            if (currentLine == line) {
                return (lineStart + col).coerceAtMost(text.length)
            }
            if (text[i] == '\n') {
                currentLine++
                lineStart = i + 1
            }
        }
        // Either the text is empty, or line == currentLine (last line, no trailing \n).
        return if (currentLine == line) {
            (lineStart + col).coerceAtMost(text.length)
        } else {
            text.length
        }
    }
}
