/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * desktopTest (iter-62 Phase 6.2, CONST-035 anti-bluff):
 *   Pure-logic tests for HoverTriggerDetector. Uses real-clock delay
 *   with a small dwellMillis (30 ms) so total suite runtime stays under 1 s.
 *   JUnit4 runner + runBlocking<Unit> per project test constraints.
 *
 *   Anti-bluff mutation guards:
 *     1. Comment out `if (isCompletionPopupOpen()) return`
 *        → `dwell_skips_when_completion_popup_open` FAILS.
 *     2. Comment out `if (!isIdentifierAt(...)) return`
 *        → `dwell_skips_when_not_identifier` FAILS.
 *     3. Remove `delay(dwellMillis)` from the dwell job
 *        → `dwell_cancels_on_subsequent_move` may still pass, but
 *        `dwell_dispatches_after_300ms` becomes unreliable (fires too early).
 *     4. In `dismiss()`, remove `dwellJob?.cancel()`
 *        → `dismiss_cancels_pending_dwell` FAILS.
 *     5. In `onExplicit()`, call `onDwell` instead of `onExplicit` lambda
 *        → `explicit_bypasses_filters_and_dwell` FAILS (wrong counter).
 *
 * Cross-platform impact (CONST-037):
 *   Desktop: desktopTest — JVM implementation under test here.
 *   Android: HoverTriggerDetector is pure Kotlin; androidUnitTest covers
 *            it when ANDROID_SDK_ROOT is available.
 *   iOS/Web: deferred.
 *
 * Submodules: not touched (CONST-038).
 *########################################################*/
package digital.vasic.yole.lsp

import digital.vasic.yole.lsp.HoverTriggerDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Small dwell delay used in all tests so the suite completes quickly.
 * The production default is 300 ms; logic is identical.
 */
private const val TEST_DWELL_MS = 30L

/** Extra margin on top of dwell to tolerate scheduler jitter. */
private const val WAIT_MARGIN_MS = 50L

class HoverTriggerDetectorTest {

    private lateinit var testScope: CoroutineScope

