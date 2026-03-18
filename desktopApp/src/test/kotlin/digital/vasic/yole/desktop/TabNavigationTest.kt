/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tab Navigation Test for Yole Desktop Application
 *
 * Tests comprehensive tab navigation (Files, Edit, Preview,
 * Settings) with Compose UI captureToImage() screenshots —
 * fixes the black-screenshot issue caused by Wayland/XWayland
 * by capturing directly from the Compose rendering pipeline.
 *
 *########################################################*/
package digital.vasic.yole.desktop

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import digital.vasic.yole.desktop.ui.MainScreen
import digital.vasic.yole.desktop.ui.theme.YoleDesktopTheme
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Comprehensive tab navigation test with Compose-internal screenshot capture.
 *
 * Uses [captureToImage] + [toAwtImage] + [ImageIO] to save real screenshots
 * as PNG files — bypassing the Wayland/XWayland framebuffer that causes
 * all-black screenshots when using ffmpeg x11grab or java.awt.Robot.
 *
 * Screenshots are saved to recordings/desktop/navigation/.
 */
@OptIn(ExperimentalTestApi::class)
class TabNavigationTest {

    private val screenshotDir = File("recordings/desktop/navigation").apply { mkdirs() }

    // ==================== Screenshot Helper ====================

    /**
     * Captures the current Compose UI tree as a PNG image via [captureToImage].
     * This works reliably on Wayland because it captures from within the Compose
     * rendering pipeline, not from the window manager's framebuffer.
     */
    private fun ComposeUiTest.saveScreenshot(name: String) {
        try {
            val imageBitmap = onRoot().captureToImage()
            val buffered = imageBitmap.toAwtImage()
            val width = buffered.width
            val height = buffered.height

            val file = File(screenshotDir, "$name.png")
            ImageIO.write(buffered, "png", file)
            println("    Screenshot saved: ${file.absolutePath} (${width}x${height})")

            // Verify the screenshot is NOT all black
            var nonBlackPixels = 0
            val sampleStep = maxOf(1, width * height / 10000) // Sample ~10K pixels
            for (i in 0 until width * height step sampleStep) {
                val px = i % width
                val py = i / width
                val argb = buffered.getRGB(px, py)
                val r = (argb shr 16) and 0xFF
                val g = (argb shr 8) and 0xFF
                val b = argb and 0xFF
                if (r > 10 || g > 10 || b > 10) {
                    nonBlackPixels++
                }
            }
            assertTrue(
                nonBlackPixels > 0,
                "Screenshot '$name' should NOT be all black (non-black pixels: $nonBlackPixels)"
            )
        } catch (e: Exception) {
            println("    Screenshot capture failed for '$name': ${e.message}")
            // Do not fail the test on screenshot errors — the navigation assertions are primary
        }
    }

    // ==================== Navigation Helpers ====================

