/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Yole iOS Application
 * Native iOS app with Compose Multiplatform
 *
 *########################################################*/

package digital.vasic.yole.ios

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.model.Document
import digital.vasic.yole.model.currentTimeMillis
import kotlinx.coroutines.*
import platform.Foundation.NSUserDefaults

// ---------------------------------------------------------------------------
// App Screen enum
// ---------------------------------------------------------------------------

/**
 * App Screens
 */
enum class AppScreen {
    Documents,
    Editor,
    Settings
}

// ---------------------------------------------------------------------------
// Persistence helpers
// ---------------------------------------------------------------------------

/**
 * Simple document storage backed by NSUserDefaults.
 *
 * Each document is stored as a set of keys under a common prefix so that
 * the list survives app restarts. This is intentionally lightweight -- a
 * production app would use Core Data, the file system, or SQLite, but
 * NSUserDefaults is sufficient for the beta.
 */
object DocumentStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    private const val COUNT_KEY = "yole_doc_count"
    private const val PREFIX = "yole_doc_"

    /**
     * Persist the full document list.
     */
    fun save(documents: List<Document>) {
        defaults.setInteger(documents.size.toLong(), COUNT_KEY)
        documents.forEachIndexed { i, doc ->
            defaults.setObject(doc.id, "${PREFIX}${i}_id")
            defaults.setObject(doc.path, "${PREFIX}${i}_path")
            defaults.setObject(doc.title, "${PREFIX}${i}_title")
            defaults.setObject(doc.extension, "${PREFIX}${i}_ext")
            defaults.setObject(doc.content, "${PREFIX}${i}_content")
            defaults.setObject(doc.format, "${PREFIX}${i}_format")
            defaults.setInteger(doc.modTime, "${PREFIX}${i}_modTime")
        }
        defaults.synchronize()
    }

    /**
     * Load the persisted document list.
     */
    fun load(): List<Document> {
        val count = defaults.integerForKey(COUNT_KEY).toInt()
        if (count <= 0) return emptyList()
        return (0 until count).mapNotNull { i ->
            val id = defaults.stringForKey("${PREFIX}${i}_id") ?: return@mapNotNull null
            val path = defaults.stringForKey("${PREFIX}${i}_path") ?: ""
            val title = defaults.stringForKey("${PREFIX}${i}_title") ?: "Untitled"
            val ext = defaults.stringForKey("${PREFIX}${i}_ext") ?: "txt"
            val content = defaults.stringForKey("${PREFIX}${i}_content") ?: ""
            val format = defaults.stringForKey("${PREFIX}${i}_format") ?: FormatRegistry.ID_PLAINTEXT
            val modTime = defaults.integerForKey("${PREFIX}${i}_modTime")
            Document(
                id = id,
                path = path,
                title = title,
                extension = ext,
                content = content,
                format = format
            ).also { it.modTime = modTime }
        }
    }
}

// ---------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------

/**
 * Settings manager for Yole iOS app
 */
class YoleIOSSettings {
    private val defaults = NSUserDefaults.standardUserDefaults

    // Theme settings
    fun getThemeMode(): String = defaults.stringForKey("theme_mode") ?: "system"
    fun setThemeMode(mode: String) = defaults.setObject(mode, "theme_mode")

    // Editor settings
    fun getShowLineNumbers(): Boolean = defaults.boolForKey("show_line_numbers")
    fun setShowLineNumbers(show: Boolean) = defaults.setBool(show, "show_line_numbers")

    fun getAutoSave(): Boolean = defaults.boolForKey("auto_save")
    fun setAutoSave(auto: Boolean) = defaults.setBool(auto, "auto_save")

    // Font size
    fun getFontSize(): Int {
        val size = defaults.integerForKey("font_size").toInt()
        return if (size > 0) size else 14
    }
    fun setFontSize(size: Int) = defaults.setInteger(size.toLong(), "font_size")

    // Animation settings
    fun getAnimationsEnabled(): Boolean {
        // If the key has never been set, default to true.
        // objectForKey returns null when no value has been stored.
        if (defaults.objectForKey("animations_enabled") == null) return true
        return defaults.boolForKey("animations_enabled")
    }
    fun setAnimationsEnabled(enabled: Boolean) = defaults.setBool(enabled, "animations_enabled")
}

