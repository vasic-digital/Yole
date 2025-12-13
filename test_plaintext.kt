import digital.vasic.yole.format.plaintext.PlaintextParser
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat

fun main() {
    println("Testing PlainText parser...")
    
    try {
        // Test format detection
        val format = FormatRegistry.getByExtension(".txt")
        println("Format detected: ${format?.name} (ID: ${format?.id})")
        
        // Test parser creation
        val parser = PlaintextParser()
        println("Parser created successfully")
        
        // Test basic parsing
        val content = "Hello, World!\nThis is a test."
        val result = parser.parse(content)
        println("Parsing successful: ${result.format.name}")
        println("Metadata: ${result.metadata}")
        
        // Test HTML conversion
        val html = parser.toHtml(result, true)
        println("HTML conversion successful: ${html.length} characters")
        
        println("All tests passed!")
        
    } catch (e: Exception) {
        println("Test failed: ${e.message}")
        e.printStackTrace()
    }
}