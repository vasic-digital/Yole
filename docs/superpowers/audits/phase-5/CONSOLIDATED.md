# Phase 5 Anti-Bluff Audit — Consolidated Findings

> **Generated from:** B01–B15 batch audit files  
> **Audit scope:** ~480 files/scripts across 15 batches  
> **Rule authority:** CONST-035, CONST-039  
> **Do not modify** the source B##-*.md files. This document is the single actionable backlog.

---

## Section 1 — Summary Counts

| Verdict | Actual Count | README Estimate | Delta |
|---------|-------------|-----------------|-------|
| BLUFF   | 55          | ~51             | +4    |
| SUSPECT | 103         | ~104             | -1    |
| **Total non-CLEAN** | **158** | **~155** | **+3** |

**CLEAN** items are not listed in this document; consult individual B##-*.md files.

Counts by batch:

| Batch | Files audited | BLUFF | SUSPECT |
|-------|--------------|-------|---------|
| B01-format-1     | 32 | 4  | 10 |
| B02-format-2     | 32 | 5  | 10 |
| B03-format-3     | 32 | 2  | 9  |
| B04-network-1    | 32 | 1  | 4  |
| B05-network-2    | 32 | 3  | 19 |
| B06-network-3    | 32 | 8  | 4  |
| B07-common-1     | 28 | 4  | 4  |
| B08-common-2     | 28 | 0  | 8  |
| B09-common-3     | 26 | 5  | 4  |
| B10-desktop-1    | 36 | 3  | 2  |
| B11-desktop-2    | 35 | 6  | 6  |
| B12-android-1    | 28 | 4  | 5  |
| B13-android-2    | 28 | 0  | 4  |
| B14-misc-platforms | 26 | 5  | 11 |
| B15-challenge-scripts | 39 | 5  | 3  |
| **Total** | **476** | **55** | **103** |

---

## Section 2 — Systemic Patterns

| ID | Name | Description | # Affected P5-FIX entries | Fix Technique |
|----|------|-------------|--------------------------|---------------|
| PAT-01 | `isSuccess \|\| isFailure` tautology | `assertTrue(result.isSuccess \|\| result.isFailure)` is structurally `assertTrue(true)` because Kotlin's `Result<T>` is a sealed class with exactly two subtypes. Zero information about correctness. | ~40 | Replace with explicit `assertTrue(result.isSuccess)` + `assertEquals(expectedValue, result.getOrThrow())` |
| PAT-02 | Performance/timing-only assertions | `assertTrue(elapsed < Xms)` with no correctness assertion. A no-op stub completes in 0 ms and passes every timing gate. | ~12 | Add correctness assertion first; only add timing constraint as a secondary guard |
| PAT-03 | `assertNotNull`-only / absence-of-error | `assertNotNull(result)` or `assert(!result.isNullOrEmpty())` with no content validation. A stub returning `""` or `emptyList()` passes. | ~20 | Assert specific expected tokens/keys/values present in output |
| PAT-04 | Self-verifying test doubles | Test exercises an inline `Fake*` or `Test*` class that shares zero code paths with production. Production class is never instantiated. | ~14 | Instantiate the real production class; use test doubles only for external dependencies (HTTP, file I/O) |
| PAT-05 | Structural source-grep tests | Test regex-scans source text for structural markers (function names, annotations). A runtime mutation that removes behavior but not structure passes. | ~8 | Replace with behavioral invocation: call the function, observe the output |
| PAT-06 | Metadata/constant-only assertions | Tests verify format IDs, enum counts, constants — never invoke the parser/formatter. Stub implementations with the right constant values pass. | ~10 | Add at least one round-trip parse assertion per format under test |
| PAT-07 | Challenge exit-3 SKIP-OK with zero evidence | Challenge script exits 3 (treated as PASS) when a runtime dependency (Puppeteer, ADB, Pitest) is absent. On most developer machines these gates never execute. | ~6 | Emit a `WARN` log and a non-zero exit code when the gate cannot run; distinguish SKIP from PASS |
| PAT-08 | One-off hardcoded / stale constants | Hardcoded quality scores (`= 1.0`), stale version constants (`EXPECTED_VERSION = "2.0.0"`), or fabricated return values. These are individual bugs, not a pattern fix. | ~6 | Update constants to match real values; drive scores from runtime measurement |
| PAT-09 | Tautological boolean `x \|\| !x` or `a \|\| a` | `assertTrue(initialized \|\| !initialized)`, `url.contains("scope=files.read") \|\| url.contains("scope=files.read")` — structurally `assertTrue(true)`. | ~8 | Delete the condition; write the real assertion |
| PAT-10 | rawContent round-trip tautology | `assertEquals(doc1.rawContent, doc2.rawContent)` where both documents were built from the same input string. Asserts input equals input, never exercises the parser. | ~4 | Assert `doc.toHtml()` or `doc.parsedContent` contains expected transformed tokens |
| PAT-11 | Self-referential input assertion | `assertTrue(content.contains("# Header 1"))` where `content` is the string the test itself constructed before passing to the parser. The parser output is never inspected. | ~3 | Assert against the return value / output of the unit under test, not the input |
| PAT-12 | `assertNotNull` on Kotlin non-nullable type | `assertNotNull(nonNullableValue)` always passes because the Kotlin type system guarantees non-null; the assertion adds zero information. | ~5 | Remove the assertion or replace with a meaningful value check |

---

## Section 3 — Full P5-FIX Backlog

> Severity: **CRITICAL** = assertion never exercises the feature, likely hiding a real broken feature visible to end-users.  
> **HIGH** = assertion exercises the feature but provides insufficient evidence.  
> **MEDIUM** = assertion is weak/noisy but provides some evidence.

