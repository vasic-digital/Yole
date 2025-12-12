import digital.vasic.yole.format.markdown.MarkdownParser

fun main() {
    val parser = MarkdownParser()
    
    // Test the specific failing case: strikethrough with bold
    val content = "~~**bold strikethrough**~~"
    val document = parser.parse(content)
    
    println("Input: $content")
    println("Actual output:")
    println(document.parsedContent)
    println()
    
    // Expected output from the test
    val expected = """<div class='markdown'><style>
.markdown { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; }
.markdown h1 { font-size: 2em; font-weight: 600; border-bottom: 1px solid #eee; padding-bottom: 0.3em; margin-top: 24px; margin-bottom: 16px; }
.markdown h2 { font-size: 1.5em; font-weight: 600; border-bottom: 1px solid #eee; padding-bottom: 0.3em; margin-top: 24px; margin-bottom: 16px; }
.markdown h3 { font-size: 1.25em; font-weight: 600; margin-top: 24px; margin-bottom: 16px; }
.markdown h4 { font-size: 1em; font-weight: 600; margin-top: 24px; margin-bottom: 16px; }
.markdown h5 { font-size: 0.875em; font-weight: 600; margin-top: 24px; margin-bottom: 16px; }
.markdown h6 { font-size: 0.85em; font-weight: 600; color: #666; margin-top: 24px; margin-bottom: 16px; }
.markdown p { margin-top: 0; margin-bottom: 16px; }
.markdown blockquote { border-left: 4px solid #ddd; padding: 0 1em; color: #666; margin: 0 0 16px 0; }
.markdown ul, .markdown ol { margin-top: 0; margin-bottom: 16px; padding-left: 2em; }
.markdown li { margin-bottom: 0.25em; }
.markdown code { background-color: rgba(27,31,35,0.05); padding: 0.2em 0.4em; margin: 0; font-size: 85%; font-family: 'SF Mono', Monaco, Consolas, 'Courier New', monospace; border-radius: 3px; }
.markdown pre { background-color: #f6f8fa; padding: 16px; overflow-x: auto; font-size: 85%; line-height: 1.45; border-radius: 6px; margin-bottom: 16px; }
.markdown pre code { background-color: transparent; padding: 0; margin: 0; border-radius: 0; }
.markdown hr { height: 0.25em; padding: 0; margin: 24px 0; background-color: #e1e4e8; border: 0; }
.markdown table { border-collapse: collapse; border-spacing: 0; margin-bottom: 16px; }
.markdown table th { font-weight: 600; padding: 6px 13px; border: 1px solid #ddd; background-color: #f6f8fa; }
.markdown table td { padding: 6px 13px; border: 1px solid #ddd; }
.markdown a { color: #0366d6; text-decoration: none; }
.markdown a:hover { text-decoration: underline; }
.markdown img { max-width: 100%; }
.markdown input[type='checkbox'] { margin-right: 0.5em; }
</style><p><del><strong>bold strikethrough</strong></del> </p></div>"""
    
    println("Expected output:")
    println(expected)
    println()
    
    println("Are they equal? ${document.parsedContent == expected}")
    
    // Let's also test some other failing cases
    println("\n=== Testing image with empty alt text ===")
    val imageContent = "![](image.jpg)"
    val imageDocument = parser.parse(imageContent)
    println("Input: $imageContent")
    println("Actual output:")
    println(imageDocument.parsedContent)
    
    println("\n=== Testing task list checkbox ===")
    val taskContent = "- [x] Completed task"
    val taskDocument = parser.parse(taskContent)
    println("Input: $taskContent")
    println("Actual output:")
    println(taskDocument.parsedContent)
    
    println("\n=== Testing escaped characters ===")
    val escapeContent = "\\*not italic\\* and \\**not bold\\**"
    val escapeDocument = parser.parse(escapeContent)
    println("Input: $escapeContent")
    println("Actual output:")
    println(escapeDocument.parsedContent)
    
    println("\n=== Testing whitespace in formatting ===")
    val whitespaceContent = "** bold ** and * italic *"
    val whitespaceDocument = parser.parse(whitespaceContent)
    println("Input: $whitespaceContent")
    println("Actual output:")
    println(whitespaceDocument.parsedContent)
}