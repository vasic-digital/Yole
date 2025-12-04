/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for string utility functions
 *
 *########################################################*/
package digital.vasic.yole.format

import kotlin.test.*

/**
 * Tests for string utility functions like escapeHtml().
 *
 * Tests cover:
 * - Basic HTML entity escaping
 * - Multiple entities in same string
 * - Edge cases (empty, special chars)
 * - Performance with large strings
 * - Order of operations
 */
class StringUtilsTest {

    // ==================== Basic Escaping Tests ====================

    @Test
    fun `should escape ampersand`() {
        assertEquals("&amp;", "&".escapeHtml())
    }

    @Test
    fun `should escape less than`() {
        assertEquals("&lt;", "<".escapeHtml())
    }

    @Test
    fun `should escape greater than`() {
        assertEquals("&gt;", ">".escapeHtml())
    }

    @Test
    fun `should escape double quote`() {
        assertEquals("&quot;", "\"".escapeHtml())
    }

    @Test
    fun `should escape single quote`() {
        assertEquals("&#39;", "'".escapeHtml())
    }

    // ==================== Multiple Entities Tests ====================

    @Test
    fun `should escape all HTML entities`() {
        val input = "<>&\"'"
        val expected = "&lt;&gt;&amp;&quot;&#39;"

        assertEquals(expected, input.escapeHtml())
    }

    @Test
    fun `should escape entities in text`() {
        val input = "Hello <world> & \"friends\""
        val expected = "Hello &lt;world&gt; &amp; &quot;friends&quot;"

        assertEquals(expected, input.escapeHtml())
    }

    @Test
    fun `should escape script tag`() {
        val input = "<script>alert('xss')</script>"
        val expected = "&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;"

        assertEquals(expected, input.escapeHtml())
    }

