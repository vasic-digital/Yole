/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for ParseOptions builder and escapeHtml utility
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Unit tests for ParseOptions builder class and related utilities.
 *
 * Tests cover:
 * - Builder pattern functionality
 * - Method chaining
 * - Option setting and retrieval
 * - HTML escaping utility
 * - Edge cases
 */
class ParseOptionsTest {

    // ==================== ParseOptions Creation Tests ====================

    @Test
    fun `should create empty ParseOptions`() {
        val options = ParseOptions.create().build()

        assertTrue(options.isEmpty())
    }

    @Test
    fun `should create ParseOptions with factory method`() {
        val builder = ParseOptions.create()

        assertNotNull(builder)
    }

    // ==================== Line Numbers Tests ====================

    @Test
    fun `should enable line numbers`() {
        val options = ParseOptions.create()
            .enableLineNumbers()
            .build()

        assertEquals(true, options["lineNumbers"])
    }

    @Test
    fun `should disable line numbers explicitly`() {
        val options = ParseOptions.create()
            .enableLineNumbers(false)
            .build()

        assertEquals(false, options["lineNumbers"])
    }

    @Test
    fun `should enable line numbers with explicit true`() {
        val options = ParseOptions.create()
            .enableLineNumbers(true)
            .build()

        assertEquals(true, options["lineNumbers"])
    }

    // ==================== Syntax Highlighting Tests ====================

    @Test
    fun `should enable syntax highlighting`() {
        val options = ParseOptions.create()
            .enableHighlighting()
            .build()

        assertEquals(true, options["highlighting"])
    }

    @Test
    fun `should disable syntax highlighting explicitly`() {
        val options = ParseOptions.create()
            .enableHighlighting(false)
            .build()

        assertEquals(false, options["highlighting"])
    }

    @Test
    fun `should enable syntax highlighting with explicit true`() {
        val options = ParseOptions.create()
            .enableHighlighting(true)
            .build()

        assertEquals(true, options["highlighting"])
    }

    // ==================== Base URL Tests ====================

    @Test
    fun `should set base URL`() {
        val options = ParseOptions.create()
            .setBaseUrl("https://example.com")
            .build()

        assertEquals("https://example.com", options["baseUrl"])
    }

    @Test
    fun `should handle empty base URL`() {
        val options = ParseOptions.create()
            .setBaseUrl("")
            .build()

        assertEquals("", options["baseUrl"])
    }

    @Test
    fun `should handle base URL with path`() {
        val options = ParseOptions.create()
            .setBaseUrl("https://example.com/docs/")
            .build()

        assertEquals("https://example.com/docs/", options["baseUrl"])
    }

    // ==================== Custom Option Tests ====================

    @Test
    fun `should set custom string option`() {
        val options = ParseOptions.create()
            .set("customKey", "customValue")
            .build()

        assertEquals("customValue", options["customKey"])
    }

    @Test
    fun `should set custom integer option`() {
        val options = ParseOptions.create()
            .set("maxLines", 100)
            .build()

        assertEquals(100, options["maxLines"])
    }

    @Test
    fun `should set custom boolean option`() {
        val options = ParseOptions.create()
            .set("enableFeature", true)
            .build()

        assertEquals(true, options["enableFeature"])
    }

    @Test
    fun `should set multiple custom options`() {
        val options = ParseOptions.create()
            .set("option1", "value1")
            .set("option2", 42)
            .set("option3", true)
            .build()

        assertEquals("value1", options["option1"])
        assertEquals(42, options["option2"])
        assertEquals(true, options["option3"])
    }

    // ==================== Method Chaining Tests ====================

    @Test
    fun `should support method chaining`() {
        val options = ParseOptions.create()
            .enableLineNumbers()
            .enableHighlighting()
            .setBaseUrl("https://example.com")
            .set("custom", "value")
            .build()

        assertEquals(true, options["lineNumbers"])
        assertEquals(true, options["highlighting"])
        assertEquals("https://example.com", options["baseUrl"])
        assertEquals("value", options["custom"])
    }

    @Test
    fun `should chain all standard methods`() {
        val options = ParseOptions.create()
            .enableLineNumbers(true)
            .enableHighlighting(false)
            .setBaseUrl("https://test.com")
            .build()

        assertEquals(3, options.size)
    }

    // ==================== Build Tests ====================

    @Test
    fun `should build immutable map`() {
        val builder = ParseOptions.create()
            .set("key", "value")

        val options1 = builder.build()
        val options2 = builder.build()

        // Should be equal
        assertEquals(options1, options2)

        // But modifications to builder shouldn't affect built maps
        builder.set("newKey", "newValue")
        val options3 = builder.build()

        assertFalse(options1.containsKey("newKey"))
        assertFalse(options2.containsKey("newKey"))
        assertTrue(options3.containsKey("newKey"))
    }

    @Test
    fun `should allow multiple builds from same builder`() {
        val builder = ParseOptions.create()
            .enableLineNumbers()

        val options1 = builder.build()
        val options2 = builder.build()

        assertEquals(options1, options2)
    }

