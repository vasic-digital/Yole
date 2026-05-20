# Phase 5 Audit — B11 shared/desktopTest part 2
Audited: 2026-05-20  |  Files: 35  |  Bluff: 6  |  Suspect: 6  |  Clean: 23

## Findings

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/HoverTriggerDetectorTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 6
- **Unit(s) under test:** HoverTriggerDetector
- **Evidence:** Tests inject lambda collaborators (isCompletionPopupOpen, isIdentifierAt, onDwell, onExplicit) and exercise the real detector's dwell-scheduling logic. Line 102 asserts onDwell fired exactly once after a real delay; line 126 asserts subsequent move cancels the previous dwell (count==1, not 2); lines 147/166 assert guards suppress dwell (count==0); line 217 asserts dismiss() cancels a pending job. Stubbing the detector's scheduling to a no-op leaves dwellCalls empty → the count assertions fail.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/LspCodeActionRequesterTest.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 1
- **Unit(s) under test:** LspCodeActionRequester (confirmed pure interface, LspCodeActionRequester.kt:34)
- **Evidence:** `FakeLspCodeActionRequester` (lines 40-59) is an inline test double that returns a constructor-injected list. The test (76-123) constructs canned CodeActions, passes them to the fake, and asserts the fake returns them (line 106) plus recorded its args (119-122). No production implementation (`LspServerHost.codeActions()`) is ever invoked — the interface has no logic. The file's own mutation procedure (14-20) mutates the test double, not production code. Stubbing the real `LspServerHost.codeActions()` to return emptyList would not fail this test.
- **Recommended fix:** Delete as redundant with `LspServerHostTest.noSpec_codeActions_returnsEmpty`, or rewrite to drive the real `LspServerHost.codeActions()` against a fake/real LSP server.

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/LspFormattingRequesterTest.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 1
- **Unit(s) under test:** LspFormattingRequester (confirmed pure interface, LspFormattingRequester.kt:33)
- **Evidence:** `FakeLspFormattingRequester` (40-62) is an inline test double returning a constructor-injected list. The test (79-102) passes canned TextEdits to the fake and asserts the fake returns them (93-95) plus recorded args (97-101). The real production implementation is never invoked; mutation procedure (14-20) mutates the test double. A broken `LspServerHost.formatting()` would still pass this test.
- **Recommended fix:** Delete as redundant with `LspServerHostTest.noSpec_formatting_returnsEmpty`, or rewrite to drive the real `LspServerHost.formatting()`.

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/LspReferencesRequesterTest.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 1
- **Unit(s) under test:** LspReferencesRequester (confirmed pure interface, LspReferencesRequester.kt:36)
- **Evidence:** `FakeLspReferencesRequester` (40-65) is an inline test double returning a constructor-injected list. The test (83-122) passes canned ReferenceLocations to the fake and asserts the fake returns them (99-114) plus recorded args (116-121). No production code is exercised; mutation procedure (14-20) mutates the test double. A broken `LspServerHost.references()` would still pass.
- **Recommended fix:** Delete as redundant with `LspServerHostTest.noSpec_references_returnsEmpty`, or rewrite to drive the real `LspServerHost.references()`.

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/LspRenameRequesterTest.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 1
- **Unit(s) under test:** LspRenameRequester (confirmed pure interface, LspRenameRequester.kt:33)
- **Evidence:** `FakeLspRenameRequester` (41-66) is an inline test double returning a constructor-injected `WorkspaceEdit?`. The test (83-122) passes a canned WorkspaceEdit to the fake and asserts the fake returns it (104-115) plus recorded args (116-121). No production code is exercised; mutation procedure (14-20) mutates the test double. A broken `LspServerHost.rename()` would still pass.
- **Recommended fix:** Delete as redundant with `LspServerHostTest.noSpec_rename_returnsNull`, or rewrite to drive the real `LspServerHost.rename()`.

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/LspServerHostTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 11
- **Unit(s) under test:** LspServerHost
- **Evidence:** 9 tests assert real honest-degradation behavior on the real host — `complete/hover/definition/rename/codeActions/signatureHelp/formatting/references/onTypeFormatting` return empty/null for an unknown langId (assertions at 87, 137, 156, 175, 193, 212, 229, 248, 268); these fail if the spec-lookup/degradation logic is stubbed. But `shutdownAll_isIdempotent` (112-120) has only a no-throw check plus a tautological `assertEquals(0, 0)` at line 119, explicitly placed "for mutation-ratchet scanner" — a no-op `shutdownAll()` stub passes it. `noSpec_didOpen_isBenignNoOp` (95-105) is no-throw-only (carries an `ANTI-BLUFF-EXEMPT` marker).
- **Recommended fix:** Replace line 119 `assertEquals(0, 0)` with a real post-condition — e.g. assert `host.states.replayCache` is empty after double shutdown, or that a subsequent operation still degrades correctly.

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/LspServerInstallerTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** LspServerInstaller
- **Evidence:** Both tests build a real installer with an executable that is intentionally absent from the classpath, call `ensureInstalled()`, and assert `result.isFailure` plus `assertIs<LspInstallError.ExtractionFailed>` (53-57, 80-86) and that the error carries the correct langId (83-86). A `Result.success`-returning stub fails these. Failure-path-only scope is honestly acknowledged in the header.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/LspServerRegistryTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** LspServerRegistry
- **Evidence:** Asserts the real registry holds 15 specs (line 30), rust resolves to executable "rust-analyzer" with marker `["Cargo.toml"]` (37-38), c and cpp both resolve to "clangd" (47-48), and an unknown lang returns null (53). Stubbing `forLanguage` to return null fails at least 2.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/LspSignatureHelpRequesterTest.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 1
- **Unit(s) under test:** LspSignatureHelpRequester (confirmed pure interface, LspSignatureHelpRequester.kt:33)
- **Evidence:** `FakeLspSignatureHelpRequester` (41-63) is an inline test double returning a constructor-injected `SignatureHelp?`. The test (80-120) passes canned SignatureHelp to the fake and asserts the fake returns it (104-113) plus recorded args (115-119). No production code is exercised; mutation procedure (14-20) mutates the test double. A broken `LspServerHost.signatureHelp()` would still pass.
- **Recommended fix:** Delete as redundant with `LspServerHostTest.noSpec_signatureHelp_returnsNull`, or rewrite to drive the real `LspServerHost.signatureHelp()`.

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/LspWorkspaceResolverTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** LspWorkspaceResolver
- **Evidence:** Real temp directories + real marker files (Cargo.toml, go.mod). Asserts the resolved workspace root for immediate-parent marker (line 60), 6-level upward traversal (80), no-marker fallback to file.parent (97), the 20-level traversal cap (120), and nearest-marker-wins (147). The stub described in the header (`return file.parent`) fails 3 of the 5.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/RealServerSmokeTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 9
- **Unit(s) under test:** Cached LSP binaries (OS subprocesses) + LspServerHost end-to-end
- **Evidence:** When a binary is staged, tests spawn a real subprocess and the helper asserts the exit code ∉ {126,127,139} (110-113) — genuine positive evidence; `marksman_viaInstallerAndHost_serverBecomesReady` (266-326) asserts `ServerState.Ready` — strong. But several test bodies carry only tautological assertions: `assertTrue(true, ...)` at lines 152, 179, 207; `assertTrue(out.isNotEmpty() || true, ...)` is always-true at line 192; line 136's `|| !out.contains("command not found")` is a near-tautology. The helper's timeout branch (114-119) asserts nothing. In a CI without staged binaries every test SKIPs via `assumeTrue` (honest, but yields zero evidence).
- **Recommended fix:** Remove the `assertTrue(true)` / `|| true` decorations and assert on real `out`/exit-code observations in each test body; add an explicit assertion in the timeout branch (process was alive at timeout).

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/SignatureHelpMappingTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** mapLspSignatureHelpToYole
- **Evidence:** Constructs real LSP4J `SignatureInformation` for both the plain-String and MarkupContent documentation branches and calls the production mapper. Asserts extracted label, documentation, parameter labels/docs, and active indices (78-84, 113-119). A null-returning stub fails `assertNotNull` in both tests.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/SignatureHelpSpanResolverTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 7
- **Unit(s) under test:** resolveActiveParamSpan, splitSignatureParams
- **Evidence:** Asserts exact span offsets and substring tokens (lines 67, 81, 97), null on no-parens and out-of-range/negative index (50, 108-109), and the generic-type depth guard so `Map<K, V>` is not split on its inner comma (97, 137-139). Stubs returning null or `0..0` fail.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/SignatureHelpTriggerTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 5
- **Unit(s) under test:** SignatureHelpTrigger
- **Evidence:** Tests 1-4 (143-250) drive the real trigger with a fake collaborator and assert callCount / onResult delivery for '(' , ',' , ')' dismiss, and non-trigger no-op — CLEAN. But `inflight_cancelled_before_new_request` (269-310) verifies its core claim (in-flight request cancellation) only via source-grep: `src.contains("requestJob?.cancel()")`, `contains("cancelDismissTimer()")`, `contains("AUTO_DISMISS_MS"...)` at 277-288 — a structural source-text match the file header itself calls "the anti-bluff anchor." Its behavioral portion only asserts `results.isNotEmpty()` / `callCount >= 1` (305-309), which tests 1-2 already cover; the cancellation is never observed at runtime.
- **Recommended fix:** Rewrite `inflight_cancelled_before_new_request` with a slow/suspendable fake requester so the first request can be observed as cancelled (first result not delivered, second delivered); remove the source-grep assertions.

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/TextEditMappingTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** mapLspTextEditWithDoc, mapLspTextEdits
- **Evidence:** Constructs real LSP4J TextEdits and asserts exact absolute character offsets (23/25 at 74-75, 4/7 and 26/27 at 96-101) and out-of-bounds clamping (117-122). Trivial stubs (`TextEdit(0..0,"")` / `emptyList()`) fail all three. Note: `outOfBounds_clamps` (110-123) is weaker — a non-trivial `TextEdit(0..0,newText)` stub would satisfy `range ≤ length`; the trivial `emptyList()` stub still fails it on the size check.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/lsp/WorkspaceEditMappingTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** mapLspWorkspaceEditToYole
- **Evidence:** Builds real LSP4J `WorkspaceEdit` objects and asserts the mapped URI/edit-count/newText for the documentChanges-only path (66-70), the legacy-changes-only path (91-95), and the documentChanges-preferred-over-legacy rule (118-126). Stripping or swapping a branch fails the respective test.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/network/platform/DesktopHttpClientFactoryTests.kt
- **Verdict:** BLUFF
- **Methods reviewed:** 11
- **Unit(s) under test:** createHttpClient (confirmed `(): HttpClient` non-nullable, HttpClientFactory.desktop.kt:7)
- **Evidence:** 10 of 11 tests assert only tautologies or absence-of-error: `assertNotNull` on the non-nullable `HttpClient`/`engine` (lines 35, 44, 67, 74, 99, 141); the lone OkHttp-engine test is defeated by `|| engineClass.isNotEmpty()` (53-56) making it always true; `assertEquals(10, clients.size)` (112) is structurally 10 regardless of `createHttpClient`; "client follows redirects by default" (62-69) never checks redirects; two tests have no assertion at all (124, 128-133). Only `assertNotSame(client1, client2)` (86) is a real assertion. A `createHttpClient` configured with the wrong engine and redirects disabled passes the entire file — exactly the configuration the header claims to cover.
- **Recommended fix:** Assert the engine is specifically OkHttp (remove the `|| engineClass.isNotEmpty()` escape); assert the `followRedirects` configuration; exercise at least one real request against a local mock server to prove the client is functional.

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/network/platform/DesktopPlatformFileIOTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~30
- **Unit(s) under test:** PlatformFileIO / DesktopFileIO (via PlatformFileIOFactory)
- **Evidence:** Real `TemporaryFolder` and real files. Content-equality roundtrips for text/binary/UTF-8/empty/large content (59, 85, 96, 147, 158), failure result on missing file (66), exact `fileSize` values (198, 205, 212, 222), parent-directory creation (231-235), read-only-file write rejection with root guard (264-269). No-op write or empty-read stubs fail multiple tests.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/network/platform/DesktopSecureStorageTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 18
- **Unit(s) under test:** DesktopSecureStorage
- **Evidence:** Most tests are strong — the encrypted file is read back raw and asserted to NOT contain plaintext value/key (132-133), unique-IV check across two stores (293), key persistence across new instances (153), corrupted-file graceful handling (170-198). But `should test AES encryption parameters` (343-357) exercises ZERO production code: it constructs its own `KeyGenerator`/`Cipher` and asserts JDK behavior (`testKey.algorithm == "AES"`, `encoded.size == 32` at 350-351, and `cipher.blockSize in 1..16` at 356 which is always true for AES). `DesktopSecureStorage` is never invoked in that test.
- **Recommended fix:** Delete `should test AES encryption parameters`, or rewrite it to inspect `DesktopSecureStorage`'s actual key-file format / cipher configuration rather than the JDK crypto API.

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/network/platform/DesktopSecureStorageTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~30
- **Unit(s) under test:** DesktopSecureStorage
- **Evidence:** Real temp-dir storage. Encryption roundtrips for plain/numeric/JSON/multiline/unicode/null-byte values (74, 83, 92, 101, 167, 175), delete→null (241), persistence across a new storage instance (367, 381), and `encrypted file does not contain plaintext values` (461-464). No-op store/retrieve stubs fail nearly all tests. Note: the "concurrent" tests (247-318) actually run sequentially (the `async`/`awaitAll` imports are unused) — a naming inaccuracy only, the data round-trips are still genuinely asserted.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/network/platform/SecureStorageFactoryDesktopTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 12
- **Unit(s) under test:** SecureStorageFactory
- **Evidence:** Asserts the factory creates a `DesktopSecureStorage` instance (44), places it in `~/.yole/secure` (reflection on `storageDir`, 68-73), creates the directory structure (89-91), produces functional store/retrieve roundtrips (131-136), and writes an encrypted data file (257-262). A `Result.failure` or wrong-type stub fails the load-bearing tests. Weak spots noted: `should maintain consistency across factory calls` (202-207) `assertEquals(result1, result2)` is tautological for a deterministic `isAvailable()`; `should report secure storage as available` (48-52) would pass a trivial `return true` stub; `should handle permission issues gracefully` (143-168) only asserts inside an `if (result.isFailure)` branch.
- **Recommended fix:** None — clean (consider strengthening the three weak tests noted above)

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/network/protocols/ftp/FtpProtocolClientTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~75
- **Unit(s) under test:** FtpProtocolClient, FtpService, FtpEntry
- **Evidence:** Exercises the real connection-state machine — connect to port 0 / invalid host fails (159, 167), all client operations return failure when not connected (733-777) — validation with typed exceptions `ConnectionException.Failed` (189-192, 211), `getParentPath` logic with concrete expected paths (300-318), and the in-memory cache add/remove/clear/prefix-filter (485-535). Stubbing `getParentPath`/validation/cache fails these. Note: ~25 `FtpEntry` tests (53-144, 643-688) are data-class boilerplate (low value); the file is honestly scoped to disconnected behavior (real FTP transfer not exercised).
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/network/protocols/sftp/SshClientTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~38
- **Unit(s) under test:** SshClient, SftpEntry, SftpFileAttributes
- **Evidence:** Exercises the real `SshClient` connection-state machine — connect failures to port 0 / invalid host (256, 264), operations return failure with `IllegalStateException("Not connected")` (295-299, 308-310, 327-331), state preserved after failed/repeated connect (342, 354, 375). These fail if the not-connected guards are stubbed away. Note: ~22 of the methods are `SftpEntry`/`SftpFileAttributes` data-class boilerplate; real SFTP transfer is honestly not exercised.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/network/protocols/smb/SmbProtocolClientTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** ~55
- **Unit(s) under test:** SmbProtocolClient, SmbService, SmbEntry/SmbFileInfo/SmbShareInfo
- **Evidence:** Exercises the real connection-state machine and not-connected guards (276-281, 312-313, 504, 530), validation (483, 490), `getParentPath` (457-469), `getStorageInfo` field computation (433-439), and cache add/remove/clear (565-595). Stubs of these fail. Note: `SmbService getQuotaInfo` (635-643) verifies a "simulated" quota (asserts `>0`) — honestly named but a simulation rather than real SMB quota; ~20 data-class boilerplate tests.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/performance/MemoryLeakDetectionTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** Format parsers, DocumentCache, StyleSheets, SecurityEventLogger
- **Evidence:** The load-bearing assertions are real bound checks: `cache.size <= 50` during put/evict cycles and `== 0` after clear (140, 151), `StyleSheets.cacheSize <= 34` and `== 0` after clearCache (192, 199), `SecurityEventLogger.eventCount() == 100` after 10000 logs and `getEvents().size == 100` (298-303). Broken eviction/capacity logic fails these. Note: the heap-growth assertions use a lenient `|| growth < 10MB` escape (116, 160, 270, 320) — weak; `creating 100 protocol configs` (218-273) has only the lenient heap check as its assertion but still constructs and accesses real config objects.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/syntax/AndroidNativeSoIntegrityTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 6
- **Unit(s) under test:** Vendored Android NDK `.so` artifacts
- **Evidence:** Opens each `.so` file and asserts ELF magic bytes 0x7F 'E' 'L' 'F' (58-61), EI_CLASS 32/64-bit (63-67), EI_DATA little-endian (69), `e_machine` architecture matching the ABI directory (71-77), and size > 100KB (51-54). A truncated, placeholder, or wrong-architecture binary fails. Legitimate build-artifact integrity test per the CONST-039 iter-71 installable-asset addendum.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/syntax/AndroidNativeUtilsPatchTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 2
- **Unit(s) under test:** `repackageTreeSitterJarForAndroid` Gradle task output
- **Evidence:** `repackagedJarExists` (51-58) is a real existence + non-empty check. `patchedJarContainsYoleNativeUtilsClass` opens the real JAR via `ZipFile` and asserts the `NativeUtils.class` entry exists (72-75) — real — but verifies it is the *Yole-patched* version only by substring-scanning the raw class bytecode for `"loadOnAndroid"` and `"java.vm.vendor"` (80-91). The header itself states "We do NOT parse the class file — a substring scan of the constant pool is sufficient." That grep cannot distinguish an actually-declared patched method from an incidental constant-pool string.
- **Recommended fix:** Parse the class file (e.g. ASM `ClassReader`) and assert `loadOnAndroid` is an actually-declared method on `org.treesitter.utils.NativeUtils`, rather than relying on a substring match.

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/syntax/BonedeGrammarSmokeTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** TokenizerEngine + bonede Tree-Sitter grammars
- **Evidence:** `allBundledLangs_loadAndParse` drives the real engine through every supported language, asserting `tokenize` returns ≥1 token for a real snippet and `successes.size == supported.size` for all 47 langs (134-167); `unsupportedLangs_throwHonestly` asserts the 7 unbundled langs throw on `loadGrammar` (188-191); `bonedeRegistry_isComplete` checks the registry count (202-207). An empty-list `tokenize` stub fails all 47 sub-assertions. Exemplary anti-bluff design with per-lang positive-evidence reporting.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/syntax/LegacyThemeParityTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 2
- **Unit(s) under test:** VsCodeThemeParser + bundled Yole-Light/Dark theme JSON
- **Evidence:** Loads the real JSON theme resources, parses them with the production `VsCodeThemeParser`, and asserts that every key in the legacy palette resolves to a byte-for-byte identical ARGB value (`assertEquals(expected, actual)` at 64-70, 86-92). Flipping any color byte in either JSON file fails the test. The legacy palette iterated over is a fixed production constant (`LegacyThemeBridge.legacyLight/Dark`), so the loop is not vacuous.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/syntax/PreviewCodeBlockHighlighterTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 3
- **Unit(s) under test:** PreviewCodeBlockHighlighter
- **Evidence:** Uses the real `TokenizerEngine` + real `SyntaxHighlighter`. `rewritesFencedMarkdownBlock` asserts the rewritten HTML contains `<span class="tok-` spans (real tokenization occurred) and preserves surrounding `<p>` markup (80-86); `preservesBlockWhenFormatDisabled` and `preservesBlockWithoutLangClass` assert no `tok-` spans and verbatim block preservation (99-126). A verbatim-return `buildTokenSpans` stub fails test 1.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/syntax/SyntaxHighlighterTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 4
- **Unit(s) under test:** SyntaxHighlighter
- **Evidence:** Real engine + real theme. `highlightingProducesNonEmptyAnnotatedString` asserts `annotated.text == input` and `spanStyles.isNotEmpty()` (78-91) — fails if `AnnotatedStringBuilder.build` is stubbed to return an unstyled string; `disabledLangReturnsUnstyledText` asserts exactly 0 span styles when grammar disabled (105-109); `tokensApi_*` assert empty vs non-empty token lists by enabled state (121, 136-139).
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/syntax/SyntaxHighlightingSourceInvariantsTest.kt
- **Verdict:** SUSPECT
- **Methods reviewed:** 3
- **Unit(s) under test:** commonMain/syntax source tree (architecture-lint invariants)
- **Evidence:** All three tests are structural source-grep tests by design (no `android./java./javax.` imports — 52-71; expect/actual symmetry — 74-107; no `runBlocking` in production — 110-129). `commonMainSyntax_hasNoPlatformImports` and `productionCode_doesNotUseRunBlocking` pass vacuously if `walkKotlin(commonMainSyntax)` returns an empty list (e.g. the directory path resolves wrong): `offenders` stays empty, the `assertTrue(offenders.isEmpty())` passes, and nothing confirms any file was actually scanned. `expectClassesHaveFourActuals` does guard against this with `assertTrue(expectClasses.isNotEmpty())` (86-89).
- **Recommended fix:** Add `assertTrue(walkKotlin(commonMainSyntax).isNotEmpty(), "must scan ≥1 source file")` to both ungar­ded tests so a wrong-path resolution fails loud instead of passing vacuously. These are acceptable as architecture-lint tests once the vacuous-pass hole is closed.

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/syntax/ThemeRegistryTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 6
- **Unit(s) under test:** ThemeRegistry
- **Evidence:** Asserts `available()` exposes built-in names (58-59), and `setActiveSwitchesTheTheme` (85-114) drives the real `setActive` and asserts the `activeTheme` StateFlow value changes across bootstrap→Yole Light→Yole Dark with distinct `editor.background` colors — a no-op `setActive` stub fails this. Also: unknown name throws `IllegalArgumentException` (117-127), `setActive(Theme)` bypasses built-in lookup and propagates a handcrafted theme (130-142).
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/syntax/TokenizerEngineJvmTest.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** TokenizerEngine (desktop actual)
- **Evidence:** Drives the real Tree-Sitter engine: `initialize` success (45-48), `isGrammarLoaded` flips false→true after `loadGrammar` (55-57), `tokenizesMarkdownSnippet` asserts ≥5 leaf tokens, byte-range coverage ≥10, a non-blank scope, first token `startByte == 0`, and scope not a placeholder ("TODO"/"STUB") (74-107), disabled grammar throws `FormatDisabledException` (118-119), unknown lang throws `IllegalArgumentException` mentioning the lang (136-141). An empty-list tokenize stub fails `tokenizesMarkdownSnippet`.
- **Recommended fix:** None — clean

---

### shared/src/desktopTest/kotlin/digital/vasic/yole/util/FileStorageDesktopTests.kt
- **Verdict:** CLEAN
- **Methods reviewed:** 5
- **Unit(s) under test:** FileHandle
- **Evidence:** Real temp files. `write and read roundtrip` asserts the written content equals the read-back content and the file exists on disk (31-39); `write overwrites existing content` (43-53); `readBytes returns null for non-existent file` (56-59); `exists returns false` (62-65); `write creates parent directories` asserts the nested file exists after write (74-76). A no-op `writeBytes` stub fails the roundtrip and parent-dir tests.
- **Recommended fix:** None — clean

---
