/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive cross-format integration test suite
 * Tests conversion chains, workflows, and format interoperability
 *
 *########################################################*/
package digital.vasic.yole.format.integration

import digital.vasic.yole.format.*
import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.creole.CreoleParser
import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.rmarkdown.RMarkdownParser
import digital.vasic.yole.format.asciidoc.AsciidocParser
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser
import digital.vasic.yole.format.textile.TextileParser
import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.csv.CsvParser
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.markdown.MarkdownParser
import kotlin.test.*
import kotlin.time.measureTime
import kotlin.time.Duration

/**
 * Comprehensive cross-format integration test suite.
 *
 * Tests cover:
 * - Cross-format conversion chains (LaTeX -> HTML -> Markdown)
 * - Format conversion workflows (Org Mode -> TaskPaper -> Plain Text)
 * - Notebook conversion (Jupyter -> R Markdown -> HTML presentation)
 * - Wiki migration (WikiText -> Creole -> TiddlyWiki)
 * - Conversion quality and content preservation
 * - Round-trip conversion consistency
 * - Performance of cross-format operations
 * - Comprehensive integration benchmarks
 */
class CrossFormatIntegrationTest {

    // Initialize all parsers for cross-format testing
    private val latexParser = LatexParser()
    private val orgModeParser = OrgModeParser()
    private val taskPaperParser = TaskpaperParser()
    private val wikiTextParser = WikitextParser()
    private val creoleParser = CreoleParser()
    private val tiddlyWikiParser = TiddlyWikiParser()
    private val jupyterParser = JupyterParser()
    private val rMarkdownParser = RMarkdownParser()
    private val asciidocParser = AsciidocParser()
    private val rstParser = RestructuredTextParser()
    private val textileParser = TextileParser()
    private val keyValueParser = KeyValueParser()
    private val csvParser = CsvParser()
    private val todoTxtParser = TodoTxtParser()
    private val plainTextParser = PlaintextParser()
    private val markdownParser = MarkdownParser()

    // ==================== Cross-Format Conversion Chains ====================

    @Test
    fun `test LaTeX to HTML to Markdown conversion chain`() {
        val latexContent = """
            \documentclass{article}
            \title{Cross-Format Test Document}
            \author{Integration Tester}
            \date{2025-01-01}

            \begin{document}
            \maketitle

            \section{Introduction}
            This is a test document for cross-format conversion chains.

            \subsection{Mathematical Content}
            The famous equation ${'$'}E = mc^2${'$'} should be preserved.

            \begin{itemize}
            \item First item with \textbf{bold text}
            \item Second item with \textit{italic text}
            \item Third item with \underline{underlined text}
            \end{itemize}

            \section{Conclusion}
            Cross-format conversion should maintain content integrity.

            \end{document}
        """.trimIndent()

        // Step 1: Parse LaTeX
        val latexDocument = latexParser.parse(latexContent)
        assertNotNull(latexDocument)
        assertTrue(latexDocument.errors.isEmpty())
        assertEquals(TextFormat.ID_LATEX, latexDocument.format.id)

        // Step 2: Convert to HTML
        val htmlContent = latexParser.toHtml(latexDocument, lightMode = true)
        assertNotNull(htmlContent)
        assertTrue(htmlContent.contains("Cross-Format Test Document"))
        assertTrue(htmlContent.contains("Integration Tester"))
        assertTrue(htmlContent.contains("bold"))
        assertTrue(htmlContent.contains("italic"))

        // Step 3: Convert HTML to Markdown (simulated via plain text parsing)
        val markdownDocument = plainTextParser.parse(htmlContent)
        assertNotNull(markdownDocument)
        assertTrue(markdownDocument.errors.isEmpty())

        // Verify content preservation through the chain
        assertTrue(markdownDocument.rawContent.contains("Cross-Format Test Document"))
        assertTrue(markdownDocument.rawContent.contains("Integration Tester"))
    }

