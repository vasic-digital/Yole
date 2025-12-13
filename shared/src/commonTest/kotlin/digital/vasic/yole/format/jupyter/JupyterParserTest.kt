/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive unit tests for Jupyter Notebook parser
 *
 *########################################################*/
package digital.vasic.yole.format.jupyter

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.format.ParsedDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse


/**
 * Comprehensive unit tests for Jupyter Notebook format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic Jupyter parsing (notebooks, cells, metadata)
 * - Different cell types (code, markdown, raw)
 * - Cell outputs and execution results
 * - Notebook metadata and kernelspec
 * - Round-trip parsing (parse → format → parse)
 * - Edge cases (empty files, malformed Jupyter)
 * - Performance benchmarks
 * - HTML conversion
 */
class JupyterParserTest {

    private val parser = JupyterParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect Jupyter format by ipynb extension`() {
        val format = FormatRegistry.getByExtension(".ipynb")

        assertNotNull(format)
        assertEquals(TextFormat.ID_JUPYTER, format.id)
        assertEquals("Jupyter Notebook", format.name)
    }

    @Test
    fun `should detect Jupyter format by filename`() {
        val format = FormatRegistry.detectByFilename("notebook.ipynb")

        assertNotNull(format)
        assertEquals(TextFormat.ID_JUPYTER, format.id)
    }

    @Test
    fun `should detect Jupyter format by content pattern`() {
        val content = """
            {
                "cells": [],
                "metadata": {},
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val format = FormatRegistry.detectByContent(content)

        assertNotNull(format)
        assertEquals(TextFormat.ID_JUPYTER, format.id)
    }

    // ==================== Basic Jupyter Parsing Tests ====================

    @Test
    fun `should parse basic Jupyter notebook structure`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["print('Hello, World!')"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "display_name": "Python 3",
                        "language": "python",
                        "name": "python3"
                    },
                    "language_info": {
                        "name": "python",
                        "version": "3.8.0"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Check metadata extraction
        assertEquals("1", result.metadata["cells"])
        assertEquals("python3", result.metadata["kernel"])
        assertEquals("python", result.metadata["language"])
        assertEquals("4.2", result.metadata["format_version"])
    }

    @Test
    fun `should parse Jupyter notebook with title`() {
        val content = """
            {
                "cells": [],
                "metadata": {
                    "title": "My Analysis Notebook",
                    "kernelspec": {
                        "name": "python3",
                        "display_name": "Python 3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 4
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("My Analysis Notebook", result.metadata["title"])
        assertEquals("0", result.metadata["cells"])
        assertEquals("python3", result.metadata["kernel"])
    }

    @Test
    fun `should parse Jupyter notebook without kernelspec`() {
        val content = """
            {
                "cells": [],
                "metadata": {},
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("python3", result.metadata["kernel"]) // Default kernel
        assertEquals("python", result.metadata["language"]) // Default language
    }

    // ==================== Cell Types Tests ====================

    @Test
    fun `should parse code cells with execution count`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 5,
                        "metadata": {},
                        "outputs": [],
                        "source": ["x = 42\\n", "print(x)"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("1", result.metadata["cells"])
        
        // Check HTML contains code cell indicators
        val html = result.parsedContent
        assertTrue(html.contains("Code [5]"))
        assertTrue(html.contains("x = 42"))
        assertTrue(html.contains("print(x)"))
    }

    @Test
    fun `should parse markdown cells`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "markdown",
                        "metadata": {},
                        "source": ["# Introduction\\n", "\\n", "This is a **markdown** cell."]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        
        // Check HTML contains markdown cell indicators
        val html = result.parsedContent
        assertTrue(html.contains("Markdown"))
        assertTrue(html.contains("# Introduction"))
        assertTrue(html.contains("This is a **markdown** cell"))
    }

    @Test
    fun `should parse raw cells`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "raw",
                        "metadata": {},
                        "source": ["This is raw text\\n", "It won't be executed or rendered."]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        
        // Check HTML contains raw cell indicators
        val html = result.parsedContent
        assertTrue(html.contains("Raw"))
        assertTrue(html.contains("This is raw text"))
        assertTrue(html.contains("It won't be executed or rendered."))
    }

    @Test
    fun `should parse mixed cell types`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "markdown",
                        "metadata": {},
                        "source": ["# Data Analysis"]
                    },
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["import pandas as pd"]
                    },
                    {
                        "cell_type": "raw",
                        "metadata": {},
                        "source": ["Raw data notes"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("3", result.metadata["cells"])
        
        val html = result.parsedContent
        assertTrue(html.contains("Data Analysis"))
        assertTrue(html.contains("import pandas as pd"))
        assertTrue(html.contains("Raw data notes"))
    }

    // ==================== Cell Outputs Tests ====================

    @Test
    fun `should parse code cells with text output`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 2,
                        "metadata": {},
                        "outputs": [
                            {
                                "name": "stdout",
                                "output_type": "stream",
                                "text": ["Hello, World!\\n", "Result: 42"]
                            }
                        ],
                        "source": ["print(\"Hello, World!\")\\n", "print(\"Result:\", 42)"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        
        val html = result.parsedContent
        assertTrue(html.contains("Hello, World!"))
        assertTrue(html.contains("Result: 42"))
    }

    @Test
    fun `should parse code cells with multiple outputs`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 3,
                        "metadata": {},
                        "outputs": [
                            {
                                "name": "stdout",
                                "output_type": "stream",
                                "text": ["First output\\n"]
                            },
                            {
                                "name": "stderr",
                                "output_type": "stream",
                                "text": ["Error message\\n"]
                            }
                        ],
                        "source": ["print(\"First output\")\\n", "import sys\\n", "sys.stderr.write(\"Error message\\n\")"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        
        val html = result.parsedContent
        assertTrue(html.contains("First output"))
        assertTrue(html.contains("Error message"))
    }

    @Test
    fun `should parse cells with display data output`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 4,
                        "metadata": {},
                        "outputs": [
                            {
                                "data": {
                                    "text/plain": ["<matplotlib.figure.Figure at 0x7f8b8c0>"]
                                },
                                "execution_count": 4,
                                "metadata": {},
                                "output_type": "execute_result"
                            }
                        ],
                        "source": ["import matplotlib.pyplot as plt\\n", "plt.figure()"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        
        val html = result.parsedContent
        assertTrue(html.contains("<matplotlib.figure.Figure"))
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert simple notebook to HTML`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "markdown",
                        "metadata": {},
                        "source": ["# Simple Notebook"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("jupyter-notebook"))
        assertTrue(html.contains("Jupyter Notebook"))
        assertTrue(html.contains("Kernel: python3"))
        assertTrue(html.contains("Cells: 1"))
        assertTrue(html.contains("Simple Notebook"))
    }

    @Test
    fun `should apply light mode styles`() {
        val content = """
            {
                "cells": [],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("jupyter-notebook light"))
        assertTrue(html.contains("background: white"))
        assertTrue(html.contains("color: black"))
    }

    @Test
    fun `should apply dark mode styles`() {
        val content = """
            {
                "cells": [],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = false)

        assertNotNull(html)
        assertTrue(html.contains("jupyter-notebook dark"))
        assertTrue(html.contains("background: #1e1e1e"))
        assertTrue(html.contains("color: #d4d4d4"))
    }

    @Test
    fun `should convert notebook with title to HTML`() {
        val content = """
            {
                "cells": [],
                "metadata": {
                    "title": "Data Analysis Report",
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("Data Analysis Report"))
        assertFalse(html.contains("Jupyter Notebook")) // Should use title instead
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun `should handle empty notebook`() {
        val content = """
            {
                "cells": [],
                "metadata": {},
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("0", result.metadata["cells"])
        assertEquals("python3", result.metadata["kernel"]) // Default kernel
        assertEquals("python", result.metadata["language"]) // Default language
    }

    @Test
    fun `should handle malformed JSON gracefully`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "source": ["print('test')"]
                        // Missing closing braces
                    }
                ]
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        // Should fallback to plain text display
        assertEquals(content, result.parsedContent)
        assertTrue(result.metadata.isEmpty())
    }

    @Test
    fun `should handle invalid JSON`() {
        val content = """
            This is not valid JSON at all
            { invalid syntax
            [missing brackets]
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        // Should fallback to plain text display
        assertEquals(content, result.parsedContent)
        assertTrue(result.metadata.isEmpty())
    }

    @Test
    fun `should handle notebook with missing required fields`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code"
                    }
                ],
                "metadata": {},
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("1", result.metadata["cells"])
        // Should handle missing source, outputs gracefully
    }

    @Test
    fun `should handle cells with array source`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": null,
                        "metadata": {},
                        "outputs": [],
                        "source": [
                            "def hello_world():\\n",
                            "    print('Hello, World!')\\n",
                            "\\n",
                            "hello_world()"
                        ]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        
        val html = result.parsedContent
        assertTrue(html.contains("def hello_world():"))
        assertTrue(html.contains("print('Hello, World!')"))
        assertTrue(html.contains("hello_world()"))
    }

    @Test
    fun `should handle cells with string source`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "markdown",
                        "metadata": {},
                        "source": "# Single String Source\\n\\nThis is markdown."
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        
        val html = result.parsedContent
        assertTrue(html.contains("Single String Source"))
        assertTrue(html.contains("This is markdown"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate valid JSON content`() {
        val content = """
            {
                "cells": [],
                "metadata": {},
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should detect invalid JSON`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "source": ["invalid json missing quotes"]
                    }
                ],
                "metadata": {},
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Invalid JSON format"))
    }

    @Test
    fun `should detect completely invalid content`() {
        val content = """
            This is not JSON at all
            Just plain text
            With multiple lines
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Invalid JSON format"))
    }

    // ==================== Round-trip Parsing Tests ====================

    @Test
    fun `should support round-trip parsing`() {
        val originalContent = """
            {
                "cells": [
                    {
                        "cell_type": "markdown",
                        "metadata": {},
                        "source": ["# Test Notebook"]
                    },
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["print(\"Hello, World!\")"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
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
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 5,
                        "metadata": {},
                        "outputs": [],
                        "source": ["x = 10"]
                    }
                ],
                "metadata": {
                    "title": "Analysis Notebook",
                    "kernelspec": {
                        "name": "python3",
                        "display_name": "Python 3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 4
            }
        """.trimIndent()

        val firstParse = parser.parse(originalContent)
        val secondParse = parser.parse(firstParse.rawContent)
        
        assertEquals(firstParse.metadata, secondParse.metadata)
        assertEquals(firstParse.format.id, secondParse.format.id)
        assertEquals("Analysis Notebook", firstParse.metadata["title"])
        assertEquals("Analysis Notebook", secondParse.metadata["title"])
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should parse large Jupyter notebooks efficiently`() {
        // Create a large notebook with many cells using a simpler approach
        val cells = (1..50).joinToString(",\n                ") { cellIndex ->
            """
            {
              "cell_type": "code",
              "execution_count": $cellIndex,
              "metadata": {},
              "outputs": [],
              "source": ["# Cell $cellIndex\\n", "print('Result: ${cellIndex * 2}')"]
            }
            """.trimIndent()
        }
        
        val largeContent = """
            {
              "cells": [
                $cells
              ],
              "metadata": {
                "kernelspec": {
                  "name": "python3",
                  "display_name": "Python 3"
                }
              },
              "nbformat": 4,
              "nbformat_minor": 2
            }
        """.trimIndent()

        val startTime = System.currentTimeMillis()
        val result = parser.parse(largeContent)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("50", result.metadata["cells"])
        
        // Performance assertion - should parse in reasonable time (less than 1 second for this size)
        val parseTime = endTime - startTime
        assertTrue(parseTime < 1000, "Parsing should complete within 1 second, took: ${parseTime}ms")
    }

    @Test
    fun `should convert large notebooks to HTML efficiently`() {
        // Create a large notebook using a simpler approach
        val cells = (1..30).joinToString(",\n                ") { cellIndex ->
            """
            {
              "cell_type": "code",
              "execution_count": $cellIndex,
              "metadata": {},
              "outputs": [
                {
                  "name": "stdout",
                  "output_type": "stream",
                  "text": ["Output from cell $cellIndex\\n"]
                }
              ],
              "source": ["# Computation $cellIndex\\n", "result = ${cellIndex} * 10\\n", "print(f'Output from cell $cellIndex')"]
            }
            """.trimIndent()
        }
        
        val largeContent = """
            {
              "cells": [
                $cells
              ],
              "metadata": {
                "title": "Large Analysis",
                "kernelspec": {
                  "name": "python3",
                  "display_name": "Python 3"
                }
              },
              "nbformat": 4,
              "nbformat_minor": 2
            }
        """.trimIndent()

        val document = parser.parse(largeContent)
        
        val startTime = System.currentTimeMillis()
        val html = parser.toHtml(document, lightMode = true)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(html)
        assertTrue(html.contains("Large Analysis"))
        assertTrue(html.contains("jupyter-notebook"))
        assertTrue(html.contains("Output from cell"))
        
        // Performance assertion - should convert in reasonable time (less than 500ms)
        val conversionTime = endTime - startTime
        assertTrue(conversionTime < 500, "HTML conversion should complete within 500ms, took: ${conversionTime}ms")
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate with ParserRegistry`() {
        // Register the parser
        ParserRegistry.register(parser)
        
        // Get parser from registry
        val registryParser = ParserRegistry.getParser(TextFormat.ID_JUPYTER)
        
        assertNotNull(registryParser)
        assertEquals(TextFormat.ID_JUPYTER, registryParser.supportedFormat.id)
        
        // Test that it works
        val content = """
            {
                "cells": [],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()
        
        val result = registryParser.parse(content)
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val jupyterFormat = allFormats.find { it.id == TextFormat.ID_JUPYTER }

        assertNotNull(jupyterFormat)
        assertEquals("Jupyter Notebook", jupyterFormat.name)
        assertEquals(".ipynb", jupyterFormat.defaultExtension)
        assertTrue(jupyterFormat.extensions.contains(".ipynb"))
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should parse complex Jupyter notebook`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "markdown",
                        "metadata": {},
                        "source": ["# Data Analysis Report\\n", "\\n", "This notebook contains our analysis of the dataset."]
                    },
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [
                            {
                                "name": "stdout",
                                "output_type": "stream",
                                "text": ["Loading libraries...\\n", "Libraries loaded successfully!\\n"]
                            }
                        ],
                        "source": ["import pandas as pd\\n", "import numpy as np\\n", "import matplotlib.pyplot as plt\\n", "print('Libraries loaded successfully!')"]
                    },
                    {
                        "cell_type": "markdown",
                        "metadata": {},
                        "source": ["## Data Loading\\n", "\\n", "Let's load our dataset and examine its structure."]
                    },
                    {
                        "cell_type": "code",
                        "execution_count": 2,
                        "metadata": {},
                        "outputs": [
                            {
                                "data": {
                                    "text/html": ["<div>Data loaded: 1000 rows × 5 columns</div>"],
                                    "text/plain": ["DataFrame shape: (1000, 5)"]
                                },
                                "execution_count": 2,
                                "metadata": {},
                                "output_type": "execute_result"
                            }
                        ],
                        "source": ["df = pd.read_csv('data.csv')\\n", "df.shape"]
                    },
                    {
                        "cell_type": "code",
                        "execution_count": 3,
                        "metadata": {},
                        "outputs": [
                            {
                                "name": "stdout",
                                "output_type": "stream",
                                "text": ["Summary statistics:\\n", "mean: 42.5, std: 15.2\\n"]
                            }
                        ],
                        "source": ["print('Summary statistics:')\\n", "print(f'mean: {df.values.mean():.1f}, std: {df.values.std():.1f}')"]
                    }
                ],
                "metadata": {
                    "title": "Comprehensive Data Analysis",
                    "kernelspec": {
                        "display_name": "Python 3",
                        "language": "python",
                        "name": "python3"
                    },
                    "language_info": {
                        "name": "python",
                        "version": "3.8.5"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 4
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("5", result.metadata["cells"])
        assertEquals("Comprehensive Data Analysis", result.metadata["title"])
        assertEquals("python3", result.metadata["kernel"])
        assertEquals("python", result.metadata["language"])
        assertEquals("4.4", result.metadata["format_version"])
        
        // Verify HTML contains expected elements
        val html = result.parsedContent
        assertTrue(html.contains("Comprehensive Data Analysis"))
        assertTrue(html.contains("Data Analysis Report"))
        assertTrue(html.contains("import pandas as pd"))
        assertTrue(html.contains("Libraries loaded successfully!"))
        assertTrue(html.contains("Data loaded: 1000 rows"))
        assertTrue(html.contains("Summary statistics:"))
        assertTrue(html.contains("mean: 42.5, std: 15.2"))
    }

    @Test
    fun `should handle notebook with different kernel`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["console.log(\"Hello from JavaScript!\");"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "display_name": "JavaScript (Node.js)",
                        "language": "javascript",
                        "name": "javascript"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("javascript", result.metadata["kernel"])
        assertEquals("javascript", result.metadata["language"])
        
        val html = result.parsedContent
        assertTrue(html.contains("Kernel: javascript"))
        assertTrue(html.contains("Language: javascript"))
        assertTrue(html.contains("console.log(\"Hello from JavaScript!\");"))
    }
}