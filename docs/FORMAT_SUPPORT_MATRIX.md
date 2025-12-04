# Format Support Matrix

Complete feature matrix showing which capabilities are supported for each format across all platforms.

**Last Updated**: November 11, 2025
**Total Formats**: 17

---

## Platform Support Legend

| Symbol | Meaning |
|--------|---------|
| ✓ | Fully supported |
| ◐ | Partially supported / Basic |
| = | In development |
| L | Not supported |
| = | Android only |
| =◐ | Desktop only |
| <○ | Web only |

---

## Format Support by Platform

| Format | Android | Desktop | iOS | Web | Extensions | Documentation |
|--------|---------|---------|-----|-----|-----------|---------------|
| **Markdown** | ✓ | ✓ | = | = | `.md`, `.markdown`, `.mdown`, `.mkd` | [Guide](user-guide/formats/markdown.md) |
| **Plain Text** | ✓ | ✓ | = | = | `.txt`, `.text`, `.log` | [Guide](user-guide/formats/plaintext.md) |
| **Todo.txt** | ✓ | ✓ | = | = | `.txt` (todo format) | [Guide](user-guide/formats/todotxt.md) |
| **CSV** | ✓ | ✓ | = | = | `.csv` | [Guide](user-guide/formats/csv.md) |
| **LaTeX** | ✓ | ◐ | = | = | `.tex`, `.latex` | [Guide](user-guide/formats/latex.md) |
| **Org Mode** | ✓ | ◐ | = | = | `.org` | [Guide](user-guide/formats/orgmode.md) |
| **WikiText** | ✓ | ◐ | = | = | `.wiki`, `.wikitext`, `.mediawiki` | [Guide](user-guide/formats/wikitext.md) |
| **AsciiDoc** | ✓ | ◐ | = | = | `.adoc`, `.asciidoc`, `.asc` | [Guide](user-guide/formats/asciidoc.md) |
| **reStructuredText** | ✓ | ◐ | = | = | `.rst`, `.rest`, `.restx`, `.rtxt` | [Guide](user-guide/formats/restructuredtext.md) |
| **Key-Value** | ✓ | ✓ | = | = | `.properties`, `.ini`, `.env`, `.conf` | [Guide](user-guide/formats/keyvalue.md) |
| **TaskPaper** | ✓ | ◐ | = | = | `.taskpaper`, `.todo` | [Guide](user-guide/formats/taskpaper.md) |
| **Textile** | ✓ | ◐ | = | = | `.textile`, `.txtl` | [Guide](user-guide/formats/textile.md) |
| **Creole** | ✓ | ◐ | = | = | `.creole`, `.wiki` | [Guide](user-guide/formats/creole.md) |
| **TiddlyWiki** | ✓ | ◐ | = | = | `.tid`, `.tiddler` | [Guide](user-guide/formats/tiddlywiki.md) |
| **Jupyter** | ✓ | ◐ | = | = | `.ipynb` | [Guide](user-guide/formats/jupyter.md) |
| **R Markdown** | ✓ | ◐ | = | = | `.Rmd`, `.rmarkdown` | [Guide](user-guide/formats/rmarkdown.md) |
| **Binary Detection** | ✓ | ✓ | = | = | All binary files | [Guide](user-guide/formats/binary.md) |

---

## Feature Support by Format

### Core Features

| Format | Syntax Highlighting | Preview/HTML | PDF Export | Format Detection | Action Buttons |
|--------|---------------------|--------------|------------|------------------|----------------|
| **Markdown** | ✓ Full | ✓ Full | ✓ | ✓ Extension + Content | ✓ Rich |
| **Plain Text** | ✓ Code Detection | ◐ Basic | ✓ | ✓ Extension | ◐ Basic |
| **Todo.txt** | ✓ Full | ✓ Custom UI | ✓ | ✓ Content Analysis | ✓ Rich |
| **CSV** | ✓ Column Colors | ✓ Table View | ✓ | ✓ Extension | ◐ Basic |
| **LaTeX** | ✓ Full | ◐ External | ◐ External | ✓ Extension | ✓ Rich |
| **Org Mode** | ✓ Full | ◐ Basic | ✓ | ✓ Extension | ✓ Rich |
| **WikiText** | ✓ Full | ◐ Basic | ✓ | ✓ Extension + Content | ◐ Basic |
| **AsciiDoc** | ✓ Full | ◐ Basic | ✓ | ✓ Extension | ◐ Basic |
| **reStructuredText** | ✓ Full | ◐ Basic | ✓ | ✓ Extension | ◐ Basic |
| **Key-Value** | ✓ Full | ✓ Structured | ✓ | ✓ Extension | ◐ Basic |
| **TaskPaper** | ✓ Full | ◐ Basic | ✓ | ✓ Extension | ◐ Basic |
| **Textile** | ✓ Full | ◐ Basic | ✓ | ✓ Extension | ◐ Basic |
| **Creole** | ✓ Full | ◐ Basic | ✓ | ✓ Extension | ◐ Basic |
| **TiddlyWiki** | ✓ Full | ◐ Basic | ✓ | ✓ Extension + Content | ◐ Basic |
| **Jupyter** | ✓ JSON | ✓ JSON View | ✓ | ✓ Extension + Content | ◐ Basic |
| **R Markdown** | ✓ Full | ◐ Basic | ✓ | ✓ Extension + Content | ◐ Basic |
| **Binary Detection** | L | L | L | ✓ Magic Numbers | L |

