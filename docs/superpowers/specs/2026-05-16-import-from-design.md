<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Import from — Design Spec (Feature 5 of 5)

> **Status:** Brainstorm complete; operator pre-approved execution.
> **Author:** brainstormed 2026-05-16 with operator.
> **Sequence:** Feature 5 — final feature of the 5-feature mandate. After Feature 4c (iter-63) ships, this closes the initiative.

---

## 1. Goal

Convert 6 document formats — **docx, pdf, rtf, html, odt, epub** — into Markdown that lands in Yole's editor as a native file. Users can import from a FILES-tab button, the File menu, drag-drop (Desktop), or system share intent (Android). Conversion preserves structure with maximum fidelity per-format heuristics.

---

## 2. Locked-in scope decisions (from brainstorm)

| Decision | Choice |
|---|---|
| Source formats | **6 formats**: docx, pdf, rtf, html, odt, epub. |
| Output format | **Markdown** (single target; matches Yole's primary native format). |
| Conversion fidelity | **Maximum**: full structure + per-format heuristics. PDF font-clustering for headings/code, table cell-grid detection, embedded image extraction, footnotes/citations preservation where library exposes them. Honestly note where fidelity is impossible (e.g., PDFs without text layer). |
| UI invocation surfaces | **All 4**: FILES-tab button + main-menu item + Desktop drag-drop + Android share intent. |

Per **CONST-037**, every commit MUST include a Cross-platform impact block. Per **CONST-038**, no Yole-specific content leaks into sibling submodules.

---

## 3. Architecture

```
shared/src/commonMain/kotlin/digital/vasic/yole/import/
├── ImportedDocument.kt              # data: sourceFormat, markdown, assetsDir?, warnings: List<String>
├── ImportWarning.kt                 # data: severity (Info/Warning), message, pageOrSection?
├── DocumentImporter.kt              # interface: suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument>
├── ImporterRegistry.kt              # registry: extension → DocumentImporter (similar to iter-58 ParserRegistry)
└── (per-format importer interfaces; per-platform actuals)

shared/src/{androidMain,desktopMain}/kotlin/digital/vasic/yole/import/
├── DocxImporter.{android,desktop}.kt    # Apache POI ooxml
├── PdfImporter.{android,desktop}.kt     # Apache PDFBox 3.0
├── RtfImporter.{android,desktop}.kt     # javax.swing.text.rtf (JVM stdlib) OR Apache POI HWPF
├── HtmlImporter.{android,desktop}.kt    # jsoup + flexmark-html2md-converter
├── OdtImporter.{android,desktop}.kt     # Apache ODFDOM
└── EpubImporter.{android,desktop}.kt    # epublib (nl.siegmann.epublib) + HtmlImporter for inner content

shared/src/{iosMain,wasmJsMain}/kotlin/digital/vasic/yole/import/
└── (all per-format importers as stubs: return Result.failure(ImportError.NotSupported))

shared/src/commonMain/kotlin/digital/vasic/yole/import/conversion/
├── HeadingDetector.kt               # font-size clustering for PDF/RTF
├── CodeBlockDetector.kt             # monospace-run detection (PDF + RTF + docx)
├── TableConverter.kt                # cell-grid → Markdown pipe-table
├── ImageExtractor.kt                # extract embedded images → companion assets dir OR base64 inline
└── LinkPreserver.kt                 # extract hyperlinks; emit Markdown link syntax

androidApp/src/main/.../ui/import/
├── ImportButton.kt                  # FILES tab button
├── ImportMenuItem.kt                # main-menu File → Import
├── ImportShareIntentHandler.kt      # Android share intent → ImportAction
├── ImportProgressDialog.kt          # progress indicator during conversion
├── ImportPreview.kt                 # show converted Markdown preview; user confirms save filename
└── ImportWarningsPanel.kt           # show list of warnings (lossy conversions, etc.)

androidApp/src/main/AndroidManifest.xml
└── (add intent-filter for SEND with MIME types per format)

desktopApp/src/main/.../DesktopImportDragDrop.kt
└── (drag-drop file handler for Compose Desktop)
```

### Invariants

- `DocumentImporter` interface is **uniform across formats** — same `import(bytes, fileName): Result<ImportedDocument>` shape.
- `ImporterRegistry` is the single source of truth for extension → importer mapping. Mirrors iter-58 `ParserRegistry` pattern.
- Conversion helpers (HeadingDetector, CodeBlockDetector, TableConverter, ImageExtractor, LinkPreserver) are **format-agnostic** in commonMain; per-format importers compose them.
- All importers MUST return `Result<ImportedDocument>` — never throw. Failures populate the Result.
- iOS + Wasm stubs return `Result.failure(ImportError.NotSupported("iOS"))` per CONST-035.
- Assets directory (for extracted images) is sibling to the saved file: `myDoc.md` → `myDoc_assets/image1.png`.
- Maximum fidelity is **best-effort honest** — `ImportWarning` list records every lossy decision (e.g., "Page 7: table column count uncertain, approximated as 3 columns").

## 4. Components

| Component | Responsibility | Cardinality |
|---|---|---|
| `ImportedDocument` | data: `(sourceFormat, markdown: String, assetsDir: String?, warnings: List<ImportWarning>)` | 1 per import |
| `ImportWarning` | data: `(severity, message, pageOrSection)` | 0–N per import |
| `DocumentImporter` interface | `suspend fun import(bytes, fileName): Result<ImportedDocument>` | 6 impls (1 per format) |
| `ImporterRegistry` | `forExtension(ext): DocumentImporter?`; lookup by file extension | 1 |
| `DocxImporter` | Apache POI ooxml; preserves headings (via Style), tables, images, links | 1 |
| `PdfImporter` | Apache PDFBox 3.0; uses PDFTextStripper + page-by-page heading heuristics; embedded image extraction via PDImageXObject | 1 |
| `RtfImporter` | javax.swing.text.rtf.RTFEditorKit OR Apache POI HWPF for richer extraction; tables + bold/italic preserved | 1 |
| `HtmlImporter` | jsoup parse + flexmark-html2md-converter (Apache 2.0). Single library does the heavy lifting | 1 |
| `OdtImporter` | Apache ODFDOM (OpenDocument Text); structure parallels docx | 1 |
| `EpubImporter` | epublib `nl.siegmann.epublib` extracts spine; each chapter passed through HtmlImporter; concatenated | 1 |
| `HeadingDetector` | Per-format heuristic: PDF font-size cluster → heading levels; docx → Style name; RTF → font-size+bold | 1 shared helper |
| `CodeBlockDetector` | Monospace font runs → ``` fenced ``` blocks. Per-format adaptation. | 1 shared helper |
| `TableConverter` | Cell-grid → Markdown pipe-table syntax. Per-format adaptation. | 1 shared helper |
| `ImageExtractor` | Extracts embedded images; saves to assets dir (`<name>_assets/`); returns Markdown image ref `![](_assets/image1.png)`. Fallback: base64 inline for transient previews. | 1 shared helper |
| `LinkPreserver` | Extract hyperlinks from per-format representation; emit Markdown `[text](url)`. | 1 shared helper |
| `ImportButton` | FILES-tab Compose button: tap → file picker → ImportAction | 1 in FILES tab |
| `ImportMenuItem` | Main-menu File → Import composable | 1 in menu |
| `ImportShareIntentHandler` | Android: registered intent-filter; receives shared file; routes to ImportAction | 1 (Android only) |
| `ImportProgressDialog` | Indeterminate progress + cancel button | 1 transient |
| `ImportPreview` | Compose: shows converted Markdown preview; user edits filename; Save/Cancel | 1 transient |
| `ImportWarningsPanel` | Compose: collapsible list of conversion warnings | 1 transient |
| `DesktopImportDragDrop` | Compose Desktop modifier: accepts drag-drop file onto editor area | 1 (Desktop only) |

## 5. Data flow

### 5.1 Generic import path

```
User invokes import (button / menu / drag-drop / share intent)
  → file URI obtained (via SAF on Android, JVM File API on Desktop)
  → read file bytes
  → determine importer: ImporterRegistry.forExtension(fileName.extension)
  → if no importer: Toast "Unsupported format"
  → else: ImportProgressDialog.show()
  → importer.import(bytes, fileName):
      ├ on Dispatchers.IO scope
      ├ withTimeout(60_000ms): per-format parse + convert
      └ return Result<ImportedDocument>
  → ImportProgressDialog.dismiss()
  → on Result.failure: Toast with error message
  → on Result.success:
      ├ ImportPreview.show(doc):
      │   ├ Markdown preview (uses existing Yole preview pipeline)
      │   ├ Warnings panel (collapsible)
      │   ├ Filename input (default: `<originalName>.md`)
      │   └ Save / Cancel buttons
      └ on Save:
          ├ write markdown to chosen path
          ├ if assets non-empty: write assets dir adjacent
          └ open the new file in editor
```

### 5.2 docx flow (Apache POI ooxml)

```
Bytes → XWPFDocument.parse(InputStream)
  → walk body elements:
      ├ XWPFParagraph:
      │   ├ check style name → HeadingDetector.headingLevelFor(styleName) or null
      │   ├ if heading: emit `#`*level + text
      │   ├ else: parse runs (XWPFRun):
      │   │   - text + bold/italic/code-style mapping → markdown
      │   │   - hyperlinks → LinkPreserver.toMarkdown
      │   │   - embedded images → ImageExtractor.extract
      │   └ emit paragraph
      ├ XWPFTable:
      │   ├ TableConverter.fromPOITable(tbl) → Markdown pipe-table
      │   └ emit table
      └ other: skip with ImportWarning(Info, "Skipped element: $type")
```

### 5.3 PDF flow (Apache PDFBox 3.0)

```
Bytes → PDDocument.load
  → for each page:
      ├ PDFTextStripper.getText(page) → raw text
      ├ extract per-character font + size info via PDFTextStripperByArea
      ├ HeadingDetector.clusterByFontSize(chars) → identify heading levels
      ├ CodeBlockDetector.identifyMonospaceRuns(chars) → wrap in ``` ```
      ├ ImageExtractor.extractFromPage(page) → save assets
      ├ TableConverter.detectFromTextLayout(chars) → best-effort table extraction
      └ append to markdown buffer
  → emit warnings for low-confidence detections
```

### 5.4 RTF flow

```
Bytes → RTFEditorKit.read(InputStream) → DefaultStyledDocument
  → walk element tree (similar to docx but lighter)
  → preserve bold/italic/font-size mapping
  → emit markdown
```

### 5.5 HTML flow

```
Bytes → jsoup.parse(String, "UTF-8")
  → flexmark FlexmarkHtmlConverter.build().convert(htmlDoc) → markdown
  → post-process:
      ├ ImageExtractor: download referenced <img src=...> + rewrite to local refs
      └ LinkPreserver: validate anchors
```

### 5.6 ODT flow (Apache ODFDOM)

```
Bytes → OdfTextDocument.loadDocument(stream)
  → walk OdfElement tree (similar shape to docx)
  → per-format heading/style/table mapping → markdown
```

### 5.7 EPUB flow

```
Bytes → EpubReader.readEpub(InputStream)
  → for each spine resource (chapter):
      ├ resource.getInputStream() → HTML bytes
      ├ HtmlImporter.import(htmlBytes, chapterName) → ImportedDocument per chapter
      └ append markdown + warnings to combined output
  → emit single ImportedDocument with concatenated content
```

### 5.8 Android share intent flow

```
External app shares file (.docx, etc.) → system share sheet → Yole appears
  → ImportShareIntentHandler.onReceive(intent):
      ├ extract file URI from intent.getParcelableExtra(Intent.EXTRA_STREAM)
      ├ resolve to bytes via ContentResolver
      └ same downstream as 5.1
```

## 6. Error handling

| Failure | Detection | Recovery |
|---|---|---|
| File extension not recognized | `ImporterRegistry.forExtension` returns null | Toast "Unsupported format: $ext" |
| File bytes truncated or corrupted | per-format library throws | Result.failure with error message; user-visible Toast |
| PDF has no text layer (scanned image) | PDFBox returns empty text | ImportedDocument with warning "PDF appears image-only; OCR not bundled"; markdown contains "[Empty page]" placeholders |
| docx XStyle reference unresolvable | catch + fall through to default | ImportWarning(Info, "Unrecognized style: $name"); paragraph still imported as plain text |
| RTF encoding mismatch | RTFEditorKit may fail on non-Latin encodings | Fall back to plain text extraction; warning |
| HTML has scripts/styles | jsoup parse handles; flexmark strips | Silently drop (Markdown can't represent JS anyway); no warning |
| ODT requires encryption password | ODFDOM throws | Toast "Encrypted ODT not supported" |
| EPUB DRM-protected | epublib detection | Toast "DRM-protected EPUB not supported" |
| Embedded image extraction fails (unknown codec) | ImageExtractor catches | Skip image; warning; markdown emits `![Image extraction failed](image_<n>_failed)` |
| Conversion exceeds 60s timeout | withTimeout | Toast "Import timed out — file too large or complex"; cancellable |
| User cancels via ImportProgressDialog | cancellation signal | Coroutine cancelled cleanly; no partial save |
| Assets dir collision (file already exists) | filesystem check before write | Auto-increment name: `myDoc_assets`, `myDoc_assets_1`, ... |
| Target markdown file path already exists | filesystem check on Save | Confirmation dialog: Overwrite / Rename / Cancel |
| Share intent fires with non-importable MIME | filter in AndroidManifest + runtime check | Show "Cannot import: this file type is not supported" |
| Drag-drop multiple files at once | accept first; warning toast for others | "Multi-file import: only first file processed; drop one at a time" |
| Library JAR signature verification fails (corrupt APK) | classloader exception | App fails to start; outside iter-64 scope |

`CancellationException` rethrown in every catch.

## 7. Per-platform feasibility

| Platform | Importers (JVM libs) | UI surfaces | Status |
|---|---|---|---|
| **Android** | All 6 (Apache POI + PDFBox + RTF + jsoup + flexmark + ODFDOM + epublib all run on Android with R8 keep rules) | FILES button + menu + **share intent** | Full support. APK growth ~30-50 MB. |
| **Desktop macOS-arm64** | All 6 | FILES button + menu + **drag-drop** | Full support. |
| **Desktop Linux/Windows** | All 6 | Same as macOS | Code-ready, distribution deferred per `#crossbuild-linux-windows-infra`. |
| **iOS** | Stubs only (Apache libs are JVM-only) | UI stubs | Hard-blocked: importers return NotSupported error. Native iOS would need different libs entirely. Future iteration. |
| **Web Wasm** | Stubs only (JVM libs incompatible) | UI stubs | Hard-blocked. WASM-friendly conversion libs exist but limited. Future iteration. |

## 8. Deep-research checklist

Phase 0 output: `docs/features/import-from/research-report.md` (≥ 600 lines, ≥ 100 citations).

1. **Apache POI on Android** — verify ooxml-schemas + R8 keep rules. POI is heavy (~30 MB); confirm modular dep (just `poi-ooxml` not full `poi-ooxml-full`).
2. **PDFBox 3.0 vs 2.x** — current stable; Android compatibility; ARM bytecode generation.
3. **PDF heading detection accuracy** — published heuristics for font-size-based clustering. Cite papers + library best practices.
4. **flexmark-html2md-converter** — Apache 2.0; output quality assessment; alternative: pandoc binary (not embeddable).
5. **ODFDOM size + Android compat** — Apache ODFDOM is JVM; size on classpath.
6. **epublib status** — last release date; maintenance status. Alternative: roll-own EPUB zip parse + HtmlImporter loop.
7. **Android intent-filter MIME types** — exact MIME strings for the 6 formats; verify share-sheet inclusion.
8. **Compose Desktop drag-drop** — `Modifier.onExternalDrag` or `dndAware()` per Compose 1.7; per-platform consistency (macOS NSPasteboard vs Linux X DnD).
9. **APK size impact projection** — sum sizes of all 6 library JARs after R8 minification.

## 9. Testing strategy (anti-bluff per CONST-035)

### 9.1 Unit tests (commonTest pure)

1. `ImportedDocumentTest` — data class fields + warning list.
2. `ImporterRegistryTest` — `forExtension(ext)` lookup for all 6 formats; unsupported extension returns null.
3. `HeadingDetectorTest` — table-driven over font-size sequences; clustering produces expected heading levels.
4. `CodeBlockDetectorTest` — monospace-run detection on synthesized runs.
5. `TableConverterTest` — cell-grid input → expected Markdown pipe-table.
6. `ImageExtractorTest` — extract synthetic image bytes; save to memory FS; verify markdown ref points correctly.
7. `LinkPreserverTest` — URL extraction + Markdown emit.

### 9.2 Per-importer tests (desktopTest, with real format fixtures)

8. `DocxImporterTest` — fixture `test.docx` with heading + paragraph + bullet list → expected markdown.
9. `PdfImporterTest` — fixture `test.pdf` (created via PDFBox itself for reproducibility) → expected markdown.
10. `RtfImporterTest` — fixture `test.rtf` → expected markdown.
11. `HtmlImporterTest` — `<h1>Title</h1><p><b>bold</b></p>` → `# Title\n\n**bold**`.
12. `OdtImporterTest` — fixture `test.odt` → expected markdown.
13. `EpubImporterTest` — fixture `test.epub` with 2 chapters → concatenated markdown.

### 9.3 Compose UI tests (Robolectric)

14. `ImportButtonRobolectricTest` — button visible in FILES tab; click invokes callback.
15. `ImportPreviewRobolectricTest` — preview displays converted markdown via testTag.
16. `ImportWarningsPanelRobolectricTest` — 3 warnings → 3 rows; click row jumps to page/section (if known).
17. `ImportShareIntentHandlerRobolectricTest` — synthesized Intent with EXTRA_STREAM → handler dispatches ImportAction.

### 9.4 Anti-bluff structural

18. `Feature5ImporterCompletenessTest` — every entry in `ImporterRegistry` has a corresponding implementing class; all 6 formats covered. Mutation: drop one importer → test FAILS.

### 9.5 Challenges (Phase 9)

19. `import_from_completeness_challenge.sh` — STATIC: 6 importer files exist + ImporterRegistry references all; RUNTIME: per-importer test suite ≥ 12 PASS + Robolectric ≥ 4 PASS.
20. `import_from_fixture_bundle_challenge.sh` — STATIC: test fixtures present for all 6 formats; assert valid fixture bytes (parseable by their respective libs).

Every test mutation-verified; procedure in KDoc.

## 10. Phase breakdown

| Phase | Scope | TDD depth |
|---|---|---|
| 0 | Deep research → `docs/features/import-from/research-report.md` | Research only |
| 1 | `ImportedDocument` + `ImportWarning` + `DocumentImporter` interface + `ImporterRegistry` | Full TDD |
| 2 | Conversion helpers: HeadingDetector + CodeBlockDetector + TableConverter + ImageExtractor + LinkPreserver | Full TDD |
| 3 | DocxImporter (Apache POI ooxml) + fixture-based test | Full TDD |
| 4 | HtmlImporter (jsoup + flexmark-html2md) + fixture test | Full TDD |
| 5 | RtfImporter (javax.swing RTFEditorKit) + fixture test | Full TDD |
| 6 | OdtImporter (Apache ODFDOM) + fixture test | Full TDD |
| 7 | PdfImporter (Apache PDFBox 3.0) + fixture test — heaviest single importer | Full TDD |
| 8 | EpubImporter (epublib + HtmlImporter loop) + fixture test | Full TDD |
| 9 | UI: ImportButton + ImportMenuItem + ImportProgressDialog + ImportPreview + ImportWarningsPanel | Full TDD + Robolectric |
| 10 | Android share intent integration (AndroidManifest + ImportShareIntentHandler) + Robolectric | Full TDD + Robolectric |
| 11 | Desktop drag-drop (DesktopImportDragDrop) — Desktop-only commit | Task-level |
| 12 | IdeEditorScreen / YoleApp integration: wire all 4 UI surfaces + iOS+Wasm stub registry | Task-level + Robolectric |
| 13 | 2 anti-bluff challenges + `qa-iter-64-gates` chained into `qa-all` | Full TDD on shell scripts |
| 14 | Documentation: user-guide, architecture, supported-formats matrix, CHANGELOG v1.7.0, CONTINUATION.md | Authoring only |
| 15 | Firebase distribution v1.7.0 | Mechanical |

**16 phases** — heaviest feature in the 5-feature mandate. 6 format-importers each get their own phase due to library-specific complexity.

## 11. Documentation deliverables

- `docs/features/import-from/user-guide.md`
- `docs/features/import-from/architecture.md`
- `docs/features/import-from/research-report.md`
- `docs/features/import-from/supported-formats.md` (6 formats × {library, fidelity quirks, known gaps})
- CHANGELOG.md v1.7.0 entry
- docs/CONTINUATION.md per CONST-036

## 12. Firebase distribution

Version target: **1.7.0 / versionCode 170 / dotted `0.0.0.1.70`**.

Same pattern as iter-60/61/62/63: Android Release + Debug + DEV + Desktop macOS-arm64 DMG.

## 13. Out of scope for Feature 5

- OCR for image-only PDFs (Tesseract integration would be a separate feature; ~50 MB dep).
- Markdown → other formats (export). Yole already has limited preview-to-HTML; full export TBD.
- Live "open as" without conversion (read-only viewer for non-native formats). Different feature shape.
- Cloud-storage browse (Yole has protocol code but no UI surface; separate feature).
- Source-control import (git clone + browse).
- Snippet/template/theme import from external editors.

## 14. Open questions for the implementation plan

- Apache POI Android compatibility: keep rules + version pin. Phase 0 §1.
- PDF heading-detection heuristic algorithm — adopt published K-means clustering or simpler max-min-gap. Phase 0 §3.
- epublib vs roll-own — based on Phase 0 §6 maintenance status.

## 15. Forensic anchor

Brainstorm session 2026-05-16 with operator. Operator chose at every round:

1. Feature 5 interpretation = "Document format conversion (Word/PDF/RTF/HTML → markdown/text)".
2. Source formats = "Comprehensive 6" (docx + pdf + rtf + html + odt + epub).
3. Conversion fidelity = "Maximum: full structure + per-format heuristics" (heaviest).
4. UI invocation = "All 4: FILES tab + menu + drag-drop + share intent" (Comprehensive).

No defaults selected; every choice explicit.

---

**Next steps:** writing-plans creates the 16-phase plan; subagent-driven execution proceeds autonomously per operator's pre-approval.
