/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive UI Tests for Yole Desktop App
 *
 *########################################################*/
package digital.vasic.yole.desktop.ui

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.StyleSheets
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.ui.ThemeMode
import digital.vasic.yole.ui.ThemeUtils
import digital.vasic.yole.ui.YoleTypography
import digital.vasic.yole.desktop.shortcut.DesktopKeyboardShortcuts
import androidx.compose.ui.input.key.Key
import org.junit.Test
import kotlin.test.*

/**
 * Comprehensive UI tests for Yole Desktop application.
 *
 * Tests cover:
 * - FormatRegistry integration
 * - Theme color accessibility
 * - App configuration and settings values
 * - Navigation state logic
 * - Keyboard shortcut mappings
 * - File manager / format detection logic
 * - StyleSheets caching
 * - Typography system
 */
class YoleDesktopUITest {

    // ==================== FormatRegistry Tests ====================

    @Test
    fun `FormatRegistry should have expected core formats`() {
        val formats = FormatRegistry.formats
        assertTrue(formats.isNotEmpty(), "FormatRegistry should contain formats")

        val formatIds = formats.map { it.id }
        assertTrue(FormatRegistry.ID_MARKDOWN in formatIds, "Should contain Markdown")
        assertTrue(FormatRegistry.ID_PLAINTEXT in formatIds, "Should contain Plain Text")
        assertTrue(FormatRegistry.ID_TODOTXT in formatIds, "Should contain Todo.txt")
        assertTrue(FormatRegistry.ID_CSV in formatIds, "Should contain CSV")
    }

    @Test
    fun `FormatRegistry should have expected wiki formats`() {
        val formatIds = FormatRegistry.formats.map { it.id }
        assertTrue(FormatRegistry.ID_WIKITEXT in formatIds, "Should contain WikiText")
        assertTrue(FormatRegistry.ID_ORGMODE in formatIds, "Should contain Org Mode")
        assertTrue(FormatRegistry.ID_CREOLE in formatIds, "Should contain Creole")
        assertTrue(FormatRegistry.ID_TIDDLYWIKI in formatIds, "Should contain TiddlyWiki")
    }

    @Test
    fun `FormatRegistry should have expected technical formats`() {
        val formatIds = FormatRegistry.formats.map { it.id }
        assertTrue(FormatRegistry.ID_LATEX in formatIds, "Should contain LaTeX")
        assertTrue(FormatRegistry.ID_ASCIIDOC in formatIds, "Should contain AsciiDoc")
        assertTrue(FormatRegistry.ID_RESTRUCTUREDTEXT in formatIds, "Should contain reStructuredText")
    }

    @Test
    fun `FormatRegistry should detect Markdown by extension`() {
        val format = FormatRegistry.detectByExtension("md")
        assertEquals(FormatRegistry.ID_MARKDOWN, format.id)
        assertEquals("Markdown", format.name)
    }

    @Test
    fun `FormatRegistry should detect format by filename`() {
        val mdFormat = FormatRegistry.detectByFilename("document.md")
        assertEquals(FormatRegistry.ID_MARKDOWN, mdFormat.id)

        val csvFormat = FormatRegistry.detectByFilename("data.csv")
        assertEquals(FormatRegistry.ID_CSV, csvFormat.id)

        val latexFormat = FormatRegistry.detectByFilename("paper.tex")
        assertEquals(FormatRegistry.ID_LATEX, latexFormat.id)
    }

    @Test
    fun `FormatRegistry should fall back to plaintext for unknown extensions`() {
        val format = FormatRegistry.detectByExtension("xyz")
        assertEquals(FormatRegistry.ID_PLAINTEXT, format.id)
    }

    @Test
    fun `FormatRegistry should detect Markdown by content`() {
        val content = "# Heading\n\nSome **bold** text."
        val detected = FormatRegistry.detectByContent(content)
        assertNotNull(detected, "Should detect format from Markdown content")
        assertEquals(FormatRegistry.ID_MARKDOWN, detected.id)
    }

    @Test
    fun `FormatRegistry should return null for empty content detection`() {
        val detected = FormatRegistry.detectByContent("")
        assertNull(detected, "Should return null for empty content")
    }