### B01 — Format-1

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-001 ✅FIXED 8a1da082 | B01 | `FormatParserSecurityTests.kt` | `testXssInMarkdown`, `testXssInHtml`, `testXssInAsciidoc` (and 14 others, all 17 parsers) | BLUFF | PAT-04 | CRITICAL | Each test must call the real parser's `parse(input).toHtml()` and assert the output does not contain raw `<script>` tags. The current test calls `escapeHtml()` utility on a hardcoded string — no parser is ever exercised. |
| P5-FIX-002 | B01 | `FormatCoverageTest.kt` | `testAllFormatsRegistered`, `testEachFormatHasParser` | BLUFF | PAT-03 | HIGH | Add a round-trip parse assertion: call `parse("<minimal valid document for format>")` and assert `toHtml()` is non-empty and contains expected markers. |
| P5-FIX-003 | B01 | `FormatDetectionPriorityTest.kt` | `testExtensionDetectionOrder`, `testContentDetectionFallback` | BLUFF | PAT-06 | HIGH | Assert the detected `TextFormat.id` equals the expected string constant; do not stop at `assertNotNull(detected)`. |
| P5-FIX-004 | B01 | `StyleSheetCacheTest.kt` | `testCacheHitOnSecondCall`, `testCacheMissAfterClear` | BLUFF | PAT-02 | MEDIUM | Assert CSS string content, not only timing. A stub that returns `""` in 0 ms passes both existing assertions. |
| P5-FIX-005 | B01 | `MarkdownParserTests.kt` | `testCodeBlockRoundTrip`, `testTableRoundTrip` | SUSPECT | PAT-10 | HIGH | Build `input` string; parse it; assert `toHtml()` contains expected HTML tags (`<code>`, `<table>`), not just `rawContent` equality. |
| P5-FIX-006 | B01 | `CsvParserTests.kt` | `testParseEmptyFile`, `testParseSingleRow` | SUSPECT | PAT-03 | MEDIUM | Assert column count and cell content, not only `assertNotNull(parsed)`. |
| P5-FIX-007 | B01 | `HtmlFormatTests.kt` | `testHtmlEscape*` (6 methods) | SUSPECT | PAT-06 | MEDIUM | Assert the output HTML string contains the escaped form; not only that escaping did not throw. |
| P5-FIX-008 | B01 | `LaTex*Tests.kt` | `testLatexFormulaRender` | SUSPECT | PAT-03 | MEDIUM | Assert rendered output contains expected token (e.g., the formula variable name). |
| P5-FIX-009 | B01 | `JsonParserTests.kt` | `testJsonSchemaValidation` | SUSPECT | PAT-03 | MEDIUM | Assert specific key names are present in `parsedContent`; `assertNotNull` is insufficient. |
| P5-FIX-010 | B01 | `DocumentCacheTest.kt` | `testCacheEviction`, `testCacheCapacityLimit` | SUSPECT | PAT-02 | MEDIUM | Assert LRU eviction correctness: insert N+1 items, check oldest key is absent; do not rely solely on timing. |
| P5-FIX-011 | B01 | `FormatRegistryInitTest.kt` | `testLazyLoadingFirstAccess` | SUSPECT | PAT-02 | MEDIUM | Assert `formats.size >= expectedMinimum` in addition to timing assertion. |
| P5-FIX-012 | B01 | `TodoTxtParserTests.kt` | `testPriorityFilter`, `testProjectTag` | SUSPECT | PAT-03 | MEDIUM | Assert filtered list size and contents; do not stop at `assertNotNull`. |
| P5-FIX-013 | B01 | `WikitextParserTests.kt` | `testInternalLinkTransform` | SUSPECT | PAT-03 | MEDIUM | Assert output HTML contains `<a href="...">` with the expected link target. |
| P5-FIX-014 | B01 | `AsciidocParserTests.kt` | `testSectionHeaderConversion` | SUSPECT | PAT-03 | MEDIUM | Assert output HTML contains `<h1>`/`<h2>` elements for each section. |

### B02 — Format-2

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-015 | B02 | `PropertyBasedFormatTests.kt` | `testFormatRoundTrip_*` (153 of 170 tests) | BLUFF | PAT-03 | CRITICAL | 153 tests assert only `assertNotNull(result)`. Add property: `result.rawContent == input` and `result.errors.isEmpty()` for valid inputs; assert `result.errors.isNotEmpty()` for known-invalid inputs. |
| P5-FIX-016 | B02 | `LazyLoadingValidationTests.kt` | `testInitializationComplete` | BLUFF | PAT-09 | HIGH | `assertTrue(initialized || !initialized)` is always true. Assert `assertTrue(initialized)` and verify `FormatRegistry.isFormatsInitialized` is true after the call. |
| P5-FIX-017 | B02 | `DocumentCacheStressTest.kt` | `testConcurrentEviction` | BLUFF | PAT-02 | HIGH | Add assertion that cache size never exceeds capacity after concurrent inserts; `elapsed < Xms` alone does not verify eviction correctness. |
| P5-FIX-018 | B02 | `StyleSheetThemeTest.kt` | `testDarkThemeHasDarkBackground` | BLUFF | PAT-11 | HIGH | Assert that the CSS string returned by `StyleSheets.generate(Theme.DARK)` contains a dark background hex code; do not assert the input enum name. |
| P5-FIX-019 | B02 | `FormatRegistryThreadSafetyTest.kt` | `testConcurrentDetection` | BLUFF | PAT-02 | MEDIUM | Verify all threads returned the same detected format ID; timing only is insufficient. |
| P5-FIX-020 | B02 | `MarkdownExtensionsTest.kt` | `testStrikethrough`, `testTaskList`, `testFootnote` | SUSPECT | PAT-10 | HIGH | Assert `toHtml()` output contains `<del>`, `<input type="checkbox">`, `<footnote>` markers. |
| P5-FIX-021 | B02 | `YamlFrontMatterTest.kt` | `testFrontMatterExtraction` | SUSPECT | PAT-03 | MEDIUM | Assert that `metadata["title"]` equals the expected title string from the fixture. |
| P5-FIX-022 | B02 | `EpubParserTests.kt` | `testChapterExtraction` | SUSPECT | PAT-03 | MEDIUM | Assert chapter count and first chapter title. |
| P5-FIX-023 | B02 | `RstParserTests.kt` | `testDirectiveRender` | SUSPECT | PAT-03 | MEDIUM | Assert rendered HTML contains directive output. |
| P5-FIX-024 | B02 | `DokuWikiParserTests.kt` | `testTableParse` | SUSPECT | PAT-03 | MEDIUM | Assert column count and cell values in parsed output. |
| P5-FIX-025 | B02 | `MediaWikiParserTests.kt` | `testTemplateParse` | SUSPECT | PAT-03 | MEDIUM | Assert template expansion produces expected token in output. |
| P5-FIX-026 | B02 | `TextileParserTests.kt` | `testBoldItalicConversion` | SUSPECT | PAT-03 | MEDIUM | Assert `<strong>` and `<em>` tags present in output HTML. |
| P5-FIX-027 | B02 | `OrgModeParserTests.kt` | `testHeadingLevels` | SUSPECT | PAT-03 | MEDIUM | Assert `<h1>`–`<h6>` count matches fixture heading count. |
| P5-FIX-028 | B02 | `PdfParserTests.kt` | `testTextExtraction` | SUSPECT | PAT-03 | MEDIUM | Assert extracted text contains at least one known sentence from the fixture PDF. |
| P5-FIX-029 | B02 | `FormatMigrationTest.kt` | `testMarkdownToHtml` | SUSPECT | PAT-10 | HIGH | Assert that the migration output is structurally different from the input (i.e., `.md` syntax is transformed to HTML tags). |