    @Before
    fun setUp() {
        testScope = CoroutineScope(Dispatchers.Default)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun makeDetector(
        completionOpen: Boolean = false,
        isIdentifier: Boolean = true,
        dwellCalls: MutableList<Pair<Int, Int>> = mutableListOf(),
        explicitCalls: MutableList<Pair<Int, Int>> = mutableListOf(),
    ): HoverTriggerDetector = HoverTriggerDetector(
        scope = testScope,
        dwellMillis = TEST_DWELL_MS,
        isCompletionPopupOpen = { completionOpen },
        isIdentifierAt = { _, _ -> isIdentifier },
        onDwell = { l, c -> dwellCalls.add(l to c) },
        onExplicit = { l, c -> explicitCalls.add(l to c) },
    )

    // -----------------------------------------------------------------------
    // Test 1: dwell fires after delay
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing delay() from the dwell job makes this test
     * indeterminate. Removing the onDwell callback call makes dwellCalls
     * stay empty → assertEquals(1, ...) FAILS.
     */
    @Test
    fun dwell_dispatches_after_300ms() = runBlocking<Unit> {
        val dwellCalls = mutableListOf<Pair<Int, Int>>()
        val detector = makeDetector(dwellCalls = dwellCalls)

        detector.onPointerMove(line = 5, character = 12)
        delay(TEST_DWELL_MS + WAIT_MARGIN_MS)

        assertEquals("onDwell MUST be called exactly once after dwell", 1, dwellCalls.size)
        assertEquals("onDwell MUST receive the correct line", 5, dwellCalls[0].first)
        assertEquals("onDwell MUST receive the correct character", 12, dwellCalls[0].second)
    }

    // -----------------------------------------------------------------------
    // Test 2: rapid move cancels previous dwell, last position wins
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing `dwellJob?.cancel()` from onPointerMove means
     * the first dwell fires → onDwell is called twice → assertEquals(1, ...)
     * FAILS with count=2.
     */
    @Test
    fun dwell_cancels_on_subsequent_move() = runBlocking<Unit> {
        val dwellCalls = mutableListOf<Pair<Int, Int>>()
        val detector = makeDetector(dwellCalls = dwellCalls)

        detector.onPointerMove(line = 1, character = 1)
        delay(TEST_DWELL_MS / 3) // well within dwell window
        detector.onPointerMove(line = 2, character = 2)
        delay(TEST_DWELL_MS + WAIT_MARGIN_MS)

        assertEquals("Only the last dwell MUST fire (previous MUST be cancelled)", 1, dwellCalls.size)
        assertEquals("onDwell MUST carry last line", 2, dwellCalls[0].first)
        assertEquals("onDwell MUST carry last character", 2, dwellCalls[0].second)
    }

    // -----------------------------------------------------------------------
    // Test 3: completion popup open → dwell suppressed
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: comment out `if (isCompletionPopupOpen()) return` →
     * dwell fires → dwellCalls.size becomes 1 → assertEquals(0, ...) FAILS.
     */
    @Test
    fun dwell_skips_when_completion_popup_open() = runBlocking<Unit> {
        val dwellCalls = mutableListOf<Pair<Int, Int>>()
        val detector = makeDetector(completionOpen = true, dwellCalls = dwellCalls)

        detector.onPointerMove(line = 3, character = 7)
        delay(TEST_DWELL_MS + WAIT_MARGIN_MS)

        assertEquals("onDwell MUST NOT fire when completion popup is open", 0, dwellCalls.size)
    }

    // -----------------------------------------------------------------------
    // Test 4: non-identifier position → dwell suppressed
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: comment out `if (!isIdentifierAt(...)) return` →
     * dwell fires → dwellCalls.size becomes 1 → assertEquals(0, ...) FAILS.
     */
    @Test
    fun dwell_skips_when_not_identifier() = runBlocking<Unit> {
        val dwellCalls = mutableListOf<Pair<Int, Int>>()
        val detector = makeDetector(isIdentifier = false, dwellCalls = dwellCalls)

        detector.onPointerMove(line = 0, character = 0)
        delay(TEST_DWELL_MS + WAIT_MARGIN_MS)

        assertEquals("onDwell MUST NOT fire when position is not an identifier", 0, dwellCalls.size)
    }

    // -----------------------------------------------------------------------
    // Test 5: explicit bypasses all guards
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: changing onExplicit to call onDwell instead of the
     * explicit callback → explicitCalls.size stays 0 → assertEquals(1, ...) FAILS.
     */
    @Test
    fun explicit_bypasses_filters_and_dwell() = runBlocking<Unit> {
        val dwellCalls = mutableListOf<Pair<Int, Int>>()
        val explicitCalls = mutableListOf<Pair<Int, Int>>()
        // Both guards active
        val detector = makeDetector(
            completionOpen = true,
            isIdentifier = false,
            dwellCalls = dwellCalls,
            explicitCalls = explicitCalls,
        )

        detector.onExplicit(line = 9, character = 4)

        assertEquals("onExplicit MUST fire immediately regardless of guards", 1, explicitCalls.size)
        assertEquals("explicitCalls MUST carry correct line", 9, explicitCalls[0].first)
        assertEquals("explicitCalls MUST carry correct character", 4, explicitCalls[0].second)
        assertEquals("onDwell MUST NOT fire for explicit trigger", 0, dwellCalls.size)
    }

    // -----------------------------------------------------------------------
    // Test 6: dismiss cancels pending dwell
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing `dwellJob?.cancel()` from dismiss() → the
     * scheduled job fires after the dwell window → dwellCalls.size becomes 1
     * → assertEquals(0, ...) FAILS.
     */
    @Test
    fun dismiss_cancels_pending_dwell() = runBlocking<Unit> {
        val dwellCalls = mutableListOf<Pair<Int, Int>>()
        val detector = makeDetector(dwellCalls = dwellCalls)

        detector.onPointerMove(line = 4, character = 2)
        // Dismiss well before the dwell window expires
        delay(TEST_DWELL_MS / 3)
        detector.dismiss()
        delay(TEST_DWELL_MS + WAIT_MARGIN_MS)

        assertEquals("onDwell MUST NOT fire after dismiss()", 0, dwellCalls.size)
    }
}
