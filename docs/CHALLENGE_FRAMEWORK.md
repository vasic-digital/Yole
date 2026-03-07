# Challenge Framework Integration Guide

This document describes how Yole integrates with the `digital.vasic.challenges` Go module for structured cross-platform testing, how to run challenges, create new challenge banks, and map challenges to Kotlin tests.

## Overview

The **Challenges** framework (`Challenges/` submodule) is a generic Go module for defining, executing, and reporting on structured test scenarios. Yole uses it to validate cross-cutting concerns that span multiple subsystems: security posture, format parser robustness, protocol resilience, performance baselines, and end-to-end user flows.

Challenges complement Kotlin unit/integration tests by testing behaviors that are difficult to express in a single test class, such as verifying that all 17 format parsers handle empty input consistently, or that no protocol service swallows `CancellationException`.

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| Challenge framework | `Challenges/pkg/` | Core Go library (runner, registry, assertion engine, reporters) |
| Challenge banks | `Challenges/banks/yole/` | JSON files defining Yole-specific challenge scenarios |
| Bank loader | `Challenges/pkg/bank/` | Loads and validates challenge definitions from JSON files |
| Assertion engine | `Challenges/pkg/assertion/` | 16 built-in evaluators for verifying challenge outputs |
| Report generators | `Challenges/pkg/report/` | Markdown, JSON, and HTML report output |
| User flow automation | `Challenges/pkg/userflow/` | Multi-platform UI testing with browser, mobile, and desktop adapters |
| CLI runner | `Challenges/cmd/userflow-runner/` | Command-line tool for running user flow challenges |

## Prerequisites

- **Go 1.24+** installed
- Git submodules initialized: `git submodule update --init --recursive`
- The `Containers/` submodule must also be present (dependency of Challenges)

## Running Challenges

### Build and Verify the Framework

```bash
# From the project root
cd Challenges

# Build all packages
go build ./...

# Run framework unit tests
go test ./... -race -count=1

# Run short tests only
go test ./... -short

# Run benchmarks
go test -bench=. ./tests/benchmark/
```

### Run Challenges Programmatically

Challenges are executed via Go code that loads bank definitions, registers them with the runner, and executes:

```go
package main

import (
    "context"
    "fmt"
    "time"

    "digital.vasic.challenges/pkg/bank"
    "digital.vasic.challenges/pkg/challenge"
    "digital.vasic.challenges/pkg/registry"
    "digital.vasic.challenges/pkg/runner"
    "digital.vasic.challenges/pkg/report"
)

func main() {
    ctx := context.Background()

    // Load challenge banks
    b := bank.New()
    if err := b.LoadDir("banks/yole"); err != nil {
        panic(err)
    }

    // Create registry and register challenges
    reg := registry.NewRegistry()
    for _, def := range b.All() {
        // Create challenge implementations from definitions
        // (application-specific mapping)
        reg.Register(createChallengeFromDefinition(def))
    }

    // Create runner
    r := runner.NewRunner(
        runner.WithRegistry(reg),
        runner.WithTimeout(10 * time.Minute),
        runner.WithResultsDir("./results"),
    )

    // Run all challenges in dependency order
    results, err := r.RunAll(ctx, &challenge.Config{
        Verbose: true,
    })
    if err != nil {
        panic(err)
    }

    // Generate reports
    mdReporter := report.NewMarkdownReporter("./reports")
    mdReporter.GenerateMasterSummary(results)

    htmlReporter := report.NewHTMLReporter("./reports")
    htmlReporter.GenerateMasterSummary(results)

    // Print summary
    for _, res := range results {
        fmt.Printf("%s: %s (%v)\n",
            res.ChallengeName, res.Status, res.Duration)
    }
}
```

### Run by Category

```go
// Load only security challenges
securityDefs := b.ByCategory("security")

// Run a specific sequence
results, err := r.RunSequence(ctx,
    []challenge.ID{"yole-secdp-path-traversal-normalization", "yole-secdp-xss-html-output"},
    config,
)

// Run independent challenges in parallel
results, err := r.RunParallel(ctx,
    []challenge.ID{"yole-fedge-empty-file-all-formats", "yole-fedge-binary-detection"},
    config,
    4, // max concurrency
)
```

