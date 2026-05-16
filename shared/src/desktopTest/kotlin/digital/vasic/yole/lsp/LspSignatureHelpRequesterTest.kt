/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 4: LspSignatureHelpRequesterTest — integration test for
 * the LspSignatureHelpRequester interface using a fake implementation.
 *
 * Uses FakeLspSignatureHelpRequester (defined inline) rather than a real or
 * mocked LspServerHost to avoid MockK dependency and the full JVM-actual
 * lifecycle — the same pattern as GoToDefinitionActionTests (iter-62 Phase 7).
 *
 * 1 test:
 *   signatureHelp_withFakeHost_returnsExpectedHelp
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub FakeLspSignatureHelpRequester.signatureHelp() to always return null.
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.LspSignatureHelpRequesterTest"
 *   3. Expect FAIL: signatureHelp_withFakeHost_returnsExpectedHelp
 *      (assertNotNull fails — null returned instead of the canned SignatureHelp).
 *   4. Revert; confirm 1/1 PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop (desktopTest): interface is commonMain; fake tested here via JVM runner.
 *   - Android: identical code path — covered by androidUnitTest when wired.
 *   - iOS/Wasm: LspSignatureHelpRequester is pure interface; no platform divergence.
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

/** Minimal fake that returns a preconfigured [SignatureHelp]. */
private class FakeLspSignatureHelpRequester(
    private val returns: SignatureHelp?,
) : LspSignatureHelpRequester {
    var callCount = 0
    var lastLangId: String? = null
    var lastUri: String? = null
    var lastLine: Int? = null
    var lastCharacter: Int? = null

    override suspend fun signatureHelp(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
    ): SignatureHelp? {
        callCount++
        lastLangId = langId
        lastUri = uri
        lastLine = line
        lastCharacter = character
        return returns
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class LspSignatureHelpRequesterTest {

    /**
     * When a FakeLspSignatureHelpRequester is configured with a canned
     * SignatureHelp, calling signatureHelp() MUST return exactly that value
     * with the correct signature count, active indices, and parameter details.
     *
     * Mutation: stub FakeLspSignatureHelpRequester to return null
     * → assertNotNull(result) FAILS.
     */
    @Test
    fun signatureHelp_withFakeHost_returnsExpectedHelp() = runBlocking<Unit> {
        val cannedHelp = SignatureHelp(
            signatures = listOf(
                SignatureInformation(
                    label = "fun process(input: String, maxLen: Int): String",
                    documentation = "Processes the input string up to maxLen characters.",
                    parameters = listOf(
                        ParameterInformation(label = "input", documentation = "The raw input text."),
                        ParameterInformation(label = "maxLen", documentation = "Maximum output length."),
                    ),
                ),
            ),
            activeSignature = 0,
            activeParameter = 1,
        )
        val requester = FakeLspSignatureHelpRequester(returns = cannedHelp)

        val result = requester.signatureHelp(
            langId = "kotlin",
            uri = "file:///src/Processor.kt",
            line = 12,
            character = 24,
        )

        assertNotNull(result, "signatureHelp() must return the canned SignatureHelp, not null")
        assertEquals(1, result.signatures.size, "must have exactly 1 signature")
        val sig = result.signatures[0]
        assertEquals("fun process(input: String, maxLen: Int): String", sig.label)
        assertEquals("Processes the input string up to maxLen characters.", sig.documentation)
        assertEquals(2, sig.parameters.size, "must have 2 parameters")
        assertEquals("input", sig.parameters[0].label)
        assertEquals("maxLen", sig.parameters[1].label)
        assertEquals(0, result.activeSignature, "activeSignature must be 0")
        assertEquals(1, result.activeParameter, "activeParameter must be 1 (second param active)")

        assertEquals(1, requester.callCount, "signatureHelp() must be called exactly once")
        assertEquals("kotlin", requester.lastLangId)
        assertEquals("file:///src/Processor.kt", requester.lastUri)
        assertEquals(12, requester.lastLine)
        assertEquals(24, requester.lastCharacter)
    }
}
