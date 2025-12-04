# Yole Integration Examples

Examples for integrating Yole into existing applications.

**Audience**: Developers adding Yole to their apps
**Platforms**: Android, Desktop, iOS, Web
**Last Updated**: November 11, 2025

---

## Integration Scenarios

### 1. Add Markdown Preview to Existing App

**Use Case**: Add Markdown rendering to your note-taking app

**Dependencies**:
```kotlin
dependencies {
    implementation("digital.vasic.yole:shared:2.15.1")
}
```

**Code**:
```kotlin
import digital.vasic.yole.format.markdown.MarkdownParser

class MarkdownPreviewHelper {
    private val parser = MarkdownParser()

    fun renderMarkdown(markdown: String): String {
        val result = parser.parse(markdown)
        return wrapInHtml(result.html)
    }

    private fun wrapInHtml(content: String) = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { font-family: sans-serif; padding: 20px; }
                code { background: #f4f4f4; padding: 2px 6px; }
                pre { background: #f4f4f4; padding: 10px; overflow-x: auto; }
            </style>
        </head>
        <body>$content</body>
        </html>
    """.trimIndent()
}

// Usage in Android
class NoteViewerActivity : AppCompatActivity() {
    private val markdownHelper = MarkdownPreviewHelper()

    fun showMarkdown(markdown: String) {
        val html = markdownHelper.renderMarkdown(markdown)
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
```

---

### 2. Add Format Detection to File Manager

**Use Case**: Show appropriate icons and handle files based on format

**Code**:
```kotlin
import digital.vasic.yole.model.Document

class FileTypeDetector {
    fun getFileType(filename: String, content: String): FileType {
        val document = Document(filename, content, 0L)
        val format = document.detectFormat()

        return when (format.name) {
            "markdown" -> FileType.MARKDOWN
            "todotxt" -> FileType.TODO
            "csv" -> FileType.DATA
            "binary" -> FileType.BINARY
            else -> FileType.TEXT
        }
    }

    fun getIconResource(fileType: FileType): Int {
        return when (fileType) {
            FileType.MARKDOWN -> R.drawable.ic_markdown
            FileType.TODO -> R.drawable.ic_checklist
            FileType.DATA -> R.drawable.ic_table
            FileType.BINARY -> R.drawable.ic_file
            FileType.TEXT -> R.drawable.ic_text
        }
    }
}

enum class FileType {
    MARKDOWN, TODO, DATA, BINARY, TEXT
}
```

---

### 3. Add Todo.txt to Project Management App

**Use Case**: Import/export tasks using Todo.txt format

**Code**:
```kotlin
import digital.vasic.yole.format.todotxt.TodoTxtParser

class TaskImporter {
    fun importFromTodoTxt(content: String): List<ProjectTask> {
        val parser = TodoTxtParser()
        val result = parser.parse(content)
        val tasks = result.metadata["tasks"] as? List<Task> ?: emptyList()

        return tasks.map { task ->
            ProjectTask(
                title = task.description,
                priority = parsePriority(task.priority),
                projects = task.projects,
                tags = task.contexts,
                completed = task.completed,
                dueDate = task.dueDate
            )
        }
    }

    fun exportToTodoTxt(tasks: List<ProjectTask>): String {
        return tasks.joinToString("\n") { task ->
            buildString {
                if (task.completed) append("x ")
                task.priority?.let { append("($it) ") }
                append(task.title)
                task.projects.forEach { append(" +$it") }
                task.tags.forEach { append(" @$it") }
                task.dueDate?.let { append(" due:$it") }
            }
        }
    }
}
```

---

### 4. Add CSV Export to Data App

**Use Case**: Export data to CSV format

**Code**:
```kotlin
import digital.vasic.yole.format.csv.CsvParser

class DataExporter {
    fun exportToCsv(
        headers: List<String>,
        rows: List<List<String>>
    ): String {
        val csv = StringBuilder()

        // Add headers
        csv.appendLine(headers.joinToString(",") { escapeValue(it) })

        // Add rows
        rows.forEach { row ->
            csv.appendLine(row.joinToString(",") { escapeValue(it) })
        }

        return csv.toString()
    }

    private fun escapeValue(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\"
        } else {
            value
        }
    }

    fun parseCsv(content: String): Pair<List<String>, List<List<String>>> {
        val parser = CsvParser()
        val result = parser.parse(content)

        val headers = result.metadata["headers"] as? List<String> ?: emptyList()
        val rows = result.metadata["rows"] as? List<List<String>> ?: emptyList()

        return Pair(headers, rows)
    }
}
```

---

### 5. Add Multi-Format Support to Editor

**Use Case**: Support editing multiple text formats in one app

