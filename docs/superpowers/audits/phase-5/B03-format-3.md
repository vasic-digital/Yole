# B03 Anti-Bluff Audit — shared/commonTest format/ parsers, part 3

**Auditor:** CONST-035 / CONST-039 automated review
**Date:** 2026-05-20
**Files audited:** 32
**Technique:** Stub-substitution thought experiment — "if every line of the unit under test were replaced with a trivial stub returning a ParsedDocument with the same rawContent but empty parsedContent/metadata, would this test still pass?"

---

## Summary

| Verdict | Count |
|---------|-------|
| BLUFF   | 2     |
| SUSPECT | 9     |
| CLEAN   | 21    |

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/rmarkdown/RMarkdownParserHtmlTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~30 (document wrapping, div classes, frontmatter HTML, metadata, code chunks, HTML escaping, dark mode, footnotes, bibliography, multicol, parameterized reports, cross-reference, toHtml pass-through)

**Units under test:** `RMarkdownParser.parse()`, `RMarkdownParser.toHtml()`

**Evidence:**
- Line 27: `assertTrue(html.contains("rmarkdown-document"))` — requires parser to emit a specific CSS class
- Line 88: `assertTrue(doc.parsedContent.contains("code-chunk"))` — requires parser to emit code-chunk markers
- Multiple tests check exact HTML class names: `"rmarkdown-section"`, `"rmarkdown-math"`, `"rmarkdown-figure"`, etc.
- `testHtmlEscaping` checks `&lt;`, `&gt;`, `&amp;` are present in output — a stub returning `rawContent` directly would fail

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/rmarkdown/RMarkdownParserTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~35 (basic parsing, metadata extraction, code chunk counts, YAML frontmatter, R content, HTML output, round-trip, performance)

**Units under test:** `RMarkdownParser.parse()`, `RMarkdownParser.toHtml()`

**Evidence:**
- Metadata assertions check exact extracted values: `assertEquals("5", result.metadata["code_chunks"])`, `assertEquals("true", result.metadata["has_frontmatter"])`, `assertEquals("RMarkdown Analysis", result.metadata["title"])`
- Line 261: `assertTrue(html.contains("library(dplyr)"))` — requires parser to include R code block content verbatim
- Tests distinguish `r_chunks` from generic `code_chunks` — a pass-through stub could not satisfy both

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/rst/RstParserEdgeCaseTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~20 (section counts, metadata values, HTML escaping, malformed RST, unicode, nested sections)

**Units under test:** `RstParser.parse()`, `RstParser.toHtml()`

**Evidence:**
- Line 54: `assertEquals("6", result.metadata["sections"])` — requires actual section detection
- `assertTrue(html.contains("&lt;html&gt;"))` — requires HTML escaping to fire
- Edge cases verify parser handles malformed input without crashing AND that metadata still reflects correct structure

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/rst/RstParserTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~40 (format detection, basic RST structure, sections, lists, code blocks, tables, emphasis, links, images, validation, HTML conversion)

**Units under test:** `RstParser.parse()`, `RstParser.toHtml()`

**Evidence:**
- Line 321: `assertTrue(html.contains("<div class=\"rst-document light\">"))` — requires exact CSS class in output
- Line 322: `assertTrue(html.contains("<div class=\"rst-section rst-section-1\">Document Title</div>"))` — requires section nesting with correct h-level
- Validation tests check specific error message substrings: `"Unbalanced section markers"`

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/snapshot/FormatSnapshotTests.kt

**Verdict:** CLEAN (one weak assertion noted)

**Methods reviewed:** ~25 (cross-format snapshot regression, binary metadata, various format round-trips)

**Units under test:** All registered parsers via `ParserRegistry`, `FormatRegistry`

**Evidence:**
- Line 64: `assertTrue(result.parsedContent.contains("Heading 1"))` — verifies content extraction
- Line 143: `assertTrue(result.parsedContent.contains("Alice"))` — CSV content verification
- Most snapshot tests check actual parsed content fragments, not just metadata

**Weak assertion noted:**
- `snapshotBinaryMetadata` (line ~498): `assertNotNull(result.metadata)` — any parser stub would satisfy this. The metadata map is always non-null.

