# Phase 5 Audit — B10 shared/desktopTest part 1
Audited: 2026-05-20  |  Files: 36  |  Bluff: 3  |  Suspect: 2  |  Clean: 31

## Findings

### shared/src/desktopTest/kotlin/digital/vasic/yole/completion/CompletionEngineParityTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** `CompletionEngine` (production factory + `providers` property)
- **Evidence:** `allProvidersAreWiredInDefaultEngine` (line 108) scans the real compiled classpath for `*Provider.class` files and cross-checks against `engine.providerSimpleNames()`; `wiredProviders_areAllCompletionProviders` (line 143) asserts `providers.size >= 4`. A stub `CompletionEngine.default` returning zero providers would fail both checks. Mutation procedure documented at lines 22-27 (IdentifierProvider removed → FAIL verified).
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/completion/CompletionEngineTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 6
- **Unit(s) under test:** `CompletionEngine` (complete Flow)
- **Evidence:** `fastProvider_itemsInFirstEmission` (line 100) asserts `first.any { it.label == "alpha" }` — requires the engine to actually emit items from the provider. `hangingProvider_doesNotHangFlow` (line 189) asserts `elapsedMs < 1_000L` — fails if `withTimeout` is removed (mutation documented at line 27). `finalEmission_containsUnionOfFastAndSlow` (line 208) asserts exact `lastLabels.size == 2` and both labels present.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/completion/providers/IdentifierProviderTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `IdentifierProvider` with real `OutlineExtractor` + `TokenizerEngine`
- **Evidence:** `headingPrefixH_returnsHello` (line 72) uses real tokenization pipeline (`engine.loadGrammar("markdown")`) and asserts `items.any { it.label == "Hello" }` + `items.none { it.label == "World" }`. `deduplication_sameNameOnce` (line 164) asserts `helloItems.size == 1` for two identical headings. Mutation procedure documented at lines 13-19 (always return emptyList → FAIL verified).
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/completion/providers/LspCompletionProviderTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 7
- **Unit(s) under test:** `LspCompletionProvider`, `lspCursorCharToLineCol`, `mapLspKindToItemKind`
- **Evidence:** `cursorCharToLineCol_multiLine` (line 78) asserts exact `(line=1, col=1)` for offset 5 in "foo\nbar" — a trivial stub returning `(0,0)` fails. `mapKind_FunctionMapsToIdentifier` (line 110) asserts `Kind.Identifier` for "Function" — stub returning `Word` fails. Mutation procedure documented at lines 13-30.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/completion/providers/SnippetProviderTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `SnippetProvider` with real `SnippetRegistry` loading from classpath
- **Evidence:** `markdown_prefixTab_returnsTableSnippet` (line 51) asserts `tableItem != null` with label "table" AND `tableItem.insertText.contains("|")` — requires real snippet file parsing. `emptyPrefix_returnsAllMarkdownSnippets` (line 121) asserts `items.size == allSnippets.size` from `SnippetRegistry`. Mutation documented at lines 12-15.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/completion/SnippetBundleCompletenessTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `SnippetRegistry.forLanguage` for 55 languages from classpath resources
- **Evidence:** `allLanguagesHaveAtLeastOneSnippet` (line 66) iterates all 55 lang IDs and fails with the list of empties. `allSnippetBodiesAreNonEmpty` (line 89) checks each body is non-blank. `distinctPrefixesAcrossLanguages` (line 138) asserts `prefixes.size >= 3` per lang. Mutation procedure documents three distinct failure scenarios at lines 13-20.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/completion/SnippetRegistryTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** `SnippetRegistry.forLanguage` (JVM classloader actual)
- **Evidence:** `markdown_snippets_loadTableAnchor` (line 44) asserts `tableSnippet != null` AND `tableSnippet.body.contains("|")` — a stub returning emptyList fails. Mutation documented at lines 13-16.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/completion/trigger/CompletionTriggerTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 6
- **Unit(s) under test:** `CompletionTrigger` (debounce, dismiss, re-arm logic)
- **Evidence:** `implicitTrigger_resetDebounceOnEachKeystroke` (line 254) asserts exactly 1 Show event despite 2 keystrokes — fails if debounce reset is removed (mutation documented at lines 235-243). `dismiss_thenImmediateRetype_doesNotAutoReopen` (line 295) asserts zero Shows after dismiss without short→long transition — fails if dismissed-state flag is removed (mutation documented lines 281-290).
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/format/memory/ParserMemoryTest.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 4
- **Unit(s) under test:** `MarkdownParser`, `CsvParser`, `TodoTxtParser`, `PlaintextParser`
- **Evidence:** All four test methods (`measureMarkdownParserMemory`, `measureCsvParserMemory`, `measureTodoTxtParserMemory`, `measurePlaintextParserMemory`) call `measureMemory(...)` which records `totalMemory - freeMemory` before and after parsing. **There are no assertions** on the recorded `MemoryStats` values anywhere in the file — the method only calls `stats.report()` (line 100) which prints to stdout. The tests will `PASS` regardless of what the parsers return, including if every parser is replaced with `fun parse(...) = ParsedDocument(...)` no-op stubs. Surviving mutant: replace `MarkdownParser.parse` to return a `ParsedDocument` with empty content — `measureMarkdownParserMemory` still PASSES because nothing asserts `avgMemoryBytes`, `contentSize`, or `memoryOverhead`.
- **Recommended fix:** Add assertions on the recorded `MemoryStats` — at minimum: `assertTrue(stats.avgMemoryBytes > 0, "Parser must allocate some memory")` and `assertTrue(stats.memoryOverhead < 10.0, "Memory overhead must be < 10x document size")` per the stated target in the class doc comment (lines 25-31). Without these, these are pure measurement-reporting tests, not quality gates.

