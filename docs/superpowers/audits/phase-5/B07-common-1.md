# Phase 5 Audit — B07 shared/commonTest completion/concurrency/api/challenges
Audited: 2026-05-20  |  Files: 28  |  Bluff: 4  |  Suspect: 4  |  Clean: 20

## Findings

### shared/src/commonTest/kotlin/digital/vasic/yole/api/ApiConsistencyTest.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 14
- **Unit(s) under test:** multiple services (FtpService, SmbService, WebDavService, GitService), MarkdownParser, FormatRegistry, NetworkStorage
- **Evidence:**
  - `all services return Result from operations` (line 152–193): every assertion is `assertTrue(result.isSuccess || result.isFailure, ...)`. This is a pure tautology — `isSuccess || isFailure` is always true for any non-null `Result<T>`. Stub all service methods to return `Result.failure(RuntimeException("stub"))` and every assertion still passes. Lines 165–193.
  - `all services implement clearCache consistently` (line 236–252): asserts `assertTrue(result.isSuccess)`. Since these services are constructed with fake host configs and no real server, the fact that `clearCache()` returns success is a property of the stub/in-memory implementation of clearCache, not a behavioural guarantee. A trivially stubbed `clearCache()` returning `Result.success(Unit)` makes this test pass. Line 249.
  - `all services implement getQuotaInfo consistently` (line 215–233): asserts `result.isSuccess` and that `quota.totalSpace >= 0` and `quota.usedSpace >= 0`. Any stub that returns `StorageQuota(0,0,0,0.0,false,false)` passes all assertions because `0 >= 0` is always true. Line 230–231.
  - `ParsedDocument implements equals and hashCode correctly` (line 358–367): calls `assertEquals(doc1, doc2)` and `assertNotEquals(doc1, doc3)` — these are valid behavioral assertions. This method is CLEAN.
  - `NetworkStorage provides computed properties consistently` (line 313–333): tests `availableSpace`, `usagePercentage`, `isFull`, `isLowOnSpace` against literal math inputs — strong behavioral assertions.
- **Recommended fix:**
  - Replace `assertTrue(fileInfoResult.isSuccess || fileInfoResult.isFailure, ...)` tautologies with checks that verify a specific failure mode (e.g., `assertTrue(fileInfoResult.isFailure); assertTrue(fileInfoResult.exceptionOrNull() is NetworkStorageException || ...)`) or that on a stub-injectable service the Result carries the expected value.
  - For `clearCache` and `getQuotaInfo`, inject a testable fake (e.g. the SmbService test constructor) and assert non-trivial constraints: quota fields must equal stub-pre-configured values, clearCache must change observable cache state.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/challenges/ChallengeValidationTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 35
- **Unit(s) under test:** embedded JSON strings (hardcoded in the test file itself); `TextFormat.ID_*` constants; `validProtocolNames` set; `Json.parseToJsonElement` from kotlinx.serialization
- **Evidence:**
  - The entire test suite operates exclusively on five hard-coded JSON strings (`sampleFormatParsingBank`, `sampleSecurityBank`, `sampleNetworkBank`, `sampleResilienceBank`, `sampleConcurrencyBank`) defined as `private val` within the test class itself (lines 126–320). These strings are perfectly well-formed and already comply with every structural rule the tests check.
  - Tests such as `testFormatParsingBankParsesAsValidJson` (line 351), `testAllChallengesHaveRequiredIdField` (line 390), `testChallengeIdsUniqueAcrossAllBanks` (line 491), etc. verify properties of **sample data the test itself authored**. If all the real bank files in `Challenges/banks/yole/` were structurally broken, all tests would still pass because they never read those files.
  - The description in the class doc (lines 26-43) explicitly acknowledges: "Since we cannot execute Go code from Kotlin tests, this test verifies the JSON structure and referential integrity of the challenge bank definitions against the Kotlin codebase." However, it does not read any real file from disk — it validates properties the test itself guarantees by construction.
  - The strongest behavioural assertion in the file is `testMalformedJsonIsRejected` (line 905): it confirms `Json.parseToJsonElement` throws on malformed input — this exercises the kotlinx.serialization library, not the project's own code. Legitimate exercise of `TextFormat.ID_*` constants happens at line 674–699 (`testFormatIdsInTextFormatCompanionAreComplete`), which has real behavioral value.
  - Mutation: stub `TextFormat.ID_MARKDOWN` to `""` → `testFormatIdsInTextFormatCompanionAreComplete` at line 683 would FAIL (contains check on empty string is true but size check at 679 still passes). So the format-ID membership tests are genuine. The JSON-structural tests are SUSPECT because they don't test production files.
