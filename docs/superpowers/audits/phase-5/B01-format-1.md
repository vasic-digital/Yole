# B01 Anti-Bluff Audit — shared/commonTest format/ parsers, part 1

**Auditor:** Claude Sonnet 4.6 (automated, CONST-035/039 mandate)
**Date:** 2026-05-20
**Batch:** B01 (32 files)
**Tally:** 32 files | 3 BLUFF | 10 SUSPECT | 19 CLEAN

---

## Schema

Each `###` block covers one test file.

- **Verdict:** CLEAN | SUSPECT | BLUFF
- **Methods reviewed:** n
- **Units under test:** production files exercised
- **Evidence (line numbers):** key assertions cited
- **Recommended fix:** where applicable

---

### 1. AsciidocParserHtmlTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 25
**Units under test:** `AsciidocParser`

**Evidence:**
- Line 43: `assertTrue(html.contains("<h1>"))` — real heading structure check
- Line 164: `assertEquals("My Document", doc.metadata["title"])` — exact metadata value
- Line 184: `assertTrue(doc.errors.any { it.contains("Unclosed code block") })` — real error detection
- Line 224: `assertTrue(html.contains("&lt;tag&gt;"))` — XSS escaping verified on actual HTML output

All 25 methods apply real inputs and assert on specific content in `parsedContent`/`toHtml()`. Replacing `AsciidocParser.parse()` with a stub returning empty strings would break every assertion. CLEAN.

---

### 2. AsciidocParserTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 28
**Units under test:** `AsciidocParser`, `FormatRegistry`

**Evidence:**
- Line 106: `assertEquals("John Doe", result.metadata["author"])` — exact metadata
- Line 108: `assertEquals("Document Title", result.metadata["title"])` — exact metadata
- Line 402: `assertTrue(errors.any { it.contains("Unclosed code block") })` — validation
- Line 554: `assertTrue(html.contains("<h1>Document Title</h1>"))` — exact element structure
- Line 630: `assertTrue(html.contains("println(&quot;Hello, World!&quot;)"))` — escaping in code block

Performance tests use `Clock.System.now()` with real time bounds. Mutation of `AsciidocParser` to return trivial stubs would break multiple assertions. CLEAN.

---

### 3. BinaryDetectionTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 22
**Units under test:** `BinaryParser`

**Evidence:**
- Line 63: `assertEquals("application/x-executable", result.metadata["mime_type"])` — exact MIME type
- Line 220: `assertEquals("1 KB", result.metadata["file_size"])` — exact formatted size
- Line 323: `assertTrue(html.contains("Binary File Preview"), ...)` — HTML content

`BinaryParser` performs byte-sequence MIME detection; assertions on exact MIME type values survive only if detection logic runs correctly. CLEAN.

---

### 4. BinaryEdgeCasesTest.kt

**Verdict:** BLUFF
**Methods reviewed:** 16
**Units under test:** `BinaryParser`

**Evidence:**
- Lines 332–360: `hasHtmlCached()`, `clearHtmlCache()` behavioral checks — CLEAN subset
- **Line 238:** `assertFalse(html.contains("<script") && !html.contains("&lt;script"))` — logical flaw

**Surviving mutant:** A stub `BinaryParser.toHtml()` that returns the empty string `""` would make `html.contains("<script")` = false and `!html.contains("&lt;script")` = true, so the compound condition `false && true` = false, and `assertFalse(false)` passes trivially. The test purports to verify XSS prevention but does NOT assert that `"&lt;script"` is actually present. Any parser returning content that lacks `<script` passes unconditionally regardless of whether proper escaping occurs.

**Recommended fix:** Split into two assertions:
```kotlin
assertFalse(html.contains("<script>"), "raw script tag must not appear")
assertTrue(html.contains("&lt;script"), "script must appear entity-escaped")
```

---

### 5. BinaryParserHtmlTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 29
**Units under test:** `BinaryParser`

**Evidence:**
- Line 48: `assertEquals("image/jpeg", doc.metadata["mime_type"])` — exact MIME type
- Line 119: `assertTrue(doc.parsedContent.contains("image-container"))` — structural element
- Line 198: `assertTrue(light != dark)` — light/dark mode actually differ (requires both code paths to run)

29 methods assert on specific parsedContent structures, metadata values, and MIME-type correctness. CLEAN.

---

### 6. BinaryPerformanceTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 7
**Units under test:** `BinaryParser`