**Recommended fix:** For `snapshotBinaryMetadata`, add a check on at least one metadata key that the binary parser is expected to populate (e.g. `assertNotNull(result.metadata["size"])` or similar).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/ComprehensiveStressTests.kt

**Verdict:** CLEAN (majority)

**Methods reviewed:** ~30 (concurrent cache hits, parse-then-HTML cycles, cache invalidation, multi-format stress, performance bounds)

**Units under test:** `DocumentCache`, `ParsedDocument.toHtml()`, `FormatRegistry`, multiple parsers

**Evidence:**
- Line 243: `assertTrue(results.all { it == 1000 })` — verifies cache hit counts are exact, not merely non-zero
- Line 531: `assertTrue(html.contains("Test Document"))` — verifies HTML contains seeded content
- Line 540 region: checks `hasHtmlCached` transitions from false to true — verifies lazy cache wiring

**Weak pattern noted:** Some coroutine stress loops check only `isNotEmpty()` on `rawContent`, which a pass-through stub satisfies. These are in the minority.

**Recommended fix:** In stress loops that only check `rawContent.isNotEmpty()`, add at least one `parsedContent.isNotEmpty()` assertion to verify the parse pipeline fired.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/ConcurrentFormatParsingStressTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~10 (concurrent parsing correctness, content identity, format detection under load)

**Units under test:** `FormatRegistry.detectByExtension()`, format-specific parsers

**Evidence:**
- Line 118: `assertEquals(tc.content, doc.rawContent, "Content corrupted for ${tc.expectedFormatId}")` — detects data corruption under concurrent load
- Line 175: checks `parsedContent` identity across 100 concurrent parses — verifies parser is thread-safe

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/DocumentCacheOverflowTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~12 (LRU eviction, hit/miss tracking, capacity enforcement, thread safety)

**Units under test:** `DocumentCache` (LRU eviction behavior)

**Evidence:**
- Line 63: `assertNull(evicted, "Oldest entry (key-1) should be evicted")` — verifies LRU policy fires
- Line 87: `assertNull(key2, "key-2 should be evicted (was LRU)")` — verifies access-order promotion
- Lines 165–167: checks `hitCount`, `missCount`, `totalRequests` — verifies internal accounting

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/EdgeCaseStressTest.kt

**Verdict:** SUSPECT

**Methods reviewed:** ~20 (empty string, whitespace, unicode extremes, deeply nested content, large documents, special characters across all parsers)

**Units under test:** All registered parsers via loop

**Evidence of weakness:**
- Lines 46–228: nearly every test body ends with `assertNotNull(result, "${parser::class.simpleName} should handle empty string")` and nothing more. A trivial stub returning `ParsedDocument(format, rawContent="", parsedContent="", metadata=emptyMap(), errors=emptyList())` would satisfy every one of these assertions.
- No test in this file checks `parsedContent`, `metadata`, or `errors` after the stub-substitution boundary.

**Recommended fix:** For each parser test in this file, add at least one assertion on `parsedContent` or `metadata` that would fail for a stub. For the empty-string tests, verify `metadata["lines"] == "0"` or similar. For the unicode tests, verify the unicode characters appear in `parsedContent`.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/FormatParsingStressTest.kt

**Verdict:** CLEAN (majority)

**Methods reviewed:** ~12 (concurrent format detection, rapid HTML conversion, cache behavior under load)

**Units under test:** `FormatRegistry`, `MarkdownParser`, `DocumentCache`

**Evidence:**
- Lines 61–62: checks `format.id` AND `rawContent` — format detection returns correct type
- Line 101: `assertTrue(results.all { it.contains("Hello") })` — verifies HTML output contains seeded content across 50 concurrent parses

**Weak pattern noted:** Some tests only check `format.id` after detection, not parse output. Detection is a registry lookup, not parsing.

**Recommended fix:** Minor. For format-detection-only tests, consider adding a parse call to verify the detected format's parser also works.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/stress/FormatRegistryLazyInitStressTest.kt

**Verdict:** SUSPECT

**Methods reviewed:** ~15 (lazy initialization, concurrent access, thread safety of format list, singleton identity)

**Units under test:** `FormatRegistry` lazy init, `FormatRegistry.formats` list

