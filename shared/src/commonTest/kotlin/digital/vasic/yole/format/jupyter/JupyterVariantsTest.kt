/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Comprehensive unit tests for Jupyter Notebook variants
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
 * Comprehensive unit tests for Jupyter Notebook format variants and edge cases.
 *
 * Tests cover:
 * - Different Jupyter notebook variants (kernels, metadata variations)
 * - Various notebook structures and cell arrangements
 * - Notebook metadata variations (different kernelspec versions)
 * - Different output types and cell outputs
 * - Round-trip parsing for variants
 * - Edge cases (corrupted notebooks, missing fields)
 * - Performance benchmarks for large notebooks
 * - HTML conversion for different variants
 */
class JupyterVariantsTest {

    private val parser = JupyterParser()

    // ==================== Kernel Variants Tests ====================

    @Test
    fun `should parse Python 2 kernel notebook`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["print 'Hello from Python 2'"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "display_name": "Python 2",
                        "language": "python",
                        "name": "python2"
                    },
                    "language_info": {
                        "name": "python",
                        "version": "2.7.18"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("python2", result.metadata["kernel"])
        assertEquals("python", result.metadata["language"])
        
        val html = result.parsedContent
        assertTrue(html.contains("Kernel: python2"))
        assertTrue(html.contains("Hello from Python 2"))
    }

    @Test
    fun `should parse R kernel notebook`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["print("Hello from R!")"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "display_name": "R",
                        "language": "R",
                        "name": "ir"
                    },
                    "language_info": {
                        "name": "R",
                        "version": "4.0.0"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("ir", result.metadata["kernel"])
        assertEquals("R", result.metadata["language"])
        
        val html = result.parsedContent
        assertTrue(html.contains("Kernel: ir"))
        assertTrue(html.contains("Language: R"))
        assertTrue(html.contains("Hello from R!"))
    }

    @Test
    fun `should parse Julia kernel notebook`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["println(\"Hello from Julia!\")"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "display_name": "Julia 1.6",
                        "language": "julia",
                        "name": "julia-1.6"
                    },
                    "language_info": {
                        "name": "julia",
                        "version": "1.6.0"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("julia-1.6", result.metadata["kernel"])
        assertEquals("julia", result.metadata["language"])
        
        val html = result.parsedContent
        assertTrue(html.contains("Kernel: julia-1.6"))
        assertTrue(html.contains("Language: julia"))
        assertTrue(html.contains("Hello from Julia!"))
    }

    @Test
    fun `should parse Scala kernel notebook`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["println(\"Hello from Scala!\")"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "display_name": "Scala",
                        "language": "scala",
                        "name": "scala"
                    },
                    "language_info": {
                        "name": "scala",
                        "version": "2.12.10"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("scala", result.metadata["kernel"])
        assertEquals("scala", result.metadata["language"])
        
        val html = result.parsedContent
        assertTrue(html.contains("Kernel: scala"))
        assertTrue(html.contains("Language: scala"))
        assertTrue(html.contains("Hello from Scala!"))
    }

    @Test
    fun `should parse C++ kernel notebook`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["#include <iostream>\\n", "std::cout << \"Hello from C++!\" << std::endl;"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "display_name": "C++17",
                        "language": "C++",
                        "name": "xcpp17"
                    },
                    "language_info": {
                        "name": "C++",
                        "version": "17"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("xcpp17", result.metadata["kernel"])
        assertEquals("C++", result.metadata["language"])
        
        val html = result.parsedContent
        assertTrue(html.contains("Kernel: xcpp17"))
        assertTrue(html.contains("Language: C++"))
        assertTrue(html.contains("#include <iostream>"))
    }

    // ==================== Metadata Variations Tests ====================

