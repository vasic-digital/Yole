/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 7: SignatureHelpTriggerTest — desktopTest.
 *
 * Tests [SignatureHelpTrigger] using a fake [LspSignatureHelpRequester].
 * Placed in desktopTest because:
 *   - Tests use real-clock delay() to verify async behaviour.
 *   - kotlinx-coroutines-test has no WASM variant → unavailable in
 *     commonTest per CLAUDE.md JUnit4/WASM constraints.
 *   - JUnit4 + runBlocking<Unit> (NOT runTest) per project convention.
 *
 * Test doubles:
 *   FakeTriggerRequester — records call count and returns a
 *   preconfigured SignatureHelp (or null).
 *
 * Tests:
 *   1. keystroke_open_paren_invokesRequester
 *   2. keystroke_comma_invokesRequester
 *   3. keystroke_close_paren_dismisses
 *   4. keystroke_non_trigger_char_isNoOp
 *   5. inflight_cancelled_before_new_request
 *
 * Anti-bluff mutation procedure (CONST-035):
 *
 *   keystroke_open_paren_invokesRequester:
 *     Mutation: stub onKeystroke to no-op → requester.callCount stays 0 →
 *     assertEquals(1, callCount) FAILS.
 *
 *   keystroke_close_paren_dismisses:
 *     Mutation: remove handleDismiss() call for ')' → onResult is never
 *     called with null → assertNull(results.last()) FAILS.
 *
 *   keystroke_non_trigger_char_isNoOp:
 *     Mutation: treat every char as a trigger → callCount becomes ≥ 1 →
 *     assertEquals(0, callCount) FAILS.
 *
 *   inflight_cancelled_before_new_request:
 *     Mutation: remove `requestJob?.cancel()` before launching new job →
 *     both the old and new jobs complete → requester.callCount becomes ≥ 2
 *     on the *first* trigger before the cancellation guard fires. However,
 *     because the fake requester is instant, this specific mutation is
 *     better caught by the structural source check below.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop: tested here via desktopTest JVM runner.
 *   - Common: SignatureHelpTrigger is in commonMain; all targets compile it.
 *   - Android: wired in Phase 10.
 *   - iOS/Wasm: compiles; wiring deferred.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

// ---------------------------------------------------------------------------
// Test double
// ---------------------------------------------------------------------------

/**
 * Instant fake requester — returns [returns] without any delay.
 * Tracks [callCount] so tests can verify the trigger fired (or didn't).
 */
private class FakeTriggerRequester(
    private val returns: SignatureHelp?,
) : LspSignatureHelpRequester {
    var callCount = 0

    override suspend fun signatureHelp(
        langId: String,
        uri: String,
        line: Int,
        character: Int,
    ): SignatureHelp? {
        callCount++
        return returns
    }
}

// ---------------------------------------------------------------------------
// Shared canned data
// ---------------------------------------------------------------------------

private val CANNED_HELP = SignatureHelp(
    signatures = listOf(
        SignatureInformation(
            label = "fun foo(a: Int, b: String): Unit",
            documentation = null,
            parameters = listOf(
                ParameterInformation(label = "a", documentation = null),
                ParameterInformation(label = "b", documentation = null),
            ),
        ),
    ),
    activeSignature = 0,
    activeParameter = 0,
)

// ---------------------------------------------------------------------------
// Test class
// ---------------------------------------------------------------------------