---

## Advanced Features by Format

### Markdown

| Feature | Support | Notes |
|---------|---------|-------|
| CommonMark | ✓ Full | Complete specification |
| GitHub Flavored Markdown | ✓ Full | Tables, task lists, strikethrough |
| Task Lists | ✓ | Interactive checkboxes |
| Tables | ✓ | Full support with alignment |
| Code Blocks | ✓ | Syntax highlighting for 50+ languages |
| Math (KaTeX) | ✓ | Inline and block equations |
| Footnotes | ✓ | Standard markdown footnotes |
| YAML Front Matter | ✓ | Jekyll-compatible |
| TOC Generation | ✓ | Automatic table of contents |
| Mermaid Diagrams | ✓ | Flowcharts, sequence diagrams |
| Emoji | ✓ | :emoji: syntax |

### Todo.txt

| Feature | Support | Notes |
|---------|---------|-------|
| Priority | ✓ | (A-Z) priority levels |
| Projects | ✓ | +project tags |
| Contexts | ✓ | @context tags |
| Due Dates | ✓ | due:YYYY-MM-DD |
| Completion Dates | ✓ | Automatic tracking |
| Advanced Search | ✓ | Query language with operators |
| Task Archiving | ✓ | Move to done.txt |
| Filtering | ✓ | By priority, project, context |
| Sorting | ✓ | Multiple sort options |

### CSV

| Feature | Support | Notes |
|---------|---------|-------|
| Column Colors | ✓ | Unique color per column |
| Table Preview | ✓ | HTML table rendering |
| Header Detection | ✓ | Automatic header row |
| Markdown in Cells | ✓ | Basic markdown rendering |
| Custom Delimiters | ◐ | Comma default, others partial |
| Excel Export | ◐ | Via PDF |

### LaTeX

| Feature | Support | Notes |
|---------|---------|-------|
| Syntax Highlighting | ✓ | Full LaTeX commands |
| Math Equations | ✓ | Inline and display mode |
| Document Structure | ✓ | Sections, chapters, parts |
| Bibliography | ◐ | BibTeX syntax only |
| PDF Compilation | ◐ | External tool required |
| Live Preview | L | Planned |

### Org Mode

| Feature | Support | Notes |
|---------|---------|-------|
| Headlines | ✓ | * ** *** structure |
| TODO Keywords | ✓ | TODO, DONE, custom |
| Tags | ✓ | :tag1:tag2: |
| Properties | ✓ | :PROPERTIES: drawer |
| Timestamps | ✓ | <2025-11-11> |
| Scheduling | ✓ | SCHEDULED, DEADLINE |
| Tables | ✓ | Org-mode tables |
| Code Blocks | ✓ | #+BEGIN_SRC |
| Links | ✓ | [[link][description]] |
| Agenda View | L | Planned |

### Key-Value Formats

| Feature | Support | Notes |
|---------|---------|-------|
| Java Properties | ✓ | key=value |
| INI Files | ✓ | [sections] with keys |
| ENV Files | ✓ | Environment variables |
| TOML | ◐ | Basic support |
| YAML | ◐ | Basic support |
| JSON | ◐ | Basic support |
| Comments | ✓ | #, ; and // |

---

## Format Detection Methods

### By Extension

All formats support detection by file extension. Multiple extensions are supported per format.

### By Content Analysis