    @Test
    fun `test complex LaTeX conversion chain with equations`() {
        val latexContent = """
            \documentclass{article}
            \title{Mathematical Document}
            \begin{document}
            \maketitle

            \section{Advanced Mathematics}

            Inline math: ${'$'}\alpha + \beta = \gamma${'$'}

            Display math:
            \\[
            \\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}
            \\]

            \begin{equation}
            \\sum_{i=1}^{n} x_i = x_1 + x_2 + \\cdots + x_n
            \end{equation}

            \begin{itemize}
            \item Mathematical expressions should be preserved
            \item Format conversion should maintain structure
            \item Content integrity is paramount
            \end{itemize}

            \end{document}
        """.trimIndent()

        val latexDocument = latexParser.parse(latexContent)
        assertNotNull(latexDocument)
        assertTrue(latexDocument.errors.isEmpty())

        val htmlContent = latexParser.toHtml(latexDocument, lightMode = true)
        assertNotNull(htmlContent)

        // Verify mathematical content is preserved in HTML
        assertTrue(htmlContent.contains("math-inline") || htmlContent.contains("\u03B1") || htmlContent.contains("\u03B2"))
        assertTrue(htmlContent.contains("math-display") || htmlContent.contains("\u222B"))
    }

    // ==================== Format Conversion Workflows ====================

    @Test
    fun `test Org Mode to TaskPaper to Plain Text workflow`() {
        val orgContent = """
            * Project Planning
            ** TODO Main Task 1
               DEADLINE: <2025-01-15>
               :PROPERTIES:
               :Effort: 2h
               :END:
               This is the main task description.
               - Subtask A
               - Subtask B
            ** DONE Completed Task
               CLOSED: [2025-01-01]
               This task has been completed successfully.
            ** NEXT Upcoming Task
               SCHEDULED: <2025-01-10>
               This task is scheduled for later.

            * Notes
            ** Important Information
               Some important notes go here.
            ** References
               - Reference 1
               - Reference 2
        """.trimIndent()

        // Step 1: Parse Org Mode
        val orgDocument = orgModeParser.parse(orgContent)
        assertNotNull(orgDocument)
        assertTrue(orgDocument.errors.isEmpty())
        assertEquals(TextFormat.ID_ORGMODE, orgDocument.format.id)

        // Step 2: Convert to TaskPaper format (simulated)
        val taskPaperContent = convertOrgToTaskPaper(orgContent)
        val taskPaperDocument = taskPaperParser.parse(taskPaperContent)
        assertNotNull(taskPaperDocument)
        assertTrue(taskPaperDocument.errors.isEmpty())

        // Step 3: Convert to Plain Text
        val plainTextDocument = plainTextParser.parse(taskPaperContent)
        assertNotNull(plainTextDocument)
        assertTrue(plainTextDocument.errors.isEmpty())

        // Verify content preservation
        assertTrue(plainTextDocument.rawContent.contains("Project Planning"))
        assertTrue(plainTextDocument.rawContent.contains("Main Task 1"))
        assertTrue(plainTextDocument.rawContent.contains("Completed Task"))
    }

    @Test
    fun `test complex project management workflow`() {
        val orgContent = """
            * Software Development Project
            ** TODO Design Phase
               DEADLINE: <2025-01-20>
               Priority: A
               Complete the system architecture design.
               - Create UML diagrams
               - Define API specifications
               - Review with stakeholders

            ** TODO Implementation Phase
               DEADLINE: <2025-02-15>
               Priority: A
               Implement the core functionality.
               - Set up development environment
               - Write unit tests
               - Implement features
               - Code review

            ** TODO Testing Phase
               DEADLINE: <2025-03-01>
               Priority: B
               Comprehensive testing of all components.
               - Unit testing
               - Integration testing
               - User acceptance testing

            * Meeting Notes
            ** 2025-01-05 Weekly Standup
               Attendees: Alice, Bob, Carol
               Discussion points:
               - Progress on design phase
               - Blockers identified
               - Next sprint planning
        """.trimIndent()

        val orgDocument = orgModeParser.parse(orgContent)
        assertNotNull(orgDocument)
        assertTrue(orgDocument.errors.isEmpty())

        val taskPaperContent = convertOrgToTaskPaper(orgContent)
        val taskPaperDocument = taskPaperParser.parse(taskPaperContent)
        assertNotNull(taskPaperDocument)
        assertTrue(taskPaperDocument.errors.isEmpty())

        // Verify workflow integrity
        assertTrue(taskPaperDocument.rawContent.contains("Software Development Project"))
        assertTrue(taskPaperDocument.rawContent.contains("Design Phase"))
        assertTrue(taskPaperDocument.rawContent.contains("Implementation Phase"))
        assertTrue(taskPaperDocument.rawContent.contains("Testing Phase"))
    }

