import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.format.restructuredtext.RestructuredTextParser

fun main() {
    println("Testing reStructuredText Parser...")
    
    // Test format detection
    val format = FormatRegistry.getByExtension(".rst")
    println("Format detected: ${format?.name ?: "null"}")
    
    if (format != null) {
        println("Format ID: ${format.id}")
        println("Expected ID: ${TextFormat.ID_RESTRUCTUREDTEXT}")
        println("Match: ${format.id == TextFormat.ID_RESTRUCTUREDTEXT}")
    }
    
    // Test parser creation
    val parser = RestructuredTextParser()
    println("Parser created successfully: ${parser.supportedFormat.name}")
    
    // Test basic parsing
    val content = """
        Document Title
        ==============
        
        This is a simple paragraph.
        
        Section 1
        ---------
        
        * List item 1
        * List item 2
        
        .. note:: This is a note directive.
    """.trimIndent()
    
    try {
        val result = parser.parse(content)
        println("Parsing successful!")
        println("Format: ${result.format.name}")
        println("Sections: ${result.metadata["sections"]}")
        println("Directives: ${result.metadata["directives"]}")
        println("Max Level: ${result.metadata["max_level"]}")
        
        // Test HTML conversion
        val html = result.toHtml(true)
        println("HTML conversion successful!")
        println("HTML length: ${html.length} characters")
        println("HTML contains document title: ${html.contains("Document Title")}")
        
    } catch (e: Exception) {
        println("Error during parsing: ${e.message}")
        e.printStackTrace()
    }
    
    println("Test completed!")
}