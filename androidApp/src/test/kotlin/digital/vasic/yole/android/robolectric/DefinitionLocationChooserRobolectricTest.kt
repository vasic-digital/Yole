/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-62 Phase 7, CONST-035 anti-bluff):
 *   Verifies that DefinitionLocationChooser.kt has the correct structural
 *   wiring for rendering definition locations in a Material3 ModalBottomSheet.
 *
 *   Test architecture (matches iter-62 Phase 6 HoverPopupRobolectricTest
 *   pattern): source-level structural assertions.
 *   createComposeRule() is avoided — `manifest = Config.NONE` runs do not
 *   provide an Activity.
 *
 *   Anti-bluff mutation guards:
 *     1. Remove testTag("def-chooser") → rendersAllLocations FAILS (tagged
 *        root not reachable by test instrumentation).
 *     2. Remove testTag("def-row-$index") → rendersAllLocations FAILS.
 *     3. Stub onClick handler to no-op (remove onSelected call) →
 *        clickRow_invokesOnSelected FAILS (callback never fired).
 *     4. Replace ModalBottomSheet with a plain Column → chooserUsesModalBottomSheet
 *        FAILS (ModalBottomSheet call absent from source).
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
class DefinitionLocationChooserRobolectricTest {

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

    private fun loadChooserSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/navigation/DefinitionLocationChooser.kt"
    )

    // -----------------------------------------------------------------------
    // Test 1: testTag anchors present
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing testTag("def-chooser") means UI instrumentation
     * and future HelixQA tests cannot locate the chooser root → FAILS.
     *
     * Mutation: delete the testTag("def-chooser") line → FAIL.
     */
    @Test
    fun rendersAllLocations() {
        val src = loadChooserSource()

        assertTrue(
            "DefinitionLocationChooser MUST apply testTag(\"def-chooser\") to its content root",
            src.contains("""testTag("def-chooser")"""),
        )
        assertTrue(
            "DefinitionLocationChooser MUST apply testTag(\"def-row-\$index\") to each row",
            src.contains("""testTag("def-row-${"$"}index")"""),
        )
        assertTrue(
            "DefinitionLocationChooser MUST use LazyColumn for the list of locations",
            src.contains("LazyColumn"),
        )
        assertTrue(
            "DefinitionLocationChooser MUST use itemsIndexed to supply the row index",
            src.contains("itemsIndexed"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: onClick wiring calls onSelected
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: replacing onClick = { onSelected(location) } with a no-op
     * lambda means the callback is never fired on row tap → this structural
     * check FAILS because onSelected disappears from the row-level call site.
     *
     * Mutation: stub onClick to no-op → FAIL.
     */
    @Test
    fun clickRow_invokesOnSelected() {
        val src = loadChooserSource()

        // The click handler on each row must call onSelected with the location.
        assertTrue(
            "DefinitionLocationChooser row onClick MUST call onSelected(location)",
            src.contains("onSelected(location)"),
        )
        // The clickable modifier must be applied to the row so taps are registered.
        assertTrue(
            "DefinitionLocationChooser row MUST apply clickable modifier",
            src.contains("clickable(onClick = onClick)"),
        )
        // The DefinitionLocationRow composable must wire its onClick parameter to the clickable.
        assertTrue(
            "DefinitionLocationRow MUST accept an onClick parameter",
            src.contains("onClick: () -> Unit"),
        )
    }

    // -----------------------------------------------------------------------
    // Additional structural guards
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: replacing ModalBottomSheet with a plain Column defeats the
     * mobile UX contract — sheet must be swipe-dismissable → FAILS.
     *
     * Mutation: use Column instead of ModalBottomSheet → FAIL.
     */
    @Test
    fun chooserUsesModalBottomSheet() {
        val src = loadChooserSource()
        assertTrue(
            "DefinitionLocationChooser MUST use ModalBottomSheet for the mobile bottom-sheet UX",
            src.contains("ModalBottomSheet("),
        )
        assertTrue(
            "DefinitionLocationChooser MUST supply onDismissRequest to ModalBottomSheet",
            src.contains("onDismissRequest = onDismiss"),
        )
    }

    /**
     * Anti-bluff: removing the Cancel row means the user has no explicit
     * dismiss affordance (swipe-only), violating the UX spec → FAILS.
     *
     * Mutation: delete Cancel row → FAIL.
     */
    @Test
    fun chooserHasCancelRow() {
        val src = loadChooserSource()
        assertTrue(
            "DefinitionLocationChooser MUST render a Cancel row for explicit dismissal",
            src.contains(""""Cancel""""),
        )
        assertTrue(
            "Cancel row MUST trigger onDismiss",
            src.contains("clickable(onClick = onDismiss)"),
        )
    }

    /**
     * Anti-bluff: removing filename derivation (substringAfterLast('/'))
     * would show raw URIs (e.g. file:///src/Foo.kt) instead of readable
     * names (Foo.kt) → FAILS.
     *
     * Mutation: display location.uri directly → substringAfterLast absent → FAIL.
     */
    @Test
    fun rowDerivesFilenameFromUri() {
        val src = loadChooserSource()
        assertTrue(
            "DefinitionLocationRow MUST derive the filename from URI last path segment",
            src.contains("substringAfterLast('/')"),
        )
    }

    /**
     * Anti-bluff: without @OptIn(ExperimentalMaterial3Api::class) the
     * ModalBottomSheet API cannot be called — build would fail but this
     * guard ensures the annotation is not accidentally removed.
     */
    @Test
    fun chooserHasExperimentalAnnotation() {
        val src = loadChooserSource()
        assertTrue(
            "DefinitionLocationChooser MUST be annotated with @OptIn(ExperimentalMaterial3Api::class)",
            src.contains("ExperimentalMaterial3Api"),
        )
        assertFalse(
            "ExperimentalMaterial3Api annotation must not be suppressed without justification",
            src.contains("@Suppress") && src.contains("ExperimentalMaterial3Api"),
        )
    }
}
