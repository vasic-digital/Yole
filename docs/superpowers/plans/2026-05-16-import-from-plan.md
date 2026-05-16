<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Import from — Implementation Plan (iter-64, Feature 5 of 5)

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development.

**Goal:** Convert 6 document formats (docx, pdf, rtf, html, odt, epub) to Markdown that lands in Yole's editor.

**Architecture:** `DocumentImporter` interface + 6 per-format JVM impls; `ImporterRegistry` maps file extension → importer; conversion helpers (HeadingDetector, CodeBlockDetector, TableConverter, ImageExtractor, LinkPreserver) in commonMain. UI: FILES tab button + main menu + Desktop drag-drop + Android share intent.

**Tech Stack:** KMP 2.0.20, Compose Multiplatform 1.7.3, Apache POI ooxml (docx), Apache PDFBox 3.0 (pdf), javax.swing.text.rtf (rtf), jsoup + flexmark-html2md-converter (html), Apache ODFDOM (odt), epublib (epub).

**Reference spec:** `docs/superpowers/specs/2026-05-16-import-from-design.md` (committed `c7ac84c6`).

**Hard governance constraints (every commit):** CONST-035 anti-bluff (mutation-verify), CONST-037 cross-platform impact block, CONST-038 no submodule edits, JUnit4 + `runBlocking<Unit>`, SPDX headers, SSH-only git, Conventional Commits with iter-64 prefix, Detekt zero new violations, `CancellationException` rethrown.

---

## Phase 0 — Deep research

**Goal:** Close 9 OPEN questions per spec §8. `docs/features/import-from/research-report.md` (≥ 600 lines, ≥ 100 citations).

### Tasks
- [ ] **0.1**: Create dir + skeleton with 9 numbered sections.
- [ ] **0.2-0.10**: Close §1-§9 per spec — Apache POI Android compat, PDFBox 3.0 vs 2.x, PDF heading detection, flexmark-html2md, ODFDOM size, epublib status, Android intent-filter MIME types, Compose Desktop drag-drop, APK size impact.
- [ ] **0.11**: Verify metrics + commit `docs(iter-64): Phase 0 deep-research report for Import from`.

---

## Phase 1 — ImportedDocument + DocumentImporter + ImporterRegistry

**Files:** commonMain `ImportedDocument.kt`, `ImportWarning.kt`, `DocumentImporter.kt`, `ImporterRegistry.kt` + tests.

### Tasks
- [ ] **1.1: `ImportedDocument` + `ImportWarning`**:
  ```kotlin
  data class ImportWarning(val severity: Severity, val message: String, val pageOrSection: String? = null) {
      enum class Severity { Info, Warning }
  }
  data class ImportedDocument(
      val sourceFormat: String,
      val markdown: String,
      val assetsDir: String? = null,
      val warnings: List<ImportWarning> = emptyList(),
  )
  ```
  Tests: 2 (round-trip + warning list).

- [ ] **1.2: `DocumentImporter` interface**:
  ```kotlin
  sealed class ImportError(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
      class NotSupported(format: String, platform: String) : ImportError("Format $format not supported on $platform")
      class Malformed(format: String, cause: Throwable) : ImportError("Malformed $format: ${cause.message}", cause)
      class Timeout(format: String) : ImportError("Import of $format timed out")
  }
  interface DocumentImporter {
      val supportedExtensions: Set<String>
      suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument>
  }
  ```

- [ ] **1.3: `ImporterRegistry`**:
  ```kotlin
  class ImporterRegistry private constructor(private val byExt: Map<String, DocumentImporter>) {
      fun forExtension(ext: String): DocumentImporter? = byExt[ext.lowercase().removePrefix(".")]
      fun supported(): Set<String> = byExt.keys
      companion object {
          fun default(importers: List<DocumentImporter>): ImporterRegistry {
              val byExt = mutableMapOf<String, DocumentImporter>()
              for (i in importers) for (ext in i.supportedExtensions) byExt[ext.lowercase()] = i
              return ImporterRegistry(byExt)
          }
      }
  }
  ```
  Tests: 3 (lookup by ext, normalization, unsupported returns null).

- [ ] **1.4**: Commit `feat(iter-64): Phase 1 — ImportedDocument + DocumentImporter + ImporterRegistry`.

---

## Phase 2 — Conversion helpers (commonMain)

**Files:** `HeadingDetector.kt`, `CodeBlockDetector.kt`, `TableConverter.kt`, `ImageExtractor.kt`, `LinkPreserver.kt` under `shared/src/commonMain/kotlin/digital/vasic/yole/import/conversion/`.

