/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Main Compose UI for Yole Desktop App
 * IDE-style layout with file explorer, tabs, status bar
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.vasic.yole.desktop.ui.theme.YoleDesktopTheme
import digital.vasic.yole.desktop.ui.theme.YoleDesktopThemeWithSettings
import digital.vasic.yole.format.FormatRegistry
import java.util.prefs.Preferences

// ============================================================================
// IDE Dark Theme Color Palette
// ============================================================================

/** Editor background color (VS Code inspired dark) */
val IdeDarkBackground = Color(0xFF1E1E1E)

/** Sidebar and panel surface color */
val IdeDarkSurface = Color(0xFF252526)

/** Tab bar and status bar surface variant */
val IdeDarkSurfaceVariant = Color(0xFF2D2D30)

/** Border color for panels and separators */
val IdeDarkBorder = Color(0xFF3C3C3C)

/** Accent color for active elements (VS Code blue) */
val IdeAccent = Color(0xFF007ACC)

/** Primary text color */
val IdeTextPrimary = Color(0xFFD4D4D4)

/** Secondary text color for line numbers, hints */
val IdeTextSecondary = Color(0xFF858585)

/** Muted text color for disabled elements */
val IdeTextMuted = Color(0xFF5A5A5A)

/** Current line highlight color */
val IdeHighlightLine = Color(0xFF2A2D2E)

/** Text selection background color */
val IdeSelection = Color(0xFF264F78)

/** IDE Light Theme Colors */
val IdeLightBackground = Color(0xFFFFFFFF)
val IdeLightSurface = Color(0xFFF3F3F3)
val IdeLightSurfaceVariant = Color(0xFFECECEC)
val IdeLightBorder = Color(0xFFD4D4D4)
val IdeLightTextPrimary = Color(0xFF1E1E1E)
val IdeLightTextSecondary = Color(0xFF6E6E6E)

// ============================================================================
// Settings Manager
// ============================================================================

/**
 * Settings manager for Yole desktop app
 */
class YoleDesktopSettings {
    private val prefs = Preferences.userNodeForPackage(YoleDesktopSettings::class.java)

    // Theme settings
    fun getThemeMode(): String = prefs.get("theme_mode", "dark")
    fun setThemeMode(mode: String) = prefs.put("theme_mode", mode)

    fun getAccentColor(): String? = prefs.get("accent_color", null)
    fun setAccentColor(colorHex: String?) = prefs.put("accent_color", colorHex)

    fun getHighContrastEnabled(): Boolean = prefs.getBoolean("high_contrast", false)
    fun setHighContrastEnabled(enabled: Boolean) = prefs.putBoolean("high_contrast", enabled)

    // Accessibility settings
    fun getReduceMotion(): Boolean = prefs.getBoolean("reduce_motion", false)
    fun setReduceMotion(reduce: Boolean) = prefs.putBoolean("reduce_motion", reduce)

    fun getFocusIndicators(): Boolean = prefs.getBoolean("focus_indicators", true)
    fun setFocusIndicators(show: Boolean) = prefs.putBoolean("focus_indicators", show)

    fun getAnnounceChanges(): Boolean = prefs.getBoolean("announce_changes", true)
    fun setAnnounceChanges(announce: Boolean) = prefs.putBoolean("announce_changes", announce)

    // Editor settings
    fun getShowLineNumbers(): Boolean = prefs.getBoolean("show_line_numbers", true)
    fun setShowLineNumbers(show: Boolean) = prefs.putBoolean("show_line_numbers", show)

    fun getAutoSave(): Boolean = prefs.getBoolean("auto_save", true)
    fun setAutoSave(auto: Boolean) = prefs.putBoolean("auto_save", auto)

    // Animation settings
    fun getAnimationsEnabled(): Boolean = prefs.getBoolean("animations_enabled", true)
    fun setAnimationsEnabled(enabled: Boolean) = prefs.putBoolean("animations_enabled", enabled)

    // Font size setting
    fun getFontSize(): Int = prefs.getInt("font_size", 14)
    fun setFontSize(size: Int) = prefs.putInt("font_size", size)
}

enum class Screen {
    FILE_BROWSER,
    EDITOR,
    PREVIEW,
    SETTINGS
}

