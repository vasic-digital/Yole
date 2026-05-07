# Yole — Continuation Document

> **MANDATORY:** This document MUST be maintained and kept in sync during any
> work, per CONST-036 in CONSTITUTION.md. If work stops for any reason, this
> document MUST enable any CLI agent or LLM model to continue exactly where
> work left off.

**Last updated:** 2026-05-07 15:15 UTC  
**Current branch:** `master` (synced with origin)  
**Working tree:** CLEAN (all committed + pushed)

---

## 1. How to Resume Work

From a new CLI agent session, paste this prompt:

```
I am resuming work on the Yole project. Read docs/CONTINUATION.md to understand
the current state, then continue with the active task described in Section 2
(Active Work Stream). For the SAF save fix plan, the full implementation plan
is in docs/superpowers/plans/2026-05-05-saf-save-fix-plan.md and the design
spec is in docs/superpowers/specs/2026-05-05-saf-save-fix-design.md.

IMPORTANT: After completing each task, update docs/CONTINUATION.md as required
by CONST-036. Also maintain the CONSTITUTION.md, AGENTS.md, and CLAUDE.md
continuation constraint.

Start by running:
  git submodule update --init --recursive
  make bootstrap
Then verify the current state matches the CONTINUATION.md task list.
Continue implementing the next incomplete task.
```

---

## 2. Active Work Stream: Visual Refinement (Phase 1)

**Priority:** Medium — brand consistency and modern UI polish

**Feature Stream:** #2 of 5 (after SAF Save Fix)

### Task Progress (Visual Refinement)