### Run User Flow Challenges via CLI

```bash
cd Challenges
go run ./cmd/userflow-runner/ \
    --platform browser \
    --report html \
    --root /path/to/yole \
    --timeout 10m \
    --output ./results \
    --verbose
```

## Challenge Bank Structure

Challenge banks are JSON files in `Challenges/banks/yole/`. Each file defines a collection of related challenges.

### File Format

```json
{
  "version": "1.0",
  "name": "Human-readable bank name",
  "metadata": {
    "project": "yole",
    "domain": "category-name",
    "description": "What this bank validates"
  },
  "challenges": [
    {
      "id": "yole-prefix-descriptive-id",
      "name": "Human-Readable Challenge Name",
      "description": "Detailed description of what this challenge verifies and how",
      "category": "category-name",
      "dependencies": ["yole-prefix-dependency-id"],
      "estimated_duration": "30s",
      "inputs": [
        {"name": "param_name", "source": "config", "required": true},
        {"name": "project_root", "source": "env", "required": true}
      ],
      "outputs": [
        {"name": "result_name", "type": "json", "description": "What this output represents"}
      ],
      "assertions": [
        {"type": "exact_count", "target": "result_name", "value": 0, "message": "Failure explanation"}
      ],
      "metrics": ["metric_name_1", "metric_name_2"]
    }
  ]
}
```

### Field Reference

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | Yes | Unique identifier, prefix with `yole-` |
| `name` | string | Yes | Human-readable name |
| `description` | string | Yes | Detailed description of what is verified and how |
| `category` | string | Yes | Challenge category for grouping and filtering |
| `dependencies` | string[] | No | IDs of challenges that must pass before this one runs |
| `estimated_duration` | string | Yes | Expected execution time (e.g., "30s", "2m") |
| `inputs` | Input[] | Yes | Parameters required by the challenge |
| `outputs` | Output[] | Yes | Named outputs produced by execution |
| `assertions` | Assertion[] | Yes | Pass/fail criteria evaluated against outputs |
| `metrics` | string[] | No | Numeric metrics collected during execution |

### Input Sources

| Source | Description | Example |
|--------|-------------|---------|
| `config` | Runtime configuration value | `{"name": "format_ids", "source": "config"}` |
| `env` | Environment variable | `{"name": "project_root", "source": "env"}` |
| `dependency:<id>` | Output from a dependency | `{"name": "token", "source": "dependency:yole-net-dropbox-auth"}` |

### Assertion Types (Built-in Evaluators)

| Type | Description | Value Field |
|------|-------------|-------------|
| `not_empty` | Target is non-nil and non-empty | N/A |
| `contains` | String contains substring | Expected substring |
| `contains_any` | String contains any of the values | Array of strings |
| `exact_count` | Count matches exactly | Expected count |
| `min_count` | Count meets minimum | Minimum value |
| `min_length` | String length meets minimum | Minimum length |
| `max_latency` | Response time within limit (ms) | Maximum ms |
| `all_valid` | All array items are valid | N/A |
| `all_pass` | All sub-assertions pass | N/A |
| `no_duplicates` | No duplicate items | N/A |
| `quality_score` | Numeric score meets threshold | Threshold |
| `min_score` | Numeric minimum score | Minimum |
| `not_mock` | Response is not mocked | N/A |
| `no_mock_responses` | No mocked responses in array | N/A |
| `reasoning_present` | Contains reasoning indicators | N/A |
| `code_valid` | Contains valid code patterns | N/A |

## Existing Challenge Banks

The following challenge banks are defined in `Challenges/banks/yole/`:

