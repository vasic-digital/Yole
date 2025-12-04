# Package digital.vasic.yole.model

Core document model for Yole text editor.

## Overview

This package contains the platform-agnostic document model used throughout Yole.
It represents text documents with associated metadata and format information.

## Key Components

### Document (`Document`)
The main document data class that represents a text file with:
- **path**: Absolute file path
- **title**: Document title (filename without extension)
- **extension**: File extension
- **format**: Format identifier (e.g., "markdown", "todotxt")
- **modTime**: Last modification timestamp
- **touchTime**: Last access timestamp

## Features

### Format Detection
```kotlin
val document = Document("/path/to/README.md", "README", "md")

// Detect by extension
document.detectFormatByExtension()
println(document.format) // "markdown"

// Detect by content
val content = "# Title\n\nMarkdown content"
document.detectFormatByContent(content)
println(document.format) // "markdown"
```

### Change Tracking
```kotlin
// Check if file changed externally
if (document.hasChanged()) {
    // Reload content
}

// Update access time
document.touch()

// Reset change tracking after save
document.resetChangeTracking()
```

### Format Integration
```kotlin
// Get the TextFormat object
val format = document.getTextFormat()
println(format.name) // "Markdown"
println(format.extensions) // [".md", ".markdown", ".mdown", ".mkd"]
```

## Platform Abstraction

This package uses Kotlin Multiplatform's expect/actual mechanism for platform-specific operations:

### Expected Functions
- `currentTimeMillis()` - Get current system time
- `Document.getFileModTime()` - Get file modification time
- `Document.getFileSize()` - Get file size in bytes
- `Document.fileExists()` - Check if file exists
- `createDocument(path)` - Create document from file path

These functions have platform-specific implementations in:
- `androidMain` - Android implementation
- `desktopMain` - JVM Desktop implementation
- `iosMain` - iOS implementation (when available)
- `wasmJsMain` - Web implementation (when available)

## Usage Examples

### Basic Document Creation
```kotlin
val document = Document(
    path = "/users/john/documents/notes.md",
    title = "notes",
    extension = "md",
    format = Document.FORMAT_MARKDOWN
)
```

### Document from File Path
```kotlin
// Platform-specific creation
val document = createDocument("/path/to/file.md")
document?.let {
    println("Title: ${it.title}")
    println("Format: ${it.format}")
    println("Size: ${it.getFileSize()} bytes")
}
```

### Change Detection Workflow
```kotlin
// Load document
val document = createDocument("/path/to/file.md")!!
document.resetChangeTracking()

// ... time passes ...

// Check for external changes
if (document.hasChanged()) {
    // File was modified externally
    val newContent = loadFileContent(document.path)
    // Update UI
}
```

## Serialization

The `Document` class is annotated with `@Serializable` and can be:
- Serialized to JSON for storage or transmission
- Stored in databases
- Passed between platform layers

Note: The `modTime` and `touchTime` fields are marked as `@Transient` and won't be serialized.

## Constants

Format constants are provided for convenience:
- `FORMAT_MARKDOWN` - "markdown"
- `FORMAT_TODOTXT` - "todotxt"
- `FORMAT_PLAINTEXT` - "plaintext"
- `FORMAT_CSV` - "csv"
- `FORMAT_LATEX` - "latex"
- And more... (see `Document.Companion`)

## See Also

- `digital.vasic.yole.format` - Format system and parsers
- Platform-specific implementations for file operations
