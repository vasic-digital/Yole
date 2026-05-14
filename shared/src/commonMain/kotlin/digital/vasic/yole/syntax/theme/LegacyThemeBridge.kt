/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 2: one-shot migration helper. Records the legacy
 * "Ide / Yole" palette values from the pre-iter-57 codebase (now
 * deleted) keyed by their VS Code JSON equivalents per spec §3.8.
 *
 * Used by LegacyThemeParityTest to assert byte-exact ARGB parity
 * between Yole-Light/Dark.json and the historical palette.
 *
 * DELETED in a later iteration after migration confidence is high
 * (per spec §4 row "LegacyThemeBridge").
 *
 *########################################################*/
package digital.vasic.yole.syntax.theme

/**
 * One-shot migration bridge: legacy "Ide / Yole" palette indexed
 * by VS Code colors.* key. Values are ARGB ints (0xFFRRGGBB) — the same
 * bit-layout returned by [Theme.uiColor].
 *
 * Source of truth for values: the pre-iter-57 palette object that used
 * to live in `shared/src/commonMain/kotlin/digital/vasic/yole/ui/Theme.kt`.
 * Every constant below was hand-verified against that file at the time of
 * the iter-57 Phase 2 commit; the file itself was removed in Phase 3b.
 *
 * Mapping notes (ambiguous cases documented):
 * - `activityBar.background` uses the legacy DarkActivityBar / LightActivityBar
 *   value (not SurfaceVariant) because the activity bar semantic maps to the
 *   sidebar icon strip in VS Code — the visual left-most panel, not the
 *   general surface.
 * - `statusBar.background` uses the legacy DarkMenuHover / LightMenuHover value
 *   because "menu hover" in the legacy palette was used as the status bar tint
 *   (same value as currentLine, both 0xFF2A2D2E for dark).
 * - `badge.background` uses the brand primary tint (light 0xFFD32F2F / dark
 *   0xFFEF9A9A) because the badge tint in Yole has always been brand-accent.
 * - `focusBorder` maps to brand primary as the single accent/focus color.
 * - `editorLineNumber.activeForeground` reuses the same muted-text token as
 *   `editorLineNumber.foreground`; Yole legacy did not differentiate active
 *   vs inactive gutter numbers.
 */
@Suppress("MagicNumber")
object LegacyThemeBridge {

    /**
     * Legacy light-mode values mapped to VS Code colors.* keys.
     * Each value is an ARGB int (0xFFRRGGBB).
     *
     * Source values (from the pre-iter-57 palette, now deleted):
     * - LightBackground      = Color(0xFFFFFFFF)
     * - TextPrimary          = Color(0xFF212121)
     * - LightMutedText       = Color(0xFF6E6E6E)
     * - LightMenuHover       = Color(0xFFE8E8E8)
     * - TextSecondary        = Color(0xFF757575)
     * - LightActivityBar     = Color(0xFFDDDDDD)
     * - LightSurface         = Color(0xFFF3F3F3)
     * - LightBorder          = Color(0xFFD4D4D4)
     * - BrandPrimary         = Color(0xFFD32F2F)
     * - LightCurrentLine     = Color(0xFFF0F0F0)
     * - LightSurfaceVariant  = Color(0xFFECECEC)
     */
    val legacyLight: Map<String, Int> = mapOf(
        // Editor area — legacy LightBackground = Color(0xFFFFFFFF)
        "editor.background" to 0xFFFFFFFF.toInt(),
        // Default text — legacy TextPrimary = Color(0xFF212121)
        "editor.foreground" to 0xFF212121.toInt(),
        // Gutter line numbers — legacy LightMutedText = Color(0xFF6E6E6E)
        "editorLineNumber.foreground" to 0xFF6E6E6E.toInt(),
        // Active gutter line (legacy had same value as inactive) — Color(0xFF6E6E6E)
        "editorLineNumber.activeForeground" to 0xFF6E6E6E.toInt(),
        // Status bar — legacy LightMenuHover = Color(0xFFE8E8E8)
        "statusBar.background" to 0xFFE8E8E8.toInt(),
        // Status bar text — legacy TextSecondary = Color(0xFF757575)
        "statusBar.foreground" to 0xFF757575.toInt(),
        // Activity bar (sidebar icon strip) — legacy LightActivityBar = Color(0xFFDDDDDD)
        "activityBar.background" to 0xFFDDDDDD.toInt(),
        // Activity bar icons — legacy TextPrimary = Color(0xFF212121)
        "activityBar.foreground" to 0xFF212121.toInt(),
        // Drawer / side panel — legacy LightSurface = Color(0xFFF3F3F3)
        "sideBar.background" to 0xFFF3F3F3.toInt(),
        // Dialog backgrounds — legacy LightSurface = Color(0xFFF3F3F3)
        "editorWidget.background" to 0xFFF3F3F3.toInt(),
        // Widget/dialog border — legacy LightBorder = Color(0xFFD4D4D4)
        "editorWidget.border" to 0xFFD4D4D4.toInt(),
        // Focus/accent ring — legacy BrandPrimary = Color(0xFFD32F2F)
        "focusBorder" to 0xFFD32F2F.toInt(),
        // Filename badge tint — legacy BrandPrimary = Color(0xFFD32F2F)
        "badge.background" to 0xFFD32F2F.toInt(),
        // Current line highlight — legacy LightCurrentLine = Color(0xFFF0F0F0)
        "editor.lineHighlightBackground" to 0xFFF0F0F0.toInt(),
        // Inactive tab background — legacy LightSurfaceVariant = Color(0xFFECECEC)
        "tab.inactiveBackground" to 0xFFECECEC.toInt(),
        // Active tab background (mirrors editor) — legacy LightBackground = Color(0xFFFFFFFF)
        "tab.activeBackground" to 0xFFFFFFFF.toInt(),
        // Menu hover — legacy LightMenuHover = Color(0xFFE8E8E8)
        "menu.selectionBackground" to 0xFFE8E8E8.toInt(),
    )