// ============================================================================
// IDE Color Helpers
// ============================================================================

/**
 * Returns IDE-appropriate colors based on the current theme.
 */
@Composable
fun ideBackground(): Color = if (isIdeInDarkMode()) IdeDarkBackground else IdeLightBackground

@Composable
fun ideSurface(): Color = if (isIdeInDarkMode()) IdeDarkSurface else IdeLightSurface

@Composable
fun ideSurfaceVariant(): Color = if (isIdeInDarkMode()) IdeDarkSurfaceVariant else IdeLightSurfaceVariant

@Composable
fun ideBorder(): Color = if (isIdeInDarkMode()) IdeDarkBorder else IdeLightBorder

@Composable
fun ideTextPrimary(): Color = if (isIdeInDarkMode()) IdeTextPrimary else IdeLightTextPrimary

@Composable
fun ideTextSecondary(): Color = if (isIdeInDarkMode()) IdeTextSecondary else IdeLightTextSecondary

@Composable
private fun isIdeInDarkMode(): Boolean {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return isDark
}

/**
 * Approximate luminance calculation for a Color.
 */
private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}

// ============================================================================
// Root App Entry Point
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoleApp() {
    val settings = remember { YoleDesktopSettings() }

    YoleDesktopThemeWithSettings(settings) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("main_app"),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen()
        }
    }
}

// ============================================================================
// MainScreen - IDE-Style Layout
// ============================================================================

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

    // Tab state: list of open tabs
    val openTabs = remember { mutableStateListOf<TabInfo>() }
    var activeTabIndex by remember { mutableStateOf(-1) }

    // Sidebar visibility
    var showSidebar by remember { mutableStateOf(true) }

    // Cursor state
    var cursorLine by remember { mutableStateOf(1) }
    var cursorColumn by remember { mutableStateOf(1) }

    Scaffold(
        modifier = Modifier
            .testTag("main_screen")
            .semantics { contentDescription = "Main application window" }
            .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.S -> {
                        if (currentScreen == Screen.EDITOR) {
                            // Save logic placeholder
                        }
                        true
                    }
                    Key.N -> {
                        currentScreen = Screen.EDITOR
                        true
                    }
                    Key.O -> {
                        currentScreen = Screen.FILE_BROWSER
                        true
                    }
                    Key.Comma -> {
                        currentScreen = Screen.SETTINGS
                        true
                    }
                    Key.Escape -> {
                        true
                    }
                    else -> false
                }
            } else false
        },
        topBar = {
            // IDE-style top bar: compact with navigation
            Column {
                // Main toolbar with app title and navigation
                TopAppBar(
                    title = {
                        Text(
                            "Yole",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    },
                    actions = {
                        // Navigation buttons styled as IDE toolbar items
                        IdeNavButton(
                            label = "Files",
                            isActive = currentScreen == Screen.FILE_BROWSER,
                            contentDesc = "Open file browser",
                            onClick = { currentScreen = Screen.FILE_BROWSER }
                        )
                        IdeNavButton(
                            label = "Edit",
                            isActive = currentScreen == Screen.EDITOR,
                            contentDesc = "Open editor",
                            onClick = { currentScreen = Screen.EDITOR }
                        )
                        IdeNavButton(
                            label = "Preview",
                            isActive = currentScreen == Screen.PREVIEW,
                            contentDesc = "Open preview",
                            onClick = { currentScreen = Screen.PREVIEW }
                        )
                        IdeNavButton(
                            label = "Settings",
                            isActive = currentScreen == Screen.SETTINGS,
                            contentDesc = "Open settings",
                            onClick = { currentScreen = Screen.SETTINGS }
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ideSurfaceVariant(),
                        titleContentColor = ideTextPrimary()
                    )
                )
                // Thin accent line under the toolbar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(ideBorder())
                )
            }
        },
        bottomBar = {
            // IDE-style status bar
            IdeStatusBar(
                currentScreen = currentScreen,
                fileName = selectedFile,
                content = fileContent,
                cursorLine = cursorLine,
                cursorColumn = cursorColumn
            )
        },
        containerColor = ideBackground()
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (animationsEnabled) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            slideInHorizontally(
                                animationSpec = tween(300),
                                initialOffsetX = { it / 4 }
                            ) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { -it / 4 }
                            ) + fadeOut(animationSpec = tween(300))
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(300),
                                initialOffsetX = { -it / 4 }
                            ) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(
                                animationSpec = tween(300),
                                targetOffsetX = { it / 4 }
                            ) + fadeOut(animationSpec = tween(300))
                        }
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    RenderScreen(
                        screen = screen,
                        selectedFile = selectedFile,
                        fileContent = fileContent,
                        themeMode = themeMode,
                        showLineNumbers = showLineNumbers,
                        autoSave = autoSave,
                        animationsEnabled = animationsEnabled,
                        onFileSelected = { file, content ->
                            selectedFile = file
                            fileContent = content
                            currentScreen = Screen.EDITOR
                        },
                        onContentChanged = { fileContent = it },
                        onThemeModeChanged = { themeMode = it; settings.setThemeMode(it) },
                        onShowLineNumbersChanged = { showLineNumbers = it; settings.setShowLineNumbers(it) },
                        onAutoSaveChanged = { autoSave = it; settings.setAutoSave(it) },
                        onAnimationsEnabledChanged = { animationsEnabled = it; settings.setAnimationsEnabled(it) },
                        onCursorPositionChanged = { line, col -> cursorLine = line; cursorColumn = col }
                    )
                }
            } else {
                RenderScreen(
                    screen = currentScreen,
                    selectedFile = selectedFile,
                    fileContent = fileContent,
                    themeMode = themeMode,
                    showLineNumbers = showLineNumbers,
                    autoSave = autoSave,
                    animationsEnabled = animationsEnabled,
                    onFileSelected = { file, content ->
                        selectedFile = file
                        fileContent = content
                        currentScreen = Screen.EDITOR
                    },
                    onContentChanged = { fileContent = it },
                    onThemeModeChanged = { themeMode = it; settings.setThemeMode(it) },
                    onShowLineNumbersChanged = { showLineNumbers = it; settings.setShowLineNumbers(it) },
                    onAutoSaveChanged = { autoSave = it; settings.setAutoSave(it) },
                    onAnimationsEnabledChanged = { animationsEnabled = it; settings.setAnimationsEnabled(it) },
                    onCursorPositionChanged = { line, col -> cursorLine = line; cursorColumn = col }
                )
            }
        }
    }
}

