import digital.vasic.yole.format.wikitext.WikitextParser
import digital.vasic.yole.format.TextFormat

fun main() {
    val parser = WikitextParser()
    val content = """
        = Main Title =
        
        This is a paragraph.
        
        == Section ==
        
        * Item 1
        * Item 2
    """.trimIndent()
    
    val result = parser.parse(content)
    println("Format ID: ${result.format.id}")
    println("Expected: ${TextFormat.ID_WIKITEXT}")
    println("Match: ${result.format.id == TextFormat.ID_WIKITEXT}")
}