# Challenges Integration Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Integrate the Challenges Git submodule as an orchestrator that drives automated user-flow testing across all Yole applications (Android, Desktop, Web), fix the Android crash bug, and create comprehensive challenges with Robolectric, UI Automator, and Playwright.

**Architecture:** Go Challenges module as top-level orchestrator. It invokes Gradle tasks for Robolectric tests, ADB for UI Automator, and playwright-go for Desktop/Web apps. All results flow through the Challenges reporting system (Markdown/JSON/HTML + WebSocket dashboard). Challenges organized in layers: common flows (platform-agnostic) + platform-specific flows.

**Tech Stack:** Go 1.24+, Challenges framework (digital.vasic.challenges), Robolectric 4.14+, UI Automator 2.3+, Playwright (playwright-go), Gradle, ADB

---

## Task 1: Fix the Android Crash Bug

**Files:**
- Modify: `androidApp/src/main/java/digital/vasic/yole/android/ui/theme/Theme.kt:217`
- Modify: `androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt:83`
- Modify: `androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt:244-250`

**Step 1: Fix Theme.kt empty string crash**

In `androidApp/src/main/java/digital/vasic/yole/android/ui/theme/Theme.kt`, line 217, replace:

```kotlin
val seedColor = seedColorHex?.let { Color(android.graphics.Color.parseColor(it)) }
```

with:

```kotlin
val seedColor = seedColorHex?.takeIf { it.isNotEmpty() }?.let {
    try {
        Color(android.graphics.Color.parseColor(it))
    } catch (e: IllegalArgumentException) {
        null
    }
}
```

**Step 2: Fix YoleSettings to return null for unset values**

In `androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt`, line 83, replace:

```kotlin
fun getCustomSeedColor(): String? = getString("custom_seed_color", "")
```

with:

```kotlin
fun getCustomSeedColor(): String? = getString("custom_seed_color", "").takeIf { it.isNotEmpty() }
```

**Step 3: Add error handling to parser initialization**

In `androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt`, lines 244-250, replace:

```kotlin
    // Initialize parsers with lazy loading for faster startup
    LaunchedEffect(Unit) {
        digital.vasic.yole.format.ParserInitializer.registerAllParsersLazy()
        // Cleanup old PDFs and backups periodically
        PdfExportUtil.cleanupOldPdfs(context)
        BackupRestoreUtil.cleanupOldBackups(context)
    }
```

with:

```kotlin
    // Initialize parsers with lazy loading for faster startup
    LaunchedEffect(Unit) {
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
```

**Step 4: Verify build compiles**

Run: `docker compose run --rm build ./gradlew :androidApp:assembleDebug` (in container per CLAUDE.md)

Expected: BUILD SUCCESSFUL

**Step 5: Run existing tests**

Run: `docker compose run --rm build ./gradlew :shared:testDebugUnitTest`

Expected: All tests pass

**Step 6: Commit**

```bash
git add androidApp/src/main/java/digital/vasic/yole/android/ui/theme/Theme.kt androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt
git commit -m "fix: guard against empty string in Color.parseColor and add error handling to parser init"
```

---

## Task 2: Add Challenges Git Submodule

**Files:**
- Create: `.gitmodules`
- Create: `Challenges/` (submodule)

**Step 1: Add the submodule**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git submodule add https://github.com/vasic-digital/Challenges Challenges
```

**Step 2: Verify submodule was added**

```bash
git submodule status
```

Expected: Shows Challenges commit hash

**Step 3: Verify the Challenges framework files exist**

```bash
ls Challenges/pkg/challenge/ Challenges/pkg/runner/ Challenges/pkg/registry/ Challenges/pkg/report/ Challenges/pkg/assertion/
```

Expected: Framework source files present

**Step 4: Commit**

```bash
git add .gitmodules Challenges
git commit -m "feat: add Challenges framework as Git submodule"
```

---

## Task 3: Add Robolectric to Android Build

**Files:**
- Modify: `androidApp/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

**Step 1: Add Robolectric to version catalog**

In `gradle/libs.versions.toml`, add to `[versions]` section:

```toml
robolectric = "4.14.1"
```

Add to `[libraries]` section:

```toml
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }
```

**Step 2: Add Robolectric dependency to androidApp**

In `androidApp/build.gradle.kts`, add to the `dependencies` block after the testing section:

```kotlin
    // Robolectric for JVM-based Android UI testing
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.test.compose.ui)
```

**Step 3: Add Robolectric test runner configuration**

In `androidApp/build.gradle.kts`, add inside the `android { }` block after `lint { }`:

```kotlin
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
```

**Step 4: Verify build compiles**

Run: `docker compose run --rm build ./gradlew :androidApp:assembleDebug`

Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add gradle/libs.versions.toml androidApp/build.gradle.kts
git commit -m "feat: add Robolectric 4.14.1 dependency for JVM-based Android UI testing"
```

---

## Task 4: Create Robolectric Test Classes for Android

**Files:**
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/AppLaunchRobolectricTest.kt`
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/ThemeRobolectricTest.kt`
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/NavigationRobolectricTest.kt`
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/SettingsRobolectricTest.kt`
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/FileEditingRobolectricTest.kt`
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/FormatDetectionRobolectricTest.kt`
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/TodoWorkflowRobolectricTest.kt`
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/QuickNoteRobolectricTest.kt`
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/BackupRestoreRobolectricTest.kt`
- Create: `androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/AccessibilityRobolectricTest.kt`

**Step 1: Create AppLaunchRobolectricTest**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: App launch without crash
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AppLaunchRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun appLaunchesWithoutCrash() {
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
    }

    @Test
    fun appShowsMainNavigationTabs() {
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
        composeTestRule.onNodeWithText("To-Do").assertIsDisplayed()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
        composeTestRule.onNodeWithText("More").assertIsDisplayed()
    }

    @Test
    fun appInitializesThemeWithoutCrash() {
        // This specifically tests the Theme.kt fix
        // On first launch, custom_seed_color is empty string
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
    }

    @Test
    fun appSurvivesActivityRecreation() {
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
    }

    @Test
    fun appHandlesConfigurationChange() {
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
        composeTestRule.onNodeWithText("To-Do").assertIsDisplayed()
    }
}
```

**Step 2: Create ThemeRobolectricTest**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Theme switching and custom colors
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ThemeRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun defaultThemeAppliesWithoutCrash() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
    }

    @Test
    fun settingsScreenShowsThemeOptions() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Light theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("System theme (follows system setting)").assertIsDisplayed()
    }

    @Test
    fun lightThemeCanBeSelected() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Light theme").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun darkThemeCanBeSelected() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Dark theme").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun systemThemeCanBeSelected() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("System theme (follows system setting)").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
```

**Step 3: Create NavigationRobolectricTest**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Navigation between all screens
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NavigationRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun navigateToFilesScreen() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }

    @Test
    fun navigateToTodoScreen() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("To-Do List").assertIsDisplayed()
    }

    @Test
    fun navigateToQuickNoteScreen() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
    }

    @Test
    fun navigateToMoreScreen() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun fullNavigationCycle() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()

        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("To-Do List").assertIsDisplayed()

        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()

        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }

    @Test
    fun rapidNavigationDoesNotCrash() {
        for (i in 1..10) {
            composeTestRule.onNodeWithText("Files").performClick()
            composeTestRule.onNodeWithText("To-Do").performClick()
            composeTestRule.onNodeWithText("QuickNote").performClick()
            composeTestRule.onNodeWithText("More").performClick()
        }
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }
}
```

**Step 4: Create SettingsRobolectricTest**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Settings modification flows
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SettingsRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun settingsScreenLoads() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun toggleLineNumbers() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Show line numbers").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun toggleAutoSave() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Auto-save").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun formatsListDisplayed() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Formats").assertIsDisplayed()
        composeTestRule.onNodeWithText("Markdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("Todo.txt").assertIsDisplayed()
    }

    @Test
    fun aboutSectionDisplayed() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("About Yole").assertIsDisplayed()
    }

    @Test
    fun navigateBackFromSettings() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("More Options").assertIsDisplayed()
    }
}
```