### shared/src/desktopTest/kotlin/digital/vasic/yole/format/performance/ParserPerformanceTest.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 4
- **Unit(s) under test:** `MarkdownParser`, `CsvParser`, `TodoTxtParser`, `PlaintextParser`
- **Evidence:** All four test methods (`measureMarkdownParserPerformance`, `measureCsvParserPerformance`, `measureTodoTxtParserPerformance`, `measurePlaintextParserPerformance`) call `measureAndReport(...)` which measures durations and prints to stdout (lines 245-258). **There are no assertions** in `measureAndReport` — it only calls `println`. The `status` string is computed (line 333-338) but never used in an assertion. Every test PASSES if all parsers are replaced with instant no-ops. Surviving mutant: stub every `parse` method to return an empty `ParsedDocument` — all four tests still PASS because timing is only printed, never asserted.
- **Recommended fix:** Add performance-gate assertions to `measureAndReport`: e.g. `assertTrue(avg < 50.0, "$testName: Avg parse time ${avg}ms exceeds 50ms limit")`. The comments in the class doc state explicit targets (lines 22-26: "e.g. small doc < 100ms") — these must be wired to actual `assertTrue`/`fail` calls.

### shared/src/desktopTest/kotlin/digital/vasic/yole/format/SimpleCompilationTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 2
- **Unit(s) under test:** `FormatRegistry` (constants + `formats` list)
- **Evidence:** `test format registry constants exist` (line 21) asserts `assertEquals("markdown", FormatRegistry.ID_MARKDOWN)` at line 28 — a stub changing `ID_MARKDOWN` to `"STUB"` would fail. However, `test format registry has all core formats` (line 34) only asserts `formatIds.contains("markdown")` etc. — this is a metadata/constants check, not a behavioral assertion. The format IDs being present in `FormatRegistry.formats` does not verify that the associated parsers work correctly; a stub `createFormats()` returning `TextFormat(id="markdown", ...)` entries with broken parsers would PASS both tests. The test is SUSPECT rather than BLUFF because the constant-equality assertions at lines 28-31 would catch renamed constants.
- **Recommended fix:** Add a behavioral assertion: call `FormatRegistry.detectByContent("# heading\n")` and assert it returns a format with `id == "markdown"`, or parse a minimal document with the registry-resolved parser.

