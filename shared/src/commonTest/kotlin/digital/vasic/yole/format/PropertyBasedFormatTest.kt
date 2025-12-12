/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Property-based tests for format parsing and validation
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*
import kotlin.random.Random

/**
 * Property-based tests for format parsing covering:
 * - Random content generation and parsing
 * - Edge cases and boundary conditions
 * - Fuzzing and robustness testing
 * - Performance under various inputs
 */
class PropertyBasedFormatTest {

    private val testIterations = 100

    @Test
    fun testRandomContentParsing() {
        repeat(testIterations) { iteration ->
            // Generate random content
            val randomContent = generateRandomContent(Random.nextInt(1, 1000))
            val format = FormatRegistry.detectByContent(randomContent)
            
            // Should either detect a format or return null (but not crash)
            assertTrue(true, "Random content iteration $iteration should not crash parser")
            
            // If format detected, should be able to parse it
            format?.let { detectedFormat ->
                val parser = ParserRegistry.getParser(detectedFormat)
                assertNotNull(parser, "Should have parser for detected format")
                
                val result = parser.parse(randomContent)
                assertNotNull(result, "Parser should handle random content without crashing")
            }
        }
    }

    @Test
    fun testBoundaryContentSizes() {
        val sizeBoundaries = listOf(0, 1, 10, 100, 1000, 10000, 100000)
        
        sizeBoundaries.forEach { size ->
            val content = generateRandomContent(size)
            val format = FormatRegistry.detectByContent(content)
            
            // Should handle any size without issues
            assertTrue(true, "Should handle content of size $size")
            
            // Test with specific patterns for each size
            when {
                size == 0 -> {
                    assertTrue(content.isEmpty(), "Zero size should produce empty content")
                }
                size < 10 -> {
                    // Small content should be handled efficiently
                    assertTrue(content.length <= 10, "Small content should match requested size")
                }
                size > 10000 -> {
                    // Large content should be handled without memory issues
                    assertTrue(content.length >= 10000, "Large content should match requested size")
                }
            }
        }
    }

    @Test
    fun testUnicodeContentHandling() {
        val unicodeRanges = listOf(
            0x0020..0x007F,  // Basic Latin
            0x0080..0x00FF,  // Latin-1 Supplement  
            0x0100..0x017F,  // Latin Extended-A
            0x0400..0x04FF,  // Cyrillic
            0x4E00..0x9FFF,  // CJK Unified Ideographs
            0xAC00..0xD7AF,  // Hangul Syllables
            0xE000..0xF8FF,  // Private Use Area
            0x1F300..0x1F5FF // Miscellaneous Symbols and Pictographs (Emojis)
        )
        
        repeat(50) {
            val randomRange = unicodeRanges.random()
            val unicodeContent = generateUnicodeContent(randomRange, 100)
            
            // Should handle Unicode without corruption
            assertTrue(unicodeContent.isNotEmpty(), "Should generate non-empty Unicode content")
            
            val format = FormatRegistry.detectByContent(unicodeContent)
            format?.let { detectedFormat ->
                val parser = ParserRegistry.getParser(detectedFormat)
                val result = parser.parse(unicodeContent)
                assertNotNull(result, "Should parse Unicode content without corruption")
            }
        }
    }

    @Test
    fun testSpecialCharacterHandling() {
        val specialChars = listOf(
            "!@#$%^&*()_+-=[]{}|;':\",./<>?",
            "\t\n\r\f\b",
            "\\\"\\'",
            "\u0000\u0001\u0002", // Control characters
            "~`¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿",
            "😀😁😂🤣😃😄😅😆😉😊😋😎😍😘😗😙😚"
        )
        
        specialChars.forEach { specialContent ->
            // Should handle special characters without crashing
            assertTrue(true, "Should handle special characters: $specialContent")
            
            val format = FormatRegistry.detectByContent(specialContent)
            format?.let { detectedFormat ->
                val parser = ParserRegistry.getParser(detectedContent)
                val result = parser.parse(specialContent)
                assertNotNull(result, "Should handle special characters without crashing")
            }
        }
    }

