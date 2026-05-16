/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-62 Phase 7: EditorNavigationStack — back-navigation history.
 *
 * Maintains a bounded circular-style deque of [NavEntry] values so the
 * editor can navigate backwards after a go-to-definition jump.
 *
 * Design invariants:
 *   - Consecutive-duplicate entries are suppressed (same URI + offset pushed
 *     twice → only one entry stored).
 *   - Non-consecutive duplicates (A, B, A) are all stored.
 *   - Capacity is enforced by dropping the oldest entry on overflow.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub pop() to always return null.
 *   2. Run: ./gradlew :shared:desktopTest \
 *        --tests "digital.vasic.yole.lsp.EditorNavigationStackTests"
 *   3. Expect: push_then_pop_returns_same_entry FAILS (got null instead of entry).
 *              canGoBack_reflects_state FAILS (canGoBack returns false after push).
 *   4. Revert; confirm all 6 tests PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Common:  this class is pure Kotlin — no platform APIs used.
 *   - Android: accessible from IdeEditorScreen wiring (Phase 8).
 *   - Desktop: accessible from IdeEditorScreen wiring (Phase 8).
 *   - iOS:     accessible; Phase 8 iOS wiring deferred.
 *   - Web:     accessible; Phase 8 Web wiring deferred.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

/**
 * A single navigation history entry produced before a go-to-definition jump.
 *
 * @param uri          Document URI of the file the user was in BEFORE the jump.
 * @param cursorOffset Cursor position (character offset) BEFORE the jump.
 */
data class NavEntry(val uri: String, val cursorOffset: Int)

/**
 * Bounded back-navigation stack for the editor's go-to-definition feature.
 *
 * Entries are pushed before each go-to-definition jump so the user can
 * return to the previous cursor location. The stack is capped at [maxEntries]
 * to prevent unbounded memory growth; oldest entries are evicted on overflow.
 *
 * @param maxEntries Maximum number of entries retained (default [DEFAULT_MAX_ENTRIES]).
 */
class EditorNavigationStack(maxEntries: Int = DEFAULT_MAX_ENTRIES) {

    companion object {
        /** Default maximum entries the stack retains before evicting the oldest. */
        const val DEFAULT_MAX_ENTRIES: Int = 100
    }

    private val entries: ArrayDeque<NavEntry> = ArrayDeque()
    private val cap: Int = maxEntries

    /** Current number of entries in the stack. */
    val size: Int get() = entries.size

    /**
     * Push [entry] onto the stack.
     *
     * If [entry] equals the top-most entry (consecutive duplicate), the push
     * is silently suppressed to avoid cluttering the history with repeated
     * jumps to the same location.
     *
     * If the stack has reached [cap], the oldest entry is removed before the
     * new one is added.
     */
    fun push(entry: NavEntry) {
        if (entries.lastOrNull() == entry) return
        entries.addLast(entry)
        if (entries.size > cap) entries.removeFirst()
    }

    /**
     * Remove and return the most-recently pushed entry, or `null` if the
     * stack is empty.
     */
    fun pop(): NavEntry? = entries.removeLastOrNull()

    /**
     * Return the most-recently pushed entry without removing it, or `null`
     * if the stack is empty.
     */
    fun peek(): NavEntry? = entries.lastOrNull()

    /** Returns `true` if there is at least one entry to navigate back to. */
    fun canGoBack(): Boolean = entries.isNotEmpty()

    /** Remove all entries from the stack. */
    fun clear() { entries.clear() }
}
