<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# iter-64 Phase 0 — Import From: Deep Research Report

**Date:** 2026-05-16
**Author:** Claude Code (Sonnet 4.6) — automated Phase 0 research
**Feature:** Feature 5 of 5 — Import From (docx + pdf + rtf + html + odt + epub → Markdown)
**Spec reference:** `docs/superpowers/specs/2026-05-16-import-from-design.md` §8

---

## Executive Summary

This report surveys the 9 open questions raised in the import-from design spec. Key findings: Apache POI `poi-ooxml-lite` (not `poi-ooxml-full`) is viable on Android with a mandatory ProGuard/R8 keep-rule set; PDFBox 3.0.7 is the current stable release but has **no Android port tracking 3.x** (the Android community port `pdfbox-android` tracks 2.0.27 and appears minimally maintained since Jan 2023); font-size clustering for heading detection has a published ML baseline (96.95% accuracy) and is practical via `TextPosition.getFontSizeInPt()`; `flexmark-html2md-converter` is BSD-2-Clause (not Apache 2.0) and is the best pure-Java HTML→Markdown converter available; ODFDOM `odfdom-java` 0.12.0 ships a 3.7 MB JAR with an Android-hostile Xerces dependency; `epublib` upstream is abandoned (last commit May 2021); the recommended MIME strings for all 6 formats are documented; Compose `dragAndDropTarget` API works on Android/Desktop/iOS but is explicitly **not supported on Web/Wasm** as of Compose Multiplatform 1.8; and APK size impact can be kept to approximately 8–15 MB net with R8 full-mode applied.

---

## Cross-platform impact (CONST-037)

| Platform | Import capability | Notes |
|---|---|---|
| Android | Full (all 6 formats via JVM importers) | Share-intent + FILES button + File menu |
| Desktop (Linux/macOS/Windows) | Full (all 6 formats via JVM importers) | Drag-drop + FILES button + File menu |
| iOS | Stub returns `Result.failure(ImportError.NotSupported)` | Native Swift bridging not in scope for this iteration |
| Web (Wasm) | Stub returns `Result.failure(ImportError.NotSupported)` | JVM libraries not available in Wasm; browser File API workaround deferred |

All four platforms compile via `commonMain` interface; actual implementations live in `androidMain` / `desktopMain`.

---

## §1. Apache POI on Android — ooxml-schemas + R8 keep rules

### 1.1 Library landscape

Apache POI [1] provides two schema artefacts for OOXML processing:

- **`poi-ooxml-lite`** (formerly `poi-ooxml-schemas`): ~6 MB JAR, contains only the commonly-needed XSD-derived classes. This is the recommended choice for mobile targets. [2]
- **`poi-ooxml-full`** (formerly `ooxml-schemas`): ~16 MB JAR, full schema coverage. Necessary only when accessing obscure OOXML features. [3]
- **`poi-ooxml`** (the main OOXML API artefact): ~1.9 MB JAR (version 5.3.0). Depends on `poi-ooxml-lite` by default. [4]

The current stable release is **Apache POI 5.5.1** (November 2025) [5], with 5.5.0 released November 6, 2025 [6].

### 1.2 Android portability situation

Two community approaches exist for running POI on Android:

1. **`centic9/poi-on-android`** [7]: Sample project demonstrating POI on Android. Uses minSdk 26. Current build.gradle references POI 5.5.0. Actively maintained and includes canonical ProGuard/R8 rules. Last updated 2024.

2. **`SUPERCILEX/poi-android`** [8]: Older JitPack-hosted port that rebundles POI as Android-compatible JARs. Uses multiDexEnabled. Tracks POI 3.17, which is outdated — **do not use**.

**Recommendation:** Use the stock `poi-ooxml` + `poi-ooxml-lite` from Maven Central, with the keep-rules from centic9. Do not use the SUPERCILEX port.

### 1.3 Required R8 / ProGuard keep rules

The canonical keep rules from `centic9/poi-on-android/poitest/proguard-rules.pro` [9] are:

```proguard
-keep class org.apache.logging.log4j.** { *; }
-keep class org.apache.commons.compress.archivers.zip.** { *; }
-keep class org.apache.poi.schemas.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.schemas.** { *; }
-keep class org.microsoft.schemas.** { *; }
-keeppackagenames org.apache.poi.ss.formula.function

# Suppress warnings for external/optional deps
-dontwarn org.etsi.**
-dontwarn org.w3c.**
-dontwarn javax.xml.**
-dontwarn java.awt.**
```

There is also a Google Issue Tracker report [10] covering `minifyReleaseWithR8` failures with POI, confirming that these keep rules are necessary for R8 full-mode shrinking.

A 2025 Android Developers Blog post [11] explains R8 keep rule diagnostics, which is useful when debugging missing classes at runtime.

### 1.4 Key constraints for Yole

- Enable `multiDexEnabled = true` in `androidApp/build.gradle.kts` — POI + its transitive deps exceed the 64k method limit.
- Use `poi-ooxml-lite`, not `poi-ooxml-full`. For docx text + heading extraction, the lite variant is sufficient.
- The stax dependency (`stax:stax-api`) conflicts with Android's built-in XML stack. Exclude it:
  ```kotlin
  implementation("org.apache.poi:poi-ooxml:5.5.1") {
      exclude(group = "stax", module = "stax-api")
  }
  ```

**References:**
[1] https://poi.apache.org/components/
[2] https://poi.apache.org/help/faq.html
[3] https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml-full/5.3.0
[4] https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml/5.3.0
[5] https://poi.apache.org/download.html
[6] https://www.mail-archive.com/announce@apache.org/msg09302.html (5.3.0 announce; 5.5.0 similarly on Nov 6, 2025)
[7] https://github.com/centic9/poi-on-android
[8] https://github.com/SUPERCILEX/poi-android
[9] https://github.com/centic9/poi-on-android/blob/master/poitest/proguard-rules.pro
[10] https://issuetracker.google.com/issues/295349278
[11] https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html

---

## §2. PDFBox 3.0 vs 2.x — current stable and Android compatibility

### 2.1 PDFBox 3.0 release status

Apache PDFBox [12] current releases as of 2026-05-16:

| Branch | Latest stable | Java requirement |
|---|---|---|
| PDFBox 3.0.x | **3.0.7** | Java 8+ |
| PDFBox 2.0.x | 2.0.36 | Java 6+ |

PDFBox 3.0 introduced on-demand (incremental) PDF parsing that reduces initial memory footprint [13]. All deprecated APIs from 2.x were removed. Migration guide is at `pdfbox.apache.org/3.0/migration.html` [14].

Sub-modules in the 3.0 distribution: `pdfbox`, `pdfbox-io`, `fontbox`, `preflight`, `xmpbox`, `pdfbox-tools`, `pdfbox-debugger`. The `pdfbox-io` sub-module is new in 3.0.

### 2.2 Android compatibility gap

PDFBox itself requires the JVM (AWT classes etc.) and **is not directly usable on Android**. The community Android port situation:

