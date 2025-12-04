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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import digital.vasic.yole.model.Document
import digital.vasic.yole.format.FormatRegistry
import kotlinx.coroutines.*
import platform.Foundation.NSUserDefaults

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

    // Animation settings
    fun getAnimationsEnabled(): Boolean = defaults.objectForKey("animations_enabled")?.boolValue ?: true
    fun setAnimationsEnabled(enabled: Boolean) = defaults.setBool(enabled, "animations_enabled")
}

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
@Composable
fun YoleAppContent() {
    val settings = remember { YoleIOSSettings() }

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Documents) }
    var documents by remember { mutableStateOf(emptyList<Document>()) }

    // Load settings
    var themeMode by remember { mutableStateOf(settings.getThemeMode()) }
    var showLineNumbers by remember { mutableStateOf(settings.getShowLineNumbers()) }
    var autoSave by remember { mutableStateOf(settings.getAutoSave()) }
    var animationsEnabled by remember { mutableStateOf(settings.getAnimationsEnabled()) }
    
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
                        AppScreen.Documents -> DocumentsScreen(
                            documents = documents,
                            onDocumentSelected = { /* TODO */ },
                            onCreateDocument = { /* TODO */ }
                        )
                        AppScreen.Editor -> EditorScreen()
                        AppScreen.Settings -> SettingsScreen(
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
                    AppScreen.Documents -> DocumentsScreen(
                        documents = documents,
                        onDocumentSelected = { /* TODO */ },
                        onCreateDocument = { /* TODO */ }
                    )
                    AppScreen.Editor -> EditorScreen()
                    AppScreen.Settings -> SettingsScreen(
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

/**
 * Top App Bar for Yole iOS
 */
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

/**
 * Documents Screen
 */
@Composable
fun DocumentsScreen(
    documents: List<Document>,
    onDocumentSelected: (Document) -> Unit,
    onCreateDocument: () -> Unit
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
                .padding(16.dp),
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
                items(documents) { document ->
                    DocumentItem(
                        document = document,
                        onClick = { onDocumentSelected(document) }
                    )
                }
            }
        }
    }
}

/**
 * Document List Item
 */
@Composable
fun DocumentItem(
    document: Document,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${document.format.name} • ${document.modifiedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Editor Screen
 */
@Composable
fun EditorScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "Editor",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Document editor will be implemented here",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

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
    animationsEnabled: Boolean,
    onAnimationsEnabledChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
    }
}

/**
 * App Screens
 */
sealed class AppScreen {
    object Documents : AppScreen()
    object Editor : AppScreen()
    object Settings : AppScreen()
}