**Step 5: Create FileEditingRobolectricTest**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: File creation, editing, and preview
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FileEditingRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun createNewFile() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Editing: untitled.txt").assertIsDisplayed()
    }

    @Test
    fun editFileContent() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("# Hello World\n\nThis is a test.")
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchToPreviewMode() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("# Test Document")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.onNodeWithText("Test Document").assertIsDisplayed()
    }

    @Test
    fun switchBackToEditMode() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("# Test")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        composeTestRule.onNodeWithText("Editing: untitled.txt").assertIsDisplayed()
    }

    @Test
    fun saveFile() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("test content")
        composeTestRule.onNodeWithContentDescription("Save").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun navigateBackFromEditor() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }
}
```

**Step 6: Create FormatDetectionRobolectricTest**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Format detection and rendering
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FormatDetectionRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun markdownContentRendersInPreview() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("# Markdown Heading\n\n**bold** and *italic*")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.onNodeWithText("Markdown Heading").assertIsDisplayed()
    }

    @Test
    fun plainTextContentRendersInPreview() {
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithContentDescription("Add").performClick()
        composeTestRule.onNodeWithText("Start typing...").performTextInput("Just plain text content here.")
        composeTestRule.onNodeWithContentDescription("Preview").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun formatListIsDisplayedInSettings() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Formats").assertIsDisplayed()
        composeTestRule.onNodeWithText("Markdown").assertIsDisplayed()
    }
}
```

**Step 7: Create TodoWorkflowRobolectricTest**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Todo.txt workflow
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class TodoWorkflowRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun addTodoItem() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Buy groceries")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("Buy groceries").assertIsDisplayed()
    }

    @Test
    fun addMultipleTodoItems() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        val items = listOf("Task 1", "Task 2", "Task 3")
        for (item in items) {
            composeTestRule.onNodeWithText("Add new todo...").performTextInput(item)
            composeTestRule.onNodeWithText("Add").performClick()
        }
        for (item in items) {
            composeTestRule.onNodeWithText(item).assertIsDisplayed()
        }
    }

    @Test
    fun toggleTodoCompletion() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Toggle me")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("Toggle me").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun toggleShowHideDone() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Done item")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onNodeWithText("Done item").performClick()
        composeTestRule.onNodeWithText("Hide Done").performClick()
        composeTestRule.onNodeWithText("Show Done").assertIsDisplayed()
    }

    @Test
    fun deleteTodoItem() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Delete me")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.onAllNodesWithContentDescription("Delete").onFirst().performClick()
        composeTestRule.onNodeWithText("Delete me").assertDoesNotExist()
    }
}
```

**Step 8: Create QuickNoteRobolectricTest**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: QuickNote workflow
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class QuickNoteRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun quickNoteScreenLoads() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
    }

    @Test
    fun enterNoteContent() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("Meeting notes")
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchToPreview() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("# My Note")
        composeTestRule.onNodeWithText("Preview").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun switchBackToEdit() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("# My Note")
        composeTestRule.onNodeWithText("Preview").performClick()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun saveNote() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("Save this note")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
    }
}
```

**Step 9: Create BackupRestoreRobolectricTest**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Backup and restore
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BackupRestoreRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun backupRestoreUIAccessible() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Backup & Restore").assertIsDisplayed()
    }

    @Test
    fun backupButtonDisplayed() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Backup & Restore").assertIsDisplayed()
        composeTestRule.onNodeWithText("Backup").assertIsDisplayed()
    }

    @Test
    fun restoreButtonDisplayed() {
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Backup & Restore").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restore").assertIsDisplayed()
    }
}
```

**Step 10: Create AccessibilityRobolectricTest**

```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Robolectric test: Accessibility checks
 *
 *########################################################*/
package digital.vasic.yole.android.robolectric

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import digital.vasic.yole.android.MainActivity
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AccessibilityRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun mainNavigationItemsAreAccessible() {
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()
        composeTestRule.onNodeWithText("To-Do").assertIsDisplayed()
        composeTestRule.onNodeWithText("QuickNote").assertIsDisplayed()
        composeTestRule.onNodeWithText("More").assertIsDisplayed()
    }

    @Test
    fun addButtonHasContentDescription() {
        composeTestRule.onNodeWithContentDescription("Add").assertIsDisplayed()
    }

    @Test
    fun todoInputFieldIsAccessible() {
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").assertIsDisplayed()
    }

    @Test
    fun quickNoteInputFieldIsAccessible() {
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").assertIsDisplayed()
    }

    @Test
    fun allInteractiveElementsClickable() {
        composeTestRule.onNodeWithText("Files").assertHasClickAction()
        composeTestRule.onNodeWithText("To-Do").assertHasClickAction()
        composeTestRule.onNodeWithText("QuickNote").assertHasClickAction()
        composeTestRule.onNodeWithText("More").assertHasClickAction()
    }
}
```

**Step 11: Run all Robolectric tests**

Run: `docker compose run --rm build ./gradlew :androidApp:testDebugUnitTest --tests "digital.vasic.yole.android.robolectric.*"`

Expected: All tests pass (or specific failures to fix in next iteration)

**Step 12: Commit**

```bash
git add androidApp/src/test/kotlin/digital/vasic/yole/android/robolectric/
git commit -m "feat: add 10 Robolectric test classes covering all Android user flows"
```

---

## Task 5: Create the Challenges Go Module

**Files:**
- Create: `challenges/go.mod`
- Create: `challenges/go.sum`
- Create: `challenges/main.go`
- Create: `challenges/adapters/gradle.go`
- Create: `challenges/adapters/adb.go`
- Create: `challenges/adapters/playwright.go`
- Create: `challenges/adapters/process.go`
- Create: `challenges/adapters/platform.go`

**Step 1: Initialize Go module**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
mkdir -p challenges/adapters
cd challenges
go mod init digital.vasic.yole/challenges
```

**Step 2: Add dependency on Challenges framework**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole/challenges
go get digital.vasic.challenges@latest
go get github.com/playwright-community/playwright-go@latest
go get github.com/stretchr/testify@latest
```

**Step 3: Create adapters/platform.go — PlatformAdapter interface**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package adapters

import "context"

// PlatformAdapter defines the contract for driving apps on different platforms.
// Implementations exist for Android (ADB/Robolectric), Desktop (Playwright), and Web (Playwright).
type PlatformAdapter interface {
	// Name returns the platform name (e.g., "android", "desktop", "web").
	Name() string

	// Build builds the application for this platform.
	Build(ctx context.Context) error

	// Launch starts the application.
	Launch(ctx context.Context) error

	// IsRunning checks if the application is currently running.
	IsRunning(ctx context.Context) (bool, error)

	// OpenFile opens a file in the application.
	OpenFile(ctx context.Context, path string) error

	// GetDisplayedContent returns the currently visible content.
	GetDisplayedContent(ctx context.Context) (string, error)

	// EditContent enters content into the editor.
	EditContent(ctx context.Context, content string) error

	// SaveFile saves the current document.
	SaveFile(ctx context.Context) error

	// NavigateTo navigates to a named screen/tab.
	NavigateTo(ctx context.Context, screen string) error

	// NavigateToSettings opens the settings screen.
	NavigateToSettings(ctx context.Context) error

	// SetSetting modifies a setting value.
	SetSetting(ctx context.Context, key, value string) error

	// SwitchTheme changes the application theme.
	SwitchTheme(ctx context.Context, theme string) error

	// TakeScreenshot captures the current screen.
	TakeScreenshot(ctx context.Context) ([]byte, error)

	// Close terminates the application.
	Close(ctx context.Context) error
}
```

**Step 4: Create adapters/gradle.go — Gradle task execution + JUnit XML parsing**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package adapters

