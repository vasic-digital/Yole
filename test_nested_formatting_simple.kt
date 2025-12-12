/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple test to understand nested formatting issues
 *
 *########################################################*/

import digital.vasic.yole.format.markdown.MarkdownParser

fun main() {
    val parser = MarkdownParser()
    
    // Test cases for nested formatting
    val testCases = listOf(
        "*italic with **bold** inside*",
        "**bold with *italic* inside**",
        "***bold and italic***",
        "~~strikethrough with **bold** inside~~",
        "`code with *italic* inside`",
        "*italic with `code` inside*",
        "**bold with `code` inside**",
        "~~strikethrough with `code` inside~~"
    )
    
    println("Testing nested formatting scenarios:")
    println("=".repeat(50))
    
    for (content in testCases) {
        println("\nInput: $content")
        val document = parser.parse(content)
        println("Output: ${document.parsedContent}")
        
        // Check for proper nesting
        val hasEm = document.parsedContent.contains("<em>")
        val hasStrong = document.parsedContent.contains("<strong>")
        val hasCode = document.parsedContent.contains("<code>")
        val hasStrike = document.parsedContent.contains("<s>")
        
        println("Contains <em>: $hasEm")
        println("Contains <strong>: $hasStrong")
        println("Contains <code>: $hasCode")
        println("Contains <s>: $hasStrike")
        println("-".repeat(30))
    }
}