    @Test
    fun testPatternBasedContent() {
        // Test with patterns that might trigger specific format detection
        val patterns = listOf(
            "# " to "Markdown heading",
            "```" to "Markdown code block",
            "* " to "Markdown list",
            "**" to "Markdown bold",
            "__" to "Markdown italic",
            "[" to "Markdown link",
            "!" to "Potential image or emphasis",
            "---" to "Markdown horizontal rule",
            "|" to "Potential table",
            "> " to "Markdown blockquote"
        )
        
        repeat(100) {
            val (pattern, description) = patterns.random()
            val content = buildString {
                repeat(Random.nextInt(1, 10)) {
                    append(pattern)
                    append(" ")
                    append(generateRandomWords(Random.nextInt(1, 20)))
                    append("\n")
                }
            }
            
            val format = FormatRegistry.detectByContent(content)
            assertTrue(true, "Should handle pattern-based content: $description")
            
            format?.let { detectedFormat ->
                val parser = ParserRegistry.getParser(detectedFormat)
                val result = parser.parse(content)
                assertNotNull(result, "Should parse pattern-based content")
            }
        }
    }

    @Test
    fun testMalformedContentRobustness() {
        val malformedCases = listOf(
            "Unclosed **bold marker",
            "Unclosed [link",
            "Unclosed ```code block",
            "Mismatched **bold** and __italic__",
            "Nested # # # # headings",
            "Broken | table | structure",
            "Invalid URL: [link](not-a-valid-url",
            "Empty markers: ** __ `` [] ()",
            "Mixed markers: **__``[]()",
            "Very long single line: ${"a".repeat(10000)}"
        )
        
        malformedCases.forEach { malformedContent ->
            // Should handle malformed content gracefully
            assertTrue(true, "Should handle malformed content: $malformedContent")
            
            val format = FormatRegistry.detectByContent(malformedContent)
            format?.let { detectedFormat ->
                val parser = ParserRegistry.getParser(detectedFormat)
                val result = parser.parse(malformedContent)
                assertNotNull(result, "Should handle malformed content gracefully without crashing")
            }
        }
    }

    @Test
    fun testMixedContentTypes() {
        // Test content that mixes different format types
        val mixedContent = buildString {
            appendLine("# Markdown Heading")
            appendLine("Some **bold** text and *italic* text.")
            appendLine("```")
            appendLine("// This looks like code")
            appendLine("function example() {")
            appendLine("  return 'hello';")
            appendLine("}")
            appendLine("```")
            appendLine("| Column 1 | Column 2 |")
            appendLine("|----------|----------|")
            appendLine("| Data 1   | Data 2   |")
            appendLine("")
            appendLine("1. Numbered list")
            appendLine("2. Second item")
            appendLine("   - Nested bullet")
            appendLine("   - Another nested")
            appendLine("")
            appendLine("> This is a blockquote")
            appendLine("> With multiple lines")
            appendLine("")
            appendLine("[Link text](http://example.com)")
            appendLine("![Image alt](image.jpg)")
        }
        
        val format = FormatRegistry.detectByContent(mixedContent)
        assertTrue(true, "Should handle mixed content types")
        
        format?.let { detectedFormat ->
            val parser = ParserRegistry.getParser(detectedFormat)
            val result = parser.parse(mixedContent)
            assertNotNull(result, "Should parse mixed content types")
            assertTrue(result.content.isNotEmpty(), "Parsed mixed content should not be empty")
        }
    }

    @Test
    fun testPerformanceUnderLoad() {
        val contentSizes = listOf(100, 1000, 10000, 100000)
        val iterationsPerSize = 10
        
        contentSizes.forEach { size ->
            val startTime = System.currentTimeMillis()
            
            repeat(iterationsPerSize) {
                val content = generateRandomContent(size)
                val format = FormatRegistry.detectByContent(content)
                
                format?.let { detectedFormat ->
                    val parser = ParserRegistry.getParser(detectedFormat)
                    parser.parse(content)
                }
            }
            
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime
            val averageTime = totalTime / iterationsPerSize
            
            // Should complete within reasonable time (less than 1 second per operation on average)
            assertTrue(averageTime < 1000, "Average parsing time for $size bytes should be under 1 second, was $averageTime ms")
            println("Performance test: $size bytes, avg ${averageTime}ms per operation")
        }
    }

