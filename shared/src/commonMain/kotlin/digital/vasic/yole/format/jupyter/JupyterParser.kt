/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Jupyter Notebook Parser for Kotlin Multiplatform
 * Supports parsing .ipynb files
 *
 *########################################################*/
package digital.vasic.yole.format.jupyter

import digital.vasic.yole.format.*
import kotlinx.serialization.json.*

/**
 * Parser for Jupyter Notebook (.ipynb) files
 */
class JupyterParser : TextParser {
    
    override val supportedFormat = FormatRegistry.formats.first { it.id == TextFormat.ID_JUPYTER }
    
    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        val filename = options["filename"] as? String ?: ""
        
        return try {
            val json = Json.parseToJsonElement(content)
            val notebook = parseNotebook(json)
            
            ParsedDocument(
                format = supportedFormat,
                rawContent = content,
                parsedContent = generateNotebookHtml(notebook, true),
                metadata = buildMap {
                    put("cells", notebook.cells.size.toString())
                    put("kernel", notebook.kernel)
                    put("language", notebook.language)
                    put("format_version", notebook.formatVersion)
                    notebook.title?.let { put("title", it) }
                }
            )
        } catch (e: Exception) {
            // If JSON parsing fails, treat as plain text
            ParsedDocument(
                format = supportedFormat,
                rawContent = content,
                parsedContent = content,
                metadata = emptyMap()
            )
        }
    }
    
    override fun toHtml(document: ParsedDocument, lightMode: Boolean): String {
        return try {
            val json = Json.parseToJsonElement(document.rawContent)
            val notebook = parseNotebook(json)
            generateNotebookHtml(notebook, lightMode)
        } catch (e: Exception) {
            // Fallback to plain text display
            """<div class="jupyter-notebook">
               |<div class="error">Failed to parse Jupyter notebook: ${e.message}</div>
               |<pre>${document.rawContent}</pre>
               |</div>
            """.trimMargin()
        }
    }
    
    override fun canParse(format: TextFormat): Boolean {
        return supportedFormat.id == format.id
    }
    
    override fun validate(content: String): List<String> {
        return try {
            Json.parseToJsonElement(content)
            emptyList()
        } catch (e: Exception) {
            listOf("Invalid JSON format: ${e.message}")
        }
    }
    
    private fun parseNotebook(json: JsonElement): JupyterNotebook {
        val obj = json.jsonObject
        
        val cells = obj["cells"]?.jsonArray?.map { parseCell(it) } ?: emptyList()
        val metadata = obj["metadata"]?.jsonObject ?: JsonObject(emptyMap())
        val nbformat = obj["nbformat"]?.jsonPrimitive?.intOrNull ?: 4
        val nbformatMinor = obj["nbformat_minor"]?.jsonPrimitive?.intOrNull ?: 0
        
        val kernel = metadata["kernelspec"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "python3"
        val language = metadata["language_info"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "python"
        val title = metadata["title"]?.jsonPrimitive?.content
        
        return JupyterNotebook(
            cells = cells,
            kernel = kernel,
            language = language,
            formatVersion = "$nbformat.$nbformatMinor",
            title = title
        )
    }
    
    private fun parseCell(cellJson: JsonElement): NotebookCell {
        val obj = cellJson.jsonObject
        val cellType = obj["cell_type"]?.jsonPrimitive?.content ?: "code"
        val source = obj["source"]?.let { 
            when (it) {
                is JsonArray -> it.joinToString("") { elem -> elem.jsonPrimitive.content }
                is JsonPrimitive -> it.content
                else -> ""
            }
        } ?: ""
        
        val outputs = obj["outputs"]?.jsonArray?.map { parseOutput(it) } ?: emptyList()
        val executionCount = obj["execution_count"]?.jsonPrimitive?.intOrNull
        
        return NotebookCell(
            cellType = cellType,
            source = source,
            outputs = outputs,
            executionCount = executionCount
        )
    }
    
    private fun parseOutput(outputJson: JsonElement): CellOutput {
        val obj = outputJson.jsonObject
        val outputType = obj["output_type"]?.jsonPrimitive?.content ?: ""
        val data = obj["data"]?.jsonObject ?: JsonObject(emptyMap())
        val text = obj["text"]?.let { 
            when (it) {
                is JsonArray -> it.joinToString("") { elem -> elem.jsonPrimitive.content }
                is JsonPrimitive -> it.content
                else -> ""
            }
        } ?: ""
        
        return CellOutput(
            outputType = outputType,
            data = data,
            text = text
        )
    }
    
    private fun generateNotebookHtml(notebook: JupyterNotebook, lightMode: Boolean): String {
        val themeClass = if (lightMode) "light" else "dark"
        
        return """
            |<div class="jupyter-notebook $themeClass">
            |<div class="notebook-header">
            |  <h1>${notebook.title ?: "Jupyter Notebook"}</h1>
            |  <div class="notebook-info">
            |    <span class="kernel">Kernel: ${notebook.kernel}</span>
            |    <span class="language">Language: ${notebook.language}</span>
            |    <span class="cells">Cells: ${notebook.cells.size}</span>
            |  </div>
            |</div>
            |<div class="notebook-cells">
            |${notebook.cells.joinToString("\n") { generateCellHtml(it, lightMode) }}
            |</div>
            |</div>
            |<style>
            |.jupyter-notebook { font-family: sans-serif; line-height: 1.6; }
            |.jupyter-notebook.light { background: white; color: black; }
            |.jupyter-notebook.dark { background: #1e1e1e; color: #d4d4d4; }
            |.notebook-header { border-bottom: 2px solid #4e9a06; padding: 1rem; margin-bottom: 1rem; }
            |.notebook-info { display: flex; gap: 1rem; font-size: 0.9rem; color: #666; }
            |.notebook-info .dark { color: #aaa; }
            |.notebook-cell { border: 1px solid #ddd; border-radius: 4px; margin: 1rem 0; overflow: hidden; }
            |.notebook-cell .dark { border-color: #444; }
            |.cell-header { background: #f5f5f5; padding: 0.5rem 1rem; font-weight: bold; border-bottom: 1px solid #ddd; }
            |.notebook-cell .dark .cell-header { background: #2d2d2d; border-color: #444; }
            |.cell-content { padding: 1rem; }
            |.code-cell .cell-header { background: #e7f2fa; }
            |.code-cell .dark .cell-header { background: #1e3a5f; }
            |.markdown-cell .cell-header { background: #f0f0f0; }
            |.markdown-cell .dark .cell-header { background: #3d3d3d; }
            |.raw-cell .cell-header { background: #f9f9f9; }
            |.raw-cell .dark .cell-header { background: #4d4d4d; }
            |.execution-count { color: #666; font-family: monospace; }
            |.dark .execution-count { color: #aaa; }
            |.code-source { font-family: monospace; white-space: pre-wrap; background: #f8f8f8; padding: 0.5rem; border-radius: 3px; }
            |.dark .code-source { background: #2d2d2d; }
            |.cell-output { margin-top: 0.5rem; }
            |.output-text { font-family: monospace; white-space: pre-wrap; background: #f0f0f0; padding: 0.5rem; border-radius: 3px; }
            |.dark .output-text { background: #3d3d3d; }
            |</style>
        """.trimMargin()
    }
    
    private fun generateCellHtml(cell: NotebookCell, lightMode: Boolean): String {
        val cellClass = when (cell.cellType) {
            "code" -> "code-cell"
            "markdown" -> "markdown-cell"
            else -> "raw-cell"
        }
        
        val headerText = when (cell.cellType) {
            "code" -> "Code" + (cell.executionCount?.let { " [${it}]" } ?: "")
            "markdown" -> "Markdown"
            else -> "Raw"
        }
        
        val sourceHtml = when (cell.cellType) {
            "markdown" -> cell.source // Markdown will be rendered by the markdown parser
            else -> "<div class=\"code-source\">${escapeHtml(cell.source)}</div>"
        }
        
        val outputsHtml = if (cell.outputs.isNotEmpty()) {
            "<div class=\"cell-output\">${
                cell.outputs.joinToString("\n") { output ->
                    "<div class=\"output-text\">${escapeHtml(output.text)}</div>"
                }
            }</div>"
        } else ""
        
        return """
            |<div class="notebook-cell $cellClass">
            |  <div class="cell-header">
            |    <span class="execution-count">$headerText</span>
            |  </div>
            |  <div class="cell-content">
            |    $sourceHtml
            |    $outputsHtml
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
 * Register the Jupyter parser with the registry
 */
fun registerJupyterParser() {
    ParserRegistry.register(JupyterParser())
}

data class JupyterNotebook(
    val cells: List<NotebookCell>,
    val kernel: String,
    val language: String,
    val formatVersion: String,
    val title: String? = null
)

data class NotebookCell(
    val cellType: String,
    val source: String,
    val outputs: List<CellOutput>,
    val executionCount: Int? = null
)

data class CellOutput(
    val outputType: String,
    val data: JsonObject,
    val text: String
)