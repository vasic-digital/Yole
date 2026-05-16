<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Import From — Supported Formats

**Feature:** iter-64 / v1.7.0  
Six formats are supported in v1.7.0.

---

## Format matrix

| Format | Extension(s) | Library (Desktop) | Library (Android) | Version | Fidelity tier | License |
|--------|-------------|-------------------|-------------------|---------|--------------|---------|
| DOCX | `.docx` | Apache POI `poi-ooxml` | Apache POI `poi-ooxml` | 5.5.1 | High | Apache-2.0 |
| HTML | `.html` `.htm` | jsoup + flexmark-html2md-converter | jsoup + flexmark-html2md-converter | jsoup 1.17.2, flexmark 0.64.8 | High | MIT (jsoup), BSD-2-Clause (flexmark) |
| RTF | `.rtf` | `javax.swing.text.rtf.RTFEditorKit` (Java SE) | **Not supported** | Java SE stdlib | Medium | — |
| ODT | `.odt` | Apache ODFDOM `odfdom-java` | ZipInputStream + `android.util.Xml` | ODFDOM 1.0.0-BETA1 | Medium | Apache-2.0 (ODFDOM) |
| PDF | `.pdf` | Apache PDFBox | pdfbox-android | PDFBox 3.0.7 / pdfbox-android 2.0.27.0 | Heuristic | Apache-2.0 |
| EPUB | `.epub` | roll-own (ZIP + jsoup + HtmlImporter) | roll-own (same) | n/a | Medium | — |

---

## DOCX

**Library:** Apache POI 5.5.1 (`org.apache.poi:poi-ooxml`)  
**Fidelity:** High

What is preserved:
- Heading styles `"Heading 1"` through `"Heading 6"` (18 variant names mapped) → ATX headings `#`–`######`
- Bold and italic runs → `**bold**`, `*italic*`, `***bold-italic***`
- Tables → GFM pipe tables via `TableConverter`
- Inline images → `![](image_N.ext)` placeholder references
- Hyperlinks → Markdown inline links via `LinkPreserver`

What is dropped: comments, tracked changes, footnotes, endnotes, page headers/footers, custom styles
not matching the known heading name set, text boxes.

**Android-specific notes:**
- `multiDexEnabled = true` required (POI + transitive deps exceed 64k method limit).
- `androidApp/proguard-rules.pro` contains centic9 keep rules for R8 (currently dormant; required if `isMinifyEnabled = true` is ever enabled).
- Plan originally specified `poi-ooxml-lite`; the lite artifact does not include XWPF classes. Full `poi-ooxml` is used instead.

**Known gaps:** None for DOCX on Android/Desktop. iOS + Web return `ImportError.NotSupported`.

---

## HTML

**Library:** jsoup 1.17.2 + flexmark-html2md-converter 0.64.8  
**Fidelity:** High

What is preserved:
- All standard heading tags `h1`–`h6` → ATX headings
- Block elements (`p`, `blockquote`, `pre`, `code`, `ul`, `ol`, `li`, `table`)
- Inline emphasis (`strong`, `b`, `em`, `i`, `code`)
- Links (`a[href]`) and images (`img[src]`)

ATX heading mode is forced (`SETEXT_HEADINGS = false`). Malformed HTML is tolerated by jsoup's
HTML5 parser. Both `.html` and `.htm` extensions are accepted.

**Android-specific notes:** Identical implementation — same JVM actuals for Android and Desktop.

**Known gaps:** iOS + Web return `ImportError.NotSupported`.

---

## RTF

**Library:** `javax.swing.text.rtf.RTFEditorKit` (Java SE standard library)  
**Fidelity:** Medium (text, bold, italic only)

What is preserved:
- Plain text content
- Bold spans → `**text**`
- Italic spans → `*text*`

What is dropped: colour, font family, embedded images, tables, footnotes.

**Android-specific notes:**
`javax.swing.text.rtf.RTFEditorKit` is a Java SE class. It is **absent from the Android SDK**.
The Android actual returns `ImportError.NotSupported("rtf", "Android")` with tracker
`#iter-64-android-rtf-no-swing`. The long-term path is to integrate a pure-Kotlin
Android-safe RTF tokeniser once one reaches the Yole quality bar; no such library was found
at the time of Phase 5 research.

**Input validation:** bytes must start with `{\rtf` — otherwise `ImportError.Malformed` is returned.