- **`com.tom-roush:pdfbox-android`** [15] — the primary Android port by Tom Roush. Latest release: **2.0.27.0** (January 2, 2023). Tracks PDFBox **2.x only**. Requires API 19+. Initialization: `PDFBoxResourceLoader.init(context)`. Has 113 open issues, 12 pending PRs, **minimal activity since 2023**.

- **`github.com/jmaksuta/pdfbox-android`** [16] — alternate Android compatibility fork. No Maven Central publication.

- **No PDFBox 3.x Android port exists** as of this research date.

### 2.3 Recommendation for Yole

Use `com.tom-roush:pdfbox-android:2.0.27.0` for the Android target. Accept the 2.0.x API. The `PdfTextStripper` class works for text extraction in 2.x exactly as in 3.x for our use case (heading detection via `TextPosition`).

For Desktop (JVM), use `org.apache.pdfbox:pdfbox:3.0.7` — the full modern release with better performance and memory characteristics.

This means the `PdfImporter` implementation will diverge slightly between Android and Desktop. Both expose `TextPosition` with `getFontSizeInPt()` / `getFontSize()` so the heading-detection algorithm is portable.

**Known issue:** `pdfbox-android` increases raw APK size by ~7 MB due to the embedded `fontbox` resources [17]. R8 shrinking reduces this but the font resources are not shrinkable — they are runtime-loaded binary assets.

**References:**
[12] https://pdfbox.apache.org/
[13] https://pdfbox.apache.org/3.0/migration.html
[14] https://pdfbox.apache.org/3.0/migration.html
[15] https://github.com/TomRoush/PdfBox-Android
[16] https://github.com/jmaksuta/pdfbox-android
[17] https://github.com/TomRoush/PdfBox-Android/issues/103

---

## §3. PDF heading detection — font-size clustering heuristics

### 3.1 Academic background

Heading detection in PDFs has an established ML literature. Key paper:

**"A Supervised Learning Approach For Heading Detection"** — Budhiraja & Mago, arXiv:1809.01477 (2018) [18].
- Method: Recursive feature elimination to select text features; supervised classifier.
- Results: **96.95% accuracy, sensitivity 0.986, specificity 0.953**.
- Features include: font size, font weight, position on page, prefix patterns, surrounding whitespace.

Earlier work:

- **Klampfl & Kern hierarchical agglomerative clustering** — groups similarly-formatted headings in scientific documents using weighted character-height and prefix differences. Evaluated on 633 PubMed documents using Zhang-Shasha tree edit distance.

- **Font clustering and classification in document images** (Coüasnon & Camillerapp, EUSIPCO 2000) [19]: Font analysis using individual character properties including font size to distinguish heading from body text.

- **HiPS: Hierarchical PDF Segmentation of Textbooks** (Wehnert, arXiv:2509.00909) [20]: Recent (2025) hierarchical approach for academic textbooks, demonstrating continued relevance of font-size clustering.

- **PDF text classification for information extraction** (PMC, 2016) [21]: Simple rule-based heuristic using font size to identify headings delivered 77.9% accuracy vs. SVM approaches, showing that even simple font-size rules have strong baseline performance.

### 3.2 Practical algorithm for Yole (PDFBox)

The PDFBox `TextPosition` class exposes [22]:
- `getFontSizeInPt()` — font size in points (accounts for transformation matrix scaling; preferred).
- `getFontSize()` — raw font size from "Tf" operator (may be 1.0 with scaling via CTM).
- `getHeight()` / `getHeightDir()` — character bounding box height.
- `isBold()` — (indirectly, via font name).

**Recommended heuristic algorithm:**

```
1. Strip all TextPositions per page → collect (text, fontSizeInPt, isBold, x, y).
2. Build a frequency histogram of fontSizePt values.
3. Identify body-text font size = mode of histogram (most frequent size).
4. Cluster sizes: sizes > body+2pt and appearing < 20% of total chars → heading candidates.
5. Sort heading candidates descending by size → assign H1, H2, H3, H4 in order.
6. Bold runs at body-text size → H3 or H4 (fallback heuristic).
7. Monospace font runs → code block candidates (font name contains "Mono", "Courier", "Code").
```

This matches the approach described in [18] and the OpenDataLoader PDF tool [23] which uses "heading tier detection via font statistics".

**Known limitation:** Some PDFs report incorrect font sizes due to font matrix scaling — using `getFontSizeInPt()` (which accounts for the text matrix horizontal scaling) is more reliable than `getFontSize()`. Note that even with this, PDFs created by certain tools (e.g., some LaTeX distributions) store text at size 1.0 with a 12× scaling matrix; `getFontSizeInPt()` normalises this correctly.

**References:**
[18] https://arxiv.org/abs/1809.01477
[19] https://www.eurasip.org/Proceedings/Eusipco/Eusipco2000/SESSIONS/WEDAM/PO4/CR1095.PDF
[20] https://arxiv.org/html/2509.00909
[21] https://pmc.ncbi.nlm.nih.gov/articles/PMC4893911/
[22] https://pdfbox.apache.org/docs/2.0.13/javadocs/org/apache/pdfbox/text/TextPosition.html
[23] https://github.com/opendataloader-project/opendataloader-pdf

---

## §4. flexmark-html2md-converter — license, quality, vs pandoc

### 4.1 Library overview

`flexmark-html2md-converter` is a module within the **flexmark-java** project [24]. It is described as an "Extensible HTML to Markdown Converter" and is part of the flexmark-all bundle.

**Maven coordinates:**
```xml
<dependency>
  <groupId>com.vladsch.flexmark</groupId>
  <artifactId>flexmark-html2md-converter</artifactId>
  <version>0.64.8</version>
</dependency>
```

Maven Central [25] | Libraries.io maintenance data [26] | GitHub activity [27].

### 4.2 License

**CORRECTION vs. spec assumption:** The flexmark-java project is licensed under **BSD-2-Clause** (not Apache 2.0) [28]. Copyright holders: Atlassian (2015-2016), Vladimir Schneider (2016-2023). BSD-2-Clause is fully compatible with Apache 2.0 projects; no legal barrier for Yole.

### 4.3 Quality assessment

Flexmark-java's strengths per its own documentation:
- CommonMark 0.28 compliant.
- Speed: optimized for parsing with many installed extensions.
- Extensibility: uniform options API across parser, renderer, and converters.
- `FlexmarkHtmlConverter` class has customization through `HtmlConverterOptions` [29].

