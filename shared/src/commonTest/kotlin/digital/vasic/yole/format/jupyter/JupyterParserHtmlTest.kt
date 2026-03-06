/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Tests for JupyterParser HTML generation
 *
 *########################################################*/
package digital.vasic.yole.format.jupyter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JupyterParserHtmlTest {

    private val parser = JupyterParser()

    private val minimalNotebook = """
        {
            "cells": [],
            "metadata": {
                "kernelspec": {"name": "python3", "language": "python"},
                "language_info": {"name": "python"}
            },
            "nbformat": 4,
            "nbformat_minor": 5
        }
    """.trimIndent()

    private val notebookWithCode = """
        {
            "cells": [
                {
                    "cell_type": "code",
                    "source": ["print('hello')"],
                    "outputs": [],
                    "execution_count": 1
                }
            ],
            "metadata": {
                "kernelspec": {"name": "python3", "language": "python"},
                "language_info": {"name": "python"}
            },
            "nbformat": 4,
            "nbformat_minor": 5
        }
    """.trimIndent()

    private val notebookWithMarkdown = """
        {
            "cells": [
                {
                    "cell_type": "markdown",
                    "source": ["# Hello World"],
                    "outputs": []
                }
            ],
            "metadata": {
                "kernelspec": {"name": "python3"},
                "language_info": {"name": "python"}
            },
            "nbformat": 4,
            "nbformat_minor": 0
        }
    """.trimIndent()

    private val notebookWithOutput = """
        {
            "cells": [
                {
                    "cell_type": "code",
                    "source": ["1 + 1"],
                    "outputs": [
                        {
                            "output_type": "execute_result",
                            "data": {"text/plain": ["2"]},
                            "text": "2"
                        }
                    ],
                    "execution_count": 1
                }
            ],
            "metadata": {
                "kernelspec": {"name": "python3"},
                "language_info": {"name": "python"}
            },
            "nbformat": 4,
            "nbformat_minor": 0
        }
    """.trimIndent()

    // ==================== Document structure ====================

    @Test
    fun testWrappedInJupyterDiv() {
        val doc = parser.parse(minimalNotebook)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("jupyter-notebook"))
    }

    @Test
    fun testContainsStylesheet() {
        val doc = parser.parse(minimalNotebook)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("<style>"))
    }

    @Test
    fun testNotebookHeader() {
        val doc = parser.parse(minimalNotebook)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("notebook-header"))
    }

    // ==================== Metadata ====================

    @Test
    fun testMetadataCellCount() {
        val doc = parser.parse(notebookWithCode)
        assertEquals("1", doc.metadata["cells"])
    }

    @Test
    fun testMetadataKernel() {
        val doc = parser.parse(minimalNotebook)
        assertEquals("python3", doc.metadata["kernel"])
    }

    @Test
    fun testMetadataLanguage() {
        val doc = parser.parse(minimalNotebook)
        assertEquals("python", doc.metadata["language"])
    }

    @Test
    fun testMetadataFormatVersion() {
        val doc = parser.parse(minimalNotebook)
        assertEquals("4.5", doc.metadata["format_version"])
    }

    // ==================== Code cells ====================

    @Test
    fun testCodeCellRendered() {
        val doc = parser.parse(notebookWithCode)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("code-cell"))
    }

    @Test
    fun testCodeCellSource() {
        val doc = parser.parse(notebookWithCode)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("code-source"))
    }

    @Test
    fun testExecutionCount() {
        val doc = parser.parse(notebookWithCode)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("[1]"))
    }

    // ==================== Markdown cells ====================

    @Test
    fun testMarkdownCellRendered() {
        val doc = parser.parse(notebookWithMarkdown)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("markdown-cell"))
    }

    @Test
    fun testMarkdownCellContent() {
        val doc = parser.parse(notebookWithMarkdown)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("Hello World"))
    }

    // ==================== Cell outputs ====================

    @Test
    fun testCellOutputRendered() {
        val doc = parser.parse(notebookWithOutput)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("cell-output"))
    }

    // ==================== Light/dark mode ====================

    @Test
    fun testLightModeClass() {
        val doc = parser.parse(minimalNotebook)
        val html = parser.toHtml(doc, lightMode = true)
        assertTrue(html.contains("light"))
    }

    @Test
    fun testDarkModeClass() {
        val doc = parser.parse(minimalNotebook)
        val html = parser.toHtml(doc, lightMode = false)
        assertTrue(html.contains("dark"))
    }

    @Test
    fun testLightAndDarkModeDiffer() {
        val doc = parser.parse(minimalNotebook)
        val light = parser.toHtml(doc, lightMode = true)
        val dark = parser.toHtml(doc, lightMode = false)
        assertTrue(light != dark)
    }

    // ==================== Invalid JSON ====================

    @Test
    fun testInvalidJsonFallback() {
        val doc = parser.parse("not json at all")
        assertNotNull(doc)
    }

    @Test
    fun testValidationInvalidJson() {
        val errors = parser.validate("not json")
        assertTrue(errors.any { it.contains("Invalid JSON") })
    }

    @Test
    fun testValidationValidJson() {
        val errors = parser.validate(minimalNotebook)
        assertTrue(errors.isEmpty())
    }

    // ==================== Empty notebook ====================

    @Test
    fun testEmptyNotebook() {
        val doc = parser.parse(minimalNotebook)
        assertEquals("0", doc.metadata["cells"])
    }

    // ==================== canParse ====================

    @Test
    fun testCanParse() {
        assertTrue(parser.canParse(parser.supportedFormat))
    }

    // ==================== Data classes ====================

    @Test
    fun testNotebookCellCreation() {
        val cell = NotebookCell(
            cellType = "code",
            source = "x = 1",
            outputs = emptyList(),
            executionCount = 5
        )
        assertEquals("code", cell.cellType)
        assertEquals(5, cell.executionCount)
    }

    @Test
    fun testJupyterNotebookCreation() {
        val notebook = JupyterNotebook(
            cells = emptyList(),
            kernel = "python3",
            language = "python",
            formatVersion = "4.5",
            title = "Test"
        )
        assertEquals("Test", notebook.title)
        assertEquals("python3", notebook.kernel)
    }
}