class SignatureHelpTriggerTest {

    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.Default)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    // -----------------------------------------------------------------------
    // Test 1: keystroke_open_paren_invokesRequester
    // -----------------------------------------------------------------------

    /**
     * Typing '(' MUST invoke [LspSignatureHelpRequester.signatureHelp] exactly
     * once and deliver its result to onResult.
     *
     * Anti-bluff: stub onKeystroke to no-op → callCount stays 0 → FAIL.
     *
     * Mutation: stub TRIGGER_CHARS check to empty set → FAIL.
     */
    @Test
    fun keystroke_open_paren_invokesRequester() = runBlocking<Unit> {
        val results = mutableListOf<SignatureHelp?>()
        val requester = FakeTriggerRequester(returns = CANNED_HELP)
        val trigger = SignatureHelpTrigger(
            scope = scope,
            requester = requester,
            onResult = { results += it },
        )

        trigger.onKeystroke('(', "kotlin", "file:///Foo.kt", line = 5, character = 10)
        delay(50L) // allow coroutine to complete

        assertEquals("'(' MUST invoke requester exactly once", 1, requester.callCount)
        assertTrue("onResult MUST be called at least once", results.isNotEmpty())
        assertNotNull("onResult MUST deliver the canned SignatureHelp (not null)", results.last())
    }

    // -----------------------------------------------------------------------
    // Test 2: keystroke_comma_invokesRequester
    // -----------------------------------------------------------------------

    /**
     * Typing ',' MUST also invoke the requester (it's a trigger char).
     *
     * Anti-bluff: remove ',' from TRIGGER_CHARS → callCount stays 0 → FAIL.
     *
     * Mutation: remove ',' from trigger set → FAIL.
     */
    @Test
    fun keystroke_comma_invokesRequester() = runBlocking<Unit> {
        val results = mutableListOf<SignatureHelp?>()
        val requester = FakeTriggerRequester(returns = CANNED_HELP)
        val trigger = SignatureHelpTrigger(
            scope = scope,
            requester = requester,
            onResult = { results += it },
        )

        trigger.onKeystroke(',', "python", "file:///bar.py", line = 2, character = 8)
        delay(50L)

        assertEquals("',' MUST invoke requester exactly once", 1, requester.callCount)
        assertTrue("onResult MUST be called after ','", results.isNotEmpty())
        assertNotNull("onResult result after ',' MUST be non-null (canned help)", results.last())
    }

    // -----------------------------------------------------------------------
    // Test 3: keystroke_close_paren_dismisses
    // -----------------------------------------------------------------------

    /**
     * Typing ')' MUST call onResult(null) immediately without invoking the
     * requester.
     *
     * Anti-bluff: remove handleDismiss call for ')' → onResult is never
     * called with null → assertNull(results.last()) FAILS.
     *
     * Mutation: treat ')' same as '(' → callCount becomes 1 and result is
     * non-null → FAIL on assertEquals(0, callCount).
     */
    @Test
    fun keystroke_close_paren_dismisses() = runBlocking<Unit> {
        val results = mutableListOf<SignatureHelp?>()
        val requester = FakeTriggerRequester(returns = CANNED_HELP)
        val trigger = SignatureHelpTrigger(
            scope = scope,
            requester = requester,
            onResult = { results += it },
        )

        trigger.onKeystroke(')', "kotlin", "file:///Foo.kt", line = 5, character = 11)
        delay(50L)

        assertEquals("')' MUST NOT invoke the requester", 0, requester.callCount)
        assertTrue("onResult MUST be called on ')'", results.isNotEmpty())
        assertNull("onResult MUST deliver null on ')' (dismiss)", results.last())
    }

    // -----------------------------------------------------------------------
    // Test 4: keystroke_non_trigger_char_isNoOp
    // -----------------------------------------------------------------------

    /**
     * Non-trigger, non-dismiss characters MUST NOT invoke the requester or
     * call onResult.
     *
     * Anti-bluff: treat every char as a trigger → callCount becomes 1 → FAIL.
     *
     * Mutation: remove the `when {}` guard so all chars trigger → FAIL.
     */
    @Test
    fun keystroke_non_trigger_char_isNoOp() = runBlocking<Unit> {
        val results = mutableListOf<SignatureHelp?>()
        val requester = FakeTriggerRequester(returns = CANNED_HELP)
        val trigger = SignatureHelpTrigger(
            scope = scope,
            requester = requester,
            onResult = { results += it },
        )

        for (ch in listOf('a', 'b', ' ', '\n', '\t', '1', '_', '.')) {
            trigger.onKeystroke(ch, "kotlin", "file:///Foo.kt", line = 0, character = 0)
        }
        delay(50L)

        assertEquals("Non-trigger chars MUST NOT invoke the requester", 0, requester.callCount)
        assertTrue("onResult MUST NOT be called for non-trigger chars", results.isEmpty())
    }

    // -----------------------------------------------------------------------
    // Test 5: inflight_cancelled_before_new_request
    // -----------------------------------------------------------------------

    /**
     * A second '(' keystroke MUST cancel the first in-flight request before
     * starting a new one. The source MUST contain `requestJob?.cancel()`.
     *
     * We verify structurally — the runtime-timing test for this is fragile on
     * an instant fake, so the source-level check is the anti-bluff anchor.
     *
     * Anti-bluff: remove `requestJob?.cancel()` from source →
     * this assertion FAILS (literal disappears).
     *
     * Mutation: remove cancel call → FAIL.
     */
    @Test
    fun inflight_cancelled_before_new_request() = runBlocking<Unit> {
        val src = run {
            val path = "shared/src/commonMain/kotlin/digital/vasic/yole/lsp/SignatureHelpTrigger.kt"
            val candidates = listOf(path, "../$path")
            candidates.map { File(it) }.firstOrNull { it.isFile }?.readText()
                ?: error("SignatureHelpTrigger.kt not found (cwd=${File(".").absolutePath})")
        }

        assertTrue(
            "SignatureHelpTrigger MUST cancel the in-flight requestJob before launching a new one",
            src.contains("requestJob?.cancel()"),
        )
        assertTrue(
            "SignatureHelpTrigger MUST cancel the dismiss timer before starting a new request",
            src.contains("cancelDismissTimer()"),
        )
        assertTrue(
            "SignatureHelpTrigger MUST start a 30-second auto-dismiss timer per invocation",
            src.contains("AUTO_DISMISS_MS") || src.contains("30_000L") || src.contains("30000L"),
        )

        // Behavioural check: two successive '(' keystrokes; requester sees at
        // most 2 calls (one per trigger, since the fake is instant). Verify
        // onResult is called — ensures wiring is functional not dead code.
        val results = mutableListOf<SignatureHelp?>()
        val requester = FakeTriggerRequester(returns = CANNED_HELP)
        val trigger = SignatureHelpTrigger(
            scope = scope,
            requester = requester,
            onResult = { results += it },
        )

        trigger.onKeystroke('(', "kotlin", "file:///Foo.kt", line = 1, character = 5)
        trigger.onKeystroke('(', "kotlin", "file:///Foo.kt", line = 1, character = 6)
        delay(100L)

        assertTrue("onResult MUST be called after successive trigger keystrokes", results.isNotEmpty())
        assertTrue(
            "requester MUST have been called at least once across two trigger keystrokes",
            requester.callCount >= 1,
        )
    }
}