    /**
     * Legacy dark-mode values mapped to VS Code colors.* keys.
     * Each value is an ARGB int (0xFFRRGGBB).
     *
     * Source values (from the pre-iter-57 palette, now deleted):
     * - DarkBackground       = Color(0xFF1E1E1E)
     * - DarkTextPrimary      = Color(0xFFFFFFFF)
     * - DarkMutedText        = Color(0xFF5A5A5A)
     * - DarkMenuHover        = Color(0xFF2A2D2E)
     * - DarkTextSecondary    = Color(0xFFB0B0B0)
     * - DarkActivityBar      = Color(0xFF333333)
     * - DarkSurface          = Color(0xFF252526)
     * - DarkBorder           = Color(0xFF3C3C3C)
     * - DarkBrandPrimary     = Color(0xFFEF9A9A)
     * - DarkCurrentLine      = Color(0xFF2A2D2E)
     * - DarkSurfaceVariant   = Color(0xFF2D2D30)
     */
    val legacyDark: Map<String, Int> = mapOf(
        // Editor area — legacy DarkBackground = Color(0xFF1E1E1E)
        "editor.background" to 0xFF1E1E1E.toInt(),
        // Default text — legacy DarkTextPrimary = Color(0xFFFFFFFF)
        "editor.foreground" to 0xFFFFFFFF.toInt(),
        // Gutter line numbers — legacy DarkMutedText = Color(0xFF5A5A5A)
        "editorLineNumber.foreground" to 0xFF5A5A5A.toInt(),
        // Active gutter (legacy same as inactive) — Color(0xFF5A5A5A)
        "editorLineNumber.activeForeground" to 0xFF5A5A5A.toInt(),
        // Status bar — legacy DarkMenuHover = Color(0xFF2A2D2E)
        "statusBar.background" to 0xFF2A2D2E.toInt(),
        // Status bar text — legacy DarkTextSecondary = Color(0xFFB0B0B0)
        "statusBar.foreground" to 0xFFB0B0B0.toInt(),
        // Activity bar — legacy DarkActivityBar = Color(0xFF333333)
        "activityBar.background" to 0xFF333333.toInt(),
        // Activity bar icons — legacy DarkTextPrimary = Color(0xFFFFFFFF)
        "activityBar.foreground" to 0xFFFFFFFF.toInt(),
        // Drawer / side panel — legacy DarkSurface = Color(0xFF252526)
        "sideBar.background" to 0xFF252526.toInt(),
        // Dialog backgrounds — legacy DarkSurface = Color(0xFF252526)
        "editorWidget.background" to 0xFF252526.toInt(),
        // Widget/dialog border — legacy DarkBorder = Color(0xFF3C3C3C)
        "editorWidget.border" to 0xFF3C3C3C.toInt(),
        // Focus/accent ring — legacy DarkBrandPrimary = Color(0xFFEF9A9A)
        "focusBorder" to 0xFFEF9A9A.toInt(),
        // Filename badge tint — legacy DarkBrandPrimary = Color(0xFFEF9A9A)
        "badge.background" to 0xFFEF9A9A.toInt(),
        // Current line highlight — legacy DarkCurrentLine = Color(0xFF2A2D2E)
        "editor.lineHighlightBackground" to 0xFF2A2D2E.toInt(),
        // Inactive tab background — legacy DarkSurfaceVariant = Color(0xFF2D2D30)
        "tab.inactiveBackground" to 0xFF2D2D30.toInt(),
        // Active tab background (mirrors editor) — legacy DarkBackground = Color(0xFF1E1E1E)
        "tab.activeBackground" to 0xFF1E1E1E.toInt(),
        // Menu hover — legacy DarkMenuHover = Color(0xFF2A2D2E)
        "menu.selectionBackground" to 0xFF2A2D2E.toInt(),
    )
}
