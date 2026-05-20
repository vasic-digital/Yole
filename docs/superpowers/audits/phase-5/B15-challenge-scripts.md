# Phase 5 Audit — B15 Challenge Scripts
Audited: 2026-05-20  |  Scripts: 39  |  Bluff: 5  |  Suspect: 5  |  Clean: 29

## Findings

### anchor_manifest_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Lines 61–98 walk every row of `docs/behavior-anchors.md`; for each `active` row it checks the anchor file exists (line 64) AND that the named symbol is present in that file via `grep -qE "(func|fun )${sym}"` (line 74) or backtick variant (line 76). PASS only exits if `failed == 0` (line 116).
- **Runtime evidence:** Reads real source files from disk; verifies function symbols exist in real `.kt`/`.go` files. Failure exits non-zero with specific `id: symbol not found` messages.
- **Recommended fix:** none

### anti_bluff_cascade_audit_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Lines 64–82 iterate over each governance file (`CLAUDE.md`, `AGENTS.md`, `CONSTITUTION.md`, `Constitution.md`) in the parent repo and each of 10 owned submodule roots; for each file runs `grep -qiE "$PATTERN" "$dir/$f"` (line 74) where `PATTERN` requires at least one of: verbatim operator quote, `CONST-035`, `CONST-039`, `Article XI §11.9`, `§11.4`, `anti-bluff`. PASS (line 107) requires `failures == 0`.
- **Runtime evidence:** Reads real governance files on disk; fails loudly on any missing anchor with per-file `[FAIL]` lines.
- **Recommended fix:** none

### app_name_survives_r8_challenge.sh
- **Verdict:** SUSPECT
- **PASS mechanism:** Layer A (lines 68–101) greps source files for `android:label=` in `AndroidManifest.xml` and `manifestPlaceholders["appLabel"]` in `build.gradle.kts`. Layer B (lines 103–201) opens an APK with `aapt2 dump badging` and checks `application-label`. However, when no APK exists OR when `aapt2` is absent, the script exits 0 at line 133 after printing only `[SKIP] LAYER B: aapt2 unavailable` — only the static source-grep (Layer A) ran.
- **Runtime evidence:** When APK and `aapt2` are both present: real artifact opened, real label extracted, compared. When either is absent: only source-grep evidence — the user-visible label on a built APK may still be wrong.
- **Recommended fix:** Add a hard FAIL when no APK has been produced (or when `aapt2` is absent), unless a dated skip-ticket is present. A source-grep alone cannot confirm the label survives R8 resource shrinking.

### auto_complete_completeness_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static layer (lines 60–128) checks filesystem existence of 3 provider files and 12 foundation files. Runtime layer (lines 135–199) runs `./gradlew :shared:desktopTest --tests "digital.vasic.yole.completion.*"` and requires `passed >= 50` (line 192) AND `unexpected_failures == 0` (line 187). Log path emitted (line 185).
- **Runtime evidence:** Real Gradle test run; log file path captured; 50+ test PASSes required; specific K2-stub exemptions tracked by name.
- **Recommended fix:** none

### bluff_scanner_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Two sequential phases: (1) `scripts/anti-bluff/tests/run-fixtures.sh` (line 20) — self-test that deliberately-bad fixtures must produce known verdicts; (2) `scripts/anti-bluff/bluff-scanner.sh --mode all` (line 24) — full tree scan. Both must exit 0.
- **Runtime evidence:** Executes the scanner against live source files; self-test ensures scanner is not itself bluffing.
- **Recommended fix:** none

### codegraph_integration_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** 10 distinct checks (lines 40–109): (1) `codegraph` binary on PATH; (2) `.codegraph/codegraph.db` exists with size reported; (3) `codegraph status` output has `Nodes: >= 5000` (line 58); (4) `codegraph query FormatRegistry` returns a real `digital/vasic/yole/...kt:NNN` file:line hit (line 68); (5) real MCP JSON-RPC handshake via stdio yielding `>= 6 codegraph_*` tools (lines 76–86); (6–10) each of 5 CLI agent config files contains a `codegraph` MCP entry verified by Node eval. PASS requires `FAIL == 0` (line 108).
- **Runtime evidence:** Queries real index DB; performs real MCP stdio handshake; checks real JSON config files; emits log artefact path.
- **Recommended fix:** none