### B03 — Format-3

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-030 | B03 | `UltimateSupremacyTest.kt` | `testQualitySupremacy`, `calculateMemoryEfficiencyScore()` | BLUFF | PAT-08 | CRITICAL | Remove hardcoded `qualityMetrics["Test Coverage"] = 1.0`. Derive the value from Kover report or a real measurement. `calculateMemoryEfficiencyScore()` must allocate real documents and measure actual heap growth. |
| P5-FIX-031 | B03 | `UltimateSupremacyTest.kt` | outer `try/catch` block swallowing exceptions | BLUFF | PAT-08 | HIGH | Remove the outer `catch (e: Exception) { /* pass */ }` — this causes assertion failures inside to be silently swallowed, making the entire test class permanently PASS. |
| P5-FIX-032 | B03 | `FormatComplianceTest.kt` | `testRfc*Compliance` (9 methods) | SUSPECT | PAT-03 | HIGH | Assert specific compliance properties (e.g., for CSV: quoted-field handling, embedded newlines). `assertNotNull(parsed)` does not verify RFC compliance. |
| P5-FIX-033 | B03 | `DocumentModelIntegrationTest.kt` | `testCreateAndParse` | SUSPECT | PAT-10 | HIGH | Assert `document.toHtml()` contains structural markers expected from the fixture input; do not only compare `rawContent` to itself. |
| P5-FIX-034 | B03 | `ParserErrorHandlingTest.kt` | `testMalformedInput*` (7 methods) | SUSPECT | PAT-03 | MEDIUM | Assert `result.errors.isNotEmpty()` and that `result.errors[0].message` contains a meaningful description; `assertNotNull(result)` does not verify error handling. |
| P5-FIX-035 | B03 | `FormatDetectionEdgeCaseTest.kt` | `testBinaryFileDetection`, `testEmptyFileDetection` | SUSPECT | PAT-03 | MEDIUM | Assert detected format ID equals `TextFormat.ID_PLAIN` for binary; assert `null` or fallback for empty. |
| P5-FIX-036 | B03 | `StyleSheetEdgeCaseTest.kt` | `testVeryLongIdentifier`, `testUnicodeSelector` | SUSPECT | PAT-03 | MEDIUM | Assert output CSS does not contain the raw identifier unescaped. |
| P5-FIX-037 | B03 | `ParsedDocumentTest.kt` | `testLazyCaching` | SUSPECT | PAT-02 | MEDIUM | Assert second `toHtml()` call returns same object reference (same instance) in addition to timing check. |
| P5-FIX-038 | B03 | `FormatCompatibilityTest.kt` | `testCrossFormatLinks` | SUSPECT | PAT-06 | MEDIUM | Assert link target survives round-trip through both formats; do not stop at `assertNotNull`. |
| P5-FIX-039 | B03 | `EncoderDecoderTest.kt` | `testBase64RoundTrip`, `testUtf8RoundTrip` | SUSPECT | PAT-03 | MEDIUM | Assert the decoded string equals the original input character-for-character; `assertNotNull` is insufficient. |
| P5-FIX-040 | B03 | `CsvEdgeCaseTest.kt` | `testQuotedComma`, `testEmbeddedNewline` | SUSPECT | PAT-03 | MEDIUM | Assert specific cell values from fixture; stub returning single empty row passes current assertions. |

### B04 — Network-1

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-041 | B04 | `AuthTokenManagerStressTest.kt` | all 17 stress test methods | BLUFF | PAT-04 | CRITICAL | All 17 methods instantiate `TestAuthTokenManagerImpl` (a test-local class). Rewrite to use `AuthTokenManager` (the real production class) with a stubbed `TokenStorage` dependency. |
| P5-FIX-042 | B04 | `OAuth2FlowUrlTest.kt` | `testScopeIncludesFilesRead` | SUSPECT | PAT-09 | HIGH | `url.contains("scope=files.read") || url.contains("scope=files.read")` — identical operands, always evaluates to one check. Fix to assert the URL is well-formed and contains the correct scope value. |
| P5-FIX-043 | B04 | `AuthTokenRefreshTest.kt` | `testRefreshOnExpiry`, `testRefreshTokenRotation` | SUSPECT | PAT-04 | HIGH | Use real `AuthTokenManager`; stub only the HTTP call; assert that `accessToken` is updated post-refresh. |
| P5-FIX-044 | B04 | `OAuth2StateValidationTest.kt` | `testStateMismatchRejected` | SUSPECT | PAT-03 | MEDIUM | Assert that mismatched state causes `Result.failure` with a specific exception message. |
| P5-FIX-045 | B04 | `TokenStorageEncryptionTest.kt` | `testStoredTokenIsEncrypted` | SUSPECT | PAT-03 | MEDIUM | Assert that the stored bytes are not equal to the plaintext token string. |

### B05 — Network-2

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-046 | B05 | `ProtocolSupremacyTests.kt` | all 40 test methods | BLUFF | PAT-01 | CRITICAL | Every method ends with `assertTrue(r.isSuccess || r.isFailure)`. Replace with `assertTrue(r.isSuccess)` and `assertNotNull(r.getOrNull())` where success is expected; assert `r.isFailure` with specific exception type where failure is expected. |
| P5-FIX-047 | B05 | `NetworkIntegrationComprehensiveTest.kt` | `testUploadDownloadRoundTrip`, `testLargeFileTransfer` | BLUFF | PAT-04 | CRITICAL | Tests use `simulateUpload()` / `simulateDownload()` local helpers that call `delay()` only. Replace with a real mock HTTP server (e.g., MockWebServer) or at minimum the real service class with a stubbed HTTP client. |
| P5-FIX-048 | B05 | `NetworkPerformanceTest.kt` | `testUploadThroughput`, `testDownloadLatency`, `testConcurrentOps` | BLUFF | PAT-02 | HIGH | Throughput is measured against `delay()` wrappers, not real services. Either remove the test or instrument real protocol service calls through a local mock HTTP server. |
| P5-FIX-049 | B05 | `DropboxServiceTest.kt` | `testListFiles`, `testDeleteFile`, `testMoveFile` | SUSPECT | PAT-01 | HIGH | Replace `assertTrue(result.isSuccess || result.isFailure)` with `assertTrue(result.isSuccess); assertEquals(expectedFileList, result.getOrThrow())`. |
| P5-FIX-050 | B05 | `FtpServiceTest.kt` | `testConnect`, `testListDirectory`, `testUploadFile` | SUSPECT | PAT-01 | HIGH | Same fix as P5-FIX-049. |
| P5-FIX-051 | B05 | `SmbServiceTest.kt` | `testShareEnumeration`, `testFileRead` | SUSPECT | PAT-01 | HIGH | Same fix as P5-FIX-049. |
| P5-FIX-052 | B05 | `WebDavServiceTest.kt` | `testPropfind`, `testPutFile` | SUSPECT | PAT-01 | HIGH | Same fix as P5-FIX-049. |
| P5-FIX-053 | B05 | `NextcloudServiceTest.kt` | `testShareCreate`, `testShareRevoke` | SUSPECT | PAT-01 | HIGH | Same fix as P5-FIX-049. |
| P5-FIX-054 | B05 | `CircuitBreakerTest.kt` | `testCircuitOpensAfterNFailures`, `testHalfOpenTransition` | SUSPECT | PAT-03 | MEDIUM | Assert `circuitBreaker.state == CircuitBreakerState.OPEN` after N failures; `assertNotNull(result)` is insufficient. |
| P5-FIX-055 | B05 | `ConnectionLimiterTest.kt` | `testMaxConcurrentEnforced` | SUSPECT | PAT-03 | MEDIUM | Assert that the (N+1)th concurrent acquire call either blocks or fails; do not only assert no exception. |
| P5-FIX-056 | B05 | `PathUtilsTest.kt` | `testNormalizePath*` (8 methods) | SUSPECT | PAT-03 | MEDIUM | Assert the exact normalized string for each input; `assertNotNull` is insufficient. |
| P5-FIX-057 | B05 | `NetworkStorageConfigServiceTest.kt` | `testConfigEmission`, `testConfigUpdate` | SUSPECT | PAT-03 | MEDIUM | Assert that `StateFlow.value` reflects the expected config object after the update call. |
| P5-FIX-058 | B05 | `RetryPolicyTest.kt` | `testExponentialBackoff`, `testMaxRetries` | SUSPECT | PAT-03 | MEDIUM | Assert the exact number of attempts made and the delay intervals (use a fake clock or capture timestamps). |
| P5-FIX-059 | B05 | `NetworkErrorCategoryTest.kt` | `testTransientVsPermanent` | SUSPECT | PAT-03 | MEDIUM | Assert that `IOException` is categorized as `TRANSIENT` and `AuthException` as `PERMANENT`. |
| P5-FIX-060 | B05 | `StorageScopeTest.kt` | `testScopeCleanupOnCancel` | SUSPECT | PAT-03 | MEDIUM | Assert that the scope's coroutine count is 0 after cancellation. |
| P5-FIX-061 | B05 | `BandwidthThrottleTest.kt` | `testThrottleEnforced` | SUSPECT | PAT-02 | MEDIUM | Assert that throughput stays within the configured limit; timing alone is insufficient. |
| P5-FIX-062 | B05 | `NetworkMetricsTest.kt` | `testRequestCountIncrement` | SUSPECT | PAT-03 | MEDIUM | Assert counter value is exactly `N` after N calls. |
| P5-FIX-063 | B05 | `ServiceScopeLifecycleTest.kt` | `testScopeActiveAfterStart` | SUSPECT | PAT-09 | MEDIUM | `assertTrue(scope.isActive || !scope.isActive)` is always true. Assert `assertTrue(scope.isActive)`. |
| P5-FIX-064 | B05 | `UploadQueueTest.kt` | `testEnqueueAndFlush` | SUSPECT | PAT-03 | MEDIUM | Assert queue size reaches expected value; then assert it drops to 0 after flush. |

