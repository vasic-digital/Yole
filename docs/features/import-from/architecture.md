<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Import From — Architecture

**Feature:** iter-64 / v1.7.0  
**Module:** `shared/src/*/kotlin/digital/vasic/yole/import_/`  
**Audience:** contributors extending or maintaining the import system

---

## 1. Core abstractions

### `DocumentImporter` (commonMain)

```kotlin
interface DocumentImporter {
    val supportedExtensions: Set<String>
    val sourceFormat: String
    suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument>
}
```

- `bytes` is the raw file content; no file-system access inside importers.
- `sourceFormat` is a short lowercase tag (`"docx"`, `"html"`, `"rtf"`, `"odt"`, `"pdf"`, `"epub"`).
- Returns `Result<ImportedDocument>` — never throws (except `CancellationException` which must always be rethrown).

### `ImportedDocument` (commonMain)

```kotlin
data class ImportedDocument(
    val markdownContent: String,
    val sourceFormat: String,
    val warnings: List<ImportWarning> = emptyList()
)
```

- `markdownContent` is CommonMark-compatible Markdown.
- `warnings` carries `ImportWarning(severity: Severity, message: String)` entries; severity is `Info` or `Warning`.

### `ImportError` (commonMain)

Sealed class with three variants:

- `ImportError.NotSupported(format, platform)` — honest stub for unsupported platform/format combinations.
- `ImportError.Malformed(format, cause)` — structurally invalid or unreadable input.
- `ImportError.IoError(message, cause)` — I/O failure during byte reading.

---

## 2. Importer registry

`ImporterRegistry` (commonMain) holds the default set of importers:

```kotlin
object ImporterRegistry {
    fun default(): List<DocumentImporter>  // 6 importers
    fun forExtension(ext: String): DocumentImporter?
    fun forMimeType(mime: String): DocumentImporter?
}
```

`default()` returns all six importers in declaration order. `forExtension` and `forMimeType` perform
linear scan — lists are small (≤ 6) so no map lookup is needed.

---

## 3. Adding a new importer

1. Create `shared/src/commonMain/kotlin/digital/vasic/yole/import_/FooImporter.kt`:

```kotlin
expect class FooImporter() : DocumentImporter {
    override val supportedExtensions: Set<String>
    override val sourceFormat: String
    override suspend fun import(bytes: ByteArray, fileName: String): Result<ImportedDocument>
}
```

2. Implement actuals in each source set:
   - `desktopMain` — full JVM implementation.
   - `androidMain` — full JVM or honest stub with `ImportError.NotSupported("foo", "Android")`.
   - `iosMain` — honest stub: `ImportError.NotSupported("foo", "iOS")`.
   - `wasmJsMain` — honest stub: `ImportError.NotSupported("foo", "Web")`.

3. Register in `ImporterRegistry.default()`.

4. Add tests in `shared/src/desktopTest/kotlin/digital/vasic/yole/import_/FooImporterTest.kt`.
   Required minimum: round-trip test with synthesised bytes, garbage-bytes malformed test, extension
   test, mutation guard test (inline stub proves the real test cannot pass against a no-op importer).

5. Update `docs/features/import-from/supported-formats.md`.

---

## 4. Conversion helpers (commonMain)

All helpers live in `shared/src/commonMain/kotlin/digital/vasic/yole/import_/conversion/`.
They are pure functions with no I/O and can be used in any source set including iOS and Web (for
future native implementations).

| Helper | Purpose |
|--------|---------|
| `HeadingDetector` | Maps font-size rank to ATX heading level (H1–H6); returns `null` for body text |
| `CodeBlockDetector` | Identifies monospace fonts (11-entry whitelist, case-insensitive) |
| `TableConverter` | Converts `List<List<String>>` rows to GFM pipe-table Markdown |
| `ImageExtractor` | Wraps raw image bytes as `ExtractedImage` data class; normalises `jpg→jpeg` |
| `LinkPreserver` | Converts text + URL pair to Markdown inline link with proper escaping |

---

## 5. Per-format notes for contributors

### DOCX — Apache POI 5.5.1

