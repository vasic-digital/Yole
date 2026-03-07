# Module 16: Challenges Framework (6 videos)

## Video 16.1: Introduction to the Challenges Framework (20 min)

### Timestamps
- 0:00 What is the Challenges framework: a Go-based testing and validation engine
- 2:00 Why Go instead of Kotlin: cross-project reuse, container orchestration, CLI tooling
- 4:00 Architecture overview: Challenges depends on Containers, both are Git submodules
- 6:00 Core concepts: Challenge interface, Registry, Runner, Assertions, Reports
- 8:00 Design patterns: Template Method (BaseChallenge), Strategy (reporters), Registry, Adapter
- 10:00 The Challenge lifecycle: Configure, Validate, Execute, Cleanup
- 12:00 Progress-based liveness detection: no hard timeouts for long-running challenges
- 14:00 Plugin architecture: extending the framework with custom challenge types
- 16:00 User flow automation: 8 adapter interfaces, 21 implementations
- 18:00 Building the framework: `cd Challenges && go build ./... && go test ./... -race -count=1`
- 19:30 Summary

### Code References
- `Challenges/pkg/challenge/challenge.go` -- Core Challenge interface and BaseChallenge
- `Challenges/pkg/challenge/config.go` -- Challenge configuration
- `Challenges/pkg/challenge/result.go` -- Result types and status constants
- `Challenges/pkg/registry/` -- Challenge registration and dependency ordering (Kahn's algorithm)
- `Challenges/pkg/runner/` -- Execution engine (sequential, parallel, pipeline)
- `Challenges/pkg/assertion/engine.go` -- Assertion evaluation engine

### Key Code Walkthrough

The Challenge interface defines the lifecycle every challenge follows:

```go
type Challenge interface {
    ID() string
    Configure(config Config) error
    Validate() error
    Execute(ctx context.Context) (*Result, error)
    Cleanup() error
}
```

BaseChallenge provides the Template Method pattern -- concrete challenges only override `Execute()`:

```go
type MyChallenge struct {
    challenge.BaseChallenge
}

func (c *MyChallenge) Execute(ctx context.Context) (*Result, error) {
    // Report progress for liveness detection
    c.ReportProgress("starting", map[string]any{"step": 1})

    // Do challenge work...
    return &Result{Status: "passed"}, nil
}
```

### Exercises
1. **Build the framework** -- Clone the Challenges submodule, run `go build ./...` and `go test ./... -race -count=1`, and verify all tests pass.
2. **Explore the interface** -- Read `challenge.go` and trace how `BaseChallenge` implements the Template Method pattern.

---

## Video 16.2: Challenge Bank Definitions (18 min)

### Timestamps
- 0:00 What is a challenge bank: JSON definitions of structured test scenarios
- 2:00 Bank schema: version, name, metadata, challenges array
- 4:00 Challenge definition fields: id, name, description, category, inputs, outputs, assertions
- 6:00 Input sources: `config` (static data), `env` (environment variables)
- 8:00 Output types: string, json, number, boolean
- 10:00 Assertion types: `not_empty`, `contains`, `exact_count`, `min_length`, `max_latency`
- 12:00 Metrics collection: `parse_time_ms`, `html_size_bytes`, custom metrics
- 14:00 Dependencies between challenges: execution ordering
- 16:00 Bank validation: schema checking and dependency cycle detection
- 17:30 Summary

### Code References
- `Challenges/pkg/bank/bank.go` -- Bank loading and management
- `Challenges/pkg/bank/schema.go` -- Bank schema definition
- `Challenges/pkg/bank/validator.go` -- Schema and dependency validation
- `Challenges/banks/yole/format-parsing.json` -- Yole format parsing challenge bank

### Key Code Walkthrough

A challenge bank definition in JSON:

```json
{
  "version": "1.0",
  "name": "Yole Format Parsing Challenges",
  "metadata": {
    "project": "yole",
    "domain": "format-parsing"
  },
  "challenges": [
    {
      "id": "yole-fmt-markdown-basic",
      "name": "Markdown Basic Parsing",
      "description": "Verify Markdown parser handles headings, bold, italic...",
      "category": "format-parsing",
      "estimated_duration": "30s",
      "inputs": [
        {"name": "markdown_content", "source": "config", "required": true}
      ],
      "outputs": [
        {"name": "parsed_html", "type": "string"}
      ],
      "assertions": [
        {"type": "not_empty", "target": "parsed_html"},
        {"type": "contains", "target": "parsed_html", "value": "<h1>"},
        {"type": "exact_count", "target": "parse_errors", "value": 0}
      ],
      "metrics": ["parse_time_ms", "html_size_bytes"]
    }
  ]
}
```

### Built-in Assertion Evaluators

| Evaluator | Description |
|-----------|-------------|
| `not_empty` | Target value is not empty/null |
| `contains` | Target string contains a value |
| `contains_any` | Target contains any of several values |
| `min_length` | Target has minimum length |
| `exact_count` | Target count equals exact value |
| `min_count` | Target count is at minimum |
| `max_latency` | Execution time under threshold |
| `quality_score` | Quality metric meets minimum |
| `all_pass` | All items in a collection pass |
| `no_duplicates` | No duplicate items |

### Exercises
1. **Read a bank** -- Open `Challenges/banks/yole/format-parsing.json` and trace how each challenge maps to a Yole format parser.
2. **Validate a bank** -- Use the bank validator to check the schema of an existing bank file for correctness.

---

## Video 16.3: Running Challenges Against Yole (20 min)

### Timestamps
- 0:00 Setting up the environment: Yole built, Challenges compiled
- 2:00 Running all Yole challenges: `go run ./cmd/... --bank banks/yole/`
- 4:00 Sequential vs. parallel execution: `RunSequence` vs. `RunParallel`
- 6:00 Progress-based liveness detection in action: `StaleThreshold` configuration
- 8:00 Live monitoring with WebSocket dashboard
- 10:00 Challenge output and assertion evaluation
- 12:00 Report generation: Markdown, JSON, and HTML formats
- 14:00 Handling failures: interpreting error messages and assertion failures
- 16:00 Retry strategies and flaky challenge mitigation
- 18:00 Performance metrics collection and analysis
- 19:30 Summary

### Code References
- `Challenges/pkg/runner/` -- Sequential, parallel, and pipeline runners
- `Challenges/pkg/report/` -- Markdown, JSON, HTML report generators
- `Challenges/pkg/monitor/` -- Live WebSocket monitoring dashboard
- `Challenges/pkg/metrics/` -- Prometheus-compatible metrics

### Key Commands

```bash
# Build the Challenges framework
cd Challenges && go build ./...

# Run all Yole challenges
go run ./cmd/runner --bank banks/yole/ --report markdown --output results/

# Run a specific category
go run ./cmd/runner --bank banks/yole/format-parsing.json

# Run with parallel execution
go run ./cmd/runner --bank banks/yole/ --parallel --workers 4

# Run with liveness detection
go run ./cmd/runner --bank banks/yole/ \
  --timeout 1h --stale-threshold 5m
```

### Exercises
1. **Run format challenges** -- Execute the `format-parsing.json` bank against Yole and review the generated report.
2. **Monitor live** -- Start the WebSocket dashboard and watch challenges execute in real time.
3. **Compare reports** -- Generate both Markdown and HTML reports and compare the information density.

---

## Video 16.4: Yole's 14 Challenge Categories (22 min)

### Timestamps
- 0:00 Overview of Yole's 14 challenge bank files
- 1:00 `format-parsing.json`: all 17 format parsers with roundtrip validation
- 2:30 `format-detection.json`: extension-based and content-based format detection
- 4:00 `network-protocols.json`: Dropbox, Google Drive, OneDrive, WebDAV, FTP, SFTP, Git, S3
- 5:30 `security.json`: XSS prevention, path traversal, credential security
- 7:00 `performance.json`: parse time benchmarks, memory limits, large file handling
- 8:30 `test-coverage.json`: minimum coverage thresholds per module
- 10:00 `ui-accessibility.json`: screen reader support, color contrast, keyboard navigation
- 11:30 `cross-platform-build.json`: build verification across Android, Desktop, Web, iOS
- 13:00 `e2e-userflow.json`: end-to-end user journeys across platforms
- 14:30 `concurrency.json`: concurrent access patterns, thread safety, lock ordering
- 16:00 `resilience.json`: CircuitBreaker, ConnectionLimiter, DocumentCache validation
- 17:30 `monitoring.json`: performance metrics, cache hit rates, circuit breaker states
- 19:00 `lazy-loading.json`: lazy parser registration, HttpClient initialization, HTML caching
- 20:00 `memory.json`: memory stability under load, GC pressure, allocation patterns
- 21:00 How categories map to Yole's architecture
- 21:30 Summary

### Code References
- `Challenges/banks/yole/format-parsing.json` -- 17 format parser challenges
- `Challenges/banks/yole/format-detection.json` -- Format detection challenges
- `Challenges/banks/yole/network-protocols.json` -- Network protocol challenges
- `Challenges/banks/yole/security.json` -- Security validation challenges
- `Challenges/banks/yole/performance.json` -- Performance benchmark challenges
- `Challenges/banks/yole/test-coverage.json` -- Coverage threshold challenges
- `Challenges/banks/yole/ui-accessibility.json` -- Accessibility challenges
- `Challenges/banks/yole/cross-platform-build.json` -- Cross-platform build challenges
- `Challenges/banks/yole/e2e-userflow.json` -- End-to-end user flow challenges
- `Challenges/banks/yole/concurrency.json` -- Concurrency safety challenges
- `Challenges/banks/yole/resilience.json` -- Resilience pattern challenges
- `Challenges/banks/yole/monitoring.json` -- Monitoring metric challenges
- `Challenges/banks/yole/lazy-loading.json` -- Lazy loading challenges
- `Challenges/banks/yole/memory.json` -- Memory stability challenges

### Exercises
1. **Category coverage** -- Map each challenge category to the Yole source directories it validates. Identify any source directories not covered by challenges.
2. **Run each category** -- Execute each of the 14 bank files individually and compare pass rates across categories.

---

## Video 16.5: Creating Custom Challenges (18 min)

### Timestamps
- 0:00 When to create custom challenges: new features, new formats, regression tests
- 2:00 Writing a JSON challenge definition from scratch
- 4:00 Defining inputs, outputs, and assertions
- 6:00 Implementing a Go challenge type using `BaseChallenge`
- 8:00 Registering challenges with the Registry
- 10:00 Writing custom assertion evaluators
- 12:00 Plugin architecture: packaging challenges as plugins
- 14:00 Testing challenges: unit tests for the challenge itself
- 16:00 Adding to an existing bank vs. creating a new bank
- 17:30 Summary

### Key Code Walkthrough

Creating a custom challenge for validating a new Yole format:

```go
// custom_format_challenge.go
type CustomFormatChallenge struct {
    challenge.BaseChallenge
    FormatID string
    Input    string
    Expected string
}

func (c *CustomFormatChallenge) Execute(ctx context.Context) (*challenge.Result, error) {
    c.ReportProgress("parsing", map[string]any{"format": c.FormatID})

    // Run Yole parser via shell command
    output, err := c.Shell().Run(ctx, fmt.Sprintf(
        "./gradlew test --tests 'digital.vasic.yole.format.%s.*'", c.FormatID,
    ))
    if err != nil {
        return challenge.FailedResult("parser tests failed: " + err.Error()), nil
    }

    return &challenge.Result{
        Status: "passed",
        Outputs: map[string]any{
            "test_output": output,
        },
    }, nil
}
```

Adding a challenge to an existing bank:

```json
{
  "id": "yole-fmt-custom-basic",
  "name": "Custom Format Basic Parsing",
  "category": "format-parsing",
  "estimated_duration": "30s",
  "inputs": [
    {"name": "custom_content", "source": "config", "required": true}
  ],
  "assertions": [
    {"type": "not_empty", "target": "parsed_html"},
    {"type": "exact_count", "target": "parse_errors", "value": 0}
  ]
}
```

### Exercises
1. **Write a bank entry** -- Add a new challenge to `format-parsing.json` for a specific edge case (e.g., Markdown with 10 levels of nested lists).
2. **Implement a challenge** -- Write a Go challenge that validates Yole's CSV parser with a 10,000-row file and asserts parse time under 500ms.

---

## Video 16.6: CI/CD Integration (15 min)

### Timestamps
- 0:00 Running challenges in CI/CD: GitHub Actions, GitLab CI
- 2:00 Docker-based execution in CI: consistent environment
- 4:00 Challenge selection: running critical challenges on every push, full suite on merge
- 6:00 Report artifacts: uploading HTML/Markdown reports to CI
- 8:00 Failure handling: blocking merges on challenge failures
- 10:00 Performance regression detection: `max_latency` assertions
- 12:00 The Containers submodule: orchestrating services for integration challenges
- 13:30 Summary

### Key Code Walkthrough

GitHub Actions workflow for running challenges:

```yaml
# .github/workflows/challenges.yml
name: Run Challenges
on: [push, pull_request]

jobs:
  challenges:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          submodules: recursive

      - uses: actions/setup-go@v5
        with:
          go-version: '1.24'

      - name: Build Challenges
        run: cd Challenges && go build ./...

      - name: Run Format Challenges
        run: |
          cd Challenges
          go run ./cmd/runner \
            --bank banks/yole/format-parsing.json \
            --report html --output ../results/

      - name: Upload Report
        uses: actions/upload-artifact@v4
        with:
          name: challenge-report
          path: results/
```

### Exercises
1. **CI pipeline** -- Create a GitHub Actions workflow that runs the `format-parsing` and `security` challenge banks on every pull request.
2. **Gate configuration** -- Configure the pipeline to fail if any challenge in the `security` bank fails, but only warn for `performance` bank failures.
