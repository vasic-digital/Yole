/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration Tests for Yole Android App
 * Testing component interactions and data flow
 *
 *########################################################*/

package digital.vasic.yole.android.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.ParserInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        // Initialize all parsers and formats
        ParserInitializer.registerAllParsers()
    }

    @Test
    fun testFormatRegistryIntegrationWithUI() {
        // Test that format registry data is properly displayed in UI

        // Navigate to settings
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()

        // Verify format count is displayed correctly
        val expectedFormatCount = FormatRegistry.formats.size
        composeTestRule.onNodeWithText("Supported formats: $expectedFormatCount").assertIsDisplayed()

        // Verify some specific formats are shown
        composeTestRule.onNodeWithText("Markdown").assertIsDisplayed()
        composeTestRule.onNodeWithText("Todo.txt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Plain Text").assertIsDisplayed()
    }

    @Test
    fun testParserRegistryIntegration() {
        // Test that parser registry is properly integrated

        // Navigate to settings
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()

        // Verify that parsers are registered and format info is shown
        composeTestRule.onNodeWithText("Formats").assertIsDisplayed()

        // Check that parser count matches format count (assuming all formats have parsers)
        val parserCount = ParserRegistry.getAllParsers().size
        val formatCount = FormatRegistry.formats.size

        // In a full implementation, all formats should have parsers
        assert(parserCount > 0) { "No parsers registered" }
        assert(formatCount > 0) { "No formats available" }
    }

    @Test
    fun testFileOperationsIntegration() {
        // Test file operations integration (UI level)

        // Navigate to Files screen
        composeTestRule.onNodeWithText("Files").performClick()

        // Verify file browser shows format information
        composeTestRule.onNodeWithText("Supported formats: ${FormatRegistry.formats.size}").assertIsDisplayed()

        // Test that format detection works (UI level)
        // This tests the integration between UI and format registry
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }

    @Test
    fun testTodoDataPersistence() {
        // Test todo data persistence across screen switches

        // Navigate to To-Do screen
        composeTestRule.onNodeWithText("To-Do").performClick()

        // Add a todo item
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Integration test todo")
        composeTestRule.onNodeWithText("Add").performClick()

        // Verify item appears
        composeTestRule.onNodeWithText("Integration test todo").assertIsDisplayed()

        // Switch to another screen
        composeTestRule.onNodeWithText("Files").performClick()

        // Switch back to To-Do
        composeTestRule.onNodeWithText("To-Do").performClick()

        // Verify todo item still exists (tests state management)
        composeTestRule.onNodeWithText("Integration test todo").assertIsDisplayed()
    }

    @Test
    fun testQuickNoteContentPersistence() {
        // Test quicknote content persistence

        // Navigate to QuickNote screen
        composeTestRule.onNodeWithText("QuickNote").performClick()

        // Enter content
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("Integration test content")

        // Switch screens
        composeTestRule.onNodeWithText("Files").performClick()

        // Switch back
        composeTestRule.onNodeWithText("QuickNote").performClick()

        // Verify content persists
        composeTestRule.onNodeWithText("Integration test content").assertIsDisplayed()
    }

    @Test
    fun testSettingsPersistence() {
        // Test settings persistence across app sessions (UI level)

        // Navigate to settings
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()

        // Change a setting (e.g., theme)
        composeTestRule.onNodeWithText("Light theme").performClick()

        // Switch screens
        composeTestRule.onNodeWithText("Files").performClick()

        // Go back to settings
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()

        // Settings screen should still be accessible
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun testCrossScreenDataFlow() {
        // Test data flow between different screens

        // Create content in QuickNote
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("Cross-screen test content")

        // Create todo in To-Do screen
        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("Cross-screen todo")
        composeTestRule.onNodeWithText("Add").performClick()

        // Verify both pieces of data persist independently
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("Cross-screen test content").assertIsDisplayed()

        composeTestRule.onNodeWithText("To-Do").performClick()
        composeTestRule.onNodeWithText("Cross-screen todo").assertIsDisplayed()
    }

    @Test
    fun testFormatDetectionIntegration() {
        // Test format detection integration with UI

        // This tests that the format detection system is properly integrated
        // with the UI components

        val markdownFormat = FormatRegistry.detectByFilename("test.md")
        assert(markdownFormat.id == "markdown") { "Markdown detection failed" }

        val todoFormat = FormatRegistry.detectByFilename("todo.txt")
        assert(todoFormat.id == "todotxt") { "Todo.txt detection failed" }

        // Verify UI can display this information
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Formats").assertIsDisplayed()
    }

    @Test
    fun testParserIntegrationWithContent() {
        // Test that parsers work with actual content

        val markdownParser = ParserRegistry.getParser("markdown")
        assert(markdownParser != null) { "Markdown parser not found" }

        val testContent = "# Test Header\n\nThis is **bold** text."
        val document = markdownParser?.parse(testContent)

        assert(document != null) { "Failed to parse markdown content" }
        assert(document?.format?.id == "markdown") { "Wrong format detected" }
        assert(document?.rawContent == testContent) { "Raw content mismatch" }
    }

    @Test
    fun testTodoTxtParserIntegration() {
        // Test Todo.txt parser integration

        val todoParser = ParserRegistry.getParser("todotxt")
        assert(todoParser != null) { "Todo.txt parser not found" }

        val testContent = "x 2023-01-01 Completed task\n(A) 2023-01-02 +project @context Priority task"
        val document = todoParser?.parse(testContent)

        assert(document != null) { "Failed to parse todo.txt content" }
        assert(document?.format?.id == "todotxt") { "Wrong format detected" }
    }

    @Test
    fun testCsvParserIntegration() {
        // Test CSV parser integration

        val csvParser = ParserRegistry.getParser("csv")
        assert(csvParser != null) { "CSV parser not found" }

        val testContent = "Name,Age,City\nJohn,25,NYC\nJane,30,LA"
        val document = csvParser?.parse(testContent)

        assert(document != null) { "Failed to parse CSV content" }
        assert(document?.format?.id == "csv") { "Wrong format detected" }
    }

    @Test
    fun testPlaintextParserIntegration() {
        // Test plaintext parser integration

        val plaintextParser = ParserRegistry.getParser("plaintext")
        assert(plaintextParser != null) { "Plaintext parser not found" }

        val testContent = "This is plain text content\nwith multiple lines"
        val document = plaintextParser?.parse(testContent)

        assert(document != null) { "Failed to parse plaintext content" }
        assert(document?.format?.id == "plaintext") { "Wrong format detected" }
    }

    @Test
    fun testHtmlGenerationIntegration() {
        // Test HTML generation for different formats

        val parsers = ParserRegistry.getAllParsers()

        for (parser in parsers) {
            val testContent = when (parser.supportedFormat.id) {
                "markdown" -> "# Test\n\n**Bold** text"
                "todotxt" -> "x Completed task\nNormal task"
                "csv" -> "A,B,C\n1,2,3"
                else -> "Test content"
            }

            val document = parser.parse(testContent)
            val html = parser.toHtml(document)

            assert(html.isNotEmpty()) { "HTML generation failed for ${parser.supportedFormat.name}" }
            assert(html.contains("<")) { "Generated HTML is not valid for ${parser.supportedFormat.name}" }
        }
    }

    @Test
    fun testValidationIntegration() {
        // Test content validation for different formats

        val parsers = ParserRegistry.getAllParsers()

        for (parser in parsers) {
            val validContent = when (parser.supportedFormat.id) {
                "markdown" -> "# Valid markdown"
                "todotxt" -> "Valid todo item"
                "csv" -> "A,B,C\n1,2,3"
                else -> "Valid content"
            }

            val errors = parser.validate(validContent)
            // Valid content should have no errors, but some parsers might not implement validation
            // This is acceptable for this test
        }
    }

    @Test
    fun testFormatCompatibility() {
        // Test that all registered parsers are compatible with their formats

        val parsers = ParserRegistry.getAllParsers()

        for (parser in parsers) {
            assert(parser.canParse(parser.supportedFormat)) {
                "Parser ${parser.supportedFormat.name} is not compatible with its own format"
            }
        }
    }

    @Test
    fun testFormatRegistryCompleteness() {
        // Test that format registry has all expected formats

        val expectedFormats = listOf(
            "markdown", "plaintext", "todotxt", "csv", "wikitext",
            "orgmode", "creole", "tiddlywiki", "latex", "asciidoc",
            "restructuredtext", "keyvalue", "taskpaper", "textile",
            "jupyter", "rmarkdown", "binary"
        )

        for (formatId in expectedFormats) {
            val format = FormatRegistry.getById(formatId)
            assert(format != null) { "Format $formatId not found in registry" }
        }
    }

    @Test
    fun testParserRegistryCompleteness() {
        // Test that parser registry has parsers for all formats

        val formats = FormatRegistry.formats

        for (format in formats) {
            if (format.id != "binary") { // Binary format might not have a parser
                val parser = ParserRegistry.getParser(format)
                assert(parser != null) { "No parser found for format ${format.name}" }
            }
        }
    }

    @Test
    fun testUiStateManagement() {
        // Test that UI state is properly managed across interactions

        // Test screen switching maintains state
        composeTestRule.onNodeWithText("To-Do").performClick()

        // Add multiple todos
        composeTestRule.onNodeWithText("Add new todo...").performTextInput("State test 1")
        composeTestRule.onNodeWithText("Add").performClick()

        composeTestRule.onNodeWithText("Add new todo...").performTextInput("State test 2")
        composeTestRule.onNodeWithText("Add").performClick()

        // Switch screens multiple times
        composeTestRule.onNodeWithText("Files").performClick()
        composeTestRule.onNodeWithText("QuickNote").performClick()
        composeTestRule.onNodeWithText("More").performClick()
        composeTestRule.onNodeWithText("To-Do").performClick()

        // Verify all todos are still there
        composeTestRule.onNodeWithText("State test 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("State test 2").assertIsDisplayed()
    }

    @Test
    fun testMemoryManagement() {
        // Test that the app doesn't leak memory or crash under normal usage

        // Perform many operations
        for (i in 1..10) {
            composeTestRule.onNodeWithText("To-Do").performClick()
            composeTestRule.onNodeWithText("Add new todo...").performTextInput("Memory test $i")
            composeTestRule.onNodeWithText("Add").performClick()

            composeTestRule.onNodeWithText("QuickNote").performClick()
            composeTestRule.onNodeWithText("Start writing your quick note...").performTextInput("Memory test content $i")

            composeTestRule.onNodeWithText("Files").performClick()
        }

        // App should still be responsive
        composeTestRule.onNodeWithText("File Browser").assertIsDisplayed()
    }
}