**Evidence:**
- Line 78: `assertEquals(expectedSize, result.metadata["file_size"])` — real size formatting check inside benchmark

All benchmarks assert both timing bounds AND real behavioral outputs (metadata values). A stub returning empty metadata would fail line 78. CLEAN.

---

### 7. CacheConcurrencyTest.kt

**Verdict:** BLUFF
**Methods reviewed:** 24
**Units under test:** `DocumentCache`, `StyleSheets`

**Evidence (clean subset):**
- Line 144: `assertEquals(10L, cache.hits)` — real hit counter
- Line 145: `assertEquals(10L, cache.misses)` — real miss counter

**BLUFF — Line 192:** In `concurrentStyleSheetAccessDoesNotCrash`:
```kotlin
assertTrue(results.all { it.isNotEmpty() || it.isEmpty() })
```
**Surviving mutant:** `StyleSheets.getStyleSheet()` stubbed to return `""` — every result is empty, so `isEmpty()` = true, the disjunction is true for all, and `assertTrue(true)` passes. This is a tautological assertion. A string is always either non-empty or empty. The test claims to verify concurrent stylesheet access returns valid CSS but the assertion places no lower bound on correctness.

**Recommended fix:**
```kotlin
// Assert actual CSS content is returned, not just any string
assertTrue(results.all { css -> css.contains("<style>") && css.contains("</style>") },
    "All concurrent stylesheet results must contain valid style tags")
```

---

### 8. AuthUiModelCoverageTests.kt

**Verdict:** CLEAN
**Methods reviewed:** 25
**Units under test:** `Document`, `StorageQuota`, `TextFormat`, `FormatRegistry`, `StorageType`

**Evidence:**
- Line 67: `assertEquals("README.md", doc.filename)` — exact filename value
- Line 137: `assertEquals("10GB", quota.formattedTotalSpace)` — exact formatting
- Line 311: `assertEquals(8, StorageType.entries.size, ...)` — enumeration cardinality

Strong behavioral assertions on model properties. Data class semantics (equals, hashCode, copy) verified with real values. CLEAN.

---

### 9. CoreInfrastructureCoverageTests.kt

**Verdict:** CLEAN
**Methods reviewed:** 19
**Units under test:** `FormatRegistry`, `DocumentCache`, `StyleSheets`, `CircuitBreaker`, `ConnectionLimiter`, `PathUtils`, `TextParser`

**Evidence:**
- Line 156: `assertNull(cache.get("key-2"), "LRU entry key-2 should be evicted")` — real LRU eviction
- Line 264: `assertEquals(it * 2, result.getOrNull())` — CircuitBreaker passes real function results
- Line 374: `assertFailsWith<IllegalArgumentException>` for path traversal — security boundary enforced

CircuitBreaker test verifies the underlying function executes and returns real computed values (`it * 2`), not just non-null. LRU eviction is asserted by `assertNull` on a specific key. CLEAN.

---

### 10. FormatParserContractTests.kt

**Verdict:** CLEAN
**Methods reviewed:** ~85 (5 contracts × 17 parsers)
**Units under test:** All 17 text parsers, `FormatRegistry`

**Evidence:**
- Line 369: `assertEquals(entry.expectedFormatId, doc.format.id, ...)` — exact format ID check per parser
- Line 374: `assertTrue(entry.parser.canParse(doc.format), ...)` — round-trip canParse check
- Idempotency contract: calls `toHtml()` twice and asserts results are equal — exercises real HTML pipeline

Each parser is called with real content; a stub returning the wrong format ID would fail line 369. The idempotency contract would fail if HTML generation is non-deterministic. CLEAN.

---

### 11. FormatParserNonBlockingTests.kt

**Verdict:** SUSPECT
**Methods reviewed:** ~68 (4 properties × 17 parsers)
**Units under test:** All 17 text parsers

**Evidence:**
- Line 320: `assertNotNull(result, ...)` inside `withTimeoutOrNull(5.seconds)` — "absence of timeout" only
- Line 333: `assertEquals(10, count)` + non-null results in `verifyConcurrentParse`

**Why SUSPECT:** The primary assertion in `verifyParseBound` is absence-of-timeout (assertNotNull after a 5-second window). A stub `parse()` returning `ParsedDocument(format, "", "", emptyMap())` in <5 seconds would pass all 68 tests. The concurrent variant checks count=10 and non-null, but not content correctness. These tests verify responsiveness, not behavioral correctness — important, but insufficient alone.

