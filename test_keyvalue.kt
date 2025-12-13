import digital.vasic.yole.format.keyvalue.KeyValueParser
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat

fun main() {
    println("Testing KeyValueParser...")
    
    val parser = KeyValueParser()
    
    // Test basic parsing
    val content = """
        name=John Doe
        age=30
        city=New York
    """.trimIndent()
    
    val result = parser.parse(content)
    println("Parse result: $result")
    println("Format ID: ${result.format.id}")
    println("Metadata: ${result.metadata}")
    
    // Test format detection
    val format = FormatRegistry.getByExtension(".ini")
    println("Format detection: ${format?.id}")
    
    println("KeyValueParser test completed successfully!")
}