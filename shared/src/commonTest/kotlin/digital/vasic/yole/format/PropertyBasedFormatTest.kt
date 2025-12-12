/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Property-Based Testing for Format Parsers
 * Uses Kotest property testing for comprehensive edge case coverage
 *
 *########################################################*/

package digital.vasic.yole.format

import io.kotest.property.*
import io.kotest.property.arbitrary.*
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.csv.CsvParser
import kotlin.test.*

/**
 * Property-based tests for format parsers using Kotest
 * Generates random inputs to find edge cases and ensure robustness
 */
class PropertyBasedFormatTest {

    @Test
    fun testMarkdownParserWithRandomContent() {
        val parser = MarkdownParser()
        
        checkAll(Arb.string(range = 0..1000, codepoints = Codepoint.all())) { randomContent ->
            // Should not crash on any input
            val result = parser.parse(randomContent)
            
            // Basic assertions that should always hold
            assertNotNull(result, "Parser should not return null for any input")
            
            // Title should be consistent with content
            if (randomContent.isNotEmpty()) {
                assertTrue(result.title.length <= randomContent.length, "Title should not be longer than input")
            }
        }
    }

    @Test
    fun testTodoTxtParserWithRandomContent() {
        val parser = TodoTxtParser()
        
        checkAll(Arb.string(range = 0..500, codepoints = Codepoint.all())) { randomContent ->
            // Should not crash on any input
            val result = parser.parse(randomContent)
            
            assertNotNull(result, "Parser should not return null for any input")
            
            // Text content should be reasonable
            if (randomContent.isNotEmpty()) {
                assertTrue(result.text.length <= randomContent.length, "Parsed text should not be longer than input")
            }
        }
    }

    @Test
    fun testCsvParserWithRandomContent() {
        val parser = CsvParser()
        
        checkAll(Arb.string(range = 0..1000, codepoints = Codepoint.all())) { randomContent ->
            // Should not crash on any input
            val result = parser.parse(randomContent)
            
            assertNotNull(result, "Parser should not return null for any input")
            
            // Basic CSV structure validation
            assertTrue(result.rows.size >= 0, "Row count should be non-negative")
            assertTrue(result.headers.size >= 0, "Header count should be non-negative")
        }
    }

    @Test
    fun testMarkdownSpecificStructures() {
        val parser = MarkdownParser()
        
        // Test with random headers
        checkAll(Arb.int(1..6), Arb.string(range = 1..100)) { level, title ->
            val markdown = "${"#".repeat(level)} $title"
            val result = parser.parse(markdown)
            
            assertNotNull(result)
            assertTrue(result.headers.isNotEmpty() || title.isBlank(), "Should parse headers when present")
        }
        
        // Test with random lists
        checkAll(Arb.list(Arb.string(range = 1..50), range = 1..20)) { items ->
            val markdown = items.joinToString("\n") { "- $it" }
            val result = parser.parse(markdown)
            
            assertNotNull(result)
            // Should handle lists gracefully
        }
        
        // Test with random code blocks
        checkAll(Arb.string(range = 1..20), Arb.string(range = 1..100)) { language, code ->
            val markdown = """
                ```$language
                $code
                ```
            """.trimIndent()
            
            val result = parser.parse(markdown)
            assertNotNull(result)
        }
    }

    @Test
    fun testTodoTxtSpecificStructures() {
        val parser = TodoTxtParser()
        
        // Test with random priorities
        checkAll(Arb.char('A'..'Z')) { priority ->
            val todoTxt = "($priority) Random task"
            val result = parser.parse(todoTxt)
            
            assertNotNull(result)
            if (priority in 'A'..'C') {
                assertEquals(priority.toString(), result.priority, "Should parse valid priorities")
            }
        }
        
        // Test with random contexts
        checkAll(Arb.string(range = 1..20, codepoints = Codepoint.alphanumeric())) { context ->
            val todoTxt = "Task @${context}"
            val result = parser.parse(todoTxt)
            
            assertNotNull(result)
            assertTrue(result.contexts.isNotEmpty() || context.isBlank(), "Should parse contexts when present")
        }
        
        // Test with random projects
        checkAll(Arb.string(range = 1..20, codepoints = Codepoint.alphanumeric())) { project ->
            val todoTxt = "Task +${project}"
            val result = parser.parse(todoTxt)
            
            assertNotNull(result)
            assertTrue(result.projects.isNotEmpty() || project.isBlank(), "Should parse projects when present")
        }
    }

    @Test
    fun testCsvSpecificStructures() {
        val parser = CsvParser()
        
        // Test with random CSV structures
        checkAll(
            Arb.int(1..10),
            Arb.int(1..10),
            Arb.string(range = 1..20, codepoints = Codepoint.alphanumeric())
        ) { rows, cols, baseContent ->
            val csvContent = buildString {
                // Generate header row
                append((1..cols).joinToString(",") { "Col$it" })
                append("\n")
                
                // Generate data rows
                repeat(rows) { rowIdx ->
                    append((1..cols).joinToString(",") { colIdx ->
                        "Row${rowIdx}Col${colIdx}_$baseContent"
                    })
                    append("\n")
                }
            }
            
            val result = parser.parse(csvContent)
            assertNotNull(result)
            assertEquals(rows, result.rows.size, "Should have correct number of rows")
            assertEquals(cols, result.headers.size, "Should have correct number of columns")
        }
    }