| Bank File | Domain | Challenges | Purpose |
|-----------|--------|------------|---------|
| `format-parsing.json` | format-parsing | 17+ | Validates all 17 text format parsers |
| `format-detection.json` | format-detection | 8 | Validates FormatRegistry detection by extension, content, filename |
| `format-edge-cases-challenges.json` | format-edge-cases | 16 | Edge cases: empty files, max size, binary detection, corrupted headers, mixed encodings, nested formats |
| `network-protocols.json` | network | 10+ | Cloud storage and network protocol CRUD operations |
| `security.json` | security | 7 | XSS, path traversal, dependency scanning, TLS, credential handling, static analysis |
| `security-challenges.json` | security-deep | 16 | Deep security: path traversal normalization, XSS per format, query injection per protocol, CancellationException rethrow, credential storage, HTTPS enforcement, certificate validation, input sanitization |
| `resilience.json` | resilience | 5 | Circuit breaker, timeout, retry, graceful degradation, connection pool recovery |
| `protocol-resilience-challenges.json` | protocol-resilience | 15 | Protocol-specific resilience: circuit breaker state transitions, connection limiter, timeout enforcement, reconnection, concurrent ops, error cascading |
| `performance.json` | performance | 8+ | Parse time budgets, memory limits, throughput baselines |
| `concurrency.json` | concurrency | 5+ | Concurrent parsing, protocol connections, shared resource contention |
| `monitoring.json` | monitoring | 6+ | Metrics collection, performance baselines, memory tracking, cache monitoring |
| `memory.json` | memory | 5+ | Memory allocation, leak detection, GC pressure |
| `lazy-loading.json` | lazy-loading | 5+ | Lazy parser initialization, deferred loading verification |
| `test-coverage.json` | test-coverage | 5+ | Coverage thresholds, untested code detection |
| `ui-accessibility.json` | ui-accessibility | 5+ | Accessibility compliance, theme support |
| `cross-platform-build.json` | cross-platform | 5+ | Build verification across Android, Desktop, Web, iOS |
| `e2e-userflow.json` | e2e | 7+ | End-to-end user flows across platforms |

## Creating New Challenge Banks

### Step 1: Choose a Domain

Pick a domain that groups related verification concerns. Use lowercase with hyphens. Existing domains include `security`, `format-parsing`, `resilience`, `performance`, `concurrency`, `monitoring`.

### Step 2: Create the JSON File

Create a new file in `Challenges/banks/yole/` following the naming convention `<domain>.json` or `<domain>-challenges.json`:

```bash
# Example
touch Challenges/banks/yole/my-new-domain.json
```

### Step 3: Define Challenges

Follow these conventions:
- **ID prefix**: Always start with `yole-` followed by a short domain code (e.g., `yole-sec-`, `yole-fmt-`, `yole-res-`)
- **Descriptions**: Be specific about what is verified, how many items are tested, and what constitutes a pass
- **Assertions**: Define clear pass/fail criteria using the built-in evaluators
- **Dependencies**: Only add dependencies when challenge B genuinely requires output from challenge A
- **Metrics**: Include timing metrics (`*_time_ms`, `*_latency_ms`) and count metrics (`*_count`, `*_tested`)

### Step 4: Validate the Bank

```go
import "digital.vasic.challenges/pkg/bank"

b := bank.New()
err := b.LoadFile("banks/yole/my-new-domain.json")
if err != nil {
    // Fix JSON syntax or missing required fields
    log.Fatal(err)
}

// Verify all definitions loaded
fmt.Printf("Loaded %d challenges\n", b.Count())
```

The bank validator checks:
- Valid JSON syntax
- Non-empty challenge IDs
- Required fields present
- No duplicate IDs across loaded banks

### Step 5: Implement Challenge Executors

Each challenge definition needs a corresponding Go implementation that:
1. Reads inputs from config/env/dependencies
2. Executes the verification logic (often by invoking Gradle tasks, parsing source code, or running HTTP requests)
3. Produces the declared outputs
4. Returns metrics

## Architecture of the Challenge System

