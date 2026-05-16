/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 7: EditorNavigationStack unit tests (commonTest).
 *
 * 6 tests cover: empty-pop, push/pop round-trip, capacity eviction,
 * consecutive-duplicate suppression, non-consecutive allowed, canGoBack.
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub pop() to always return null.
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.EditorNavigationStackTests"
 *   3. Expect FAIL: push_then_pop_returns_same_entry (null ≠ NavEntry).
 *              FAIL: canGoBack_reflects_state (canGoBack() false after pop).
 *   4. Revert; confirm all 6 GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Common: pure logic, runs on all targets.
 *   - Desktop/Android: covered by :shared:desktopTest / androidUnitTest.
 *   - iOS/Web: shared commonTest compilation covers those targets too.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorNavigationStackTests {

    /**
     * An empty stack MUST return null on pop — no exception.
     *
     * Mutation: if pop() returned a sentinel entry, this assertion would FAIL.
     */
    @Test
    fun empty_pop_returns_null() {
        val stack = EditorNavigationStack()
        assertNull(stack.pop(), "pop on empty stack must be null")
    }

    /**
     * After pushing one entry, pop() MUST return the same entry.
     *
     * Mutation: stub pop() to return null → FAIL.
     */
    @Test
    fun push_then_pop_returns_same_entry() {
        val stack = EditorNavigationStack()
        val entry = NavEntry(uri = "file:///src/Foo.kt", cursorOffset = 42)
        stack.push(entry)
        val result = stack.pop()
        assertEquals(entry, result, "popped entry must equal pushed entry")
        assertEquals(0, stack.size, "stack must be empty after single pop")
    }

    /**
     * Pushing more entries than the cap MUST evict the oldest so that
     * size == cap and the earliest entry is gone.
     *
     * Mutation: removing the eviction logic (entries.removeFirst()) would make
     * size > cap and the first-pushed entry would still be present → FAIL.
     */
    @Test
    fun cap_drops_oldest() {
        val cap = 100
        val stack = EditorNavigationStack(maxEntries = cap)
        // Push cap + 5 entries; first 5 should be evicted.
        for (i in 0 until (cap + 5)) {
            stack.push(NavEntry("file:///f$i.kt", i))
        }
        assertEquals(cap, stack.size, "size must equal cap after overflow")
        // Pop all; the LAST entry added must be at the top.
        val top = stack.peek()
        assertEquals(NavEntry("file:///f${cap + 4}.kt", cap + 4), top, "youngest entry must be at top")
        // The oldest entry (i=0..4) must be gone — verify by draining.
        val all = mutableListOf<NavEntry>()
        repeat(cap) { all.add(stack.pop()!!) }
        val uris = all.map { it.uri }.toSet()
        for (i in 0 until 5) {
            assertFalse(uris.contains("file:///f$i.kt"), "evicted entry f$i must not be present")
        }
    }

    /**
     * Pushing the same entry twice consecutively MUST result in only one
     * entry stored (duplicate suppression).
     *
     * Mutation: removing the duplicate-suppression guard makes size == 2 → FAIL.
     */
    @Test
    fun consecutive_duplicate_suppressed() {
        val stack = EditorNavigationStack()
        val entry = NavEntry("file:///Dup.kt", 10)
        stack.push(entry)
        stack.push(entry) // identical — must be suppressed
        assertEquals(1, stack.size, "consecutive duplicate must be suppressed")
    }

    /**
     * Pushing A, B, A (non-consecutive duplicate) MUST store all three.
     *
     * Mutation: if the suppression guard compared any previous occurrence
     * (not just the top), this would incorrectly drop the second 'A' → FAIL.
     */
    @Test
    fun non_consecutive_duplicate_allowed() {
        val stack = EditorNavigationStack()
        val a = NavEntry("file:///A.kt", 0)
        val b = NavEntry("file:///B.kt", 5)
        stack.push(a)
        stack.push(b)
        stack.push(a) // non-consecutive — must be allowed
        assertEquals(3, stack.size, "non-consecutive duplicate must be stored")
    }

    /**
     * canGoBack() MUST reflect stack emptiness: false when empty, true after
     * push, false again after all entries are popped.
     *
     * Mutation: stub pop() to return null → after simulated pop the stack is
     * still non-empty, so canGoBack() remains true → assertion FAILS.
     */
    @Test
    fun canGoBack_reflects_state() {
        val stack = EditorNavigationStack()
        assertFalse(stack.canGoBack(), "empty stack: canGoBack must be false")
        stack.push(NavEntry("file:///F.kt", 1))
        assertTrue(stack.canGoBack(), "after push: canGoBack must be true")
        stack.pop()
        assertFalse(stack.canGoBack(), "after pop to empty: canGoBack must be false")
    }
}