### B06 — Network-3

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-065 | B06 | `GoogleDriveServiceTest.kt` | `testListFilesInFolder`, `testUploadDocument`, `testDeleteDocument`, `testMoveDocument` (and ~6 more) | BLUFF | PAT-01 | HIGH | All methods use `assertTrue(result.isSuccess || result.isFailure)`. Replace with `assertTrue(result.isSuccess)` and assert response content. |
| P5-FIX-066 | B06 | `OneDriveServiceTest.kt` | `testGetFileMetadata`, `testUploadLargeFile`, `testCreateFolder` (and ~5 more) | BLUFF | PAT-01 | HIGH | Same fix as P5-FIX-065. |
| P5-FIX-067 | B06 | `SftpServiceTest.kt` | `testConnectWithPassword`, `testConnectWithKey`, `testListRemote`, `testDownload` (and ~4 more) | BLUFF | PAT-01 | HIGH | Same fix as P5-FIX-065. |
| P5-FIX-068 | B06 | `SafetyFixesTest.kt` | `testPathTraversalBlocked_*` (~20 methods) | BLUFF | PAT-01 | CRITICAL | Methods assert `assertTrue(result.isSuccess || result.isFailure)` after path-traversal attempts. A service implementation that permits traversal and returns success passes every test. Replace with `assertTrue(result.isFailure)` and assert the exception type is `SecurityException` or `IllegalArgumentException`. |
| P5-FIX-069 | B06 | `DropboxOAuth2ServiceTest.kt` | `testTokenExchangeSuccess`, `testTokenRefreshFlow` | SUSPECT | PAT-01 | HIGH | Replace tautological assertions with `assertTrue(result.isSuccess); assertNotNull(result.getOrNull()?.accessToken)`. |
| P5-FIX-070 | B06 | `GoogleDriveOAuth2Test.kt` | `testConsentScreenRedirect`, `testScopeNegotiation` | SUSPECT | PAT-01 | HIGH | Same fix as P5-FIX-069. |
| P5-FIX-071 | B06 | `OneDriveOAuth2Test.kt` | `testPkceFlow`, `testOfflineAccessScope` | SUSPECT | PAT-01 | HIGH | Same fix as P5-FIX-069. |
| P5-FIX-072 | B06 | `SshKeyValidationTest.kt` | `testValidRsaKey`, `testInvalidKeyRejected` | SUSPECT | PAT-03 | MEDIUM | Assert key fingerprint value for valid key; assert specific exception for invalid key. |

### B07 — Common-1

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-073 | B07 | `ApiConsistencyTest.kt` | `testAll*ServiceConformance` (one method per service, 8 total) | BLUFF | PAT-01 | HIGH | All 8 methods use `assertTrue(result.isSuccess || result.isFailure)`. Add real assertions: call a known method, assert known response shape. |
| P5-FIX-074 | B07 | `VolatileFieldSafetyTests.kt` | `testConcurrentFlagAccess_*` (8 methods) | BLUFF | PAT-09 | HIGH | All end with `assertTrue(true, "No exception thrown")`. Replace with a data-race detector approach: assert all concurrent reads returned the same value. |
| P5-FIX-075 | B07 | `LockOrderingTest.kt` | `testLockAcquisitionOrder` | BLUFF | PAT-05 | HIGH | Test scans source for lock field names; it cannot detect runtime reordering. Add a runtime test: acquire locks in both orders from competing threads and assert no deadlock occurs (use timeout). |
| P5-FIX-076 | B07 | `ServiceShutdownTest.kt` | `testGracefulShutdown` | BLUFF | PAT-09 | MEDIUM | `assertTrue(completed || !completed)` is always true. Assert `assertTrue(completed)` and verify active coroutine count is 0. |
| P5-FIX-077 | B07 | `MonitoringIntegrationTest.kt` | `testMetricsAggregation` | SUSPECT | PAT-03 | HIGH | Assert specific metric values (e.g., `requestCount == 5`) after 5 calls; `assertNotNull(snapshot)` is insufficient. |
| P5-FIX-078 | B07 | `CancellationHandlingTest.kt` | `testCancellationRethrown_*` (5 methods) | SUSPECT | PAT-03 | MEDIUM | Assert that `CancellationException` propagates to the caller; do not only assert it does not reach the catch block. |
| P5-FIX-079 | B07 | `ServiceInitOrderTest.kt` | `testInitializationSequence` | SUSPECT | PAT-06 | MEDIUM | Assert that a service call before `init()` throws `IllegalStateException`; `assertNotNull(service)` does not verify init order. |
| P5-FIX-080 | B07 | `GlobalScopeAbsenceTest.kt` | `testNoGlobalScopeInSource` | SUSPECT | PAT-05 | MEDIUM | Add a runtime complement: create the service, cancel its scope, and assert inflight coroutines complete; source scan alone does not verify runtime behavior. |

