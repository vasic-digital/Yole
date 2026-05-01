/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS Platform Tests
 * Tests for iOS-specific functionality
 *
 *########################################################*/
package digital.vasic.yole.ios

import kotlin.test.*

/**
 * iOS Settings Tests
 * Tests the YoleIOSSettings class functionality
 */
class YoleIOSSettingsTests {
    
    @Test
    fun testSettingsClassExists() {
        val settings = YoleIOSSettings()
        assertNotNull(settings)
    }
    
    @Test
    fun testThemeModes() {
        val settings = YoleIOSSettings()
        
        // Test default theme mode
        val defaultTheme = settings.getThemeMode()
        assertTrue(defaultTheme in listOf("system", "light", "dark"))
    }
    
    @Test
    fun testLineNumbersSetting() {
        val settings = YoleIOSSettings()
        settings.setShowLineNumbers(true)
        assertTrue(settings.getShowLineNumbers())
        
        settings.setShowLineNumbers(false)
        assertFalse(settings.getShowLineNumbers())
    }
    
    @Test
    fun testAutoSaveSetting() {
        val settings = YoleIOSSettings()
        settings.setAutoSave(true)
        assertTrue(settings.getAutoSave())
        
        settings.setAutoSave(false)
        assertFalse(settings.getAutoSave())
    }
    
    @Test
    fun testAnimationsSetting() {
        val settings = YoleIOSSettings()
        settings.setAnimationsEnabled(true)
        assertTrue(settings.getAnimationsEnabled())
        
        settings.setAnimationsEnabled(false)
        assertFalse(settings.getAnimationsEnabled())
    }
}

/**
 * iOS App Screen Tests
 * Tests the AppScreen sealed class
 */
class AppScreenTests {
    
    @Test
    fun testAppScreenValues() {
        assertEquals(3, AppScreen::class.sealedSubclasses.size)
    }
    
    @Test
    fun testAppScreenInstances() {
        val documents = AppScreen.Documents
        val editor = AppScreen.Editor
        val settings = AppScreen.Settings
        
        assertNotNull(documents)
        assertNotNull(editor)
        assertNotNull(settings)
    }
}

/**
 * iOS Platform Integration Tests
 */
class IOSPlatformIntegrationTests {

    @Test
    fun testPlatformModuleAccessible() {
        // Verify iOS-specific implementations are reachable from the iosTest source set
        // by instantiating a platform-specific type. If the iOS platform module fails to
        // link, this test fails to compile (proving the import surface is intact).
        val settings = YoleIOSSettings()
        assertNotNull(settings, "YoleIOSSettings must be instantiable from iosTest source set")
    }
}
