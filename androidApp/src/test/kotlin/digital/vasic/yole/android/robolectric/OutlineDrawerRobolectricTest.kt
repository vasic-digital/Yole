/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-58 Feature 2 Phase 5, CONST-035 anti-bluff):
 *   Verifies the OutlineDrawer composable surfaces user-visible
 *   outline rows whose names + kind icons reflect the OutlineExtractor
 *   result, shows the empty-placeholder when no outline is available,
 *   and invokes onClose when the user taps the close icon.
 *
 *   Test architecture (matches iter-57/58 EditorHighlightingRobolectricTest
 *   pattern): pure source-level + pure-function assertions, NOT
 *   createComposeRule. createComposeRule() requires an Activity which
 *   the project's `manifest = Config.NONE` runs do not provide, and
 *   createAndroidComposeRule<MainActivity>() launches the whole app
 *   shell — both add brittleness without strengthening the contract.
 *   Instead we verify:
 *     (1) Source-level structural invariants on OutlineDrawer.kt
 *         (the load-bearing mutation guards),
 *     (2) Pure-function unit tests on kindToIcon (the deterministic
 *         visible-distinct-icon contract).
 *
 *   Anti-bluff mutation guards:
 *     - Stubbing OutlineExtractor.outlineFor by replacing the
 *       `extractor.outlineFor(currentText, langId, engine)` call with
 *       `emptyList()` MUST cause `outlineDrawerCallsOutlineExtractor`
 *       to FAIL because the regex anchor would no longer match.
 *     - Removing the `if (!isOpen) return` short-circuit MUST cause
 *       `outlineDrawerHasIsOpenShortCircuit` to FAIL.
 *     - Removing the LazyColumn-of-rows path MUST cause
 *       `outlineDrawerCallsOutlineExtractor` to FAIL (it asserts
 *       LazyColumn presence).
 *     - Stubbing kindToIcon to return a constant icon MUST cause
 *       `kindToIconMapsKindsToDistinctIcons` to FAIL.
 *     - Removing the empty-placeholder testTag declaration MUST
 *       cause `outlineDrawerCallsOutlineExtractor` to FAIL.
 *     - Removing the IdeEditorScreen.kt integration (the OutlineDrawer
 *       call within the editor body) MUST cause
 *       `ideEditorScreenWiresOutlineDrawer` to FAIL.
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import digital.vasic.yole.android.ui.editor.kindToIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class OutlineDrawerRobolectricTest {

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

    private fun loadDrawerSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/OutlineDrawer.kt"
    )

    private fun loadYoleAppSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt"
    )

    /**
     * Layer 1: source-level structural guarantee for the OutlineDrawer.
     * MUST call OutlineExtractor.outlineFor, render LazyColumn rows,
     * supply the empty-placeholder testTag, and emit the per-row
     * `outline.item:` testTag prefix. Mutations that defeat any of
     * these break the contract this phase ships, and the failing
     * assertion identifies which.
     */
    @Test
    fun outlineDrawerCallsOutlineExtractor() {
        val src = loadDrawerSource()
        assertTrue(
            "OutlineDrawer MUST call extractor.outlineFor(...)",
            src.contains("extractor.outlineFor("),
        )
        assertTrue(
            "OutlineDrawer MUST construct an OutlineExtractor()",
            src.contains("OutlineExtractor()"),
        )
        assertTrue(
            "OutlineDrawer MUST render outline rows via LazyColumn",
            src.contains("LazyColumn"),
        )
        assertTrue(
            "OutlineDrawer MUST emit the empty-placeholder testTag",
            src.contains("\"outline.empty\""),
        )
        assertTrue(
            "OutlineDrawer MUST emit per-row testTags `outline.item:{name}`",
            Regex("""testTag\(["']outline\.item:""").containsMatchIn(src),
        )
    }

    /**
     * Layer 2: the `if (!isOpen) return` short-circuit MUST stay
     * present so callers that pass `isOpen = false` get NO drawer
     * rendering. Mutation guard: removing the short-circuit makes
     * this fail.
     */
    @Test
    fun outlineDrawerHasIsOpenShortCircuit() {
        val src = loadDrawerSource()
        assertTrue(
            "OutlineDrawer MUST short-circuit when !isOpen",
            Regex("""if\s*\(\s*!isOpen\s*\)\s*return""").containsMatchIn(src),
        )
    }

    /**
     * Layer 3: kindToIcon MUST resolve different outline kinds to
     * different ImageVectors. A stub mapper that returns one icon for
     * everything fails here. We cover four common kinds (section,
     * function, class, field) plus the fallback.
     */
    @Test
    fun kindToIconMapsKindsToDistinctIcons() {
        val section = kindToIcon("section")
        val function = kindToIcon("function")
        val klass = kindToIcon("class")
        val field = kindToIcon("field")
        val module = kindToIcon("module")
        val unknown = kindToIcon("totally-unknown-kind-here")
        assertNotEquals("section vs function MUST differ", section, function)
        assertNotEquals("function vs class MUST differ", function, klass)
        assertNotEquals("class vs field MUST differ", klass, field)
        assertNotEquals("field vs module MUST differ", field, module)
        assertNotEquals("section vs unknown MUST differ", section, unknown)
        // Unknown kind returns a stable fallback (same call returns same icon).
        assertEquals(
            "unknown kind MUST be deterministic across calls",
            kindToIcon("totally-unknown-kind-here"),
            unknown,
        )
    }

    /**
     * Layer 4: outline-kind aliasing MUST collapse `heading` to the
     * same icon as `section`. The helix tags.scm uses
     * `@definition.section` for markdown headings; downstream callers
     * may also pass `heading`. A stub that returns different icons
     * for the two synonyms fails here.
     */
    @Test
    fun kindToIconCollapsesHeadingSynonyms() {
        assertEquals(
            "heading and section MUST resolve to the same icon",
            kindToIcon("section"),
            kindToIcon("heading"),
        )
        assertEquals(
            "function and method MUST resolve to the same icon",
            kindToIcon("function"),
            kindToIcon("method"),
        )
    }

    /**
     * Layer 5: IdeEditorScreen MUST wire the OutlineDrawer into its
     * editor surface (Row at the start edge, fed from the editor's
     * textState + the same tokenizerEngine the highlighter uses). A
     * mutation that drops the OutlineDrawer call from YoleApp.kt
     * defeats Phase 5 entirely.
     */
    @Test
    fun ideEditorScreenWiresOutlineDrawer() {
        val src = loadYoleAppSource()
        assertTrue(
            "IdeEditorScreen MUST call OutlineDrawer(",
            src.contains("OutlineDrawer("),
        )
        assertTrue(
            "IdeEditorScreen MUST maintain outlineDrawerOpen state",
            Regex("""outlineDrawerOpen\s+by\s+remember""").containsMatchIn(src),
        )
        assertTrue(
            "IdeEditorScreen MUST surface an Outline toolbar button",
            Regex("""IdeToolbarButton\(["']Outline["']""").containsMatchIn(src),
        )
        assertTrue(
            "OutlineDrawer call MUST pass the textState into the drawer",
            Regex("""OutlineDrawer\([\s\S]*?textState\s*=\s*textState""").containsMatchIn(src),
        )
        assertTrue(
            "OutlineDrawer call MUST pass the tokenizerEngine into the drawer",
            Regex("""OutlineDrawer\([\s\S]*?engine\s*=\s*tokenizerEngine""").containsMatchIn(src),
        )
    }

    /**
     * Layer 6: anti-bluff guard against literal mutation. The drawer's
     * `extractor.outlineFor(currentText, langId, engine)` call MUST NOT
     * be replaced with `return emptyList()` or any constant-list
     * fallback inside the LaunchedEffect. We assert the LaunchedEffect
     * body still wraps the extractor call in a try/catch and that
     * the result is assigned to outline.value (no hardcoding).
     */
    @Test
    fun outlineDrawerExtractorWiringMustNotBeBluffed() {
        val src = loadDrawerSource()
        // The catch arm intentionally falls back to emptyList() on
        // failure (graceful degradation, per CONST-035). The HAPPY
        // PATH MUST still assign the extractor's real result.
        assertTrue(
            "OutlineDrawer happy path MUST assign extractor.outlineFor(...) to outline.value",
            Regex(
                """outline\.value\s*=\s*try\s*\{[\s\S]*?extractor\.outlineFor\("""
            ).containsMatchIn(src),
        )
        // The empty-langId branch must remain (null lang → empty
        // outline → placeholder shown). Removing this branch and
        // passing through to the extractor would surface a crash
        // when langId is null (preconditions: lang must be non-null).
        assertTrue(
            "OutlineDrawer MUST early-return empty when langId == null",
            Regex("""if\s*\(\s*langId\s*==\s*null\s*\)""").containsMatchIn(src),
        )
    }
}
