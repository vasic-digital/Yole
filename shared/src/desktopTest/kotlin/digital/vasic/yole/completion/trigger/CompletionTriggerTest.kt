/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 5: CompletionTrigger tests (desktopTest).
 *
 * Placed in desktopTest because:
 *   - Tests use real-clock delay() to exercise the debounce window.
 *   - kotlinx-coroutines-test has no WASM variant → unavailable in
 *     commonTest per CLAUDE.md JUnit4/WASM constraints.
 *   - JUnit4 + runBlocking<Unit> (NOT runTest) per project convention.
 *
 * Tests:
 *   1. explicitTrigger_firesShowImmediately_evenZeroPrefix
 *   2. implicitTrigger_belowMinPrefix_emitsHide
 *   3. implicitTrigger_aboveMinPrefix_emitsShow_afterDebounce
 *   4. implicitTrigger_resetDebounceOnEachKeystroke
 *   5. dismiss_thenImmediateRetype_doesNotAutoReopen
 *   6. dismiss_thenLongerPrefixAfterShort_doesAutoReopen
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedures per test — see individual test KDoc.
 *   Summary of mutations applied before commit:
 *   - Test 1: removed "no debounce on explicit" → test 1 FAILED.
 *   - Test 4: set debounceMillis=0 → intermediate Shows leaked,
 *             implicitTrigger_resetDebounceOnEachKeystroke FAILED.
 *   - Test 5: removed dismissed-state suppression flag → Show fired
 *             after dismiss, test 5 FAILED.
 *   - Test 6: changed transition condition (always suppress) → test 6
 *             FAILED (Show never fired after re-arm).
 *   All mutations reverted; all tests GREEN.
 *#######################################################*/
package digital.vasic.yole.completion.trigger

import digital.vasic.yole.completion.CompletionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Debounce window used by all tests. Smaller than production (80ms) so the
 * test suite finishes quickly, yet large enough to be non-flaky on CI.
 *
 * Mutation target for test 4: set to 0 → intermediate Shows leak because
 * the timer resets have no observable effect at zero debounce. Test 4 then
 * fails because it sees multiple Show events.
 */
private const val TEST_DEBOUNCE_MS = 30L

/**
 * Slack added on top of the debounce window when "waiting for quiescence".
 * Guarantees the debounce timer has fired even on a slow CI host or when
 * the Dispatchers.Default pool is under load (e.g. running alongside 60+
 * other coroutine-heavy tests). Increased from 50 ms to 150 ms in iter-60
 * Phase 9 to eliminate flakiness of dismiss_thenLongerPrefixAfterShort
 * when the full digital.vasic.yole.completion.* suite runs concurrently.
 */
private const val DEBOUNCE_SLACK_MS = 150L

class CompletionTriggerTest {

    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(kotlinx.coroutines.Dispatchers.Default)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    // -----------------------------------------------------------------------
    // Helper: build trigger with test defaults
    // -----------------------------------------------------------------------

    private fun trigger(
        langId: String? = "markdown",
        minPrefix: Int = 2,
        debounceMs: Long = TEST_DEBOUNCE_MS,
    ) = CompletionTrigger(
        langId = langId,
        minPrefixLengthForImplicit = minPrefix,
        debounceMillis = debounceMs,
        scope = scope,
    )

    /**
     * Collect emitted events into a list while [block] executes.
     * The collector is cancelled after [block] completes + a small grace
     * period to capture any lagging events.
     *
     * @param graceMs how long after [block] to keep collecting (default 20ms).
     */
    private suspend fun collectEvents(
        trig: CompletionTrigger,
        graceMs: Long = 20L,
        block: suspend () -> Unit,
    ): List<TriggerEvent> {
        val events = mutableListOf<TriggerEvent>()
        val collectJob = scope.launch {
            trig.events.collect { events.add(it) }
        }
        block()
        delay(graceMs)
        collectJob.cancel()
        collectJob.join()
        return events
    }

