# Yole — Continuation Document

> **MANDATORY (CONST-036):** This document MUST be maintained and kept in sync
> during any work. If work stops for any reason, this document MUST enable any
> CLI agent or LLM model to continue exactly where work left off. A stale or
> inaccurate Continuation document is a CONST-036 violation and MUST be
> corrected before proceeding with any other work.

**Last updated:** 2026-05-12 (iter 29 — macOS environment remediation: case-collision fix + sibling KMP clone + bash 5 + HelixQA reset + macOS pmset branch + BSD-portability trim)
**Current branch:** `master`
**HEAD (parent of this commit):** `62b93272` — `refactor: rename challenges/ → yole-challenges/ (macOS case-collision fix)` (one earlier commit; this commit lands the script-portability batch on top).
**Submodule SHAs (per HEAD tree):** Challenges `0da3d92` (iter 30 bumped from `19e1c33d`), Containers `af51968` (iter 30 bumped from `7813c986`), HelixQA `f0399a82` (already at latest). All initialized; HelixQA nested submodules reset to pinned SHAs.
**Test status:** `:shared:desktopTest` 8,954/8,954 PASS on macOS audit host (iter 29 verified, 2026-05-12, 8m25s); Linux dev host last green at same count. Robolectric (dedicated container) 49/49 PASS on Linux — not reverified on macOS yet.
**Release artifacts:** v0.0.0.0.7 present in `releases/` for Android Debug+Release, Desktop linux-x64, Web Wasm
**Anti-bluff gates (macOS iter 29 reverified under bash 5):** `bluff-scanner.sh --mode all` PASS, `anchor_manifest_challenge.sh` PASS, `mutation_ratchet_challenge.sh` PASS (stub), `no_suspend_calls_challenge.sh` PASS, `host_no_auto_suspend_challenge.sh` PASS (2/2 macOS pmset assertions).

---

## 1. How to Resume Work — Paste Prompt

From a new CLI agent session (any model, any agent), paste this prompt verbatim:

```
I am resuming work on the Yole project (/run/media/milosvasic/DATA4TB/Projects/Yole).

CRITICAL: Read docs/CONTINUATION.md FIRST. It is the single source of truth
for current state, in-flight work, known defects, and remaining phases.
Per CONST-036 in CONSTITUTION.md, this document is mandatorily maintained
and reflects exact current state.

After reading, verify ground truth by running (in order):

  git submodule update --init --recursive
  git log --oneline -3
  git status -s
  cat docs/CONTINUATION.md | head -20

Then check what Section 7 marks as NEXT and Section 4 marks as OPEN.
Pick the highest-priority item that is unblocked and start there.

IMPORTANT: After completing ANY task, ANY commit, ANY defect discovery, or
ANY file creation, you MUST update docs/CONTINUATION.md in the SAME commit
per CONST-036. The document is a living single-source-of-truth, not a
historical log.
```

---

## 2. Current State (Iter 28 — 2026-05-12)

### What Was Just Done

- **Iter 27** (commit `5446d5d4`): All 49 Robolectric tests now pass in the
  dedicated `robolectric-test` container per the user mandate to keep
  Robolectric isolated. Fixed via:
  - `docker-compose.yml`: pass `-PincludeRobolectric=true` so the include-property
    override actually fires (the default test filter was suppressing the tests
    even when `--tests '*.robolectric.*'` selected them).
  - `androidApp/.../ui/YoleApp.kt`: renamed bottom-nav label `"Edit"` → `"QuickNote"`
    so user-visible label matches the screen name everywhere else.
  - `FileEditingRobolectricTest` / `FormatDetectionRobolectricTest`: route through
    the New Document dialog (Add → Create), target editor via semantic
    `contentDescription = "Code editor for untitled.md"` instead of the
    template-hidden `"Start typing..."` placeholder, use `"Edit"` icon
    contentDescription for the preview-back toggle.
  - `ThemeRobolectricTest`: align with current strings (`"Dark theme (IDE)"`,
    `"System theme"`).

