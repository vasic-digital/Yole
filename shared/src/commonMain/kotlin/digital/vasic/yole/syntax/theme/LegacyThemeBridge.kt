/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-57 Phase 2: one-shot migration helper. Records the legacy
 * IdeTheme/YoleColors palette values from:
 *   shared/src/commonMain/kotlin/digital/vasic/yole/ui/Theme.kt
 *   (YoleColors.Ide.* and YoleColors / YoleColors.Dark color tokens)
 * keyed by their VS Code JSON equivalents per spec §3.8.
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
 * One-shot migration bridge: legacy IdeTheme/YoleColors palette indexed
 * by VS Code colors.* key. Values are ARGB ints (0xFFRRGGBB) — the same
 * bit-layout returned by [Theme.uiColor].
 *
 * Source of truth for values: `shared/src/commonMain/kotlin/digital/vasic/yole/ui/Theme.kt`.
 * Every constant below was hand-verified against that file at the time of this commit.
 *
 * Mapping notes (ambiguous cases documented):
 * - `activityBar.background` uses YoleColors.Ide.DarkActivityBar / LightActivityBar
 *   (not SurfaceVariant) because the activity bar semantic maps to the sidebar icon strip
 *   in VS Code — the visual left-most panel, not the general surface.
 * - `statusBar.background` uses YoleColors.Ide.DarkMenuHover / LightMenuHover
 *   because "menu hover" in the legacy palette was used as the status bar tint
 *   (same value as currentLine, both 0xFF2A2D2E for dark).
 * - `badge.background` uses YoleColors.BrandPrimary (light) / YoleColors.Dark.BrandPrimary (dark)
 *   because the badge tint in Yole has always been brand-accent colored.
 * - `focusBorder` maps to BrandPrimary as the single accent/focus color.
 * - `editorLineNumber.activeForeground` reuses the same muted-text token as
 *   `editorLineNumber.foreground`; Yole legacy did not differentiate active vs inactive
 *   gutter numbers.
 */
@Suppress("MagicNumber")
object LegacyThemeBridge {

    /**
     * Legacy light-mode values mapped to VS Code colors.* keys.
     * Each value is an ARGB int (0xFFRRGGBB).
     *
     * Sources in Theme.kt:
     * - YoleColors.Ide.LightBackground      = Color(0xFFFFFFFF)
     * - YoleColors.TextPrimary              = Color(0xFF212121)
     * - YoleColors.Ide.LightMutedText       = Color(0xFF6E6E6E)
     * - YoleColors.Ide.LightMenuHover       = Color(0xFFE8E8E8)
     * - YoleColors.TextSecondary            = Color(0xFF757575)
     * - YoleColors.Ide.LightActivityBar     = Color(0xFFDDDDDD)
     * - YoleColors.Ide.LightSurface         = Color(0xFFF3F3F3)
     * - YoleColors.Ide.LightBorder          = Color(0xFFD4D4D4)
     * - YoleColors.BrandPrimary             = Color(0xFFD32F2F)
     * - YoleColors.Ide.LightCurrentLine     = Color(0xFFF0F0F0)
     */
    val legacyLight: Map<String, Int> = mapOf(
        // Editor area — YoleColors.Ide.LightBackground = Color(0xFFFFFFFF)
        "editor.background" to 0xFFFFFFFF.toInt(),
        // Default text — YoleColors.TextPrimary = Color(0xFF212121)
        "editor.foreground" to 0xFF212121.toInt(),
        // Gutter line numbers — YoleColors.Ide.LightMutedText = Color(0xFF6E6E6E)
        "editorLineNumber.foreground" to 0xFF6E6E6E.toInt(),
        // Active gutter line (legacy had same value as inactive) — Color(0xFF6E6E6E)
        "editorLineNumber.activeForeground" to 0xFF6E6E6E.toInt(),
        // Status bar — YoleColors.Ide.LightMenuHover = Color(0xFFE8E8E8)
        "statusBar.background" to 0xFFE8E8E8.toInt(),
        // Status bar text — YoleColors.TextSecondary = Color(0xFF757575)
        "statusBar.foreground" to 0xFF757575.toInt(),
        // Activity bar (sidebar icon strip) — YoleColors.Ide.LightActivityBar = Color(0xFFDDDDDD)
        "activityBar.background" to 0xFFDDDDDD.toInt(),
        // Activity bar icons — YoleColors.TextPrimary = Color(0xFF212121)
        "activityBar.foreground" to 0xFF212121.toInt(),
        // Drawer / side panel — YoleColors.Ide.LightSurface = Color(0xFFF3F3F3)
        "sideBar.background" to 0xFFF3F3F3.toInt(),
        // Dialog backgrounds — YoleColors.Ide.LightSurface = Color(0xFFF3F3F3)
        "editorWidget.background" to 0xFFF3F3F3.toInt(),
        // Widget/dialog border — YoleColors.Ide.LightBorder = Color(0xFFD4D4D4)
        "editorWidget.border" to 0xFFD4D4D4.toInt(),
        // Focus/accent ring — YoleColors.BrandPrimary = Color(0xFFD32F2F)
        "focusBorder" to 0xFFD32F2F.toInt(),
        // Filename badge tint — YoleColors.BrandPrimary = Color(0xFFD32F2F)
        "badge.background" to 0xFFD32F2F.toInt(),
        // Current line highlight — YoleColors.Ide.LightCurrentLine = Color(0xFFF0F0F0)
        "editor.lineHighlightBackground" to 0xFFF0F0F0.toInt(),
        // Inactive tab background — YoleColors.Ide.LightSurfaceVariant = Color(0xFFECECEC)
        "tab.inactiveBackground" to 0xFFECECEC.toInt(),
        // Active tab background (mirrors editor) — YoleColors.Ide.LightBackground = Color(0xFFFFFFFF)
        "tab.activeBackground" to 0xFFFFFFFF.toInt(),
        // Menu hover — YoleColors.Ide.LightMenuHover = Color(0xFFE8E8E8)
        "menu.selectionBackground" to 0xFFE8E8E8.toInt(),
    )