### cross_platform_parity_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Lines 49–63 grep each of 4 platform source roots for composable definitions named `FileBrowserScreen` or `IdeFileBrowser`, compare counts against per-platform max limits, and emit per-platform actual count. PASS prints `OK: <platform> — <count> File Browser composable(s)` (line 63) for every platform.
- **Runtime evidence:** Reads real Kotlin source files on disk; emits actual counts found per platform.
- **Recommended fix:** none

### display_version_consistency_challenge.sh
- **Verdict:** SUSPECT
- **PASS mechanism:** Static layer (lines 40–94) extracts `versionName` from `androidApp/build.gradle.kts` then greps production Kotlin source for `X.Y.Z` strings in version contexts that don't match canonical. Runtime APK layer (lines 98–135) uses `aapt2` to check the release APK, but at line 121 the APK glob uses `|| true` and at line 123 an empty-string check exits cleanly with `[SKIP-OK: no ... apk; build + retry]`. The desktop check (lines 138–145) only greps `build.gradle.kts` for `packageVersion` string — never opens the desktop binary.
- **Runtime evidence:** APK layer: real artifact if present. Desktop layer: source grep only — no binary opened. Static layer: source grep. If APK is absent (common on dev machines), only source-grep evidence.
- **Recommended fix:** Open the desktop binary (DMG or tarball) and extract version from it. Make APK-absent path exit non-zero unless an explicit skip-ticket is provided.

### format_enablement_default_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static layer (lines 30–50) greps `FormatRegistry.kt` for the exact `defaultEnabledFormatIds()` function returning `setOf("markdown")` or `setOf(ID_MARKDOWN)` with constant resolution. Runtime layer (lines 52–69) runs `./gradlew :shared:desktopTest --tests "*FormatEnablementDefaultTest*" --tests "*FormatEnablementGateTest*"` and requires `passed > 0`. Log path emitted.
- **Runtime evidence:** Real Gradle test execution; log path emitted; PASS count reported.
- **Recommended fix:** none

### helixqa_evidence_size_portable_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** 5 checks (lines 34–93): (1) `helixqa-validate.sh` exists; (2) `get_file_size()` declared in it; (3) no bare `stat -c%s` in it; (4) extracts `get_file_size()` via awk, sources it, creates a known 1234-byte temp file via `dd` (line 64), calls the function, and asserts reported size equals 1234 (lines 86–92); (5) documentation note. PASS requires `FAIL_COUNT == 0` (line 108).
- **Runtime evidence:** Executes the real `get_file_size` function against a freshly-created 1234-byte file; verifies reported size matches on the running host OS.
- **Recommended fix:** none

### helixqa_scenario_coverage_challenge.sh
- **Verdict:** SUSPECT
- **PASS mechanism:** Static layer (lines 47–115): directory exists, `>= 7` scenario YAMLs counted, `coverage-matrix.md` present, per-scenario mandatory fields present by `grep "$field"`, each YAML has `evidence_type:` key, matrix references all 7 known features. Runtime layer (lines 119–139): if ADB device absent, emits `skip "No ADB device/emulator available — runtime execution DEFERRED"` and exits 0 (lines 137–139, 152–155). On a dev machine without emulator, the gate always exits 0 after static checks only.
- **Runtime evidence:** Static: YAML field-presence checks by `grep` (not YAML parse). Runtime: entirely deferred if no ADB device — no evidence any HelixQA scenario was ever executed with a screenshot.
- **Recommended fix:** (a) Validate YAML syntax via a proper parser (not just grep). (b) Require at least one evidence artefact file per scenario to exist from a prior run — if absent, FAIL rather than SKIP. This proves scenarios have been run at least once. Currently a YAML file with `evidence_type:` in a comment would pass.

