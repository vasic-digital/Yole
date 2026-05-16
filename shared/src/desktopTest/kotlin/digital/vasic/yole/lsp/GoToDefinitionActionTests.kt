/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 7: GoToDefinitionAction unit tests (desktopTest).
 *
 * Tests use FakeLspDefinitionRequester (defined inline) rather than
 * a real or mocked LspServerHost to avoid MockK dependency and the
 * full JVM-actual lifecycle.  This is the documented plan deviation
 * (LspDefinitionRequester interface introduced for testability).
 *
 * 3 tests:
 *   1. zero_results_emits_toast
 *   2. one_result_pushes_and_opens
 *   3. multi_results_invokes_chooser
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub FakeLspDefinitionRequester to always return emptyList().
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.GoToDefinitionActionTests"
 *   3. Expect FAIL: one_result_pushes_and_opens  (onOpenFileAt never called).
 *              FAIL: multi_results_invokes_chooser (onChoose never called).
 *   4. Revert; confirm all 3 GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop: tested here (JVM runner, desktopTest source set).
 *   - Android: identical logic path; covered by androidUnitTest.
 *   - iOS/Web:  GoToDefinitionAction is commonMain — logic unchanged on those targets.
 *               Platform-specific test runners not required for pure-logic coverage.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// ---------------------------------------------------------------------------
// Test double
// ---------------------------------------------------------------------------

/** Minimal fake that returns a preconfigured list of [DefinitionLocation]. */
private class FakeLspDefinitionRequester(
    private val returns: List<DefinitionLocation>,
) : LspDefinitionRequester {
    var callCount = 0

    override suspend fun definition(
        langId: String,
        documentUri: String,
        line: Int,
        character: Int,
    ): List<DefinitionLocation> {
        callCount++
        return returns
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class GoToDefinitionActionTests {

    /**
     * When the LSP server returns zero results, onToast MUST be called with
     * the standard message and neither onOpenFileAt nor onChoose is invoked.
     *
     * Mutation: stub requester to return listOf(DefinitionLocation("x", 0..0))
     * → toast is never called → FAIL.
     */
    @Test
    fun zero_results_emits_toast() = runBlocking<Unit> {
        val requester = FakeLspDefinitionRequester(returns = emptyList())
        val stack = EditorNavigationStack()
        var toastMessage: String? = null
        var openFileCalled = false
        var chooseCalled = false

        GoToDefinitionAction.goToDefinition(
            requester = requester,
            langId = "kotlin",
            currentUri = "file:///src/Main.kt",
            currentCursor = 100,
            line = 5,
            character = 10,
            stack = stack,
            onOpenFileAt = { _, _ -> openFileCalled = true },
            onChoose = { _ -> chooseCalled = true },
            onToast = { msg -> toastMessage = msg },
        )

        assertEquals("No definition found at cursor", toastMessage, "toast message must match")
        assertFalse(openFileCalled, "onOpenFileAt must NOT be called on zero results")
        assertFalse(chooseCalled, "onChoose must NOT be called on zero results")
        assertEquals(0, stack.size, "stack must NOT be pushed on zero results")
        assertEquals(1, requester.callCount, "definition() must be called exactly once")
    }

    /**
     * When the LSP server returns exactly one result, the current location
     * MUST be pushed onto the navigation stack and onOpenFileAt called with
     * the target URI and the first character of the result's range.
     *
     * Mutation: stub requester to always return emptyList() → onOpenFileAt
     * never called, stack not pushed → FAIL.
     */
    @Test
    fun one_result_pushes_and_opens() = runBlocking<Unit> {
        val targetUri = "file:///src/Definition.kt"
        val targetRange = 250..280
        val requester = FakeLspDefinitionRequester(
            returns = listOf(DefinitionLocation(uri = targetUri, range = targetRange)),
        )
        val stack = EditorNavigationStack()
        val currentUri = "file:///src/Caller.kt"
        val currentCursor = 55
        var openedUri: String? = null
        var openedCursor: Int? = null
        var chooseCalled = false
        var toastCalled = false

        GoToDefinitionAction.goToDefinition(
            requester = requester,
            langId = "kotlin",
            currentUri = currentUri,
            currentCursor = currentCursor,
            line = 3,
            character = 8,
            stack = stack,
            onOpenFileAt = { uri, cur -> openedUri = uri; openedCursor = cur },
            onChoose = { _ -> chooseCalled = true },
            onToast = { _ -> toastCalled = true },
        )

        // Navigation stack must contain the origin location.
        assertEquals(1, stack.size, "stack must contain the origin entry")
        val pushed = stack.pop()!!
        assertEquals(currentUri, pushed.uri, "pushed URI must be the current document")
        assertEquals(currentCursor, pushed.cursorOffset, "pushed offset must be the current cursor")

        // onOpenFileAt must be called with target details.
        assertEquals(targetUri, openedUri, "openedUri must match the definition target")
        assertEquals(targetRange.first, openedCursor, "openedCursor must be range.first")

        assertFalse(chooseCalled, "onChoose must NOT be called on single result")
        assertFalse(toastCalled, "onToast must NOT be called on single result")
    }

    /**
     * When the LSP server returns multiple results, onChoose MUST be called
     * with the full list. The navigation stack MUST NOT be pushed (the push
     * happens later, once the user has selected from the chooser in Phase 8).
     *
     * Mutation: stub requester to always return emptyList() → onChoose never
     * called → FAIL.
     */
    @Test
    fun multi_results_invokes_chooser() = runBlocking<Unit> {
        val locations = listOf(
            DefinitionLocation("file:///A.kt", 10..20),
            DefinitionLocation("file:///B.kt", 30..40),
            DefinitionLocation("file:///C.kt", 50..60),
        )
        val requester = FakeLspDefinitionRequester(returns = locations)
        val stack = EditorNavigationStack()
        var chosenList: List<DefinitionLocation>? = null
        var openFileCalled = false
        var toastCalled = false

        GoToDefinitionAction.goToDefinition(
            requester = requester,
            langId = "python",
            currentUri = "file:///src/main.py",
            currentCursor = 200,
            line = 10,
            character = 4,
            stack = stack,
            onOpenFileAt = { _, _ -> openFileCalled = true },
            onChoose = { list -> chosenList = list },
            onToast = { _ -> toastCalled = true },
        )

        assertEquals(3, chosenList?.size, "onChoose must receive all 3 locations")
        assertEquals(locations, chosenList, "onChoose list must match the requester result")
        assertEquals(0, stack.size, "stack must NOT be pushed on multi-result (Phase 8 does that post-selection)")
        assertFalse(openFileCalled, "onOpenFileAt must NOT be called on multi-result")
        assertFalse(toastCalled, "onToast must NOT be called on multi-result")
    }
}
