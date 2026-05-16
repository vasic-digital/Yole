/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 4: LspCodeActionRequesterTest — integration test for
 * the LspCodeActionRequester interface using a fake implementation.
 *
 * Uses FakeLspCodeActionRequester (defined inline) rather than a real or
 * mocked LspServerHost to avoid MockK dependency and the full JVM-actual
 * lifecycle — the same pattern as GoToDefinitionActionTests (iter-62 Phase 7).
 *
 * 1 test:
 *   codeActions_withFakeHost_returnsExpectedActions
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub FakeLspCodeActionRequester.codeActions() to always return emptyList().
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.LspCodeActionRequesterTest"
 *   3. Expect FAIL: codeActions_withFakeHost_returnsExpectedActions
 *      (assertEquals(2, result.size) fails — 0 ≠ 2).
 *   4. Revert; confirm 1/1 PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop (desktopTest): interface is commonMain; fake tested here via JVM runner.
 *   - Android: identical code path — covered by androidUnitTest when wired.
 *   - iOS/Wasm: LspCodeActionRequester is pure interface; no platform divergence.
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

/** Minimal fake that returns a preconfigured list of [CodeAction]. */
private class FakeLspCodeActionRequester(
    private val returns: List<CodeAction>,
) : LspCodeActionRequester {
    var callCount = 0
    var lastLangId: String? = null
    var lastUri: String? = null
    var lastRange: IntRange? = null

    override suspend fun codeActions(
        langId: String,
        uri: String,
        range: IntRange,
    ): List<CodeAction> {
        callCount++
        lastLangId = langId
        lastUri = uri
        lastRange = range
        return returns
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class LspCodeActionRequesterTest {

    /**
     * When a FakeLspCodeActionRequester is configured with canned CodeActions,
     * calling codeActions() MUST return exactly those actions with the expected
     * titles, kinds, edits, and commands.
     *
     * Mutation: stub FakeLspCodeActionRequester to return emptyList()
     * → assertEquals(2, result.size) FAILS.
     */
    @Test
    fun codeActions_withFakeHost_returnsExpectedActions() = runBlocking<Unit> {
        val cannedEdit = WorkspaceEdit(
            changes = mapOf(
                "file:///src/Util.kt" to listOf(
                    TextEdit(range = 5..15, newText = "import digital.vasic.yole.util.Helper"),
                ),
            ),
        )
        val cannedActions = listOf(
            CodeAction(
                title = "Import 'Helper'",
                kind = "quickfix",
                edit = cannedEdit,
                command = null,
            ),
            CodeAction(
                title = "Extract function",
                kind = "refactor.extract",
                edit = null,
                command = "yole.extractFunction",
            ),
        )
        val requester = FakeLspCodeActionRequester(returns = cannedActions)

        val result = requester.codeActions(
            langId = "kotlin",
            uri = "file:///src/Util.kt",
            range = 5..15,
        )

        assertEquals(2, result.size, "codeActions() must return 2 canned actions")
        val importAction = result[0]
        assertEquals("Import 'Helper'", importAction.title)
        assertEquals("quickfix", importAction.kind)
        assertEquals(cannedEdit, importAction.edit)
        assertEquals(null, importAction.command)

        val extractAction = result[1]
        assertEquals("Extract function", extractAction.title)
        assertEquals("refactor.extract", extractAction.kind)
        assertEquals(null, extractAction.edit)
        assertEquals("yole.extractFunction", extractAction.command)

        assertEquals(1, requester.callCount, "codeActions() must be called exactly once")
        assertEquals("kotlin", requester.lastLangId)
        assertEquals("file:///src/Util.kt", requester.lastUri)
        assertEquals(5..15, requester.lastRange)
    }
}