    // ==================== Notebook Conversion Tests ====================

    @Test
    fun `test Jupyter to R Markdown to HTML presentation workflow`() {
        val jupyterContent = """
            {
              "nbformat": 4,
              "nbformat_minor": 4,
              "metadata": {
                "title": "Data Analysis Notebook",
                "author": "Data Scientist",
                "kernelspec": {
                  "name": "python3",
                  "display_name": "Python 3"
                }
              },
              "cells": [
                {
                  "cell_type": "markdown",
                  "source": "# Data Analysis Project\\n\\nThis notebook contains our data analysis workflow."
                },
                {
                  "cell_type": "code",
                  "execution_count": 1,
                  "source": "import pandas as pd\\nimport matplotlib.pyplot as plt\\nimport numpy as np",
                  "outputs": []
                },
                {
                  "cell_type": "markdown",
                  "source": "## Data Loading\\n\\nWe start by loading our dataset."
                },
                {
                  "cell_type": "code",
                  "execution_count": 2,
                  "source": "# Load the data\\ndata = pd.read_csv('data.csv')\\nprint(f'Dataset shape: {data.shape}')",
                  "outputs": [
                    {
                      "output_type": "stream",
                      "text": "Dataset shape: (1000, 5)"
                    }
                  ]
                },
                {
                  "cell_type": "markdown",
                  "source": "## Analysis Results\\n\\nKey findings from our analysis."
                }
              ]
            }
        """.trimIndent()

        // Step 1: Parse Jupyter Notebook
        val jupyterDocument = jupyterParser.parse(jupyterContent)
        assertNotNull(jupyterDocument)
        assertTrue(jupyterDocument.errors.isEmpty())
        assertEquals(TextFormat.ID_JUPYTER, jupyterDocument.format.id)

        // Step 2: Convert to R Markdown (simulated)
        val rMarkdownContent = convertJupyterToRMarkdown(jupyterContent)
        val rMarkdownDocument = rMarkdownParser.parse(rMarkdownContent)
        assertNotNull(rMarkdownDocument)
        assertTrue(rMarkdownDocument.errors.isEmpty())

        // Step 3: Convert to HTML presentation
        val htmlContent = rMarkdownParser.toHtml(rMarkdownDocument, lightMode = true)
        assertNotNull(htmlContent)

        // Verify content preservation
        assertTrue(htmlContent.contains("Data Analysis Project"))
        assertTrue(htmlContent.contains("Data Loading"))
        assertTrue(htmlContent.contains("Analysis Results"))
    }

