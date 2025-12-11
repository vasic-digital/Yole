/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Debug test for nested formatting
 *
 *########################################################*/

import digital.vasic.yole.format.markdown.MarkdownParser

fun main() {
    val parser = MarkdownParser()
    val content = "*italic with **bold** inside*"
    
    println("Input: $content")
    
    val document = parser.parse(content)
    println("Parsed content: ${document.parsedContent}")
    println("Contains <em>: ${document.parsedContent.contains("<em>")}")
    println("Contains <strong>: ${document.parsedContent.contains("<strong>")}")
    println("Contains </em>: ${document.parsedContent.contains("</em>")}")
    println("Contains </strong>: ${document.parsedContent.contains("</strong>")}")
}