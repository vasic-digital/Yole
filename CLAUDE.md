# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Yole** is a cross-platform text editor supporting 17+ text formats including Markdown, todo.txt, CSV, LaTeX, WikiText, reStructuredText, and more. Built with Kotlin Multiplatform (KMP) for maximum code sharing across Android, Desktop, iOS, and Web platforms.

**Key Characteristics:**
- Kotlin Multiplatform (KMP) architecture
- Android (production), Desktop (beta), iOS (disabled), Web (stub)
- 17 text formats with comprehensive parsing
- Offline-first, no internet required
- Package namespace: `digital.vasic.yole.*`

## Build & Development Commands

### Essential Build Commands

**Android:**
```bash
./gradlew :androidApp:assembleDebug
# or
make build
```

**Desktop:**
```bash
./gradlew :desktopApp:run
# or
make desktop
```

**Web (Wasm):**
```bash
./gradlew :webApp:wasmJsBrowserRun
# or
make web
```

**iOS:**
```bash
# Currently disabled - see shared/build.gradle.kts:52
# Open iosApp/iosApp.xcodeproj in Xcode when re-enabled
```

### Testing Commands

**Run all tests:**
```bash
./gradlew test
# or
make test
# or (comprehensive suite)
./run_all_tests.sh
```

**Run specific test class:**
```bash
./gradlew test --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests.ParseQuery"
```

**Test with coverage:**
```bash
./gradlew test koverHtmlReport
open build/reports/kover/html/index.html
```

### Other Commands

**Lint:**
```bash
./gradlew lintFlavorDefaultDebug
# or
make lint
```

**Clean:**
```bash
./gradlew clean
# or
make clean
```

**Generate API documentation:**
```bash
./gradlew :shared:dokkaHtml
open shared/build/dokka/html/index.html
```

**Verify architecture:**
```bash
./verify_build.sh
```

**Build, install, and run on Android device:**
```bash
make all install run
```

## Architecture

### Kotlin Multiplatform Structure

**CRITICAL:** This is a **Kotlin Multiplatform (KMP)** project. The format system lives in the `shared` module, NOT in `core` or separate format modules.

```
┌─────────────────────────────────────────┐
│      Shared Business Logic (KMP)       │
│     shared/src/commonMain/kotlin/      │
│                                         │
│  • FormatRegistry                       │
│  • Format Parsers (17 formats)          │
│  • Document Model                       │
│  • Core Utilities                       │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴────────┐
       ▼                ▼
   Android          Desktop
   (prod)           (beta)
       ▼                ▼
     iOS              Web
  (disabled)         (stub)
```

### Module Breakdown

**Kotlin Multiplatform Modules:**

