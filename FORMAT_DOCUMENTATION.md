# Format Documentation Guide

**Version**: 2.0
**Date**: 2026-03-17
**Formats**: 17

This document provides comprehensive documentation for all text formats supported by Yole's Kotlin Multiplatform shared module.

---

## Format Architecture Overview

### KMP Shared Module Structure

All format parsers live in the shared KMP module at `shared/src/commonMain/kotlin/digital/vasic/yole/format/`. Each format is implemented once and runs on all platforms (Android, Desktop, iOS, Web/Wasm).

```
shared/src/commonMain/kotlin/digital/vasic/yole/format/
├── FormatRegistry.kt      # Central registry: lazy-loaded, detection priority order
├── TextFormat.kt          # Format metadata (id, name, extensions, detectionPatterns)
├── TextParser.kt          # ParsedDocument class with lazy HTML caching
├── ParserInitializer.kt   # Lazy and eager parser registration
├── DocumentCache.kt       # LRU cache for ParsedDocument with hit/miss tracking
├── StyleSheets.kt         # CSS generation with styleSheetCache
├── markdown/              # MarkdownParser.kt
├── todotxt/               # TodoTxtParser.kt
├── csv/                   # CsvParser.kt
├── latex/                 # LatexParser.kt
├── asciidoc/              # AsciidocParser.kt
├── orgmode/               # OrgModeParser.kt
├── wikitext/              # WikitextParser.kt
├── restructuredtext/      # RestructuredTextParser.kt
├── taskpaper/             # TaskpaperParser.kt
├── textile/               # TextileParser.kt
├── creole/                # CreoleParser.kt
├── tiddlywiki/            # TiddlyWikiParser.kt
├── jupyter/               # JupyterParser.kt
├── rmarkdown/             # RMarkdownParser.kt
├── plaintext/             # PlaintextParser.kt
└── keyvalue/              # KeyValueParser.kt
```

### Text Parsing Pipeline

1. **Detection** -- `FormatRegistry.detectByExtension()` (O(1) map lookup) or `detectByContent()` (regex patterns in priority order)
2. **Parsing** -- Format-specific parser produces a `ParsedDocument` (raw content + parsed content + metadata + errors)
3. **HTML Generation** -- `ParsedDocument.toHtml(lightMode)` with lazy caching (first call generates, subsequent calls return cached)
4. **Styling** -- `StyleSheets.kt` generates CSS for light/dark themes with `styleSheetCache`

### Code Example: Parsing a Document

```kotlin
// Register all parsers lazily (done once at app startup)
ParserInitializer.registerAllParsersLazy()

// Detect format by file extension
val format = FormatRegistry.detectByExtension("notes.md")
// -> TextFormat(id = "markdown", ...)

// Get parser and parse content
val parser = ParserRegistry.getParser(format!!)
val doc = parser!!.parse("# Hello\n\nThis is **bold** text.")

// Access parsed content
println(doc.rawContent)      // "# Hello\n\nThis is **bold** text."
println(doc.parsedContent)   // Parsed representation
println(doc.metadata)        // {"title": "Hello", ...}
println(doc.errors)          // [] (empty if valid)
```

### Code Example: HTML Generation

```kotlin
// Generate HTML (lazy: first call computes, subsequent calls return cached)
val htmlLight = doc.toHtml(lightMode = true)
val htmlDark = doc.toHtml(lightMode = false)

// Check cache state
println(doc.hasHtmlCached(lightMode = true))   // true
println(doc.hasHtmlCached(lightMode = false))   // true

// Free memory if needed
doc.clearHtmlCache()
```

### Code Example: DocumentCache with Hit/Miss Tracking

```kotlin
val cache = DocumentCache(maxSize = 100)

// Parse with caching
val doc = FormatRegistry.parseWithCache(content, format, cache)

// Monitor cache effectiveness
println("Cache size: ${cache.size}")
println("Hit rate: ${cache.hitRate}")
println("Hits: ${cache.hits}, Misses: ${cache.misses}")
```

---

## Supported Formats

### 1. Markdown

**Parser**: `markdown/MarkdownParser.kt`
**Format ID**: `TextFormat.ID_MARKDOWN`
**Extensions**: `.md`, `.markdown`, `.mdown`, `.mkd`
**MIME Types**: `text/markdown`, `text/x-markdown`

