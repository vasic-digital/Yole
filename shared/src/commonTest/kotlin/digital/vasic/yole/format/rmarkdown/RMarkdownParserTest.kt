/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive unit tests for R Markdown parser
 *
 *########################################################*/
package digital.vasic.yole.format.rmarkdown

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.BeforeTest


/**
 * Comprehensive unit tests for R Markdown format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic R Markdown parsing (YAML header, markdown content, R code chunks)
 * - R code chunks with various options
 * - Markdown formatting with R integration
 * - YAML metadata and front matter
 * - Round-trip parsing (parse -> format -> parse)
 * - Edge cases (empty files, malformed R Markdown)
 * - Performance benchmarks
 * - HTML conversion
 */
class RMarkdownParserTest {

    private val parser = RMarkdownParser()

    @BeforeTest
    fun setUp() {
        ParserRegistry.clear()
    }

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect R Markdown format by rmd extension`() {
        val format = FormatRegistry.getByExtension(".rmd")

        assertNotNull(format)
        assertEquals(TextFormat.ID_RMARKDOWN, format.id)
        assertEquals("R Markdown", format.name)
    }

    @Test
    fun `should detect R Markdown format by rmarkdown extension`() {
        val format = FormatRegistry.getByExtension(".rmarkdown")

        assertNotNull(format)
        assertEquals(TextFormat.ID_RMARKDOWN, format.id)
        assertEquals("R Markdown", format.name)
    }

    @Test
    fun `should detect R Markdown format by filename`() {
        val format = FormatRegistry.detectByFilename("analysis.rmd")

        assertNotNull(format)
        assertEquals(TextFormat.ID_RMARKDOWN, format.id)
    }

    @Test
    fun `should detect R Markdown format by content pattern`() {
        val content = """
            ---
            title: "Data Analysis"
            author: "John Doe"
            ---

            # Introduction

            ```{r}
            x <- 1:10
            mean(x)
            ```
        """.trimIndent()

        val format = FormatRegistry.detectByContent(content)

        assertNotNull(format)
        assertEquals(TextFormat.ID_RMARKDOWN, format.id)
    }

    // ==================== Basic R Markdown Parsing Tests ====================

    @Test
    fun `should parse basic R Markdown document structure`() {
        val content = """
            ---
            title: "Statistical Analysis"
            author: "Jane Smith"
            date: "2024-01-15"
            ---

            # Introduction

            This is a sample R Markdown document.

            ```{r}
            data <- c(1, 2, 3, 4, 5)
            mean(data)
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals(content, result.rawContent)

