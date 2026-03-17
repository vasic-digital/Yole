# Yole Quick Start Guide

**Version**: 2.0
**Date**: 2026-03-17

Yole is a **cross-platform text editor** supporting Android, Desktop (Windows/macOS/Linux), iOS, and Web with **17 text formats** and **8 storage protocols**.

---

## Platform Setup

### Android

```bash
# Build and install debug APK
./gradlew :androidApp:assembleFlavorDefaultDebug
adb install androidApp/build/outputs/apk/flavorDefault/debug/androidApp-flavorDefault-debug.apk

# Or install directly
./gradlew :androidApp:installFlavorDefaultDebug
```

### Desktop (Windows/macOS/Linux)

```bash
# Run desktop app
./gradlew :desktopApp:run

# Build distributable for your OS
./gradlew :desktopApp:packageDistributionForCurrentOS
```

### Web (Wasm PWA)

```bash
# Start development server
./gradlew :webApp:wasmJsBrowserRun

# Build for production
./gradlew :webApp:wasmJsBrowserDistribution
```

### iOS

Open `iosApp/iosApp.xcodeproj` in Xcode and build from there.

---

## Container Setup

Release builds and full CI test suites must be executed inside Docker/Podman containers.

### Prerequisites

- Docker or Podman installed
- At least 4 GB RAM allocated to containers

### Build the Container

```bash
# Build the build container image
docker compose build build

# On ALT Linux with Podman
podman compose build build
```

### Run in Container

```bash
# Run all tests
docker compose run --rm build ./docker/scripts/test-all.sh

# Build releases
docker compose run --rm build ./docker/scripts/build.sh

# Run specific Gradle command
docker compose run --rm build ./gradlew :shared:desktopTest
```

### Troubleshooting Container Issues

- **Exit code 137**: OOM kill. Increase container memory limits in `docker-compose.yml`
- **Slow builds**: Enable Gradle caching by mounting a volume for `~/.gradle`
- **Permission errors**: Ensure the user inside the container matches your UID/GID

---

## Test Execution

### Quick Test (No Android SDK needed)

```bash
# Run shared module tests on JVM (primary dev command)
./gradlew :shared:desktopTest

# Makefile shortcut
make test-shared
```

### Single Test Class

```bash
./gradlew :shared:desktopTest --tests "digital.vasic.yole.format.markdown.MarkdownParserTest"
```

### Single Test Method

```bash
./gradlew :shared:desktopTest --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests.ParseQuery"
```

### All Tests (Requires Android SDK)

```bash
./gradlew test

# With coverage report
./gradlew test koverHtmlReport
# Report at: shared/build/reports/kover/html/index.html
```

### Stress Tests

```bash
./gradlew :shared:desktopTest --tests "digital.vasic.yole.format.stress.*"
```

### Challenge Framework

```bash
# Run Go-based challenges
./gradlew runChallenges

# Requires Go 1.24+ and initialized submodules
git submodule update --init --recursive
```

### Test Count

The project has **9,400+ test methods** across **~215 test files** covering 16 test types.

---

## Security Scanning

### Start Local SonarQube

```bash
# Start SonarQube (Docker required)
docker compose --profile security up -d sonarqube
# Access at http://localhost:9000 (admin/admin)

# Run analysis
docker compose run --rm build ./gradlew sonar \
    -Dsonar.host.url=http://sonarqube:9000 \
    -Dsonar.token=YOUR_TOKEN_HERE
```

### Run Static Analysis

```bash
# Kotlin static analysis
./gradlew detekt

# Android lint
./gradlew lintFlavorDefaultDebug
```

### Full Security Stack

```bash
# Start all security services
docker compose --profile security up -d

# Or the full stack (build + security)
docker compose --profile full up -d
```

### Secret Detection

```bash
# Scan for committed secrets
gitleaks detect --source . --verbose

# Pre-commit hook
gitleaks protect --staged --verbose
```

---

## Supported Formats

| Category | Formats |
|----------|---------|
| Core | Markdown, Plain Text, Todo.txt, CSV |
| Technical | LaTeX, AsciiDoc, Org Mode, reStructuredText |
| Wiki | WikiText, Creole, TiddlyWiki |
| Specialized | Key-Value, TaskPaper, Textile |
| Data Science | Jupyter, R Markdown |
| Other | Binary Detection |

---

## Key Build Commands

```bash
# Development
./gradlew :desktopApp:run                        # Run desktop app
./gradlew :androidApp:assembleFlavorDefaultDebug  # Build Android APK
./gradlew :webApp:wasmJsBrowserRun               # Run web app

# Testing
./gradlew :shared:desktopTest                    # Quick tests
./gradlew test koverHtmlReport                    # All tests + coverage

# Quality
./gradlew detekt                                  # Static analysis
./gradlew lintFlavorDefaultDebug                 # Android lint

# Documentation
./gradlew :shared:dokkaHtml                      # API docs

# Makefile shortcuts
make test-shared    # = :shared:desktopTest
make desktop        # = :desktopApp:run
make build          # = :androidApp:assembleFlavorDefaultDebug
make help           # Show all available targets
```

---

## Documentation

| Document | Purpose |
|----------|---------|
| `ARCHITECTURE.md` | System architecture and module structure |
| `CLAUDE.md` | AI agent guidance |
| `AGENTS.md` | Development guide for AI agents |
| `TESTING_GUIDELINES.md` | Testing patterns and conventions |
| `TESTING_STRATEGY.md` | Overall testing strategy |
| `FORMAT_DOCUMENTATION.md` | All 17 format parser documentation |
| `docs/user-guide/` | User manuals (Android, Desktop, Web) |
| `docs/CONCURRENCY_SAFETY.md` | Concurrency patterns |
| `docs/LOCK_ORDERING.md` | Mutex lock ordering convention |
| `docs/SECURITY_SCANNING.md` | Security tool setup and usage |
| `docs/PERFORMANCE_TUNING.md` | Performance tuning guide |

---

## Getting Help

- Check `TROUBLESHOOTING.md` for common issues
- Review `CHANGELOG.md` for recent changes
- Open an issue on GitHub for bugs or feature requests
- See `CONTRIBUTING.md` for contribution guidelines

---

**Last Updated**: 2026-03-17
