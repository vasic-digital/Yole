/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-61 Phase 2: LspWorkspaceResolver tests.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub resolve() body to: return file.parent ?: file
 *   2. Re-run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.LspWorkspaceResolverTest"
 *   3. Expect: at least 3 tests FAIL:
 *      - findsProjectMarker_inImmediateParent (file.parent = src/, expected tmpDir)
 *      - findsProjectMarker_traversingMultipleLevels (file.parent = deepDir, expected tmp)
 *      - firstMatchingMarker_wins (file.parent = inner/src, expected inner/)
 *   4. Revert; confirm 5/5 PASS.
 *#######################################################*/
package digital.vasic.yole.lsp

import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LspWorkspaceResolverTest {

    private lateinit var tmpDir: okio.Path
    private val fs = FileSystem.SYSTEM

    @BeforeTest
    fun setUp() {
        val base = java.nio.file.Files.createTempDirectory("lsp-ws-test-").toString()
        tmpDir = base.toPath()
    }

    @AfterTest
    fun tearDown() {
        if (fs.exists(tmpDir)) {
            fs.deleteRecursively(tmpDir)
        }
    }

    /**
     * A Cargo.toml lives directly in tmpDir; the file being edited is under
     * tmpDir/src/main.rs. The resolver must step one level up from src/ and
     * find the marker at tmpDir.
     *
     * Mutation check: stub returns file.parent = tmpDir/src — test FAILS.
     */
    @Test
    fun findsProjectMarker_inImmediateParent() {
        val srcDir = tmpDir.resolve("src")
        fs.createDirectory(srcDir)
        fs.write(tmpDir.resolve("Cargo.toml")) { writeUtf8("") }
        val file = srcDir.resolve("main.rs")
        fs.write(file) { writeUtf8("") }

        val resolved = LspWorkspaceResolver.resolve(file, listOf("Cargo.toml"), fs)

        assertEquals(tmpDir, resolved)
    }

    /**
     * go.mod is at tmpDir; the edited file is 6 levels deep under tmp/a/b/c/d/e/f/.
     * The resolver must traverse all 6 levels to find go.mod at the root.
     *
     * Mutation check: stub returns file.parent = tmp/a/b/c/d/e/f — test FAILS.
     */
    @Test
    fun findsProjectMarker_traversingMultipleLevels() {
        val deepDir = tmpDir.resolve("a").resolve("b").resolve("c")
            .resolve("d").resolve("e").resolve("f")
        fs.createDirectories(deepDir)
        fs.write(tmpDir.resolve("go.mod")) { writeUtf8("") }
        val file = deepDir.resolve("main.go")
        fs.write(file) { writeUtf8("") }

        val resolved = LspWorkspaceResolver.resolve(file, listOf("go.mod"), fs)

        assertEquals(tmpDir, resolved)
    }

    /**
     * No marker exists anywhere in the tree. The resolver must fall back to
     * file.parent (the immediate parent of the edited file).
     *
     * Mutation check: stub returns file.parent = tmpDir, expected = tmpDir — PASS.
     * (This test is not in the 3-failure set; it validates the fallback path.)
     */
    @Test
    fun fallsBackToParent_whenNoMarkerFound() {
        val file = tmpDir.resolve("orphan.rs")
        fs.write(file) { writeUtf8("") }

        val resolved = LspWorkspaceResolver.resolve(file, listOf("Cargo.toml"), fs)

        assertEquals(tmpDir, resolved)
    }

    /**
     * Tree is 25 levels deep with no marker. After 20 steps the resolver gives up
     * and returns file.parent (the 25th-level directory), not any ancestor above.
     *
     * Mutation check: stub returns file.parent = deepest dir, expected = deepest dir — PASS.
     * (This test is not in the 3-failure set; it validates the 20-level cap.)
     */
    @Test
    fun stopsAfter20Levels_evenIfMarkerWouldExistAbove() {
        var current = tmpDir
        repeat(25) { idx ->
            current = current.resolve("d$idx")
            fs.createDirectory(current)
        }
        val file = current.resolve("deep.rs")
        fs.write(file) { writeUtf8("") }

        val resolved = LspWorkspaceResolver.resolve(file, listOf("Cargo.toml"), fs)

        // Should fall back to file.parent (the deepest directory), NOT walk past 20 levels.
        assertEquals(file.parent, resolved)
    }

    /**
     * An outer go.mod lives at tmpDir; an inner Cargo.toml lives at tmpDir/inner/.
     * The file being edited is at tmpDir/inner/src/main.rs (one level below inner/).
     * With markers [Cargo.toml, go.mod], the resolver should pick up the closer marker
     * (Cargo.toml at inner/) rather than the farther go.mod at tmpDir.
     *
     * Mutation check: stub returns file.parent = tmpDir/inner/src — test FAILS
     * (expected is tmpDir/inner/).
     */
    @Test
    fun firstMatchingMarker_wins() {
        val inner = tmpDir.resolve("inner")
        fs.createDirectory(inner)
        val innerSrc = inner.resolve("src")
        fs.createDirectory(innerSrc)
        fs.write(tmpDir.resolve("go.mod")) { writeUtf8("") }
        fs.write(inner.resolve("Cargo.toml")) { writeUtf8("") }
        val file = innerSrc.resolve("main.rs")
        fs.write(file) { writeUtf8("") }

        // Markers listed: Cargo.toml first, then go.mod.
        // Walking up from innerSrc: first parent = inner/ → Cargo.toml found → stop.
        val resolved = LspWorkspaceResolver.resolve(file, listOf("Cargo.toml", "go.mod"), fs)

        assertEquals(inner, resolved)
    }
}