    @Test
    fun `FormatRegistry should report correct format count`() {
        val formats = FormatRegistry.formats
        assertTrue(formats.size >= 17, "Should have at least 17 formats, got ${formats.size}")
    }

    @Test
    fun `FormatRegistry getById should return correct format`() {
        val md = FormatRegistry.getById(FormatRegistry.ID_MARKDOWN)
        assertNotNull(md)
        assertEquals("Markdown", md.name)
        assertTrue(md.extensions.contains(".md"))

        val nonExistent = FormatRegistry.getById("nonexistent_format")
        assertNull(nonExistent)
    }

    @Test
    fun `FormatRegistry isSupported should work correctly`() {
        assertTrue(FormatRegistry.isSupported(FormatRegistry.ID_MARKDOWN))
        assertTrue(FormatRegistry.isSupported(FormatRegistry.ID_CSV))
        assertFalse(FormatRegistry.isSupported("nonexistent_format"))
    }

    // ==================== Theme Utility Tests ====================
    //
    // iter-57 Phase 3b: deleted alongside the YoleColors palette they referenced:
    //   - `YoleColors light theme colors should be accessible`
    //   - `YoleColors dark theme colors should be accessible`
    //   - `YoleColors surface colors should be accessible`
    //   - `YoleColors text colors should be accessible`
    // The ThemeMode/ThemeUtils API survives. The two tests below exercise the
    // surviving entry points with literal Compose Color values rather than the
    // legacy palette.

