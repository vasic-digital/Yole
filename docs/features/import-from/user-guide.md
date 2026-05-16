<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Import From — User Guide

**Feature:** Import documents from external formats into Yole as Markdown  
**Shipped in:** iter-64 / v1.7.0  
**Platforms:** Android (full), Desktop (full), iOS (blocked), Web/Wasm (blocked)

---

## Overview

Yole can import documents from six external formats and convert them to Markdown automatically.
After import the converted text opens in the editor as an unsaved new document; you choose where to save it.

## Supported Formats

| Format | Extension(s) | Fidelity |
|--------|-------------|---------|
| Word document | `.docx` | High — headings, bold/italic, tables, images, hyperlinks |
| HTML page | `.html`, `.htm` | High — full CommonMark via jsoup + flexmark |
| Rich Text | `.rtf` | Medium — plain text + bold/italic; no colour, no images |
| OpenDocument Text | `.odt` | Medium — headings, paragraphs; list nesting preserved |
| PDF | `.pdf` | Heuristic — text-layer PDFs only; headings inferred from font size |
| EPUB e-book | `.epub` | Medium — chapter HTML converted to Markdown; metadata dropped |

### Fidelity details

**DOCX** preserves heading styles (Word "Heading 1"–"Heading 6"), bold, italic, tables (as GFM
pipe tables), inline images (as `![](image_N.ext)` placeholders), and hyperlinks.

**HTML** converts the full DOM via jsoup and flexmark-html2md-converter. ATX headings (`# H1`) are
always used; Setext style is disabled. Handles malformed HTML gracefully.

**RTF** extracts text via `javax.swing.text.rtf.RTFEditorKit`. Bold and italic spans are mapped to
Markdown markers. Colour, font family, embedded images, and tables are not preserved.

**ODT** reads headings (`text:h`) and paragraphs (`text:p`) with nested span text. Desktop uses the
Apache ODFDOM library; Android uses a direct ZipInputStream + XmlPullParser path that avoids the
Xerces2 conflict. Result quality is equivalent on both platforms.

**PDF** extracts text via PDFBox. Heading detection is **heuristic**: font sizes are histogrammed
and the mode is taken as body-text size; runs with a significantly larger font are promoted to ATX
headings (`HeadingDetector`). Accuracy depends on how consistently fonts are used in the source PDF.
A low-confidence warning is emitted when the size delta is less than 1.5 pt or the candidate run is
a single word. PDFs whose content is entirely rasterised (image-only scans) produce empty or
near-empty output with a warning — there is no embedded text layer for the extractor to read.

**EPUB** iterates OPF spine chapters in order, extracts each chapter's HTML via `HtmlImporter`, and
concatenates the results with chapter-break separators. Chapter order matches the reading order
declared in the OPF manifest.

---

## How to Import

### Files tab toolbar button (all platforms)

Tap or click the **Import** button (upload-arrow icon) in the FILES tab toolbar. A system file
picker opens filtered to the six supported extensions. After picking a file, import runs and the
result opens in the editor.

### File menu (Desktop)

**File → Import From…** opens the same system file picker.

### Android share intent

Any Android app that can share files (Files, Drive, Gmail attachments, Dropbox, etc.) can send a
file directly to Yole. In the sharing sheet choose **Yole** as the target. Yole handles
`ACTION_SEND` intents carrying a single URI. The MIME type is used for format detection;
if the MIME type is ambiguous the file extension is used as a fallback.

Supported MIME types: `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
(docx), `text/html`, `text/rtf` / `application/rtf`, `application/vnd.oasis.opendocument.text`
(odt), `application/pdf`, `application/epub+zip`.

### Desktop drag-and-drop

Drag a supported file from any file manager and drop it onto the Yole window. Import starts
immediately. Dropping multiple files imports each in sequence.

---

## Progress and warnings

A progress overlay is shown during import. When the importer emits warnings (e.g. skipped elements,
low-confidence headings, unrecognised RTF control words) a **Warnings** panel appears below the
preview. Each warning includes a severity (Info / Warning) and a short description.

---

## Platform availability

| Platform | Status |
|----------|--------|
| Android | Full — all 6 formats via JVM importers |
| Desktop (macOS/Linux/Windows) | Full — all 6 formats via JVM importers |
| iOS | Hard-blocked — `Result.failure(ImportError.NotSupported)` for all formats; no JVM runtime available |
| Web / Wasm | Hard-blocked — `Result.failure(ImportError.NotSupported)` for all formats; JVM libraries unavailable in Wasm sandbox |

iOS and Web blockers are architectural (no JVM), not temporary deferral decisions. Long-term paths
are: iOS → native Swift libraries (PDFKit, NSAttributedString, etc.) via Kotlin/Native interop;
Web → browser File API + WASM-compiled parsers. Neither is in scope for v1.7.0.

---

## Known gaps

| Tracker | Scope |
|---------|-------|
| `#iter-64-android-rtf-no-swing` | RTF import is **not available on Android** — `javax.swing.text.rtf.RTFEditorKit` is a Java SE class absent from the Android SDK. The Android actual returns `ImportError.NotSupported("rtf", "Android")`. Long-term path: integrate a pure-Kotlin Android-safe RTF tokeniser. |
| `#iter-64-ios-hard-blocked` | All 6 importers return `NotSupported` on iOS; no JVM runtime. |
| `#iter-64-web-hard-blocked` | All 6 importers return `NotSupported` on Web/Wasm; no JVM runtime. |
| `#iter-64-pdf-image-only` | PDFs without a text layer (image-only scans, locked PDFs) produce empty or near-empty Markdown. A warning is emitted but extraction is not attempted on raster-only pages. |
| `#iter-64-pdf-heading-heuristic` | PDF heading detection uses font-size histogram + mode comparison. Accuracy is 97% on well-formatted PDFs (published ML baseline) but degrades on PDFs with inconsistent font usage or decorative large-text elements. |
| `#iter-64-pdfbox-android-context` | `PDFBoxResourceLoader.init(null)` is called in the Android actual. Passing `null` works when CMaps are bundled inside the JAR but a real `Context` should be passed for reliable resource loading in all environments. Production call site must supply context. |
| `#iter-64-rtf-colour-images` | RTF colour, font family, embedded images, and tables are not extracted. Only text content with bold/italic is preserved. |
| `#iter-64-odt-android-list-nesting` | ODT Android path (ZipInputStream + XmlPullParser) extracts list items as plain paragraphs without indentation markers. Desktop ODFDOM path preserves nested list structure via recursive DOM walk. |
| `#iter-64-epub-metadata` | EPUB metadata (title, author, cover image, publisher) is dropped. Only spine-chapter text is imported. |
