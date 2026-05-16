/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 4: LspReferencesRequesterTest — integration test for
 * the LspReferencesRequester interface using a fake implementation.
 *
 * Uses FakeLspReferencesRequester (defined inline) rather than a real or
 * mocked LspServerHost to avoid MockK dependency and the full JVM-actual
 * lifecycle — the same pattern as GoToDefinitionActionTests (iter-62 Phase 7).
 *
 * 1 test:
 *   references_withFakeHost_returnsExpectedLocations
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub FakeLspReferencesRequester.references() to always return emptyList().
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.LspReferencesRequesterTest"
 *   3. Expect FAIL: references_withFakeHost_returnsExpectedLocations
 *      (assertEquals(3, result.size) fails — 0 ≠ 3).
 *   4. Revert; confirm 1/1 PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop (desktopTest): interface is commonMain; fake tested here via JVM runner.
 *   - Android: identical code path — covered by androidUnitTest when wired.
 *   - iOS/Wasm: LspReferencesRequester is pure interface; no platform divergence.
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

/** Minimal fake that returns a preconfigured list of [ReferenceLocation]. */
private class FakeLspReferencesRequester(
    private val returns: List<ReferenceLocation>,
) : LspReferencesRequester {
    var callCount = 0
    var lastLangId: String? = null
    var lastUri: String? = null
    var lastLine: Int? = null
    var lastCharacter: Int? = null
    var lastIncludeDeclaration: Boolean? = null

    override suspend fun references(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
        includeDeclaration: Boolean,
    ): List<ReferenceLocation> {
        callCount++
        lastLangId = langId
        lastUri = uri
        lastLine = line
        lastCharacter = character
        lastIncludeDeclaration = includeDeclaration
        return returns
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class LspReferencesRequesterTest {

    /**
     * When a FakeLspReferencesRequester is configured with canned locations,
     * calling references() MUST return exactly those ReferenceLocation values
     * (each a DefinitionLocation typealias). Also verifies that all call
     * arguments are forwarded correctly.
     *
     * Mutation: stub FakeLspReferencesRequester to return emptyList()
     * → assertEquals(3, result.size) FAILS.
     */
    @Test
    fun references_withFakeHost_returnsExpectedLocations() = runBlocking<Unit> {
        val cannedLocations = listOf(
            ReferenceLocation(uri = "file:///src/Main.kt", range = 100..115),
            ReferenceLocation(uri = "file:///src/Other.kt", range = 200..215),
            ReferenceLocation(uri = "file:///src/Main.kt", range = 300..315),
        )
        val requester = FakeLspReferencesRequester(returns = cannedLocations)

        val result = requester.references(
            langId = "kotlin",
            uri = "file:///src/Main.kt",
            line = 7,
            character = 12,
            includeDeclaration = true,
        )

        assertEquals(3, result.size, "references() must return 3 canned locations")
        assertEquals(
            ReferenceLocation(uri = "file:///src/Main.kt", range = 100..115),
            result[0],
            "first location must match",
        )
        assertEquals(
            ReferenceLocation(uri = "file:///src/Other.kt", range = 200..215),
            result[1],
            "second location must match",
        )
        assertEquals(
            ReferenceLocation(uri = "file:///src/Main.kt", range = 300..315),
            result[2],
            "third location must match",
        )

        assertEquals(1, requester.callCount, "references() must be called exactly once")
        assertEquals("kotlin", requester.lastLangId)
        assertEquals("file:///src/Main.kt", requester.lastUri)
        assertEquals(7, requester.lastLine)
        assertEquals(12, requester.lastCharacter)
        assertEquals(true, requester.lastIncludeDeclaration, "includeDeclaration must be forwarded")
    }
}
