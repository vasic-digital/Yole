# Phase 5 Audit — B08 shared/commonTest integration/language/lsp/model/monitoring/performance/safety/security
Audited: 2026-05-20  |  Files: 28  |  Bluff: 0  |  Suspect: 8  |  Clean: 20

## Findings

### shared/src/commonTest/kotlin/digital/vasic/yole/format/integration/EndToEndResponsivenessTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** ~10
- **Unit(s) under test:** FormatRegistry, all 18 format parsers, StyleSheets, DocumentCache
- **Evidence:** Line 256 contains a tautological disjunction: `assertTrue(htmlLight != htmlDark || htmlLight.isNotEmpty(), "HTML should be generated for both modes")`. The second operand (`htmlLight.isNotEmpty()`) is always true for any non-trivially-broken parser, meaning the assertion passes even if `htmlLight == htmlDark` (i.e., theme-differentiation is broken). The intent was to verify both that HTML is generated AND that light ≠ dark, but the `||` short-circuits so the first condition is never actually required. A stub returning the same string for both themes passes this assertion.
- **Recommended fix:** Split into two assertions: `assertTrue(htmlLight.isNotEmpty())` and `assertNotEquals(htmlLight, htmlDark)` (or document why identical light/dark output is valid for this format).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/format/integration/NonBlockingGuaranteeTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~8
- **Unit(s) under test:** FormatRegistry, all 18 format parsers, DocumentCache, withTimeoutOrNull
- **Evidence:** Tests use real parsers with realistic content and assert non-null results within timeouts, plus content presence checks. A stub returning null instantly would fail the timeout-based non-null assertions. Real behavioral coverage confirmed.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/language/BracketPairsTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~6
- **Unit(s) under test:** `language/affordance/BracketPairs.kt` — `closerFor()`
- **Evidence:** `assertEquals(')', pairs.closerFor('('))`, `assertEquals(']', pairs.closerFor('['))`, `assertNull(pairs.closerFor('x'))`. A stub `closerFor()` returning null would fail the first two; a stub returning `'x'` would fail all three. Production `closerFor()` uses `firstOrNull { it.open == ch }?.close` — fully exercised.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/language/CommentSyntaxTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~5
- **Unit(s) under test:** `language/affordance/CommentSyntax.kt` — `toggleLine()`
- **Evidence:** `assertEquals("    // val x = 42", result)` for comment-on case; `assertEquals("    val x = 42", result)` for comment-off (uncomment) case. A stub returning the input unchanged fails both. Production logic trims prefix detection and handles indent-preserving insertion — all branches are exercised.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/language/IndentRulesTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~5
- **Unit(s) under test:** `language/affordance/IndentRules.kt` — `computeIndent()`
- **Evidence:** `assertEquals("        ", next)` for opener-line case (double indent width), `assertEquals("    ", next)` for continuation, `assertEquals("", next)` for empty line. A stub returning "" always fails the opener and continuation cases. Real behavior is exercised.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/language/LanguageAffordanceParityTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~8
- **Unit(s) under test:** `language/LanguageMetadata.kt` — `LanguageMetadata.all` (55-language manifest), `BracketPairs`, `CommentSyntax`, `IndentRules`
- **Evidence:** `fifty_fiveLanguagesShipped` asserts `assertEquals(55, LanguageMetadata.all.size)` — a stub returning an empty list fails this. Per-language property assertions (`assertEquals(1, meta.comment.prefixes.size)` etc.) are killed by zero-filling stubs. Documented mutation anchor in file header covers the ≥2 failure requirement.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/language/LanguageRegistryTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~6
- **Unit(s) under test:** `language/LanguageRegistry.kt` — `get()`, `detectByFilename()`, `all()`
- **Evidence:** `assertEquals("kotlin", registry.get("kotlin")?.id)`, `assertEquals("python", registry.detectByFilename("script.py")?.id)`, `assertEquals(55, registry.all().size)`. Stub returning null kills first two; stub returning empty list kills third. Fully behavioral.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/lsp/DiagnosticTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~4
- **Unit(s) under test:** `lsp/Diagnostic.kt` — `Severity` enum, `Diagnostic` data class
- **Evidence:** `assertEquals(setOf("Error","Warning","Information","Hint"), all.map { it.name }.toSet())` — a stub returning a 3-value enum fails this. `assertEquals(Severity.Error, diag.severity)` — stub returning Warning fails. Field round-trip assertions on message, range, source also killed by stubs.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/lsp/EditorNavigationStackTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 8 (6 in main class + 2 cross-file/intra-file simulateBackHandler tests)
- **Unit(s) under test:** `lsp/EditorNavigationStack.kt`, `lsp/NavEntry.kt`
- **Evidence:** `assertEquals(entry, result)` for push/pop round-trip — stub returning null fails. `assertEquals(cap, stack.size)` after overflow — stub without eviction fails. `assertEquals(1, stack.size)` for consecutive duplicate suppression — stub without dedup fails. Cross-file nav: `assertTrue(crossFileNavigated)` and `assertFalse(contentChangedCalled)` — stub routing to wrong branch fails. All documented mutation procedures confirmed.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/lsp/LspRangeMappingTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** `lsp/LspRangeMapping.kt` — `lineColToOffset()`
- **Evidence:** `assertEquals(6, offset)` for line1/col0 — stub returning 0 fails. `assertEquals(8, offset)` for line1/col2 — stub returning 0 fails. `assertEquals(5, offset)` for out-of-bounds clamp — stub returning 0 fails. Production implementation confirmed pure and exercised by real numeric assertions. Documented mutation procedure in file header satisfies ≥2 failure requirement.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/lsp/LspServerSpecTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~4
- **Unit(s) under test:** `lsp/LspServerSpec.kt` — serialization, deserialization, equality
- **Evidence:** JSON round-trip: serialize to JSON string then deserialize back and assert `assertEquals(spec, roundTripped)`. `assertEquals(listOf("rust"), spec.langIds)` — stub returning empty list fails. `assertNotEquals(specA, specB)` for different specs — stub returning always-equal fails. Real behavioral coverage.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/lsp/WorkspaceEditPhase1Tests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 9 (3 TextEdit + 2 WorkspaceEdit + 4 WorkspaceEditApplier)
- **Unit(s) under test:** `lsp/TextEdit.kt`, `lsp/WorkspaceEdit.kt`, `lsp/WorkspaceEditApplier.kt`
- **Evidence:** `assertEquals("Hello, Kotlin!", result)` for replace-middle — stub returning input unchanged fails. `assertEquals("abXYZ", result)` for clamping — stub fails. `assertFailsWith<WorkspaceEditApplier.ApplyConflict>` for overlapping ranges — stub not throwing fails. `assertFalse(result.containsKey(unknownUri))` — stub adding unknown key fails. All documented mutation procedures confirmed.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/model/DocumentAdvancedTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** ~20
- **Unit(s) under test:** `model/Document.kt`, `format/TextFormat.kt`, `format/FormatRegistry.kt`
- **Evidence:** The first block of tests (roughly 8–10 methods) consists exclusively of constant-equality checks: `assertEquals("markdown", TextFormat.ID_MARKDOWN)`, `assertEquals("todotxt", TextFormat.ID_TODOTXT)`, etc. These assertions survive any behavioral stub of the parsing logic — they only verify that compile-time string constants haven't changed. A stub `Document` class that returns hardcoded format IDs would pass all these constant checks. Behavioral tests (detectFormatByExtension, detectFormatByContent, touch timing) are present later and are genuine. The constant-only tests provide zero mutation kill evidence for the document processing pipeline.
- **Recommended fix:** Convert constant checks into registry-lookup round-trips: `assertEquals(TextFormat.ID_MARKDOWN, FormatRegistry.detectByExtension("md")?.id)` instead of bare constant equality. Alternatively, add ANTI-BLUFF-EXEMPT comments if these are intentional format-ID contract guards.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/model/DocumentFormatTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** ~12
- **Unit(s) under test:** `model/Document.kt`, `format/TextFormat.kt`, `format/FormatRegistry.kt`
- **Evidence:** `testAllFormatConstants` iterates `TextFormat.Companion` reflective fields and asserts each is a non-empty string — survives any behavioral stub; it only verifies the constant declarations. `testFormatConstantsDelegateToTextFormat` checks that `Document.FORMAT_MARKDOWN == TextFormat.ID_MARKDOWN` — again pure constant equality, no behavioral exercise. Additionally, `testDetectFormatByContentCsv` at line ~182–185 uses a conditional: `if (detected) { assertEquals(...) }` — if `detected` is false (parser unregistered or broken) the assertion is silently skipped rather than failed. A stub returning `detected = false` passes this method without exercising CSV content detection.
- **Recommended fix:** Replace `if (detected) assertEquals(...)` with a direct `assertEquals(expectedFormat, actualFormat)` so detection failures cause test failures. Remove or annotate pure constant-equality tests.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/model/DocumentModelTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** ~50
- **Unit(s) under test:** `model/Document.kt`, `model/DocumentSyncStatus.kt`
- **Evidence:** The first ~20 test methods are constant-equality checks on `Document.FORMAT_*`, `Document.STATUS_*`, and similar string/int constants. These survive any behavioral stub. `DocumentSyncStatus` behavioral tests are strong: `assertEquals(75, status.progressPercentage)`, clamping of withProgress to [0,100], `withError` incrementing retryCount — all killed by stubs. The constant-only block (roughly the first third of the file) is the suspect portion; the sync-status behavioral block is clean and provides genuine coverage.
- **Recommended fix:** Move constant checks to a dedicated `DocumentConstantsContractTest` with an ANTI-BLUFF-EXEMPT marker explaining they are API contract guards, and keep behavioral tests in the main test class.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/model/DocumentStressTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~8
- **Unit(s) under test:** `model/Document.kt`, `format/FormatRegistry.kt`, `format/DocumentCache.kt`
- **Evidence:** Serialization round-trips assert field-by-field equality; concurrent creation verifies all 100 documents are distinct and non-null; idempotent detection asserts same format returned on repeated calls; change-tracking stress asserts modified flag correctly set. All stub-killable assertions confirmed.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/model/DocumentTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~10
- **Unit(s) under test:** `model/Document.kt`, file operations, timestamp tracking
- **Evidence:** `assertFalse(doc.fileExists())` for non-existent path — stub returning `true` always fails. `assertEquals(expectedFormat, doc.format)` for format detection — stub returning null fails. `assertTrue(doc.lastModified > before)` after modification — stub returning 0 fails. Real behavioral assertions throughout.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/monitoring/MetricsCollectionTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~8
- **Unit(s) under test:** `monitoring/MetricsReporter.kt`, `format/DocumentCache.kt`, `format/TextParser.kt`
- **Evidence:** `assertEquals(3L, cache.hits)` after 3 cache hits — stub returning 0 fails. `assertEquals(0.75, cache.hitRate, 0.01)` — stub fails. `parsedDocumentHtmlNotAllocatedUntilNeeded` tests lazy HTML allocation by checking `assertFalse(doc.isHtmlCached)` before access and `assertTrue(doc.isHtmlCached)` after — stub returning always-true for both fails. Parse time budget assertions confirm wall-clock behavior.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/monitoring/MetricsReporterUnitTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 32
- **Unit(s) under test:** `monitoring/MetricsReporter.kt`, `monitoring/MetricsSnapshot.kt`, `monitoring/PerformanceMetrics.kt`
- **Evidence:** `reportNowReturnsCurrentMetricsState`: `assertEquals(2L, snapshot.parseCount)`, `assertEquals(150L, snapshot.parseTotalMs)`, `assertEquals(75.0, snapshot.parseAvgMs)` — stub returning zeros fails all three. `snapshotParseMaxMsIsMaximumOfAllRecorded`: `assertEquals(200L, snap.parseMaxMs)` — stub returning minimum fails. Reset behavior, concurrent reporting, observer notification all have precise numeric assertions killed by stubs.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/monitoring/PerformanceMetricsTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~10
- **Unit(s) under test:** `monitoring/PerformanceMetrics.kt`, `monitoring/MetricsSnapshot.kt`
- **Evidence:** Counter increment: `assertEquals(3L, metrics.parseCount)` after 3 recordings — stub returning 0 fails. Concurrent recording: `assertEquals(5000L, snap.parseCount)` after 5000 coroutine increments — stub fails. JSON round-trip: deserialized snapshot equals original. Computed fields (parse average, cache ratio) assert within delta. All stub-killable.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/performance/LazyInitSemaphoreTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~6
- **Unit(s) under test:** `format/FormatRegistry.kt` (lazy init), `format/StyleSheets.kt` (cache), `network/common/ConnectionLimiter.kt`
- **Evidence:** `assertEquals(2, limiter.availablePermits)` after 10 concurrent ops with 2-permit semaphore — stub not releasing permits would leave 0 available and fail. `assertFalse(FormatRegistry.isFormatsInitialized)` before first access — stub pre-initializing fails. `assertTrue(cache.size <= maxCacheSize)` after overflow stress — stub with unbounded cache fails. Real resource accounting exercised.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/performance/OverloadResilienceTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~8
- **Unit(s) under test:** `format/FormatRegistry.kt`, `format/DocumentCache.kt` (LRU eviction), `format/StyleSheets.kt`, `security/SecurityEventLogger.kt`, `network/common/CircuitBreaker.kt`
- **Evidence:** `assertNull(oldEntry)` after inserting 1001 entries into a 1000-cap LRU cache — stub without eviction leaves oldEntry non-null and fails. `assertTrue(results.all { it != null })` for 500 concurrent detections. SecurityEventLogger capacity enforcement: `assertEquals(maxEvents, logger.size)` — stub without cap enforcement fails. CircuitBreaker alternation assertions. All stub-killable.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/performance/PerformanceBaselineRegressionTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** ~8
- **Unit(s) under test:** `format/FormatRegistry.kt`, `format/DocumentCache.kt`, all 18 format parsers
- **Evidence:** All per-format parse time assertions use `assertTrue(elapsed.inWholeMilliseconds < 200)`. A stub parser returning `ParsedDocument("", "")` instantly would complete in ~0ms and pass every timing assertion. `formatRegistryIsLazyInitialized` only checks `assertFalse(FormatRegistry.isFormatsInitialized)` before first access — metadata assertion surviving any behavioral stub. `documentCacheHitReturnsUnder1ms` asserts only timing, not correctness of the returned document — a stub returning a stale or empty document in <1ms passes. No assertions verify the content or correctness of parse results.
- **Recommended fix:** Add content-correctness assertions alongside timing assertions: after parsing known input, assert that key structural elements are present in the output (e.g., `assertTrue(result.parsedContent.contains("expected heading"))`) so that no-op stubs fail.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/performance/ResponsivenessStressTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~6
- **Unit(s) under test:** `format/FormatRegistry.kt`, `format/DocumentCache.kt`, `format/StyleSheets.kt`
- **Evidence:** `assertTrue(css.contains("<style>"))` for styled formats — CSS stub returning empty string fails. `assertTrue(result.all { it.second })` where `second` is `rawContent.isNotEmpty()` — stub returning empty string fails. Cache concurrency stress: `assertEquals(1000, populated)` after 1000 inserts — stub failing concurrent writes fails. Strong behavioral assertions throughout.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/safety/MemoryLeakRegressionTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~8
- **Unit(s) under test:** `format/DocumentCache.kt` (LRU bounds), `network/common/CircuitBreaker.kt`, `network/common/ConnectionLimiter.kt`
- **Evidence:** Loop assertion after each of 200 cache inserts: `assertTrue(cache.size <= maxSize)` at every iteration — stub with unbounded growth fails on iteration maxSize+1. LRU eviction: `assertNull(firstInserted)` after overflow — stub fails. CircuitBreaker call count: `assertEquals(expectedCalls, breaker.totalCallCount)` — stub fails. Permit leak check: `assertEquals(initialPermits, limiter.availablePermits)` after acquiring and releasing — stub not releasing fails.
- **Recommended fix:** None.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/safety/NullSafetyTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** ~12
- **Unit(s) under test:** `model/Document.kt`, `network/protocol/NetworkOperation.kt`, `network/common/StorageConfig.kt`
- **Evidence:** Several tests assert properties of objects the test itself just constructed with those values: `assertNull(op.error)` where the test passed `error = null` to the constructor — this only verifies the constructor stores what it receives, not that the production code handles null correctly. `assertNull(entry.checksum)` where `checksum = null` was passed. `assertNotNull(config.name)` on a field the test just set. `parsers handle null-like edge cases` uses only `assertNotNull(result)` (absence-of-crash only — a stub returning `ParsedDocument("","")` passes). `service rejects path traversal` using `assertTrue(result.isFailure)` is behavioral and clean. The trivially-true constructor-readback assertions are the suspect portion.
- **Recommended fix:** Replace trivially-true constructor readback assertions with tests that exercise null propagation through real logic: pass null to a method that processes the field and assert the correct downstream behavior. Keep `isFailure` assertions as-is.