| Format | Detection Pattern | Example |
|--------|-------------------|---------|
| **Markdown** | `# Heading` or `## Heading` | Lines starting with # |
| **Todo.txt** | Priority patterns | `(A) Task description` |
| **WikiText** | `== Heading ==` | MediaWiki heading syntax |
| **Org Mode** | `* Heading` | Lines starting with * |
| **TiddlyWiki** | Metadata fields | `created:`, `modified:`, `tags:` |
| **Jupyter** | JSON structure | `{"cells": [...]` |
| **R Markdown** | YAML + code chunks | `---` + ` ```{r}` |
| **Binary** | Magic numbers | File headers (PDF: %PDF-, PNG: 89504E47) |

### Automatic Detection Priority

1. **User Override**: Manual format selection
2. **File Extension**: Primary detection method
3. **Content Analysis**: Fallback for ambiguous extensions
4. **Default**: Plain text format

---

## Platform-Specific Notes

### Android (Production)

- **Status**: ✓ Fully functional
- **All Formats**: Full support with syntax highlighting, preview, and PDF export
- **Performance**: Optimized for mobile devices
- **File System**: Full access to internal storage and SD cards
- **Permissions**: READ/WRITE_EXTERNAL_STORAGE

### Desktop (Beta - 30% Complete)

- **Status**: ◐ Beta quality
- **Supported Formats**: Markdown, Plain Text, Todo.txt, CSV, Key-Value, Binary Detection (full support)
- **Partial Support**: Other formats have basic parsing but limited UI features
- **Missing Features**: Some action buttons, advanced preview options
- **Platforms**: Windows, macOS, Linux (via Compose Desktop)
- **File System**: Native file system access

### iOS (In Development)

- **Status**: = Disabled due to compilation issues
- **Target**: Q2 2026
- **Planned**: SwiftUI + KMP shared code
- **Formats**: All 17 formats planned
- **Note**: Requires resolution of compilation issues in `shared/build.gradle.kts:41`

### Web (Stub - 0% Complete)

- **Status**: = Build configuration only
- **Target**: Q3 2026
- **Technology**: Kotlin/Wasm + Compose for Web
- **Formats**: All 17 formats planned
- **Storage**: Browser local storage + File System Access API

---

## Testing Coverage by Format

| Format | Unit Tests | Integration Tests | Coverage | Status |
|--------|-----------|-------------------|----------|--------|
| **Markdown** | ✓ 50+ | ✓ 10+ | 95%+ | Excellent |
| **Todo.txt** | ✓ 150+ | ✓ 20+ | 95%+ | Excellent |
| **CSV** | ✓ 30+ | ✓ 5+ | 90%+ | Excellent |
| **Plain Text** | ✓ 20+ | ✓ 5+ | 95%+ | Excellent |
| **LaTeX** | ✓ 40+ | ✓ 5+ | 85%+ | Good |
| **Org Mode** | ✓ 30+ | ✓ 5+ | 85%+ | Good |
| **Key-Value** | ✓ 40+ | ✓ 5+ | 90%+ | Excellent |
| **Other Formats** | ✓ 20+ each | ✓ 3+ each | 75-85% | Good |
| **Overall** | 852+ tests | 100+ tests | 93% | Excellent |

**Test Pass Rate**: 100% (all tests passing)

---

## Future Format Support

### Planned Formats (Future)

- **Fountain**: Screenplay format
- **MultiMarkdown**: Enhanced Markdown with additional features
- **Org-roam**: Zettelkasten for Org Mode
- **Dendron**: Note-taking with hierarchies
- **Obsidian-flavored**: Obsidian-specific features

### Requested Formats

See [GitHub Issues](https://github.com/vasic-digital/Yole/issues?q=is%3Aissue+label%3Aformat) for community requests.

---

## Adding Custom Formats

Developers can add new format support. See:
- **[Contributing Guide](../CONTRIBUTING.md)** - How to contribute
- **[Architecture Guide](../ARCHITECTURE.md)** - System design
- **[Build System Guide](BUILD_SYSTEM.md)** - Build configuration

### Format Implementation Checklist

- [ ] Create format parser in `shared/src/commonMain/kotlin/digital/vasic/yole/format/[name]/`
- [ ] Implement `TextParser` interface
- [ ] Register in `FormatRegistry`
- [ ] Add format detection logic
- [ ] Implement syntax highlighting (platform-specific)
- [ ] Add preview/HTML conversion (platform-specific)
- [ ] Create action buttons (platform-specific)
- [ ] Write comprehensive unit tests (50+ tests recommended)
- [ ] Write integration tests (5+ tests recommended)
- [ ] Create user documentation guide (400+ lines)
- [ ] Update format support matrix
- [ ] Test on all platforms

---

## Related Documentation

- **[Format Guides Index](user-guide/formats/)** - Complete guides for all 17 formats
- **[Getting Started](user-guide/getting-started.md)** - Quick start guide
- **[FAQ](user-guide/faq.md)** - Frequently asked questions
- **[Architecture Guide](../ARCHITECTURE.md)** - System architecture
- **[API Documentation](api/)** - KDoc API reference

---

## Version History

| Date | Version | Changes |
|------|---------|---------|
| 2025-11-11 | 1.0 | Initial format support matrix with all 17 formats |

---

*Last updated: November 11, 2025*
*Formats supported: 17*
*Test coverage: 93% (852+ tests)*
*Documentation: 13,200+ lines*
