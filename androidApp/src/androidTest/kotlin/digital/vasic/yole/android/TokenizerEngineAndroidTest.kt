/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 — On-device anti-bluff verification of the Tree-Sitter
 * tokenizer engine on Android. Closes the iter-57 Phase 5 defect
 * `#android-tree-sitter-ndk-so-missing`.
 *
 * This is an *instrumented* test by necessity: it dlopens the
 * arm64-v8a or x86_64 Android NDK build of libtree-sitter.so +
 * libtree-sitter-markdown.so (substituted into the bonede JARs at
 * build time by shared/build.gradle.kts:repackageTreeSitterJarForAndroid).
 * A host JVM cannot load Android-ABI ELFs, so this MUST run on a
 * real device or emulator — no Robolectric shortcut available.
 *
 * Anti-bluff anchor per CONST-035:
 *   - Engine.initialize() MUST succeed (returns Result.success).
 *   - Engine.tokenize(...) MUST emit >= 5 tokens with a non-blank
 *     first scope — same bar as the Desktop JvmTest.
 *   - Mutation step (Phase 5.7 contract): if the JNI binding fails
 *     to actually parse, .tokenize returns an empty list, the
 *     `tokens.size >= 5` assertion fails, and this test exits FAIL.
 *
 *########################################################*/
package digital.vasic.yole.android

import androidx.test.platform.app.InstrumentationRegistry
import digital.vasic.yole.syntax.TokenizerEngine
import digital.vasic.yole.util.AppContextHolder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenizerEngineAndroidTest {

    @Before
    fun setUp() {
        AppContextHolder.context =
            InstrumentationRegistry.getInstrumentation().targetContext
    }

    /**
     * Anti-bluff: initialise the engine and assert it actually loaded
     * the Android NDK shared libraries. Any failure here means the
     * `repackageBonedeJarsForAndroid` Gradle task family did not
     * substitute the .so files correctly, or our NDK build is broken
     * for the device ABI under test.
     */
    @Test
    fun initializeSucceedsOnAndroidDevice() = runBlocking {
        val engine = TokenizerEngine()
        val result = engine.initialize()
        assertTrue(
            "TokenizerEngine.initialize() failed on Android device: " +
                "${result.exceptionOrNull()?.javaClass?.name} " +
                "${result.exceptionOrNull()?.message} — this means the " +
                "Android NDK .so substitution in shared/build.gradle.kts " +
                "(#android-tree-sitter-ndk-so-missing fix) did not apply.",
            result.isSuccess
        )
    }

    /**
     * Anti-bluff: tokenize a known markdown snippet on the device and
     * assert real Tree-Sitter output. If the JNI binding is faulty
     * (wrong .so loaded, wrong grammar pointer, etc.) the parser
     * returns a single ERROR token or empty list — either fails the
     * tokens.size >= 5 assertion below.
     */
    @Test
    fun tokenizesMarkdownSnippetOnDevice() = runBlocking {
        val engine = TokenizerEngine()
        val initResult = engine.initialize()
        assertTrue(
            "Engine init must succeed before tokenize. exception=" +
                "${initResult.exceptionOrNull()}",
            initResult.isSuccess
        )

        val snippet = "# Heading\n\nA paragraph.\n"
        val tokens = engine.tokenize(snippet, "markdown")

        assertNotNull(tokens)
        assertTrue(
            "Expected >= 5 leaf tokens from `$snippet`, got ${tokens.size}: $tokens",
            tokens.size >= 5
        )
        val first = tokens.first()
        assertTrue(
            "First token must have a non-blank scope: ${first}",
            first.scope.isNotBlank()
        )
        // Anti-bluff: assert tokens cover the heading marker. A faked
        // implementation that returns hardcoded tokens unrelated to
        // input bytes would fail this byte-range sanity check.
        assertEquals(
            "First token should start at byte 0 of the input",
            0,
            first.startByte
        )
        assertTrue(
            "Some token's end-byte must reach into the paragraph (>=15)",
            tokens.any { it.endByte >= 15 }
        )
    }

    /**
     * Mutation guard: re-running the same engine instance MUST be
     * idempotent and continue to produce non-empty token streams.
     * Catches state corruption / use-after-close defects.
     */
    @Test
    fun tokenizesReentrantOnSameEngine() = runBlocking {
        val engine = TokenizerEngine()
        assertTrue(engine.initialize().isSuccess)

        val a = engine.tokenize("# A\n", "markdown")
        val b = engine.tokenize("## B\n", "markdown")
        val c = engine.tokenize("# A\n", "markdown")  // same as first
        assertTrue("first tokenize empty: $a", a.isNotEmpty())
        assertTrue("second tokenize empty: $b", b.isNotEmpty())
        assertEquals(
            "Re-tokenizing the same input MUST yield identical scopes",
            a.map { it.scope },
            c.map { it.scope }
        )
    }
}