**Evidence of weakness:**
- Line 48: `assertSame(reference, formats, "All coroutines should get the same list instance")` — this is a registry configuration test, not a parsing test. A stub `FormatRegistry` that returns any consistent list would pass.
- Line 156: `assertEquals(23, formats.size, ...)` — hardcoded count test; this is metadata about registry configuration, not about whether parsers parse correctly.
- No test in this file parses any document or checks any parsed output.

**Note:** These tests are VALID tests of the `FormatRegistry` initialization contract. The SUSPECT flag is not that these are wrong tests, but that they do not constitute end-user behavioral evidence. They cannot detect a broken parser.

**Recommended fix:** These tests are appropriate for what they test (registry init behavior). They should be supplemented by parser-round-trip tests elsewhere (which exist). No change required here — the SUSPECT rating reflects scope limitation, not a defect.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/StyleSheetsContentTest.kt

**Verdict:** SUSPECT

**Methods reviewed:** ~20 (CSS string constants: h1, h2, code, table, blockquote, etc. across multiple format stylesheets)

**Units under test:** `StyleSheets` CSS constant strings

**Evidence of weakness:**
- All tests check that `StyleSheets.MARKDOWN_STYLES.contains(".markdown h1")`, `StyleSheets.CSV_STYLES.contains(".csv-table")`, etc.
- These are string-constant content tests. If `toHtml()` completely ignored the stylesheets and returned un-styled HTML, all tests in this file would still pass.
- No test verifies that a stylesheet string is actually applied in `toHtml()` output.

**Recommended fix:** Add at least one integration test per format that calls `toHtml()` and verifies the expected CSS class appears in both the `<style>` block and the document structure. The current tests only validate the constants exist, not that they are used.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/StyleSheetsTest.kt

**Verdict:** SUSPECT

**Methods reviewed:** ~15 (non-empty checks, light ≠ dark, cache hit, cache clear, cache miss after clear)

**Units under test:** `StyleSheets` (CSS generation and caching)

**Evidence of weakness:**
- Tests check that `getStyleSheet(format, light=true).isNotEmpty()` and that the light version differs from the dark version. A stub returning `"a"` for light and `"b"` for dark would satisfy both.
- Cache tests verify the same object is returned on second call — a stub with a cached constant would pass.
- No test verifies that the returned CSS contains any format-specific selector or that it would make HTML legible.

**Recommended fix:** Add assertions on specific CSS content in the returned sheet (e.g. verify the sheet contains `"font-family"` or a format-specific class). This would fail for a stub that returns arbitrary non-empty strings.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/supremacy/UltimateSupremacyTest.kt

**Verdict:** BLUFF

**Methods reviewed:** ~12 (quality metrics, memory efficiency score, quality supremacy validation, concurrency supremacy, ultimate absolute supremacy)

**Units under test:** Multiple parsers, `DocumentCache`, `FormatRegistry`

**Evidence of bluff:**

1. **Hardcoded PASS at line 372:** `qualityMetrics["Test Coverage"] = 1.0` — this key is set unconditionally to 1.0. The subsequent assertion `assertTrue(qualityMetrics.values.all { it >= 0.95 })` will always pass for `"Test Coverage"` regardless of actual test coverage.

2. **`calculateMemoryEfficiencyScore()` always returns 1.0** (lines 506–514): the function calls `parser.parse(content)` and immediately returns `1.0`. No memory measurement is taken. No GC is invoked. The score is fabricated.

3. **`validateQualitySupremacy()` (lines 531–537):** only asserts `assertEquals(content, result.rawContent)` — this passes for any parser that preserves rawContent. A stub that sets `parsedContent = ""` and `metadata = emptyMap()` passes this check.

4. **`validateConcurrencySupremacy()` (lines 570–578):** calls `assertNotNull(result)` in a loop. A stub returning a non-null object always passes.

5. **Exception-swallowing in "ULTIMATE" test (lines 408–421):** the top-level test catches `AssertionError` from sub-validators and logs them as errors, then continues. Sub-validator failures do not cause the outer test to fail.

