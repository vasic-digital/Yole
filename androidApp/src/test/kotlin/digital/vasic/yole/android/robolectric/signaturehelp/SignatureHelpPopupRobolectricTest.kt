/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test (iter-63 Phase 7, CONST-035 anti-bluff):
 *   Verifies that SignatureHelpPopup.kt has the correct structural wiring
 *   for a floating Compose Popup anchored near the cursor, showing the
 *   active signature with active-parameter bold and optional param-doc.
 *
 *   Test architecture: source-level structural assertions (mirrors
 *   HoverPopupRobolectricTest / Phase 6 pattern). createComposeRule() is
 *   avoided — `manifest = Config.NONE` runs do not provide an Activity.
 *
 *   Anti-bluff mutation guards (CONST-035):
 *
 *   popupHasTestTag:
 *     Mutation: remove testTag("signature-popup") →
 *     FAILS because the literal disappears from source.
 *
 *   popupUsesComposePopup:
 *     Mutation: replace Popup() with a Box → FAILS; "Popup(" gone.
 *     Mutation: remove Alignment.TopStart → second assertion FAILS.
 *
 *   activeParam_isHighlightedBold:
 *     Mutation: remove SpanStyle(fontWeight = FontWeight.Bold) →
 *     FAILS because "FontWeight.Bold" disappears.
 *
 *   paramDocIsRenderedWhenPresent:
 *     Mutation: remove the paramDoc Text block entirely → FAILS because
 *     "signature-popup-paramdoc" testTag disappears.
 *
 *   popup_rendersNothingWhenInfoIsNull:
 *     Mutation: remove null / empty guard → FAILS because guard disappears.
 *
 * Cross-platform impact (CONST-037):
 *   - Android: tested here.
 *   - Desktop: same Popup API; Phase 10 integration will wire Desktop.
 *   - iOS/Web: N/A this phase.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.android.robolectric.signaturehelp

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SignatureHelpPopupRobolectricTest {

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
        "androidApp/src/main/java/digital/vasic/yole/android/ui/editor/signaturehelp/SignatureHelpPopup.kt",
    )

    // -----------------------------------------------------------------------
    // Test 1: popupHasTestTag
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: remove testTag("signature-popup") →
     * FAILS because literal disappears from source.
     *
     * Mutation: remove testTag → FAIL.
     */
    @Test
    fun popupHasTestTag() {
        val src = loadPopupSource()
        assertTrue(
            """SignatureHelpPopup MUST apply testTag("signature-popup") to its root Column""",
            src.contains(""""signature-popup""""),
        )
    }

    // -----------------------------------------------------------------------
    // Test 2: popupUsesComposePopup
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: replace Popup() with a Box → FAILS; Popup( disappears.
     * Anti-bluff: remove anchorOffset → "offset = anchorOffset" disappears → FAILS.
     *
     * Mutation: stub to Box → FAIL.
     */
    @Test
    fun popupUsesComposePopup() {
        val src = loadPopupSource()
        assertTrue(
            "SignatureHelpPopup MUST use androidx.compose.ui.window.Popup",
            src.contains("Popup("),
        )
        assertTrue(
            "SignatureHelpPopup MUST anchor at Alignment.TopStart",
            src.contains("Alignment.TopStart"),
        )
        assertTrue(
            "SignatureHelpPopup MUST use anchorOffset for positioning",
            src.contains("offset = anchorOffset"),
        )
        assertTrue(
            "SignatureHelpPopup MUST wire onDismissRequest = onDismiss",
            src.contains("onDismissRequest = onDismiss"),
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
        val src = loadPopupSource()
        assertTrue(
            "SignatureHelpPopup MUST use buildAnnotatedString for the signature label",
            src.contains("buildAnnotatedString"),
        )
        assertTrue(
            "SignatureHelpPopup MUST apply SpanStyle with FontWeight.Bold for active parameter",
            src.contains("FontWeight.Bold"),
        )
        assertTrue(
            "SignatureHelpPopup MUST call resolveActiveParamSpan to locate the active param",
            src.contains("resolveActiveParamSpan("),
        )
    }

    // -----------------------------------------------------------------------
    // Test 4: paramDocIsRenderedWhenPresent
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: remove the paramDoc Text block → "signature-popup-paramdoc"
     * testTag disappears → FAILS.
     *
     * Mutation: remove paramDoc block → FAIL.
     */
    @Test
    fun paramDocIsRenderedWhenPresent() {
        val src = loadPopupSource()
        assertTrue(
            "SignatureHelpPopup MUST render parameter documentation when present",
            src.contains(""""signature-popup-paramdoc""""),
        )
        assertTrue(
            "SignatureHelpPopup MUST guard paramDoc rendering with isNullOrBlank check",
            src.contains("isNullOrBlank()"),
        )
        assertTrue(
            "SignatureHelpPopup MUST show a HorizontalDivider between label and paramDoc",
            src.contains("HorizontalDivider("),
        )
    }

    // -----------------------------------------------------------------------
    // Test 5: popup_rendersNothingWhenInfoIsNull
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: remove null / empty guard from SignatureHelpPopup →
     * FAILS because "info == null" disappears from source.
     *
     * Mutation: remove early return → FAIL.
     */
    @Test
    fun popup_rendersNothingWhenInfoIsNull() {
        val src = loadPopupSource()
        assertTrue(
            "SignatureHelpPopup MUST guard against null info (early return)",
            src.contains("info == null"),
        )
        assertTrue(
            "SignatureHelpPopup MUST guard against empty signatures list",
            src.contains("signatures.isEmpty()"),
        )
    }

    // -----------------------------------------------------------------------
    // Test 6: popupHasSizeConstraints
    // -----------------------------------------------------------------------

    /**
     * Anti-bluff: removing widthIn/heightIn causes the popup to fill the
     * screen. FAILS when constraints disappear from source.
     *
     * Mutation: remove widthIn/heightIn → FAIL.
     */
    @Test
    fun popupHasSizeConstraints() {
        val src = loadPopupSource()
        assertTrue(
            "SignatureHelpPopup MUST apply widthIn(max = ...) constraint",
            src.contains("widthIn(max"),
        )
        assertTrue(
            "SignatureHelpPopup MUST apply heightIn(max = ...) constraint",
            src.contains("heightIn(max"),
        )
        assertTrue(
            "SignatureHelpPopup max width MUST be 480 dp",
            src.contains("480.dp"),
        )
        assertTrue(
            "SignatureHelpPopup max height MUST be 200 dp",
            src.contains("200.dp"),
        )
    }
}
