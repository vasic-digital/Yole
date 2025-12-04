/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Main Compose UI for Yole Desktop App
 *
 *########################################################*/

package digital.vasic.yole.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import digital.vasic.yole.format.FormatRegistry
import java.util.prefs.Preferences

/**
 * Settings manager for Yole desktop app
 */
class YoleDesktopSettings {
    private val prefs = Preferences.userNodeForPackage(YoleDesktopSettings::class.java)

    // Theme settings
    fun getThemeMode(): String = prefs.get("theme_mode", "system")
    fun setThemeMode(mode: String) = prefs.put("theme_mode", mode)

    // Editor settings
    fun getShowLineNumbers(): Boolean = prefs.getBoolean("show_line_numbers", true)
    fun setShowLineNumbers(show: Boolean) = prefs.putBoolean("show_line_numbers", show)

    fun getAutoSave(): Boolean = prefs.getBoolean("auto_save", true)
    fun setAutoSave(auto: Boolean) = prefs.putBoolean("auto_save", auto)

    // Animation settings
    fun getAnimationsEnabled(): Boolean = prefs.getBoolean("animations_enabled", true)
    fun setAnimationsEnabled(enabled: Boolean) = prefs.putBoolean("animations_enabled", enabled)
}

enum class Screen {
    FILE_BROWSER,
    EDITOR,
    PREVIEW,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoleApp() {
    val systemInDarkTheme = isSystemInDarkTheme()
    val colorScheme = if (systemInDarkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val settings = remember { YoleDesktopSettings() }

    var currentScreen by remember { mutableStateOf(Screen.FILE_BROWSER) }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf("") }

    // Load settings
    var themeMode by remember { mutableStateOf(settings.getThemeMode()) }
    var showLineNumbers by remember { mutableStateOf(settings.getShowLineNumbers()) }
    var autoSave by remember { mutableStateOf(settings.getAutoSave()) }
    var animationsEnabled by remember { mutableStateOf(settings.getAnimationsEnabled()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yole") },
                actions = {
                    // Navigation buttons with icons
                    TextButton(onClick = { currentScreen = Screen.FILE_BROWSER }) {
                        Text(if (currentScreen == Screen.FILE_BROWSER) "📁 Files" else "Files")
                    }
                    TextButton(onClick = { currentScreen = Screen.EDITOR }) {
                        Text(if (currentScreen == Screen.EDITOR) "✏️ Edit" else "Edit")
                    }
                    TextButton(onClick = { currentScreen = Screen.PREVIEW }) {
                        Text(if (currentScreen == Screen.PREVIEW) "👁️ Preview" else "Preview")
                    }
                    TextButton(onClick = { currentScreen = Screen.SETTINGS }) {
                        Text(if (currentScreen == Screen.SETTINGS) "⚙️ Settings" else "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (animationsEnabled) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        // Screen transitions - slide horizontally
                        if (targetState.ordinal > initialState.ordinal) {
                            slideInHorizontally(
                                animationSpec = tween(600),
                                initialOffsetX = { it }
                            ) + fadeIn(animationSpec = tween(600)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(600),
                                targetOffsetX = { -it / 2 }
                            ) + fadeOut(animationSpec = tween(600))
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(600),
                                initialOffsetX = { -it }
                            ) + fadeIn(animationSpec = tween(600)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(600),
                                targetOffsetX = { it / 2 }
                            ) + fadeOut(animationSpec = tween(600))
                        }
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        Screen.FILE_BROWSER -> FileBrowserScreen(
                            onFileSelected = { file, content ->
                                selectedFile = file
                                fileContent = content
                                currentScreen = Screen.EDITOR
                            }
                        )
                        Screen.EDITOR -> EditorScreen(
                            fileName = selectedFile ?: "Untitled",
                            content = fileContent,
                            onContentChanged = { fileContent = it }
                        )
                        Screen.PREVIEW -> PreviewScreen(
                            fileName = selectedFile ?: "Untitled",
                            content = fileContent
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            themeMode = themeMode,
                            onThemeModeChanged = {
                                themeMode = it
                                settings.setThemeMode(it)
                            },
                            showLineNumbers = showLineNumbers,
                            onShowLineNumbersChanged = {
                                showLineNumbers = it
                                settings.setShowLineNumbers(it)
                            },
                            autoSave = autoSave,
                            onAutoSaveChanged = {
                                autoSave = it
                                settings.setAutoSave(it)
                            },
                            animationsEnabled = animationsEnabled,
                            onAnimationsEnabledChanged = {
                                animationsEnabled = it
                                settings.setAnimationsEnabled(it)
                            }
                        )
                    }
                }
            } else {
                // No animations
                when (currentScreen) {
                    Screen.FILE_BROWSER -> FileBrowserScreen(
                        onFileSelected = { file, content ->
                            selectedFile = file
                            fileContent = content
                            currentScreen = Screen.EDITOR
                        }
                    )
                    Screen.EDITOR -> EditorScreen(
                        fileName = selectedFile ?: "Untitled",
                        content = fileContent,
                        onContentChanged = { fileContent = it }
                    )
                    Screen.PREVIEW -> PreviewScreen(
                        fileName = selectedFile ?: "Untitled",
                        content = fileContent
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        themeMode = themeMode,
                        onThemeModeChanged = {
                            themeMode = it
                            settings.setThemeMode(it)
                        },
                        showLineNumbers = showLineNumbers,
                        onShowLineNumbersChanged = {
                            showLineNumbers = it
                            settings.setShowLineNumbers(it)
                        },
                        autoSave = autoSave,
                        onAutoSaveChanged = {
                            autoSave = it
                            settings.setAutoSave(it)
                        },
                        animationsEnabled = animationsEnabled,
                        onAnimationsEnabledChanged = {
                            animationsEnabled = it
                            settings.setAnimationsEnabled(it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FileBrowserScreen(onFileSelected: (String, String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "File Browser",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Sample files for demonstration - will be replaced with actual file browsing
        val sampleFiles = listOf(
            "sample.md" to "# Sample Markdown\n\nThis is a sample document.",
            "todo.txt" to "x 2018-01-01 Complete task @work\n2018-01-02 New task +project @home",
            "notes.txt" to "Plain text notes\n\n- Item 1\n- Item 2"
        )

        sampleFiles.forEach { (fileName, content) ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = { onFileSelected(fileName, content) }
            ) {
                Text(
                    text = fileName,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Supported formats: ${FormatRegistry.formats.size}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Note: File system access will be implemented next",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun EditorScreen(fileName: String, content: String, onContentChanged: (String) -> Unit) {
    var text by remember { mutableStateOf(content) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Editing: $fileName",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onContentChanged(it)
            },
            modifier = Modifier.fillMaxSize(),
            placeholder = { Text("Start typing...") }
        )
    }
}

@Composable
fun PreviewScreen(fileName: String, content: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Preview: $fileName",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Use format parsers for preview
        val previewContent = remember(content, fileName) {
            try {
                val format = FormatRegistry.detectByFilename(fileName)
                // For now, just show the content with format info
                "Format: ${format.name}\n\n$content"
            } catch (e: Exception) {
                content // Fallback to raw content
            }
        }

        Text(
            text = previewContent,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SettingsScreen(
    themeMode: String,
    onThemeModeChanged: (String) -> Unit,
    showLineNumbers: Boolean,
    onShowLineNumbersChanged: (Boolean) -> Unit,
    autoSave: Boolean,
    onAutoSaveChanged: (Boolean) -> Unit,
    animationsEnabled: Boolean,
    onAnimationsEnabledChanged: (Boolean) -> Unit
) {

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Theme settings
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = themeMode == "system",
                onClick = { onThemeModeChanged("system") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("System theme (follows system setting)")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = themeMode == "light",
                onClick = { onThemeModeChanged("light") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Light theme")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = themeMode == "dark",
                onClick = { onThemeModeChanged("dark") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dark theme")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Editor settings
        Text(
            text = "Editor",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show line numbers")
            Switch(
                checked = showLineNumbers,
                onCheckedChange = onShowLineNumbersChanged
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto-save")
            Switch(
                checked = autoSave,
                onCheckedChange = onAutoSaveChanged
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Animation settings
        Text(
            text = "Animations",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable smooth transitions")
            Switch(
                checked = animationsEnabled,
                onCheckedChange = onAnimationsEnabledChanged
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Format settings
        Text(
            text = "Formats",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Supported formats: ${FormatRegistry.formats.size}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        FormatRegistry.formats.take(5).forEach { format ->
            Text(
                text = "• ${format.name} (${format.extensions.joinToString(", ")})",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        if (FormatRegistry.formats.size > 5) {
            Text(
                text = "... and ${FormatRegistry.formats.size - 5} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About
        Text(
            text = "About Yole",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Yole is a cross-platform text editor supporting 18+ markup formats including Markdown, todo.txt, CSV, and more.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Version: 2.15.1",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}