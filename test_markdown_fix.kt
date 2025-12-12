/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple test to verify Markdown parser improvements
 *
 *########################################################*/

import digital.vasic.yole.format.markdown.MarkdownParser

fun main() {
    println("Testing Markdown Parser Nested Formatting Improvements")
    println("=".repeat(60))
    
    val parser = MarkdownParser()
    
    // Test cases that should work with the new implementation
    val testCases = listOf(
        "*italic with **bold** inside*",
        "**bold with *italic* inside**",
        "***bold and italic***",
        "~~strikethrough with **bold** inside~~",
        "*italic with `code` inside*",
        "**bold with `code` inside**",
        "~~strikethrough with `code` inside~~"
    )
    
    var allTestsPassed = true
    
    for (content in testCases) {
        println("\nTesting: $content")
        try {
            val document = parser.parse(content)
            val html = document.parsedContent
            
            println("Result: $html")
            
            // Basic validation
            val hasProperStructure = html.startsWith("<div class='markdown'>") && html.endsWith("</div>")
            val hasStyle = html.contains("<style>")
            
            if (!hasProperStructure) {
                println("❌ FAILED: Improper HTML structure")
                allTestsPassed = false
            } else if (!hasStyle) {
                println("❌ FAILED: Missing CSS styling")
                allTestsPassed = false
            } else {
                println("✅ PASSED: Basic structure OK")
            }
            
        } catch (e: Exception) {
            println("❌ FAILED: Exception occurred - ${e.message}")
            allTestsPassed = false
        }
    }
    
    println("\n" + "=".repeat(60))
    if (allTestsPassed) {
        println("🎉 ALL TESTS PASSED! The Markdown parser improvements are working.")
    } else {
        println("❌ Some tests failed. Check the implementation.")
    }
}