### Tasks
- [ ] **2.1: `HeadingDetector`** — `fun headingLevelByFontSize(currentSize: Float, sortedDistinctSizes: List<Float>): Int?`. Returns 1-6 based on rank, or null if smallest size (= body text). 4 tests.
- [ ] **2.2: `CodeBlockDetector`** — `fun isMonospaceRun(fontName: String): Boolean`. Whitelist common monospace fonts (Courier, Consolas, Menlo, Monaco, Fira Code, etc.). 3 tests.
- [ ] **2.3: `TableConverter`** — `fun toMarkdownTable(rows: List<List<String>>): String`. Returns pipe-table Markdown. Header row + separator + data rows. 3 tests.
- [ ] **2.4: `ImageExtractor`** — `data class ExtractedImage(val data: ByteArray, val format: String, val suggestedFileName: String)`. Stateless transformer. 2 tests.
- [ ] **2.5: `LinkPreserver`** — `fun toMarkdownLink(text: String, url: String): String`. Escape special chars in `text`. 3 tests.
- [ ] **2.6**: Commit `feat(iter-64): Phase 2 — Conversion helpers`.

---

## Phase 3 — DocxImporter (Apache POI)

**Files:** commonMain expect, Desktop+Android JVM actuals, iOS+Wasm stubs, test fixture (synthesize via POI itself).

### Tasks
- [ ] **3.1**: Add Apache POI dep to `gradle/libs.versions.toml` + `shared/build.gradle.kts` (JVM source sets). Use `poi-ooxml` (the lighter variant, not `poi-ooxml-full`).
- [ ] **3.2**: `expect class DocxImporter() : DocumentImporter`. supportedExtensions = setOf("docx").
- [ ] **3.3**: Desktop + Android JVM bodies: parse XWPFDocument; walk body elements; emit markdown:
  - XWPFParagraph: check style → heading level via HeadingDetector; runs → bold/italic/code marker; hyperlinks via LinkPreserver; embedded images via ImageExtractor.
  - XWPFTable: collect cells → TableConverter.
  - other elements: skip with ImportWarning(Info, "Skipped element: $type").
- [ ] **3.4**: iOS + Wasm stubs return `Result.failure(ImportError.NotSupported("docx", "iOS"))` / Wasm.
- [ ] **3.5**: Test (desktopTest): synthesize a docx via POI containing heading + para + bold/italic + list; import; assert markdown matches expected.
- [ ] **3.6**: Commit `feat(iter-64): Phase 3 — DocxImporter (Apache POI)`.

---

## Phase 4 — HtmlImporter (jsoup + flexmark-html2md)

### Tasks
- [ ] **4.1**: Add jsoup + flexmark-html2md-converter deps.
- [ ] **4.2**: Expect class + JVM actuals: parse via jsoup; convert via flexmark `FlexmarkHtmlConverter.builder().build().convert(htmlDoc)`. Post-process: download referenced `<img src=...>` if data: URL → ImageExtractor; rewrite to local refs.
- [ ] **4.3**: iOS + Wasm stubs.
- [ ] **4.4**: Test: `<h1>Title</h1><p><b>bold</b></p>` → `# Title\n\n**bold**`.
- [ ] **4.5**: Commit `feat(iter-64): Phase 4 — HtmlImporter (jsoup + flexmark-html2md)`.

---

## Phase 5 — RtfImporter (javax.swing.text.rtf)

### Tasks
- [ ] **5.1**: Expect class + JVM actuals using `javax.swing.text.rtf.RTFEditorKit` to parse into `DefaultStyledDocument`; walk element tree.
- [ ] **5.2**: iOS + Wasm stubs.
- [ ] **5.3**: Test: synthesize a small RTF string (just RTF header + plain text + bold span) → expected markdown.
- [ ] **5.4**: Commit `feat(iter-64): Phase 5 — RtfImporter`.

---

## Phase 6 — OdtImporter (Apache ODFDOM)

### Tasks
- [ ] **6.1**: Add ODFDOM dep. Per Phase 0 §5 confirm size + Android compat.
- [ ] **6.2**: Expect class + JVM actuals: `OdfTextDocument.loadDocument(stream)`; walk OdfElement tree.
- [ ] **6.3**: iOS + Wasm stubs.
- [ ] **6.4**: Test: programmatically create ODT containing heading + para → import → expected markdown.
- [ ] **6.5**: Commit `feat(iter-64): Phase 6 — OdtImporter`.

---

## Phase 7 — PdfImporter (Apache PDFBox 3.0) — HEAVIEST

### Tasks
- [ ] **7.1**: Add PDFBox 3.0 dep. Per Phase 0 §2 confirm Android compat.
- [ ] **7.2**: Expect class + JVM actuals:
  - Per page: PDFTextStripper.getText(page) → raw text.
  - HeadingDetector on font sizes → identify heading levels.
  - CodeBlockDetector on font names → wrap in fences.
  - ImageExtractor from PDImageXObject for embedded images.
  - TableConverter best-effort from text-layout detection.
  - Emit warnings for low-confidence detections.
- [ ] **7.3**: iOS + Wasm stubs.
- [ ] **7.4**: Test: create a small PDF via PDFBox containing heading + paragraph → import → expected markdown with heading recognized.
- [ ] **7.5**: Commit `feat(iter-64): Phase 7 — PdfImporter`.

---

## Phase 8 — EpubImporter (epublib + HtmlImporter loop)