**Recommended fix:**
- Remove the hardcoded `qualityMetrics["Test Coverage"] = 1.0` assignment. Derive coverage from actual parser output validation.
- Replace `calculateMemoryEfficiencyScore()` with a real measurement (e.g. compare `parsedContent.length / rawContent.length` or check that metadata has expected keys).
- In `validateQualitySupremacy()`, add assertions on `parsedContent` structure and `metadata` keys, not just `rawContent` pass-through.
- In the "ULTIMATE" test, either propagate sub-validator `AssertionError`s or remove the try-catch so failures surface properly.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/taskpaper/TaskpaperParserHtmlTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~20 (document wrapping, project/task/note div classes, metadata counts, validation errors, done state, nested structure, tag rendering)

**Units under test:** `TaskpaperParser.parse()`, `TaskpaperParser.toHtml()`

**Evidence:**
- Line 38: `assertTrue(doc.parsedContent.contains("taskpaper-project"))` — requires CSS class emission
- Line 119: `assertEquals("3", doc.metadata["tasks"])` — requires counting logic
- Line 157: validation error messages are tested by substring

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/taskpaper/TaskPaperParserTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~50 (project/task/note parsing, metadata, validation, HTML conversion, performance, edge cases, unicode)

**Units under test:** `TaskpaperParser.parse()`, `TaskpaperParser.toHtml()`, `TaskpaperParser.validate()`

**Evidence:**
- Lines 477–484: checks HTML contains multiple specific CSS classes simultaneously
- Line 809: `assertTrue(html.contains("中文"))` — verifies unicode passthrough in HTML
- Validation messages include exact error strings: `"Project name cannot be empty"`

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/taskpaper/TaskpaperParserUnitTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~25 (TaskpaperItem direct API: hasTag, isDone, isToday, getDueDate, subtask counts)

**Units under test:** `TaskpaperItem`, `TaskpaperParser`

**Evidence:**
- Tests call `item.hasTag("@today")` and assert `true` — requires real tag parsing
- `getDueDate()` returns correct date string from `@due(2025-12-31)` — requires tag value extraction
- `isDone()` returns correct boolean based on `@done` tag presence

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/TextFormatComprehensiveTest.kt

**Verdict:** SUSPECT

**Methods reviewed:** ~35 (all 18 format IDs are unique, extensions valid, names non-empty, detection patterns compile, extension detection round-trip)

**Units under test:** `TextFormat` data class fields, `FormatRegistry` registry configuration

**Evidence of weakness:**
- All tests exercise format metadata (IDs, names, extensions, regex patterns). None invoke `parse()` or check `parsedContent`.
- If every parser were replaced with a stub, all tests here would still pass because they only validate registry configuration.
- "Extension detection round-trip" verifies `getByExtension()` returns the right format object — this is a registry lookup, not parsing.

**Note:** These configuration-level tests have value (they detect ID typos, duplicate registrations, broken regex patterns). The SUSPECT flag means they cannot substitute for behavioral tests.

**Recommended fix:** These tests are appropriate for their scope. They should be supplemented by parser smoke tests that actually parse content for each format.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/TextFormatExtendedTest.kt

**Verdict:** SUSPECT

**Methods reviewed:** ~30 (ID constant values, extension lists, detection pattern regex compilation, name uniqueness, default extension validity)

**Units under test:** `TextFormat` constants and `FormatRegistry` configuration

**Evidence of weakness:**
- `TextFormat.ID_MARKDOWN == "markdown"` — constant-value test; passes for any string assignment
- Regex pattern compilation tests: `Regex(pattern).containsMatchIn(sampleContent)` — verifies patterns compile and match sample text, but does not invoke the actual parser
- No `parse()` call in the entire file

**Recommended fix:** Same as `TextFormatComprehensiveTest.kt` — add smoke-parse tests per format or rely on dedicated parser test classes. The config tests themselves are useful but insufficient alone.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/TextFormatTest.kt

**Verdict:** SUSPECT

**Methods reviewed:** ~15 (data class equality, copy, hashCode, toString, ID constant values)

**Units under test:** `TextFormat` Kotlin data class behavior

**Evidence of weakness:**
- Tests verify `TextFormat(id="markdown", ...) == TextFormat(id="markdown", ...)` — pure data class contract test
- ID constant tests: `assertEquals("markdown", TextFormat.ID_MARKDOWN)` — passes for any string assignment
- Zero parsing tested in this file

**Note:** These tests are appropriate for their scope (data class contract). They are correctly placed as a foundation, but they cannot detect parsing regressions.