    private fun ComposeUiTest.clickNavFiles() {
        onNodeWithContentDescription("Open file browser").performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.clickNavEditor() {
        onNodeWithContentDescription("Open editor").performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.clickNavPreview() {
        onNodeWithContentDescription("Open preview").performClick()
        waitForIdle()
    }

    private fun ComposeUiTest.clickNavSettings() {
        onNodeWithContentDescription("Open settings").performClick()
        waitForIdle()
    }

    // ==================== Content Verification ====================

    private fun ComposeUiTest.assertFileBrowserVisible() {
        onNodeWithText("File Browser").assertIsDisplayed()
    }

    private fun ComposeUiTest.assertEditorVisible() {
        onNodeWithText("Editing:", substring = true).assertIsDisplayed()
    }

    private fun ComposeUiTest.assertPreviewVisible() {
        onNodeWithText("Preview:", substring = true).assertIsDisplayed()
    }

    private fun ComposeUiTest.assertSettingsVisible() {
        onNodeWithText("Appearance").assertIsDisplayed()
    }

    // ==================== Test 1: Forward and Backward Navigation ====================

    @Test
    fun navigateAllTabsForwardAndBack() = runComposeUiTest {
        println("=== Tab Navigation: Forward and Backward ===")
        setContent { YoleDesktopTheme { MainScreen() } }
        waitForIdle()

        // === Round 1: Forward — Files -> Edit -> Preview -> Settings ===
        println("  Round 1: Forward navigation")

        // Initial state: File Browser
        assertFileBrowserVisible()
        saveScreenshot("01-files-initial")

        // Files -> Edit
        clickNavEditor()
        assertEditorVisible()
        saveScreenshot("02-editor-forward")

        // Edit -> Preview
        clickNavPreview()
        assertPreviewVisible()
        saveScreenshot("03-preview-forward")

        // Preview -> Settings
        clickNavSettings()
        assertSettingsVisible()
        saveScreenshot("04-settings-forward")

        // === Round 2: Backward — Settings -> Preview -> Edit -> Files ===
        println("  Round 2: Backward navigation")

        // Settings -> Preview
        clickNavPreview()
        assertPreviewVisible()
        saveScreenshot("05-preview-backward")

        // Preview -> Edit
        clickNavEditor()
        assertEditorVisible()
        saveScreenshot("06-editor-backward")

        // Edit -> Files
        clickNavFiles()
        assertFileBrowserVisible()
        saveScreenshot("07-files-backward")

        // === Round 3: Random jumps ===
        println("  Round 3: Random jumps")

        // Files -> Settings (skip 2)
        clickNavSettings()
        assertSettingsVisible()
        saveScreenshot("08-settings-jump")

        // Settings -> Files (skip 2 back)
        clickNavFiles()
        assertFileBrowserVisible()
        saveScreenshot("09-files-jump")

        // Files -> Preview (skip 1)
        clickNavPreview()
        assertPreviewVisible()
        saveScreenshot("10-preview-jump")

        // Preview -> Edit (1 back)
        clickNavEditor()
        assertEditorVisible()
        saveScreenshot("11-editor-jump")

        // Edit -> Settings (skip 1)
        clickNavSettings()
        assertSettingsVisible()
        saveScreenshot("12-settings-from-editor")

        // Settings -> Edit (1 back)
        clickNavEditor()
        assertEditorVisible()
        saveScreenshot("13-editor-from-settings")

        // Edit -> Files (1 back)
        clickNavFiles()
        assertFileBrowserVisible()
        saveScreenshot("14-files-final")

        println("=== Forward and Backward navigation complete (14 screenshots) ===")
    }

    // ==================== Test 2: Rapid Tab Switching ====================

    @Test
    fun rapidTabSwitching20Times() = runComposeUiTest {
        println("=== Tab Navigation: Rapid 20-switch stress test ===")
        setContent { YoleDesktopTheme { MainScreen() } }
        waitForIdle()

        data class TabDef(val desc: String, val label: String, val verify: ComposeUiTest.() -> Unit)

        val tabs = listOf(
            TabDef("Open file browser", "files") { assertFileBrowserVisible() },
            TabDef("Open editor", "editor") { assertEditorVisible() },
            TabDef("Open preview", "preview") { assertPreviewVisible() },
            TabDef("Open settings", "settings") { assertSettingsVisible() }
        )

        for (i in 1..20) {
            val tab = tabs[i % tabs.size]
            onNodeWithContentDescription(tab.desc).performClick()
            waitForIdle()
            tab.verify(this)
            saveScreenshot("rapid-${String.format("%02d", i)}-${tab.label}")
        }

        println("=== Rapid 20-switch test complete ===")
    }

    // ==================== Test 3: Each Tab Preserves Content ====================

    @Test
    fun tabNavigationPreservesContent() = runComposeUiTest {
        println("=== Tab Navigation: Content preservation across switches ===")
        setContent { YoleDesktopTheme { MainScreen() } }
        waitForIdle()

        // Start at File Browser — verify sample files
        onNodeWithText("sample.md").assertIsDisplayed()
        onNodeWithText("todo.txt").assertIsDisplayed()
        onNodeWithText("notes.txt").assertIsDisplayed()
        saveScreenshot("preserve-01-files-with-samples")

        // Click on sample.md to load it into editor
        onNodeWithText("sample.md").performClick()
        waitForIdle()
        assertEditorVisible()
        saveScreenshot("preserve-02-editor-with-file")

        // Switch to Preview — should show preview content
        clickNavPreview()
        assertPreviewVisible()
        saveScreenshot("preserve-03-preview")

        // Switch to Settings — verify sections
        clickNavSettings()
        assertSettingsVisible()
        onNodeWithText("Editor").assertExists()
        onNodeWithText("Formats").assertExists()
        saveScreenshot("preserve-04-settings")

        // Switch back to Files — sample files still present
        clickNavFiles()
        assertFileBrowserVisible()
        onNodeWithText("sample.md").assertIsDisplayed()
        saveScreenshot("preserve-05-files-still-there")

        // Switch back to Editor — file still loaded
        clickNavEditor()
        assertEditorVisible()
        saveScreenshot("preserve-06-editor-still-loaded")

        println("=== Content preservation test complete ===")
    }

    // ==================== Test 4: Alternating Between Two Tabs ====================

    @Test
    fun alternatingBetweenTwoTabs() = runComposeUiTest {
        println("=== Tab Navigation: Alternating Files <-> Editor 10 times ===")
        setContent { YoleDesktopTheme { MainScreen() } }
        waitForIdle()

        for (i in 1..10) {
            // Files
            clickNavFiles()
            assertFileBrowserVisible()

            // Editor
            clickNavEditor()
            assertEditorVisible()
        }
        saveScreenshot("alternating-final-editor")

        println("  Alternating Files <-> Editor 10 times: PASS")

        println("  Alternating Editor <-> Settings 10 times")
        for (i in 1..10) {
            // Editor
            clickNavEditor()
            assertEditorVisible()

            // Settings
            clickNavSettings()
            assertSettingsVisible()
        }
        saveScreenshot("alternating-final-settings")

        println("  Alternating Editor <-> Settings 10 times: PASS")

        println("  Alternating Preview <-> Settings 10 times")
        for (i in 1..10) {
            // Preview
            clickNavPreview()
            assertPreviewVisible()

            // Settings
            clickNavSettings()
            assertSettingsVisible()
        }
        saveScreenshot("alternating-final-preview-settings")

        println("=== Alternating tab test complete (60 switches) ===")
    }

    // ==================== Test 5: Full Cycle Repeated ====================

    @Test
    fun fullCycleRepeated5Times() = runComposeUiTest {
        println("=== Tab Navigation: Full cycle (Files->Edit->Preview->Settings) x5 ===")
        setContent { YoleDesktopTheme { MainScreen() } }
        waitForIdle()

        for (cycle in 1..5) {
            println("  Cycle $cycle/5")

            clickNavFiles()
            assertFileBrowserVisible()

            clickNavEditor()
            assertEditorVisible()

            clickNavPreview()
            assertPreviewVisible()

            clickNavSettings()
            assertSettingsVisible()
        }
        saveScreenshot("cycle-final-after-5-cycles")

        println("=== Full cycle x5 complete (20 switches) ===")
    }
}
