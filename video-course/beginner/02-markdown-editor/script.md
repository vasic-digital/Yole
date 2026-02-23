# Module 2: Building a Simple Markdown Editor (8 videos)

## Video 2.1: Project Setup and Architecture (12 min)

### Script Outline

- Create project structure for a Markdown-only editor
- Design the shared module: parser, model, formatting
- Set up Gradle with KMP targets (Android + Desktop)

---

## Video 2.2: Creating the Shared Parser (15 min)

### Script Outline

- Implement `MarkdownParser` in commonMain
- Handle headings, paragraphs, emphasis, code blocks
- Write tests: `MarkdownParserTests` with 20+ test cases
- Demonstrate TDD: write test first, then implement

---

## Video 2.3: Android UI Implementation (12 min)

### Script Outline

- Create Compose UI with `TextField` for editing
- Add toolbar with formatting buttons (bold, italic, heading)
- Implement live preview using parser output
- Handle file open/save with Android file picker

---

## Video 2.4: Desktop UI Implementation (12 min)

### Script Outline

- Create Compose Desktop window with menu bar
- Add file tree sidebar
- Implement split-pane editor + preview
- Add keyboard shortcuts (Ctrl+B, Ctrl+I, Ctrl+S)

---

## Video 2.5: Web UI Implementation (15 min)

### Script Outline

- Set up Kotlin/Wasm target
- Create browser-based editor with `CanvasBasedWindow`
- Handle Wasm-specific constraints (no `synchronized`, no `@Volatile`)
- Implement basic PWA with service worker

---

## Video 2.6: Adding Syntax Highlighting (10 min)

### Script Outline

- Implement token-based highlighter in shared code
- Map tokens to colors per platform
- Optimize for large files (incremental re-highlight)

---

## Video 2.7: File Operations (12 min)

### Script Outline

- Implement expect/actual for file I/O
- Android: ContentResolver, Desktop: java.io.File, Web: File API
- Add recent files list
- Handle encoding detection (UTF-8, UTF-16, Latin-1)

---

## Video 2.8: Testing Your Editor (10 min)

### Script Outline

- Unit tests for parser (commonTest)
- Snapshot tests for HTML output
- Run tests across all platforms
- Achieve 90%+ code coverage with Kover