// ---------------------------------------------------------------------------
// Root composable
// ---------------------------------------------------------------------------

/**
 * Main entry point for Yole iOS Application
 */
@Composable
fun YoleIOSApp() {
    MaterialTheme {
        YoleAppContent()
    }
}

/**
 * Main Yole iOS App Content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoleAppContent() {
    val settings = remember { YoleIOSSettings() }

    // Initialise haptic engine once
    LaunchedEffect(Unit) {
        YoleHapticFeedback.initialize()
    }

    var currentScreen by remember { mutableStateOf(AppScreen.Documents) }

    // Document state -- loaded from persistence on first composition
    var documents by remember { mutableStateOf(DocumentStorage.load()) }
    var selectedDocIndex by remember { mutableStateOf(-1) }

    // Helper to persist after any mutation
    fun persistDocuments(docs: List<Document>) {
        documents = docs
        DocumentStorage.save(docs)
    }

    // Load settings
    var themeMode by remember { mutableStateOf(settings.getThemeMode()) }
    var showLineNumbers by remember { mutableStateOf(settings.getShowLineNumbers()) }
    var autoSave by remember { mutableStateOf(settings.getAutoSave()) }
    var fontSize by remember { mutableStateOf(settings.getFontSize()) }
    var animationsEnabled by remember { mutableStateOf(settings.getAnimationsEnabled()) }

    // ---------------------------------------------------------------------------
    // Screen content builder (shared by animated & non-animated paths)
    // ---------------------------------------------------------------------------
    @Composable
    fun ScreenContent(screen: AppScreen) {
        when (screen) {
            AppScreen.Documents -> DocumentsScreen(
                documents = documents,
                onDocumentSelected = { index ->
                    selectedDocIndex = index
                    YoleHapticExtensions.onSelectionChange()
                    currentScreen = AppScreen.Editor
                },
                onCreateDocument = {
                    val now = currentTimeMillis()
                    val docId = "doc_${now}"
                    val newDoc = Document(
                        id = docId,
                        path = "",
                        title = "Untitled",
                        extension = "txt",
                        content = "",
                        format = FormatRegistry.ID_PLAINTEXT
                    ).also { it.modTime = now }
                    val updated = documents + newDoc
                    persistDocuments(updated)
                    selectedDocIndex = updated.lastIndex
                    YoleHapticExtensions.onSuccess()
                    currentScreen = AppScreen.Editor
                },
                onDeleteDocument = { index ->
                    val updated = documents.toMutableList().apply { removeAt(index) }
                    persistDocuments(updated)
                    if (selectedDocIndex == index) selectedDocIndex = -1
                    YoleHapticExtensions.onHeavyAction()
                }
            )

            AppScreen.Editor -> {
                if (selectedDocIndex in documents.indices) {
                    EditorScreen(
                        document = documents[selectedDocIndex],
                        showLineNumbers = showLineNumbers,
                        autoSave = autoSave,
                        fontSize = fontSize,
                        onDocumentChanged = { updatedDoc ->
                            val updated = documents.toMutableList()
                            updated[selectedDocIndex] = updatedDoc
                            persistDocuments(updated)
                        },
                        onSave = {
                            YoleHapticExtensions.onSuccess()
                        }
                    )
                } else {
                    // No document selected -- show placeholder
                    EmptyEditorScreen(
                        onCreateNew = {
                            val now = currentTimeMillis()
                            val docId = "doc_${now}"
                            val newDoc = Document(
                                id = docId,
                                path = "",
                                title = "Untitled",
                                extension = "txt",
                                content = "",
                                format = FormatRegistry.ID_PLAINTEXT
                            ).also { it.modTime = now }
                            val updated = documents + newDoc
                            persistDocuments(updated)
                            selectedDocIndex = updated.lastIndex
                            YoleHapticExtensions.onSuccess()
                        }
                    )
                }
            }

            AppScreen.Settings -> SettingsScreen(
                themeMode = themeMode,
                onThemeModeChanged = {
                    themeMode = it
                    settings.setThemeMode(it)
                    YoleHapticExtensions.onSelectionChange()
                },
                showLineNumbers = showLineNumbers,
                onShowLineNumbersChanged = {
                    showLineNumbers = it
                    settings.setShowLineNumbers(it)
                    YoleHapticExtensions.onSelectionChange()
                },
                autoSave = autoSave,
                onAutoSaveChanged = {
                    autoSave = it
                    settings.setAutoSave(it)
                    YoleHapticExtensions.onSelectionChange()
                },
                fontSize = fontSize,
                onFontSizeChanged = {
                    fontSize = it
                    settings.setFontSize(it)
                },
                animationsEnabled = animationsEnabled,
                onAnimationsEnabledChanged = {
                    animationsEnabled = it
                    settings.setAnimationsEnabled(it)
                    YoleHapticExtensions.onSelectionChange()
                }
            )
        }
    }

    // ---------------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------------
    Scaffold(
        topBar = {
            YoleTopAppBar(
                currentScreen = currentScreen,
                onScreenChange = { screen -> currentScreen = screen }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (animationsEnabled) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
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
                    ScreenContent(screen)
                }
            } else {
                ScreenContent(currentScreen)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Top App Bar
// ---------------------------------------------------------------------------

/**
 * Top App Bar for Yole iOS
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YoleTopAppBar(
    currentScreen: AppScreen,
    onScreenChange: (AppScreen) -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Yole",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        actions = {
            Row {
                IconButton(
                    onClick = { onScreenChange(AppScreen.Documents) },
                    modifier = Modifier
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = "Documents",
                        tint = if (currentScreen == AppScreen.Documents)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = { onScreenChange(AppScreen.Editor) },
                    modifier = Modifier
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editor",
                        tint = if (currentScreen == AppScreen.Editor)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = { onScreenChange(AppScreen.Settings) },
                    modifier = Modifier
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (currentScreen == AppScreen.Settings)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Documents Screen
// ---------------------------------------------------------------------------

/**
 * Documents Screen
 */