### Tasks
- [ ] **8.1**: Add epublib dep (`nl.siegmann.epublib:epublib-core` or modern alternative per Phase 0 §6).
- [ ] **8.2**: Expect class + JVM actuals: `EpubReader.readEpub(stream)`; for each spine chapter → HtmlImporter.import. Concatenate.
- [ ] **8.3**: iOS + Wasm stubs.
- [ ] **8.4**: Test: synthesize tiny epub with 2 HTML chapters → expected concatenated markdown.
- [ ] **8.5**: Commit `feat(iter-64): Phase 8 — EpubImporter`.

---

## Phase 9 — UI primitives

**Files:** `ImportButton.kt`, `ImportMenuItem.kt`, `ImportProgressDialog.kt`, `ImportPreview.kt`, `ImportWarningsPanel.kt` under `androidApp/src/main/.../ui/import/`.

### Tasks
- [ ] **9.1: `ImportButton`** — Composable in FILES tab; click → file picker.
- [ ] **9.2: `ImportMenuItem`** — Material3 DropdownMenuItem in main menu.
- [ ] **9.3: `ImportProgressDialog`** — Material3 AlertDialog with CircularProgressIndicator + Cancel button.
- [ ] **9.4: `ImportPreview`** — modal showing converted markdown + warnings panel + filename input + Save/Cancel buttons.
- [ ] **9.5: `ImportWarningsPanel`** — collapsible LazyColumn rows.
- [ ] **9.6**: 4 Robolectric tests covering rendering + click callbacks.
- [ ] **9.7**: Commit `feat(iter-64): Phase 9 — Import UI primitives`.

---

## Phase 10 — Android share intent integration

### Tasks
- [ ] **10.1**: `androidApp/src/main/AndroidManifest.xml` — add intent-filter for `ACTION_SEND` + `ACTION_VIEW` with MIME types for the 6 formats per Phase 0 §7.
- [ ] **10.2**: `ImportShareIntentHandler` class: receives Intent → resolves URI → reads bytes via ContentResolver → routes to import flow.
- [ ] **10.3**: Robolectric test: synthesized Intent → handler dispatches expected ImportAction call.
- [ ] **10.4**: Commit `feat(iter-64): Phase 10 — Android share intent integration`.

---

## Phase 11 — Desktop drag-drop

### Tasks
- [ ] **11.1**: `desktopApp/src/main/.../DesktopImportDragDrop.kt` — Compose Desktop `Modifier.onExternalDrag` per Phase 0 §8. Drop file → bytes → import.
- [ ] **11.2**: Manual smoke test (no Robolectric on Desktop; verify build success + visual confirmation deferred to operator).
- [ ] **11.3**: Commit `feat(iter-64): Phase 11 — Desktop drag-drop`.

---

## Phase 12 — Editor / YoleApp integration

### Tasks
- [ ] **12.1**: Wire ImportButton into existing FILES tab UI.
- [ ] **12.2**: Wire ImportMenuItem into main menu (if Yole has one — otherwise into Settings or toolbar).
- [ ] **12.3**: Wire ImportShareIntentHandler at YoleActivity / MainActivity level.
- [ ] **12.4**: Wire DesktopImportDragDrop into desktopApp's main editor surface.
- [ ] **12.5**: ImporterRegistry instance construction at app-level (wire all 6 importers).
- [ ] **12.6**: 3 Robolectric end-to-end tests covering: button click → file picker → preview → save; share intent → preview → save; etc.
- [ ] **12.7**: Commit `feat(iter-64): Phase 12 — YoleApp integration`.

---

## Phase 13 — 2 anti-bluff challenges + qa-iter-64-gates

### Tasks
- [ ] **13.1**: `import_from_completeness_challenge.sh` per spec §9.5.
- [ ] **13.2**: `import_from_fixture_bundle_challenge.sh` per spec §9.5.
- [ ] **13.3**: Makefile `qa-iter-64-gates` chained into `qa-all`.
- [ ] **13.4**: Run + commit.

---

## Phase 14 — Documentation

### Tasks
- [ ] **14.1-14.6**: user-guide.md, architecture.md, supported-formats matrix, CHANGELOG v1.7.0, CONTINUATION.md. Single commit.

---

## Phase 15 — Firebase distribution v1.7.0

### Tasks
- [ ] **15.1**: Bump versionCode 160→170, versionName 1.6.0→1.7.0, packageVersion 1.6.0→1.7.0.
- [ ] **15.2-15.5**: Build 3 Android variants + DMG; copy to releases; Firebase distribute Release + Debug + DEV (DEV via direct firebase CLI).
- [ ] **15.6**: Update CHANGELOG distribution subsection + CONTINUATION; commit. **iter-64 marks the 5-feature mandate as COMPLETE.**

---

## Final completion checklist

- [ ] All 16 phases shipped.
- [ ] `make qa-all` includes `qa-iter-64-gates` and PASSES.
- [ ] v1.7.0 in `releases/`.
- [ ] Firebase v1.7.0 release distributed to testers.
- [ ] CONTINUATION current.
- [ ] No submodule changes.
- [ ] CHANGELOG iter-64 entry.
- [ ] **Original 5-feature mandate COMPLETE** (Features 1+2+3+4a+4b+4c+5 all shipped).
