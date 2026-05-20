# Phase 5 Audit — B02 shared/commonTest format/ part 2

Audited: 2026-05-20  |  Files: 32  |  Bluff: 5  |  Suspect: 10  |  Clean: 17

---

## Findings

### shared/src/commonTest/kotlin/digital/vasic/yole/format/jupyter/JupyterParserTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 38
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/jupyter/JupyterParser.kt`
- **Evidence:** Round-trip tests at lines 797–801 assert `assertEquals(firstParse.rawContent, secondParse.rawContent)` — rawContent is just the input string stored verbatim; a stub returning `ParsedDocument(rawContent = content)` makes this pass. Core behavioral assertions at line 199 (`assertTrue(html.contains("Code [5]"))`) and line 115–118 (metadata `cells`, `kernel`) are genuine. The round-trip-only methods (`testRoundTrip*`) survive stub substitution.
- **Recommended fix:** Assert on `parsedContent` or specific HTML tokens across the round-trip (e.g., that the second parse still generates the same cell headings), not on `rawContent` which is trivially preserved by any parser.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/jupyter/JupyterVariantsTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 12
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/jupyter/JupyterParser.kt`
- **Evidence:** Line 78: `assertTrue(html.contains("Kernel: python2"))` — requires the parser to extract the kernel name from the JSON `kernelspec.language` field and emit it into the HTML header. A trivial stub returning `""` fails immediately.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/keyvalue/KeyValueParserHtmlTest.kt
- **Verdict:** BLUFF (partial — 2 specific methods)
- **Methods reviewed:** 35
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/keyvalue/KeyValueParser.kt`
- **Evidence:**
  - `testKeyValueTypeEntries()` (line 211): `assertEquals(9, KeyValueType.entries.size)` — pure enum count assertion. Zero lines of the parser need to exist for this to pass.
  - `testKeyValueEntryCreation()` (lines 216–228): constructs `KeyValueEntry` data class instances directly and asserts `.key`, `.value`, `.type` properties. Never calls the parser. Both survive complete parser stub substitution.
  - Remaining methods (HTML color/class assertions, metadata counts) are genuine behavioral.
- **Recommended fix:** Remove or replace `testKeyValueTypeEntries` with a test that parses a file containing every KeyValueType variant and verifies each type is recognized. Replace `testKeyValueEntryCreation` with a parse-and-verify test checking that specific raw text produces entries of the expected type.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/keyvalue/KeyValueParserTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 42
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/keyvalue/KeyValueParser.kt`
- **Evidence:** Line 148–150: `assertEquals("6", result.metadata["entries"])` and `assertEquals("2", result.metadata["sections"])` on a document with precisely 6 KV pairs across 2 sections. A stub that always returns `metadata["entries"] = "0"` fails.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/keyvalue/SimpleKeyValueTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 7
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/keyvalue/KeyValueParser.kt`
- **Evidence:** Line 34: `assertEquals("3", result.metadata["entries"])` on three `key=value` lines. Real counting behavior required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/latex/LatexParserHtmlTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 25
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/latex/LatexParser.kt`
- **Evidence:** Line 268: `assertTrue(light != dark)` — real behavioral check that light/dark theming actually changes output. Line 197: `testMathModeSkipsSpecialCharCheck()` — requires the parser to maintain math-mode state across the line; a stub that always appends `\&` errors would fail. Error detection tests check for precise error message substrings.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/latex/LatexParserTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 40
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/latex/LatexParser.kt`
- **Evidence:** `should parse LaTeX sections and paragraphs` (line 134) asserts `assertEquals(content, result.parsedContent)`. If `parsedContent` is simply the raw content returned verbatim by the parser (which `PlaintextParser` does and some other parsers might do for certain modes), this passes with a stub that returns `parsedContent = content`. The parser behavior needs to be verified: if `parsedContent` is the HTML-rendered form, then `content != parsedContent` and this is checking identity rather than transformation. Without seeing the full production code, this is a suspect tautology.
- **Recommended fix:** Assert on a transformed property (e.g., `assertTrue(result.parsedContent.contains("<section"))`) rather than raw-content identity. If `parsedContent` is the HTML form, then the equality assertion proves nothing about correct parsing.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/lazy/LazyLoadingValidationTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 30
- **Unit(s) under test:** `LazyDocumentLoader`, `LazyStringLoader`, `FlowLazyLoader`, `ParsedDocument`
- **Evidence:** Lines 85–103: double-access to a chunk verifies `loadCount` stays at 1 — proves caching semantics. Lines 476–485: `assertSame(html1, html2)` proves same object reference is returned (reference identity, not just value equality), ensuring no re-computation.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/load/FormatLoadTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 20
- **Unit(s) under test:** All 18 format parsers
- **Evidence:** Most soak tests assert only `assertNotNull(result.parsedContent)` — a stub returning `ParsedDocument(parsedContent = "stub")` passes every such assertion. Exception: `loadTestLargeMarkdownDocument()` (line 169) also asserts `assertTrue(result.errors.isEmpty())`. The soak stability test at lines 389–406 uses `lastBatch < firstBatch * 20 + 10` — the +10 baseline offset means this passes even when `firstBatch = 0` (e.g., a stub that parses instantly), making the regression window effectively unbounded.
- **Recommended fix:** For load tests, add at least one content-presence assertion per parser (e.g., `assertTrue(result.parsedContent.contains("heading"))`). For the stability soak, tighten the margin to `< firstBatch * 3` and document the expected range.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/markdown/MarkdownParserHtmlTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 60
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/markdown/MarkdownParser.kt`
- **Evidence:** Line 128: `assertTrue(html.contains("<strong>bold</strong>"))` — requires Flexmark to process `**bold**`. Line 179: `assertTrue(html.contains("<a href='https://google.com'>Google</a>"))` — requires link rendering. Line 223: `assertTrue(html.contains("<pre><code class=\"language-kotlin\">"))` — requires fenced-code class attribution. All three fail with a stub.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/markdown/MarkdownParserTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 50
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/markdown/MarkdownParser.kt`
- **Evidence:** Same HTML-tag presence assertions as HtmlTest; the strongest is checking that ATX headings become `<h1>`, `<h2>` etc., which fails with any stub.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/MonitoringMetricsTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 40
- **Unit(s) under test:** All format parsers, `ParsedDocument`, `DocumentCache`
- **Evidence:** Many parse-time tests assert only `elapsed < 50ms` — a stub that returns instantly always passes. However, `testHtmlGenerationFirstTimeUncached` (line 254) checks `assertFalse(doc.hasHtmlCached(lightMode = true))` before and `assertTrue` after `toHtml()` — real behavioral. `testBatchParsingMemoryStability` (line 500) checks `assertTrue(doc.rawContent.contains("Doc ${idx + 1}"))` — real content check. The pure-timing tests (≈12 methods) are the suspect subset.
- **Recommended fix:** All parse-time tests should add at least one content-correctness assertion alongside the timing bound (e.g., `assertNotNull(result.metadata["headings"])` for an OrgMode doc with known heading count).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/nonblocking/NonBlockingOperationTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 25
- **Unit(s) under test:** `DocumentCache`, `ConnectionLimiter`, `CircuitBreaker`
- **Evidence:** Line 194: ordering assertion `assertTrue(order.indexOf("job1-end") < order.indexOf("job2-start"))` — proves sequential non-blocking ordering. Line 477: `assertEquals(CircuitBreaker.State.CLOSED, cb.state)` after a `CancellationException` — verifies the circuit does not count cancellations as failures, which requires the real CircuitBreaker to catch and rethrow `CancellationException`.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/orgmode/OrgModeParserHtmlTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 20
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/orgmode/OrgModeParser.kt`
- **Evidence:** Line 46: `assertEquals("3", doc.metadata["headings"])` on a three-heading document. Line 172: `testLightAndDarkModeDiffer()` confirms theming produces distinct HTML.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/orgmode/OrgModeParserTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 40
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/orgmode/OrgModeParser.kt`
- **Evidence:** Line 712: `assertEquals("400", result.metadata["headings"])` on a synthetically generated document with exactly 100 level-1 + 300 level-2 headings — a trivial stub returning `"0"` fails.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/ParsedDocumentEdgeCaseTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 30
- **Unit(s) under test:** `ParsedDocument` data class, `ParserRegistry`
- **Evidence:** `testHashCodeConsistency()` (line 94): calls `hashCode()` twice and asserts they are equal — Kotlin data class `hashCode` is deterministic by definition; this always passes regardless of implementation. `testCopyDoesNotShareHtmlCache()` (line 205) and `testEscapeHtmlAllSpecialChars()` (lines 273–285) are real behavioral.
- **Recommended fix:** Replace `testHashCodeConsistency` with a test that verifies two `ParsedDocument` instances with identical fields have equal hash codes AND two with different `rawContent` have different hash codes (a mutation test).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/ParserInitializerTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 25
- **Unit(s) under test:** `ParserInitializer`, `ParserRegistry`
- **Evidence:** `lazy then eager registration silently skips` (line 450) only asserts no exception is thrown — an absence-of-error PASS. `registerAllParsers parsers can parse content` (lines 93–101) is real behavioral: parses `# Hello` and checks `format.name == "Markdown"`. The no-throw test is the suspect subset.
- **Recommended fix:** Replace the no-throw test with a positive assertion: after attempting to re-register a lazy parser as eager, verify the parser count did not increase (already at 18) and the parser identity is unchanged.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/performance/CacheEfficiencyTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 18
- **Unit(s) under test:** `DocumentCache`
- **Evidence:** Line 55: `assertEquals(100L, cache.hits)` after 100 lookups on a known key. Line 126–143: `assertNull(cache.get("b"))` after inserting 3 entries into a cache with `maxSize = 3` where "b" was accessed least recently — proves real LRU eviction.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/performance/LazyInitializationMetricsTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 12
- **Unit(s) under test:** `FormatRegistry`, `StyleSheets`, `ParserInitializer`, `ParsedDocument`
- **Evidence:** Pure timing tests (lines 45–99, 106–122) assert only time bounds — a stub returning instantly always passes. `StyleSheets cache grows correctly with format and mode combinations` (line 125): `assertEquals(3, StyleSheets.cacheSize)` after 3 distinct getStyleSheet calls — real behavioral. `ParsedDocument HTML lazy caching does not generate until called` (lines 236–250): `assertFalse(doc.hasHtmlCached(...))` before and `assertTrue` after — real behavioral. 8 of 12 tests are timing-only.
- **Recommended fix:** Timing tests should add a correctness assertion (e.g., for StyleSheets: verify the returned CSS is non-empty and contains `<style>`).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/performance/LazyLoadingValidationTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 30
- **Unit(s) under test:** `FormatRegistry`, `StyleSheets`, `DocumentCache`, `ParserInitializer`
- **Evidence:** `FormatRegistry isFormatsInitialized returns false before access` (lines 49–57): contains the tautology `assertTrue(initialized || !initialized, ...)` — this ALWAYS passes. It is self-described as testing that "the property is accessible without error." Genuine behavioral tests are present (lazy instantiation count at line 356–358, identity test at line 370–372). The tautology is the bluff subset.
- **Recommended fix:** Replace `assertTrue(initialized || !initialized)` with a fresh-process or cleared-registry test that verifies the initial state. At minimum, assert that `formats.size == 23` immediately after the access that triggered initialization.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/performance/MetricsCollectionTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 22
- **Unit(s) under test:** All 18 format parsers, `FormatRegistry`
- **Evidence:** All throughput and p50/p95/p99 latency tests assert only time bounds — stub parsers returning instantly always pass. `parsed document parsedContent not excessively larger than rawContent for markdown` (line 195): `assertTrue(ratio < 20.0)` — this is a ratio bound and a stub returning `parsedContent = ""` gives `ratio = 0`, which still passes. Only the `parsed document metadata size is bounded` test (line 222) and the `html generation p50` test (which parses AND calls `toHtml()`) exercise real pipeline behavior.
- **Recommended fix:** Add a per-format assertion that the parsed document contains a known string from the generated content (e.g., `assertTrue(doc.parsedContent.contains("Heading"))` for Markdown throughput tests).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/performance/PerformanceBaselineTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 60+
- **Unit(s) under test:** All 18 format parsers, `FormatRegistry`, `DocumentCache`, `CircuitBreaker`, `ConnectionLimiter`
- **Evidence:** All 60+ parse-time and HTML-generation tests assert only time bounds — stub parsers always pass. `html caching returns in under 5ms` (line 557–565) is the closest to behavioral: it calls `toHtml()` twice and verifies the second call is fast, which implicitly tests caching — but a stub always returning `""` instantly also satisfies `< 5ms`. The `connectionLimiter permits released promptly` test (line 813) implicitly tests that permits are released (50 sequential calls to a max=1 limiter finish in < 500ms), which would hang with a broken limiter.
- **Recommended fix:** For each format's parse-time baseline test, add a 1-line assertion on parsed content (e.g., `assertTrue(doc.rawContent.length >= 1024)` for 1KB tests, which is already checked in one case at line 411). HTML generation tests should additionally verify the HTML contains expected structural elements.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/PerformanceMetricsTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 30
- **Unit(s) under test:** All 18 format parsers, `FormatRegistry`
- **Evidence:** All per-parser performance tests assert only time bounds. The batch test `testBatchParsingMultipleFormats` (line 552) does call `toHtml()` but still only asserts timing. The registry batch test `testFormatRegistryAllOperationsBatch` (line 584) calls 11 different registry operations 100 times but only asserts a time bound — a stub that returns `null` for every lookup still completes in < 2s.
- **Recommended fix:** Add one behavioral assertion per batch: e.g., in `testBatchParsingMultipleFormats`, assert `assertTrue(doc.parsedContent.isNotEmpty())` for each parsed document.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/plaintext/PlaintextParserExtendedTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 70+
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/plaintext/PlaintextParser.kt`
- **Evidence:** Line 83–84: `assertTrue(result.parsedContent.contains("true"))` and `contains("false")` and `contains("null")` on a JSON input with booleans and null — requires the JSON pretty-printer to preserve boolean/null literals. Line 497: `assertTrue(html.contains("language-python"))` for a `.py` file — requires extension-to-language mapping. Line 684: `assertTrue(html.contains("&lt;script&gt;"))` for JSON containing `<script>` — requires HTML escaping in the output.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/plaintext/PlaintextParserHtmlTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 45
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/plaintext/PlaintextParser.kt`
- **Evidence:** Line 39–40: `assertFalse(doc.parsedContent.contains("<script>"))` + `assertTrue(doc.parsedContent.contains("&lt;script&gt;"))` — XSS prevention test. Line 177–181: multiple entity escaping checks (`&lt;`, `&gt;`, `&amp;`) in code blocks. These fail with any stub that passes content through unescaped.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/plaintext/PlainTextParserTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 40
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/plaintext/PlaintextParser.kt`
- **Evidence:** Round-trip tests at lines 458–493 assert `assertEquals(firstParse.rawContent, secondParse.rawContent)` — since `rawContent` is always the input string preserved verbatim by any parser, this is tautological for the round-trip direction. The performance tests at lines 509–577 assert only time bounds. The strong behavioral assertions (HTML structure, XSS escaping, code highlighting) are present in other methods and are genuine.
- **Recommended fix:** Replace `assertEquals(firstParse.rawContent, secondParse.rawContent)` with `assertEquals(firstParse.parsedContent, secondParse.parsedContent)` to actually verify round-trip rendering consistency.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/plaintext/PlaintextParserUnitTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 45
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/plaintext/PlaintextParser.kt`
- **Evidence:** Line 89–91: `assertEquals(content.length, chars)` on "Hello World" — requires `characters` metadata to count exactly 11 characters. Line 109: `assertEquals(5, lines)` on five-line input. Line 275: `assertFalse(doc.parsedContent.contains("<script>"))` — XSS prevention. Line 313: `assertEquals(10_000, lines)` for 10,000-line input. All fail with stubs.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/plaintext/SimplePlainTextTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/plaintext/PlaintextParser.kt`
- **Evidence:** Line 34–36: `assertEquals("plain", result.metadata["type"])` + `assertEquals("3", result.metadata["lines"])` on a 3-line input — precise metadata verification.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/platform/PlatformParsingTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 45
- **Unit(s) under test:** All 18 format parsers, `FormatRegistry`
- **Evidence:** The determinism tests (lines 112–194) call `verifyDeterministic()` which asserts `assertEquals(doc1, doc2)` on two parses of the same input — a stub that is purely functional (same input → same output) passes this. The UTF-8 BOM test (line 362–368) is `assertTrue(detected != null || detected == null, ...)` — a literal tautology. The extension case-insensitivity tests (lines 200–258) are real behavioral: `verifyCaseInsensitiveExtension("md", "MD", "Md", "mD")` verifies all 4 produce the same format ID.
- **Recommended fix:** Remove the BOM tautology at line 362. For determinism tests, add per-format content assertions (e.g., for Markdown: verify `doc1.parsedContent.contains("<p>")`) rather than just equality between the two parsed documents.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/PropertyBasedFormatTests.kt
- **Verdict:** BLUFF (structurally)
- **Methods reviewed:** 170
- **Unit(s) under test:** All 18 format parsers
- **Evidence:** 153 of 170 tests assert only `assertNotNull(doc)` (the crash-free invariant). A stub that returns `ParsedDocument(rawContent = content, parsedContent = "stub")` passes every such test. The 17 cross-parser invariant tests at lines 1359–1448 are slightly stronger: `testAllParsersPreserveRawContent` (line 1359) checks `assertEquals(testInput, doc.rawContent)`, which passes with any stub; `testAllParsersReturnNonEmptyParsedContentForNonEmpty` (line 1396) checks `parsedContent.isNotEmpty()` — a stub returning `"stub"` passes. `testAllParsersHandleRepeatedParsing` (line 1429) checks `doc1.parsedContent == doc2.parsedContent` — a deterministic stub passes.
- **Recommended fix:** For each of the 18 parsers, replace at least 3 of the 10 scenario tests with assertions that verify format-specific output properties (e.g., Markdown: assert `parsedContent.contains("<p>")` for the Unicode scenario; LaTeX: assert `errors` is a list for the null-chars scenario; KeyValue: assert `metadata["entries"]` is parseable as a number).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/restructuredtext/RestructuredTextParserUnitTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 40
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/restructuredtext/RestructuredTextParser.kt`
- **Evidence:** Line 133: `assertEquals("3", doc.metadata["sections"])` on a document with precisely 3 RST section underlines. Line 265: `assertTrue(errors.any { it.contains("underline too short") })` on a title with a 3-char underline under a 17-char title — requires the validator to check relative underline length. Line 335: `assertEquals("100", doc.metadata["sections"])` on a synthetically generated 100-section document.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/restructuredtext/RstParserHtmlTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 22
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/format/restructuredtext/RestructuredTextParser.kt`
- **Evidence:** Line 43: `assertTrue(html.contains("rst-section-1"))` — requires the HTML generator to apply level-based CSS class names. Line 136–140: `assertTrue(light != dark)` — real behavioral check. Line 161: `assertTrue(html.contains("&lt;tag&gt;"))` — HTML escaping verification. All fail with stubs.

---

## Summary Table

| File | Verdict |
|------|---------|
| jupyter/JupyterParserTest.kt | SUSPECT |
| jupyter/JupyterVariantsTest.kt | CLEAN |
| keyvalue/KeyValueParserHtmlTest.kt | BLUFF |
| keyvalue/KeyValueParserTest.kt | CLEAN |
| keyvalue/SimpleKeyValueTest.kt | CLEAN |
| latex/LatexParserHtmlTest.kt | CLEAN |
| latex/LatexParserTest.kt | SUSPECT |
| lazy/LazyLoadingValidationTests.kt | CLEAN |
| load/FormatLoadTests.kt | SUSPECT |
| markdown/MarkdownParserHtmlTest.kt | CLEAN |
| markdown/MarkdownParserTest.kt | CLEAN |
| MonitoringMetricsTests.kt | SUSPECT |
| nonblocking/NonBlockingOperationTests.kt | CLEAN |
| orgmode/OrgModeParserHtmlTest.kt | CLEAN |
| orgmode/OrgModeParserTest.kt | CLEAN |
| ParsedDocumentEdgeCaseTest.kt | SUSPECT |
| ParserInitializerTest.kt | SUSPECT |
| performance/CacheEfficiencyTest.kt | CLEAN |
| performance/LazyInitializationMetricsTest.kt | SUSPECT |
| performance/LazyLoadingValidationTests.kt | SUSPECT |
| performance/MetricsCollectionTest.kt | SUSPECT |
| performance/PerformanceBaselineTests.kt | SUSPECT |
| PerformanceMetricsTests.kt | SUSPECT |
| plaintext/PlaintextParserExtendedTests.kt | CLEAN |
| plaintext/PlaintextParserHtmlTest.kt | CLEAN |
| plaintext/PlainTextParserTest.kt | SUSPECT |
| plaintext/PlaintextParserUnitTest.kt | CLEAN |
| plaintext/SimplePlainTextTest.kt | CLEAN |
| platform/PlatformParsingTests.kt | SUSPECT |
| PropertyBasedFormatTests.kt | BLUFF |
| restructuredtext/RestructuredTextParserUnitTest.kt | CLEAN |
| restructuredtext/RstParserHtmlTest.kt | CLEAN |

---

## Key Patterns Observed

**Pattern 1 — Performance-test-only assertion (most prevalent SUSPECT cause):**
Approximately 8 test files use `measureTime { parser.parse(content) }` and assert only the elapsed time. A stub parser that returns `ParsedDocument(parsedContent = "")` instantly passes every timing gate. This affects: `LazyInitializationMetricsTest`, `LazyLoadingValidationTests` (partially), `MetricsCollectionTest`, `PerformanceBaselineTests`, `PerformanceMetricsTests`, `MonitoringMetricsTests` (partially).

**Pattern 2 — rawContent round-trip tautology:**
`assertEquals(firstParse.rawContent, secondParse.rawContent)` is structurally identical to `assertEquals(content, content)` because every parser must set `rawContent = content`. Affects: `JupyterParserTest`, `PlainTextParserTest`.

**Pattern 3 — assertNotNull / assertNotNull(parsedContent) bluff:**
`assertNotNull(doc)` and `assertNotNull(doc.parsedContent)` pass for any parser that constructs a document. `PropertyBasedFormatTests` (153/170 tests) is the primary instance.

**Pattern 4 — Enum/data-class-constructor-only test:**
`KeyValueParserHtmlTest.testKeyValueTypeEntries()` and `testKeyValueEntryCreation()` — zero parser involvement.

**Pattern 5 — Tautological boolean assertion:**
`assertTrue(x || !x)` always passes. Found in `LazyLoadingValidationTests` line 57 and `PlatformParsingTests` line 366.