// ============================================================================
// Screen Renderer
// ============================================================================

@Composable
private fun RenderScreen(
    screen: Screen,
    selectedFile: String?,
    fileContent: String,
    themeMode: String,
    showLineNumbers: Boolean,
    autoSave: Boolean,
    animationsEnabled: Boolean,
    onFileSelected: (String, String) -> Unit,
    onContentChanged: (String) -> Unit,
    onThemeModeChanged: (String) -> Unit,
    onShowLineNumbersChanged: (Boolean) -> Unit,
    onAutoSaveChanged: (Boolean) -> Unit,
    onAnimationsEnabledChanged: (Boolean) -> Unit,
    onCursorPositionChanged: (Int, Int) -> Unit
) {
    when (screen) {
        Screen.FILE_BROWSER -> FileBrowserScreen(
            onFileSelected = onFileSelected
        )
        Screen.EDITOR -> EditorScreen(
            fileName = selectedFile ?: "Untitled",
            content = fileContent,
            onContentChanged = onContentChanged,
            showLineNumbers = showLineNumbers,
            onCursorPositionChanged = onCursorPositionChanged
        )
        Screen.PREVIEW -> PreviewScreen(
            fileName = selectedFile ?: "Untitled",
            content = fileContent
        )
        Screen.SETTINGS -> SettingsScreen(
            themeMode = themeMode,
            onThemeModeChanged = onThemeModeChanged,
            showLineNumbers = showLineNumbers,
            onShowLineNumbersChanged = onShowLineNumbersChanged,
            autoSave = autoSave,
            onAutoSaveChanged = onAutoSaveChanged,
            animationsEnabled = animationsEnabled,
            onAnimationsEnabledChanged = onAnimationsEnabledChanged
        )
    }
}

// ============================================================================
// IDE Navigation Button
// ============================================================================

