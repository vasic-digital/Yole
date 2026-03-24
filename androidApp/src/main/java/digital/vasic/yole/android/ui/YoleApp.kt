/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Main Compose UI for Yole Android App
 * IDE-style mobile layout with tabs, line numbers, status bar
 *
 *########################################################*/

package digital.vasic.yole.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import digital.vasic.yole.android.ui.theme.YoleAndroidTheme
import digital.vasic.yole.android.ui.theme.YoleAndroidThemeWithSettings
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.core.content.FileProvider
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import digital.vasic.opoc.model.GsSharedPreferencesPropertyBackend
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.ParseOptions
import digital.vasic.yole.ui.pressScale
import digital.vasic.yole.ui.hoverScale
import digital.vasic.yole.ui.ScreenTransitions
import digital.vasic.yole.ui.ListAnimations
import digital.vasic.yole.ui.LoadingStateWrapper
import digital.vasic.yole.ui.LoadingAnimations
import digital.vasic.yole.android.util.PdfExportUtil
import digital.vasic.yole.android.util.BackupRestoreUtil
import java.io.File

// ===== IDE Theme Colors =====
object IdeTheme {
    // Dark
    val darkBackground = Color(0xFF1E1E1E)
    val darkSurface = Color(0xFF252526)
    val darkSurfaceVariant = Color(0xFF2D2D30)
    val darkBorder = Color(0xFF3C3C3C)
    val darkAccent = Color(0xFF007ACC)
    val darkText = Color(0xFFD4D4D4)
    val darkTextSecondary = Color(0xFF858585)
    val darkCurrentLine = Color(0xFF2A2D2E)
    val darkStatusBar = Color(0xFF007ACC)
    val darkTabActive = Color(0xFF1E1E1E)
    val darkTabInactive = Color(0xFF2D2D30)
    val darkLineNumbers = Color(0xFF858585)

    // Light
    val lightBackground = Color(0xFFFFFFFF)
    val lightSurface = Color(0xFFF3F3F3)
    val lightSurfaceVariant = Color(0xFFECECEC)
    val lightBorder = Color(0xFFD4D4D4)
    val lightAccent = Color(0xFF007ACC)
    val lightText = Color(0xFF1E1E1E)
    val lightTextSecondary = Color(0xFF6E6E6E)
    val lightCurrentLine = Color(0xFFF0F0F0)
    val lightStatusBar = Color(0xFF007ACC)
    val lightTabActive = Color(0xFFFFFFFF)
    val lightTabInactive = Color(0xFFECECEC)
    val lightLineNumbers = Color(0xFF6E6E6E)
}

/**
 * Settings manager for Yole app
 */
class YoleSettings(context: android.content.Context) : GsSharedPreferencesPropertyBackend(context, "yole_settings") {

    // Theme settings
    fun getThemeMode(): String = getString("theme_mode", "dark") // IDE dark theme default
    fun setThemeMode(mode: String) = setString("theme_mode", mode)

    fun getDynamicColorsEnabled(): Boolean = getBool("dynamic_colors_enabled", true)
    fun setDynamicColorsEnabled(enabled: Boolean) = setBool("dynamic_colors_enabled", enabled)

    fun getCustomSeedColor(): String? = getString("custom_seed_color", "").takeIf { it.isNotEmpty() }
    fun setCustomSeedColor(colorHex: String?) = setString("custom_seed_color", colorHex ?: "")

    // Accessibility settings
    fun getReduceMotion(): Boolean = getBool("reduce_motion", false)
    fun setReduceMotion(reduce: Boolean) = setBool("reduce_motion", reduce)

    fun getHighContrast(): Boolean = getBool("high_contrast", false)
    fun setHighContrast(highContrast: Boolean) = setBool("high_contrast", highContrast)

    fun getAnnounceChanges(): Boolean = getBool("announce_changes", true)
    fun setAnnounceChanges(announce: Boolean) = setBool("announce_changes", announce)

    // Editor settings
    fun getShowLineNumbers(): Boolean = getBool("show_line_numbers", true)
    fun setShowLineNumbers(show: Boolean) = setBool("show_line_numbers", show)

    fun getAutoSave(): Boolean = getBool("auto_save", true)
    fun setAutoSave(auto: Boolean) = setBool("auto_save", auto)

    // Animation settings
    fun getAnimationsEnabled(): Boolean = getBool("animations_enabled", true)
    fun setAnimationsEnabled(enabled: Boolean) = setBool("animations_enabled", enabled)
}

/**
 * Save content to a file
 */
