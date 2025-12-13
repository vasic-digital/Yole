import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.TextFormat

fun main() {
    val parser = WikitextParser()
    
    val content = """
        = Template Test =
        
        {{TemplateName}}
        
        This document uses a simple template.
    """.trimIndent()
    
    val result = parser.parse(content)
    
    println("Test Result:")
    println("Format ID: ${result.format.id}")
    println("Expected: ${TextFormat.ID_WIKITEXT}")
    println("Content matches: ${content == result.rawContent}")
    println("Success: ${result.format.id == TextFormat.ID_WIKITEXT && content == result.rawContent}")
}