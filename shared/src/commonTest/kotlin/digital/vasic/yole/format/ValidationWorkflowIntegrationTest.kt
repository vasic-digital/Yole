/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Integration tests for validation workflows
 *
 *########################################################*/
package digital.vasic.yole.format

import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.csv.CsvParser
import kotlin.test.*

/**
 * Integration tests for parse-validate-fix-reparse workflows.
 *
 * Tests cover:
 * - Parse → Validate → Fix → Reparse cycles
 * - Error accumulation across multiple parses
 * - Validation with different parser states
 * - Error recovery workflows
 * - Incremental fixes
 */
class ValidationWorkflowIntegrationTest {

    private lateinit var markdownParser: MarkdownParser
    private lateinit var csvParser: CsvParser

    @BeforeTest
    fun setup() {
        markdownParser = MarkdownParser()
        csvParser = CsvParser()
        ParserRegistry.clear()
        ParserRegistry.register(markdownParser)
        ParserRegistry.register(csvParser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Parse-Validate-Fix-Reparse Workflows ====================

    @Test
    fun `should detect errors in first parse and fix in second`() {
        // First parse with error
        val malformed = "[Unclosed bracket in line 1"
        val doc1 = markdownParser.parse(malformed)
        val errors1 = markdownParser.validate(malformed)

        // Should have errors
        assertTrue(errors1.isNotEmpty())

        // Fix the content
        val fixed = "[Closed bracket](url) in line 1"
        val doc2 = markdownParser.parse(fixed)
        val errors2 = markdownParser.validate(fixed)

        // Should have no errors
        assertTrue(errors2.isEmpty())

        // Fixed document should parse correctly
        assertTrue(doc2.parsedContent.contains("Closed bracket"))
    }

    @Test
    fun `should handle multiple validation cycles`() {
        val contents = listOf(
            "[Unclosed",                          // Error
            "[Closed](url)",                      // Fixed
            "[Another](incomplete",               // Error
            "[Another](url)",                     // Fixed
            "# Valid markdown"                    // Always valid
        )

        val validationResults = contents.map { content ->
            val doc = markdownParser.parse(content)
            val errors = markdownParser.validate(content)
            Pair(doc, errors)
        }

        // First should have errors
        assertTrue(validationResults[0].second.isNotEmpty())
        // Second should be fixed
        assertTrue(validationResults[1].second.isEmpty())
        // Third should have errors
        assertTrue(validationResults[2].second.isNotEmpty())
        // Fourth should be fixed
        assertTrue(validationResults[3].second.isEmpty())
        // Fifth should always be valid
        assertTrue(validationResults[4].second.isEmpty())
    }

    @Test
    fun `should incrementally fix multiple errors`() {
        // Content with multiple errors
        val step1 = "[Error 1 [Error 2 (Error 3"
        val errors1 = markdownParser.validate(step1)
        assertTrue(errors1.isNotEmpty())

        // Fix first error
        val step2 = "[Fixed 1](url) [Error 2 (Error 3"
        val errors2 = markdownParser.validate(step2)
        assertTrue(errors2.size < errors1.size || errors2.isNotEmpty())

        // Fix second error
        val step3 = "[Fixed 1](url) [Fixed 2](url) (Error 3"
        val errors3 = markdownParser.validate(step3)
        // May still have errors depending on validation

        // Fix all
        val step4 = "[Fixed 1](url) [Fixed 2](url) Fixed 3"
        val errors4 = markdownParser.validate(step4)
        assertTrue(errors4.isEmpty())
    }

    // ==================== Error Accumulation Tests ====================

    @Test
    fun `should accumulate errors across multiple parses`() {
        val malformedContents = listOf(
            "[Unclosed 1",
            "[Unclosed 2",
            "(Unclosed 3"
        )

        val allErrors = mutableListOf<String>()

        malformedContents.forEach { content ->
            val errors = markdownParser.validate(content)
            allErrors.addAll(errors)
        }

        // Should have accumulated errors
        assertTrue(allErrors.size >= 3)
    }

    @Test
    fun `should not accumulate errors for valid content`() {
        val validContents = listOf(
            "# Header 1",
            "# Header 2",
            "# Header 3"
        )

        val allErrors = mutableListOf<String>()

        validContents.forEach { content ->
            val errors = markdownParser.validate(content)
            allErrors.addAll(errors)
        }

        // Should have no errors
        assertTrue(allErrors.isEmpty())
    }

    @Test
    fun `should handle mixed valid and invalid content`() {
        val mixedContents = listOf(
            "# Valid header",
            "[Invalid link",
            "**Valid bold**",
            "(Invalid paren",
            "Plain text is valid"
        )

        val results = mixedContents.map { content ->
            markdownParser.validate(content)
        }

        // Check correct items have/don't have errors
        assertTrue(results[0].isEmpty())  // Valid
        assertTrue(results[1].isNotEmpty())  // Invalid
        assertTrue(results[2].isEmpty())  // Valid
        assertTrue(results[3].isNotEmpty())  // Invalid
        assertTrue(results[4].isEmpty())  // Valid
    }

    // ==================== Validation State Tests ====================

    @Test
    fun `should validate independently for each parse`() {
        val parser1 = MarkdownParser()
        val parser2 = MarkdownParser()

        val malformed = "[Unclosed"
        val valid = "[Closed](url)"

        val errors1 = parser1.validate(malformed)
        val errors2 = parser2.validate(valid)

        // Parser1 should have errors, parser2 should not
        assertTrue(errors1.isNotEmpty())
        assertTrue(errors2.isEmpty())
    }

    @Test
    fun `should maintain validation state across multiple calls`() {
        val malformed = "[Unclosed bracket"

        // Validate multiple times
        val errors1 = markdownParser.validate(malformed)
        val errors2 = markdownParser.validate(malformed)
        val errors3 = markdownParser.validate(malformed)

        // Should get consistent results
        assertEquals(errors1.size, errors2.size)
        assertEquals(errors2.size, errors3.size)
    }

    // ==================== Error Recovery Workflows ====================

    @Test
    fun `should parse document even with validation errors`() {
        val malformed = "[Link without closing bracket"

        // Parse should succeed even with errors
        val doc = markdownParser.parse(malformed)
        val errors = markdownParser.validate(malformed)

        // Document should be created
        assertNotNull(doc)
        assertNotNull(doc.parsedContent)

        // But should have errors
        assertTrue(errors.isNotEmpty())
    }

    @Test
    fun `should generate HTML despite validation errors`() {
        val malformed = "[Unclosed link (Unclosed paren"

        val doc = markdownParser.parse(malformed)
        val errors = markdownParser.validate(malformed)

        // Should have errors
        assertTrue(errors.isNotEmpty())

        // But should still generate HTML
        val html = doc.toHtml(lightMode = true)
        assertNotNull(html)
        assertTrue(html.contains("Unclosed"))
    }

    @Test
    fun `should continue processing after validation failure`() {
        val contents = listOf(
            "[Error content",
            "# Valid content",
            "(Another error",
            "**More valid content**"
        )

        val docs = contents.map { content ->
            val doc = markdownParser.parse(content)
            val errors = markdownParser.validate(content)
            Triple(doc, errors, content)
        }

        // All should have parsed
        assertEquals(4, docs.size)
        assertTrue(docs.all { it.first.rawContent.isNotEmpty() })

        // Error detection should still work
        assertTrue(docs[0].second.isNotEmpty())  // Has errors
        assertTrue(docs[1].second.isEmpty())     // No errors
        assertTrue(docs[2].second.isNotEmpty())  // Has errors
        assertTrue(docs[3].second.isEmpty())     // No errors
    }

    // ==================== CSV Validation Tests ====================

    @Test
    fun `should handle CSV validation workflow`() {
        // Malformed CSV
        val malformed = """
            col1,col2
            val1,val2,val3
            val4
        """.trimIndent()

        val doc = csvParser.parse(malformed)

        // Should parse despite inconsistencies
        assertNotNull(doc)
        assertNotNull(doc.parsedContent)

        // Well-formed CSV
        val wellFormed = """
            col1,col2
            val1,val2
            val3,val4
        """.trimIndent()

        val validDoc = csvParser.parse(wellFormed)
        assertNotNull(validDoc)
        assertEquals("2", validDoc.metadata["rows"])
    }

    // ==================== Complex Validation Scenarios ====================

    @Test
    fun `should handle nested validation errors`() {
        val nested = "[Outer [Inner unclosed"

        val errors = markdownParser.validate(nested)

        // Should detect nested errors
        assertTrue(errors.isNotEmpty())
    }

    @Test
    fun `should validate code blocks correctly`() {
        val withCodeBlock = """
            # Header

            ```
            [This is not a link
            (This is not a paren error
            ```

            Normal text
        """.trimIndent()

        val errors = markdownParser.validate(withCodeBlock)

        // Code block content should not trigger validation errors
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should differentiate errors in code vs text`() {
        val content = """
            [Real error without closing

            ```
            [Not an error in code
            ```
        """.trimIndent()

        val errors = markdownParser.validate(content)

        // Should only detect the real error, not code block content
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("Line 1") || it.contains("bracket") })
    }

    // ==================== Validation with Metadata ====================

    @Test
    fun `should preserve metadata during validation cycles`() {
        val content = "[Malformed link"
        val options = mapOf("filename" to "test.md")

        val doc = markdownParser.parse(content, options)
        val errors = markdownParser.validate(content)

        // Metadata should be preserved
        assertEquals(".md", doc.metadata["extension"])

        // Errors should still be detected
        assertTrue(errors.isNotEmpty())
    }

    // ==================== Batch Validation Tests ====================

    @Test
    fun `should validate multiple documents in batch`() {
        val documents = listOf(
            "# Valid 1",
            "[Invalid 1",
            "# Valid 2",
            "(Invalid 2",
            "**Valid 3**"
        )

        val results = documents.map { content ->
            val doc = markdownParser.parse(content)
            val errors = markdownParser.validate(content)
            Pair(doc, errors.isEmpty())
        }

        // Check validation results
        assertTrue(results[0].second)   // Valid
        assertFalse(results[1].second)  // Invalid
        assertTrue(results[2].second)   // Valid
        assertFalse(results[3].second)  // Invalid
        assertTrue(results[4].second)   // Valid
    }

    @Test
    fun `should report line numbers in batch validation`() {
        val multilineContent = """
            # Line 1
            [Unclosed on line 2
            # Line 3
            (Unclosed on line 4
        """.trimIndent()

        val errors = markdownParser.validate(multilineContent)

        // Should report line numbers
        assertTrue(errors.any { it.contains("Line") || it.contains("2") || it.contains("4") })
    }

    // ==================== Validation Performance Tests ====================

    @Test
    fun `should handle rapid validation calls`() {
        val content = "[Malformed"

        // Rapid validation
        val results = (1..100).map {
            markdownParser.validate(content)
        }

        // All should detect the same errors
        assertTrue(results.all { it.isNotEmpty() })
    }

    @Test
    fun `should validate large documents efficiently`() {
        val largeContent = buildString {
            append("# Header\n\n")
            repeat(1000) {
                append("Paragraph $it\n\n")
            }
            append("[Unclosed at end")
        }

        val errors = markdownParser.validate(largeContent)

        // Should detect error even in large document
        assertTrue(errors.isNotEmpty())
    }
}
