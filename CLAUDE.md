# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Yole** is a cross-platform text editor supporting 17+ text formats (Markdown, todo.txt, CSV, LaTeX, Org Mode, etc.) built with Kotlin Multiplatform (KMP). The app is offline-first with optional cloud storage integration.

**Package namespace:** `digital.vasic.yole.*` (legacy: `net.gsantner.opoc.*`)

## Build Commands

```bash
# Android (production)
./gradlew :androidApp:assembleDebug        # or: make build

# Desktop (beta)
./gradlew :desktopApp:run                  # or: make desktop

# Web (Wasm)
./gradlew :webApp:wasmJsBrowserRun         # or: make web

# iOS
# Open iosApp/iosApp.xcodeproj in Xcode

# Testing
./gradlew test                             # All tests
./gradlew test --tests "digital.vasic.yole.format.todotxt.TodoTxtQuerySyntaxTests.ParseQuery"  # Single test
./gradlew test koverHtmlReport             # Tests with coverage

# Other
./gradlew lintFlavorDefaultDebug           # Lint
./gradlew :shared:dokkaHtml                # API docs
./gradlew clean                            # Clean
make all install run                       # Build, install, run on Android device
```

## Architecture

This is a **Kotlin Multiplatform (KMP)** project. All shared business logic lives in the `shared` module.

```
shared/src/commonMain/kotlin/digital/vasic/yole/
├── format/                    # Format system (17 text formats + cloud storage protocols)
│   ├── FormatRegistry.kt      # Central format registry
│   ├── TextFormat.kt          # Format metadata
│   ├── TextParser.kt          # Parser interface
│   ├── markdown/              # Markdown, todotxt, csv, latex, orgmode, etc.
│   ├── dropbox/               # Cloud: Dropbox, Google Drive, OneDrive
│   └── ftp/                   # Network: FTP, SFTP, WebDAV
└── model/                     # Document model
```

**Platform apps** (`androidApp/`, `desktopApp/`, `iosApp/`, `webApp/`) depend on `shared` for business logic.

**Legacy modules** (`app/`, `core/`, `commons/`) are Android-specific and being phased out:
- `commons/` - Android utilities (`GsFileUtils`, `GsContextUtils`)
- `core/` - Third-party encryption code (`JavaPasswordbasedCryption.java`)

### Text Parsing Pipeline

1. **Detection** → `TextFormat.detectFormat(content, extension)`
2. **Parsing** → `TextParser.parse(content)`
3. **Rendering** → Platform-specific HTML converters
4. **Highlighting** → Platform-specific syntax highlighters

## Platform Status

| Platform | Status | Notes |
|----------|--------|-------|
| Android | ✅ Production | Fully functional |
| Desktop | ⚠️ Beta | ~30% complete |
| iOS | 🚧 Development | Targets enabled in `shared/build.gradle.kts` |
| Web | 🚧 Stub | Build config only, no source |

## Adding New Formats

1. Create parser in `shared/src/commonMain/kotlin/digital/vasic/yole/format/[name]/`
2. Implement `TextParser` interface
3. Register in `FormatRegistry.kt`
4. Add tests in `shared/src/commonTest/kotlin/`
5. Add platform-specific code in `androidMain/`, `desktopMain/`, etc. if needed

## Code Conventions

- **Kotlin** primary, Java for legacy
- **Test classes** end with `Tests` or `Test`
- **File headers**: SPDX license header (Apache-2.0, CC0-1.0, or Unlicense)
- **Build variants**: `flavorDefault` for dev, `flavorAtest` for testing
- All tests must pass before merging

## Key Files

- `shared/build.gradle.kts` - KMP configuration with platform targets
- `settings.gradle.kts` - Module includes
- `gradle/libs.versions.toml` - Dependency versions
- `Makefile` - Build automation
- `run_all_tests.sh` - Comprehensive test runner