- **Iter 28** (commit `492ef100`): Deep-recursive submodule fetch + pull + cross-fork merge.
  - `Challenges` (`a70c5b16`): merged origin/main (governance cascade §§6.R/6.S/6.T/6.U/6.V/6.W + Article XI §11.9), fast-forwarded `Panoptic` to `c22df66` (bluff-scan exemption annotations + 17 upstream commits including CONSTITUTION.md scaffold), then merged the fresh `android-save` 100%-decoupled gate fix. Pushed to all 4 remotes.
  - `Containers` (`53f48c60`): fast-forwarded 4 commits (HelixCode infra config + §§6.U/6.V/6.W cascade + boot binary refresh). Pushed; gitlab caught up.
  - `HelixQA` (`f0399a82`): cross-fork merge with `helixgithub/main` (HelixDevelopment fork — 6 ahead) into vasic-digital fork (2 ahead). Clean `ort` strategy, no conflicts. Pulled in `cmd/helixqa-bridge` (~1.7k LoC + tests), `cmd/recording-analyzer` (~1.5k LoC + tests), `banks/atmosphere.yaml`, governance additions, `rest-demo` pointer update. Pushed to all 6 remotes.
  - Superproject commit `492ef100` pushed to github, origin, upstream.

### Working Tree State

```
Clean. All submodule pointers committed. No uncommitted changes.
```

---

## 3. Uncommitted Files in Working Tree

(None as of `492ef100`. Any new work MUST update this section before commit.)

---

## 4. Known Defects

From `docs/KNOWN_DEFECTS.md` (authoritative — keep that file in sync with this section):

### OPEN

#### `#robolectric-compose-ui-tests-brittle` — MITIGATED (dedicated container)
- **Symptom:** ~25 Robolectric UI tests historically matched against runtime-evolving UI strings (now `contentDescription`-based after iter 27).
- **Mitigation (iter 27):** Tests run in dedicated `robolectric-test` container via `make container-robolectric-test` — isolated from main build, won't gate release pipeline. All 49 tests now pass green.
- **Proper fix (still open):** Long-term, migrate to HelixQA on-device automation or `testTag`-based matching. Out of scope for any single iteration.
- **Exemption:** `androidApp/build.gradle.kts` `tasks.withType<Test>().configureEach` excludes `"*.robolectric.*"` from default test task. Search for `SKIP-OK: #robolectric-compose-ui-tests-brittle`.

#### `#helixqa-missing-sibling-repos`
- **Symptom:** 31 HelixQA packages fail with "replacement directory does not exist".
- **Missing repos:** DocProcessor, LLMsVerifier/llm-verifier, LLMOrchestrator, VisionEngine.
- **Status:** Not a code defect — environment bootstrap gap. These repos must be present as siblings to HelixQA for those packages to compile.

### CLOSED (record for forensic continuity — do NOT re-open without reason)

- `#smb-stub-no-negotiation` — FIXED 2026-05-07 (commit `1f6472c9`). `SmbService.connect()` performs real SMB protocol negotiation and authentication; `_isConnected = true` only after real success. Test lambda injection (`testConnectFn`/`testAuthenticateFn`) for test control. 441/441 SMB+WebDAV tests pass.
- `#webdav-always-online-stub` — FIXED 2026-05-07 (commit `1f6472c9`). Removed the catch block that suppressed network errors and lied about online state. `isOnline` honestly reflects reachability per CONST-035.
- `#webdav-stackoverflow` — FIXED 2026-05-07 (commit `15f5d10f`). Replaced recursive XML namespace stripping with iterative approach. WebDavMockHttpTest 28 failures → 0.
- `#pre-existing-concurrency-flakes` — FIXED 2026-05-07 (commit `30022538`). All 37 test failures resolved by injecting test lambdas. 8,954/8,954 PASS.

---

## 5. Anti-Bluff Campaign (CONST-035) — Remaining Work

### What's Done
- CONST-035 in all 4 main repos' governance docs (CONSTITUTION/CLAUDE/AGENTS).
- Verbatim user-mandate quote in all governance docs.
- Scanner enforcing CONST-035 via `make qa-all` (pre-commit + pre-push hooks).
- Bootstrap verification scripts (submodule SHA check + governance audit).
- CONST-036 (Continuation maintenance) in main repo + Challenges, Containers, HelixQA (CONSTITUTION + CLAUDE; pending AGENTS.md in Challenges and Containers — addressed in iter 28).
- 0 pre-existing bluff hits in scanner baseline.
- 123 anchor manifest rows across 4 repos.
- 13 self-test fixtures covering all 8 BLUFF patterns.
- `make bootstrap` for fresh-clone setup + verification.

