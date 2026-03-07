<!-- SPDX-FileCopyrightText: 2025 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Challenge Framework

This document describes the Challenge testing framework integrated into Yole via Git submodules, including its purpose, structure, challenge bank format, and usage.

## What is the Challenges Submodule?

The `Challenges/` directory is a Git submodule pointing to `digital.vasic.challenges`, a generic, reusable **Go module** for defining, registering, executing, and reporting on structured test scenarios (challenges). It provides:

- A **plugin-based architecture** with a registry, runner, and assertion engine.
- **16 built-in assertion evaluators** (e.g., `not_empty`, `contains`, `exact_count`, `min_score`, `max_latency`).
- **Multi-format reporting** in Markdown, JSON, and HTML.
- **Dependency ordering** via topological sort (Kahn's algorithm) so challenges run in correct order.
- **Parallel execution** for independent challenges.
- **Progress-based liveness detection** for long-running challenges (stale threshold vs. hard timeout).
- **User flow automation** with 8 adapter interfaces and 21 implementations covering browser, mobile, desktop, API, gRPC, WebSocket, and build tool testing.
- **Live monitoring** via a WebSocket-based real-time dashboard.
- **Prometheus metrics** for challenge execution tracking.

The module requires **Go 1.24+** and is located at:

```
Challenges/
├── banks/yole/         # Yole-specific challenge definitions (JSON)
├── cmd/                # CLI tools (userflow-runner)
├── pkg/                # Core library packages
│   ├── assertion/      # Assertion engine (16 evaluators)
│   ├── bank/           # Challenge bank loader + validator
│   ├── challenge/      # Core types (Challenge, Result, Config)
│   ├── env/            # Environment variable handling
│   ├── httpclient/     # HTTP client with JWT auth
│   ├── infra/          # Infrastructure bridge to Containers
│   ├── logging/        # Structured logging
│   ├── metrics/        # Prometheus metrics
│   ├── monitor/        # WebSocket live monitoring
│   ├── plugin/         # Plugin system
│   ├── registry/       # Challenge registration + ordering
│   ├── report/         # Report generation (MD/JSON/HTML)
│   ├── runner/         # Execution engine
│   └── userflow/       # Multi-platform user flow automation
├── docs/               # Framework documentation
└── README.md
```

## What is the Containers Submodule?

The `Containers/` directory is a Git submodule pointing to `digital.vasic.containers`, a generic, reusable **Go module** for container orchestration, health checking, lifecycle management, and service discovery. It is a **dependency of Challenges** (referenced via `replace` directive in `go.mod`).

Containers provides:

- **Multi-runtime support**: Docker, Podman, Kubernetes.
- **Auto-detection** of available container runtimes.
- **Health checking**: TCP, HTTP, gRPC, and custom health checks with retry.
- **Compose orchestration**: Batch operations grouped by compose file/profile.
- **Lifecycle management**: Lazy boot, idle shutdown, concurrency semaphores.
- **Resource monitoring**: CPU/memory/disk per container and cluster-wide snapshots.
- **Remote distribution**: Distribute containers across hosts via SSH with 5 scheduling strategies.
- **Service discovery**: TCP port probe and DNS-based discovery.

The Challenges framework uses Containers via an `infra.InfraProvider` bridge to manage test infrastructure (spinning up databases, emulators, network services, etc.) required by challenges.

Module path: `digital.vasic.containers` (Go 1.24+).

## How Challenge Banks Work

Challenge banks are **JSON files** stored in `Challenges/banks/yole/`. Each file defines a collection of related challenges for a specific domain (e.g., security, format-parsing, resilience).

### JSON Structure

Every bank file follows this schema:

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
      "description": "Detailed description of what is verified",
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
        {"type": "exact_count", "target": "result_name", "value": 0, "message": "Failure message"}
      ],
      "metrics": ["metric_name_1", "metric_name_2"]
    }
  ]
}
```

### Key Fields

| Field | Required | Description |
|-------|----------|-------------|
| `id` | Yes | Unique identifier, always prefixed with `yole-` |
| `name` | Yes | Human-readable name |
| `description` | Yes | What is verified and how |
| `category` | Yes | Domain grouping for filtering |
| `dependencies` | No | IDs of challenges that must pass first |
| `estimated_duration` | Yes | Expected execution time (e.g., `"30s"`, `"2m"`) |
| `inputs` | Yes | Parameters required (from `config`, `env`, or `dependency:<id>`) |
| `outputs` | Yes | Named outputs produced by execution |
| `assertions` | Yes | Pass/fail criteria evaluated against outputs |
| `metrics` | No | Numeric metrics collected during execution |

### Assertion Types

| Evaluator | Description |
|-----------|-------------|
| `not_empty` | Value is non-nil and non-empty |
| `not_mock` | Response is not mocked/placeholder |
| `contains` | String contains substring (case-insensitive) |
| `contains_any` | String contains any of the given values |
| `min_length` | String length meets minimum |
| `quality_score` | Numeric score meets threshold |
| `reasoning_present` | Response contains reasoning indicators |
| `code_valid` | Response contains valid code patterns |
| `min_count` | Count meets minimum |
| `exact_count` | Count matches exactly |
| `max_latency` | Response time within limit |
| `all_valid` | All array items are valid |
| `no_duplicates` | No duplicate items in array |
| `all_pass` | All sub-assertions pass |
| `no_mock_responses` | No mocked responses in array |
| `min_score` | Numeric minimum score |

## Challenge Banks in Challenges/banks/yole/

There are **17 challenge bank files** containing a total of **139 challenges**:

| # | Bank File | Domain | Challenges | Purpose |
|---|-----------|--------|:----------:|---------|
| 1 | `concurrency.json` | concurrency | 5 | Concurrent parsing, protocol connections, deadlock detection, race conditions, lock contention |
| 2 | `cross-platform-build.json` | cross-platform | 5 | Build verification across Android, Desktop, Web, iOS |
| 3 | `e2e-userflow.json` | e2e-userflow | 6 | End-to-end user flows across all platforms |
| 4 | `format-detection.json` | format-detection | 8 | FormatRegistry detection by extension, content, filename |
| 5 | `format-edge-cases-challenges.json` | format-edge-cases | 15 | Empty files, max size, binary detection, corrupted headers, mixed encodings, nested formats |
| 6 | `format-parsing.json` | format-parsing | 17 | All 17 text format parsers (Markdown, Todo.txt, CSV, LaTeX, Org-mode, RST, AsciiDoc, WikiText, Key-Value, TaskPaper, Textile, Creole, TiddlyWiki, Jupyter, R Markdown, etc.) |
| 7 | `lazy-loading.json` | lazy-loading | 4 | Lazy parser initialization, deferred loading correctness |
| 8 | `memory.json` | memory | 5 | Memory allocation tracking, leak detection, GC pressure |
| 9 | `monitoring.json` | monitoring | 5 | Metrics collection accuracy, performance baselines, memory tracking, cache monitoring |
| 10 | `network-protocols.json` | network-protocols | 9 | Cloud storage (Dropbox, Google Drive, OneDrive) and network protocol (FTP, SFTP) CRUD and auth |
| 11 | `performance.json` | performance | 7 | Parse time budgets, memory limits, throughput baselines |
| 12 | `protocol-resilience-challenges.json` | protocol-resilience | 15 | Circuit breaker states, connection limiter, timeout enforcement, reconnection, concurrent ops, error cascading |
| 13 | `resilience.json` | resilience | 5 | Circuit breaker activation/recovery, timeout handling, retry with backoff, graceful degradation, connection pool exhaustion |
| 14 | `security-challenges.json` | security-deep | 16 | Path traversal normalization, XSS per format, query injection per protocol, CancellationException rethrow, credential storage, HTTPS enforcement, certificate validation, input sanitization |
| 15 | `security.json` | security | 7 | HTML escaping, CSS injection, path traversal, dependency scanning, TLS, credential handling, Detekt static analysis |
| 16 | `test-coverage.json` | test-coverage | 6 | Coverage thresholds, untested code detection |
| 17 | `ui-accessibility.json` | ui-accessibility | 6 | Accessibility compliance, theme support, contrast ratios |

## How to Run Challenges

### Prerequisites

1. **Go 1.24+** installed.
2. Git submodules initialized:
   ```bash
   git submodule update --init --recursive
   ```

### Build and Run Framework Tests

```bash
# Build Containers (dependency)
cd Containers && go build ./... && go vet ./...

