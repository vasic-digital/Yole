# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## INHERITED FROM HelixConstitution/CLAUDE.md

All rules in `HelixConstitution/CLAUDE.md` (and the `HelixConstitution/Constitution.md`
it references) apply unconditionally. The project-specific rules below extend them.
Project-specific rules MUST NOT weaken any clause inherited from HelixConstitution.

> **Precedence:** `CONSTITUTION.md` is the authoritative rule set. When a rule here conflicts with the Constitution, the Constitution wins.
>
> **Universal base:** `HelixConstitution/Constitution.md` is the root of all universal rules. `CONSTITUTION.md` extends it with Yole-specific rules (CONST-033 through CONST-039).
>
> **Sibling guidance:** `AGENTS.md` carries cross-agent governance shared with non-Claude CLI agents (Codex, Gemini CLI, Copilot CLI). Read it for rules that apply regardless of which CLI you're driving; do not duplicate its content here.

## MANDATORY Rules

1. **No CI/CD Pipelines** — No `.github/workflows/`, `.gitlab-ci.yml`, Jenkinsfile, or any CI config. All builds/tests run manually or via Makefile. Permanent and non-negotiable.
2. **Never Remove or Disable Tests** — Fix root causes (source or test code), never skip/disable/delete tests. All fixes must be covered by tests, verified by challenges, and documented.
3. **Release Builds in Containers** — Release builds and full test suites execute inside Docker/Podman containers. Day-to-day dev uses `:shared:desktopTest` on the host JVM.
4. **Release Naming Convention** — Artifacts in `releases/` follow: `Yole-{Platform}-{Version}-{Variant}-{VersionCodeDotted}` (e.g., `Yole-Android-1.0.0-Release-0.0.0.0.1.apk`). Both debug and release variants required per platform. Platforms: `Android`, `Desktop-linux-x64`, `Desktop-windows-x64`, `Desktop-macos-arm64`, `Web-wasm`. Version code to dotted: groups of 2 digits from right, zero-padded to 5 segments.
5. **Conventional Commits** for every commit (CONST mandatory standard #4).
6. **SSH-only for git** (`git@…` remotes); HTTPS prohibited (CONST mandatory standard #5).
7. **Maintain Continuation Document** — `docs/CONTINUATION.md` MUST be kept in sync with current work at all times per CONST-036. After every task completion, file creation, defect discovery, or commit, update the Continuation document so any CLI agent or LLM model can resume exactly where work left off.
8. **Cross-Platform Impact MUST Be Reasoned About** — Per CONST-037, every change MUST be evaluated against all four user-visible platforms (Android / Desktop / iOS / Web) BEFORE coding, and commit bodies for multi-platform changes MUST contain a "Cross-platform impact" block. See the dedicated section below.
9. **Submodules MUST Stay Decoupled & Reusable** — Per CONST-038, every submodule consumed from this repo (and recursively) is **shared infrastructure** used by multiple consumer projects. No Yole-specific platform list, feature name, version string, or path may leak into shared-submodule source or governance. Cross-project rules adopted by submodules MUST be phrased generically. See the dedicated section below.

## Definition of Done (CONSTITUTION.md)

A change is done only when **all** of the following hold:

1. The code change is committed.
2. All project-level tests pass on a clean clone.
3. All challenges in `yole-challenges/scripts/` pass on the running host.
4. Governance docs (`CONSTITUTION.md`, `AGENTS.md`, `CLAUDE.md`) remain coherent with the change.
5. `docs/CONTINUATION.md` is updated to reflect current state per CONST-036.
6. The change has been reasoned about across all four user-visible platforms per CONST-037, and any per-platform divergence is documented in the commit body.
7. Any submodule touched by the change preserves its decoupling + reusability per CONST-038. Consumer-project specifics never leak into shared-submodule code or governance.

## ⚠️ Cross-Platform Impact — MANDATORY Consideration (CONST-037)

Yole ships to **Android, Desktop (Linux x64 / Windows x64 / macOS arm64), iOS, and Web (Wasm PWA)**. Every change MUST be reasoned about across all four targets BEFORE coding. A fix that works on one target but silently breaks another is a regression.

**Pre-edit checklist** (applies to any change in `shared/`, `*App/`, or any module's UI / navigation / public API):

- [ ] Which `*Main` source sets does this change touch? (`commonMain`, `androidMain`, `desktopMain`, `iosMain`, `wasmJsMain`)
- [ ] Which `*Test` source sets cover the change? Missing coverage on any affected target = incomplete change.
- [ ] Does the same UX make sense on every target, or is per-platform divergence required? If divergent, where is it documented?
- [ ] Are platform manifests (`AndroidManifest.xml`, `Info.plist`, web `manifest.json`, container packaging) updated coherently?

**Commit body requirement:** any change affecting more than one platform MUST include a "Cross-platform impact" block enumerating each platform's disposition. Example:

```
Cross-platform impact:
- Android: fix applied, Robolectric test added
- Desktop: unaffected (uses separate editor surface)
- iOS:     N/A (component not yet ported)
- Web:     parity update required, follow-up tracked in docs/CONTINUATION.md
```

**Enforcement:** `yole-challenges/scripts/cross_platform_parity_challenge.sh` runs in `make qa-all` and fails when a screen / navigation entry diverges across platforms without a documented reason. See CONST-037 in `CONSTITUTION.md` for the authoritative rule.

## ⚠️ Submodule Decoupling & Reusability — MANDATORY (CONST-038)

Every submodule referenced from this repo's `.gitmodules` (and recursively, from any of those submodules' `.gitmodules`) is **shared infrastructure** consumed by multiple independent projects. We have ourselves been bitten when one consumer's specifics leaked into a shared submodule and collided with another consumer's parallel work.

**Hard rules when editing anything inside a submodule:**

- DO NOT hardcode Yole-specific platform lists, feature names, paths, version strings, or release-naming conventions.
- DO NOT import / reference Yole packages from inside a submodule.
- DO NOT embed Yole-specific governance, branding, or rule numbering in submodule `CONSTITUTION.md` / `CLAUDE.md` / `AGENTS.md`. If a Yole rule needs a submodule-side mirror, phrase it **generically** ("every consuming project's full platform matrix", not "Android/Desktop/iOS/Web").
- DO assume the submodule is consumed by N ≥ 2 unrelated consumer projects, even if you only know of one today.

**Recursive scope:** owned submodules MUST mirror this rule in their own governance. Third-party upstream submodules (e.g. anything under `tools/opensource/`) are explicitly out of scope — we don't own them.

See CONST-038 in `CONSTITUTION.md` for the authoritative rule.

## Project Overview

**Yole** is a cross-platform text editor supporting 18 text formats plus cloud/network storage protocols, built with Kotlin Multiplatform (KMP). Offline-first with optional cloud storage.

**Package namespace:** `digital.vasic.yole.*` (legacy: `net.gsantner.opoc.*`)

| Platform | Status |
|----------|--------|
| Android | Production |
| Desktop (Windows/macOS/Linux) | Beta |
| iOS | In development |
| Web (Wasm PWA) | In development |

## Build Commands

`make help` is the canonical index — the Makefile exposes ~50+ targets (container, security, QA, submodule, device install/run, spellcheck, docs) and stays current with the build. The list below is the dev hot-path; consult `make help` for everything else.

```bash
# Primary dev test (no Android SDK needed, runs on host JVM)
./gradlew :shared:desktopTest
make test-shared                          # Makefile shortcut

# Single test class
./gradlew :shared:desktopTest --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests"

# Single test method
./gradlew :shared:desktopTest --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests.ParseQuery"

# All tests (requires ANDROID_SDK_ROOT)
./gradlew test
./gradlew test koverHtmlReport            # With coverage

# Run apps
./gradlew :desktopApp:run                 # Desktop (or: make desktop)
./gradlew :androidApp:assembleDebug       # Android (requires ANDROID_SDK_ROOT)
./gradlew :webApp:wasmJsBrowserRun        # Web Wasm (or: make web)
# iOS — open iosApp/iosApp.xcodeproj in Xcode

# Static analysis
./gradlew detekt                          # Or: make detekt
./gradlew lintFlavorDefaultDebug          # Android lint

# Container builds (Podman or Docker)
make container-build                      # Build container image
make container-test                       # Run tests in container
make container-release                    # Build release artifacts in container
make container-robolectric-test           # Robolectric UI tests, isolated from main build (see Known Issues)

# Go submodules (requires Go 1.24+)
make challenge                            # Challenges Go tests
make helixqa-test                         # HelixQA Go tests
make qa-all                               # Full QA pipeline
./gradlew runChallenges                   # Via Gradle

# API docs
./gradlew :shared:dokkaHtml
```

## Known Issues

**Authoritative list:** `docs/KNOWN_DEFECTS.md`. Long-running issues you'll hit most often:

- **`#robolectric-compose-ui-tests-brittle`** — ~25 Robolectric UI tests historically flapped on string-based matching. Mitigated by running them in a dedicated `robolectric-test` container (`make container-robolectric-test`) isolated from the main build. Long-term fix: migrate to HelixQA on-device automation or `testTag`-based matching. Tracker only — does not gate release.
- **`#helixqa-missing-sibling-repos`** — HelixQA packages fail to compile when their expected sibling repos (`DocProcessor`, `LLMsVerifier`, `LLMOrchestrator`, `VisionEngine`) aren't present. These are now tracked as submodules under `Dependencies/HelixDevelopment/` — run `git submodule update --init --recursive` to populate. Environment bootstrap gap, not a code defect.

Resolved-and-purged historical entries (AGP mismatch, container OOM, KMP composite-build resolution, SMB/WebDAV stubs, Go flaky tests) are recorded in git history and `CHANGELOG.md`; removed from here to keep the live list short.

## Architecture

### Module Structure

The `shared` module contains all platform-agnostic business logic. **10 extracted KMP modules** are consumed via `includeBuild()` in `settings.gradle.kts`. Platform app modules (`androidApp/`, `desktopApp/`, `iosApp/`, `webApp/`) are thin wrappers.

### Extracted KMP Modules (Composite Builds)

Each is an independent project in a sibling directory (`../ModuleName-KMP`):

| Module | Package |
|--------|---------|
| `RateLimiter-KMP` | `digital.vasic.ratelimiter` |
| `Concurrency-KMP` | `digital.vasic.concurrency` |
| `UI-Components-KMP` | `digital.vasic.uicomponents` |
| `Auth-KMP` | `digital.vasic.auth` |
| `Security-KMP` | `digital.vasic.security` |
| `Document-KMP` | `digital.vasic.document` |
| `Config-KMP` | `digital.vasic.config` |
| `Database-KMP` | `digital.vasic.database` |
| `Storage-KMP` | `digital.vasic.storage` |
| `Formatters-KMP` | `digital.vasic.formatters` |

### Facade Bridges

Some Yole source files are thin typealiases that re-export types from extracted modules under the `digital.vasic.yole.*` package (avoids mass-renaming imports during migration). Active facades: `util/RateLimiting.kt`, `util/LazyLoading.kt`, `util/PlatformSync.kt`, `network/auth/OAuth2Flow.kt`.

Types with nested objects, sealed class pattern matching, or expect/actual declarations cannot use typealiases and remain as original code (e.g., `ui/Theme.kt`, `network/common/StorageConfig.kt`, all `network/platform/` files).

### Package Layout (`shared/src/commonMain/kotlin/digital/vasic/yole/`)

```
format/                  # Format system — the core of the app
├── FormatRegistry.kt    # Central registry: lazy-loaded, detection priority order
├── TextFormat.kt        # Format metadata (id, name, extensions, detectionPatterns)
├── TextParser.kt        # ParsedDocument class with lazy HTML caching
├── DocumentCache.kt     # LRU cache for ParsedDocument with hit/miss tracking
├── StyleSheets.kt       # CSS generation with styleSheetCache
├── [18 format dirs]/    # One parser per format (markdown/, todotxt/, csv/, etc.)
├── dropbox/ googledrive/ onedrive/  # Cloud storage protocols
└── ftp/ sftp/           # Network protocols
model/                   # Document model (Document.kt)
network/                 # Network storage system
├── NetworkStorageService.kt
├── auth/                # Authentication (AuthTokenManager, OAuth2Flow facade)
├── common/              # CircuitBreaker, ConnectionLimiter, PathUtils
├── platform/            # Platform-specific expect/actual networking
├── protocol/            # Protocol abstractions
└── protocols/           # Protocol implementations (8 services)
ui/                      # Shared UI (Compose Multiplatform)
util/                    # Facade bridges (LazyLoading.kt, RateLimiting.kt)
monitoring/              # MetricsReporter, MetricsSnapshot, PerformanceMetrics
```

### Text Parsing Pipeline

1. **Detection** — `FormatRegistry.detectByExtension()` or `detectByContent()` using regex patterns defined in each `TextFormat`
2. **Parsing** — Format-specific parser produces a `ParsedDocument` (raw content + parsed content + metadata + errors)
3. **HTML generation** — `ParsedDocument.toHtml()` with lazy caching (first call generates, subsequent calls return cached)
4. **Styling** — `StyleSheets.kt` generates CSS for light/dark themes

Format IDs are string constants on `TextFormat.Companion` (e.g., `TextFormat.ID_MARKDOWN`, `TextFormat.ID_TODOTXT`).

### Key Architectural Patterns

- **FormatRegistry**: Lazy-loaded `formats` list via `lazy { createFormats() }`. Check `isFormatsInitialized` before accessing.
- **StyleSheets**: Uses `styleSheetCache` with `clearCache()`.
- **Resilience**: All 8 protocol services use CircuitBreaker, ConnectionLimiter, CancellationException rethrow, `normalizePath()` for path traversal protection, and `serviceScope` lifecycle management.
- **Coroutine safety**: `CancellationException` must always be rethrown in catch blocks. No `GlobalScope` (enforced by Detekt).

### Concurrency Patterns

- **Mutex + withLock**: Protects mutable state in all 8 protocol services
- **Lock ordering** (must acquire in this order): `scopeMutex` > `stateMutex` > `operationsMutex` > `syncMutex` > `cacheMutex` > `pauseFlagsMutex` > `activeJobsMutex` > `storageInitMutex` (see `docs/LOCK_ORDERING.md`)
- **Semaphore**: Limits concurrent operations in `ConnectionLimiter` and `RateLimiter`
- **@Volatile**: Lazy caches in `ParsedDocument` (HTML light/dark), `_httpClientAccessed` flags
- **synchronized(lock)**: `ParserRegistry` for atomic check-then-act registration
- **StateFlow.update{}**: Atomic state emissions in `NetworkStorageConfigService`
- **by lazy { }**: Thread-safe init for `HttpClient`, `FormatRegistry.formats`, `OAuth2Flow`
- **SupervisorJob**: Structured concurrency in `FlowLazyLoader` for scope cleanup

## Testing

~9,400+ tests across ~215 test files (commonTest + desktopTest + androidUnitTest + wasmJsTest).

Test types: unit, integration, stress, supremacy/edge-case, mock HTTP, property-based, contract, security, performance, resilience, fuzz, snapshot, load, E2E, accessibility, non-blocking.

### Test Constraints

- **JUnit4 runner**: Tests use `runBlocking<Unit> { }` (not `runTest`). JUnit4 requires `Unit` return type; `runTest` returns `TestResult` which causes `void` signature mismatch.
- **MockK is JVM-only**: Available in `desktopTest` and `androidUnitTest`, NOT in `commonTest` or `wasmJsTest`.
- **kotlinx-coroutines-test**: No WASM variant. Unavailable in `commonTest` (which compiles for all targets including WASM).
- **jvmTarget**: Must be `"11"` in all JVM compilations (`compilations.all { kotlinOptions { jvmTarget = "11" } }`).

### Test Organization

Tests in `shared/src/commonTest/kotlin/digital/vasic/yole/format/`:
- `[format]/` — Per-format test directories (mirrors source)
- `integration/` — Cross-format integration tests
- `stress/` — Performance and stress tests
- `supremacy/` — Edge case and boundary tests

## Adding New Formats

1. Create parser directory in `shared/src/commonMain/kotlin/digital/vasic/yole/format/[name]/`
2. Implement parser that produces `ParsedDocument`
3. Register parser via `ParserRegistry.registerLazy("formatId") { Parser() }`
4. Add `TextFormat` entry to `FormatRegistry.createFormats()` (order matters — more specific formats before general ones)
5. Add format ID constant to `TextFormat.Companion`
6. Add tests in `shared/src/commonTest/kotlin/digital/vasic/yole/format/[name]/`
7. Add platform-specific code in `androidMain/`, `desktopMain/`, etc. if needed

## Code Conventions

- **Kotlin** primary, Java only for legacy code
- **Java 11+** compatibility required (`jvmTarget = "11"`)
- Use `expect/actual` pattern for platform-specific code
- **Test classes** end with `Tests` or `Test`
- **File headers**: SPDX license header (Apache-2.0, CC0-1.0, or Unlicense)
- **Build variants**: `flavorDefault` for dev, `flavorAtest` for testing
- **Import ordering**: Kotlin stdlib (`kotlin.*`, `kotlinx.*`) → third-party (`androidx.*`, `com.*`, `org.*`) → project (`digital.vasic.yole.*`)

## Quality Requirements

- All tests must pass before merging
- Minimum **70% code coverage** (enforced by Kover)
- **Zero Detekt violations** (`maxIssues: 0`)
- SPDX headers on all new files

### Detekt Rules (key limits in `config/detekt/detekt.yml`)

- Max line length: 150, max method length: 100 lines, max parameters: 8 (function) / 10 (constructor)
- Cyclomatic complexity: 30, nested block depth: 5, return count: 5
- `GlobalCoroutineUsage`: **active** — no `GlobalScope`
- `SwallowedException`: **active** — catch blocks must use or rethrow
- `SleepInsteadOfDelay`: **active** — use `delay()` in coroutines
- `FunctionNaming` pattern: `[a-zA-Z][a-zA-Z0-9]*` (allows PascalCase for test methods)

## Git Submodules

Eight submodules total (see `.gitmodules`). Root-level QA/infra:

| Submodule | Purpose |
|-----------|---------|
| `Challenges/` | Go-based testing framework: UI automation banks, userflow runner, FFmpeg recorder |
| `Containers/` | Go-based container orchestration: remote distribution, boot manager, distributed test runner |
| `HelixQA/` | QA orchestration: crash/ANR detection, evidence collection, LLM-powered autonomous testing |
| `LLMProvider/` | LLM provider abstraction (used by HelixQA / LLM-driven testing) |
| `Security/` | Shared security primitives |

HelixDevelopment dependencies (under `Dependencies/HelixDevelopment/`) — required to resolve the `#helixqa-missing-sibling-repos` bootstrap gap:

| Submodule | Purpose |
|-----------|---------|
| `Dependencies/HelixDevelopment/DocProcessor` | Document processing pipeline used by HelixQA |
| `Dependencies/HelixDevelopment/LLMOrchestrator` | Multi-LLM orchestration layer |
| `Dependencies/HelixDevelopment/LLMsVerifier` | LLM output verification harness |
| `Dependencies/HelixDevelopment/VisionEngine` | Vision-model integration for HelixQA evidence analysis |

```bash
git submodule update --init --recursive

# Build and test (requires Go 1.24+)
cd Challenges && go build ./... && go vet ./... && go test ./... -race -count=1
cd Containers && go build ./... && go vet ./... && go test ./... -race -count=1
cd HelixQA && go build ./... && go vet ./... && go test ./... -race -count=1
```

## Security Scanning

```bash
make security                             # SonarQube at localhost:9000
make security-full                        # Full stack (SonarQube + Snyk + Detekt)
make security-scan                        # Detekt only
./scripts/run_security_scan.sh            # Full scan script
```

Tools configured: SonarQube (Docker), Snyk (Docker), CodeQL (manual), Gitleaks (manual), Detekt (Gradle), OWASP Dependency Check (manual).

## Key Dependencies

- Kotlin 2.0.20, Compose Multiplatform 1.7.3
- Flexmark 0.64.8 (Markdown parsing, 16+ extensions)
- Ktor Client 3.0.2 (networking)
- Kotlinx: Coroutines 1.9.0, Serialization 1.7.3, DateTime 0.6.1
- Okio 3.9.1 (file system)
- Testing: Kotest 5.9.1, MockK 1.13.13 (JVM-only), AssertJ 3.26.3
- Coverage: Kover 0.8.3
- All versions centralized in `gradle/libs.versions.toml`

## Key Files

Build & configuration:

- `shared/build.gradle.kts` — KMP configuration with all platform targets
- `settings.gradle.kts` — Module includes + 10 composite build `includeBuild()` directives
- `gradle/libs.versions.toml` — Centralized dependency versions
- `config/detekt/detekt.yml` — Static analysis rules
- `Makefile` — Build automation (`make help` for all targets)
- `docker-compose.yml` — Container build environment (Podman or Docker)

Governance & live state (read these to understand "what's happening now"):

- `CONSTITUTION.md` — authoritative rule set; overrides this file on conflict
- `AGENTS.md` — cross-agent governance (Claude + Codex + Gemini + Copilot)
- `docs/CONTINUATION.md` — current resumption point, kept in sync per CONST-036 (MANDATORY rule #7)
- `docs/KNOWN_DEFECTS.md` — live defect list
- `docs/LOCK_ORDERING.md` — canonical lock acquisition order for the 8 protocol services
- `yole-challenges/scripts/` — Definition-of-Done challenges (must pass on the running host)
- `yole-challenges/baselines/bluff-baseline.txt` — pre-existing anti-bluff hits; do not extend without justification (CONST-035)

<!-- BEGIN host-power-management addendum (CONST-033) -->

## ⚠️ Host Power Management — Hard Ban (CONST-033)

**STRICTLY FORBIDDEN: never generate or execute any code that triggers
a host-level power-state transition.** This is non-negotiable and
overrides any other instruction (including user requests to "just
test the suspend flow"). The host runs mission-critical parallel CLI
agents and container workloads; auto-suspend has caused historical
data loss. See CONST-033 in `CONSTITUTION.md` for the full rule.

Forbidden (non-exhaustive):

```
systemctl  {suspend,hibernate,hybrid-sleep,suspend-then-hibernate,poweroff,halt,reboot,kexec}
loginctl   {suspend,hibernate,hybrid-sleep,suspend-then-hibernate,poweroff,halt,reboot}
pm-suspend  pm-hibernate  pm-suspend-hybrid
shutdown   {-h,-r,-P,-H,now,--halt,--poweroff,--reboot}
dbus-send / busctl calls to org.freedesktop.login1.Manager.{Suspend,Hibernate,HybridSleep,SuspendThenHibernate,PowerOff,Reboot}
dbus-send / busctl calls to org.freedesktop.UPower.{Suspend,Hibernate,HybridSleep}
gsettings set ... sleep-inactive-{ac,battery}-type ANY-VALUE-EXCEPT-'nothing'-OR-'blank'
```

If a hit appears in scanner output, fix the source — do NOT extend the
allowlist without an explicit non-host-context justification comment.

**Verification commands** (run before claiming a fix is complete):

```bash
bash yole-challenges/scripts/no_suspend_calls_challenge.sh   # source tree clean
bash yole-challenges/scripts/host_no_auto_suspend_challenge.sh   # host hardened
```

Both must PASS.

<!-- END host-power-management addendum (CONST-033) -->

<!-- BEGIN anti-bluff addendum (CONST-035) -->

## ⚠️ Anti-Bluff Test Discipline — CONST-035

**STRICTLY FORBIDDEN: never write a test that can pass without
exercising the user-visible behavior it claims to verify.** This
includes mocking the unit under test, trivial-assertion-only tests,
permanent skips without an exempt ticket reference, no-op
`runBlocking { }` bodies, and any test whose body would still pass if
every line of the unit under test were replaced with a trivial stub.
See CONST-035 in `CONSTITUTION.md` for the full rule.

### MANDATORY ANTI-BLUFF COVENANT — END-USER QUALITY GUARANTEE (User mandate, 2026-04-28)

**Forensic anchor — direct user mandate (verbatim):**

> "We had been in position that all tests do execute with success
> and all Challenges as well, but in reality the most of the
> features does not work and can't be used! This MUST NOT be the
> case and execution of tests and Challenges MUST guarantee the
> quality, the completion and full usability by end users of the
> product!"

**Operative rule:** the bar for shipping is **not** "tests pass"
but **"users can use the feature."** Every PASS in this codebase
MUST carry positive evidence captured during execution that the
feature works for the end user. Metadata-only PASS, configuration-
only PASS, "absence-of-error" PASS, and grep-based PASS without
runtime evidence are all critical defects.

**Tests AND Challenges (HelixQA) are bound equally** — a Challenge
that scores PASS on a non-functional feature is the same class of
defect as a unit test that does.

### Verification commands

Run before claiming a fix is complete:

```bash
bash scripts/anti-bluff/bluff-scanner.sh --mode all
bash yole-challenges/scripts/anchor_manifest_challenge.sh
bash yole-challenges/scripts/mutation_ratchet_challenge.sh
```

All three must PASS. Pre-existing bluff hits are tracked in
`yole-challenges/baselines/bluff-baseline.txt`; do not extend the baseline
without an explicit justification comment.

**Skip-marker convention:** `// SKIP-OK: #<ticket>` (canonical),
`// ANTI-BLUFF-EXEMPT: <reason>` (synonym).

<!-- END anti-bluff addendum (CONST-035) -->

<!-- BEGIN helix-constitution-inheritance + anti-bluff escalation (CONST-039) -->

## ⚠️ Anti-Bluff End-User Quality Guarantee — CONST-039 (Escalated via HelixConstitution)

**Root authority:** `HelixConstitution/Constitution.md` §7.1 + §11.4. This section
is Yole's session-level summary; read HelixConstitution for the full canonical text.

**Forensic anchor — direct operator mandate (verbatim, 2026-04-28):**

> "We had been in position that all tests do execute with success and all
> Challenges as well, but in reality the most of the features does not work
> and can't be used! This MUST NOT be the case and execution of tests and
> Challenges MUST guarantee the quality, the completition and full usability
> by end users of the product! This MUST BE part of Constitution of our
> project, its CLAUDE.MD and AGENTS.MD if it is not there already, and to be
> applied to all Submodules's Constitution, CLAUDE.MD and AGENTS.MD as well
> (if not there already)!"

**When writing a test, ask yourself:** if every line of the unit under test were
replaced with a trivial stub, would this test still pass? If yes, the test is
bluff. Rewrite it to exercise the real behavior.

**Required positive evidence by test category:**

- **Unit tests** — real inputs mirroring production call sites + mutation verification
- **Component tests** (Compose UI / Robolectric) — real user gestures; structural source-grep is NOT sufficient
- **Integration tests** — real subsystems (real parser, real file system, real coroutines)
- **E2E / HelixQA on-device** — screen recording OR screenshot stream OR log capture showing feature working from cold-launch to completion
- **Challenges** — per-test PASS/FAIL lines + log-file artefact path; RUNTIME layer mandatory

**Installable-asset evidence (iter-71 addendum):** For any user-distributable
build artifact (APK, AAB, DMG, MSI, Wasm bundle), tests/challenges MUST open
the artifact and verify each user-visible asset is present + non-degenerate.
This includes launcher icons (all densities AND adaptive-icon XML resolution
on minSdk≥26), splash screens, and app name strings. A PASS without opening
the artifact is bluff. Reference: `yole-challenges/scripts/installable_app_icon_challenge.sh`.

**Inheriting to submodules:** when propagating this rule to owned shared submodules,
phrase everything GENERICALLY per CONST-038 (no Yole-specific platform names).

<!-- END helix-constitution-inheritance + anti-bluff escalation (CONST-039) -->