- **Classpath:** `poi-ooxml` on both `androidMain` and `desktopMain`. `poi-ooxml-lite` was attempted but lacks XWPF classes — the full artifact is required.
- **Android multidex:** `multiDexEnabled = true` is set in `androidApp/build.gradle.kts`. POI + transitive deps push Android method count past the 64k limit.
- **ProGuard keep rules:** `androidApp/proguard-rules.pro` contains the canonical centic9 keep-rule set (8 directives). Currently dormant while `isMinifyEnabled = false`; required for future release minification.
- **Heading detection:** Mapping of 18 Word style name variants (`"Heading 1"` … `"Heading 6"`, lower-case, and numeric ID variants `"1"`–`"6"`).
- **Hyperlink URLs:** Resolved via `packagePart.getRelationship(hyperlinkId)?.targetURI` — the `XWPFHyperlinkRun.hyperlink` field is private in POI 5.x.
- **JAXB stub note:** `exclude(group = "stax", ...)` is not applied — KMP `Provider<T>` does not accept the exclude lambda; AGP conflict resolution handles the stax-api clash.

### HTML — jsoup 1.17.2 + flexmark-html2md-converter 0.64.8

- **Classpath:** `jsoup` + `flexmark-html2md-converter` on both `androidMain` and `desktopMain`.
- **ATX headings forced:** `FlexmarkHtmlConverter` option `SETEXT_HEADINGS = false` ensures `# H1` output instead of underline style.
- **Both extensions covered:** `.html` and `.htm` both dispatch to `HtmlImporter`.
- **flexmark license:** BSD-2-Clause (not Apache 2.0). Acceptable for Yole's Apache-2.0 distribution.

### RTF — `javax.swing.text.rtf.RTFEditorKit` (Desktop JVM standard library)

- **No new dependencies** — `RTFEditorKit` is part of Java SE, not Android SDK.
- **Android:** Returns `ImportError.NotSupported("rtf", "Android")` with tracker `#iter-64-android-rtf-no-swing`. `javax.swing` is absent from the Android SDK. Long-term path: pure-Kotlin RTF tokeniser.
- **Header guard:** Bytes must start with `{\rtf` — validated before parsing; otherwise `IllegalArgumentException` becomes `ImportError.Malformed`.
- **Element walk:** `doc.defaultRootElement` → paragraph elements → leaf elements; sentinel `\n` leaves are skipped.

### ODT — Apache ODFDOM 1.0.0-BETA1 (Desktop) / ZipInputStream+XmlPullParser (Android)

- **ODFDOM is Desktop-only:** `odfdom-java` is added to `desktopMain` only. Android excludes ODFDOM because it pulls in Xerces2 (`xml-apis` + `xercesImpl`) which conflicts with Android's built-in XML parser.
- **ODFDOM version:** `1.0.0-BETA1` is the only release on Maven Central; no stable release exists.
- **`office:text` navigation:** ODFDOM's `OdfTextDocument` has no direct `officeText` getter in `1.0.0-BETA1`; obtained via `getElementsByTagNameNS("urn:oasis:names:tc:opendocument:xmlns:office:1.0", "text").item(0)` or recursive `findOfficeText()`.
- **Android ZipInputStream path:** `ZipInputStream(ByteArrayInputStream(bytes))` → scan to `content.xml` → `android.util.Xml.newPullParser()` state machine. Namespace URI matched as `urn:oasis:names:tc:opendocument:xmlns:text:1.0` with empty-namespace fallback. List items appear as plain paragraphs (see `#iter-64-odt-android-list-nesting`).
- **Test structural check:** `parseOdtViaZipDesktop()` in `OdtImporterTest` mirrors the Android algorithm using `javax.xml.parsers.SAXParser` so the ZIP-path algorithm is verified at `desktopTest` time without Robolectric.

### PDF — Apache PDFBox 3.0.7 (Desktop) / pdfbox-android 2.0.27.0 (Android)

- **Version split:** PDFBox 3.x and pdfbox-android 2.x have divergent APIs. The Android community port is minimally maintained (last release Jan 2023).
- **3.x API:** `Loader.loadPDF(bytes)` + `PDFTextStripper` subclass `RunCollector`.
- **2.x API:** `PDDocument.load(ByteArrayInputStream(bytes))` (`Loader` class does not exist in 2.x). `PDFBoxResourceLoader.init(null)` required for CMap bundling. Package prefix is `com.tom_roush.pdfbox.*` vs `org.apache.pdfbox.*`.
- **Heading heuristic:** Font-size histogram → mode = body size; `sortedDescending().distinct()` sizes → `HeadingDetector.headingLevelByFontSize`. Low-confidence warning when size delta < 1.5 pt or single-word candidate.
- **Image extraction:** Desktop: `PDImageXObject.image` → `javax.imageio.ImageIO.write()` → `ImageExtractor.fromBytes()`. Android: `PDImageXObject.image` returns `android.graphics.Bitmap` → `Bitmap.compress(PNG)`.
- **APK size:** pdfbox-android adds approximately 5–7 MB of dex + resources. Android `multiDexEnabled = true` was already set by the POI Phase 3 change.
- **Context gap:** `PDFBoxResourceLoader.init(null)` is called with `null` context in the Android actual. Production call site (not yet wired) should pass a valid `Context`. See `#iter-64-pdfbox-android-context`.