**Capabilities**:
- Full CommonMark specification support
- GitHub Flavored Markdown (GFM) extensions
- Tables with alignment
- Task lists with interactive checkboxes
- Code blocks with syntax highlighting (50+ languages)
- KaTeX math rendering (inline and block)
- Footnotes, abbreviations, definition lists
- YAML front matter parsing
- Table of contents generation
- Mermaid diagram rendering
- Emoji shortcodes
- Auto-link detection

**Parsing Example**:
```kotlin
val parser = MarkdownParser()
val doc = parser.parse("""
    # Project Plan

    ## Tasks
    - [x] Setup repository
    - [ ] Write documentation

    | Feature | Status |
    |---------|--------|
    | Parsing | Done   |
""".trimIndent())

val html = doc.toHtml(lightMode = true)
// Produces full HTML with table, checkboxes, headings
```

**Dependencies**: Flexmark 0.64.8 with 16+ extensions

---

### 2. Todo.txt

**Parser**: `todotxt/TodoTxtParser.kt`
**Format ID**: `TextFormat.ID_TODOTXT`
**Extensions**: `.txt` (detected by content pattern)
**MIME Types**: `text/plain`

**Capabilities**:
- Task management with completion tracking
- Priority levels (A-Z)
- Contexts (@context)
- Projects (+project)
- Due dates (due:YYYY-MM-DD)
- Creation dates
- Advanced query syntax with boolean operators
- Archive functionality
- Sorting and filtering

**Parsing Example**:
```kotlin
val parser = TodoTxtParser()
val doc = parser.parse("""
    (A) Call dentist @phone +health due:2026-04-01
    x 2026-03-15 2026-03-10 Submit report @work +quarterly
    (B) Buy groceries @errands
""".trimIndent())
```

**Note**: Input guard limits regex processing to 10K characters to prevent backtracking on very long lines.

---

### 3. CSV

**Parser**: `csv/CsvParser.kt`
**Format ID**: `TextFormat.ID_CSV`
**Extensions**: `.csv`
**MIME Types**: `text/csv`, `application/csv`

**Capabilities**:
- Table preview with HTML table rendering
- Column-based syntax highlighting
- CSV parsing with quote handling
- Automatic header row detection
- Sorting and filtering
- Markdown rendering within cells

**Parsing Example**:
```kotlin
val parser = CsvParser()
val doc = parser.parse("""
    Name,Age,City
    Alice,30,New York
    Bob,25,San Francisco
""".trimIndent())

val html = doc.toHtml(lightMode = true)
// Produces an HTML table with headers and rows
```

---

### 4. WikiText (Zim/MediaWiki)

**Parser**: `wikitext/WikitextParser.kt`
**Format ID**: `TextFormat.ID_WIKITEXT`
**Extensions**: `.wiki`, `.wikitext`, `.mediawiki`
**MIME Types**: `text/x-wiki`

**Capabilities**:
- MediaWiki/Zim wiki format support
- Heading syntax (`== Heading ==`)
- Link resolution and validation
- Bold/italic formatting
- Lists (ordered and unordered)
- Table support
- Transclusion support
- Backlink detection

---

### 5. Key-Value Formats

**Parser**: `keyvalue/KeyValueParser.kt`
**Format ID**: `TextFormat.ID_KEYVALUE`
**Extensions**: `.properties`, `.ini`, `.env`, `.conf`, `.config`, `.cfg`
**MIME Types**: `text/x-java-properties`, `text/x-ini`

**Capabilities**:
- Java properties format (`key=value`)
- INI format with sections (`[section]`)
- Environment variable files (`KEY=value`)
- Comment styles: `#`, `;`, `//`
- Structure validation
- Syntax highlighting for all sub-formats
- TOML basic support
- YAML basic support

**Parsing Example**:
```kotlin
val parser = KeyValueParser()
val doc = parser.parse("""
    [database]
    host=localhost
    port=5432
    # Connection pool
    max_connections=10
""".trimIndent())
```

---

### 6. AsciiDoc

**Parser**: `asciidoc/AsciidocParser.kt`
**Format ID**: `TextFormat.ID_ASCIIDOC`
**Extensions**: `.adoc`, `.asciidoc`, `.asc`
**MIME Types**: `text/asciidoc`

**Capabilities**:
- Technical documentation format
- Document structure parsing (sections, chapters)
- Admonition blocks (NOTE, TIP, WARNING, CAUTION, IMPORTANT)
- Code blocks with syntax highlighting
- Cross-references and anchors
- Attributes and variables
- Table of contents generation
- Table support
- Include directives

---

### 7. Org Mode

**Parser**: `orgmode/OrgModeParser.kt`
**Format ID**: `TextFormat.ID_ORGMODE`
**Extensions**: `.org`
**MIME Types**: `text/x-org`

