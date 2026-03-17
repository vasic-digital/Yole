# Module 2: Building a Simple Markdown Editor (8 videos)

## Video 2.1: Project Setup and Architecture (12 min)

### Script Outline

**[0:00-1:30] Module Overview**
- Recap of Module 1 environment setup
- Goal: build a working Markdown editor that runs on Android, Desktop, and Web
- What we will cover: shared parser, platform UIs, file I/O, testing

**[1:30-3:30] Project Structure**
- Create the KMP module layout: `shared/`, `androidApp/`, `desktopApp/`, `webApp/`
- Walk through `settings.gradle.kts` module includes
- Reference: `settings.gradle.kts` in the Yole repository root

**[3:30-6:00] Shared Module Design**
- Define the three layers: parser, model, formatting
- Show `shared/build.gradle.kts` with platform targets
- Explain source sets: `commonMain` for shared code, platform-specific `*Main` for expect/actual
- Reference: `shared/build.gradle.kts`

**[6:00-8:30] Data Model**
- Create `ParsedDocument` data class with raw content, parsed content, metadata, and errors
- Explain lazy HTML caching strategy used in Yole
- Reference: `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt`

**[8:30-10:30] Format Registry Pattern**
- Explain how `FormatRegistry` detects file types by extension and content
- Show `TextFormat` metadata class: id, name, extensions, detectionPatterns
- Reference: `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`
- Reference: `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextFormat.kt`

**[10:30-12:00] Build Verification**
- Run `./gradlew :shared:compileKotlinDesktop` to verify the shared module compiles
- Run `./gradlew :shared:compileKotlinAndroid` for the Android target
- Troubleshoot common errors: missing SDK, wrong JDK version

### Hands-On Exercise
- Create a minimal KMP project with a `shared` module containing a `MarkdownDocument` data class
- Add `commonMain`, `androidMain`, and `desktopMain` source sets
- Verify compilation on both platforms

---

## Video 2.2: Creating the Shared Parser (15 min)

### Script Outline

**[0:00-2:00] TDD Approach**
- Explain test-driven development: write the test first, then the code
- Create test file: `MarkdownParserTests.kt` in `commonTest`
- Reference: `shared/src/commonTest/kotlin/digital/vasic/yole/format/markdown/` for real-world examples

**[2:00-5:00] Heading Parsing**
- Write tests for `# H1`, `## H2`, through `###### H6`
- Implement heading detection with regex: `^(#{1,6})\\s+(.+)$`
- Convert to HTML: `<h1>`, `<h2>`, etc.
- Edge cases: `#` with no space, `#######` (7+ hashes), empty heading

**[5:00-8:00] Paragraph and Emphasis**
- Write tests for paragraphs separated by blank lines
- Implement bold (`**text**` to `<strong>`), italic (`*text*` to `<em>`)
- Nested emphasis: `***bold and italic***`
- Reference: Yole's Markdown parser handles these via Flexmark, but we build from scratch for learning

