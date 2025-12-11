import digital.vasic.yole.format.markdown.MarkdownParser

fun main() {
    val parser = MarkdownParser()
    val content = "[link text](https://example.com)"
    val document = parser.parse(content)
    
    println("Input: $content")
    println("Output: ${document.parsedContent}")
}