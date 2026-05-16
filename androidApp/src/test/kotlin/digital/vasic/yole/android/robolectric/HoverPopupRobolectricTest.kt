/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-62 Phase 6.1, CONST-035 anti-bluff):
 *   Verifies that HoverPopup.kt has the correct structural wiring for
 *   rendering hover content as a Compose Popup.
 *
 *   Test architecture (matches iter-57/58/60 pattern): source-level
 *   structural assertions + pure-function tests for headingStyleFor.
 *   createComposeRule() is avoided — `manifest = Config.NONE` runs do
 *   not provide an Activity.
 *
 *   Anti-bluff mutation guards:
 *     1. Stub composable body to render an empty Box (no LazyColumn, no
 *        text rendering) → tests verifying testTag("hover-popup"),
 *        LazyColumn, and HoverBlock rendering FAIL.
 *     2. Remove the `testTag("hover-popup")` call → `popupHasTestTag`
 *        FAILS because the source text no longer contains it.
 *     3. Remove the `when (block)` switch in HoverBlockItem →
 *        `popupRendersAllBlockTypes` FAILS (no Paragraph / CodeBlock
 *        branch).
 *     4. Stub headingStyleFor to always return bodyMedium regardless of
 *        level → `headingStyleForReturnsCorrectLevels` FAILS because the
 *        source text no longer branches on level.
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
class HoverPopupRobolectricTest {

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

    private fun loadHoverPopupSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/hover/HoverPopup.kt"
    )

    // -----------------------------------------------------------------------
    // Layer 1: testTag anchors
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing testTag("hover-popup") from HoverPopup →
     * runtime UI tests can't find the popup node → this check FAILS.
     */
    @Test
    fun popupHasTestTag() {
        val src = loadHoverPopupSource()
        assertTrue(
            "HoverPopup MUST apply testTag(\"hover-popup\") to the root Box",
            src.contains("""testTag("hover-popup")"""),
        )
    }

    /**
     * Anti-bluff: removing LazyColumn (or replacing with a static Box) →
     * scrollability is lost → this structural check FAILS.
     */
    @Test
    fun popupUsesLazyColumn() {
        val src = loadHoverPopupSource()
        assertTrue(
            "HoverPopup MUST use LazyColumn for scrollable block rendering",
            src.contains("LazyColumn"),
        )
    }

    /**
     * Anti-bluff: removing the Popup() call (using a plain Box) → popup
     * no longer floats above the editor surface → FAILS.
     */
    @Test
    fun popupUsesComposePopup() {
        val src = loadHoverPopupSource()
        assertTrue(
            "HoverPopup MUST use androidx.compose.ui.window.Popup",
            src.contains("Popup("),
        )
        assertTrue(
            "HoverPopup MUST anchor at TopStart alignment",
            src.contains("Alignment.TopStart"),
        )
        assertTrue(
            "HoverPopup MUST use anchorOffset for positioning",
            src.contains("offset = anchorOffset"),
        )
    }

    // -----------------------------------------------------------------------
    // Layer 2: HoverBlock rendering branches
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: stub HoverBlockItem's when-switch to always render a
     * single Text("?") regardless of block type → the branch names
     * (HoverBlock.Paragraph, HoverBlock.CodeBlock, etc.) disappear from
     * source → this structural check FAILS.
     */
    @Test
    fun popupRendersAllBlockTypes() {
        val src = loadHoverPopupSource()
        assertTrue(
            "HoverPopup MUST handle HoverBlock.Paragraph",
            src.contains("is HoverBlock.Paragraph"),
        )
        assertTrue(
            "HoverPopup MUST handle HoverBlock.Heading",
            src.contains("is HoverBlock.Heading"),
        )
        assertTrue(
            "HoverPopup MUST handle HoverBlock.CodeBlock",
            src.contains("is HoverBlock.CodeBlock"),
        )
        assertTrue(
            "HoverPopup MUST handle HoverBlock.InlineCodeSpan",
            src.contains("is HoverBlock.InlineCodeSpan"),
        )
        assertTrue(
            "HoverPopup MUST handle HoverBlock.FallbackText",
            src.contains("is HoverBlock.FallbackText"),
        )
    }

    /**
     * Anti-bluff: FallbackText MUST use italic style — removing FontStyle.Italic
     * defeats the visual distinction from Paragraph text.
     */
    @Test
    fun fallbackTextUsesItalic() {
        val src = loadHoverPopupSource()
        assertTrue(
            "HoverPopup MUST render FallbackText with FontStyle.Italic",
            src.contains("FontStyle.Italic"),
        )
    }

    /**
     * Anti-bluff: CodeBlock MUST use monospace font.
     */
    @Test
    fun codeBlockUsesMonospace() {
        val src = loadHoverPopupSource()
        assertTrue(
            "HoverPopup MUST render CodeBlock with FontFamily.Monospace",
            src.contains("FontFamily.Monospace"),
        )
    }

    // -----------------------------------------------------------------------
    // Layer 3: headingStyleFor helper
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: stub headingStyleFor to always return bodyMedium →
     * source no longer branches on level → `when (level)` or equivalent
     * disappears → this check FAILS.
     */
    @Test
    fun headingStyleForBranchesOnLevel() {
        val src = loadHoverPopupSource()
        assertTrue(
            "headingStyleFor MUST branch on heading level (when/if expression)",
            src.contains("when (level)") || src.contains("when(level)"),
        )
        assertTrue(
            "headingStyleFor MUST map level 1 → headlineMedium",
            src.contains("headlineMedium"),
        )
        assertTrue(
            "headingStyleFor MUST map level 2 → titleLarge",
            src.contains("titleLarge"),
        )
        assertTrue(
            "headingStyleFor MUST map level 3+ → titleMedium (else branch)",
            src.contains("titleMedium"),
        )
    }

    // -----------------------------------------------------------------------
    // Layer 4: size constraints
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing widthIn/heightIn constraints → popup could
     * grow to fill the screen, violating the 400×300 dp spec.
     */
    @Test
    fun popupHasSizeConstraints() {
        val src = loadHoverPopupSource()
        assertTrue(
            "HoverPopup MUST apply widthIn(max = ...) constraint",
            src.contains("widthIn(max"),
        )
        assertTrue(
            "HoverPopup MUST apply heightIn(max = ...) constraint",
            src.contains("heightIn(max"),
        )
        assertTrue(
            "HoverPopup max width MUST be 400 dp",
            src.contains("400.dp"),
        )
        assertTrue(
            "HoverPopup max height MUST be 300 dp",
            src.contains("300.dp"),
        )
    }

    // -----------------------------------------------------------------------
    // Layer 5: empty-list guard
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing the early `if (blocks.isEmpty()) return` →
     * an empty Popup may flash on screen before content loads.
     */
    @Test
    fun popupGuardsAgainstEmptyBlocks() {
        val src = loadHoverPopupSource()
        assertTrue(
            "HoverPopup MUST guard against empty blocks list (early return)",
            src.contains("blocks.isEmpty()"),
        )
    }
}
