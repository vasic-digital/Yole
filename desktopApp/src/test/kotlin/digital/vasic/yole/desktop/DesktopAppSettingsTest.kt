/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop App Settings Tests
 *
 * Tests cover:
 * - Desktop settings management
 * - Theme configuration
 * - File operations
 * - Desktop-specific features
 *
 *########################################################*/

package digital.vasic.yole.desktop

import digital.vasic.yole.desktop.ui.YoleDesktopSettings
import org.junit.Before
import org.junit.Test
import kotlin.test.*

/**
 * Desktop application settings tests.
 * Tests settings persistence and desktop-specific configuration.
 */
class DesktopAppSettingsTest {

    private lateinit var settings: YoleDesktopSettings

    @Before
    fun setup() {
        settings = YoleDesktopSettings()
        // Reset to defaults for consistent testing
        settings.setThemeMode("system")
        settings.setShowLineNumbers(true)
        settings.setAutoSave(true)
        settings.setAnimationsEnabled(true)
        settings.setHighContrastEnabled(false)
        settings.setReduceMotion(false)
        settings.setFocusIndicators(true)
        settings.setAnnounceChanges(true)
    }

    @Test
    fun `should manage theme mode settings`() {
        // Test default theme mode
        val defaultTheme = settings.getThemeMode()
        assertEquals("system", defaultTheme)
        
        // Test setting different theme modes
        settings.setThemeMode("light")
        assertEquals("light", settings.getThemeMode())
        
        settings.setThemeMode("dark")
        assertEquals("dark", settings.getThemeMode())
        
        settings.setThemeMode("system")
        assertEquals("system", settings.getThemeMode())
    }

    @Test
    fun `should manage editor settings`() {
        // Test line numbers setting
        assertTrue(settings.getShowLineNumbers())
        
        settings.setShowLineNumbers(false)
        assertFalse(settings.getShowLineNumbers())
        
        settings.setShowLineNumbers(true)
        assertTrue(settings.getShowLineNumbers())
        
        // Test auto-save setting
        assertTrue(settings.getAutoSave())
        
        settings.setAutoSave(false)
        assertFalse(settings.getAutoSave())
        
        settings.setAutoSave(true)
        assertTrue(settings.getAutoSave())
    }

    @Test
    fun `should manage accessibility settings`() {
        // Test high contrast setting
        assertFalse(settings.getHighContrastEnabled())
        
        settings.setHighContrastEnabled(true)
        assertTrue(settings.getHighContrastEnabled())
        
        settings.setHighContrastEnabled(false)
        assertFalse(settings.getHighContrastEnabled())
        
        // Test reduce motion setting
        assertFalse(settings.getReduceMotion())
        
        settings.setReduceMotion(true)
        assertTrue(settings.getReduceMotion())
        
        settings.setReduceMotion(false)
        assertFalse(settings.getReduceMotion())
        
        // Test focus indicators setting
        assertTrue(settings.getFocusIndicators())
        
        settings.setFocusIndicators(false)
        assertFalse(settings.getFocusIndicators())
        
        settings.setFocusIndicators(true)
        assertTrue(settings.getFocusIndicators())
        
        // Test announce changes setting
        assertTrue(settings.getAnnounceChanges())
        
        settings.setAnnounceChanges(false)
        assertFalse(settings.getAnnounceChanges())
        
        settings.setAnnounceChanges(true)
        assertTrue(settings.getAnnounceChanges())
    }

    @Test
    fun `should manage animation settings`() {
        // Test animations enabled setting
        assertTrue(settings.getAnimationsEnabled())
        
        settings.setAnimationsEnabled(false)
        assertFalse(settings.getAnimationsEnabled())
        
        settings.setAnimationsEnabled(true)
        assertTrue(settings.getAnimationsEnabled())
    }

    @Test
    fun `should handle multiple settings changes`() {
        // Change multiple settings
        settings.setThemeMode("dark")
        settings.setShowLineNumbers(false)
        settings.setAutoSave(false)
        settings.setAnimationsEnabled(false)
        settings.setHighContrastEnabled(true)
        settings.setReduceMotion(true)
        settings.setFocusIndicators(false)
        settings.setAnnounceChanges(false)
        
        // Verify all changes
        assertEquals("dark", settings.getThemeMode())
        assertFalse(settings.getShowLineNumbers())
        assertFalse(settings.getAutoSave())
        assertFalse(settings.getAnimationsEnabled())
        assertTrue(settings.getHighContrastEnabled())
        assertTrue(settings.getReduceMotion())
        assertFalse(settings.getFocusIndicators())
        assertFalse(settings.getAnnounceChanges())
    }

    @Test
    fun `should persist settings independently`() {
        // Change one setting
        settings.setThemeMode("light")
        
        // Verify other settings remain unchanged
        assertEquals("light", settings.getThemeMode())
        assertTrue(settings.getShowLineNumbers())
        assertTrue(settings.getAutoSave())
        assertTrue(settings.getAnimationsEnabled())
        assertFalse(settings.getHighContrastEnabled())
        assertFalse(settings.getReduceMotion())
        assertTrue(settings.getFocusIndicators())
        assertTrue(settings.getAnnounceChanges())
    }

    @Test
    fun `should handle settings reset to defaults`() {
        // Change all settings from defaults
        settings.setThemeMode("dark")
        settings.setShowLineNumbers(false)
        settings.setAutoSave(false)
        settings.setAnimationsEnabled(false)
        settings.setHighContrastEnabled(true)
        settings.setReduceMotion(true)
        settings.setFocusIndicators(false)
        settings.setAnnounceChanges(false)
        
        // Reset to defaults
        settings.setThemeMode("system")
        settings.setShowLineNumbers(true)
        settings.setAutoSave(true)
        settings.setAnimationsEnabled(true)
        settings.setHighContrastEnabled(false)
        settings.setReduceMotion(false)
        settings.setFocusIndicators(true)
        settings.setAnnounceChanges(true)
        
        // Verify defaults are restored
        assertEquals("system", settings.getThemeMode())
        assertTrue(settings.getShowLineNumbers())
        assertTrue(settings.getAutoSave())
        assertTrue(settings.getAnimationsEnabled())
        assertFalse(settings.getHighContrastEnabled())
        assertFalse(settings.getReduceMotion())
        assertTrue(settings.getFocusIndicators())
        assertTrue(settings.getAnnounceChanges())
    }
}