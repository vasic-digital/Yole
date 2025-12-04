# Format Documentation Guide

This document provides comprehensive documentation for all text formats supported by Yole across all platforms.

## Format Architecture Overview

### Platform-Specific Format Structure
Each format is implemented natively for each platform:
```
format-[name]/
├── src/main/java/digital/vasic/yole/format/[name]/  # Android implementation
│   ├── [Name]TextConverter.java      # HTML conversion
│   ├── [Name]SyntaxHighlighter.java  # Editor highlighting
│   └── [Name]ActionButtons.java      # Toolbar actions
├── src/test/java/                    # Unit tests
└── src/androidTest/java/             # Integration tests
```

Platform-specific implementations ensure optimal performance and native integration.

## Supported Formats

### 1. Markdown
**Module**: `format-markdown`

**Features**:
- Full CommonMark specification support
- GitHub Flavored Markdown extensions
- KaTeX math rendering
- Table support
- Code block syntax highlighting
- Auto-link detection
- Task lists

**File Extensions**: `.md`, `.markdown`

**Parser**: `MarkdownTextConverter.java`
```java
public class MarkdownTextConverter extends TextConverterBase {
    @Override
    public boolean isFileOutOfThisFormat(String filePath, String fileContent) {
        // Check if file is markdown
    }

    @Override
    public String convertMarkupToHtml(String markup) {
        // Convert markdown to HTML
    }
}
```

**Action Buttons**:
- Bold, Italic, Code formatting
- Headers (H1-H6)
- Links and images
- Lists (ordered/unordered)
- Code blocks
- Math expressions
- Tables

### 2. Todo.txt
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/todotxt/`
**Legacy Module**: `format-todotxt`

**Features**:
- Task management with completion tracking
- Priority levels (A-Z)
- Contexts (@context)
- Projects (+project)
- Due dates (due:YYYY-MM-DD)
- Creation dates (YYYY-MM-DD)
- Advanced search and filtering
- Archive functionality

**File Extensions**: `.todo.txt`, `.txt`

**KMP Parser**: `TodoTxtParser.kt`
```kotlin
class TodoTxtParser : TextParser {
    override fun parse(content: String): Document {
        // Parse todo.txt format
    }
    
    fun parseTask(line: String): TodoTask {
        // Parse individual task
    }
}
```

**Action Buttons**:
- Toggle task completion
- Set priority
- Add context/project
- Set due date
- Archive completed tasks

### 3. CSV
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/csv/`
**Legacy Module**: `format-csv`

**Features**:
- Table preview with HTML export
- Column-based syntax highlighting
- CSV parsing with quote handling
- Header detection
- Sorting capabilities
- Filter functionality

**File Extensions**: `.csv`

**KMP Parser**: `CsvParser.kt`
```kotlin
class CsvParser : TextParser {
    override fun parse(content: String): Document {
        // Parse CSV content
    }
    
    fun parseTable(content: String): CsvTable {
        // Parse CSV table structure
    }
}
```

**Action Buttons**:
- Add row/column
- Sort by column
- Filter rows
- Export to HTML/PDF

### 4. WikiText (Zim)
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/wikitext/`
**Legacy Module**: `format-wikitext`

**Features**:
- Zim wiki format support
- Link resolution and validation
- Transclusion support
- Attachment directory handling
- Notebook structure
- Backlink detection

**File Extensions**: `.txt`, `.wiki`

**KMP Parser**: `WikitextParser.kt`

**Action Buttons**:
- Wiki links
- Headers
- Formatting (bold, italic, etc.)
- Lists
- Attachments

### 5. Key-Value Formats
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/keyvalue/`
**Legacy Module**: `format-keyvalue`

**Supported Formats**:
- **JSON**: `.json` - JavaScript Object Notation
- **YAML**: `.yml`, `.yaml` - YAML Ain't Markup Language
- **TOML**: `.toml` - Tom's Obvious, Minimal Language
- **INI**: `.ini` - Configuration files
- **VCF**: `.vcf` - vCard contacts
- **ICS**: `.ics` - iCalendar events

**Features**:
- Syntax highlighting for all formats
- Structure validation
- Auto-completion suggestions
- Format conversion between types

**KMP Parser**: `KeyValueParser.kt`

### 6. AsciiDoc
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/asciidoc/`
**Legacy Module**: `format-asciidoc`

**Features**:
- Technical documentation format
- Document structure parsing
- Cross-references
- Attributes and variables
- Table of contents generation
- PDF export capability

**File Extensions**: `.adoc`, `.asciidoc`

**KMP Parser**: `AsciidocParser.kt`

**Action Buttons**:
- Document title
- Section headers
- Lists
- Tables
- Cross-references
- Attributes

### 7. Org-mode
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/orgmode/`
**Legacy Module**: `format-orgmode`

**Features**:
- Emacs org-mode compatibility
- Heading hierarchy
- TODO states
- Timestamps and scheduling
- Tags and properties
- Table editing
- Source code blocks

**File Extensions**: `.org`

**KMP Parser**: `OrgModeParser.kt`

**Action Buttons**:
- TODO state cycling
- Timestamp insertion
- Tags
- Source blocks
- Tables