Vs. pandoc:
- pandoc is the gold standard for document conversion quality. However, pandoc is a Haskell binary — it is **not usable as a Java library** and cannot be bundled in an Android APK.
- A GitHub issue in flexmark-java (#176) notes interest in "pandoc emulation mode" for docx-converter output, suggesting flexmark-java's authors are aware of pandoc's quality bar [30].
- For HTML→Markdown specifically, flexmark-html2md-converter handles: headings, bold/italic/strikethrough, ordered/unordered lists, blockquotes, tables, code blocks, links, images. It does not attempt to reconstruct complex layouts.
- The `copy-down` Java library [31] is an alternative that relies on JSoup for HTML parsing and maps HTML semantics to Markdown. It is simpler but less featureful than flexmark's converter.

**For Yole's HtmlImporter and for converting intermediate HTML produced by other importers (e.g., RTF → HTML → Markdown), flexmark-html2md-converter is the best pure-JVM option available.**

### 4.4 Maintenance status

The library is at version 0.64.8 (same as the Yole project's existing flexmark-java dependency [see `gradle/libs.versions.toml`]). The GitHub repository shows 1,814 commits [32]. The Fedora project packages it as `flexmark-java-html2md-converter` [33]. Activity log shows periodic commits in 2024–2025 [27].

Since Yole already ships `com.vladsch.flexmark:flexmark-all:0.64.8` (used in the hover-Markdown renderer from iter-62), the html2md-converter module is **already on the classpath at zero additional size cost**.

**References:**
[24] https://github.com/vsch/flexmark-java
[25] https://mvnrepository.com/artifact/com.vladsch.flexmark/flexmark-html2md-converter
[26] https://libraries.io/maven/com.vladsch.flexmark:flexmark-html2md-converter
[27] https://github.com/vsch/flexmark-java/activity
[28] https://github.com/vsch/flexmark-java (LICENSE.txt — BSD-2-Clause)
[29] https://github.com/vsch/flexmark-java/blob/master/flexmark-html2md-converter/src/main/java/com/vladsch/flexmark/html2md/converter/HtmlConverterOptions.java
[30] https://github.com/vsch/flexmark-java/issues/176
[31] https://github.com/furstenheim/copy-down
[32] https://github.com/vsch/flexmark-java (master branch commit count)
[33] https://packages.fedoraproject.org/pkgs/flexmark-java/flexmark-java-html2md-converter/

---

## §5. ODFDOM — classpath size and Android compatibility

### 5.1 Library overview

ODFDOM (`odfdom-java`) is the Document Foundation's Java implementation of the ODF API [34]. The TDF fork [35] is the active maintained version; the Apache ODF Toolkit [36] is **retired**.

**Maven coordinates:**
```xml
<dependency>
  <groupId>org.odftoolkit</groupId>
  <artifactId>odfdom-java</artifactId>
  <version>0.12.0</version>
</dependency>
```

Latest version: **0.12.0** (released December 21, 2023) [37].

### 5.2 JAR size

| Artefact | Size |
|---|---|
| `odfdom-java-0.12.0.jar` | **3.7 MB** |
| `odfdom-java-0.12.0-jar-with-dependencies.jar` | Larger; includes Xerces |

The slim JAR (3.7 MB) is reasonable for Desktop. Android compatibility is more complex.

### 5.3 Android compatibility issues

ODFDOM depends on **Apache Xerces-J** as its XML parser and DOM implementation [38]. Xerces-J is:
1. Not bundled with Android (which uses a forked XML implementation).
2. Conflicts with Android's `org.apache.harmony.xml` when both are on the classpath.

ODT files are ZIP archives containing XML. For the Yole Android `OdtImporter`, the pragmatic approach is:
- **Do not use ODFDOM on Android**. Instead, parse the ODT ZIP directly:
  - Open the ZIP, extract `content.xml`.
  - Parse it with Android's built-in XML parser (`XmlPullParser` or `DocumentBuilder`).
  - Walk `<text:h style:outline-level="N">` for headings, `<text:p>` for paragraphs, `<table:table>` for tables.

- **Use ODFDOM on Desktop (JVM)** — no Xerces conflict there. ODFDOM provides `OdfTextDocument`, `OdfParagraph`, `applyHeading()` etc. [39].

The heading/paragraph style API in ODFDOM [40]: heading styles use `style:name` attribute (e.g., `Heading_20_1` for "Heading 1"); paragraph styles belong to the Paragraph family.

### 5.4 Manual ODT ZIP parsing alternative

EPUB is a ZIP; ODT is a ZIP. Both are XML-inside-ZIP formats. A minimal ODT parser:

```kotlin
// On Android (desktopMain uses ODFDOM instead)
ZipInputStream(inputStream).use { zip ->
    var entry = zip.nextEntry
    while (entry != null) {
        if (entry.name == "content.xml") {
            parseOdtContent(zip.readBytes())
            break
        }
        entry = zip.nextEntry
    }
}
```

This avoids the Xerces dependency entirely on Android.

**References:**
[34] https://odftoolkit.org/odfdom/
[35] https://github.com/tdf/odftoolkit
[36] https://github.com/apache/odftoolkit (RETIRED)
[37] https://mvnrepository.com/artifact/org.odftoolkit/odfdom-java/0.12.0
[38] https://odftoolkit.org/odfdom/index.html
[39] https://github.com/tdf/odftoolkit/blob/master/odfdom/src/main/java/org/odftoolkit/odfdom/doc/OdfTextDocument.java
[40] https://www.langintro.com/odfdom_tutorials/create_odt.html

---

## §6. epublib status — maintenance, alternatives

### 6.1 Original epublib (psiegman)

`epublib` by Paul Siegmann [41]:
- **Last commit:** May 3, 2021.
- **GitHub releases:** "No releases published" (only SNAPSHOT versions referenced).
- **Open issues:** Ongoing issue reports in 2023–2024 with no responses.
- **Status: Effectively abandoned.** The author has not committed in 3+ years.

The core did run on Android explicitly (README states: "The core runs both on android and a standard java environment") [42].

### 6.2 epub4j (Document Node fork)

`epub4j` by documentnode [43]:
- Fork of epublib with improvements.
- Latest version: **4.2.3** on Maven Central [44].
- Maven coordinates: `io.documentnode:epub4j-core:4.2.3`
- More active than the original.

### 6.3 epub4j-kotlin (KMP port)

`epub4j-kotlin` by B1ays [45]:
- Kotlin Multiplatform library based on epub4j.
- Available on GitHub; not yet on Maven Central as of this writing.
- Promising for future KMP-native EPUB support.

### 6.4 Roll-own EPUB ZIP parsing — recommended approach

EPUB 3 is a ZIP archive [46] with a well-specified structure [47]:

```
mimetype                    (first file; ASCII "application/epub+zip")
META-INF/container.xml      (points to the OPF package document)
OEBPS/content.opf           (package document: manifest + spine)
OEBPS/*.xhtml               (content files, referenced by spine)
OEBPS/toc.ncx               (EPUB 2 navigation; deprecated in EPUB 3)
OEBPS/nav.xhtml             (EPUB 3 navigation document)
```

Algorithm for Yole's `EpubImporter`:

1. Open as `ZipFile` / `ZipInputStream`.
2. Parse `META-INF/container.xml` → locate OPF path.
3. Parse OPF: extract `<spine>` (ordered `<itemref>` list) + `<manifest>` (id → href map).
4. For each spine item: read XHTML, pass to `HtmlImporter` (which uses jsoup + flexmark-html2md-converter).
5. Concatenate per-chapter Markdown, separated by `---` horizontal rules.

This approach:
- Has **zero additional library dependencies** beyond jsoup and flexmark already needed for HtmlImporter.
- Works on **all JVM targets** (Android + Desktop) using standard `java.util.zip.ZipInputStream`.
- Avoids the abandoned upstream and the transitive deps of epub4j.

EPUB `application/epub+zip` is IANA-registered (RFC 4839 / EPUB 3 supersedes) [48].

**References:**
[41] https://github.com/psiegman/epublib
[42] https://github.com/psiegman/epublib/blob/master/README.md
[43] https://github.com/documentnode/epub4j
[44] https://mvnrepository.com/artifact/io.documentnode/epub4j-core/4.2.1
[45] https://github.com/B1ays/epub4j-kotlin
[46] https://www.edrlab.org/open-standards/anatomy-of-an-epub-3-file/
[47] https://www.w3.org/TR/epub-33/
[48] https://www.iana.org/assignments/media-types/application/epub+zip

---

## §7. Android intent-filter MIME types — exact strings for all 6 formats

Android intent-filter `<data android:mimeType="...">` strings are **case-sensitive** and must be lowercase [49]. RTF has multiple MIME type aliases — declare all. The IANA-registered types [50][51] are canonical.

### 7.1 Exact MIME type strings

| Format | Extension | MIME type(s) for intent-filter | Notes |
|---|---|---|---|
| DOCX | `.docx` | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | Single canonical type [52] |
| PDF | `.pdf` | `application/pdf` | Single canonical type [53] |
| RTF | `.rtf` | `text/rtf` AND `application/rtf` AND `application/x-rtf` AND `text/richtext` | Multiple aliases exist; declare all 4 [54] |
| HTML | `.html`, `.htm` | `text/html` | Single canonical type |
| ODT | `.odt` | `application/vnd.oasis.opendocument.text` | IANA-registered [55] |
| EPUB | `.epub` | `application/epub+zip` | IANA-registered [48] |

### 7.2 Android Manifest structure

```xml
<!-- In AndroidManifest.xml, inside the Activity receiving shared files -->
<intent-filter>
    <action android:name="android.intent.action.SEND" />
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <!-- DOCX -->
    <data android:mimeType=
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" />
    <!-- PDF -->
    <data android:mimeType="application/pdf" />
    <!-- RTF — all aliases -->
    <data android:mimeType="text/rtf" />
    <data android:mimeType="application/rtf" />
    <data android:mimeType="application/x-rtf" />
    <data android:mimeType="text/richtext" />
    <!-- HTML -->
    <data android:mimeType="text/html" />
    <!-- ODT -->
    <data android:mimeType="application/vnd.oasis.opendocument.text" />
    <!-- EPUB -->
    <data android:mimeType="application/epub+zip" />
</intent-filter>
```

### 7.3 File picker (ACTION_OPEN_DOCUMENT)

For the FILES-tab Import button, use `ACTION_OPEN_DOCUMENT` with `EXTRA_MIME_TYPES` [56]:

```kotlin
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "*/*"
    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/pdf",
        "text/rtf", "application/rtf", "application/x-rtf", "text/richtext",
        "text/html",
        "application/vnd.oasis.opendocument.text",
        "application/epub+zip"
    ))
}
startActivityForResult(intent, REQUEST_IMPORT)
```

`ACTION_OPEN_DOCUMENT` grants persistent URI permissions (survives process restart); `ACTION_GET_CONTENT` grants temporary permissions only. Use `ACTION_OPEN_DOCUMENT` [57].

Reading the file: `contentResolver.openInputStream(uri)` → `ByteArray` → pass to `ImporterRegistry` [58].

**References:**
[49] https://developer.android.com/guide/components/intents-filters
[50] https://www.iana.org/assignments/media-types/application/vnd.oasis.opendocument.text
[51] https://mimetype.io/application/vnd.oasis.opendocument.text
[52] https://developer.android.com/guide/components/intents-common
[53] https://www.c-sharpcorner.com/article/how-to-make-your-pdf-viewer-handle-pdf-files-with-intent-filters-in-android/
[54] https://www.b4x.com/android/forum/threads/manifests-intent-filter-for-mimetype-text.102328/
[55] https://www.iana.org/assignments/media-types/application/vnd.oasis.opendocument.text
[56] http://android-er.blogspot.com/2015/09/using-intentactionopendocument-with.html
[57] https://developer.android.com/training/data-storage/shared/documents-files
[58] https://developer.gini.net/gini-vision-lib-android/html/guide-for-open-with.html

---

## §8. Compose Desktop drag-drop — Modifier.dragAndDropTarget per Compose 1.7

### 8.1 API evolution timeline

| Version | API | Status |
|---|---|---|
| < 1.7.0 | `Modifier.onExternalDrag` | Deprecated in 1.7.0 |
| 1.7.0 | `Modifier.dragAndDropTarget` + `Modifier.dragAndDropSource` | New stable replacement |
| 1.8.0 | `Modifier.onExternalDrag` | **Removed** |
| 1.8.0 | iOS drag-and-drop support added | `dragAndDropTarget` / `dragAndDropSource` on iOS |

Yole uses Compose Multiplatform 1.7.3. The correct API is `Modifier.dragAndDropTarget` [59].

### 8.2 Platform support matrix

| Platform | `dragAndDropTarget` | Notes |
|---|---|---|
| Android | Supported | Full API |
| Desktop (JVM) | Supported | Uses AWT `DataFlavor` via `awtTransferable` |
| iOS | Supported (from 1.8.0) | `UIDragItem`-based; String and NSObject types only |
| Web/Wasm | **Not supported** | "Stay tuned for future releases" [60] |

Since Yole targets Compose 1.7.3 (not 1.8.x), iOS drag-drop is not yet available in the version in use. Web/Wasm has no drag-drop support in either version.

### 8.3 Desktop file drop implementation

For accepting dropped files (documents) on Desktop:

```kotlin
@OptIn(ExperimentalFoundationApi::class)
val dropTarget = remember {
    object : DragAndDropTarget {
        override fun onDrop(event: DragAndDropEvent): Boolean {
            val transferable = event.awtTransferable
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @Suppress("UNCHECKED_CAST")
                val files = transferable.getTransferData(
                    DataFlavor.javaFileListFlavor
                ) as List<java.io.File>
                files.firstOrNull()?.let { file ->
                    // dispatch import
                    onFileDropped(file)
                }
                return true
            }
            return false
        }
    }
}

Box(
    modifier = Modifier
        .fillMaxSize()
        .dragAndDropTarget(
            shouldStartDragAndDrop = { true },
            target = dropTarget
        )
)
```

The `DataFlavor.javaFileListFlavor` approach is Swing-compatible and works with all OS-native file drag from Finder (macOS), Nautilus (Linux), Explorer (Windows) [61].

### 8.4 Web/Wasm alternative

Since `dragAndDropTarget` is unsupported on Web, the Import button (FILES tab) via the browser File API is the sole entry point for Web. No drag-drop UI should be shown on Wasm targets [60].

### 8.5 iOS alternative

For iOS (stub currently), future support could use `UIDragInteraction` at the UIKit layer or wait for Compose 1.8.x iOS drag-drop stabilisation. Since iOS importers are stubs in iter-64, no drag-drop UI is shown on iOS.

**References:**
[59] https://kotlinlang.org/docs/multiplatform/compose-drag-drop.html
[60] https://www.jetbrains.com/help/kotlin-multiplatform-dev/whats-new-compose-180.html
[61] https://github.com/g3th/Compose-Desktop-Drag-And-Drop

---

## §9. APK size impact projection — all 6 library JARs after R8

### 9.1 Raw JAR sizes (pre-shrink)

| Library | Version | JAR size (raw) | Target platform |
|---|---|---|---|
| `poi-ooxml` | 5.5.1 | ~1.9 MB | Android + Desktop |
| `poi-ooxml-lite` | 5.5.1 | ~6 MB | Android + Desktop |
| `pdfbox-android` | 2.0.27.0 | ~7 MB (incl. fontbox) | Android only |
| `pdfbox` | 3.0.7 | ~3.5 MB | Desktop only |
| `odfdom-java` | 0.12.0 | 3.7 MB | Desktop only (see §5) |
| `flexmark-html2md-converter` | 0.64.8 | ~0 (already bundled via flexmark-all) | Android + Desktop |
| `rtfparserkit` | 1.16.0 | ~0.2 MB | Android + Desktop |
| `jsoup` | latest | ~0.5 MB | Android + Desktop |

**Not used on Android:** `pdfbox` 3.0, `odfdom-java`. On Android, ODT is parsed via raw ZIP XML (§5.3), eliminating the Xerces conflict.

### 9.2 R8 shrinking estimates

R8 in full-mode [62] removes all unreachable code from library JARs. Observed ratios from community reports:

- `pdfbox-android`: raw 7 MB → after R8 ~**4–5 MB** (font resources not shrinkable [63]).
- `poi-ooxml` + `poi-ooxml-lite`: combined ~8 MB raw → R8 reduces class code; schema keep-rules (§1.3) force retention of XMLBeans/schema classes → estimated **~5–6 MB** in DEX.
- `rtfparserkit`: ~0.2 MB → negligible.
- `jsoup`: ~0.5 MB → after R8 ~**0.2 MB** (only used APIs retained).

### 9.3 Total Android APK size increase estimate

| Component | Estimated DEX contribution |
|---|---|
| pdfbox-android (code + font resources) | +4–5 MB |
| poi-ooxml + poi-ooxml-lite | +5–6 MB |
| jsoup | +0.2 MB |
| rtfparserkit | +0.1 MB |
| flexmark-html2md-converter | ~0 (already bundled) |
| **Total estimate** | **+9–11 MB** |

This is a significant but acceptable increase for a document-import feature. The Yole base APK (pre-import) is not known precisely from this research; the 9–11 MB addend is the marginal cost.

**Mitigation strategies:**
1. Deliver import libraries as an **on-demand dynamic feature module** (Play Feature Delivery [64]) so they are only downloaded when the user first invokes Import.
2. If dynamic modules are not feasible, use `poi-ooxml-lite` (not `poi-ooxml-full`) and exclude unused schemas in the keep rules.
3. Use `pdfbox-android`'s `PDFTextStripper` only (not the rendering pipeline) — font loading is lazy; documents without embedded fonts will not trigger full fontbox load.

**⚠️ Gap:** No exact measured APK size data for the combined Yole baseline + these libraries after R8 is available from this research. The estimate (+9–11 MB) is based on community reports of individual libraries, not an end-to-end Yole APK build. Actual measurement should be performed in Phase 1 and reported.

**References:**
[62] https://developer.android.com/studio/build/shrink-code
[63] https://github.com/TomRoush/PdfBox-Android/issues/103
[64] https://developer.android.com/guide/playcore/feature-delivery

---

## §10. RTF parsing — library options

The spec mentions `javax.swing.text.rtf.RTFEditorKit` as one RTF option. Research surfaced a better alternative.

### 10.1 javax.swing.text.rtf.RTFEditorKit

- Part of JDK (java.desktop module).
- Available on Desktop JVM targets.
- **Not available on Android** — Android does not ship `javax.swing.*`. [65]
- RTF support quality: described as "not written by the Swing team" with plans to improve; limited in practice.

### 10.2 rtfparserkit (recommended for Android)

`rtfparserkit` by Jon Iles [66]:
- **License:** Apache 2.0.
- **Last stable release:** 1.16.0 (February 10, 2021). No commits since.
- **Maven coordinates:** `com.github.joniles:rtfparserkit:1.16.0` (via JitPack).
- Provides: `StandardRtfParser`, `RtfStreamSource`, `StringTextConverter` for plain-text extraction.
- Handles: character encoding, Unicode, event-driven listener model.
- **Android compatibility:** Pure Java, no Swing/AWT deps. Works on Android.

Alternative for RTF: **Apache POI HWPF** (`poi-scratchpad` module) reads `.doc` (old binary Word) and includes limited RTF support via `RTFParser`. However, HWPF is for binary `.doc`, not modern RTF. Using rtfparserkit is cleaner.

### 10.3 RTF → Markdown pipeline

RTF is a complex format. Recommended pipeline:

```
RTF bytes
→ rtfparserkit StandardRtfParser + HTMLListener (produces HTML)
→ jsoup (clean + normalize HTML)
→ flexmark-html2md-converter
→ Markdown string
```

The `HTMLListener` in rtfparserkit converts RTF paragraph styles, bold/italic runs, and basic table cells to HTML. For heading detection in RTF, the `\outlinelevel N` control word maps to heading levels (0=H1, 1=H2, etc.).

**References:**
[65] https://github.com/kschroeer/rtf-html-java
[66] https://github.com/joniles/rtfparserkit

---

## §11. DOCX structural extraction — Apache POI XWPF

### 11.1 XWPFDocument API

For DOCX import, the Apache POI XWPF API [67] is used:

```kotlin
val doc = XWPFDocument(inputStream)
for (paragraph in doc.paragraphs) {
    val style = paragraph.style  // e.g. "Heading1", "Heading2", "Normal"
    val outlineLevel = paragraph.ctp.pPr?.outlineLvl?.getVal()?.intValue() ?: -1
    val text = paragraph.text
    val isBold = paragraph.runs.any { it.isBold }
}
```

Heading styles in DOCX [68]:
- Style ID `Heading1` → H1, `Heading2` → H2, etc.
- `outlinelevel` attribute in paragraph properties confirms heading level independently of style name (useful when non-standard style names are used).

### 11.2 Tables

`doc.tables` → `XWPFTable` → rows → cells. Each cell contains a list of `XWPFParagraph`. Mapping to Markdown pipe-table is straightforward.

### 11.3 Images

XWPF does not expose images directly per paragraph position. `XWPFPictureData` is accessible via `doc.allPictures`. For the ImageExtractor component, extract all pictures and embed as base64 data URIs or save to an `assetsDir`. [69]

**References:**
[67] https://poi.apache.org/components/document/quick-guide-xwpf.html
[68] https://poi.apache.org/components/document/
[69] https://www.concretepage.com/apache-api/apache-poi-xwpf-read-ms-word-docx-header-footer-paragraph-table-example

---

## §12. HTML import — jsoup + flexmark integration

### 12.1 Pipeline

```
HTML bytes (from HtmlImporter or intermediate step)
→ Jsoup.parse(html, baseUri)          // DOM normalization
→ jsoup cleanup: .select("script, style").remove()
→ doc.html()                           // normalized HTML string
→ FlexmarkHtmlConverter.convert(html)  // → Markdown
```

Jsoup [70] is MIT-licensed, pure Java, Android-compatible, API 1+. Its selector and cleaning APIs handle malformed HTML well.

`FlexmarkHtmlConverter` processes: `<h1>`–`<h6>`, `<p>`, `<ul>/<ol>/<li>`, `<table>`, `<code>/<pre>`, `<blockquote>`, `<a>`, `<img>`, `<strong>/<em>/<del>`. Custom node renderers can be registered for exotic tags.

### 12.2 Base URI handling

HTML imported from docx (via XWPF's embedded HTML serialization) or EPUB (XHTML content) may have relative URIs. Jsoup's `parse(html, baseUri)` resolves these. For EPUB content, the `baseUri` is the EPUB package root.

**References:**
[70] https://jsoup.org/
[71] https://github.com/jhy/jsoup

---

## §13. EPUB internal structure and roll-own parser details

### 13.1 EPUB 3 container specification

EPUB 3.3 [72] mandates:
1. `mimetype` file at root (ASCII `application/epub+zip`, uncompressed).
2. `META-INF/container.xml` identifies the Package Document (OPF file).
3. Package Document (`.opf`) contains `<manifest>` (all resources) and `<spine>` (reading order).
4. Navigation Document (`nav.xhtml`) replaces EPUB 2's `toc.ncx`.

EPUB 2 `toc.ncx` is deprecated in EPUB 3 but must be tolerated for backward compatibility (many legacy EPUBs in the wild still use NCX only) [73].

### 13.2 Parsing algorithm detail

```kotlin
fun parseEpub(bytes: ByteArray): ImportedDocument {
    ZipInputStream(bytes.inputStream()).use { zip ->
        val entries = mutableMapOf<String, ByteArray>()
        var entry = zip.nextEntry
        while (entry != null) {
            entries[entry.name] = zip.readBytes()
            entry = zip.nextEntry
        }
        // 1. locate OPF
        val containerXml = entries["META-INF/container.xml"]
            ?: error("Missing container.xml")
        val opfPath = parseContainerXml(containerXml)  // XPath: rootfile/@full-path

        // 2. parse OPF
        val opfBytes = entries[opfPath] ?: error("Missing OPF: $opfPath")
        val (manifest, spine) = parseOpf(opfBytes)
        // manifest: id → (href, mediaType)
        // spine: ordered list of idref

        // 3. extract content
        val opfDir = opfPath.substringBeforeLast("/")
        val markdownParts = spine.mapNotNull { idref ->
            val href = manifest[idref]?.href ?: return@mapNotNull null
            val fullPath = if (opfDir.isNotEmpty()) "$opfDir/$href" else href
            val html = entries[fullPath]?.decodeToString() ?: return@mapNotNull null
            htmlToMarkdown(html)  // jsoup + flexmark
        }
        return ImportedDocument(
            sourceFormat = "epub",
            markdown = markdownParts.joinToString("\n\n---\n\n")
        )
    }
}
```

### 13.3 Nav document for TOC → Markdown headings

EPUB 3 `<nav epub:type="toc">` contains the table of contents as nested `<ol>/<li>/<a>` elements. This can be parsed to prepend a Markdown TOC to the imported document.

**References:**
[72] https://www.w3.org/TR/epub-33/
[73] https://idpf.org/epub/30/spec/epub30-publications-20111011.html
[74] https://www.edrlab.org/open-standards/anatomy-of-an-epub-3-file/

---

## §14. Desktop drag-drop — per-OS file type handling

On Desktop, when the user drags a file from the OS file manager onto the Yole window, `DataFlavor.javaFileListFlavor` provides `List<java.io.File>` [75]. The `File.extension` determines which importer to invoke via `ImporterRegistry`.

The Compose Desktop `DragAndDropTarget.onDrop()` receives a `DragAndDropEvent` whose `awtTransferable` field is the underlying Swing `Transferable`. This bridges the Compose DnD system to Java's AWT data transfer [76].

OS-specific considerations:
- **macOS:** Finder drag works out-of-the-box with `javaFileListFlavor`.
- **Linux:** Nautilus uses `text/uri-list` as an alternative; `javaFileListFlavor` is usually synthesized by the JVM from the URI list. If `javaFileListFlavor` is not supported, fall back to parsing `DataFlavor` with MIME `text/uri-list`.
- **Windows:** Explorer drag uses `javaFileListFlavor` natively.

**References:**
[75] https://docs.oracle.com/javase/tutorial/uiswing/dnd/dataflavor.html
[76] https://github.com/g3th/Compose-Desktop-Drag-And-Drop/blob/main/README.MD

---

## §15. iOS and Web stub rationale

### 15.1 iOS

On iOS, Kotlin/Native cannot load JVM-only libraries (POI, PDFBox, ODFDOM). The only available parsing approach would be:
- Swift-side bridging via `expect/actual` to use `NSAttributedString` (RTF, HTML), `PDFKit` (PDF), or third-party Swift libraries.
- This is out of scope for iter-64 per the spec.

Stub implementation per CONST-035 anti-bluff rules: `Result.failure(ImportError.NotSupported("iOS: document import requires JVM"))`.

### 15.2 Web/Wasm

- JVM libraries compile to JVM bytecode; Wasm target does not support this.
- Browser File API can provide bytes; parsing would require Wasm-compiled native libraries or pure Kotlin Wasm implementations.
- No pure-Kotlin DOCX/PDF/ODT parsers suitable for Wasm exist as of this research.
- Stub: `Result.failure(ImportError.NotSupported("Web: document import not yet supported"))`.

Future option: Emscripten-compiled `poppler` for PDF, `libxml2` for ODT — but this requires native Wasm modules beyond iter-64 scope.

---

## §16. KMP architecture patterns for expect/actual importers

### 16.1 Interface placement

The `DocumentImporter` interface and `ImportedDocument` data class are `commonMain` — no platform types leak in [77]. Platform-specific importers are `expect class` in `commonMain` with `actual class` in `androidMain` / `desktopMain` / `iosMain` / `wasmJsMain`.

### 16.2 ByteArray-based API

Using `ByteArray` as the input type (not `java.io.File` or `java.io.InputStream`) ensures commonMain-safe signatures. Platform code converts platform file handles to `ByteArray` before calling into the importer.

Android `ContentResolver.openInputStream(uri)?.readBytes()` → `ByteArray`.
Desktop `java.io.File.readBytes()` → `ByteArray`.

### 16.3 Pattern mirror: ParserRegistry → ImporterRegistry

Iter-58 established `ParserRegistry.registerLazy("formatId") { Parser() }` [see `format/` module]. `ImporterRegistry` mirrors this: `registerLazy("docx") { DocxImporter() }` in each platform's `init` block.

**References:**
[77] https://proandroiddev.com/expect-actual-mechanism-in-kotlin-multiplatform-explained-a91e7d85af4e

---

## §17. RTF MIME type disambiguation and file identification

Since RTF has 4 possible MIME types, Yole's `ImporterRegistry` should also match by file extension (`.rtf`) as a fallback when the MIME type is ambiguous. The `ImportShareIntentHandler` on Android should check:

1. `intent.type` against all 4 RTF MIME variants.
2. If `intent.type == "*/*"` (some apps send no MIME type), fall back to URI path extension.

**References:**
[78] https://developer.android.com/guide/topics/manifest/data-element

---

## §18. Conversion quality — known fidelity limits per format

| Format | High-fidelity elements | Known fidelity limits |
|---|---|---|
| DOCX | Headings (via style ID), bold/italic, ordered/unordered lists, tables, hyperlinks, footnotes | Images are position-approximate; complex SmartArt/shapes lost |
| PDF | Text, headings (via font-size heuristic), tables (basic), hyperlinks | Scanned PDFs (image-only) produce empty output; complex multi-column lost; math formulas garbled |
| RTF | Headings (via `\outlinelevel`), bold/italic, tables (limited), lists | Images in RTF lost (format embeds binary blobs; extraction complex) |
| HTML | Full fidelity for semantic HTML; headings, lists, tables, code | Inline CSS layouts produce garbage; JavaScript-rendered content not extracted |
| ODT | Headings (via style family), bold/italic, lists, tables | Complex page-layout elements lost; embedded macros ignored |
| EPUB | Full fidelity per chapter (each XHTML → HtmlImporter); TOC preserved as Markdown TOC | DRM-encrypted EPUBs fail at ZIP parse level; fixed-layout EPUB (comic/textbook) produces poor output |

**Gap marker (CONST-035 honesty):** No end-to-end conversion fidelity benchmarks for this pipeline have been measured. The above table is derived from library documentation and format specifications, not empirical testing. Empirical fidelity testing should occur in Phase 1 before the ANTI-BLUFF challenge is written.

---

## §19. Summary table — 9 open questions answered

| # | Question | Answer |
|---|---|---|
| 1 | Apache POI on Android: `poi-ooxml-lite` vs `poi-ooxml-full`; R8 keep rules | Use `poi-ooxml-lite` (~6 MB); require 8 keep-rule directives (§1.3); exclude `stax:stax-api`; enable multiDex |
| 2 | PDFBox 3.0 stable; Android compatibility | PDFBox 3.0.7 stable (Java 8+); Android uses community port `pdfbox-android:2.0.27.0` (tracks 2.x, last release Jan 2023, minimally maintained) |
| 3 | PDF heading detection accuracy; published heuristics | ML baseline 96.95% accuracy (arXiv:1809.01477); practical heuristic: `getFontSizeInPt()` histogram → frequency-mode = body text; outlier larger sizes → heading tiers |
| 4 | flexmark-html2md-converter license and quality | **BSD-2-Clause** (not Apache 2.0); already in Yole's classpath (0 extra cost); best pure-JVM HTML→MD converter available |
| 5 | ODFDOM size and Android compat | 3.7 MB JAR; **not Android-compatible** (Xerces dependency); use raw ZIP+XML parsing on Android instead |
| 6 | epublib status | Abandoned (last commit May 2021); use roll-own EPUB ZIP parser (zero new deps) via jsoup + flexmark |
| 7 | Android intent-filter MIME strings | DOCX: `application/vnd.openxmlformats-officedocument.wordprocessingml.document`; PDF: `application/pdf`; RTF: 4 aliases; HTML: `text/html`; ODT: `application/vnd.oasis.opendocument.text`; EPUB: `application/epub+zip` |
| 8 | Compose Desktop drag-drop API | Use `Modifier.dragAndDropTarget` (not deprecated `onExternalDrag`); works on Android/Desktop/iOS (1.8+); **not on Web/Wasm** |
| 9 | APK size impact (6 library JARs, after R8) | Estimated +9–11 MB net (pdfbox-android +4–5 MB; poi +5–6 MB; others negligible); dynamic feature module recommended to defer download |

---

## §20. Complete URL reference index

All sources cited in this report:

1. https://poi.apache.org/components/
2. https://poi.apache.org/help/faq.html
3. https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml-full/5.3.0
4. https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml/5.3.0
5. https://poi.apache.org/download.html
6. https://www.mail-archive.com/announce@apache.org/msg09302.html
7. https://github.com/centic9/poi-on-android
8. https://github.com/SUPERCILEX/poi-android
9. https://github.com/centic9/poi-on-android/blob/master/poitest/proguard-rules.pro
10. https://issuetracker.google.com/issues/295349278
11. https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html
12. https://pdfbox.apache.org/
13. https://pdfbox.apache.org/3.0/migration.html
14. https://pdfbox.apache.org/3.0/migration.html
15. https://github.com/TomRoush/PdfBox-Android
16. https://github.com/jmaksuta/pdfbox-android
17. https://github.com/TomRoush/PdfBox-Android/issues/103
18. https://arxiv.org/abs/1809.01477
19. https://www.eurasip.org/Proceedings/Eusipco/Eusipco2000/SESSIONS/WEDAM/PO4/CR1095.PDF
20. https://arxiv.org/html/2509.00909
21. https://pmc.ncbi.nlm.nih.gov/articles/PMC4893911/
22. https://pdfbox.apache.org/docs/2.0.13/javadocs/org/apache/pdfbox/text/TextPosition.html
23. https://github.com/opendataloader-project/opendataloader-pdf
24. https://github.com/vsch/flexmark-java
25. https://mvnrepository.com/artifact/com.vladsch.flexmark/flexmark-html2md-converter
26. https://libraries.io/maven/com.vladsch.flexmark:flexmark-html2md-converter
27. https://github.com/vsch/flexmark-java/activity
28. https://github.com/vsch/flexmark-java (LICENSE.txt)
29. https://github.com/vsch/flexmark-java/blob/master/flexmark-html2md-converter/src/main/java/com/vladsch/flexmark/html2md/converter/HtmlConverterOptions.java
30. https://github.com/vsch/flexmark-java/issues/176
31. https://github.com/furstenheim/copy-down
32. https://github.com/vsch/flexmark-java
33. https://packages.fedoraproject.org/pkgs/flexmark-java/flexmark-java-html2md-converter/
34. https://odftoolkit.org/odfdom/
35. https://github.com/tdf/odftoolkit
36. https://github.com/apache/odftoolkit
37. https://mvnrepository.com/artifact/org.odftoolkit/odfdom-java/0.12.0
38. https://odftoolkit.org/odfdom/index.html
39. https://github.com/tdf/odftoolkit/blob/master/odfdom/src/main/java/org/odftoolkit/odfdom/doc/OdfTextDocument.java
40. https://www.langintro.com/odfdom_tutorials/create_odt.html
41. https://github.com/psiegman/epublib
42. https://github.com/psiegman/epublib/blob/master/README.md
43. https://github.com/documentnode/epub4j
44. https://mvnrepository.com/artifact/io.documentnode/epub4j-core/4.2.1
45. https://github.com/B1ays/epub4j-kotlin
46. https://www.edrlab.org/open-standards/anatomy-of-an-epub-3-file/
47. https://www.w3.org/TR/epub-33/
48. https://www.iana.org/assignments/media-types/application/epub+zip
49. https://developer.android.com/guide/components/intents-filters
50. https://www.iana.org/assignments/media-types/application/vnd.oasis.opendocument.text
51. https://mimetype.io/application/vnd.oasis.opendocument.text
52. https://developer.android.com/guide/components/intents-common
53. https://www.c-sharpcorner.com/article/how-to-make-your-pdf-viewer-handle-pdf-files-with-intent-filters-in-android/
54. https://www.b4x.com/android/forum/threads/manifests-intent-filter-for-mimetype-text.102328/
55. https://www.iana.org/assignments/media-types/application/vnd.oasis.opendocument.text
56. http://android-er.blogspot.com/2015/09/using-intentactionopendocument-with.html
57. https://developer.android.com/training/data-storage/shared/documents-files
58. https://developer.gini.net/gini-vision-lib-android/html/guide-for-open-with.html
59. https://kotlinlang.org/docs/multiplatform/compose-drag-drop.html
60. https://www.jetbrains.com/help/kotlin-multiplatform-dev/whats-new-compose-180.html
61. https://github.com/g3th/Compose-Desktop-Drag-And-Drop
62. https://developer.android.com/studio/build/shrink-code
63. https://github.com/TomRoush/PdfBox-Android/issues/103
64. https://developer.android.com/guide/playcore/feature-delivery
65. https://github.com/kschroeer/rtf-html-java
66. https://github.com/joniles/rtfparserkit
67. https://poi.apache.org/components/document/quick-guide-xwpf.html
68. https://poi.apache.org/components/document/
69. https://www.concretepage.com/apache-api/apache-poi-xwpf-read-ms-word-docx-header-footer-paragraph-table-example
70. https://jsoup.org/
71. https://github.com/jhy/jsoup
72. https://www.w3.org/TR/epub-33/
73. https://idpf.org/epub/30/spec/epub30-publications-20111011.html
74. https://www.edrlab.org/open-standards/anatomy-of-an-epub-3-file/
75. https://docs.oracle.com/javase/tutorial/uiswing/dnd/dataflavor.html
76. https://github.com/g3th/Compose-Desktop-Drag-And-Drop/blob/main/README.MD
77. https://proandroiddev.com/expect-actual-mechanism-in-kotlin-multiplatform-explained-a91e7d85af4e
78. https://developer.android.com/guide/topics/manifest/data-element
79. https://lists.apache.org/thread/4jb78qfqmbcdrnvk64k2s8g53hg9yh12
80. https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml-lite
81. https://jar-download.com/artifacts/org.apache.poi/poi-ooxml
82. https://mvnrepository.com/artifact/org.apache.poi/poi-ooxml-full/5.3.0
83. https://github.com/apache/poi
84. https://mvnrepository.com/artifact/com.tom-roush/pdfbox-android/2.0.27.0
85. https://central.sonatype.com/artifact/com.tom-roush/pdfbox-android
86. https://www.researchgate.net/publication/327405802_A_Supervised_Learning_Approach_For_Heading_Detection
87. https://www.researchgate.net/publication/338460776_A_supervised_learning_approach_for_heading_detection
88. https://www.researchgate.net/publication/228990634_Font_clustering_and_classification_in_document_images
89. https://github.com/vsch/flexmark-java/tree/master/flexmark-html2md-converter
90. https://central.sonatype.com/artifact/com.vladsch.flexmark/flexmark-html2md-converter
91. https://jarcasting.in/artifacts/org.odftoolkit/odfdom-java/
92. https://mvnrepository.com/artifact/org.odftoolkit
93. https://odftoolkit.org/ReleaseNotes.html
94. https://www.langintro.com/odfdom_tutorials/quick_odt.html
95. https://www.javaspring.net/javaexamples/extracting_content_from_odf_files_using_java/
96. https://github.com/psiegman/epublib/issues
97. https://jitpack.io/p/ephemerial/epublib
98. https://idpf.org/epub/301/spec/epub-publications-20140626.html
99. https://w3c.github.io/epub-specs/archive/epub32/spec/epub-packages.html
100. https://developer.android.com/training/sharing/receive
101. https://guides.codepath.com/android/Sharing-Content-with-Intents
102. https://vadzimv.dev/2021/01/01/android-pick-file.html
103. https://blog.jetbrains.com/kotlin/2024/10/compose-multiplatform-1-7-0-released/
104. https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.7.0
105. https://medium.com/@mmartosdev/web-based-drag-and-drop-in-compose-multiplatform-b4d7e2a0529d
106. https://github.com/JetBrains/compose-multiplatform/issues/4235
107. https://github.com/JetBrains/compose-multiplatform/issues/4101
108. https://github.com/JetBrains/compose-multiplatform/issues/3309
109. https://newreleases.io/project/github/JetBrains/compose-multiplatform/release/v1.7.0
110. https://developer.android.com/build/shrink-code
111. https://medium.com/codex/optimizing-apk-size-in-android-660362504c1b
112. https://carrion.dev/en/posts/reducing-app-size/
113. https://dev.to/wise4rmgod/reducing-apk-size-in-android-made-easy-with-r8-d31
114. https://medium.com/better-programming/shrink-your-android-app-with-r8-afe17c4d393
115. https://github.com/joniles/rtfparserkit/blob/master/src/main/java/com/rtfparserkit/converter/text/StringTextConverter.java
116. https://github.com/ranaparamveer/RTF-Parser
117. https://github.com/apache/tika
118. https://github.com/nilenso/markdown2docx
119. https://arxiv.org/html/2501.17887v1 (Docling paper)
120. https://infoworld.com/article/3963991/markitdown-microsofts-open-source-tool-for-markdown-conversion.html

---

*End of report — 120+ URL citations, 20 sections.*
