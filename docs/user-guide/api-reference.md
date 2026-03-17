# Yole API Reference

**Version**: 1.0
**Date**: 2026-03-17

This document provides a reference for Yole's public APIs in the shared KMP module. For full KDoc documentation, run `./gradlew :shared:dokkaHtml`.

---

## FormatRegistry API

**Location**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`

The central registry for all supported text formats. Lazy-loaded on first access.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `formats` | `List<TextFormat>` | All registered formats (lazy-loaded via `lazy { createFormats() }`) |
| `isFormatsInitialized` | `Boolean` | Whether the lazy format list has been initialized |

### Functions

| Function | Return Type | Description |
|----------|-------------|-------------|
| `detectByExtension(filename: String)` | `TextFormat?` | Detect format by file extension (O(1) map lookup) |
| `detectByContent(content: String)` | `TextFormat?` | Detect format by content analysis (regex patterns in priority order) |
| `detectByFilename(filename: String)` | `TextFormat?` | Detect format by filename pattern |
| `parseWithCache(content: String, format: TextFormat, cache: DocumentCache)` | `ParsedDocument` | Parse with automatic DocumentCache integration |

### Usage

```kotlin
// Initialize (done once at app startup)
ParserInitializer.registerAllParsersLazy()

// Detect format
val format = FormatRegistry.detectByExtension("document.md")
// Returns TextFormat(id = "markdown", ...)

// Content-based detection (fallback)
val format2 = FormatRegistry.detectByContent("# Hello World")

// Parse with caching
val cache = DocumentCache(maxSize = 100)
val doc = FormatRegistry.parseWithCache(content, format!!, cache)
```

---

## TextParser API

**Location**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt`

Interface implemented by all format parsers.

### TextParser Interface

| Function | Return Type | Description |
|----------|-------------|-------------|
| `parse(content: String)` | `ParsedDocument` | Parse raw content into a structured document |
| `toHtml(document: ParsedDocument, lightMode: Boolean)` | `String` | Convert parsed document to HTML |
| `validate(content: String)` | `List<String>` | Validate content and return error messages |

### ParsedDocument Class

| Property | Type | Description |
|----------|------|-------------|
| `format` | `TextFormat` | The format of this document |
| `rawContent` | `String` | Original unparsed content |
| `parsedContent` | `String` | Parsed representation |
| `metadata` | `Map<String, Any>` | Extracted metadata (title, author, etc.) |
| `errors` | `List<ParseError>` | Parsing errors encountered |

| Function | Return Type | Description |
|----------|-------------|-------------|
| `toHtml(lightMode: Boolean = true)` | `String` | Generate HTML (lazy, cached per light/dark mode) |
| `clearHtmlCache()` | `Unit` | Clear cached HTML to free memory |
| `hasHtmlCached(lightMode: Boolean = true)` | `Boolean` | Check if HTML is cached for the given mode |

### ParserRegistry

| Function | Return Type | Description |
|----------|-------------|-------------|
| `register(parser: TextParser)` | `Unit` | Eagerly register a parser |
| `registerLazy(formatId: String, factory: () -> TextParser)` | `Unit` | Lazily register a parser factory |
| `getParser(format: TextFormat)` | `TextParser?` | Get parser (triggers lazy instantiation if needed) |
| `getParser(formatId: String)` | `TextParser?` | Get parser by format ID |
| `hasParser(format: TextFormat)` | `Boolean` | Check without triggering instantiation |
| `getAllParsers()` | `List<TextParser>` | Get all instantiated parsers |
| `getPendingParserCount()` | `Int` | Count of parsers not yet instantiated |
| `getInstantiatedParserCount()` | `Int` | Count of parsers already instantiated |
| `clear()` | `Unit` | Remove all registered parsers |

### Usage

```kotlin
// Get a parser
val parser = ParserRegistry.getParser("markdown")

// Parse content
val doc = parser!!.parse("# Hello\n\nThis is **bold**.")

// Generate HTML
val html = doc.toHtml(lightMode = true)

// Check metadata
val title = doc.metadata["title"] as? String

// Validate
val errors = parser.validate("some content")
if (errors.isNotEmpty()) {
    println("Validation errors: $errors")
}
```

---

## NetworkStorageService API

**Location**: `shared/src/commonMain/kotlin/digital/vasic/yole/network/NetworkStorageService.kt`

Unified interface implemented by all 8 protocol services.

### Interface

| Function | Return Type | Description |
|----------|-------------|-------------|
| `connect(config: StorageConfig)` | `Result<Unit>` | Connect to the storage service |
| `disconnect()` | `Result<Unit>` | Disconnect and clean up resources |
| `isConnected()` | `Boolean` | Check connection state (thread-safe via `stateMutex`) |
| `listFiles(remotePath: String)` | `Result<List<NetworkDocument>>` | List files in a directory |
| `downloadFile(remotePath: String, localPath: String)` | `Flow<NetworkOperation>` | Download a file with progress |
| `uploadFile(localPath: String, remotePath: String)` | `Flow<NetworkOperation>` | Upload a file with progress |
| `deleteFile(remotePath: String)` | `Result<Unit>` | Delete a file |
| `getFileInfo(remotePath: String)` | `Result<NetworkDocument>` | Get file metadata |
| `searchFiles(query: String, remotePath: String)` | `Result<List<NetworkDocument>>` | Search for files |
| `syncAll()` | `Flow<NetworkOperation>` | Synchronize all files |
| `getRecentChanges(limit: Int)` | `Flow<List<NetworkDocument>>` | Get recently modified files |

### Protocol Implementations

