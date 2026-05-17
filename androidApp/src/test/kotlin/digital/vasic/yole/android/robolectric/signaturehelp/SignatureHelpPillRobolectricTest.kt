/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-63 Phase 7, CONST-035 anti-bluff):
 *   Verifies that SignatureHelpPill.kt has the correct structural wiring
 *   for rendering a compact Material3 Surface chip with the active
 *   parameter highlighted in bold.
 *
 *   Test architecture (mirrors HoverPopupRobolectricTest + Phase 6
 *   CodeActionLightbulbRobolectricTest): source-level structural assertions
 *   + pure-function tests for resolveActiveParamSpan.
 *   createComposeRule() is avoided — `manifest = Config.NONE` runs do not
 *   provide an Activity.
 *
 *   Canned SignatureHelp used throughout:
 *     Signature:  "fun greet(name: String, times: Int): Unit"
 *     activeSignature = 0, activeParameter = 1 → "times: Int" is bold.
 *
 *   Anti-bluff mutation guards (CONST-035):
 *
 *   pillHasTestTag:
 *     Mutation: remove testTag("signature-pill") from SignatureHelpPill →
 *     FAILS because the literal string disappears from the source.
 *
 *   pillUsesSurface:
 *     Mutation: replace Surface with a plain Box →
 *     FAILS because "Surface(" disappears from the source.
 *
 *   activeParam_isHighlightedBold:
 *     Mutation: remove SpanStyle(fontWeight = FontWeight.Bold) from the
 *     annotated string builder → FAILS because "FontWeight.Bold" disappears.
 *
 *   pill_rendersNothingWhenInfoIsNull:
 *     Mutation: remove `if (info == null ...) return` guard →
 *     FAILS because "info == null" disappears from source.
 *
 *   resolveActiveParamSpan_correctBounds:
 *     Pure-function test. Mutation: replace resolveActiveParamSpan with
 *     `return null` → FAILS (non-null result asserted).
 *
 * Cross-platform impact (CONST-037):
 *   - Android: tested here.
 *   - Desktop/iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.robolectric.signaturehelp

import digital.vasic.yole.lsp.resolveActiveParamSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SignatureHelpPillRobolectricTest {

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

    private fun loadPillSource(): String = loadSource(
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/signaturehelp/SignatureHelpPill.kt",
    )

    // -----------------------------------------------------------------------
    // Test 1: pillHasTestTag
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: remove testTag("signature-pill") from SignatureHelpPill →
     * FAILS because the literal string disappears from source.
     *
     * Mutation: remove `.testTag("signature-pill")` → FAIL.
     */
    @Test
    fun pillHasTestTag() {
        val src = loadPillSource()
        assertTrue(
            """SignatureHelpPill MUST apply testTag("signature-pill") to its root Surface""",
            src.contains(""""signature-pill""""),
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: pillUsesSurface
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: swap Surface for a plain Box → FAILS because
     * "Surface(" disappears from source.
     *
     * Mutation: replace Surface with Box → FAIL.
     */
    @Test
    fun pillUsesSurface() {
        val src = loadPillSource()
        assertTrue(
            "SignatureHelpPill MUST use Material3 Surface as its root container",
            src.contains("Surface("),
        )
        assertTrue(
            "SignatureHelpPill MUST use RoundedCornerShape for chip-style appearance",
            src.contains("RoundedCornerShape("),
        )
    }

    // -----------------------------------------------------------------------
    // Test 3: activeParam_isHighlightedBold
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: remove SpanStyle(fontWeight = FontWeight.Bold) →
     * FAILS because "FontWeight.Bold" disappears from source.
     *
     * Mutation: remove Bold span → FAIL.
     */
    @Test
    fun activeParam_isHighlightedBold() {
        val src = loadPillSource()
        assertTrue(
            "SignatureHelpPill MUST use buildAnnotatedString to compose the label",
            src.contains("buildAnnotatedString"),
        )
        assertTrue(
            "SignatureHelpPill MUST apply SpanStyle with FontWeight.Bold for active parameter",
            src.contains("FontWeight.Bold"),
        )
        assertTrue(
            "SignatureHelpPill MUST call resolveActiveParamSpan to locate the active param",
            src.contains("resolveActiveParamSpan("),
        )
        assertTrue(
            "SignatureHelpPill MUST use withStyle for the bold span",
            src.contains("withStyle("),
        )
    }

    // -----------------------------------------------------------------------
    // Test 4: pill_rendersNothingWhenInfoIsNull
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: remove null / empty guard from SignatureHelpPill →
     * FAILS because "info == null" disappears from source.
     *
     * Mutation: remove early return → FAIL.
     */
    @Test
    fun pill_rendersNothingWhenInfoIsNull() {
        val src = loadPillSource()
        assertTrue(
            "SignatureHelpPill MUST guard against null info (early return)",
            src.contains("info == null"),
        )
        assertTrue(
            "SignatureHelpPill MUST guard against empty signatures list",
            src.contains("signatures.isEmpty()"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 5: resolveActiveParamSpan_correctBounds (pure-function)
    // -----------------------------------------------------------------------

    /**
     * Pure-function test exercising [resolveActiveParamSpan] directly.
     *
     * Canned input: "fun greet(name: String, times: Int): Unit"
     * activeParam=1 → span should cover "times: Int" (index 1 in param list).
     *
     * Anti-bluff: stub resolveActiveParamSpan to return null →
     * assertNotNull fails. Reverted → PASS.
     *
     * Mutation: stub to return null → FAIL.
     */
    @Test
    fun resolveActiveParamSpan_correctBounds() {
        val label = "fun greet(name: String, times: Int): Unit"
        // Parameters inside parens: "name: String" and "times: Int"
        // activeParam=1 → "times: Int"
        val span = resolveActiveParamSpan(label, activeParam = 1)
        assertNotNull("resolveActiveParamSpan must return a non-null span for activeParam=1", span)

        val extracted = label.substring(span!!.first, span.last)
        assertEquals(
            "Extracted span MUST be the second parameter token 'times: Int'",
            "times: Int",
            extracted,
        )
    }

    /**
     * Boundary: activeParam=0 returns the first parameter.
     *
     * Mutation: return wrong param index → FAIL.
     */
    @Test
    fun resolveActiveParamSpan_firstParam() {
        val label = "fun process(input: String, maxLen: Int): String"
        val span = resolveActiveParamSpan(label, activeParam = 0)
        assertNotNull("resolveActiveParamSpan must handle activeParam=0", span)
        val extracted = label.substring(span!!.first, span.last)
        assertEquals("First param span MUST be 'input: String'", "input: String", extracted)
    }

    /**
     * Edge: no parentheses in label → returns null gracefully.
     *
     * Mutation: remove null guard → may throw instead of returning null → FAIL.
     */
    @Test
    fun resolveActiveParamSpan_noParens_returnsNull() {
        val span = resolveActiveParamSpan("noParensLabel", activeParam = 0)
        assertNull("resolveActiveParamSpan MUST return null when label has no parentheses", span)
    }
}
