/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Main Compose UI for Yole Android App
 *
 *########################################################*/

package digital.vasic.yole.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.Checkbox
import androidx.compose.ui.text.style.TextDecoration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
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
import java.io.File

/**
 * Settings manager for Yole app
 */
class YoleSettings(context: android.content.Context) : GsSharedPreferencesPropertyBackend(context, "yole_settings") {

    // Theme settings
    fun getThemeMode(): String = getString("theme_mode", "system")
    fun setThemeMode(mode: String) = setString("theme_mode", mode)

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
        file.parentFile?.mkdirs() // Create parent directories if they don't exist
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
    val context = LocalContext.current
    val settings = remember { YoleSettings(context) }

    var currentScreen by remember { mutableStateOf(Screen.FILES) }
    var currentSubScreen by remember { mutableStateOf<SubScreen?>(null) }
    var selectedFile by remember { mutableStateOf<String?>(null) }
    var fileContent by remember { mutableStateOf("") }
    var quickNoteContent by remember { mutableStateOf("") }
    var showFileSearch by remember { mutableStateOf(false) }
    var fileSearchQuery by remember { mutableStateOf("") }
    var fileSortBy by remember { mutableStateOf("name") }

    // TODO screen state
    var showTodoSearch by remember { mutableStateOf(false) }
    var todoSearchQuery by remember { mutableStateOf("") }
    var showTodoFilter by remember { mutableStateOf(false) }
    var todoFilterType by remember { mutableStateOf("all") } // all, active, completed
    var todoItems by remember { mutableStateOf(listOf<String>()) }

    // Editor state
    var editorHistory by remember { mutableStateOf(listOf<String>()) }
    var editorHistoryIndex by remember { mutableStateOf(-1) }

    // Dialog states
    var showAboutDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    // Load settings
    var themeMode by remember { mutableStateOf(settings.getThemeMode()) }
    var showLineNumbers by remember { mutableStateOf(settings.getShowLineNumbers()) }
    var autoSave by remember { mutableStateOf(settings.getAutoSave()) }
    var animationsEnabled by remember { mutableStateOf(settings.getAnimationsEnabled()) }

    // Initialize parsers with lazy loading for faster startup
    LaunchedEffect(Unit) {
        digital.vasic.yole.format.ParserInitializer.registerAllParsersLazy()
    }

