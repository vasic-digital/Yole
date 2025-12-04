/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive tests for Jupyter parser JSON parsing logic
 *
 *########################################################*/
package digital.vasic.yole.format.jupyter

import digital.vasic.yole.format.ParserRegistry
import kotlin.test.*

/**
 * Comprehensive tests for Jupyter parser covering all JSON parsing branches.
 *
 * Tests cover:
 * - Valid Jupyter notebooks with different cell types
 * - Source as array vs string
 * - Outputs parsing
 * - Missing optional fields
 * - Invalid JSON handling
 * - Metadata extraction
 * - HTML generation
 */
class JupyterParserComprehensiveTest {

    private lateinit var parser: JupyterParser

    @BeforeTest
    fun setup() {
        parser = JupyterParser()
        ParserRegistry.clear()
        ParserRegistry.register(parser)
    }

    @AfterTest
    fun teardown() {
        ParserRegistry.clear()
    }

    // ==================== Valid Notebook Tests ====================

    @Test
    fun `should parse notebook with code cell and array source`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "code",
                  "source": ["print('Hello')", "\n", "print('World')"],
                  "execution_count": 1,
                  "outputs": []
                }
              ],
              "metadata": {
                "kernelspec": {"name": "python3"},
                "language_info": {"name": "python"}
              },
              "nbformat": 4,
              "nbformat_minor": 5
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
        assertEquals("python3", doc.metadata["kernel"])
        assertEquals("python", doc.metadata["language"])
        assertEquals("4.5", doc.metadata["format_version"])
        assertTrue(doc.parsedContent.contains("Code [1]"))
        assertTrue(doc.parsedContent.contains("Hello"))
        assertTrue(doc.parsedContent.contains("World"))
    }

    @Test
    fun `should parse notebook with markdown cell`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "markdown",
                  "source": "# Title\n\nParagraph text"
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
        assertTrue(doc.parsedContent.contains("Markdown"))
        assertTrue(doc.parsedContent.contains("Title"))
        assertTrue(doc.parsedContent.contains("Paragraph"))
    }

    @Test
    fun `should parse notebook with raw cell`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "raw",
                  "source": "Raw content here"
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
        assertTrue(doc.parsedContent.contains("Raw"))
        assertTrue(doc.parsedContent.contains("Raw content"))
    }

    @Test
    fun `should handle source as string instead of array`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "code",
                  "source": "print('single string source')"
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
        assertTrue(doc.parsedContent.contains("single string source"))
    }

    @Test
    fun `should handle source as neither array nor string`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "code",
                  "source": null
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
        // Should handle gracefully with empty source
    }

    @Test
    fun `should parse cell with outputs containing text array`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "code",
                  "source": "print('test')",
                  "outputs": [
                    {
                      "output_type": "stream",
                      "name": "stdout",
                      "text": ["test", "\n", "output"]
                    }
                  ]
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
        assertTrue(doc.parsedContent.contains("test") || doc.parsedContent.contains("output"))
    }

    @Test
    fun `should parse cell with outputs containing text string`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "code",
                  "source": "print('hello')",
                  "outputs": [
                    {
                      "output_type": "stream",
                      "text": "hello\n"
                    }
                  ]
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
        assertTrue(doc.parsedContent.contains("hello"))
    }

    @Test
    fun `should parse cell without execution count`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "code",
                  "source": "code",
                  "outputs": []
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
        assertTrue(doc.parsedContent.contains("Code"))
        assertFalse(doc.parsedContent.contains("[null]"))
    }

    // ==================== Missing Fields Tests ====================

    @Test
    fun `should handle notebook without cells array`() {
        val notebook = """
            {
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("0", doc.metadata["cells"])
    }

    @Test
    fun `should handle notebook without metadata`() {
        val notebook = """
            {
              "cells": [],
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("0", doc.metadata["cells"])
        assertEquals("python3", doc.metadata["kernel"])  // Default
        assertEquals("python", doc.metadata["language"])  // Default
    }

    @Test
    fun `should handle notebook without nbformat`() {
        val notebook = """
            {
              "cells": [],
              "metadata": {}
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("4.0", doc.metadata["format_version"])  // Default 4.0
    }

    @Test
    fun `should handle notebook without kernelspec`() {
        val notebook = """
            {
              "cells": [],
              "metadata": {
                "language_info": {"name": "julia"}
              },
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("python3", doc.metadata["kernel"])  // Default
        assertEquals("julia", doc.metadata["language"])
    }

    @Test
    fun `should handle notebook without language_info`() {
        val notebook = """
            {
              "cells": [],
              "metadata": {
                "kernelspec": {"name": "ir"}
              },
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("ir", doc.metadata["kernel"])
        assertEquals("python", doc.metadata["language"])  // Default
    }

    @Test
    fun `should extract title from metadata if present`() {
        val notebook = """
            {
              "cells": [],
              "metadata": {
                "title": "My Notebook Title"
              },
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("My Notebook Title", doc.metadata["title"])
    }

    @Test
    fun `should not have title metadata if not present`() {
        val notebook = """
            {
              "cells": [],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertFalse(doc.metadata.containsKey("title"))
    }

    // ==================== Multiple Cells Tests ====================

    @Test
    fun `should parse notebook with multiple mixed cells`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "markdown",
                  "source": "# Introduction"
                },
                {
                  "cell_type": "code",
                  "source": "x = 42",
                  "execution_count": 1,
                  "outputs": []
                },
                {
                  "cell_type": "code",
                  "source": "print(x)",
                  "execution_count": 2,
                  "outputs": [
                    {
                      "output_type": "stream",
                      "text": "42\n"
                    }
                  ]
                },
                {
                  "cell_type": "raw",
                  "source": "Raw data"
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("4", doc.metadata["cells"])
        assertTrue(doc.parsedContent.contains("Markdown"))
        assertTrue(doc.parsedContent.contains("Code [1]"))
        assertTrue(doc.parsedContent.contains("Code [2]"))
        assertTrue(doc.parsedContent.contains("Raw"))
    }

    // ==================== HTML Generation Tests ====================

    @Test
    fun `should generate light mode HTML`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "code",
                  "source": "test"
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("jupyter-notebook"))
        assertTrue(html.contains("light"))
        assertTrue(html.contains("Jupyter Notebook"))
    }

    @Test
    fun `should generate dark mode HTML`() {
        val notebook = """
            {
              "cells": [],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)
        val html = doc.toHtml(lightMode = false)

        assertTrue(html.contains("jupyter-notebook"))
        assertTrue(html.contains("dark"))
    }

    @Test
    fun `should use custom title in HTML if provided`() {
        val notebook = """
            {
              "cells": [],
              "metadata": {
                "title": "Custom Title"
              },
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("Custom Title"))
    }

    @Test
    fun `should use default title if not provided`() {
        val notebook = """
            {
              "cells": [],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("Jupyter Notebook"))
    }

    // ==================== Invalid JSON Tests ====================

    @Test
    fun `should handle invalid JSON gracefully`() {
        val invalid = "{ invalid json here"

        val doc = parser.parse(invalid)

        assertEquals(invalid, doc.rawContent)
        assertEquals(invalid, doc.parsedContent)
        assertTrue(doc.metadata.isEmpty())
    }

    @Test
    fun `should handle non-JSON text`() {
        val text = "This is just plain text, not JSON"

        val doc = parser.parse(text)

        assertEquals(text, doc.rawContent)
        assertEquals(text, doc.parsedContent)
    }

    @Test
    fun `should show error in HTML for invalid JSON`() {
        val invalid = "{ not valid }"

        val doc = parser.parse(invalid)
        val html = doc.toHtml(lightMode = true)

        assertTrue(html.contains("error") || html.contains("Failed"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate correct JSON`() {
        val valid = """
            {
              "cells": [],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val errors = parser.validate(valid)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should report error for invalid JSON`() {
        val invalid = "{ not valid JSON"

        val errors = parser.validate(invalid)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("JSON") || it.contains("format") })
    }

    @Test
    fun `should validate empty notebook`() {
        val empty = "{}"

        val errors = parser.validate(empty)

        assertTrue(errors.isEmpty())  // Empty but valid JSON
    }

    // ==================== Edge Cases ====================

    @Test
    fun `should handle empty cells array`() {
        val notebook = """
            {
              "cells": [],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("0", doc.metadata["cells"])
        assertNotNull(doc.parsedContent)
    }

    @Test
    fun `should handle cell with empty source`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "code",
                  "source": ""
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
    }

    @Test
    fun `should handle cell with empty outputs`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "code",
                  "source": "x = 1",
                  "outputs": []
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
    }

    @Test
    fun `should handle unknown cell type`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "unknown_type",
                  "source": "content"
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
        // Should use fallback handling for unknown type
    }

    @Test
    fun `should preserve special characters in source`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "code",
                  "source": "print('<>&\"')"
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
        assertTrue(doc.parsedContent.contains("print"))
    }

    @Test
    fun `should handle unicode in source`() {
        val notebook = """
            {
              "cells": [
                {
                  "cell_type": "code",
                  "source": "print('你好世界 🌍')"
                }
              ],
              "metadata": {},
              "nbformat": 4
            }
        """.trimIndent()

        val doc = parser.parse(notebook)

        assertEquals("1", doc.metadata["cells"])
        assertTrue(doc.parsedContent.contains("你好世界") || doc.parsedContent.contains("🌍"))
    }
}
