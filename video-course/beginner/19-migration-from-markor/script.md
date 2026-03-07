# Module 19: Migration from Markor (5 videos)

## Video 19.1: What's New in Yole vs. Markor (12 min)

### Script Outline

**[0:00-1:30] Welcome**
- Welcome to the migration guide
- Target audience: existing Markor users moving to Yole
- What you will learn: differences, compatibility, migration steps

**[1:30-3:30] History: Markor to Yole**
- Markor: Android-only Markdown and Todo.txt editor
- Yole: cross-platform successor built with Kotlin Multiplatform
- Package namespace evolution: `net.gsantner.opoc.*` (legacy) to `digital.vasic.yole.*`
- Legacy modules (`commons/`, `core/`) being phased out

**[3:30-6:00] New Capabilities**
- Platform support: Android (production), Desktop (beta), iOS (in development), Web/Wasm (in development)
- Format support: from 2-3 formats to 17 formats (Markdown, Todo.txt, CSV, LaTeX, Org Mode, Plain Text, WikiText, AsciiDoc, reStructuredText, R Markdown, TaskPaper, Textile, Creole, TiddlyWiki, Jupyter, Key-Value, Binary)
- Cloud storage: 8 protocols (Dropbox, Google Drive, OneDrive, WebDAV, FTP, SFTP, Git, S3)
- Offline-first architecture with optional cloud sync

**[6:00-9:00] Architecture Differences**
- Markor: single Android app with embedded logic
- Yole: shared module (business logic) + thin platform wrappers
- Same parsing engine on every platform: identical results guaranteed
- Live demo: same document on Android and Desktop side by side

**[9:00-11:00] Developer Experience Improvements**
- 8,800+ test methods across 170+ test files (vs. minimal tests in Markor)
- 10 extracted KMP modules with independent CI/CD
- Container-based builds for reproducibility
- 14 challenge banks in the Go-based Challenges framework
- Resilience patterns: CircuitBreaker, ConnectionLimiter, DocumentCache
- 6 security scanners: SonarQube, Snyk, CodeQL, Gitleaks, OWASP, Detekt

**[11:00-12:00] Summary**
- 100% file compatibility with Markor
- Massive feature expansion
- Same simplicity for basic use, powerful features for advanced users

---

## Video 19.2: File Compatibility (10 min)

### Script Outline

**[0:00-1:30] The Compatibility Promise**
- All Markor files work in Yole without modification
- Markdown files: `.md`, `.markdown`, `.mkd`, `.mdown` -- all recognized
- Todo.txt files: `todo.txt`, `done.txt` -- fully compatible
- Plain text files: `.txt` -- always supported

**[1:30-3:30] Format Detection**
- Yole uses `FormatRegistry.detectByExtension()` and `detectByContent()` for automatic format detection
- Markor's file extensions are a subset of Yole's supported extensions
- No manual format selection needed: Yole detects the right parser automatically

**[3:30-5:30] Markdown Compatibility**
- All Markdown syntax supported by Markor works identically in Yole
- Yole adds: tables, task lists, strikethrough, footnotes, abbreviations, definition lists, emoji, table of contents, YAML front matter, math (via Flexmark extensions)
- Rendering differences: Yole uses `StyleSheets.kt` for light/dark theme CSS

**[5:30-7:30] Todo.txt Compatibility**
- Full Todo.txt specification support: priorities, dates, projects, contexts
- Query syntax: same search/filter capabilities
- Yole adds: enhanced metadata parsing, better date handling

**[7:30-9:00] Testing Compatibility**
- Yole's format parser tests include Markor-compatible inputs
- FormatRegistry stress tests verify detection across all file extensions
- Roundtrip testing: parse and re-serialize to verify no data loss

**[9:00-10:00] Summary**
- Zero data loss guarantee when moving files from Markor to Yole
- Automatic format detection eliminates manual configuration
- New formats available for new files without affecting existing ones

---

## Video 19.3: New Formats Available (15 min)

### Script Outline

**[0:00-2:00] The Format Landscape**
- Markor supported: Markdown, Todo.txt, Plain Text
- Yole supports: 17 formats total
- All formats share the same parsing pipeline: detect, parse, generate HTML, style

**[2:00-5:00] Document Formats**
- **CSV**: spreadsheet data with table rendering
- **LaTeX**: academic papers with math rendering
- **Org Mode**: Emacs Org Mode with headings, TODO items, tables
- **WikiText**: MediaWiki markup
- **AsciiDoc**: technical documentation format
- **reStructuredText**: Python documentation standard (Sphinx)
- **R Markdown**: data science notebooks with embedded R code blocks
- **Textile**: lightweight markup used in Redmine and other tools