| # | Task | Status | Description |
|---|------|--------|-------------|
| 1 | Unify accent colors (web was blue, now red #D32F2F) | DONE | 5 files updated |
| 2 | Modernize web loading screen | DONE | Dark gradient + Yole logo + spinner |
| 3 | Update PWA manifest + service worker theme | DONE | #1976d2 → #D32F2F |
| 4 | Phase 2: IDE layout polish | DONE | TabBar, Sidebar, MenuBar, StatusBar enhanced |
| 5 | Phase 3: Shared theme tokens audit | DONE | Eliminated 30+ duplicate hardcoded colors, added YoleColors.Ide |
| 6 | Phase 4: Eliminate remaining hardcoded colors | DONE | Android + desktop Theme.kt + EnhancedYoleApp cleaned |

### Files Modified

- `webApp/src/wasmJsMain/kotlin/digital/vasic/yole/web/EnhancedWebApp.kt` — accent/statusBar from #007ACC → #D32F2F
- `webApp/src/wasmJsMain/kotlin/digital/vasic/yole/web/Main.kt` — Load button from #1976d2 → #D32F2F
- `webApp/src/wasmJsMain/resources/index.html` — theme-color, loading screen redesign
- `webApp/src/wasmJsMain/resources/manifest.json` — theme_color → #D32F2F
- `webApp/src/wasmJsMain/resources/service-worker.js` — offline page heading → #D32F2F

### Resume Prompt for Visual Refinement

```
Continue with Yole visual refinement. Read docs/CONTINUATION.md Section 2
for the current state. The web accent colors are unified. Next: IDE layout
polish (tab bar, sidebar, status bar) and shared theme token audit to
eliminate hardcoded colors in favor of YoleColors from shared/ui/Theme.kt.

Start by reading:
- webApp/.../EnhancedWebApp.kt (IdeColors object + composables)
- shared/.../ui/Theme.kt (YoleColors tokens)
- desktopApp/.../theme/Theme.kt (desktop theme reference)
```

---

## 2b. Completed Work Stream: SAF-First File Save Fix

**Feature Stream:** #1 of 5  
**Full plan:** `docs/superpowers/plans/2026-05-05-saf-save-fix-plan.md`  
**Full spec:** `docs/superpowers/specs/2026-05-05-saf-save-fix-design.md`

### Task Progress (13 tasks total)

| # | Task | Status | Commit |
|---|------|--------|--------|
| 1 | Create FileHandle expect declaration | DONE | `1c8ba718` |
| 2 | Create Desktop actual implementation | DONE | `ec80e7a2` |
| 3 | Create Android actual implementation | DONE | `da8e733e` |
| 4 | Create stubs for iOS and WASM | DONE | `099d713a` |
| 5 | Update EditorTab to use contentUri | DONE | `41c4bb74` |
| 6 | Rewrite saveFile to use FileHandle | DONE | `41c4bb74` |
| 7 | Write shared unit tests for FileHandle | DONE | `34829478` |
| 8 | Write desktop-specific FileHandle tests | DONE | `5396cfe4` |
| 9 | Create AVD config files for Android 9-16 | DONE | `e377dea` (Containers) |
| 10 | Create Android emulator Docker container | DONE | `e377dea` (Containers) |
| 11 | Create Go challenge test for save verification | DONE | `4c8fc60` (Challenges) |
| 12 | Write Android instrumentation save tests | DONE | `a845ef65` |
| 13 | Run full test suite and push | PENDING PUSH | `a845ef65` |

### Task 3 DETAIL (Android actual — DONE)

**Committed:** `da8e733e`
- `shared/src/androidMain/kotlin/digital/vasic/yole/util/FileStorage.android.kt` — actual SAF implementation with ContentResolver
- `androidApp/src/main/java/digital/vasic/yole/android/MainActivity.kt` — AppContextHolder initialization

### Task 4 DETAIL (iOS/WASM stubs — DONE)

**Committed:** `099d713a`
- `shared/src/iosMain/kotlin/digital/vasic/yole/util/FileStorage.ios.kt` — stub (all operations return null/false)
- `shared/src/wasmJsMain/kotlin/digital/vasic/yole/util/FileStorage.wasmJs.kt` — stub (all operations return null/false)

### Tasks 9-13 DETAIL

**Tasks 9-10 (AVD configs + Docker, committed Containers e377dea):**
- `Containers/images/android-test/avds/config_api28.ini` through `config_api36.ini` — 8 AVD configs
- `Containers/images/android-test/Dockerfile` — emulator container with all system images
- `Containers/images/android-test/entrypoint.sh` — loops API levels, runs SaveTests, collects SAVE_VERIFIED evidence
- `Containers/docs/behavior-anchors.md` — fixed 3 stale anchor symbols (CAP-008, CAP-017, CAP-019)

**Task 11 (Go challenge, committed Challenges 4c8fc60):**
- `Challenges/pkg/challenge/android_save_test.go` — Go test with adb guard, emulator-only execution, anti-bluff skip on missing env
- `Challenges/challenges/scripts/android_save_challenge.sh` — shell wrapper with SAVE_VERIFIED evidence extraction
- `Challenges/CONSTITUTION.md` — added CONST-035 verbatim user mandate quote (was missing)

**Task 12 (Android instrumentation tests, committed a845ef65):**
- `androidApp/src/androidTest/kotlin/digital/vasic/yole/android/SaveTests.kt` — 5 instrumented tests
  - saveToCacheAndReadBack: writes file, reads via FileHandle, asserts content
  - writeAndExists: writes via FileHandle.writeBytes(), asserts exists()
  - readNonExistentReturnsNull: asserts null for missing file
  - writeEmptyContent: asserts 0-byte write succeeds
  - writeAndReadRoundtrip: roundtrip with special characters
  - All emit `SAVE_VERIFIED: N bytes` for anti-bluff evidence
  - `@Before` initializes `AppContextHolder.context` for SAF resolution

**Task 13 (Verification):**
- `./gradlew :shared:desktopTest` — PASS
- `./gradlew :desktopApp:compileKotlin :webApp:compileKotlinWasmJs` — PASS
- `go test ./... -count=1 -short` (Challenges) — ALL PASS (TestAndroidSave skips properly)
- `bash scripts/anti-bluff/bluff-scanner.sh --mode all` — PASS (scanner clean)
- Anchor manifest challenges — PASS (all repos)
- PUSH pending

---

## 3. Uncommitted Files in Working Tree

| File | Change |
|------|--------|
| `webApp/.../EnhancedWebApp.kt` | Accent #007ACC → #D32F2F |
| `webApp/.../Main.kt` | Load button #1976d2 → #D32F2F |
| `webApp/.../index.html` | Loading screen redesign + theme-color |
| `webApp/.../manifest.json` | theme_color → #D32F2F |
| `webApp/.../service-worker.js` | Offline heading → #D32F2F |

(Waiting to be staged and committed)

---

## 4. Known Defects (Not Yet Fixed)

From `docs/KNOWN_DEFECTS.md`:

### ~~#smb-stub-no-negotiation~~ — FIXED (2026-05-07, commit `1f6472c9`)
- **Fixed:** SmbService.connect() now performs real protocol negotiation and authentication via SmbProtocolClient (or injected test lambdas). `_isConnected` is set only after real success — `isOnline` honestly reflects reachability. Tests updated with lambda injection pattern.

### ~~#webdav-always-online-stub~~ — FIXED (2026-05-07, commit `1f6472c9`)
- **Fixed:** WebDavService.connect() no longer catches network errors and lies about online state. Removed the `catch` block that suppressed exceptions. Added `testConnectFn` lambda injection for test control. Tests updated to handle the real connection behavior.

### Current Open Defects:

### #robolectric-compose-ui-tests-brittle
- **Symptom:** ~25 Robolectric UI tests flap on string-based matching
- **Discovered:** Container-release build (iter 26)
- **Proper fix:** Migrate to HelixQA on-device tests or test-tag-based matching
- **Blocked by:** Multi-day work; out of scope for any single iteration
- **Exemption:** Robolectric tests excluded in androidApp/build.gradle.kts

---

## 5. Anti-Bluff Campaign (CONST-035) — Remaining Work

From `docs/campaigns/anti-bluff/MILESTONE-2026-05-01.md`:

### What's Done
- CONST-035 in all 4 repos' governance docs (12 total)
- Verbatim user mandate quote now in ALL governance docs (fixed Challenges/CONSTITUTION.md gap)
- Scanner enforcing CONST-035 via `make qa-all`
- 0 pre-existing bluff hits (was 24)
- 123 anchor manifest rows across 4 repos
- 13 self-test fixtures covering all 8 BLUFF patterns
- `make bootstrap` for fresh-clone setup
- Containers anchor manifest fixed (3 stale test symbols: CAP-008, CAP-017, CAP-019)

### What's NOT Yet Enforced
1. **AST-aware scanner patterns** — BLUFF-K-001, K-005, K-007, G-002, G-004 need real Kotlin/Go parser
2. **Pitest mutation gate for Yole main** — `:shared:jvm` + 10 KMP modules deferred
3. **Definition-of-Done linkage** — PR-body-evidence-block enforcement (sub-project 6)
4. **Recursive submodule bootstrap verification** — `make bootstrap` doesn't verify nested submodule state

### Resume Protocol (from MILESTONE)
1. Read `docs/campaigns/anti-bluff/CAMPAIGN.md` — full iter log
2. Read `docs/campaigns/anti-bluff/MILESTONE-2026-05-01.md` — high-level state
3. Pick next leverage point from the "NOT yet enforced" list

---

## 6. Repo State

### Main Repo
```
Branch: master (synced with origin)
Last 5 commits:
  3f6c1ae5 refactor(theme): consolidate IDE colors into shared YoleColors tokens
  bf58dbb4 style(web): modernize IDE layout with polished TabBar, Sidebar, MenuBar, and StatusBar
  4c96e8e5 style(web): unify brand accent color and modernize loading screen
  82e3a3a7 docs(continuation): finalize visual refinement Phase 1-2 status
  ee6abb3a docs: update CONTINUATION.md and CLAUDE.md for fixed SMB/WebDAV stubs
```

### Submodules
```
Challenges:  727353b (main) — CONST-035 labels added to governance docs
Containers:  e377dea (main) — AVD configs + Dockerfile
HelixQA:     71560fd (main) — merged upstream governance updates
```
† helixgithub/helixgitlab for HelixQA have SSH-auth errors (known issue)

---

## 7. Phase / Feature Streams Overview

| Stream | Priority | Status | Description |
|--------|----------|--------|-------------|
| SAF Save Fix | Critical | COMPLETE (13/13 tasks) | Fix file saving on Android 9-16 |
| Visual Refinement | Medium | COMPLETE (Phases 1-3 done) | Unified brand colors, modernized IDE layout, shared tokens |
| (TBD) | — | Not started | Additional feature streams 3-5 |
| Known Defects | High | Deferred | #robolectric |
| Anti-Bluff | Medium | Ongoing | 4 remaining enforcement dimensions |

---

## 8. Quick Verification Commands

Before claiming any task is complete, run:

```bash
# Compilation sanity
./gradlew :shared:compileKotlinDesktop :shared:compileKotlinAndroid --no-daemon

# Primary test suite (no Android SDK)
./gradlew :shared:desktopTest --no-daemon
make test-shared

# Anti-bluff gates
bash scripts/anti-bluff/bluff-scanner.sh --mode all
bash challenges/scripts/anchor_manifest_challenge.sh
bash challenges/scripts/mutation_ratchet_challenge.sh

# Full QA
make qa-all
```

---

## 9. After Each Task — Update This Document

Per CONST-036 (continuation constraint in CONSTITUTION.md), after completing ANY task:

1. Mark the task as DONE in Section 2
2. Update "Last updated" timestamp at top
3. If the working tree state changes, update Section 3
4. If new commits are created, update Section 6
5. Refresh the "How to Resume" prompt in Section 1 if needed

---

<!-- END OF CONTINUATION DOCUMENT -->