**[8:00-11:00] Code Blocks and Inline Code**
- Fenced code blocks with triple backticks
- Inline code with single backticks
- Language detection from the fence info string: `` ```kotlin ``
- Write tests for each variant including edge cases (empty code blocks, unclosed fences)

**[11:00-13:00] Links and Images**
- Parse `[text](url)` into `<a href="url">text</a>`
- Parse `![alt](src)` into `<img src="src" alt="alt">`
- Handle special characters in URLs

**[13:00-15:00] Running Tests**
- Execute: `./gradlew test --tests "*.MarkdownParserTests"`
- Review test output, verify all 20+ tests pass
- Show how Yole runs 9,400+ test methods across ~215 test files: `./gradlew test`
- Reference: `shared/src/commonTest/kotlin/digital/vasic/yole/format/markdown/`

### Hands-On Exercise
- Write 5 additional test cases for: blockquotes, horizontal rules, unordered lists, ordered lists, and nested lists
- Implement the parser methods to make all tests pass
- Run tests on both JVM and JS targets

---

## Video 2.3: Android UI Implementation (12 min)

### Script Outline

**[0:00-1:30] Android Module Setup**
- Verify `androidApp/build.gradle.kts` depends on `:shared`
- Set minimum SDK to API 24, target SDK to latest
- Add Compose dependencies from the version catalog
- Reference: `gradle/libs.versions.toml` for dependency versions

**[1:30-4:00] Editor Screen**
- Create `EditorScreen` composable with a `TextField` for editing
- Use `mutableStateOf` for document content tracking
- Implement `isDirty` flag for unsaved changes detection
- Style the text field: monospaced font, appropriate line height
- Reference: Yole's approach in the Android app module

**[4:00-6:30] Formatting Toolbar**
- Create a horizontal toolbar Row with IconButtons
- Add buttons: Bold (B), Italic (I), Heading (H), Code (`<>`), Link
- Implement insertion helpers: wrap selected text with `**`, `*`, backticks
- Handle cursor position and selection range

**[6:30-9:00] Live Preview Panel**
- Create split-pane layout: editor on top, preview on bottom
- Pass document content through the shared parser
- Render HTML output using Android `WebView` or Compose HTML renderer
- Debounce parsing to avoid re-parsing on every keystroke (300ms delay)
- Reference: `shared/src/commonMain/kotlin/digital/vasic/yole/format/TextParser.kt` for `ParsedDocument.toHtml()`

**[9:00-11:00] File Picker Integration**
- Use Android `ActivityResultContracts.OpenDocument` for file selection
- Read file content via `ContentResolver.openInputStream()`
- Detect format from filename using `FormatRegistry.detectByFilename()`
- Reference: `shared/src/commonMain/kotlin/digital/vasic/yole/format/FormatRegistry.kt`

**[11:00-12:00] Run on Device**
- Build and install: `./gradlew :androidApp:installDebug`
- Demonstrate opening a `.md` file and editing with live preview
- Show the formatting toolbar in action

### Hands-On Exercise
- Add a "Save" button that writes the edited content back to the file
- Implement an "Undo" stack that stores the last 10 edits
- Test on a physical device or emulator

---

## Video 2.4: Desktop UI Implementation (12 min)

### Script Outline

**[0:00-1:30] Desktop Module Setup**
- Verify `desktopApp/build.gradle.kts` depends on `:shared`
- Configure Compose Desktop with `application { }` block
- Set window title, size, and icon
- Reference: `desktopApp/src/main/kotlin/digital/vasic/yole/desktop/`

**[1:30-4:00] Menu Bar**
- Create a composable menu bar with File, Edit, View, Help menus
- File menu: New, Open, Save, Save As, Print, Exit
- Edit menu: Undo, Redo, Cut, Copy, Paste, Find, Replace
- Reference: `desktopApp/src/main/kotlin/digital/vasic/yole/desktop/ui/DesktopAppCompletion.kt` for `CompleteDesktopMenuBar`

**[4:00-6:30] File Tree Sidebar**
- Create a collapsible file tree panel on the left
- Use `java.io.File` to list directory contents
- Display file icons based on extension
- Handle directory expansion/collapse with animation

**[6:30-9:00] Split-Pane Editor and Preview**
- Implement a horizontal split layout: editor left, preview right
- Use a draggable divider for resizing panes
- Sync scroll positions between editor and preview
- Parse content through the shared `MarkdownParser` for the preview

**[9:00-11:00] Keyboard Shortcuts**
- Implement `Ctrl+B` for bold, `Ctrl+I` for italic
- `Ctrl+S` for save, `Ctrl+Shift+S` for save as
- `Ctrl+N` for new file, `Ctrl+O` for open
- `Ctrl+P` for print using `java.awt.print.PrinterJob`
- Reference: `desktopApp/src/main/kotlin/digital/vasic/yole/desktop/shortcut/DesktopKeyboardShortcuts.kt`

**[11:00-12:00] Run Desktop App**
- Launch: `./gradlew :desktopApp:run`
- Demonstrate opening a file from the file tree
- Show keyboard shortcuts and live preview

### Hands-On Exercise
- Add a "Recent Files" submenu under the File menu that lists the last 5 opened files
- Implement `Ctrl+G` (Go to Line) with a dialog that jumps to a line number
- Add a status bar at the bottom showing line count, word count, and current format

---

## Video 2.5: Web UI Implementation (15 min)

### Script Outline

**[0:00-2:00] Wasm Target Setup**
- Add `wasmJs()` target to `shared/build.gradle.kts`
- Configure `webApp/build.gradle.kts` with Kotlin/Wasm
- Explain the Wasm compilation pipeline: Kotlin to Wasm bytecode
- Reference: `shared/build.gradle.kts` for the `wasmJs()` target configuration

**[2:00-4:30] Wasm-Specific Constraints**
- No `synchronized` keyword: use `atomicfu` or restructure for single-threaded execution
- No `@Volatile`: Wasm is single-threaded so volatility is unnecessary
- No reflection: use explicit serialization with `kotlinx.serialization`
- No `java.*` imports: all code must use pure Kotlin or expect/actual

**[4:30-7:30] Browser-Based Editor**
- Create the Compose canvas-based window using `CanvasBasedWindow`
- Implement a `TextField` composable for the editor area
- Add a toolbar row with formatting buttons
- Handle browser keyboard events for shortcuts

**[7:30-10:00] Preview Rendering**
- Use the shared parser to generate HTML from Markdown input
- Display preview using Compose HTML rendering
- Implement debounced parsing (use `kotlinx.coroutines.delay`)
- Handle large documents without blocking the UI thread

**[10:00-12:30] File Operations in the Browser**
- Use the Web File API via `js()` interop for opening files
- Implement file download for saving (create Blob and trigger download)
- Handle drag-and-drop file loading
- Reference: `shared/src/wasmJsMain/` for Wasm-specific implementations

**[12:30-14:00] PWA Setup**
- Create `manifest.json` with app name, icons, and theme color
- Write a basic service worker for offline caching
- Configure `index.html` with meta tags for mobile viewport

**[14:00-15:00] Run in Browser**
- Launch: `./gradlew :webApp:wasmJsBrowserRun`
- Demonstrate the editor in Chrome, Firefox, and Safari
- Show offline functionality after service worker registration

### Hands-On Exercise
- Add localStorage persistence so the editor content survives page reloads
- Implement a "Share" button that copies the Markdown as HTML to the clipboard
- Test the PWA install experience on a mobile browser

---

## Video 2.6: Adding Syntax Highlighting (10 min)

### Script Outline

**[0:00-1:30] Highlighter Design**
- Explain token-based highlighting: lexer produces tokens, renderer colors them
- Define token types: `HEADING`, `BOLD`, `ITALIC`, `CODE`, `LINK`, `TEXT`
- Create `HighlightToken` data class with type, start, end, and style

**[1:30-4:00] Token Lexer Implementation**
- Build a `MarkdownLexer` in `commonMain` that scans input line-by-line
- Use regex patterns to identify tokens within each line
- Handle overlapping tokens (e.g., bold inside a heading)
- Return a list of `HighlightToken` for each line
- Reference: Yole uses Flexmark's AST for production highlighting, but we build a simpler version

**[4:00-6:30] Platform-Specific Rendering**
- Define a `HighlightTheme` with colors for each token type
- Android/Desktop: use `AnnotatedString` with `SpanStyle` for coloring
- Web: use CSS classes injected into the editor DOM
- Show light and dark theme variants
- Reference: `shared/src/commonMain/kotlin/digital/vasic/yole/format/StyleSheets.kt` for theme CSS generation

**[6:30-8:30] Performance Optimization**
- Incremental re-highlighting: only re-lex changed lines
- Use line-level caching: store tokens per line, invalidate on edit
- Benchmark: highlight 10,000 lines in under 50ms on Desktop
- Reference: `shared/src/commonMain/kotlin/digital/vasic/yole/util/LazyLoading.kt` for lazy evaluation patterns

**[8:30-10:00] Testing the Highlighter**
- Write tests verifying token positions for sample Markdown input
- Test edge cases: empty lines, very long lines, mixed formatting
- Run performance benchmarks: `./gradlew :shared:desktopBenchmark`

### Hands-On Exercise
- Add syntax highlighting for two additional token types: `BLOCKQUOTE` and `LIST_ITEM`
- Implement a "rainbow brackets" mode that colors nested parentheses differently
- Measure highlighting performance on a 5,000-line Markdown file

---

## Video 2.7: File Operations (12 min)

### Script Outline

**[0:00-1:30] Expect/Actual Pattern for File I/O**
- Explain the expect/actual mechanism for platform-specific file operations
- Define `expect` functions in `commonMain`: `readFileContent()`, `writeFileContent()`, `listDirectory()`
- Show the pattern used in Yole for platform abstraction

**[1:30-4:00] Android File I/O**
- Implement `actual` functions in `androidMain`
- Use `ContentResolver.openInputStream()` and `openOutputStream()` for SAF (Storage Access Framework)
- Handle `content://` URIs and `file://` paths
- Detect encoding: try UTF-8 first, fall back to platform default

