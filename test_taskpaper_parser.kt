/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Simple test runner for TaskPaper parser
 *
 *########################################################*/

import digital.vasic.yole.format.taskpaper.TaskpaperParser
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.format.ParserRegistry

fun main() {
    println("TaskPaper Parser Test Runner")
    println("============================")
    
    try {
        // Test 1: Basic parsing
        println("\n1. Testing basic TaskPaper parsing...")
        testBasicParsing()
        
        // Test 2: Format detection
        println("\n2. Testing format detection...")
        testFormatDetection()
        
        // Test 3: Tags and metadata
        println("\n3. Testing tags and metadata...")
        testTagsAndMetadata()
        
        // Test 4: HTML conversion
        println("\n4. Testing HTML conversion...")
        testHtmlConversion()
        
        // Test 5: Validation
        println("\n5. Testing validation...")
        testValidation()
        
        println("\n✅ All tests passed successfully!")
        
    } catch (e: Exception) {
        println("\n❌ Test failed: ${e.message}")
        e.printStackTrace()
    }
}

fun testBasicParsing() {
    val parser = TaskpaperParser()
    val content = """
        Work Project:
        - Complete documentation
        - Review code changes
        Note about the project
        
        Personal Tasks:
        - Buy groceries
        - Call dentist
    """.trimIndent()
    
    val result = parser.parse(content)
    
    // Verify basic properties
    assert(result.format.id == TextFormat.ID_TASKPAPER) { "Wrong format ID" }
    assert(result.rawContent == content) { "Raw content mismatch" }
    assert(result.metadata["projects"] == "2") { "Expected 2 projects, got ${result.metadata["projects"]}" }
    assert(result.metadata["tasks"] == "4") { "Expected 4 tasks, got ${result.metadata["tasks"]}" }
    assert(result.metadata["notes"] == "1") { "Expected 1 note, got ${result.metadata["notes"]}" }
    
    println("✓ Basic parsing test passed")
}

fun testFormatDetection() {
    // Test extension detection
    val format = FormatRegistry.getByExtension(".taskpaper")
    assert(format != null) { "TaskPaper format not found" }
    assert(format?.id == TextFormat.ID_TASKPAPER) { "Wrong format ID" }
    
    // Test filename detection
    val formatByFilename = FormatRegistry.detectByFilename("tasks.taskpaper")
    assert(formatByFilename != null) { "TaskPaper format not detected by filename" }
    assert(formatByFilename?.id == TextFormat.ID_TASKPAPER) { "Wrong format ID from filename" }
    
    println("✓ Format detection test passed")
}

fun testTagsAndMetadata() {
    val parser = TaskpaperParser()
    val content = """
        - Complete task @today @high
        - Another task @done(2025-01-10)
        - Task with due date @due(2025-01-15)
    """.trimIndent()
    
    val result = parser.parse(content)
    
    assert(result.metadata["tasks"] == "3") { "Expected 3 tasks" }
    assert(result.metadata["todayTasks"] == "1") { "Expected 1 today task" }
    assert(result.metadata["doneTasks"] == "1") { "Expected 1 done task" }
    
    println("✓ Tags and metadata test passed")
}

fun testHtmlConversion() {
    val parser = TaskpaperParser()
    val content = """
        Project:
        - Task @today @done
        Note about project
    """.trimIndent()
    
    val document = parser.parse(content)
    val html = parser.toHtml(document, lightMode = true)
    
    assert(html.contains("<div class='taskpaper'>")) { "Missing taskpaper div" }
    assert(html.contains("taskpaper-project")) { "Missing project styling" }
    assert(html.contains("taskpaper-task")) { "Missing task styling" }
    assert(html.contains("taskpaper-note")) { "Missing note styling" }
    assert(html.contains("taskpaper-tag-today")) { "Missing today tag styling" }
    assert(html.contains("taskpaper-tag-done")) { "Missing done tag styling" }
    
    println("✓ HTML conversion test passed")
}

fun testValidation() {
    val parser = TaskpaperParser()
    
    // Test valid content
    val validContent = """
        Project:
        - Valid task
        Note
    """.trimIndent()
    
    val validErrors = parser.validate(validContent)
    assert(validErrors.isEmpty()) { "Valid content should have no errors" }
    
    // Test malformed task marker
    val invalidContent = """
        -Invalid task (no space)
        -  Invalid task (extra space)
    """.trimIndent()
    
    val invalidErrors = parser.validate(invalidContent)
    assert(invalidErrors.isNotEmpty()) { "Invalid content should have errors" }
    
    println("✓ Validation test passed")
}

// Simple assert function
fun assert(condition: Boolean, message: String) {
    if (!condition) {
        throw AssertionError(message)
    }
}