    @Test
    fun `should parse notebook with minimal metadata`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["print('minimal')"]
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
        assertEquals("python3", result.metadata["kernel"]) // Default
        assertEquals("python", result.metadata["language"]) // Default
        assertEquals("4.2", result.metadata["format_version"])
    }

    @Test
    fun `should parse notebook with extended metadata`() {
        val content = """
            {
                "cells": [],
                "metadata": {
                    "title": "Advanced Analysis",
                    "description": "A comprehensive data analysis notebook",
                    "authors": ["John Doe", "Jane Smith"],
                    "kernelspec": {
                        "display_name": "Python 3 (ipykernel)",
                        "language": "python",
                        "name": "python3"
                    },
                    "language_info": {
                        "name": "python",
                        "version": "3.9.7",
                        "mimetype": "text/x-python",
                        "codemirror_mode": {
                            "name": "ipython",
                            "version": 3
                        },
                        "pygments_lexer": "ipython3",
                        "nbconvert_exporter": "python",
                        "file_extension": ".py"
                    }
                },
                "nbformat": 4,
                "nbformat_minor": 5
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("Advanced Analysis", result.metadata["title"])
        assertEquals("python3", result.metadata["kernel"])
        assertEquals("python", result.metadata["language"])
        assertEquals("4.5", result.metadata["format_version"])
    }

    @Test
    fun `should parse notebook with custom metadata`() {
        val content = """
            {
                "cells": [],
                "metadata": {
                    "custom_field": "custom_value",
                    "numeric_field": 42,
                    "boolean_field": true,
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
        assertEquals("python3", result.metadata["kernel"])
        // Custom fields are not extracted to metadata map, only standard ones
    }

    // ==================== Cell Structure Variations Tests ====================

    @Test
    fun `should parse notebook with empty cells array`() {
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

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        assertEquals("0", result.metadata["cells"])
        
        val html = result.parsedContent
        assertTrue(html.contains("Cells: 0"))
    }

    @Test
    fun `should parse notebook with null execution count`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": null,
                        "metadata": {},
                        "outputs": [],
                        "source": ["print('not executed')"]
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
        assertTrue(html.contains("Code")) // Should not show [null]
        assertFalse(html.contains("[null]"))
        assertTrue(html.contains("not executed"))
    }

    @Test
    fun `should parse notebook with missing cell fields`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code"
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
        
        val html = result.parsedContent
        assertTrue(html.contains("Code"))
    }

    @Test
    fun `should parse notebook with different cell arrangements`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "markdown",
                        "metadata": {},
                        "source": ["# Header"]
                    },
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["x = 1"]
                    },
                    {
                        "cell_type": "raw",
                        "metadata": {},
                        "source": ["Raw content"]
                    },
                    {
                        "cell_type": "markdown",
                        "metadata": {},
                        "source": ["## Subheader"]
                    },
                    {
                        "cell_type": "code",
                        "execution_count": 2,
                        "metadata": {},
                        "outputs": [],
                        "source": ["y = x + 1"]
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
        assertEquals("5", result.metadata["cells"])
        
        val html = result.parsedContent
        assertTrue(html.contains("Header"))
        assertTrue(html.contains("x = 1"))
        assertTrue(html.contains("Raw content"))
        assertTrue(html.contains("Subheader"))
        assertTrue(html.contains("y = x + 1"))
    }

    // ==================== Output Types Tests ====================

    @Test
    fun `should parse notebook with stream output types`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [
                            {
                                "name": "stdout",
                                "output_type": "stream",
                                "text": ["Standard output\\n"]
                            },
                            {
                                "name": "stderr",
                                "output_type": "stream",
                                "text": ["Error output\\n"]
                            }
                        ],
                        "source": ["print('Standard output')\\n", "import sys\\n", "sys.stderr.write('Error output\\\n')"]
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
        assertTrue(html.contains("Standard output"))
        assertTrue(html.contains("Error output"))
    }

    @Test
    fun `should parse notebook with display data output`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [
                            {
                                "data": {
                                    "text/plain": ["array([1, 2, 3, 4, 5])"],
                                    "text/html": ["<pre>[1 2 3 4 5]</pre>"]
                                },
                                "execution_count": 1,
                                "metadata": {},
                                "output_type": "execute_result"
                            }
                        ],
                        "source": ["import numpy as np\\n", "np.array([1, 2, 3, 4, 5])"]
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
        assertTrue(html.contains("array([1, 2, 3, 4, 5])"))
    }

    @Test
    fun `should parse notebook with error output`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [
                            {
                                "ename": "NameError",
                                "evalue": "name 'undefined_var' is not defined",
                                "output_type": "error",
                                "traceback": [
                                    "Traceback (most recent call last):",
                                    "  File \"<ipython-input-1-1234567890ab>\", line 1, in <module>",
                                    "    undefined_var",
                                    "NameError: name 'undefined_var' is not defined"
                                ]
                            }
                        ],
                        "source": ["undefined_var"]
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
        assertTrue(html.contains("NameError"))
        assertTrue(html.contains("undefined_var"))
    }

    // ==================== Corrupted Notebooks Tests ====================

    @Test
    fun `should handle corrupted JSON gracefully`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "source": ["print('test')"]
                        // Missing closing braces and comma
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
    fun `should handle notebook with invalid nbformat`() {
        val content = """
            {
                "cells": [],
                "metadata": {
                    "kernelspec": {
                        "name": "python3"
                    }
                },
                "nbformat": "invalid",
                "nbformat_minor": 2
            }
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_JUPYTER, result.format.id)
        // Should handle gracefully and use defaults
        assertEquals("4.2", result.metadata["format_version"])
    }

    @Test
    fun `should handle notebook with missing cells field`() {
        val content = """
            {
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
        assertEquals("0", result.metadata["cells"]) // Should default to 0
    }

    @Test
    fun `should handle notebook with null cells`() {
        val content = """
            {
                "cells": null,
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
        assertEquals("0", result.metadata["cells"]) // Should handle null gracefully
    }

    // ==================== Round-trip Parsing Tests ====================

    @Test
    fun `should support round-trip parsing for kernel variants`() {
        val originalContent = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["console.log('JavaScript round-trip test');"]
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
        assertEquals("javascript", firstParse.metadata["kernel"])
        assertEquals("javascript", secondParse.metadata["kernel"])
    }

    @Test
    fun `should preserve metadata through round-trip for variants`() {
        val originalContent = """
            {
                "cells": [
                    {
                        "cell_type": "markdown",
                        "metadata": {},
                        "source": ["# R Analysis"]
                    },
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["data <- c(1, 2, 3, 4, 5)"]
                    }
                ],
                "metadata": {
                    "title": "Statistical Analysis",
                    "kernelspec": {
                        "display_name": "R",
                        "language": "R",
                        "name": "ir"
                    },
                    "language_info": {
                        "name": "R",
                        "version": "4.0.0"
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
        assertEquals("Statistical Analysis", firstParse.metadata["title"])
        assertEquals("Statistical Analysis", secondParse.metadata["title"])
        assertEquals("ir", firstParse.metadata["kernel"])
        assertEquals("ir", secondParse.metadata["kernel"])
    }

    // ==================== Performance Benchmarks ====================

    @Test
    fun `should parse large notebook with many cells efficiently`() {
        // Create a large notebook with 100 cells
        val cells = (1..100).joinToString(",\n                ") { cellIndex ->
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
              "source": ["# Computation $cellIndex\\n", "result = ${cellIndex} * 2\\n", "print(f'Output from cell $cellIndex')"]
            }
            """.trimIndent()
        }
        
        val largeContent = """
            {
              "cells": [
                $cells
              ],
              "metadata": {
                "title": "Large Performance Test",
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
        assertEquals("100", result.metadata["cells"])
        
        // Performance assertion - should parse in reasonable time (less than 2 seconds for this size)
        val parseTime = endTime - startTime
        assertTrue(parseTime < 2000, "Parsing should complete within 2 seconds, took: ${parseTime}ms")
    }

    @Test
    fun `should parse notebook with large cell content efficiently`() {
        // Create a notebook with cells containing large content
        val largeSource = (1..50).joinToString("\\n") { line ->
            "# This is line $line with some content to make it larger and test performance"
        }
        
        val cells = (1..20).joinToString(",\n                ") { cellIndex ->
            """
            {
              "cell_type": "code",
              "execution_count": $cellIndex,
              "metadata": {},
              "outputs": [],
              "source": ["$largeSource"]
            }
            """.trimIndent()
        }
        
        val largeContent = """
            {
              "cells": [
                $cells
              ],
              "metadata": {
                "title": "Large Content Test",
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
        assertEquals("20", result.metadata["cells"])
        
        // Performance assertion - should parse in reasonable time (less than 1 second)
        val parseTime = endTime - startTime
        assertTrue(parseTime < 1000, "Parsing should complete within 1 second, took: ${parseTime}ms")
    }

    @Test
    fun `should convert large notebook to HTML efficiently`() {
        // Create a large notebook for HTML conversion test
        val cells = (1..50).joinToString(",\n                ") { cellIndex ->
            """
            {
              "cell_type": "code",
              "execution_count": $cellIndex,
              "metadata": {},
              "outputs": [
                {
                  "name": "stdout",
                  "output_type": "stream",
                  "text": ["Result $cellIndex\\n"]
                }
              ],
              "source": ["# Large computation $cellIndex\\n", "print(f'Result $cellIndex')"]
            }
            """.trimIndent()
        }
        
        val largeContent = """
            {
              "cells": [
                $cells
              ],
              "metadata": {
                "title": "Large HTML Conversion Test",
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
        assertTrue(html.contains("Large HTML Conversion Test"))
        assertTrue(html.contains("jupyter-notebook"))
        assertTrue(html.contains("Result 1"))
        assertTrue(html.contains("Result 50"))
        
        // Performance assertion - should convert in reasonable time (less than 1 second)
        val conversionTime = endTime - startTime
        assertTrue(conversionTime < 1000, "HTML conversion should complete within 1 second, took: ${conversionTime}ms")
    }

    // ==================== HTML Conversion Tests for Variants ====================

    @Test
    fun `should convert different kernel notebooks to HTML with appropriate styling`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "markdown",
                        "metadata": {},
                        "source": ["# Multi-language Notebook"]
                    },
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["print('Hello from Python')"]
                    }
                ],
                "metadata": {
                    "title": "Cross-language Analysis",
                    "kernelspec": {
                        "display_name": "Python 3",
                        "language": "python",
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
        assertTrue(html.contains("Cross-language Analysis"))
        assertTrue(html.contains("Kernel: python3"))
        assertTrue(html.contains("Language: python"))
        assertTrue(html.contains("Multi-language Notebook"))
        assertTrue(html.contains("Hello from Python"))
        assertTrue(html.contains("jupyter-notebook light"))
    }

    @Test
    fun `should apply dark mode to different variants`() {
        val content = """
            {
                "cells": [
                    {
                        "cell_type": "code",
                        "execution_count": 1,
                        "metadata": {},
                        "outputs": [],
                        "source": ["console.log('Dark mode test');"]
                    }
                ],
                "metadata": {
                    "kernelspec": {
                        "name": "javascript"
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
        assertTrue(html.contains("Kernel: javascript"))
    }
}