### EPUB — roll-own ZIP+HtmlImporter loop

- **Library choice:** `epublib` upstream is abandoned (last commit May 2021). Yole uses a roll-own implementation: ZipInputStream scan for OPF manifest → parse `<spine>` chapter order → extract each chapter HTML → `HtmlImporter.import()`.
- **No new library dependency:** EPUB is ZIP + HTML; jsoup (already on classpath from `HtmlImporter`) parses each chapter.
- **Chapter order:** Follows OPF `<spine>` `<itemref>` order; chapters are separated by `---` Markdown horizontal rules.
- **Metadata:** Title, author, cover image, and publisher from `<metadata>` are not imported. See `#iter-64-epub-metadata`.

---

## 6. Invocation surfaces

| Surface | Code path |
|---------|----------|
| FILES tab button | `ImportButton` Composable → `ImporterRegistry.forExtension` → importer → `YoleApp.openImportedDocument()` |
| File menu (Desktop) | `ImportMenuItem` → same path |
| Android share intent | `MainActivity.onNewIntent` → `ImportShareIntentHandler` → intent URI → `YoleApp.singleton` → main screen polling loop |
| Desktop drag-drop | `acceptImportFileDrops` on main `Box` → same `YoleApp` path |

`ImportProgressDialog` and `ImportPreview` composables display state during and after import.
`ImportWarningsPanel` renders the `warnings` list from `ImportedDocument`.

---

## 7. Cross-platform disposition

| Platform | Importer actuals | Notes |
|----------|-----------------|-------|
| Android | `DocxImporter.android.kt`, `HtmlImporter.android.kt`, `RtfImporter.android.kt` (stub), `OdtImporter.android.kt`, `PdfImporter.android.kt`, `EpubImporter.android.kt` | RTF stub; ODT uses ZipInputStream path; PDF uses pdfbox-android 2.x |
| Desktop | `*.desktop.kt` for all 6 | Full JVM; PDFBox 3.x; ODFDOM; RTFEditorKit |
| iOS | `*.ios.kt` stubs for all 6 | All return `ImportError.NotSupported` |
| Web/Wasm | `*.wasmJs.kt` stubs for all 6 | All return `ImportError.NotSupported` |

---

## 8. Testing strategy

All round-trip tests synthesise input bytes programmatically (Apache POI for DOCX, PDFBox 3.x for
PDF, ODFDOM for ODT, raw RTF literals for RTF, jsoup-generated HTML for HTML, hand-assembled ZIP
for EPUB). No binary fixture files are committed — synthesis is self-documenting and avoids blob
tracking in git.

Every importer test class follows the same four-test minimum:

1. **Round-trip test** — synthesises valid bytes, asserts `isSuccess`, checks key Markdown elements.
2. **Mutation guard test** — inline stub always returns `Result.failure`; asserts `isFailure` (proves test 1 cannot pass against a no-op importer, satisfying CONST-035).
3. **Extension test** — `supportedExtensions` contains the expected string.
4. **Malformed test** — garbage bytes produce `Result.failure(ImportError.Malformed)`.

Tests live in `shared/src/desktopTest/kotlin/digital/vasic/yole/import_/`.

---

## 9. Known gaps (consolidated)

| Tracker | Area |
|---------|------|
| `#iter-64-android-rtf-no-swing` | RTF not supported on Android; `javax.swing` absent from Android SDK |
| `#iter-64-ios-hard-blocked` | All 6 formats blocked on iOS; no JVM |
| `#iter-64-web-hard-blocked` | All 6 formats blocked on Web/Wasm; no JVM |
| `#iter-64-pdf-image-only` | Image-only PDFs produce empty output |
| `#iter-64-pdf-heading-heuristic` | Font-size heading detection is heuristic; degrades on irregular PDFs |
| `#iter-64-pdfbox-android-context` | `PDFBoxResourceLoader.init(null)` — production call site must supply `Context` |
| `#iter-64-rtf-colour-images` | RTF colour, images, tables not extracted |
| `#iter-64-odt-android-list-nesting` | Android ODT path does not indent nested lists |
| `#iter-64-epub-metadata` | EPUB title/author/cover dropped |