### host_no_auto_suspend_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** On macOS (lines 46–75): runs `pmset -g` and checks `sleep` value is `0` or has a runtime prevention annotation (e.g., from `caffeinate`). On Linux (lines 83–142): 4 real assertions: `systemctl is-enabled` for 4 sleep targets must return `masked`; `AllowSuspend=no` in `sleep.conf`; logind `IdleAction == ignore|<unset>`; `journalctl` search for suspend events since fix marker. PASS requires `FAIL_COUNT == 0`.
- **Runtime evidence:** Queries real system state via `pmset`, `systemctl`, `grep` on config files, `journalctl`. These are live host-configuration checks, not source-tree checks.
- **Recommended fix:** none

### import_from_completeness_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static layer checks 14 files exist. Desktop runtime layer runs `./gradlew :shared:desktopTest --tests "digital.vasic.yole.import_.*"` with `passed >= 30` and `failed == 0` (lines 156–166). Robolectric layer runs `:androidApp:testDebugUnitTest -PincludeRobolectric=true --tests "*Import*"` with `passed >= 8` and `failed == 0` (lines 196–208). Both log paths emitted.
- **Runtime evidence:** Real Gradle test execution on both desktop JVM and Android/Robolectric; counts and log paths emitted.
- **Recommended fix:** none

### import_from_fixture_bundle_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static layer checks 7 UI files exist. Runtime layer runs 6 importer test classes via `./gradlew :shared:desktopTest` and verifies each class produced `>= 1 PASSED` (lines 145–157) AND `total_failed == 0` (line 139). Total PASS count and log path emitted.
- **Runtime evidence:** Real Gradle test execution; per-class PASSED counts verified individually; log path emitted.
- **Recommended fix:** none

### installable_app_icon_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Layer A (lines 74–146): 7 source-tree static checks including vector XML existence and `@mipmap/` reference absence. Layer B (lines 148–268): opens real APK with `aapt2 dump badging`; verifies `>= 5` `application-icon-*` lines; `mipmap/ic_launcher` `(anydpi)` slot; drawable in resource table; each icon file `>= 100` bytes via `unzip -l`. Layer C (lines 271–319): `grep` for `viewportWidth="108"`, `pathData`, no `@mipmap/` in `android:foreground/monochrome`. Layer D (lines 327–421): Python3 XML parser extracts `<path> fillColor`; resolves `@color/BG` from `colors.xml`; asserts `FG_NORM != BG_NORM` (catches the iter-91 invisible-icon bug).
- **Runtime evidence:** APK opened with `aapt2`; file sizes checked via `unzip -l`; icon colors computed via Python3 XML parser. Layers B+D skip gracefully with documented `SKIP-OK` tickets when APK is absent.
- **Recommended fix:** none

### installable_desktop_icon_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Layer A: `xxd -l 4` magic-byte check for ICNS `69636e73` (not PNG `89504e47`), file size `>= 30720` bytes, `build.gradle.kts` references `icons/icon.icns`. Layer B: mounts DMG via `hdiutil attach`, reads `Yole.app/Contents/Resources/Yole.icns`, verifies magic bytes and size `>= 30720`, checks `Info.plist CFBundleIconFile`. Layer C: Python3 parses ICNS binary structure and verifies `>= 2` size-variant chunks from known list (ic04 through it32).
- **Runtime evidence:** `xxd` reads real file bytes; Python3 parses real ICNS chunk structure; `hdiutil` mounts real DMG when available. Layer B skips with `SKIP-OK` ticket when DMG absent.
- **Recommended fix:** none

### language_grammar_bundle_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static layer (lines 55–85) counts `implementation(libs.ts.<lang>)` and `implementation(libs.tree.sitter.markdown)` lines in `shared/build.gradle.kts`, requires `>= 47`. Runtime layer runs `./gradlew :shared:desktopTest --tests "*.BonedeGrammarSmokeTest.allBundledLangs_loadAndParse" --tests "*.BonedeGrammarSmokeTest.bonedeRegistry_isComplete"`, requires `passed >= 2`, `failed == 0`, AND verifies the log contains `"47"` (line 136) — the registry-count assertion output.
- **Runtime evidence:** Real Gradle test execution; per-language parse report extracted from log; log path emitted; log must contain "47" count assertion output from the real test.
- **Recommended fix:** none