@Composable
private fun IdeNavButton(
    label: String,
    isActive: Boolean,
    contentDesc: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .semantics { contentDescription = contentDesc }
            .padding(horizontal = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isActive) label else label,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) IdeAccent else ideTextSecondary()
                )
            )
            if (isActive) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(2.dp)
                        .background(IdeAccent)
                )
            }
        }
    }
}

// ============================================================================
// Tab Info Data Class
// ============================================================================

data class TabInfo(
    val fileName: String,
    val content: String,
    val isModified: Boolean = false
)

// ============================================================================
// IDE Status Bar
// ============================================================================

@Composable
private fun IdeStatusBar(
    currentScreen: Screen,
    fileName: String?,
    content: String,
    cursorLine: Int,
    cursorColumn: Int
) {
    val bgColor = ideSurfaceVariant()
    val textColor = ideTextSecondary()
    val accentTextColor = ideTextPrimary()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(bgColor)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: cursor position
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ln $cursorLine, Col $cursorColumn",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = textColor
                )
            )

            // Format detection
            if (fileName != null && currentScreen == Screen.EDITOR) {
                val format = try {
                    FormatRegistry.detectByFilename(fileName)
                } catch (_: Exception) { null }
                format?.let {
                    Text(
                        text = it.name,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = accentTextColor
                        )
                    )
                }
            }
        }

        // Right: encoding, line ending, word count
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "UTF-8",
                style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = textColor)
            )
            Text(
                text = "LF",
                style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = textColor)
            )
            if (content.isNotEmpty()) {
                val wordCount = content.split(Regex("\\s+")).count { it.isNotEmpty() }
                Text(
                    text = "$wordCount words",
                    style = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = textColor)
                )
            }
        }
    }
}

// ============================================================================
// File Browser Screen (IDE-style)
// ============================================================================