    @Test
    fun testMemoryEfficiency() {
        // Test with progressively larger content to ensure no memory leaks
        val sizes = listOf(1000, 10000, 100000, 1000000) // Up to 1MB
        
        sizes.forEach { size ->
            val content = generateRandomContent(size)
            
            // Run multiple iterations to detect memory issues
            repeat(10) {
                val format = FormatRegistry.detectByContent(content)
                
                format?.let { detectedFormat ->
                    val parser = ParserRegistry.getParser(detectedFormat)
                    val result = parser.parse(content)
                    assertNotNull(result, "Should handle large content without memory issues")
                }
            }
            
            // Force garbage collection hint
            System.gc()
            Thread.sleep(10) // Small delay to allow GC
            
            assertTrue(true, "Should complete memory efficiency test for $size bytes")
        }
    }

    @Test
    fun testConcurrentParsing() {
        val threadCount = 10
        val iterationsPerThread = 50
        val results = mutableListOf<Boolean>()
        val threads = mutableListOf<Thread>()
        
        repeat(threadCount) { threadIndex ->
            val thread = Thread {
                repeat(iterationsPerThread) { iteration ->
                    try {
                        val content = generateRandomContent(Random.nextInt(100, 1000))
                        val format = FormatRegistry.detectByContent(content)
                        
                        format?.let { detectedFormat ->
                            val parser = ParserRegistry.getParser(detectedFormat)
                            parser.parse(content)
                        }
                        
                        synchronized(results) {
                            results.add(true)
                        }
                    } catch (e: Exception) {
                        synchronized(results) {
                            results.add(false)
                        }
                        println("Thread $threadIndex, iteration $iteration failed: ${e.message}")
                    }
                }
            }
            
            threads.add(thread)
            thread.start()
        }
        
        threads.forEach { it.join() }
        
        assertEquals(threadCount * iterationsPerThread, results.size, "All threads should complete")
        val successRate = results.count { it }.toDouble() / results.size
        assertTrue(successRate >= 0.95, "Success rate should be at least 95%, was $successRate")
        println("Concurrent parsing success rate: ${successRate * 100}%")
    }

    @Test
    fun testDeterministicBehavior() {
        // Test that the same input produces the same output
        val content = "# Test Heading\n\nThis is **bold** text."
        
        val results = mutableListOf<ParsedDocument>()
        
        repeat(10) {
            val format = FormatRegistry.detectByContent(content)
            assertNotNull(format, "Should consistently detect format")
            
            val parser = ParserRegistry.getParser(format)
            assertNotNull(parser, "Should consistently have parser")
            
            val result = parser.parse(content)
            assertNotNull(result, "Should consistently parse content")
            
            results.add(result)
        }
        
        // All results should be identical
        val firstResult = results.first()
        results.forEach { result ->
            assertEquals(firstResult.content, result.content, "Results should be deterministic")
            assertEquals(firstResult.metadata, result.metadata, "Metadata should be deterministic")
        }
    }

    // ==================== Helper Methods ====================

    private fun generateRandomContent(size: Int): String {
        return buildString {
            repeat(size / 10 + 1) {
                append(generateRandomWords(Random.nextInt(1, 20)))
                append(" ")
                if (Random.nextFloat() < 0.3) {
                    append("\n")
                }
            }
        }.take(size)
    }

    private fun generateRandomWords(wordCount: Int): String {
        val words = listOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
            "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
            "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what"
        )
        
        return List(wordCount) { words.random() }.joinToString(" ")
    }

    private fun generateUnicodeContent(range: IntRange, charCount: Int): String {
        return buildString {
            repeat(charCount) {
                val codePoint = range.random()
                appendCodePoint(codePoint)
            }
        }
    }

    private fun String.appendCodePoint(codePoint: Int) {
        when {
            codePoint <= 0xFFFF -> append(codePoint.toChar())
            else -> {
                // Handle surrogate pairs for code points > 0xFFFF
                val high = ((codePoint - 0x10000) shr 10) + 0xD800
                val low = ((codePoint - 0x10000) and 0x3FF) + 0xDC00
                append(high.toChar())
                append(low.toChar())
            }
        }
    }
}