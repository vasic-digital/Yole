# Package digital.vasic.yole.format

Core format system for Yole text editor.

## Overview

This package contains the complete text format parsing system for Yole, including:
- Format registry and detection
- Parser interface and registry
- 17+ format-specific parsers
- Parsing utilities and configuration

## Key Components

### Format Registry (`FormatRegistry`)
Central registry for all supported text formats. Provides:
- Format lookup by ID, extension, or filename
- Content-based format detection
- Format enumeration and querying

### Text Format (`TextFormat`)
Data class representing a text format with:
- Unique identifier
- Human-readable name
- File extensions
- Content detection patterns

### Text Parser Interface (`TextParser`)
Common interface for all format parsers:
- `parse()` - Convert markup to structured document
- `toHtml()` - Generate HTML representation
- `validate()` - Syntax validation

### Parser Registry (`ParserRegistry`)
Registry for all available parsers:
- Parser registration
- Parser lookup by format
- Parser enumeration

### Parser Initializer (`ParserInitializer`)
Convenience object for registering all parsers during application initialization.

## Supported Formats

### Core Formats
- **Markdown** - CommonMark + GitHub Flavored Markdown
- **Plain Text** - Simple text with optional syntax highlighting
- **Todo.txt** - Task management format (http://todotxt.org/)
- **CSV** - Comma-separated values

### Wiki Formats
- **WikiText** - MediaWiki markup
- **Org Mode** - Emacs Org mode
- **Creole** - Wiki Creole markup
- **TiddlyWiki** - TiddlyWiki markup

### Technical Formats
- **LaTeX** - TeX typesetting language
- **AsciiDoc** - Documentation format
- **reStructuredText** - Python documentation format

### Specialized Formats
- **Key-Value** - Properties/INI files
- **TaskPaper** - Task management format
- **Textile** - Textile markup

### Data Science Formats
- **Jupyter** - Jupyter Notebook (.ipynb)
- **R Markdown** - R Markdown documents

### Binary Format
- **Binary** - Binary file detection and handling

## Usage Examples

### Basic Parsing

```kotlin
// Initialize all parsers
ParserInitializer.registerAllParsers()

// Get a format
val format = FormatRegistry.getById("markdown")

// Get the parser for that format
val parser = ParserRegistry.getParser(format!!)

// Parse content
val content = "# Hello World\n\nThis is **markdown**."
val document = parser?.parse(content)

// Convert to HTML
val html = parser?.toHtml(document!!)
```

### Format Detection

```kotlin
// Detect by extension
val format = FormatRegistry.detectByExtension(".md")
println(format.name) // "Markdown"

// Detect by filename
val format2 = FormatRegistry.detectByFilename("README.md")
println(format2.name) // "Markdown"

// Detect by content
val content = "# This looks like markdown"
val format3 = FormatRegistry.detectByContent(content)
println(format3?.name) // "Markdown"
```

### Parse Options

```kotlin
val options = ParseOptions.create()
    .enableLineNumbers(true)
    .enableHighlighting(true)
    .setBaseUrl("https://example.com")
    .build()

val parser = MarkdownParser()
val document = parser.parse(content, options)
```

## Architecture

The format system uses a plugin-like architecture where:
1. Each format is defined as a `TextFormat` data class
2. Each parser implements the `TextParser` interface
3. The `FormatRegistry` maintains all format metadata
4. The `ParserRegistry` maintains all parser instances
5. Format detection is done through file extensions and content patterns

This design allows easy addition of new formats by:
1. Creating a new `TextFormat` instance
2. Implementing a `TextParser` for the format
3. Registering both with their respective registries

## Thread Safety

- `FormatRegistry` is an immutable singleton (thread-safe)
- `ParserRegistry` uses a synchronized list for thread-safe registration
- Individual parsers should be stateless and thread-safe

## Performance Considerations

- Format detection by extension is O(n) where n is the number of formats
- Content detection is O(n*m) where m is the number of patterns per format
- Parsers should handle large files efficiently (streaming when possible)

## Extension Points

To add a new format:

1. Define the format:
```kotlin
val myFormat = TextFormat(
    id = "myformat",
    name = "My Format",
    defaultExtension = ".myf",
    extensions = listOf(".myf", ".myformat"),
    detectionPatterns = listOf("^@myformat", "^%MY")
)
```

2. Implement the parser:
```kotlin
class MyFormatParser : TextParser {
    override val supportedFormat = myFormat

    override fun parse(content: String, options: Map<String, Any>): ParsedDocument {
        // Parse implementation
    }
}
```

3. Register:
```kotlin
// Add to FormatRegistry.formats list
// Register parser: ParserRegistry.register(MyFormatParser())
```

## See Also

- `digital.vasic.yole.model` - Document model
- Platform-specific implementations in androidMain, desktopMain, etc.