**Known gaps:** RTF not available on Android (`#iter-64-android-rtf-no-swing`), iOS, or Web.
Colour, images, and tables not extracted on Desktop either (`#iter-64-rtf-colour-images`).

---

## ODT

**Library (Desktop):** Apache ODFDOM `odfdom-java` 1.0.0-BETA1  
**Library (Android):** ZipInputStream + `android.util.Xml.newPullParser()` (Android SDK built-in)  
**Fidelity:** Medium

What is preserved:
- `text:h` elements with `text:outline-level` attribute → ATX headings; level clamped to [1, 6]
- `text:p` elements → plain paragraphs
- Nested `text:span` and `text:a` text content concatenated

What is dropped: character styles, list indentation on Android, embedded images, tables, footnotes.

**Android-specific notes:**
ODFDOM pulls in Xerces2 (`xml-apis` + `xercesImpl`) which conflicts with Android's built-in XML
parser. ODFDOM is therefore excluded from `androidMain`. The Android actual reads `content.xml`
directly from the ODT ZIP container using `ZipInputStream` and parses it with
`android.util.Xml.newPullParser()`. Both paths produce structurally equivalent Markdown for the
common case; list nesting is not preserved on Android (see `#iter-64-odt-android-list-nesting`).

**Library status:** `odfdom-java` 1.0.0-BETA1 is the only release on Maven Central. No stable
release exists as of May 2026.

**Known gaps:** Android list nesting not preserved (`#iter-64-odt-android-list-nesting`);
iOS + Web return `ImportError.NotSupported`.

---

## PDF

**Library (Desktop):** Apache PDFBox 3.0.7 (`org.apache.pdfbox:pdfbox`)  
**Library (Android):** pdfbox-android 2.0.27.0 (`com.tom-roush:pdfbox-android`)  
**Fidelity:** Heuristic

What is preserved:
- Selectable text content (text layer must be present)
- Heading structure inferred from font size via histogram analysis → ATX headings
- Monospace font runs → fenced code blocks via `CodeBlockDetector`
- Embedded images → `![](image_N.ext)` placeholders

Heading detection algorithm:
1. Custom `PDFTextStripper` subclass captures `(text, fontSize, fontName)` per run.
2. Character-count-weighted font-size histogram; mode = body-text size.
3. `sortedDescending().distinct()` size list → `HeadingDetector.headingLevelByFontSize` maps ranks.
4. Low-confidence warning when size delta < 1.5 pt or run is a single word.

Published baseline accuracy: 96.95% on well-formatted PDFs.

**Android-specific notes:**
The Android community port `pdfbox-android` tracks PDFBox 2.0.27 API (not 3.x). API divergences:
- Entry point: `PDDocument.load(InputStream)` (2.x) vs `Loader.loadPDF(byte[])` (3.x).
- `PDFBoxResourceLoader.init(null)` required for CMap resource loading on Android.
- Image bytes: `android.graphics.Bitmap.compress(PNG)` instead of `javax.imageio.ImageIO.write()`.
- Package prefix: `com.tom_roush.pdfbox.*` instead of `org.apache.pdfbox.*`.
- APK size impact: ~5–7 MB of dex + resources.

**Known gaps:**
- Image-only PDFs produce empty output (`#iter-64-pdf-image-only`).
- Heading detection is heuristic (`#iter-64-pdf-heading-heuristic`).
- Android actual calls `init(null)` — production code should pass `Context` (`#iter-64-pdfbox-android-context`).
- iOS + Web return `ImportError.NotSupported`.

---

## EPUB

**Library:** Roll-own (no third-party EPUB library)  
**Fidelity:** Medium

What is preserved:
- Spine chapter text in reading order (OPF `<spine>` `<itemref>` order)
- All HTML content within each chapter via `HtmlImporter` (same fidelity as HTML import)
- Chapter breaks as `---` Markdown horizontal rules

What is dropped: EPUB metadata (title, author, publisher, cover image, table of contents).

**Library choice rationale:**
`epublib` (the most popular Java EPUB library) is abandoned — last commit May 2021, no releases
since 2017. The roll-own approach (ZipInputStream + OPF XML parsing + HtmlImporter delegation)
is ~120 lines and avoids a stale dependency. jsoup is already on the classpath from `HtmlImporter`.

**Android-specific notes:** Same implementation as Desktop — EPUB extraction uses
`ZipInputStream` and `HtmlImporter` which are both available on Android.

**Known gaps:** Metadata dropped (`#iter-64-epub-metadata`). iOS + Web return `ImportError.NotSupported`.
