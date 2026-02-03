/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop Settings Storage for Yole
 * Native settings storage with cross-platform support
 *
 *########################################################*/
package digital.vasic.yole.desktop.storage

import digital.vasic.yole.desktop.ui.YoleDesktopSettings
import java.io.File
import java.util.prefs.Preferences
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Enhanced settings storage for desktop application.
 * Provides comprehensive settings management with backup and restore functionality.
 * Thread-safe implementation using synchronized blocks for all preference operations.
 */
class DesktopSettingsStorage {
    // Lazy initialization to avoid blocking constructor
    private val prefs by lazy { Preferences.userNodeForPackage(DesktopSettingsStorage::class.java) }
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    // Lock object for thread-safe preference access
    private val settingsLock = Any()
    
    companion object {
        private const val SETTINGS_VERSION = "1.0"
        private const val SETTINGS_KEY = "app_settings"
        private const val BACKUP_KEY = "settings_backup"
        private const val EXPORT_FILE_NAME = "yole_settings.json"
    }
    
    /**
     * Complete application settings data class.
     */
    @Serializable
    data class AppSettings(
        val version: String = SETTINGS_VERSION,
        val appearance: AppearanceSettings = AppearanceSettings(),
        val editor: EditorSettings = EditorSettings(),
        val file: FileSettings = FileSettings(),
        val window: WindowSettings = WindowSettings(),
        val shortcuts: ShortcutSettings = ShortcutSettings(),
        val advanced: AdvancedSettings = AdvancedSettings()
    )
    
    @Serializable
    data class AppearanceSettings(
        val themeMode: String = "system",
        val accentColor: String? = null,
        val highContrastEnabled: Boolean = false,
        val reduceMotion: Boolean = false,
        val focusIndicators: Boolean = true,
        val announceChanges: Boolean = true,
        val fontSize: Int = 14,
        val fontFamily: String = "Monospace",
        val lineHeight: Float = 1.5f,
        val tabSize: Int = 4
    )
    
    @Serializable
    data class EditorSettings(
        val showLineNumbers: Boolean = true,
        val autoSave: Boolean = true,
        val autoSaveInterval: Int = 300, // seconds
        val wordWrap: Boolean = false,
        val highlightCurrentLine: Boolean = true,
        val showWhitespace: Boolean = false,
        val showIndentGuides: Boolean = true,
        val enableCodeFolding: Boolean = true,
        val enableAutoIndent: Boolean = true,
        val enableBracketMatching: Boolean = true,
        val enableSyntaxHighlighting: Boolean = true,
        val enableSpellCheck: Boolean = false,
        val enableAutoComplete: Boolean = true,
        val enableAutoPair: Boolean = true,
        val insertSpacesForTabs: Boolean = true,
        val trimTrailingWhitespace: Boolean = true,
        val ensureNewlineAtEnd: Boolean = true
    )
    
    @Serializable
    data class FileSettings(
        val defaultEncoding: String = "UTF-8",
        val detectEncoding: Boolean = true,
        val backupOnSave: Boolean = true,
        val maxRecentFiles: Int = 20,
        val showHiddenFiles: Boolean = false,
        val confirmOverwrite: Boolean = true,
        val autoDetectFormat: Boolean = true,
        val defaultFormat: String = "markdown",
        val recentFiles: List<String> = emptyList()
    )
    
    @Serializable
    data class WindowSettings(
        val rememberWindowPosition: Boolean = true,
        val rememberWindowSize: Boolean = true,
        val defaultWidth: Int = 1200,
        val defaultHeight: Int = 800,
        val defaultX: Int = 100,
        val defaultY: Int = 100,
        val minimizeToTray: Boolean = true,
        val startMinimized: Boolean = false,
        val confirmOnExit: Boolean = true,
        val showInTaskbar: Boolean = true,
        val alwaysOnTop: Boolean = false
    )
    
    @Serializable
    data class ShortcutSettings(
        val enableCustomShortcuts: Boolean = true,
        val shortcuts: Map<String, String> = emptyMap()
    )
    
    @Serializable
    data class AdvancedSettings(
        val enableLogging: Boolean = false,
        val logLevel: String = "INFO",
        val enableCrashReporting: Boolean = true,
        val checkForUpdates: Boolean = true,
        val updateChannel: String = "stable",
        val enableTelemetry: Boolean = false,
        val enableBetaFeatures: Boolean = false,
        val customPluginsPath: String? = null,
        val maxMemoryUsage: Long = 512 * 1024 * 1024, // 512MB
        val enableHardwareAcceleration: Boolean = true
    )
    
    /**
     * Saves the complete application settings.
     * Thread-safe: Uses synchronized block for preference access.
     */
    fun saveSettings(settings: AppSettings) {
        synchronized(settingsLock) {
            try {
                val settingsJson = json.encodeToString(settings)
                prefs.put(SETTINGS_KEY, settingsJson)

                // Also update legacy settings for compatibility
                updateLegacySettings(settings)
            } catch (e: Exception) {
                // Handle error saving settings
                println("Error saving settings: ${e.message}")
            }
        }
    }

