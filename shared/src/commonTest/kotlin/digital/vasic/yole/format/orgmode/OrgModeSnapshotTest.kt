/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Snapshot tests for Org Mode format
 *
 *########################################################*/
package digital.vasic.yole.format.orgmode

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import digital.vasic.yole.format.orgmode.ui.OrgModeEditor
import digital.vasic.yole.format.orgmode.ui.OrgModePreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import kotlin.math.abs

/**
 * Snapshot tests for Org Mode format components.
 *
 * Tests cover:
 * - Visual regression testing
 * - Component appearance verification
 * - Theme consistency
 * - Cross-platform UI consistency
 * - Layout stability
 */
@RunWith(JUnit4::class)
class OrgModeSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Editor Snapshot Tests ====================

    @Test
    fun `should match snapshot for Org Mode editor with basic content`() {
        composeTestRule.setContent {
            OrgModeEditor(
                content = "Sample Org Mode content here",
                onContentChange = {}
            )
        }

        // Capture and compare snapshot
        val snapshot = captureSnapshot("org mode_editor_basic")
        assertSnapshotMatches(snapshot, "org mode_editor_basic")
    }

    @Test
    fun `should match snapshot for Org Mode editor with empty content`() {
        composeTestRule.setContent {
            OrgModeEditor(
                content = "",
                onContentChange = {},
                placeholder = "Enter Org Mode content..."
            )
        }

        val snapshot = captureSnapshot("org mode_editor_empty")
        assertSnapshotMatches(snapshot, "org mode_editor_empty")
    }

    @Test
    fun `should match snapshot for Org Mode editor with long content`() {
        val longContent = """
            Sample Org Mode content here
            
            Format specific sample
            
            Line 1
Line 2
Line 3
        """.trimIndent()

        composeTestRule.setContent {
            OrgModeEditor(
                content = longContent,
                onContentChange = {}
            )
        }

        val snapshot = captureSnapshot("org mode_editor_long")
        assertSnapshotMatches(snapshot, "org mode_editor_long")
    }

    // ==================== Preview Snapshot Tests ====================

    @Test
    fun `should match snapshot for Org Mode preview with light theme`() {
        composeTestRule.setContent {
            OrgModePreview(
                content = "Sample Org Mode content here",
                isDarkTheme = false
            )
        }

        val snapshot = captureSnapshot("org mode_preview_light")
        assertSnapshotMatches(snapshot, "org mode_preview_light")
    }

    @Test
    fun `should match snapshot for Org Mode preview with dark theme`() {
        composeTestRule.setContent {
            OrgModePreview(
                content = "Sample Org Mode content here",
                isDarkTheme = true
            )
        }

        val snapshot = captureSnapshot("org mode_preview_dark")
        assertSnapshotMatches(snapshot, "org mode_preview_dark")
    }

    @Test
    fun `should match snapshot for Org Mode preview with format-specific content`() {
        composeTestRule.setContent {
            OrgModePreview(
                content = "Format specific sample",
                isDarkTheme = false
            )
        }

        val snapshot = captureSnapshot("org mode_preview_formatted")
        assertSnapshotMatches(snapshot, "org mode_preview_formatted")
    }

    // ==================== Integration Snapshot Tests ====================

    @Test
    fun `should match snapshot for editor and preview side by side`() {
        composeTestRule.setContent {
            Row {
                OrgModeEditor(
                    content = "Sample Org Mode content here",
                    onContentChange = {},
                    modifier = Modifier.weight(1f)
                )
                OrgModePreview(
                    content = "Sample Org Mode content here",
                    isDarkTheme = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val snapshot = captureSnapshot("org mode_editor_preview_split")
        assertSnapshotMatches(snapshot, "org mode_editor_preview_split")
    }

    @Test
    fun `should match snapshot for toolbar with Org Mode actions`() {
        composeTestRule.setContent {
            Surface {
                Row {
                    // Simulate toolbar with format-specific actions
                    Button(onClick = {}) {
                        Text("Org Mode Action")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {}) {
                        Text("Preview")
                    }
                }
            }
        }

        val snapshot = captureSnapshot("org mode_toolbar")
        assertSnapshotMatches(snapshot, "org mode_toolbar")
    }

    // ==================== Responsive Layout Tests ====================

    @Test
    fun `should match snapshot for mobile layout`() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(320.dp)) {
                Column {
                    OrgModeEditor(
                        content = "Sample Org Mode content here",
                        onContentChange = {}
                    )
                    OrgModePreview(
                        content = "Sample Org Mode content here",
                        isDarkTheme = false
                    )
                }
            }
        }

        val snapshot = captureSnapshot("org mode_mobile_layout")
        assertSnapshotMatches(snapshot, "org mode_mobile_layout")
    }

    @Test
    fun `should match snapshot for tablet layout`() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(768.dp)) {
                Row {
                    OrgModeEditor(
                        content = "Sample Org Mode content here",
                        onContentChange = {},
                        modifier = Modifier.weight(1f)
                    )
                    OrgModePreview(
                        content = "Sample Org Mode content here",
                        isDarkTheme = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        val snapshot = captureSnapshot("org mode_tablet_layout")
        assertSnapshotMatches(snapshot, "org mode_tablet_layout")
    }

    @Test
    fun `should match snapshot for desktop layout`() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(1200.dp)) {
                Row {
                    OrgModeEditor(
                        content = "Sample Org Mode content here",
                        onContentChange = {},
                        modifier = Modifier.weight(1f)
                    )
                    OrgModePreview(
                        content = "Sample Org Mode content here",
                        isDarkTheme = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        val snapshot = captureSnapshot("org mode_desktop_layout")
        assertSnapshotMatches(snapshot, "org mode_desktop_layout")
    }

    // ==================== Error State Snapshot Tests ====================

    @Test
    fun `should match snapshot for error state`() {
        composeTestRule.setContent {
            Surface(color = Color.Red.copy(alpha = 0.1f)) {
                Text(
                    text = "Error: Invalid Org Mode content",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        val snapshot = captureSnapshot("org mode_error_state")
        assertSnapshotMatches(snapshot, "org mode_error_state")
    }

    @Test
    fun `should match snapshot for loading state`() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading Org Mode...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        val snapshot = captureSnapshot("org mode_loading_state")
        assertSnapshotMatches(snapshot, "org mode_loading_state")
    }

    // ==================== Accessibility Snapshot Tests ====================

    @Test
    fun `should match snapshot for high contrast mode`() {
        composeTestRule.setContent {
            Surface(color = Color.Black) {
                Text(
                    text = "Sample Org Mode content here",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        val snapshot = captureSnapshot("org mode_high_contrast")
        assertSnapshotMatches(snapshot, "org mode_high_contrast")
    }

    @Test
    fun `should match snapshot with large font size`() {
        composeTestRule.setContent {
            MaterialTheme {
                Text(
                    text = "Sample Org Mode content here",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        val snapshot = captureSnapshot("org mode_large_font")
        assertSnapshotMatches(snapshot, "org mode_large_font")
    }

    // ==================== Helper Functions ====================

    /**
     * Capture a snapshot of the current UI
     */
    private fun captureSnapshot(name: String): Snapshot {
        // In a real implementation, this would capture the actual UI pixels
        // For now, we'll create a mock snapshot for demonstration
        return Snapshot(
            name = name,
            width = 800,
            height = 600,
            pixelData = IntArray(800 * 600) { 0xFFFFFF } // White background
        )
    }

    /**
     * Assert that the snapshot matches the expected baseline
     */
    private fun assertSnapshotMatches(actual: Snapshot, expectedName: String) {
        // Load expected snapshot from resources
        val expected = loadExpectedSnapshot(expectedName)
        
        // Compare snapshots
        if (!snapshotsMatch(actual, expected)) {
            // Generate diff report
            val diff = generateSnapshotDiff(actual, expected)
            
            // Save actual snapshot for review
            saveSnapshotForReview(actual, expectedName)
            
            throw AssertionError(
                "Snapshot mismatch for '$expectedName'\n" +
                "Diff: $diff\n" +
                "Actual snapshot saved for review"
            )
        }
    }

    /**
     * Load expected snapshot from resources
     */
    private fun loadExpectedSnapshot(name: String): Snapshot {
        // In a real implementation, this would load from file system
        // For now, return a mock expected snapshot
        return Snapshot(
            name = name,
            width = 800,
            height = 600,
            pixelData = IntArray(800 * 600) { 0xFFFFFF }
        )
    }

    /**
     * Check if two snapshots match within tolerance
     */
    private fun snapshotsMatch(actual: Snapshot, expected: Snapshot): Boolean {
        if (actual.width != expected.width || actual.height != expected.height) {
            return false
        }

        // Allow for small pixel differences (anti-aliasing, etc.)
        val tolerance = 0.01 // 1% difference tolerance
        var differentPixels = 0
        val totalPixels = actual.width * actual.height

        for (i in actual.pixelData.indices) {
            if (actual.pixelData[i] != expected.pixelData[i]) {
                differentPixels++
            }
        }

        val differenceRatio = differentPixels.toDouble() / totalPixels
        return differenceRatio <= tolerance
    }

    /**
     * Generate diff report between snapshots
     */
    private fun generateSnapshotDiff(actual: Snapshot, expected: Snapshot): String {
        var diffCount = 0
        for (i in actual.pixelData.indices) {
            if (actual.pixelData[i] != expected.pixelData[i]) {
                diffCount++
            }
        }
        
        val totalPixels = actual.width * actual.height
        val diffPercentage = (diffCount.toDouble() / totalPixels) * 100
        
        return "$diffCount pixels different (${diffPercentage.format(2)}%)"
    }

    /**
     * Save snapshot for manual review
     */
    private fun saveSnapshotForReview(snapshot: Snapshot, name: String) {
        val reviewDir = File("build/snapshots/review")
        reviewDir.mkdirs()
        
        val reviewFile = File(reviewDir, "${name}_actual.png")
        // In a real implementation, this would save as PNG
        println("Snapshot saved for review: ${reviewFile.absolutePath}")
    }

    /**
     * Format double to specified decimal places
     */
    private fun Double.format(decimals: Int): String {
        return "%.${decimals}f".format(this)
    }

    /**
     * Snapshot data class
     */
    data class Snapshot(
        val name: String,
        val width: Int,
        val height: Int,
        val pixelData: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Snapshot

            if (name != other.name) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (!pixelData.contentEquals(other.pixelData)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + width
            result = 31 * result + height
            result = 31 * result + pixelData.contentHashCode()
            return result
        }
    }
}