### language_support_completeness_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static layer (lines 57–135): checks `LanguageMetadata.kt` has `>= 50` `LanguageFormat` properties; for each language extracted by grep, checks 3 `.scm` files (highlights, folds, outline) exist and are non-empty (`-s` check), AND a fixture directory has files. Runtime layer (lines 140–185) runs 7 test filters via `./gradlew :shared:desktopTest`, requires `passed >= 30` and `failed == 0`. Log path emitted.
- **Runtime evidence:** Per-language filesystem checks with non-empty file assertions (`[[ ! -s "${scm_path}" ]]` at line 104); real Gradle test run; PASS count emitted.
- **Recommended fix:** none

### lsp_binary_bundle_challenge.sh
- **Verdict:** SUSPECT
- **PASS mechanism:** Static: runs `./gradlew :shared:lspBundleStage`, uses `file <binary>` to check `Mach-O|ELF|PE32|executable` but only emits `WARN` (not `FAIL`) for unrecognised types (lines 107–115). Runtime (lines 143–181): for each staged binary, runs `chmod +x && <binary> --version` and checks the exit code is NOT in `{126, 127, 139}`. Exit codes 0 or 1 from the binary are both "acceptable" (line 177).
- **Runtime evidence:** `file` check verifies binary type (with WARN not FAIL for unknowns). `--version` exec verifies binary can be invoked. However accepting exit code 1 from `--version` is insufficient: most real LSP servers exit 1 for unrecognised arguments the same way a non-functional binary would. A wrapper shell script (e.g., for `lua-language-server`) can pass the `file` WARN and the `--version` exit-code check without being a real bundled LSP server.
- **Recommended fix:** (1) Treat `file` unrecognised type as FAIL, not WARN. (2) Capture stdout of `--version` and verify it contains a non-empty version string (semver pattern or `<lang>-language-server` text). This distinguishes a real binary from a stub.

### lsp_diagnostics_render_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static: checks 3 render composable files, `DiagnosticsPalette.kt`, and `DiagnosticsCache` reference in both platform host files. Desktop runtime: `./gradlew :shared:desktopTest --tests "*.lsp.Diagnostic*" --tests "*.lsp.DiagnosticsCache*"`, `passed >= 8`, `failed == 0`. Android runtime: `./gradlew :androidApp:testDebugUnitTest -PincludeRobolectric=true --tests "*Diagnostics*"`, `passed >= 3`, `failed == 0`. Both log paths emitted.
- **Runtime evidence:** Real Gradle test executions on two runtimes; counts and log paths emitted.
- **Recommended fix:** none

### lsp_hosting_completeness_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static: 15 `server.json` resource files + 6 foundation source files + `LspCompletionProvider(` reference in `CompletionEngine.kt`. Runtime: `./gradlew :shared:desktopTest --tests "digital.vasic.yole.lsp.*"`, `passed >= 25`, `failed == 0`. Log emitted.
- **Runtime evidence:** Real Gradle test run; 25+ PASSED required; log path emitted.
- **Recommended fix:** none

### lsp_hover_definition_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static: 7 foundation files, 3 Android UI files, `fun hover` and `fun definition` in `LspServerHost.kt`. Runtime: `./gradlew :shared:desktopTest` with 5 test filters (Hover*, Definition*, EditorNavigationStack*, GoToDefinition*, LspRangeMapping*), `passed >= 10`, `failed == 0`. Log emitted.
- **Runtime evidence:** Real Gradle test run; 10+ PASSED required; log path emitted.
- **Recommended fix:** none

### lsp_refactoring_capabilities_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static: 14 files (10 core + 4 Android UI). Desktop runtime: `./gradlew :shared:desktopTest --tests "digital.vasic.yole.lsp.*"`, `passed >= 50`, `failed == 0`. Robolectric runtime: 5 test filters (`*Rename*RobolectricTest*` etc.), `passed >= 14`, `failed == 0`. Both log paths emitted.
- **Runtime evidence:** Two real Gradle test runs with separate PASSED thresholds; log paths emitted for both.
- **Recommended fix:** none

### lsp_workspace_edit_applier_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static: `WorkspaceEditApplier.kt` exists AND `class ApplyConflict` declared in it (line 77). Runtime: `./gradlew :shared:desktopTest --tests "*WorkspaceEdit*" --tests "*TextEdit*"`, `passed >= 8`, `failed == 0`. Log emitted.
- **Runtime evidence:** Real Gradle test run; 8+ PASSED required; log path emitted. Mutation guards documented at lines 28–35.
- **Recommended fix:** none