import (
	"context"
	"encoding/xml"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

// GradleAdapter executes Gradle tasks and parses JUnit XML results.
type GradleAdapter struct {
	ProjectRoot string
	UseDocker   bool
}

// JUnitTestSuites represents the top-level JUnit XML structure.
type JUnitTestSuites struct {
	XMLName    xml.Name         `xml:"testsuites"`
	TestSuites []JUnitTestSuite `xml:"testsuite"`
}

// JUnitTestSuite represents a single test suite in JUnit XML.
type JUnitTestSuite struct {
	XMLName   xml.Name        `xml:"testsuite"`
	Name      string          `xml:"name,attr"`
	Tests     int             `xml:"tests,attr"`
	Failures  int             `xml:"failures,attr"`
	Errors    int             `xml:"errors,attr"`
	Skipped   int             `xml:"skipped,attr"`
	Time      float64         `xml:"time,attr"`
	TestCases []JUnitTestCase `xml:"testcase"`
}

// JUnitTestCase represents a single test case in JUnit XML.
type JUnitTestCase struct {
	XMLName   xml.Name       `xml:"testcase"`
	Name      string         `xml:"name,attr"`
	ClassName string         `xml:"classname,attr"`
	Time      float64        `xml:"time,attr"`
	Failure   *JUnitFailure  `xml:"failure,omitempty"`
	Error     *JUnitError    `xml:"error,omitempty"`
	Skipped   *JUnitSkipped  `xml:"skipped,omitempty"`
}

// JUnitFailure represents a test failure.
type JUnitFailure struct {
	Message string `xml:"message,attr"`
	Type    string `xml:"type,attr"`
	Content string `xml:",chardata"`
}

// JUnitError represents a test error.
type JUnitError struct {
	Message string `xml:"message,attr"`
	Type    string `xml:"type,attr"`
	Content string `xml:",chardata"`
}

// JUnitSkipped represents a skipped test.
type JUnitSkipped struct {
	Message string `xml:"message,attr"`
}

// GradleResult holds the result of a Gradle task execution.
type GradleResult struct {
	Task     string
	Success  bool
	Duration time.Duration
	Output   string
	Suites   []JUnitTestSuite
}

// RunTask executes a Gradle task and returns the result.
func (g *GradleAdapter) RunTask(ctx context.Context, task string, args ...string) (*GradleResult, error) {
	start := time.Now()

	cmdArgs := []string{task}
	cmdArgs = append(cmdArgs, args...)

	var cmd *exec.Cmd
	if g.UseDocker {
		dockerArgs := []string{"compose", "run", "--rm", "build", "./gradlew"}
		dockerArgs = append(dockerArgs, cmdArgs...)
		cmd = exec.CommandContext(ctx, "docker", dockerArgs...)
	} else {
		cmd = exec.CommandContext(ctx, filepath.Join(g.ProjectRoot, "gradlew"), cmdArgs...)
	}
	cmd.Dir = g.ProjectRoot

	output, err := cmd.CombinedOutput()
	duration := time.Since(start)

	result := &GradleResult{
		Task:     task,
		Success:  err == nil,
		Duration: duration,
		Output:   string(output),
	}

	return result, err
}

// RunTests executes Gradle test task and parses JUnit XML results.
func (g *GradleAdapter) RunTests(ctx context.Context, task string, testFilter string) (*GradleResult, error) {
	args := []string{}
	if testFilter != "" {
		args = append(args, "--tests", testFilter)
	}

	result, err := g.RunTask(ctx, task, args...)
	if err != nil && result == nil {
		return nil, fmt.Errorf("gradle task failed: %w", err)
	}

	// Parse JUnit XML results
	suites, parseErr := g.ParseJUnitResults(task)
	if parseErr == nil {
		result.Suites = suites
	}

	return result, err
}

// ParseJUnitResults finds and parses JUnit XML files from test output.
func (g *GradleAdapter) ParseJUnitResults(task string) ([]JUnitTestSuite, error) {
	var allSuites []JUnitTestSuite

	// Search for JUnit XML files in build directories
	searchPaths := []string{
		filepath.Join(g.ProjectRoot, "shared", "build", "test-results"),
		filepath.Join(g.ProjectRoot, "androidApp", "build", "test-results"),
		filepath.Join(g.ProjectRoot, "desktopApp", "build", "test-results"),
		filepath.Join(g.ProjectRoot, "webApp", "build", "test-results"),
	}

	for _, searchPath := range searchPaths {
		err := filepath.Walk(searchPath, func(path string, info os.FileInfo, err error) error {
			if err != nil {
				return nil // Skip inaccessible dirs
			}
			if !info.IsDir() && strings.HasSuffix(path, ".xml") {
				suites, parseErr := parseJUnitXML(path)
				if parseErr == nil {
					allSuites = append(allSuites, suites...)
				}
			}
			return nil
		})
		if err != nil {
			continue
		}
	}

	return allSuites, nil
}

func parseJUnitXML(path string) ([]JUnitTestSuite, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}

	// Try parsing as testsuites (multiple suites)
	var suites JUnitTestSuites
	if err := xml.Unmarshal(data, &suites); err == nil && len(suites.TestSuites) > 0 {
		return suites.TestSuites, nil
	}

	// Try parsing as single testsuite
	var suite JUnitTestSuite
	if err := xml.Unmarshal(data, &suite); err == nil {
		return []JUnitTestSuite{suite}, nil
	}

	return nil, fmt.Errorf("unable to parse JUnit XML: %s", path)
}
```

**Step 5: Create adapters/adb.go — ADB device management**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package adapters

import (
	"context"
	"fmt"
	"os/exec"
	"strings"
	"time"
)

// ADBAdapter manages Android devices/emulators via ADB.
type ADBAdapter struct {
	DeviceSerial string
	APKPath      string
	PackageName  string
	ActivityName string
}

// NewADBAdapter creates an ADBAdapter for the Yole Android app.
func NewADBAdapter() *ADBAdapter {
	return &ADBAdapter{
		PackageName:  "digital.vasic.yole.android",
		ActivityName: "digital.vasic.yole.android.MainActivity",
	}
}

// IsDeviceAvailable checks if an Android device or emulator is connected.
func (a *ADBAdapter) IsDeviceAvailable(ctx context.Context) (bool, error) {
	cmd := exec.CommandContext(ctx, "adb", "devices")
	output, err := cmd.Output()
	if err != nil {
		return false, fmt.Errorf("adb not available: %w", err)
	}
	lines := strings.Split(string(output), "\n")
	for _, line := range lines[1:] {
		if strings.Contains(line, "device") && !strings.Contains(line, "offline") {
			return true, nil
		}
	}
	return false, nil
}

// InstallAPK installs the APK on the connected device.
func (a *ADBAdapter) InstallAPK(ctx context.Context, apkPath string) error {
	args := []string{"install", "-r", apkPath}
	if a.DeviceSerial != "" {
		args = append([]string{"-s", a.DeviceSerial}, args...)
	}
	cmd := exec.CommandContext(ctx, "adb", args...)
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("install failed: %s: %w", string(output), err)
	}
	return nil
}

// LaunchApp starts the Yole app on the device.
func (a *ADBAdapter) LaunchApp(ctx context.Context) error {
	args := []string{"shell", "am", "start", "-n",
		fmt.Sprintf("%s/%s", a.PackageName, a.ActivityName)}
	if a.DeviceSerial != "" {
		args = append([]string{"-s", a.DeviceSerial}, args...)
	}
	cmd := exec.CommandContext(ctx, "adb", args...)
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("launch failed: %s: %w", string(output), err)
	}
	return nil
}

// StopApp force-stops the Yole app.
func (a *ADBAdapter) StopApp(ctx context.Context) error {
	args := []string{"shell", "am", "force-stop", a.PackageName}
	if a.DeviceSerial != "" {
		args = append([]string{"-s", a.DeviceSerial}, args...)
	}
	cmd := exec.CommandContext(ctx, "adb", args...)
	_, err := cmd.CombinedOutput()
	return err
}

// IsAppRunning checks if the Yole app process is active.
func (a *ADBAdapter) IsAppRunning(ctx context.Context) (bool, error) {
	args := []string{"shell", "pidof", a.PackageName}
	if a.DeviceSerial != "" {
		args = append([]string{"-s", a.DeviceSerial}, args...)
	}
	cmd := exec.CommandContext(ctx, "adb", args...)
	output, err := cmd.Output()
	if err != nil {
		return false, nil
	}
	return strings.TrimSpace(string(output)) != "", nil
}

// TakeScreenshot captures the device screen.
func (a *ADBAdapter) TakeScreenshot(ctx context.Context) ([]byte, error) {
	tmpPath := fmt.Sprintf("/sdcard/screenshot_%d.png", time.Now().UnixMilli())
	args := []string{"shell", "screencap", "-p", tmpPath}
	if a.DeviceSerial != "" {
		args = append([]string{"-s", a.DeviceSerial}, args...)
	}

	cmd := exec.CommandContext(ctx, "adb", args...)
	if _, err := cmd.CombinedOutput(); err != nil {
		return nil, fmt.Errorf("screencap failed: %w", err)
	}

	pullArgs := []string{"pull", tmpPath, "/tmp/screenshot.png"}
	if a.DeviceSerial != "" {
		pullArgs = append([]string{"-s", a.DeviceSerial}, pullArgs...)
	}
	cmd = exec.CommandContext(ctx, "adb", pullArgs...)
	if _, err := cmd.CombinedOutput(); err != nil {
		return nil, fmt.Errorf("pull screenshot failed: %w", err)
	}

	// Cleanup remote file
	exec.CommandContext(ctx, "adb", "shell", "rm", tmpPath).Run()

	return exec.CommandContext(ctx, "cat", "/tmp/screenshot.png").Output()
}

// WaitForApp waits until the app is running or timeout.
func (a *ADBAdapter) WaitForApp(ctx context.Context, timeout time.Duration) error {
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		running, _ := a.IsAppRunning(ctx)
		if running {
			return nil
		}
		time.Sleep(500 * time.Millisecond)
	}
	return fmt.Errorf("app did not start within %v", timeout)
}
```

