# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## MANDATORY: Build and Test in Containers

**ALL builds and tests MUST be executed inside Docker/Podman containers, NOT directly on the host machine!**

This ensures consistent environment, proper dependencies, reproducible builds, Android emulator access, and all integration services.

```bash
# Build container
docker compose build build

# Run tests in container
docker compose run --rm build ./docker/scripts/test-all.sh

# Build releases in container
docker compose run --rm build ./docker/scripts/build.sh
```

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

| Submodule | Purpose | URL |
|-----------|---------|-----|
| `Challenges/` | Go-based testing framework with cross-platform challenges | `git@github.com:vasic-digital/Challenges.git` |
| `Containers/` | Go-based container orchestration (dependency of Challenges) | `git@github.com:vasic-digital/Containers.git` |

```bash
# Initialize submodules after clone
git submodule update --init --recursive

# Build and test Challenges
cd Challenges && go build ./... && go vet ./... && go test ./... -race -count=1

# Build and test Containers
cd Containers && go build ./... && go vet ./... && go test ./... -race -count=1
```

## Build Commands

```bash
# Android
./gradlew :androidApp:assembleDebug

# Desktop
./gradlew :desktopApp:run

# Web (Wasm)
./gradlew :webApp:wasmJsBrowserRun

# iOS — open iosApp/iosApp.xcodeproj in Xcode

# All tests
./gradlew test

# Single test class
./gradlew test --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests"

# Single test method
./gradlew test --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests.ParseQuery"

# Tests with coverage report
./gradlew test koverHtmlReport

# Lint (Android)
./gradlew lintFlavorDefaultDebug

# API docs
./gradlew :shared:dokkaHtml

# Makefile (legacy Android-oriented, requires ANDROID_SDK_ROOT)
make all install run
```

## Architecture

### Module Structure

Yole's shared business logic is split between the `shared` module and **10 extracted KMP modules** consumed via Gradle composite builds. Platform app modules (`androidApp/`, `desktopApp/`, `iosApp/`, `webApp/`) are thin wrappers that depend on `shared`.

Legacy Android-only modules (`commons/`, `core/`) are minimal and being phased out.

### Extracted KMP Modules (Composite Builds)

Each module is an independent project with its own repo, tests, and documentation. They are wired into Yole via `includeBuild()` in `settings.gradle.kts`:

| Module | Package | Source |
|--------|---------|--------|
| `RateLimiter-KMP` | `digital.vasic.ratelimiter` | Rate limiting utilities |
| `Concurrency-KMP` | `digital.vasic.concurrency` | Lazy loading, platform sync |
| `UI-Components-KMP` | `digital.vasic.uicomponents` | Theme, animations, accessibility |
| `Auth-KMP` | `digital.vasic.auth` | OAuth2 flows, token management |
| `Security-KMP` | `digital.vasic.security` | Secure storage |
| `Document-KMP` | `digital.vasic.document` | Document model |
| `Config-KMP` | `digital.vasic.config` | Network configuration |
| `Database-KMP` | `digital.vasic.database` | Metadata storage |
| `Storage-KMP` | `digital.vasic.storage` | Protocol abstractions + implementations |
| `Formatters-KMP` | `digital.vasic.formatters` | 17 text format parsers |

### Facade Bridges

During the transition, some Yole source files are **facade bridges** — thin typealiases that re-export types from extracted modules under the original `digital.vasic.yole.*` package. This allows existing code to compile without mass-renaming imports.

**Active facades** (typealias files):
- `util/RateLimiting.kt` — `RateLimiter`, `TokenBucket`, `AdaptiveRateLimiter`, `OperationThrottler`
- `util/LazyLoading.kt` — `LazyDocumentLoader<T>`, `LazyStringLoader`, `FlowLazyLoader<T>`
- `util/PlatformSync.kt` — `platformSynchronized()` delegate function
- `network/auth/OAuth2Flow.kt` — `OAuth2Flow`, `TokenResponse`, `DropboxOAuth2Flow`, `GoogleDriveOAuth2Flow`, `OneDriveOAuth2Flow`

**Not facaded** (original code kept in Yole due to Kotlin typealias limitations with nested objects, sealed class pattern matching, and expect/actual declarations):
- `ui/Theme.kt`, `ui/Animations.kt`, `ui/Accessibility.kt`
- `network/common/StorageConfig.kt`
- `network/auth/AuthTokenManager.kt`
- All `network/platform/` expect/actual files

### Shared Module Source Sets

```
shared/src/
├── commonMain/          # All shared code (the primary codebase)
├── commonTest/          # All shared tests
├── commonBenchmark/     # Benchmarks
├── androidMain/         # Android-specific expect/actual
├── androidTest/
├── desktopMain/         # Desktop (JVM) expect/actual
├── desktopTest/
├── desktopBenchmark/
├── iosMain/             # iOS expect/actual
├── iosTest/
├── wasmJsMain/          # Web/Wasm expect/actual
└── wasmJsTest/
```

### Package Layout (`shared/src/commonMain/kotlin/digital/vasic/yole/`)

