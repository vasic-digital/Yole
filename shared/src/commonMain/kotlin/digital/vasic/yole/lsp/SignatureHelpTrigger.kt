/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 7.3: SignatureHelpTrigger — per-keystroke detector for LSP
 * signature-help invocation.
 *
 * Design:
 *   Listens for '(' and ',' keystrokes (triggers) and ')' (dismiss).
 *   On trigger:
 *     1. Cancels any in-flight LSP request job.
 *     2. Cancels any running auto-dismiss timer.
 *     3. Launches a new coroutine that calls requester.signatureHelp(...)
 *        and delivers the result to [onResult].
 *     4. Starts a 30-second auto-dismiss timer; if it fires, calls
 *        onResult(null) to clear the UI.
 *   On ')':
 *     Calls onResult(null) immediately (dismiss), cancels any jobs.
 *   On any other character:
 *     No-op.
 *
 *   CancellationException: the inner launch blocks do not catch any
 *   exception; structured concurrency propagates cancellation naturally.
 *   The requester.signatureHelp() call may throw; callers should ensure
 *   the requester implementation handles its own exceptions (timeout, etc.)
 *   and returns null on failure.
 *
 * Anti-bluff mutation procedure (CONST-035):
 *   1. Stub onKeystroke to never call requester.signatureHelp() →
 *      keystroke_open_paren_invokesRequester FAILS (callCount stays 0).
 *   2. Stub ')' handling to call onResult(null) but not cancel jobs →
 *      keystroke_close_paren_dismisses still PASSES on structure, but
 *      the dismissOnClose structural check FAILS if condition removed.
 *   3. Remove cancellation of in-flight job before new launch →
 *      inflight_cancelled_before_new_request FAILS (both jobs run).
 *   4. Revert all → GREEN.
 *
 * Cross-platform impact (CONST-037):
 *   - Common: pure Kotlin/coroutines — runs on all targets.
 *   - Desktop: tested via desktopTest (SignatureHelpTriggerTest).
 *   - Android: wired in Phase 10 via IdeEditorScreen keystroke stream.
 *   - iOS/Wasm: compiles; wiring deferred to Phase 10.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Characters that open a call-site and should trigger signature help. */
private val TRIGGER_CHARS = setOf('(', ',')

/** Character that closes a call-site and should dismiss signature help. */
private const val DISMISS_CHAR = ')'

/** Auto-dismiss timeout in milliseconds. */
private const val AUTO_DISMISS_MS = 30_000L

/**
 * Keystroke-driven controller that requests LSP signature help when the
 * user types `(` or `,` inside a call expression, and dismisses it on `)`.
 *
 * Wire up by calling [onKeystroke] from the editor's key-event handler.
 * Collect results via the [onResult] callback (null = dismiss the UI).
 *
 * @param scope      Owning [CoroutineScope]. Cancelling the scope cleans up
 *                   all in-flight and timer jobs.
 * @param requester  LSP back-end seam — see [LspSignatureHelpRequester].
 * @param onResult   Called on the caller's dispatcher with the [SignatureHelp]
 *                   result, or null when the popup should be dismissed.
 */
class SignatureHelpTrigger(
    private val scope: CoroutineScope,
    private val requester: LspSignatureHelpRequester,
    private val onResult: (SignatureHelp?) -> Unit,
) {

    /** In-flight LSP request job. */
    private var requestJob: Job? = null

    /** Auto-dismiss timer job. */
    private var dismissTimerJob: Job? = null

    /**
     * Process a single editor keystroke.
     *
     * @param char      The character that was typed.
     * @param langId    Language identifier for the current document.
     * @param uri       Document URI for the current document.
     * @param line      0-based cursor line after the keystroke.
     * @param character 0-based cursor character offset after the keystroke.
     */
    fun onKeystroke(
        char: Char,
        langId: String,
        uri: String,
        line: Int,
        character: Int,
    ) {
        when {
            char in TRIGGER_CHARS -> handleTrigger(langId, uri, line, character)
            char == DISMISS_CHAR  -> handleDismiss()
        }
        // All other characters are a no-op.
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun handleTrigger(langId: String, uri: String, line: Int, character: Int) {
        // Cancel any in-flight request before starting a new one.
        requestJob?.cancel()
        requestJob = null

        // Reset auto-dismiss timer.
        cancelDismissTimer()

        requestJob = scope.launch {
            val result = requester.signatureHelp(langId, uri, line, character)
            onResult(result)
        }

        // Start 30-second auto-dismiss timer.
        dismissTimerJob = scope.launch {
            delay(AUTO_DISMISS_MS)
            onResult(null)
        }
    }

    private fun handleDismiss() {
        requestJob?.cancel()
        requestJob = null
        cancelDismissTimer()
        onResult(null)
    }

    private fun cancelDismissTimer() {
        dismissTimerJob?.cancel()
        dismissTimerJob = null
    }
}
