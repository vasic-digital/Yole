# Yole Challenges

Automated cross-platform testing orchestrator for Yole, built on the
[Challenges](https://github.com/vasic-digital/Challenges) framework.

## Architecture

```
challenges/           Go orchestrator module
├── main.go           CLI entry point
├── adapters/         Platform abstraction layer
│   ├── platform.go   PlatformAdapter interface
│   ├── gradle.go     Gradle task runner + JUnit XML parser
│   ├── adb.go        Android device management via ADB
│   ├── playwright.go Playwright browser automation
│   └── process.go    JVM process lifecycle
├── infra/            Infrastructure challenges
│   ├── gradle_build.go   Compile all modules
│   ├── gradle_tests.go   Run all test suites
│   └── lint.go           Lint + static analysis
├── android/          Android challenges
│   ├── robolectric_launch.go  JVM-based launch test
│   ├── robolectric_flows.go   All Robolectric user flows
│   └── uiautomator_launch.go  Device/emulator launch
├── desktop/          Desktop challenges
│   ├── launch.go     JVM desktop app launch
│   └── user_flows.go Desktop test suite
├── web/              Web challenges
│   ├── launch.go     Wasm build + Playwright launch
│   └── user_flows.go Browser-based flow tests
├── common/           Platform-agnostic challenges
│   ├── app_launch.go        Generic launch via PlatformAdapter
│   └── format_rendering.go  Multi-format file rendering
└── testdata/         Sample files for format testing
```

## Dependency Graph

```
infra-gradle-build
├── infra-gradle-tests
├── infra-lint
├── android-robolectric-launch
│   └── android-robolectric-flows
├── android-uiautomator-launch (requires ADB device)
├── desktop-launch
│   └── desktop-user-flows
└── web-launch
    └── web-user-flows
```

## Prerequisites

- Go 1.24+
- Docker/Podman (for container-based builds)
- ADB (for Android device tests, optional)
- Node.js + Playwright (`npx playwright install`, for web tests)

## Usage

Run from the `challenges/` directory:

```bash
# All platforms
go run . --platform=all

# Single platform
go run . --platform=android
go run . --platform=desktop
go run . --platform=web

# With Docker for Gradle tasks
go run . --platform=all --docker

# Custom timeout and report format
go run . --platform=all --timeout=1h --report=json --output=reports
```

### CLI Flags

| Flag | Default | Description |
|------|---------|-------------|
| `--platform` | `all` | Platform to test: `android`, `desktop`, `web`, `all` |
| `--report` | `markdown` | Report format: `markdown`, `json`, `html` |
| `--output` | `reports` | Output directory for reports |
| `--docker` | `false` | Run Gradle tasks in Docker containers |
| `--timeout` | `30m` | Global timeout for all challenges |

## Challenge Catalog

### Infrastructure (always run)

| ID | Description |
|----|-------------|
| `infra-gradle-build` | Compiles Android, Desktop, and Shared modules |
| `infra-gradle-tests` | Runs all unit/integration test suites |
| `infra-lint` | Android lint and Detekt static analysis |

### Android

| ID | Description |
|----|-------------|
| `android-robolectric-launch` | Robolectric app launch test |
| `android-robolectric-flows` | 9 Robolectric user flow test classes |
| `android-uiautomator-launch` | APK install + launch on device/emulator |

### Desktop

| ID | Description |
|----|-------------|
| `desktop-launch` | Build JAR + launch JVM app |
| `desktop-user-flows` | Desktop test suite via Gradle |

### Web

| ID | Description |
|----|-------------|
| `web-launch` | Wasm build + Playwright browser test |
| `web-user-flows` | Browser-based flow tests via Playwright |

## Adding New Challenges

1. Create a new Go file in the appropriate package (`infra/`, `android/`,
   `desktop/`, `web/`, or `common/`).
2. Embed `challenge.BaseChallenge` and initialize with `NewBaseChallenge()`.
3. Implement the `Execute(ctx context.Context) (*challenge.Result, error)` method.
4. Register the challenge in the appropriate `register*Challenges()` function
   in `main.go`.

### Example

```go
type MyChallenge struct {
    challenge.BaseChallenge
}

func NewMyChallenge() *MyChallenge {
    return &MyChallenge{
        BaseChallenge: challenge.NewBaseChallenge(
            "my-challenge-id",
            "My Challenge Name",
            "Description of what this challenge tests",
            "category",
            []challenge.ID{"dependency-id"},  // or nil
        ),
    }
}

func (c *MyChallenge) Execute(
    ctx context.Context,
) (*challenge.Result, error) {
    start := time.Now()
    outputs := make(map[string]string)
    metrics := make(map[string]challenge.MetricValue)
    var assertions []challenge.AssertionResult

    // ... run tests, collect results ...

    return c.CreateResult(
        challenge.StatusPassed, start, assertions,
        metrics, outputs, "",
    ), nil
}
```

## Extending to Other Projects

The `PlatformAdapter` interface in `adapters/platform.go` defines a generic
contract for driving any application. Implement this interface for your
platform and use the `common/` challenges for platform-agnostic testing.

## Reports

Reports are written to the `--output` directory (default: `reports/`).
Supported formats:

- **Markdown** (`results.md`) — human-readable summary
- **JSON** (`results.json`) — machine-readable for CI/CD pipelines
- **HTML** (`results.html`) — visual report with styling