@Composable
fun FileBrowserScreen(onFileSelected: (String, String) -> Unit) {
    val bgColor = ideBackground()
    val surfaceColor = ideSurface()
    val textColor = ideTextPrimary()
    val secondaryColor = ideTextSecondary()
    val borderColor = ideBorder()

    Row(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // Left sidebar: file tree
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(surfaceColor)
                .border(width = 1.dp, color = borderColor)
                .padding(12.dp)
        ) {
            // Section header
            Text(
                text = "EXPLORER",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = secondaryColor
                ),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Read actual files from user's Documents and home directory
            val userHome = System.getProperty("user.home") ?: "."
            val docsDir = java.io.File(userHome, "Documents")
            val browseDir = if (docsDir.exists() && docsDir.isDirectory) docsDir else java.io.File(userHome)
            var currentDir by remember { mutableStateOf(browseDir) }
            val fileManager = remember { digital.vasic.yole.desktop.file.DesktopFileManager() }

            val fileList = remember(currentDir) {
                val files = currentDir.listFiles()?.toList() ?: emptyList()
                files.sortedWith(compareBy<java.io.File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            }

            // Parent directory navigation
            if (currentDir != browseDir && currentDir.parentFile != null) {
                IdeFileItem(
                    fileName = "..",
                    onClick = { currentDir = currentDir.parentFile }
                )
            }

            // Current path indicator
            Text(
                text = currentDir.absolutePath,
                style = TextStyle(fontSize = 9.sp, color = secondaryColor),
                maxLines = 1,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // File list with scroll
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                fileList.forEach { file ->
                    if (file.isDirectory) {
                        IdeFileItem(
                            fileName = "\uD83D\uDCC1 ${file.name}",
                            onClick = { currentDir = file }
                        )
                    } else {
                        IdeFileItem(
                            fileName = file.name,
                            onClick = {
                                val content = fileManager.readFile(file) ?: file.readText()
                                onFileSelected(file.name, content)
                            }
                        )
                    }
                }
                if (fileList.isEmpty()) {
                    Text(
                        text = "Empty directory",
                        style = TextStyle(fontSize = 11.sp, color = secondaryColor),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Divider(
                color = borderColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Text(
                text = "Supported formats: ${FormatRegistry.formats.size}",
                style = TextStyle(fontSize = 11.sp, color = secondaryColor)
            )
        }

        // Right: main content area with welcome message
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "File Browser",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = textColor,
                    fontWeight = FontWeight.Light
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Select a file from the explorer to begin editing",
                style = TextStyle(fontSize = 14.sp, color = secondaryColor)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Or press Ctrl+N to create a new file",
                style = TextStyle(fontSize = 12.sp, color = IdeTextMuted)
            )
        }
    }
}

/**
 * Single file item in the IDE file explorer.
 */
@Composable
private fun IdeFileItem(
    fileName: String,
    onClick: () -> Unit
) {
    val extension = fileName.substringAfterLast('.', "")
    val iconPrefix = when (extension) {
        "md", "markdown" -> "M"
        "txt" -> "T"
        "csv" -> "C"
        "json" -> "J"
        "xml", "html" -> "X"
        else -> "F"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File type indicator
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    color = IdeAccent.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.extraSmall
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconPrefix,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = IdeAccent
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = fileName,
            style = TextStyle(
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = ideTextPrimary()
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================================
// Editor Screen (IDE-style with line numbers)
// ============================================================================

@Composable
fun EditorScreen(
    fileName: String,
    content: String,
    onContentChanged: (String) -> Unit,
    showLineNumbers: Boolean = true,
    onCursorPositionChanged: (Int, Int) -> Unit = { _, _ -> }
) {
    var text by remember { mutableStateOf(content) }

    // Sync with external content changes
    LaunchedEffect(content) {
        if (text != content) {
            text = content
        }
    }

    val bgColor = ideBackground()
    val gutterBg = ideSurface()
    val borderColor = ideBorder()
    val textColor = ideTextPrimary()
    val lineNumColor = ideTextSecondary()

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // Tab bar header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(ideSurfaceVariant())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active tab
            Box(
                modifier = Modifier
                    .background(bgColor)
                    .border(width = 1.dp, color = borderColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Editing: $fileName",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = textColor
                        )
                    )
                }
            }
        }

        // Border under tab bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(borderColor)
        )

        // Editor area with line numbers
        Row(modifier = Modifier.fillMaxSize()) {
            // Line number gutter
            if (showLineNumbers) {
                val lineCount = text.count { it == '\n' } + 1
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight()
                        .background(gutterBg)
                        .verticalScroll(scrollState)
                        .padding(end = 8.dp, top = 8.dp, start = 4.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 1..lineCount) {
                            Text(
                                text = i.toString(),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = lineNumColor,
                                    lineHeight = 20.sp
                                ),
                                modifier = Modifier.padding(vertical = 0.5.dp)
                            )
                        }
                    }
                }

                // Gutter separator
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(borderColor)
                )
            }

            // Editor text field
            val editorScrollState = rememberScrollState()
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    onContentChanged(it)
                    // Compute cursor position
                    val lines = it.split('\n')
                    val lineNum = lines.size
                    val colNum = if (lines.isNotEmpty()) lines.last().length + 1 else 1
                    onCursorPositionChanged(lineNum, colNum)
                },
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = textColor,
                    lineHeight = 20.sp
                ),
                cursorBrush = SolidColor(IdeAccent),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(bgColor)
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                    .verticalScroll(editorScrollState),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = "Start typing...",
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = IdeTextMuted
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

// ============================================================================
// Preview Screen (IDE-style split pane look)
// ============================================================================

