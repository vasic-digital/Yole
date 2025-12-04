# Yole Examples and Code Samples

Complete collection of examples, tutorials, and integration guides for Yole.

**Audience**: All developers using Yole
**Last Updated**: November 11, 2025
**Version**: 1.0

---

## Quick Navigation

| What do you want to do? | Go to |
|--------------------------|-------|
| **Learn the APIs** | [API Usage Examples](#api-usage-examples) |
| **See sample files** | [Sample Files](#sample-files) |
| **Follow a tutorial** | [Tutorials](#tutorials) |
| **Integrate into my app** | [Integration Examples](#integration-examples) |
| **Get started quickly** | [Quick Start](#quick-start) |

---

## Quick Start

### 1. Add Yole to Your Project

**Gradle (Kotlin DSL)**:
```kotlin
dependencies {
    implementation("digital.vasic.yole:shared:2.15.1")
}
```

### 2. Detect File Format

```kotlin
import digital.vasic.yole.model.Document

val document = Document("notes.md", content, System.currentTimeMillis())
val format = document.detectFormat()
println("Format: ${format.name}")  // "markdown"
```

### 3. Parse and Render

```kotlin
import digital.vasic.yole.format.markdown.MarkdownParser

val parser = MarkdownParser()
val result = parser.parse("# Hello\n\n**Bold** text")
println(result.html)  // <h1>Hello</h1><p><strong>Bold</strong> text</p>
```

**More details**: See [API Usage Examples](api-usage/)

---

## API Usage Examples

**Location**: [`examples/api-usage/`](api-usage/)
**Lines**: 5,500+
**Topics**: 8 major categories

### What's Included

1. **Getting Started** - Project setup, imports, basic usage
2. **Format Detection** - By extension, by content, binary detection
3. **Parsing Text Files** - Markdown, Todo.txt, CSV, LaTeX, Org Mode
4. **Working with Documents** - Create, modify, track changes
5. **Format Registry** - Get formats, check support
6. **Platform-Specific** - Android, Desktop examples
7. **Advanced Usage** - Custom detection, batch processing, conversion
8. **Error Handling** - Safe loading, parsing, fallbacks

### Example Formats Covered

-  Markdown
-  Todo.txt
-  CSV
-  LaTeX
-  Org Mode
-  All 17 formats

**[’ Browse API Examples](api-usage/)**

---

## Sample Files

**Location**: [`samples/`](../samples/)
**Files**: 15+ sample files
**Format Coverage**: 9/17 formats (53%)

### Available Samples

| Format | Files | Description |
|--------|-------|-------------|
| **Markdown** | 6 files | Reference, demos, recipes, presentations |
| **CSV** | 1 file | Data table with headers |
| **Todo.txt** | 1 file | Task examples with priorities |
| **Org Mode** | 1 file | Complete syntax reference |
| **AsciiDoc** | 1 file | Jekyll post example |
| **WikiText** | 2 files | Zim wiki examples |
| **TaskPaper** | 1 file | Task management demo |

### Featured Samples

**`markor-markdown-reference.md`** - Complete Markdown syntax guide with:
- Headers, emphasis, lists
- Code blocks with highlighting
- Tables, task lists
- Math equations (KaTeX)
- Mermaid diagrams

**`todo.sample.txt`** - Todo.txt examples with:
- Priorities (A), (B), (C)
- Projects and contexts
- Due dates
- Completed tasks

**`orgmode-reference.org`** - Org Mode reference with:
- Headlines and structure
- TODO items
- Tables and timestamps
- Code blocks

**[’ Browse Sample Files](../samples/)**

---

## Tutorials

**Location**: [`examples/tutorials/`](tutorials/)
**Count**: 4 tutorials
**Difficulty**: Beginner to Expert

### Tutorial Catalog

**Tutorial 1: Simple Markdown Editor** (30 min, Beginner)
- Setup Yole dependency
- Load and save files
- Parse Markdown
- Display preview

**Tutorial 2: Todo.txt Manager** (45 min, Intermediate)
- Todo.txt format basics
- Parsing tasks
- Filtering and sorting
- Advanced queries

**Tutorial 3: Multi-Format Note App** (2 hours, Advanced)
- Handle all 17 formats
- Dynamic UI
- Format-specific features
- Export functionality

**Tutorial 4: Cross-Platform Viewer** (4 hours, Expert)
- Kotlin Multiplatform setup
- Shared business logic
- Platform-specific UI
- Android, Desktop, iOS, Web

**[’ Start Learning](tutorials/)**

---

## Integration Examples

**Location**: [`examples/integration/`](integration/)
**Scenarios**: 5 integration patterns
**Platforms**: Android, Desktop, iOS, Web

### Integration Scenarios

1. **Add Markdown Preview** - Render Markdown in existing app
2. **Add Format Detection** - File type detection for file managers
3. **Add Todo.txt** - Import/export tasks
4. **Add CSV Export** - Export data to CSV
5. **Add Multi-Format** - Support all formats in one app

### Integration Patterns

- **Drop-in Component** - Self-contained wrapper
- **Service Integration** - As a service layer
- **Plugin Architecture** - As a pluggable module

### Platform Examples

- **Android** - Activity/ViewModel integration
- **Desktop** - Compose Desktop integration
- **iOS** - SwiftUI + KMP bridge
- **Web** - Kotlin/Wasm integration

**[’ Integration Guide](integration/)**

---

## Examples by Use Case

### Building a Note-Taking App?

1. Start with [Tutorial 1: Markdown Editor](tutorials/)
2. Add more formats from [API Examples](api-usage/)
3. See [Integration: Multi-Format](integration/)
4. Use [Sample Files](../samples/) for testing

### Building a Task Manager?

1. Start with [Tutorial 2: Todo.txt Manager](tutorials/)
2. Learn [Todo.txt API](api-usage/)
3. See [Integration: Todo.txt](integration/)
4. Test with [todo.sample.txt](../samples/todo.sample.txt)

### Building a Documentation Viewer?

1. Learn [Format Detection API](api-usage/)
2. See [Integration: Format Detection](integration/)
3. Try [Tutorial 3: Multi-Format App](tutorials/)
4. Test with all [Sample Files](../samples/)

### Building a Data Tool?

1. Learn [CSV API](api-usage/)
2. See [Integration: CSV Export](integration/)
3. Use [sample.csv](../samples/sample.csv) for testing

### Building Cross-Platform App?

1. Follow [Tutorial 4: Cross-Platform Viewer](tutorials/)
2. Study [Architecture Guide](../ARCHITECTURE.md)
3. See [Platform-Specific Examples](api-usage/)
4. Review [Build System Guide](../docs/BUILD_SYSTEM.md)

---

## Examples by Platform

### Android

**Tutorials**:
- Tutorial 1-3 include Android examples

**API Examples**:
- Android ViewModel example
- Activity integration
- WebView preview

**Integration**:
- Add to existing Activity
- Material Design integration
- File picker integration

### Desktop (Compose)

**Tutorials**:
- Tutorial 4: Cross-platform app

**API Examples**:
- Compose Desktop example
- File chooser integration

**Integration**:
- Compose UI integration
- Native file access

### iOS (SwiftUI + KMP)

**Tutorials**:
- Tutorial 4: Cross-platform app

**Integration**:
- SwiftUI + Kotlin bridge
- iOS file system access

### Web (Kotlin/Wasm)

**Tutorials**:
- Tutorial 4: Cross-platform app

**Integration**:
- Compose for Web
- Browser file access

---

## Examples by Format

### Markdown

- **API**: [Markdown Parser](api-usage/)
- **Tutorial**: [Tutorial 1](tutorials/)
- **Samples**: 6 files in [samples/](../samples/)
- **Guide**: [Markdown Format Guide](../docs/user-guide/formats/markdown.md)

### Todo.txt

- **API**: [Todo.txt Parser](api-usage/)
- **Tutorial**: [Tutorial 2](tutorials/)
- **Samples**: [todo.sample.txt](../samples/todo.sample.txt)
- **Guide**: [Todo.txt Format Guide](../docs/user-guide/formats/todotxt.md)

### CSV

- **API**: [CSV Parser](api-usage/)
- **Integration**: [CSV Export](integration/)
- **Samples**: [sample.csv](../samples/sample.csv)
- **Guide**: [CSV Format Guide](../docs/user-guide/formats/csv.md)

### All 17 Formats

See [Format Guides](../docs/user-guide/formats/) for complete documentation.

---

## Learning Path

### I'm new to Yole

1.  Read [Getting Started](../docs/user-guide/getting-started.md)
2.  Try [Quick Start](#quick-start) above
3.  Follow [Tutorial 1](tutorials/)
4.  Browse [Sample Files](../samples/)
5.  Explore [API Examples](api-usage/)

### I want to add Yole to my app

1.  Check [Integration Examples](integration/)
2.  Find your use case above
3.  Review [API Documentation](../docs/api/)
4.  Test with [Sample Files](../samples/)
5.  Follow relevant [Tutorial](tutorials/)

### I want to contribute

1.  Read [Contributing Guide](../CONTRIBUTING.md)
2.  Study [Architecture Guide](../ARCHITECTURE.md)
3.  Review [API Examples](api-usage/)
4.  Check [Testing Guide](../docs/TESTING_GUIDE.md)
5.  See [Build System Guide](../docs/BUILD_SYSTEM.md)

---

## Examples Statistics

### Coverage

| Category | Items | Lines | Completion |
|----------|-------|-------|------------|
| **API Examples** | 8 topics | 5,500+ | 100% |
| **Sample Files** | 15+ files | - | 53% of formats |
| **Tutorials** | 4 tutorials | 2,000+ | 100% |
| **Integration** | 5 scenarios | 1,500+ | 100% |
| **Total** | **32+ items** | **9,000+** | **Comprehensive** |

### Formats with Examples

- **Full Coverage** (9 formats): Markdown, Todo.txt, CSV, LaTeX, Org Mode, AsciiDoc, WikiText, TaskPaper, Plain Text
- **API Only** (8 formats): reStructuredText, Key-Value, Textile, Creole, TiddlyWiki, Jupyter, R Markdown, Binary

### Platform Coverage

- **Android**:  Full coverage
- **Desktop**:  Full coverage
- **iOS**:   KMP examples
- **Web**:   KMP examples

---

## Related Resources

### Documentation

- **[Getting Started](../docs/user-guide/getting-started.md)** - Quick introduction
- **[Format Guides](../docs/user-guide/formats/)** - All 17 format guides
- **[API Documentation](../docs/api/)** - Complete KDoc reference
- **[FAQ](../docs/user-guide/faq.md)** - Common questions

### Development

- **[Architecture Guide](../ARCHITECTURE.md)** - System design
- **[Build System](../docs/BUILD_SYSTEM.md)** - Building Yole
- **[Testing Guide](../docs/TESTING_GUIDE.md)** - Testing practices
- **[Contributing](../CONTRIBUTING.md)** - How to contribute

### Community

- **[GitHub](https://github.com/vasic-digital/Yole)** - Source code
- **[Issues](https://github.com/vasic-digital/Yole/issues)** - Report bugs
- **[Discussions](https://github.com/vasic-digital/Yole/discussions)** - Ask questions
- **[Releases](https://github.com/vasic-digital/Yole/releases)** - Download

---

## Contributing Examples

We welcome example contributions!

### How to Contribute

1. **Sample Files**: Add files for missing formats
2. **API Examples**: Add more use cases
3. **Tutorials**: Create new step-by-step guides
4. **Integration**: Share your integration story

### Guidelines

- **Clear**: Easy to understand
- **Concise**: Focus on key concepts
- **Complete**: Runnable code
- **Commented**: Explain non-obvious parts
- **Tested**: Verify examples work

See [Contributing Guide](../CONTRIBUTING.md) for details.

---

## Feedback

Found an issue or have suggestions?

- **Report Bug**: [GitHub Issues](https://github.com/vasic-digital/Yole/issues)
- **Ask Question**: [Discussions](https://github.com/vasic-digital/Yole/discussions)
- **Suggest Example**: [Feature Request](https://github.com/vasic-digital/Yole/issues/new)
- **Contribute**: [Pull Request](https://github.com/vasic-digital/Yole/pulls)

---

**Examples Collection Version**: 1.0
**Last Updated**: November 11, 2025
**Total Items**: 32+ examples, tutorials, and samples
**Total Lines**: 9,000+ lines of example code and documentation
**Format Coverage**: 17/17 formats documented, 9/17 with sample files
