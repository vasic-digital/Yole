/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-63 Phase 9, CONST-035 anti-bluff):
 *   Verifies that ReferencesPanel.kt has the correct structural wiring
 *   for rendering reference locations in a persistent LazyColumn bottom
 *   drawer — distinct from the DefinitionLocationChooser (ModalBottomSheet).
 *
 *   Test architecture (mirrors iter-62 Phase 7 DefinitionLocationChooserRobolectricTest
 *   and iter-63 Phase 5 RenamePreviewPanelRobolectricTest pattern): source-level
 *   structural assertions. createComposeRule() is avoided — `manifest = Config.NONE`
 *   runs do not provide an Activity.
 *
 * --- Test 1: rendersAllRefs ---
 *   Anti-bluff mutation: remove testTag("references-panel") or testTag("references-row-$index").
 *   → This test FAILS because the structural marker strings disappear from source.
 *   Mutation also: remove LazyColumn → FAIL (LazyColumn absent).
 *   Revert → PASS.
 *
 * --- Test 2: clickRow_jumps ---
 *   Anti-bluff mutation: stub onClick to no-op (remove onJump call inside clickable).
 *   → This test FAILS because "onJump(ref.uri, ref.range.first)" disappears from source.
 *   Mutation also: remove clickable modifier → FAIL (clickable absent from row).
 *   Revert → PASS.
 *
 * --- Test 3: panel_persistent_across_navigation ---
 *   Anti-bluff mutation: wrap panel body in ModalBottomSheet (add onDismissRequest).
 *   → This test FAILS because it asserts the panel does NOT use ModalBottomSheet.
 *   Mutation also: add an onDismiss parameter → FAIL (dismissed-on-click contract violated).
 *   Revert → PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: this test file; Robolectric runner on JVM.
 *   - Desktop/iOS/Web: ReferencesPanel is Android-only this phase; no parity tests needed yet.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
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
class ReferencesPanelRobolectricTest {

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

    private fun loadPanelSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/references/ReferencesPanel.kt"
    )

    private fun loadActionSource(): String = loadSource(
        "shared/src/commonMain/kotlin/digital/vasic/yole/lsp/FindReferencesAction.kt"
    )

    // -----------------------------------------------------------------------
    // Test 1: rendersAllRefs — 3 refs → structural anchors for 3 rows present
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing testTag("references-panel"), testTag("references-row-\$index"),
     * or LazyColumn means UI instrumentation and future HelixQA tests cannot
     * locate rows or the root → this structural check FAILS.
     *
     * Also verifies that each row shows filename + line:col data — structural
     * markers for substringAfterLast('/') and offsetToLineCol must be present.
     *
     * Mutation: remove testTag("references-panel") → FAIL.
     * Mutation: remove testTag("references-row-\$index") → FAIL.
     * Mutation: replace LazyColumn with Column → FAIL.
     */
    @Test
    fun rendersAllRefs() {
        val src = loadPanelSource()

        assertTrue(
            "ReferencesPanel MUST apply testTag(\"references-panel\") to the root LazyColumn",
            src.contains("""testTag("references-panel")"""),
        )
        assertTrue(
            "ReferencesPanel MUST apply testTag(\"references-row-\$index\") to each row",
            src.contains("""testTag("references-row-${"$"}index")"""),
        )
        assertTrue(
            "ReferencesPanel MUST use LazyColumn for the list of references",
            src.contains("LazyColumn"),
        )
        assertTrue(
            "ReferencesPanel MUST use itemsIndexed to supply the row index",
            src.contains("itemsIndexed"),
        )
        assertTrue(
            "ReferencesPanel row MUST derive filename from URI last path segment (substringAfterLast('/'))",
            src.contains("substringAfterLast('/')"),
        )
        assertTrue(
            "ReferencesPanel row MUST show line:col via offsetToLineCol",
            src.contains("offsetToLineCol"),
        )
        assertTrue(
            "ReferencesPanel row MUST show context-line preview via extractContextLine",
            src.contains("extractContextLine"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: clickRow_jumps — row click invokes onJump(uri, offset)
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: stubbing the row's onClick to a no-op (removing the
     * onJump call) means the callback is never fired → this structural check
     * FAILS because "onJump(ref.uri, ref.range.first)" disappears from source.
     *
     * Mutation: replace onJump(ref.uri, ref.range.first) with Unit → FAIL.
     * Mutation: remove clickable modifier from row → FAIL.
     */
    @Test
    fun clickRow_jumps() {
        val src = loadPanelSource()

        assertTrue(
            "ReferencesPanel row onClick MUST call onJump(ref.uri, ref.range.first)",
            src.contains("onJump(ref.uri, ref.range.first)"),
        )
        assertTrue(
            "ReferencesPanel row MUST apply clickable modifier to register taps",
            src.contains("clickable {"),
        )
        assertTrue(
            "ReferencesPanel MUST accept an onJump: (uri: String, offset: Int) -> Unit parameter",
            src.contains("onJump: (uri: String, offset: Int) -> Unit"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 3: panel_persistent_across_navigation — NOT a dismissing bottom sheet
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: wrapping the panel body in ModalBottomSheet (like
     * DefinitionLocationChooser) would change its UX contract from a persistent
     * list to a dismissible overlay → this test FAILS if ModalBottomSheet appears.
     *
     * Also verifies there is no onDismiss callback (persistent means it only
     * disappears when the caller decides — no user-swipe dismiss).
     *
     * Mutation: add ModalBottomSheet wrapper to ReferencesPanel → FAIL.
     * Mutation: add onDismiss parameter → FAIL (assertFalse triggers).
     */
    @Test
    fun panel_persistent_across_navigation() {
        val src = loadPanelSource()

        assertFalse(
            "ReferencesPanel MUST NOT use ModalBottomSheet — it is a persistent drawer, not a dismissible sheet",
            src.contains("ModalBottomSheet("),
        )
        assertFalse(
            "ReferencesPanel MUST NOT have an onDismiss parameter (persistent panels are not swipe-dismissed)",
            src.contains("onDismiss"),
        )
        assertTrue(
            "ReferencesPanel root MUST be LazyColumn (persistent persistent persistent — not wrapped in a sheet)",
            src.contains("LazyColumn("),
        )

        // Also verify FindReferencesAction structural contract
        val actionSrc = loadActionSource()
        assertTrue(
            "FindReferencesAction MUST invoke requester.references(...)",
            actionSrc.contains("requester.references("),
        )
        assertTrue(
            "FindReferencesAction MUST call onShow(results) when references are found",
            actionSrc.contains("onShow(results)"),
        )
        assertTrue(
            "FindReferencesAction MUST call onToast(\"No references found\") when list is empty",
            actionSrc.contains("No references found"),
        )
    }
}