@Composable
fun PreviewScreen(fileName: String, content: String) {
    val bgColor = ideBackground()
    val textColor = ideTextPrimary()
    val secondaryColor = ideTextSecondary()
    val borderColor = ideBorder()

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // Preview header tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(ideSurfaceVariant())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(bgColor)
                    .border(width = 1.dp, color = borderColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Preview: $fileName",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(borderColor)
        )

        // Preview content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Use format parsers for live preview rendering
            val previewContent = remember(content, fileName) {
                try {
                    val format = FormatRegistry.detectByFilename(fileName)
                    val parser = digital.vasic.yole.format.ParserRegistry.getParser(format)
                    if (parser != null) {
                        val document = parser.parse(content)
                        val html = document.toHtml(lightMode = true)
                        val stripped = html
                            .replace(Regex("<br\\s*/?>"), "\n")
                            .replace(Regex("</?p>"), "\n")
                            .replace(Regex("</?div>"), "\n")
                            .replace(Regex("<li>"), "\n- ")
                            .replace(Regex("<h[1-6][^>]*>"), "\n")
                            .replace(Regex("</h[1-6]>"), "\n")
                            .replace(Regex("<hr\\s*/?>"), "\n---\n")
                            .replace(Regex("<[^>]+>"), "")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&amp;", "&")
                            .replace("&quot;", "\"")
                            .replace("&#39;", "'")
                            .replace("&nbsp;", " ")
                            .replace(Regex("\n{3,}"), "\n\n")
                            .trim()
                        "Format: ${format.name}\n\n$stripped"
                    } else {
                        "Format: ${format.name}\n\n$content"
                    }
                } catch (e: Exception) {
                    content // Fallback to raw content
                }
            }

            Text(
                text = previewContent,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = textColor,
                    lineHeight = 22.sp
                )
            )
        }
    }
}

// ============================================================================
// Settings Screen (IDE-style panel)
// ============================================================================

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
    val bgColor = ideBackground()
    val surfaceColor = ideSurface()
    val textColor = ideTextPrimary()
    val secondaryColor = ideTextSecondary()
    val borderColor = ideBorder()

    Row(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // Settings sidebar accent line
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(IdeAccent)
        )

        // Settings content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(32.dp)
        ) {
            // Appearance section
            IdeSettingsSectionHeader("Appearance")

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = themeMode == "system",
                    onClick = { onThemeModeChanged("system") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "System theme (follows system setting)",
                    style = TextStyle(fontSize = 13.sp, color = textColor)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = themeMode == "light",
                    onClick = { onThemeModeChanged("light") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Light theme",
                    style = TextStyle(fontSize = 13.sp, color = textColor)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = themeMode == "dark",
                    onClick = { onThemeModeChanged("dark") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Dark theme",
                    style = TextStyle(fontSize = 13.sp, color = textColor)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Editor section
            IdeSettingsSectionHeader("Editor")

            Spacer(modifier = Modifier.height(12.dp))

            IdeSettingsToggle(
                label = "Show line numbers",
                checked = showLineNumbers,
                onCheckedChange = onShowLineNumbersChanged
            )

            IdeSettingsToggle(
                label = "Auto-save",
                checked = autoSave,
                onCheckedChange = onAutoSaveChanged
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Animations section
            IdeSettingsSectionHeader("Animations")

            Spacer(modifier = Modifier.height(12.dp))

            IdeSettingsToggle(
                label = "Enable smooth transitions",
                checked = animationsEnabled,
                onCheckedChange = onAnimationsEnabledChanged
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Formats section
            IdeSettingsSectionHeader("Formats")

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Supported formats: ${FormatRegistry.formats.size}",
                style = TextStyle(fontSize = 13.sp, color = textColor)
            )

            Spacer(modifier = Modifier.height(8.dp))

            FormatRegistry.formats.take(5).forEach { format ->
                Text(
                    text = "  ${format.name} (${format.extensions.joinToString(", ")})",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = secondaryColor
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            if (FormatRegistry.formats.size > 5) {
                Text(
                    text = "  ... and ${FormatRegistry.formats.size - 5} more",
                    style = TextStyle(fontSize = 12.sp, color = IdeTextMuted)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // About section
            IdeSettingsSectionHeader("About Yole")

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Yole is a cross-platform text editor supporting 18+ markup formats including Markdown, todo.txt, CSV, and more.",
                style = TextStyle(fontSize = 13.sp, color = textColor, lineHeight = 20.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Version: 2.19.3.5",
                style = TextStyle(fontSize = 12.sp, color = IdeTextMuted)
            )
        }
    }
}

// ============================================================================
// IDE Settings Helpers
// ============================================================================

@Composable
private fun IdeSettingsSectionHeader(title: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = ideTextPrimary()
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(2.dp)
                .background(IdeAccent)
        )
    }
}

@Composable
private fun IdeSettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = TextStyle(fontSize = 13.sp, color = ideTextPrimary())
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = IdeAccent,
                uncheckedThumbColor = ideTextSecondary(),
                uncheckedTrackColor = ideBorder()
            )
        )
    }
}