- **Recommended fix:** Add a test that reads at least one real bank file from `Challenges/banks/yole/` (via a resource or relative path assertion) and validates its structure. The structural tests over the embedded samples are fine as a smoke-check but should be labelled as schema contract tests, not validation of the real bank files.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/completion/CompletionContextTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/completion/CompletionContext.kt`
- **Evidence:** `prefix_extractedFromCursorByte` (line 13): calls `CompletionContext.of("fun foo() {...}", cursorChar=7)` and asserts `ctx.prefix == "foo"` and `ctx.prefixRange == 4..7` (line 19–20). If `CompletionContext.of` were stubbed to return a blank prefix, both assertions fail. The cursor boundary walking logic in the production code (`isWordChar` walk-back, lines 47–58 of CompletionContext.kt) is genuinely exercised.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/completion/CompletionItemTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** `CompletionItem` data class (inferred from package; constructor + `Kind.values()`)
- **Evidence:** `construct_carriesAllFields` (line 14): asserts five specific field values against the constructed item — label, insertText, kind, score (delta 1e-9), range. Stubbing the constructor to zero-fill would break all five assertions. `kind_hasFourVariants` (line 29): verifies `Kind.values().size == 4` and enumerates all four variants — fails if any variant is removed.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/completion/CompletionRankerTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/completion/CompletionRanker.kt` + `ScopeAwareRanker.kt`
- **Evidence:** `dedupe_keepHighestScore` (line 51): provides two providers both with label "foo" at scores 0.5 and 0.9; asserts exactly 1 result and score == 0.9. Stubbing dedup to keep min-score breaks this (line 64). `boostIsApplied_memberAccessIdentifier` (line 93): asserts final score == 3.0 (1.0 + 2.0 boost); documented mutation test confirms this fails when boost step is skipped (header comment lines 18–20). Both production files are real implementations (verified), not stubs.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/completion/providers/TokenFrequencyProviderTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/completion/providers/TokenFrequencyProvider.kt`
- **Evidence:** `fooAppears3Times_scoreIs3` (line 36): constructs real text "foo bar foo baz foo ", calls `provider.complete(ctx)`, and asserts `fooItem.score >= 3.0` (line 53). The production code counts word occurrences via regex (lines 51–63 of provider); stubbing `complete` to `emptyList()` breaks the assertion. `cursorWordFo_fooReturnedNotCursorPartial` (line 76): verifies the exclusion logic — "fo" excluded while "foo" returned. Documented mutation (header lines 3–10) confirmed both tests fail when provider always returns emptyList.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/completion/ScopeAwareRankerTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 8
- **Unit(s) under test:** `shared/src/commonMain/kotlin/digital/vasic/yole/completion/ScopeAwareRanker.kt`
- **Evidence:** `memberAccess_identifier_boostsBy2` (line 45): calls `ScopeAwareRanker.boost(item(Kind.Identifier), "member_access")` and asserts exactly 2.0 (delta 1e-9). Production code returns `BOOST_MEMBER_IDENTIFIER = 2.0` for this branch. Documented mutation (header lines 17–22) confirmed failure when this branch was stubbed to 0.0. `stringLiteral_word_suppresses` (line 119): asserts -3.0 — a distinct behavioral case that would survive if suppression were only applied to Identifier kind. All eight scope×kind combinations are table-driven and distinct.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/completion/SnippetPlaceholderNavigatorTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 9
- **Unit(s) under test:** `VsCodeSnippetExpander` and `SnippetPlaceholderNavigator` (inferred from imports, package `digital.vasic.yole.completion.snippet`)
- **Evidence:** `expand_singlePlaceholder_extractsRange` (line 61): parses `"x = ${1:val};"` and asserts `strippedBody == "x = val;"`, `placeholders.size == 1`, `ph.rangeInBody == 4..6`. Stubbing `expand` to return `ExpandedSnippet(body, emptyList(), false)` breaks lines 65–69. `navigator_advanceReturnsAbsoluteRanges` (line 181): verifies absolute offset shifting (baseOffset=100 → first range 100..100). Documented mutation (header lines 12–16) confirmed failure. Tests 7 and 8 together verify the advance/exhaust lifecycle.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/completion/VsCodeSnippetParserTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `VsCodeSnippetParser` (package `digital.vasic.yole.completion.snippet`)
- **Evidence:** `parses_singleLineBody` (line 50): calls `VsCodeSnippetParser.parse(singleLineJson)` and asserts `snippets.size == 1`, `s.prefix == "hello"`, `s.body.contains("Hello, World!")`. Documented mutation to `return null` breaks this. `malformed_throws` (line 70): asserts `SnippetParseException` is thrown on `"{ not json }"` — genuine error-path verification.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/concurrency/CancellationSafetyTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 12
- **Unit(s) under test:** `DropboxService.getRecentChanges`, `GoogleDriveService.getRecentChanges`, `OneDriveService.getRecentChanges`
- **Evidence:**
  - `DropboxService getRecentChanges returns a Flow` (line 71): asserts `flow is Flow<*>` — this is a type-check tautology. Any implementation returning a non-null Flow passes, including a stub that returns `flowOf()`. Line 78.
  - Stress tests `Stress test - 50 coroutines collecting from GoogleDrive...` (line 242) and `OneDrive` variant (line 282): assert `assertTrue(finalCompleted >= 0, ...)` at lines 278 and 315 respectively. This asserts a non-negative count which is always true for an `Int`. These are "absence of crash" tests with a tautological numeric assertion.
  - The disconnected-service tests (lines 104–151) are genuine: they assert `firstEmission is List<*>` AND `(firstEmission as List<*>).isEmpty()` — stub implementations must return an emptyList emission to pass these.
  - The `CancellationException` rethrow safety cannot be verified from this test alone since the services are offline — the tests only confirm the service emits something, not that `CancellationException` is rethrown rather than swallowed in the real network path.
- **Recommended fix:** Remove or correct the `assertTrue(finalCompleted >= 0)` tautologies (lines 278, 315) — replace with `assertTrue(finalCompleted + cancelled > 0)` style (as already done in the Dropbox variant at line 234). The `is Flow<*>` type-check is borderline; consider replacing with a collect-and-assert-empty pattern.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/concurrency/ConcurrencyFixesTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 33
- **Unit(s) under test:** SmbService, SftpService, AuthTokenManager (real implementations with testConnectFn injection)
- **Evidence:**
  - Most tests use `testConnectFn` injection so SmbService uses in-memory state. This is good design for concurrency tests. Tests like `SMB concurrent pause and resume operations are thread-safe` (line 121) and `Concurrent mutex operations on different resources do not deadlock` (line 794) are genuine: they assert `assertTrue(result.isSuccess)` on 20–300 operations against real in-memory concurrency paths.
  - `SFTP concurrent connect disconnect do not crash` (line 198) uses standard `SftpService` without injection; each result asserts `assertTrue(result.isSuccess || result.isFailure)` (line 215) — tautology. The non-tautological value comes from the `withTimeout` + deadlock prevention, but the assertion itself is weak. Line 215.
  - `SMB concurrent connect disconnect maintains connection state consistency` (line 220): ends with `assertTrue(testResult.isSuccess || testResult.isFailure)` — same tautology pattern at line 239.
  - `SFTP concurrent connect calls are safe` (line 664): asserts `assertTrue(it.isSuccess || it.isFailure)` — tautology at line 673.
  - CLEAN methods include: `AuthTokenManager concurrent getAccessToken calls are safe` (line 318) — asserts all 100 results succeed; `SMB concurrent createFolder does not corrupt file tree` (line 494) — asserts all 50 folders exist via `exists` verification (lines 506–512, strongest behavioral assertion in the file).
- **Recommended fix:** For SFTP tests using real network (where connect will fail), use `assertFalse(service.isOnline)` after rapid connect/disconnect cycles rather than tautological `isSuccess || isFailure`. The important property is the resulting state, not that a Result was returned.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/concurrency/ConcurrencySafetyTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 14
- **Unit(s) under test:** `FormatRegistry`, `NetworkOperation`, `StorageConfig`, `NetworkDocument`, `Mutex`
- **Evidence:** `Mutex protects shared state correctly` (line 189): 1000 coroutines increment a counter under mutex; asserts `counter == 1000` (line 204) — precise behavioral assertion that fails if the mutex doesn't serialize access. `FormatRegistry concurrent access is thread-safe` (line 34): 100 coroutines read 3 format IDs concurrently; asserts each `getById` result is non-null (line 52–55). `Random delays do not cause consistency issues` (line 384): asserts `sum == 5050` — mutation-killing arithmetic assertion (line 398).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/concurrency/FormatRegistrySemaphoreTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 17
- **Unit(s) under test:** `FormatRegistry.configureParseConcurrency`, `FormatRegistry.DEFAULT_PARSE_CONCURRENCY`
- **Evidence:** `configureParseConcurrency with 0 throws IllegalArgumentException` (line 96): calls the production method and asserts the exception message contains "0" (line 103). Stubbing `configureParseConcurrency` to never throw breaks this. `Boundary - 1 is valid, 0 is invalid` (line 162): additionally asserts `FormatRegistry.formats.isNotEmpty()` after reconfiguration (line 166), verifying registry remains operational — this kills stubs that leave formats empty.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/concurrency/HttpClientLifecycleTests.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 16
- **Unit(s) under test:** `WebDavService`, `GitService` — `disconnect()` and `isOnline` lifecycle
- **Evidence:**
  - `WebDavService can be created and disconnected without crash` (line 61): asserts `result.isSuccess`. The production code's `disconnect()` without prior connection returns success by design (guarded by `@Volatile httpClientInitialized`). Stubbing `disconnect()` to always return `Result.success(Unit)` passes this test. The test verifies absence-of-crash, not a behavioral property. Line 65.
  - The entire file is structured around "does not crash" and `assertTrue(result.isSuccess || result.isFailure)` patterns. Lines 131–178 (`concurrent connect/disconnect cycles`) assert tautological results at lines 132–133 and 154–155. Line 175: `assertEquals(20, results.size)` is the only non-tautological assertion but only checks count, not service state.
  - `WebDavService isOnline is false after creation` (line 258): asserts initial state — valid, but trivially passes if `isOnline` defaults to false. The only mutation-killing assertion is `isOnline == false after disconnect` (line 277–278) which would fail if disconnect set `isOnline = true`.
  - `Creating 50 WebDavService instances concurrently does not crash` (line 205): asserts `services.size == 50` — count check, not behavioral. The real test is whether it doesn't crash (absence of exception).
- **Recommended fix:** Add assertions that verify a behavioral state change: e.g. use the `testConnectFn` constructor injection (as done in ConcurrencyFixesTest) to create a service that successfully connects, then verify `isOnline == true`, then disconnect and verify `isOnline == false`. Currently the tests cannot distinguish a service whose `disconnect()` is a no-op from a correct implementation.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/concurrency/LockOrderingComplianceTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 16
- **Unit(s) under test:** all 8 protocol services — `cancelOperation`, `pauseOperation`, `resumeOperation`
- **Evidence:** `concurrentCancelOperations_Dropbox` (line 113): wraps 50 concurrent `cancelOperation` calls in `withTimeout(5000L)` — deadlock would cause `TimeoutCancellationException` and fail the test. `assertEquals(50, results.size)` at line 120 verifies all 50 completed. The comment (lines 106–112) documents the exact deadlock scenario being guarded against. `interleavedCancelPauseResume_AllServices` (line 332): exercises all 8 services simultaneously — cross-service deadlock detection. CLEAN because the timeout is the meaningful assertion: a hanging test is a real failure.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/concurrency/QuotaThresholdTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 13
- **Unit(s) under test:** `StorageQuota` data class threshold logic (the `isLowOnSpace = usagePercentage >= 0.9` expression, via the `createQuotaWithUsage` helper)
- **Evidence:** `StorageQuota with exactly 0_9 usage marks isLowOnSpace true` (line 61): provides `usagePercentage = 0.9` and asserts `quota.isLowOnSpace == true`. If the threshold were changed to `> 0.9` (the pre-fix bug), this test fails. `Boundary - exactly 0_9 IS low on space (ge not gt)` (line 96): directly evaluates the expression `usagePercentage >= 0.9` for `0.9` — documents the regression. `All three cloud services produce consistent results for same percentage at boundary` (line 133): verifies agreement at the exact boundary value.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/concurrency/StyleSheetsSynchronizationTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 14
- **Unit(s) under test:** `StyleSheets.getStyleSheet`, `StyleSheets.clearCache`, `StyleSheets.cacheSize`
- **Evidence:** `getStyleSheet returns non-empty CSS for markdown light` (line 48): asserts `css.isNotEmpty()`. Stubbing `getStyleSheet` to return `""` breaks this. `clearCache followed by getStyleSheet regenerates correctly` (line 168): asserts `assertEquals(0, StyleSheets.cacheSize)` after clear then re-asserts `cssBefore == cssAfter` — verifies both the state transition and output determinism. `cacheSize increments as entries are added` (line 260): precise size assertions (1, 2, 3) kill stubs that don't actually cache.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/concurrency/VolatileFieldSafetyTests.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 13
- **Unit(s) under test:** all 8 protocol services — `isOnline`, `disconnect()`, `FormatRegistry.configureParseConcurrency`
- **Evidence:**
  - `isOnlineReflectsState_AllServices` (line 107): asserts `assertFalse(service.isOnline)` for freshly created services. If `isOnline` defaults to false (which it does in every well-written service), this passes trivially whether or not `@Volatile` is applied. The `@Volatile` annotation cannot be verified from Kotlin/JVM coroutines alone — visibility guarantees only manifest in multi-threaded contexts. Line 107–139.
  - `concurrentDisconnectReadsShouldNotThrow_Dropbox` (line 199): launches concurrent readers of `isOnline` and disconnectors. Ends with `assertTrue(true, ...)` at line 211 — pure tautology. The value is "no exception was thrown" (absence of crash). A service whose `disconnect()` is a no-op passes all assertions. Line 211.
  - All 8 `concurrentDisconnectReadsShouldNotThrow_*` methods (lines 199–317) end with `assertTrue(true, ...)` — every single one is a tautology after the concurrent operations. The real safety guarantee is "no uncaught exception within withTimeout", not the explicit assertion.
  - `parseSemaphoreVisibleAfterReconfiguration` (line 332): ends with `assertEquals(8, results.size)` and range checks — these are meaningful. Also `parseSemaphoreReconfigureToMaxThenDefault` (line 388) calls invalid values and expects `IllegalArgumentException` — genuine behavioral assertions. These two methods are CLEAN.
- **Recommended fix:** Replace `assertTrue(true, "No exception...")` with an assertion about the final state, e.g. `assertFalse(service.isOnline, "Service must be offline after all disconnects ran")`. The concurrent readers should also verify that `isOnline` values observed are always `Boolean` (no torn read) — but since Kotlin on JVM guarantees boolean reads are atomic regardless of `@Volatile`, the real test would need to use a non-boolean volatile field or run under a race-detector.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/database/DatabaseInterfaceTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 9
- **Unit(s) under test:** `NetworkStorageDatabase` (via `InMemoryDatabase` implementation)
- **Evidence:** `testStorageOperations` (line 54): performs full CRUD cycle — insert, get (asserts `id` and `name` match), update (asserts name changed to "Updated Storage"), getAll (asserts size == 1), delete (asserts `getOrNull() == null`). These are mutation-killing assertions: a stub returning null for `getStorage` breaks line 65, a stub ignoring updates breaks line 74. `testExpiredCacheCleanup` (line 184): inserts expired and non-expired entries; calls `deleteExpiredCacheEntries()`; asserts count == 1 deleted, expired entry null, valid entry non-null (lines 222–231) — precise behavioral verification.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/e2e/ErrorRecoveryE2ETests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 16
- **Unit(s) under test:** `CircuitBreaker`, `ConnectionLimiter`, `FtpService`, `SftpService`, `SmbService`, `WebDavService`, `MarkdownParser`, `DocumentCache`, `FormatRegistry`
- **Evidence:** `CircuitBreakerOpensAfterConsecutiveFailures` (line 63): asserts `cb.state == CircuitBreaker.State.OPEN` after 3 failures — fails if the circuit breaker doesn't transition. `CircuitBreakerRecoveryAfterManualReset` (line 80): asserts CLOSED after reset and `result.getOrNull() == "recovered"` — double behavioral verification. `ConnectionLimiterExhaustionRecoveryEndToEnd` (line 278): asserts `results.size == 10` and `limiter.availablePermits == 2` — verifies both all operations completed and the semaphore returned to initial count (line 296). `ConcurrentFormatPipelineAndNetworkErrorsAreIsolated` (line 443): dual assertions — all parse results non-null AND all network results isFailure (line 466–469).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/e2e/FormatPipelineE2ETests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 20
- **Unit(s) under test:** all 18 format parsers + `FormatRegistry` pipeline
- **Evidence:** `MarkdownPipelineDetectParseHtml` (line 64): asserts `format.id == ID_MARKDOWN`, `doc.rawContent == content`, and `html.contains("<h1>")` (line 75). All three assertions fail independently under different stub mutations. `RestructuredTextMetadataContainsSectionAndDirectiveCounts` (line 265): asserts `doc.metadata["sections"] == "2"` and `doc.metadata["directives"] == "1"` — precise metadata counts from real parsing. `AllFormatsParsedDocHtmlIsNonEmpty` (line 400): exercises all 17 parsers in a loop with `assertTrue(html.isNotEmpty())`.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/import_/conversion/CodeBlockDetectorTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** `CodeBlockDetector.isMonospaceRun`
- **Evidence:** `knownMonospaceFonts_areRecognised` (line 19): tests 12 real font names; documented mutation stub (`return false`) fails all 12 assertions (header lines 6–8). `proportionalFonts_areNotRecognised` (line 51): verifies 5 proportional fonts return false — kills stubs that return `true` for everything.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/import_/conversion/HeadingDetectorTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** `HeadingDetector.headingLevelByFontSize`
- **Evidence:** `headingLevelForLargestSize_returnsH1` (line 20): supplies a 4-element list and asserts `result == 1`. `clampsAtH6_whenMoreThan6DistinctSizes` (line 44): 8-element list; asserts rank-6 returns 6 and rank-7 returns null — two distinct behavioral cases that would fail under a stub returning null (header lines 6–9).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/import_/conversion/ImageExtractorTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** `ImageExtractor.fromBytes` / `ExtractedImage`
- **Evidence:** `jpgFormat_normalisedToJpeg` (line 28): asserts `image.format == "jpeg"` when input is `"jpg"`. Documented mutation (`return format` without normalisation) breaks this (header lines 6–8). `assertContentEquals(bytes, image.data)` kills stubs that modify bytes during normalisation.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/import_/conversion/LinkPreserverTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** `LinkPreserver.toMarkdownLink`
- **Evidence:** `plainLink_producesCorrectMarkdown` (line 19): asserts exact string `"[Click here](https://example.com)"`. `closingParenInUrl_isPercentEncoded` (line 33): asserts `%29` encoding — documented mutation `return text` breaks all three tests (header lines 5–10).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/import_/conversion/TableConverterTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** `TableConverter.toMarkdownTable`
- **Evidence:** `twoByTwo_producesValidGfmTable` (line 19): asserts exact content of all 4 lines — `assertEquals("| Name | Age |", lines[0])` (line 31) kills any stub returning empty or malformed output. `pipeInCell_isEscaped` (line 37): asserts `\|` escaping — documented mutation `return ""` fails both (header lines 5–9).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/import_/ImportedDocumentTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** `ImportedDocument`, `ImportWarning`, `Severity`
- **Evidence:** `roundTrip_allFieldsPreserved` (line 16): asserts 5 specific field values including `severity`, `message`, and `pageOrSection`. `warningList_mixedSeverities_preserved` (line 39): asserts `Severity.values().size == 2` — kills stubs that add or remove severity cases.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/import_/ImporterRegistryTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** `ImporterRegistry.forExtension`, `ImporterRegistry.default`, `ImporterRegistry.supported`
- **Evidence:** `lookupByExactExtension_returnsImporter` (line 33): uses `assertSame(docxImporter, found)` — reference equality, not just non-null. `normalization_dotPrefixAndCase_resolvedCorrectly` (line 43): verifies `.PDF` and `PDF` both resolve to the same importer with `assertSame`, and checks `"pdf" in registry.supported()` (line 58). `unsupportedExtension_returnsNull` (line 62): asserts null for `"xyz"` and `".unknown"` — kills stubs that return a default importer.
