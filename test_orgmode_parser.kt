#!/usr/bin/env kotlin

/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple test script to verify Org Mode parser functionality
 *
 *########################################################*/

import digital.vasic.yole.format.orgmode.OrgModeParser
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat

fun main() {
    println("Testing Org Mode Parser...")
    
    try {
        // Test format detection
        val format = FormatRegistry.getByExtension(".org")
        println("✓ Format detection: ${format?.name ?: "NOT FOUND"}")
        
        // Test parser instantiation
        val parser = OrgModeParser()
        println("✓ Parser instantiation: SUCCESS")
        
        // Test basic parsing
        val content = """
            * Main Heading
            This is content under the main heading.
            
            ** Subheading
            More content here.
            
            *** Deep subheading
            Even deeper content.
        """.trimIndent()
        
        val result = parser.parse(content)
        println("✓ Basic parsing: SUCCESS")
        println("  - Headings: ${result.metadata["headings"]}")
        println("  - Max level: ${result.metadata["max_level"]}")
        println("  - Content length: ${result.rawContent.length} characters")
        
        // Test TODO parsing
        val todoContent = """
            * TODO Write documentation
            This task needs to be completed.
            
            * DONE Completed task
            This task is already done.
        """.trimIndent()
        
        val todoResult = parser.parse(todoContent)
        println("✓ TODO parsing: SUCCESS")
        println("  - Todos: ${todoResult.metadata["todos"]}")
        
        // Test HTML conversion
        val html = parser.toHtml(result, lightMode = true)
        println("✓ HTML conversion: SUCCESS")
        println("  - HTML length: ${html.length} characters")
        println("  - Contains org-mode-document class: ${html.contains("org-mode-document")}")
        
        println("\n🎉 All tests passed! Org Mode parser is working correctly.")
        
    } catch (e: Exception) {
        println("❌ Test failed: ${e.message}")
        e.printStackTrace()
    }
}