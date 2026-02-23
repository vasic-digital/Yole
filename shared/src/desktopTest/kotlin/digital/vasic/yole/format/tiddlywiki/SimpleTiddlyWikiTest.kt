/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple test for TiddlyWiki parser
 *
 *########################################################*/
package digital.vasic.yole.format.tiddlywiki

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Simple test to verify TiddlyWiki parser functionality
 */
class SimpleTiddlyWikiTest {

    private val parser = TiddlyWikiParser()

    @Test
    fun `should parse basic TiddlyWiki document`() {
        val content = """
            title: My First Tiddler
            tags: introduction tutorial
            created: 20250101120000000
            modified: 20250102150000000
            
            This is the content of my first tiddler.
            It can span multiple lines and contain various formatting.
            
            ! Heading 1
            Some content under the heading.
            
            !! Heading 2
            More content here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_TIDDLYWIKI, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Check metadata extraction
        assertEquals("My First Tiddler", result.metadata["title"])
        assertEquals("introduction, tutorial", result.metadata["tags"])
        assertEquals("", result.metadata["extension"])
        assertTrue(result.metadata["lines"]!!.toInt() > 0)
        
        // Check HTML conversion
        assertTrue(result.parsedContent.contains("tiddlywiki"))
        assertTrue(result.parsedContent.contains("My First Tiddler"))
        assertTrue(result.parsedContent.contains("Heading 1"))
        assertTrue(result.parsedContent.contains("Heading 2"))
    }

    @Test
    fun `should convert TiddlyWiki to HTML`() {
        val content = """
            title: HTML Test
            tags: test
            
            ! Main Heading
            This is ''bold'' text and //italic// text.
            
            * List item 1
            * List item 2
            
            Here is a [[Wiki Link]] and an [ext[https://example.com|external link]].
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("tiddlywiki"))
        assertTrue(html.contains("Main Heading"))
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<em>italic</em>"))
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<a href='Wiki Link'>Wiki Link</a>"))
        assertTrue(html.contains("target='_blank'"))
    }

    @Test
    fun `should validate well-formed TiddlyWiki`() {
        val content = """
            title: Valid Tiddler
            tags: valid test
            
            ! Valid heading
            Valid content with ''bold'' text.
            
            * List item 1
            * List item 2
            
            [[Valid Wiki Link]] and [ext[https://example.com]].
        """.trimIndent()

        val issues = parser.validate(content)

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `should detect unclosed brackets in links`() {
        val content = """
            title: Invalid Links
            
            Broken link: [[Unclosed Tiddler
            Another broken: [ext[https://example.com
            
            Regular text continues.
        """.trimIndent()

        val issues = parser.validate(content)

        assertTrue(issues.isNotEmpty())
        assertTrue(issues.any { it.contains("Unclosed brackets") })
    }
}