### 8. LaTeX
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/latex/`
**Legacy Module**: `format-latex`

**Features**:
- LaTeX command highlighting
- Math expression parsing
- Document structure detection
- Environment support
- Bibliography handling
- Cross-references

**File Extensions**: `.tex`, `.latex`

**KMP Parser**: `LatexParser.kt`

**Action Buttons**:
- Math delimiters
- Common commands
- Environments
- Sections
- References

### 9. reStructuredText
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/restructuredtext/`
**Legacy Module**: `format-restructuredtext`

**Features**:
- reStructuredText specification
- Directive support
- Role support
- Field lists
- Option lists
- Table of contents
- Cross-references

**File Extensions**: `.rst`, `.rest`

**KMP Parser**: `RestructuredTextParser.kt`

**Action Buttons**:
- Section headers
- Directives
- Roles
- Tables
- Links

### 10. TaskPaper
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/taskpaper/`
**Legacy Module**: `format-taskpaper`

**Features**:
- Project-based task organization
- Tag support
- Note attachments
- Search and filtering
- Archive functionality

**File Extensions**: `.taskpaper`

**KMP Parser**: `TaskpaperParser.kt`

**Action Buttons**:
- Create project
- Add task
- Add tag
- Archive completed

### 11. Textile
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/textile/`
**Legacy Module**: `format-textile`

**Features**:
- Textile markup language
- Table support
- Image handling
- Link formatting
- Text formatting

**File Extensions**: `.textile`

**KMP Parser**: `TextileParser.kt`

**Action Buttons**:
- Text formatting
- Links
- Images
- Tables

### 12. Creole
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/creole/`
**Legacy Module**: `format-creole`

**Features**:
- Creole wiki markup standard
- Cross-wiki compatibility
- Link handling
- Table support
- Image embedding

**File Extensions**: `.creole`

**KMP Parser**: `CreoleParser.kt`

**Action Buttons**:
- Wiki links
- Headers
- Formatting
- Tables

### 13. TiddlyWiki
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/tiddlywiki/`
**Legacy Module**: `format-tiddlywiki`

**Features**:
- Personal wiki format
- Tiddler structure
- Tagging system
- Field support
- Transclusion
- Macros

**File Extensions**: `.tid`, `.tiddler`

**KMP Parser**: `TiddlyWikiParser.kt`

**Action Buttons**:
- Create tiddler
- Add tags
- Fields
- Links

### 14. Jupyter
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/jupyter/`
**Legacy Module**: `format-jupyter`

**Features**:
- Jupyter notebook format
- Cell-based structure
- Code cell highlighting
- Markdown cell support
- Output handling
- Metadata support

**File Extensions**: `.ipynb`

**KMP Parser**: `JupyterParser.kt`

**Action Buttons**:
- Add code cell
- Add markdown cell
- Cell types

### 15. R Markdown
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/rmarkdown/`
**Legacy Module**: `format-rmarkdown`

**Features**:
- R code integration
- Markdown with R chunks
- Code execution
- Output formatting
- Bibliography support
- Cross-references

**File Extensions**: `.Rmd`, `.rmd`

**KMP Parser**: `RMarkdownParser.kt`

**Action Buttons**:
- Code chunk
- Inline code
- Knit options

### 16. Plaintext
**KMP Implementation**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/plaintext/`
**Legacy Module**: `format-plaintext`

**Features**:
- Basic text editing
- Language detection
- Syntax highlighting for code
- Line numbering
- Search and replace

**File Extensions**: `.txt`, `.log`, `.conf`

**KMP Parser**: `PlaintextParser.kt`

## Adding New Formats

### Platform-Specific Format Implementation

1. **Create Module**: `format-newformat/`
2. **Implement Required Classes**:
    - `NewformatTextConverter.java` - HTML conversion
    - `NewformatSyntaxHighlighter.java` - Editor highlighting
    - `NewformatActionButtons.java` - Toolbar actions
3. **Add to Registry**: Update platform-specific `FormatRegistry`
4. **Add Tests**: Unit and integration tests
5. **Update Documentation**: Add to this guide

### Cross-Platform Considerations
- Implement for each target platform (Android, iOS, Desktop, Web)
- Ensure consistent behavior across platforms
- Handle platform-specific file system differences
- Test on all target platforms

## Format Testing Strategy

### Platform Testing
- **Unit Tests**: Parser logic and conversion
- **UI Tests**: Action buttons and highlighting
- **Integration Tests**: Format conversion and rendering
- **Cross-Platform Tests**: Consistency across platforms

## Performance Considerations

### Large File Handling
- Streaming parsers for large files
- Lazy loading of syntax highlighting
- Background processing for conversions

### Memory Management
- Efficient data structures
- Garbage collection optimization
- Resource cleanup

## Future Format Support

### Planned Formats
- **Notion**: `.notion` - Notion database export
- **Obsidian**: `.md` with Obsidian-specific syntax
- **Logseq**: `.md` with Logseq blocks
- **Roam**: `.json` - Roam Research export
- **Bear**: `.md` with Bear-specific features

### Plugin System
- Dynamic format loading
- Third-party format support
- Format marketplace

## Documentation Maintenance

### Updating Format Docs
1. Update this document when adding new formats
2. Update parser documentation in code
3. Update action button documentation
4. Update sample files

### Version Compatibility
- Document format version changes
- Migration guides for format updates
- Backward compatibility notes