```
Challenges/
+-- banks/
|   +-- yole/                      # Yole-specific challenge definitions
|       +-- security.json
|       +-- format-parsing.json
|       +-- protocol-resilience-challenges.json
|       +-- ...
+-- cmd/
|   +-- userflow-runner/           # CLI for running user flow challenges
+-- pkg/
|   +-- assertion/                 # Assertion engine (16 evaluators)
|   +-- bank/                      # Challenge bank loader + validator
|   +-- challenge/                 # Core types: Challenge, Result, Definition
|   +-- container/                 # Bridge to Containers module
|   +-- env/                       # Environment variable handling
|   +-- httpclient/                # HTTP client with JWT auth
|   +-- infra/                     # Infrastructure bridge
|   +-- logging/                   # Structured logging (JSON, Console, Redacting)
|   +-- metrics/                   # Prometheus-compatible metrics
|   +-- monitor/                   # Live WebSocket monitoring dashboard
|   +-- panoptic/                  # Screen recording integration
|   +-- plugin/                    # Plugin system
|   +-- registry/                  # Challenge registration + dependency ordering
|   +-- report/                    # Report generation (Markdown, JSON, HTML)
|   +-- runner/                    # Execution engine (sequential, parallel, pipeline)
|   +-- userflow/                  # Multi-platform user flow automation
+-- Containers/                    # Go container orchestration (submodule)
+-- docs/                          # Framework documentation
```

### Execution Flow

```
1. Load bank files         bank.LoadDir("banks/yole/")
          |
2. Register challenges     registry.Register(challenge)
          |
3. Resolve dependencies    registry.TopologicalSort()  (Kahn's algorithm)
          |
4. Execute in order        runner.RunAll(ctx, config)
          |                   +-- Configure challenge
          |                   +-- Validate prerequisites
          |                   +-- Execute with timeout
          |                   +-- Evaluate assertions
          |
5. Generate reports        report.GenerateMasterSummary(results)
          |
6. Emit metrics            metrics.RecordChallengeResult(result)
```

### Progress-Based Liveness Detection

For long-running challenges (e.g., scanning large remote filesystems), the framework uses progress-based liveness detection instead of hard timeouts:

- Challenges call `ReportProgress(msg, data)` periodically to signal forward progress
- A liveness monitor watches the progress channel
- If no progress is reported within `StaleThreshold`, the challenge is cancelled as "stuck"
- This is distinct from "timed out" (hard timeout exceeded)

Configure per runner or per challenge:

```go
runner.NewRunner(
    runner.WithTimeout(72*time.Hour),          // Hard upper bound
    runner.WithStaleThreshold(5*time.Minute),  // Kill if no progress for 5 min
)
```

## CI Integration Guide

### GitHub Actions Integration

Add a challenge execution step to the CI workflow. Create or extend `.github/workflows/ci.yml`:

```yaml
  challenges:
    name: Run Challenge Banks
    runs-on: ubuntu-latest
    needs: build  # Run after Kotlin build/test passes

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          submodules: recursive

      - name: Set up Go
        uses: actions/setup-go@v5
        with:
          go-version: '1.24'

      - name: Build Challenges framework
        working-directory: Challenges
        run: go build ./...

      - name: Run Challenges framework tests
        working-directory: Challenges
        run: go test ./... -race -count=1

      - name: Validate challenge banks
        working-directory: Challenges
        run: |
          go run ./cmd/validate-banks/ --dir banks/yole/

      - name: Upload challenge reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: challenge-reports
          path: Challenges/reports/
          retention-days: 14
```

### Container-Based Execution

For comprehensive challenge execution with all dependencies (Android SDK, network services, emulators), use the Docker/Podman container environment:

```bash
# Build the container
docker compose build build

# Run challenges inside the container
docker compose run --rm build bash -c "
    cd Challenges && \
    go build ./... && \
    go test ./... -race -count=1
"
```

### Pre-commit Hook

Validate bank JSON syntax before committing:

```bash
#!/bin/bash
# .git/hooks/pre-commit (excerpt)
for f in Challenges/banks/yole/*.json; do
    python3 -m json.tool "$f" > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo "ERROR: Invalid JSON in $f"
        exit 1
    fi
done
```

## Mapping Between Challenges and Kotlin Tests