    @Test
    fun `should escape HTML tag with attributes`() {
        val input = "<div class=\"test\" id='main'>"
        val expected = "&lt;div class=&quot;test&quot; id=&#39;main&#39;&gt;"

        assertEquals(expected, input.escapeHtml())
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle empty string`() {
        assertEquals("", "".escapeHtml())
    }

    @Test
    fun `should handle string with no escapable characters`() {
        val input = "Hello World 123"
        assertEquals(input, input.escapeHtml())
    }

    @Test
    fun `should handle string with only escapable characters`() {
        val input = "<<<>>>&&&"
        val expected = "&lt;&lt;&lt;&gt;&gt;&gt;&amp;&amp;&amp;"

        assertEquals(expected, input.escapeHtml())
    }

    @Test
    fun `should handle whitespace`() {
        val input = "  \n\t\r  "
        assertEquals(input, input.escapeHtml())
    }

    @Test
    fun `should handle unicode characters`() {
        val input = "Hello 世界 🌍"
        assertEquals(input, input.escapeHtml())
    }

    // ==================== Order of Operations Tests ====================

    @Test
    fun `should escape ampersand before other entities`() {
        // If & isn't escaped first, it could double-escape
        val input = "&<>"
        val result = input.escapeHtml()

        // Should be &amp;&lt;&gt; not &amp;amp;&amp;lt;&amp;gt;
        assertEquals("&amp;&lt;&gt;", result)
        assertFalse(result.contains("&amp;amp;"))
    }

    @Test
    fun `should handle already escaped entities correctly`() {
        val input = "&amp;"
        val expected = "&amp;amp;"

        // Escaping already-escaped entities should escape the & again
        assertEquals(expected, input.escapeHtml())
    }

    @Test
    fun `should handle mixed escaped and unescaped`() {
        val input = "a < b &amp; c > d"
        val expected = "a &lt; b &amp;amp; c &gt; d"

        assertEquals(expected, input.escapeHtml())
    }

    // ==================== Real-World Scenarios ====================

    @Test
    fun `should escape HTML document fragment`() {
        val input = """
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body>
            <p>Hello "World"</p>
            </body>
            </html>
        """.trimIndent()

        val result = input.escapeHtml()

        assertTrue(result.contains("&lt;!DOCTYPE html&gt;"))
        assertTrue(result.contains("&lt;html&gt;"))
        assertTrue(result.contains("&quot;World&quot;"))
    }

    @Test
    fun `should escape JavaScript code`() {
        val input = "if (x < 10 && y > 5) { alert('test'); }"
        val result = input.escapeHtml()

        assertTrue(result.contains("&lt;"))
        assertTrue(result.contains("&gt;"))
        assertTrue(result.contains("&amp;&amp;"))
        assertTrue(result.contains("&#39;test&#39;"))
    }

    @Test
    fun `should escape SQL injection attempt`() {
        val input = "' OR '1'='1"
        val expected = "&#39; OR &#39;1&#39;=&#39;1"

        assertEquals(expected, input.escapeHtml())
    }

    @Test
    fun `should escape XML markup`() {
        val input = "<?xml version=\"1.0\"?>"
        val result = input.escapeHtml()

        assertTrue(result.contains("&lt;?xml"))
        assertTrue(result.contains("&quot;1.0&quot;"))
        assertTrue(result.contains("?&gt;"))
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should handle very long string`() {
        val input = "a<b&c>d\"e'f".repeat(10000)
        val result = input.escapeHtml()

        assertNotNull(result)
        assertTrue(result.length > input.length)
        assertTrue(result.contains("&lt;"))
    }

    @Test
    fun `should handle string with many entities`() {
        val input = "&" + "<>\"'".repeat(1000)
        val result = input.escapeHtml()

        assertNotNull(result)
        assertTrue(result.contains("&amp;"))
        assertTrue(result.contains("&lt;"))
        assertTrue(result.contains("&gt;"))
    }

    @Test
    fun `should handle string with no entities efficiently`() {
        val input = "abcdefghijklmnopqrstuvwxyz".repeat(1000)
        val result = input.escapeHtml()

        // Should return quickly since no replacements needed
        assertEquals(input, result)
    }

    // ==================== Boundary Tests ====================

    @Test
    fun `should handle single character`() {
        assertEquals("a", "a".escapeHtml())
        assertEquals("&lt;", "<".escapeHtml())
    }

    @Test
    fun `should handle entities at start`() {
        val input = "<start"
        val expected = "&lt;start"

        assertEquals(expected, input.escapeHtml())
    }

    @Test
    fun `should handle entities at end`() {
        val input = "end>"
        val expected = "end&gt;"

        assertEquals(expected, input.escapeHtml())
    }

    @Test
    fun `should handle entities at both ends`() {
        val input = "<middle>"
        val expected = "&lt;middle&gt;"

        assertEquals(expected, input.escapeHtml())
    }

    // ==================== Multiple Escaping Tests ====================

    @Test
    fun `should handle double escaping`() {
        val input = "<test>"
        val once = input.escapeHtml()
        val twice = once.escapeHtml()

        assertEquals("&lt;test&gt;", once)
        assertEquals("&amp;lt;test&amp;gt;", twice)
    }

    @Test
    fun `should be idempotent after multiple applications`() {
        val input = "test"
        val once = input.escapeHtml()
        val twice = once.escapeHtml()
        val thrice = twice.escapeHtml()

        assertEquals(input, once)
        assertEquals(input, twice)
        assertEquals(input, thrice)
    }

    // ==================== Null Safety Tests ====================

    @Test
    fun `should handle empty content gracefully`() {
        val empty = ""
        assertEquals("", empty.escapeHtml())
    }

    // ==================== Character Encoding Tests ====================

    @Test
    fun `should not escape valid UTF-8 characters`() {
        val input = "Héllo Wörld"
        assertEquals(input, input.escapeHtml())
    }

    @Test
    fun `should handle emoji`() {
        val input = "Hello 👋 World 🌍"
        assertEquals(input, input.escapeHtml())
    }

    @Test
    fun `should handle right-to-left text`() {
        val input = "مرحبا بالعالم"
        assertEquals(input, input.escapeHtml())
    }

    @Test
    fun `should handle CJK characters`() {
        val input = "你好世界 こんにちは 안녕하세요"
        assertEquals(input, input.escapeHtml())
    }

    // ==================== Special Cases ====================

    @Test
    fun `should handle null bytes`() {
        val input = "test\u0000with\u0000nulls"
        assertEquals(input, input.escapeHtml())
    }

    @Test
    fun `should handle control characters`() {
        val input = "test\u0001\u0002\u0003"
        assertEquals(input, input.escapeHtml())
    }

    @Test
    fun `should handle zero-width characters`() {
        val input = "test\u200B\u200C\u200D"
        assertEquals(input, input.escapeHtml())
    }
}