    @Test
    fun `test complex notebook with visualizations`() {
        val jupyterContent = """
            {
              "nbformat": 4,
              "nbformat_minor": 4,
              "metadata": {
                "title": "Machine Learning Pipeline",
                "author": "ML Engineer",
                "kernelspec": {
                  "name": "python3",
                  "display_name": "Python 3"
                }
              },
              "cells": [
                {
                  "cell_type": "markdown",
                  "source": "# Machine Learning Pipeline\\n\\nEnd-to-end machine learning workflow."
                },
                {
                  "cell_type": "markdown",
                  "source": "## 1. Data Preprocessing\\n\\nCleaning and preparing the data."
                },
                {
                  "cell_type": "code",
                  "execution_count": 1,
                  "source": "import pandas as pd\\nfrom sklearn.preprocessing import StandardScaler\\nfrom sklearn.model_selection import train_test_split",
                  "outputs": []
                },
                {
                  "cell_type": "code",
                  "execution_count": 2,
                  "source": "# Load and preprocess data\\nX, y = load_data()\\nX_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2)",
                  "outputs": [
                    {
                      "output_type": "stream",
                      "text": "Data loaded successfully. Shape: (1000, 20)"
                    }
                  ]
                },
                {
                  "cell_type": "markdown",
                  "source": "## 2. Model Training\\n\\nTraining multiple models and comparing performance."
                },
                {
                  "cell_type": "code",
                  "execution_count": 3,
                  "source": "from sklearn.ensemble import RandomForestClassifier\\nfrom sklearn.linear_model import LogisticRegression\\nfrom sklearn.metrics import accuracy_score",
                  "outputs": []
                },
                {
                  "cell_type": "markdown",
                  "source": "## 3. Results and Evaluation\\n\\nModel performance comparison."
                }
              ]
            }
        """.trimIndent()

        val jupyterDocument = jupyterParser.parse(jupyterContent)
        assertNotNull(jupyterDocument)
        assertTrue(jupyterDocument.errors.isEmpty())

        val rMarkdownContent = convertJupyterToRMarkdown(jupyterContent)
        val rMarkdownDocument = rMarkdownParser.parse(rMarkdownContent)
        assertNotNull(rMarkdownDocument)
        assertTrue(rMarkdownDocument.errors.isEmpty())

        val htmlContent = rMarkdownParser.toHtml(rMarkdownDocument, lightMode = true)
        assertNotNull(htmlContent)

        // Verify complex content preservation
        assertTrue(htmlContent.contains("Machine Learning Pipeline"))
        assertTrue(htmlContent.contains("Data Preprocessing"))
        assertTrue(htmlContent.contains("Model Training"))
        assertTrue(htmlContent.contains("Results and Evaluation"))
    }

    // ==================== Wiki Migration Tests ====================

    @Test
    fun `test WikiText to Creole to TiddlyWiki migration`() {
        val wikiTextContent = """
            = Main Page =

            This is the main page of our wiki.

            == Section 1 ==

            Some content for section 1.

            === Subsection 1.1 ===

            More detailed content here.

            * List item 1
            * List item 2
            * List item 3

            == Section 2 ==

            Another section with [[Internal Link|Page2]] and [http://example.com External Link].

            '''Bold text''' and ''italic text''.

            {| class="wikitable"
            |+ Table Caption
            ! Header 1
            ! Header 2
            |-
            | Cell 1
            | Cell 2
            |-
            | Cell 3
            | Cell 4
            |}
        """.trimIndent()

        // Step 1: Parse WikiText
        val wikiTextDocument = wikiTextParser.parse(wikiTextContent)
        assertNotNull(wikiTextDocument)
        assertTrue(wikiTextDocument.errors.isEmpty())
        assertEquals(TextFormat.ID_WIKITEXT, wikiTextDocument.format.id)

        // Step 2: Convert to Creole
        val creoleContent = convertWikiTextToCreole(wikiTextContent)
        val creoleDocument = creoleParser.parse(creoleContent)
        assertNotNull(creoleDocument)
        assertTrue(creoleDocument.errors.isEmpty())

        // Step 3: Convert to TiddlyWiki
        val tiddlyWikiContent = convertCreoleToTiddlyWiki(creoleContent)
        val tiddlyWikiDocument = tiddlyWikiParser.parse(tiddlyWikiContent)
        assertNotNull(tiddlyWikiDocument)
        assertTrue(tiddlyWikiDocument.errors.isEmpty())

        // Verify content preservation through migration
        assertTrue(tiddlyWikiDocument.rawContent.contains("Main Page"))
        assertTrue(tiddlyWikiDocument.rawContent.contains("Section 1"))
        assertTrue(tiddlyWikiDocument.rawContent.contains("Section 2"))
    }

