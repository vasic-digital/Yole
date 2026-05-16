/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-64 Phase 2: anti-bluff tests for CodeBlockDetector.
 *
 * Mutation stub: replace isMonospaceRun body with `return false`
 * → tests knownMonospaceFonts_areRecognised and
 *          caseInsensitiveMatch_works all FAIL.
 *#######################################################*/
package digital.vasic.yole.import_.conversion

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeBlockDetectorTests {

    @Test
    fun knownMonospaceFonts_areRecognised() {
        val monospaceNames = listOf(
            "Courier",
            "Courier New",
            "Consolas",
            "Menlo",
            "Monaco",
            "Fira Code",
            "Roboto Mono",
            "Source Code Pro",
            "JetBrains Mono",
            "Inconsolata",
            "DejaVu Sans Mono",
            "Andale Mono",
        )
        for (name in monospaceNames) {
            assertTrue(
                CodeBlockDetector.isMonospaceRun(name),
                "Expected '$name' to be classified as monospace",
            )
        }
    }

    @Test
    fun caseInsensitiveMatch_works() {
        // All-caps and mixed-case variants must still match
        assertTrue(CodeBlockDetector.isMonospaceRun("COURIER"), "All-caps COURIER must match")
        assertTrue(CodeBlockDetector.isMonospaceRun("consolas"), "Lower-case consolas must match")
        assertTrue(CodeBlockDetector.isMonospaceRun("JetBrains mono"), "Lowercase 'mono' suffix must match")
    }

    @Test
    fun proportionalFonts_areNotRecognised() {
        val proportionalNames = listOf("Arial", "Helvetica", "Times New Roman", "Georgia", "Calibri")
        for (name in proportionalNames) {
            assertFalse(
                CodeBlockDetector.isMonospaceRun(name),
                "Expected '$name' NOT to be classified as monospace",
            )
        }
    }
}