    @Test
    fun `ThemeUtils shouldUseDarkTheme should resolve correctly`() {
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.LIGHT, systemInDarkTheme = true))
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.LIGHT, systemInDarkTheme = false))

        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.DARK, systemInDarkTheme = true))
        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.DARK, systemInDarkTheme = false))

        assertTrue(ThemeUtils.shouldUseDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = true))
        assertFalse(ThemeUtils.shouldUseDarkTheme(ThemeMode.SYSTEM, systemInDarkTheme = false))
    }

    @Test
    fun `ThemeUtils getSemanticColor should select correct theme color`() {
        val lightColor = androidx.compose.ui.graphics.Color(0xFFD32F2F)
        val darkColor = androidx.compose.ui.graphics.Color(0xFFEF9A9A)

        val resultLight = ThemeUtils.getSemanticColor(isDarkTheme = false, lightColor, darkColor)
        assertEquals(lightColor, resultLight)

        val resultDark = ThemeUtils.getSemanticColor(isDarkTheme = true, lightColor, darkColor)
        assertEquals(darkColor, resultDark)
    }

    // ==================== Navigation State Tests ====================

    @Test
    fun `Screen enum should have all expected values`() {
        val screens = Screen.values()
        assertEquals(4, screens.size, "Should have exactly 4 screens")
        assertTrue(Screen.FILE_BROWSER in screens)
        assertTrue(Screen.EDITOR in screens)
        assertTrue(Screen.PREVIEW in screens)
        assertTrue(Screen.SETTINGS in screens)
    }

    @Test
    fun `Screen enum should preserve ordinal order`() {
        assertEquals(0, Screen.FILE_BROWSER.ordinal)
        assertEquals(1, Screen.EDITOR.ordinal)
        assertEquals(2, Screen.PREVIEW.ordinal)
        assertEquals(3, Screen.SETTINGS.ordinal)
    }

    @Test
    fun `Screen enum valueOf should work correctly`() {
        assertEquals(Screen.FILE_BROWSER, Screen.valueOf("FILE_BROWSER"))
        assertEquals(Screen.EDITOR, Screen.valueOf("EDITOR"))
        assertEquals(Screen.PREVIEW, Screen.valueOf("PREVIEW"))
        assertEquals(Screen.SETTINGS, Screen.valueOf("SETTINGS"))
    }

    @Test
    fun `Screen enum should reject invalid names`() {
        assertFailsWith<IllegalArgumentException> {
            Screen.valueOf("INVALID_SCREEN")
        }
    }

    // ==================== Keyboard Shortcut Tests ====================

    @Test
    fun `should have default file operation shortcuts`() {
        val shortcuts = DesktopKeyboardShortcuts()

        val newFile = shortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_NEW_FILE)
        assertNotNull(newFile)
        assertEquals(Key.N, newFile.key)
        assertTrue(newFile.ctrl)

        val openFile = shortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_OPEN_FILE)
        assertNotNull(openFile)
        assertEquals(Key.O, openFile.key)
        assertTrue(openFile.ctrl)

        val saveFile = shortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_SAVE_FILE)
        assertNotNull(saveFile)
        assertEquals(Key.S, saveFile.key)
        assertTrue(saveFile.ctrl)
    }

    @Test
    fun `should have default edit operation shortcuts`() {
        val shortcuts = DesktopKeyboardShortcuts()

        val undo = shortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_UNDO)
        assertNotNull(undo)
        assertEquals(Key.Z, undo.key)
        assertTrue(undo.ctrl)

        val copy = shortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_COPY)
        assertNotNull(copy)
        assertEquals(Key.C, copy.key)
        assertTrue(copy.ctrl)

        val paste = shortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_PASTE)
        assertNotNull(paste)
        assertEquals(Key.V, paste.key)
        assertTrue(paste.ctrl)
    }

    @Test
    fun `should have default view operation shortcuts`() {
        val shortcuts = DesktopKeyboardShortcuts()

        val zoomIn = shortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_ZOOM_IN)
        assertNotNull(zoomIn)

        val zoomOut = shortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_ZOOM_OUT)
        assertNotNull(zoomOut)

        val fullscreen = shortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_FULLSCREEN)
        assertNotNull(fullscreen)
    }

    @Test
    fun `should have Save As shortcut with Ctrl+Shift+S`() {
        val shortcuts = DesktopKeyboardShortcuts()
        val saveAs = shortcuts.getShortcut(DesktopKeyboardShortcuts.ACTION_SAVE_AS_FILE)
        assertNotNull(saveAs)
        assertEquals(Key.S, saveAs.key)
        assertTrue(saveAs.ctrl)
        assertTrue(saveAs.shift)
    }

    // ==================== App Configuration Tests ====================

    @Test
    fun `YoleDesktopSettings should have default theme mode`() {
        val settings = YoleDesktopSettings()
        val themeMode = settings.getThemeMode()
        assertTrue(
            themeMode in listOf("light", "dark", "system"),
            "Theme mode should be one of light/dark/system, was: $themeMode"
        )
    }

    @Test
    fun `YoleDesktopSettings should manage line numbers setting`() {
        val settings = YoleDesktopSettings()
        val original = settings.getShowLineNumbers()

        settings.setShowLineNumbers(!original)
        assertEquals(!original, settings.getShowLineNumbers())

        // Restore
        settings.setShowLineNumbers(original)
        assertEquals(original, settings.getShowLineNumbers())
    }

    @Test
    fun `YoleDesktopSettings should manage auto-save setting`() {
        val settings = YoleDesktopSettings()
        val original = settings.getAutoSave()

        settings.setAutoSave(!original)
        assertEquals(!original, settings.getAutoSave())

        // Restore
        settings.setAutoSave(original)
        assertEquals(original, settings.getAutoSave())
    }

    @Test
    fun `YoleDesktopSettings should manage animations setting`() {
        val settings = YoleDesktopSettings()
        val original = settings.getAnimationsEnabled()

        settings.setAnimationsEnabled(!original)
        assertEquals(!original, settings.getAnimationsEnabled())

        // Restore
        settings.setAnimationsEnabled(original)
        assertEquals(original, settings.getAnimationsEnabled())
    }

    @Test
    fun `YoleDesktopSettings should manage high contrast setting`() {
        val settings = YoleDesktopSettings()
        val original = settings.getHighContrastEnabled()

        settings.setHighContrastEnabled(!original)
        assertEquals(!original, settings.getHighContrastEnabled())

        // Restore
        settings.setHighContrastEnabled(original)
        assertEquals(original, settings.getHighContrastEnabled())
    }

    // ==================== StyleSheets Cache Tests ====================

    @Test
    fun `StyleSheets should return styles for known formats`() {
        val markdownStyle = StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertTrue(markdownStyle.isNotEmpty(), "Markdown stylesheet should not be empty")
        assertTrue(markdownStyle.contains("<style>"), "Should contain style tag")
    }

    @Test
    fun `StyleSheets should return empty string for unknown format`() {
        val unknownStyle = StyleSheets.getStyleSheet("unknown_format_xyz", lightMode = true)
        assertEquals("", unknownStyle)
    }

    @Test
    fun `StyleSheets cache should grow as formats are accessed`() {
        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)

        StyleSheets.getStyleSheet(TextFormat.ID_MARKDOWN, lightMode = true)
        assertTrue(StyleSheets.cacheSize > 0)

        StyleSheets.getStyleSheet(TextFormat.ID_LATEX, lightMode = true)
        StyleSheets.getStyleSheet(TextFormat.ID_LATEX, lightMode = false)
        assertTrue(StyleSheets.cacheSize >= 3)

        StyleSheets.clearCache()
        assertEquals(0, StyleSheets.cacheSize)
    }

    // ==================== Typography Tests ====================

    @Test
    fun `YoleTypography should provide all standard text styles`() {
        assertNotNull(YoleTypography.DisplayLarge)
        assertNotNull(YoleTypography.DisplayMedium)
        assertNotNull(YoleTypography.DisplaySmall)
        assertNotNull(YoleTypography.HeadlineLarge)
        assertNotNull(YoleTypography.HeadlineMedium)
        assertNotNull(YoleTypography.HeadlineSmall)
        assertNotNull(YoleTypography.TitleLarge)
        assertNotNull(YoleTypography.TitleMedium)
        assertNotNull(YoleTypography.TitleSmall)
        assertNotNull(YoleTypography.BodyLarge)
        assertNotNull(YoleTypography.BodyMedium)
        assertNotNull(YoleTypography.BodySmall)
        assertNotNull(YoleTypography.LabelLarge)
        assertNotNull(YoleTypography.LabelMedium)
        assertNotNull(YoleTypography.LabelSmall)
        assertNotNull(YoleTypography.Code)
    }

    // ==================== Format Extension Tests ====================

    @Test
    fun `FormatRegistry should support common file extensions`() {
        assertTrue(FormatRegistry.isExtensionSupported(".md"))
        assertTrue(FormatRegistry.isExtensionSupported(".txt"))
        assertTrue(FormatRegistry.isExtensionSupported(".csv"))
        assertTrue(FormatRegistry.isExtensionSupported(".tex"))
        assertTrue(FormatRegistry.isExtensionSupported(".org"))
        assertTrue(FormatRegistry.isExtensionSupported(".adoc"))
        assertTrue(FormatRegistry.isExtensionSupported(".rst"))
        assertTrue(FormatRegistry.isExtensionSupported(".wiki"))
    }

    @Test
    fun `FormatRegistry getFormatNames should return non-empty list`() {
        val names = FormatRegistry.getFormatNames()
        assertTrue(names.isNotEmpty())
        assertTrue("Markdown" in names)
        assertTrue("Plain Text" in names)
        assertTrue("CSV" in names)
    }

    @Test
    fun `FormatRegistry getAllExtensions should return distinct extensions`() {
        val extensions = FormatRegistry.getAllExtensions()
        assertTrue(extensions.isNotEmpty())
        assertEquals(extensions.distinct().size, extensions.size, "Extensions should be distinct")
        assertTrue(".md" in extensions)
        assertTrue(".txt" in extensions)
    }

    @Test
    fun `FormatRegistry getFormatsByExtension should return multiple for txt`() {
        val txtFormats = FormatRegistry.getFormatsByExtension("txt")
        assertTrue(txtFormats.size >= 2, "Should have at least 2 formats for .txt extension")
    }

    // ==================== ThemeMode Tests ====================

    @Test
    fun `ThemeMode enum should have all expected values`() {
        val modes = ThemeMode.values()
        assertEquals(3, modes.size)
        assertTrue(ThemeMode.LIGHT in modes)
        assertTrue(ThemeMode.DARK in modes)
        assertTrue(ThemeMode.SYSTEM in modes)
    }
}