**Capabilities**:
- Emacs org-mode compatibility
- Heading hierarchy (`*`, `**`, `***`)
- TODO state tracking (TODO, DONE, custom keywords)
- Tags (`:tag1:tag2:`)
- Properties drawers (`:PROPERTIES:`)
- Timestamps (`<2026-03-17>`)
- Scheduling (SCHEDULED, DEADLINE)
- Org-mode tables
- Source code blocks (`#+BEGIN_SRC`)
- Links (`[[link][description]]`)

---

### 8. LaTeX

**Parser**: `latex/LatexParser.kt`
**Format ID**: `TextFormat.ID_LATEX`
**Extensions**: `.tex`, `.latex`
**MIME Types**: `application/x-latex`, `text/x-latex`

**Capabilities**:
- LaTeX command highlighting
- Math expression parsing (inline `$...$` and display `$$...$$`)
- Document structure detection (sections, chapters, parts)
- Environment support (`\begin{...}` / `\end{...}`)
- Bibliography reference handling
- Cross-references (`\ref`, `\label`)
- Command auto-detection

---

### 9. reStructuredText

**Parser**: `restructuredtext/RestructuredTextParser.kt`
**Format ID**: `TextFormat.ID_RESTRUCTUREDTEXT`
**Extensions**: `.rst`, `.rest`, `.restx`, `.rtxt`
**MIME Types**: `text/x-rst`, `text/prs.fallenstein.rst`

**Capabilities**:
- reStructuredText specification support
- Section headers with underline characters
- Directive support (code-block, image, note, warning)
- Role support (`:ref:`, `:doc:`, `:math:`)
- Field lists and option lists
- Table of contents generation
- Cross-references
- Grid and simple table formats

---

### 10. TaskPaper

**Parser**: `taskpaper/TaskpaperParser.kt`
**Format ID**: `TextFormat.ID_TASKPAPER`
**Extensions**: `.taskpaper`, `.todo`
**MIME Types**: `text/x-taskpaper`

**Capabilities**:
- Project-based task organization (lines ending with `:`)
- Task items (lines starting with `- `)
- Tag support (`@tag`, `@tag(value)`)
- Note attachments
- Search and filtering
- Done tag tracking (`@done`)
- Archive functionality

---

### 11. Textile

**Parser**: `textile/TextileParser.kt`
**Format ID**: `TextFormat.ID_TEXTILE`
**Extensions**: `.textile`, `.txtl`
**MIME Types**: `text/x-textile`

**Capabilities**:
- Textile markup language support
- Text formatting (`*bold*`, `_italic_`, `-strikethrough-`)
- Headings (`h1.`, `h2.`, etc.)
- Block quotes (`bq.`)
- Code blocks (`bc.`)
- Table support (`|cell|cell|`)
- Image embedding (`!url!`)
- Link formatting (`"text":url`)

---

### 12. Creole

**Parser**: `creole/CreoleParser.kt`
**Format ID**: `TextFormat.ID_CREOLE`
**Extensions**: `.creole`, `.wiki`
**MIME Types**: `text/x-creole`

**Capabilities**:
- Creole wiki markup standard
- Bold (`**bold**`) and italic (`//italic//`)
- Headings (`= H1`, `== H2`, etc.)
- Lists (unordered `*`, ordered `#`)
- Links (`[[link|text]]`)
- Images (`{{image.png|alt}}`)
- Table support (`|cell|cell|`)
- Horizontal rules (`----`)
- Nowiki/preformatted blocks (`{{{ ... }}}`)

---

### 13. TiddlyWiki

**Parser**: `tiddlywiki/TiddlyWikiParser.kt`
**Format ID**: `TextFormat.ID_TIDDLYWIKI`
**Extensions**: `.tid`, `.tiddler`
**MIME Types**: `text/x-tiddlywiki`

**Capabilities**:
- Personal wiki format (tiddler structure)
- Metadata fields (created, modified, tags, type)
- Tagging system
- Bold, italic, underline, strikethrough
- Headings (`! H1`, `!! H2`, etc.)
- Lists (ordered and unordered)
- Transclusion
- Macros

---

### 14. Jupyter

**Parser**: `jupyter/JupyterParser.kt`
**Format ID**: `TextFormat.ID_JUPYTER`
**Extensions**: `.ipynb`
**MIME Types**: `application/x-ipynb+json`