### B08 — Common-2

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-081 | B08 | `DocumentAdvancedTest.kt` | `testDocumentEquality`, `testDocumentCopy` | SUSPECT | PAT-06 | HIGH | Assert behavioral properties: after `copy()`, mutating copy does not affect original; equality is based on content, not reference. |
| P5-FIX-082 | B08 | `DocumentFormatTest.kt` | `testFormatIdStability`, `testFormatDisplayName` | SUSPECT | PAT-06 | MEDIUM | Assert format ID constant values (not just non-null); assert display name is non-blank and human-readable. |
| P5-FIX-083 | B08 | `DocumentModelTests.kt` | `testCreateDocument`, `testUpdateContent` | SUSPECT | PAT-06 | HIGH | Assert document content is updated after `update()`; `assertNotNull(doc)` does not verify update semantics. |
| P5-FIX-084 | B08 | `FormatDetectionConditionalTest.kt` | `testDetectByExtension_if_detected` | SUSPECT | PAT-03 | HIGH | Remove the conditional: `if (detected) assertEquals(...)`. If detection can return null for this input, the test spec is wrong; fix the test to always assert. |
| P5-FIX-085 | B08 | `ParsedDocumentCacheTest.kt` | `testHtmlCacheCoherence` | SUSPECT | PAT-02 | MEDIUM | Assert HTML string equality across calls, not only timing. |
| P5-FIX-086 | B08 | `MetricsSnapshotTest.kt` | `testSnapshotImmutability` | SUSPECT | PAT-03 | MEDIUM | Assert that mutating the snapshot copy does not affect the original object. |
| P5-FIX-087 | B08 | `PerformanceMetricsTest.kt` | `testP99Latency` | SUSPECT | PAT-02 | MEDIUM | Assert that inserting a single 1000 ms observation raises P99 above 900 ms; pure timing assertion without constructed data is insufficient. |
| P5-FIX-088 | B08 | `MetricsReporterTest.kt` | `testReportFlush` | SUSPECT | PAT-03 | MEDIUM | Assert that after `flush()`, the buffer is empty and emitted events match. |

### B09 — Common-3

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-089 ✅FIXED 8a1da082 | B09 | `SecurityValidationTests.kt` | `assertNoRawScript()` helper + all 13 methods using it | BLUFF | PAT-11 | CRITICAL | `assertNoRawScript(html)` runs `input.escapeHtml()` on a hardcoded string and never inspects the `html` argument. Fix: assert `assertFalse(html.contains("<script"), "raw script tag found in parser output")`. |
| P5-FIX-090 | B09 | `AccessibilityTest.kt` | `testScreenReaderSupport`, `testFocusTraversal` | BLUFF | PAT-09 | HIGH | `assertTrue(isActive || !isActive)` is always true. Assert `assertTrue(isActive)` and that expected accessibility nodes exist. |
| P5-FIX-091 | B09 | `AnimationConstantsTest.kt` | `testEnterTransitionNotNull` and 11 similar | BLUFF | PAT-03 | MEDIUM | Assert that `transition != EnterTransition.None` (a stub returns `None`, which is non-null). |
| P5-FIX-092 | B09 | `AnimationTests.kt` | `testTransitionDuration_*` (14 methods) | BLUFF | PAT-03 | MEDIUM | Assert duration value > 0 ms; `assertNotNull(transition)` passes for `EnterTransition.None` stub. |
| P5-FIX-093 | B09 | `ThemeColorTest.kt` | `testDarkThemeColors` | BLUFF | PAT-06 | MEDIUM | Assert specific color values (e.g., surface color has luminance < 0.5 for dark theme); `assertNotNull` does not verify dark-ness. |
| P5-FIX-094 | B09 | `ComposableLayoutTest.kt` | `testEditorLayoutNonEmpty` | SUSPECT | PAT-03 | MEDIUM | Assert node count > 0 and that the editor composable node exists; `assertNotNull(root)` is insufficient. |
| P5-FIX-095 | B09 | `InputHandlerTest.kt` | `testKeyboardShortcuts` | SUSPECT | PAT-03 | MEDIUM | Assert that the expected command was dispatched for each shortcut; do not only assert no exception. |
| P5-FIX-096 | B09 | `FontLoadingTest.kt` | `testCustomFontLoaded` | SUSPECT | PAT-03 | MEDIUM | Assert font family name and weight are non-default values. |
| P5-FIX-097 | B09 | `UiStateTest.kt` | `testStateTransitions` | SUSPECT | PAT-03 | MEDIUM | Assert that each state transition produces the expected `UiState` enum value. |

### B10 — Desktop-1

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-098 | B10 | `ParserMemoryTest.kt` | `testMemoryFootprintLargeDocument`, `testMemoryFootprintManyDocuments` | BLUFF | PAT-02 | HIGH | Both methods contain only `println()` calls; no assertions. Add `assertTrue(memoryUsed < maxAllowedBytes)` with a real heap measurement. |
| P5-FIX-099 | B10 | `ParserPerformanceTest.kt` | `testParseSpeed*` (all methods) | BLUFF | PAT-02 | HIGH | Same as P5-FIX-098: `println()` only. Add `assertTrue(elapsed < maxAllowedMs)` and also assert parse result is non-empty. |
| P5-FIX-100 | B10 | `InitializationTest.kt` | `testFormatRegistryStartupTime` | BLUFF | PAT-02 | MEDIUM | `println("Startup time: $ms")` — no assertion. Add `assertTrue(ms < 1000)`. |
| P5-FIX-101 | B10 | `DesktopFileSystemTest.kt` | `testTempDirCreation` | SUSPECT | PAT-03 | MEDIUM | Assert that the directory exists on disk after creation; `assertNotNull(dir)` does not verify filesystem side-effect. |
| P5-FIX-102 | B10 | `DesktopClipboardTest.kt` | `testClipboardWriteRead` | SUSPECT | PAT-03 | MEDIUM | Assert that the string read back from clipboard equals the string written. |

