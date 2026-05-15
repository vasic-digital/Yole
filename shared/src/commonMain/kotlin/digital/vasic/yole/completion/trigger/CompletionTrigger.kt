/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 5: CompletionTrigger — debounce + prefix-guard +
 * explicit-trigger state machine.
 *
 * Design decisions:
 *
 *   State machine — three orthogonal bits of mutable state are tracked:
 *     - isPopupOpen: whether the completion popup is currently visible.
 *     - isDismissed: whether onDismiss() was called and the prefix has
 *       not yet gone through a short→long transition to re-arm.
 *     - lastPrefixWasShort: tracks whether the previous text-change call
 *       had a prefix below minPrefixLengthForImplicit. Used to detect
 *       the short→long transition that re-arms after a dismiss.
 *
 *   Debounce — implemented with a `Job?` field (debounceJob). Each call
 *   to onTextChanged cancels the pending job (if any) and launches a new
 *   one that delays debounceMillis then emits. This gives O(1) cancellation
 *   per keystroke with no timer allocations beyond a single coroutine.
 *
 *   Explicit trigger — bypasses debounce and prefix guard entirely. Builds
 *   a context from the last text + cursor seen, emits Show immediately.
 *
 *   Dismiss — emits Hide and sets isDismissed=true. Suppresses implicit
 *   Show/Update until the prefix makes a short→long transition
 *   (prefix.length transitions from < minPrefix to ≥ minPrefix). That
 *   transition clears isDismissed and allows normal auto-open to resume.
 *
 *   SharedFlow — MutableSharedFlow(replay=0, extraBufferCapacity=16) so
 *   the emitter never suspends on a busy collector. replay=0 prevents
 *   stale events from being delivered to late subscribers.
 *
 *   CancellationException handling — the debounce coroutine is cancelled
 *   via job.cancel(), which is structured and does NOT require a catch.
 *   The scope's own cancellation propagates naturally (no catch needed).
 *
 *   Thread-safety — all mutable state is guarded by Mutex. The trigger
 *   is safe to call from any coroutine dispatcher.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure applied before commit —
 *   1. Set debounceMillis=0 in CompletionTriggerTest → intermediate Shows
 *      leaked; implicitTrigger_resetDebounceOnEachKeystroke FAILED. Reverted.
 *   2. Removed explicit-path bypass (routed through debounce) →
 *      explicitTrigger_firesShowImmediately_evenZeroPrefix FAILED. Reverted.
 *   3. Removed isDismissed flag →
 *      dismiss_thenImmediateRetype_doesNotAutoReopen FAILED. Reverted.
 *   4. Changed re-arm condition to never clear isDismissed →
 *      dismiss_thenLongerPrefixAfterShort_doesAutoReopen FAILED. Reverted.
 *   All tests GREEN after all reverts.
 *#######################################################*/
package digital.vasic.yole.completion.trigger

import digital.vasic.yole.completion.CompletionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// ---------------------------------------------------------------------------
// TriggerEvent — sealed hierarchy
// ---------------------------------------------------------------------------

/**
 * Events emitted by [CompletionTrigger] to drive the completion popup.
 *
 * - [Show] — open the popup and start a completion request.
 * - [Update] — update an already-open popup with a new context.
 * - [Hide] — close the popup.
 */
sealed class TriggerEvent {
    /** Open the popup and run completion for this context. */
    data class Show(val context: CompletionContext) : TriggerEvent()

    /** Update the popup's context (text/prefix changed while open). */
    data class Update(val context: CompletionContext) : TriggerEvent()

    /** Close the popup. */
    data object Hide : TriggerEvent()
}

// ---------------------------------------------------------------------------
// CompletionTrigger
// ---------------------------------------------------------------------------

/**
 * State machine that decides WHEN to open / update / close the completion
 * popup by emitting [TriggerEvent]s on [events].
 *
 * Wire up by:
 * 1. Calling [onTextChanged] on every editor text-change event.
 * 2. Calling [onExplicitTrigger] on Ctrl+Space or toolbar-tap.
 * 3. Calling [onDismiss] when the user closes the popup via Esc or tap-outside.
 * 4. Collecting [events] in the UI layer to drive popup open/close.
 *
 * @param langId language identifier passed into [CompletionContext.of]. Null = plaintext.
 * @param minPrefixLengthForImplicit minimum prefix length before an implicit
 *   (text-change-driven) trigger fires. Default 2. Explicit triggers bypass this guard.
 * @param debounceMillis milliseconds of keystroke quiescence before the implicit
 *   debounce fires. Default 80 ms. Set lower in tests for faster execution.
 * @param scope [CoroutineScope] that owns the debounce coroutine. Cancelling the
 *   scope cleanly cancels any pending debounce.
 */
