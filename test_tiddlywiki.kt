import digital.vasic.yole.format.tiddlywiki.TiddlyWikiParser
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat

fun main() {
    println("Testing TiddlyWiki Parser...")
    
    val parser = TiddlyWikiParser()
    
    // Test basic parsing
    val content = """
        title: My First Tiddler
        tags: introduction tutorial
        created: 20250101120000000
        modified: 20250102150000000
        
        This is the content of my first tiddler.
        It can span multiple lines and contain various formatting.
        
        ! Heading 1
        Some content under the heading.
        
        !! Heading 2
        More content here.
    """.trimIndent()

    try {
        val result = parser.parse(content)
        println("✓ Parsing successful!")
        println("  Title: ${result.metadata["title"]}")
        println("  Tags: ${result.metadata["tags"]}")
        println("  Format: ${result.format.name}")
        println("  HTML length: ${result.parsedContent.length} characters")
        
        // Test HTML conversion
        val html = parser.toHtml(result, lightMode = true)
        println("✓ HTML conversion successful!")
        println("  HTML contains tiddlywiki class: ${html.contains("tiddlywiki")}")
        
        // Test validation
        val issues = parser.validate(content)
        println("✓ Validation successful!")
        println("  Issues found: ${issues.size}")
        
        println("\nAll tests passed! 🎉")
        
    } catch (e: Exception) {
        println("✗ Error: ${e.message}")
        e.printStackTrace()
    }
}