    // ==================== Override Tests ====================

    @Test
    fun `should override option value`() {
        val options = ParseOptions.create()
            .set("key", "value1")
            .set("key", "value2")
            .build()

        assertEquals("value2", options["key"])
    }

    @Test
    fun `should override line numbers setting`() {
        val options = ParseOptions.create()
            .enableLineNumbers(true)
            .enableLineNumbers(false)
            .build()

        assertEquals(false, options["lineNumbers"])
    }

    // ==================== Complex Scenario Tests ====================

    @Test
    fun `should handle comprehensive configuration`() {
        val options = ParseOptions.create()
            .enableLineNumbers(true)
            .enableHighlighting(true)
            .setBaseUrl("https://docs.example.com/v2/")
            .set("theme", "dark")
            .set("fontSize", 14)
            .set("fontFamily", "monospace")
            .set("tabSize", 4)
            .set("wordWrap", true)
            .build()

        assertEquals(8, options.size)
        assertEquals(true, options["lineNumbers"])
        assertEquals(true, options["highlighting"])
        assertEquals("https://docs.example.com/v2/", options["baseUrl"])
        assertEquals("dark", options["theme"])
        assertEquals(14, options["fontSize"])
        assertEquals("monospace", options["fontFamily"])
        assertEquals(4, options["tabSize"])
        assertEquals(true, options["wordWrap"])
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle empty string values`() {
        val options = ParseOptions.create()
            .set("emptyString", "")
            .build()

        assertEquals("", options["emptyString"])
    }

    @Test
    fun `should handle zero and negative numbers`() {
        val options = ParseOptions.create()
            .set("zero", 0)
            .set("negative", -42)
            .build()

        assertEquals(0, options["zero"])
        assertEquals(-42, options["negative"])
    }
}

/**
 * Tests for String.escapeHtml() extension function.
 */
class EscapeHtmlTest {

    @Test
    fun `should escape ampersand`() {
        val result = "A & B".escapeHtml()

        assertEquals("A &amp; B", result)
    }

    @Test
    fun `should escape less than`() {
        val result = "A < B".escapeHtml()

        assertEquals("A &lt; B", result)
    }

    @Test
    fun `should escape greater than`() {
        val result = "A > B".escapeHtml()

        assertEquals("A &gt; B", result)
    }

    @Test
    fun `should escape double quotes`() {
        val result = "Say \"Hello\"".escapeHtml()

        assertEquals("Say &quot;Hello&quot;", result)
    }

    @Test
    fun `should escape single quotes`() {
        val result = "It's working".escapeHtml()

        assertEquals("It&#39;s working", result)
    }

    @Test
    fun `should escape all special characters`() {
        val result = "<>&\"'".escapeHtml()

        assertEquals("&lt;&gt;&amp;&quot;&#39;", result)
    }

    @Test
    fun `should escape HTML tag`() {
        val result = "<script>alert('xss')</script>".escapeHtml()

        assertEquals("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;", result)
    }

    @Test
    fun `should handle empty string`() {
        val result = "".escapeHtml()

        assertEquals("", result)
    }

    @Test
    fun `should not modify plain text`() {
        val result = "Hello World 123".escapeHtml()

        assertEquals("Hello World 123", result)
    }

    @Test
    fun `should handle string with only special characters`() {
        val result = "&&&<<<>>>".escapeHtml()

        assertEquals("&amp;&amp;&amp;&lt;&lt;&lt;&gt;&gt;&gt;", result)
    }

    @Test
    fun `should preserve unicode characters`() {
        val result = "Hello 世界 🌍".escapeHtml()

        assertEquals("Hello 世界 🌍", result)
    }

    @Test
    fun `should escape in correct order`() {
        // Ampersand must be escaped first to avoid double-escaping
        val result = "A&B".escapeHtml()

        assertEquals("A&amp;B", result)
        assertFalse(result.contains("&amp;amp;"))
    }

    @Test
    fun `should handle multiple escapes in sequence`() {
        val input = "Test"
        val escaped1 = input.escapeHtml()
        val escaped2 = escaped1.escapeHtml()

        // First escape: no change (no special chars)
        assertEquals("Test", escaped1)
        // Second escape: still no change
        assertEquals("Test", escaped2)
    }

    @Test
    fun `should handle mixed content`() {
        val result = "Name: <John> Age: 30 & active".escapeHtml()

        assertEquals("Name: &lt;John&gt; Age: 30 &amp; active", result)
    }

    @Test
    fun `should escape HTML entities safely`() {
        val result = "Price: &pound;10 < &pound;20".escapeHtml()

        // Should escape the ampersands in entities
        assertEquals("Price: &amp;pound;10 &lt; &amp;pound;20", result)
    }

    @Test
    fun `should handle very long strings`() {
        val longString = "<script>" + "A".repeat(10000) + "</script>"
        val result = longString.escapeHtml()

        assertTrue(result.startsWith("&lt;script&gt;"))
        assertTrue(result.endsWith("&lt;/script&gt;"))
        assertTrue(result.contains("A".repeat(10000)))
    }
}
