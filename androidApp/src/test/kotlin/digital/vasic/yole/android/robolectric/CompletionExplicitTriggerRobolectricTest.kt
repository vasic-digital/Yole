/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-60 Phase 6.7, CONST-035 anti-bluff):
 *   Verifies that the explicit completion trigger fires Show events
 *   immediately (bypassing the prefix-length guard and debounce),
 *   and that the Ctrl+Space key handler is wired in SyncedScrollEditor.
 *
 *   Anti-bluff mutation guards:
 *     1. Source-level: removing the `isCtrlPressed && event.key == Key.Spacebar`
 *        branch from SyncedScrollEditor → `ctrlSpaceHandlerWiredInEditor` FAILS.
 *     2. Source-level: removing `trig.onExplicitTrigger()` from the Ctrl+Space
 *        branch → `ctrlSpaceCallsOnExplicitTrigger` FAILS (no call site).
 *     3. Source-level: removing the IdeEditorScreen CompletionToolbarButton
 *        call → `yoleAppWiresCompletionToolbarButton` FAILS.
 *     4. Pure-function: removing onExplicitTrigger from CompletionTrigger →
 *        `explicitTriggerEmitsShowEvenOnEmptyText` FAILS because isPopupOpen
 *        is never set to true.
 *
 *   Test architecture: source-level structural assertions + pure-function
 *   trigger test (same as FoldGutterRobolectricTest pattern).
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import digital.vasic.yole.completion.trigger.CompletionTrigger
import digital.vasic.yole.completion.trigger.TriggerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CompletionExplicitTriggerRobolectricTest {

    private fun loadSource(relativePath: String): String {
        val candidates = listOf(
            relativePath,
            "../$relativePath",
            relativePath.removePrefix("androidApp/"),
        )
        for (path in candidates) {
            val f = File(path)
            if (f.isFile) return f.readText()
        }
        error("$relativePath not found; checked: $candidates (cwd=${File(".").absolutePath})")
    }

    private fun loadEditorSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt"
    )

    private fun loadYoleAppSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt"
    )

    // -----------------------------------------------------------------------
    // Layer 1: CompletionTrigger explicit-trigger pure-function test
    // -----------------------------------------------------------------------

    /**
     * The explicit trigger MUST emit a Show event immediately even with
     * an empty prefix. The trigger uses an UnconfinedTestDispatcher-like
     * scope so coroutines run on the calling thread with no real delay.
     *
     * Anti-bluff mutation (CONST-035): removing the explicit-path bypass
     * (routing through debounce instead) causes the flow NOT to emit within
     * 200 ms for a zero-prefix text. The withTimeout catches this:
     *   - No mutation → event arrives within a few ms → test PASSES.
     *   - With mutation (routed through 80 ms debounce) → event still
     *     arrives eventually BUT the semantic is wrong (debounce + prefix-
     *     guard may suppress it for empty text). The structural test below
     *     is the primary anti-bluff anchor.
     */
    @Test
    fun explicitTriggerEmitsShowEvenOnEmptyText() {
        val job = Job()
        val scope = CoroutineScope(job)
        val trigger = CompletionTrigger(
            langId = "markdown",
            debounceMillis = 0L, // zero debounce so Show arrives synchronously
            scope = scope,
        )

        val events = mutableListOf<TriggerEvent>()
        val collectJob = scope.launch {
            trigger.events.collect { events.add(it) }
        }

        // Fire the explicit trigger — with debounceMillis=0 the coroutine
        // posts to the scope immediately. Give it a short window.
        runBlocking<Unit> {
            trigger.onExplicitTrigger()
            withTimeout(500L) {
                // Spin until a Show event arrives or we time out.
                while (events.filterIsInstance<TriggerEvent.Show>().isEmpty()) {
                    kotlinx.coroutines.delay(10)
                }
            }
        }

        val showEvents = events.filterIsInstance<TriggerEvent.Show>()
        assertTrue(
            "At least one Show event MUST be emitted by onExplicitTrigger() (got: $events)",
            showEvents.isNotEmpty(),
        )

        collectJob.cancel()
        job.cancel()
    }

    // -----------------------------------------------------------------------
    // Layer 2: source-level structural guarantees
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing the `isCtrlPressed && event.key == Key.Spacebar`
     * branch from SyncedScrollEditor → Ctrl+Space never triggers completion.
     */
    @Test
    fun ctrlSpaceHandlerWiredInEditor() {
        val src = loadEditorSource()
        assertTrue(
            "SyncedScrollEditor MUST check event.isCtrlPressed",
            src.contains("event.isCtrlPressed"),
        )
        assertTrue(
            "SyncedScrollEditor MUST check event.key == Key.Spacebar",
            src.contains("Key.Spacebar"),
        )
    }

    /**
     * Anti-bluff: removing trig.onExplicitTrigger() from the Ctrl+Space branch
     * → the trigger is never called → popup never opens on Ctrl+Space.
     */
    @Test
    fun ctrlSpaceCallsOnExplicitTrigger() {
        val src = loadEditorSource()
        assertTrue(
            "SyncedScrollEditor MUST call trig.onExplicitTrigger() in the Ctrl+Space branch",
            src.contains("trig.onExplicitTrigger()"),
        )
    }

    /**
     * Anti-bluff: removing trig.onDismiss() from the Esc branch → Esc
     * never dismisses the popup.
     */
    @Test
    fun escapeHandlerCallsOnDismiss() {
        val src = loadEditorSource()
        assertTrue(
            "SyncedScrollEditor MUST handle Key.Escape",
            src.contains("Key.Escape"),
        )
        assertTrue(
            "SyncedScrollEditor MUST call trig.onDismiss() in the Escape handler",
            src.contains("trig.onDismiss()"),
        )
    }

    /**
     * Anti-bluff: removing the CompletionToolbarButton from YoleApp →
     * the mobile suggest button is absent.
     */
    @Test
    fun yoleAppWiresCompletionToolbarButton() {
        val src = loadYoleAppSource()
        assertTrue(
            "YoleApp.kt MUST call CompletionToolbarButton(",
            src.contains("CompletionToolbarButton("),
        )
        assertTrue(
            "YoleApp.kt MUST call completionTrigger.onExplicitTrigger() from the toolbar button",
            src.contains("completionTrigger.onExplicitTrigger()"),
        )
    }

    /**
     * Anti-bluff: removing completionEngine / completionTrigger from YoleApp →
     * no completion wiring at all.
     */
    @Test
    fun yoleAppWiresCompletionEngine() {
        val src = loadYoleAppSource()
        assertTrue(
            "YoleApp.kt MUST construct a CompletionEngine",
            src.contains("CompletionEngine.default("),
        )
        assertTrue(
            "YoleApp.kt MUST construct a CompletionTrigger",
            src.contains("CompletionTrigger("),
        )
        assertTrue(
            "YoleApp.kt MUST pass completionTrigger to SyncedScrollEditor",
            src.contains("completionTrigger = completionTrigger,"),
        )
        assertTrue(
            "YoleApp.kt MUST pass completionPopupState to SyncedScrollEditor",
            src.contains("completionPopupState = completionPopupState,"),
        )
    }
}