    /**
     * Loads the complete application settings.
     * Thread-safe: Uses synchronized block for preference access.
     */
    fun loadSettings(): AppSettings {
        return synchronized(settingsLock) {
            try {
                val settingsJson = prefs.get(SETTINGS_KEY, null)
                if (settingsJson != null) {
                    json.decodeFromString<AppSettings>(settingsJson)
                } else {
                    // Create default settings
                    val defaultSettings = AppSettings()
                    saveSettingsInternal(defaultSettings)
                    defaultSettings
                }
            } catch (e: Exception) {
                // Return default settings if loading fails
                AppSettings()
            }
        }
    }

    /**
     * Internal save without locking - must be called from within synchronized block.
     */
    private fun saveSettingsInternal(settings: AppSettings) {
        val settingsJson = json.encodeToString(settings)
        prefs.put(SETTINGS_KEY, settingsJson)
        updateLegacySettings(settings)
    }

    /**
     * Resets settings to default values.
     * Thread-safe: Uses synchronized block for preference access.
     */
    fun resetSettings() {
        synchronized(settingsLock) {
            try {
                prefs.remove(SETTINGS_KEY)
                // Create and save default settings
                val defaultSettings = AppSettings()
                saveSettingsInternal(defaultSettings)
            } catch (e: Exception) {
                // Handle error resetting settings
            }
        }
    }
    
    /**
     * Exports settings to a file.
     */
    fun exportSettings(file: File): Boolean {
        return try {
            val settings = loadSettings()
            val settingsJson = json.encodeToString(settings)
            file.writeText(settingsJson)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Imports settings from a file.
     */
    fun importSettings(file: File): Boolean {
        return try {
            val settingsJson = file.readText()
            val settings = json.decodeFromString<AppSettings>(settingsJson)
            saveSettings(settings)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Creates a backup of current settings.
     * Thread-safe: Uses synchronized block for preference access.
     */
    fun backupSettings(): Boolean {
        return synchronized(settingsLock) {
            try {
                val settingsJson = prefs.get(SETTINGS_KEY, null)
                if (settingsJson != null) {
                    prefs.put(BACKUP_KEY, settingsJson)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Restores settings from backup.
     * Thread-safe: Uses synchronized block for preference access.
     */
    fun restoreSettings(): Boolean {
        return synchronized(settingsLock) {
            try {
                val backupJson = prefs.get(BACKUP_KEY, null)
                if (backupJson != null) {
                    val settings = json.decodeFromString<AppSettings>(backupJson)
                    saveSettingsInternal(settings)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Gets a specific setting value.
     */
    inline fun <reified T> getSetting(path: String, defaultValue: T): T {
        val settings = loadSettings()
        return when (path) {
            "appearance.themeMode" -> settings.appearance.themeMode as T
            "appearance.accentColor" -> settings.appearance.accentColor as T
            "appearance.highContrastEnabled" -> settings.appearance.highContrastEnabled as T
            "editor.showLineNumbers" -> settings.editor.showLineNumbers as T
            "editor.autoSave" -> settings.editor.autoSave as T
            "file.defaultEncoding" -> settings.file.defaultEncoding as T
            "window.rememberWindowPosition" -> settings.window.rememberWindowPosition as T
            "advanced.enableLogging" -> settings.advanced.enableLogging as T
            else -> defaultValue
        }
    }
    
    /**
     * Sets a specific setting value.
     */
    inline fun <reified T> setSetting(path: String, value: T): Boolean {
        return try {
            val settings = loadSettings()
            val newSettings = when (path) {
                "appearance.themeMode" -> settings.copy(appearance = settings.appearance.copy(themeMode = value as String))
                "appearance.accentColor" -> settings.copy(appearance = settings.appearance.copy(accentColor = value as String?))
                "appearance.highContrastEnabled" -> settings.copy(appearance = settings.appearance.copy(highContrastEnabled = value as Boolean))
                "editor.showLineNumbers" -> settings.copy(editor = settings.editor.copy(showLineNumbers = value as Boolean))
                "editor.autoSave" -> settings.copy(editor = settings.editor.copy(autoSave = value as Boolean))
                "file.defaultEncoding" -> settings.copy(file = settings.file.copy(defaultEncoding = value as String))
                "window.rememberWindowPosition" -> settings.copy(window = settings.window.copy(rememberWindowPosition = value as Boolean))
                "advanced.enableLogging" -> settings.copy(advanced = settings.advanced.copy(enableLogging = value as Boolean))
                else -> settings
            }
            saveSettings(newSettings)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Updates legacy settings for backward compatibility.
     */
    private fun updateLegacySettings(settings: AppSettings) {
        try {
            // Update YoleDesktopSettings with new values
            val legacySettings = YoleDesktopSettings()
            legacySettings.setThemeMode(settings.appearance.themeMode)
            settings.appearance.accentColor?.let { legacySettings.setAccentColor(it) }
            legacySettings.setHighContrastEnabled(settings.appearance.highContrastEnabled)
            legacySettings.setShowLineNumbers(settings.editor.showLineNumbers)
            legacySettings.setAutoSave(settings.editor.autoSave)
        } catch (e: Exception) {
            // Ignore errors updating legacy settings
        }
    }
    
    /**
     * Validates settings before saving.
     */
    private fun validateSettings(settings: AppSettings): Boolean {
        return try {
            // Basic validation
            settings.appearance.fontSize in 8..72 &&
            settings.editor.autoSaveInterval in 30..3600 &&
            settings.file.maxRecentFiles in 5..50 &&
            settings.window.defaultWidth in 400..3840 &&
            settings.window.defaultHeight in 300..2160
        } catch (e: Exception) {
            false
        }
    }
}