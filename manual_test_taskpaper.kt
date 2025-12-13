// Manual test for TaskPaper parser
// Run this after building the project

fun main() {
    println("Manual TaskPaper Parser Test")
    println("============================")
    
    try {
        // Create parser instance
        val parser = digital.vasic.yole.format.taskpaper.TaskpaperParser()
        
        // Test 1: Basic parsing
        println("\n1. Testing basic TaskPaper parsing...")
        val basicContent = """
            Work Project:
            - Complete documentation
            - Review code changes
            Note about the project
            
            Personal Tasks:
            - Buy groceries
            - Call dentist
        """.trimIndent()
        
        val result = parser.parse(basicContent)
        println("Format: ${result.format.id}")
        println("Projects: ${result.metadata["projects"]}")
        println("Tasks: ${result.metadata["tasks"]}")
        println("Notes: ${result.metadata["notes"]}")
        
        // Test 2: Tags and metadata
        println("\n2. Testing tags and metadata...")
        val taggedContent = """
            - Complete task @today @high
            - Another task @done(2025-01-10)
            - Task with due date @due(2025-01-15)
        """.trimIndent()
        
        val taggedResult = parser.parse(taggedContent)
        println("Tasks: ${taggedResult.metadata["tasks"]}")
        println("Today tasks: ${taggedResult.metadata["todayTasks"]}")
        println("Done tasks: ${taggedResult.metadata["doneTasks"]}")
        
        // Test 3: HTML conversion
        println("\n3. Testing HTML conversion...")
        val htmlContent = """
            Project:
            - Task @today @done
            Note about project
        """.trimIndent()
        
        val document = parser.parse(htmlContent)
        val html = parser.toHtml(document, lightMode = true)
        println("HTML length: ${html.length}")
        println("Contains taskpaper div: ${html.contains("<div class='taskpaper'>")}")
        println("Contains project styling: ${html.contains("taskpaper-project")}")
        println("Contains task styling: ${html.contains("taskpaper-task")}")
        println("Contains note styling: ${html.contains("taskpaper-note")}")
        
        // Test 4: Validation
        println("\n4. Testing validation...")
        val validContent = """
            Project:
            - Valid task
            Note
        """.trimIndent()
        
        val validErrors = parser.validate(validContent)
        println("Valid content errors: ${validErrors.size}")
        
        val invalidContent = """
            -Invalid task (no space)
        """.trimIndent()
        
        val invalidErrors = parser.validate(invalidContent)
        println("Invalid content errors: ${invalidErrors.size}")
        if (invalidErrors.isNotEmpty()) {
            println("First error: ${invalidErrors.first()}")
        }
        
        // Test 5: Complex document
        println("\n5. Testing complex document...")
        val complexContent = """
            Work Projects: @work @q1-2025
            	Urgent Tasks: @urgent
            		- Fix critical bug @today @high @due(2025-01-14)
            		This bug is affecting production users
            		
            		- Deploy hotfix @done(2025-01-13)
            		Hotfix deployed successfully
            
            Personal Goals: @personal @2025
            	Health:
            		- Exercise 3 times this week @today
            		- Schedule annual checkup @due(2025-02-01)
            		Remember to exercise
        """.trimIndent()
        
        val complexResult = parser.parse(complexContent)
        println("Complex document - Projects: ${complexResult.metadata["projects"]}")
        println("Complex document - Tasks: ${complexResult.metadata["tasks"]}")
        println("Complex document - Notes: ${complexResult.metadata["notes"]}")
        println("Complex document - Done tasks: ${complexResult.metadata["doneTasks"]}")
        println("Complex document - Today tasks: ${complexResult.metadata["todayTasks"]}")
        
        println("\n✅ All manual tests completed successfully!")
        
    } catch (e: Exception) {
        println("\n❌ Manual test failed: ${e.message}")
        e.printStackTrace()
    }
}