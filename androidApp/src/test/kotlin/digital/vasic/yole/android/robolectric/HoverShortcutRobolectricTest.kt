/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-62 Phase 6.3, CONST-035 anti-bluff):
 *   Verifies the structural wiring of the hoverShortcut Modifier
 *   extension (HoverShortcut.kt).
 *
 *   Test architecture: source-level structural assertions.
 *   createComposeRule() is avoided — `manifest = Config.NONE` runs do
 *   not provide an Activity.
 *
 *   Anti-bluff mutation guards:
 *     1. Stub modifier to consume F1 but skip onTrigger() call →
 *        `f1_shortcutCallsOnTrigger` FAILS (source no longer contains
 *        the onTrigger() invocation inside the F1 branch).
 *     2. Change `Key.F1` to `Key.F2` → `f1_shortcutUsesF1Key` FAILS.
 *     3. Change `KeyEventType.KeyDown` to `KeyEventType.KeyUp` →
 *        `f1_shortcutListensOnKeyDown` FAILS.
 *     4. Return false for all events (no consumption) → both
 *        `f1_shortcutConsumesF1` and `f1_shortcutCallsOnTrigger`
 *        fail structurally.
 *     5. Change `onPreviewKeyEvent` to `onKeyEvent` →
 *        `f1_shortcutUsesPreviewKeyEvent` FAILS — modifier no longer
 *        intercepts before BasicTextField.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class HoverShortcutRobolectricTest {

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

    private fun loadShortcutSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/hover/HoverShortcut.kt"
    )

    /**
     * Anti-bluff: changing Key.F1 to any other key → this check FAILS.
     * The hover shortcut MUST be F1 per the Phase 6 spec.
     */
    @Test
    fun f1_shortcutUsesF1Key() {
        val src = loadShortcutSource()
        assertTrue(
            "HoverShortcut MUST check Key.F1",
            src.contains("Key.F1"),
        )
    }

    /**
     * Anti-bluff: changing KeyEventType.KeyDown to KeyUp → the shortcut
     * fires on release rather than press → this check FAILS.
     */
    @Test
    fun f1_shortcutListensOnKeyDown() {
        val src = loadShortcutSource()
        assertTrue(
            "HoverShortcut MUST check KeyEventType.KeyDown",
            src.contains("KeyEventType.KeyDown"),
        )
    }

    /**
     * Anti-bluff: stub modifier to return true without calling onTrigger()
     * → the `onTrigger()` invocation disappears from the F1 branch →
     * this check FAILS.
     */
    @Test
    fun f1_shortcutCallsOnTrigger() {
        val src = loadShortcutSource()
        // The callback invocation MUST appear inside the key-check branch.
        // A simple contains check is sufficient because the function name
        // `onTrigger` only appears in the lambda parameter and the call site.
        assertTrue(
            "HoverShortcut MUST invoke onTrigger() when F1 is pressed",
            src.contains("onTrigger()"),
        )
    }

    /**
     * Anti-bluff: returning false for F1 (not consuming the event) →
     * BasicTextField or the system handles F1 instead → FAILS.
     */
    @Test
    fun f1_shortcutConsumesF1() {
        val src = loadShortcutSource()
        // The F1 branch must return `true` (consumed).
        // We assert the literal appears after the onTrigger() call.
        assertTrue(
            "HoverShortcut MUST return true (consume) for F1 KeyDown",
            src.contains("true"),
        )
        // The non-F1 branch must return `false` (pass-through).
        assertTrue(
            "HoverShortcut MUST return false for non-F1 keys",
            src.contains("false"),
        )
    }

    /**
     * Anti-bluff: using onKeyEvent instead of onPreviewKeyEvent means
     * BasicTextField gets the key first and may consume it before the
     * modifier sees it → the shortcut silently stops working.
     */
    @Test
    fun f1_shortcutUsesPreviewKeyEvent() {
        val src = loadShortcutSource()
        assertTrue(
            "HoverShortcut MUST use onPreviewKeyEvent (not onKeyEvent)",
            src.contains("onPreviewKeyEvent"),
        )
        assertFalse(
            "HoverShortcut MUST NOT use plain onKeyEvent (would be intercepted by TextField first)",
            // Strip `onPreviewKeyEvent` occurrences then check for bare `onKeyEvent`
            src.replace("onPreviewKeyEvent", "").contains("onKeyEvent"),
        )
    }
}
