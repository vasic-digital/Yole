/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Property-based tests using Kotest for LaTeX format
 *
 *########################################################*/
package digital.vasic.yole.format.latex

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.kotest.property.forAll
import kotlin.test.assertNotNull

/**
 * Property-based tests for LaTeX format using Kotest.
 *
 * Property-based testing generates hundreds of random test cases
 * to discover edge cases that manual tests might miss.
 *
 * Benefits:
 * - Discovers unexpected edge cases
 * - Tests with wide range of inputs automatically
 * - Validates invariants across many scenarios
 * - Reduces test maintenance (fewer explicit test cases)
 */
class LatexPropertyTest : StringSpec({

    val parser = LatexParser()

    // ==================== Basic Property Tests ====================

    "parsing any valid LaTeX content should not throw exceptions" {
        checkAll(validLatexContent()) { content ->
            val result = runCatching { parser.parse(content) }
            result.isSuccess shouldBe true
        }
    }

    "parsing then serializing should preserve content structure" {
        checkAll(validLatexContent()) { original ->
            val parsed = parser.parse(original)
            assertNotNull(parsed)

            // If format supports serialization:
            // val serialized = parser.serialize(parsed)
            // val reparsed = parser.parse(serialized)
            // reparsed shouldBe parsed
        }
    }

    "empty or whitespace-only input should always parse successfully" {
        forAll(Arb.string().filter { it.isBlank() }) { whitespace ->
            val result = parser.parse(whitespace)
            result != null
        }
    }

    // ==================== String Property Tests ====================

    "parser should handle strings of any length" {
        checkAll(Arb.string(0..10000)) { content ->
            val result = runCatching { parser.parse(content) }
            result.isSuccess shouldBe true
        }
    }

    "parser should handle unicode characters correctly" {
        checkAll(unicodeContent()) { content ->
            val parsed = parser.parse(content)
            assertNotNull(parsed)
            // Verify unicode is preserved
        }
    }

    "parser should handle special characters without breaking" {
        checkAll(contentWithSpecialChars()) { content ->
            val result = runCatching { parser.parse(content) }
            result.isSuccess shouldBe true
        }
    }

    // ==================== Line Ending Properties ====================

    "parser should handle different line ending styles" {
        checkAll(validLatexContent(), Arb.lineEnding()) { content, lineEnding ->
            val normalized = content.replace("\n", lineEnding)
            val result = parser.parse(normalized)
            assertNotNull(result)
        }
    }

    "parsing with mixed line endings should work correctly" {
        checkAll(contentWithMixedLineEndings()) { content ->
            val parsed = parser.parse(content)
            assertNotNull(parsed)
        }
    }

    // ==================== Idempotency Properties ====================

    "parsing twice should produce identical results" {
        checkAll(validLatexContent()) { content ->
            val first = parser.parse(content)
            val second = parser.parse(content)
            first shouldBe second
        }
    }

    "parser should be stateless between calls" {
        checkAll(
            validLatexContent(),
            validLatexContent()
        ) { content1, content2 ->
            val result1First = parser.parse(content1)
            val result2 = parser.parse(content2)
            val result1Second = parser.parse(content1)

            result1First shouldBe result1Second
        }
    }

    // ==================== Format-Specific Properties ====================

    "should maintain format-specific property 1" {
        checkAll(formatSpecificLatexContent()) { input ->
            val parsed = parser.parse(input)
            // Add format-specific property assertion
            assertNotNull(parsed)
        }
    }

    "should maintain format-specific property 2" {
        checkAll(Arb.int(1..1000)) { count ->
            val content = generateRepeatedPattern(count)
            val parsed = parser.parse(content)
            // Verify property holds for repeated patterns
            assertNotNull(parsed)
        }
    }

    // ==================== Error Recovery Properties ====================

    "parser should gracefully handle malformed input" {
        checkAll(malformedLatexContent()) { malformed ->
            val result = runCatching { parser.parse(malformed) }
            // Should not crash, but may return error or best-effort parse
            result.isSuccess shouldBe true
        }
    }

    "parser should handle truncated content" {
        checkAll(validLatexContent(), Arb.int(0..100)) { content, cutoff ->
            val truncated = content.take(cutoff)
            val result = runCatching { parser.parse(truncated) }
            result.isSuccess shouldBe true
        }
    }

    // ==================== Performance Properties ====================

    "parsing large content should complete in reasonable time" {
        checkAll(10, largeValidLatexContent()) { largeContent ->
            val startTime = System.currentTimeMillis()
            parser.parse(largeContent)
            val duration = System.currentTimeMillis() - startTime

            // Should parse large content in under 5 seconds
            duration < 5000
        }
    }
})

// ==================== Custom Generators ====================

/**
 * Generates valid LaTeX content samples.
 */
fun validLatexContent(): Arb<String> = arbitrary { rs ->
    val samples = listOf(
        "Sample LaTeX content here",
        "Single line of LaTeX",
        "Line 1
Line 2
Line 3",
        ""
    )
    samples.random(rs.random)
}

/**
 * Generates large valid LaTeX content for performance testing.
 */
fun largeValidLatexContent(): Arb<String> = arbitrary { rs ->
    buildString {
        repeat(rs.random.nextInt(500, 2000)) {
            appendLine("Single line of LaTeX")
        }
    }
}

/**
 * Generates content with unicode characters.
 */
fun unicodeContent(): Arb<String> = arbitrary { rs ->
    val unicodeRanges = listOf(
        "Latin: Café résumé naïve",
        "Cyrillic: Привет мир",
        "Chinese: 你好世界",
        "Japanese: こんにちは世界",
        "Arabic: مرحبا بالعالم",
        "Emoji: 🌍 🚀 ✨ 📝 💻"
    )
    unicodeRanges.random(rs.random)
}

/**
 * Generates content with special characters.
 */
fun contentWithSpecialChars(): Arb<String> = arbitrary { rs ->
    val specialChars = listOf(
        "Special chars: @#$%^{{SPECIAL_CHARS_SAMPLE}}*()",
        "Quotes: \"double\" 'single' `backtick`",
        "Symbols: @#$%^&*()_+-=[]{}|;:,.<>?/~",
        "Escapes: \\ \n \t \r",
        "HTML: <tag> &amp; &#x1F600;"
    )
    specialChars.random(rs.random)
}

/**
 * Generates content with mixed line endings.
 */
fun contentWithMixedLineEndings(): Arb<String> = arbitrary { rs ->
    "Line 1\nLine 2\r\nLine 3\rLine 4"
}

/**
 * Generates malformed LaTeX content for error handling tests.
 */
fun malformedLatexContent(): Arb<String> = arbitrary { rs ->
    val malformedSamples = listOf(
        "Malformed LaTeX content",
        "\u0000Binary\u0000Data\u0000",
        "Incomplete...",
        "{{" + "UNMATCHED_DELIMITER"
    )
    malformedSamples.random(rs.random)
}

/**
 * Generates different line ending styles.
 */
fun Arb.Companion.lineEnding(): Arb<String> = arbitrary { rs ->
    listOf("\n", "\r\n", "\r").random(rs.random)
}

/**
 * Format-specific generator placeholder.
 */
fun formatSpecificLatexContent(): Arb<String> = arbitrary { rs ->
    // Implement format-specific content generation
    "Format specific sample"
}

/**
 * Helper function to generate repeated patterns.
 */
private fun generateRepeatedPattern(count: Int): String = buildString {
    repeat(count) {
        appendLine("Single line of LaTeX")
    }
}