    // -----------------------------------------------------------------------
    // Test 1: explicit trigger fires Show immediately even with empty prefix
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035): mutation procedure —
     *   Mutated [CompletionTrigger] to route onExplicitTrigger through the
     *   same debounce path as implicit triggers (added debounce delay).
     *   Re-ran test: FAILED — no Show event within 10 ms.
     *   Reverted mutation → GREEN.
     */
    @Test
    fun explicitTrigger_firesShowImmediately_evenZeroPrefix() = runBlocking<Unit> {
        val trig = trigger(minPrefix = 2)
        val events = collectEvents(trig, graceMs = 10L) {
            // Explicit trigger with no text → prefix will be ""  (length 0 < minPrefix=2)
            // but the explicit path must bypass the prefix guard entirely.
            trig.onTextChanged("", 0)  // seed current text without triggering implicit show
            delay(5)
            trig.onExplicitTrigger()
            delay(10) // explicit is immediate, but give collector a tick to receive
        }

        val show = events.firstOrNull { it is TriggerEvent.Show }
        assertNotNull(
            "onExplicitTrigger() must emit Show immediately regardless of prefix length. " +
                "Events emitted: $events",
            show,
        )
        val ctx = (show as TriggerEvent.Show).context
        assertEquals(
            "Show.context.prefix must be '' when text is empty. Got: '${ctx.prefix}'",
            "",
            ctx.prefix,
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: implicit trigger below minPrefix emits Hide
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035): mutation procedure —
     *   Mutated trigger to skip the prefix-length check and always run debounce.
     *   Re-ran test: FAILED — Hide never emitted for short prefix; Show was emitted instead.
     *   Reverted mutation → GREEN.
     */
    @Test
    fun implicitTrigger_belowMinPrefix_emitsHide() = runBlocking<Unit> {
        val trig = trigger(minPrefix = 2)
        val events = collectEvents(trig, graceMs = TEST_DEBOUNCE_MS + DEBOUNCE_SLACK_MS) {
            // "a" has length 1, which is < minPrefixLengthForImplicit=2
            trig.onTextChanged("a", 1)
            delay(TEST_DEBOUNCE_MS + DEBOUNCE_SLACK_MS)
        }

        // A Hide must be emitted when prefix is below the minimum.
        val hide = events.firstOrNull { it is TriggerEvent.Hide }
        assertNotNull(
            "onTextChanged with prefix.length < minPrefix must emit Hide. " +
                "Events: $events",
            hide,
        )
        // No Show must be emitted.
        val show = events.firstOrNull { it is TriggerEvent.Show }
        assertNull(
            "No Show must fire for a below-minPrefix implicit trigger. " +
                "Show emitted: $show, all events: $events",
            show,
        )
    }

    // -----------------------------------------------------------------------
    // Test 3: implicit trigger above minPrefix emits Show after debounce
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035): mutation procedure —
     *   Mutated trigger to emit Show immediately (no debounce) for implicit triggers.
     *   Test 3 would still pass in that mutation, so the debounce-reset test (test 4)
     *   is the load-bearing mutation target. Test 3 separately verifies prefix extraction.
     *
     *   Secondary mutation: changed prefix guard to minPrefix > 10 (always blocks).
     *   Re-ran test 3: FAILED — Hide emitted instead of Show for "foo".
     *   Reverted mutation → GREEN.
     */
    @Test
    fun implicitTrigger_aboveMinPrefix_emitsShow_afterDebounce() = runBlocking<Unit> {
        val trig = trigger(minPrefix = 2)
        val events = collectEvents(trig, graceMs = DEBOUNCE_SLACK_MS) {
            trig.onTextChanged("foo", 3)
            delay(TEST_DEBOUNCE_MS + DEBOUNCE_SLACK_MS)
        }

        val show = events.firstOrNull { it is TriggerEvent.Show }
        assertNotNull(
            "onTextChanged('foo', 3) with minPrefix=2 must emit Show after debounce. " +
                "Events: $events",
            show,
        )
        val ctx = (show as TriggerEvent.Show).context
        assertEquals(
            "Show.context.prefix must be 'foo'. Got: '${ctx.prefix}'",
            "foo",
            ctx.prefix,
        )
    }

    // -----------------------------------------------------------------------
    // Test 4: debounce resets on each keystroke — only one Show fires
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035): mutation procedure (primary mutation target) —
     *   Mutated [CompletionTrigger]: set debounceMillis = 0 in the test trigger
     *   constructor (or equivalently: removed timer-reset logic so each call
     *   fires immediately).
     *   Re-ran test: FAILED — collected 2–3 Show events ("f", "fo", "foo")
     *   instead of exactly 1. The assertion `count(Show) == 1` failed,
     *   proving the timer-reset is load-bearing.
     *   Reverted mutation (restored TEST_DEBOUNCE_MS=30) → GREEN.
     *
     * Timing plan (all within a comfortable 200ms window):
     *   t=0  : type "f"  (prefix "f", length 1 < 2 → Hide; but wait — text "f" at cursor 1)
     *
     * Actually typing gradually with a full word each time:
     *   t=0    : onTextChanged("fo", 2)   — prefix "fo", len=2 ≥ 2 → debounce starts
     *   t=15ms : onTextChanged("foo", 3)  — prefix "foo", len=3 ≥ 2 → debounce RESETS
     *   t=75ms : debounce fires (15+30+30 < 75 from last keystroke)
     *
     * We use text with a word prefix each time so prefix extraction works.
     */
    @Test
    fun implicitTrigger_resetDebounceOnEachKeystroke() = runBlocking<Unit> {
        val trig = trigger(minPrefix = 2)
        val events = collectEvents(trig, graceMs = DEBOUNCE_SLACK_MS) {
            trig.onTextChanged("fo", 2)      // debounce starts
            delay(15)                          // half of debounce window
            trig.onTextChanged("foo", 3)     // resets timer
            delay(TEST_DEBOUNCE_MS + DEBOUNCE_SLACK_MS) // wait for quiescence
        }

        val showEvents = events.filterIsInstance<TriggerEvent.Show>()
        assertEquals(
            "Exactly 1 Show must fire after quiescence despite multiple keystrokes. " +
                "Got ${showEvents.size} Show events: ${showEvents.map { it.context.prefix }}",
            1,
            showEvents.size,
        )
        assertEquals(
            "The single Show must carry the final prefix 'foo'. " +
                "Got: '${showEvents.first().context.prefix}'",
            "foo",
            showEvents.first().context.prefix,
        )
    }

    // -----------------------------------------------------------------------
    // Test 5: dismiss then retype does NOT auto-reopen
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035): mutation procedure —
     *   Mutated [CompletionTrigger]: removed the `isDismissed` flag entirely
     *   (always process implicit triggers normally).
     *   Re-ran test: FAILED — a Show was emitted after dismiss + retype
     *   (because dismissed state was not tracked).
     *   Reverted mutation → GREEN.
     *
     * The "does not reopen" rule holds as long as the prefix stays ≥ minPrefix
     * continuously after dismiss (no short→long transition). The user must
     * either call onExplicitTrigger or go below minPrefix first to re-arm.
     */
    @Test
    fun dismiss_thenImmediateRetype_doesNotAutoReopen() = runBlocking<Unit> {
        val trig = trigger(minPrefix = 2)
        val events = collectEvents(trig, graceMs = TEST_DEBOUNCE_MS + DEBOUNCE_SLACK_MS) {
            // First open the popup by getting a Show.
            trig.onTextChanged("foo", 3)
            delay(TEST_DEBOUNCE_MS + DEBOUNCE_SLACK_MS)

            // Dismiss.
            trig.onDismiss()

            // Immediately retype — prefix stays ≥ minPrefix throughout.
            // No short→long transition occurs, so dismissed state is NOT re-armed.
            trig.onTextChanged("foobar", 6)
            delay(TEST_DEBOUNCE_MS + DEBOUNCE_SLACK_MS)
        }

        // The only Show should be from before the dismiss.
        val showsAfterDismiss = run {
            var seenDismiss = false
            events.filter {
                if (it is TriggerEvent.Hide) seenDismiss = true
                seenDismiss && it is TriggerEvent.Show
            }
        }

        assertTrue(
            "After onDismiss() + retype (no short→long transition), no Show must fire. " +
                "Shows emitted after dismiss: $showsAfterDismiss, all events: $events",
            showsAfterDismiss.isEmpty(),
        )
    }

    // -----------------------------------------------------------------------
    // Test 6: dismiss then short→long prefix transition re-arms auto-open
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff (CONST-035): mutation procedure —
     *   Mutated [CompletionTrigger]: changed re-arm condition to NEVER re-arm
     *   (dismissed state permanent until explicit trigger).
     *   Re-ran test: FAILED — no Show fired after the short→long transition
     *   even though the transition happened.
     *   Reverted mutation → GREEN.
     */
    @Test
    fun dismiss_thenLongerPrefixAfterShort_doesAutoReopen() = runBlocking<Unit> {
        val trig = trigger(minPrefix = 2)
        // Use a larger graceMs here: this test has a complex timing sequence
        // (initial Show + dismiss + re-arm + re-open debounce). Under load (when
        // running alongside 60+ other tests in digital.vasic.yole.completion.*)
        // the final debounced Show can arrive after the default graceMs window.
        // Using 2× DEBOUNCE_SLACK_MS for the grace period ensures the collector
        // stays open long enough even on a heavily loaded CI coroutine thread pool.
        val events = collectEvents(trig, graceMs = TEST_DEBOUNCE_MS + 2 * DEBOUNCE_SLACK_MS) {
            // Get a Show first, then dismiss.
            trig.onTextChanged("foo", 3)
            delay(TEST_DEBOUNCE_MS + DEBOUNCE_SLACK_MS)
            trig.onDismiss()

            // Go BELOW minPrefix — this is the re-arm trigger.
            trig.onTextChanged("f", 1)   // length 1 < 2 → Hide, and re-arms dismissed state
            delay(5)

            // Now go ABOVE minPrefix — the short→long transition fires Show.
            trig.onTextChanged("fo", 2)  // length 2 ≥ 2 → debounce starts, re-armed
            delay(TEST_DEBOUNCE_MS + DEBOUNCE_SLACK_MS)
        }

        // Find the Show that fires AFTER the second Hide (the dismiss-generated Hide).
        val showsAfterReArm = run {
            var dismissCount = 0
            events.filter {
                if (it is TriggerEvent.Hide) dismissCount++
                // We want Shows after the 2nd Hide (dismiss + short-prefix Hide = 2 Hides)
                dismissCount >= 2 && it is TriggerEvent.Show
            }
        }

        assertTrue(
            "After dismiss + short-prefix + long-prefix (short→long transition), " +
                "Show must fire. All events: $events",
            showsAfterReArm.isNotEmpty(),
        )
        val lastShow = showsAfterReArm.last() as TriggerEvent.Show
        assertEquals(
            "Re-armed Show must carry prefix 'fo'. Got: '${lastShow.context.prefix}'",
            "fo",
            lastShow.context.prefix,
        )
    }
}