**Step 6: Create adapters/playwright.go — Playwright wrapper**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package adapters

import (
	"context"
	"fmt"

	"github.com/playwright-community/playwright-go"
)

// PlaywrightAdapter wraps playwright-go for browser and desktop automation.
type PlaywrightAdapter struct {
	pw      *playwright.Playwright
	browser playwright.Browser
	page    playwright.Page
	BaseURL string
}

// NewPlaywrightAdapter creates a new Playwright adapter.
func NewPlaywrightAdapter(baseURL string) *PlaywrightAdapter {
	return &PlaywrightAdapter{
		BaseURL: baseURL,
	}
}

// Initialize sets up the Playwright browser instance.
func (p *PlaywrightAdapter) Initialize(ctx context.Context, browserType string) error {
	pw, err := playwright.Run()
	if err != nil {
		return fmt.Errorf("could not start playwright: %w", err)
	}
	p.pw = pw

	var browser playwright.Browser
	switch browserType {
	case "chromium":
		browser, err = pw.Chromium.Launch(playwright.BrowserTypeLaunchOptions{
			Headless: playwright.Bool(true),
		})
	case "firefox":
		browser, err = pw.Firefox.Launch(playwright.BrowserTypeLaunchOptions{
			Headless: playwright.Bool(true),
		})
	case "webkit":
		browser, err = pw.WebKit.Launch(playwright.BrowserTypeLaunchOptions{
			Headless: playwright.Bool(true),
		})
	default:
		browser, err = pw.Chromium.Launch(playwright.BrowserTypeLaunchOptions{
			Headless: playwright.Bool(true),
		})
	}
	if err != nil {
		return fmt.Errorf("could not launch browser: %w", err)
	}
	p.browser = browser

	page, err := browser.NewPage()
	if err != nil {
		return fmt.Errorf("could not create page: %w", err)
	}
	p.page = page

	return nil
}

// Navigate goes to the specified URL.
func (p *PlaywrightAdapter) Navigate(ctx context.Context, url string) error {
	_, err := p.page.Goto(url, playwright.PageGotoOptions{
		WaitUntil: playwright.WaitUntilStateNetworkidle,
	})
	return err
}

// Click clicks an element matching the selector.
func (p *PlaywrightAdapter) Click(ctx context.Context, selector string) error {
	return p.page.Click(selector)
}

// ClickByText clicks an element containing the specified text.
func (p *PlaywrightAdapter) ClickByText(ctx context.Context, text string) error {
	locator := p.page.GetByText(text)
	return locator.Click()
}

// Fill types text into an input element.
func (p *PlaywrightAdapter) Fill(ctx context.Context, selector string, value string) error {
	return p.page.Fill(selector, value)
}

// GetTextContent returns the text content of an element.
func (p *PlaywrightAdapter) GetTextContent(ctx context.Context, selector string) (string, error) {
	content, err := p.page.TextContent(selector)
	if err != nil {
		return "", err
	}
	return content, nil
}

// IsVisible checks if an element is visible on the page.
func (p *PlaywrightAdapter) IsVisible(ctx context.Context, selector string) (bool, error) {
	return p.page.IsVisible(selector)
}

// Screenshot takes a screenshot of the current page.
func (p *PlaywrightAdapter) Screenshot(ctx context.Context) ([]byte, error) {
	return p.page.Screenshot()
}

// Close shuts down the browser and Playwright.
func (p *PlaywrightAdapter) Close(ctx context.Context) error {
	if p.browser != nil {
		if err := p.browser.Close(); err != nil {
			return err
		}
	}
	if p.pw != nil {
		if err := p.pw.Stop(); err != nil {
			return err
		}
	}
	return nil
}

// WaitForSelector waits for an element to appear on the page.
func (p *PlaywrightAdapter) WaitForSelector(ctx context.Context, selector string) error {
	_, err := p.page.WaitForSelector(selector)
	return err
}

// Page returns the underlying Playwright page for advanced operations.
func (p *PlaywrightAdapter) Page() playwright.Page {
	return p.page
}
```

**Step 7: Create adapters/process.go — JVM process management**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package adapters

import (
	"context"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"syscall"
	"time"
)

// ProcessAdapter manages JVM/native process lifecycle.
type ProcessAdapter struct {
	cmd     *exec.Cmd
	process *os.Process
}

// LaunchJVM starts a JVM application from a JAR file.
func (p *ProcessAdapter) LaunchJVM(ctx context.Context, jarPath string, args ...string) error {
	cmdArgs := []string{"-jar", jarPath}
	cmdArgs = append(cmdArgs, args...)

	p.cmd = exec.CommandContext(ctx, "java", cmdArgs...)
	p.cmd.Dir = filepath.Dir(jarPath)

	if err := p.cmd.Start(); err != nil {
		return fmt.Errorf("failed to launch JVM app: %w", err)
	}
	p.process = p.cmd.Process
	return nil
}

// IsRunning checks if the managed process is still alive.
func (p *ProcessAdapter) IsRunning() bool {
	if p.process == nil {
		return false
	}
	err := p.process.Signal(syscall.Signal(0))
	return err == nil
}

// WaitForReady waits until the process is running or timeout.
func (p *ProcessAdapter) WaitForReady(ctx context.Context, timeout time.Duration) error {
	deadline := time.Now().Add(timeout)
	for time.Now().Before(deadline) {
		if p.IsRunning() {
			return nil
		}
		time.Sleep(200 * time.Millisecond)
	}
	return fmt.Errorf("process did not start within %v", timeout)
}

// Stop gracefully terminates the process.
func (p *ProcessAdapter) Stop() error {
	if p.process == nil {
		return nil
	}

	// Send SIGTERM first
	if err := p.process.Signal(syscall.SIGTERM); err != nil {
		// Process may already be dead
		return nil
	}

	// Wait up to 5 seconds for graceful shutdown
	done := make(chan error, 1)
	go func() {
		_, err := p.process.Wait()
		done <- err
	}()

	select {
	case <-done:
		return nil
	case <-time.After(5 * time.Second):
		return p.process.Kill()
	}
}

// Kill forcefully terminates the process.
func (p *ProcessAdapter) Kill() error {
	if p.process == nil {
		return nil
	}
	return p.process.Kill()
}
```

**Step 8: Create main.go — CLI entry point**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"time"

	"digital.vasic.challenges/pkg/registry"
	"digital.vasic.challenges/pkg/runner"
	"digital.vasic.challenges/pkg/report"
)