**Not BLUFF** because: timing is a real behavioral property; a parser that deadlocks or hangs would be correctly flagged. The concern is that correctness is not verified alongside timing.

**Recommended fix:** Add one spot-check assertion per parser inside `verifyParseBound` that verifies a format-specific property of the result, e.g., format ID or non-empty parsedContent with a specific expected substring.

---

### 12. FormatParserPropertyTests.kt

**Verdict:** SUSPECT
**Methods reviewed:** ~85 (5 properties × 17 parsers)
**Units under test:** All 17 text parsers

**Evidence (strong):**
- Line 421: `assertEquals(input, doc.rawContent, ...)` — rawContent preservation (real)
- Lines 443–464 (`verifyDeterministic`): compares `parsedContent`, `metadata`, `toHtml()` between two separate parse calls — real determinism check

**Evidence (weak — SUSPECT trigger):**
- Line 432: `verifyHtmlNonEmpty` asserts only `isNotEmpty()` — a stub returning `"x"` passes
- Property `verifyEmptyInputSafety` asserts only non-null rawContent and non-null HTML — crash-only

**Why SUSPECT not BLUFF:** The `verifyDeterministic` helper (lines 443–464) is a strong behavioral check that would fail if `parsedContent` differed across parses. However, `verifyHtmlNonEmpty` in isolation is insufficient; it does not verify the HTML represents the input in any meaningful way. The file's overall value comes from `verifyDeterministic` and `verifyRawContentPreservation`, but roughly one-third of the property variants are crash-only.

---

### 13. FormatParserResilienceTests.kt

**Verdict:** SUSPECT
**Methods reviewed:** ~85 (5 resilience cases × 17 parsers)
**Units under test:** All 17 text parsers

**Evidence (strongest):**
- Line 414: `doc.rawContent.length >= 100_000` in `verifyLongInput` — rawContent size preserved

**Evidence (weakest — SUSPECT trigger):**
- Lines 392–396 (`verifyRandomBytes`): asserts only `assertNotNull(doc)` and `assertNotNull(html)` — pure crash-only
- `verifyMixedEncoding`: asserts only `assertNotNull(doc)` — crash-only
- `verifyWhitespaceOnly`: asserts only `assertNotNull(doc)` and `assertNotNull(html)` — crash-only
- `verifySpecialChars`: asserts only `assertNotNull(doc)` and non-null HTML — crash-only

**Why SUSPECT not BLUFF:** Resilience tests legitimately test crash safety, which is a real behavioral property. However, across ~68/85 methods the only assertion is "did not return null or throw." A stub `parse()` returning a hardcoded `ParsedDocument` with empty strings would pass all these variants. The `verifyLongInput` variant is the only one with a content-verification assertion.

**Recommended fix:** Add to each helper a single assertion that `doc.rawContent` is non-null AND equals `input` for the non-random variants (mixed encoding, whitespace, special chars), and add `assertTrue(doc.rawContent.isNotEmpty())` with size check for random-bytes variants.

---

### 14. FormatParserSecurityTests.kt

**Verdict:** BLUFF
**Methods reviewed:** ~85 (5 security properties × 17 parsers)
**Units under test:** All 17 text parsers, `String.escapeHtml()`

**Evidence of BLUFF:**

**`verifyXssPrevention` (the critical bluff):**
```kotlin
// What the test actually does (reconstructed from reading):
val escaped = payload.escapeHtml()       // calls the UTILITY, not the parser
assertFalse(escaped.contains("<script>")) // asserts the utility works
val doc = parser.parse(payload)          // parser IS called...
val html = doc.toHtml()                  // ...HTML IS generated...
// but html is NEVER checked for <script> or XSS-unsafe content
```
**Surviving mutant:** Replace `AsciidocParser.toHtml()` with a stub that returns `"<script>alert('xss')</script>"`. The test calls `payload.escapeHtml()`, asserts the *escaped string* doesn't contain `<script>`, and passes. The real parser HTML output is never asserted on for XSS safety. This pattern repeats for ALL 17 parsers × the XSS property = ~17 bluff test methods.

**`verifyHtmlInjection`:** Same pattern — tests `escapeHtml()` utility in isolation rather than verifying `doc.toHtml()` doesn't emit raw injection strings.