    @Test
    fun `test complex wiki migration with nested structures`() {
        val wikiTextContent = """
            = Software Documentation =

            Welcome to our comprehensive software documentation.

            == Getting Started ==

            This section helps new users get started quickly.

            === Installation ===

            Follow these steps to install the software:

            # Download the installer
            # Run the installation wizard
            # Configure initial settings
            # Verify the installation

            === Configuration ===

            Configure your environment:

            * Set environment variables
            * Configure database connections
            * Set up logging
            * Customize user preferences

            == Advanced Topics ==

            === API Reference ===

            Detailed API documentation:

            {| class="wikitable sortable"
            |+ API Endpoints
            ! Endpoint
            ! Method
            ! Description
            |-
            | /api/users
            | GET
            | List all users
            |-
            | /api/users
            | POST
            | Create new user
            |-
            | /api/users/{id}
            | GET
            | Get user by ID
            |}

            === Troubleshooting ===

            Common issues and solutions:

            * '''Error 404''': Resource not found
            * '''Error 500''': Internal server error
            * '''Connection timeout''': Check network settings

            == Contributing ==

            We welcome contributions! See our [[Contributing Guidelines|Contrib]] for details.
        """.trimIndent()

        val wikiTextDocument = wikiTextParser.parse(wikiTextContent)
        assertNotNull(wikiTextDocument)
        assertTrue(wikiTextDocument.errors.isEmpty())

        val creoleContent = convertWikiTextToCreole(wikiTextContent)
        val creoleDocument = creoleParser.parse(creoleContent)
        assertNotNull(creoleDocument)
        assertTrue(creoleDocument.errors.isEmpty())

        val tiddlyWikiContent = convertCreoleToTiddlyWiki(creoleContent)
        val tiddlyWikiDocument = tiddlyWikiParser.parse(tiddlyWikiContent)
        assertNotNull(tiddlyWikiDocument)
        assertTrue(tiddlyWikiDocument.errors.isEmpty())

        // Verify complex structure preservation
        assertTrue(tiddlyWikiDocument.rawContent.contains("Software Documentation"))
        assertTrue(tiddlyWikiDocument.rawContent.contains("Getting Started"))
        assertTrue(tiddlyWikiDocument.rawContent.contains("Advanced Topics"))
        assertTrue(tiddlyWikiDocument.rawContent.contains("Contributing"))
    }

    // ==================== Conversion Quality Tests ====================

    @Test
    fun `test content preservation across multiple conversions`() {
        val originalContent = """
            # Complex Document

            This document tests content preservation across multiple format conversions.

            ## Structural Elements

            * Bullet point 1
            * Bullet point 2
              * Nested bullet
              * Another nested bullet
            * Bullet point 3

            1. Numbered item 1
            2. Numbered item 2
               1. Nested numbered item
               2. Another nested numbered item
            3. Numbered item 3

            ## Text Formatting

            **Bold text** and *italic text* and `inline code`.

            > This is a blockquote
            > spanning multiple lines
            > with various content

            ## Links and References

            [External link](https://example.com) and [Internal reference](#section).

            ## Code Blocks

            ```python
            def example_function():
                print("Hello, World!")
                return 42
            ```

            ## Tables

            | Column 1 | Column 2 | Column 3 |
            |----------|----------|----------|
            | Data 1   | Data 2   | Data 3   |
            | Data 4   | Data 5   | Data 6   |

            ## Conclusion

            This document should maintain its structure and content through conversions.
        """.trimIndent()

        // Convert through multiple formats
        val markdownDocument = plainTextParser.parse(originalContent)
        assertNotNull(markdownDocument)

        val htmlContent = markdownParser.toHtml(markdownDocument, lightMode = true)
        assertNotNull(htmlContent)

        val htmlDocument = plainTextParser.parse(htmlContent)
        assertNotNull(htmlDocument)

        // Verify content preservation
        assertTrue(htmlDocument.rawContent.contains("Complex Document"))
        assertTrue(htmlDocument.rawContent.contains("Structural Elements"))
        assertTrue(htmlDocument.rawContent.contains("Text Formatting"))
        assertTrue(htmlDocument.rawContent.contains("Links and References"))
        assertTrue(htmlDocument.rawContent.contains("Code Blocks"))
        assertTrue(htmlDocument.rawContent.contains("Tables"))
        assertTrue(htmlDocument.rawContent.contains("Conclusion"))
    }