**Recommended fix:** No change to existing tests. Supplement with a parser integration test that invokes `parse()` and verifies `parsedContent`.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/textile/TextileParserHtmlTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~30 (h1–h6 tags, bold, italic, strikethrough, superscript, subscript, blockquote, ordered/unordered lists, links, images, inline code, pre, validation errors)

**Units under test:** `TextileParser.parse()`, `TextileParser.toHtml()`

**Evidence:**
- Every test checks a specific HTML tag is emitted: `<h1>`, `<b>`, `<em>`, `<s>`, `<sup>`, `<sub>`, `<blockquote>`, `<pre>`, `<code>`
- Validation tests check error message substrings

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/textile/TextileParserTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~50 (format detection, basic structure, heading levels, emphasis, links, images, blockquote, code, tables, validation, HTML conversion)

**Units under test:** `TextileParser.parse()`, `TextileParser.toHtml()`, `TextileParser.validate()`

**Evidence:**
- Line 543: `assertTrue(html.contains("<h1>Document Title</h1>"))` — exact tag + text
- Line 569: lists exact heading HTML for h1–h6
- Line 614: `assertTrue(html.contains("<b>bold</b>"))` — verifies bold tag wrapping

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/TextParserTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~25 (ParsedDocument data class, escapeHtml behavior, ParseOptions builder, ParserRegistry with StubParser, HTML caching lifecycle)

**Units under test:** `ParsedDocument`, `escapeHtml()` extension, `ParserRegistry`, `TextParser`

**Evidence:**
- Lines 101–127: `assertEquals("&amp;", "&".escapeHtml())` — verifies character encoding
- `testDefaultToHtml` checks HTML output contains properly escaped content
- `testLazyHtmlCaching` verifies the cached HTML is identical to the first-generated HTML, and that `hasHtmlCached` transitions from false to true

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/tiddlywiki/TiddlyWikiParserHtmlTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~30 (document structure, heading levels, inline formatting, links, images, code blocks, blockquote, metadata, validation, dark mode)

**Units under test:** `TiddlyWikiParser.parse()`, `TiddlyWikiParser.toHtml()`

**Evidence:**
- Tests check specific HTML elements: `<h1>`, `<h2>`, `<strong>`, `<em>`, `<u>`, `<s>`, `<sup>`, `<sub>`, `<code>`, `<pre>`, `<blockquote>`, `<hr>`, `<img>`, `<a>`
- Metadata extraction tested with exact values
- Validation error messages tested by substring

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/tiddlywiki/TiddlyWikiParserTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~45 (format detection, basic parsing, headings, lists, text formatting, links, code blocks, blockquotes, validation, edge cases, round-trip, performance, integration)

**Units under test:** `TiddlyWikiParser.parse()`, `TiddlyWikiParser.toHtml()`, `TiddlyWikiParser.validate()`

**Evidence:**
- Line 290: `assertTrue(result.parsedContent.contains("<strong>"))` — requires bold markup conversion
- Line 416: `assertTrue(result.parsedContent.contains("<a href='Another Tiddler'>Another Tiddler</a>"))` — exact link HTML
- Line 755: `assertTrue(result.parsedContent.contains("&lt;"))` — HTML escaping verified
- Line 1029: complex document test verifies 15+ distinct HTML elements simultaneously

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParserHtmlTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~30 (document structure, checkbox rendering, priority CSS classes, done state, description, projects, contexts, due dates, metadata counts, multiple tasks, HTML escaping, `parseTask` API)

**Units under test:** `TodoTxtParser.parse()`, `TodoTxtParser.parseTask()`, `TodoTxtParser.parseAllTasks()`

**Evidence:**
- Tests verify exact CSS class names: `"priority-a"`, `"priority-b"`, `"done"`, `"task"`, `"projects"`, `"contexts"`, `"due-date"`
- Checkbox symbols verified: `☐` (unchecked) and `☑` (checked)
- `testDescriptionEscapesHtml` at line 203: verifies `<script>` is escaped in output — a stub returning `rawContent` directly would expose the tag and fail this test
- Metadata exact count tests: `assertEquals("3", doc.metadata["totalTasks"])`

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/todotxt/TodoTxtParserTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~50 (priority parsing, completion status, date extraction, project/context tags, key-value pairs, complex tasks, HTML output, format detection, data class tests, edge cases)