Challenges define *what* to verify at a cross-cutting level. Kotlin tests implement the *detailed* verification. The mapping is by domain:

### Security Domain

| Challenge ID | Kotlin Test File(s) |
|-------------|---------------------|
| `yole-secdp-path-traversal-normalization` | `SecurityValidationTests.kt` (path traversal tests) |
| `yole-secdp-xss-html-output` | `SecurityValidationTests.kt` (XSS prevention tests) |
| `yole-secdp-query-injection-gdrive` | `SecurityValidationTests.kt` (injection tests), `SafetyFixesTest.kt` |
| `yole-secdp-cancellation-rethrow` | `SafetyFixesTest.kt` (CancellationException rethrow verification) |
| `yole-secdp-credential-plaintext` | `SecurityValidationTests.kt` (credential storage tests) |
| `yole-secdp-scope-lifecycle` | `SafetyFixesTest.kt` (CoroutineScope lifecycle tests) |
| `yole-sec-xss-html-escape` | `SecurityValidationTests.kt`, per-format `*ParserTests.kt` |
| `yole-sec-static-analysis` | Detekt Gradle task (`./gradlew detekt`) |

### Format Edge Cases Domain

| Challenge ID | Kotlin Test File(s) |
|-------------|---------------------|
| `yole-fedge-empty-file-all-formats` | `PropertyBasedFormatTests.kt` (empty input tests) |
| `yole-fedge-max-file-size` | `ComprehensiveStressTests.kt` (large document tests) |
| `yole-fedge-binary-detection` | `FormatRegistryStressTest.kt` (binary detection) |
| `yole-fedge-corrupted-*` | Per-format `*ParserTests.kt`, `PropertyBasedFormatTests.kt` |
| `yole-fedge-mixed-encoding` | `PropertyBasedFormatTests.kt` (Unicode handling tests) |
| `yole-fedge-nested-*` | `ComprehensiveIntegrationTests.kt` (cross-format tests) |
| `yole-fedge-html-cache-consistency` | `MonitoringMetricsTests.kt` (HTML generation tests) |
| `yole-fedge-deep-nesting` | `ComprehensiveStressTests.kt` |

### Protocol Resilience Domain

| Challenge ID | Kotlin Test File(s) |
|-------------|---------------------|
| `yole-pres-cb-*` | `ResilienceTests.kt` (CircuitBreaker tests) |
| `yole-pres-cl-*` | `ResilienceTests.kt` (ConnectionLimiter tests) |
| `yole-pres-timeout-*` | `ContractTestsForProtocols.kt` (timeout contract tests) |
| `yole-pres-reconnect-*` | `ContractTestsForProtocols.kt` (connection lifecycle tests) |
| `yole-pres-concurrent-ops-limit` | `ComprehensiveStressTests.kt` (concurrent operation tests) |
| `yole-pres-error-cascade-*` | `ComprehensiveIntegrationTests.kt` |
| `yole-pres-document-cache-eviction` | `ResilienceTests.kt` (DocumentCache tests) |

### Other Domains

| Domain | Kotlin Test Location |
|--------|---------------------|
| `format-parsing` | `shared/src/commonTest/.../format/[formatname]/` |
| `format-detection` | `shared/src/commonTest/.../format/FormatRegistryStressTest.kt` |
| `performance` | `PerformanceMetricsTests.kt` |
| `concurrency` | `ComprehensiveStressTests.kt`, `ContractTestsForProtocols.kt` |
| `monitoring` | `MonitoringMetricsTests.kt` |
| `resilience` | `ResilienceTests.kt` |

### How to Add a New Mapping

When creating a new challenge:

1. Identify which Kotlin test class(es) cover the same behavior
2. If no Kotlin test exists, create one in the appropriate `shared/src/commonTest/` directory
3. Name the Kotlin test so it clearly corresponds to the challenge domain
4. Add the mapping to this table

The goal is bidirectional traceability: every challenge has at least one Kotlin test backing it, and every Kotlin test covering a cross-cutting concern is represented in a challenge bank.