### mutation_ratchet_challenge.sh
- **Verdict:** BLUFF
- **PASS mechanism:** Lines 25–30: `grep -q '^# === SECTION 1'` AND `'^# === SECTION 2'` AND `'^# === SECTION 3'` in the baseline file. If all 3 markers are present, exits 0 with `"OK: mutation ratchet stub (Section 2 deferred to sub-project 4)."`. No Pitest is invoked; no mutations are killed; no mutation score is checked.
- **Runtime evidence:** NONE. The script explicitly says "Until sub-project 4 wires up Pitest" (line 21). It never executes Pitest, never parses `mutations.xml`, never checks any mutation score. A mutation ratchet challenge that does not run mutations is definitional bluff under CONST-039.
- **Recommended fix:** Replace the stub with a real Pitest invocation: run `./gradlew :shared:pitest`, parse `build/reports/pitest/<run>/mutations.xml`, compute the killed-mutation ratio, and FAIL if below the threshold stored in Section 2 of the baseline. Until then, remove this challenge from `qa-all` or make it explicitly emit `DEFERRED — NOT A GATE`.

### no_suspend_calls_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Locates `scripts/host-power-management/check-no-suspend-calls.sh` by walking up the directory tree (lines 21–30), then executes it via `bash "$SCANNER" "$PROJECT_ROOT"` (line 44). Exit code propagated directly.
- **Runtime evidence:** Delegates to the real scanner which scans the source tree for forbidden power-management syscalls.
- **Recommended fix:** none

### scroll_sync_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Layer (a) (lines 37–57): strips comments from `SyncedScrollEditor.kt`, counts `rememberScrollState()` calls (must equal 1), extracts `verticalScroll(var)` arguments and verifies they are all the same variable (must have only 1 distinct value via `sort -u | wc -l`). Layer (b) (lines 61–78): runs `./gradlew :androidApp:testDebugUnitTest --tests "*.EditorScrollSyncRobolectricTest" -PincludeRobolectric=true` and verifies `grep -q "EditorScrollSyncRobolectricTest .* PASSED"` in the log.
- **Runtime evidence:** Real Kotlin source structural analysis (not pure grep — extracts variable names and checks uniqueness); real Robolectric test execution; PASSED evidence verified in log.
- **Recommended fix:** none

### snippet_library_bundle_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static (lines 61–122): counts `snippets.json` files (`>= 50`), validates each as JSON via `python3 -m json.tool` or `jq`. Runtime (lines 130–165): runs `./gradlew :shared:desktopTest --tests "*SnippetBundleCompletenessTest*" --tests "*SnippetRegistryTest*" --tests "*VsCodeSnippetParserTest*"`, `passed >= 10`, `failed == 0`. Log path emitted.
- **Runtime evidence:** Real JSON parsing of each bundle file (not just extension check); real Gradle test run; 10+ PASSED required; log path emitted.
- **Recommended fix:** none

### syntax_highlighting_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Two separate Gradle invocations: (1) `./gradlew :shared:desktopTest` with 13 test filters (lines 25–38), requiring `shared_passed > 0`; (2) `./gradlew :androidApp:testDebugUnitTest -PincludeRobolectric=true` with 7 test filters (lines 48–57), requiring `android_passed > 0`. Both counts emitted. Either zero causes FAIL (line 64).
- **Runtime evidence:** Two real Gradle test runs; PASSED counts from both runtimes emitted; log paths captured.
- **Recommended fix:** none

### syntax_highlighting_per_platform_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** For each of 4 platforms (`android`, `desktop`, `ios`, `wasmJs`), checks the corresponding `actual` Kotlin file exists (line 32) AND that it contains `actual class TokenizerEngine` (line 38). PASS only if all 4 platforms have actual files with the declaration.
- **Runtime evidence:** Reads real platform-specific source files from disk; verifies the `actual` keyword is present in each — confirms KMP expect/actual wiring for all 4 platforms.
- **Recommended fix:** none