    Scaffold(
        topBar = {
            when (currentSubScreen) {
                SubScreen.EDITOR -> EditorTopBar(
                    fileName = selectedFile ?: "Untitled",
                    onSaveClick = {
                        selectedFile?.let { fileName ->
                            val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                            if (!docsDir.exists()) docsDir.mkdirs()
                            val filePath = File(docsDir, fileName).absolutePath
                            if (saveFile(filePath, fileContent)) {
                                // Could show a success snackbar here
                            }
                        }
                    },
                    onPreviewClick = { currentSubScreen = SubScreen.PREVIEW },
                    onBackClick = { currentSubScreen = null }
                )
                SubScreen.PREVIEW -> PreviewTopBar(
                    fileName = selectedFile ?: "Untitled",
                    onEditClick = { currentSubScreen = SubScreen.EDITOR },
                    onBackClick = { currentSubScreen = null }
                )
                SubScreen.SETTINGS -> SettingsTopBar(
                    onBackClick = { currentSubScreen = null }
                )
                null -> {
                    when (currentScreen) {
                        Screen.FILES -> FilesTopBar(
                            onSearchClick = { showFileSearch = !showFileSearch },
                            onSortClick = {
                                fileSortBy = when (fileSortBy) {
                                    "name" -> "date"
                                    "date" -> "size"
                                    "size" -> "name"
                                    else -> "name"
                                }
                            },
                            onMoreClick = { currentSubScreen = SubScreen.SETTINGS }
                        )
                        Screen.TODO -> TodoTopBar(
                            onSearchClick = { showTodoSearch = !showTodoSearch },
                            onFilterClick = { showTodoFilter = !showTodoFilter },
                            onMoreClick = { currentSubScreen = SubScreen.SETTINGS }
                        )
                        Screen.QUICKNOTE -> QuickNoteTopBar(
                            onSaveClick = {
                                val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                                if (!docsDir.exists()) docsDir.mkdirs()
                                val filePath = File(docsDir, "quicknote.md").absolutePath
                                if (saveFile(filePath, quickNoteContent)) {
                                    // Could show success message
                                }
                            },
                            onMoreClick = { currentSubScreen = SubScreen.SETTINGS }
                        )
                        Screen.MORE -> MoreTopBar()
                    }
                }
                else -> {} // Exhaustive when
            }
        },
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (currentScreen) {
                        Screen.FILES -> {
                            // Create new file
                            selectedFile = "untitled.txt"
                            fileContent = ""
                            currentSubScreen = SubScreen.EDITOR
                        }
                        Screen.TODO -> {
                            // Add new todo item
                            todoItems = todoItems + "New task"
                        }
                        Screen.QUICKNOTE -> {
                            // Quick note functionality - clear content
                            quickNoteContent = ""
                        }
                        Screen.MORE -> {
                            // More options - navigate to settings
                            currentSubScreen = SubScreen.SETTINGS
                        }
                    }
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (animationsEnabled) {
                AnimatedContent(
                    targetState = currentSubScreen,
                    transitionSpec = {
                        if (targetState != null && initialState == null) {
                            // Entering sub-screen (slide in from right) - enhanced with shared animations
                            ScreenTransitions.slideIn(durationMillis = 600) togetherWith
                            ScreenTransitions.slideOut(durationMillis = 600)
                        } else if (targetState == null && initialState != null) {
                            // Exiting sub-screen (slide back) - enhanced with shared animations
                            ScreenTransitions.slideOut(durationMillis = 600).reversed() togetherWith
                            ScreenTransitions.slideIn(durationMillis = 600).reversed()
                        } else {
                            // Same level transitions or null to null - faster fade
                            ScreenTransitions.fade(durationMillis = 250) togetherWith
                            fadeOut(animationSpec = tween(250))
                        }
                    },
                    label = "SubScreenTransition"
                ) { subScreen ->
                    when (subScreen) {
                        SubScreen.EDITOR -> EditorScreen(
                            fileName = selectedFile ?: "Untitled",
                            content = fileContent,
                            onContentChanged = { fileContent = it },
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
                            onExportClick = {
                                // TODO: Implement actual PDF export using iText or PDFDocument
                                // For now, just save as text file with .pdf extension
                                selectedFile?.let { fileName ->
                                    val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                                    if (!docsDir.exists()) docsDir.mkdirs()
                                    val pdfName = fileName.replace(Regex("\\.[^.]*$"), ".txt") // Save as .txt for now
                                    val filePath = File(docsDir, pdfName).absolutePath
                                    saveFile(filePath, "Exported from Yole\n\n$fileContent")
                                }
                            }
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
                                    // Enhanced tab transitions - smoother with shared animations
                                    if (targetState.ordinal > initialState.ordinal) {
                                        // Swipe left (moving forward)
                                        ScreenTransitions.slideIn(durationMillis = 450) togetherWith
                                        ScreenTransitions.slideOut(durationMillis = 450)
                                    } else {
                                        // Swipe right (moving backward)
                                        ScreenTransitions.slideOut(durationMillis = 450).reversed() togetherWith
                                        ScreenTransitions.slideIn(durationMillis = 450).reversed()
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
                                            selectedFile = file
                                            fileContent = content
                                            currentSubScreen = SubScreen.EDITOR
                                        },
                                        onSettingsClick = { currentSubScreen = SubScreen.SETTINGS }
                                    )
                                    Screen.TODO -> TodoScreen()
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
                        else -> {} // Exhaustive when
                    }
                }
            } else {
                // No animations - direct content switching
                when (currentSubScreen) {
                    SubScreen.EDITOR -> EditorScreen(
                        fileName = selectedFile ?: "Untitled",
                        content = fileContent,
                        onContentChanged = { fileContent = it },
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
                        onExportClick = {
                            // PDF export functionality
                            selectedFile?.let { fileName ->
                                val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
                                if (!docsDir.exists()) docsDir.mkdirs()
                                val pdfName = fileName.replace(Regex("\\.[^.]*$"), ".txt") // Save as .txt for now
                                val filePath = File(docsDir, pdfName).absolutePath
                                saveFile(filePath, "Exported from Yole\n\n$fileContent")
                            }
                        }
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
                                    selectedFile = file
                                    fileContent = content
                                    currentSubScreen = SubScreen.EDITOR
                                },
                                onSettingsClick = { currentSubScreen = SubScreen.SETTINGS }
                            )
                            Screen.TODO -> TodoScreen()
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
                    else -> {} // Exhaustive when
                }
            }
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
                        Text("© 2025 Milos Vasic")
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
                            // TODO: Implement backup functionality
                            showBackupDialog = false
                        }) {
                            Text("Backup Now")
                        }
                        TextButton(onClick = {
                            // TODO: Implement restore functionality
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

/**
 * Empty file list state
 */
@Composable
fun EmptyFileListState(onCreateFile: () -> Unit) {
    EmptyState(
        icon = Icons.Filled.FolderOpen,
        title = "No files yet",
        description = "This folder is empty.\nCreate your first file to get started.",
        actionLabel = "Create File",
        onActionClick = onCreateFile
    )
}

/**
 * Empty search results state
 */
@Composable
fun EmptySearchState(searchQuery: String) {
    EmptyState(
        icon = Icons.Filled.Search,
        title = "No results found",
        description = "No files match \"$searchQuery\".\nTry a different search term."
    )
}

/**
 * Empty todo list state
 */
@Composable
fun EmptyTodoListState() {
    EmptyState(
        icon = Icons.Filled.CheckCircle,
        title = "No tasks yet",
        description = "Add your first task above to get started.\nStay organized and productive!"
    )
}

/**
 * Error state component
 */
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

/**
 * Shimmer skeleton for file cards (loading state)
 */
@Composable
fun FileCardSkeleton() {
    val shimmerProgress = LoadingAnimations.rememberShimmer()
    val shimmerAlpha = 0.3f + (shimmerProgress * 0.3f) // Animate between 0.3 and 0.6

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
                // Icon placeholder
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha),
                            shape = MaterialTheme.shapes.small
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Filename placeholder
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
            // File size placeholder
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
    var currentDirectory by remember { mutableStateOf<File?>(null) }
    var allFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoadingFiles by remember { mutableStateOf(true) }

    // Initialize with documents directory
    LaunchedEffect(Unit) {
        isLoadingFiles = true
        kotlinx.coroutines.delay(300) // Simulate file system access delay
        val docsDir = File(context.getExternalFilesDir(null)?.parentFile, "Documents")
        if (docsDir.exists()) {
            currentDirectory = docsDir
            allFiles = docsDir.listFiles()?.toList() ?: emptyList()
        } else {
            // Fallback to app's private directory
            currentDirectory = context.filesDir
            allFiles = context.filesDir.listFiles()?.toList() ?: emptyList()
        }
        isLoadingFiles = false
    }

    // Filter and sort files
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
            // For now, just show a message - full implementation would require more work
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Search bar
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
                Text("📂 Open Folder")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Current directory path
        currentDirectory?.let { dir ->
            Text(
                text = "📁 ${dir.absolutePath}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // File list with loading state
        LoadingStateWrapper(
            isLoading = isLoadingFiles,
            loadingContent = {
                // Show shimmer skeleton cards while loading
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(5) { // Show 5 skeleton cards
                        FileCardSkeleton()
                    }
                }
            }
        ) {
            // Actual file list or empty state
            if (files.isEmpty()) {
                // Show appropriate empty state
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (searchQuery.isNotEmpty()) {
                        // Empty search results
                        EmptySearchState(searchQuery = searchQuery)
                    } else {
                        // Empty folder
                        EmptyFileListState(onCreateFile = { onFileSelected("untitled.txt", "") })
                    }
                }
            } else {
                // File list with staggered animations
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(
                        items = files,
                        key = { _, file -> file.absolutePath }
                    ) { index, file ->
                        val isDirectory = file.isDirectory
                        val fileName = file.name
                        val fileSize = if (file.isFile) "${file.length()} bytes" else ""

                        // Animated list item with staggered entrance
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = ListAnimations.itemEnter(index),
                            modifier = Modifier.animateItem()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .pressScale(scale = 0.97f), // Add press animation
                            onClick = {
                                if (isDirectory) {
                                    // Navigate into directory with loading state
                                    isLoadingFiles = true
                                    currentDirectory = file
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        kotlinx.coroutines.delay(200) // Brief delay for loading animation
                                        allFiles = file.listFiles()?.toList() ?: emptyList()
                                        isLoadingFiles = false
                                    }
                                } else {
                                    // Try to read file content
                                    try {
                                        val content = file.readText()
                                        onFileSelected(fileName, content)
                                    } catch (e: Exception) {
                                        // If reading fails, show empty content
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
                                Row(horizontalArrangement = Arrangement.Start) {
                                    Text(
                                        text = if (isDirectory) "📁" else "📄",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
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

        // Quick access buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = {
                    // Go up one directory with loading state
                    currentDirectory?.parentFile?.let { parent ->
                        isLoadingFiles = true
                        currentDirectory = parent
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            kotlinx.coroutines.delay(200) // Brief delay for loading animation
                            allFiles = parent.listFiles()?.toList() ?: emptyList()
                            isLoadingFiles = false
                        }
                    }
                },
                enabled = currentDirectory?.parentFile != null && !isLoadingFiles,
                modifier = Modifier.pressScale() // Add press animation
            ) {
                Text("⬆️ Up")
            }

            OutlinedButton(
                onClick = {
                    // Create new file
                    onFileSelected("untitled.txt", "")
                },
                modifier = Modifier.pressScale() // Add press animation
            ) {
                Text("➕ New File")
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
    var text by remember { mutableStateOf(content) }
    val format = remember(fileName) { FormatRegistry.detectByFilename(fileName) }

    // Undo/Redo history
    var history by remember { mutableStateOf(listOf(content)) }
    var historyIndex by remember { mutableStateOf(0) }

    // Update history when text changes
    fun addToHistory(newText: String) {
        // Remove any forward history if we're not at the end
        val newHistory = history.take(historyIndex + 1) + newText
        history = newHistory.takeLast(50) // Keep last 50 changes
        historyIndex = (history.size - 1).coerceAtLeast(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Editing: $fileName",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Format: ${format.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row {
                TextButton(onClick = {
                    onContentChanged(text)
                    onSaveClick()
                }) {
                    Text("💾 Save")
                }
                TextButton(
                    onClick = {
                        if (historyIndex > 0) {
                            historyIndex--
                            text = history[historyIndex]
                            onContentChanged(text)
                        }
                    },
                    enabled = historyIndex > 0
                ) {
                    Text("↶ Undo")
                }
                TextButton(
                    onClick = {
                        if (historyIndex < history.size - 1) {
                            historyIndex++
                            text = history[historyIndex]
                            onContentChanged(text)
                        }
                    },
                    enabled = historyIndex < history.size - 1
                ) {
                    Text("↷ Redo")
                }
            }
        }

        // Action buttons for format-specific actions
        if (format.id == "markdown") {
            MarkdownActionButtons(
                onInsert = { action ->
                    val insertText = when (action) {
                        "bold" -> "**bold text**"
                        "italic" -> "*italic text*"
                        "header" -> "# Header"
                        "link" -> "[link text](url)"
                        "code" -> "`code`"
                        "list" -> "- List item"
                        else -> ""
                    }
                    text += insertText
                    onContentChanged(text)
                }
            )
        }

        // Editor
        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                val oldText = text
                text = newText
                onContentChanged(newText)

                // Add to history on significant changes (word boundaries)
                if (newText.length - oldText.length > 5 || oldText.length - newText.length > 5 ||
                    newText.endsWith(" ") || newText.endsWith("\n")) {
                    addToHistory(newText)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Start typing...") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        )
    }
}

@Composable
fun MarkdownActionButtons(onInsert: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionButton("B", "Bold") { onInsert("bold") }
        ActionButton("I", "Italic") { onInsert("italic") }
        ActionButton("H", "Header") { onInsert("header") }
        ActionButton("🔗", "Link") { onInsert("link") }
        ActionButton("</>", "Code") { onInsert("code") }
        ActionButton("•", "List") { onInsert("list") }
    }
}

@Composable
fun ActionButton(text: String, description: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun PreviewScreen(fileName: String, content: String, onBackClick: () -> Unit = {}, onExportClick: () -> Unit = {}) {
    val context = LocalContext.current
    val format = remember(fileName) { FormatRegistry.detectByFilename(fileName) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
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
                    Text("📤 Export to PDF")
                }
            }
        }

        // Preview content
        val isDarkTheme = isSystemInDarkTheme()
        val htmlContent = remember(content, format, isDarkTheme) {
            generateHtmlPreview(content, format, isDarkTheme)
        }

        // For now, show as text. In a full implementation, this would be a WebView
        Text(
            text = htmlContent,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
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
        else -> "<pre>$content</pre>" // Fallback
    }
}

fun convertMarkdownToHtml(content: String): String {
    // Basic markdown conversion - in a real implementation, this would use a proper parser
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

// Bottom Navigation Bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.List, contentDescription = "Files") },
            label = { Text("Files") },
            selected = currentScreen == Screen.FILES,
            onClick = { onScreenSelected(Screen.FILES) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = "To-Do") },
            label = { Text("To-Do") },
            selected = currentScreen == Screen.TODO,
            onClick = { onScreenSelected(Screen.TODO) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Edit, contentDescription = "QuickNote") },
            label = { Text("QuickNote") },
            selected = currentScreen == Screen.QUICKNOTE,
            onClick = { onScreenSelected(Screen.QUICKNOTE) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Menu, contentDescription = "More") },
            label = { Text("More") },
            selected = currentScreen == Screen.MORE,
            onClick = { onScreenSelected(Screen.MORE) }
        )
    }
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
        title = { Text("Files") },
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
        title = { Text("To-Do") },
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
        title = { Text("QuickNote") },
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
        title = { Text("More") }
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
        title = { Text(fileName, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
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
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text("$fileName (Preview)", maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
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
        title = { Text("Settings") },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
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
fun TodoScreen() {
    var todoItems by remember { mutableStateOf(listOf<TodoItem>()) }
    var newTodoText by remember { mutableStateOf("") }
    var showCompleted by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter/Sort Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "To-Do List",
                style = MaterialTheme.typography.headlineMedium
            )

            Row {
                TextButton(onClick = { showCompleted = !showCompleted }) {
                    Text(if (showCompleted) "Hide Done" else "Show Done")
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
                        todoItems = todoItems + newItem
                        newTodoText = ""
                    }
                },
                modifier = Modifier.pressScale() // Add press animation
            ) {
                Text("Add")
            }
        }

        // Todo list or empty state
        val filteredTodos = todoItems.filter { showCompleted || !it.completed }
        if (filteredTodos.isEmpty() && todoItems.isEmpty()) {
            // No todos at all
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                EmptyTodoListState()
            }
        } else if (filteredTodos.isEmpty()) {
            // All todos are completed and hidden
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All tasks completed! 🎉",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            // Show todo list with staggered animations
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
                                todoItems = todoItems.map {
                                    if (it.id == item.id) it.copy(completed = completed) else it
                                }
                            },
                            onDelete = {
                                todoItems = todoItems.filter { it.id != item.id }
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
            .pressScale(scale = 0.98f) // Add press animation
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

                // Show projects and contexts
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

    Column(modifier = Modifier.fillMaxSize()) {
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
                style = MaterialTheme.typography.headlineMedium
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
            // Preview mode
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
            // Edit mode
            OutlinedTextField(
                value = noteContent,
                onValueChange = {
                    noteContent = it
                    onContentChanged(it)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Start writing your quick note...") }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "More Options",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Settings option
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(scale = 0.98f), // Add press animation
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
                .pressScale(scale = 0.98f), // Add press animation
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
                .pressScale(scale = 0.98f), // Add press animation
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
                .pressScale(scale = 0.98f), // Add press animation
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
                .pressScale(scale = 0.98f), // Add press animation
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