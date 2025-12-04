# Yole API Usage Examples

Comprehensive examples showing how to use Yole's Kotlin Multiplatform APIs.

**Target Audience**: Developers integrating Yole into their applications
**API Version**: 2.15.1
**Last Updated**: November 11, 2025

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Format Detection](#format-detection)
3. [Parsing Text Files](#parsing-text-files)
4. [Working with Documents](#working-with-documents)
5. [Format Registry](#format-registry)
6. [Platform-Specific Examples](#platform-specific-examples)
7. [Advanced Usage](#advanced-usage)
8. [Error Handling](#error-handling)

---

## Getting Started

### Adding Yole to Your Project

**Gradle (Kotlin DSL)**:
```kotlin
// In your shared module build.gradle.kts
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("digital.vasic.yole:shared:2.15.1")
            }
        }
    }
}
```

**Gradle (Groovy)**:
```groovy
dependencies {
    implementation 'digital.vasic.yole:shared:2.15.1'
}
```

### Basic Imports

```kotlin
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.model.Document
```

---

## Format Detection

### Detect Format by File Extension

```kotlin
import digital.vasic.yole.model.Document

fun detectFormatByExtension(filename: String): String {
    val document = Document(
        filename = filename,
        content = "",
        lastModified = 0L
    )

    val format = document.detectFormat()
    return format.name
}

// Usage examples
fun main() {
    println(detectFormatByExtension("notes.md"))        // "markdown"
    println(detectFormatByExtension("todo.txt"))        // "todotxt"
    println(detectFormatByExtension("data.csv"))        // "csv"
    println(detectFormatByExtension("document.tex"))    // "latex"
    println(detectFormatByExtension("config.properties")) // "keyvalue"
}
```

### Detect Format by Content

```kotlin
import digital.vasic.yole.model.Document

fun detectFormatByContent(content: String, filename: String = "unknown.txt"): String {
    val document = Document(
        filename = filename,
        content = content,
        lastModified = System.currentTimeMillis()
    )

    val format = document.detectFormat()
    return format.name
}

// Usage examples
fun main() {
    // Markdown content
    val markdownContent = "# Heading\n\nParagraph with **bold** text."
    println(detectFormatByContent(markdownContent))  // "markdown"

    // Todo.txt content
    val todoContent = "(A) Important task +project @context"
    println(detectFormatByContent(todoContent))  // "todotxt"

    // WikiText content
    val wikiContent = "== Section ==\n\nText with [[links]]."
    println(detectFormatByContent(wikiContent))  // "wikitext"

    // Org Mode content
    val orgContent = "* TODO Task\n** DONE Subtask"
    println(detectFormatByContent(orgContent))  // "orgmode"
}
```

### Check if File is Binary

```kotlin
import digital.vasic.yole.model.Document

fun isBinaryFile(content: ByteArray): Boolean {
    val document = Document(
        filename = "file",
        content = content.decodeToString(),
        lastModified = 0L
    )

    return document.detectFormat().name == "binary"
}

// Usage example
fun main() {
    val pdfBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
    println(isBinaryFile(pdfBytes))  // true

    val textBytes = "Hello, world!".encodeToByteArray()
    println(isBinaryFile(textBytes))  // false
}
```

---

## Parsing Text Files

### Parse Markdown

```kotlin
import digital.vasic.yole.format.markdown.MarkdownParser

fun parseMarkdown(content: String) {
    val parser = MarkdownParser()
    val result = parser.parse(content)

    println("HTML: ${result.html}")
    println("Plain text: ${result.plainText}")
    println("Has code blocks: ${result.metadata["hasCodeBlocks"]}")
}

// Usage example
fun main() {
    val markdown = """
        # My Document

        This is a paragraph with **bold** and *italic* text.

        ## Code Example

        ```kotlin
        fun hello() = println("Hello, Yole!")
        ```

        - Item 1
        - Item 2
        - Item 3
    """.trimIndent()

    parseMarkdown(markdown)
}
```

### Parse Todo.txt

```kotlin
import digital.vasic.yole.format.todotxt.TodoTxtParser
import digital.vasic.yole.format.todotxt.Task

fun parseTodoTxt(content: String): List<Task> {
    val parser = TodoTxtParser()
    val result = parser.parse(content)

    // Extract tasks from parsed result
    val tasks = result.metadata["tasks"] as? List<Task> ?: emptyList()

    return tasks
}

// Usage example
fun main() {
    val todoContent = """
        (A) Important task +project @home due:2025-11-15
        x 2025-11-10 Completed task
        (B) Medium priority task +work @office
        Regular task without priority
    """.trimIndent()

    val tasks = parseTodoTxt(todoContent)

    tasks.forEach { task ->
        println("Priority: ${task.priority}")
        println("Description: ${task.description}")
        println("Projects: ${task.projects}")
        println("Contexts: ${task.contexts}")
        println("Completed: ${task.completed}")
        println("---")
    }
}
```

### Parse CSV

```kotlin
import digital.vasic.yole.format.csv.CsvParser

fun parseCSV(content: String) {
    val parser = CsvParser()
    val result = parser.parse(content)

    val headers = result.metadata["headers"] as? List<String>
    val rows = result.metadata["rows"] as? List<List<String>>

    println("Headers: $headers")
    println("Row count: ${rows?.size}")

    rows?.forEach { row ->
        println(row.joinToString(" | "))
    }
}

// Usage example
fun main() {
    val csvContent = """
        Name,Age,City
        Alice,30,New York
        Bob,25,San Francisco
        Charlie,35,Chicago
    """.trimIndent()

    parseCSV(csvContent)
}
```

### Parse LaTeX

```kotlin
import digital.vasic.yole.format.latex.LatexParser

fun parseLatex(content: String) {
    val parser = LatexParser()
    val result = parser.parse(content)

    println("Document structure:")
    println(result.metadata["structure"])

    println("\nMath equations found:")
    val equations = result.metadata["equations"] as? List<String> ?: emptyList()
    equations.forEach { println("  $it") }
}

// Usage example
fun main() {
    val latexContent = """
        \documentclass{article}
        \begin{document}

        \section{Introduction}
        This is a LaTeX document with math: $E = mc^2$

        \subsection{Example Equation}
        $$
        \int_0^\infty e^{-x^2} dx = \frac{\sqrt{\pi}}{2}
        $$

        \end{document}
    """.trimIndent()

    parseLatex(latexContent)
}
```

### Parse Org Mode

```kotlin
import digital.vasic.yole.format.orgmode.OrgModeParser

fun parseOrgMode(content: String) {
    val parser = OrgModeParser()
    val result = parser.parse(content)

    val headlines = result.metadata["headlines"] as? List<Map<String, Any>>
    val todos = result.metadata["todos"] as? List<String>

    println("Headlines:")
    headlines?.forEach { headline ->
        println("  ${headline["level"]} ${headline["title"]}")
    }

    println("\nTODO items:")
    todos?.forEach { println("  - $it") }
}

// Usage example
fun main() {
    val orgContent = """
        * TODO Project Planning
        ** DONE Research phase
           CLOSED: [2025-11-10]
        ** IN-PROGRESS Development
        ** TODO Testing

        * Meeting Notes
        ** 2025-11-11 Team Sync
        - Discussed roadmap
        - Reviewed sprint goals
    """.trimIndent()

    parseOrgMode(orgContent)
}
```

---

## Working with Documents

### Create a New Document

```kotlin
import digital.vasic.yole.model.Document

fun createDocument(
    filename: String,
    content: String
): Document {
    return Document(
        filename = filename,
        content = content,
        lastModified = System.currentTimeMillis()
    )
}

// Usage example
fun main() {
    val doc = createDocument(
        filename = "notes.md",
        content = "# My Notes\n\nFirst note."
    )

    println("Filename: ${doc.filename}")
    println("Format: ${doc.detectFormat().name}")
    println("Content length: ${doc.content.length}")
}
```

### Modify Document Content

```kotlin
import digital.vasic.yole.model.Document

fun modifyDocument(document: Document, newContent: String): Document {
    // Documents are immutable, so we create a new one
    return document.copy(
        content = newContent,
        lastModified = System.currentTimeMillis()
    )
}

// Usage example
fun main() {
    val original = Document(
        filename = "notes.md",
        content = "# Original",
        lastModified = 0L
    )

    val modified = modifyDocument(original, "# Updated\n\nNew content")

    println("Original: ${original.content}")
    println("Modified: ${modified.content}")
}
```

### Track Document Changes

```kotlin
import digital.vasic.yole.model.Document

fun trackChanges(document: Document) {
    println("Has changes: ${document.hasChanges()}")
    println("Last modified: ${document.lastModified}")

    // Simulate edit
    val edited = document.copy(
        content = document.content + "\n\nNew paragraph"
    )
    edited.touch()  // Mark as modified

    println("After edit - Has changes: ${edited.hasChanges()}")

    // Reset change tracking
    edited.resetChangeTracking()
    println("After reset - Has changes: ${edited.hasChanges()}")
}
```

### Get File Extension

```kotlin
import digital.vasic.yole.model.Document

fun getFileExtension(document: Document): String {
    return document.filename.substringAfterLast('.', "")
}

// Usage examples
fun main() {
    val doc1 = Document("notes.md", "", 0L)
    println(getFileExtension(doc1))  // "md"

    val doc2 = Document("todo.txt", "", 0L)
    println(getFileExtension(doc2))  // "txt"

    val doc3 = Document("document.name.tex", "", 0L)
    println(getFileExtension(doc3))  // "tex"

    val doc4 = Document("README", "", 0L)
    println(getFileExtension(doc4))  // ""
}
```

---

## Format Registry

### Get All Registered Formats

```kotlin
import digital.vasic.yole.format.FormatRegistry

fun listAllFormats() {
    val formats = FormatRegistry.getAllFormats()

    println("Registered formats (${formats.size}):")
    formats.forEach { format ->
        println("  - ${format.name}: ${format.extensions.joinToString()}")
    }
}

// Output:
// Registered formats (17):
//   - markdown: .md, .markdown, .mdown, .mkd
//   - todotxt: .txt
//   - csv: .csv
//   - latex: .tex, .latex
//   - ...
```

### Get Format by Name

```kotlin
import digital.vasic.yole.format.FormatRegistry

fun getFormatInfo(formatName: String) {
    val format = FormatRegistry.getFormat(formatName)

    if (format != null) {
        println("Format: ${format.name}")
        println("Display name: ${format.displayName}")
        println("Extensions: ${format.extensions.joinToString()}")
        println("MIME type: ${format.mimeType}")
    } else {
        println("Format not found: $formatName")
    }
}

// Usage examples
fun main() {
    getFormatInfo("markdown")
    getFormatInfo("todotxt")
    getFormatInfo("unknown")  // Format not found
}
```

### Get Format by Extension

```kotlin
import digital.vasic.yole.format.FormatRegistry

fun getFormatForFile(filename: String): String? {
    val extension = filename.substringAfterLast('.', "")
    val format = FormatRegistry.getFormatByExtension(".$extension")
    return format?.name
}

// Usage examples
fun main() {
    println(getFormatForFile("notes.md"))       // "markdown"
    println(getFormatForFile("data.csv"))       // "csv"
    println(getFormatForFile("config.ini"))     // "keyvalue"
    println(getFormatForFile("unknown.xyz"))    // null
}
```

### Check if Format is Supported

```kotlin
import digital.vasic.yole.format.FormatRegistry

fun isFormatSupported(extension: String): Boolean {
    return FormatRegistry.getFormatByExtension(extension) != null
}

// Usage examples
fun main() {
    println(isFormatSupported(".md"))        // true
    println(isFormatSupported(".txt"))       // true (but could be multiple formats)
    println(isFormatSupported(".docx"))      // false
}
```

---

## Platform-Specific Examples

### Android Example

```kotlin
// In your Android ViewModel or Activity

import android.content.Context
import digital.vasic.yole.model.Document
import digital.vasic.yole.format.markdown.MarkdownParser
import java.io.File

class DocumentViewModel(private val context: Context) {

    fun loadDocument(file: File): Document {
        val content = file.readText()
        return Document(
            filename = file.name,
            content = content,
            lastModified = file.lastModified()
        )
    }

    fun saveDocument(document: Document, file: File) {
        file.writeText(document.content)
    }

    fun parseAndDisplay(document: Document): String {
        val format = document.detectFormat()

        return when (format.name) {
            "markdown" -> {
                val parser = MarkdownParser()
                val result = parser.parse(document.content)
                result.html
            }
            "plaintext" -> {
                "<pre>${document.content}</pre>"
            }
            else -> {
                "Format not supported for preview: ${format.name}"
            }
        }
    }
}

// Usage in Activity
class EditorActivity : AppCompatActivity() {
    private lateinit var viewModel: DocumentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = DocumentViewModel(this)

        val file = File(filesDir, "notes.md")
        val document = viewModel.loadDocument(file)

        // Display in WebView
        val html = viewModel.parseAndDisplay(document)
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
```

### Desktop Example (Compose)

```kotlin
// In your Compose Desktop application

import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import digital.vasic.yole.model.Document
import digital.vasic.yole.format.markdown.MarkdownParser
import java.io.File

class DocumentState {
    var document by mutableStateOf<Document?>(null)
    var parsedHtml by mutableStateOf("")

    fun loadFile(file: File) {
        document = Document(
            filename = file.name,
            content = file.readText(),
            lastModified = file.lastModified()
        )
        parseDocument()
    }

    private fun parseDocument() {
        document?.let { doc ->
            val format = doc.detectFormat()
            if (format.name == "markdown") {
                val parser = MarkdownParser()
                val result = parser.parse(doc.content)
                parsedHtml = result.html
            }
        }
    }
}

fun main() = application {
    val state = remember { DocumentState() }

    Window(onCloseRequest = ::exitApplication, title = "Yole Editor") {
        // Your Compose UI here
        // Use state.document and state.parsedHtml
    }
}
```

---

## Advanced Usage

### Custom Format Detection

```kotlin
import digital.vasic.yole.model.Document
import digital.vasic.yole.format.FormatRegistry

fun detectWithFallback(content: String, filename: String): String {
    val document = Document(filename, content, 0L)
    var format = document.detectFormat()

    // If detected as plaintext, try content-based detection
    if (format.name == "plaintext" && content.isNotEmpty()) {
        // Custom heuristics
        format = when {
            content.startsWith("# ") || content.contains("\n## ") ->
                FormatRegistry.getFormat("markdown")!!
            content.contains("(A) ") || content.contains("+project") ->
                FormatRegistry.getFormat("todotxt")!!
            content.startsWith("* TODO") || content.startsWith("* DONE") ->
                FormatRegistry.getFormat("orgmode")!!
            else -> format
        }
    }

    return format.name
}
```

### Batch Process Files

```kotlin
import digital.vasic.yole.model.Document
import java.io.File

fun batchProcessMarkdownFiles(directory: File) {
    directory.walk()
        .filter { it.extension == "md" }
        .forEach { file ->
            val document = Document(
                filename = file.name,
                content = file.readText(),
                lastModified = file.lastModified()
            )

            // Process document
            println("Processing: ${file.name}")
            println("Format: ${document.detectFormat().name}")
            println("Lines: ${document.content.lines().size}")
            println("---")
        }
}

// Usage
fun main() {
    val dir = File("/path/to/notes")
    batchProcessMarkdownFiles(dir)
}
```

### Format Conversion

```kotlin
import digital.vasic.yole.format.markdown.MarkdownParser
import digital.vasic.yole.model.Document

fun convertMarkdownToHtml(markdownFile: File, outputFile: File) {
    val content = markdownFile.readText()
    val parser = MarkdownParser()
    val result = parser.parse(content)

    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>${markdownFile.nameWithoutExtension}</title>
            <style>
                body { font-family: sans-serif; max-width: 800px; margin: 50px auto; }
                code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; }
                pre { background: #f4f4f4; padding: 10px; border-radius: 5px; }
            </style>
        </head>
        <body>
            ${result.html}
        </body>
        </html>
    """.trimIndent()

    outputFile.writeText(html)
}

// Usage
fun main() {
    val input = File("notes.md")
    val output = File("notes.html")
    convertMarkdownToHtml(input, output)
    println("Converted ${input.name} to ${output.name}")
}
```

---

## Error Handling

### Safe File Loading

```kotlin
import digital.vasic.yole.model.Document
import java.io.File

fun loadDocumentSafely(file: File): Result<Document> {
    return runCatching {
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }
        require(file.isFile) { "Not a file: ${file.absolutePath}" }
        require(file.canRead()) { "Cannot read file: ${file.absolutePath}" }

        val content = file.readText()
        Document(
            filename = file.name,
            content = content,
            lastModified = file.lastModified()
        )
    }
}

// Usage
fun main() {
    val file = File("notes.md")
    loadDocumentSafely(file)
        .onSuccess { document ->
            println("Loaded: ${document.filename}")
            println("Format: ${document.detectFormat().name}")
        }
        .onFailure { error ->
            println("Error: ${error.message}")
        }
}
```

### Safe Parsing

```kotlin
import digital.vasic.yole.format.markdown.MarkdownParser

fun parseMarkdownSafely(content: String): Result<String> {
    return runCatching {
        val parser = MarkdownParser()
        val result = parser.parse(content)
        result.html
    }
}

// Usage
fun main() {
    val markdown = "# Title\n\nContent"
    parseMarkdownSafely(markdown)
        .onSuccess { html -> println("HTML: $html") }
        .onFailure { error -> println("Parse error: ${error.message}") }
}
```

### Handle Unknown Formats

```kotlin
import digital.vasic.yole.model.Document
import digital.vasic.yole.format.FormatRegistry

fun processWithFallback(document: Document) {
    val format = document.detectFormat()

    when (format.name) {
        "markdown", "todotxt", "csv", "latex", "orgmode" -> {
            println("Processing ${format.displayName} document")
            // Format-specific processing
        }
        "plaintext" -> {
            println("Processing as plain text")
            // Basic text processing
        }
        "binary" -> {
            println("Binary file detected, cannot process as text")
        }
        "unknown" -> {
            println("Unknown format, treating as plain text")
            // Fallback processing
        }
        else -> {
            println("Format ${format.name} not specifically handled")
        }
    }
}
```

---

## Next Steps

- **[Tutorial Projects](../tutorials/)** - Step-by-step tutorial projects
- **[Integration Examples](../integration/)** - Integrate Yole into your app
- **[Sample Files](../../samples/)** - Example files for all 17 formats
- **[API Documentation](../../docs/api/)** - Complete KDoc reference
- **[Contributing Guide](../../CONTRIBUTING.md)** - Contribute to Yole

---

## Related Resources

- **[Format Guides](../../docs/user-guide/formats/)** - Detailed format documentation
- **[Architecture Guide](../../ARCHITECTURE.md)** - System architecture
- **[Build System Guide](../../docs/BUILD_SYSTEM.md)** - Building Yole
- **[Testing Guide](../../docs/TESTING_GUIDE.md)** - Testing practices

---

**Examples Version**: 1.0
**Last Updated**: November 11, 2025
**Target API**: Yole 2.15.1
**Platform**: Kotlin Multiplatform (Android, Desktop, iOS, Web)
