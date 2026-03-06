/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * R Markdown Parser for Kotlin Multiplatform
 * Supports parsing .rmd and .rmarkdown files
 *
 *########################################################*/
package digital.vasic.yole.format.rmarkdown

import digital.vasic.yole.format.*

/**
 * Parser for R Markdown (.rmd, .rmarkdown) files
 */
class RMarkdownParser : TextParser {
    
    override val supportedFormat = FormatRegistry.formats.first { it.id == TextFormat.ID_RMARKDOWN }
    
    /** Parses R Markdown [content] into a [ParsedDocument], extracting YAML front matter and code chunks. */
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val filename = options["filename"] as? String ?: ""

        val (frontMatter, markdownContent, hasFrontMatter) = extractFrontMatter(content)
        val codeChunks = extractCodeChunks(markdownContent)

        return ParsedDocument(
            format = supportedFormat,
            rawContent = content,
            parsedContent = generateContentHtml(markdownContent, codeChunks, true),
            metadata = buildMap {
                put("has_frontmatter", hasFrontMatter.toString())
                put("code_chunks", codeChunks.size.toString())
                put("r_chunks", codeChunks.count { it.language == "r" }.toString())
                frontMatter.forEach { (key, value) -> put(key, value) }
            }
        )
    }
    
    /** Renders [document] as themed HTML with styled code chunks and document header based on [lightMode]. */
    override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
        val (frontMatter, markdownContent, _) = extractFrontMatter(document.rawContent)
        val codeChunks = extractCodeChunks(markdownContent)
        
        val themeClass = if (lightMode) "light" else "dark"
        val title = frontMatter["title"] ?: "R Markdown Document"
        
        return """
            |<div class="rmarkdown-document $themeClass">
            |<div class="rmarkdown-header">
            |  <h1>$title</h1>
            |  <div class="document-info">
            |    <span class="code-chunks">R Code Chunks: ${codeChunks.count { it.language == "r" }}</span>
            |    <span class="total-chunks">Total Chunks: ${codeChunks.size}</span>
            |  </div>
            |</div>
            |<div class="rmarkdown-content">
            |${generateContentHtml(markdownContent, codeChunks, lightMode)}
            |</div>
            |</div>
            |<style>
            |.rmarkdown-document { font-family: sans-serif; line-height: 1.6; }
            |.rmarkdown-document.light { background: white; color: black; }
            |.rmarkdown-document.dark { background: #1e1e1e; color: #d4d4d4; }
            |.rmarkdown-header { border-bottom: 2px solid #276dc2; padding: 1rem; margin-bottom: 1rem; }
            |.document-info { display: flex; gap: 1rem; font-size: 0.9rem; color: #666; }
            |.document-info .dark { color: #aaa; }
            |.rmarkdown-content { padding: 0 1rem; }
            |.code-chunk { border: 1px solid #ddd; border-radius: 4px; margin: 1rem 0; overflow: hidden; }
            |.code-chunk .dark { border-color: #444; }
            |.chunk-header { background: #276dc2; color: white; padding: 0.5rem 1rem; font-family: monospace; font-size: 0.9rem; }
            |.r-chunk .chunk-header { background: #276dc2; }
            |.python-chunk .chunk-header { background: #3776ab; }
            |.bash-chunk .chunk-header { background: #4eaa25; }
            |.sql-chunk .chunk-header { background: #e38c00; }
            |.chunk-content { padding: 1rem; background: #f8f8f8; }
            |.dark .chunk-content { background: #2d2d2d; }
            |.chunk-code { font-family: monospace; white-space: pre-wrap; }
            |</style>
        """.trimMargin()
    }
    
    /** Returns true if this parser supports the given [format]. */
    override fun canParse(format: TextFormat): Boolean {
        return supportedFormat.id == format.id
    }

    /** Validates R Markdown [content] for mismatched code chunk delimiters and unclosed YAML front matter. */
    override fun validate(content: String): List<String> {
        val issues = mutableListOf<String>()

        // Check for unclosed code chunks
        val chunkStartRegex = "^[ \\t]*```\\{[^}]*\\}".toRegex(RegexOption.MULTILINE)
        val chunkEndRegex = "^[ \\t]*```\\s*$".toRegex(RegexOption.MULTILINE)
        val chunkStarts = chunkStartRegex.findAll(content).count()
        val chunkEnds = chunkEndRegex.findAll(content).count()

        // Each code chunk needs an opening ```{...} and a closing ```
        // Total ``` lines that are pure closings = chunkEnds - those that are also openings
        // Actually: chunkEndRegex matches lines that are ONLY ```, not ```{...}
        // So we need chunkStarts == (chunkEnds count of standalone ``` closings)
        if (chunkStarts != chunkEnds) {
            issues.add("Mismatched code chunk delimiters")
        }

        // Check for unclosed front matter - look for --- on its own line
        val lines = content.lines()
        val yamlDelimiterIndices = lines.indices.filter { lines[it].trim() == "---" }
        if (yamlDelimiterIndices.size == 1) {
            // Only one --- delimiter found, front matter is unclosed
            issues.add("Unclosed YAML front matter")
        }

        return issues
    }
    
    private fun extractFrontMatter(content: String): Triple<Map<String, String>, String, Boolean> {
        val lines = content.lines()
        if (lines.isEmpty() || lines[0].trim() != "---") {
            return Triple(emptyMap(), content, false)
        }

        val frontMatter = mutableMapOf<String, String>()
        var i = 1

        while (i < lines.size && lines[i].trim() != "---") {
            val line = lines[i].trim()
            if (line.contains(":")) {
                val parts = line.split(":", limit = 2)
                val key = parts[0].trim()
                var value = parts[1].trim()
                // Strip surrounding quotes from values
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length - 1)
                }
                frontMatter[key] = value
            }
            i++
        }

        // Check if we found a closing ---
        val hasFrontMatter = i < lines.size && lines[i].trim() == "---"
        val remainingContent = if (hasFrontMatter) lines.drop(i + 1).joinToString("\n") else lines.drop(1).joinToString("\n")
        return Triple(frontMatter, remainingContent, hasFrontMatter)
    }
    
    private fun extractCodeChunks(content: String): List<CodeChunk> {
        val chunks = mutableListOf<CodeChunk>()
        // Match code chunks with optional leading whitespace before backticks
        // Also match chunks that reach end-of-content without a closing delimiter
        val chunkRegex = "[ \\t]*```\\{([^}]+)\\}\\s*\\n([\\s\\S]*?)(?:\\n[ \\t]*```|$)".toRegex()

        chunkRegex.findAll(content).forEach { matchResult ->
            val languageAndOptions = matchResult.groupValues[1].trim()
            val code = matchResult.groupValues[2]

            // Extract language (first word before any options)
            val language = languageAndOptions.split(",", " ").first().trim()

            chunks.add(CodeChunk(
                language = language,
                code = code,
                options = languageAndOptions
            ))
        }

        return chunks
    }
    
    private fun generateContentHtml(content: String, codeChunks: List<CodeChunk>, lightMode: Boolean): String {
        var processedContent = content

        // Replace code chunks with styled versions
        // Use regex to find and replace chunks including optional leading whitespace
        val chunkReplaceRegex = "[ \\t]*```\\{([^}]+)\\}\\s*\\n([\\s\\S]*?)(?:\\n[ \\t]*```|$)".toRegex()
        var chunkIndex = 0
        processedContent = chunkReplaceRegex.replace(processedContent) { matchResult ->
            if (chunkIndex < codeChunks.size) {
                val styledChunk = generateCodeChunkHtml(codeChunks[chunkIndex], lightMode)
                chunkIndex++
                styledChunk
            } else {
                matchResult.value
            }
        }

        // Process regular markdown content (simplified - in production, use a markdown parser)
        return processedContent
            .replace("```([^`]+)```".toRegex()) { match ->
                val code = match.groupValues[1]
                "<pre><code>${escapeHtml(code)}</code></pre>"
            }
            .replace("\n\n", "</p><p>")
            .replace("# (.+)".toRegex()) { match ->
                "<h1>${match.groupValues[1]}</h1>"
            }
            .replace("## (.+)".toRegex()) { match ->
                "<h2>${match.groupValues[1]}</h2>"
            }
            .replace("### (.+)".toRegex()) { match ->
                "<h3>${match.groupValues[1]}</h3>"
            }
            .replace("\\*\\*(.+)\\*\\*".toRegex()) { match ->
                "<strong>${match.groupValues[1]}</strong>"
            }
            .replace("\\*(.+)\\*".toRegex()) { match ->
                "<em>${match.groupValues[1]}</em>"
            }
    }
    
    private fun generateCodeChunkHtml(chunk: CodeChunk, lightMode: Boolean): String {
        val chunkClass = when (chunk.language) {
            "r" -> "r-chunk"
            "python" -> "python-chunk"
            "bash" -> "bash-chunk"
            "sql" -> "sql-chunk"
            else -> "code-chunk"
        }

        return """
            |<div class="code-chunk $chunkClass">
            |  <div class="chunk-header">
            |    ${chunk.language.uppercase()} Code Chunk
            |  </div>
            |  <div class="chunk-content">
            |    <pre class="chunk-code">${chunk.code}</pre>
            |  </div>
            |</div>
        """.trimMargin()
    }
    
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

/**
 * Register the R Markdown parser with the registry
 */
fun registerRMarkdownParser() {
    ParserRegistry.register(RMarkdownParser())
}

/** Represents an R Markdown code chunk with its [language], [code] body, and raw [options] string. */
data class CodeChunk(
    val language: String,
    val code: String,
    val options: String
)