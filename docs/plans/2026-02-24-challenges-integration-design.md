# Challenges Integration Design

**Date:** 2026-02-24
**Status:** Approved

## Problem Statement

The Yole Android app crashes on launch due to an unguarded `Color.parseColor("")` call in `Theme.kt:217`. Other apps and services likely have similar untested initialization paths. There is no automated end-to-end user flow testing across any platform. We need comprehensive coverage that simulates real user behavior.

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Orchestration | Go Challenges module as orchestrator | Decoupled from Kotlin, drives any technology |
| Android testing | Robolectric + UI Automator | Fast JVM tests + real device flows |
| Desktop/Web testing | Playwright (via playwright-go) | Modern, cross-browser, Go-native bindings |
| Reporting | Challenges as single source of truth | Unified dashboard for all test types |
| Challenge organization | Layered: common + platform-specific | Maximizes reuse, avoids duplication |

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│              Go Challenges Orchestrator                  │
│  (Single source of truth for all test reporting)        │
├─────────────┬──────────────┬──────────────┬─────────────┤
│  Common     │  Android     │  Desktop     │  Web        │
│  Challenges │  Challenges  │  Challenges  │  Challenges │
│  (shared    │  (Robolectric│  (Playwright)│  (Playwright│
│   flows)    │  +UIAutomator│              │   -go)      │
└──────┬──────┴──────┬───────┴──────┬───────┴──────┬──────┘
       │             │              │              │
       │        ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
       │        │ Android │   │ Desktop │   │  Wasm   │
       │        │   App   │   │   App   │   │   App   │
       │        │(ADB/    │   │(JVM     │   │(Browser)│
       │        │Emulator)│   │Process) │   │         │
       │        └─────────┘   └─────────┘   └─────────┘
       │
  ┌────▼──────────────────────────────┐
  │  Gradle Test Runner (existing)    │
  │  - Unit tests (Kotest/MockK)      │
  │  - Results consumed by Challenges │
  └───────────────────────────────────┘
```

## Git Submodule Integration

The `vasic-digital/Challenges` repository is added as a Git submodule at `Challenges/` in the project root. This provides the core framework (interfaces, registry, runner, reporting, assertions). Yole-specific challenge implementations live in a separate `challenges/` directory.

## Directory Structure

```
Yole/
├── Challenges/                    # Git submodule (framework)
│   └── pkg/
│       ├── challenge/             # Challenge interface, BaseChallenge
│       ├── registry/              # Registration + dependency ordering
│       ├── runner/                # Execution orchestration
│       ├── assertion/             # 16 built-in evaluators
│       ├── report/                # Markdown/JSON/HTML reporting
│       ├── monitor/               # WebSocket dashboard
│       ├── metrics/               # Prometheus metrics
│       └── ...
├── challenges/                    # Yole-specific challenges
│   ├── go.mod                     # Go module depending on Challenges
│   ├── go.sum
│   ├── main.go                    # CLI entry point
│   ├── common/                    # Platform-agnostic flows
│   │   ├── app_launch.go          # App launches without crash
│   │   ├── file_open.go           # Open files of each format
│   │   ├── file_edit.go           # Edit and save documents
│   │   ├── format_detection.go    # Auto-detect format by extension/content
│   │   ├── format_rendering.go    # Parse and render each format
│   │   ├── theme_switching.go     # Light/dark theme toggle
│   │   ├── settings.go            # Settings modification flows
│   │   └── ...
│   ├── android/                   # Android-specific challenges
│   │   ├── robolectric/           # Fast JVM-based UI tests
│   │   │   ├── launch.go          # Orchestrates Robolectric test execution
│   │   │   └── ...
│   │   ├── uiautomator/           # Device-level flows
│   │   │   ├── launch.go          # Orchestrates UI Automator via ADB
│   │   │   └── ...
│   │   ├── backup_restore.go      # Backup/restore flow
│   │   ├── pdf_export.go          # PDF export flow
│   │   └── permissions.go         # Runtime permissions
│   ├── desktop/                   # Desktop-specific challenges
│   │   ├── window_management.go   # Window state, resize, minimize
│   │   ├── file_dialog.go         # File open/save dialogs
│   │   ├── drag_drop.go           # Drag-and-drop files
│   │   ├── keyboard_shortcuts.go  # Menu bar + shortcuts
│   │   ├── system_tray.go         # Tray integration
│   │   └── ...
│   ├── web/                       # Web/Wasm-specific challenges
│   │   ├── pwa_install.go         # PWA installation flow
│   │   ├── browser_compat.go      # Cross-browser testing
│   │   ├── offline.go             # Offline functionality
│   │   └── ...
│   ├── infra/                     # Infrastructure challenges
│   │   ├── gradle_build.go        # Build verification
│   │   ├── gradle_tests.go        # Existing test execution + result collection
│   │   ├── docker_build.go        # Container build verification
│   │   └── lint.go                # Lint and static analysis
│   ├── adapters/                  # Framework adapters
│   │   ├── gradle.go              # Gradle invocation + JUnit XML parsing
│   │   ├── adb.go                 # ADB device management
│   │   ├── playwright.go          # Playwright-go wrapper
│   │   └── process.go             # JVM process management
│   └── testdata/                  # Sample files for testing
│       ├── sample.md
│       ├── sample.txt
│       ├── sample.csv
│       └── ...
```

## Challenge Layers

### Common Challenges (`challenges/common/`)
Platform-agnostic user flows parameterized by a platform adapter interface:

```go
type PlatformAdapter interface {
    Launch(ctx context.Context) error
    OpenFile(ctx context.Context, path string) error
    GetDisplayedContent(ctx context.Context) (string, error)
    EditContent(ctx context.Context, content string) error
    SaveFile(ctx context.Context) error
    NavigateToSettings(ctx context.Context) error
    SetSetting(ctx context.Context, key, value string) error
    SwitchTheme(ctx context.Context, theme string) error
    TakeScreenshot(ctx context.Context) ([]byte, error)
    Close(ctx context.Context) error
}
```

Each platform implements this interface, and common challenges use it to run the same flow everywhere.

### Android Challenges (`challenges/android/`)

**Robolectric layer:**
- Go orchestrator invokes `./gradlew :androidApp:testDebugUnitTest --tests "...Robolectric..."`
- Robolectric test classes live in `androidApp/src/test/` (JVM tests, no device needed)
- Tests use Robolectric's `@Config` annotation for API level simulation
- Results collected from JUnit XML and fed to Challenges reporting

**UI Automator layer:**
- Go orchestrator manages ADB: install APK, launch UI Automator tests
- UI Automator test classes live in `androidApp/src/androidTest/`
- Tests use `UiDevice`, `UiSelector` for real device interaction
- Go orchestrator controls emulator lifecycle (start, snapshot, teardown)

### Desktop Challenges (`challenges/desktop/`)
- Go orchestrator builds and launches desktop JAR via `java -jar`
- Playwright-go connects to the running application
- Challenges drive window management, file operations, keyboard shortcuts
- Process management handles app lifecycle

### Web Challenges (`challenges/web/`)
- Go orchestrator starts local HTTP server serving Wasm build
- Playwright-go launches browser and navigates to app
- Challenges test PWA features, cross-browser compatibility, offline mode
- Tests run against Chromium, Firefox, and WebKit

### Infrastructure Challenges (`challenges/infra/`)
- Build verification: all Gradle tasks succeed
- Existing test collection: run `./gradlew test`, parse JUnit XML, feed to Challenges
- Docker build verification
- Lint and security scan results

## Execution Flow

1. `go run ./challenges/ --all` (or specific platform/flow)
2. Infrastructure challenges run first (build, existing tests)
3. Common challenges run per-platform via adapters
4. Platform-specific challenges run
5. All results aggregated in Challenges reporting
6. WebSocket dashboard shows live progress
7. Final reports generated (Markdown/JSON/HTML)
8. Prometheus metrics exported

## Bug Fixes Required

### Critical: Theme.kt crash (Theme.kt:217)
```kotlin
// Before (crashes on empty string):
val seedColor = seedColorHex?.let { Color(android.graphics.Color.parseColor(it)) }

// After (guards against empty string):
val seedColor = seedColorHex?.takeIf { it.isNotEmpty() }?.let {
    Color(android.graphics.Color.parseColor(it))
}
```

### Secondary: Parser initialization error handling
Wrap `ParserInitializer.registerAllParsersLazy()` in try-catch in `YoleApp.kt`.

### Secondary: YoleSettings type safety
Make `getCustomSeedColor()` return null instead of empty string for unset values.

## Challenges Module Extensions

The Challenges submodule needs these extensions to support UI testing orchestration:

1. **Platform adapter interface** in `pkg/challenge/` — generic `PlatformAdapter` for driving apps
2. **Playwright integration** in `pkg/playwright/` — wrapper around playwright-go for browser/desktop automation
3. **ADB integration** in `pkg/adb/` — Android device management utilities
4. **Gradle integration** in `pkg/gradle/` — Gradle task execution + JUnit XML result parsing
5. **Process management** in `pkg/process/` — JVM/native process lifecycle management
6. **Screenshot comparison** in `pkg/assertion/` — Visual regression assertion evaluator

## Test Coverage Map

| User Flow | Android (Robolectric) | Android (UIAutomator) | Desktop (Playwright) | Web (Playwright) |
|-----------|----------------------|----------------------|---------------------|-----------------|
| App launch | Y | Y | Y | Y |
| File open (per format) | Y | Y | Y | Y |
| File edit + save | Y | Y | Y | Y |
| Format auto-detection | Y | - | Y | Y |
| Theme switching | Y | Y | Y | Y |
| Settings modification | Y | Y | Y | Y |
| Backup/restore | - | Y | - | - |
| PDF export | - | Y | - | - |
| Window management | - | - | Y | - |
| Drag-and-drop | - | - | Y | - |
| Keyboard shortcuts | - | - | Y | Y |
| System tray | - | - | Y | - |
| PWA install | - | - | - | Y |
| Offline mode | - | - | - | Y |
| Cross-browser | - | - | - | Y |

## Reusability

The design is generic for use across projects:
- The `PlatformAdapter` interface works for any app, not just Yole
- Playwright/ADB/Gradle adapters are project-agnostic
- Common challenge patterns (launch, file operations, settings) are parameterized
- The Challenges submodule provides the universal framework
- Only `challenges/` content is project-specific