        // Check metadata extraction
        assertEquals("true", result.metadata["has_frontmatter"])
        assertEquals("1", result.metadata["code_chunks"])
        assertEquals("1", result.metadata["r_chunks"])
        assertEquals("Statistical Analysis", result.metadata["title"])
        assertEquals("Jane Smith", result.metadata["author"])
        assertEquals("2024-01-15", result.metadata["date"])
    }

    @Test
    fun `should parse R Markdown without YAML front matter`() {
        val content = """
            # Simple Analysis

            This is a simple R Markdown without front matter.

            ```{r}
            x <- 42
            print(x)
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("false", result.metadata["has_frontmatter"])
        assertEquals("1", result.metadata["code_chunks"])
        assertEquals("1", result.metadata["r_chunks"])
    }

    @Test
    fun `should parse R Markdown with multiple code chunks`() {
        val content = """
            ---
            title: "Multiple Chunks"
            ---

            # Data Preparation

            ```{r}
            library(ggplot2)
            data <- mtcars
            ```

            ## Analysis

            ```{r}
            summary(data)
            ```

            ## Visualization

            ```{r plot, fig.width=7, fig.height=5}
            ggplot(data, aes(x=mpg, y=hp)) +
              geom_point()
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("true", result.metadata["has_frontmatter"])
        assertEquals("3", result.metadata["code_chunks"])
        assertEquals("3", result.metadata["r_chunks"])
        assertEquals("Multiple Chunks", result.metadata["title"])
    }

    // ==================== R Code Chunks Tests ====================

    @Test
    fun `should parse R code chunks with various options`() {
        val content = """
            # Analysis with Options

            ```{r setup, include=FALSE}
            knitr::opts_chunk${'$'}set(echo = TRUE)
            ```

            ```{r data, echo=TRUE, warning=FALSE}
            data <- read.csv("data.csv")
            head(data)
            ```

            ```{r plot, fig.cap="Scatter plot", out.width='80%'}
            plot(data${'$'}x, data${'$'}y)
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("3", result.metadata["code_chunks"])
        assertEquals("3", result.metadata["r_chunks"])
    }

    @Test
    fun `should parse R code chunks with complex code`() {
        val content = """
            # Statistical Analysis

            ```{r}
            # Load required libraries
            library(dplyr)
            library(ggplot2)

            # Create sample data
            set.seed(123)
            n <- 100
            df <- data.frame(
              x = rnorm(n),
              y = rnorm(n, mean = 2),
              group = sample(c("A", "B", "C"), n, replace = TRUE)
            )

            # Summary statistics
            summary_stats <- df %>%
              group_by(group) %>%
              summarise(
                mean_x = mean(x),
                mean_y = mean(y),
                n = n()
              )

            print(summary_stats)
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("1", result.metadata["code_chunks"])
        assertEquals("1", result.metadata["r_chunks"])

        // Verify HTML contains the code. The R `<-` assignment operator MUST be
        // HTML-entity-escaped in the rendered output (P5-FIX-001 / CONST-039):
        // an unescaped `<` in document content is an XSS vector, so the code
        // chunk body legitimately appears as `&lt;-` in the HTML.
        val html = result.parsedContent
        assertTrue(html.contains("library(dplyr)"))
        assertTrue(html.contains("summary_stats &lt;- df"))
        assertTrue(html.contains("group_by(group)"))
    }

    @Test
    fun `should parse inline R code`() {
        val content = """
            # Inline Code

            The mean of x is `r mean(c(1,2,3,4,5))`.

            The current date is `r Sys.Date()`.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("false", result.metadata["has_frontmatter"])
        assertEquals("0", result.metadata["code_chunks"])
        assertEquals("0", result.metadata["r_chunks"])
    }

    // ==================== Markdown Content Tests ====================

    @Test
    fun `should parse markdown formatting with R integration`() {
        val content = """
            ---
            title: "Formatting Test"
            ---

            # Main Heading

            ## Subheading

            This is **bold** text and *italic* text.

            - List item 1
            - List item 2
              - Nested item

            > This is a blockquote
            > with multiple lines

            [Link to R documentation](https://www.r-project.org/)

            ```{r}
            # Code comment
            result <- 2 + 2
            print(result)
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("true", result.metadata["has_frontmatter"])
        assertEquals("1", result.metadata["code_chunks"])
        assertEquals("Formatting Test", result.metadata["title"])
    }

    @Test
    fun `should parse tables in R Markdown`() {
        val content = """
            # Data Table

            | Column 1 | Column 2 | Column 3 |
            |----------|----------|----------|
            | A        | B        | C        |
            | 1        | 2        | 3        |

            ```{r}
            # Create a data frame
df <- data.frame(
              Name = c("Alice", "Bob", "Charlie"),
              Age = c(25, 30, 35),
              Score = c(85, 92, 78)
            )
            print(df)
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("1", result.metadata["code_chunks"])
        assertEquals("1", result.metadata["r_chunks"])
    }

    // ==================== YAML Front Matter Tests ====================

    @Test
    fun `should parse YAML front matter with various data types`() {
        val content = """
            ---
            title: "Complex YAML"
            author: "Research Team"
            date: "2024-01-15"
            abstract: "This is a multi-line
              abstract that spans
              multiple lines"
            keywords: ["statistics", "analysis", "R"]
            output:
              html_document:
                toc: true
                toc_float: true
                theme: united
            params:
              data_file: "data.csv"
              alpha: 0.05
              sample_size: 1000
            ---

            # Document with Complex YAML

            ```{r}
            # Access YAML parameters
            print(params${'$'}data_file)
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("true", result.metadata["has_frontmatter"])
        assertEquals("Complex YAML", result.metadata["title"])
        assertEquals("Research Team", result.metadata["author"])
        assertEquals("2024-01-15", result.metadata["date"])
    }

    @Test
    fun `should handle empty YAML front matter`() {
        val content = """
            ---
            ---

            # Document with Empty YAML

            Some content here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("true", result.metadata["has_frontmatter"])
        assertEquals("0", result.metadata["code_chunks"])
        assertEquals("0", result.metadata["r_chunks"])
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun `should handle empty document`() {
        val content = ""

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("false", result.metadata["has_frontmatter"])
        assertEquals("0", result.metadata["code_chunks"])
        assertEquals("0", result.metadata["r_chunks"])
    }

    @Test
    fun `should handle document with only YAML front matter`() {
        val content = """
            ---
            title: "YAML Only"
            author: "Test Author"
            ---
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("true", result.metadata["has_frontmatter"])
        assertEquals("YAML Only", result.metadata["title"])
        assertEquals("Test Author", result.metadata["author"])
        assertEquals("0", result.metadata["code_chunks"])
        assertEquals("0", result.metadata["r_chunks"])
    }

    @Test
    fun `should handle malformed YAML front matter`() {
        val content = """
            ---
            title: "Test"
            invalid yaml without proper closing
            ---

            # Content

            Some text here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        // Should still parse the content after malformed YAML
        assertTrue(result.parsedContent.contains("Content"))
    }

    @Test
    fun `should handle unclosed code chunks`() {
        val content = """
            # Document with Error

            ```{r}
            x <- 1:10
            # Missing closing backticks

            More content here.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        // Should handle gracefully
        assertTrue(result.parsedContent.isNotEmpty())
    }

    @Test
    fun `should handle mismatched code chunk delimiters`() {
        val content = """
            # Mismatched Chunks

            ```{r}
            x <- 1:10
            ```

            ```{r}
            y <- 20:30
            # Missing closing

            More content.
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("2", result.metadata["code_chunks"]) // Should find both chunks
    }

    @Test
    fun `should handle R code chunks with syntax errors`() {
        val content = """
            # Code with Errors

            ```{r}
            # This code has syntax errors
            x <- 1:10
            y <- # incomplete assignment
            z <- mean(x, y  # missing closing parenthesis
            ```
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
        assertEquals("1", result.metadata["code_chunks"])
        // Should still parse the chunk content
        val html = result.parsedContent
        assertTrue(html.contains("incomplete assignment"))
        assertTrue(html.contains("missing closing parenthesis"))
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert simple R Markdown to HTML`() {
        val content = """
            ---
            title: "Simple Document"
            ---

            # Hello World

            This is a simple R Markdown document.

            ```{r}
            print("Hello, R Markdown!")
            ```
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("rmarkdown-document"))
        assertTrue(html.contains("Simple Document"))
        assertTrue(html.contains("Hello World"))
        assertTrue(html.contains("R Code Chunks: 1"))
        assertTrue(html.contains("Total Chunks: 1"))
    }

    @Test
    fun `should apply light mode styles`() {
        val content = """
            ---
            title: "Light Mode Test"
            ---

            # Testing Light Mode

            ```{r}
            x <- 1:5
            ```
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("rmarkdown-document light"))
        assertTrue(html.contains("background: white"))
        assertTrue(html.contains("color: black"))
    }

    @Test
    fun `should apply dark mode styles`() {
        val content = """
            ---
            title: "Dark Mode Test"
            ---

            # Testing Dark Mode

            ```{r}
            x <- 1:5
            ```
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = false)

        assertNotNull(html)
        assertTrue(html.contains("rmarkdown-document dark"))
        assertTrue(html.contains("background: #1e1e1e"))
        assertTrue(html.contains("color: #d4d4d4"))
    }

    @Test
    fun `should convert R Markdown without title to HTML`() {
        val content = """
            # Document without YAML

            This document has no YAML front matter.

            ```{r}
            y <- 10:20
            ```
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("rmarkdown-document"))
        assertTrue(html.contains("R Markdown Document")) // Default title
        assertTrue(html.contains("Document without YAML"))
        assertTrue(html.contains("R Code Chunks: 1"))
    }

    @Test
    fun `should convert R Markdown with multiple chunk types to HTML`() {
        val content = """
            ---
            title: "Multiple Languages"
            ---

            # Different Languages

            ```{r}
            x <- 1:10
            ```

            ```{python}
            y = [i*2 for i in range(5)]
            print(y)
            ```

            ```{bash}
            echo "Hello from bash"
            ```

            ```{sql}
            SELECT * FROM users WHERE age > 18;
            ```
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("Multiple Languages"))
        assertTrue(html.contains("R Code Chunks: 1"))
        assertTrue(html.contains("Total Chunks: 4"))
        assertTrue(html.contains("r-chunk"))
        assertTrue(html.contains("python-chunk"))
        assertTrue(html.contains("bash-chunk"))
        assertTrue(html.contains("sql-chunk"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate well-formed R Markdown content`() {
        val content = """
            ---
            title: "Valid Document"
            ---

            # Valid Content

            ```{r}
            x <- 1:10
            ```

            More content.
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should detect mismatched code chunk delimiters`() {
        val content = """
            # Invalid Document

            ```{r}
            x <- 1:10
            # Missing closing backticks

            More content.
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Mismatched code chunk delimiters"))
    }

    @Test
    fun `should detect unclosed YAML front matter`() {
        val content = """
            ---
            title: "Test"
            author: "John"
            # Missing closing ---

            # Content

            ```{r}
            x <- 1:10
            ```
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Unclosed YAML front matter"))
    }

    @Test
    fun `should validate complex R Markdown document`() {
        val content = """
            ---
            title: "Complex Analysis"
            author: "Research Team"
            date: "2024-01-15"
            output: html_document
            ---

            # Introduction

            This is a comprehensive analysis.

            ```{r setup, include=FALSE}
            knitr::opts_chunk${'$'}set(echo = TRUE)
            library(ggplot2)
            ```

            ## Data Loading

            ```{r data}
            # Load the data
            data <- read.csv("data.csv")
            head(data)
            ```

            ## Analysis

            ```{r analysis, fig.cap="Analysis Results"}
            # Perform analysis
            model <- lm(y ~ x, data=data)
            summary(model)
            ```

            ## Conclusion

            The analysis is complete.
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isEmpty())
    }

    // ==================== Round-trip Parsing Tests ====================

    @Test
    fun `should support round-trip parsing`() {
        val originalContent = """
            ---
            title: "Round-trip Test"
            author: "Test Author"
            ---

            # Testing Round-trip

            This content should be preserved.

            ```{r}
            x <- 1:100
            mean(x)
            ```

            More content after the chunk.
        """.trimIndent()

        // Parse the original content
        val firstParse = parser.parse(originalContent)

        // Get the formatted content (should be same as original)
        val formattedContent = firstParse.rawContent

        // Parse the formatted content again
        val secondParse = parser.parse(formattedContent)

        // Verify round-trip consistency
        assertEquals(firstParse.format.id, secondParse.format.id)
        assertEquals(firstParse.rawContent, secondParse.rawContent)
        assertEquals(firstParse.metadata, secondParse.metadata)
    }

    @Test
    fun `should preserve metadata through round-trip`() {
        val originalContent = """
            ---
            title: "Metadata Preservation"
            author: "John Doe"
            date: "2024-01-15"
            output:
              html_document:
                toc: true
            ---

            # Content

            ```{r}
            data <- rnorm(100)
            hist(data)
            ```
        """.trimIndent()

        val firstParse = parser.parse(originalContent)
        val secondParse = parser.parse(firstParse.rawContent)

        assertEquals(firstParse.metadata, secondParse.metadata)
        assertEquals(firstParse.format.id, secondParse.format.id)
        assertEquals("Metadata Preservation", firstParse.metadata["title"])
        assertEquals("Metadata Preservation", secondParse.metadata["title"])
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate with ParserRegistry`() {
        // Register the parser
        ParserRegistry.register(parser)

        // Get parser from registry
        val registryParser = ParserRegistry.getParser(TextFormat.ID_RMARKDOWN)

        assertNotNull(registryParser)
        assertEquals(TextFormat.ID_RMARKDOWN, registryParser.supportedFormat.id)

        // Test that it works
        val content = """
            ---
            title: "Registry Test"
            ---

            # Content

            ```{r}
            x <- 1:10
            ```
        """.trimIndent()

        val result = registryParser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_RMARKDOWN, result.format.id)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val rMarkdownFormat = allFormats.find { it.id == TextFormat.ID_RMARKDOWN }

        assertNotNull(rMarkdownFormat)
        assertEquals("R Markdown", rMarkdownFormat.name)
        assertEquals(".rmd", rMarkdownFormat.defaultExtension)
        assertTrue(rMarkdownFormat.extensions.contains(".rmd"))
        assertTrue(rMarkdownFormat.extensions.contains(".rmarkdown"))
    }
}