**[4:00-6:30] Desktop File I/O**
- Implement `actual` functions in `desktopMain`
- Use `java.io.File` for direct filesystem access
- Integrate with `JFileChooser` for native open/save dialogs
- Handle file watching with `java.nio.file.WatchService` for auto-reload
- Reference: `desktopApp/src/main/kotlin/digital/vasic/yole/desktop/file/DesktopFileManager.kt`

**[6:30-8:30] Web File I/O**
- Implement `actual` functions in `wasmJsMain`
- Use the File API for reading files from `<input type="file">`
- Use `Blob` and `URL.createObjectURL()` for saving files
- Handle the asynchronous nature of browser file APIs with coroutines

**[8:30-10:30] Recent Files List**
- Create a `RecentFilesManager` in `commonMain` with expect/actual storage
- Android: use `SharedPreferences` for persistence
- Desktop: use a JSON file in the user's config directory
- Web: use `localStorage`
- Limit to 10 recent files, remove duplicates, validate paths on load
- Reference: `desktopApp/src/main/kotlin/digital/vasic/yole/desktop/storage/DesktopSettingsStorage.kt`

**[10:30-12:00] Encoding Detection**
- Check for BOM (Byte Order Mark) to detect UTF-16 LE/BE and UTF-8 BOM
- Fall back to charset detection heuristics for UTF-8 vs Latin-1
- Handle Shift-JIS and other encodings via platform libraries
- Write tests: file with BOM, file without BOM, mixed content