### B11 — Desktop-2

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-103 | B11 | `LspCodeActionRequesterTest.kt` | all test methods | BLUFF | PAT-04 | CRITICAL | All tests exercise `FakeLspCodeActionRequester` (test-local class). The production `LspCodeActionRequester` is never instantiated. Rewrite to instantiate production class; stub only the LSP socket/channel. |
| P5-FIX-104 | B11 | `LspFormattingRequesterTest.kt` | all test methods | BLUFF | PAT-04 | CRITICAL | Same as P5-FIX-103 for `LspFormattingRequester`. |
| P5-FIX-105 | B11 | `LspReferencesRequesterTest.kt` | all test methods | BLUFF | PAT-04 | CRITICAL | Same as P5-FIX-103 for `LspReferencesRequester`. |
| P5-FIX-106 | B11 | `LspRenameRequesterTest.kt` | all test methods | BLUFF | PAT-04 | CRITICAL | Same as P5-FIX-103 for `LspRenameRequester`. |
| P5-FIX-107 | B11 | `LspSignatureHelpRequesterTest.kt` | all test methods | BLUFF | PAT-04 | CRITICAL | Same as P5-FIX-103 for `LspSignatureHelpRequester`. |
| P5-FIX-108 | B11 | `DesktopHttpClientFactoryTests.kt` | 10 of 11 test methods | BLUFF | PAT-12 | HIGH | 10 tests assert `assertNotNull(nonNullableValue)` on Kotlin non-nullable types. Replace with actual HTTP call assertions: stub a local server and assert the request reaches it. |
| P5-FIX-109 | B11 | `LspDiagnosticsParserTest.kt` | `testDiagnosticSeverityMapping` | SUSPECT | PAT-03 | HIGH | Assert that severity level `1` maps to `DiagnosticSeverity.ERROR` and `4` maps to `HINT`; `assertNotNull` is insufficient. |
| P5-FIX-110 | B11 | `LspProtocolVersionTest.kt` | `testProtocolVersionNegotiation` | SUSPECT | PAT-06 | MEDIUM | Assert negotiated version string equals expected value from LSP spec. |
| P5-FIX-111 | B11 | `SyntaxHighlighterTest.kt` | `testTokenTypeMapping` | SUSPECT | PAT-03 | MEDIUM | Assert that token type `COMMENT` maps to a distinct color from `KEYWORD`. |
| P5-FIX-112 | B11 | `AutocompleteProviderTest.kt` | `testCompletionItemCount` | SUSPECT | PAT-03 | MEDIUM | Assert completions list is non-empty AND each item has non-blank `label` and `kind`. |
| P5-FIX-113 | B11 | `OutlinePanelProviderTest.kt` | `testSymbolHierarchy` | SUSPECT | PAT-03 | MEDIUM | Assert child symbol count and that nested names are non-blank. |
| P5-FIX-114 | B11 | `DesktopEditorStateTest.kt` | `testCursorPositionAfterInsert` | SUSPECT | PAT-03 | MEDIUM | Assert exact cursor line/col value after inserting known text at a known position. |

### B12 — Android-1

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-115 | B12 | `FirebaseIntegrationTests.kt` | `testFirebaseInitialization`, `testAnalyticsEventLogging`, `testCrashlyticsIntegration`, `testPerformanceMonitoring` | BLUFF | PAT-08 | CRITICAL | All 4 methods end with `assertTrue("...", true)` — hardcoded pass. Remove hardcoded `true`; assert against real Firebase SDK state. |
| P5-FIX-116 | B12 | `VersionConsistencyTests.kt` | `testVersionName`, `testVersionCode` | BLUFF | PAT-08 | CRITICAL | `EXPECTED_VERSION = "2.0.0"` (actual: `2.0.6`), `EXPECTED_VERSION_CODE = 200` (actual: `206`). This test is actively FAILING at runtime. Update constants to match `BuildConfig`. |
| P5-FIX-117 | B12 | `IntegrationTest.kt` | `testValidationIntegration` | BLUFF | PAT-03 | HIGH | Errors collected into `errors` list but never asserted. Add `assertTrue(errors.isEmpty())` for the valid-input case. |
| P5-FIX-118 | B12 | `ActivityResultContractTest.kt` | `testOpenDocumentResult` | BLUFF | PAT-03 | HIGH | Assert that the returned URI from the contract equals the test URI injected via `ActivityScenario`. |
| P5-FIX-119 | B12 | `AndroidPermissionTest.kt` | `testReadExternalStorageGranted` | SUSPECT | PAT-03 | HIGH | Assert that the permission grant triggers the expected callback with `PERMISSION_GRANTED`; `assertNotNull(result)` is insufficient. |
| P5-FIX-120 | B12 | `BackgroundSyncTest.kt` | `testWorkManagerEnqueue` | SUSPECT | PAT-03 | MEDIUM | Assert that WorkManager has exactly one enqueued request with the expected tag. |
| P5-FIX-121 | B12 | `ContentProviderTest.kt` | `testQueryDocuments` | SUSPECT | PAT-03 | MEDIUM | Assert cursor row count and column values. |
| P5-FIX-122 | B12 | `NotificationTest.kt` | `testNotificationPosted` | SUSPECT | PAT-03 | MEDIUM | Use `NotificationManagerCompat` to assert the notification exists with the expected channel ID. |
| P5-FIX-123 | B12 | `ShareIntentTest.kt` | `testShareTextIntent` | SUSPECT | PAT-03 | MEDIUM | Assert intent action is `ACTION_SEND` and `EXTRA_TEXT` equals the expected string. |

### B13 — Android-2

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-124 | B13 | `FormatDetectionRobolectricTest.kt` | `testDetectionAfterClick` | SUSPECT | PAT-05 | HIGH | `performClick() + waitForIdle()` with no assertion on resulting state. Assert that the detected format badge/label text changed to the expected value. |
| P5-FIX-125 | B13 | `NavigationRobolectricTest.kt` | `testNavigateToEditor`, `testBackNavigation` | SUSPECT | PAT-05 | HIGH | Assert that the target composable node is visible after navigation; do not only assert no exception after `performClick()`. |
| P5-FIX-126 | B13 | `QuickNoteRobolectricTest.kt` | `testQuickNoteCreate` | SUSPECT | PAT-05 | HIGH | Assert that the newly created note's title appears in the list composable; click alone does not verify persistence. |
| P5-FIX-127 | B13 | `SettingsRobolectricTest.kt` | `testToggleDarkMode` | SUSPECT | PAT-05 | HIGH | Assert that the theme state transitions to dark after toggle; `performClick()` alone does not verify the theme switch. |

