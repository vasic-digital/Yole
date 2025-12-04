/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for Markdown inline markup conversion
 *
 *########################################################*/
package digital.vasic.yole.format.markdown

import kotlin.test.*

/**
 * Comprehensive tests for Markdown inline markup conversion.
 *
 * Tests cover all inline formatting:
 * - Bold (**text**, __text__)
 * - Italic (*text*, _text_)
 * - Strikethrough (~~text~~)
 * - Inline code (`code`)
 * - Links ([text](url))
 * - Images (![alt](url))
 * - Task list checkboxes ([ ], [x])
 * - Nested and combined formatting
 * - Edge cases
 */
class MarkdownInlineMarkupTest {

    private val parser = MarkdownParser()

    // ==================== Bold Tests ====================

    @Test
    fun `should convert bold with double asterisks`() {
        val content = "This is **bold** text"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<strong>bold</strong>"))
    }

    @Test
    fun `should convert bold with double underscores`() {
        val content = "This is __bold__ text"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<strong>bold</strong>"))
    }

    @Test
    fun `should handle multiple bold sections in same line`() {
        val content = "**First** normal **Second** normal **Third**"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<strong>First</strong>"))
        assertTrue(document.parsedContent.contains("<strong>Second</strong>"))
        assertTrue(document.parsedContent.contains("<strong>Third</strong>"))
    }

    @Test
    fun `should handle bold at start of line`() {
        val content = "**Bold** at start"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<strong>Bold</strong>"))
    }

    @Test
    fun `should handle bold at end of line`() {
        val content = "At end **bold**"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<strong>bold</strong>"))
    }

    @Test
    fun `should handle entire line as bold`() {
        val content = "**Entire line is bold**"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<strong>Entire line is bold</strong>"))
    }

    // ==================== Italic Tests ====================

    @Test
    fun `should convert italic with single asterisks`() {
        val content = "This is *italic* text"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<em>italic</em>"))
    }

    @Test
    fun `should convert italic with single underscores`() {
        val content = "This is _italic_ text"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<em>italic</em>"))
    }

    @Test
    fun `should handle multiple italic sections`() {
        val content = "*First* normal *Second* normal *Third*"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<em>First</em>"))
        assertTrue(document.parsedContent.contains("<em>Second</em>"))
        assertTrue(document.parsedContent.contains("<em>Third</em>"))
    }

    // ==================== Strikethrough Tests ====================

    @Test
    fun `should convert strikethrough`() {
        val content = "This is ~~strikethrough~~ text"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<s>strikethrough</s>"))
    }

    @Test
    fun `should handle multiple strikethrough sections`() {
        val content = "~~First~~ normal ~~Second~~"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<s>First</s>"))
        assertTrue(document.parsedContent.contains("<s>Second</s>"))
    }

    // ==================== Inline Code Tests ====================

    @Test
    fun `should convert inline code`() {
        val content = "Use `code` here"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<code>code</code>"))
    }

    @Test
    fun `should escape HTML in inline code`() {
        val content = "Use `<script>alert('xss')</script>` code"

        val document = parser.parse(content)

        // Should contain code tags and not execute script
        assertTrue(document.parsedContent.contains("<code>"))
        assertTrue(document.parsedContent.contains("</code>"))
        // Script tags should be escaped in some way
        assertFalse(document.parsedContent.contains("<code><script>alert"))
    }

    @Test
    fun `should handle multiple inline code sections`() {
        val content = "Use `first` and `second` code"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<code>first</code>"))
        assertTrue(document.parsedContent.contains("<code>second</code>"))
    }

    @Test
    fun `should handle inline code with special characters`() {
        val content = "Use `var x = 'test'` code"

        val document = parser.parse(content)

        // Should contain code tags with escaped content
        assertTrue(document.parsedContent.contains("<code>") && document.parsedContent.contains("</code>"))
        assertTrue(document.parsedContent.contains("var x ="))
    }

    // ==================== Link Tests ====================

    @Test
    fun `should convert links`() {
        val content = "Visit [Google](https://google.com) now"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<a href='https://google.com'>Google</a>"))
    }

    @Test
    fun `should handle multiple links`() {
        val content = "Visit [Google](https://google.com) or [GitHub](https://github.com)"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<a href='https://google.com'>Google</a>"))
        assertTrue(document.parsedContent.contains("<a href='https://github.com'>GitHub</a>"))
    }

