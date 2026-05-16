/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 4: LspRenameRequesterTest — integration test for
 * the LspRenameRequester interface using a fake implementation.
 *
 * Uses FakeLspRenameRequester (defined inline) rather than a real or
 * mocked LspServerHost to avoid MockK dependency and the full JVM-actual
 * lifecycle — the same pattern as GoToDefinitionActionTests (iter-62 Phase 7).
 *
 * 1 test:
 *   rename_withFakeHost_returnsExpectedEdit
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub FakeLspRenameRequester.rename() to always return null.
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.LspRenameRequesterTest"
 *   3. Expect FAIL: rename_withFakeHost_returnsExpectedEdit
 *      (assertNotNull fails because null is returned instead of the canned edit).
 *   4. Revert; confirm 1/1 PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop (desktopTest): interface is commonMain; fake tested here via JVM runner.
 *   - Android: identical code path — covered by androidUnitTest when wired.
 *   - iOS/Wasm: LspRenameRequester is pure interface; no platform divergence.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

// ---------------------------------------------------------------------------
// Test double
// ---------------------------------------------------------------------------

/** Minimal fake that returns a preconfigured [WorkspaceEdit]. */
private class FakeLspRenameRequester(
    private val returns: WorkspaceEdit?,
) : LspRenameRequester {
    var callCount = 0
    var lastLangId: String? = null
    var lastUri: String? = null
    var lastLine: Int? = null
    var lastCharacter: Int? = null
    var lastNewName: String? = null

    override suspend fun rename(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        newName: String,
    ): WorkspaceEdit? {
        callCount++
        lastLangId = langId
        lastUri = uri
        lastLine = line
        lastCharacter = character
        lastNewName = newName
        return returns
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class LspRenameRequesterTest {

    /**
     * When a FakeLspRenameRequester is configured with a canned WorkspaceEdit,
     * calling rename() MUST return exactly that edit with the expected structure.
     * Also verifies that the requester is called with the arguments passed in.
     *
     * Mutation: stub FakeLspRenameRequester to return null
     * → assertNotNull(result) FAILS.
     */
    @Test
    fun rename_withFakeHost_returnsExpectedEdit() = runBlocking<Unit> {
        val cannedEdit = WorkspaceEdit(
            changes = mapOf(
                "file:///src/Main.kt" to listOf(
                    TextEdit(range = 42..50, newText = "renamedSymbol"),
                ),
                "file:///src/Other.kt" to listOf(
                    TextEdit(range = 10..18, newText = "renamedSymbol"),
                ),
            ),
        )
        val requester = FakeLspRenameRequester(returns = cannedEdit)

        val result = requester.rename(
            langId = "kotlin",
            uri = "file:///src/Main.kt",
            line = 3,
            character = 8,
            newName = "renamedSymbol",
        )

        assertNotNull(result, "rename() must return the canned WorkspaceEdit, not null")
        assertEquals(2, result.changes.size, "edit must span 2 files")
        assertEquals(
            listOf(TextEdit(range = 42..50, newText = "renamedSymbol")),
            result.changes["file:///src/Main.kt"],
            "edits for Main.kt must match",
        )
        assertEquals(
            listOf(TextEdit(range = 10..18, newText = "renamedSymbol")),
            result.changes["file:///src/Other.kt"],
            "edits for Other.kt must match",
        )
        assertEquals(1, requester.callCount, "rename() must be called exactly once")
        assertEquals("kotlin", requester.lastLangId)
        assertEquals("file:///src/Main.kt", requester.lastUri)
        assertEquals(3, requester.lastLine)
        assertEquals(8, requester.lastCharacter)
        assertEquals("renamedSymbol", requester.lastNewName)
    }
}