```
format/                  # Format system — the core of the app
├── FormatRegistry.kt    # Central registry: all formats with detection priority
├── TextFormat.kt        # Format metadata (id, name, extensions, detectionPatterns)
├── TextParser.kt        # ParsedDocument class with lazy HTML caching
├── ParserInitializer.kt # Format initialization
├── StyleSheets.kt       # CSS generation for HTML rendering
├── markdown/            # 17 text format parsers (one dir each)
├── todotxt/
├── csv/ latex/ orgmode/ plaintext/ wikitext/ asciidoc/
├── restructuredtext/ rmarkdown/ taskpaper/ textile/
├── creole/ tiddlywiki/ jupyter/ keyvalue/ binary/
├── dropbox/             # Cloud storage protocols
├── googledrive/
├── onedrive/
├── ftp/                 # Network protocols
└── sftp/
model/                   # Document model (Document.kt)
network/                 # Network storage system
├── NetworkStorageService.kt
├── auth/                # Authentication
├── common/              # Shared network utilities
├── config/              # Network configuration
├── database/            # Metadata storage
├── platform/            # Platform-specific networking
├── protocol/            # Protocol abstractions
└── protocols/           # Protocol implementations
ui/                      # Shared UI (Compose Multiplatform)
├── Theme.kt
├── Accessibility.kt
└── Animations.kt
util/                    # Utilities
├── LazyLoading.kt
└── RateLimiting.kt
```

### Text Parsing Pipeline

1. **Detection** — `FormatRegistry.detectByExtension()` or `detectByContent()` using regex patterns defined in each `TextFormat`
2. **Parsing** — Format-specific parser produces a `ParsedDocument` (raw content + parsed content + metadata + errors)
3. **HTML generation** — `ParsedDocument.toHtml()` with lazy caching (first call generates, subsequent calls return cached)
4. **Styling** — `StyleSheets.kt` generates CSS for light/dark themes

Format IDs are string constants on `TextFormat.Companion` (e.g., `TextFormat.ID_MARKDOWN`, `TextFormat.ID_TODOTXT`).

### Test Organization

Tests live in `shared/src/commonTest/kotlin/digital/vasic/yole/format/`:

```
├── [format]/            # Per-format test directories (mirrors source)
├── integration/         # Cross-format integration tests
├── stress/              # Performance and stress tests
├── supremacy/           # Edge case and boundary tests
└── FormatRegistryStressTest.kt
```

~9,100+ tests across ~192 test files (commonTest + desktopTest + androidUnitTest + wasmJsTest).

Test types include: unit, integration, stress, supremacy/edge-case, mock HTTP, property-based, contract, security, performance metrics, monitoring, resilience, fuzz, snapshot, load, E2E, accessibility, and non-blocking tests.

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

## Resilience Patterns

The network protocol layer includes these resilience mechanisms:

- **CircuitBreaker** (`network/common/CircuitBreaker.kt`) — CLOSED/OPEN/HALF_OPEN states, configurable failure threshold and reset timeout
- **ConnectionLimiter** (`network/common/ConnectionLimiter.kt`) — Semaphore-based concurrent connection limiting, non-blocking
- **DocumentCache** (`format/DocumentCache.kt`) — LRU cache for ParsedDocument with hit/miss tracking
- **CancellationException safety** — All catch blocks in all 8 protocol services rethrow CancellationException
- **Path traversal protection** — normalizePath() resolves `..` segments and enforces root boundary
- **Query injection protection** — API query strings are sanitized (single-quote escaping, URL encoding, JSON escaping)
- **CoroutineScope lifecycle** — serviceScope cancelled on reconnect/disconnect to prevent coroutine leaks

## Security Scanning

```bash
# Start SonarQube locally
docker compose --profile security up -d sonarqube

# Run Detekt static analysis
./gradlew detekt

# Run security scan script
./scripts/run_security_scan.sh

# Full security stack via Docker
docker compose --profile full up -d
```

Tools configured: SonarQube (Docker), Snyk (CI + Docker), CodeQL (CI), Gitleaks (CI), Detekt (Gradle), OWASP Dependency Check (CI).

## Key Dependencies

- Kotlin 2.0.20, Compose Multiplatform 1.7.3
- Flexmark 0.64.8 (Markdown parsing, with 16+ extensions)
- Ktor Client 3.0.2 (networking)
- Kotlinx: Coroutines 1.9.0, Serialization 1.7.3, DateTime 0.6.1
- Okio 3.9.1 (file system)
- Testing: Kotest 5.9.1, MockK 1.13.13, AssertJ 3.26.3
- Coverage: Kover 0.8.3
- Version catalog: `gradle/libs.versions.toml`

## Key Files

- `shared/build.gradle.kts` — KMP configuration with all platform targets
- `settings.gradle.kts` — Module includes + 10 composite build `includeBuild()` directives for extracted KMP modules
- `gradle/libs.versions.toml` — Centralized dependency versions
- `Makefile` — Legacy Android build automation (requires `ANDROID_SDK_ROOT`)
- `docker-compose.yml` — Podman/Docker container build environment
- `docker/scripts/test-all.sh` — Comprehensive multi-platform test runner
- `docker/scripts/build.sh` — Container build script
- `Challenges/` — Go testing framework submodule (cross-platform challenges)
- `Containers/` — Go container orchestration submodule (Challenges dependency)