| Service | Protocol | Authentication |
|---------|----------|---------------|
| `DropboxService` | Dropbox API | OAuth2 |
| `GoogleDriveService` | Google Drive API | OAuth2 |
| `OneDriveService` | Microsoft Graph API | OAuth2 |
| `WebDavService` | WebDAV | Username/Password |
| `FtpService` | FTP | Username/Password |
| `SftpService` | SFTP | Username/Password or SSH Key |
| `SmbService` | SMB/CIFS | Domain/Username/Password |
| `GitService` | Git | SSH Key or HTTPS |

### Common Resilience Features (All 8 Services)

| Component | Purpose | Default |
|-----------|---------|---------|
| `CircuitBreaker` | Prevent cascading failures | 5 failures, 30s reset |
| `ConnectionLimiter` | Cap concurrent connections | 5 max, 30s timeout |
| `normalizePath()` | Path traversal protection | Always applied |
| `CancellationException` rethrow | Coroutine cancellation safety | Always enforced |
| `serviceScope` | Structured concurrency lifecycle | Cancelled on disconnect |

### Usage

```kotlin
// Create and connect
val service = DropboxService(
    authTokenManager = authManager,
    secureStorage = platformSecureStorage
)
val connectResult = service.connect(StorageConfig(
    type = "dropbox",
    appKey = "your-app-key",
    appSecret = "your-app-secret"
))

// List files
val files = service.listFiles("/documents")
files.onSuccess { docs ->
    docs.forEach { println("${it.name} (${it.size} bytes)") }
}

// Download with progress
service.downloadFile("/documents/report.md", "/local/report.md")
    .collect { operation ->
        println("Progress: ${operation.progress}%")
    }

// Disconnect
service.disconnect()
```

---

## DocumentCache API

**Location**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/DocumentCache.kt`

LRU cache for parsed documents with hit/miss tracking.

### Constructor

```kotlin
DocumentCache(maxSize: Int = 100)
```

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `size` | `Int` | Current number of cached entries |
| `hits` | `Long` | Total cache hits |
| `misses` | `Long` | Total cache misses |
| `hitRate` | `Double` | Hit ratio (0.0 to 1.0) |

### Functions

| Function | Return Type | Description |
|----------|-------------|-------------|
| `get(key: String)` | `ParsedDocument?` | Get cached document (null if not cached) |
| `put(key: String, document: ParsedDocument)` | `Unit` | Cache a document (may trigger LRU eviction) |
| `getOrPut(key: String, factory: () -> ParsedDocument)` | `ParsedDocument` | Get or compute and cache |
| `remove(key: String)` | `ParsedDocument?` | Remove specific entry |
| `clear()` | `Unit` | Remove all entries |
| `contains(key: String)` | `Boolean` | Check if key is cached |

### Usage

```kotlin
val cache = DocumentCache(maxSize = 100)

// Cache a parsed document
val doc = parser.parse(content)
cache.put("file.md", doc)

// Retrieve from cache
val cached = cache.get("file.md") // Returns cached, hits++
val missing = cache.get("other.md") // Returns null, misses++

// Monitor effectiveness
println("Hit rate: ${cache.hitRate}") // e.g., 0.85
```

---

## StyleSheets API

**Location**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt`

CSS generation for format-specific styling with caching.

### Functions

| Function | Return Type | Description |
|----------|-------------|-------------|
| `getStyleSheet(formatId: String, lightMode: Boolean)` | `String` | Get CSS for a format/theme combination (cached) |
| `clearCache()` | `Unit` | Clear all cached style sheets |

### Usage

```kotlin
// Get CSS for Markdown in light mode
val css = StyleSheets.getStyleSheet("markdown", lightMode = true)

// Clear cache on theme change
StyleSheets.clearCache()
```

---

## TextFormat Class

**Location**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextFormat.kt`

Metadata describing a text format.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `id` | `String` | Unique format identifier (e.g., `"markdown"`) |
| `name` | `String` | Display name (e.g., `"Markdown"`) |
| `extensions` | `List<String>` | Supported file extensions (e.g., `[".md", ".markdown"]`) |
| `detectionPatterns` | `List<Regex>` | Content detection patterns |
| `mimeTypes` | `List<String>` | Associated MIME types |

### Format ID Constants

Available on `TextFormat.Companion`:

| Constant | Value |
|----------|-------|
| `ID_MARKDOWN` | `"markdown"` |
| `ID_TODOTXT` | `"todotxt"` |
| `ID_CSV` | `"csv"` |
| `ID_PLAINTEXT` | `"plaintext"` |
| `ID_LATEX` | `"latex"` |
| `ID_ORGMODE` | `"orgmode"` |
| `ID_WIKITEXT` | `"wikitext"` |
| `ID_ASCIIDOC` | `"asciidoc"` |
| `ID_RESTRUCTUREDTEXT` | `"restructuredtext"` |
| `ID_KEYVALUE` | `"keyvalue"` |
| `ID_TASKPAPER` | `"taskpaper"` |
| `ID_TEXTILE` | `"textile"` |
| `ID_CREOLE` | `"creole"` |
| `ID_TIDDLYWIKI` | `"tiddlywiki"` |
| `ID_JUPYTER` | `"jupyter"` |
| `ID_RMARKDOWN` | `"rmarkdown"` |
| `ID_BINARY` | `"binary"` |

---

## Related Documentation

- [Format Documentation](../../FORMAT_DOCUMENTATION.md) -- Detailed format parser documentation
- [Architecture Guide](../../ARCHITECTURE.md) -- System design overview
- [Concurrency Safety](../CONCURRENCY_SAFETY.md) -- Thread safety patterns
- [Performance Tuning](../PERFORMANCE_TUNING.md) -- Optimization guide

---

**Last Updated**: 2026-03-17