### What's NOT Yet Enforced (resume here for anti-bluff work)
1. **AST-aware scanner patterns** — BLUFF-K-001, K-005, K-007, G-002, G-004 still grep-only; need real Kotlin/Go parser to eliminate false negatives.
2. **Pitest mutation gate for Yole main** — `:shared:jvm` + 10 KMP modules deferred. Currently only Challenges has mutation ratchet via go-mutesting.
3. **Definition-of-Done PR-body-evidence-block** — sub-project 6 of the anti-bluff campaign — not yet automated.

### Resume Protocol for Anti-Bluff
1. Read `docs/campaigns/anti-bluff/CAMPAIGN.md` — full iter log.
2. Read `docs/campaigns/anti-bluff/MILESTONE-2026-05-01.md` — high-level state.
3. Pick the next leverage point from the "NOT yet enforced" list above.

---

## 6. Repo State (Exact SHAs as of iter 28)

### Main Repo (Yole)
```
Branch:  master  (in sync with github, origin, upstream)
HEAD:    0a58f372  docs(continuation): rewrite to current state + cascade CONST-036 (iter 28)
                   492ef100  chore(submodules): deep-recursive fetch + pull + cross-fork merge
                   d30c0408  feat(firebase): integrate Firebase Analytics, Crashlytics, and Distribution
                   b5e3da41  fix(ftp): wire real file I/O for upload/download via PlatformFileIO
                   20cd132c  docs(network): update KDoc to match actual file I/O implementation
```

### Submodules
```
Challenges/  19e1c33d  chore(governance): append CONST-036 to AGENTS.md
             - 4 remotes: github, gitlab, origin (multi-URL), upstream — ALL pushed
             - nested Panoptic at c22df66 (clean, in sync with origin)

Containers/  7813c986  chore(governance): append CONST-036 to AGENTS.md
             - 4 remotes: github, gitlab, origin, upstream — ALL pushed

HelixQA/     f0399a82  Merge helixgithub/main into vasic-digital fork
             - 6 remotes: github, gitlab, helixgithub, helixgitlab, origin, upstream — ALL pushed
             - 30+ nested third-party submodules in tools/opensource
               * AS-OF iter 29 audit on macOS host: 18 of these nested submodules show
                 working-tree drift (M) vs the SHAs HelixQA's f0399a82 commit pins.
                 Drift not committed inside HelixQA. Touched: allure2, appium,
                 browser-use, chroma, docker-android, docling, kiwi-tcms,
                 llama-index, marker, mem0, midscene, moondream, perfetto, scrcpy,
                 signoz, skyvern, stagehand, unstructured.
               * Decision needed (see §11 Environment Notes): reset to pinned SHAs
                 OR commit the bumps inside HelixQA and bump HelixQA pointer.
```

### Sibling KMP Modules (composite builds)
```
RateLimiter-KMP, Concurrency-KMP, UI-Components-KMP, Auth-KMP, Security-KMP,
Document-KMP, Config-KMP, Database-KMP, Storage-KMP, Formatters-KMP
- All at version 1.0.0 with group=digital.vasic.<name>
- jvmTarget=11 across desktop targets, AGP 8.9.0 unified
- Governance docs (CONSTITUTION/CLAUDE/AGENTS) exist but DO NOT yet carry the anti-bluff covenant —
  see Section 7 phase "Sibling KMP Governance Cascade" for the propagation task.
```

---

## 7. Phases / Feature Streams — Roadmap

| # | Stream | Priority | Status | Resume Point |
|---|--------|----------|--------|-------------|
| 1 | SAF Save Fix | Critical | COMPLETE (13/13) | — |
| 2 | Visual Refinement | Medium | COMPLETE (6/6) | — |
| 3 | Network File Transfer (upload/download honesty) | Critical | COMPLETE (FTP/SFTP/SMB/WebDAV/Git/Dropbox/GDrive/OneDrive wired through PlatformFileIO) | — |
| 4 | Platform Completion (iOS/WASM protocol coverage) | High | NOT STARTED | §7.4 below |
| 5 | Protocol Hardening (real SFTP/SMB/JSON) | Medium | NOT STARTED | §7.5 below |
| 6 | Anti-Bluff Enforcement (3 remaining dimensions) | Medium | Ongoing | §5 above |
| 7 | Sibling KMP Governance Cascade | Low | NOT STARTED | §7.7 below |
| 8 | Robolectric Long-Term Migration | Low | DEFERRED (mitigated by dedicated container) | §4 `#robolectric-compose-ui-tests-brittle` |

