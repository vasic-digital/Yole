import digital.vasic.yole.format.markdown.MarkdownParser

fun main() {
    val parser = MarkdownParser()
    val content = "*italic with **bold** inside*"
    val document = parser.parse(content)
    
    println("Input: $content")
    println("Output: ${document.parsedContent}")
    println("Has em tags: ${document.parsedContent.contains("<em>")}")
    println("Has strong tags: ${document.parsedContent.contains("<strong>")}")
}