fun saveFile(filePath: String, content: String): Boolean {
    return try {
        val file = File(filePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Load content from a file
 */
fun loadFile(filePath: String): String? {
    return try {
        val file = File(filePath)
        if (file.exists()) file.readText() else null
    } catch (e: Exception) {
        null
    }
}

/**
 * Delete a file
 */
fun deleteFile(filePath: String): Boolean {
    return try {
        val file = File(filePath)
        file.delete()
    } catch (e: Exception) {
        false
    }
}

// Represents an open editor tab
data class EditorTab(
    val fileName: String,
    val content: String,
    val isDirty: Boolean = false
)

enum class Screen {
    FILES,
    TODO,
    QUICKNOTE,
    MORE
}

enum class SubScreen {
    FILE_BROWSER,
    EDITOR,
    PREVIEW,
    SETTINGS
}

@Composable
fun YoleApp() {
    val context = LocalContext.current
    val settings = remember { YoleSettings(context) }

    YoleAndroidThemeWithSettings(settings) {
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
    val context = LocalContext.current
    val settings = remember { YoleSettings(context) }
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme() || settings.getThemeMode() == "dark"

    var currentScreen by remember { mutableStateOf(Screen.FILES) }
    var currentSubScreen by remember { mutableStateOf<SubScreen?>(null) }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf("") }
    var quickNoteContent by remember { mutableStateOf("") }
    var showFileSearch by remember { mutableStateOf(false) }
    var fileSearchQuery by remember { mutableStateOf("") }
    var fileSortBy by remember { mutableStateOf("name") }

    // Tab management
    var openTabs by remember { mutableStateOf(listOf<EditorTab>()) }
    var activeTabIndex by remember { mutableStateOf(0) }

    // Complete screen state management for TODO screen
    var showTodoSearch by remember { mutableStateOf(false) }
    var todoSearchQuery by remember { mutableStateOf("") }
    var showTodoFilter by remember { mutableStateOf(false) }
    var todoFilterType by remember { mutableStateOf("all") }
    var todoItems by remember { mutableStateOf(listOf<TodoItem>()) }

    // Editor state
    var editorHistory by remember { mutableStateOf(listOf<String>()) }
    var editorHistoryIndex by remember { mutableStateOf(-1) }

    // Dialog states
    var showAboutDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportInProgress by remember { mutableStateOf(false) }
    var showNewDocDialog by remember { mutableStateOf(false) }
    var newDocFormat by remember { mutableStateOf("markdown") }

    // Drawer state
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Load settings
    var themeMode by remember { mutableStateOf(settings.getThemeMode()) }
    var showLineNumbers by remember { mutableStateOf(settings.getShowLineNumbers()) }
    var autoSave by remember { mutableStateOf(settings.getAutoSave()) }
    var animationsEnabled by remember { mutableStateOf(settings.getAnimationsEnabled()) }

    // IDE theme colors
    val bg = if (isDarkTheme) IdeTheme.darkBackground else IdeTheme.lightBackground
    val surface = if (isDarkTheme) IdeTheme.darkSurface else IdeTheme.lightSurface
    val border = if (isDarkTheme) IdeTheme.darkBorder else IdeTheme.lightBorder
    val accent = if (isDarkTheme) IdeTheme.darkAccent else IdeTheme.lightAccent
    val textColor = if (isDarkTheme) IdeTheme.darkText else IdeTheme.lightText
    val textSecondary = if (isDarkTheme) IdeTheme.darkTextSecondary else IdeTheme.lightTextSecondary
    val statusBarBg = if (isDarkTheme) IdeTheme.darkStatusBar else IdeTheme.lightStatusBar
    val tabActive = if (isDarkTheme) IdeTheme.darkTabActive else IdeTheme.lightTabActive
    val tabInactive = if (isDarkTheme) IdeTheme.darkTabInactive else IdeTheme.lightTabInactive
    val lineNumColor = if (isDarkTheme) IdeTheme.darkLineNumbers else IdeTheme.lightLineNumbers

    // Helper: open file in a new tab or switch to existing tab
    fun openFileInTab(fileName: String, content: String) {
        val existingIndex = openTabs.indexOfFirst { it.fileName == fileName }
        if (existingIndex >= 0) {
            activeTabIndex = existingIndex
            fileContent = openTabs[existingIndex].content
        } else {
            openTabs = openTabs + EditorTab(fileName = fileName, content = content)
            activeTabIndex = openTabs.size // will be the new last index
            fileContent = content
        }
        selectedFile = fileName
        currentSubScreen = SubScreen.EDITOR
    }

    // File pickers
    val backupFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { backupUri ->
            coroutineScope.launch {
                val result = BackupRestoreUtil.restoreBackup(context, backupUri, settings)
                if (result.isSuccess) {
                    Toast.makeText(context, "Backup restored successfully", Toast.LENGTH_SHORT).show()
                    when (currentScreen) {
                        Screen.FILES -> { fileSearchQuery = fileSearchQuery }
                        Screen.TODO -> { todoItems = todoItems }
                        else -> {}
                    }
                } else {
                    Toast.makeText(context, "Failed to restore backup: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Initialize secure storage and parsers with lazy loading for faster startup
    LaunchedEffect(Unit) {
        try {
            digital.vasic.yole.network.platform.SecureStorageFactory.initialize(context)
        } catch (e: Exception) {
            android.util.Log.e("YoleApp", "Failed to initialize secure storage", e)
        }
        try {
            digital.vasic.yole.format.ParserInitializer.registerAllParsersLazy()
        } catch (e: Exception) {
            android.util.Log.e("YoleApp", "Failed to initialize parsers", e)
        }
        try {
            PdfExportUtil.cleanupOldPdfs(context)
            BackupRestoreUtil.cleanupOldBackups(context)
        } catch (e: Exception) {
            android.util.Log.e("YoleApp", "Failed to cleanup old files", e)
        }
    }

    // Keyboard shortcuts
    LaunchedEffect(Unit) {
        // Add global keyboard shortcuts here
    }

    // Navigation drawer with file explorer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = surface
            ) {
                IdeDrawerContent(
                    isDarkTheme = isDarkTheme,
                    surface = surface,
                    border = border,
                    textColor = textColor,
                    textSecondary = textSecondary,
                    accent = accent,
                    openTabs = openTabs,
                    activeTabIndex = activeTabIndex,
                    onNewDocument = { showNewDocDialog = true },
                    onTabSelected = { index ->
                        activeTabIndex = index
                        selectedFile = openTabs[index].fileName
                        fileContent = openTabs[index].content
                        currentSubScreen = SubScreen.EDITOR
                        coroutineScope.launch { drawerState.close() }
                    },
                    onTabDeleted = { index ->
                        val tab = openTabs[index]
                        val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                        val filePath = File(docsDir, tab.fileName).absolutePath
                        deleteFile(filePath)
                        openTabs = openTabs.toMutableList().also { it.removeAt(index) }
                        if (activeTabIndex >= openTabs.size) {
                            activeTabIndex = (openTabs.size - 1).coerceAtLeast(0)
                        }
                        Toast.makeText(context, "Deleted: ${tab.fileName}", Toast.LENGTH_SHORT).show()
                    },
                    onOpenFileBrowser = {
                        currentSubScreen = SubScreen.FILE_BROWSER
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSettingsClick = {
                        currentSubScreen = SubScreen.SETTINGS
                        coroutineScope.launch { drawerState.close() }
                    },
                    onBackupClick = {
                        showBackupDialog = true
                        coroutineScope.launch { drawerState.close() }
                    },
                    onAboutClick = {
                        showAboutDialog = true
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.S -> {
                            if (currentSubScreen == SubScreen.EDITOR) {
                                selectedFile?.let { file ->
                                    val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                                    if (!docsDir.exists()) docsDir.mkdirs()
                                    val filePath = File(docsDir, file).absolutePath
                                    try {
                                        File(filePath).writeText(fileContent)
                                        if (settings.getAnnounceChanges()) {
                                            Toast.makeText(context, "File saved: $file", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            true
                        }
                        Key.N -> {
                            selectedFile = null
                            fileContent = ""
                            currentSubScreen = SubScreen.EDITOR
                            true
                        }
                        Key.O -> {
                            currentSubScreen = SubScreen.FILE_BROWSER
                            true
                        }
                        Key.Escape -> {
                            currentSubScreen = null
                            true
                        }
                        else -> false
                    }
                } else false
            },
            topBar = {
                Column {
                    // IDE App Bar
                    when (currentSubScreen) {
                        SubScreen.EDITOR -> IdeEditorTopBar(
                            fileName = selectedFile ?: "Untitled",
                            isDarkTheme = isDarkTheme,
                            onMenuClick = { coroutineScope.launch { drawerState.open() } },
                            onSaveClick = {
                                selectedFile?.let { fileName ->
                                    val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                                    if (!docsDir.exists()) docsDir.mkdirs()
                                    val filePath = File(docsDir, fileName).absolutePath
                                    if (saveFile(filePath, fileContent)) {
                                        if (settings.getAnnounceChanges()) {
                                            Toast.makeText(context, "File saved successfully", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to save file", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onPreviewClick = { currentSubScreen = SubScreen.PREVIEW },
                            onBackClick = { currentSubScreen = null }
                        )
                        SubScreen.PREVIEW -> PreviewTopBar(
                            fileName = selectedFile ?: "Untitled",
                            onEditClick = { currentSubScreen = SubScreen.EDITOR },
                            onBackClick = { currentSubScreen = null },
                            onExportClick = { showExportDialog = true }
                        )
                        SubScreen.SETTINGS -> SettingsTopBar(
                            onBackClick = { currentSubScreen = null }
                        )
                        null -> {
                            IdeMainTopBar(
                                currentScreen = currentScreen,
                                isDarkTheme = isDarkTheme,
                                onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                onSearchClick = {
                                    when (currentScreen) {
                                        Screen.FILES -> showFileSearch = !showFileSearch
                                        Screen.TODO -> showTodoSearch = !showTodoSearch
                                        else -> {}
                                    }
                                },
                                onMoreClick = { currentSubScreen = SubScreen.SETTINGS }
                            )
                        }
                        else -> {}
                    }

                    // Tab Bar (when in editor or files mode with open tabs)
                    if (openTabs.isNotEmpty() && (currentSubScreen == SubScreen.EDITOR || currentSubScreen == null)) {
                        IdeTabBar(
                            tabs = openTabs,
                            activeIndex = activeTabIndex,
                            isDarkTheme = isDarkTheme,
                            tabActive = tabActive,
                            tabInactive = tabInactive,
                            border = border,
                            textColor = textColor,
                            textSecondary = textSecondary,
                            onTabSelected = { index ->
                                activeTabIndex = index
                                selectedFile = openTabs[index].fileName
                                fileContent = openTabs[index].content
                                currentSubScreen = SubScreen.EDITOR
                            },
                            onTabClosed = { index ->
                                openTabs = openTabs.toMutableList().also { it.removeAt(index) }
                                if (activeTabIndex >= openTabs.size) {
                                    activeTabIndex = (openTabs.size - 1).coerceAtLeast(0)
                                }
                                if (openTabs.isEmpty()) {
                                    currentSubScreen = null
                                    selectedFile = null
                                    fileContent = ""
                                }
                            },
                            onNewTab = { showNewDocDialog = true }
                        )
                    }
                }
            },
            bottomBar = {
                Column {
                    // Status Bar
                    if (currentSubScreen == SubScreen.EDITOR && selectedFile != null) {
                        IdeStatusBar(
                            content = fileContent,
                            fileName = selectedFile ?: "",
                            isDarkTheme = isDarkTheme,
                            statusBarBg = statusBarBg
                        )
                    }

                    // Bottom Navigation
                    IdeBottomNavBar(
                        currentScreen = currentScreen,
                        isDarkTheme = isDarkTheme,
                        onScreenSelected = { screen ->
                            currentScreen = screen
                            // CRITICAL: Reset sub-screen overlay when switching main tabs
                            // This prevents Settings/Editor/Preview from staying on top
                            currentSubScreen = null
                        }
                    )
                }
            },
            floatingActionButton = {
                if (currentSubScreen == null) {
                    FloatingActionButton(
                        onClick = {
                            when (currentScreen) {
                                Screen.FILES -> {
                                    showNewDocDialog = true
                                }
                                Screen.TODO -> {
                                    val newItem = TodoItem(
                                        id = System.currentTimeMillis().toString(),
                                        text = "New task",
                                        completed = false,
                                        priority = null,
                                        projects = emptyList(),
                                        contexts = emptyList(),
                                        dueDate = null
                                    )
                                    todoItems = todoItems + newItem
                                }
                                Screen.QUICKNOTE -> {
                                    quickNoteContent = ""
                                }
                                Screen.MORE -> {
                                    currentSubScreen = SubScreen.SETTINGS
                                }
                            }
                        },
                        containerColor = accent
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (animationsEnabled) {
                    AnimatedContent(
                        targetState = currentSubScreen,
                        transitionSpec = {
                            if (targetState != null && initialState == null) {
                                ScreenTransitions.slideIn(durationMillis = 600) togetherWith
                                ScreenTransitions.slideOut(durationMillis = 600)
                            } else if (targetState == null && initialState != null) {
                                slideInHorizontally(animationSpec = tween(600)) { -it } togetherWith
                                slideOutHorizontally(animationSpec = tween(600)) { it }
                            } else {
                                ScreenTransitions.fade(durationMillis = 250) togetherWith
                                fadeOut(animationSpec = tween(250))
                            }
                        },
                        label = "SubScreenTransition"
                    ) { subScreen ->
                        when (subScreen) {
                            SubScreen.EDITOR -> IdeEditorScreen(
                                fileName = selectedFile ?: "Untitled",
                                content = fileContent,
                                onContentChanged = {
                                    fileContent = it
                                    // Update tab content
                                    if (activeTabIndex in openTabs.indices) {
                                        openTabs = openTabs.toMutableList().also { list ->
                                            list[activeTabIndex] = list[activeTabIndex].copy(
                                                content = it,
                                                isDirty = true
                                            )
                                        }
                                    }
                                },
                                showLineNumbers = showLineNumbers,
                                isDarkTheme = isDarkTheme,
                                onBackClick = { currentSubScreen = null },
                                onSaveClick = {
                                    selectedFile?.let { fileName ->
                                        val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                                        if (!docsDir.exists()) docsDir.mkdirs()
                                        val filePath = File(docsDir, fileName).absolutePath
                                        saveFile(filePath, fileContent)
                                    }
                                }
                            )
                            SubScreen.PREVIEW -> PreviewScreen(
                                fileName = selectedFile ?: "Untitled",
                                content = fileContent,
                                onBackClick = { currentSubScreen = null },
                                onExportClick = { showExportDialog = true }
                            )
                            SubScreen.SETTINGS -> SettingsScreen(
                                onBackClick = { currentSubScreen = null },
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
                            null -> {
                                AnimatedContent(
                                    targetState = currentScreen,
                                    transitionSpec = {
                                        if (targetState.ordinal > initialState.ordinal) {
                                            ScreenTransitions.slideIn(durationMillis = 450) togetherWith
                                            ScreenTransitions.slideOut(durationMillis = 450)
                                        } else {
                                            slideInHorizontally(animationSpec = tween(450)) { it } togetherWith
                                            slideOutHorizontally(animationSpec = tween(450)) { -it }
                                        }
                                    },
                                    label = "MainScreenTransition"
                                ) { screen ->
                                    when (screen) {
                                        Screen.FILES -> FilesScreen(
                                            searchQuery = fileSearchQuery,
                                            sortBy = fileSortBy,
                                            onSearchQueryChanged = { fileSearchQuery = it },
                                            onSortChanged = { fileSortBy = it },
                                            showSearch = showFileSearch,
                                            onShowSearchChanged = { showFileSearch = it },
                                            onFileSelected = { file, content ->
                                                openFileInTab(file, content)
                                            },
                                            onSettingsClick = { currentSubScreen = SubScreen.SETTINGS }
                                        )
                                        Screen.TODO -> TodoScreen(
                                            searchQuery = todoSearchQuery,
                                            filterType = todoFilterType,
                                            showSearch = showTodoSearch,
                                            showFilter = showTodoFilter,
                                            onSearchQueryChanged = { todoSearchQuery = it },
                                            onFilterTypeChanged = { todoFilterType = it },
                                            onShowSearchChanged = { showTodoSearch = it },
                                            onShowFilterChanged = { showTodoFilter = it },
                                            todoItems = todoItems,
                                            onTodoItemsChanged = { todoItems = it }
                                        )
                                        Screen.QUICKNOTE -> QuickNoteScreen(
                                            content = quickNoteContent,
                                            onContentChanged = { quickNoteContent = it },
                                            onSaveClick = {
                                                val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                                                if (!docsDir.exists()) docsDir.mkdirs()
                                                val filePath = File(docsDir, "quicknote.md").absolutePath
                                                saveFile(filePath, quickNoteContent)
                                            }
                                        )
                                        Screen.MORE -> MoreScreen(
                                            onSettingsClick = { currentSubScreen = SubScreen.SETTINGS },
                                            onFileBrowserClick = { currentSubScreen = SubScreen.FILE_BROWSER },
                                            onSearchClick = { showFileSearch = true },
                                            onBackupClick = { showBackupDialog = true },
                                            onAboutClick = { showAboutDialog = true }
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                } else {
                    // No animations - direct content switching
                    when (currentSubScreen) {
                        SubScreen.EDITOR -> IdeEditorScreen(
                            fileName = selectedFile ?: "Untitled",
                            content = fileContent,
                            onContentChanged = {
                                fileContent = it
                                if (activeTabIndex in openTabs.indices) {
                                    openTabs = openTabs.toMutableList().also { list ->
                                        list[activeTabIndex] = list[activeTabIndex].copy(
                                            content = it,
                                            isDirty = true
                                        )
                                    }
                                }
                            },
                            showLineNumbers = showLineNumbers,
                            isDarkTheme = isDarkTheme,
                            onBackClick = { currentSubScreen = null },
                            onSaveClick = {
                                selectedFile?.let { fileName ->
                                    val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                                    if (!docsDir.exists()) docsDir.mkdirs()
                                    val filePath = File(docsDir, fileName).absolutePath
                                    saveFile(filePath, fileContent)
                                }
                            }
                        )
                        SubScreen.PREVIEW -> PreviewScreen(
                            fileName = selectedFile ?: "Untitled",
                            content = fileContent,
                            onBackClick = { currentSubScreen = null },
                            onExportClick = { showExportDialog = true }
                        )
                        SubScreen.SETTINGS -> SettingsScreen(
                            onBackClick = { currentSubScreen = null },
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
                        null -> {
                            when (currentScreen) {
                                Screen.FILES -> FilesScreen(
                                    searchQuery = fileSearchQuery,
                                    sortBy = fileSortBy,
                                    onSearchQueryChanged = { fileSearchQuery = it },
                                    onSortChanged = { fileSortBy = it },
                                    showSearch = showFileSearch,
                                    onShowSearchChanged = { showFileSearch = it },
                                    onFileSelected = { file, content ->
                                        openFileInTab(file, content)
                                    },
                                    onSettingsClick = { currentSubScreen = SubScreen.SETTINGS }
                                )
                                Screen.TODO -> TodoScreen(
                                    searchQuery = todoSearchQuery,
                                    filterType = todoFilterType,
                                    showSearch = showTodoSearch,
                                    showFilter = showTodoFilter,
                                    onSearchQueryChanged = { todoSearchQuery = it },
                                    onFilterTypeChanged = { todoFilterType = it },
                                    onShowSearchChanged = { showTodoSearch = it },
                                    onShowFilterChanged = { showTodoFilter = it },
                                    todoItems = todoItems,
                                    onTodoItemsChanged = { todoItems = it }
                                )
                                Screen.QUICKNOTE -> QuickNoteScreen(
                                    content = quickNoteContent,
                                    onContentChanged = { quickNoteContent = it },
                                    onSaveClick = {
                                        val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                                        if (!docsDir.exists()) docsDir.mkdirs()
                                        val filePath = File(docsDir, "quicknote.md").absolutePath
                                        saveFile(filePath, quickNoteContent)
                                    }
                                )
                                Screen.MORE -> MoreScreen(
                                    onSettingsClick = { currentSubScreen = SubScreen.SETTINGS },
                                    onFileBrowserClick = { currentSubScreen = SubScreen.FILE_BROWSER },
                                    onSearchClick = { showFileSearch = true },
                                    onBackupClick = { showBackupDialog = true },
                                    onAboutClick = { showAboutDialog = true }
                                )
                            }
                        }
                        else -> {}
                    }
                }
            }

            // ===== DIALOGS =====

            // New Document Dialog
            if (showNewDocDialog) {
                IdeNewDocumentDialog(
                    selectedFormat = newDocFormat,
                    onFormatSelected = { newDocFormat = it },
                    onConfirm = {
                        val ext = when (newDocFormat) {
                            "markdown" -> ".md"
                            "todotxt" -> ".txt"
                            "csv" -> ".csv"
                            "latex" -> ".tex"
                            "orgmode" -> ".org"
                            else -> ".txt"
                        }
                        val fileName = "untitled$ext"
                        val template = when (newDocFormat) {
                            "markdown" -> "# New Document\n\nStart writing..."
                            "todotxt" -> "(A) New task @work +project"
                            "csv" -> "Name,Email,Phone\n"
                            "latex" -> "\\documentclass{article}\n\\begin{document}\n\n\\end{document}"
                            else -> ""
                        }
                        openFileInTab(fileName, template)
                        showNewDocDialog = false
                    },
                    onDismiss = { showNewDocDialog = false }
                )
            }

            // About Dialog
            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    title = { Text("About Yole") },
                    text = {
                        Column {
                            Text("Yole - Universal Text Editor")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Version: 2.15.1")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Platforms: Android, Desktop, iOS, Web")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Supports 17+ text formats including Markdown, LaTeX, CSV, and more.")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("(c) 2025 Milos Vasic")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Apache-2.0 License")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAboutDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // Export Dialog
            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    title = { Text("Export to PDF") },
                    text = {
                        Column {
                            Text("Export your document as a PDF file that can be shared or printed.")
                            Spacer(modifier = Modifier.height(16.dp))
                            if (exportInProgress) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Exporting...")
                                }
                            } else {
                                Text("Choose export format:")
                            }
                        }
                    },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    if (!exportInProgress) {
                                        selectedFile?.let { file ->
                                            exportInProgress = true
                                            coroutineScope.launch {
                                                val format = FormatRegistry.detectByFilename(file)
                                                val result = PdfExportUtil.exportToPdf(
                                                    context = context,
                                                    content = fileContent,
                                                    fileName = file,
                                                    format = format.id
                                                )

                                                exportInProgress = false
                                                showExportDialog = false

                                                if (result.isSuccess) {
                                                    val pdfUri = result.getOrNull()
                                                    if (pdfUri != null) {
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "application/pdf"
                                                            putExtra(Intent.EXTRA_STREAM, pdfUri)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            putExtra(Intent.EXTRA_SUBJECT, "Yole Document: $file")
                                                            putExtra(Intent.EXTRA_TEXT, "Here's the PDF export of my document from Yole.")
                                                        }
                                                        context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
                                                        Toast.makeText(context, "PDF exported successfully", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Failed to export PDF: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !exportInProgress
                            ) {
                                Text("Export")
                            }
                            TextButton(
                                onClick = { showExportDialog = false },
                                enabled = !exportInProgress
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                )
            }

            // Backup & Restore Dialog
            if (showBackupDialog) {
                AlertDialog(
                    onDismissRequest = { showBackupDialog = false },
                    title = { Text("Backup & Restore") },
                    text = {
                        Column {
                            Text("Backup your documents and settings to ensure you never lose your work.")
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Choose an option:")
                        }
                    },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                coroutineScope.launch {
                                    val result = BackupRestoreUtil.createBackup(context, settings)
                                    showBackupDialog = false
                                    if (result.isSuccess) {
                                        val backupUri = result.getOrNull()
                                        if (backupUri != null) {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/zip"
                                                putExtra(Intent.EXTRA_STREAM, backupUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                putExtra(Intent.EXTRA_SUBJECT, "Yole Backup")
                                                putExtra(Intent.EXTRA_TEXT, "Here's my Yole backup file.")
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
                                            Toast.makeText(context, "Backup created successfully", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Failed to create backup: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }) {
                                Text("Backup Now")
                            }
                            TextButton(onClick = {
                                backupFilePicker.launch(arrayOf("application/zip"))
                                showBackupDialog = false
                            }) {
                                Text("Restore")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBackupDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

// ===== IDE MAIN TOP BAR =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeMainTopBar(
    currentScreen: Screen,
    isDarkTheme: Boolean,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val bg = if (isDarkTheme) IdeTheme.darkSurface else IdeTheme.lightSurface
    TopAppBar(
        title = {
            Text(
                "Yole",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.semantics { contentDescription = "Open file explorer" }
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.semantics { contentDescription = "Search" }
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            }
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.semantics { contentDescription = "Settings" }
            ) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = bg
        )
    )
}

// ===== IDE EDITOR TOP BAR =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeEditorTopBar(
    fileName: String,
    isDarkTheme: Boolean,
    onMenuClick: () -> Unit,
    onSaveClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val bg = if (isDarkTheme) IdeTheme.darkSurface else IdeTheme.lightSurface
    TopAppBar(
        title = {
            Text(
                fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.semantics { contentDescription = "File explorer" }
            ) {
                Icon(Icons.Filled.Menu, contentDescription = "Explorer")
            }
            IconButton(
                onClick = onSaveClick,
                modifier = Modifier.semantics { contentDescription = "Save file" }
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Save")
            }
            IconButton(
                onClick = onPreviewClick,
                modifier = Modifier.semantics { contentDescription = "Preview document" }
            ) {
                Icon(Icons.Filled.Info, contentDescription = "Preview")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = bg
        )
    )
}

// ===== IDE TAB BAR =====

@Composable
fun IdeTabBar(
    tabs: List<EditorTab>,
    activeIndex: Int,
    isDarkTheme: Boolean,
    tabActive: Color,
    tabInactive: Color,
    border: Color,
    textColor: Color,
    textSecondary: Color,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    onNewTab: () -> Unit
) {
    val surfaceVar = if (isDarkTheme) IdeTheme.darkSurfaceVariant else IdeTheme.lightSurfaceVariant

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(surfaceVar)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom
        ) {
            tabs.forEachIndexed { index, tab ->
                val isActive = index == activeIndex
                val bg = if (isActive) tabActive else tabInactive
                val txt = if (isActive) textColor else textSecondary

                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clickable { onTabSelected(index) }
                        .background(bg)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        tab.fileName + if (tab.isDirty) " *" else "",
                        color = txt,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 120.dp)
                            .semantics { contentDescription = "Tab: ${tab.fileName}" }
                    )
                    Box(
                        modifier = Modifier
                            .clickable { onTabClosed(index) }
                            .padding(start = 2.dp)
                            .semantics { contentDescription = "Close tab ${tab.fileName}" }
                    ) {
                        Text(
                            "x",
                            color = textSecondary.copy(alpha = if (isActive) 0.7f else 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // New tab button
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clickable(onClick = onNewTab)
                    .padding(horizontal = 10.dp)
                    .semantics { contentDescription = "New tab" },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+",
                    color = textSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(border))
    }
}

// ===== IDE EDITOR SCREEN =====

@Composable
fun IdeEditorScreen(
    fileName: String,
    content: String,
    onContentChanged: (String) -> Unit,
    showLineNumbers: Boolean,
    isDarkTheme: Boolean,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    var text by remember(content) { mutableStateOf(content) }
    val format = remember(fileName) { FormatRegistry.detectByFilename(fileName) }
    val bg = if (isDarkTheme) IdeTheme.darkBackground else IdeTheme.lightBackground
    val textColor = if (isDarkTheme) IdeTheme.darkText else IdeTheme.lightText
    val lineNumColor = if (isDarkTheme) IdeTheme.darkLineNumbers else IdeTheme.lightLineNumbers
    val borderColor = if (isDarkTheme) IdeTheme.darkBorder else IdeTheme.lightBorder

    // Undo/Redo history
    var history by remember { mutableStateOf(listOf(content)) }
    var historyIndex by remember { mutableStateOf(0) }

    fun addToHistory(newText: String) {
        val newHistory = history.take(historyIndex + 1) + newText
        history = newHistory.takeLast(50)
        historyIndex = (history.size - 1).coerceAtLeast(0)
    }

    var showFindBar by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var findMatchCount by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        // General editor toolbar (undo/redo/find)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(if (isDarkTheme) IdeTheme.darkSurface else IdeTheme.lightSurface)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IdeToolbarButton("Undo", "Undo", textColor) {
                if (historyIndex > 0) {
                    historyIndex--
                    text = history[historyIndex]
                    onContentChanged(text)
                }
            }
            IdeToolbarButton("Redo", "Redo", textColor) {
                if (historyIndex < history.size - 1) {
                    historyIndex++
                    text = history[historyIndex]
                    onContentChanged(text)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IdeToolbarButton("Find", "Find in document", textColor) {
                showFindBar = !showFindBar
            }

            // Format-specific tools (markdown)
            if (format.id == "markdown") {
                Spacer(modifier = Modifier.width(8.dp))
                IdeToolbarButton("B", "Bold", textColor) { text += "**bold**"; onContentChanged(text) }
                IdeToolbarButton("I", "Italic", textColor) { text += "*italic*"; onContentChanged(text) }
                IdeToolbarButton("H", "Header", textColor) { text += "# "; onContentChanged(text) }
                IdeToolbarButton("Lk", "Link", textColor) { text += "[text](url)"; onContentChanged(text) }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))

        // Find bar
        if (showFindBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkTheme) Color(0xFF252526) else Color(0xFFF3F3F3))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = findText,
                    onValueChange = { query ->
                        findText = query
                        findMatchCount = if (query.isNotEmpty()) {
                            text.lowercase().split(query.lowercase()).size - 1
                        } else 0
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    placeholder = { Text("Find...", fontSize = 12.sp) },
                    textStyle = TextStyle(fontSize = 12.sp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (findText.isNotEmpty()) "$findMatchCount matches" else "",
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
                IconButton(
                    onClick = { showFindBar = false; findText = "" },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Close, "Close find", tint = textColor, modifier = Modifier.size(16.dp))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(borderColor))
        }

        // Editor with line numbers
        Row(modifier = Modifier.weight(1f)) {
            // Line number gutter
            if (showLineNumbers) {
                val lines = text.lines()
                val gutterBg = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF8F8F8)
                val gutterWidth = when {
                    lines.size >= 1000 -> 48.dp
                    lines.size >= 100 -> 40.dp
                    else -> 32.dp
                }

                Column(
                    modifier = Modifier
                        .width(gutterWidth)
                        .fillMaxHeight()
                        .background(gutterBg)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp, end = 4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    lines.forEachIndexed { index, _ ->
                        Text(
                            text = "${index + 1}",
                            color = lineNumColor,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp,
                            modifier = Modifier.semantics {
                                contentDescription = "Line ${index + 1}"
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(borderColor)
                )
            }

            // Text editor
            OutlinedTextField(
                value = text,
                onValueChange = { newText ->
                    val oldText = text
                    text = newText
                    onContentChanged(newText)
                    if (newText.length - oldText.length > 5 || oldText.length - newText.length > 5 ||
                        newText.endsWith(" ") || newText.endsWith("\n")) {
                        addToHistory(newText)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics { contentDescription = "Code editor for $fileName" },
                placeholder = { Text("Start typing...") },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = textColor
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = bg,
                    unfocusedContainerColor = bg,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }
    }
}

// ===== IDE MARKDOWN TOOLBAR =====

@Composable
fun IdeMarkdownToolbar(
    isDarkTheme: Boolean,
    onInsert: (String) -> Unit
) {
    val surface = if (isDarkTheme) IdeTheme.darkSurface else IdeTheme.lightSurface
    val textColor = if (isDarkTheme) IdeTheme.darkText else IdeTheme.lightText
    val border = if (isDarkTheme) IdeTheme.darkBorder else IdeTheme.lightBorder

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(surface)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IdeToolbarButton("B", "Bold", textColor) { onInsert("bold") }
            IdeToolbarButton("I", "Italic", textColor) { onInsert("italic") }
            IdeToolbarButton("H", "Header", textColor) { onInsert("header") }
            IdeToolbarButton("Lk", "Link", textColor) { onInsert("link") }
            IdeToolbarButton("<>", "Code", textColor) { onInsert("code") }
            IdeToolbarButton("Li", "List", textColor) { onInsert("list") }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(border))
    }
}

@Composable
fun IdeToolbarButton(label: String, description: String, textColor: Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(28.dp)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 11.sp, color = textColor, fontFamily = FontFamily.Monospace)
    }
}

// ===== IDE STATUS BAR =====

@Composable
fun IdeStatusBar(
    content: String,
    fileName: String,
    isDarkTheme: Boolean,
    statusBarBg: Color
) {
    val lines = content.lines()
    val lineCount = lines.size
    val cursorLine = lineCount
    val cursorCol = (lines.lastOrNull()?.length ?: 0) + 1
    val wordCount = content.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    val format = remember(fileName) { FormatRegistry.detectByFilename(fileName) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(statusBarBg)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Ln $cursorLine, Col $cursorCol",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.semantics {
                    contentDescription = "Line $cursorLine, Column $cursorCol"
                }
            )
            Text(
                "$wordCount words",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
        }

        // Right side
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                format.name,
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.semantics { contentDescription = "Format: ${format.name}" }
            )
            Text("UTF-8", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
        }
    }
}

// ===== IDE BOTTOM NAVIGATION =====

@Composable
fun IdeBottomNavBar(
    currentScreen: Screen,
    isDarkTheme: Boolean,
    onScreenSelected: (Screen) -> Unit
) {
    val bg = if (isDarkTheme) IdeTheme.darkSurface else IdeTheme.lightSurface
    val border = if (isDarkTheme) IdeTheme.darkBorder else IdeTheme.lightBorder
    val accent = if (isDarkTheme) IdeTheme.darkAccent else IdeTheme.lightAccent

    Column {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(border))
        NavigationBar(
            containerColor = bg
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Filled.List, contentDescription = "Files") },
                label = { Text("Files", fontSize = 10.sp) },
                selected = currentScreen == Screen.FILES,
                onClick = { onScreenSelected(Screen.FILES) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    indicatorColor = accent.copy(alpha = 0.15f)
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Filled.CheckCircle, contentDescription = "To-Do") },
                label = { Text("To-Do", fontSize = 10.sp) },
                selected = currentScreen == Screen.TODO,
                onClick = { onScreenSelected(Screen.TODO) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    indicatorColor = accent.copy(alpha = 0.15f)
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Filled.Edit, contentDescription = "QuickNote") },
                label = { Text("Edit", fontSize = 10.sp) },
                selected = currentScreen == Screen.QUICKNOTE,
                onClick = { onScreenSelected(Screen.QUICKNOTE) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    indicatorColor = accent.copy(alpha = 0.15f)
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                label = { Text("More", fontSize = 10.sp) },
                selected = currentScreen == Screen.MORE,
                onClick = { onScreenSelected(Screen.MORE) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = accent,
                    indicatorColor = accent.copy(alpha = 0.15f)
                )
            )
        }
    }
}

// ===== IDE DRAWER CONTENT =====

@Composable
fun IdeDrawerContent(
    isDarkTheme: Boolean,
    surface: Color,
    border: Color,
    textColor: Color,
    textSecondary: Color,
    accent: Color,
    openTabs: List<EditorTab>,
    activeTabIndex: Int,
    onNewDocument: () -> Unit,
    onTabSelected: (Int) -> Unit,
    onTabDeleted: (Int) -> Unit,
    onOpenFileBrowser: () -> Unit,
    onSettingsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(surface)
    ) {
        // Drawer header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(accent)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "EXPLORER",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            IconButton(
                onClick = onNewDocument,
                modifier = Modifier.semantics { contentDescription = "New document" }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New", tint = Color.White)
            }
        }

        // Open documents section
        Text(
            "OPEN DOCUMENTS",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (openTabs.isEmpty()) {
            Text(
                "No documents open",
                color = textSecondary.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        } else {
            openTabs.forEachIndexed { index, tab ->
                val isActive = index == activeTabIndex
                val itemBg = if (isActive) accent.copy(alpha = 0.15f) else Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTabSelected(index) }
                        .background(itemBg)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isActive) accent else textSecondary
                        )
                        Text(
                            tab.fileName + if (tab.isDirty) " *" else "",
                            color = if (isActive) textColor else textSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { onTabDeleted(index) },
                        modifier = Modifier
                            .size(24.dp)
                            .semantics { contentDescription = "Delete ${tab.fileName}" }
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(14.dp),
                            tint = textSecondary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = border)

        // Actions section
        Text(
            "ACTIONS",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        DrawerActionItem(
            icon = Icons.Filled.List,
            label = "Browse Files",
            textColor = textColor,
            onClick = onOpenFileBrowser
        )
        DrawerActionItem(
            icon = Icons.Filled.Add,
            label = "New Document",
            textColor = textColor,
            onClick = onNewDocument
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = border)

        // Settings section
        Text(
            "CONFIGURATION",
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        DrawerActionItem(
            icon = Icons.Filled.Settings,
            label = "Settings",
            textColor = textColor,
            onClick = onSettingsClick
        )
        DrawerActionItem(
            icon = Icons.Filled.Check,
            label = "Backup & Restore",
            textColor = textColor,
            onClick = onBackupClick
        )
        DrawerActionItem(
            icon = Icons.Filled.Info,
            label = "About Yole",
            textColor = textColor,
            onClick = onAboutClick
        )

        Spacer(modifier = Modifier.weight(1f))

        // Version info at bottom
        Text(
            "Yole v2.15.1",
            color = textSecondary.copy(alpha = 0.5f),
            fontSize = 10.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun DrawerActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = textColor.copy(alpha = 0.7f)
        )
        Text(label, color = textColor, fontSize = 14.sp)
    }
}

// ===== NEW DOCUMENT DIALOG =====

@Composable
fun IdeNewDocumentDialog(
    selectedFormat: String,
    onFormatSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val formats = listOf(
        "markdown" to "Markdown (.md)",
        "plaintext" to "Plain Text (.txt)",
        "todotxt" to "Todo.txt (.todo.txt)",
        "csv" to "CSV (.csv)",
        "latex" to "LaTeX (.tex)",
        "orgmode" to "Org Mode (.org)",
        "asciidoc" to "AsciiDoc (.adoc)",
        "wikitext" to "WikiText (.wiki)",
        "restructuredtext" to "reStructuredText (.rst)",
        "rmarkdown" to "R Markdown (.Rmd)",
        "taskpaper" to "TaskPaper (.taskpaper)",
        "textile" to "Textile (.textile)",
        "creole" to "Creole (.creole)",
        "tiddlywiki" to "TiddlyWiki (.tid)",
        "jupyter" to "Jupyter Notebook (.ipynb)",
        "keyvalue" to "Key-Value (.properties)",
        "binary" to "Binary (.bin)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Document") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Choose format (${formats.size} available):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                formats.forEach { (id, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFormatSelected(id) }
                            .background(
                                if (selectedFormat == id) Color(0xFF007ACC).copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == id,
                            onClick = { onFormatSelected(id) },
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ===== EXISTING COMPONENTS (preserved with IDE styling) =====

/**
 * Generic empty state component
 */
@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onActionClick,
                modifier = Modifier.pressScale()
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun EmptyFileListState(onCreateFile: () -> Unit) {
    EmptyState(
        icon = Icons.Filled.Add,
        title = "No files yet",
        description = "This folder is empty.\nCreate your first file to get started.",
        actionLabel = "Create File",
        onActionClick = onCreateFile
    )
}

@Composable
fun EmptySearchState(searchQuery: String) {
    EmptyState(
        icon = Icons.Filled.Search,
        title = "No results found",
        description = "No files match \"$searchQuery\".\nTry a different search term."
    )
}

@Composable
fun EmptyTodoListState() {
    EmptyState(
        icon = Icons.Filled.CheckCircle,
        title = "No tasks yet",
        description = "Add your first task above to get started.\nStay organized and productive!"
    )
}

@Composable
fun ErrorState(
    title: String = "Something went wrong",
    description: String = "An error occurred while loading.\nPlease try again.",
    actionLabel: String = "Retry",
    onRetry: () -> Unit
) {
    EmptyState(
        icon = Icons.Filled.Warning,
        title = title,
        description = description,
        actionLabel = actionLabel,
        onActionClick = onRetry
    )
}

@Composable
fun FileCardSkeleton() {
    val shimmerProgress = LoadingAnimations.rememberShimmer()
    val shimmerAlpha = 0.3f + (shimmerProgress * 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.Start) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha),
                            shape = MaterialTheme.shapes.small
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(20.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(16.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha),
                        shape = MaterialTheme.shapes.small
                    )
            )
        }
    }
}

@Composable
fun FileBrowserScreen(
    searchQuery: String = "",
    sortBy: String = "name",
    onSearchQueryChanged: (String) -> Unit = {},
    onSortChanged: (String) -> Unit = {},
    showSearch: Boolean = false,
    onShowSearchChanged: (Boolean) -> Unit = {},
    onFileSelected: (String, String) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentDirectory by remember { mutableStateOf<File?>(null) }
    var allFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoadingFiles by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoadingFiles = true
        kotlinx.coroutines.delay(300)
        val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
        if (docsDir.exists()) {
            currentDirectory = docsDir
            allFiles = docsDir.listFiles()?.toList() ?: emptyList()
        } else {
            currentDirectory = context.filesDir
            allFiles = context.filesDir.listFiles()?.toList() ?: emptyList()
        }
        isLoadingFiles = false
    }

    val files = remember(allFiles, searchQuery, sortBy) {
        var filtered = allFiles.filter { file ->
            searchQuery.isEmpty() || file.name.contains(searchQuery, ignoreCase = true)
        }
        filtered = when (sortBy) {
            "name" -> filtered.sortedBy { it.name.lowercase() }
            "date" -> filtered.sortedByDescending { it.lastModified() }
            "size" -> filtered.sortedByDescending { if (it.isFile) it.length() else 0L }
            else -> filtered
        }
        filtered
    }

    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val documentFile = DocumentFile.fromTreeUri(context, it)
            Toast.makeText(context, "Directory selected: ${documentFile?.name}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search files...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "File Browser",
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = { directoryPicker.launch(null) }) {
                Icon(Icons.Filled.List, contentDescription = "Open Folder", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Open Folder")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        currentDirectory?.let { dir ->
            Text(
                text = dir.absolutePath,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LoadingStateWrapper(
            isLoading = isLoadingFiles,
            loadingContent = {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(5) {
                        FileCardSkeleton()
                    }
                }
            }
        ) {
            if (files.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (searchQuery.isNotEmpty()) {
                        EmptySearchState(searchQuery = searchQuery)
                    } else {
                        EmptyFileListState(onCreateFile = { onFileSelected("untitled.txt", "") })
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(
                        items = files,
                        key = { _, file -> file.absolutePath }
                    ) { index, file ->
                        val isDirectory = file.isDirectory
                        val fileName = file.name
                        val fileSize = if (file.isFile) "${file.length()} bytes" else ""

                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = ListAnimations.itemEnter(index),
                            modifier = Modifier
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .pressScale(scale = 0.97f),
                                onClick = {
                                    if (isDirectory) {
                                        isLoadingFiles = true
                                        currentDirectory = file
                                        coroutineScope.launch {
                                            delay(200)
                                            allFiles = file.listFiles()?.toList() ?: emptyList()
                                            isLoadingFiles = false
                                        }
                                    } else {
                                        try {
                                            val content = file.readText()
                                            onFileSelected(fileName, content)
                                        } catch (e: Exception) {
                                            onFileSelected(fileName, "")
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            if (isDirectory) Icons.Filled.List else Icons.Filled.Edit,
                                            contentDescription = if (isDirectory) "Folder" else "File",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = fileName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    if (!isDirectory && fileSize.isNotEmpty()) {
                                        Text(
                                            text = fileSize,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = {
                    currentDirectory?.parentFile?.let { parent ->
                        isLoadingFiles = true
                        currentDirectory = parent
                        coroutineScope.launch {
                            delay(200)
                            allFiles = parent.listFiles()?.toList() ?: emptyList()
                            isLoadingFiles = false
                        }
                    }
                },
                enabled = currentDirectory?.parentFile != null && !isLoadingFiles,
                modifier = Modifier.pressScale()
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Up")
            }

            OutlinedButton(
                onClick = { onFileSelected("untitled.txt", "") },
                modifier = Modifier.pressScale()
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New File", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New File")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Supported formats: ${FormatRegistry.formats.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EditorScreen(
    fileName: String,
    content: String,
    onContentChanged: (String) -> Unit,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {}
) {
    // Redirect to IDE editor
    IdeEditorScreen(
        fileName = fileName,
        content = content,
        onContentChanged = onContentChanged,
        showLineNumbers = true,
        isDarkTheme = isSystemInDarkTheme(),
        onBackClick = onBackClick,
        onSaveClick = onSaveClick
    )
}

@Composable
fun MarkdownActionButtons(onInsert: (String) -> Unit) {
    IdeMarkdownToolbar(
        isDarkTheme = isSystemInDarkTheme(),
        onInsert = onInsert
    )
}

@Composable
fun ActionButton(text: String, description: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .height(48.dp)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PreviewScreen(
    fileName: String,
    content: String,
    onBackClick: () -> Unit = {},
    onExportClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val format = remember(fileName) { FormatRegistry.detectByFilename(fileName) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Preview: $fileName",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Format: ${format.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row {
                TextButton(onClick = { onExportClick() }) {
                    Icon(Icons.Filled.Share, contentDescription = "Export", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export PDF")
                }
            }
        }

        // Use shared parsers for proper format rendering
        val parsedContent = remember(content, format) {
            try {
                val parser = digital.vasic.yole.format.ParserRegistry.getParser(format)
                parser?.parse(content)?.parsedContent ?: content
            } catch (_: Exception) {
                content
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = parsedContent,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 22.sp
                )
            )
        }
    }
}

fun generateHtmlPreview(content: String, format: digital.vasic.yole.format.TextFormat, isDark: Boolean): String {
    val themeClass = if (isDark) "dark-theme" else "light-theme"

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <style>
            body {
                font-family: 'Roboto', sans-serif;
                margin: 16px;
                line-height: 1.6;
            }
            .$themeClass {
                ${if (isDark) """
                    background-color: #121212;
                    color: #ffffff;
                """ else """
                    background-color: #ffffff;
                    color: #000000;
                """}
            }
            h1, h2, h3, h4, h5, h6 {
                color: ${if (isDark) "#bb86fc" else "#1976d2"};
                margin-top: 24px;
                margin-bottom: 16px;
            }
            code {
                background-color: ${if (isDark) "#333333" else "#f5f5f5"};
                padding: 2px 4px;
                border-radius: 4px;
                font-family: 'Courier New', monospace;
            }
            pre {
                background-color: ${if (isDark) "#333333" else "#f5f5f5"};
                padding: 16px;
                border-radius: 8px;
                overflow-x: auto;
            }
            blockquote {
                border-left: 4px solid ${if (isDark) "#bb86fc" else "#1976d2"};
                padding-left: 16px;
                margin-left: 0;
                color: ${if (isDark) "#cccccc" else "#666666"};
            }
        </style>
    </head>
    <body class="$themeClass">
        ${convertToHtml(content, format)}
    </body>
    </html>
    """.trimIndent()
}

fun convertToHtml(content: String, format: digital.vasic.yole.format.TextFormat): String {
    return when (format.id) {
        "markdown" -> convertMarkdownToHtml(content)
        "plaintext" -> "<pre>$content</pre>"
        "todotxt" -> convertTodoTxtToHtml(content)
        else -> "<pre>$content</pre>"
    }
}

fun convertMarkdownToHtml(content: String): String {
    return content
        .replace(Regex("^### (.*)$", RegexOption.MULTILINE), "<h3>$1</h3>")
        .replace(Regex("^## (.*)$", RegexOption.MULTILINE), "<h2>$1</h2>")
        .replace(Regex("^# (.*)$", RegexOption.MULTILINE), "<h1>$1</h1>")
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "<strong>$1</strong>")
        .replace(Regex("\\*(.*?)\\*"), "<em>$1</em>")
        .replace(Regex("`(.*?)`"), "<code>$1</code>")
        .replace(Regex("\n"), "<br>")
}

fun convertTodoTxtToHtml(content: String): String {
    val lines = content.lines()
    val html = lines.joinToString("<br>") { line ->
        if (line.trim().isEmpty()) {
            ""
        } else if (line.startsWith("x ")) {
            "<span style='text-decoration: line-through; color: #666;'>$line</span>"
        } else {
            "<span>$line</span>"
        }
    }
    return html
}

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    themeMode: String,
    onThemeModeChanged: (String) -> Unit,
    showLineNumbers: Boolean,
    onShowLineNumbersChanged: (Boolean) -> Unit,
    autoSave: Boolean,
    onAutoSaveChanged: (Boolean) -> Unit,
    animationsEnabled: Boolean,
    onAnimationsEnabledChanged: (Boolean) -> Unit
) {
    val isDarkTheme = isSystemInDarkTheme()
    val bg = if (isDarkTheme) IdeTheme.darkBackground else IdeTheme.lightBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Theme settings
        Text(
            text = "APPEARANCE",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = themeMode == "system",
                onClick = { onThemeModeChanged("system") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("System theme")
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
            Text("Dark theme (IDE)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Editor settings
        Text(
            text = "EDITOR",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
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
            text = "ANIMATIONS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
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
            text = "FORMATS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Supported formats: ${FormatRegistry.formats.size}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        FormatRegistry.formats.take(5).forEach { format ->
            Text(
                text = "  ${format.name} (${format.extensions.joinToString(", ")})",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        if (FormatRegistry.formats.size > 5) {
            Text(
                text = "  ... and ${FormatRegistry.formats.size - 5} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About
        Text(
            text = "ABOUT",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Yole is a cross-platform text editor supporting 17 text formats including Markdown, Todo.txt, CSV, LaTeX, AsciiDoc, and more.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Version: 2.15.1",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        // Storage settings section
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "STORAGE",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Local file system storage is used for documents.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Cloud storage protocols: Dropbox, Google Drive, OneDrive, FTP, SFTP, SMB, WebDAV",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// Bottom Navigation Bar - now redirects to IDE bottom nav
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    IdeBottomNavBar(
        currentScreen = currentScreen,
        isDarkTheme = isSystemInDarkTheme(),
        onScreenSelected = onScreenSelected
    )
}

// Top Bars
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesTopBar(
    onSearchClick: () -> Unit,
    onSortClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Files", fontFamily = FontFamily.Monospace) },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            }
            IconButton(onClick = onSortClick) {
                Icon(Icons.Filled.List, contentDescription = "Sort")
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoTopBar(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    TopAppBar(
        title = { Text("To-Do", fontFamily = FontFamily.Monospace) },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            }
            IconButton(onClick = onFilterClick) {
                Icon(Icons.Filled.Search, contentDescription = "Filter")
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNoteTopBar(
    onSaveClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    TopAppBar(
        title = { Text("QuickNote", fontFamily = FontFamily.Monospace) },
        actions = {
            IconButton(onClick = onSaveClick) {
                Icon(Icons.Filled.Check, contentDescription = "Save")
            }
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreTopBar() {
    TopAppBar(
        title = { Text("More", fontFamily = FontFamily.Monospace) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    fileName: String,
    onSaveClick: () -> Unit,
    onPreviewClick: () -> Unit,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text(fileName, maxLines = 1, fontFamily = FontFamily.Monospace, fontSize = 14.sp) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onSaveClick) {
                Icon(Icons.Filled.Check, contentDescription = "Save")
            }
            IconButton(onClick = onPreviewClick) {
                Icon(Icons.Filled.Info, contentDescription = "Preview")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewTopBar(
    fileName: String,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit,
    onExportClick: () -> Unit
) {
    TopAppBar(
        title = { Text("$fileName (Preview)", maxLines = 1, fontFamily = FontFamily.Monospace, fontSize = 14.sp) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onExportClick) {
                Icon(Icons.Filled.Share, contentDescription = "Export")
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text("Settings", fontFamily = FontFamily.Monospace) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    )
}

// Screen Composables
@Composable
fun FilesScreen(
    searchQuery: String,
    sortBy: String,
    onSearchQueryChanged: (String) -> Unit,
    onSortChanged: (String) -> Unit,
    showSearch: Boolean,
    onShowSearchChanged: (Boolean) -> Unit,
    onFileSelected: (String, String) -> Unit,
    onSettingsClick: () -> Unit
) {
    FileBrowserScreen(
        searchQuery = searchQuery,
        sortBy = sortBy,
        onSearchQueryChanged = onSearchQueryChanged,
        onSortChanged = onSortChanged,
        showSearch = showSearch,
        onShowSearchChanged = onShowSearchChanged,
        onFileSelected = onFileSelected,
        onSettingsClick = onSettingsClick
    )
}

@Composable
fun TodoScreen(
    searchQuery: String,
    filterType: String,
    showSearch: Boolean,
    showFilter: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onFilterTypeChanged: (String) -> Unit,
    onShowSearchChanged: (Boolean) -> Unit,
    onShowFilterChanged: (Boolean) -> Unit,
    todoItems: List<TodoItem>,
    onTodoItemsChanged: (List<TodoItem>) -> Unit
) {
    var newTodoText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Search and Filter Row
        if (showSearch || showFilter) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search todos...") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                }

                if (showFilter) {
                    var expanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(
                                text = when (filterType) {
                                    "all" -> "All Tasks"
                                    "active" -> "Active"
                                    "completed" -> "Completed"
                                    else -> "Filter"
                                }
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Filter options",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Tasks") },
                                onClick = {
                                    onFilterTypeChanged("all")
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Active Tasks") },
                                onClick = {
                                    onFilterTypeChanged("active")
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Completed Tasks") },
                                onClick = {
                                    onFilterTypeChanged("completed")
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Header and Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "To-Do List",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace
            )

            Row {
                TextButton(onClick = {
                    onFilterTypeChanged(
                        when (filterType) {
                            "all" -> "active"
                            "active" -> "completed"
                            "completed" -> "all"
                            else -> "all"
                        }
                    )
                }) {
                    Text(
                        when (filterType) {
                            "all" -> "Show Active"
                            "active" -> "Show Completed"
                            "completed" -> "Show All"
                            else -> "Filter"
                        }
                    )
                }
            }
        }

        // Add new todo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTodoText,
                onValueChange = { newTodoText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add new todo...") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (newTodoText.isNotBlank()) {
                        val newItem = TodoItem(
                            id = System.currentTimeMillis().toString(),
                            text = newTodoText,
                            completed = false,
                            priority = null,
                            projects = emptyList(),
                            contexts = emptyList(),
                            dueDate = null
                        )
                        onTodoItemsChanged(todoItems + newItem)
                        newTodoText = ""
                    }
                },
                modifier = Modifier.pressScale()
            ) {
                Text("Add")
            }
        }

        // Filter todos
        val filteredTodos = todoItems.filter { item ->
            val matchesSearch = searchQuery.isEmpty() ||
                item.text.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterType) {
                "active" -> !item.completed
                "completed" -> item.completed
                else -> true
            }

            matchesSearch && matchesFilter
        }

        // Todo list or empty state
        if (filteredTodos.isEmpty() && todoItems.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                EmptyTodoListState()
            }
        } else if (filteredTodos.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) {
                        "No tasks match your search."
                    } else {
                        "No tasks in this filter view."
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(
                    items = filteredTodos,
                    key = { _, item -> item.id }
                ) { index, item ->
                    androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = ListAnimations.itemEnter(index),
                        exit = ListAnimations.itemExit(),
                        modifier = Modifier.animateItem()
                    ) {
                        TodoItemRow(
                            item = item,
                            onToggleComplete = { completed ->
                                onTodoItemsChanged(
                                    todoItems.map {
                                        if (it.id == item.id) it.copy(completed = completed) else it
                                    }
                                )
                            },
                            onDelete = {
                                onTodoItemsChanged(todoItems.filter { it.id != item.id })
                            }
                        )
                    }
                }
            }
        }
    }
}

data class TodoItem(
    val id: String,
    val text: String,
    val completed: Boolean,
    val priority: Char?,
    val projects: List<String>,
    val contexts: List<String>,
    val dueDate: String?
)

@Composable
fun TodoItemRow(
    item: TodoItem,
    onToggleComplete: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pressScale(scale = 0.98f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.completed,
                onCheckedChange = onToggleComplete
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.text,
                    style = if (item.completed) {
                        MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = TextDecoration.LineThrough
                        )
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    color = if (item.completed) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                val tags = (item.projects.map { "+$it" } + item.contexts.map { "@$it" }).joinToString(" ")
                if (tags.isNotEmpty()) {
                    Text(
                        text = tags,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun QuickNoteScreen(content: String, onContentChanged: (String) -> Unit, onSaveClick: () -> Unit = {}) {
    var noteContent by remember { mutableStateOf(content) }
    var isPreviewMode by remember { mutableStateOf(false) }
    val isDarkTheme = isSystemInDarkTheme()
    val bg = if (isDarkTheme) IdeTheme.darkBackground else IdeTheme.lightBackground

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        // Quick actions toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QuickNote",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace
            )

            Row {
                TextButton(onClick = { isPreviewMode = !isPreviewMode }) {
                    Text(if (isPreviewMode) "Edit" else "Preview")
                }
                TextButton(onClick = {
                    onContentChanged(noteContent)
                    onSaveClick()
                }) {
                    Text("Save")
                }
            }
        }

        if (isPreviewMode) {
            val format = remember { FormatRegistry.detectByFilename("quicknote.md") }
            val htmlContent = remember(noteContent, format, isDarkTheme) {
                generateHtmlPreview(noteContent, format, isDarkTheme)
            }

            Text(
                text = htmlContent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            OutlinedTextField(
                value = noteContent,
                onValueChange = {
                    noteContent = it
                    onContentChanged(it)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Start writing your quick note...") },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            )
        }
    }
}

@Composable
fun MoreScreen(
    onSettingsClick: () -> Unit = {},
    onFileBrowserClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val isDarkTheme = isSystemInDarkTheme()
    val bg = if (isDarkTheme) IdeTheme.darkBackground else IdeTheme.lightBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(16.dp)
    ) {
        Text(
            text = "More Options",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Settings option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(scale = 0.98f),
            onClick = onSettingsClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Settings", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Configure app preferences and behavior",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // File browser option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(scale = 0.98f),
            onClick = onFileBrowserClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.List, contentDescription = "File Browser")
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("File Browser", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Browse and manage your files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(scale = 0.98f),
            onClick = onSearchClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Search")
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Search", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Search through your notes and files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Backup/Restore option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(scale = 0.98f),
            onClick = onBackupClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Backup")
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Backup & Restore", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Backup your data or restore from backup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // About option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(scale = 0.98f),
            onClick = onAboutClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Info, contentDescription = "About")
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("About Yole", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Version 2.15.1 - Text editor for Android, Desktop, iOS & Web",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewYoleApp() {
    YoleApp()
}
