# Module 5: Custom Format Development (10 videos)

## Video 5.1: Format Architecture Deep Dive (20 min)

### Timestamps
- 0:00 Yole's format system overview
- 2:00 `TextParser` interface: `parse()`, `toHtml()`, `canParse()` methods
- 4:00 `ParsedDocument` class: raw content, parsed content, metadata, errors
- 6:00 Lazy HTML caching in `ParsedDocument.toHtml()`
- 8:00 `FormatRegistry` internals: thread-safe initialization
- 10:00 Format metadata in `TextFormat`: id, name, extensions, detection patterns
- 12:00 Format ID constants on `TextFormat.Companion`
- 14:00 Registration order and detection priority
- 16:00 The parsing pipeline: detect, parse, generate HTML, style
- 18:00 `StyleSheets.kt`: CSS generation for light/dark themes
- 19:30 Summary

### Code Example: UI Theme System

```kotlin
// StyleSheets.kt generates CSS for each theme mode
object StyleSheets {
    fun generateCss(darkMode: Boolean): String {
        val bg = if (darkMode) "#1e1e1e" else "#ffffff"
        val fg = if (darkMode) "#d4d4d4" else "#1e1e1e"
        return """
            body { background: $bg; color: $fg; font-family: system-ui; }
            code { background: ${if (darkMode) "#2d2d2d" else "#f5f5f5"}; }
            /* ... format-specific styles ... */
        """.trimIndent()
    }
}
```

---

## Video 5.2: Advanced Parsing Techniques (18 min)

### Timestamps
- 0:00 Parsing strategy comparison
- 2:00 Recursive descent parsing for nested structures
- 4:00 Regex-based parsing for line-oriented formats
- 6:00 Handling nested LaTeX environments (`\begin{...}...\end{...}`)
- 8:00 Handling Org Mode heading hierarchies
- 10:00 Single-pass vs. multi-pass parsing tradeoffs
- 12:00 Performance: lazy evaluation and incremental parsing
- 14:00 Error recovery in parsers
- 16:00 Building the parse tree vs. direct HTML generation
- 17:30 Summary

### Code Example: Format-Specific Parsing

```kotlin
// Each parser produces a ParsedDocument with structured output
val parser = TodoTxtParser()
val document = parser.parse("(A) 2025-01-15 Call mom +Family @phone due:2025-02-01")
// document.rawContent -> original text
// document.parsedContent -> structured parsed output
// document.metadata -> format-specific metadata (task count, projects, contexts)
// document.toHtml() -> HTML representation with lazy caching
```

---

## Video 5.3: Error Handling and Recovery (15 min)

### Timestamps
- 0:00 Why parsers must handle malformed input gracefully
- 2:00 Error recovery strategies: skip, insert, replace
- 4:00 Graceful degradation: show raw text when parsing fails
- 6:00 Error collection in ParsedDocument
- 8:00 User-facing error messages: informative but non-technical
- 10:00 Testing error recovery with malformed inputs
- 12:00 Edge cases: binary content, huge files, empty files
- 14:00 Summary

### Code Example: Error Recovery

```kotlin
// ParsedDocument tracks errors without crashing
val result = parser.parse(malformedInput)
if (result.errors.isNotEmpty()) {
    // Show errors in UI but still display parseable content
    result.errors.forEach { error ->
        println("Parse warning at line ${error.line}: ${error.message}")
    }
}
// Partial results are always available
val html = result.toHtml() // Returns best-effort HTML
```

---

## Video 5.4: Syntax Highlighting Engine (20 min)

### Timestamps
- 0:00 Token-based highlighting overview
- 3:00 Tokenizer design: splitting source into typed tokens
- 6:00 Token types: keyword, string, comment, operator, etc.
- 9:00 TextMate grammar compatibility considerations
- 12:00 Theme system: light/dark mode with `Theme.kt`
- 15:00 Custom color definitions and overrides
- 18:00 Performance: incremental re-highlighting on edits
- 19:30 Summary

### Code Example: Theme Token Architecture

```kotlin
// Theme.kt defines the token-based theme system
object YoleTheme {
    val lightColors = ThemeColors(
        background = Color(0xFFFFFFFF),
        text = Color(0xFF1E1E1E),
        primary = Color(0xFF2563EB),
        // ...
    )
    val darkColors = ThemeColors(
        background = Color(0xFF1E1E1E),
        text = Color(0xFFD4D4D4),
        primary = Color(0xFF60A5FA),
        // ...
    )
}
```

---

## Videos 5.5-5.10: Building Specific Parsers

### Video 5.5: Build a YAML Parser from Scratch (15 min)

#### Timestamps
- 0:00 YAML format overview and use cases
- 2:00 Key-value parsing and indentation-based nesting
- 5:00 List and map structures
- 8:00 Multi-line strings (block scalars)
- 11:00 Integrating with FormatRegistry
- 14:00 Summary

### Video 5.6: Build a TOML Parser (15 min)

#### Timestamps
- 0:00 TOML format overview
- 2:00 Table and array-of-tables syntax
- 5:00 Type system: strings, integers, floats, booleans, dates
- 8:00 Inline tables and arrays
- 11:00 Validation and error reporting
- 14:00 Summary

### Video 5.7: Build a Mermaid Diagram Parser (15 min)

#### Timestamps
- 0:00 Mermaid diagram types: flowchart, sequence, class
- 2:00 Graph definition syntax
- 5:00 Node and edge parsing
- 8:00 Rendering to SVG or HTML
- 11:00 Integration with Markdown code blocks
- 14:00 Summary

### Video 5.8: Build a Graphviz DOT Parser (15 min)

#### Timestamps
- 0:00 DOT language overview
- 2:00 Graph, digraph, and subgraph parsing
- 5:00 Node attributes and edge definitions
- 8:00 Layout algorithms overview
- 11:00 HTML output generation
- 14:00 Summary

### Video 5.9: Build a Custom DSL Parser (15 min)

#### Timestamps
- 0:00 When to build a custom DSL
- 2:00 Defining grammar rules
- 5:00 Implementing a recursive descent parser
- 8:00 AST construction and traversal
- 11:00 HTML code generation from AST
- 14:00 Summary

### Video 5.10: Parser Testing Patterns and Benchmarks (15 min)

#### Timestamps
- 0:00 Testing strategy for new parsers
- 2:00 Unit tests: one test per grammar rule
- 5:00 Integration tests: full document parsing
- 8:00 Benchmarking: measuring parse time and memory
- 11:00 Regression tests: protecting against future breaks
- 14:00 Summary