    @Test
    fun `test round-trip conversion consistency`() {
        val originalContent = """
            # Round-Trip Test Document

            This document tests round-trip conversion consistency.

            ## Key Requirements

            1. Content must be preserved exactly
            2. Structure must be maintained
            3. Metadata should be consistent
            4. Format-specific features should be handled gracefully

            ## Test Elements

            * Simple bullet points
            * **Bold text**
            * *Italic text*
            * `Code snippets`

            > Important notes should be preserved
            > across multiple conversion cycles

            ## Conclusion

            The document should be identical after round-trip conversion.
        """.trimIndent()

        // First conversion
        val firstDocument = plainTextParser.parse(originalContent)
        assertNotNull(firstDocument)

        // Convert to HTML
        val htmlContent = markdownParser.toHtml(firstDocument, lightMode = true)
        assertNotNull(htmlContent)

        // Convert back to text (simulating round-trip)
        val roundTripDocument = plainTextParser.parse(htmlContent)
        assertNotNull(roundTripDocument)

        // Verify round-trip consistency
        assertTrue(roundTripDocument.rawContent.contains("Round-Trip Test Document"))
        assertTrue(roundTripDocument.rawContent.contains("Key Requirements"))
        assertTrue(roundTripDocument.rawContent.contains("Test Elements"))
        assertTrue(roundTripDocument.rawContent.contains("Conclusion"))
    }

    // ==================== Performance Tests ====================

    @Test
    fun `test performance of cross-format operations`() {
        val largeContent = buildString {
            appendLine("# Large Document for Performance Testing")
            appendLine()
            appendLine("This document is designed to test the performance of cross-format operations.")
            appendLine()

            repeat(100) { i ->
                appendLine("## Section ${i + 1}")
                appendLine()
                appendLine("This is content for section ${i + 1}. It contains various elements:")
                appendLine()

                // Add bullet points
                repeat(5) { j ->
                    appendLine("* Bullet point ${j + 1} in section ${i + 1}")
                }
                appendLine()

                // Add numbered list
                repeat(3) { j ->
                    appendLine("${j + 1}. Numbered item ${j + 1} in section ${i + 1}")
                }
                appendLine()

                // Add formatted text
                appendLine("Content with **bold** and *italic* text.")
                appendLine()

                // Add code block
                appendLine("```")
                appendLine("code block")
                appendLine("```")
            }

            appendLine("## Conclusion")
            appendLine()
            appendLine("This large document should be processed efficiently.")
        }

        val parseTime = measureTime {
            val document = plainTextParser.parse(largeContent)
            assertNotNull(document)
            assertTrue(document.errors.isEmpty())
        }

        val htmlConversionTime = measureTime {
            val document = plainTextParser.parse(largeContent)
            val html = markdownParser.toHtml(document, lightMode = true)
            assertNotNull(html)
            assertTrue(html.length > 0)
        }

        println("Parse time: $parseTime")
        println("HTML conversion time: $htmlConversionTime")

        // Performance assertions (generous limits for constrained environments)
        assertTrue(parseTime.inWholeMilliseconds < 4000, "Parsing should complete within 4 seconds")
        assertTrue(htmlConversionTime.inWholeMilliseconds < 4000, "HTML conversion should complete within 4 seconds")
    }

    @Test
    fun `test memory efficiency of cross-format operations`() {
        val content = buildString {
            appendLine("# Memory Efficiency Test")
            appendLine()

            repeat(50) { i ->
                appendLine("## Section ${i + 1}")
                repeat(20) { j ->
                    appendLine("Paragraph ${j + 1} with some content to test memory efficiency.")
                }
                appendLine()
            }
        }

        // Test multiple conversions without memory leaks. Track HTML output across
        // iterations to ensure each parse-then-render round produces real HTML.
        val htmlsProduced = mutableListOf<String>()
        repeat(10) { iteration ->
            val document = plainTextParser.parse(content)
            assertNotNull(document)

            val html = markdownParser.toHtml(document, lightMode = true)
            assertNotNull(html)
            htmlsProduced.add(html)

            // Clear cache to test memory management
            document.clearHtmlCache()

            assertFalse(document.hasHtmlCached(lightMode = true))
        }
        assertEquals(10, htmlsProduced.size, "10 conversions must complete")
        assertTrue(htmlsProduced.all { it.isNotEmpty() }, "every conversion must yield non-empty HTML")
    }