class CompletionTrigger(
    private val langId: String?,
    private val minPrefixLengthForImplicit: Int = 2,
    private val debounceMillis: Long = 80L,
    private val scope: CoroutineScope,
) {

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Downstream popup collects this flow to drive open/update/close. */
    val events: SharedFlow<TriggerEvent> get() = _events

    // -----------------------------------------------------------------------
    // Internal state
    // -----------------------------------------------------------------------

    private val _events = MutableSharedFlow<TriggerEvent>(
        replay = 0,
        extraBufferCapacity = 16,
    )

    /** Protects all mutable state below. */
    private val mutex = Mutex()

    /** Pending debounce job; cancelled when a new keystroke arrives. */
    private var debounceJob: Job? = null

    /** Whether the completion popup is currently open. */
    private var isPopupOpen: Boolean = false

    /**
     * Whether the popup was explicitly dismissed. Suppresses implicit
     * triggers until a short→long prefix transition re-arms this flag.
     */
    private var isDismissed: Boolean = false

    /**
     * Whether the last [onTextChanged] call produced a prefix shorter than
     * [minPrefixLengthForImplicit]. Tracks the transition edge that re-arms
     * the trigger after a dismiss.
     */
    private var lastPrefixWasShort: Boolean = true

    /** Latest text + cursor, kept for explicit-trigger context building. */
    private var lastText: String = ""
    private var lastCursorChar: Int = 0

    // -----------------------------------------------------------------------
    // Public methods
    // -----------------------------------------------------------------------

    /**
     * Called on every text / cursor change in the editor.
     *
     * Logic:
     * - Build a [CompletionContext] from [newText] + [cursorChar].
     * - If prefix.length < [minPrefixLengthForImplicit]: emit [TriggerEvent.Hide]
     *   immediately, cancel any pending debounce, and track `lastPrefixWasShort`.
     *   If previously dismissed and now short, mark that we are in a short state
     *   so that the next long prefix re-arms.
     * - Else (prefix.length ≥ minPrefix):
     *   - If dismissed AND the previous prefix was NOT short (no short→long transition):
     *     suppress. Do nothing.
     *   - If dismissed AND the previous prefix WAS short (short→long transition):
     *     clear dismissed flag — the user has typed back past the threshold.
     *   - Reset debounce: cancel pending job, launch new one. After [debounceMillis]
     *     of quiescence, emit [TriggerEvent.Show] or [TriggerEvent.Update].
     */
    fun onTextChanged(newText: String, cursorChar: Int) {
        scope.launch {
            val ctx = CompletionContext.of(newText, cursorChar, langId)

            mutex.withLock {
                lastText = newText
                lastCursorChar = cursorChar

                if (ctx.prefix.length < minPrefixLengthForImplicit) {
                    // Short prefix — emit Hide, cancel debounce, mark short state.
                    debounceJob?.cancel()
                    debounceJob = null
                    val wasOpen = isPopupOpen
                    isPopupOpen = false
                    // If dismissed, being in short state means next long prefix re-arms.
                    lastPrefixWasShort = true
                    if (wasOpen || !isDismissed) {
                        // Always emit Hide when prefix drops below threshold
                        // (whether popup was open or not, so the UI can clean up).
                        emitHideUnlocked()
                    }
                    return@withLock
                }

                // Prefix is ≥ minPrefix.
                val wasDismissedAndNoTransition = isDismissed && !lastPrefixWasShort
                val isTransitionFromShort = isDismissed && lastPrefixWasShort
                lastPrefixWasShort = false

                if (isTransitionFromShort) {
                    // short→long transition clears the dismissed flag — re-arm.
                    isDismissed = false
                }

                if (wasDismissedAndNoTransition) {
                    // Still dismissed, no short→long transition → suppress.
                    return@withLock
                }

                // Start / reset debounce.
                debounceJob?.cancel()
                val capturedCtx = ctx
                val capturedIsOpen = isPopupOpen
                debounceJob = scope.launch {
                    delay(debounceMillis)
                    mutex.withLock {
                        isPopupOpen = true
                        if (capturedIsOpen) {
                            _events.emit(TriggerEvent.Update(capturedCtx))
                        } else {
                            _events.emit(TriggerEvent.Show(capturedCtx))
                        }
                    }
                }
            }
        }
    }

    /**
     * Called on Ctrl+Space or toolbar tap (explicit trigger).
     *
     * Bypasses the prefix-length guard and debounce. Emits [TriggerEvent.Show]
     * immediately (within the current coroutine dispatch cycle). Also clears the
     * dismissed flag so the popup can open even after a prior dismiss.
     */
    fun onExplicitTrigger() {
        scope.launch {
            val ctx: CompletionContext
            mutex.withLock {
                debounceJob?.cancel()
                debounceJob = null
                isDismissed = false
                isPopupOpen = true
                lastPrefixWasShort = lastText.isEmpty() || CompletionContext.of(lastText, lastCursorChar, langId).prefix.length < minPrefixLengthForImplicit
                ctx = CompletionContext.of(lastText, lastCursorChar, langId)
            }
            _events.emit(TriggerEvent.Show(ctx))
        }
    }

    /**
     * Called when the user closes the popup (Esc, tap-outside).
     *
     * Emits [TriggerEvent.Hide] and sets [isDismissed] = true, suppressing
     * further implicit triggers until a short→long prefix transition occurs.
     */
    fun onDismiss() {
        scope.launch {
            mutex.withLock {
                debounceJob?.cancel()
                debounceJob = null
                isPopupOpen = false
                isDismissed = true
                // lastPrefixWasShort retains its current value so that the next
                // onTextChanged can detect the short→long transition correctly.
            }
            _events.emit(TriggerEvent.Hide)
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers (must be called with mutex held)
    // -----------------------------------------------------------------------

    /** Emit Hide without acquiring the mutex (caller must hold it). */
    private suspend fun emitHideUnlocked() {
        // Release mutex before emitting to avoid re-entrancy if the collector
        // calls back into the trigger synchronously. We use a side-channel:
        // emit outside the lock by scheduling via scope.launch.
        scope.launch { _events.emit(TriggerEvent.Hide) }
    }
}
