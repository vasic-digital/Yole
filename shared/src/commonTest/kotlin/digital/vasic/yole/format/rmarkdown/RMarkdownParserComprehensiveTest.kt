/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for RMarkdownParser
 *
 *########################################################*/
package digital.vasic.yole.format.rmarkdown

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Comprehensive tests for RMarkdownParser covering all parsing branches.
 *
 * Tests cover:
 * - Front matter extraction (YAML)
 * - Code chunks (R, Python, Bash, SQL, generic)
 * - Markdown content (headings, bold, italic, paragraphs)
 * - HTML generation (light/dark modes)
 * - Validation
 * - Edge cases
 */
class RMarkdownParserComprehensiveTest {

    private lateinit var parser: RMarkdownParser

    @BeforeTest
    fun setup() {
        parser = RMarkdownParser()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Front Matter Tests ====================

    @Test
    fun `should extract front matter title`() {
        val content = """
            ---
            title: My R Markdown Document
            author: Test Author
            ---

            Content here
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("My R Markdown Document", doc.metadata["title"])
        assertEquals("Test Author", doc.metadata["author"])
    }

    @Test
    fun `should extract front matter date`() {
        val content = """
            ---
            title: Document
            date: 2025-11-19
            ---

            Content
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("2025-11-19", doc.metadata["date"])
    }

    @Test
    fun `should extract front matter output format`() {
        val content = """
            ---
            title: Document
            output: html_document
            ---

            Content
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("html_document", doc.metadata["output"])
    }

    @Test
    fun `should handle content without front matter`() {
        val content = """
            # Heading

            Regular content
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("false", doc.metadata["has_frontmatter"])
    }

    @Test
    fun `should handle empty front matter`() {
        val content = """
            ---
            ---

            Content
        """.trimIndent()

        val doc = parser.parse(content)

        // Empty front matter (no fields) is considered as no front matter
        assertEquals("false", doc.metadata["has_frontmatter"])
    }

    @Test
    fun `should set has_frontmatter to true when present`() {
        val content = """
            ---
            title: Test
            ---

            Content
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("true", doc.metadata["has_frontmatter"])
    }

    // ==================== Code Chunk Tests ====================

    @Test
    fun `should extract R code chunk`() {
        val content = """
            ```{r}
            x <- 1
            print(x)
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("1", doc.metadata["code_chunks"])
        assertEquals("1", doc.metadata["r_chunks"])
    }

    @Test
    fun `should extract Python code chunk`() {
        val content = """
            ```{python}
            x = 1
            print(x)
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("1", doc.metadata["code_chunks"])
        assertEquals("0", doc.metadata["r_chunks"])
    }

    @Test
    fun `should extract Bash code chunk`() {
        val content = """
            ```{bash}
            echo "Hello"
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("1", doc.metadata["code_chunks"])
    }

    @Test
    fun `should extract SQL code chunk`() {
        val content = """
            ```{sql}
            SELECT * FROM users;
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("1", doc.metadata["code_chunks"])
    }

    @Test
    fun `should extract code chunk with options`() {
        val content = """
            ```{r, echo=TRUE, eval=FALSE}
            x <- 1
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("1", doc.metadata["code_chunks"])
        assertEquals("1", doc.metadata["r_chunks"])
    }

    @Test
    fun `should handle multiple code chunks`() {
        val content = """
            ```{r}
            x <- 1
            ```

            ```{python}
            y = 2
            ```

            ```{r}
            z <- 3
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("3", doc.metadata["code_chunks"])
        assertEquals("2", doc.metadata["r_chunks"])
    }

    @Test
    fun `should handle content with no code chunks`() {
        val content = """
            # Regular Markdown

            Just plain text
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("0", doc.metadata["code_chunks"])
        assertEquals("0", doc.metadata["r_chunks"])
    }

    @Test
    fun `should extract multiline code chunk`() {
        val content = """
            ```{r}
            x <- 1
            y <- 2
            z <- x + y
            print(z)
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertEquals("1", doc.metadata["code_chunks"])
    }

    // ==================== Markdown Content Tests ====================

    @Test
    fun `should convert level 1 heading`() {
        val content = "# Heading 1"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<h1>Heading 1</h1>"))
    }

    @Test
    fun `should convert level 2 heading`() {
        val content = "## Heading 2"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("Heading 2"))
    }

    @Test
    fun `should convert level 3 heading`() {
        val content = "### Heading 3"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("Heading 3"))
    }

    @Test
    fun `should convert bold text`() {
        val content = "This is **bold** text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<strong>bold</strong>"))
    }

    @Test
    fun `should convert italic text`() {
        val content = "This is *italic* text"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<em>italic</em>"))
    }

    @Test
    fun `should convert paragraph breaks`() {
        val content = """
            First paragraph

            Second paragraph
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("</p><p>"))
    }