1. **shared** - PRIMARY MODULE for cross-platform code
   - Location: `shared/src/commonMain/kotlin/digital/vasic/yole/`
   - **FormatRegistry**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`
   - **Format Parsers**: `shared/src/commonMain/kotlin/digital/vasic/yole/format/[format]/`
   - **Models**: `shared/src/commonMain/kotlin/digital/vasic/yole/model/`
   - Platform implementations: `androidMain/`, `desktopMain/`, `iosMain/`, `wasmJsMain/`

**Platform Application Modules:**

2. **androidApp** - Native Android app (production ready)
   - Modern Jetpack Compose UI
   - Android-specific file system access
   - Status: ✅ Production

3. **desktopApp** - Desktop app (Windows/macOS/Linux)
   - Compose Desktop UI
   - Status: ⚠️ Beta (30% complete)

4. **iosApp** - iOS app
   - SwiftUI + KMP
   - Status: 🚧 Disabled (targets commented out in `shared/build.gradle.kts:52`)

5. **webApp** - Progressive Web App
   - Kotlin/Wasm + Compose for Web
   - Status: 🚧 Stub (build config only, no source code)

**Legacy Android Modules:**

6. **commons** - Android shared utilities
   - `GsFileUtils`, `GsContextUtils`, `GsCollectionUtils`
   - Location: `commons/src/main/kotlin/net/gsantner/opoc/`

7. **core** - Legacy encryption module
   - **Contains ONLY third-party encryption code**
   - `JavaPasswordbasedCryption.java` - AES256 encryption
   - `PasswordStore.java` - Password storage
   - Location: `core/thirdparty/java/`
   - **Note:** Does NOT contain FormatRegistry (that's in `shared`)

8. **app** - Legacy Android app (being phased out)
   - Legacy UI components
   - Being replaced by `androidApp`

### Format System

All 17+ formats are implemented as **parsers in the shared module**, not as separate Gradle modules:

```
shared/src/commonMain/kotlin/digital/vasic/yole/format/
├── FormatRegistry.kt              # Central format registry
├── TextFormat.kt                  # Format metadata
├── TextParser.kt                  # Parser interface
├── markdown/MarkdownParser.kt     # Markdown parsing
├── todotxt/TodoTxtParser.kt       # Todo.txt parsing
├── csv/CsvParser.kt               # CSV parsing
├── latex/LatexParser.kt           # LaTeX parsing
├── asciidoc/AsciidocParser.kt     # AsciiDoc parsing
├── orgmode/OrgModeParser.kt       # Org Mode parsing
├── wikitext/WikitextParser.kt     # WikiText parsing
├── restructuredtext/RestructuredTextParser.kt
├── taskpaper/TaskpaperParser.kt
├── textile/TextileParser.kt
├── creole/CreoleParser.kt
├── tiddlywiki/TiddlyWikiParser.kt
├── jupyter/JupyterParser.kt
├── rmarkdown/RMarkdownParser.kt
├── plaintext/PlaintextParser.kt
├── keyvalue/KeyValueParser.kt
└── binary/BinaryParser.kt
```

**Supported Formats (17 total):**
- **Core**: Markdown, Plain Text, Todo.txt, CSV, LaTeX, Org Mode
- **Wiki**: WikiText, Creole, TiddlyWiki
- **Technical**: AsciiDoc, reStructuredText
- **Specialized**: Key-Value, TaskPaper, Textile
- **Data Science**: Jupyter, R Markdown
- **Other**: Binary Detection

### Dependency Flow

```
Platform Apps (androidApp, desktopApp, iosApp, webApp)
       ↓
    shared (Kotlin Multiplatform - PRIMARY)
       ↓
   commons (Android utilities - legacy)
```

### Text Parsing Pipeline

1. **File Detection** → `TextFormat.detectFormat(content, extension)`
2. **Markup Parsing** → `TextParser.parse(content)`
3. **Platform Rendering** → Platform-specific converters
4. **Syntax Highlighting** → Platform-specific highlighters

## Code Style & Conventions

- **Language:** Kotlin (primary), Java (legacy)
- **Package Structure:**
  - App code: `digital.vasic.yole.*`
  - Legacy utilities: `net.gsantner.opoc.*`
- **Naming:**
  - Classes: `CamelCase`
  - Methods/variables: `lowerCamelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Imports:** Group standard libraries, third-party, then project imports
- **Error Handling:** Use try-catch blocks, log errors, null-check rigorously
- **Testing:** JUnit 4/5 with AssertJ assertions, test classes end with `Tests` or `Test`
- **File Headers:** Include SPDX license header (Apache-2.0, CC0-1.0, or Unlicense)

## Testing Strategy

### Test Locations

- **Shared Module**: `shared/src/commonTest/kotlin/`
- **Platform Tests**: `androidApp/src/test/`, `desktopApp/src/test/`, etc.
- **Legacy Tests**: Individual format module tests (being phased out)

### Test Requirements

- All tests must pass (100% success rate required)
- Use `./run_all_tests.sh` for comprehensive testing
- Current coverage: ~15% (goal: >80%)
- Generate coverage: `./gradlew koverHtmlReport`

### Test Types

1. **Unit Tests** - Format parsing, detection, conversion
2. **Integration Tests** - Module interaction, full pipelines
3. **Platform Tests** - UI components, device-specific functionality

## Key Dependencies

- **Kotlin Multiplatform**: Core architecture
- **Android**: AndroidX, Material Design, Jetpack Compose
- **Desktop**: Compose Desktop, JVM 11
- **Web**: Kotlin/Wasm, Compose for Web
- **Markdown**: Flexmark library
- **CSV**: OpenCSV
- **Math Rendering**: KaTeX (via WebView)
- **Testing**: JUnit, AssertJ, Espresso, Kover
- **Serialization**: kotlinx.serialization
- **Coroutines**: kotlinx.coroutines
- **File System**: Okio

## Platform Status (November 2025)

