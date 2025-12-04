# Yole Cross-Platform Architecture

## Overview

Yole is a **Kotlin Multiplatform (KMP)** text editor supporting Android, Desktop, iOS, and Web platforms with 17 text formats. This document describes the architecture, module structure, and implementation details.

**Architecture Philosophy**: Share as much code as possible through Kotlin Multiplatform, with platform-specific implementations only where necessary for optimal user experience.

## Current Platform Status (November 2025)

| Platform | Status | Notes |
|----------|--------|-------|
| **Android** | ✅ Production Ready | Fully functional with comprehensive test coverage |
| **Desktop** | ⚠️ Beta (30% complete) | Basic implementation, needs completion and testing |
| **iOS** | 🚧 Disabled | iOS targets temporarily disabled in `shared/build.gradle.kts:41` due to compilation issues. Requires resolution before re-enabling |
| **Web** | 🚧 Stub (0% complete) | Build configuration complete, but no source implementation. Infrastructure in place for future development |

**Critical Issues:**
- **iOS**: All iOS targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`) commented out. TODO at line 41 of `shared/build.gradle.kts`: "Re-enable iOS targets once basic compilation is working"
- **Web**: `webApp/src/main/` has no source code despite complete build configuration
- **Core Module**: Investigation needed - main source directory structure unclear

## Kotlin Multiplatform Architecture

### Core Concept

Yole uses **Kotlin Multiplatform (KMP)** to maximize code sharing across platforms:

```
┌─────────────────────────────────────────┐
│         Shared Business Logic           │
│  (Kotlin Multiplatform - shared module) │
│                                          │
│  • Format Parsers                        │
│  • Document Model                        │
│  • Format Registry                       │
│  • Core Utilities                        │
└──────────────┬───────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
   ┌───▼───┐      ┌────▼────┐
   │Android│      │ Desktop │
   │  UI   │      │   UI    │
   └───────┘      └─────────┘
       │                │
   ┌───▼───┐      ┌────▼────┐
   │  iOS  │      │   Web   │
   │   UI  │      │   UI    │
   └───────┘      └─────────┘
```

### Shared Module (`shared/`)

**Primary location** for cross-platform code:

```
shared/src/
├── commonMain/kotlin/digital/vasic/yole/
│   ├── format/                    # Format system (PRIMARY)
│   │   ├── FormatRegistry.kt      # Central format registry
│   │   ├── TextFormat.kt          # Format metadata
│   │   ├── TextParser.kt          # Parser interface
│   │   ├── markdown/              # Markdown parser
│   │   ├── todotxt/               # Todo.txt parser
│   │   ├── csv/                   # CSV parser
│   │   ├── latex/                 # LaTeX parser
│   │   ├── asciidoc/              # AsciiDoc parser
│   │   ├── orgmode/               # Org Mode parser
│   │   ├── wikitext/              # WikiText parser
│   │   ├── restructuredtext/      # reStructuredText parser
│   │   ├── taskpaper/             # TaskPaper parser
│   │   ├── textile/               # Textile parser
│   │   ├── creole/                # Creole parser
│   │   ├── tiddlywiki/            # TiddlyWiki parser
│   │   ├── jupyter/               # Jupyter parser
│   │   ├── rmarkdown/             # R Markdown parser
│   │   ├── plaintext/             # Plain text parser
│   │   └── keyvalue/              # Key-value parser
│   │
│   └── model/                     # Document model
│       └── Document.kt            # Cross-platform document
│
├── androidMain/kotlin/            # Android-specific code
├── desktopMain/kotlin/            # Desktop-specific code
├── iosMain/kotlin/                # iOS-specific code (disabled)
└── wasmJsMain/kotlin/             # Web-specific code
```

### Platform Modules

**Each platform has a dedicated app module**:

```
androidApp/     # Android application (Compose UI)
desktopApp/     # Desktop application (Compose Desktop)
iosApp/         # iOS application (SwiftUI + KMP)
webApp/         # Web application (Compose for Web)
```

### Dependency Flow

```
Platform Apps (androidApp, desktopApp, etc.)
       ↓
    shared (Kotlin Multiplatform)
       ↓
   commons (Android utilities - legacy)