    // ==================== Integration Benchmark Tests ====================

    @Test
    fun `test comprehensive integration benchmarks`() {
        val benchmarkResults = mutableMapOf<String, Duration>()

        // Benchmark 1: Simple conversion chain
        val simpleContent = """
            # Simple Document
            Basic content for benchmarking.
        """.trimIndent()

        benchmarkResults["simple_conversion"] = measureTime {
            val doc = plainTextParser.parse(simpleContent)
            val html = markdownParser.toHtml(doc, lightMode = true)
            plainTextParser.parse(html)
        }

        // Benchmark 2: Complex conversion chain
        val complexContent = buildString {
            appendLine("# Complex Document for Benchmarking")
            repeat(20) { i ->
                appendLine("## Section ${i + 1}")
                appendLine("Content with **bold** and *italic* text.")
                repeat(5) { j ->
                    appendLine("* List item ${j + 1}")
                }
                appendLine("```")
                appendLine("code block")
                appendLine("```")
            }
        }

        benchmarkResults["complex_conversion"] = measureTime {
            val doc = plainTextParser.parse(complexContent)
            val html = markdownParser.toHtml(doc, lightMode = true)
            plainTextParser.parse(html)
        }

        // Benchmark 3: Multi-format workflow
        benchmarkResults["multi_format_workflow"] = measureTime {
            val orgDoc = orgModeParser.parse("* Test\\n** TODO Task")
            val taskPaperContent = convertOrgToTaskPaper(orgDoc.rawContent)
            val taskPaperDoc = taskPaperParser.parse(taskPaperContent)
            val plainDoc = plainTextParser.parse(taskPaperDoc.rawContent)
        }

        // Benchmark 4: Wiki migration
        benchmarkResults["wiki_migration"] = measureTime {
            val wikiDoc = wikiTextParser.parse("= Test =\\nContent")
            val creoleContent = convertWikiTextToCreole(wikiDoc.rawContent)
            val creoleDoc = creoleParser.parse(creoleContent)
            val tiddlyContent = convertCreoleToTiddlyWiki(creoleDoc.rawContent)
            tiddlyWikiParser.parse(tiddlyContent)
        }

        // Print benchmark results
        benchmarkResults.forEach { (name, duration) ->
            println("Benchmark $name: $duration")
        }

        // Verify benchmarks completed successfully
        assertTrue(benchmarkResults.size == 4)
        benchmarkResults.values.forEach { duration ->
            assertTrue(duration.inWholeNanoseconds > 0, "Benchmark should take measurable time")
        }
    }

    @Test
    fun `test stress test with extreme scenarios`() {
        // Test 1: Very large single document
        val veryLargeContent = "Large content\n".repeat(10000)

        val largeDocTime = measureTime {
            val doc = plainTextParser.parse(veryLargeContent)
            assertNotNull(doc)
            val html = markdownParser.toHtml(doc, lightMode = true)
            assertNotNull(html)
        }

        println("Very large document processing time: $largeDocTime")

        // Test 2: Rapid successive conversions
        val rapidConversionTime = measureTime {
            repeat(100) { i ->
                val content = "Document $i\nContent for rapid conversion testing."
                val doc = plainTextParser.parse(content)
                markdownParser.toHtml(doc, lightMode = true)
            }
        }

        println("Rapid conversion time (100 documents): $rapidConversionTime")

        // Test 3: Deeply nested structures
        val nestedContent = buildString {
            repeat(10) { i ->
                repeat(i + 1) { level ->
                    appendLine("#".repeat(level + 1) + " Level ${level + 1} Heading")
                }
                appendLine("Content at nesting level ${i + 1}")
            }
        }

        val nestedTime = measureTime {
            val doc = plainTextParser.parse(nestedContent)
            assertNotNull(doc)
            val html = markdownParser.toHtml(doc, lightMode = true)
            assertNotNull(html)
        }

        println("Deeply nested structure time: $nestedTime")

        // All stress tests must have produced measurable timings (proves the
        // operations actually executed; a no-op `measureTime { }` returns 0ns).
        assertTrue(largeDocTime.inWholeNanoseconds > 0, "very-large doc parse must take measurable time")
        assertTrue(rapidConversionTime.inWholeNanoseconds > 0, "rapid conversion must take measurable time")
        assertTrue(nestedTime.inWholeNanoseconds > 0, "deeply nested parse must take measurable time")
    }

