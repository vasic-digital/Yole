/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-60 Phase 6.6, CONST-035 anti-bluff):
 *   Verifies that CompletionPopupState drives the popup correctly:
 *   after calling show() with ≥ 1 item the state is open; after hide()
 *   it resets; after update() items are refreshed.
 *
 *   Anti-bluff mutation guards:
 *     1. Stubbing CompletionPopupState.show() to a no-op → isOpen stays
 *        false → `popupOpensOnShow` FAILS.
 *     2. Removing `isOpen = true` from show() → same failure.
 *     3. Stubbing hide() to a no-op → `popupClosesOnHide` FAILS because
 *        isOpen stays true after hide() is called.
 *     4. Stubbing moveSelection to a no-op → `selectionMoves` FAILS
 *        because selectedIndex does not advance.
 *     5. Source-level guard: removing `testTag("completion-popup")` from
 *        CompletionPopup.kt → `popupHasTestTag` FAILS.
 *     6. Source-level guard: removing `testTag("completion-suggest-button")`
 *        from CompletionToolbarButton.kt → `toolbarButtonHasTestTag` FAILS.
 *
 *   Test architecture (matches iter-57/58 pattern): pure state-object +
 *   source-level structural assertions. createComposeRule() is avoided —
 *   the project's `manifest = Config.NONE` runs do not provide an Activity,
 *   and the source-level assertions are the load-bearing anti-bluff anchors.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import digital.vasic.yole.android.ui.editor.CompletionPopupState
import digital.vasic.yole.completion.CompletionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CompletionPopupRobolectricTest {

    private fun makeItem(label: String): CompletionItem = CompletionItem(
        label = label,
        insertText = label,
        kind = CompletionItem.Kind.Word,
        score = 1.0,
        range = 0..0,
    )

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

    private fun loadPopupSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/CompletionPopup.kt"
    )

    private fun loadButtonSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/CompletionToolbarButton.kt"
    )

    private fun loadEditorSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt"
    )

    // -----------------------------------------------------------------------
    // Layer 1: CompletionPopupState pure-function unit tests
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: stubbing show() to no-op would keep isOpen=false;
     * this assertion catches it.
     */
    @Test
    fun popupOpensOnShow() {
        val state = CompletionPopupState()
        assertFalse("initially closed", state.isOpen)
        state.show(listOf(makeItem("foo")), cursorAnchor = 3)
        assertTrue("isOpen after show()", state.isOpen)
        assertEquals(1, state.items.size)
        assertEquals(0, state.selectedIndex)
        assertEquals(3, state.anchorOffset)
    }

    /**
     * Anti-bluff: stubbing hide() to no-op keeps isOpen=true after hide();
     * `popupClosesOnHide` catches it.
     */
    @Test
    fun popupClosesOnHide() {
        val state = CompletionPopupState()
        state.show(listOf(makeItem("bar")), cursorAnchor = 1)
        assertTrue(state.isOpen)
        state.hide()
        assertFalse("isOpen must be false after hide()", state.isOpen)
        assertTrue("items must be empty after hide()", state.items.isEmpty())
        assertEquals(0, state.selectedIndex)
    }

    /**
     * Anti-bluff: stubbing update() to no-op keeps old items;
     * the second items.size assertion catches it.
     */
    @Test
    fun updateRefreshesItems() {
        val state = CompletionPopupState()
        state.show(listOf(makeItem("a")), cursorAnchor = 0)
        assertEquals(1, state.items.size)
        state.update(listOf(makeItem("a"), makeItem("b"), makeItem("c")))
        assertEquals(3, state.items.size)
        assertTrue(state.isOpen)
    }

    /**
     * Anti-bluff: stubbing moveSelection to no-op keeps selectedIndex=0;
     * the post-move assertion catches it.
     */
    @Test
    fun selectionMoves() {
        val state = CompletionPopupState()
        state.show(listOf(makeItem("x"), makeItem("y"), makeItem("z")), cursorAnchor = 0)
        assertEquals(0, state.selectedIndex)
        state.moveSelection(1)
        assertEquals(1, state.selectedIndex)
        state.moveSelection(1)
        assertEquals(2, state.selectedIndex)
        // Wrap-around
        state.moveSelection(1)
        assertEquals(0, state.selectedIndex)
        // Backwards
        state.moveSelection(-1)
        assertEquals(2, state.selectedIndex)
    }

    /**
     * show() with empty list MUST be a no-op (no empty-popup flash).
     */
    @Test
    fun showWithEmptyListIsNoOp() {
        val state = CompletionPopupState()
        state.show(emptyList(), cursorAnchor = 0)
        assertFalse("isOpen must stay false when items is empty", state.isOpen)
    }

    // -----------------------------------------------------------------------
    // Layer 2: source-level structural guarantees
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing testTag("completion-popup") from CompletionPopup
     * → runtime UI tests cannot find the node → this structural check FAILS.
     */
    @Test
    fun popupHasTestTag() {
        val src = loadPopupSource()
        assertTrue(
            "CompletionPopup MUST apply testTag(\"completion-popup\")",
            src.contains("""testTag("completion-popup")"""),
        )
        assertTrue(
            "CompletionPopup MUST apply per-item testTag(\"completion-item-\$index\")",
            src.contains("""testTag("completion-item-${'$'}index")"""),
        )
    }

    /**
     * Anti-bluff: removing testTag("completion-suggest-button") from
     * CompletionToolbarButton → MobileSuggestButtonRobolectricTest cannot
     * find the button → this check FAILS first.
     */
    @Test
    fun toolbarButtonHasTestTag() {
        val src = loadButtonSource()
        assertTrue(
            "CompletionToolbarButton MUST apply testTag(\"completion-suggest-button\")",
            src.contains("""testTag("completion-suggest-button")"""),
        )
    }

    /**
     * Anti-bluff: removing the CompletionPopup call from SyncedScrollEditor
     * → popup never renders → this check FAILS.
     */
    @Test
    fun editorRendersCompletionPopup() {
        val src = loadEditorSource()
        assertTrue(
            "SyncedScrollEditor MUST call CompletionPopup(",
            src.contains("CompletionPopup("),
        )
        assertTrue(
            "SyncedScrollEditor MUST accept completionTrigger parameter",
            Regex("""completionTrigger:\s*CompletionTrigger\?""").containsMatchIn(src),
        )
        assertTrue(
            "SyncedScrollEditor MUST accept completionPopupState parameter",
            Regex("""completionPopupState:\s*CompletionPopupState\?""").containsMatchIn(src),
        )
    }

    /**
     * Anti-bluff: removing the trigger.onTextChanged call in onValueChange →
     * the trigger never learns about text changes → implicit popup never opens.
     * This structural check fires before the runtime path is exercised.
     */
    @Test
    fun editorFeedsCompletionTriggerOnTextChange() {
        val src = loadEditorSource()
        assertTrue(
            "SyncedScrollEditor onValueChange MUST call completionTrigger?.onTextChanged",
            src.contains("completionTrigger?.onTextChanged("),
        )
    }
}