    /**
     * Legacy dark-mode values mapped to VS Code colors.* keys.
     * Each value is an ARGB int (0xFFRRGGBB).
     *
     * Sources in Theme.kt:
     * - YoleColors.Ide.DarkBackground       = Color(0xFF1E1E1E)
     * - YoleColors.Dark.TextPrimary         = Color(0xFFFFFFFF)
     * - YoleColors.Ide.DarkMutedText        = Color(0xFF5A5A5A)
     * - YoleColors.Ide.DarkMenuHover        = Color(0xFF2A2D2E)
     * - YoleColors.Dark.TextSecondary       = Color(0xFFB0B0B0)
     * - YoleColors.Ide.DarkActivityBar      = Color(0xFF333333)
     * - YoleColors.Ide.DarkSurface          = Color(0xFF252526)
     * - YoleColors.Ide.DarkBorder           = Color(0xFF3C3C3C)
     * - YoleColors.Dark.BrandPrimary        = Color(0xFFEF9A9A)
     * - YoleColors.Ide.DarkCurrentLine      = Color(0xFF2A2D2E)
     */
    val legacyDark: Map<String, Int> = mapOf(
        // Editor area — YoleColors.Ide.DarkBackground = Color(0xFF1E1E1E)
        "editor.background" to 0xFF1E1E1E.toInt(),
        // Default text — YoleColors.Dark.TextPrimary = Color(0xFFFFFFFF)
        "editor.foreground" to 0xFFFFFFFF.toInt(),
        // Gutter line numbers — YoleColors.Ide.DarkMutedText = Color(0xFF5A5A5A)
        "editorLineNumber.foreground" to 0xFF5A5A5A.toInt(),
        // Active gutter (legacy same as inactive) — Color(0xFF5A5A5A)
        "editorLineNumber.activeForeground" to 0xFF5A5A5A.toInt(),
        // Status bar — YoleColors.Ide.DarkMenuHover = Color(0xFF2A2D2E)
        "statusBar.background" to 0xFF2A2D2E.toInt(),
        // Status bar text — YoleColors.Dark.TextSecondary = Color(0xFFB0B0B0)
        "statusBar.foreground" to 0xFFB0B0B0.toInt(),
        // Activity bar — YoleColors.Ide.DarkActivityBar = Color(0xFF333333)
        "activityBar.background" to 0xFF333333.toInt(),
        // Activity bar icons — YoleColors.Dark.TextPrimary = Color(0xFFFFFFFF)
        "activityBar.foreground" to 0xFFFFFFFF.toInt(),
        // Drawer / side panel — YoleColors.Ide.DarkSurface = Color(0xFF252526)
        "sideBar.background" to 0xFF252526.toInt(),
        // Dialog backgrounds — YoleColors.Ide.DarkSurface = Color(0xFF252526)
        "editorWidget.background" to 0xFF252526.toInt(),
        // Widget/dialog border — YoleColors.Ide.DarkBorder = Color(0xFF3C3C3C)
        "editorWidget.border" to 0xFF3C3C3C.toInt(),
        // Focus/accent ring — YoleColors.Dark.BrandPrimary = Color(0xFFEF9A9A)
        "focusBorder" to 0xFFEF9A9A.toInt(),
        // Filename badge tint — YoleColors.Dark.BrandPrimary = Color(0xFFEF9A9A)
        "badge.background" to 0xFFEF9A9A.toInt(),
        // Current line highlight — YoleColors.Ide.DarkCurrentLine = Color(0xFF2A2D2E)
        "editor.lineHighlightBackground" to 0xFF2A2D2E.toInt(),
        // Inactive tab background — YoleColors.Ide.DarkSurfaceVariant = Color(0xFF2D2D30)
        "tab.inactiveBackground" to 0xFF2D2D30.toInt(),
        // Active tab background (mirrors editor) — YoleColors.Ide.DarkBackground = Color(0xFF1E1E1E)
        "tab.activeBackground" to 0xFF1E1E1E.toInt(),
        // Menu hover — YoleColors.Ide.DarkMenuHover = Color(0xFF2A2D2E)
        "menu.selectionBackground" to 0xFF2A2D2E.toInt(),
    )
}