### §7.4 Platform Completion (HIGH)

**Problem:** FTP, SFTP, and SMB are completely non-functional on iOS and WASM (all methods throw `PlatformNotSupportedException`). Users on mobile/web cannot use these protocols at all.

**iOS scope:**
1. Implement FTP via NWConnection or libcurl cinterop.
2. Implement SFTP via libssh2 Kotlin/Native cinterop.
3. Implement SMB via libsmb2 Kotlin/Native cinterop.
4. Implement iOS FileHandle for local file I/O.

**WASM scope:**
1. Implement FTP/SFTP/SMB via server-side WebSocket proxy bridges.
2. Implement WASM FileHandle for browser file I/O (IndexedDB/OPFS).
3. Ensure HTTP-based protocols (WebDAV, Git, Dropbox, GDrive, OneDrive) actually function on WASM (they inherit from `commonMain` via Ktor — verify with end-to-end test).

**Resume command:**
```bash
grep -rn "PlatformNotSupportedException" shared/src/iosMain shared/src/wasmJsMain | head -10
```

### §7.5 Protocol Hardening (MEDIUM)

**Problem:** Multiple protocol implementations have simulation gaps.

**Scope:**
1. SFTP service: `commonMain` currently uses in-memory virtual filesystem instead of real SshClient/SftpChannel — wire the real protocol.
2. SMB service: `commonMain` uses in-memory file tree — wire `SmbProtocolClient` (already partially done in iter 26; finish for list/read/write operations beyond connect).
3. JSON format: registered in `FormatRegistry` but has no parser — implement JSON syntax highlighting/formatter.
4. FTP: add server-side operations (if protocol supports) or document limitations.
5. Fix `NetworkProtocolStatus.kt` discrepancies — some protocols claim `FULLY_IMPLEMENTED` but their KDoc says `PARTIALLY_IMPLEMENTED`.

### §7.7 Sibling KMP Governance Cascade (LOW)

**Problem:** 10 sibling KMP repos (RateLimiter-KMP, Concurrency-KMP, UI-Components-KMP, Auth-KMP, Security-KMP, Document-KMP, Config-KMP, Database-KMP, Storage-KMP, Formatters-KMP) have CONSTITUTION/CLAUDE/AGENTS files but do not carry:
- The CONST-035 anti-bluff covenant (verbatim user-mandate quote).
- The CONST-036 Continuation maintenance constraint.

**Scope:**
1. Append CONST-035 + CONST-036 sections to each KMP's CONSTITUTION.md, CLAUDE.md, AGENTS.md.
2. For each KMP, create or update its own `docs/CONTINUATION.md` if it carries independent in-flight work.
3. Commit + push each KMP to its remotes.

**Resume command:**
```bash
for kmp in ../RateLimiter-KMP ../Concurrency-KMP ../UI-Components-KMP ../Auth-KMP ../Security-KMP ../Document-KMP ../Config-KMP ../Database-KMP ../Storage-KMP ../Formatters-KMP; do
  grep -L "MANDATORY ANTI-BLUFF COVENANT" "$kmp"/CONSTITUTION.md "$kmp"/CLAUDE.md "$kmp"/AGENTS.md 2>/dev/null
done
```

---

## 8. Quick Verification Commands

Before claiming any task is complete, run:

```bash
# Compilation sanity
./gradlew :shared:compileKotlinDesktop :shared:compileKotlinAndroid --no-daemon

# Primary test suite (no Android SDK)
./gradlew :shared:desktopTest --no-daemon
make test-shared

# Robolectric (dedicated container, see iter 27)
make container-robolectric-test

# Container release pipeline
make container-release

# Anti-bluff gates
bash scripts/anti-bluff/bluff-scanner.sh --mode all
bash yole-challenges/scripts/anchor_manifest_challenge.sh
bash yole-challenges/scripts/mutation_ratchet_challenge.sh

# Host power management ban (CONST-033)
bash yole-challenges/scripts/no_suspend_calls_challenge.sh
bash yole-challenges/scripts/host_no_auto_suspend_challenge.sh

# Full QA
make qa-all
```

---

## 9. After Each Task — UPDATE THIS DOCUMENT (CONST-036)

After completing ANY task, BEFORE the commit that finishes it:

1. **Mark the task** as DONE in Section 7 (or whichever section tracks it).
2. **Update "Last updated"** timestamp at the top of this file to today's date.
3. **Update "HEAD"** line at the top to the new commit SHA (after committing).
4. **Update Section 3** (Uncommitted Files) — must be empty `(None)` after commit.
5. **Update Section 6** (Repo State) — refresh `HEAD` and submodule SHA lines.
6. **Refresh "How to Resume"** prompt in Section 1 if the verification commands changed.
7. **If a new defect was discovered:** add it to Section 4 AND `docs/KNOWN_DEFECTS.md`.
8. **If a new uncommitted file was created and is intentional:** add to Section 3 with explanation.

A commit that changes code without also touching `docs/CONTINUATION.md` is a CONST-036 violation unless the change is purely a typo fix that doesn't affect state, in which case note it inline.

---

## 10. Submodule Independence

Each submodule (`Challenges/`, `Containers/`, `HelixQA/`) has its own governance and its own continuation requirements per CONST-036. When working within a submodule:

1. Update that submodule's `docs/CONTINUATION.md` (or create one if absent).
2. Commit + push within the submodule first.
3. Then return to the superproject and bump the submodule pointer in a follow-up commit.

The main Yole CONTINUATION.md tracks SUPERPROJECT state; submodule-local state is tracked in each submodule's own CONTINUATION.md.

---

## 11. Environment Notes — Host Capability Matrix (NEW iter 29)

> Recorded after a macOS host audit on 2026-05-12. CONST-035 (zero-bluff)
> demands that test/gate status only be reported as PASS when reverified
> on the current host. This section captures which hosts CAN run which
> verifications so future agents know what their environment supports.

### Linux primary dev host (`/run/media/milosvasic/DATA4TB/Projects/Yole`)
- Full Gradle build + `:shared:desktopTest` runnable (8,954/8,954 last green).
- Container release pipeline runnable.
- Bluff scanner + anchor + mutation challenges runnable.
- Sibling KMP repos present in `../`.
- This is the canonical workstation. Test/gate green claims at the top
  of this document refer to this host's most recent run.

### macOS audit host (`/Users/milosvasic/Projects/Yole`) — LIMITED
The following make the macOS host UNABLE to reverify the green claims at
the top of this document. They are environment gaps, not code defects:

1. **`Challenges/` vs `challenges/` case-collision — RESOLVED iter 29.**
   Parent repo previously registered `Challenges` (capital) as a submodule
   AND tracked a separate `challenges` (lowercase) tree at the root, which
   collided on macOS case-insensitive filesystems. Resolved by renaming
   the lowercase parent-tracked tree to `yole-challenges/` (`git mv`,
   blob history preserved). All live references in CLAUDE.md / AGENTS.md
   / CONSTITUTION.md / Makefile / docs/ANTI_BLUFF.md /
   docs/HOST_POWER_MANAGEMENT.md / docs/behavior-anchors.md /
   docs/campaigns/anti-bluff/{CAMPAIGN,MILESTONE-2026-05-01}.md /
   scripts/anti-bluff/{bluff-scanner,pre-commit-hook}.sh /
   scripts/host-power-management/install-host-suspend-guard.sh and the
   six renamed scripts' own self-refs updated to `yole-challenges/`.
   Verification: `bash yole-challenges/scripts/no_suspend_calls_challenge.sh`
   PASSES from the new path (iter 29 verified). Historical plan/spec
   docs in `docs/plans/` and `docs/superpowers/` retain the old
   `challenges/` paths as accurate snapshots of past planning state —
   left intentionally untouched. Challenges submodule now initializes
   cleanly on macOS (verified at SHA `19e1c33d` + nested Panoptic at
   `c22df66`).

2. **10 sibling KMP repos missing — RESOLVED iter 29.**
   `settings.gradle.kts` declares `includeBuild()` for ten sibling
   `../*-KMP` repos. All cloned from `git@github.com:vasic-digital/*-KMP.git`
   into `/Users/milosvasic/Projects/`. Gradle build now resolves
   (`./gradlew :shared:tasks` succeeds; `:shared:desktopTest` invocation
   recorded as iter 29 in-flight verification — see Section 12 below).