### B14 — Misc Platforms

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-128 | B14 | `TestStubs.kt` (stub file in production package) | N/A — structural defect | BLUFF | PAT-04 | CRITICAL | `TestStubs.kt` is in package `digital.vasic.yole.desktop.ui` (same as production composables). Any test that wildcard-imports this package receives stubs silently shadowing production types. Move `TestStubs.kt` to `test` source root or rename to avoid package collision. |
| P5-FIX-129 | B14 | `DesktopAppParserTest.kt` | `testMarkdownParsing`, `testHtmlParsing` | BLUFF | PAT-11 | CRITICAL | `assertTrue(content.contains("# Header 1"))` where `content` is the string the test built before calling the parser. Parser output is never inspected. Assert against `parser.parse(content).toHtml()`. |
| P5-FIX-130 | B14 | `WebAppUITest.kt` | `testAppRendersWithoutError`, `testInitialState` | BLUFF | PAT-09 | CRITICAL | `var appRendered = false; try { appRendered = true } catch (e: Exception) { fail(...) }; assertTrue(appRendered)` is structurally `assertTrue(true)`. Replace with WASM compose test node assertions. |
| P5-FIX-131 | B14 | `IosAppBridgeTest.kt` | `testSwiftBridgeConnects` | BLUFF | PAT-03 | HIGH | `assertNotNull(bridge)` — bridge is constructed unconditionally and never given a real platform call. Add a round-trip call through the bridge and assert the return value. |
| P5-FIX-132 | B14 | `CrossPlatformParityTest.kt` | `testParityAndroid`, `testParityDesktop`, `testParityIos`, `testParityWeb` | BLUFF | PAT-06 | HIGH | All 4 methods assert only constant feature-flag names. Add behavioral assertions: parse the same document on each platform path and compare `toHtml()` output. |
| P5-FIX-133 | B14 | `DesktopWindowStateTest.kt` | `testWindowTitleBarNotNull` | SUSPECT | PAT-12 | MEDIUM | `assertNotNull` on a Kotlin non-nullable `WindowState`. Assert specific property values (width, height, initial title string). |
| P5-FIX-134 | B14 | `WasmRenderTest.kt` | `testCanvasNotNull` | SUSPECT | PAT-12 | MEDIUM | Same as P5-FIX-133; assert canvas dimensions > 0. |
| P5-FIX-135 | B14 | `PlatformFilePickerTest.kt` | `testFilePickerOpens` | SUSPECT | PAT-09 | MEDIUM | `assertTrue(opened || !opened)` is always true. Assert `assertTrue(opened)`. |
| P5-FIX-136 | B14 | `DesktopMenuBarTest.kt` | `testMenuItemCount` | SUSPECT | PAT-03 | MEDIUM | Assert exact menu item count equals expected value; `assertTrue(count > 0)` passes with a single placeholder item. |
| P5-FIX-137 | B14 | `IosKotlinInteropTest.kt` | `testNumberConversion` | SUSPECT | PAT-03 | MEDIUM | Assert converted value equals original value numerically. |
| P5-FIX-138 | B14 | `WebPwaManifestTest.kt` | `testManifestFields` | SUSPECT | PAT-03 | MEDIUM | Assert `name`, `start_url`, and `display` fields contain expected string values from `manifest.json`. |
| P5-FIX-139 | B14 | `WasmComposableTest.kt` | `testComposableRendersText` | SUSPECT | PAT-03 | MEDIUM | Assert that the rendered text node contains the expected string. |
| P5-FIX-140 | B14 | `PlatformDispatcherTest.kt` | `testMainDispatcherNotNull` | SUSPECT | PAT-12 | MEDIUM | Non-nullable `MainCoroutineDispatcher`; assert it dispatches coroutines correctly with a timing probe. |
| P5-FIX-141 | B14 | `DesktopSystemTrayTest.kt` | `testTrayIconVisible` | SUSPECT | PAT-03 | MEDIUM | Assert tray icon dimensions are non-zero and icon is visible in the system tray (platform API check). |
| P5-FIX-142 | B14 | `WasmServiceWorkerTest.kt` | `testServiceWorkerRegistration` | SUSPECT | PAT-03 | MEDIUM | Assert registration scope URL matches expected origin. |
| P5-FIX-143 | B14 | `IosViewControllerBridgeTest.kt` | `testViewControllerLifecycle` | SUSPECT | PAT-03 | MEDIUM | Assert that `viewDidAppear` callback fires after `show()`; do not only assert no exception. |

### B15 — Challenge Scripts

| P5-FIX | Batch | File | Method(s) | Verdict | Pattern | Severity | Recommended Fix |
|--------|-------|------|-----------|---------|---------|----------|-----------------|
| P5-FIX-144 | B15 | `mutation_ratchet_challenge.sh` | entire script | BLUFF | PAT-07 | CRITICAL | Script checks for 3 section markers in baseline file and exits PASS without ever invoking Pitest or checking mutation score. Add actual Pitest invocation; parse the mutation score from the XML report; fail if score regressed below baseline. |
| P5-FIX-145 | B15 | `web_full_ui_suite_challenge.sh` | entire script | BLUFF | PAT-07 | HIGH | Exits 3 (SKIP-OK) when Puppeteer is absent. This provides zero runtime evidence on every developer machine. Convert to WARN + non-zero exit when Puppeteer is absent, so the CI/CD gate would catch a missing dependency. |
| P5-FIX-146 | B15 | `web_interactive_flow_challenge.sh` | entire script | BLUFF | PAT-07 | HIGH | Same as P5-FIX-145. |
| P5-FIX-147 | B15 | `web_responsive_suite_challenge.sh` | entire script | BLUFF | PAT-07 | CRITICAL | Always SKIPs on dev machines. Was the gate that should have caught the iter-90 mobile layout regression but did not. Add a fallback path using `wasm-pack test` or fail rather than skip. |
| P5-FIX-148 | B15 | `webapp_render_validation_challenge.sh` | entire script | BLUFF | PAT-07 | HIGH | Same as P5-FIX-145. |
| P5-FIX-149 | B15 | `anchor_manifest_challenge.sh` | `verify_evidence_section()` function | SUSPECT | PAT-05 | HIGH | Verifies that the word "evidence" appears in a challenge script comment; a stub challenge with a comment block passes. Add a check that each challenge script also emits a `RUNTIME_EVIDENCE:` log line during execution. |
| P5-FIX-150 | B15 | `bluff_scanner.sh` | tautology detector regex | SUSPECT | PAT-05 | MEDIUM | Regex does not detect all tautology forms (e.g., misses `assertTrue(x.isSuccess || x.isFailure)` when split across lines). Improve to multi-line scan. |
| P5-FIX-151 | B15 | `cross_platform_parity_challenge.sh` | platform file existence check | SUSPECT | PAT-05 | MEDIUM | Checks that platform source files exist; does not compile or invoke them. Add a `./gradlew :shared:desktopTest` invocation guard or a code-level parse assertion. |
| P5-FIX-152 | B15 | `installable_app_icon_challenge.sh` | `verify_icon_density` function | SUSPECT | PAT-05 | MEDIUM | Checks file size > 0 bytes; does not decode the PNG. A corrupt but non-empty PNG passes. Add `pngcheck` or `identify -format '%wx%h'` assertion. |
| P5-FIX-153 | B15 | `no_suspend_calls_challenge.sh` | grep pattern | SUSPECT | PAT-05 | LOW | Regex could miss obfuscated or split invocations. Low priority but worth hardening to multi-line scan. |

---

## Section 4 — CRITICAL Items (Possibly Hiding Genuinely Broken Features)

The following P5-FIX rows are CRITICAL because the existing bluff test would pass even if the user-visible feature were completely non-functional:

1. **P5-FIX-001 — FormatParserSecurityTests (B01):** XSS protection for all 17 parsers is unverified. A parser that emits raw `<script>` tags passes every security test. If a parser were modified to stop escaping HTML, no test would catch it. End-user impact: stored XSS when viewing attacker-controlled documents.

2. **P5-FIX-089 — SecurityValidationTests (B09):** `assertNoRawScript()` never inspects the `html` argument passed to it (it runs `escapeHtml()` on a hardcoded string instead). All 13 XSS tests permanently PASS regardless of parser output. Same end-user impact as P5-FIX-001 but affects the common-layer security gate specifically.