@Composable
fun DocumentsScreen(
    documents: List<Document>,
    onDocumentSelected: (Int) -> Unit,
    onCreateDocument: () -> Unit,
    onDeleteDocument: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Text(
            text = "Documents",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        // Create New Document Button
        Button(
            onClick = onCreateDocument,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Document")
        }

        // Documents List
        if (documents.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = "No Documents",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No documents yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Text(
                    text = "Create your first document to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(documents) { index, document ->
                    DocumentItem(
                        document = document,
                        onClick = { onDocumentSelected(index) },
                        onDelete = { onDeleteDocument(index) }
                    )
                }
            }
        }

        // Footer showing supported formats count
        Text(
            text = "Supported formats: ${FormatRegistry.formats.filter { it.extensions.isNotEmpty() }.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Document List Item
 */
@Composable
fun DocumentItem(
    document: Document,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val formatName = remember(document.format) {
        document.getTextFormat().name
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Format badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = formatName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (document.content.isNotEmpty()) {
                            "${document.content.lines().size} lines"
                        } else {
                            "Empty"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Editor Screen
// ---------------------------------------------------------------------------

/**
 * Full-featured Editor Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    document: Document,
    showLineNumbers: Boolean,
    autoSave: Boolean,
    fontSize: Int,
    onDocumentChanged: (Document) -> Unit,
    onSave: () -> Unit
) {
    // Local mutable copies of editable fields
    var title by remember(document.id) { mutableStateOf(document.title) }
    var content by remember(document.id) { mutableStateOf(document.content) }
    var showPreview by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showFormatPicker by remember { mutableStateOf(false) }
    var currentFormatId by remember(document.id) { mutableStateOf(document.format) }

    // Derive format info
    val currentFormat: TextFormat = remember(currentFormatId) {
        FormatRegistry.getById(currentFormatId)
            ?: FormatRegistry.getById(FormatRegistry.ID_PLAINTEXT)!!
    }

    // Detect format from content when content changes significantly
    val detectedFormat: TextFormat? = remember(content) {
        if (content.length > 5) FormatRegistry.detectByContent(content) else null
    }

    // Build the updated Document from local state
    fun buildUpdatedDoc(): Document {
        return document.copy(
            title = title,
            extension = currentFormat.defaultExtension.removePrefix(".").ifEmpty { "txt" },
            content = content,
            format = currentFormatId
        ).also { it.modTime = currentTimeMillis() }
    }

    // Save action
    fun doSave() {
        onDocumentChanged(buildUpdatedDoc())
        hasUnsavedChanges = false
        onSave()
    }

    // Auto-save with debounce
    val autoSaveScope = rememberCoroutineScope()
    var autoSaveJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleAutoSave() {
        if (!autoSave) return
        autoSaveJob?.cancel()
        autoSaveJob = autoSaveScope.launch {
            delay(2000) // 2 second debounce
            doSave()
        }
    }

    // Cleanup auto-save on disposal
    DisposableEffect(document.id) {
        onDispose {
            autoSaveJob?.cancel()
            // Final save if there are unsaved changes
            if (hasUnsavedChanges) {
                onDocumentChanged(buildUpdatedDoc())
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Rename dialog
    // ---------------------------------------------------------------------------
    if (showRenameDialog) {
        var renameText by remember { mutableStateOf(title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Title") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    title = renameText.ifBlank { "Untitled" }
                    hasUnsavedChanges = true
                    scheduleAutoSave()
                    showRenameDialog = false
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ---------------------------------------------------------------------------
    // Format picker dialog
    // ---------------------------------------------------------------------------
    if (showFormatPicker) {
        val textFormats = remember {
            FormatRegistry.formats.filter { it.extensions.isNotEmpty() }
        }
        AlertDialog(
            onDismissRequest = { showFormatPicker = false },
            title = { Text("Select Format") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(textFormats) { fmt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = fmt.id == currentFormatId,
                                onClick = {
                                    currentFormatId = fmt.id
                                    hasUnsavedChanges = true
                                    scheduleAutoSave()
                                    YoleHapticExtensions.onSelectionChange()
                                    showFormatPicker = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = fmt.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = fmt.extensions.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormatPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // ---------------------------------------------------------------------------
    // Main editor layout
    // ---------------------------------------------------------------------------
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Editor toolbar ---
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Title row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title (tappable to rename)
                    TextButton(onClick = { showRenameDialog = true }) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (hasUnsavedChanges) {
                            Text(
                                text = " *",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    // Format badge (tappable to change)
                    TextButton(onClick = { showFormatPicker = true }) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = currentFormat.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Action buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save button
                    FilledTonalButton(
                        onClick = { doSave() },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = hasUnsavedChanges
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", style = MaterialTheme.typography.labelMedium)
                    }

                    // Preview toggle
                    FilledTonalButton(
                        onClick = {
                            showPreview = !showPreview
                            YoleHapticExtensions.onButtonPress()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            if (showPreview) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (showPreview) "Edit" else "Preview",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (showPreview) "Edit" else "Preview",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // Format detection hint
                    if (detectedFormat != null && detectedFormat.id != currentFormatId) {
                        OutlinedButton(
                            onClick = {
                                currentFormatId = detectedFormat.id
                                hasUnsavedChanges = true
                                scheduleAutoSave()
                                YoleHapticExtensions.onSelectionChange()
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "Detected: ${detectedFormat.name}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Word / line count
                    Text(
                        text = "${content.lines().size}L ${content.split(Regex("\\s+")).filter { it.isNotEmpty() }.size}W",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                HorizontalDivider()
            }
        }

        // --- Content area ---
        if (showPreview) {
            PreviewPane(
                content = content,
                format = currentFormat,
                fontSize = fontSize
            )
        } else {
            EditorPane(
                content = content,
                onContentChanged = { newContent ->
                    content = newContent
                    hasUnsavedChanges = true
                    scheduleAutoSave()
                },
                showLineNumbers = showLineNumbers,
                fontSize = fontSize
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Editor Pane (text editing with optional line numbers)
// ---------------------------------------------------------------------------

/**
 * Text editing pane with optional line numbers
 */
@Composable
fun EditorPane(
    content: String,
    onContentChanged: (String) -> Unit,
    showLineNumbers: Boolean,
    fontSize: Int
) {
    val textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
    val lineNumberStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
    )

    val scrollState = rememberScrollState()
    val lines = content.lines()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Line numbers gutter
        if (showLineNumbers) {
            val gutterWidth = remember(lines.size) {
                // Calculate gutter width based on number of digits
                val digits = lines.size.toString().length
                (digits * 10 + 16).dp
            }
            Box(
                modifier = Modifier
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .verticalScroll(scrollState)
                    .padding(end = 8.dp, top = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    lines.forEachIndexed { index, _ ->
                        Text(
                            text = "${index + 1}",
                            style = lineNumberStyle,
                            modifier = Modifier.padding(vertical = 0.dp)
                        )
                    }
                }
            }
        }

        // Editor text field
        BasicTextField(
            value = content,
            onValueChange = onContentChanged,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 8.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)
                .verticalScroll(scrollState),
            decorationBox = { innerTextField ->
                Box {
                    if (content.isEmpty()) {
                        Text(
                            text = "Start typing...",
                            style = textStyle.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Preview Pane (format-aware content preview)
// ---------------------------------------------------------------------------

/**
 * Format-aware preview pane.
 *
 * Attempts to use the ParserRegistry to parse and render content as HTML.
 * Falls back to displaying the raw parsed content or plain text with format
 * metadata if no parser is registered.
 */
@Composable
fun PreviewPane(
    content: String,
    format: TextFormat,
    fontSize: Int
) {
    val previewText = remember(content, format.id) {
        try {
            val parser = ParserRegistry.getParser(format)
            if (parser != null) {
                val parsed = parser.parse(content)
                // Use the parsedContent (structured output) rather than raw HTML
                // since we cannot render HTML in Compose directly
                buildString {
                    appendLine("--- ${format.name} Preview ---")
                    appendLine()
                    if (parsed.metadata.isNotEmpty()) {
                        parsed.metadata.forEach { (key, value) ->
                            appendLine("$key: $value")
                        }
                        appendLine()
                    }
                    append(parsed.parsedContent)
                    if (parsed.errors.isNotEmpty()) {
                        appendLine()
                        appendLine()
                        appendLine("--- Warnings ---")
                        parsed.errors.forEach { appendLine(it) }
                    }
                }
            } else {
                // No parser available -- show content with format header
                buildString {
                    appendLine("--- ${format.name} ---")
                    appendLine("(No parser registered for this format)")
                    appendLine()
                    append(content)
                }
            }
        } catch (e: Exception) {
            // Fallback on any error
            buildString {
                appendLine("--- Preview Error ---")
                appendLine(e.message ?: "Unknown error")
                appendLine()
                append(content)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        // Preview header
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = "Preview",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${format.name} Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Preview content
        val scrollState = rememberScrollState()
        Text(
            text = previewText,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Empty Editor Screen (no document selected)
// ---------------------------------------------------------------------------

/**
 * Placeholder shown when no document is selected in the editor
 */
@Composable
fun EmptyEditorScreen(
    onCreateNew: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Edit,
            contentDescription = "No Document",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No document selected",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select a document from the Documents screen or create a new one",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateNew) {
            Icon(Icons.Default.Add, contentDescription = "Create")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Document")
        }
    }
}

// ---------------------------------------------------------------------------
// Settings Screen
// ---------------------------------------------------------------------------

/**
 * Settings Screen
 */
@Composable
fun SettingsScreen(
    themeMode: String,
    onThemeModeChanged: (String) -> Unit,
    showLineNumbers: Boolean,
    onShowLineNumbersChanged: (Boolean) -> Unit,
    autoSave: Boolean,
    onAutoSaveChanged: (Boolean) -> Unit,
    fontSize: Int,
    onFontSizeChanged: (Int) -> Unit,
    animationsEnabled: Boolean,
    onAnimationsEnabledChanged: (Boolean) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
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

        Spacer(modifier = Modifier.height(16.dp))

        // Font size
        Text(
            text = "Font size: $fontSize sp",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("10", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { onFontSizeChanged(it.toInt()) },
                valueRange = 10f..28f,
                steps = 17,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Text("28", style = MaterialTheme.typography.bodySmall)
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

        // Formats section
        Text(
            text = "Formats",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        val editableFormats = remember {
            FormatRegistry.formats.filter { it.extensions.isNotEmpty() }
        }

        Text(
            text = "Supported formats: ${editableFormats.size}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        editableFormats.take(8).forEach { format ->
            Text(
                text = "${format.name} (${format.extensions.joinToString(", ")})",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        if (editableFormats.size > 8) {
            Text(
                text = "... and ${editableFormats.size - 8} more",
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
            text = "Yole is a cross-platform text editor supporting 17+ markup formats including Markdown, todo.txt, CSV, LaTeX, Org Mode, and more.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Version: 2.15.1 (iOS Beta)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        // Bottom padding for safe area
        Spacer(modifier = Modifier.height(48.dp))
    }
}