```

## Module Structure

### Platform-Specific Applications

#### `androidApp` - Android Application
- **Purpose**: Native Android application with modern Android development practices
- **Key Components**:
  - Native Android UI components
  - Android-specific file system access
  - Platform-optimized performance
- **Technologies**: AndroidX, Kotlin, modern Android architecture

#### `desktopApp` - Desktop Application
- **Purpose**: Native desktop application for Windows, macOS, and Linux
- **Key Components**:
  - Desktop-specific UI framework
  - Native file system integration
  - Cross-platform desktop support
- **Technologies**: Compose Desktop, Kotlin

#### `iosApp` - iOS Application
- **Purpose**: Native iOS application
- **Key Components**:
  - iOS-specific UI components
  - iOS file system integration
  - Apple ecosystem integration
- **Technologies**: SwiftUI or UIKit, platform-specific development

#### `webApp` - Web Application
- **Purpose**: Progressive Web App with modern web technologies
- **Key Components**:
  - Web-specific UI framework
  - Browser-based file handling
  - PWA capabilities
- **Technologies**: WebAssembly, modern web development

### Legacy Android Modules

#### `commons` - Shared Utilities
- **Purpose**: Contains shared utilities, base classes, and interfaces used across Android modules
- **Key Components**:
  - `GsFileUtils` - File operations and utilities
  - `GsContextUtils` - Android context utilities
  - `GsCollectionUtils` - Collection manipulation utilities
  - Base classes for format implementations

#### `core` - Core Functionality
- **Purpose**: Contains reusable editors, syntax highlighters, and text converters
- **Key Components**:
  - Android-specific implementations
  - `TextConverterBase` - Base class for text format converters
  - `SyntaxHighlighterBase` - Base class for syntax highlighters
  - `ActionButtonBase` - Base class for format-specific action buttons

#### `app` - Legacy Android Application
- **Purpose**: Legacy Android application (being phased out)
- **Key Components**:
  - `MainActivity` - Main application screen
  - `DocumentActivity` - Document editing/viewing
  - `SettingsActivity` - Application settings
  - Legacy Android UI components

### Format Modules

#### Platform-Specific Format Implementations
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

Platform-specific implementations are maintained separately for optimal performance and native integration.
shared/src/commonMain/kotlin/digital/vasic/yole/format/[name]/
├── [Name]Parser.kt                   # Common parsing logic
└── Platform-specific implementations
    ├── androidMain/[Name]Parser.android.kt
    ├── desktopMain/[Name]Parser.desktop.kt
    └── iosMain/[Name]Parser.ios.kt
```

#### Legacy Android Format Modules
Each format also has a legacy Android module for UI-specific components:

```
format-[name]/
├── src/main/java/digital/vasic/yole/format/[name]/
│   ├── [Name]TextConverter.java      # HTML conversion
│   ├── [Name]SyntaxHighlighter.java  # Editor highlighting
│   └── [Name]ActionButtons.java      # Toolbar actions
├── src/test/java/                    # Unit tests
└── src/androidTest/java/             # Integration tests
```

## Supported Formats (17 Total)

All formats are implemented as parsers in the **`shared/src/commonMain/kotlin/digital/vasic/yole/format/`** directory.

### Core Formats (6)

1. **Markdown** - `markdown/MarkdownParser.kt`
   - Extensions: `.md`, `.markdown`, `.mdown`, `.mkd`
   - Features: CommonMark + GFM, tables, task lists, code blocks
   - Status: ✅ Full support

2. **Plain Text** - `plaintext/PlaintextParser.kt`
   - Extensions: `.txt`, `.text`, `.log`
   - Features: Universal text format, syntax highlighting for code
   - Status: ✅ Full support

3. **Todo.txt** - `todotxt/TodoTxtParser.kt`
   - Extensions: `.txt` (with todo.txt format)
   - Features: Task management, priorities, projects, contexts, due dates
   - Status: ✅ Full support with advanced query syntax

4. **CSV** - `csv/CsvParser.kt`
   - Extensions: `.csv`
   - Features: Comma-separated values, table view
   - Status: ✅ Full support

5. **LaTeX** - `latex/LatexParser.kt`
   - Extensions: `.tex`, `.latex`
   - Features: Academic documents, mathematical equations
   - Status: ✅ Full support

6. **Org Mode** - `orgmode/OrgModeParser.kt`
   - Extensions: `.org`
   - Features: Emacs org-mode, TODO items, scheduling, tables
   - Status: ✅ Full support

### Wiki Formats (3)

7. **WikiText** - `wikitext/WikitextParser.kt`
   - Extensions: `.wiki`, `.wikitext`, `.mediawiki`
   - Features: MediaWiki markup, Wikipedia-compatible
   - Status: ✅ Basic parsing

8. **Creole** - `creole/CreoleParser.kt`
   - Extensions: `.creole`, `.wiki`
   - Features: Standardized wiki markup, cross-wiki compatibility
   - Status: ✅ Basic parsing

9. **TiddlyWiki** - `tiddlywiki/TiddlyWikiParser.kt`
   - Extensions: `.tid`, `.tiddler`
   - Features: Non-linear personal wiki, tiddler format
   - Status: ✅ Basic parsing

### Technical Documentation Formats (2)

10. **AsciiDoc** - `asciidoc/AsciidocParser.kt`
    - Extensions: `.adoc`, `.asciidoc`, `.asc`
    - Features: Technical documentation, powerful features
    - Status: ✅ Basic parsing

11. **reStructuredText** - `restructuredtext/RestructuredTextParser.kt`
    - Extensions: `.rst`, `.rest`, `.restx`, `.rtxt`
    - Features: Python documentation standard, Sphinx integration
    - Status: ✅ Basic parsing

### Specialized Formats (3)