# Build Challenges
cd Challenges && go build ./... && go vet ./...

# Run all framework tests (with race detector)
cd Challenges && go test ./... -race -count=1

# Run only short (unit) tests
cd Challenges && go test ./... -short

# Run benchmarks
cd Challenges && go test -bench=. ./tests/benchmark/
```

### Run via Gradle

A convenience Gradle task is available:

```bash
./gradlew :shared:runChallenges
```

This checks for Go availability, builds the Challenges and Containers modules, and runs all framework tests.

### Run via Docker/Podman

```bash
docker compose run --rm build bash -c "
    cd Challenges && \
    go build ./... && \
    go test ./... -race -count=1
"
```

### Run Specific Challenge Categories

```go
// Programmatic usage
b := bank.New()
b.LoadDir("banks/yole/")

// Filter by category
securityDefs := b.ByCategory("security")

// Run in parallel
results, err := r.RunParallel(ctx, ids, config, 4)
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

## How to Add New Challenge Banks

### Step 1: Choose a Domain

Pick a domain that groups related verification concerns. Use lowercase with hyphens. Existing domains: `security`, `format-parsing`, `format-detection`, `format-edge-cases`, `resilience`, `protocol-resilience`, `performance`, `concurrency`, `monitoring`, `memory`, `lazy-loading`, `test-coverage`, `ui-accessibility`, `cross-platform`, `e2e-userflow`.