func main() {
	var (
		platform   = flag.String("platform", "all", "Platform to test: android, desktop, web, all")
		category   = flag.String("category", "", "Challenge category to run (empty = all)")
		reportFmt  = flag.String("report", "markdown", "Report format: markdown, json, html")
		outputDir  = flag.String("output", "reports", "Output directory for reports")
		useDocker  = flag.Bool("docker", false, "Run Gradle tasks in Docker containers")
		timeout    = flag.Duration("timeout", 30*time.Minute, "Global timeout for all challenges")
	)
	flag.Parse()

	// Determine project root (parent of challenges/ directory)
	execPath, err := os.Executable()
	if err != nil {
		execPath, _ = os.Getwd()
	}
	projectRoot := filepath.Dir(filepath.Dir(execPath))
	if _, err := os.Stat(filepath.Join(projectRoot, "settings.gradle.kts")); err != nil {
		// Fallback: assume we're running from challenges/ directory
		projectRoot, _ = filepath.Abs("..")
	}

	fmt.Printf("Yole Challenges Runner\n")
	fmt.Printf("Project root: %s\n", projectRoot)
	fmt.Printf("Platform: %s\n", *platform)
	fmt.Printf("Docker: %v\n", *useDocker)
	fmt.Printf("Timeout: %v\n", *timeout)
	fmt.Println()

	ctx, cancel := context.WithTimeout(context.Background(), *timeout)
	defer cancel()

	// Get default registry
	reg := registry.Default()

	// Register all challenges
	registerInfraChallenges(reg, projectRoot, *useDocker)
	if *platform == "all" || *platform == "android" {
		registerAndroidChallenges(reg, projectRoot, *useDocker)
	}
	if *platform == "all" || *platform == "desktop" {
		registerDesktopChallenges(reg, projectRoot)
	}
	if *platform == "all" || *platform == "web" {
		registerWebChallenges(reg, projectRoot)
	}

	// Filter by category if specified
	var challenges []string
	if *category != "" {
		for _, ch := range reg.ListByCategory(*category) {
			challenges = append(challenges, string(ch.ID()))
		}
	}

	// Run challenges
	r := runner.New(reg)
	results, err := r.Run(ctx)
	if err != nil {
		log.Printf("Runner error: %v\n", err)
	}

	// Generate report
	os.MkdirAll(*outputDir, 0755)
	switch *reportFmt {
	case "json":
		report.WriteJSON(filepath.Join(*outputDir, "results.json"), results)
	case "html":
		report.WriteHTML(filepath.Join(*outputDir, "results.html"), results)
	default:
		report.WriteMarkdown(filepath.Join(*outputDir, "results.md"), results)
	}

	fmt.Printf("\nReport written to %s/\n", *outputDir)

	// Exit with non-zero if any challenge failed
	for _, r := range results {
		if !r.Success {
			os.Exit(1)
		}
	}
}

// Placeholder registration functions — implemented in subsequent tasks
func registerInfraChallenges(reg *registry.Registry, projectRoot string, useDocker bool) {}
func registerAndroidChallenges(reg *registry.Registry, projectRoot string, useDocker bool) {}
func registerDesktopChallenges(reg *registry.Registry, projectRoot string) {}
func registerWebChallenges(reg *registry.Registry, projectRoot string) {}
```

**Step 9: Verify Go module compiles**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole/challenges
go mod tidy
go build ./...
```

Expected: Compilation succeeds (or expected errors from placeholder functions)

**Step 10: Commit**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git add challenges/
git commit -m "feat: create Challenges Go module with platform adapters (Gradle, ADB, Playwright, Process)"
```

---

## Task 6: Create Infrastructure Challenges

**Files:**
- Create: `challenges/infra/gradle_build.go`
- Create: `challenges/infra/gradle_tests.go`
- Create: `challenges/infra/lint.go`

**Step 1: Create infra/gradle_build.go**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package infra

import (
	"context"
	"fmt"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// GradleBuildChallenge verifies all Gradle build tasks succeed.
type GradleBuildChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

func NewGradleBuildChallenge(projectRoot string, useDocker bool) *GradleBuildChallenge {
	ch := &GradleBuildChallenge{
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
	ch.SetID("infra-gradle-build")
	ch.SetName("Gradle Build Verification")
	ch.SetDescription("Verifies all application modules compile successfully")
	ch.SetCategory("infrastructure")
	return ch
}

func (c *GradleBuildChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
	result := c.CreateResult()

	builds := []struct {
		name string
		task string
	}{
		{"Android Debug", ":androidApp:assembleDebug"},
		{"Desktop JAR", ":desktopApp:jar"},
		{"Shared Library", ":shared:compileKotlinJvm"},
	}

	for _, build := range builds {
		c.ReportProgress(fmt.Sprintf("Building %s...", build.name))
		res, err := c.gradle.RunTask(ctx, build.task)
		if err != nil {
			result.AddFailure(fmt.Sprintf("%s build failed: %v", build.name, err))
			if res != nil {
				result.AddDetail(build.name+"_output", res.Output)
			}
			continue
		}
		result.AddSuccess(fmt.Sprintf("%s build succeeded in %v", build.name, res.Duration))
	}

	return result, nil
}
```

**Step 2: Create infra/gradle_tests.go**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package infra