### Hands-On Exercise
- Implement auto-save that writes to a temporary file every 60 seconds
- Add a "Revert to Saved" feature that reloads the file from disk
- Create a file watcher on Desktop that prompts the user when a file changes externally

---

## Video 2.8: Testing Your Editor (10 min)

### Script Outline

**[0:00-1:30] Test Strategy Overview**
- Explain the testing pyramid: unit tests (most), integration tests, UI tests (fewest)
- Show Yole's test count: 9,400+ test methods across ~215 test files
- Goal for this module: achieve 90%+ code coverage on the shared parser
- Reference: `shared/src/commonTest/kotlin/digital/vasic/yole/format/` for test organization

**[1:30-3:30] Parser Unit Tests**
- Review the `MarkdownParserTests` written in Video 2.2
- Add edge case tests: empty input, whitespace-only input, extremely long lines
- Test Unicode content: CJK characters, emoji, RTL text
- Run: `./gradlew test --tests "*.MarkdownParserTests"`

**[3:30-5:30] HTML Snapshot Tests**
- Create golden files with expected HTML output for sample Markdown inputs
- Write a test that parses input and compares output to the golden file
- Demonstrate updating snapshots when parser behavior changes intentionally
- Reference: `shared/src/commonTest/kotlin/digital/vasic/yole/format/integration/` for cross-format integration tests

**[5:30-7:30] Cross-Platform Test Execution**
- Run tests on JVM: `./gradlew :shared:desktopTest`
- Run tests on Android: `./gradlew :shared:testDebugUnitTest`
- Run tests on Wasm: `./gradlew :shared:wasmJsTest`
- Explain why a test might pass on JVM but fail on Wasm (regex differences, missing APIs)
- Reference: `docker/scripts/test-all.sh` for the comprehensive test runner

**[7:30-9:00] Code Coverage with Kover**
- Add Kover plugin to `build.gradle.kts`
- Run: `./gradlew test koverHtmlReport`
- Open the HTML report and walk through coverage metrics
- Identify uncovered branches and write targeted tests to fill gaps
- Reference: Yole uses Kover 0.8.3 configured in `shared/build.gradle.kts`

**[9:00-10:00] Continuous Integration**
- Show how to run tests in Docker containers for reproducibility
- Reference: `docker/scripts/test-all.sh` and `docker-compose.yml`
- Brief mention of GitHub Actions or other CI pipelines
- Emphasize: all tests must pass before merging (project rule)

### Hands-On Exercise
- Write 10 additional edge-case tests for your parser (target specific uncovered branches)
- Set up a `koverVerify` task that fails the build if coverage drops below 85%
- Create a stress test that parses 1,000 different Markdown files and verifies no exceptions are thrown
- Reference: `shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/` for stress test examples