**Units under test:** `TodoTxtParser.parse()`, `TodoTxtParser.parseAllTasks()`, `TodoTxtParser.parseTask()`, `TodoTxtTask` data class

**Evidence:**
- Line 79: `assertEquals('A', tasks[0].priority)` — requires character extraction from `(A)` prefix
- Lines 165, 172–175: creation and completion date field extraction verified
- Line 283–288: "fully featured task" test verifies priority, creationDate, projects, contexts, dueDate all at once

**Weak assertion noted:**
- `test HTML shows completed status` (line 312–318): only asserts `html.isNotEmpty()`. Any non-empty output passes.

**Recommended fix:** Replace `assertTrue(html.isNotEmpty())` in `test HTML shows completed status` with a check that the HTML contains a done indicator (e.g. `assertTrue(html.contains("done") || html.contains("☑"))`).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/UxComplianceTest.kt

**Verdict:** SUSPECT

**Methods reviewed:** 9 (registry completeness, format ID existence, name non-empty, extension coverage, no-duplicate IDs, filename detection, parser accessibility, parser produces valid documents, format count matches documentation)

**Units under test:** `FormatRegistry`, `ParserRegistry`, multiple parsers (partially)

**Evidence of weakness:**
- `allSeventeenFormatsRegistered`: `assertTrue(formats.size >= 18)` — registry configuration check only
- `allFormatIdsExistInRegistry`: loops over expected IDs and calls `getById()` — registry lookup only, no parsing
- `allParsersProduceValidDocuments` (lines 155–183): this test IS behavioral — it calls `parse()` and checks `rawContent` and `parsedContent.isNotBlank()`. However, it only covers 6 of the 18 formats: markdown, plaintext, todotxt, csv, latex, orgmode. The other 12 formats are not exercised.
- `parserRegistryIsAccessible` (lines 139–151): the core assertion is guarded by `if (parser != null)` — if no markdown parser is registered at test time, the entire behavioral check is skipped silently.

**Recommended fix:**
1. Remove the `if (parser != null)` guard in `parserRegistryIsAccessible` or assert `assertNotNull(parser)` before using it.
2. Expand `allParsersProduceValidDocuments` to cover all 18 formats with representative sample content.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/wikitext/WikiTextExtendedTest.kt

**Verdict:** SUSPECT

**Methods reviewed:** ~30 (templates, parser functions, variables, complex tables, category/interwiki/file links, nested templates, edge cases, performance, HTML conversion, round-trip)

**Units under test:** `WikitextParser.parse()`, `WikitextParser.toHtml()`, `WikitextParser.validate()`

**Evidence of weakness:**
- The majority of tests (lines 42–545 approximately) only assert:
  ```kotlin
  assertNotNull(result)
  assertEquals(TextFormat.ID_WIKITEXT, result.format.id)
  assertEquals(content, result.rawContent)
  ```
  A stub parser that sets `format = WikitextFormat` and `rawContent = input` passes every one of these.
- The only HTML tests with behavioral assertions are in lines 640–746: `html.contains("<div class='wikitext'>")`, `html.contains("<h1>...")`, `html.contains("<strong>all</strong>")`, `html.contains("<em>advanced</em>")` — these are CLEAN.
- Round-trip tests (lines 751–815) only compare `rawContent` and `metadata` between two parses. A stub would produce identical rawContent on both parses.

**Recommended fix:** For every non-HTML test that only checks `rawContent == content`, add a `assertTrue(result.parsedContent.contains(...))` assertion verifying that at least one feature claimed by the test name (templates, variables, tables) appears in parsed output.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/wikitext/WikitextParserHtmlTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~30 (document wrapping, h1–h3 headings, bold, italic, highlight, strikethrough, inline code, superscript, subscript, unordered/ordered lists, checklist, code blocks, HTML escaping, links, images, tables, Zim header, metadata, validation, paragraphs, templates, toHtml pass-through)

**Units under test:** `WikitextParser.parse()` (parsedContent), `WikitextParser.validate()`

