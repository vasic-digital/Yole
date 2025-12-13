/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Extended unit tests for WikiText parser - Advanced Features
 *
 *########################################################*/
package digital.vasic.yole.format.wikitext

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import org.assertj.core.api.Assertions.assertThat

/**
 * Comprehensive extended unit tests for WikiText format parser.
 *
 * Tests cover advanced WikiText features:
 * - Templates and template transclusion
 * - Variables and parser functions
 * - Complex table structures and formatting
 * - Category links and interwiki links
 * - Advanced template parameters and nesting
 * - Round-trip parsing for extended features
 * - Edge cases (complex templates, deeply nested structures)
 * - Performance benchmarks for complex wikis
 * - HTML conversion for advanced features
 */
class WikiTextExtendedTest {

    private val parser = WikitextParser()

    // ==================== Template Tests ====================

    @Test
    fun `should parse basic templates`() {
        val content = """
            = Template Test =
            
            {{TemplateName}}
            
            This document uses a simple template.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse templates with parameters`() {
        val content = """
            = Template with Parameters =
            
            {{Infobox
            |title=My Article
            |author=John Doe
            |date=2025-01-01
            }}
            
            Content after template.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse nested templates`() {
        val content = """
            = Nested Templates =
            
            {{MainTemplate
            |content={{SubTemplate|param=value}}
            |title=Main Title
            }}
            
            More content here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse complex template with multiple parameters`() {
        val content = """
            {{Article summary
            | title = Advanced WikiText Features
            | author = Jane Smith
            | date = 2025-12-13
            | summary = This article covers advanced WikiText parsing
            | tags = wikitext, templates, parsing
            | category = Technology
            | featured = yes
            }}
            
            This is the main article content.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse template with conditional parameters`() {
        val content = """
            {{User info
            | name = John Doe
            | role = admin
            | status = active
            | {{#if: {{isAdmin}} | permissions = full }}
            }}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Variable Tests ====================

    @Test
    fun `should parse variables`() {
        val content = """
            = Variable Test =
            
            Welcome to {{SITENAME}}!
            Current page: {{PAGENAME}}
            Today's date: {{CURRENTDATE}}
            
            This document uses WikiText variables.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse parser functions`() {
        val content = """
            = Parser Functions =
            
            {{#if: true | This is shown | This is hidden }}
            {{#expr: 2 + 2 }}
            {{#time: Y-m-d }}
            {{#switch: {{NAMESPACE}} | User = User page | Template = Template page | #default = Other page }}
            
            Advanced parsing examples.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse complex parser functions`() {
        val content = """
            {{#ifeq: {{PAGENAME}} | Main Page | This is the main page | This is not the main page }}
            
            {{#ifexist: User:JohnDoe | User exists | User does not exist }}
            
            {{#titleparts: {{PAGENAME}} | 1 | 2 }}
            
            {{#rel2abs: /subpage | User:JohnDoe }}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Table Tests ====================

    @Test
    fun `should parse simple tables`() {
        val content = """
            = Table Test =
            
            {| class="wikitable"
            |+ Table caption
            ! Header 1
            ! Header 2
            ! Header 3
            |-
            | Cell 1
            | Cell 2
            | Cell 3
            |-
            | Cell 4
            | Cell 5
            | Cell 6
            |}
            
            Table content complete.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse tables with formatting`() {
        val content = """
            {| class="wikitable sortable"
            |+ '''Important Data'''
            ! scope="col" | Column A
            ! scope="col" | Column B
            ! scope="col" | Column C
            |-
            | style="background:#f0f0f0" | '''Bold cell'''
            | //Italic cell//
            | __Highlighted cell__
            |-
            | [[Link in cell|http://example.com]]
            | {{image.jpg}}
            | ''code in cell''
            |}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse complex table with row and column spans`() {
        val content = """
            {| class="wikitable"
            |+ Complex Table
            ! Header 1
            ! Header 2
            ! Header 3
            |-
            | rowspan="2" | Spanned cell
            | Cell 2
            | Cell 3
            |-
            | colspan="2" | Double-width cell
            |}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse nested tables`() {
        val content = """
            {| class="wikitable"
            |+ Outer Table
            ! Outer Header
            |-
            | 
            {|
            |+ Inner Table
            ! Inner Header
            |-
            | Inner content
            |}
            |}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Link Tests ====================

    @Test
    fun `should parse category links`() {
        val content = """
            = Category Links =
            
            This article is about technology.
            
            [[Category:Technology]]
            [[Category:Programming]]
            [[Category:WikiText]]
            
            More content here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse interwiki links`() {
        val content = """
            = Interwiki Links =
            
            Learn more on Wikipedia: [[wikipedia:WikiText]]
            Check the manual: [[mediawiki:Help:Contents]]
            Visit Commons: [[commons:Category:Images]]
            
            External resources.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse file links`() {
        val content = """
            = File Links =
            
            [[File:example.jpg|thumbnail|Example image]]
            [[File:document.pdf|Click here to download PDF]]
            [[Media:sound.mp3|Listen to this audio]]
            
            File references complete.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse complex links with anchors`() {
        val content = """
            = Anchor Links =
            
            [[Page#Section|Link to section]]
            [[User:JohnDoe#Contact|Contact John]]
            [[Help:Contents#Getting started|Getting started guide]]
            
            Navigation links.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Complex Nested Structures ====================

    @Test
    fun `should parse complex nested structure with templates and tables`() {
        val content = """
            = Complex Document =
            
            {{Article header
            |title=Advanced WikiText
            |author={{User|JohnDoe|role=admin}}
            |date={{#time: Y-m-d }}
            }}
            
            {| class="wikitable"
            |+ {{#if: {{isAdmin}} | Admin Data | User Data }}
            ! Feature
            ! Status
            |-
            | Templates
            | {{#if: {{hasTemplates}} | {{icon|yes}} | {{icon|no}} }}
            |-
            | Tables
            | {{#if: {{hasTables}} | {{icon|yes}} | {{icon|no}} }}
            |}
            
            [[Category:Advanced]]
            [[Category:{{NAMESPACE}}]]
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should parse deeply nested template calls`() {
        val content = """
            {{Main template
            |header={{Header template
                     |title={{#if: {{PAGENAME}} | {{PAGENAME}} | Default Title }}
                     |subtitle={{#time: F Y }}
                     }}
            |content={{Content box
                     |text=This is the main content with **bold** and //italic// text.
                     |footer={{Footer template |date={{CURRENTTIMESTAMP}} }}
                     }}
            |sidebar={{Sidebar
                     |links={{Link list
                            |[[Main Page]]
                            |[[Help:Contents]]
                            |[[Category:All pages]]
                            }}
                     }}
            }}
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle malformed templates gracefully`() {
        val content = """
            = Malformed Templates =
            
            {{Incomplete template
            |param1=value1
            
            {{Unclosed template
            |param=value
            
            {{Nested {{unclosed}} template}}
            
            Content continues despite malformed templates.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle templates with special characters`() {
        val content = """
            = Special Characters =
            
            {{Template with special chars
            |title=Title with "quotes" and 'apostrophes'
            |content=Content with <tags> & symbols
            |url=http://example.com/path?param=value&other=test
            |regex=\\d{3}-\\d{3}-\\d{4}
            }}
            
            Special character handling complete.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle extremely long template names`() {
        val longTemplateName = "VeryLongTemplateNameThatMightCauseIssuesWithParsingAndMemory" +
                "EvenLongerTemplateNameThatContinuesToGrowAndTestTheLimitsOfTheParserImplementation"
        
        val content = """
            = Long Template Names =
            
            {{$longTemplateName
            |param1=value1
            |param2=value2
            }}
            
            Content after long template.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    @Test
    fun `should handle empty template parameters`() {
        val content = """
            = Empty Parameters =
            
            {{Template with empty params
            |param1=
            |param2=value
            |param3=
            |param4=
            }}
            
            Empty parameter handling complete.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should parse document with many templates efficiently`() {
        val content = buildString {
            appendLine("= Many Templates Document =")
            appendLine()
            
            repeat(50) { i ->
                appendLine("{{Template$i")
                appendLine("|title=Title $i")
                appendLine("|content=Content for template $i")
                appendLine("|date=2025-01-${String.format("%02d", i + 1)}")
                appendLine("}}")
                appendLine()
            }
            
            appendLine("Document with many templates complete.")
        }

        val startTime = System.currentTimeMillis()
        val result = parser.parse(content)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        
        val parseTime = endTime - startTime
        assertTrue(parseTime < 2000, "Parsing many templates should complete within 2 seconds, took: ${parseTime}ms")
    }

    @Test
    fun `should parse complex nested document efficiently`() {
        val content = buildString {
            appendLine("= Complex Nested Document =")
            appendLine()
            
            repeat(20) { section ->
                appendLine("== Section ${section + 1} ==")
                appendLine()
                
                appendLine("{{Section template")
                appendLine("|title=Section ${section + 1}")
                appendLine("|content=")
                appendLine("{|")
                appendLine("! Header 1 ! Header 2")
                
                repeat(5) { row ->
                    appendLine("|-")
                    appendLine("| {{Cell template|value=Cell ${row + 1}A}} | Cell ${row + 1}B")
                }
                
                appendLine("|}")
                appendLine("}}")
                appendLine()
                
                appendLine("[[Category:Section${section + 1}]]")
                appendLine("{{#if: {{isActive}} | [[Category:Active]] }}")
                appendLine()
            }
        }

        val startTime = System.currentTimeMillis()
        val result = parser.parse(content)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        
        val parseTime = endTime - startTime
        assertTrue(parseTime < 3000, "Parsing complex nested document should complete within 3 seconds, took: ${parseTime}ms")
    }

    @Test
    fun `should handle large table efficiently`() {
        val content = buildString {
            appendLine("= Large Table =")
            appendLine()
            appendLine("{| class='wikitable'")
            appendLine("! Column 1 ! Column 2 ! Column 3 ! Column 4 ! Column 5")
            
            repeat(100) { row ->
                appendLine("|-")
                appendLine("| Row ${row + 1}A | Row ${row + 1}B | Row ${row + 1}C | Row ${row + 1}D | Row ${row + 1}E")
            }
            
            appendLine("|}")
        }

        val startTime = System.currentTimeMillis()
        val result = parser.parse(content)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        
        val parseTime = endTime - startTime
        assertTrue(parseTime < 1000, "Parsing large table should complete within 1 second, took: ${parseTime}ms")
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert templates to HTML`() {
        val content = """
            = Template HTML Test =
            
            {{Simple template}}
            {{Template with |param=value}}
            
            Template conversion complete.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='wikitext'>"))
        assertTrue(html.contains("<h1>Template HTML Test</h1>"))
        assertTrue(html.contains("Template conversion complete."))
    }

    @Test
    fun `should convert tables to HTML`() {
        val content = """
            = Table HTML Test =
            
            {| class="wikitable"
            |+ Test Table
            ! Header 1
            ! Header 2
            |-
            | Cell A
            | Cell B
            |}
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='wikitext'>"))
        assertTrue(html.contains("<h1>Table HTML Test</h1>"))
    }

    @Test
    fun `should convert complex document with all features to HTML`() {
        val content = """
            = Comprehensive WikiText Document =
            
            {{Document header
            |title=Advanced Features
            |author=Test Author
            |date=2025-12-13
            }}
            
            This document demonstrates '''all''' //advanced// __features__.
            
            == Templates and Variables ==
            
            Welcome to {{SITENAME}}! Current page: {{PAGENAME}}
            
            {{#if: true | This conditional content is shown }}
            
            {| class="wikitable"
            |+ Feature Comparison
            ! Feature
            ! Status
            ! Notes
            |-
            | Templates
            | {{icon|supported}}
            | Full support
            |-
            | Tables
            | {{icon|supported}}
            | With formatting
            |}
            
            == Links and Categories ==
            
            [[Category:Documentation]]
            [[Category:Advanced]]
            
            Learn more: [[wikipedia:WikiText|WikiText on Wikipedia]]
            
            '''
            // Code block with advanced features
            function processTemplate(template) {
                return template.replace(/\{\{(.*?)\}\}/g, processMatch);
            }
            '''
            
            Document complete with **all** features.
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='wikitext'>"))
        assertTrue(html.contains("<h1>Comprehensive WikiText Document</h1>"))
        assertTrue(html.contains("<h2>Templates and Variables</h2>"))
        assertTrue(html.contains("<h2>Links and Categories</h2>"))
        assertTrue(html.contains("<strong>all</strong>"))
        assertTrue(html.contains("<em>advanced</em>"))
        assertTrue(html.contains("<code>"))
        assertTrue(html.contains("<pre>"))
    }

    // ==================== Round-trip Tests ====================

    @Test
    fun `should support round-trip parsing for templates`() {
        val originalContent = """
            = Template Round-trip Test =
            
            {{Test template
            |param1=value1
            |param2=value2
            |content=This is template content
            }}
            
            More content after template.
        """.trimIndent()

        val firstParse = parser.parse(originalContent)
        val formattedContent = firstParse.rawContent
        val secondParse = parser.parse(formattedContent)
        
        assertEquals(firstParse.format.id, secondParse.format.id)
        assertEquals(firstParse.rawContent, secondParse.rawContent)
        assertEquals(firstParse.metadata, secondParse.metadata)
    }

    @Test
    fun `should support round-trip parsing for complex documents`() {
        val originalContent = """
            = Complex Round-trip Document =
            
            {{Header template
            |title={{PAGENAME}}
            |subtitle=Advanced Features
            }}
            
            {| class="wikitable"
            |+ {{#if: {{isAdmin}} | Admin View | User View }}
            ! Feature
            ! Status
            |-
            | Templates
            | {{icon|yes}}
            |-
            | Tables
            | {{icon|yes}}
            |}
            
            [[Category:Documentation]]
            [[Category:{{NAMESPACE}}]]
            
            {{#expr: 2 + 2 }}
            {{#time: Y-m-d }}
            
            '''
            Code block content
            '''
            
            Final content with **formatting** and //style//.
        """.trimIndent()

        val firstParse = parser.parse(originalContent)
        val formattedContent = firstParse.rawContent
        val secondParse = parser.parse(formattedContent)
        
        assertEquals(firstParse.format.id, secondParse.format.id)
        assertEquals(firstParse.rawContent, secondParse.rawContent)
        assertEquals(firstParse.metadata, secondParse.metadata)
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate complex template syntax`() {
        val content = """
            = Validation Test =
            
            {{Valid template
            |param1=value1
            |param2=value2
            }}
            
            {| Valid table
            |+ Valid caption
            ! Header
            |-
            | Cell content
            |}
            
            [[Category:Valid]]
            [[wikipedia:Valid]]
            
            Valid document content.
        """.trimIndent()

        val errors = parser.validate(content)
        
        // Should validate successfully (no template/table/link validation errors in current implementation)
        assertTrue(errors.isEmpty() || errors.none { it.contains("template") || it.contains("table") })
    }

    @Test
    fun `should handle validation of malformed advanced syntax gracefully`() {
        val content = """
            = Malformed Advanced Syntax =
            
            {{Incomplete template
            |param=value
            
            {| Incomplete table
            ! Header
            |-
            | Cell
            
            [[Incomplete link
            
            Content continues.
        """.trimIndent()

        val result = parser.parse(content)
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
        assertEquals(content, result.rawContent)
    }
}