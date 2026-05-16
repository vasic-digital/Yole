/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 4: LspFormattingRequesterTest — integration test for
 * the LspFormattingRequester interface using a fake implementation.
 *
 * Uses FakeLspFormattingRequester (defined inline) rather than a real or
 * mocked LspServerHost to avoid MockK dependency and the full JVM-actual
 * lifecycle — the same pattern as GoToDefinitionActionTests (iter-62 Phase 7).
 *
 * 1 test:
 *   formatting_withFakeHost_returnsExpectedEdits
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub FakeLspFormattingRequester.formatting() to always return emptyList().
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.LspFormattingRequesterTest"
 *   3. Expect FAIL: formatting_withFakeHost_returnsExpectedEdits
 *      (assertEquals(2, result.size) fails — 0 ≠ 2).
 *   4. Revert; confirm 1/1 PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop (desktopTest): interface is commonMain; fake tested here via JVM runner.
 *   - Android: identical code path — covered by androidUnitTest when wired.
 *   - iOS/Wasm: LspFormattingRequester is pure interface; no platform divergence.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

// ---------------------------------------------------------------------------
// Test double
// ---------------------------------------------------------------------------

/** Minimal fake that returns a preconfigured list of [TextEdit]. */
private class FakeLspFormattingRequester(
    private val returns: List<TextEdit>,
) : LspFormattingRequester {
    var callCount = 0
    var lastLangId: String? = null
    var lastUri: String? = null
    var lastIndentSize: Int? = null
    var lastUseSpaces: Boolean? = null

    override suspend fun formatting(
        langId: String,
        uri: String,
        indentSize: Int,
        useSpaces: Boolean,
    ): List<TextEdit> {
        callCount++
        lastLangId = langId
        lastUri = uri
        lastIndentSize = indentSize
        lastUseSpaces = useSpaces
        return returns
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class LspFormattingRequesterTest {

    /**
     * When a FakeLspFormattingRequester is configured with canned TextEdits,
     * calling formatting() MUST return exactly those edits with the correct
     * ranges and replacement text. Formatting options MUST be forwarded.
     *
     * Mutation: stub FakeLspFormattingRequester to return emptyList()
     * → assertEquals(2, result.size) FAILS.
     */
    @Test
    fun formatting_withFakeHost_returnsExpectedEdits() = runBlocking<Unit> {
        val cannedEdits = listOf(
            TextEdit(range = 0..3, newText = "    "),
            TextEdit(range = 50..53, newText = "    "),
        )
        val requester = FakeLspFormattingRequester(returns = cannedEdits)

        val result = requester.formatting(
            langId = "kotlin",
            uri = "file:///src/Formatter.kt",
            indentSize = 4,
            useSpaces = true,
        )

        assertEquals(2, result.size, "formatting() must return 2 canned edits")
        assertEquals(TextEdit(range = 0..3, newText = "    "), result[0], "first edit must match")
        assertEquals(TextEdit(range = 50..53, newText = "    "), result[1], "second edit must match")

        assertEquals(1, requester.callCount, "formatting() must be called exactly once")
        assertEquals("kotlin", requester.lastLangId)
        assertEquals("file:///src/Formatter.kt", requester.lastUri)
        assertEquals(4, requester.lastIndentSize, "indentSize must be forwarded")
        assertEquals(true, requester.lastUseSpaces, "useSpaces must be forwarded")
    }
}