3. **`GRADLE_USER_HOME` hard-pointed to `/Volumes/T7/Gradle`** (external
   SSD, not mounted). Workaround in active use:
   `GRADLE_USER_HOME=~/.gradle ./gradlew …`. macOS workflow viable.
   Suggested follow-up: switch invocation pattern in Makefile to
   `${GRADLE_USER_HOME:-$HOME/.gradle}` so users don't need the manual
   override (LOW priority; not blocking).

4. **`bluff-scanner.sh` required bash 4+ — RESOLVED iter 29.**
   `brew install bash` → 5.3.9 at `/opt/homebrew/bin/bash`. Both
   `bluff-scanner.sh` and `anchor_manifest_challenge.sh` now carry a
   `BASH_VERSINFO[0] < 4` guard that prints a clear remediation
   message instead of the cryptic `mapfile: command not found`.

5. **HelixQA nested submodule drift — RESOLVED iter 29.**
   Investigated: drift was forward-only (local SHAs descendants of
   pinned). Cause: accidental recursive update during iter 28 cascade.
   Reset all 18 nested submodules to pinned SHAs via
   `git submodule update --recursive` inside HelixQA. Parent repo
   `git status` clean.

6. **`host_no_auto_suspend_challenge.sh` was systemd-only — RESOLVED iter 29.**
   Added Darwin branch (pmset-based). Two real assertions on macOS:
   (a) system won't auto-sleep — passes if `pmset sleep=0` OR a
       runtime prevention annotation present (e.g., "sleep prevented
       by powerd, caffeinate");
   (b) `pmset disksleep=0` so mid-workload I/O isn't interrupted.
   `install-host-suspend-guard.sh` now exits with concrete pmset
   commands on macOS instead of failing on `systemctl`.

7. **`anchor_manifest_challenge.sh` used BSD-incompatible `xargs` for trim — RESOLVED iter 29.**
   Replaced 6 `echo … | xargs` invocations (which threw
   `unterminated quote` on macOS when row text contained `'`) with a
   pure-bash `trim()` function. No semantics change; warning gone.

### What CAN be run on the macOS host (iter 29)
- `:shared:desktopTest` (in-flight verification — see §12)
- `bash yole-challenges/scripts/no_suspend_calls_challenge.sh` → PASS
- `bash yole-challenges/scripts/host_no_auto_suspend_challenge.sh` → PASS (2/2)
- `bash yole-challenges/scripts/anchor_manifest_challenge.sh` → PASS (under bash 5)
- `bash yole-challenges/scripts/mutation_ratchet_challenge.sh` → PASS (stub)
- `bash scripts/anti-bluff/bluff-scanner.sh --mode all` → PASS (under bash 5)
- File edits, git operations, documentation updates.

### What still CANNOT be run on the macOS host
- Container release pipeline (`make container-release` — Docker/Podman setup not validated yet).
- The Go-based qa-all challenges that depend on `Challenges` submodule's Go binary (untested).
- Anti-bluff scanner (`scripts/anti-bluff/bluff-scanner.sh`).
### Implication for "Resume work" on macOS (post-iter-29)
macOS workflow is now viable for documentation, text/code edits, all
anti-bluff and CONST-033 gates, and Gradle-driven test execution. The
remaining gap is the container release pipeline (Docker/Podman setup
on macOS not yet validated end-to-end). Feature work on §7.4 / §7.5 /
§7.6 / §7.7 is unblocked on macOS as long as the workflow doesn't
require container-based artifacts.

---

## 12. Iter 29 Verification — `:shared:desktopTest` ON macOS

Canonical zero-bluff reverification of CONTINUATION.md's
`8,954/8,954 PASS` claim, run on the macOS audit host immediately
after the iter 29 environment remediation.

- **Date:** 2026-05-12
- **Command:** `GRADLE_USER_HOME=~/.gradle ./gradlew :shared:desktopTest --no-daemon`
- **Duration:** 8 minutes 25 seconds
- **Result (from `shared/build/reports/tests/desktopTest/index.html`):**
  - **Tests:** 8,954
  - **Failures:** 0
  - **Ignored:** 0
- **Build status:** `BUILD SUCCESSFUL`, exit code 0.
- **Conclusion:** Doc claim PROVEN on macOS post-remediation. The two
  dev hosts (Linux primary + macOS audit) are now both functional
  workstations for primary test target.

If this section needs further updates (different test target, new host
introduction, regression observed), agents resuming should append a
dated subsection rather than overwrite — historical verification
records are evidence per CONST-035.

---

<!-- END OF CONTINUATION DOCUMENT -->