**[5:00-8:00] Specialized Formats**
- **TaskPaper**: task management with projects, tags, and notes
- **Creole**: common wiki markup standard
- **TiddlyWiki**: personal wiki format with tiddlers
- **Jupyter**: data science notebooks (`.ipynb` JSON format)
- **Key-Value**: configuration files (`.properties`, `.ini`, `.env`)
- **Binary**: hex viewer for binary files

**[8:00-11:00] Live Demo**
- Open a LaTeX document: see rendered math equations
- Open an Org Mode file: see structured headings and TODO items
- Open a CSV file: see rendered table
- Open a Jupyter notebook: see code cells and output

**[11:00-13:00] Format Registration and Priority**
- `FormatRegistry.formats` list: order determines detection priority
- More specific formats (Jupyter, TaskPaper) before general ones (Plain Text)
- Format ID constants: `TextFormat.ID_MARKDOWN`, `TextFormat.ID_TODOTXT`, etc.

**[13:00-14:00] Adding Your Own**
- Reference: Module 5 (Custom Format Development) for building new parsers
- Community contributions welcome for new format support

**[14:00-15:00] Summary**
- 17 formats cover most text-based workflows
- Same parsing quality across all platforms
- New formats do not affect existing file handling

---

## Video 19.4: Cross-Platform Usage (12 min)

### Script Outline

**[0:00-2:00] Platform Overview**
- Android: production-ready, available via APK
- Desktop: beta (Windows, macOS, Linux via JVM)
- iOS: in development (Xcode project)
- Web: in development (Wasm PWA)

**[2:00-4:30] Running on Desktop**
- `./gradlew :desktopApp:run` to launch the desktop app
- Same shared module, same parsers, same rendering
- Desktop uses JDK 21 for the app (JDK 11 for shared module)
- Live demo: editing a Markdown file on Desktop

**[4:30-7:00] Running on Web**
- `./gradlew :webApp:wasmJsBrowserRun` to launch the web app
- Kotlin/Wasm compiled to WebAssembly for near-native performance
- PWA support: installable as a web app
- File loading: browser file picker for local files
- All 17 formats available in the browser

**[7:00-9:00] Cross-Platform Sync**
- Cloud storage connects all platforms
- Edit on Android, sync to Dropbox, continue on Desktop
- Edit on Desktop, push to Git, pull on another machine
- WebDAV: self-hosted sync via Nextcloud
- Offline-first: all edits happen locally, sync when connected

**[9:00-11:00] Shared Code, Platform Differences**
- `expect`/`actual` declarations: 13 in commonMain, all with actuals on 4 platforms
- Platform-specific: secure storage, file system access, HTTP engine
- Shared: all parsers, all format logic, all network protocol logic

**[11:00-12:00] Summary**
- Same editing experience across Android, Desktop, Web, and (soon) iOS
- Cloud storage bridges all platforms seamlessly
- Shared code guarantees identical behavior

---

## Video 19.5: Step-by-Step Migration (10 min)

### Script Outline

**[0:00-1:30] Before You Start**
- Prerequisites: Yole installed on your target platform(s)
- Optional: cloud storage account for cross-platform sync
- Markor data location: typically in device storage under `Documents/` or a custom path

**[1:30-3:30] Step 1: Locate Your Markor Files**
- Default Markor storage: `/storage/emulated/0/Documents/markor/`
- Custom paths: check Markor settings for your configured directory
- File types: `.md`, `.txt`, `todo.txt`, `done.txt`

**[3:30-5:00] Step 2: Open Files in Yole**
- Method A (same device): point Yole to the same directory -- files open directly
- Method B (copy): copy files to Yole's working directory
- No conversion needed: Yole reads Markor files natively
- Format detection is automatic

**[5:00-6:30] Step 3: Verify Your Documents**
- Open each document type and verify rendering
- Markdown: check headings, bold, links, images, code blocks
- Todo.txt: check priorities, dates, projects, contexts
- Plain text: should display identically

**[6:30-8:00] Step 4: Set Up Cloud Storage (Optional)**
- Connect a cloud storage provider (Dropbox, Google Drive, OneDrive, WebDAV, Git)
- Upload your documents to the cloud
- Access them from Desktop or Web platforms
- See Module 13 (Cloud Storage Integration) for detailed setup

**[8:00-9:00] Step 5: Explore New Features**
- Try new formats: open a CSV file, a LaTeX document, or a Jupyter notebook
- Try new platforms: launch the Desktop or Web app
- Try cloud sync: edit on one device, verify changes appear on another

**[9:00-10:00] Summary and Next Steps**
- Migration is instant: no conversion, no data loss, no configuration
- Markor files continue to work everywhere
- Explore the full Yole feature set at your own pace
- Recommended next: Module 1 (Getting Started) for a full tour, or Module 13 (Cloud Storage) for sync setup