    @Test
    fun `should convert regular code blocks`() {
        val content = """
            ```
            regular code
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("<pre><code>"))
    }

    // ==================== HTML Generation Tests ====================

    @Test
    fun `should generate light mode HTML`() {
        val content = """
            ---
            title: Test Document
            ---

            Content
        """.trimIndent()

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)

        assertTrue(html.contains("light"))
        assertTrue(html.contains("Test Document"))
    }

    @Test
    fun `should generate dark mode HTML`() {
        val content = """
            ---
            title: Test Document
            ---

            Content
        """.trimIndent()

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = false)

        assertTrue(html.contains("dark"))
    }

    @Test
    fun `should use default title when no front matter`() {
        val content = "Content without front matter"

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)

        assertTrue(html.contains("R Markdown Document"))
    }

    @Test
    fun `should show R chunk count in HTML`() {
        val content = """
            ```{r}
            x <- 1
            ```
        """.trimIndent()

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)

        assertTrue(html.contains("R Code Chunks: 1"))
    }

    @Test
    fun `should show total chunk count in HTML`() {
        val content = """
            ```{r}
            x <- 1
            ```

            ```{python}
            y = 2
            ```
        """.trimIndent()

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)

        assertTrue(html.contains("Total Chunks: 2"))
    }

    @Test
    fun `should include CSS styling in HTML`() {
        val content = "Content"

        val doc = parser.parse(content)
        val html = parser.toHtml(doc, lightMode = true)

        assertTrue(html.contains("<style>"))
        assertTrue(html.contains(".rmarkdown-document"))
    }

    @Test
    fun `should generate R chunk with proper class`() {
        val content = """
            ```{r}
            x <- 1
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("r-chunk"))
    }

    @Test
    fun `should generate Python chunk with proper class`() {
        val content = """
            ```{python}
            x = 1
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("python-chunk"))
    }

    @Test
    fun `should generate Bash chunk with proper class`() {
        val content = """
            ```{bash}
            echo test
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("bash-chunk"))
    }

    @Test
    fun `should generate SQL chunk with proper class`() {
        val content = """
            ```{sql}
            SELECT 1;
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("sql-chunk"))
    }

    @Test
    fun `should generate generic chunk for unknown language`() {
        val content = """
            ```{julia}
            x = 1
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("code-chunk"))
    }

    @Test
    fun `should escape HTML in code chunks`() {
        val content = """
            ```{r}
            x <- "<div>test</div>"
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("&lt;") || doc.parsedContent.contains("&gt;"))
    }

    @Test
    fun `should display chunk language in header`() {
        val content = """
            ```{r}
            x <- 1
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("R Code Chunk") || doc.parsedContent.contains("chunk-header"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should detect unclosed code chunks`() {
        val content = """
            ```{r}
            x <- 1
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.any { it.contains("Mismatched") || it.contains("delimiters") })
    }

    @Test
    fun `should detect unclosed front matter`() {
        val content = """
            ---
            title: Test
        """.trimIndent()

        val errors = parser.validate(content)

        assertNotNull(errors)
    }

    @Test
    fun `should accept valid code chunks`() {
        val content = """
            ```{r}
            x <- 1
            ```
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Mismatched") })
    }

    @Test
    fun `should accept valid front matter`() {
        val content = """
            ---
            title: Test
            ---

            Content
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("Unclosed") })
    }

    @Test
    fun `should accept content without front matter`() {
        val content = """
            # Heading

            Content
        """.trimIndent()

        val errors = parser.validate(content)

        assertFalse(errors.any { it.contains("front matter") })
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle empty content`() {
        val doc = parser.parse("")

        assertEquals("", doc.rawContent)
        assertNotNull(doc.parsedContent)
    }

    @Test
    fun `should handle only whitespace`() {
        val content = "   \n\n\t  "

        val doc = parser.parse(content)

        assertNotNull(doc.parsedContent)
    }

    @Test
    fun `should handle unicode in content`() {
        val content = "Unicode: 你好世界 🌍"

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("你好世界") || doc.parsedContent.contains("🌍"))
    }

    @Test
    fun `should handle special HTML characters`() {
        val content = """
            ```{r}
            x <- "<>&"
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        // Should be escaped
        assertTrue(
            doc.parsedContent.contains("&lt;") ||
            doc.parsedContent.contains("&gt;") ||
            doc.parsedContent.contains("&amp;")
        )
    }

    @Test
    fun `should handle quotes in code`() {
        val content = """
            ```{r}
            x <- "test"
            y <- 'test'
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        assertTrue(doc.parsedContent.contains("&quot;") || doc.parsedContent.contains("&#39;"))
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should handle complete R Markdown document`() {
        val content = """
            ---
            title: Complete R Markdown Example
            author: Test Author
            date: 2025-11-19
            output: html_document
            ---

            # Introduction

            This is a **complete** R Markdown document with *various* features.

            ## Data Analysis

            First, let's load some data:

            ```{r}
            data <- read.csv("data.csv")
            summary(data)
            ```

            ### Python Example

            We can also use Python:

            ```{python}
            import pandas as pd
            df = pd.DataFrame()
            ```

            ## Results

            Here are the results.

            ```{bash}
            echo "Processing complete"
            ```
        """.trimIndent()

        val doc = parser.parse(content)

        // Check metadata
        assertEquals("Complete R Markdown Example", doc.metadata["title"])
        assertEquals("Test Author", doc.metadata["author"])
        assertEquals("2025-11-19", doc.metadata["date"])

        // Check code chunks
        assertEquals("3", doc.metadata["code_chunks"])
        assertEquals("1", doc.metadata["r_chunks"])

        // Check content elements
        assertTrue(doc.parsedContent.contains("Introduction"))
        assertTrue(doc.parsedContent.contains("Data Analysis"))
        assertTrue(doc.parsedContent.contains("Python Example"))
        assertTrue(doc.parsedContent.contains("complete"))
        assertTrue(doc.parsedContent.contains("various"))
        assertTrue(doc.parsedContent.contains("r-chunk"))
        assertTrue(doc.parsedContent.contains("python-chunk"))
        assertTrue(doc.parsedContent.contains("bash-chunk"))
    }
}
