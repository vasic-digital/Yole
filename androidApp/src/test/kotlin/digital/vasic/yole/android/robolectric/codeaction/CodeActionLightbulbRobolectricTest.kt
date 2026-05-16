/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-63 Phase 6, CONST-035 anti-bluff):
 *   Verifies that CodeActionLightbulb.kt, CodeActionMenu.kt, and
 *   CodeActionInvoker.kt have the correct structural wiring for the
 *   3rd gutter column implementation.
 *
 *   Test architecture (mirrors iter-62 Phase 7 DiagnosticsGutterTest +
 *   iter-63 Phase 5 RenamePreviewPanelRobolectricTest pattern):
 *   source-level structural assertions. createComposeRule() is avoided
 *   because `manifest = Config.NONE` runs do not provide an Activity.
 *
 *   Anti-bluff mutation guards (CONST-035):
 *
 *   lightbulb_visible_when_actions_present:
 *     Mutation: stub CodeActionLightbulb body to empty Column (remove icon
 *     rendering) → FAILS because the "lightbulb-line-$lineNum" testTag
 *     string is absent from the source.
 *
 *   lightbulb_hidden_when_no_actions:
 *     Mutation: remove the Spacer fallback branch (else { Spacer(...) }) →
 *     FAILS because "Spacer" is absent from the lightbulb source.
 *
 *   menu_renders_all_actions:
 *     Mutation: stub CodeActionMenu body to empty (remove DropdownMenuItem
 *     loop) → FAILS because "code-action-item-" testTag disappears.
 *
 *   menu_item_click_invokes_callback:
 *     Mutation: remove onSelected(action) call from the DropdownMenuItem
 *     onClick → FAILS because "onSelected(action)" is absent.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: tested here.
 *   - Desktop/iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.robolectric.codeaction

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CodeActionLightbulbRobolectricTest {

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

    private fun loadLightbulbSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/codeaction/CodeActionLightbulb.kt",
    )

    private fun loadMenuSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/codeaction/CodeActionMenu.kt",
    )

    private fun loadInvokerSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/codeaction/CodeActionInvoker.kt",
    )

    private fun loadEditorSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/SyncedScrollEditor.kt",
    )

    // -----------------------------------------------------------------------
    // Test 1: lightbulb_visible_when_actions_present
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: stub the Composable body to empty Column (remove Icon +
     * testTag("lightbulb-line-\$lineNum")) → this test FAILS because the
     * structural marker disappears from the source.
     *
     * Mutation: remove testTag("lightbulb-line-\$lineNum") from source → FAIL.
     */
    @Test
    fun lightbulb_visible_when_actions_present() {
        val src = loadLightbulbSource()

        assertTrue(
            "CodeActionLightbulb MUST apply testTag(\"code-action-lightbulb\") to its root Column",
            src.contains(""""code-action-lightbulb""""),
        )
        assertTrue(
            "CodeActionLightbulb MUST apply testTag(\"lightbulb-line-\$lineNum\") per icon",
            src.contains(""""lightbulb-line-${"$"}lineNum""""),
        )
        assertTrue(
            "CodeActionLightbulb MUST iterate actionsByLine entries by lineNum",
            src.contains("actionsByLine"),
        )
        assertTrue(
            "CodeActionLightbulb MUST render an Icon when actions are present",
            src.contains("Icon("),
        )
        assertTrue(
            "CodeActionLightbulb icon MUST be clickable and invoke onTap(lineNum)",
            src.contains("onTap(lineNum)"),
        )
        assertTrue(
            "CodeActionLightbulb MUST use LightbulbTint (amber #FFC107 colour)",
            src.contains("LightbulbTint") || src.contains("0xFFFFC107"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: lightbulb_hidden_when_no_actions
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: remove the Spacer fallback in the else branch → FAIL
     * because "Spacer" disappears from source (the empty-slot Box may remain
     * but provides no visible affordance, and test would catch the absence).
     *
     * Mutation: replace else { Spacer(...) } with else { } → FAIL.
     */
    @Test
    fun lightbulb_hidden_when_no_actions() {
        val src = loadLightbulbSource()

        assertTrue(
            "CodeActionLightbulb MUST render a Spacer placeholder when no actions are present",
            src.contains("Spacer("),
        )
        assertTrue(
            "CodeActionLightbulb MUST use isNullOrEmpty() or equivalent check to guard icon rendering",
            src.contains("isNullOrEmpty()") || src.contains("isEmpty()") || src.contains("== null"),
        )
        assertTrue(
            "CodeActionLightbulb MUST reserve a fixed height Box for each line (icon or spacer)",
            src.contains("Box(") && src.contains("height(lineHeight)"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 3: menu_renders_all_actions
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: remove DropdownMenuItem loop from CodeActionMenu → FAIL
     * because "code-action-item-" testTag disappears from source.
     *
     * Mutation: stub CodeActionMenu body to empty DropdownMenu → FAIL.
     */
    @Test
    fun menu_renders_all_actions() {
        val src = loadMenuSource()

        assertTrue(
            "CodeActionMenu MUST apply testTag(\"code-action-menu\") to its DropdownMenu",
            src.contains(""""code-action-menu""""),
        )
        assertTrue(
            "CodeActionMenu MUST apply testTag(\"code-action-item-\$index\") to each item",
            src.contains(""""code-action-item-${"$"}index""""),
        )
        assertTrue(
            "CodeActionMenu MUST use DropdownMenu",
            src.contains("DropdownMenu("),
        )
        assertTrue(
            "CodeActionMenu MUST use DropdownMenuItem for each action",
            src.contains("DropdownMenuItem("),
        )
        assertTrue(
            "CodeActionMenu MUST iterate actions with forEachIndexed",
            src.contains("forEachIndexed"),
        )
        assertTrue(
            "CodeActionMenu MUST display action.title in the item text",
            src.contains("action.title"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 4: menu_item_click_invokes_callback
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: remove onSelected(action) from the DropdownMenuItem onClick
     * body → this structural check FAILS because the call-site disappears.
     *
     * Mutation: replace onSelected(action) with Unit → FAIL.
     */
    @Test
    fun menu_item_click_invokes_callback() {
        val menuSrc = loadMenuSource()

        assertTrue(
            "CodeActionMenu DropdownMenuItem onClick MUST call onSelected(action)",
            menuSrc.contains("onSelected(action)"),
        )
        assertTrue(
            "CodeActionMenu DropdownMenuItem onClick MUST also call onDismissRequest()",
            menuSrc.contains("onDismissRequest()"),
        )

        // Verify invoker structural wiring.
        val invokerSrc = loadInvokerSource()

        assertTrue(
            "CodeActionInvoker MUST call WorkspaceEditApplier.apply when edit is non-null",
            invokerSrc.contains("WorkspaceEditApplier.apply("),
        )
        assertTrue(
            "CodeActionInvoker MUST call onEdit(result) after applying the workspace edit",
            invokerSrc.contains("onEdit(result)"),
        )
        assertTrue(
            "CodeActionInvoker MUST call onCommand(command) when command is non-null and edit is null",
            invokerSrc.contains("onCommand(command)"),
        )
        assertTrue(
            "CodeActionInvoker.invoke MUST be a suspend fun",
            invokerSrc.contains("suspend fun invoke("),
        )

        // Verify SyncedScrollEditor wires CodeActionLightbulb in the gutter.
        val editorSrc = loadEditorSource()

        assertTrue(
            "SyncedScrollEditor MUST accept actionsByLine parameter",
            editorSrc.contains("actionsByLine"),
        )
        assertTrue(
            "SyncedScrollEditor MUST render CodeActionLightbulb when actionsByLine is non-empty",
            editorSrc.contains("CodeActionLightbulb("),
        )
        assertTrue(
            "SyncedScrollEditor MUST accept onCodeActionLineTap parameter",
            editorSrc.contains("onCodeActionLineTap"),
        )
    }
}