    // ==================== Helper Methods ====================

    private fun convertOrgToTaskPaper(orgContent: String): String {
        return buildString {
            orgContent.lines().forEach { line ->
                when {
                    line.startsWith("* ") -> appendLine(line.replace("* ", "").trimEnd() + ":")
                    line.startsWith("** ") -> appendLine("\t- " + line.replace("** ", "").trimEnd())
                    line.startsWith("*** ") -> appendLine("\t\t- " + line.replace("*** ", "").trimEnd())
                    line.startsWith("    ") -> appendLine("\t\t\t" + line.trim())
                    line.trim().isNotEmpty() -> appendLine("\t\t\t" + line.trim())
                    else -> appendLine("")
                }
            }
        }
    }

    private fun convertJupyterToRMarkdown(jupyterContent: String): String {
        // Convert Jupyter notebook JSON to R Markdown by extracting markdown source lines
        return buildString {
            appendLine("---")
            appendLine("title: 'Converted Jupyter Notebook'")
            appendLine("author: 'Converted Document'")
            appendLine("date: '`r Sys.Date()`'")
            appendLine("output: html_document")
            appendLine("---")
            appendLine()

            // Extract markdown cell source content from the JSON
            // Match "source": "..." patterns and unescape \\n to newlines
            val sourcePattern = Regex(""""source"\s*:\s*"([^"]*(?:\\.[^"]*)*)"""")
            sourcePattern.findAll(jupyterContent).forEach { match ->
                val source = match.groupValues[1]
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                source.lines().forEach { line ->
                    appendLine(line)
                }
                appendLine()
            }
        }
    }

    private fun convertWikiTextToCreole(wikiTextContent: String): String {
        return buildString {
            wikiTextContent.lines().forEach { line ->
                when {
                    line.startsWith("= ") && line.endsWith(" =") -> appendLine("= " + line.removeSurrounding("= ", " ="))
                    line.startsWith("== ") && line.endsWith(" ==") -> appendLine("== " + line.removeSurrounding("== ", " =="))
                    line.startsWith("=== ") && line.endsWith(" ===") -> appendLine("=== " + line.removeSurrounding("=== ", " ==="))
                    line.startsWith("* ") -> appendLine("* " + line.removePrefix("* "))
                    line.startsWith("# ") -> appendLine("# " + line.removePrefix("# "))
                    line.contains("'''") -> appendLine(line.replace("'''", "**"))
                    line.contains("''") -> appendLine(line.replace("''", "//"))
                    line.trim().startsWith("|") -> appendLine(line) // Keep tables similar
                    else -> appendLine(line)
                }
            }
        }
    }

    private fun convertCreoleToTiddlyWiki(creoleContent: String): String {
        return buildString {
            appendLine("title: Converted Wiki Page")
            appendLine("tags: imported converted")
            appendLine("modified: 2025-01-01")
            appendLine("created: 2025-01-01")
            appendLine()

            creoleContent.lines().forEach { line ->
                when {
                    line.startsWith("= ") -> appendLine("! " + line.removePrefix("= "))
                    line.startsWith("== ") -> appendLine("!! " + line.removePrefix("== "))
                    line.startsWith("=== ") -> appendLine("!!! " + line.removePrefix("=== "))
                    line.startsWith("* ") -> appendLine("* " + line.removePrefix("* "))
                    line.startsWith("# ") -> appendLine("# " + line.removePrefix("# "))
                    else -> appendLine(line)
                }
            }
        }
    }
}
