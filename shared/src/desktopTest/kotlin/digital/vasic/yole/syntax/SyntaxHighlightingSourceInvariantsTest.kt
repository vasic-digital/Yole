/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 8: structural anti-bluff tests. Walks the
 * shared/src/commonMain/kotlin/digital/vasic/yole/syntax/ source tree
 * and enforces invariants that can't be expressed at the type system
 * level:
 *   1. No platform-specific imports in commonMain (android./java./javax.).
 *   2. Every `expect class` in commonMain has the expected 4 actuals
 *      (androidMain, desktopMain, iosMain, wasmJsMain).
 *   3. No `runBlocking` calls in production code (commonMain).
 *
 * Anti-bluff (CONST-035): adding `import java.io.File` to
 * SyntaxHighlighter.kt flips invariant #1 to FAIL.
 *
 *########################################################*/
package digital.vasic.yole.syntax

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class SyntaxHighlightingSourceInvariantsTest {

    private val repoRoot: File = locateRepoRoot()

    private val commonMainSyntax: File =
        File(repoRoot, "shared/src/commonMain/kotlin/digital/vasic/yole/syntax")

    private fun locateRepoRoot(): File {
        // Tests run from <repo>/shared (Gradle working dir) or sometimes
        // from <repo>. Walk up until we find settings.gradle.kts.
        var dir: File? = File("").absoluteFile
        repeat(6) {
            val d = dir ?: return@repeat
            if (File(d, "settings.gradle.kts").exists()) return d
            dir = d.parentFile
        }
        fail("could not locate repo root (no settings.gradle.kts in ancestors)")
        error("unreachable")
    }

    private fun walkKotlin(dir: File): List<File> =
        if (!dir.exists()) emptyList()
        else dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun commonMainSyntax_hasNoPlatformImports() {
        val offenders = mutableListOf<String>()
        val patterns = listOf(
            Regex("""^\s*import\s+android\."""),
            Regex("""^\s*import\s+java\."""),
            Regex("""^\s*import\s+javax\."""),
        )
        for (file in walkKotlin(commonMainSyntax)) {
            val text = file.readText()
            for (line in text.lineSequence()) {
                if (patterns.any { it.containsMatchIn(line) }) {
                    offenders += "${file.relativeTo(repoRoot)}: $line"
                }
            }
        }
        assertTrue(
            "commonMain/syntax must not contain android./java./javax. imports, but found:\n${offenders.joinToString("\n")}",
            offenders.isEmpty(),
        )
    }

    @Test
    fun expectClassesHaveFourActuals() {
        // Find every `expect class Foo` in commonMain/syntax.
        val expectClassRegex = Regex("""^\s*expect\s+class\s+([A-Za-z_][A-Za-z0-9_]*)""")
        val expectClasses = mutableListOf<String>()
        for (file in walkKotlin(commonMainSyntax)) {
            for (line in file.readText().lineSequence()) {
                expectClassRegex.find(line)?.groupValues?.getOrNull(1)?.let { expectClasses += it }
            }
        }
        // Per Phase 5/6/7, the only expect class in syntax/ is TokenizerEngine.
        // This assertion documents that — if a new expect class is added, the test
        // immediately surfaces it.
        assertTrue(
            "expected at least one expect class in commonMain/syntax (got $expectClasses)",
            expectClasses.isNotEmpty(),
        )

        val expectedActualDirs = listOf("androidMain", "desktopMain", "iosMain", "wasmJsMain")
        for (cls in expectClasses) {
            val actualCount = expectedActualDirs.count { platform ->
                val platformSyntaxDir = File(
                    repoRoot,
                    "shared/src/$platform/kotlin/digital/vasic/yole/syntax",
                )
                val actualRegex = Regex("""actual\s+class\s+$cls\b""")
                walkKotlin(platformSyntaxDir).any { actualRegex.containsMatchIn(it.readText()) }
            }
            assertEquals(
                "expect class $cls should have actuals in all 4 platforms (${expectedActualDirs.joinToString()})",
                4,
                actualCount,
            )
        }
    }

    @Test
    fun productionCode_doesNotUseRunBlocking() {
        val runBlockingRegex = Regex("""\brunBlocking\s*[<({]""")
        val offenders = mutableListOf<String>()
        for (file in walkKotlin(commonMainSyntax)) {
            val text = file.readText()
            // Allow runBlocking inside KDoc / // comments — easier: just check
            // non-comment lines (strip // … and rough KDoc /* … */ spans).
            val noComments = text.replace(Regex("""/\*[\s\S]*?\*/"""), "")
                .lineSequence()
                .map { it.substringBefore("//") }
                .joinToString("\n")
            if (runBlockingRegex.containsMatchIn(noComments)) {
                offenders += file.relativeTo(repoRoot).toString()
            }
        }
        assertTrue(
            "commonMain/syntax production code must not call runBlocking, but found in: $offenders",
            offenders.isEmpty(),
        )
    }
}