12. **Key-Value** - `keyvalue/KeyValueParser.kt`
    - Extensions: `.properties`, `.ini`, `.env`, `.conf`, `.config`, `.cfg`
    - Features: Configuration files (Java properties, INI, ENV)
    - Status: ✅ Full support

13. **TaskPaper** - `taskpaper/TaskpaperParser.kt`
    - Extensions: `.taskpaper`, `.todo`
    - Features: Plain-text task management, projects, tasks, tags
    - Status: ✅ Basic parsing

14. **Textile** - `textile/TextileParser.kt`
    - Extensions: `.textile`, `.txtl`
    - Features: Lightweight markup, CMS-friendly
    - Status: ✅ Basic parsing

### Data Science Formats (2)

15. **Jupyter** - `jupyter/JupyterParser.kt`
    - Extensions: `.ipynb`
    - Features: Interactive notebooks, JSON format, cells
    - Status: ✅ JSON viewing

16. **R Markdown** - `rmarkdown/RMarkdownParser.kt`
    - Extensions: `.Rmd`, `.rmarkdown`
    - Features: Reproducible R research, markdown + R code chunks
    - Status: ✅ Basic parsing

### Other (1)

17. **Binary** - Binary file detection
    - All binary file types
    - Features: Detects and prevents editing non-text files
    - Status: ✅ Full detection

## Implementation Details

### Format Registration

Formats are registered in platform-specific registries:

```kotlin
object FormatRegistry {
    private val formats = mutableMapOf<String, TextFormat>()

    fun registerFormat(format: TextFormat) {
        formats[format.name] = format
    }

    fun getFormat(name: String): TextFormat? = formats[name]
}
```

### Text Parsing Pipeline

1. **File Detection**: `TextFormat.detectFormat(content: String, extension: String)`
2. **Markup Parsing**: `TextParser.parse(content: String): Document`
3. **Platform Rendering**: Platform-specific converters to HTML/other formats
4. **Syntax Highlighting**: Platform-specific syntax highlighters

### Android Pipeline

1. **File Detection**: `TextConverter.isFileOutOfThisFormat()`
2. **Markup Parsing**: `TextConverter.convertMarkupToHtml()`
3. **HTML Rendering**: WebView with format-specific CSS
4. **Syntax Highlighting**: `SyntaxHighlighter.applySyntaxHighlighting()`

### Testing Strategy

#### Platform-Specific Tests
- `androidApp/src/test/`: Android unit tests
- `desktopApp/src/test/`: Desktop unit tests
- `iosApp/src/test/`: iOS unit tests
- `webApp/src/test/`: Web unit tests

#### Legacy Android Tests
- Format detection accuracy
- Markup conversion correctness
- Syntax highlighting patterns
- Action button functionality
- UI component integration

#### E2E Tests
- Full application workflows
- Real device/emulator testing
- Cross-platform compatibility testing

## Build System

### Gradle Configuration
- Multi-platform project with 25+ modules
- Version catalog for dependency management (`libs.versions.toml`)
- Shared build logic in root `build.gradle.kts`
- Module-specific dependencies
- Automated testing pipeline

### Platform Dependencies
- **Android**: AndroidX, Material Design, Kotlin
- **Desktop**: Compose Desktop, Kotlin
- **iOS**: Platform-specific frameworks
- **Web**: WebAssembly, modern web technologies

### Legacy Android Dependencies
- **Core**: AndroidX, Material Design
- **Markdown**: Flexmark library
- **CSV**: OpenCSV
- **Math**: KaTeX (via WebView)
- **Testing**: JUnit, AssertJ, Espresso

## Development Guide

### Platform Development

1. **Android Development**:
    - Use `androidApp/` module for modern Android development
    - Follow Android development best practices
    - Test on various Android devices and versions

2. **Desktop Development**:
    - Use `desktopApp/` module for cross-platform desktop
    - Test on Windows, macOS, and Linux
    - Ensure native look and feel

3. **iOS Development**:
    - Use `iosApp/` module for iOS development
    - Follow iOS development guidelines
    - Test on various iOS devices

4. **Web Development**:
    - Use `webApp/` module for PWA development
    - Ensure responsive design
    - Test across modern browsers

### Code Organization
- Keep platform-specific code in respective app modules
- Share common utilities through `commons/` module
- Use `core/` for shared Android functionality
- Maintain format modules for extensibility

## Future Enhancements

### Planned Features
- Plugin system for third-party formats
- Cloud synchronization for all platforms
- Advanced format conversion between types
- Collaborative editing support
- AI-powered features

### Performance Optimizations
- Lazy loading of format modules
- Caching of converted content
- Background processing for large files
- Platform-specific optimizations

## Contributing

When adding new formats:

### Platform-Specific Implementation
1. Create new `format-[name]` module
2. Implement platform-specific parsers and converters
3. Add to platform-specific `FormatRegistry`
4. Update string resources for each platform
5. Add comprehensive tests for each platform
6. Update documentation

### Cross-Platform Considerations
- Ensure consistent behavior across platforms
- Handle platform-specific file system differences
- Test on all target platforms
- Maintain performance optimizations

See existing format modules as reference implementations.