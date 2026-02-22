# Yole - Development Guide for AI Agents

## MANDATORY: Build and Test in Containers

**ALL builds and tests MUST be executed inside Docker/Podman containers, NOT directly on the host machine!**

This is required to ensure:
- Consistent environment across all platforms
- Proper dependency management
- Reproducible builds
- Proper Android emulator access
- All integration services available

```bash
# Build the container first
docker compose build build

# Run tests inside container
docker compose run --rm build ./docker/scripts/test-all.sh

# Build releases inside container
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

**Yole** is a cross-platform text editor built with Kotlin Multiplatform (KMP), supporting Android (production), Desktop (beta), iOS/Web (development). Supports 17 text formats.

**Package namespace:** `digital.vasic.yole.*` (legacy: `net.gsantner.opoc.*`)

## Build Commands

```bash
# Android
./gradlew :androidApp:assembleDebug
make build

# Desktop  
./gradlew :desktopApp:run
make desktop

# Web (WASM)
./gradlew :webApp:wasmJsBrowserRun
make web

# Testing
./gradlew test                                    # All tests
./gradlew test --tests "full.TestClassName.testName"  # Single test
./gradlew test koverHtmlReport                   # With coverage

# Lint & Clean
./gradlew lintFlavorDefaultDebug
./gradlew clean

# API Docs
./gradlew :shared:dokkaHtml

# Benchmarks
./gradlew :shared:runSimpleBenchmarks
```

## Architecture

- **Shared module** (`shared/src/commonMain/kotlin/digital/vasic/yole/`) - All business logic
- **Platform apps** (`androidApp/`, `desktopApp/`, `iosApp/`, `webApp/`) - UI shells
- **Legacy** (`app/`, `core/`, `commons/`) - Android-specific, being phased out

```
shared/src/commonMain/kotlin/digital/vasic/yole/
├── format/          # 17 format parsers + registry
├── network/          # Cloud protocols (Dropbox, FTP, SFTP, etc.)
├── model/           # Document representation
└── ui/              # Shared Compose components
```

## Code Style Guidelines

### Language Standards
- **Kotlin** primary; Java only for legacy code
- **Java 11+** compatibility required
- Use `expect/actual` pattern for platform-specific code

### Naming Conventions
- **Classes**: PascalCase (`TextFormat`, `MarkdownParser`)
- **Functions**: camelCase (`parseContent()`, `detectFormat()`)
- **Variables**: camelCase (`documentContent`, `formatRegistry`)
- **Constants**: UPPER_SNAKE_CASE (`ID_MARKDOWN`, `EXTENSION_MD`)
- **Test classes**: End with `Tests` or `Test`

### File Headers
```kotlin
/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Brief description
 *########################################################*/
package digital.vasic.yole.format
```

### Import Ordering
1. Kotlin standard library (`kotlin.*`)
2. Third-party libraries (`androidx.*`, `com.*`, `org.*`)
3. Project imports (`digital.vasic.yole.*`)

### Code Organization
- **KDoc** required for all public APIs with examples
- **Group** related functionality
- Use **data classes** for simple data holders
- Leverage Kotlin **null safety** features

### Error Handling
- Use specific exception types
- Provide meaningful error messages
- Fail fast with validation at boundaries

## Adding New Formats

1. Create parser in `shared/src/commonMain/kotlin/digital/vasic/yole/format/[name]/`
2. Implement `TextParser` interface
3. Register in `FormatRegistry.kt`
4. Add tests in `shared/src/commonTest/kotlin/`

## Key Files

- `shared/build.gradle.kts` - KMP configuration
- `gradle/libs.versions.toml` - Dependency versions
- `run_all_tests.sh` - Comprehensive test runner

## Quality Requirements

- All tests must pass
- Minimum 70% code coverage (enforced by Kover)
- No lint violations
- SPDX headers required on all new files

## Security Scanning

Free security scanning is integrated via GitHub Actions:

```bash
# Start local SonarQube (requires Docker/Podman)
docker compose up -d sonarqube

# SonarQube available at: http://localhost:9000
# Default credentials: admin/admin
```

### Security Tools Configured
- **Snyk**: Free tier for vulnerability scanning (set SNYK_TOKEN in repo secrets)
- **SonarQube**: Community edition via Docker Compose
- **CodeQL**: GitHub native security analysis
- **OWASP Dependency Check**: Gradle plugin
- **Gitleaks**: Secret scanning

## Docker Services

```bash
# Start all services
docker compose up -d

# Start specific service
docker compose up -d sonarqube

# Full security stack
docker compose up -d --profile full
```