    @Test
    fun testFormatDetectionWithRandomFilenames() {
        checkAll(Arb.string(range = 1..50, codepoints = Codepoint.alphanumeric())) { baseName ->
            val markdownFilename = "${baseName}.md"
            val detectedFormat = FormatRegistry.detectFormat(markdownFilename)
            assertEquals("markdown", detectedFormat, "Should detect markdown files")
            
            val csvFilename = "${baseName}.csv"
            val detectedCsvFormat = FormatRegistry.detectFormat(csvFilename)
            assertEquals("csv", detectedCsvFormat, "Should detect CSV files")
            
            val txtFilename = "${baseName}.txt"
            val detectedTxtFormat = FormatRegistry.detectFormat(txtFilename)
            assertEquals("plaintext", detectedTxtFormat, "Should detect text files")
        }
    }

    @Test
    fun testRoundTripConsistency() {
        val markdownParser = MarkdownParser()
        val todoParser = TodoTxtParser()
        val csvParser = CsvParser()
        
        // Test markdown round trip
        checkAll(Arb.string(range = 10..200, codepoints = Codepoint.printable())) { content ->
            val originalDocument = markdownParser.parse(content)
            assertNotNull(originalDocument)
            
            val regeneratedContent = markdownParser.toMarkdown(originalDocument)
            assertNotNull(regeneratedContent)
            
            val reparsedDocument = markdownParser.parse(regeneratedContent)
            assertNotNull(reparsedDocument)
            
            // Basic consistency checks
            assertEquals(originalDocument.title, reparsedDocument.title)
            assertEquals(originalDocument.headers.size, reparsedDocument.headers.size)
        }
        
        // Test todo.txt round trip
        checkAll(
            Arb.char('A'..'C').orNull(),
            Arb.string(range = 5..50, codepoints = Codepoint.printable()),
            Arb.list(Arb.string(range = 1..10, codepoints = Codepoint.alphanumeric()), range = 0..3),
            Arb.list(Arb.string(range = 1..10, codepoints = Codepoint.alphanumeric()), range = 0..3)
        ) { priority, text, projects, contexts ->
            val todoContent = buildString {
                priority?.let { append("($it) ") }
                append(text)
                projects.forEach { append(" +$it") }
                contexts.forEach { append(" @$it") }
            }
            
            val originalDocument = todoParser.parse(todoContent)
            assertNotNull(originalDocument)
            
            val regeneratedContent = todoParser.toTodoTxt(originalDocument)
            assertNotNull(regeneratedContent)
            
            val reparsedDocument = todoParser.parse(regeneratedContent)
            assertNotNull(reparsedDocument)
            
            // Basic consistency checks
            assertEquals(originalDocument.text, reparsedDocument.text)
            assertEquals(originalDocument.projects.size, reparsedDocument.projects.size)
            assertEquals(originalDocument.contexts.size, reparsedDocument.contexts.size)
        }
    }

    @Test
    fun testPerformanceWithLargeContent() {
        val parser = MarkdownParser()
        
        checkAll(Arb.int(100..1000)) { paragraphCount ->
            val largeContent = buildString {
                repeat(paragraphCount) { i ->
                    appendLine("# Heading $i")
                    appendLine()
                    appendLine("This is paragraph $i with some **bold** and *italic* text.")
                    appendLine()
                    appendLine("- List item 1 for paragraph $i")
                    appendLine("- List item 2 for paragraph $i")
                    appendLine()
                }
            }
            
            val startTime = System.currentTimeMillis()
            val result = parser.parse(largeContent)
            val endTime = System.currentTimeMillis()
            
            assertNotNull(result)
            
            // Performance should be reasonable
            val parsingTime = endTime - startTime
            assertTrue(parsingTime < 5000, "Parsing should complete within 5 seconds for large content")
        }
    }

    @Test
    fun testUnicodeHandling() {
        val parser = MarkdownParser()
        
        // Test with various Unicode ranges
        checkAll(
            Arb.string(range = 1..50, codepoints = Codepoint.unicode()),
            Arb.string(range = 1..50, codepoints = Codepoint.ascii())
        ) { unicodeContent, asciiContent ->
            val mixedContent = """
                # $unicodeContent
                
                Some $asciiContent mixed with $unicodeContent
                
                ## More Unicode: $unicodeContent
                
                Final $asciiContent content
            """.trimIndent()
            
            val result = parser.parse(mixedContent)
            assertNotNull(result)
            
            // Should handle Unicode gracefully
            assertTrue(result.title.length <= mixedContent.length)
        }
    }

    @Test
    fun testMalformedContentHandling() {
        val parsers = listOf(
            MarkdownParser(),
            TodoTxtParser(),
            CsvParser()
        )
        
        parsers.forEach { parser ->
            checkAll(Arb.string(range = 1..100, codepoints = Arb.codepoints())) { malformedContent ->
                // Should not crash on malformed content
                shouldNotThrowAny {
                    when (parser) {
                        is MarkdownParser -> parser.parse(malformedContent)
                        is TodoTxtParser -> parser.parse(malformedContent)
                        is CsvParser -> parser.parse(malformedContent)
                        else -> fail("Unknown parser type")
                    }
                }
            }
        }
    }

    @Test
    fun testEmptyAndWhitespaceContent() {
        val parsers = listOf(
            MarkdownParser(),
            TodoTxtParser(),
            CsvParser()
        )
        
        val emptyInputs = listOf(
            "",
            "   ",
            "\n\n\n",
            "\t\t\t",
            " \n \t \n "
        )
        
        parsers.forEach { parser ->
            emptyInputs.forEach { emptyInput ->
                val result = when (parser) {
                    is MarkdownParser -> parser.parse(emptyInput)
                    is TodoTxtParser -> parser.parse(emptyInput)
                    is CsvParser -> parser.parse(emptyInput)
                    else -> fail("Unknown parser type")
                }
                
                assertNotNull(result, "Should handle empty input: '$emptyInput'")
            }
        }
    }
}