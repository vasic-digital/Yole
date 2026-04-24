# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## MANDATORY Rules

1. **No CI/CD Pipelines** — No `.github/workflows/`, `.gitlab-ci.yml`, Jenkinsfile, or any CI config. All builds/tests run manually or via Makefile. Permanent and non-negotiable.
2. **Never Remove or Disable Tests** — Fix root causes (source or test code), never skip/disable/delete tests. All fixes must be covered by tests, verified by challenges, and documented.
3. **Release Builds in Containers** — Release builds and full test suites execute inside Docker/Podman containers. Day-to-day dev uses `:shared:desktopTest` on the host JVM.
4. **Release Naming Convention** — Artifacts in `releases/` follow: `Yole-{Platform}-{Version}-{Variant}-{VersionCodeDotted}` (e.g., `Yole-Android-1.0.0-Release-0.0.0.0.1.apk`). Both debug and release variants required per platform. Platforms: `Android`, `Desktop-linux-x64`, `Desktop-windows-x64`, `Desktop-macos-arm64`, `Web-wasm`. Version code to dotted: groups of 2 digits from right, zero-padded to 5 segments.

## Definition of Done

A change is NOT done because code compiles and tests pass. "Done" requires pasted
terminal output from a real run of the real system, produced in the same session as
the change. Coverage and passing suites measure the LLM's model of the product, not
the product.

1. **No self-certification.** *Verified, tested, working, complete, fixed, passing*
   are forbidden in commits, PRs, and agent replies without accompanying pasted
   output from a same-session real-system run.
2. **Demo before code.** Every task begins with the runnable acceptance demo below.
3. **Real system.** Android demos run against a real debug APK installed on an
   emulator or device; desktop demos run the built Kotlin/Native or JVM binary.
   Robolectric / JVM unit tests do not count as proof-of-done.
4. **Skips are loud.** `@Ignore` / `@Ignored` / `assumeTrue` without a trailing
   `SKIP-OK: #<ticket>` comment fails `make ci-validate-all`.
5. **Contract tests on every seam.** Shared KMP↔Android↔Desktop boundaries
   carry a smoke test that loads a real document end-to-end on each surface.
6. **Evidence in the PR.** PR body contains a fenced `## Demo` block with exact
   command(s) and output (or screenshot/logcat excerpt for on-device demos).

### Acceptance demo for this module

```bash
# Yole acceptance demo: build the shared KMP module and the Android debug APK,
# run the host-JVM shared test suite, and assert the APK was produced.
set -e
cd "$(dirname "$0")" 2>/dev/null || true

# Fast host-JVM shared tests — always runs.
./gradlew --no-daemon :shared:desktopTest

# Android debug APK build — skips loudly if ANDROID_SDK_ROOT not set, rather
# than silently doing nothing.
if [ -z "${ANDROID_SDK_ROOT:-}" ] && [ -z "${ANDROID_HOME:-}" ]; then
  echo "demo-android-skipped: no ANDROID_SDK_ROOT / ANDROID_HOME in env"
  echo "demo-android-skipped: SKIP-OK: no-android-sdk-in-env"
else
  ./gradlew --no-daemon :androidApp:assembleDebug
  apk=$(find androidApp/build/outputs/apk/debug -name '*.apk' | head -1)
  [ -n "$apk" ] && [ -f "$apk" ] || { echo "no APK produced"; exit 1; }
  echo "APK built: $apk ($(stat -c%s "$apk") bytes)"
fi
```

Expect: `:shared:desktopTest` passes, and (when Android SDK is present) an
APK artifact is produced at `androidApp/build/outputs/apk/debug/*.apk`.
Without the Android SDK, the Android portion skips loudly — annotated so
`no-silent-skips` doesn't flag it as invisible debt.

## Project Overview

**Yole** is a cross-platform text editor supporting 17 text formats plus cloud/network storage protocols, built with Kotlin Multiplatform (KMP). Offline-first with optional cloud storage.

**Package namespace:** `digital.vasic.yole.*` (legacy: `net.gsantner.opoc.*`)

| Platform | Status |
|----------|--------|
| Android | Production |
| Desktop (Windows/macOS/Linux) | Beta |
| iOS | In development |
| Web (Wasm PWA) | In development |

## Build Commands

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

# Go submodules (requires Go 1.24+)
make challenge                            # Challenges Go tests
make helixqa-test                         # HelixQA Go tests
make qa-all                               # Full QA pipeline
./gradlew runChallenges                   # Via Gradle

# API docs
./gradlew :shared:dokkaHtml
```

## Known Issues

- **AGP version mismatch**: `androidApp` tests may fail due to AGP version conflicts. Use `:shared:desktopTest` for routine testing.
- **Container OOM**: Exit code 137 = OOM kill. Increase container memory limits (`mem_limit` in `docker-compose.yml`).
- **Go flaky tests**: `TestStress_ConcurrentJWTRefresh` (Auth) and `TestGenericPool_HealthyConnectionsSurvive` (Database) are pre-existing.

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
├── [17 format dirs]/    # One parser per format (markdown/, todotxt/, csv/, etc.)
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

| Submodule | Purpose |
|-----------|---------|
| `Challenges/` | Go-based testing framework: UI automation banks, userflow runner, FFmpeg recorder |
| `Containers/` | Go-based container orchestration: remote distribution, boot manager, distributed test runner |
| `HelixQA/` | QA orchestration: crash/ANR detection, evidence collection, LLM-powered autonomous testing |

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

- `shared/build.gradle.kts` — KMP configuration with all platform targets
- `settings.gradle.kts` — Module includes + 10 composite build `includeBuild()` directives
- `gradle/libs.versions.toml` — Centralized dependency versions
- `config/detekt/detekt.yml` — Static analysis rules
- `Makefile` — Build automation (`make help` for all targets)
- `docker-compose.yml` — Container build environment (Podman or Docker)