### theme_unification_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Layer (a) (lines 35–51): greps all 9 production source paths for `YoleColors.Ide.` or `YoleColors.Dark.` patterns, excluding `LegacyThemeBridge.kt` and comment lines; requires `hits == 0`. Layer (b) (lines 54–71): runs `./gradlew :shared:desktopTest --tests "*LegacyThemeParityTest*" --tests "*ThemeWcagContrastTest*"`, requires `passed > 0`. Log emitted.
- **Runtime evidence:** Real source tree scan for legacy references; real Gradle test run; PASSED count emitted.
- **Recommended fix:** none

### tokenizer_android_real_tokens_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Checks for real ADB device (lines 32–36); if none, exits 2 (`SKIP-OK`). When device is present, runs `./gradlew :androidApp:connectedDebugAndroidTest` scoped to `TokenizerEngineAndroidTest` (lines 41–48), then verifies `failed == 0` AND `passed >= 3` (lines 55–63) with the 3 expected method names cited by name.
- **Runtime evidence:** Real on-device instrumented test execution; PASSED lines verified in log; specific test method names cited as the minimum expected set.
- **Recommended fix:** none

### web_full_ui_suite_challenge.sh
- **Verdict:** BLUFF
- **PASS mechanism:** Static: checks `tools/node-render-gate/full-ui-suite.js` exists (line 53). Runtime: if `node` absent (line 64) exits 3; if `puppeteer` not installed (line 69) exits 3. Both paths are treated as `SKIP-OK` (per exit-code comment and line 80 message). When node+puppeteer present: `node "$GATE_SCRIPT" "$TARGET_URL"` (line 78) — Puppeteer walks a11y tree, checks 19 UI elements, asserts no CSS leaks.
- **Runtime evidence:** When node+puppeteer present: Puppeteer opens live URL, walks a11y tree, captures `a11y-tree.json` + screenshot — genuine CONST-039 evidence. When node or puppeteer absent: NONE. Exit 3 is treated as SKIP-OK by the challenge caller, so on a dev machine without Puppeteer this challenge always "passes" with no evidence.
- **Recommended fix:** Exit 3 must not be accepted as PASS by `qa-all`. Treat missing Puppeteer as FAIL unless an explicit skip-ticket is provided (e.g., `SKIP-OK: #iter-86-puppeteer-not-installed`). The JS gate logic itself is solid — the problem is the unconditional skip path.

### web_interactive_flow_challenge.sh
- **Verdict:** BLUFF
- **PASS mechanism:** Static: checks `tools/node-render-gate/interactive-flow-suite.js` exists. Runtime: if `node` absent (line 51) or `puppeteer` absent (line 55), exits 3 (`SKIP-OK`). When present: `node "$GATE_SCRIPT" "$TARGET_URL"` (line 63) — Puppeteer clicks each toolbar button and verifies UI state changes.
- **Runtime evidence:** When node+puppeteer present: Puppeteer performs real functional clicks and verifies state. When absent: NONE — exits 3 with no evidence that any button works.
- **Recommended fix:** Same as `web_full_ui_suite_challenge.sh` — exit 3 must not be treated as PASS by `qa-all`. The forensic anchor for this challenge (dead buttons) means every PASS-without-clicking is definitional bluff.

### web_logo_presence_challenge.sh
- **Verdict:** SUSPECT
- **PASS mechanism:** Static layer (lines 45–99): checks `favicon.ico` and `manifest.json` source files exist; extracts required icon sizes from manifest via Python3; checks each icon file exists in `resources/`. Runtime layer (lines 103–174): if `curl` absent, exits 3 (`SKIP-OK`). When `curl` present: HTTP GETs each icon URL and verifies `Content-Type: image/` (not just HTTP 200, blocking the Firebase rewrite-as-200 trick documented at lines 115–118). Also checks `<title>` contains "Yole". The in-app YOLE logo a11y check (lines 163–172) runs only if `node`+`puppeteer` present — otherwise `SKIP-OK`.
- **Runtime evidence:** When `curl` present: real HTTP requests with content-type verification against deployed site — strong evidence. When `curl` absent: static filesystem checks only. The Puppeteer in-app logo check is double-gated and effectively never runs.
- **Recommended fix:** `curl` absent should be FAIL (or explicit SKIP-OK with ticket), not silent green. The content-type runtime check is genuinely strong; the problem is the skip path. The in-app logo a11y layer should be a harder requirement given it explicitly guards against the iter-71 class of regression.

