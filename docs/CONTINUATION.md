# Yole — Continuation Document

> **MANDATORY:** This document MUST be maintained and kept in sync during any
> work, per CONST-036 in CONSTITUTION.md. If work stops for any reason, this
> document MUST enable any CLI agent or LLM model to continue exactly where
> work left off.

**Last updated:** 2026-05-06  
**Current branch:** `master` (up to date with remotes)  
**Working tree:** CLEAN (committed)

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

## 2. Active Work Stream: SAF-First File Save Fix

**Priority:** Critical — blocks basic app usage on Android 16 (API 36)

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
| 9 | Create AVD config files for Android 9-16 | NOT STARTED | — |
| 10 | Create Android emulator Docker container | NOT STARTED | — |
| 11 | Create Go challenge test for save verification | NOT STARTED | — |
| 12 | Write Android instrumentation save tests | NOT STARTED | — |
| 13 | Run full test suite and push | NOT STARTED | — |

### Task 3 DETAIL (Android actual — DONE)

**Committed:** `da8e733e`
- `shared/src/androidMain/kotlin/digital/vasic/yole/util/FileStorage.android.kt` — actual SAF implementation with ContentResolver
- `androidApp/src/main/java/digital/vasic/yole/android/MainActivity.kt` — AppContextHolder initialization

### Task 4 DETAIL (iOS/WASM stubs — DONE)

**Committed:** `099d713a`
- `shared/src/iosMain/kotlin/digital/vasic/yole/util/FileStorage.ios.kt` — stub (all operations return null/false)
- `shared/src/wasmJsMain/kotlin/digital/vasic/yole/util/FileStorage.wasmJs.kt` — stub (all operations return null/false)

### Tasks 9-13 DETAIL

See `docs/superpowers/plans/2026-05-05-saf-save-fix-plan.md` for complete step-by-step instructions, including exact code to write, files to modify, and commands to run.

**Key files to modify for Tasks 5-6:**
- `androidApp/src/main/java/digital/vasic/yole/android/ui/YoleApp.kt` — EditorTab data class (line ~247) and saveFile function (line ~168-188)

**Key directories for Task 9-10:**
- `Containers/images/android-test/avds/` — 8 AVD configs (config_api28.ini through config_api36.ini)
- `Containers/images/android-test/Dockerfile` — emulator Docker container

**Key directories for Task 11:**
- `Challenges/challenges/android_save_challenge.go`
- `Challenges/scripts/android_save_challenge.sh`

**Key directories for Task 12:**
- `androidApp/src/androidTest/kotlin/digital/vasic/yole/android/SaveTests.kt`

---

## 3. Uncommitted Files in Working Tree

(None — working tree is clean)

---

## 4. Known Defects (Not Yet Fixed)

From `docs/KNOWN_DEFECTS.md`:

### #smb-stub-no-negotiation
- **Symptom:** SMB connect succeeds without real negotiation
- **Discovered:** `ErrorRecoveryE2ETests.kt`
- **Proper fix:** Wire real SMB connect/authenticate, injectable `SmbProtocolClient`, test fake
- **Blocked by:** ~12 tests assert fake-host connect succeeds; need refactored constructor + FakeSMBProtocolClient
- **Exemption:** `SKIP-OK: #smb-stub-no-negotiation` in ErrorRecoveryE2ETests.kt

### #webdav-always-online-stub
- **Symptom:** WebDAV connect sets isOnline=true even when unreachable
- **Discovered:** `ErrorRecoveryE2ETests.kt`
- **Proper fix:** Remove catch block, let exception propagate, separate `isOfflineCapable` flag
- **Blocked by:** ~10 tests assert fake-host connect succeeds; need injectable HttpClient + MockEngine
- **Exemption:** `SKIP-OK: #webdav-always-online-stub` in ErrorRecoveryE2ETests.kt

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
- Scanner enforcing CONST-035 via `make qa-all`
- 0 pre-existing bluff hits (was 24)
- 123 anchor manifest rows across 4 repos
- 13 self-test fixtures covering all 8 BLUFF patterns
- `make bootstrap` for fresh-clone setup

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
Branch: master (up to date with remotes)
Remotes: github (origin), upstream (all same git@github.com:vasic-digital/Yole.git)
Last 5 commits:
  5396cfe4 test(file): add Desktop-specific FileHandle roundtrip tests
  34829478 test(file): add FileHandle contract tests
  41c4bb74 refactor(save): update EditorTab with contentUri and migrate saveFile to FileHandle
  099d713a feat(file): add iOS/WASM stubs for FileHandle
  da8e733e feat(file): add Android actual for FileHandle using ContentResolver/SAF
```

### Submodules
```
Challenges:  f2ea964 (main) — remotes: github, gitlab, origin, upstream
Containers:  84c381c (main) — remotes: github, gitlab, origin, upstream
HelixQA:     1efd359 (main) — remotes: github, gitlab, origin, upstream
```
† helixgithub/helixgitlab for HelixQA have SSH-auth errors (known issue)

---

## 7. Phase / Feature Streams Overview

| Stream | Priority | Status | Description |
|--------|----------|--------|-------------|
| SAF Save Fix | Critical | In progress (3/13 tasks) | Fix file saving on Android 9-16 |
| (TBD) | — | Not started | Additional feature streams 2-5 |
| Known Defects | High | Deferred | #smb, #webdav, #robolectric |
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