3. **P5-FIX-068 — SafetyFixesTest path-traversal section (B06):** ~20 methods claim to verify that path traversal attacks are blocked by the 8 protocol services. All use `assertTrue(result.isSuccess || result.isFailure)`. A service that permits `../../etc/passwd` reads returns `success` and every test still passes. End-user impact: potential file exfiltration via crafted paths.

4. **P5-FIX-046 — ProtocolSupremacyTests (B05):** 40 tests covering edge cases (unicode paths, very long paths, concurrent access, reconnection) all assert only `assertTrue(true)`. Every edge case could be broken with no test failure. End-user impact: network file operations may silently fail or corrupt data on edge-case inputs.

5. **P5-FIX-144 — mutation_ratchet_challenge.sh (B15):** The mutation ratchet is the last line of defense against gradual test degradation. The challenge never runs Pitest. The "mutation score" is fictional. Entire mutation-coverage governance gate is non-functional.

6. **P5-FIX-147 — web_responsive_suite_challenge.sh (B15):** This gate always SKIPs on developer machines. It did not catch the iter-90 mobile layout regression. End-user impact: mobile layout bugs ship undetected.

7. **P5-FIX-041 — AuthTokenManagerStressTest (B04):** All 17 stress tests exercise `TestAuthTokenManagerImpl`, not `AuthTokenManager`. Race conditions in the real token manager are invisible. End-user impact: token expiry races → authentication failures under load.

8. **P5-FIX-103–107 — All 5 LSP Requester Tests (B11):** No LSP production code has ever been exercised by the test suite. LSP code action, formatting, references, rename, and signature help could all be completely broken with PASS status. End-user impact: every LSP feature (a core desktop feature) is unverified.

9. **P5-FIX-115–116 — FirebaseIntegrationTests + VersionConsistencyTests (B12):** Firebase tests hardcode `assertTrue(true)`. Version tests use stale constants and are actively FAILING at runtime. End-user impact: Firebase analytics/crashlytics initialization is unverified; release version metadata is unchecked.

10. **P5-FIX-030–031 — UltimateSupremacyTest (B03):** Hardcoded quality metrics (`Test Coverage = 1.0`) and a global exception swallower make this test permanently PASS. It is used as a quality-gate signal. The signal is meaningless.

---

## Section 5 — Fix-Effort Estimates

### Mechanical Sweeps (bulk fix, consistent technique)

These patterns have a uniform fix technique; a single engineer can resolve the entire pattern in a focused pass:

| Pattern | Entries | Effort estimate | Technique |
|---------|---------|-----------------|-----------|
| PAT-01 `isSuccess \|\| isFailure` tautology | ~40 entries (B04–B07, B09) | 2–3 days | Search-replace `assertTrue(*.isSuccess \|\| *.isFailure)` → `assertTrue(*.isSuccess)` + add `getOrThrow()` value assertion. Mechanical but requires per-method expected-value knowledge. |
| PAT-09 tautological boolean `x \|\| !x` | ~8 entries | 0.5 days | Trivial: delete the condition, write the intended assertion. |
| PAT-12 `assertNotNull` on non-nullable | ~5 entries | 0.5 days | Delete assertion or replace with value check. Kotlin type system guides the fix. |
| PAT-02 timing-only assertions | ~12 entries | 1 day | Add correctness assertion before timing; existing timing line can stay as secondary guard. |
| PAT-03 `assertNotNull`-only | ~20 entries | 2–3 days | For each method, determine expected content and add `assertEquals` / `assertTrue(contains(...))`. |
| PAT-07 challenge exit-3 SKIP | ~6 entries | 1 day | Change `exit 3` to `echo "WARN: runtime dep absent"; exit 1` and update callers that treat exit-3 as PASS. |
| PAT-10 rawContent round-trip tautology | ~4 entries | 0.5 days | Replace `assertEquals(raw1, raw2)` with `assertEquals(expectedHtml, toHtml())`. |
| PAT-11 self-referential input assertion | ~3 entries | 0.5 days | Assert against the return value of the unit under test, not the input variable. |

### Independent Investigations (complex, per-entry analysis)

These patterns require understanding production code semantics for each entry and cannot be fixed with a uniform template:

| Pattern | Entries | Effort estimate | Why complex |
|---------|---------|-----------------|-------------|
| PAT-04 self-verifying test doubles | ~14 entries | 4–6 days | For each test, identify the real production class, determine which dependencies need stubbing (HTTP, file I/O, coroutine scope), and wire up a minimal real instantiation. LSP requesters (P5-FIX-103–107) require a mock LSP server. Auth stress tests (P5-FIX-041) require understanding token storage interface. |
| PAT-08 hardcoded/stale one-off bugs | ~6 entries | 1–2 days | Each is a unique defect: P5-FIX-030 requires hooking into Kover XML; P5-FIX-115 requires Firebase SDK test doubles; P5-FIX-116 requires updating constants (trivial but must be kept in sync); P5-FIX-031 requires exception propagation audit. |
| PAT-05 structural source-grep tests | ~8 entries | 2–3 days | Each grep test needs a runtime behavioral complement. Lock ordering (P5-FIX-075) requires a deadlock detection harness. Challenge script greps (P5-FIX-149–153) require emitting structured RUNTIME_EVIDENCE log lines. |
| PAT-06 metadata/constant-only | ~10 entries | 2–3 days | Requires understanding the minimum valid document for each format and adding one parse + HTML round-trip assertion per entry. |
| Security bluff (P5-FIX-001, P5-FIX-089) | 2 entries | 1 day | Highest-priority security fix: straightforward once the pattern is clear. Add `assertFalse(html.contains("<script"))` in the right place. |
| Challenge mutation ratchet (P5-FIX-144) | 1 entry | 2–3 days | Requires wiring Pitest into the Makefile/shell, establishing a baseline score file, and writing a score-comparison script. |

### Priority Order

Recommended sequencing for maximum risk reduction per unit of effort:

1. **Day 1** — Fix P5-FIX-001, P5-FIX-089 (XSS bluff, security-critical, quick fix)
2. **Day 1–2** — Fix P5-FIX-068, P5-FIX-046 (path traversal + protocol supremacy, security + core network)
3. **Day 2–3** — Fix P5-FIX-115–116 (Firebase hardcoded PASS + stale version constants, two trivial fixes)
4. **Day 3–5** — PAT-01 mechanical sweep (~40 entries, reduces noise dramatically)
5. **Day 5–7** — Fix P5-FIX-103–107 (5 LSP requester tests, requires mock LSP server)
6. **Day 7–8** — Fix P5-FIX-041 (auth stress test self-verifying double)
7. **Day 8–9** — Fix P5-FIX-144 (mutation ratchet challenge, governance gate)
8. **Day 9–10** — Fix P5-FIX-147 (web responsive challenge, iter-90 regression gate)
9. **Week 3** — PAT-03, PAT-04, PAT-06 sweeps
10. **Week 4** — PAT-02, PAT-05, PAT-08, PAT-10, PAT-11, PAT-12 sweeps

**Total estimated effort:** 4–5 engineer-weeks for all 158 entries.  
**Minimum viable security pass** (items 1–3 above): ~2 engineer-days.
