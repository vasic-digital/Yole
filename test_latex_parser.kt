#!/usr/bin/env kotlin

/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple test runner for LaTeX parser
 *
 *########################################################*/

import digital.vasic.yole.format.latex.LatexParser
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat

fun main() {
    println("Testing LaTeX Parser...")
    
    // Test 1: Format detection
    println("\n=== Test 1: Format Detection ===")
    val format = FormatRegistry.getByExtension(".tex")
    if (format != null) {
        println("✓ Format detected: ${format.name} (${format.id})")
    } else {
        println("✗ Format not detected")
        return
    }
    
    // Test 2: Parser instantiation
    println("\n=== Test 2: Parser Instantiation ===")
    val parser = LatexParser()
    println("✓ Parser created: ${parser.supportedFormat.name}")
    
    // Test 3: Basic parsing
    println("\n=== Test 3: Basic Parsing ===")
    val content = """
        \documentclass{article}
        \title{Test Document}
        \author{John Doe}
        \date{2025-01-01}
        
        \begin{document}
        \maketitle
        
        \section{Introduction}
        This is a test document.
        
        \end{document}
    """.trimIndent()
    
    val result = parser.parse(content)
    println("✓ Document parsed successfully")
    println("  - Format: ${result.format.name}")
    println("  - Metadata: ${result.metadata}")
    println("  - Errors: ${result.errors.size}")
    
    // Test 4: HTML conversion
    println("\n=== Test 4: HTML Conversion ===")
    val html = parser.toHtml(result, lightMode = true)
    println("✓ HTML generated (${html.length} characters)")
    println("  - Contains title: ${html.contains("document-title")}")
    println("  - Contains author: ${html.contains("document-author")}")
    println("  - Contains section: ${html.contains("section")}")
    
    // Test 5: Validation
    println("\n=== Test 5: Validation ===")
    val errors = parser.validate(content)
    println("✓ Validation completed: ${errors.size} errors found")
    errors.forEach { error ->
        println("  - $error")
    }
    
    // Test 6: Error detection
    println("\n=== Test 6: Error Detection ===")
    val malformedContent = """
        \begin{itemize}
        \item First item
        \end{enumerate}
    """.trimIndent()
    
    val malformedResult = parser.parse(malformedContent)
    println("✓ Malformed document parsed: ${malformedResult.errors.size} errors")
    malformedResult.errors.forEach { error ->
        println("  - $error")
    }
    
    println("\n=== All Tests Completed ===")
}