**Code**:
```kotlin
import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.model.Document

class MultiFormatEditor {
    fun openFile(file: File): EditorState {
        val content = file.readText()
        val document = Document(file.name, content, file.lastModified())
        val format = document.detectFormat()

        return EditorState(
            document = document,
            format = format,
            syntaxHighlighting = getSyntaxHighlighter(format.name),
            actions = getFormatActions(format.name)
        )
    }

    private fun getSyntaxHighlighter(formatName: String): SyntaxHighlighter {
        return when (formatName) {
            "markdown" -> MarkdownHighlighter()
            "todotxt" -> TodoTxtHighlighter()
            "csv" -> CsvHighlighter()
            else -> PlainTextHighlighter()
        }
    }

    private fun getFormatActions(formatName: String): List<EditorAction> {
        return when (formatName) {
            "markdown" -> listOf(
                EditorAction("Bold", "**", "**"),
                EditorAction("Italic", "*", "*"),
                EditorAction("Link", "[", "](url)")
            )
            "todotxt" -> listOf(
                EditorAction("Priority", "(A) ", ""),
                EditorAction("Project", "+", ""),
                EditorAction("Context", "@", "")
            )
            else -> emptyList()
        }
    }
}
```

---

## Integration Patterns

### Pattern 1: Drop-in Component

Add Yole as a self-contained component:
```kotlin
// Create a wrapper class
class YoleTextProcessor {
    fun processText(content: String, filename: String): ProcessedText {
        // Use Yole internally
    }
}

// Use in your app
val processor = YoleTextProcessor()
val result = processor.processText(content, "file.md")
```

### Pattern 2: Service Integration

Integrate Yole as a service:
```kotlin
interface TextProcessingService {
    fun detectFormat(filename: String, content: String): String
    fun parseToHtml(content: String, format: String): String
}

class YoleTextProcessingService : TextProcessingService {
    // Implement using Yole
}
```

### Pattern 3: Plugin Architecture

Use Yole as a plugin:
```kotlin
interface FormatPlugin {
    fun supportedExtensions(): List<String>
    fun parse(content: String): String
}

class YoleFormatPlugin : FormatPlugin {
    // Use Yole for all format support
}
```

---

## Platform-Specific Integration

### Android

**Add to existing Activity**:
```kotlin
class MainActivity : AppCompatActivity() {
    private val yoleHelper = YoleIntegration()

    fun handleFile(file: File) {
        val result = yoleHelper.processFile(file)
        showResult(result)
    }
}
```

### Desktop (Compose)

**Add to Compose app**:
```kotlin
@Composable
fun DocumentViewer(file: File) {
    val yoleHelper = remember { YoleIntegration() }
    val content by remember { mutableStateOf(yoleHelper.loadAndParse(file)) }

    // Display content
}
```

### iOS (SwiftUI + KMP)

**Bridge Kotlin to Swift**:
```swift
import shared

class DocumentProcessor {
    let yoleHelper = YoleIntegration()

    func processFile(_ url: URL) -> String {
        // Use Yole KMP shared code
        return yoleHelper.parse(content: content, filename: url.lastPathComponent)
    }
}
```

---

## Best Practices

### 1. Initialize Once

```kotlin
// Good: Singleton or dependency injection
object YoleManager {
    val formatRegistry = FormatRegistry
}

// Bad: Creating new instances repeatedly
fun process(content: String) {
    val registry = FormatRegistry()  // Don't do this
}
```

### 2. Handle Errors Gracefully

```kotlin
fun safelyParseDocument(content: String): Result<String> {
    return runCatching {
        val document = Document("file.md", content, 0L)
        val format = document.detectFormat()
        // ... parsing logic
    }
}
```

### 3. Cache Parsed Results

```kotlin
class DocumentCache {
    private val cache = mutableMapOf<String, ParsedDocument>()

    fun getOrParse(key: String, content: String): ParsedDocument {
        return cache.getOrPut(key) {
            parseDocument(content)
        }
    }
}
```

### 4. Use Coroutines for Large Files

```kotlin
suspend fun parseAsync(content: String): String = withContext(Dispatchers.IO) {
    val parser = MarkdownParser()
    parser.parse(content).html
}
```

---

## Troubleshooting

### Issue: Format not detected correctly

**Solution**: Provide explicit format hint
```kotlin
val format = FormatRegistry.getFormatByExtension(".md")
```

### Issue: Memory usage high with large files

**Solution**: Stream processing or chunking
```kotlin
fun processLargeFile(file: File) {
    file.useLines { lines ->
        lines.chunked(1000).forEach { chunk ->
            processChunk(chunk)
        }
    }
}
```

### Issue: Platform-specific behavior

**Solution**: Use expect/actual pattern
```kotlin
expect fun getPlatformSpecificParser(): TextParser

// Implement per platform
actual fun getPlatformSpecificParser(): TextParser = AndroidMarkdownParser()
```

---

## Related Resources

- **[API Usage](../api-usage/)** - Complete API examples
- **[Tutorials](../tutorials/)** - Step-by-step guides
- **[Samples](../../samples/)** - Sample files
- **[API Docs](../../docs/api/)** - KDoc reference
- **[Architecture](../../ARCHITECTURE.md)** - System design

---

**Integration Guide Version**: 1.0
**Last Updated**: November 11, 2025
**Integration Patterns**: 5
**Platform Coverage**: Android, Desktop, iOS, Web
