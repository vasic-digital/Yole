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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
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
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.core.content.FileProvider
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.Context
import android.net.Uri
import android.os.Environment
import digital.vasic.opoc.model.GsSharedPreferencesPropertyBackend
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.ParseOptions
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.android.util.FirebaseUtil
import digital.vasic.yole.util.FileHandle
import digital.vasic.yole.util.readBytes
import digital.vasic.yole.util.writeBytes
import digital.vasic.yole.network.common.StorageConfig
import digital.vasic.yole.network.common.StorageType
import digital.vasic.yole.network.config.NetworkStorageConfigService
import digital.vasic.yole.ui.pressScale
import digital.vasic.yole.ui.hoverScale
import digital.vasic.yole.ui.ScreenTransitions
import digital.vasic.yole.ui.ListAnimations
import digital.vasic.yole.ui.LoadingStateWrapper
import digital.vasic.yole.ui.LoadingAnimations
import digital.vasic.yole.android.ui.editor.CompletionPopupState
import digital.vasic.yole.android.ui.editor.CompletionToolbarButton
import digital.vasic.yole.android.ui.editor.codeaction.CodeActionMenu
import digital.vasic.yole.android.ui.editor.diagnostics.DiagnosticsProblemsPanel
import digital.vasic.yole.android.ui.editor.hover.HoverPopup
import digital.vasic.yole.android.ui.editor.navigation.DefinitionLocationChooser
import digital.vasic.yole.android.ui.editor.references.ReferencesPanel
import digital.vasic.yole.android.ui.editor.rename.RenameAction
import digital.vasic.yole.android.ui.editor.signaturehelp.SignatureHelpPill
import digital.vasic.yole.completion.CompletionEngine
import digital.vasic.yole.completion.trigger.CompletionTrigger
import digital.vasic.yole.lsp.CodeAction
import digital.vasic.yole.lsp.DefinitionLocation
import digital.vasic.yole.lsp.DiagnosticsCache
import digital.vasic.yole.lsp.EditorNavigationStack
import digital.vasic.yole.lsp.FindReferencesAction
import digital.vasic.yole.lsp.FormattingTrigger
import digital.vasic.yole.lsp.GoToDefinitionAction
import digital.vasic.yole.lsp.HoverBlock
import digital.vasic.yole.lsp.HoverMarkdownRenderer
import digital.vasic.yole.lsp.LspCodeActionRequester
import digital.vasic.yole.lsp.LspDefinitionRequester
import digital.vasic.yole.lsp.LspFormattingRequester
import digital.vasic.yole.lsp.LspReferencesRequester
import digital.vasic.yole.lsp.LspRenameRequester
import digital.vasic.yole.lsp.LspServerHost
import digital.vasic.yole.lsp.LspServerRegistry
import digital.vasic.yole.lsp.LspSignatureHelpRequester
import digital.vasic.yole.lsp.NavEntry
import digital.vasic.yole.lsp.ReferenceLocation
import digital.vasic.yole.lsp.SignatureHelp
import digital.vasic.yole.lsp.SignatureHelpTrigger
import digital.vasic.yole.lsp.TextEdit
import digital.vasic.yole.lsp.WorkspaceEdit
import digital.vasic.yole.lsp.WorkspaceEditApplier
import digital.vasic.yole.language.LanguageRegistry
import digital.vasic.yole.language.LocalLanguage
import digital.vasic.yole.language.affordance.OutlineExtractor
import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.SyntaxHighlighter
import digital.vasic.yole.syntax.Token
import digital.vasic.yole.syntax.TokenizerEngine
import digital.vasic.yole.syntax.grammar.GrammarRegistry
import digital.vasic.yole.syntax.theme.LocalTheme
import digital.vasic.yole.syntax.theme.themeUiColor
import digital.vasic.yole.android.ui.settings.FormatsSettingsScreen
import digital.vasic.yole.android.ui.settings.FormatsSettingsTopBar
import digital.vasic.yole.android.ui.settings.LspSettingsScreen
import digital.vasic.yole.android.ui.settings.LspSettingsTopBar
import digital.vasic.yole.android.ui.settings.MaybeShowFormatMigrationDialog
import digital.vasic.yole.android.util.PdfExportUtil
import digital.vasic.yole.android.util.BackupRestoreUtil
import digital.vasic.yole.android.ui.import_.ImportButton
import digital.vasic.yole.android.ui.import_.ImportMenuItem
import digital.vasic.yole.android.ui.import_.ImportPreview
import digital.vasic.yole.android.ui.import_.ImportProgressDialog
import digital.vasic.yole.android.ui.import_.ImportShareIntentHandler
import digital.vasic.yole.import_.DocxImporter
import digital.vasic.yole.import_.EpubImporter
import digital.vasic.yole.import_.HtmlImporter
import digital.vasic.yole.import_.ImportedDocument
import digital.vasic.yole.import_.ImporterRegistry
import digital.vasic.yole.import_.OdtImporter
import digital.vasic.yole.import_.PdfImporter
import digital.vasic.yole.import_.RtfImporter
import java.io.File

/**
 * iter-64 Phase 12: Process-singleton bridge for share/open-with Intents.
 *
 * [MainActivity.onNewIntent] writes here; [MainScreen] polls via a
 * [LaunchedEffect]. Declared as an [object] so [MainActivity] can reference
 * it without holding a reference to any Activity or Composable.
 *
 * Thread-safety: reads and writes happen on the main thread only (Activity
 * lifecycle callbacks are always on main; Compose state reads are on the
 * Compose main-thread). @Volatile provides the visibility guarantee for any
 * edge case where a background thread writes first (e.g. deep-link routing
 * from a notification service).
 *
 * Submodules: not touched (CONST-038).
 */
object YoleApp {
    @Volatile var pendingShareBytes: ByteArray? = null
    @Volatile var pendingShareFileName: String = ""
}