**`verifySqlInjection`:** Asserts `assertNotNull(doc)` and `errors.none { it.contains("SQL") }` — crash-only plus a trivially true assertion (parsers don't report SQL errors by design).

The production unit under test (`String.escapeHtml()`) is a 5-line pure function (TextParser.kt lines 558–565) that is tested correctly in isolation. The problem is that this test *claims* to verify parser XSS safety but only verifies the utility.

**Recommended fix:** Replace the core assertion in `verifyXssPrevention`:
```kotlin
val html = doc.toHtml(lightMode = true)
assertFalse(html.contains("<script>"),
    "Parser HTML output for ${parser.supportedFormat.id} must not emit raw script tags")
assertTrue(html.contains("&lt;script") || !html.contains("script"),
    "Parser HTML output must entity-escape script tags if present")
```

---

### 15. FormatParserSupremacyTests.kt

**Verdict:** SUSPECT
**Methods reviewed:** ~102 (6 supremacy cases × 17 parsers)
**Units under test:** All 17 text parsers

**Evidence (strongest):**
- Line 482: `assertEquals(input, doc.rawContent, ...)` in `verifyUnicodeOnly` — content preservation
- Line 422–430 (`verifyEmptyString`): calls `validate("")` and asserts non-null HTML

**Evidence (weakest — SUSPECT trigger):**
- `verifyEmptyString`: only asserts non-null rawContent and non-null HTML (crash-only for most parsers)
- `verifySingleChar`: asserts non-null rawContent + non-null HTML — crash-only
- `verifyNewlinesOnly`: asserts non-null rawContent + non-null HTML — crash-only
- `verifyBinaryLikeContent`: asserts only `assertNotNull(doc)` — crash-only

**Why SUSPECT not BLUFF:** `verifyUnicodeOnly` (line 482) is a real behavioral check. `verifyEmptyString` calls `validate("")` which exercises additional code paths. But 4 of 6 supremacy helpers are crash-only, meaning ~68 of ~102 methods pass even against trivial stubs.

---

### 16. CreoleParserHtmlTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 29
**Units under test:** `CreoleParser`

**Evidence:**
- Line 39: `assertTrue(doc.parsedContent.contains("<h1>"))` — heading rendered
- Line 111: `assertTrue(doc.parsedContent.contains("<th>"))` — table header rendered
- Line 187: `assertEquals("3", doc.metadata["lines"])` — exact metadata
- Line 200: `assertTrue(errors.any { it.contains("Malformed table row") })` — validation

29 methods assert specific HTML elements, exact metadata values, and validation errors. CLEAN.

---

### 17. CreoleParserTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 35
**Units under test:** `CreoleParser`

**Evidence:**
- Line 424: `assertTrue(html.contains("<h1>Document Title</h1>"))` — exact element
- Line 497: `assertTrue(html.contains("<strong>bold</strong>"))` — exact inline formatting
- Line 528: `assertTrue(html.contains("<img src='image.jpg' alt='Alternative text'/>"))` — exact image tag
- Line 566: `assertTrue(html.contains("<th>Header 1</th>"))` — exact table header

Strong behavioral assertions throughout. Stub returning empty HTML would break all element-level checks. CLEAN.

---

### 18. SimpleCreoleParserTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 2
**Units under test:** `CreoleParser`, `FormatRegistry`

**Evidence:**
- Line 46: `assert(html.contains("<strong>bold</strong>"))` — inline element rendered
- Line 56: `assertEquals("Creole", format.name)` — exact format name

Two focused tests, both with real behavioral assertions. CLEAN.

---

### 19. CsvParserHtmlTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 26
**Units under test:** `CsvParser`

**Evidence:**
- Line 76: `assertEquals("\t", doc.metadata["delimiter"])` — exact delimiter detection
- Line 101: `assertEquals("2", doc.metadata["rows"])` — exact row count
- Line 124: `assertEquals("Doe, John", fields[0])` — quoted field parsing (comma-in-value)
- Line 228: `assertEquals("1", table.rowCount.toString())` — table structure

Delimiter detection, quoted field handling, and row/column counts all have exact-value assertions. CLEAN.

---

### 20. CsvParserTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 23
**Units under test:** `CsvParser`

**Evidence:**
- Line 129: `assertEquals("A small, handy tool", table.rows[0][1])` — exact cell value
- Line 299: `assertTrue(markdown.contains("| Name | Age |"))` — Markdown table output structure

Real data parsing with exact cell content assertions. Stub returning empty table would fail line 129. CLEAN.

---

### 21. DocumentCacheIntegrationTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 16
**Units under test:** `DocumentCache`, `FormatRegistry.parseWithCache()`

**Evidence:**
- Line 48: `assertEquals(hitsBefore + 1, cache.hits)` — real cache hit counting
- Line 241: `assertEquals("new", retrieved.rawContent)` — cache overwrite verified on content

Hit/miss counting assertions exercise the actual cache tracking. Exact rawContent check on overwrite verifies the cache stores and retrieves correctly. CLEAN.

---

### 22. EndToEndFormatTests.kt

**Verdict:** SUSPECT
**Methods reviewed:** ~100+
**Units under test:** All 17 parsers, `FormatRegistry`, `ParsedDocument`

**Evidence (strong):**
- Line 346: `assertEquals(FormatRegistry.ID_MARKDOWN, detected.id)` — detection accuracy
- Line 916: `assertTrue(escaped.contains("&lt;script&gt;"))` — XSS escaping on real HTML output
- `verifyFullPipeline`: checks format ID, rawContent match, HTML non-empty, and an expected element with fallback to `parsedContent`

**Evidence (SUSPECT trigger):**
- Line 380: OrgMode detection test allows Textile as a fallback — `assertEquals(... || detected.id == "textile")` pattern weakens the assertion; a parser misdetecting OrgMode content as Textile would still pass
- The `verifyFullPipeline` OR-fallback (`assertTrue(html.contains(expectedElement) || doc.parsedContent.contains(expectedElement))`) means if a parser fails to produce HTML with the expected element but puts it in `parsedContent`, the test still passes — the HTML generation path is not strictly verified

**Why SUSPECT:** The pipeline is real and exercises genuine code paths, but the OR-fallback in `verifyFullPipeline` and the multi-format detection fallback at line 380 introduce conditions where incorrect implementations pass.

---

### 23. FormatCoverageTest.kt

**Verdict:** BLUFF
**Methods reviewed:** 62
**Units under test:** All 17 parsers, `DocumentCache`, `StyleSheets`

**Evidence (BLUFF — pervasive):**
Multiple methods contain only:
```kotlin
assertTrue(doc.parsedContent.isNotEmpty())
```
at lines 204, 209, 213, 217, 221, 225, 229, 233, 237, 304, 309, 315, 319, 323, 327, 332, 338, 341, 363, 369, 375, 380, 385 (and more).

**Surviving mutant:** A stub `parse()` that returns `ParsedDocument(format, input, "x")` — where `parsedContent = "x"` — passes every one of these assertions. `"x".isNotEmpty()` = true. The parsers could return any non-empty garbage string and these tests pass. This covers the majority of the 62 methods.

**Evidence (clean subset):**
- Line 452: `assertEquals(longLine, doc.rawContent)` — content preservation (real)
- Line 123: `assertSame(html1, html2)` for HTML caching — object identity check (real)

**Recommended fix:** Replace the mass `isNotEmpty()` assertions with format-specific expected substrings, e.g.:
```kotlin
// Instead of:
assertTrue(doc.parsedContent.isNotEmpty())
// Use:
assertTrue(doc.parsedContent.contains("<h1>") || doc.parsedContent.contains("<h2>"),
    "Heading content must produce heading HTML elements")
```

---

### 24. FormatRegistryEdgeCaseTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 43
**Units under test:** `FormatRegistry`

**Evidence:**
- Line 198: `assertEquals(FormatRegistry.ID_RMARKDOWN, format.id)` — exact format ID
- Line 444: `testRMarkdownComesBeforeMarkdown` checks list ordering index comparison — behavioral priority
- Lines 453–476: `testFormatIdConstantsMatchRegistryIds` — exact string value matching for all 17 ID constants

Detection accuracy checked with exact format ID assertions. Ordering test would fail if format registration order changes. CLEAN.

---

### 25. FormatRegistryStressTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 22
**Units under test:** `FormatRegistry`

**Evidence:**
- Line 356: `assertEquals(expectedId, detected?.id, ...)` in `format patterns match expected content` — named content→format mappings with exact IDs
- Line 314: `detectByFilename resolves todo dot txt to todotxt not plaintext` — regression test asserting specific non-plaintext detection

1000-concurrent-coroutine test asserts all results are non-null AND have the correct ID (line 356). Stub returning wrong format would fail. CLEAN.

---

### 26. FormatRegistryUnitTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 45
**Units under test:** `FormatRegistry`

**Evidence:**
- Line 69: `assertEquals("plaintext", FormatRegistry.ID_PLAINTEXT)` — exact constant value
- Line 181: `assertEquals(FormatRegistry.ID_MARKDOWN, fmt.id)` — extension detection result
- Line 420: `getFormatsByExtension(".txt")` asserts BOTH plaintext AND todotxt present — multi-result check
- Lines 363–380: `configureParseConcurrency` boundary tests — expect `IllegalArgumentException` for out-of-range values

ID constant values, extension→format mappings, and error boundaries all use exact assertions. CLEAN.

---

### 27. FormatToggleTests.kt

**Verdict:** CLEAN
**Methods reviewed:** 14
**Units under test:** `FormatRegistry` (toggle/enable/disable API)

**Evidence:**
- Line 20: `assertEquals(setOf(TextFormat.ID_MARKDOWN), enabledIds, ...)` — exact default enabled set
- Line 46: `assertFalse(FormatRegistry.isFormatEnabled(TextFormat.ID_PLAINTEXT), ...)` after `setFormatDisabled` — toggle verified
- Line 78: `assertEquals(1, enabled.size)` + `assertEquals(TextFormat.ID_MARKDOWN, enabled[0].id)` — count and identity

Toggle behavior, idempotency, and Markdown-always-enabled invariant all have exact assertions. Stub ignoring enable/disable would fail the state-after-toggle assertions. CLEAN.

---

### 28. FormatFuzzTests.kt

**Verdict:** SUSPECT
**Methods reviewed:** 22 (17 per-format + 5 cross-format)
**Units under test:** All 17 parsers, `FormatRegistry`

**Evidence:**
- Lines 116–123 (`fuzzParser`): asserts `result.isSuccess || result.exceptionOrNull() !is OutOfMemoryError` + `assertNotNull(doc.rawContent)` + `assertNotNull(doc.parsedContent)` — crash/OOM safety only

**Why SUSPECT:** Fuzz tests are explicitly crash-safety tests; their purpose is to verify no OOM, no exception propagation, and no null fields under random input. For that purpose, the assertions are appropriate. However, they cannot verify that parsers produce correct output — by design a fuzz test cannot know the expected output for random input. The issue is that these 22 test methods are the *only* test coverage for some parsers' robustness, and they pass for stubs. They are appropriately classified SUSPECT (not BLUFF) because the testing intent is legitimate and the assertions match that intent.

**Note:** `fuzzFormatDetection` uses `result.isSuccess` only — a stub `detectByContent` returning null would pass. `fuzzExtensionDetection` additionally asserts `assertNotNull(result.getOrNull())` — stronger.

---

### 29. ComprehensiveIntegrationTests.kt

**Verdict:** CLEAN
**Methods reviewed:** 21
**Units under test:** All 17 parsers, `StyleSheets`, `FormatRegistry`, `ParsedDocument`, `ParseOptions`

**Evidence:**
- Line 104: `assertEquals(FormatRegistry.ID_MARKDOWN, doc.format.id)` — format assignment
- Line 159: `assertTrue(lightStyle.contains(".markdown"), "Markdown stylesheet must contain .markdown class")` — CSS class present
- Line 177: `assertNotEquals(lightStyle, darkStyle, ...)` — themes actually differ
- Line 326: `assertEquals(FormatRegistry.ID_JUPYTER, jupyterDocument.format.id)` — Jupyter detection
- Line 408: `assertEquals(content, doc.rawContent, ...)` across all parsers — content preservation
- Line 499: `assertEquals(mdDoc.format.id, ...)` — all parsers assign own format ID
- Lines 745–748: full round-trip compare `html == html2` — determinism across re-parses
- Line 695: `assertEquals(true, options["lineNumbers"])` — ParseOptions values correct

The round-trip test at lines 724–748 is especially strong: it parses, generates HTML, clears cache, re-parses, re-generates HTML, and asserts the two HTML strings are equal — exercising the full pipeline with a determinism check. CLEAN.

---

### 30. CrossFormatIntegrationTest.kt

**Verdict:** SUSPECT
**Methods reviewed:** 12
**Units under test:** Multiple parsers, custom conversion helpers

**Evidence (strong):**
- Line 101: `assertEquals(TextFormat.ID_LATEX, latexDocument.format.id)` — format check
- Line 106–109: `assertTrue(htmlContent.contains("Cross-Format Test Document"))` + author + bold + italic — real content in HTML output
- Line 323: `assertEquals(TextFormat.ID_JUPYTER, jupyterDocument.format.id)` — format assignment
- Line 320: `assertTrue(jupyterDocument.errors.isEmpty())` — valid parse
- Line 906: measurable `largeDocTime.inWholeNanoseconds > 0` — proves code ran

**Evidence (SUSPECT trigger):**
- Lines 210–212: the "content preservation" assertions check `rawContent` of plain-text-parsed HTML (line 627: `plainTextParser.parse(htmlContent)`), not the original structure. `assertTrue(plainTextDocument.rawContent.contains("Project Planning"))` — this checks that the plain-text parser stored the HTML as rawContent, which is trivially true since `plainTextParser.parse(x)` always sets `rawContent = x`.
- The `convertOrgToTaskPaper`, `convertWikiTextToCreole`, `convertCreoleToTiddlyWiki` helpers are test-local string manipulation functions — they are never tested themselves, so errors in the conversion could mask format-specific failures.
- `test round-trip conversion consistency` (line 646): parses with `plainTextParser` then calls `markdownParser.toHtml(firstDocument, ...)` — this calls the markdown parser on a document whose `format.id` is "plaintext", which may invoke the wrong rendering path. The result is checked only for rawContent substring presence.

**Why SUSPECT:** Several tests pass when the conversion helpers produce any valid-looking output, and some "content preservation" assertions check `rawContent` of a secondary parser (always equals the input to that parser). The LaTeX, Jupyter, and performance tests are genuinely behavioral.

---

### 31. JsonParserTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 10
**Units under test:** `JsonParser`

**Evidence:**
- Line 41: `assertTrue(html.contains("<span class='json-key'>&quot;name&quot;</span>"), ...)` — exact HTML span with escaped content
- Line 55: `assertTrue(html.contains("<span class='json-bool'>true</span>"))` — token classification
- Line 72: `assertFalse(html.contains("<b>"), ...)` — raw tag absent
- Line 73: `assertTrue(html.contains("&lt;b&gt;"), ...)` — escaped form present
- Line 91: `assertTrue(errors.any { it.contains("Unbalanced braces") || it.contains("Unterminated") })` — real error reporting
- Line 136: `assertTrue(lines >= 4, "Pretty-printed object should span at least 4 lines")` — actual pretty-printing

XSS escaping test (lines 70–77) correctly asserts BOTH that the raw tag is absent AND the escaped form is present in the HTML output — in contrast to `FormatParserSecurityTests`. Line 46: `assertEquals("true", doc.metadata["pretty_printed"])` asserts real metadata. CLEAN.

---

### 32. JupyterParserHtmlTest.kt

**Verdict:** CLEAN
**Methods reviewed:** 27
**Units under test:** `JupyterParser`, `NotebookCell`, `JupyterNotebook` data classes

**Evidence:**
- Line 122: `assertEquals("1", doc.metadata["cells"])` — exact cell count
- Line 128: `assertEquals("python3", doc.metadata["kernel"])` — exact kernel name
- Line 134: `assertEquals("python", doc.metadata["language"])` — exact language
- Line 164: `assertTrue(html.contains("[1]"))` — execution count in HTML
- Line 225: `assertTrue(errors.any { it.contains("Invalid JSON") })` — real error reporting
- Line 241: `assertEquals("0", doc.metadata["cells"])` for empty notebook — exact
- Line 212: `assertTrue(light != dark)` — themes actually produce different output

27 methods with exact metadata assertions and real HTML content checks. Stub returning empty metadata would fail lines 122, 128, 134, 139, 241. CLEAN.

---

## Summary Table

| # | File | Verdict |
|---|------|---------|
| 1 | asciidoc/AsciidocParserHtmlTest.kt | CLEAN |
| 2 | asciidoc/AsciidocParserTest.kt | CLEAN |
| 3 | binary/BinaryDetectionTest.kt | CLEAN |
| 4 | binary/BinaryEdgeCasesTest.kt | **BLUFF** |
| 5 | binary/BinaryParserHtmlTest.kt | CLEAN |
| 6 | binary/BinaryPerformanceTest.kt | CLEAN |
| 7 | concurrency/CacheConcurrencyTest.kt | **BLUFF** |
| 8 | coverage/AuthUiModelCoverageTests.kt | CLEAN |
| 9 | coverage/CoreInfrastructureCoverageTests.kt | CLEAN |
| 10 | coverage/FormatParserContractTests.kt | CLEAN |
| 11 | coverage/FormatParserNonBlockingTests.kt | SUSPECT |
| 12 | coverage/FormatParserPropertyTests.kt | SUSPECT |
| 13 | coverage/FormatParserResilienceTests.kt | SUSPECT |
| 14 | coverage/FormatParserSecurityTests.kt | **BLUFF** |
| 15 | coverage/FormatParserSupremacyTests.kt | SUSPECT |
| 16 | creole/CreoleParserHtmlTest.kt | CLEAN |
| 17 | creole/CreoleParserTest.kt | CLEAN |
| 18 | creole/SimpleCreoleParserTest.kt | CLEAN |
| 19 | csv/CsvParserHtmlTest.kt | CLEAN |
| 20 | csv/CsvParserTest.kt | CLEAN |
| 21 | DocumentCacheIntegrationTest.kt | CLEAN |
| 22 | e2e/EndToEndFormatTests.kt | SUSPECT |
| 23 | FormatCoverageTest.kt | **BLUFF** |
| 24 | FormatRegistryEdgeCaseTest.kt | CLEAN |
| 25 | FormatRegistryStressTest.kt | CLEAN |
| 26 | FormatRegistryUnitTest.kt | CLEAN |
| 27 | FormatToggleTests.kt | CLEAN |
| 28 | fuzz/FormatFuzzTests.kt | SUSPECT |
| 29 | integration/ComprehensiveIntegrationTests.kt | CLEAN |
| 30 | integration/CrossFormatIntegrationTest.kt | SUSPECT |
| 31 | json/JsonParserTest.kt | CLEAN |
| 32 | jupyter/JupyterParserHtmlTest.kt | CLEAN |

---

## Critical Findings

### BLUFF #1 — `FormatParserSecurityTests.kt`: XSS tests do not check parser HTML output
The `verifyXssPrevention` and `verifyHtmlInjection` helpers call `payload.escapeHtml()` and assert on the *escaped string*, not on `doc.toHtml()`. The actual HTML produced by all 17 parsers is never checked for injection safety. A parser emitting `<script>alert('xss')</script>` in its HTML output would pass every security test in this file. This is the highest-severity bluff in B01 and affects all 17 parser × 2 security properties = ~34 test methods.

### BLUFF #2 — `FormatCoverageTest.kt`: Mass `isNotEmpty()` assertions
~30+ of 62 methods assert only `assertTrue(doc.parsedContent.isNotEmpty())`. A stub returning `parsedContent = "x"` passes every one. The test names describe specific format features (heading rendering, list rendering, table rendering) but the assertions do not verify any of those features.

### BLUFF #3 — `BinaryEdgeCasesTest.kt`, line 238: Logically flawed XSS assertion
`assertFalse(html.contains("<script") && !html.contains("&lt;script"))` passes trivially when neither string is present (empty output, no script content). The intended assertion was that `<script` is absent AND `&lt;script` is present, but the logical structure allows both to be absent.

### BLUFF #4 — `CacheConcurrencyTest.kt`, line 192: Tautological assertion
`assertTrue(results.all { it.isNotEmpty() || it.isEmpty() })` is always true. The test claims to verify concurrent stylesheet access but places no bound on CSS correctness.

---

## SUSPECT Patterns Observed

All 5 `FormatParser*` coverage files follow a shared `verify*` helper pattern. The helpers vary in assertion strength:
- `verifyDeterministic` and `verifyRawContentPreservation` — genuinely strong
- `verifyHtmlNonEmpty`, `verifyRandomBytes`, `verifyMixedEncoding`, `verifyWhitespaceOnly`, `verifySpecialChars`, `verifyEmptyString`, `verifySingleChar` — crash-only (assertNotNull / isNotEmpty)

The ratio of strong-to-weak assertions varies per file. Files are SUSPECT rather than BLUFF because at least one strong helper exists per file, but the crash-only helpers represent ~50–70% of methods in FormatParserResilienceTests and FormatParserSupremacyTests.