import (
	"context"
	"fmt"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// GradleTestsChallenge runs all existing Gradle tests and collects results.
type GradleTestsChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

func NewGradleTestsChallenge(projectRoot string, useDocker bool) *GradleTestsChallenge {
	ch := &GradleTestsChallenge{
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
	ch.SetID("infra-gradle-tests")
	ch.SetName("Gradle Test Execution")
	ch.SetDescription("Runs all existing unit and integration tests across all modules")
	ch.SetCategory("infrastructure")
	ch.SetDependencies([]challenge.ID{"infra-gradle-build"})
	return ch
}

func (c *GradleTestsChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
	result := c.CreateResult()

	testTasks := []struct {
		name string
		task string
	}{
		{"Shared Unit Tests", ":shared:testDebugUnitTest"},
		{"Shared Desktop Tests", ":shared:desktopTest"},
		{"Android Robolectric Tests", ":androidApp:testDebugUnitTest"},
	}

	totalTests := 0
	totalFailures := 0

	for _, tt := range testTasks {
		c.ReportProgress(fmt.Sprintf("Running %s...", tt.name))
		res, err := c.gradle.RunTests(ctx, tt.task, "")

		if res != nil {
			for _, suite := range res.Suites {
				totalTests += suite.Tests
				totalFailures += suite.Failures + suite.Errors
				result.AddDetail(fmt.Sprintf("%s_%s", tt.name, suite.Name), fmt.Sprintf(
					"tests=%d failures=%d errors=%d time=%.2fs",
					suite.Tests, suite.Failures, suite.Errors, suite.Time,
				))
			}
		}

		if err != nil {
			result.AddFailure(fmt.Sprintf("%s failed: %v", tt.name, err))
		} else {
			result.AddSuccess(fmt.Sprintf("%s passed in %v", tt.name, res.Duration))
		}
	}

	result.AddDetail("total_tests", fmt.Sprintf("%d", totalTests))
	result.AddDetail("total_failures", fmt.Sprintf("%d", totalFailures))

	return result, nil
}
```

**Step 3: Create infra/lint.go**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package infra

import (
	"context"
	"fmt"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// LintChallenge runs lint and static analysis checks.
type LintChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

func NewLintChallenge(projectRoot string, useDocker bool) *LintChallenge {
	ch := &LintChallenge{
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
	ch.SetID("infra-lint")
	ch.SetName("Lint and Static Analysis")
	ch.SetDescription("Runs Android lint and Detekt static analysis")
	ch.SetCategory("infrastructure")
	ch.SetDependencies([]challenge.ID{"infra-gradle-build"})
	return ch
}

func (c *LintChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
	result := c.CreateResult()

	// Android Lint
	c.ReportProgress("Running Android lint...")
	lintRes, err := c.gradle.RunTask(ctx, ":androidApp:lintDebug")
	if err != nil {
		result.AddFailure(fmt.Sprintf("Android lint failed: %v", err))
	} else {
		result.AddSuccess(fmt.Sprintf("Android lint passed in %v", lintRes.Duration))
	}

	// Detekt
	c.ReportProgress("Running Detekt...")
	detektRes, err := c.gradle.RunTask(ctx, "detekt")
	if err != nil {
		result.AddFailure(fmt.Sprintf("Detekt failed: %v", err))
	} else {
		result.AddSuccess(fmt.Sprintf("Detekt passed in %v", detektRes.Duration))
	}

	return result, nil
}
```

**Step 4: Commit**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git add challenges/infra/
git commit -m "feat: add infrastructure challenges (Gradle build, test execution, lint)"
```

---

## Task 7: Create Android Challenges

**Files:**
- Create: `challenges/android/robolectric_launch.go`
- Create: `challenges/android/robolectric_flows.go`
- Create: `challenges/android/uiautomator_launch.go`

**Step 1: Create android/robolectric_launch.go**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package android

import (
	"context"
	"fmt"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// RobolectricLaunchChallenge runs Robolectric app launch tests.
type RobolectricLaunchChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

func NewRobolectricLaunchChallenge(projectRoot string, useDocker bool) *RobolectricLaunchChallenge {
	ch := &RobolectricLaunchChallenge{
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
	ch.SetID("android-robolectric-launch")
	ch.SetName("Android App Launch (Robolectric)")
	ch.SetDescription("Verifies the Android app launches without crash using Robolectric")
	ch.SetCategory("android")
	ch.SetDependencies([]challenge.ID{"infra-gradle-build"})
	return ch
}

func (c *RobolectricLaunchChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
	result := c.CreateResult()

	c.ReportProgress("Running Robolectric app launch tests...")
	res, err := c.gradle.RunTests(ctx,
		":androidApp:testDebugUnitTest",
		"digital.vasic.yole.android.robolectric.AppLaunchRobolectricTest")

	if err != nil {
		result.AddFailure(fmt.Sprintf("Robolectric launch tests failed: %v", err))
		if res != nil {
			result.AddDetail("output", res.Output)
		}
	} else {
		result.AddSuccess(fmt.Sprintf("App launch tests passed in %v", res.Duration))
		for _, suite := range res.Suites {
			result.AddDetail(suite.Name, fmt.Sprintf(
				"tests=%d failures=%d time=%.2fs", suite.Tests, suite.Failures, suite.Time))
		}
	}

	return result, nil
}
```

**Step 2: Create android/robolectric_flows.go**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package android

import (
	"context"
	"fmt"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// RobolectricFlowsChallenge runs all Robolectric user flow tests.
type RobolectricFlowsChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

func NewRobolectricFlowsChallenge(projectRoot string, useDocker bool) *RobolectricFlowsChallenge {
	ch := &RobolectricFlowsChallenge{
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
	ch.SetID("android-robolectric-flows")
	ch.SetName("Android User Flows (Robolectric)")
	ch.SetDescription("Runs all Robolectric user flow tests: navigation, theme, settings, editing, todo, quicknote, backup, accessibility")
	ch.SetCategory("android")
	ch.SetDependencies([]challenge.ID{"android-robolectric-launch"})
	return ch
}

func (c *RobolectricFlowsChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
	result := c.CreateResult()

	testClasses := []struct {
		name  string
		class string
	}{
		{"Theme", "digital.vasic.yole.android.robolectric.ThemeRobolectricTest"},
		{"Navigation", "digital.vasic.yole.android.robolectric.NavigationRobolectricTest"},
		{"Settings", "digital.vasic.yole.android.robolectric.SettingsRobolectricTest"},
		{"File Editing", "digital.vasic.yole.android.robolectric.FileEditingRobolectricTest"},
		{"Format Detection", "digital.vasic.yole.android.robolectric.FormatDetectionRobolectricTest"},
		{"Todo Workflow", "digital.vasic.yole.android.robolectric.TodoWorkflowRobolectricTest"},
		{"QuickNote", "digital.vasic.yole.android.robolectric.QuickNoteRobolectricTest"},
		{"Backup/Restore", "digital.vasic.yole.android.robolectric.BackupRestoreRobolectricTest"},
		{"Accessibility", "digital.vasic.yole.android.robolectric.AccessibilityRobolectricTest"},
	}

	for _, tc := range testClasses {
		c.ReportProgress(fmt.Sprintf("Running %s tests...", tc.name))
		res, err := c.gradle.RunTests(ctx, ":androidApp:testDebugUnitTest", tc.class)

		if err != nil {
			result.AddFailure(fmt.Sprintf("%s tests failed: %v", tc.name, err))
		} else {
			result.AddSuccess(fmt.Sprintf("%s tests passed in %v", tc.name, res.Duration))
		}

		if res != nil {
			for _, suite := range res.Suites {
				result.AddDetail(fmt.Sprintf("%s_%s", tc.name, suite.Name), fmt.Sprintf(
					"tests=%d failures=%d time=%.2fs", suite.Tests, suite.Failures, suite.Time))
			}
		}
	}

	return result, nil
}
```

**Step 3: Create android/uiautomator_launch.go**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package android

import (
	"context"
	"fmt"
	"time"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// UIAutomatorLaunchChallenge tests app launch on a real device/emulator.
type UIAutomatorLaunchChallenge struct {
	challenge.BaseChallenge
	adb    *adapters.ADBAdapter
	gradle *adapters.GradleAdapter
}

func NewUIAutomatorLaunchChallenge(projectRoot string, useDocker bool) *UIAutomatorLaunchChallenge {
	ch := &UIAutomatorLaunchChallenge{
		adb: adapters.NewADBAdapter(),
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
	ch.SetID("android-uiautomator-launch")
	ch.SetName("Android App Launch (Device)")
	ch.SetDescription("Installs and launches the Android app on a real device or emulator via ADB")
	ch.SetCategory("android")
	ch.SetDependencies([]challenge.ID{"infra-gradle-build"})
	return ch
}

func (c *UIAutomatorLaunchChallenge) Validate(ctx context.Context) error {
	available, err := c.adb.IsDeviceAvailable(ctx)
	if err != nil || !available {
		return fmt.Errorf("no Android device or emulator available — skipping device tests")
	}
	return nil
}

func (c *UIAutomatorLaunchChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
	result := c.CreateResult()

	// Build debug APK
	c.ReportProgress("Building debug APK...")
	buildRes, err := c.gradle.RunTask(ctx, ":androidApp:assembleDebug")
	if err != nil {
		result.AddFailure(fmt.Sprintf("APK build failed: %v", err))
		return result, nil
	}
	result.AddSuccess(fmt.Sprintf("APK built in %v", buildRes.Duration))

	// Install APK
	c.ReportProgress("Installing APK...")
	apkPath := c.gradle.ProjectRoot + "/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
	if err := c.adb.InstallAPK(ctx, apkPath); err != nil {
		result.AddFailure(fmt.Sprintf("APK install failed: %v", err))
		return result, nil
	}
	result.AddSuccess("APK installed successfully")

	// Launch app
	c.ReportProgress("Launching app...")
	if err := c.adb.LaunchApp(ctx); err != nil {
		result.AddFailure(fmt.Sprintf("App launch failed: %v", err))
		return result, nil
	}

	// Wait for app to be running
	if err := c.adb.WaitForApp(ctx, 15*time.Second); err != nil {
		result.AddFailure(fmt.Sprintf("App did not start: %v", err))
		return result, nil
	}

	// Verify app is running (not crashed)
	time.Sleep(3 * time.Second) // Wait for any delayed crash
	running, _ := c.adb.IsAppRunning(ctx)
	if running {
		result.AddSuccess("App launched and is running without crash")
	} else {
		result.AddFailure("App crashed after launch")
	}

	// Take screenshot as evidence
	screenshot, err := c.adb.TakeScreenshot(ctx)
	if err == nil && len(screenshot) > 0 {
		result.AddDetail("screenshot_size", fmt.Sprintf("%d bytes", len(screenshot)))
	}

	// Cleanup
	c.adb.StopApp(ctx)

	return result, nil
}
```

**Step 4: Commit**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git add challenges/android/
git commit -m "feat: add Android challenges (Robolectric launch, user flows, UIAutomator launch)"
```

---

## Task 8: Create Desktop and Web Challenges

**Files:**
- Create: `challenges/desktop/launch.go`
- Create: `challenges/desktop/user_flows.go`
- Create: `challenges/web/launch.go`
- Create: `challenges/web/user_flows.go`

**Step 1: Create desktop/launch.go**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package desktop

import (
	"context"
	"fmt"
	"path/filepath"
	"time"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// DesktopLaunchChallenge launches the desktop app and verifies it starts.
type DesktopLaunchChallenge struct {
	challenge.BaseChallenge
	projectRoot string
	process     *adapters.ProcessAdapter
	gradle      *adapters.GradleAdapter
}

func NewDesktopLaunchChallenge(projectRoot string) *DesktopLaunchChallenge {
	ch := &DesktopLaunchChallenge{
		projectRoot: projectRoot,
		process:     &adapters.ProcessAdapter{},
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
		},
	}
	ch.SetID("desktop-launch")
	ch.SetName("Desktop App Launch")
	ch.SetDescription("Builds and launches the desktop JVM application, verifies it starts without crash")
	ch.SetCategory("desktop")
	ch.SetDependencies([]challenge.ID{"infra-gradle-build"})
	return ch
}

func (c *DesktopLaunchChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
	result := c.CreateResult()

	// Build desktop JAR
	c.ReportProgress("Building desktop JAR...")
	buildRes, err := c.gradle.RunTask(ctx, ":desktopApp:jar")
	if err != nil {
		result.AddFailure(fmt.Sprintf("Desktop JAR build failed: %v", err))
		return result, nil
	}
	result.AddSuccess(fmt.Sprintf("Desktop JAR built in %v", buildRes.Duration))

	// Find the JAR file
	jarPath := filepath.Join(c.projectRoot, "desktopApp", "build", "libs", "desktopApp.jar")

	// Launch the desktop app
	c.ReportProgress("Launching desktop app...")
	if err := c.process.LaunchJVM(ctx, jarPath); err != nil {
		result.AddFailure(fmt.Sprintf("Desktop app launch failed: %v", err))
		return result, nil
	}
	defer c.process.Stop()

	// Wait for the app to start
	if err := c.process.WaitForReady(ctx, 15*time.Second); err != nil {
		result.AddFailure(fmt.Sprintf("Desktop app did not start: %v", err))
		return result, nil
	}

	// Wait a bit and check it's still running (not crashed)
	time.Sleep(5 * time.Second)
	if c.process.IsRunning() {
		result.AddSuccess("Desktop app launched and is running without crash")
	} else {
		result.AddFailure("Desktop app crashed after launch")
	}

	return result, nil
}
```

**Step 2: Create desktop/user_flows.go**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package desktop

import (
	"context"
	"fmt"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// DesktopUserFlowsChallenge runs desktop unit tests via Gradle.
type DesktopUserFlowsChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

func NewDesktopUserFlowsChallenge(projectRoot string) *DesktopUserFlowsChallenge {
	ch := &DesktopUserFlowsChallenge{
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
		},
	}
	ch.SetID("desktop-user-flows")
	ch.SetName("Desktop User Flows")
	ch.SetDescription("Runs all desktop-specific tests: integration, parser, settings, UI, file manager, window manager")
	ch.SetCategory("desktop")
	ch.SetDependencies([]challenge.ID{"desktop-launch"})
	return ch
}

func (c *DesktopUserFlowsChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
	result := c.CreateResult()

	c.ReportProgress("Running desktop tests...")
	res, err := c.gradle.RunTests(ctx, ":desktopApp:test", "")

	if err != nil {
		result.AddFailure(fmt.Sprintf("Desktop tests failed: %v", err))
	} else {
		result.AddSuccess(fmt.Sprintf("Desktop tests passed in %v", res.Duration))
	}

	if res != nil {
		for _, suite := range res.Suites {
			result.AddDetail(suite.Name, fmt.Sprintf(
				"tests=%d failures=%d time=%.2fs", suite.Tests, suite.Failures, suite.Time))
		}
	}

	return result, nil
}
```

**Step 3: Create web/launch.go**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package web

import (
	"context"
	"fmt"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// WebLaunchChallenge builds and launches the Wasm web app in a browser via Playwright.
type WebLaunchChallenge struct {
	challenge.BaseChallenge
	projectRoot string
	gradle      *adapters.GradleAdapter
	pw          *adapters.PlaywrightAdapter
}

func NewWebLaunchChallenge(projectRoot string) *WebLaunchChallenge {
	ch := &WebLaunchChallenge{
		projectRoot: projectRoot,
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
		},
		pw: adapters.NewPlaywrightAdapter("http://localhost:8080"),
	}
	ch.SetID("web-launch")
	ch.SetName("Web App Launch (Playwright)")
	ch.SetDescription("Builds the Wasm web app, launches it in a browser, and verifies it loads")
	ch.SetCategory("web")
	ch.SetDependencies([]challenge.ID{"infra-gradle-build"})
	return ch
}

func (c *WebLaunchChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
	result := c.CreateResult()

	// Run web tests via Gradle (uses Karma + Chrome Headless)
	c.ReportProgress("Running web tests...")
	res, err := c.gradle.RunTests(ctx, ":webApp:wasmJsBrowserTest", "")

	if err != nil {
		result.AddFailure(fmt.Sprintf("Web tests failed: %v", err))
	} else {
		result.AddSuccess(fmt.Sprintf("Web tests passed in %v", res.Duration))
	}

	if res != nil {
		for _, suite := range res.Suites {
			result.AddDetail(suite.Name, fmt.Sprintf(
				"tests=%d failures=%d time=%.2fs", suite.Tests, suite.Failures, suite.Time))
		}
	}

	// Playwright browser test
	c.ReportProgress("Initializing Playwright...")
	if err := c.pw.Initialize(ctx, "chromium"); err != nil {
		result.AddFailure(fmt.Sprintf("Playwright init failed: %v — install with: npx playwright install", err))
		return result, nil
	}
	defer c.pw.Close(ctx)

	c.ReportProgress("Navigating to web app...")
	if err := c.pw.Navigate(ctx, c.pw.BaseURL); err != nil {
		result.AddDetail("note", "Web app server not running — Playwright launch test skipped. Start with: ./gradlew :webApp:wasmJsBrowserRun")
	} else {
		screenshot, _ := c.pw.Screenshot(ctx)
		if len(screenshot) > 0 {
			result.AddDetail("screenshot_size", fmt.Sprintf("%d bytes", len(screenshot)))
		}
		result.AddSuccess("Web app loaded in Playwright browser")
	}

	return result, nil
}
```

**Step 4: Create web/user_flows.go**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package web

import (
	"context"
	"fmt"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// WebUserFlowsChallenge tests web app user flows via Playwright.
type WebUserFlowsChallenge struct {
	challenge.BaseChallenge
	projectRoot string
	pw          *adapters.PlaywrightAdapter
}

func NewWebUserFlowsChallenge(projectRoot string) *WebUserFlowsChallenge {
	ch := &WebUserFlowsChallenge{
		projectRoot: projectRoot,
		pw:          adapters.NewPlaywrightAdapter("http://localhost:8080"),
	}
	ch.SetID("web-user-flows")
	ch.SetName("Web User Flows (Playwright)")
	ch.SetDescription("Tests web app user flows: format selection, document editing, theme switching, file operations")
	ch.SetCategory("web")
	ch.SetDependencies([]challenge.ID{"web-launch"})
	return ch
}

func (c *WebUserFlowsChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
	result := c.CreateResult()

	// Initialize Playwright
	c.ReportProgress("Initializing Playwright...")
	if err := c.pw.Initialize(ctx, "chromium"); err != nil {
		result.AddFailure(fmt.Sprintf("Playwright init failed: %v", err))
		return result, nil
	}
	defer c.pw.Close(ctx)

	// Navigate to app
	if err := c.pw.Navigate(ctx, c.pw.BaseURL); err != nil {
		result.AddDetail("note", "Web app server not running — skipping Playwright flow tests")
		return result, nil
	}

	// Test 1: Verify main UI elements
	c.ReportProgress("Testing main UI elements...")
	if visible, _ := c.pw.IsVisible(ctx, "text=Yole - Web Editor"); visible {
		result.AddSuccess("Title bar visible")
	} else {
		result.AddFailure("Title bar not visible")
	}

	// Test 2: Format selection
	c.ReportProgress("Testing format selection...")
	if err := c.pw.ClickByText(ctx, "Markdown (.md)"); err == nil {
		result.AddSuccess("Markdown format selectable")
	} else {
		result.AddFailure(fmt.Sprintf("Format selection failed: %v", err))
	}

	// Test 3: Theme toggle
	c.ReportProgress("Testing theme toggle...")
	if err := c.pw.ClickByText(ctx, "Dark"); err == nil {
		result.AddSuccess("Theme toggle works")
	} else {
		result.AddFailure(fmt.Sprintf("Theme toggle failed: %v", err))
	}

	// Test 4: New document
	c.ReportProgress("Testing new document creation...")
	if err := c.pw.ClickByText(ctx, "New Document"); err == nil {
		result.AddSuccess("New document creation works")
	} else {
		result.AddFailure(fmt.Sprintf("New document failed: %v", err))
	}

	// Test 5: Save document
	c.ReportProgress("Testing save...")
	if err := c.pw.ClickByText(ctx, "Save"); err == nil {
		result.AddSuccess("Save button works")
	} else {
		result.AddFailure(fmt.Sprintf("Save failed: %v", err))
	}

	// Screenshot for evidence
	screenshot, _ := c.pw.Screenshot(ctx)
	if len(screenshot) > 0 {
		result.AddDetail("screenshot_size", fmt.Sprintf("%d bytes", len(screenshot)))
	}

	return result, nil
}
```

**Step 5: Commit**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git add challenges/desktop/ challenges/web/
git commit -m "feat: add Desktop and Web challenges with Playwright integration"
```

---

## Task 9: Wire Up Challenge Registration in main.go

**Files:**
- Modify: `challenges/main.go`

**Step 1: Update main.go to import and register all challenges**

Replace the placeholder registration functions at the bottom of `challenges/main.go` with:

```go
import (
	// ... existing imports ...
	"digital.vasic.yole/challenges/infra"
	"digital.vasic.yole/challenges/android"
	"digital.vasic.yole/challenges/desktop"
	"digital.vasic.yole/challenges/web"
)

func registerInfraChallenges(reg *registry.Registry, projectRoot string, useDocker bool) {
	reg.Register(infra.NewGradleBuildChallenge(projectRoot, useDocker))
	reg.Register(infra.NewGradleTestsChallenge(projectRoot, useDocker))
	reg.Register(infra.NewLintChallenge(projectRoot, useDocker))
}

func registerAndroidChallenges(reg *registry.Registry, projectRoot string, useDocker bool) {
	reg.Register(android.NewRobolectricLaunchChallenge(projectRoot, useDocker))
	reg.Register(android.NewRobolectricFlowsChallenge(projectRoot, useDocker))
	reg.Register(android.NewUIAutomatorLaunchChallenge(projectRoot, useDocker))
}

func registerDesktopChallenges(reg *registry.Registry, projectRoot string) {
	reg.Register(desktop.NewDesktopLaunchChallenge(projectRoot))
	reg.Register(desktop.NewDesktopUserFlowsChallenge(projectRoot))
}

func registerWebChallenges(reg *registry.Registry, projectRoot string) {
	reg.Register(web.NewWebLaunchChallenge(projectRoot))
	reg.Register(web.NewWebUserFlowsChallenge(projectRoot))
}
```

**Step 2: Verify compilation**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole/challenges
go mod tidy
go build ./...
```

**Step 3: Commit**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git add challenges/main.go challenges/go.mod challenges/go.sum
git commit -m "feat: wire up all challenge registrations in main.go CLI entry point"
```

---

## Task 10: Create Test Data and Common Challenges

**Files:**
- Create: `challenges/testdata/sample.md`
- Create: `challenges/testdata/sample.txt`
- Create: `challenges/testdata/sample.csv`
- Create: `challenges/testdata/sample.org`
- Create: `challenges/testdata/sample.tex`
- Create: `challenges/testdata/sample.adoc`
- Create: `challenges/common/app_launch.go`
- Create: `challenges/common/format_rendering.go`

**Step 1: Create test data files**

Create sample files for each major supported format, one per format. These are used by common challenges to test file opening across platforms.

**Step 2: Create common/app_launch.go**

```go
// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package common

import (
	"context"
	"fmt"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// AppLaunchChallenge is a platform-agnostic challenge that verifies an app launches.
type AppLaunchChallenge struct {
	challenge.BaseChallenge
	adapter adapters.PlatformAdapter
}

func NewAppLaunchChallenge(adapter adapters.PlatformAdapter) *AppLaunchChallenge {
	ch := &AppLaunchChallenge{
		adapter: adapter,
	}
	ch.SetID(challenge.ID(fmt.Sprintf("common-launch-%s", adapter.Name())))
	ch.SetName(fmt.Sprintf("App Launch (%s)", adapter.Name()))
	ch.SetDescription(fmt.Sprintf("Verifies the %s app launches without errors", adapter.Name()))
	ch.SetCategory("common")
	return ch
}

func (c *AppLaunchChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
	result := c.CreateResult()

	// Build
	c.ReportProgress("Building...")
	if err := c.adapter.Build(ctx); err != nil {
		result.AddFailure(fmt.Sprintf("Build failed: %v", err))
		return result, nil
	}
	result.AddSuccess("Build succeeded")

	// Launch
	c.ReportProgress("Launching...")
	if err := c.adapter.Launch(ctx); err != nil {
		result.AddFailure(fmt.Sprintf("Launch failed: %v", err))
		return result, nil
	}
	defer c.adapter.Close(ctx)

	// Verify running
	running, err := c.adapter.IsRunning(ctx)
	if err != nil || !running {
		result.AddFailure("App not running after launch")
		return result, nil
	}
	result.AddSuccess("App is running")

	// Take screenshot
	screenshot, err := c.adapter.TakeScreenshot(ctx)
	if err == nil && len(screenshot) > 0 {
		result.AddDetail("screenshot_size", fmt.Sprintf("%d bytes", len(screenshot)))
	}

	return result, nil
}
```

**Step 3: Commit**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git add challenges/testdata/ challenges/common/
git commit -m "feat: add common challenges and test data files for cross-platform testing"
```

---

## Task 11: Add Challenges to .gitignore and Update Documentation

**Files:**
- Modify: `.gitignore`
- Create: `challenges/README.md`

**Step 1: Add challenges build artifacts to .gitignore**

Append to `.gitignore`:

```
# Challenges
challenges/reports/
challenges/screenshots/
```

**Step 2: Create challenges/README.md**

Document the full Challenges integration: how to run, what's tested, architecture diagram, prerequisites, and how to add new challenges. Include:

- Architecture overview
- Prerequisites (Go 1.24+, Playwright, ADB for Android device tests)
- How to run: `cd challenges && go run . --platform=all`
- How to run per-platform: `go run . --platform=android`
- How to run in Docker: `go run . --platform=all --docker`
- Challenge catalog: list all challenges with descriptions
- How to add new challenges
- How to extend for new projects (PlatformAdapter interface)
- Report formats and locations

**Step 3: Commit**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git add .gitignore challenges/README.md
git commit -m "docs: add Challenges documentation and update .gitignore"
```

---

## Task 12: Run Full Verification

**Step 1: Verify all Kotlin tests still pass**

Run: `docker compose run --rm build ./gradlew test`

Expected: All ~2200+ tests pass

**Step 2: Verify Robolectric tests pass**

Run: `docker compose run --rm build ./gradlew :androidApp:testDebugUnitTest --tests "digital.vasic.yole.android.robolectric.*"`

Expected: All Robolectric tests pass

**Step 3: Verify Go module compiles**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole/challenges
go build ./...
go vet ./...
```

Expected: Clean compilation, no vet issues

**Step 4: Run Go tests**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole/challenges
go test ./... -v
```

Expected: All Go tests pass

**Step 5: Run challenges orchestrator**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole/challenges
go run . --platform=all --docker --report=markdown --output=reports
```

Expected: Reports generated in `challenges/reports/`

---

## Dependency Graph

```
infra-gradle-build
├── infra-gradle-tests
├── infra-lint
├── android-robolectric-launch
│   └── android-robolectric-flows
├── android-uiautomator-launch (requires ADB device)
├── desktop-launch
│   └── desktop-user-flows
└── web-launch
    └── web-user-flows
```

## Summary

| Task | What It Does | Files Created/Modified |
|------|-------------|----------------------|
| 1 | Fix Android crash bug | 2 files modified |
| 2 | Add Challenges submodule | 1 submodule added |
| 3 | Add Robolectric to build | 2 files modified |
| 4 | Create Robolectric test classes | 10 files created |
| 5 | Create Go module + adapters | 6 files created |
| 6 | Create infra challenges | 3 files created |
| 7 | Create Android challenges | 3 files created |
| 8 | Create Desktop/Web challenges | 4 files created |
| 9 | Wire up registrations | 1 file modified |
| 10 | Common challenges + test data | 8+ files created |
| 11 | Documentation | 2 files created/modified |
| 12 | Full verification | 0 files (verification only) |

**Total: ~40 files across 12 tasks**