// ===== IDE Theme Color Accessors =====
// iter-57 Phase 3b: replaced legacy IdeTheme/YoleColors palette. All UI
// colors are now read from the active VS Code theme via LocalTheme.current
// (wired by ThemeProvider in MainActivity). The accessor functions below
// preserve the same semantic role names that the rest of this file uses;
// they MUST be invoked from a Composable scope.
@Composable private fun ideBackground(): Color = themeUiColor("editor.background")
@Composable private fun ideSurface(): Color = themeUiColor("sideBar.background")
@Composable private fun ideSurfaceVariant(): Color = themeUiColor("tab.inactiveBackground")
@Composable private fun ideBorder(): Color = themeUiColor("editorWidget.border")
@Composable private fun ideAccent(): Color = themeUiColor("focusBorder")
@Composable private fun ideText(): Color = themeUiColor("editor.foreground")
@Composable private fun ideTextSecondary(): Color = themeUiColor("editorLineNumber.foreground")
@Composable private fun ideCurrentLine(): Color = themeUiColor("editor.lineHighlightBackground")
@Composable private fun ideStatusBar(): Color = themeUiColor("focusBorder")
@Composable private fun ideTabActive(): Color = themeUiColor("tab.activeBackground")
@Composable private fun ideTabInactive(): Color = themeUiColor("tab.inactiveBackground")
@Composable private fun ideLineNumbers(): Color = themeUiColor("editorLineNumber.foreground")

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

    // Format toggle settings
    fun getEnabledFormatIds(): Set<String> {
        val raw = getString("enabled_format_ids", "markdown") // Default: only Markdown
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    fun setEnabledFormatIds(ids: Set<String>) = setString("enabled_format_ids", ids.joinToString(","))

    // iter-57 Phase 4.6: format-enablement migration tracking.
    // When the user upgrades from a pre-iter-57 build (where every format
    // was enabled by default) to an iter-57+ build (where ONLY Markdown is
    // enabled by default, per spec §3.7), we show a one-time dialog asking
    // whether to keep their prior set or adopt the new default. These two
    // flags record (1) whether the choice has been made and (2) the snapshot
    // of the prior enabled set so "Keep mine" can restore it.
    fun getFormatEnablementMigrationChoiceMade(): Boolean =
        getBool("format_enablement_migration_choice_made", false)
    fun setFormatEnablementMigrationChoiceMade(made: Boolean) =
        setBool("format_enablement_migration_choice_made", made)

    fun getPriorEnabledFormatIds(): Set<String> {
        val raw = getString("prior_enabled_format_ids", "")
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    fun setPriorEnabledFormatIds(ids: Set<String>) =
        setString("prior_enabled_format_ids", ids.joinToString(","))

    // iter-74 (#iter-63-format-on-save-settings-toggle): LSP format-on-save toggle.
    // Default is TRUE so users benefit automatically when an LSP server is available.
    // Persisted under key "lsp_format_on_save" (matches PREF_KEY_LSP_FORMAT_ON_SAVE).
    fun getLspFormatOnSave(): Boolean = getBool("lsp_format_on_save", true)
    fun setLspFormatOnSave(enabled: Boolean) = setBool("lsp_format_on_save", enabled)
}

/**
 * Save content to a file using FileHandle (SAF for Android, File for Desktop).
 * Returns Pair(saved, newUri) where newUri is non-null only when a new SAF URI was obtained.
 */
fun saveFile(context: Context, contentUri: String?, content: String, fileName: String): Pair<Boolean, String?> {
    val trace = FirebaseUtil.startTrace(FirebaseUtil.Traces.FILE_SAVE)
    return try {
        val (ok, newUri) = if (contentUri != null) {
            val handle = FileHandle(contentUri)
            val writeOk = handle.writeBytes(content.toByteArray())
            if (writeOk) Pair(true, contentUri) else Pair(false, null)
        } else {
            val cacheDir = File(context.filesDir, "autosave")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val cacheFile = File(cacheDir, fileName)
            cacheFile.writeText(content)
            Pair(true, null)
        }
        if (ok) {
            FirebaseUtil.logEvent(
                FirebaseUtil.Events.FILE_SAVED,
                mapOf(
                    FirebaseUtil.Params.FILE_FORMAT to (fileName.substringAfterLast('.', "unknown")),
                    FirebaseUtil.Params.FILE_SIZE to content.length.toString()
                )
            )
        }
        Pair(ok, newUri)
    } catch (e: Exception) {
        FirebaseUtil.recordNonFatal(e, "saveFile failed for ${fileName.substringAfterLast('/', fileName)}")
        FirebaseUtil.logEvent(
            FirebaseUtil.Events.ERROR_OCCURRED,
            mapOf(
                FirebaseUtil.Params.ERROR_TYPE to "save_failed",
                FirebaseUtil.Params.ERROR_MESSAGE to (e.message ?: e.javaClass.simpleName)
            )
        )
        Pair(false, null)
    } finally {
        FirebaseUtil.stopTrace(trace)
    }
}

/**
 * Create a new file using SAF (for Android 11+ compatibility)
 */
fun createFileWithSAF(context: Context, parentUri: Uri, fileName: String, content: String): Boolean {
    return try {
        val parentDoc = DocumentFile.fromTreeUri(context, parentUri)
        if (parentDoc != null && parentDoc.isDirectory) {
            val existingFile = parentDoc.findFile(fileName)
            val isNew = existingFile == null
            if (existingFile != null) {
                context.contentResolver.openOutputStream(existingFile.uri, "wt")?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
            } else {
                val newFile = parentDoc.createFile("text/plain", fileName)
                if (newFile != null) {
                    context.contentResolver.openOutputStream(newFile.uri, "wt")?.use { outputStream ->
                        outputStream.write(content.toByteArray())
                    }
                }
            }
            FirebaseUtil.logEvent(
                if (isNew) FirebaseUtil.Events.FILE_CREATED else FirebaseUtil.Events.FILE_SAVED,
                mapOf(
                    FirebaseUtil.Params.FILE_FORMAT to fileName.substringAfterLast('.', "unknown"),
                    FirebaseUtil.Params.FILE_SIZE to content.length.toString()
                )
            )
            true
        } else {
            false
        }
    } catch (e: Exception) {
        FirebaseUtil.recordNonFatal(e, "createFileWithSAF failed for $fileName")
        FirebaseUtil.logEvent(
            FirebaseUtil.Events.ERROR_OCCURRED,
            mapOf(
                FirebaseUtil.Params.ERROR_TYPE to "saf_create_failed",
                FirebaseUtil.Params.ERROR_MESSAGE to (e.message ?: e.javaClass.simpleName)
            )
        )
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
    val isDirty: Boolean = false,
    val contentUri: String? = null
)

enum class Screen {
    FILES,
    TODO,
    QUICKNOTE,
    MORE
}

enum class SubScreen {
    // FILE_BROWSER removed in iter-55: the FILES bottom-nav tab
    // (Screen.FILES → FilesScreen → FileBrowserScreen) is the
    // canonical file-browsing surface. The duplicate SubScreen entry
    // confused users and was reachable from both MoreScreen and the
    // editor's "open file" action; both now route to the FILES tab.
    EDITOR,
    PREVIEW,
    SETTINGS,
    // iter-57 Phase 4.5: Settings → Formats sub-screen (Markdown-default
    // operator constraint UI per spec §3.7).
    FORMATS_SETTINGS,
    // iter-61 Phase 8: Settings → LSP Language Support sub-screen.
    // v1: Android shows honest "not available" stub for all 15 LSP servers.
    LSP_SETTINGS
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

    // ── iter-64 Phase 12: ImporterRegistry + import flow state ──────────────
    // ImporterRegistry is constructed once per MainScreen composition.
    // All 6 importer instances are created eagerly; construction is cheap
    // (no I/O). The registry is passed by lambda capture to avoid a
    // CompositionLocal (the rest of the app does not need it).
    val importerRegistry = remember {
        ImporterRegistry.default(
            listOf(
                DocxImporter(),
                HtmlImporter(),
                RtfImporter(),
                OdtImporter(),
                PdfImporter(),
                EpubImporter(),
            )
        )
    }

    // State for the in-flight import: bytes + filename resolved from the file picker.
    var importPendingBytes by remember { mutableStateOf<ByteArray?>(null) }
    var importPendingFileName by remember { mutableStateOf("") }

    // Progress dialog visibility and result/preview state.
    var showImportProgress by remember { mutableStateOf(false) }
    var importedDoc by remember { mutableStateOf<ImportedDocument?>(null) }

    // File picker for the import flow (any MIME type because all 6 formats are
    // supported; "*/*" lets the user choose among them).
    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (_: Exception) { null }
            if (bytes == null) {
                Toast.makeText(context, "Cannot read selected file", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "imported"
            importPendingBytes = bytes
            importPendingFileName = name
            showImportProgress = true
            val ext = name.substringAfterLast('.', "")
            val importer = importerRegistry.forExtension(ext)
            if (importer == null) {
                showImportProgress = false
                Toast.makeText(context, "No importer for .$ext files", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val result = importer.import(bytes, name)
            showImportProgress = false
            result.onSuccess { doc ->
                importedDoc = doc
            }.onFailure { err ->
                Toast.makeText(context, "Import failed: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Share-intent import: MainActivity.onNewIntent writes to YoleApp singleton;
    // we poll it on resume via a LaunchedEffect loop. The loop runs on the
    // Compose coroutine dispatcher (main thread) so no synchronisation needed
    // beyond the @Volatile guarantee on the singleton fields.
    LaunchedEffect(Unit) {
        while (true) {
            val bytes = YoleApp.pendingShareBytes
            if (bytes != null) {
                val name = YoleApp.pendingShareFileName.ifBlank { "shared" }
                YoleApp.pendingShareBytes = null
                YoleApp.pendingShareFileName = ""
                importPendingBytes = bytes
                importPendingFileName = name
                showImportProgress = true
                val ext = name.substringAfterLast('.', "")
                val importer = importerRegistry.forExtension(ext)
                if (importer == null) {
                    showImportProgress = false
                    Toast.makeText(context, "No importer for .$ext files", Toast.LENGTH_SHORT).show()
                } else {
                    val result = importer.import(bytes, name)
                    showImportProgress = false
                    result.onSuccess { doc -> importedDoc = doc }
                        .onFailure { err ->
                            Toast.makeText(
                                context,
                                "Import failed: ${err.message}",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                }
            }
            delay(300L) // poll interval
        }
    }

    // Load settings
    var themeMode by remember { mutableStateOf(settings.getThemeMode()) }
    var showLineNumbers by remember { mutableStateOf(settings.getShowLineNumbers()) }
    var autoSave by remember { mutableStateOf(settings.getAutoSave()) }
    var animationsEnabled by remember { mutableStateOf(settings.getAnimationsEnabled()) }
    var enabledFormatIds by remember {
        mutableStateOf(settings.getEnabledFormatIds().also { FormatRegistry.setEnabledFormatIds(it) })
    }

    // IDE theme colors
    val bg = ideBackground()
    val surface = ideSurface()
    val border = ideBorder()
    val accent = ideAccent()
    val textColor = ideText()
    val textSecondary = ideTextSecondary()
    val statusBarBg = ideStatusBar()
    val tabActive = ideTabActive()
    val tabInactive = ideTabInactive()
    val lineNumColor = ideLineNumbers()

    // Helper: open file in a new tab or switch to existing tab
    fun openFileInTab(fileName: String, content: String, contentUri: String? = null) {
        val trace = FirebaseUtil.startTrace(FirebaseUtil.Traces.FILE_OPEN)
        try {
            val existingIndex = openTabs.indexOfFirst { it.fileName == fileName }
            if (existingIndex >= 0) {
                activeTabIndex = existingIndex
                fileContent = openTabs[existingIndex].content
            } else {
                openTabs = openTabs + EditorTab(
                    fileName = fileName,
                    content = content,
                    contentUri = contentUri
                )
                activeTabIndex = openTabs.size // will be the new last index
                fileContent = content
            }
            selectedFile = fileName
            currentSubScreen = SubScreen.EDITOR
            FirebaseUtil.logEvent(
                FirebaseUtil.Events.FILE_OPENED,
                mapOf(
                    FirebaseUtil.Params.FILE_FORMAT to fileName.substringAfterLast('.', "unknown"),
                    FirebaseUtil.Params.FILE_SIZE to content.length.toString()
                )
            )
        } finally {
            FirebaseUtil.stopTrace(trace)
        }
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
            FirebaseUtil.recordNonFatal(e, "SecureStorageFactory.initialize failed")
        }
        try {
            digital.vasic.yole.format.ParserInitializer.registerAllParsersLazy()
        } catch (e: Exception) {
            android.util.Log.e("YoleApp", "Failed to initialize parsers", e)
            FirebaseUtil.recordNonFatal(e, "ParserInitializer.registerAllParsersLazy failed")
        }
        try {
            PdfExportUtil.cleanupOldPdfs(context)
            BackupRestoreUtil.cleanupOldBackups(context)
        } catch (e: Exception) {
            android.util.Log.e("YoleApp", "Failed to cleanup old files", e)
            FirebaseUtil.recordNonFatal(e, "PDF/backup cleanup failed")
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
                        // iter-55 dedup: route editor "open file" to the
                        // canonical FILES bottom-nav tab (the duplicate
                        // SubScreen.FILE_BROWSER was removed).
                        currentSubScreen = null
                        currentScreen = Screen.FILES
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
                            // iter-55 dedup: route Ctrl+O to the FILES tab.
                            currentSubScreen = null
                            currentScreen = Screen.FILES
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
                                    val currentTab = openTabs.getOrNull(activeTabIndex)
                                    val (saved, newUri) = saveFile(context, currentTab?.contentUri, fileContent, currentTab?.fileName ?: fileName)
                                    if (saved) {
                                        if (newUri != null && currentTab?.contentUri != newUri) {
                                            openTabs = openTabs.mapIndexed { index, tab ->
                                                if (index == activeTabIndex) tab.copy(contentUri = newUri, isDirty = false)
                                                else tab
                                            }
                                        } else {
                                            openTabs = openTabs.mapIndexed { index, tab ->
                                                if (index == activeTabIndex) tab.copy(isDirty = false) else tab
                                            }
                                        }
                                        if (settings.getAnnounceChanges()) {
                                            Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show()
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
                        SubScreen.FORMATS_SETTINGS -> FormatsSettingsTopBar(
                            onBackClick = { currentSubScreen = SubScreen.SETTINGS }
                        )
                        SubScreen.LSP_SETTINGS -> LspSettingsTopBar(
                            onBackClick = { currentSubScreen = SubScreen.SETTINGS }
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
                                onMoreClick = { currentSubScreen = SubScreen.SETTINGS },
                                // iter-64 Phase 12: expose import on the FILES tab.
                                onImportClick = if (currentScreen == Screen.FILES) {
                                    { importFilePicker.launch(arrayOf("*/*")) }
                                } else null,
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
                                        saveFile(context, null, fileContent, fileName)
                                    }
                                },
                                onNavigateToFile = { uri, _ ->
                                    // #iter-62-phase-8-cross-file-back-nav: strip "file://"
                                    // prefix, read the file, and open it in a tab.
                                    val path = uri.removePrefix("file://")
                                    val prevContent = java.io.File(path).takeIf { it.exists() }
                                        ?.readText() ?: ""
                                    openFileInTab(path, prevContent, uri)
                                },
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
                                },
                                enabledFormatIds = enabledFormatIds,
                                onFormatToggled = { id, enabled ->
                                    val newIds = if (enabled) {
                                        enabledFormatIds + id
                                    } else {
                                        enabledFormatIds - id
                                    }
                                    enabledFormatIds = newIds
                                    settings.setEnabledFormatIds(newIds)
                                    if (enabled) FormatRegistry.setFormatEnabled(id)
                                    else FormatRegistry.setFormatDisabled(id)
                                },
                                onOpenFormatsClick = {
                                    currentSubScreen = SubScreen.FORMATS_SETTINGS
                                },
                                onOpenLspSettingsClick = {
                                    currentSubScreen = SubScreen.LSP_SETTINGS
                                }
                            )
                            SubScreen.FORMATS_SETTINGS -> FormatsSettingsScreen(
                                settings = settings,
                                onBackClick = { currentSubScreen = SubScreen.SETTINGS }
                            )
                            SubScreen.LSP_SETTINGS -> LspSettingsScreen(
                                onBackClick = { currentSubScreen = SubScreen.SETTINGS }
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
                                            onFileSelected = { file, content, safUri, filePath ->
                                                openFileInTab(file, content, safUri?.toString() ?: filePath)
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
                                                saveFile(context, null, quickNoteContent, "quicknote.md")
                                            }
                                        )
                                        Screen.MORE -> MoreScreen(
                                            onSettingsClick = { currentSubScreen = SubScreen.SETTINGS },
                                            onSearchClick = { showFileSearch = true },
                                            onBackupClick = { showBackupDialog = true },
                                            onAboutClick = { showAboutDialog = true }
                                        )
                                    }
                                }
                            }
                            // iter-55 dedup: SubScreen.FILE_BROWSER render branch removed.
                            // FILES bottom-nav tab is the canonical file browser entry.
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
                                    saveFile(context, null, fileContent, fileName)
                                }
                            },
                            onNavigateToFile = { uri, _ ->
                                val path = uri.removePrefix("file://")
                                val prevContent = java.io.File(path).takeIf { it.exists() }
                                    ?.readText() ?: ""
                                openFileInTab(path, prevContent, uri)
                            },
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
                            },
                            enabledFormatIds = enabledFormatIds,
                            onFormatToggled = { id, enabled ->
                                val newIds = if (enabled) {
                                    enabledFormatIds + id
                                } else {
                                    enabledFormatIds - id
                                }
                                enabledFormatIds = newIds
                                settings.setEnabledFormatIds(newIds)
                                if (enabled) FormatRegistry.setFormatEnabled(id)
                                else FormatRegistry.setFormatDisabled(id)
                            },
                            onOpenFormatsClick = {
                                currentSubScreen = SubScreen.FORMATS_SETTINGS
                            },
                            onOpenLspSettingsClick = {
                                currentSubScreen = SubScreen.LSP_SETTINGS
                            }
                        )
                        SubScreen.FORMATS_SETTINGS -> FormatsSettingsScreen(
                            settings = settings,
                            onBackClick = { currentSubScreen = SubScreen.SETTINGS }
                        )
                        SubScreen.LSP_SETTINGS -> LspSettingsScreen(
                            onBackClick = { currentSubScreen = SubScreen.SETTINGS }
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
                                    onFileSelected = { file, content, safUri, filePath ->
                                        openFileInTab(file, content, safUri?.toString() ?: filePath)
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
                                        saveFile(context, null, quickNoteContent, "quicknote.md")
                                    }
                                )
                                Screen.MORE -> MoreScreen(
                                    onSettingsClick = { currentSubScreen = SubScreen.SETTINGS },
                                    onSearchClick = {
                                        currentScreen = Screen.FILES
                                        showFileSearch = true
                                    },
                                    onBackupClick = { showBackupDialog = true },
                                    onAboutClick = { showAboutDialog = true }
                                )
                            }
                        }
                        // iter-55 dedup: SubScreen.FILE_BROWSER render branch removed
                        // (second occurrence). Canonical entry is the FILES tab.
                        else -> {}
                    }
                }
            }

            // ===== DIALOGS =====

            // iter-57 Phase 4.6: one-time format-enablement migration dialog.
            // Surfaces on the first launch after upgrading from a pre-iter-57
            // build (where every format was enabled by default) to iter-57+
            // (where ONLY Markdown is enabled by default — spec §3.7). The
            // Composable renders nothing on the steady-state path; the
            // trigger check + LaunchedEffect inside short-circuits when
            // the migration choice has already been recorded.
            MaybeShowFormatMigrationDialog(settings)

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
                val ctx = context
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                val copyrightAnnotated = buildAnnotatedString {
                    append("© $currentYear ")
                    pushStringAnnotation("URL", "https://www.milosvasic.ru/")
                    withStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )) {
                        append("Milos Vasic")
                    }
                    pop()
                    append("\nApache-2.0 License")
                }
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    title = { Text("About Yole") },
                    text = {
                        Column {
                            Text("Yole - Universal Text Editor")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Version: 1.0.0")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Platforms: Android, Desktop, iOS, Web")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Supports 17+ text formats including Markdown, LaTeX, CSV, and more.")
                            Spacer(modifier = Modifier.height(8.dp))
                            ClickableText(
                                text = copyrightAnnotated,
                                style = MaterialTheme.typography.bodyMedium
                            ) { offset ->
                                copyrightAnnotated.getStringAnnotations("URL", offset, offset)
                                    .firstOrNull()?.let {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it.item))
                                        ctx.startActivity(intent)
                                    }
                            }
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

            // ── iter-64 Phase 12: Import flow dialogs ───────────────────────
            // ImportProgressDialog: shown while the importer coroutine runs.
            if (showImportProgress) {
                ImportProgressDialog(
                    fileName = importPendingFileName,
                    onCancel = { showImportProgress = false },
                )
            }

            // ImportPreview: shown when the importer has produced a result.
            val doc = importedDoc
            if (doc != null) {
                val suggestedName = importPendingFileName
                    .substringBeforeLast('.').ifBlank { "imported" } + ".md"
                ImportPreview(
                    doc = doc,
                    suggestedFileName = suggestedName,
                    onSave = { filename ->
                        openFileInTab(filename, doc.markdown)
                        importedDoc = null
                    },
                    onCancel = { importedDoc = null },
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
    onMoreClick: () -> Unit,
    // iter-64 Phase 12: import button shown on the FILES tab only.
    // null = do not show the button (other tabs, sub-screens, etc.)
    onImportClick: (() -> Unit)? = null,
) {
    val bg = ideSurface()
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
            // ImportButton: shown on the FILES tab so users can import documents.
            if (onImportClick != null) {
                ImportButton(onImportRequest = onImportClick)
            }
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.semantics { contentDescription = "Search" }
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            }
            // MoreVert overflow — contains ImportMenuItem as the first item.
            var showMoreDropdown by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showMoreDropdown = true },
                    modifier = Modifier.semantics { contentDescription = "Settings" }
                ) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMoreDropdown,
                    onDismissRequest = { showMoreDropdown = false },
                ) {
                    // Import from... entry always visible in the overflow menu.
                    if (onImportClick != null) {
                        ImportMenuItem(
                            onImportRequest = {
                                showMoreDropdown = false
                                onImportClick()
                            },
                        )
                        HorizontalDivider()
                    }
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            showMoreDropdown = false
                            onMoreClick()
                        },
                    )
                }
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
    val bg = ideSurface()
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
    val surfaceVar = ideSurfaceVariant()

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
    onSaveClick: () -> Unit = {},
    /**
     * #iter-62-phase-8-cross-file-back-nav:
     * Called when back-navigation pops a NavEntry whose URI refers to a
     * different file than the one currently open.  The caller (MainScreen /
     * IdeMainScreen) should open the file at [uri] and position the cursor
     * at [cursorOffset].
     */
    onNavigateToFile: (uri: String, cursorOffset: Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val settings = remember { YoleSettings(context) }
    var text by remember(content) { mutableStateOf(content) }
    val format = remember(fileName) { FormatRegistry.detectByFilename(fileName) }
    val bg = ideBackground()
    val textColor = ideText()
    val lineNumColor = ideLineNumbers()
    val borderColor = ideBorder()

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

    // iter-58 Feature 2 Phase 5: outline drawer toggle. Tapping the
    // "Outline" toolbar icon flips this and overlays the OutlineDrawer
    // on the left edge of the editor surface.
    var outlineDrawerOpen by remember { mutableStateOf(false) }

    // iter-60 Phase 6.5: completion engine + trigger + popup state.
    // Hoisted here so the toolbar button can call trigger.onExplicitTrigger
    // independently of the SyncedScrollEditor's onPreviewKeyEvent. The
    // engine is re-built when detectedLangId changes (different file).
    // CompletionPopupState is a plain class with Compose-observable
    // fields — no need to wrap in remember {} (it is stable).
    val completionPopupState = remember { CompletionPopupState() }
    // The coroutine scope for the trigger is remembered so it matches
    // the composable's lifecycle; it is cancelled when IdeEditorScreen
    // leaves the composition.
    val completionScope = rememberCoroutineScope()

    // Hoist lang detection + engine + trigger here (above the Column) so
    // both the toolbar button and the SyncedScrollEditor share the same
    // trigger instance without needing to thread it through composable args.
    val detectedLangId = remember(fileName) { GrammarRegistry.detectLangId(fileName) }
    val tokenizerEngine = remember { TokenizerEngine() }
    LaunchedEffect(tokenizerEngine) {
        tokenizerEngine.initialize()
    }
    // iter-61 Phase 6.4: one LspServerHost per editor session, lifetime-tied
    // to IdeEditorScreen composition. The host is created once (not keyed on
    // detectedLangId) so state is preserved across document switches inside
    // the same screen instance.
    val lspHost = remember { LspServerHost(LspServerRegistry.default()) }
    DisposableEffect(lspHost) {
        onDispose {
            completionScope.launch {
                try { lspHost.shutdownAll() } catch (_: Throwable) {}
            }
        }
    }
    val passedLangId = if (detectedLangId != "plaintext") detectedLangId else null
    val completionEngine = remember(detectedLangId, lspHost) {
        val extractor = OutlineExtractor()
        CompletionEngine.default(extractor, tokenizerEngine, lspHost)
    }
    val completionTrigger = remember(completionEngine) {
        CompletionTrigger(
            langId = passedLangId,
            scope = completionScope,
        )
    }

    // ── iter-62 Phase 8: LSP diagnostics observer ────────────────────────
    // Subscribe to DiagnosticsCache.states (a StateFlow) and filter to the
    // currently-open file URI. The URI convention mirrors what LSP servers
    // emit: "file://<absolutePath>". For files without an absolute path
    // (e.g. unsaved buffers) we use the raw fileName so the filter can
    // still match when the server publishes to that key.
    val diagnosticsByUri by lspHost.diagnosticsCache.states.collectAsState()
    val currentFileUri = remember(fileName) { "file://$fileName" }
    val currentFileDiagnostics = remember(diagnosticsByUri, currentFileUri, fileName) {
        diagnosticsByUri[currentFileUri].orEmpty()
            .ifEmpty { diagnosticsByUri[fileName].orEmpty() }
    }

    // ── iter-62 Phase 8: problems panel toggle ───────────────────────────
    var isProblemsPanelOpen by remember { mutableStateOf(false) }

    // ── iter-62 Phase 8: hover state ─────────────────────────────────────
    var hoverBlocks by remember { mutableStateOf<List<HoverBlock>>(emptyList()) }
    // iter-74 (#iter-62-phase-8-hover-precise-anchor): replaced always-(0,0) stub.
    // The cursor rect is written back by SyncedScrollEditor.onCursorRectChanged
    // via BasicTextField.onTextLayout → TextLayoutResult.getCursorRect(cursorOffset).
    // The HoverPopup is positioned at (cursorRect.left.toInt(), cursorRect.bottom.toInt())
    // which places it just below the cursor in the BTF's local coordinate space.
    var hoverPopupAnchor by remember { mutableStateOf(androidx.compose.ui.unit.IntOffset.Zero) }

    // ── iter-62 Phase 8: go-to-definition state ──────────────────────────
    val navStack = remember { EditorNavigationStack() }
    var chooserLocations by remember { mutableStateOf<List<DefinitionLocation>>(emptyList()) }

    // ── iter-62 Phase 8: back handler (navigate back from def-jump) ──────
    // #iter-62-phase-8-cross-file-back-nav: if the popped entry refers to a
    // different file, delegate to onNavigateToFile so the caller can open it.
    BackHandler(enabled = navStack.canGoBack()) {
        val entry = navStack.pop()
        if (entry != null) {
            if (entry.uri != currentFileUri && entry.uri.isNotBlank()) {
                // Cross-file: ask the host screen to open the previous file.
                // The host uses openFileInTab which either switches to an
                // existing tab or opens a new one.
                onNavigateToFile(entry.uri, entry.cursorOffset)
            } else {
                // Intra-file: trigger a no-op content change so the editor
                // re-renders at the restored cursor offset (full intra-file
                // cursor restoration requires SyncedScrollEditor integration;
                // the push already captures cursorOffset).
                onContentChanged(text)
            }
        }
    }

    // ── iter-63 Phase 10: rename wiring ──────────────────────────────────
    // showRenameAction is raised by F2 shortcut or long-press "Rename" menu.
    var showRenameAction by remember { mutableStateOf(false) }
    // LspRenameRequester adapter that wraps LspServerHost.rename().
    val lspRenameRequester = remember(lspHost) {
        object : LspRenameRequester {
            override suspend fun rename(
                langId: String,
                uri: String,
                line: Int,
                character: Int,
                newName: String,
            ): WorkspaceEdit? = lspHost.rename(langId, uri, line, character, newName)
        }
    }

    // ── iter-63 Phase 10: code-action lightbulb polling ──────────────────
    // Populated by the 500ms polling loop below. Passed to SyncedScrollEditor
    // so CodeActionLightbulb renders in the 2nd gutter column (between
    // diagnostic dot and fold chevron).
    var actionsByLine by remember { mutableStateOf<Map<Int, List<CodeAction>>>(emptyMap()) }
    // Code-action menu state: which line is the lightbulb-tap for.
    var codeActionMenuLine by remember { mutableStateOf<Int?>(null) }
    // LspCodeActionRequester adapter.
    val lspCodeActionRequester = remember(lspHost) {
        object : LspCodeActionRequester {
            override suspend fun codeActions(
                langId: String,
                uri: String,
                range: IntRange,
            ): List<CodeAction> = lspHost.codeActions(langId, uri, range)
        }
    }
    // 500ms polling loop for code actions (per Phase 10.2 spec).
    // #iter-63-server-trigger-chars-hardcoded — server-capability query is a
    // future improvement; Phase 10 v1 polls for visible-range actions directly.
    if (passedLangId != null) {
        LaunchedEffect(passedLangId, currentFileUri) {
            while (true) {
                delay(500L)
                try {
                    val textNow = text
                    val lineCount = textNow.lines().size
                    // Poll the full document range as a single request for Phase 10 v1.
                    val actions = lspCodeActionRequester.codeActions(
                        langId = passedLangId,
                        uri = currentFileUri,
                        range = 0 until textNow.length.coerceAtLeast(1),
                    )
                    // Distribute actions to their respective lines by evenly assigning
                    // to line 0 for Phase 10 v1 (server-side line mapping is a future
                    // improvement tracked as #iter-63-code-action-per-line-mapping).
                    actionsByLine = if (actions.isNotEmpty()) {
                        (0 until lineCount.coerceAtLeast(1)).associateWith { _ -> actions }
                    } else {
                        emptyMap()
                    }
                } catch (_: Throwable) {
                    // Degraded — keep previous actionsByLine; server may be unavailable.
                }
            }
        }
    }

    // ── iter-63 Phase 10: signature help ─────────────────────────────────
    // Current SignatureHelp result; null = no signature popup shown.
    var currentSignatureHelp by remember { mutableStateOf<SignatureHelp?>(null) }
    // LspSignatureHelpRequester adapter.
    val lspSignatureHelpRequester = remember(lspHost) {
        object : LspSignatureHelpRequester {
            override suspend fun signatureHelp(
                langId: String,
                uri: String,
                line: Int,
                character: Int,
            ): SignatureHelp? = lspHost.signatureHelp(langId, uri, line, character)
        }
    }
    // SignatureHelpTrigger — feeds keystrokes from onTextChanged.
    val signatureHelpTrigger = if (passedLangId != null) {
        remember(lspHost, passedLangId) {
            SignatureHelpTrigger(
                scope = completionScope,
                requester = lspSignatureHelpRequester,
                onResult = { sig -> currentSignatureHelp = sig },
            )
        }
    } else {
        null
    }

    // ── iter-63 Phase 10: formatting trigger ─────────────────────────────
    // LspFormattingRequester adapter.
    val lspFormattingRequester = remember(lspHost) {
        object : LspFormattingRequester {
            override suspend fun formatting(
                langId: String,
                uri: String,
                indentSize: Int,
                useSpaces: Boolean,
            ): List<TextEdit> = lspHost.formatting(langId, uri, indentSize, useSpaces)
        }
    }
    // iter-74 (#iter-63-server-trigger-chars-hardcoded): on-type trigger chars.
    // Query server capabilities first; fall back to the conservative default set
    // when the server has not yet initialised or returns no on-type spec.
    // LspServerHost.getOnTypeTriggerChars() returns null until the server is Ready;
    // we refresh whenever the server state map changes via the states SharedFlow.
    var onTypeTriggerChars by remember(passedLangId) {
        mutableStateOf<Set<Char>>(setOf(';', '}', '\n'))
    }
    LaunchedEffect(lspHost, passedLangId) {
        lspHost.states.collect { stateMap ->
            val ready = passedLangId != null && stateMap[passedLangId]?.let {
                it is digital.vasic.yole.lsp.ServerState.Ready
            } == true
            if (ready && passedLangId != null) {
                val caps = lspHost.getOnTypeTriggerChars(passedLangId)
                if (caps != null && caps.isNotEmpty()) {
                    onTypeTriggerChars = caps
                }
            }
        }
    }
    val formattingTrigger = if (passedLangId != null) {
        remember(lspHost, passedLangId) {
            FormattingTrigger(
                formatter = lspFormattingRequester,
                onTypeFormatter = { langId, uri, line, character, triggerChar ->
                    lspHost.onTypeFormatting(langId, uri, line, character, triggerChar)
                },
                // iter-74 (#iter-63-format-on-save-settings-toggle): read from real
                // SharedPreferences key instead of hardcoded false.
                settings = { settings.getLspFormatOnSave() },
            )
        }
    } else {
        null
    }

    // ── iter-63 Phase 10: find-references wiring ──────────────────────────
    var showReferencesPanel by remember { mutableStateOf(false) }
    var referencesList by remember { mutableStateOf<List<ReferenceLocation>>(emptyList()) }
    // LspReferencesRequester adapter.
    val lspReferencesRequester = remember(lspHost) {
        object : LspReferencesRequester {
            override suspend fun references(
                langId: String,
                uri: String,
                line: Int,
                character: Int,
                includeDeclaration: Boolean,
            ): List<ReferenceLocation> = lspHost.references(langId, uri, line, character, includeDeclaration)
        }
    }

    // ── iter-63 Phase 10: mobile long-press context menu ──────────────────
    // "Rename" and "Find references" items are surfaced via a DropdownMenu
    // anchored to the editor surface. The menu is raised by an explicit
    // toolbar button ("…" / "More") in Phase 10 v1; a full long-press
    // gesture detector on the BasicTextField requires a custom Indication
    // and is deferred as #iter-63-longpress-gesture-detector.
    var showEditorContextMenu by remember { mutableStateOf(false) }

    // ── iter-63 Phase 10.4: onSave hook for FormattingTrigger ───────────
    // The existing onSaveClick callback is extended here so that
    // FormattingTrigger.onSave() runs before the file write.
    // Phase 10 v1: edits are obtained but application deferred as
    // #iter-63-save-format-edit-apply.
    val wrappedOnSaveClick: () -> Unit = {
        if (passedLangId != null && formattingTrigger != null) {
            completionScope.launch {
                try {
                    val edits = formattingTrigger.onSave(
                        langId = passedLangId,
                        uri = currentFileUri,
                    )
                    if (edits.isNotEmpty()) {
                        android.util.Log.d("YoleApp", "on-save formatting: ${edits.size} edits")
                    }
                } catch (_: Throwable) {
                    // Degraded — on-save formatting unavailable.
                }
            }
        }
        onSaveClick()
    }

    Column(modifier = Modifier.fillMaxSize().background(bg)) {
        // General editor toolbar (undo/redo/find)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(ideSurface())
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
            // iter-58 Feature 2 Phase 5: outline drawer toggle button.
            // The label uses the same monospace badge style as the
            // existing toolbar buttons; the IconButton variant lives
            // inside the drawer for closing.
            IdeToolbarButton("Outline", "Outline", textColor) {
                outlineDrawerOpen = !outlineDrawerOpen
            }
            // iter-60 Phase 6.5: completion suggest button. Fires an
            // explicit trigger so touchscreen users can open the popup
            // without Ctrl+Space.
            CompletionToolbarButton(
                onTrigger = { completionTrigger.onExplicitTrigger() },
            )
            // iter-62 Phase 8: problems panel toggle. When diagnostics
            // exist, shows a badge count. Tapping opens/closes the panel.
            val diagCount = currentFileDiagnostics.size
            val problemsLabel = if (diagCount > 0) "$diagCount issues" else "Problems"
            val problemsBtnColor = if (diagCount > 0) {
                textColor.copy(red = 1f, green = 0.3f, blue = 0.3f)
            } else {
                textColor
            }
            IdeToolbarButton(
                label = problemsLabel,
                description = "Toggle problems panel",
                textColor = problemsBtnColor,
            ) { isProblemsPanelOpen = !isProblemsPanelOpen }

            // iter-63 Phase 10: "Refs" toolbar shortcut for find-references.
            // Surfaces the ReferencesPanel without requiring long-press.
            if (passedLangId != null) {
                IdeToolbarButton(
                    label = "Refs",
                    description = "Find references (Shift+F12)",
                    textColor = textColor,
                ) {
                    completionScope.launch {
                        Toast.makeText(context, "Searching references…", Toast.LENGTH_SHORT).show()
                        FindReferencesAction.findReferences(
                            requester = lspReferencesRequester,
                            langId = passedLangId,
                            currentUri = currentFileUri,
                            currentCursor = 0,
                            line = 0,
                            character = 0,
                            stack = navStack,
                            onShow = { refs ->
                                referencesList = refs
                                showReferencesPanel = true
                            },
                            onToast = { msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }

                // iter-63 Phase 10: "Rename" toolbar shortcut (F2).
                // Also surfaced via context menu "Rename" item.
                IdeToolbarButton(
                    label = "Rename",
                    description = "Rename symbol (F2)",
                    textColor = textColor,
                ) { showRenameAction = true }
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
                    .background(ideSurface())
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

        // iter-74 (#iter-62-phase-8-problems-scroll-to-line): scroll-to-line state.
        // The Problems panel sets this to the target line; SyncedScrollEditor
        // LaunchedEffect fires and animates sharedScroll to the approximate offset.
        // Reset to null after a short delay so repeated clicks to the same line work.
        val scrollToLineState = remember { mutableStateOf<Int?>(null) }

        // iter-74 (#iter-62-phase-8-tree-sitter-hover-filter-stubbed):
        // Cursor offset written back from SyncedScrollEditor on every keystroke.
        // Used by onHoverRequest to check whether the cursor sits on an identifier
        // token before firing the LSP hover request.
        var lastCursorOffset by remember { mutableStateOf(0) }

        // Editor with line numbers — gutter + text share a single ScrollState
        // via SyncedScrollEditor (iter-55 fix for horizontal desync on vertical
        // scroll). See androidApp/.../ui/editor/SyncedScrollEditor.kt and
        // EditorScrollSyncRobolectricTest for the structural invariants.
        val textState = remember { mutableStateOf(text) }
        // Keep textState in sync with external content changes (e.g., undo/redo
        // that re-assign `text` from history).
        if (textState.value != text) {
            textState.value = text
        }

        // iter-57 Phase 9: detect the file's lang via GrammarRegistry and
        // build a SyntaxHighlighter that reads from the active theme via
        // LocalTheme. LocalTheme.current must be accessed during composition,
        // so we capture it into `theme` then close over it inside the
        // highlighter's theme-supplier lambda. Passing `theme` as a remember
        // key ensures the highlighter is rebuilt when the theme changes.
        // If lang resolves to "plaintext" we pass null to keep the editor
        // unmodified (graceful no-op) and to match the iter-55 baseline
        // behavior for unsupported / disabled formats.
        //
        // Note: detectedLangId, tokenizerEngine, passedLangId,
        // completionEngine, and completionTrigger are all hoisted ABOVE
        // the Column so the toolbar's CompletionToolbarButton can share
        // the trigger. Only the highlighter needs LocalTheme, which must
        // be read inside composition, so it remains here.
        val theme = LocalTheme.current
        val highlighter = remember(tokenizerEngine, detectedLangId, theme) {
            if (detectedLangId != "plaintext") {
                SyntaxHighlighter(tokenizerEngine) { theme }
            } else {
                null
            }
        }

        // iter-74 (#iter-62-phase-8-tree-sitter-hover-filter-stubbed):
        // Token list refreshed whenever text or langId changes. Used by
        // onHoverRequest to check whether the cursor sits on an identifier-
        // scope token before firing the LSP hover request.
        var lastTokens by remember { mutableStateOf<List<Token>>(emptyList()) }
        LaunchedEffect(text, passedLangId, highlighter) {
            val h = highlighter ?: return@LaunchedEffect
            val langForTokens = passedLangId ?: return@LaunchedEffect
            lastTokens = try {
                h.tokens(text, langForTokens)
            } catch (_: Throwable) {
                emptyList()
            }
        }

        // iter-58 Feature 2 Phase 4: resolve the active LanguageFormat from
        // the detected lang id and provide it via LocalLanguage so the
        // editor's affordance handlers (CommentToggleAction, IndentEngine,
        // BracketAutoCompleter) can read it without a parameter.
        val activeLanguage = remember(detectedLangId) {
            LanguageRegistry.get(detectedLangId)
        }

        CompositionLocalProvider(LocalLanguage provides activeLanguage) {
            // iter-58 Feature 2 Phase 5: editor body + OutlineDrawer
            // overlay. The Row arranges the drawer on the LEFT (when
            // open) and the editor surface fills the remaining width.
            // Closing the drawer reclaims the full editor width.
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    digital.vasic.yole.android.ui.editor.OutlineDrawer(
                        textState = textState,
                        langId = passedLangId,
                        engine = tokenizerEngine,
                        isOpen = outlineDrawerOpen,
                        onClose = { outlineDrawerOpen = false },
                        onItemClick = { _ ->
                            // Phase 5 v1: tapping an outline item only
                            // dismisses the drawer. Scroll-to-byte wiring
                            // is a deferred follow-up; the BasicTextField
                            // does not expose a public scroll-to-offset
                            // API today, and the iter-55 SyncedScrollEditor
                            // intentionally hides the scroll state from
                            // outside callers to keep the gutter+body
                            // invariant. Closing the drawer at least makes
                            // the click visible.
                            outlineDrawerOpen = false
                        },
                    )
                    // iter-62 Phase 8: wrap editor surface in a Box so
                    // HoverPopup can be rendered as an overlay inside the
                    // same layout scope.
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        digital.vasic.yole.android.ui.editor.SyncedScrollEditor(
                            textState = textState,
                            showLineNumbers = showLineNumbers,
                            isDarkTheme = isDarkTheme,
                            modifier = Modifier.fillMaxSize(),
                            onTextChanged = { newText ->
                                val oldText = text
                                text = newText
                                onContentChanged(newText)
                                if (newText.length - oldText.length > 5 ||
                                    oldText.length - newText.length > 5 ||
                                    newText.endsWith(" ") || newText.endsWith("\n")) {
                                    addToHistory(newText)
                                }
                                // iter-63 Phase 10.3: feed signature help trigger.
                                // The last typed character drives '(' / ',' / ')' detection.
                                if (passedLangId != null && signatureHelpTrigger != null) {
                                    val lastChar = newText.lastOrNull()
                                    if (lastChar != null) {
                                        signatureHelpTrigger.onKeystroke(
                                            char = lastChar,
                                            langId = passedLangId,
                                            uri = currentFileUri,
                                            line = 0,
                                            character = newText.length,
                                        )
                                    }
                                }
                                // iter-74 (#iter-63-on-type-edit-apply): on-type formatting.
                                // Trigger chars are now queried from server capabilities via
                                // getOnTypeTriggerChars() (iter-74 #iter-63-server-trigger-chars-hardcoded).
                                // Edits are now applied to the buffer (closes #iter-63-on-type-edit-apply).
                                val typedChar = newText.lastOrNull()
                                if (passedLangId != null && formattingTrigger != null &&
                                    typedChar != null && typedChar in onTypeTriggerChars
                                ) {
                                    completionScope.launch {
                                        try {
                                            val edits = formattingTrigger.onType(
                                                langId = passedLangId,
                                                uri = currentFileUri,
                                                line = 0,
                                                character = newText.length,
                                                triggerChar = typedChar,
                                                serverTriggerChars = onTypeTriggerChars,
                                            )
                                            if (edits.isNotEmpty()) {
                                                // Apply edits to the buffer.
                                                val workspaceEdit = WorkspaceEdit(
                                                    changes = mapOf(currentFileUri to edits),
                                                )
                                                val sources = mapOf(currentFileUri to text)
                                                val updated = WorkspaceEditApplier.apply(workspaceEdit, sources)
                                                val newContent = updated[currentFileUri]
                                                if (newContent != null && newContent != text) {
                                                    text = newContent
                                                    onContentChanged(newContent)
                                                }
                                            }
                                        } catch (_: Throwable) {
                                            // Degraded — on-type formatting unavailable.
                                        }
                                    }
                                }
                            },
                            semanticsLabel = "Code editor for $fileName",
                            placeholder = "Start typing...",
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = textColor,
                            ),
                            highlighter = highlighter,
                            langId = passedLangId,
                            tokenizerEngine = tokenizerEngine,
                            completionTrigger = completionTrigger,
                            completionPopupState = completionPopupState,
                            completionEngine = completionEngine,
                            // iter-62 Phase 8.1: pass current-file diagnostics.
                            diagnostics = currentFileDiagnostics,
                            // iter-62 Phase 8.4: F1 explicit hover trigger.
                            // Tree-Sitter identifier lookup deferred per plan:
                            // #iter-62-phase-8-tree-sitter-hover-filter-stubbed.
                            // iter-74 (#iter-62-phase-8-tree-sitter-hover-filter-stubbed):
                            // Replaced always-true stub (line=0,character=0) with real
                            // identifier-at-cursor check using lastTokens + lastCursorOffset.
                            // Identifier scopes: variable, function, method, type, class,
                            // parameter, and raw Tree-Sitter node names ending with "identifier"
                            // or starting with "identifier". Falls back gracefully (fires hover)
                            // when the token list is empty (engine not yet ready).
                            onHoverRequest = if (passedLangId != null) {
                                {
                                    val offset = lastCursorOffset
                                    // Convert char offset to UTF-8 byte offset for Token comparison.
                                    val byteOffset = text.substring(0, offset.coerceIn(0, text.length))
                                        .encodeToByteArray().size
                                    // Identifier scope predicate — matches Tree-Sitter node types
                                    // produced by the tokenizer that represent named symbols.
                                    val identifierScopes = setOf(
                                        "variable", "function", "method", "type", "class",
                                        "parameter",
                                    )
                                    fun isIdentifierScope(scope: String): Boolean =
                                        identifierScopes.any { scope.startsWith(it) } ||
                                            scope.endsWith("identifier") ||
                                            scope == "identifier"
                                    // Check if byte offset falls inside any identifier token.
                                    // When lastTokens is empty (engine warming up) we allow hover
                                    // to fire so we do not suppress it during initial load.
                                    val isOnIdentifier = lastTokens.isEmpty() ||
                                        lastTokens.any { tok ->
                                            byteOffset in tok.startByte until tok.endByte &&
                                                isIdentifierScope(tok.scope)
                                        }
                                    // Only fire hover when cursor is on an identifier token.
                                    if (isOnIdentifier) {
                                        // Compute (line, character) from char offset for LSP call.
                                        val safeOffset = offset.coerceIn(0, text.length)
                                        var hoverLine = 0
                                        var lastNl = -1
                                        for (i in 0 until safeOffset) {
                                            if (text[i] == '\n') { hoverLine++; lastNl = i }
                                        }
                                        val hoverChar = safeOffset - lastNl - 1
                                        completionScope.launch {
                                            val info = lspHost.hover(
                                                langId = passedLangId,
                                                documentUri = currentFileUri,
                                                line = hoverLine,
                                                character = hoverChar,
                                            )
                                            if (info != null) {
                                                hoverBlocks = HoverMarkdownRenderer.render(info.contents)
                                            }
                                        }
                                    }
                                }
                            } else null,
                            // iter-63 Phase 10.2: code-action lightbulb (3rd gutter column).
                            actionsByLine = actionsByLine,
                            onCodeActionLineTap = { line -> codeActionMenuLine = line },
                            // iter-63 Phase 10.4: Ctrl+Shift+F explicit format shortcut.
                            onExplicitFormat = {
                                if (passedLangId != null && formattingTrigger != null) {
                                    completionScope.launch {
                                        try {
                                            val edits = formattingTrigger.onExplicit(
                                                langId = passedLangId,
                                                uri = currentFileUri,
                                            )
                                            // iter-74 (#iter-63-explicit-format-edit-apply):
                                            // apply edits to buffer instead of just logging.
                                            if (edits.isNotEmpty()) {
                                                val workspaceEdit = WorkspaceEdit(
                                                    changes = mapOf(currentFileUri to edits),
                                                )
                                                val sources = mapOf(currentFileUri to text)
                                                val updated = WorkspaceEditApplier.apply(workspaceEdit, sources)
                                                val newContent = updated[currentFileUri]
                                                if (newContent != null && newContent != text) {
                                                    text = newContent
                                                    onContentChanged(newContent)
                                                }
                                            }
                                        } catch (_: Throwable) {
                                            // Degraded — formatting unavailable.
                                        }
                                    }
                                }
                            },
                            // iter-63 Phase 10.1: F2 rename shortcut.
                            onRenameRequest = { showRenameAction = true },
                            // iter-63 Phase 10.5: Shift+F12 find-references shortcut.
                            onFindReferencesRequest = {
                                if (passedLangId != null) {
                                    completionScope.launch {
                                        FindReferencesAction.findReferences(
                                            requester = lspReferencesRequester,
                                            langId = passedLangId,
                                            currentUri = currentFileUri,
                                            currentCursor = 0,
                                            line = 0,
                                            character = 0,
                                            stack = navStack,
                                            onShow = { refs ->
                                                referencesList = refs
                                                showReferencesPanel = true
                                            },
                                            onToast = { msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            },
                                        )
                                    }
                                }
                            },
                            // iter-74 (#iter-62-phase-8-problems-scroll-to-line):
                            // pass scroll-to-line state so DiagnosticsProblemsPanel row
                            // clicks scroll the editor instead of just closing the panel.
                            scrollToLineRequest = scrollToLineState,
                            // iter-74 (#iter-62-phase-8-tree-sitter-hover-filter-stubbed):
                            // write cursor offset back so onHoverRequest can check the token
                            // at the real cursor position instead of always using (0,0).
                            onCursorOffsetChanged = { offset -> lastCursorOffset = offset },
                            // iter-74 (#iter-62-phase-8-hover-precise-anchor):
                            // write cursor pixel rect back so HoverPopup appears near cursor.
                            onCursorRectChanged = { rect ->
                                if (rect != null) {
                                    hoverPopupAnchor = androidx.compose.ui.unit.IntOffset(
                                        rect.left.toInt(),
                                        rect.bottom.toInt(),
                                    )
                                }
                            },
                        )
                        // iter-62 Phase 8.4: HoverPopup overlay.
                        if (hoverBlocks.isNotEmpty()) {
                            HoverPopup(
                                blocks = hoverBlocks,
                                anchorOffset = hoverPopupAnchor,
                                syntaxHighlighter = highlighter,
                                onDismiss = { hoverBlocks = emptyList() },
                            )
                        }
                        // iter-63 Phase 10.3: SignatureHelpPill overlay (mobile).
                        // Rendered as a top-aligned chip above the editor content.
                        // Desktop SignatureHelpPopup wiring is deferred:
                        // #iter-63-desktop-signature-help-popup-deferred.
                        if (currentSignatureHelp != null) {
                            androidx.compose.foundation.layout.Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.TopStart,
                            ) {
                                SignatureHelpPill(info = currentSignatureHelp)
                            }
                        }
                        // iter-63 Phase 10.2: CodeActionMenu overlay.
                        // Shown when the user taps a lightbulb (codeActionMenuLine is non-null).
                        val caLine = codeActionMenuLine
                        if (caLine != null) {
                            val caActions = actionsByLine[caLine] ?: emptyList()
                            CodeActionMenu(
                                actions = caActions,
                                expanded = caActions.isNotEmpty(),
                                onDismissRequest = { codeActionMenuLine = null },
                                onSelected = { action ->
                                    codeActionMenuLine = null
                                    completionScope.launch {
                                        try {
                                            val edit = action.edit
                                            if (edit != null && !edit.isEmpty) {
                                                val sources = mapOf(currentFileUri to text)
                                                val updated = WorkspaceEditApplier.apply(edit, sources)
                                                val newText = updated[currentFileUri]
                                                if (newText != null && newText != text) {
                                                    text = newText
                                                    onContentChanged(newText)
                                                }
                                            }
                                        } catch (_: Throwable) {
                                            // Degraded — code action apply failed.
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
                // iter-62 Phase 8.2: collapsible Problems panel bottom drawer.
                // Shown when isProblemsPanelOpen AND diagnostics exist.
                if (isProblemsPanelOpen && currentFileDiagnostics.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(ideSurface()),
                    ) {
                        DiagnosticsProblemsPanel(
                            diagnostics = currentFileDiagnostics,
                            text = text,
                            onJumpToLine = { line ->
                                // iter-74 (#iter-62-phase-8-problems-scroll-to-line):
                                // set scroll-to-line request; SyncedScrollEditor will
                                // animate sharedScroll to the target line offset.
                                // Reset afterwards so a repeat-click on the same line works.
                                scrollToLineState.value = line
                                completionScope.launch {
                                    kotlinx.coroutines.delay(300)
                                    scrollToLineState.value = null
                                }
                            },
                        )
                    }
                }
                // iter-63 Phase 10.5 / 10.6: ReferencesPanel persistent bottom drawer.
                // Companion to iter-62 DiagnosticsProblemsPanel.
                // Shown when showReferencesPanel is true and referencesList is non-empty.
                if (showReferencesPanel && referencesList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(ideSurface()),
                    ) {
                        ReferencesPanel(
                            references = referencesList,
                            sources = mapOf(currentFileUri to text),
                            onJump = { uri, offset ->
                                navStack.push(NavEntry(currentFileUri, 0))
                                // #iter-62-phase-8-cross-file-back-nav CLOSED:
                                // cross-file navigation is now handled by the BackHandler
                                // via onNavigateToFile. The jump itself is currently
                                // no-op (the references panel shows the line); full
                                // jump-to-location would require opening the file here
                                // and scrolling to `offset`.
                                android.util.Log.d("YoleApp", "references: jump to $uri:$offset")
                            },
                        )
                    }
                }
            }
        }

        // iter-63 Phase 10.1: RenameAction (modal) — shown when showRenameAction is true.
        // The RenameAction Composable manages its own AlertDialog + RenamePreviewPanel.
        if (showRenameAction && passedLangId != null) {
            RenameAction(
                langId = passedLangId,
                uri = currentFileUri,
                line = 0,
                character = 0,
                requester = lspRenameRequester,
                onApplyEdit = { edit ->
                    val sources = mapOf(currentFileUri to text)
                    val updated = WorkspaceEditApplier.apply(edit, sources)
                    val newText = updated[currentFileUri]
                    if (newText != null && newText != text) {
                        text = newText
                        onContentChanged(newText)
                    }
                    showRenameAction = false
                },
                onDismiss = { showRenameAction = false },
            )
        }

        // iter-62 Phase 8.5: DefinitionLocationChooser (multi-result).
        // Rendered as a ModalBottomSheet when chooserLocations is non-empty.
        if (chooserLocations.isNotEmpty()) {
            DefinitionLocationChooser(
                locations = chooserLocations,
                onSelected = { loc ->
                    chooserLocations = emptyList()
                    navStack.push(NavEntry(currentFileUri, 0))
                    // #iter-62-phase-8-cross-file-back-nav CLOSED:
                    // back-handler now routes cross-file pops via onNavigateToFile.
                    onContentChanged(text)
                },
                onDismiss = { chooserLocations = emptyList() },
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
    val surface = ideSurface()
    val textColor = ideText()
    val border = ideBorder()

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
    val bg = ideSurface()
    val border = ideBorder()
    val accent = ideAccent()

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
                label = { Text("QuickNote", fontSize = 10.sp) },
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
            "Yole v1.0.0",
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
    val enabledFormats = FormatRegistry.getEnabledTextFormats()
    val formats = enabledFormats.map { it.id to "${it.name} (${it.defaultExtension})" }

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
                                if (selectedFormat == id) ideAccent().copy(alpha = 0.2f)
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

/**
 * Represents a file entry that can come from either the local filesystem or SAF.
 */
private data class BrowsableFile(
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val localFile: File? = null,
    val safUri: Uri? = null
)

@Composable
fun FileBrowserScreen(
    searchQuery: String = "",
    sortBy: String = "name",
    onSearchQueryChanged: (String) -> Unit = {},
    onSortChanged: (String) -> Unit = {},
    showSearch: Boolean = false,
    onShowSearchChanged: (Boolean) -> Unit = {},
    onFileSelected: (String, String, Uri?, String?) -> Unit,
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentDirectory by remember { mutableStateOf<File?>(null) }
    var allFiles by remember { mutableStateOf<List<BrowsableFile>>(emptyList()) }
    var isLoadingFiles by remember { mutableStateOf(true) }
    // SAF browsing state
    var safTreeUri by remember { mutableStateOf<Uri?>(null) }
    var safCurrentDoc by remember { mutableStateOf<DocumentFile?>(null) }
    var safParentStack by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var currentPathLabel by remember { mutableStateOf("") }
    var showPermissionPrompt by remember { mutableStateOf(false) }

    // Helper: load files from a local File directory
    fun loadLocalDirectory(dir: File) {
        isLoadingFiles = true
        safTreeUri = null
        safCurrentDoc = null
        safParentStack = emptyList()
        currentDirectory = dir
        currentPathLabel = dir.absolutePath
        coroutineScope.launch {
            delay(200)
            val children = dir.listFiles()?.toList()
            if (children != null && children.isNotEmpty()) {
                // Successfully loaded files via direct access
                allFiles = children.map { f ->
                    BrowsableFile(
                        name = f.name,
                        isDirectory = f.isDirectory,
                        size = if (f.isFile) f.length() else 0L,
                        lastModified = f.lastModified(),
                        localFile = f
                    )
                }
                isLoadingFiles = false
            } else if (children == null) {
                // listFiles() returned null - permission denied or access error
                // Try to use SAF instead by prompting user to pick folder
                allFiles = emptyList()
                isLoadingFiles = false
                showPermissionPrompt = true
            } else if (dir.exists() && !dir.canRead()) {
                // Directory exists but we can't read it — likely a permission issue.
                // Show the files as empty and prompt user to grant access or use SAF.
                allFiles = emptyList()
                isLoadingFiles = false
                showPermissionPrompt = true
            } else {
                // Empty directory (no files)
                allFiles = emptyList()
                isLoadingFiles = false
            }
        }
    }

    // Helper: load files from a SAF DocumentFile directory
    fun loadSafDirectory(doc: DocumentFile) {
        isLoadingFiles = true
        currentDirectory = null
        safCurrentDoc = doc
        currentPathLabel = doc.name ?: "Selected folder"
        coroutineScope.launch {
            delay(200)
            val children = doc.listFiles()
            allFiles = children.map { child ->
                BrowsableFile(
                    name = child.name ?: "unknown",
                    isDirectory = child.isDirectory,
                    size = if (!child.isDirectory) child.length() else 0L,
                    lastModified = child.lastModified(),
                    safUri = child.uri
                )
            }
            isLoadingFiles = false
        }
    }

    // Check if we have broad file access (Android 11+).
    // Try/catch protects Robolectric where the shadow Environment throws
    // ArrayIndexOutOfBoundsException; production-runtime returns a normal
    // boolean. In the test path we conservatively default to false so the
    // permission prompt logic is exercised.
    val hasFileAccess = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                Environment.isExternalStorageManager()
            } catch (_: Throwable) {
                false
            }
        } else {
            true // Pre-Android 11: legacy permissions suffice
        }
    }

    // Permission request launcher for MANAGE_EXTERNAL_STORAGE
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // After returning from settings, reload with hopefully granted permission
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (docsDir.exists() && docsDir.canRead()) {
            loadLocalDirectory(docsDir)
        }
    }

    LaunchedEffect(Unit) {
        isLoadingFiles = true
        kotlinx.coroutines.delay(300)

        // Try direct file access first (works with MANAGE_EXTERNAL_STORAGE or pre-Android 11)
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val directFiles = if (docsDir.exists() && docsDir.canRead()) {
            docsDir.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
        
        if (directFiles.isNotEmpty()) {
            // We have direct access and files exist
            currentDirectory = docsDir
            currentPathLabel = docsDir.absolutePath
            allFiles = directFiles.map { f ->
                BrowsableFile(
                    name = f.name,
                    isDirectory = f.isDirectory,
                    size = if (f.isFile) f.length() else 0L,
                    lastModified = f.lastModified(),
                    localFile = f
                )
            }
            isLoadingFiles = false
        } else {
            // No direct access or directory is empty — check persisted SAF permissions
            val persistedUris = context.contentResolver.persistedUriPermissions
            if (persistedUris.isNotEmpty()) {
                // Use the most recently granted SAF tree
                val lastUri = persistedUris.last().uri
                val doc = DocumentFile.fromTreeUri(context, lastUri)
                if (doc != null && doc.isDirectory) {
                    safTreeUri = lastUri
                    loadSafDirectory(doc)
                } else {
                    // Invalid SAF URI, show permission prompt but also load empty local directory
                    currentDirectory = docsDir
                    currentPathLabel = docsDir.absolutePath
                    allFiles = emptyList()
                    isLoadingFiles = false
                    showPermissionPrompt = true
                }
            } else {
                // No SAF permission either - show local directory (even if empty) with permission prompt
                currentDirectory = docsDir
                currentPathLabel = docsDir.absolutePath
                allFiles = emptyList()
                isLoadingFiles = false
                showPermissionPrompt = true
            }
        }
    }

    val files = remember(allFiles, searchQuery, sortBy) {
        var filtered = allFiles.filter { file ->
            searchQuery.isEmpty() || file.name.contains(searchQuery, ignoreCase = true)
        }
        filtered = when (sortBy) {
            "name" -> filtered.sortedBy { it.name.lowercase() }
            "date" -> filtered.sortedByDescending { it.lastModified }
            "size" -> filtered.sortedByDescending { it.size }
            else -> filtered
        }
        filtered
    }

    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { treeUri ->
            // Persist read permission so we can re-access later
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
            } catch (_: SecurityException) {
                // Read-only fallback
                try {
                    context.contentResolver.takePersistableUriPermission(
                        treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Permission could not be persisted; browsing still works this session
                }
            }
            val documentFile = DocumentFile.fromTreeUri(context, treeUri)
            if (documentFile != null && documentFile.isDirectory) {
                safTreeUri = treeUri
                safParentStack = emptyList()
                loadSafDirectory(documentFile)
            } else {
                Toast.makeText(
                    context, "Could not open the selected directory",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Permission prompt: shown when no file access and no SAF tree
        if (showPermissionPrompt && allFiles.isEmpty() && !isLoadingFiles) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "File Access Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Yole needs permission to browse your files. You can either grant full storage access or pick a folder to browse.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            Button(onClick = {
                                val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                permissionLauncher.launch(intent)
                            }) {
                                Text("Grant Access")
                            }
                        }
                        OutlinedButton(onClick = { directoryPicker.launch(null) }) {
                            Text("Pick a Folder")
                        }
                    }
                }
            }
        }

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

        Spacer(modifier = Modifier.height(4.dp))

        // Quick-access directory buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val documentsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            )
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val internalStorageDir = Environment.getExternalStorageDirectory()

            AssistChip(
                onClick = {
                    if (documentsDir.exists() && documentsDir.canRead()) {
                        loadLocalDirectory(documentsDir)
                    } else {
                        // No direct access — launch SAF picker for Documents
                        Toast.makeText(context, "Pick your Documents folder", Toast.LENGTH_SHORT).show()
                        directoryPicker.launch(null)
                    }
                },
                label = { Text("Documents", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )

            AssistChip(
                onClick = {
                    if (downloadsDir.exists() && downloadsDir.canRead()) {
                        loadLocalDirectory(downloadsDir)
                    } else {
                        Toast.makeText(context, "Pick your Downloads folder", Toast.LENGTH_SHORT).show()
                        directoryPicker.launch(null)
                    }
                },
                label = { Text("Downloads", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )

            AssistChip(
                onClick = {
                    if (internalStorageDir.exists() && internalStorageDir.canRead()) {
                        loadLocalDirectory(internalStorageDir)
                    } else {
                        Toast.makeText(context, "Pick a storage folder", Toast.LENGTH_SHORT).show()
                        directoryPicker.launch(null)
                    }
                },
                label = { Text("Internal Storage", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )

            AssistChip(
                onClick = {
                    val appDir = context.getExternalFilesDir(null) ?: context.filesDir
                    loadLocalDirectory(appDir)
                },
                label = { Text("App Files", style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = currentPathLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

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
                        EmptyFileListState(onCreateFile = { onFileSelected("untitled.txt", "", null, null) })
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(
                        items = files,
                        key = { index, file ->
                            file.safUri?.toString() ?: file.localFile?.absolutePath ?: "$index-${file.name}"
                        }
                    ) { index, file ->
                        val fileName = file.name
                        val fileSize = if (!file.isDirectory && file.size > 0) {
                            formatFileSize(file.size)
                        } else {
                            ""
                        }

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
                                    if (file.isDirectory) {
                                        // Navigate into directory
                                        if (file.safUri != null) {
                                            // SAF directory navigation: use findFile() which is
                                            // the reliable way to traverse tree document URIs.
                                            // findFile() queries the DocumentProvider by name
                                            // and returns a proper browsable DocumentFile.
                                            coroutineScope.launch {
                                                val childDoc = safCurrentDoc?.findFile(file.name)
                                                    ?: safCurrentDoc?.listFiles()?.find { child ->
                                                        child.name == file.name && child.isDirectory
                                                    }
                                                if (childDoc != null && childDoc.isDirectory) {
                                                    safCurrentDoc?.let { parent ->
                                                        safParentStack = safParentStack + parent
                                                    }
                                                    loadSafDirectory(childDoc)
                                                } else {
                                                    // Could not resolve child — show message
                                                    Toast.makeText(
                                                        context,
                                                        "Could not open folder: ${file.name}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } else if (file.localFile != null) {
                                            // Local directory navigation
                                            loadLocalDirectory(file.localFile)
                                        }
                                    } else {
                                        // Open file
                                        if (file.safUri != null) {
                                            try {
                                                val inputStream = context.contentResolver
                                                    .openInputStream(file.safUri)
                                                val content = inputStream?.bufferedReader()
                                                    ?.use { it.readText() } ?: ""
                                                onFileSelected(fileName, content, file.safUri, null)
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Could not read file: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onFileSelected(fileName, "", file.safUri, null)
                                            }
                                        } else if (file.localFile != null) {
                                            try {
                                                val content = file.localFile.readText()
                                                onFileSelected(fileName, content, null, file.localFile.absolutePath)
                                            } catch (e: Exception) {
                                                onFileSelected(fileName, "", null, file.localFile?.absolutePath)
                                            }
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
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            if (file.isDirectory) Icons.Filled.List else Icons.Filled.Edit,
                                            contentDescription = if (file.isDirectory) "Folder" else "File",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        // iter-57 Phase 11: language badge chip — drawn only for
                                        // non-directory files with a recognized, enabled grammar.
                                        // BadgeTinter returns null otherwise (no chip rendered).
                                        if (!file.isDirectory) {
                                            val activeTheme = digital.vasic.yole.syntax.theme.LocalTheme.current
                                            val tintArgb = digital.vasic.yole.syntax.render.BadgeTinter.tintFor(fileName, activeTheme)
                                            val badgeLangId = digital.vasic.yole.syntax.render.BadgeTinter.langIdFor(fileName)
                                            if (tintArgb != null && badgeLangId != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .testTag("fileBadge:$badgeLangId")
                                                        .background(
                                                            color = Color(tintArgb),
                                                            shape = RoundedCornerShape(3.dp),
                                                        )
                                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        text = badgeLangId.take(2).uppercase(),
                                                        fontSize = 9.sp,
                                                        color = Color.White,
                                                        fontFamily = FontFamily.Monospace,
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = fileName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    if (!file.isDirectory && fileSize.isNotEmpty()) {
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
                    if (safCurrentDoc != null && safParentStack.isNotEmpty()) {
                        // SAF: go up to parent
                        val parent = safParentStack.last()
                        safParentStack = safParentStack.dropLast(1)
                        loadSafDirectory(parent)
                    } else if (safCurrentDoc != null && safTreeUri != null) {
                        // SAF: at tree root, switch back to local default
                        safTreeUri = null
                        safCurrentDoc = null
                        safParentStack = emptyList()
                        val docsDir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOCUMENTS
                        )
                        val fallback = if (docsDir.exists() && docsDir.canRead()) docsDir
                            else context.filesDir
                        loadLocalDirectory(fallback)
                    } else {
                        // Local filesystem: go up to parent
                        currentDirectory?.parentFile?.let { parent ->
                            loadLocalDirectory(parent)
                        }
                    }
                },
                enabled = !isLoadingFiles && (
                    (safCurrentDoc != null) ||
                    (currentDirectory?.parentFile != null)
                ),
                modifier = Modifier.pressScale()
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Up")
            }

            OutlinedButton(
                onClick = { onFileSelected("untitled.txt", "", null, null) },
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

/**
 * Formats a byte count into a human-readable size string.
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
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
    onAnimationsEnabledChanged: (Boolean) -> Unit,
    enabledFormatIds: Set<String>,
    onFormatToggled: (String, Boolean) -> Unit,
    onOpenFormatsClick: () -> Unit = {},
    // iter-61 Phase 8: navigate to the LSP language support sub-screen.
    onOpenLspSettingsClick: () -> Unit = {}
) {
    val isDarkTheme = isSystemInDarkTheme()
    val bg = ideBackground()

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

        // iter-57 Phase 4.5: FORMATS section is now a navigable entry that
        // opens the dedicated FormatsSettingsScreen (three-section UI with
        // Default-always-on + Text formats + Programming languages). The
        // inline toggle list previously rendered here was a stop-gap; the
        // sub-screen enforces spec §3.7's Markdown-default constraint and
        // surfaces the v1 programming-language list.
        Text(
            text = "FORMATS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(scale = 0.98f),
            onClick = { onOpenFormatsClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Formats",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Markdown is always on. Toggle text formats and programming languages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        // Suppress unused-parameter warnings for the legacy inline-toggle
        // callbacks; they remain in the signature for source compatibility
        // until iter-58 (the Formats sub-screen is now the canonical path).
        @Suppress("UNUSED_EXPRESSION")
        enabledFormatIds
        @Suppress("UNUSED_EXPRESSION")
        onFormatToggled

        Spacer(modifier = Modifier.height(24.dp))

        // iter-61 Phase 8: LSP LANGUAGE SUPPORT section navigating to LspSettingsScreen.
        // v1: all 15 servers show "not available on Android" (honest stub per CONST-035).
        Text(
            text = "LANGUAGE SERVERS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-lsp-entry"),
            onClick = { onOpenLspSettingsClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LSP language support",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "15 bundled language servers. Desktop only in v1.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = "›",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
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
            text = "Yole is a cross-platform text editor supporting 18 text formats including Markdown, Todo.txt, CSV, JSON, LaTeX, AsciiDoc, and more.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Version: 1.0.0",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        // Storage settings section
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CLOUD STORAGE",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Configure cloud and network storage connections.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        var showConfigDialog by remember { mutableStateOf(false) }
        var selectedStorageType by remember { mutableStateOf<StorageType?>(null) }

        // Storage protocol cards
        val storageProtocols = remember {
            listOf(
                Triple(StorageType.WEBDAV, "WebDAV", "Nextcloud, ownCloud, SharePoint"),
                Triple(StorageType.FTP, "FTP", "File Transfer Protocol"),
                Triple(StorageType.SFTP, "SFTP", "Secure File Transfer Protocol"),
                Triple(StorageType.SMB, "SMB/CIFS", "Windows file sharing"),
                Triple(StorageType.GIT, "Git", "Git repository storage"),
                Triple(StorageType.DROPBOX, "Dropbox", "Dropbox cloud storage"),
                Triple(StorageType.GOOGLE_DRIVE, "Google Drive", "Google cloud storage"),
                Triple(StorageType.ONEDRIVE, "OneDrive", "Microsoft cloud storage")
            )
        }

        storageProtocols.forEach { (type, name, description) ->
            val icon = when (type) {
                StorageType.WEBDAV -> Icons.Filled.AccountBox
                StorageType.FTP -> Icons.Filled.Share
                StorageType.SFTP -> Icons.Filled.Lock
                StorageType.SMB -> Icons.Filled.Home
                StorageType.GIT -> Icons.Filled.Build
                StorageType.DROPBOX -> Icons.Filled.Favorite
                StorageType.GOOGLE_DRIVE -> Icons.Filled.Star
                StorageType.ONEDRIVE -> Icons.Filled.Place
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            icon,
                            contentDescription = name,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            selectedStorageType = type
                            showConfigDialog = true
                        }
                    ) {
                        Text("Configure")
                    }
                }
            }
        }

        // Storage configuration dialog
        if (showConfigDialog && selectedStorageType != null) {
            StorageConfigDialog(
                storageType = selectedStorageType!!,
                onDismiss = {
                    showConfigDialog = false
                    selectedStorageType = null
                }
            )
        }
    }
}

@Composable
private fun StorageConfigDialog(
    storageType: StorageType,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configService = remember { NetworkStorageConfigService() }

    // Common fields
    var name by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    // Credential-based fields
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(storageType.defaultPort.toString()) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var share by remember { mutableStateOf("") }
    var repositoryUrl by remember { mutableStateOf("") }
    var localCachePath by remember { mutableStateOf("") }

    // OAuth fields
    var clientId by remember { mutableStateOf("") }
    var clientSecret by remember { mutableStateOf("") }
    var appKey by remember { mutableStateOf("") }
    var appSecret by remember { mutableStateOf("") }

    // Build config from current form fields
    fun buildConfig(): StorageConfig? {
        val configName = name.ifBlank {
            storageType.displayName
        }
        return when (storageType) {
            StorageType.WEBDAV -> {
                if (serverUrl.isBlank()) return null
                StorageConfig.WebDavConfig(
                    name = configName,
                    url = serverUrl,
                    username = username,
                    password = password
                )
            }
            StorageType.FTP -> {
                if (host.isBlank()) return null
                StorageConfig.FtpConfig(
                    name = configName,
                    host = host,
                    port = port.toIntOrNull() ?: 21,
                    username = username,
                    password = password
                )
            }
            StorageType.SFTP -> {
                if (host.isBlank()) return null
                StorageConfig.SftpConfig(
                    name = configName,
                    host = host,
                    port = port.toIntOrNull() ?: 22,
                    username = username.ifBlank { null },
                    password = password.ifBlank { null }
                )
            }
            StorageType.SMB -> {
                if (host.isBlank() || share.isBlank()) return null
                StorageConfig.SmbConfig(
                    name = configName,
                    host = host,
                    share = share,
                    username = username,
                    password = password,
                    port = port.toIntOrNull() ?: 445
                )
            }
            StorageType.GIT -> {
                if (repositoryUrl.isBlank()) return null
                StorageConfig.GitConfig(
                    name = configName,
                    repositoryUrl = repositoryUrl,
                    username = username.ifBlank { null },
                    password = password.ifBlank { null },
                    localCachePath = localCachePath.ifBlank {
                        "${Environment.getExternalStorageDirectory()}/Yole/git-cache/$configName"
                    }
                )
            }
            StorageType.DROPBOX -> {
                if (appKey.isBlank() || appSecret.isBlank()) return null
                StorageConfig.DropboxConfig(
                    name = configName,
                    accessToken = "",
                    appKey = appKey,
                    appSecret = appSecret
                )
            }
            StorageType.GOOGLE_DRIVE -> {
                if (clientId.isBlank() || clientSecret.isBlank()) return null
                StorageConfig.GoogleDriveConfig(
                    name = configName,
                    clientId = clientId,
                    clientSecret = clientSecret
                )
            }
            StorageType.ONEDRIVE -> {
                if (clientId.isBlank() || clientSecret.isBlank()) return null
                StorageConfig.OneDriveConfig(
                    name = configName,
                    clientId = clientId,
                    clientSecret = clientSecret
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isTesting && !isSaving) onDismiss() },
        title = {
            Text(
                text = "Configure ${storageType.displayName}",
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Connection name") },
                    placeholder = { Text(storageType.displayName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                when (storageType) {
                    StorageType.WEBDAV -> {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Server URL") },
                            placeholder = { Text("https://cloud.example.com/remote.php/dav") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    StorageType.FTP, StorageType.SFTP -> {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Host") },
                            placeholder = { Text("ftp.example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    StorageType.SMB -> {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Host") },
                            placeholder = { Text("192.168.1.100") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = share,
                            onValueChange = { share = it },
                            label = { Text("Share name") },
                            placeholder = { Text("Documents") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    StorageType.GIT -> {
                        OutlinedTextField(
                            value = repositoryUrl,
                            onValueChange = { repositoryUrl = it },
                            label = { Text("Repository URL") },
                            placeholder = { Text("https://github.com/user/repo.git") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password / Token (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    StorageType.DROPBOX -> {
                        OutlinedTextField(
                            value = appKey,
                            onValueChange = { appKey = it },
                            label = { Text("App Key") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = appSecret,
                            onValueChange = { appSecret = it },
                            label = { Text("App Secret") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text(
                            text = "Get credentials from the Dropbox App Console.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    StorageType.GOOGLE_DRIVE, StorageType.ONEDRIVE -> {
                        OutlinedTextField(
                            value = clientId,
                            onValueChange = { clientId = it },
                            label = { Text("Client ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = clientSecret,
                            onValueChange = { clientSecret = it },
                            label = { Text("Client Secret") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        val consoleName = if (storageType == StorageType.GOOGLE_DRIVE) {
                            "Google Cloud Console"
                        } else {
                            "Azure Portal"
                        }
                        Text(
                            text = "Get credentials from the $consoleName.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Status message
                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (statusIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Test Connection button
                OutlinedButton(
                    onClick = {
                        val config = buildConfig()
                        if (config == null) {
                            statusMessage = "Please fill in all required fields."
                            statusIsError = true
                            return@OutlinedButton
                        }
                        isTesting = true
                        statusMessage = "Testing connection..."
                        statusIsError = false
                        coroutineScope.launch {
                            val result = configService.testConnection(config)
                            isTesting = false
                            if (result.isSuccess && result.getOrDefault(false)) {
                                statusMessage = "Connection successful!"
                                statusIsError = false
                            } else {
                                val error = result.exceptionOrNull()?.message
                                    ?: "Connection failed."
                                statusMessage = error
                                statusIsError = true
                            }
                        }
                    },
                    enabled = !isTesting && !isSaving
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("Test")
                }

                // Save button
                Button(
                    onClick = {
                        val config = buildConfig()
                        if (config == null) {
                            statusMessage = "Please fill in all required fields."
                            statusIsError = true
                            return@Button
                        }
                        isSaving = true
                        statusMessage = "Saving..."
                        statusIsError = false
                        coroutineScope.launch {
                            val result = configService.addStorage(config)
                            isSaving = false
                            if (result.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "${storageType.displayName} configured successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                            } else {
                                val error = result.exceptionOrNull()?.message
                                    ?: "Failed to save configuration."
                                statusMessage = error
                                statusIsError = true
                            }
                        }
                    },
                    enabled = !isTesting && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isTesting && !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
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
    onFileSelected: (String, String, Uri?, String?) -> Unit,
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
    val bg = ideBackground()

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
    // iter-55 dedup: onFileBrowserClick removed. Canonical file browser
    // is the FILES bottom-nav tab; MoreScreen no longer offers a
    // duplicate entry point.
    onSearchClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val isDarkTheme = isSystemInDarkTheme()
    val bg = ideBackground()

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

        // iter-55 dedup: the "File Browser" Card was removed here. The
        // FILES bottom-nav tab is the canonical file browser; offering
        // a duplicate entry from More Options confused users.

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
                        "Version 1.0.0 - Text editor for Android, Desktop, iOS & Web",
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
