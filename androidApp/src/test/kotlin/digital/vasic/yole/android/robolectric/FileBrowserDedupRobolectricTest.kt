/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-55, CONST-035 anti-bluff):
 *   Asserts that the Android UI no longer exposes a duplicate File
 *   Browser entry point. Per the iter-55 dedup, the canonical entry
 *   is the FILES bottom-nav tab; the "More" screen's "File Browser"
 *   card and the SubScreen.FILE_BROWSER enum value MUST be removed.
 *
 *   This is a STRUCTURAL anti-bluff test: it asserts source-level
 *   invariants that a runtime UI test could fake-pass for. Mutating
 *   YoleApp.kt to restore the duplicate (e.g. adding back the
 *   File Browser Card to MoreScreen) MUST fail this test.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FileBrowserDedupRobolectricTest {

    private fun loadYoleApp(): String {
        val candidates = listOf(
            "androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt",
            "../androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt",
            "src/main/java/digital/vasic/yole/android/ui/YoleApp.kt",
        )
        for (path in candidates) {
            val f = File(path)
            if (f.isFile) return f.readText()
        }
        error("YoleApp.kt not found; checked: $candidates (cwd=${File(".").absolutePath})")
    }

    private fun stripComments(src: String): String =
        src
            .replace(Regex("""/\*[\s\S]*?\*/"""), "")
            .lines()
            .joinToString("\n") { it.substringBefore("//") }

    /**
     * The SubScreen enum MUST NOT contain a FILE_BROWSER entry. The
     * canonical file-browsing UI is reached via Screen.FILES (bottom
     * nav). Re-adding SubScreen.FILE_BROWSER would re-introduce the
     * duplicate entry point the iter-55 dedup removed.
     */
    @Test
    fun subScreenEnumHasNoFileBrowserEntry() {
        val src = stripComments(loadYoleApp())
        val enumBlock = Regex("""enum\s+class\s+SubScreen\s*\{[^}]*}""")
            .find(src)
            ?.value
            ?: error("SubScreen enum class not found in YoleApp.kt")
        assertFalse(
            "SubScreen enum MUST NOT contain FILE_BROWSER (iter-55 dedup): re-adding it would re-create the duplicate File Browser entry point. Enum body found: $enumBlock",
            enumBlock.contains("FILE_BROWSER"),
        )
    }

    /**
     * No render branch for SubScreen.FILE_BROWSER should exist. Catches
     * the case where someone re-adds the enum value AND a render branch
     * via copy-paste from git history.
     */
    @Test
    fun noRenderBranchForFileBrowserSubScreen() {
        val src = stripComments(loadYoleApp())
        val matches = Regex("""SubScreen\.FILE_BROWSER""").findAll(src).count()
        assertEquals(
            "no source references to SubScreen.FILE_BROWSER allowed (iter-55 dedup); found $matches",
            0, matches,
        )
    }

    /**
     * MoreScreen MUST NOT declare an onFileBrowserClick parameter.
     * That parameter previously wired a Card in MoreScreen to navigate
     * to the duplicate File Browser SubScreen. We extract the
     * MoreScreen function body by paren-counting (regex can't handle
     * nested parens reliably) and assert the substring is absent.
     */
    @Test
    fun moreScreenHasNoOnFileBrowserClickParameter() {
        val src = stripComments(loadYoleApp())
        val funStart = src.indexOf("fun MoreScreen(")
        assert(funStart >= 0) { "MoreScreen function not found" }
        val parenStart = src.indexOf('(', funStart)
        // Paren-counting walk through the parameter list.
        var depth = 0
        var end = -1
        for (i in parenStart until src.length) {
            when (src[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) { end = i; break }
                }
            }
        }
        assert(end > parenStart) { "could not find end of MoreScreen parameter list" }
        val signature = src.substring(funStart, end + 1)
        assertFalse(
            "MoreScreen MUST NOT declare onFileBrowserClick (iter-55 dedup). Signature found: $signature",
            signature.contains("onFileBrowserClick"),
        )
    }

    /**
     * No source references anywhere should call onFileBrowserClick.
     * Catches the case where the parameter is removed from MoreScreen
     * but a stale invocation remains in a parent screen.
     */
    @Test
    fun noSourceReferencesToOnFileBrowserClick() {
        val src = stripComments(loadYoleApp())
        val count = Regex("""onFileBrowserClick""").findAll(src).count()
        assertEquals(
            "no source references to onFileBrowserClick allowed (iter-55 dedup); found $count",
            0, count,
        )
    }

    /**
     * The FilesScreen composable MUST still exist — it is the canonical
     * file-browsing surface (bottom-nav FILES tab). This guards against
     * accidental over-deletion in the dedup refactor.
     */
    @Test
    fun filesScreenStillExists() {
        val src = loadYoleApp()
        assert(src.contains("fun FilesScreen(")) {
            "FilesScreen composable (canonical Files tab) MUST remain after iter-55 dedup"
        }
    }
}