**Capabilities**:
- Jupyter notebook JSON format parsing
- Cell-based structure (code, markdown, raw)
- Code cell output handling (text, images, HTML)
- Markdown cell rendering
- Cell metadata parsing
- Notebook metadata extraction (kernel, language)
- Source handling (array and string formats)
- Error output display

**Parsing Example**:
```kotlin
val parser = JupyterParser()
val doc = parser.parse("""
    {
      "cells": [
        {
          "cell_type": "markdown",
          "source": ["# Analysis\n", "Results below."]
        },
        {
          "cell_type": "code",
          "source": ["import pandas as pd\n", "df = pd.read_csv('data.csv')"],
          "outputs": [{"output_type": "stream", "text": ["   Name  Age\n0  Alice   30"]}]
        }
      ],
      "metadata": {"kernelspec": {"language": "python"}}
    }
""".trimIndent())
```

---

### 15. R Markdown

**Parser**: `rmarkdown/RMarkdownParser.kt`
**Format ID**: `TextFormat.ID_RMARKDOWN`
**Extensions**: `.Rmd`, `.rmd`, `.rmarkdown`
**MIME Types**: `text/x-r-markdown`

**Capabilities**:
- R code chunk integration (` ```{r} ... ``` `)
- YAML front matter parsing
- Markdown rendering between code chunks
- Inline R expressions (`` `r expression` ``)
- Output format specification
- Bibliography support
- Cross-references

---

### 16. Plain Text

**Parser**: `plaintext/PlaintextParser.kt`
**Format ID**: `TextFormat.ID_PLAINTEXT`
**Extensions**: `.txt`, `.text`, `.log`
**MIME Types**: `text/plain`

**Capabilities**:
- Universal text format (fallback)
- Language detection for code files
- Line numbering
- Search and replace
- Minimal processing overhead

---

### 17. Binary Detection

**Format ID**: `TextFormat.ID_BINARY`
**Extensions**: All binary file types
**MIME Types**: Various binary types

**Capabilities**:
- Detects binary files by magic numbers (PDF: `%PDF-`, PNG: `89504E47`, etc.)
- Prevents editing of non-text files
- Displays file type information
- No parsing or HTML generation

---

## Format Detection

### Detection Priority

1. **User Override** -- Manual format selection by the user
2. **File Extension** -- Primary detection via `FormatRegistry.detectByExtension()` (O(1) map lookup)
3. **Content Analysis** -- Fallback via `FormatRegistry.detectByContent()` (regex patterns in priority order)
4. **Default** -- Plain text format

### Content Detection Patterns

| Format | Detection Pattern |
|--------|-------------------|
| Markdown | Lines starting with `#` |
| Todo.txt | Priority pattern `(A)` at start of line |
| WikiText | `== Heading ==` syntax |
| Org Mode | Lines starting with `*` followed by space |
| TiddlyWiki | Metadata fields: `created:`, `modified:`, `tags:` |
| Jupyter | JSON with `"cells"` array |
| R Markdown | YAML front matter + ` ```{r}` code chunks |
| Binary | File header magic numbers |

---

## Adding New Formats

1. Create parser directory in `shared/src/commonMain/kotlin/digital/vasic/yole/format/[name]/`
2. Implement parser class that extends `TextParser` and produces `ParsedDocument`
3. Add `TextFormat` entry to `FormatRegistry.formats` list (order matters -- more specific formats before general ones)
4. Add format ID constant to `TextFormat.Companion`
5. Register in `ParserInitializer.kt` (both eager and lazy paths)
6. Add tests in `shared/src/commonTest/kotlin/digital/vasic/yole/format/[name]/`
7. Add platform-specific code in `androidMain/`, `desktopMain/`, etc. if needed
8. Update `FORMAT_SUPPORT_MATRIX.md`

---

## Performance Considerations

### Lazy Parser Registration

Parsers are registered lazily via `ParserInitializer.registerAllParsersLazy()`, which stores factory lambdas instead of parser instances. Each parser is instantiated on first access, saving 30-50ms at startup.

### Lazy HTML Generation

`ParsedDocument.toHtml()` generates HTML only on first call and caches the result. Separate caches exist for light and dark modes.

### DocumentCache

An LRU cache (`DocumentCache`) stores recently parsed documents with hit/miss tracking. Use `FormatRegistry.parseWithCache()` for automatic caching.

### Large File Handling

- TodoTxtParser has a 10K character guard to prevent regex backtracking
- DocumentCache uses cooperative cancellation (`yield()`) for long operations
- Streaming is used for large file operations

---

**Document Version**: 2.0
**Last Updated**: 2026-03-17
**Maintained By**: Engineering Team
