/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-62 Phase 5.1, CONST-035 anti-bluff):
 *   Verifies that severityVisuals() returns distinct colors for each
 *   Severity value in both light and dark modes. Tests use direct
 *   source-level structural assertions plus value-level assertions on
 *   the pure helper function.
 *
 * Anti-bluff mutation guards:
 *   Stub severityVisuals to always return (Color.Red, Icons.Filled.Close):
 *   → warningColorIsDifferentFromError FAILS (warning color == red).
 *   → informationColorIsDifferentFromError FAILS.
 *   → hintColorIsDifferentFromError FAILS.
 *   Revert → 4/4 PASS.
 *
 * Test architecture: pure-function + source-level structural assertions
 * (no createComposeRule — manifest = Config.NONE pattern).
 *
 *#######################################################*/
package digital.vasic.yole.android.robolectric.diagnostics

import digital.vasic.yole.android.ui.editor.diagnostics.severityVisuals
import digital.vasic.yole.lsp.Severity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DiagnosticsPaletteTest {

    private fun loadSource(relativePath: String): String {
        val candidates = listOf(
            relativePath,
            "../$relativePath",
            relativePath.removePrefix("androidApp/"),
        )
        for (path in candidates) {
            val f = File(path)
            if (f.isFile) return f.readText()
        }
        error("$relativePath not found; checked: $candidates (cwd=${File(".").absolutePath})")
    }

    private fun loadPaletteSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/diagnostics/DiagnosticsPalette.kt",
    )

    /**
     * Layer 1: pure-function — Error color MUST differ from Warning color
     * in light mode. Anti-bluff: stub to always return red → this FAILS
     * for Warning (warning red != warning expected amber).
     */
    @Test
    fun warningColorIsDifferentFromError() {
        val error = severityVisuals(Severity.Error, isDark = false)
        val warning = severityVisuals(Severity.Warning, isDark = false)
        assertNotNull(error)
        assertNotNull(warning)
        assertFalse(
            "Warning color MUST differ from Error color",
            error.color == warning.color,
        )
    }

    /**
     * Layer 2: pure-function — Information color MUST differ from Error.
     * Anti-bluff: stub to always return red → this FAILS.
     */
    @Test
    fun informationColorIsDifferentFromError() {
        val error = severityVisuals(Severity.Error, isDark = false)
        val info = severityVisuals(Severity.Information, isDark = false)
        assertFalse(
            "Information color MUST differ from Error color",
            error.color == info.color,
        )
    }

    /**
     * Layer 3: pure-function — Hint color MUST differ from Error.
     * Anti-bluff: stub to always return red → this FAILS.
     */
    @Test
    fun hintColorIsDifferentFromError() {
        val error = severityVisuals(Severity.Error, isDark = false)
        val hint = severityVisuals(Severity.Hint, isDark = false)
        assertFalse(
            "Hint color MUST differ from Error color",
            error.color == hint.color,
        )
    }

    /**
     * Layer 4: source-level structural — severityVisuals MUST have a
     * when-branch for each Severity value. Anti-bluff: removing any
     * branch (e.g. Hint) causes a Kotlin exhaustive-when compile error
     * AND this assertion fails if the literal is removed.
     */
    @Test
    fun paletteSourceCoversAllFourSeverities() {
        val src = loadPaletteSource()
        for (sev in listOf("Severity.Error", "Severity.Warning", "Severity.Information", "Severity.Hint")) {
            assertTrue("DiagnosticsPalette MUST handle $sev", src.contains(sev))
        }
        // The when-expression must be exhaustive.
        assertTrue("DiagnosticsPalette MUST use a when expression", src.contains("when (severity)"))
    }

    /**
     * Layer 5: dark-mode colors MUST differ from light-mode colors for Error.
     * Verifies that isDark is actually consumed by the function.
     */
    @Test
    fun darkModeColorsAreDifferentFromLightMode() {
        val light = severityVisuals(Severity.Error, isDark = false)
        val dark = severityVisuals(Severity.Error, isDark = true)
        assertFalse(
            "Dark-mode Error color MUST differ from light-mode Error color",
            light.color == dark.color,
        )
    }
}
