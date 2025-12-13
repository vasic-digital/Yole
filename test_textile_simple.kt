import digital.vasic.yole.format.textile.TextileParser

fun main() {
    val parser = TextileParser()
    val content = """
        h1. Test Document
        
        This is a *bold* text and _italic_ text.
        
        * List item 1
        * List item 2
        
        "Link":http://example.com
        
        !image.jpg!
    """.trimIndent()
    
    val result = parser.parse(content)
    println("Format: ${result.format.name}")
    println("Raw content: ${result.rawContent}")
    println("Parsed content: ${result.parsedContent}")
    println("Metadata: ${result.metadata}")
    
    val html = parser.toHtml(result, true)
    println("HTML: $html")
}