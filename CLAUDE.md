# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## MANDATORY: Build and Test in Containers

**Release builds and full CI test suites MUST be executed inside Docker/Podman containers.**

```bash
# Build container
docker compose build build

# Run tests in container
docker compose run --rm build ./docker/scripts/test-all.sh

# Build releases in container
docker compose run --rm build ./docker/scripts/build.sh
```

For day-to-day development, `:shared:desktopTest` runs on the host (JVM). See Build Commands below.

## MANDATORY: Never Remove or Disable Tests

**NO test may ever be removed, disabled, skipped, or left broken!**

All issues must be fixed by addressing the root causes:
- Fix the source code to match tests if tests are correct
- Fix the tests to match source code if source is correct
- Add missing classes/methods to make code compile
- Add missing imports to tests
- Fix syntax errors
- Fix parameter name mismatches

Any fix applied must be:
- Covered by all supported test types in depth
- Verified by running all challenges
- Properly documented

## Project Overview

**Yole** is a cross-platform text editor supporting 17 text formats plus cloud/network storage protocols, built with Kotlin Multiplatform (KMP). The app is offline-first with optional cloud storage integration.

**Package namespace:** `digital.vasic.yole.*` (legacy: `net.gsantner.opoc.*`)

| Platform | Status |
|----------|--------|
| Android | Production |
| Desktop (Windows/macOS/Linux) | Beta |
| iOS | In development |
| Web (Wasm PWA) | In development |

## Git Submodules

| Submodule | Purpose |
|-----------|---------|
| `Challenges/` | Go-based testing framework with cross-platform challenges |
| `Containers/` | Go-based container orchestration (dependency of Challenges) |

```bash
git submodule update --init --recursive

# Build and test (requires Go 1.24+)
cd Challenges && go build ./... && go vet ./... && go test ./... -race -count=1
cd Containers && go build ./... && go vet ./... && go test ./... -race -count=1
```

## Build Commands

```bash
# Primary dev test command (no Android SDK needed, runs on host JVM)
./gradlew :shared:desktopTest

# Single test class
./gradlew :shared:desktopTest --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests"

# Single test method
./gradlew :shared:desktopTest --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests.ParseQuery"

# All tests (requires Android SDK for androidApp module)
./gradlew test

# Tests with coverage report
./gradlew test koverHtmlReport

# Run apps
./gradlew :desktopApp:run                 # Desktop
./gradlew :androidApp:assembleDebug       # Android (requires ANDROID_SDK_ROOT)
./gradlew :webApp:wasmJsBrowserRun        # Web (Wasm)
# iOS — open iosApp/iosApp.xcodeproj in Xcode

# Static analysis
./gradlew detekt
./gradlew lintFlavorDefaultDebug          # Android lint

# API docs
./gradlew :shared:dokkaHtml

# Makefile shortcuts (no Android SDK needed for these)
make test-shared    # = :shared:desktopTest
make desktop        # = :desktopApp:run

# Go challenge framework
./gradlew runChallenges
```

## Known Issues

- **AGP version mismatch**: `androidApp` tests may fail due to AGP 8.2.2 vs 8.7.3 mismatch. Use `:shared:desktopTest` for routine testing.
- **Container OOM**: Exit code 137 in containers = OOM kill. Increase container memory limits.
- **Go flaky tests**: `TestStress_ConcurrentJWTRefresh` (Auth) and `TestGenericPool_HealthyConnectionsSurvive` (Database) are pre-existing flaky tests in the Go submodules.

## Architecture

### Module Structure

The `shared` module contains all platform-agnostic business logic. **10 extracted KMP modules** are consumed via `includeBuild()` in `settings.gradle.kts`. Platform app modules (`androidApp/`, `desktopApp/`, `iosApp/`, `webApp/`) are thin wrappers. Legacy modules (`commons/`, `core/`) are being phased out.

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

Types with nested objects, sealed class pattern matching, or expect/actual declarations cannot use typealiases and remain as original code in Yole (e.g., `ui/Theme.kt`, `network/common/StorageConfig.kt`, all `network/platform/` files).

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
- **Resilience**: All 8 protocol services use CircuitBreaker, ConnectionLimiter, CancellationException rethrow, normalizePath() for path traversal protection, and serviceScope lifecycle management.
- **Coroutine safety**: `scopeMutex`, `pauseFlagsMutex` for concurrent state. `CancellationException` must always be rethrown in catch blocks.

## Testing

~9,400+ tests across ~195 test files (commonTest + desktopTest + androidUnitTest + wasmJsTest).

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
3. Add `TextFormat` entry to `FormatRegistry.formats` list (order matters — more specific formats before general ones)
4. Add format ID constant to `TextFormat.Companion`
5. Add tests in `shared/src/commonTest/kotlin/digital/vasic/yole/format/[name]/`
6. Add platform-specific code in `androidMain/`, `desktopMain/`, etc. if needed

## Code Conventions

- **Kotlin** primary, Java only for legacy code
- **Test classes** end with `Tests` or `Test`
- **File headers**: SPDX license header (Apache-2.0, CC0-1.0, or Unlicense)
- **Build variants**: `flavorDefault` for dev, `flavorAtest` for testing
- All tests must pass before merging

## Security Scanning

```bash
docker compose --profile security up -d sonarqube   # SonarQube at localhost:9000
./gradlew detekt                                     # Static analysis
./scripts/run_security_scan.sh                       # Full scan script
docker compose --profile full up -d                  # Full security stack
```

Tools configured: SonarQube (Docker), Snyk (CI + Docker), CodeQL (CI), Gitleaks (CI), Detekt (Gradle), OWASP Dependency Check (CI).

## Key Dependencies

- Kotlin 2.0.20, Compose Multiplatform 1.7.3
- Flexmark 0.64.8 (Markdown parsing, with 16+ extensions)
- Ktor Client 3.0.2 (networking)
- Kotlinx: Coroutines 1.9.0, Serialization 1.7.3, DateTime 0.6.1
- Okio 3.9.1 (file system)
- Testing: Kotest 5.9.1, MockK 1.13.13 (JVM-only), AssertJ 3.26.3
- Coverage: Kover 0.8.3
- Version catalog: `gradle/libs.versions.toml`

## Key Files

- `shared/build.gradle.kts` — KMP configuration with all platform targets
- `settings.gradle.kts` — Module includes + 10 composite build `includeBuild()` directives
- `gradle/libs.versions.toml` — Centralized dependency versions
- `Makefile` — Build automation with `make help` for all targets
- `docker-compose.yml` — Container build environment (Podman or Docker)
- `Challenges/` — Go testing framework submodule (run via `./gradlew runChallenges`)