### shared/src/desktopTest/kotlin/digital/vasic/yole/format/startup/InitializationTest.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 6
- **Unit(s) under test:** `FormatRegistry`, `MarkdownParser`, `CsvParser`, `TodoTxtParser`, `PlaintextParser`
- **Evidence:** All six methods (`measureFormatRegistryInitialization`, `measureParserInstantiation`, `measureFormatDetection`, `measureExtensionDetection`, `measureFirstParseOperation`, `measureTotalStartupOverhead`) call `report(testName, times)`. The `report` method (line 311) computes avg/min/max and calls `println` — **no assertions are made on the measured times**. The `status` string ("✅ Excellent", "⚠️ Acceptable", "❌ Slow") is computed and printed but never asserted. A stub `FormatRegistry.detectByContent` returning `null` would cause a null-pointer internally, but `detectByExtension` and `formats.size` checks would still PASS. More fundamentally, stubbing `MarkdownParser.parse` to return an instant no-op means all timing methods still PASS with zero assertions failed. Surviving mutant: remove all computation from `FormatRegistry.getById` returning `null` always — none of the six tests assert on the returned `format` value (lines 68-80 use `val format = ...` but the result is only passed to `parser.parse` where the parser itself may handle `null` gracefully).
- **Recommended fix:** Add hard-limit assertions to `report`: e.g. `assertTrue(avgMs < 50.0, "$testName: avg ${avgMs}ms exceeds 50ms startup budget")`. The class doc states explicit targets (lines 32-37) — these must be enforced with real `assertTrue`/`fail` calls.

### shared/src/desktopTest/kotlin/digital/vasic/yole/format/taskpaper/TaskPaperParserSimpleTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 7
- **Unit(s) under test:** `TaskpaperParser`
- **Evidence:** `should parse basic TaskPaper document` (line 26) asserts `result.metadata["projects"] == "2"`, `metadata["tasks"] == "4"`, `metadata["notes"] == "1"` — a stub parser returning empty metadata fails. `should convert to HTML` (line 75) asserts `html.contains("<div class='taskpaper'>")` AND specific CSS class names present. `should validate content` (line 95) asserts invalid content produces non-empty errors.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/format/textile/TextileParserSimpleTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 7
- **Unit(s) under test:** `TextileParser`
- **Evidence:** `should convert to HTML` (line 100) asserts `html.contains("<h1>Test Document</h1>")` — this is a strong behavioral assertion. However, most other tests only assert `result.format.id == TextFormat.ID_TEXTILE` and `result.rawContent == content` or `result.metadata["lines"] == "7"` — these are metadata assertions that a stub returning `ParsedDocument(format=textileFormat, rawContent=content, metadata=mapOf("lines" to content.lines().size.toString()))` would satisfy without any real parsing. The `should parse bold text` (line 63) test only checks `result.rawContent == content`, not that bold was parsed. The test is SUSPECT because the HTML conversion test does verify real behavior but the majority of tests are metadata-only.
- **Recommended fix:** Add HTML-level assertions to the structural tests: e.g. in `should parse bold text`, assert `parser.toHtml(result).contains("<b>")` or `"<strong>"` to verify the bold syntax is actually processed.

### shared/src/desktopTest/kotlin/digital/vasic/yole/format/tiddlywiki/SimpleTiddlyWikiTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `TiddlyWikiParser`
- **Evidence:** `should parse basic TiddlyWiki document` (line 26) asserts `result.metadata["title"] == "My First Tiddler"`, `result.parsedContent.contains("Heading 1")`, and `result.parsedContent.contains("My First Tiddler")` — a no-op stub fails. `should convert TiddlyWiki to HTML` (line 63) asserts `html.contains("<strong>bold</strong>")`, `html.contains("<em>italic</em>")`, `html.contains("<a href='Wiki Link'>")` — extremely strong behavioral assertions. `should detect unclosed brackets in links` (line 111) asserts `issues.any { it.contains("Unclosed brackets") }`.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/format/wikitext/SimpleWikiTextTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 1
- **Unit(s) under test:** `WikitextParser`
- **Evidence:** `test basic WikiText parsing` (line 19) only asserts `result.format.id == TextFormat.ID_WIKITEXT` and `result.rawContent == content`. If `WikitextParser.parse` returns a `ParsedDocument` with the correct format ID and passes rawContent through unchanged (which a trivial stub would do), this test PASSES. There is no assertion that `= Main Title =` was parsed into any structure, no HTML conversion test, no metadata count assertion. A stub `fun parse(content: String) = ParsedDocument(format = wikitextFormat, rawContent = content)` completely satisfies this test.
- **Recommended fix:** Add `val html = parser.toHtml(result)` and assert `html.contains("<h1>")` or equivalent, plus metadata assertions like `result.parsedContent.contains("Main Title")`.