### Step 2: Create the JSON File

```bash
touch Challenges/banks/yole/my-new-domain.json
```

### Step 3: Define Challenges

Follow these conventions:

- **ID prefix**: Always start with `yole-` followed by a short domain code (e.g., `yole-sec-`, `yole-fmt-`, `yole-res-`).
- **Descriptions**: Be specific about what is verified, how many items are tested, and what constitutes a pass.
- **Assertions**: Use the built-in evaluators listed above.
- **Dependencies**: Only add when challenge B genuinely requires output from challenge A.
- **Metrics**: Include timing metrics (`*_time_ms`) and count metrics (`*_count`, `*_tested`).

### Step 4: Validate

```bash
# Quick JSON syntax check
python3 -m json.tool Challenges/banks/yole/my-new-domain.json > /dev/null

# Full validation via Go
cd Challenges
go run ./cmd/validate-banks/ --dir banks/yole/
```

### Step 5: Implement Executors

Each challenge definition needs a corresponding Go implementation that reads inputs, executes verification logic, produces declared outputs, and returns metrics.

### Step 6: Add Kotlin Test Mapping

Create corresponding Kotlin tests in `shared/src/commonTest/` that validate the same behaviors at the unit/integration level. Update the mapping table in this document.

## CI Integration

Challenge framework tests run automatically via `.github/workflows/challenges.yml` on every push and pull request to the `master` branch.

The workflow:

1. Checks out the repository with submodules (recursive).
2. Sets up Go 1.24.
3. Builds and tests the Containers submodule.
4. Builds and tests the Challenges submodule.
5. Validates all challenge bank JSON files for syntax and schema correctness.
6. Uploads test results and challenge reports as artifacts.

See `.github/workflows/challenges.yml` for the full workflow definition.

## Mapping Between Challenges and Kotlin Tests

Every challenge bank domain has corresponding Kotlin test files:

| Domain | Kotlin Test Location |
|--------|---------------------|
| `format-parsing` | `shared/src/commonTest/.../format/[formatname]/` (per-format test files) |
| `format-detection` | `FormatRegistryStressTest.kt`, `FormatRegistryEdgeCaseTest.kt` |
| `format-edge-cases` | `PropertyBasedFormatTests.kt`, `ComprehensiveStressTests.kt` |
| `security`, `security-deep` | `SecurityValidationTests.kt`, `SafetyFixesTest.kt` |
| `resilience`, `protocol-resilience` | `ResilienceTests.kt`, `ContractTestsForProtocols.kt` |
| `performance` | `PerformanceMetricsTests.kt` |
| `concurrency` | `ComprehensiveStressTests.kt` |
| `monitoring` | `MonitoringMetricsTests.kt` |
| `lazy-loading` | Tests in `shared/src/commonTest/.../format/lazy/` |
| `memory` | `ComprehensiveStressTests.kt` |
| `test-coverage` | Kover coverage reports (`./gradlew koverHtmlReport`) |
| `ui-accessibility` | UI component tests |
| `cross-platform` | Build verification tasks |
| `e2e-userflow` | End-to-end tests via userflow CLI |