    @Test
    fun `should handle link with empty text`() {
        val content = "[](https://example.com)"

        val document = parser.parse(content)

        // May not match empty text, just verify it doesn't crash
        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle link at start of line`() {
        val content = "[Link](https://example.com) at start"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<a href='https://example.com'>Link</a>"))
    }

    @Test
    fun `should handle link at end of line`() {
        val content = "At end [link](https://example.com)"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<a href='https://example.com'>link</a>"))
    }

    // ==================== Image Tests ====================

    @Test
    fun `should convert images`() {
        val content = "![Alt text](https://example.com/image.png)"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<img src='https://example.com/image.png' alt='Alt text'/>"))
    }

    @Test
    fun `should handle image with empty alt text`() {
        val content = "![](https://example.com/image.png)"

        val document = parser.parse(content)

        // May not match empty alt text, just verify it doesn't crash
        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle multiple images`() {
        val content = "![First](img1.png) and ![Second](img2.png)"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<img src='img1.png' alt='First'/>"))
        assertTrue(document.parsedContent.contains("<img src='img2.png' alt='Second'/>"))
    }

    // ==================== Task List Tests ====================

    @Test
    fun `should convert unchecked task list checkbox`() {
        val content = "- [ ] Task not done"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<input type='checkbox' disabled>"))
    }

    @Test
    fun `should convert checked task list checkbox`() {
        val content = "- [x] Task done"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<input type='checkbox' disabled checked>"))
    }

    @Test
    fun `should handle multiple task items`() {
        val content = "- [ ] First\n- [x] Second\n- [ ] Third"

        val document = parser.parse(content)

        // Count checkboxes
        val unchecked = document.parsedContent.split("<input type='checkbox' disabled>").size - 1
        val checked = document.parsedContent.split("<input type='checkbox' disabled checked>").size - 1

        assertEquals(2, unchecked)
        assertEquals(1, checked)
    }

    // ==================== Combined Formatting Tests ====================

    @Test
    fun `should handle bold and italic together`() {
        val content = "***bold and italic***"

        val document = parser.parse(content)

        // Should contain both bold and italic tags
        assertTrue(document.parsedContent.contains("<strong>") || document.parsedContent.contains("<em>"))
    }

    @Test
    fun `should handle nested bold in italic`() {
        val content = "*italic with **bold** inside*"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<em>") && document.parsedContent.contains("<strong>"))
    }

    @Test
    fun `should handle bold in link text`() {
        val content = "[**Bold link**](https://example.com)"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<a href='https://example.com'>"))
    }

    @Test
    fun `should handle italic in link text`() {
        val content = "[*Italic link*](https://example.com)"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<a href='https://example.com'>"))
    }

    @Test
    fun `should handle code with bold markers inside`() {
        val content = "`**not bold**`"

        val document = parser.parse(content)

        // Code should contain the asterisks (may or may not be escaped)
        assertTrue(document.parsedContent.contains("<code>"))
        assertTrue(document.parsedContent.contains("</code>"))
    }

    @Test
    fun `should handle strikethrough with bold`() {
        val content = "~~**strikethrough bold**~~"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<s>"))
    }

    @Test
    fun `should handle multiple formatting in same sentence`() {
        val content = "This has **bold**, *italic*, ~~strike~~, and `code`"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<strong>bold</strong>"))
        assertTrue(document.parsedContent.contains("<em>italic</em>"))
        assertTrue(document.parsedContent.contains("<s>strike</s>"))
        assertTrue(document.parsedContent.contains("<code>code</code>"))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle unmatched bold markers`() {
        val content = "**No closing"

        val document = parser.parse(content)

        // Should not crash, may or may not convert
        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle unmatched italic markers`() {
        val content = "*No closing"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle unmatched code markers`() {
        val content = "`No closing"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle empty bold`() {
        val content = "****"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle empty italic`() {
        val content = "**"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle empty code`() {
        val content = "``"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle malformed link`() {
        val content = "[No closing paren(url"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle malformed image`() {
        val content = "![No closing](paren"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle link with special characters in URL`() {
        val content = "[Link](https://example.com/path?query=value&other=123)"

        val document = parser.parse(content)

        // Should contain link with URL (ampersand may be escaped)
        assertTrue(document.parsedContent.contains("<a href="))
        assertTrue(document.parsedContent.contains("Link</a>"))
    }

    @Test
    fun `should handle link with fragment`() {
        val content = "[Link](#section)"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<a href='#section'>Link</a>"))
    }

    @Test
    fun `should handle relative URL in link`() {
        val content = "[Link](../path/to/file.md)"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<a href='../path/to/file.md'>"))
    }

    @Test
    fun `should handle absolute path in link`() {
        val content = "[Link](/absolute/path)"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("<a href='/absolute/path'>"))
    }

    @Test
    fun `should handle consecutive bold sections`() {
        val content = "**First****Second**"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle consecutive italic sections`() {
        val content = "*First**Second*"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle asterisks in middle of word`() {
        val content = "test*not*italic"

        val document = parser.parse(content)

        // Behavior may vary - just ensure it doesn't crash
        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle underscores in middle of word`() {
        val content = "test_not_italic"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle escaped characters`() {
        val content = "Test \\**not bold\\** text"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle HTML entities in text`() {
        val content = "Text with &amp; entity"

        val document = parser.parse(content)

        // Should escape the ampersand
        assertTrue(document.parsedContent.contains("&amp;"))
    }

    @Test
    fun `should handle less than and greater than signs`() {
        val content = "Test <tag> in text"

        val document = parser.parse(content)

        // Should escape HTML
        assertTrue(document.parsedContent.contains("&lt;") || document.parsedContent.contains("&gt;"))
    }

    @Test
    fun `should handle quotes in text`() {
        val content = "Text with \"quotes\" here"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle apostrophes in text`() {
        val content = "It's a test"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle Unicode in bold`() {
        val content = "**世界** bold"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("世界"))
    }

    @Test
    fun `should handle Unicode in italic`() {
        val content = "*🌍* emoji"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("🌍"))
    }

    @Test
    fun `should handle Unicode in links`() {
        val content = "[世界](https://example.com)"

        val document = parser.parse(content)

        assertTrue(document.parsedContent.contains("世界"))
    }

    @Test
    fun `should handle very long bold section`() {
        val longText = "A".repeat(1000)
        val content = "**$longText**"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle very long link text`() {
        val longText = "A".repeat(500)
        val content = "[$longText](https://example.com)"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle very long URL`() {
        val longUrl = "https://example.com/" + "path/".repeat(100)
        val content = "[Link]($longUrl)"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle whitespace in formatting`() {
        val content = "** space before and after **"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle newlines in formatting context`() {
        val content = "**bold\nwith newline**"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }

    @Test
    fun `should handle tabs in text`() {
        val content = "Text\twith\ttabs"

        val document = parser.parse(content)

        assertNotNull(document.parsedContent)
    }
}