### shared/src/desktopTest/kotlin/digital/vasic/yole/import_/DocxImporterTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `DocxImporter` (real Apache POI pipeline)
- **Evidence:** Primary test (line 60) synthesises a real `.docx` in-memory via Apache POI APIs and asserts `md.contains("# Title")` and `md.contains("**World**") || md.contains("***World***")` and `md.contains("Hello")`. Mutation guard (line 89) verifies a failing stub produces `isFailure`. Malformed bytes test (line 123) asserts `error is ImportError.Malformed`. Full mutation procedure documented at lines 12-14.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/import_/EpubImporterTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 9
- **Unit(s) under test:** `EpubImporter` (real ZIP→jsoup→HtmlImporter pipeline)
- **Evidence:** Primary test (line 139) synthesises a 2-chapter EPUB in-memory and asserts `md.contains("# Chapter 1")`, `md.contains("Hello")`, `md.contains("---")`, `md.contains("# Chapter 2")`, `md.contains("World")`. Chapter ordering test (line 187) asserts `idx1 < idx2`. Metadata test `epubMetadata_prependsYamlFrontmatter` (line 279) asserts `md.startsWith("---")` and `md.contains("title: \"The Great Book\"")`. Unit test `buildYamlFrontmatter_allFields` (line 336) asserts each field present. Mutation procedures documented throughout.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/import_/HtmlImporterTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `HtmlImporter` (real jsoup + Flexmark pipeline)
- **Evidence:** `HtmlImporter converts h1 heading and bold text to Markdown` (line 30) asserts `md.contains("# Title")` and `md.contains("**bold**") || md.contains("__bold__")`. `HtmlImporter converts complex HTML` (line 93) asserts heading, plain text, strong, and italic markers all present. Mutation guard documents stub-failure verification.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/import_/OdtImporterTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 8
- **Unit(s) under test:** `OdtImporter` (ODFDOM path) + `parseOdtViaZipDesktop` (Android ZIP-path mirror)
- **Evidence:** `OdtImporter Desktop ODFDOM path imports heading and paragraph correctly` (line 101) synthesises real ODT via ODFDOM APIs and asserts `assertContains(md, "# Title")` and `assertContains(md, "Body")`. Android ZIP path test (line 144) runs both paths on same bytes and asserts both contain `"# Title"` and `"Body"`. List-nesting tests `odtAndroidPath_flatList_producesListItems` (line 401) asserts `md.contains("- Alpha")` and `"- Beta"`. `odtAndroidPath_nestedList_preservesIndentation` (line 425) asserts `"  - Inner"` (2-space indent).
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/import_/PdfImporterTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `PdfImporter` (PDFBox 3.x pipeline)
- **Evidence:** `PdfImporter maps large-font title to heading and body text to paragraph` (line 91) synthesises a real single-page PDF via PDFBox content-stream APIs and asserts `md.contains("# Title") || md.contains("## Title") ...` and `assertContains(md, "Body text")`. Malformed bytes test asserts `error is ImportError.Malformed`.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/import_/RtfImporterTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `RtfImporter` (RTF → Markdown pipeline)
- **Evidence:** Primary test (line 59) asserts `md.contains("**bold**") || md.contains("***bold***")` plus `assertContains(md, "Hello")` and `assertContains(md, "world")`. `rtfImporter_colouredText_emitsWarning` (line 133) asserts `doc.warnings.any { w.message.contains("colour", ...) }` — a stub omitting colour detection fails (mutation documented at lines 20-24).
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/language/Feature2LanguageSmokeTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `TokenizerEngine`, `OutlineExtractor`, `FoldQueryRunner` (real bonede JNI pipeline)
- **Evidence:** `markdownEndToEndProducesTokens` (line 99) asserts `tokens.isNotEmpty()`. `markdownEndToEndProducesOutlineItems` (line 114) asserts `items.isNotEmpty()`. `realTokenizationForAllBundledLangs` (line 165) iterates all bonede-bundled langs, runs real tokenization per fixture, fails with names of failing langs. `inputSmokeCheckForAllLanguages` (line 234) checks fixture + .scm structural validity with `@` captures. All mutations documented at lines 46-55.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/language/FoldQueryRunnerTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** `FoldQueryRunner` (real bonede TSQuery pipeline)
- **Evidence:** `markdownHeadingProducesFoldRange` (line 47) asserts `folds.isNotEmpty()` and `folds.any { it.endLine > it.startLine }` (multi-line fold). Coordinate sanity checks on all fold properties. `emptyInputProducesNoFolds` (line 105) asserts `size == 0`. Anti-bluff anchor documented at lines 16-19.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/language/LanguageMetadataCompletenessTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 6
- **Unit(s) under test:** `LanguageMetadata`, `ScmQueryLoader`, bundled classpath resources
- **Evidence:** `everyLanguageHasHighlightsScm` (line 57) loads each `.scm` resource and fails with the missing path. `loaderRoundtripWorksForEveryLanguage` (line 146) calls `ScmQueryLoader.load` for each lang/query combination and asserts non-blank. Deleting any `.scm` file causes a named failure. Anti-bluff anchor documented at lines 22-27.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/language/OutlineExtractorTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** `OutlineExtractor` (real bonede TSQuery pipeline)
- **Evidence:** `markdownHeadingsProduceOutlineItems` (line 48) asserts `items.size == 2` for two ATX headings, then checks `items[0].name.contains("H1")` and `items[1].name.contains("H2")`. A stub returning emptyList fails `assertEquals(2, items.size)` immediately. Anti-bluff anchor documented at lines 14-18.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/language/ScmQueryLoaderTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `ScmQueryLoader`
- **Evidence:** `loadMarkdownFolds` (line 37) asserts `content.contains("@fold")` in the loaded file content. `loadMarkdownOutline` (line 51) asserts `content.contains("@definition")`. `loadIsCached` (line 79) asserts reference equality `first === second`. A stub returning `""` fails all three.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/language/special/HtmlEmbeddedLangTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** `HtmlEmbeddedLang.tokenize` (real bonede HTML + CSS sub-engine)
- **Evidence:** `tokenizesEmbeddedCssInStyleElement` (line 70) filters tokens inside the `<style>` byte range and asserts at least one `cssScopedBodyTokens` whose scope is NOT in `htmlOuterLeafScopes` — a stub that skips CSS re-tokenization leaves only `raw_text` in the body and fails (documented line 107-115). `fallsBackToPlainHtmlWhenCssEngineNull` (line 119) asserts NO CSS-bluff scopes when `cssEngine=null`.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/language/special/MarkdownCodeFencesTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** `MarkdownCodeFences.tokenize` (real bonede Markdown + Kotlin sub-engine)
- **Evidence:** `tokenizesKotlinFenceWithKotlinEngine` (line 67) filters tokens inside the `\`\`\`kotlin` fence body and asserts `nonMarkdownBodyTokens.isNotEmpty()` — scopes not in `markdownFenceBodyLeafScopes`. A stub that skips sub-tokenization leaves only `code_fence_content`/`text` scopes and fails. `fallsBackToPlainMarkdownWhenSubEngineMissing` (line 119) asserts no Kotlin-bluff scopes with `emptyMap()`.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/CodeActionMappingTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** `mapLspCodeAction` (internal production helper)
- **Evidence:** `commandEither_mapsToCodeAction` (line 55) asserts `result.title == "Fix unused import"` and `result.command == "fix.unusedImport"` and `result.edit == null`. `codeActionEither_withEdit_mapsWorkspaceEdit` (line 75) asserts `result.edit!!.changes.containsKey("file:///project/Foo.kt")` and `result.edit!!.changes[...][0].newText == "newFoo"`. Mutation procedure at lines 13-18.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/DesktopLspSurfacesLogicTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `diagnosticOffsetToLine` (production pure-logic helper)
- **Evidence:** `offsetToLine_multiLine` (line 67) asserts exact line numbers for 6 specific offsets in "abc\nde\nfg" — a stub returning 0 always fails `assertEquals(2, diagnosticOffsetToLine(text, 7))`. `offsetToLine_beyondEnd_clampsToLastLine` (line 85) asserts line 1 for offset 999 in "ab\ncd". Mutation procedure at lines 14-19.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/DiagnosticsCacheTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `DiagnosticsCache`
- **Evidence:** `upsert_replaces_previous` (line 33) asserts `cache.diagnosticsFor` returns only `newDiag` after second upsert. `upsert_empty_clears_uri` (line 42) asserts empty list after upsert with emptyList. `cap_1000_truncates` (line 50) asserts `size == 1000` when 1500 inserted. Mutation procedure documented at lines 5-9.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/FormattingTriggerTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `FormattingTrigger` with `FakeFormattingRequester` (collaborator fake, not UUT mock)
- **Evidence:** Tests use a real `FormattingTrigger` instance with a fake requester for the LSP collaborator (correct mock usage — collaborator is mocked, not UUT). `onSave_appliesFormat_whenEnabled` (line 100) asserts `result.size == 2` and `formatter.callCount == 1`. `onSave_skipsFormat_whenDisabled` (line 145) asserts `formatter.callCount == 0`. `onType_appliesOnlyMatchingChar` (line 224) asserts `onTypeCallCount == 1` for matching char and `resultNonMatching.isEmpty()` for non-matching. Each mutation procedure documented.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/GoToDefinitionActionTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `GoToDefinitionAction.goToDefinition` with `FakeLspDefinitionRequester`
- **Evidence:** `one_result_pushes_and_opens` (line 113) asserts `stack.size == 1`, `pushed.uri == currentUri`, `openedUri == targetUri`, `openedCursor == targetRange.first`. `jdt_uri_routes_to_onOpenJdtUri` (line 207) asserts `jdtUriCalled == true`, `openFileCalled == false`, and `stack.size == 1`. Mutation procedures documented at lines 22-28.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/HoverInfoMappingTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** `mapHoverContentsToMarkdown` (production internal helper)
- **Evidence:** `markupContent_right_returnsValue` (line 53) asserts `result?.contents == "# Header\n\nbody"` with exact string match. `markedStringList_left_wrapsLanguageInFences` (line 73) asserts `text.contains("plain text")` and `text.contains("fn main() {}")`. Stub returning `null` fails both. Mutation documented at lines 12-18.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/HoverMarkdownRendererTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 8
- **Unit(s) under test:** `HoverMarkdownRenderer.render` (real Flexmark-based renderer)
- **Evidence:** `heading_extractsLevelAndText` (line 81) asserts `blocks[0] is HoverBlock.Heading` AND `heading.level == 1` AND `heading.text == "Title"`. `fencedCodeBlock_extractsLangAndCode` (line 100) asserts code block `lang == "rust"` AND `code.contains("fn main()")`. `mixedContent_orderedCorrectly` (line 168) asserts 3 specific block types in order. `rustAnalyzerStyle_signatureBlock` (line 194) asserts both CodeBlock and Paragraph with "macro". Stub returning FallbackText fails all structural checks. Mutation documented at lines 11-20.
- **Recommended fix:** none.

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/HoverTriggerDetectorTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 6
- **Unit(s) under test:** `HoverTriggerDetector` (real coroutine-based dwell logic)
- **Evidence:** `dwell_dispatches_after_300ms` (line 95) asserts `dwellCalls.size == 1` and `dwellCalls[0] == (5, 12)`. `dwell_cancels_on_subsequent_move` (line 116) asserts `dwellCalls.size == 1` (only last fires). `dwell_skips_when_completion_popup_open` (line 140) asserts `dwellCalls.size == 0`. `dismiss_cancels_pending_dwell` (line 207) asserts `dwellCalls.size == 0` after dismiss. All five mutation procedures documented.
- **Recommended fix:** none.