---

### shared/src/commonTest/kotlin/digital/vasic/yole/security/CredentialStorageSecurityTests.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** ~10
- **Unit(s) under test:** `network/common/StorageConfig.kt`, `network/NetworkStorageConfigService.kt`
- **Evidence:** `configServiceValidationDoesNotLeakCredentials`: `assertFalse(msg.contains("SuperSecret123"))` is behavioral. `assertNotEquals(configA, configB)` for configs with different passwords is behavioral. However, `webDavConfigPasswordAccessible`: `assertEquals("SuperSecretP@ss", config.password)` is a trivially-true constructor readback — the test set the password and immediately reads it back; no processing logic is exercised. `quotaInfoDoesNotContainAuthData`: `assertFalse(str.contains("key", ignoreCase = true))` uses "key" as the exclusion token, which is so broad it would flag legitimate non-credential field names (e.g., `apiKey` in quota metadata is expected, but "key" alone also matches benign strings like "monkey").
- **Recommended fix:** Replace trivially-true password readback with a test that exercises serialization or logging code paths to confirm credentials don't appear in output. Tighten the `quotaInfo` assertion to check for specific credential patterns (e.g., `assertFalse(str.contains(password))` using the actual password value).

---

### shared/src/commonTest/kotlin/digital/vasic/yole/security/InputValidationSecurityTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** ~8
- **Unit(s) under test:** `format/TextParser.kt` (escapeHtml), `network/protocol/NetworkService.kt` (validatePath), `network/common/StorageConfig.kt`
- **Evidence:** `HTML escaping prevents script injection`: `assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", escaped)` is strong and stub-killable. `service rejects path traversal attempts`: uses only `assertNotNull(result)` — does NOT assert `result.isFailure`; a stub returning `Result.success("")` passes without any path validation. `link URLs are sanitized`: only `assertNotNull(result)` (absence-of-crash), no assertion on sanitized content. `storage handles many concurrent requests`: only `assertEquals(100, results.size)` — a stub returning 100 successes without processing passes. `todotxt handles malformed priority`: only `assertNotNull(result)` — stub returning empty doc passes.
- **Recommended fix:** Replace `assertNotNull(result)` with `assertTrue(result.isFailure)` for path traversal tests, and `assertEquals(sanitizedUrl, result.getOrThrow())` for URL sanitization. Assert on result content, not just result presence.