**Evidence:**
- Line 39: `assertTrue(doc.parsedContent.contains("<h1>"))` — exact tag required
- Line 61–62: `assertTrue(doc.parsedContent.contains("<strong>"))` and content text
- Line 141: `assertTrue(!doc.parsedContent.contains("<script>alert"))` — HTML injection prevention verified
- Line 204: `assertEquals("true", doc.metadata["hasZimHeader"])` — Zim detection verified

**Recommended fix:** None required.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/wikitext/WikiTextParserTest.kt

**Verdict:** CLEAN (majority)

**Methods reviewed:** ~40 (format detection, basic structure, headings, lists, checklists, code blocks, formatting, links, images, Zim header, validation, edge cases, HTML conversion, round-trip, performance)

**Units under test:** `WikitextParser.parse()`, `WikitextParser.toHtml()`, `WikitextParser.validate()`

**Evidence:**
- Lines 513–516: `html.contains("<div class='wikitext'>")`, `html.contains("<h1>Document Title</h1>")`, `html.contains("<h2>Section</h2>")`, `html.contains("<p>This is a paragraph.</p>")` — exact HTML elements
- Lines 535–540: all 6 heading levels checked individually
- Lines 563–568: exact list item HTML: `<li>Item 1</li>`, `<li>Item 2</li>`
- Lines 586–594: complete formatting suite: bold, italic, highlight, strikethrough, sup, sub, code

**Weak pattern noted:**
- Some basic structure tests (lines 88–195) check only `format.id` and `rawContent`. These are in the minority of the file.

**Recommended fix:** None required for the HTML tests. The basic-structure tests could be strengthened but are supplemented by the stronger HTML conversion tests.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/wikitext/WikitextParserUnitTest.kt

**Verdict:** CLEAN

**Methods reviewed:** ~45 (supported format, empty input, headings h1–h6, bold, italic, code, strikethrough, highlight, wikilinks, templates, image detection, lists, tables, Zim header removal, code blocks, validation, special characters, unicode, metadata)

**Units under test:** `WikitextParser.parse()` (parsedContent), `WikitextParser.validate()`

**Evidence:**
- Line 93: `assertTrue(doc.parsedContent.contains("<h1>"))` — requires heading conversion
- Line 138: `assertTrue(doc.parsedContent.contains("<strong>bold</strong>"))` — exact inline markup
- Line 143: `assertTrue(doc.parsedContent.contains("<em>italic</em>"))` — exact
- Line 155: `assertTrue(doc.parsedContent.contains("<s>struck</s>"))` — exact strikethrough
- Line 264: `assertFalse(doc.parsedContent.contains("DocumentAttributes"))` — verifies Zim header is stripped
- Line 285: `assertTrue(doc.parsedContent.contains("<pre><code>"))` — verifies code block wrapping

**Recommended fix:** None required.

---

## BLUFF Files (require fixes before CONST-039 compliance)

1. **`supremacy/UltimateSupremacyTest.kt`** — Hardcoded `qualityMetrics["Test Coverage"] = 1.0` (line 372), fabricated memory efficiency score (lines 506–514), rawContent-only quality supremacy check (lines 531–537), assertNotNull-only concurrency check (lines 570–578), exception-swallowing outer test (lines 408–421).

2. *(Secondary BLUFF within CLEAN file)* **`stress/EdgeCaseStressTest.kt`** — Reclassified SUSPECT (not full BLUFF) because the test intent is legitimate but assertions are universally weak (`assertNotNull` only). Does not meet CONST-039 positive-evidence requirement.

## SUSPECT Files (weak — do not provide positive end-user evidence)

1. `stress/EdgeCaseStressTest.kt` — `assertNotNull` only across all parsers
2. `stress/FormatRegistryLazyInitStressTest.kt` — registry config checks only
3. `StyleSheetsContentTest.kt` — CSS string constant existence only
4. `StyleSheetsTest.kt` — CSS non-empty and light≠dark only
5. `TextFormatTest.kt` — data class contract only
6. `TextFormatExtendedTest.kt` — ID constant and regex compile only
7. `TextFormatComprehensiveTest.kt` — registry configuration only
8. `UxComplianceTest.kt` — mostly registry checks; `allParsersProduceValidDocuments` is partially behavioral but covers only 6/18 formats and has a guarded skip
9. `wikitext/WikiTextExtendedTest.kt` — most tests only assert `rawContent == input`; HTML section is CLEAN
