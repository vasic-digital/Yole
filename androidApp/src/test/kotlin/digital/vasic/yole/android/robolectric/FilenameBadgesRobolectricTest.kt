/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 11: anti-bluff structural test for the FILES tab
 * filename-badge wiring. The mutation-verifiable contract is enforced
 * primarily in commonTest BadgeTinterTest (mutation-verified there).
 * This Robolectric test guards the Compose call-site so a future
 * refactor of FileBrowserScreen can't accidentally drop the chip
 * rendering without test failure.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.render.BadgeTinter
import digital.vasic.yole.syntax.theme.Theme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FilenameBadgesRobolectricTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    private val theme = Theme(
        name = "T", type = "dark",
        uiColors = mapOf(
            "badge.background" to 0xFF888888.toInt(),
            "badge.background.markdown" to 0xFF00AA00.toInt(),
        ),
        tokenColors = emptyMap(),
    )

    @Before
    fun resetGate() = runBlocking<Unit> {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    @Test
    fun badgeWiringCompilesAndAppActivates() {
        // Structural guard: launching MainActivity through the existing
        // ThemeProvider + Compose graph must not crash with the badge
        // wiring in FileBrowserScreen. Full UI nav to the FILES tab + a
        // fixture markdown file is brittle in Robolectric (storage perms,
        // SAF, fixture data); the BadgeTinter contract tests cover the
        // mutation-verifiable logic in commonTest.
        composeRule.waitForIdle()
    }

    @Test
    fun fileBrowserScreenSourceReferencesBadgeTinter() {
        val candidates = listOf(
            "androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt",
            "../androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt",
            "src/main/java/digital/vasic/yole/android/ui/YoleApp.kt",
        )
        val src = candidates.firstNotNullOfOrNull { p ->
            val f = File(p)
            if (f.isFile) f.readText() else null
        } ?: error("YoleApp.kt not found from cwd=" + File(".").absolutePath)
        val stripped = src
            .replace(Regex("""/\*[\s\S]*?\*/"""), "")
            .lines()
            .joinToString("\n") { it.substringBefore("//") }
        val tintRefs = Regex("""BadgeTinter\.tintFor""").findAll(stripped).count()
        val langRefs = Regex("""BadgeTinter\.langIdFor""").findAll(stripped).count()
        assertEquals(
            "FileBrowserScreen must consult BadgeTinter.tintFor (iter-57 Phase 11)",
            true, tintRefs >= 1,
        )
        assertEquals(
            "FileBrowserScreen must consult BadgeTinter.langIdFor (iter-57 Phase 11)",
            true, langRefs >= 1,
        )
    }

    @Test
    fun badgeTinterContractAcrossEnabledStates() = runBlocking<Unit> {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        val argb = BadgeTinter.tintFor("README.md", theme)
        assertEquals("expected per-lang markdown tint", 0xFF00AA00.toInt(), argb)
        EnabledFormatGate.setEnabled(emptySet())
        assertNull("disabling markdown must hide its badge", BadgeTinter.tintFor("README.md", theme))
        // Reset
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }
}
