/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-62 Phase 6.2: HoverTriggerDetector — decides when to fire a
 * hover request based on pointer dwell time, completion-popup state,
 * and identifier presence at the hovered position.
 *
 * Design:
 *   - Dwell path: onPointerMove → cancel pending job → launch new job
 *     with delay(dwellMillis) → onDwell(line, character).
 *   - Guards: skip if completion popup open OR not an identifier.
 *   - Explicit path: bypasses all guards + dwell delay; fires immediately.
 *   - dismiss(): cancels any pending dwell job.
 *
 * Lives in commonMain so it is available to both Android UI and Desktop
 * integration (Phase 8) without duplication. Has no platform dependencies.
 *
 * Coroutine cancellation: dwellJob?.cancel() is intentional; delay()
 * inside the job throws CancellationException which propagates normally.
 * No catch block needed — this is the intended lifecycle.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation 1: comment out `if (isCompletionPopupOpen()) return`
 *     → `dwell_skips_when_completion_popup_open` FAILS.
 *   Mutation 2: comment out `if (!isIdentifierAt(line, character)) return`
 *     → `dwell_skips_when_not_identifier` FAILS.
 *   Reverted; all 6 tests GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   Android: production use via Modifier.pointerInput in Phase 8.
 *   Desktop: same class consumed in Phase 8 integration.
 *   iOS:     deferred.
 *   Web:     deferred.
 *
 * Submodules: not touched (CONST-038).
 *########################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Stateful detector that converts raw pointer events into hover triggers.
 *
 * All callbacks are invoked on the [scope]'s dispatcher (typically Main).
 *
 * @param scope coroutine scope that owns the dwell job lifecycle.
 * @param dwellMillis milliseconds the pointer must remain still before
 *   [onDwell] fires. Default 300 ms per Phase 0 §1 spec.
 * @param isCompletionPopupOpen returns true when the completion popup is
 *   currently visible; dwell hover is suppressed in that state.
 * @param isIdentifierAt returns true when the given (line, character) falls
 *   inside an identifier node per the Tree-Sitter parse tree.
 * @param onDwell called after a successful dwell; triggers the LSP hover request.
 * @param onExplicit called by [onExplicit] regardless of guards or dwell.
 */
class HoverTriggerDetector(
    private val scope: CoroutineScope,
    private val dwellMillis: Long = 300L,
    private val isCompletionPopupOpen: () -> Boolean,
    private val isIdentifierAt: (line: Int, character: Int) -> Boolean,
    private val onDwell: (line: Int, character: Int) -> Unit,
    private val onExplicit: (line: Int, character: Int) -> Unit,
) {
    // Alias to avoid name collision between the `onExplicit` lambda field
    // and the `onExplicit(line, character)` public method. Without this,
    // calling `onExplicit(line, character)` inside the method would recurse.
    private val explicitCallback: (Int, Int) -> Unit = onExplicit

    private var dwellJob: Job? = null

    /**
     * Called on every pointer-move event with the editor-space position.
     *
     * Cancels any previous pending dwell and schedules a new one after
     * [dwellMillis]. Silently ignored when the completion popup is open or
     * the position is not over an identifier.
     */
    fun onPointerMove(line: Int, character: Int) {
        if (isCompletionPopupOpen()) return
        if (!isIdentifierAt(line, character)) return
        dwellJob?.cancel()
        dwellJob = scope.launch {
            delay(dwellMillis)
            onDwell(line, character)
        }
    }

    /**
     * Called when the pointer leaves the editor surface.
     * Cancels any pending dwell job.
     */
    fun onPointerExit() {
        dwellJob?.cancel()
        dwellJob = null
    }

    /**
     * Explicit hover trigger (e.g. F1 shortcut or long-press fallback).
     *
     * Bypasses the completion-popup guard and identifier check. Fires
     * the [onExplicit] callback synchronously (no delay).
     */
    fun onExplicit(line: Int, character: Int) {
        dwellJob?.cancel()
        explicitCallback(line, character)
    }

    /**
     * Cancels any pending dwell job without firing [onDwell].
     * Call on popup close or editor focus loss.
     */
    fun dismiss() {
        dwellJob?.cancel()
        dwellJob = null
    }
}
