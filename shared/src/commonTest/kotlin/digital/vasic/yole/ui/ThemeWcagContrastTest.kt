/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 3b: anchor test for CAP-033 — replaces the deleted
 * ThemeAccessibilityTests::light theme TextPrimary on SurfacePrimary meets
 * WCAG AA. After Phase 3b, the "primary text on primary surface" semantic
 * is resolved via VS Code keys (editor.foreground vs editor.background),
 * sourced from LegacyThemeBridge.legacyLight which still encodes the
 * historical palette byte-for-byte (and is byte-equal to Yole-Light.json
 * via LegacyThemeParityTest).
 *
 *########################################################*/
package digital.vasic.yole.ui

import androidx.compose.ui.graphics.Color
import digital.vasic.yole.syntax.theme.LegacyThemeBridge
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WCAG AA contrast guarantee for the light-theme primary text on the primary
 * surface — i.e., editor.foreground vs editor.background.
 *
 * Pre-iter-57 wording: "light theme TextPrimary on SurfacePrimary meets
 * WCAG AA". The semantic carries forward; only the value source changes
 * from `YoleColors.TextPrimary`/`SurfacePrimary` to LegacyThemeBridge.
 */
class ThemeWcagContrastTest {

    /**
     * Anchor for CAP-033. Mutation-verified by flipping the ARGB byte for
     * editor.foreground in LegacyThemeBridge — any change reducing contrast
     * below 4.5 must fail this test.
     */
    @Test
    fun `light theme TextPrimary on SurfacePrimary meets WCAG AA`() {
        val fg = Color(LegacyThemeBridge.legacyLight.getValue("editor.foreground"))
        val bg = Color(LegacyThemeBridge.legacyLight.getValue("editor.background"))
        assertTrue(
            ThemeUtils.meetsWcagAA(fg, bg),
            "editor.foreground on editor.background must meet WCAG AA (≥ 4.5 ratio). " +
                "Got contrast = ${ThemeUtils.calculateContrastRatio(fg, bg)}"
        )
    }

    /**
     * Companion test for the dark theme — same semantic, dark palette.
     */
    @Test
    fun `dark theme TextPrimary on SurfacePrimary meets WCAG AA`() {
        val fg = Color(LegacyThemeBridge.legacyDark.getValue("editor.foreground"))
        val bg = Color(LegacyThemeBridge.legacyDark.getValue("editor.background"))
        assertTrue(
            ThemeUtils.meetsWcagAA(fg, bg),
            "Dark editor.foreground on editor.background must meet WCAG AA (≥ 4.5 ratio). " +
                "Got contrast = ${ThemeUtils.calculateContrastRatio(fg, bg)}"
        )
    }
}