| Platform | Status | Notes |
|----------|--------|-------|
| **Android** | ✅ Production | Fully functional |
| **Desktop** | ⚠️ Beta | 30% complete, needs work |
| **iOS** | 🚧 Disabled | Targets commented out in `shared/build.gradle.kts:52` |
| **Web** | 🚧 Stub | Build config only, no source implementation |

**Critical Issues:**
- **iOS**: All targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`) disabled
- **Web**: `webApp/src/main/` has no source code despite build configuration

## Adding New Formats

When implementing a new format:

1. **Create Parser** in `shared/src/commonMain/kotlin/digital/vasic/yole/format/[name]/`
   - Implement `TextParser` interface
   - Handle file detection via extensions
   - Implement `parse()` method

2. **Register Format** in `FormatRegistry.kt`
   - Add to format list
   - Define extensions
   - Set metadata

3. **Add Tests** in `shared/src/commonTest/kotlin/digital/vasic/yole/format/[name]/`
   - Test file detection
   - Test parsing accuracy
   - Test edge cases

4. **Update Documentation**
   - Add user guide in `docs/user-guide/formats/[name].md`
   - Update README.md
   - Update ARCHITECTURE.md

5. **Platform Implementations** (if needed)
   - Add platform-specific code in `androidMain/`, `desktopMain/`, etc.
   - Implement syntax highlighting per platform
   - Add platform-specific UI components

## Special Files & Scripts

- `ARCHITECTURE.md` - Detailed architecture documentation
- `AGENTS.md` - Build commands and CI/CD quick reference
- `run_all_tests.sh` - Comprehensive test runner with colored output
- `verify_build.sh` - Architecture verification script
- `Makefile` - Build automation with common targets
- `settings.gradle.kts` - Kotlin Multiplatform configuration
- `CONTRIBUTING.md` - Contribution guidelines
- `docs/` - Comprehensive documentation (13,200+ lines)

## CI/CD

### GitHub Actions Workflows

- **Main Build**: `.github/workflows/build-android-project.yml`
- **Tests & Coverage**: `.github/workflows/test-and-coverage.yml`
- **Lint & Docs**: `.github/workflows/lint-and-docs.yml`
- **PR Validation**: `.github/workflows/pr-validation.yml`

### Required Secrets

- `CODECOV_TOKEN` - For coverage reports
- `GITHUB_TOKEN` - Auto-provided for GitHub Pages

See `AGENTS.md` for complete CI/CD documentation.

## Common Pitfalls & Important Notes

1. **Format System Location**: Formats are in `shared` module, NOT in `core` or separate modules
2. **iOS Builds**: Currently disabled - check `shared/build.gradle.kts:52` before attempting iOS work
3. **Legacy Code**: `app`, `core`, and `commons` are legacy modules being phased out
4. **Test Requirements**: All tests MUST pass - no exceptions
5. **Build Variants**: Use `flavorDefault` for development, `flavorAtest` for testing
6. **Documentation**: Always update both code documentation (KDoc) and user guides

## Application Features

- **Notebook:** Root folder for all documents (user-configurable)
- **QuickNote:** Fast-access Markdown file
- **ToDo:** Main todo.txt file for task management
- **File Encryption:** AES256 encryption support
- **Syntax Highlighting:** Real-time highlighting for all formats
- **HTML Preview:** WebView-based rendering
- **PDF Export:** Convert documents to PDF
- **Cross-Platform:** Files are plaintext, compatible with any editor

## Debugging & Troubleshooting

- Use Android Studio's built-in debugger
- Check `dist/log/gradle.log` for build errors
- Test failures require investigation before proceeding
- Format issues: Check corresponding parser in `shared/src/commonMain/kotlin/digital/vasic/yole/format/`
- Platform issues: Check platform-specific implementations in `[platform]Main/`

## Active Technologies
- Kotlin 2.1.0 (Multiplatform) + Ktor Client 3.0.2, Okio, platform-specific SDKs, kotlinx.coroutines, kotlinx.serialization (002-network-protocols)
- Local cache with SQLite metadata store, platform secure storage for credentials (002-network-protocols)

## Recent Changes
- 002-network-protocols: Added Kotlin 2.1.0 (Multiplatform) + Ktor Client 3.0.2, Okio, platform-specific SDKs, kotlinx.coroutines, kotlinx.serialization
