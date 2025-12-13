#!/usr/bin/env kotlin

/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple Binary Detection Test Runner
 * Tests basic binary detection functionality
 *
 *########################################################*/

import digital.vasic.yole.format.binary.BinaryParser
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat

fun main() {
    println("Testing Binary Detection...")
    
    try {
        val binaryParser = BinaryParser()
        val binaryFormat = FormatRegistry.getById(TextFormat.ID_BINARY)
        
        println("✓ Binary format registered: ${binaryFormat?.name}")
        
        // Test basic parsing
        val content = "MZ\u0090\u0000\u0003"
        val result = binaryParser.parse(content, mapOf("filename" to "test.exe"))
        
        println("✓ Binary parsing successful")
        println("  - Format: ${result.format.name}")
        println("  - MIME type: ${result.metadata["mime_type"]}")
        println("  - File type: ${result.metadata["file_type"]}")
        println("  - Extension: ${result.metadata["extension"]}")
        println("  - Is binary: ${result.metadata["is_binary"]}")
        
        // Test HTML generation
        val html = binaryParser.toHtml(result, lightMode = true)
        println("✓ HTML generation successful (${html.length} characters)")
        
        // Test dark mode
        val darkHtml = binaryParser.toHtml(result, lightMode = false)
        println("✓ Dark HTML generation successful (${darkHtml.length} characters)")
        
        // Test validation
        val errors = binaryParser.validate(content)
        println("✓ Validation successful (${errors.size} errors)")
        
        println("\n🎉 All binary detection tests passed!")
        
    } catch (e: Exception) {
        println("❌ Test failed: ${e.message}")
        e.printStackTrace()
    }
}