### web_pwa_icon_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Layer A (lines 60–99): manifest schema checks — `"icons"` array present, `192x192` and `512x512` PNG entries via grep. Layer B (lines 102–152): for each PNG icon src extracted from manifest, checks file exists, reads first 4 bytes via `xxd` or `od` and verifies PNG magic `89504e47`, verifies size `>= 500` bytes. Layer C (lines 155–189): when a Web Wasm bundle zip is present, `unzip -l` checks each icon is `>= 500` bytes inside the bundle; explicit skip with tracker comment if no bundle.
- **Runtime evidence:** `xxd`/`od` reads real file bytes; PNG magic verified byte-by-byte; file sizes checked; bundle zip inspection when available.
- **Recommended fix:** none

### web_responsive_suite_challenge.sh
- **Verdict:** BLUFF
- **PASS mechanism:** Static: checks `tools/node-render-gate/responsive-suite.js` exists (line 37). Runtime: if `node` absent (line 45) or `puppeteer` absent (line 49), exits 3 (`SKIP-OK`). When present: `node "$GATE_SCRIPT" "$TARGET_URL"` (line 55) — Puppeteer tests 6 viewport sizes (320–1280 px) for critical UI elements and responsive-layout breakpoint contract.
- **Runtime evidence:** When node+puppeteer present: Puppeteer runs at 6 real viewport sizes. When absent: NONE — exits 3. Given the forensic anchor (iter-90: operator discovered mobile layout was broken while all gates PASSed because they all ran at 1280×800), the skip path is a critical gap.
- **Recommended fix:** Exit 3 must not be accepted as PASS by `qa-all`. This challenge exists precisely because the prior gates' 1280px-only coverage missed the mobile regression — allowing this gate to skip on a machine without Puppeteer perpetuates exactly the same coverage gap.

### web_sw_cache_version_challenge.sh
- **Verdict:** CLEAN
- **PASS mechanism:** Static layer (lines 48–85): reads `versionName` from `androidApp/build.gradle.kts`; reads `CACHE_VERSION` from `service-worker.js` via `grep -E "^const CACHE_VERSION = "`; asserts they are equal (line 74). Runtime layer (lines 88–136): if `curl` absent, exits 3. When present: fetches `$TARGET_URL/service-worker.js`, extracts `CACHE_VERSION`, asserts matches canonical. Also checks `networkFirst` strategy (line 129).
- **Runtime evidence:** Static: reads real source files and compares version strings — genuinely detects the iter-89 bug class (SW version never bumped). Runtime: fetches real deployed SW JS and compares version strings.
- **Recommended fix:** none

### webapp_render_validation_challenge.sh
- **Verdict:** BLUFF
- **PASS mechanism:** Static layer (lines 50–87): checks `index.html` and `Main.kt` exist; extracts `viewportContainerId` from `Main.kt` via `grep -oE 'viewportContainerId\s*=\s*"[^"]+"'`; verifies `index.html` has matching `<div id="...">` and `<script src="yole-web.js">`. Runtime: if `node` absent (line 94) exits 3; if `render-gate.js` missing (line 99) exits 3; if `puppeteer` absent (line 104) exits 3. When present: `node "$RENDER_GATE" "$TARGET_URL"` (line 113) — Puppeteer loads live URL, verifies canvas dimensions, pixel variance, captures screenshot.
- **Runtime evidence:** Static layer: source-file cross-checks — valuable but cannot prove the page renders. Runtime: when node+puppeteer present — Puppeteer performs full render verification with screenshot as CONST-039 evidence. When absent: NONE — exits 3. Given this is the foundational web render gate (forensic anchor: iter-83 shipped splash-then-blank across v2.0.0), a SKIP-OK exit without rendering is definitional bluff for this challenge's purpose.
- **Recommended fix:** Treat missing Puppeteer as FAIL unless explicit skip-ticket. The static layer is genuinely useful — split it into its own clean challenge. The render gate proper must not be allowed to skip silently.
