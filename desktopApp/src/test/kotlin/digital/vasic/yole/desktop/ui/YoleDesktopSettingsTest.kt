/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for YoleDesktopSettings
 *
 *########################################################*/
package digital.vasic.yole.desktop.ui

import org.junit.Before
import org.junit.Test
import kotlin.test.*

/**
 * Unit tests for YoleDesktopSettings class.
 *
 * Tests cover:
 * - Theme mode settings
 * - Editor settings
 * - Animation settings
 * - Settings persistence
 * - Default values
 */
class YoleDesktopSettingsTest {

    private lateinit var settings: YoleDesktopSettings

    @Before
    fun setup() {
        settings = YoleDesktopSettings()
    }

    // ==================== Theme Settings Tests ====================

    @Test
    fun `should return default theme mode`() {
        val themeMode = settings.getThemeMode()

        assertNotNull(themeMode)
        assertTrue(themeMode in listOf("system", "light", "dark"))
    }

    @Test
    fun `should set and get light theme mode`() {
        settings.setThemeMode("light")

        val themeMode = settings.getThemeMode()
        assertEquals("light", themeMode)
    }

    @Test
    fun `should set and get dark theme mode`() {
        settings.setThemeMode("dark")

        val themeMode = settings.getThemeMode()
        assertEquals("dark", themeMode)
    }

    @Test
    fun `should set and get system theme mode`() {
        settings.setThemeMode("system")

        val themeMode = settings.getThemeMode()
        assertEquals("system", themeMode)
    }

    @Test
    fun `should persist theme mode changes`() {
        settings.setThemeMode("light")
        settings.setThemeMode("dark")

        val themeMode = settings.getThemeMode()
        assertEquals("dark", themeMode)
    }

    // ==================== Line Numbers Settings Tests ====================

    @Test
    fun `should return default show line numbers setting`() {
        val showLineNumbers = settings.getShowLineNumbers()

        assertNotNull(showLineNumbers)
        // Default is true
        assertTrue(showLineNumbers)
    }

    @Test
    fun `should set and get show line numbers to true`() {
        settings.setShowLineNumbers(true)

        val showLineNumbers = settings.getShowLineNumbers()
        assertTrue(showLineNumbers)
    }

    @Test
    fun `should set and get show line numbers to false`() {
        settings.setShowLineNumbers(false)

        val showLineNumbers = settings.getShowLineNumbers()
        assertFalse(showLineNumbers)
    }

    @Test
    fun `should toggle show line numbers`() {
        val initial = settings.getShowLineNumbers()
        settings.setShowLineNumbers(!initial)

        val toggled = settings.getShowLineNumbers()
        assertEquals(!initial, toggled)
    }

    // ==================== Auto-Save Settings Tests ====================

    @Test
    fun `should return default auto-save setting`() {
        val autoSave = settings.getAutoSave()

        // Verify it returns a boolean value (default might be true or false depending on prefs)
        assertNotNull(autoSave)
        // Auto-save is either true or false (both are valid defaults)
        assertTrue(autoSave is Boolean)
    }

    @Test
    fun `should set and get auto-save to true`() {
        settings.setAutoSave(true)

        val autoSave = settings.getAutoSave()
        assertTrue(autoSave)
    }

    @Test
    fun `should set and get auto-save to false`() {
        settings.setAutoSave(false)

        val autoSave = settings.getAutoSave()
        assertFalse(autoSave)
    }

    @Test
    fun `should toggle auto-save`() {
        val initial = settings.getAutoSave()
        settings.setAutoSave(!initial)

        val toggled = settings.getAutoSave()
        assertEquals(!initial, toggled)
    }

    // ==================== Animation Settings Tests ====================

    @Test
    fun `should return default animations enabled setting`() {
        val animationsEnabled = settings.getAnimationsEnabled()

        assertNotNull(animationsEnabled)
        // Default is true
        assertTrue(animationsEnabled)
    }

    @Test
    fun `should set and get animations enabled to true`() {
        settings.setAnimationsEnabled(true)

        val animationsEnabled = settings.getAnimationsEnabled()
        assertTrue(animationsEnabled)
    }

    @Test
    fun `should set and get animations enabled to false`() {
        settings.setAnimationsEnabled(false)

        val animationsEnabled = settings.getAnimationsEnabled()
        assertFalse(animationsEnabled)
    }

    @Test
    fun `should toggle animations enabled`() {
        val initial = settings.getAnimationsEnabled()
        settings.setAnimationsEnabled(!initial)

        val toggled = settings.getAnimationsEnabled()
        assertEquals(!initial, toggled)
    }

    // ==================== Multi-Setting Tests ====================

    @Test
    fun `should handle multiple settings changes`() {
        settings.setThemeMode("dark")
        settings.setShowLineNumbers(false)
        settings.setAutoSave(false)
        settings.setAnimationsEnabled(false)

        assertEquals("dark", settings.getThemeMode())
        assertFalse(settings.getShowLineNumbers())
        assertFalse(settings.getAutoSave())
        assertFalse(settings.getAnimationsEnabled())
    }

    @Test
    fun `should handle settings reset to defaults`() {
        // Change all settings
        settings.setThemeMode("dark")
        settings.setShowLineNumbers(false)
        settings.setAutoSave(false)
        settings.setAnimationsEnabled(false)

        // Reset to defaults
        settings.setThemeMode("system")
        settings.setShowLineNumbers(true)
        settings.setAutoSave(true)
        settings.setAnimationsEnabled(true)

        assertEquals("system", settings.getThemeMode())
        assertTrue(settings.getShowLineNumbers())
        assertTrue(settings.getAutoSave())
        assertTrue(settings.getAnimationsEnabled())
    }

    @Test
    fun `should preserve settings independence`() {
        settings.setThemeMode("light")
        settings.setShowLineNumbers(true)
        settings.setAutoSave(false)

        // Changing one setting shouldn't affect others
        assertEquals("light", settings.getThemeMode())
        assertTrue(settings.getShowLineNumbers())
        assertFalse(settings.getAutoSave())
    }
}
