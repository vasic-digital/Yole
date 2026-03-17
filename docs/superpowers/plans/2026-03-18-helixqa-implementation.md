# HelixQA Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create HelixQA — an AI-driven QA orchestration Go module that reuses existing vasic-digital Go modules (Challenges, Containers) to provide intelligent test execution, multi-platform automation, crash/ANR detection with real-time validation, documentation-to-test consistency verification, and automated ticket generation. Integrate as Yole's primary QA brain for all testing operations.

**Architecture:** HelixQA is a new Go module (`digital.vasic.helixqa`) that sits above Challenges and Containers as an orchestration layer. It does NOT duplicate their functionality — it imports and composes them. HelixQA adds: (1) QA orchestration engine that drives Challenges runner with intelligent scheduling, (2) crash/ANR detection with logcat/screenshot validation at every step, (3) test bank management with YAML definitions, (4) report generation with evidence collection, (5) CLI entry point. Yole integrates HelixQA as a Git submodule alongside Challenges and Containers.

**Tech Stack:** Go 1.24+, digital.vasic.challenges (Challenges module), digital.vasic.containers (Containers module), testify, gopkg.in/yaml.v3, gorilla/websocket

---

## Phase 1: Repository Setup & Module Skeleton

### Task 1.1: Create GitHub and GitLab Repositories

- [ ] **Step 1: Create GitHub repo**
```bash
gh repo create vasic-digital/HelixQA --public --description "AI-driven QA orchestration for multi-platform testing" --clone=false
```

- [ ] **Step 2: Create GitLab repo**
```bash
glab repo create HelixQA --group vasic-digital --public --description "AI-driven QA orchestration for multi-platform testing"
```

- [ ] **Step 3: Initialize local module**
```bash
cd /run/media/milosvasic/DATA4TB/Projects
mkdir HelixQA && cd HelixQA
git init
go mod init digital.vasic.helixqa
```

- [ ] **Step 4: Add remotes**
```bash
git remote add origin git@github.com:vasic-digital/HelixQA.git
git remote add gitlab git@gitlab.com:vasic-digital/HelixQA.git
```

- [ ] **Step 5: Create go.mod with dependencies on existing modules**
```go
module digital.vasic.helixqa

go 1.24.0

require (
    digital.vasic.challenges v0.0.0
    digital.vasic.containers v0.0.0
    github.com/stretchr/testify v1.11.1
    gopkg.in/yaml.v3 v3.0.1
)

replace (
    digital.vasic.challenges => ../Challenges
    digital.vasic.containers => ../Containers
)
```

- [ ] **Step 6: Create directory structure**
```bash
mkdir -p cmd/helixqa
mkdir -p pkg/{orchestrator,testbank,detector,reporter,validator,config}
mkdir -p banks/
mkdir -p docs/
mkdir -p tests/{unit,integration,e2e,stress,benchmark,security}
```

- [ ] **Step 7: Create SPDX-licensed root files**
Create: `LICENSE` (Apache-2.0), `README.md`, `CLAUDE.md`, `AGENTS.md`, `Makefile`, `.gitignore`

- [ ] **Step 8: Initial commit and push**
```bash
git add . && git commit -m "feat: initialize HelixQA Go module with dependency skeleton"
git push origin main && git push gitlab main
```

### Task 1.2: Add HelixQA as Yole Git Submodule

- [ ] **Step 1: Add submodule to Yole**
```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git submodule add git@github.com:vasic-digital/HelixQA.git HelixQA
```

- [ ] **Step 2: Update settings.gradle.kts** (if needed for Gradle integration)
- [ ] **Step 3: Commit submodule addition**

---

## Phase 2: Core Orchestrator Engine

### Task 2.1: QA Orchestrator — Main Brain

**Files:**
- Create: `pkg/orchestrator/orchestrator.go`
- Create: `pkg/orchestrator/options.go`
- Create: `pkg/orchestrator/result.go`
- Test: `pkg/orchestrator/orchestrator_test.go`

The orchestrator is the central brain. It:
1. Loads test banks (YAML challenge definitions)
2. Creates a Challenges runner with the loaded banks
3. Executes challenges with platform-appropriate adapters
4. Monitors execution for crashes/ANRs in real-time
5. Validates every step (no false positives)
6. Collects evidence (screenshots, videos, logs)
7. Generates reports

Key interface:
```go
type Orchestrator struct {
    config     *Config
    runner     *runner.Runner        // from digital.vasic.challenges
    detector   *detector.Detector    // crash/ANR detection
    reporter   *reporter.Reporter    // evidence collection + reports
    validator  *validator.Validator  // step validation
    logger     logging.Logger        // from digital.vasic.challenges
}

func New(opts ...Option) (*Orchestrator, error)
func (o *Orchestrator) Run(ctx context.Context, bankPaths []string) (*Result, error)
func (o *Orchestrator) RunPlatform(ctx context.Context, platform string, bankPaths []string) (*Result, error)
```

### Task 2.2: Configuration System

**Files:**
- Create: `pkg/config/config.go`
- Create: `pkg/config/platform.go`
- Test: `pkg/config/config_test.go`

```go
type Config struct {
    Platforms      []PlatformConfig
    TestBankPaths  []string
    OutputDir      string
    RecordVideo    bool
    TakeScreenshots bool
    ValidateSteps  bool  // mandatory step validation
    CrashDetection bool  // real-time crash monitoring
    MaxParallel    int
    Timeout        time.Duration
}

type PlatformConfig struct {
    Name       string // "android", "web", "desktop"
    Enabled    bool
    Device     string // "emulator-5554", "chromium", etc.
    SpeedMode  string // "slow", "normal", "fast"
}
```

### Task 2.3: Test Bank Manager

**Files:**
- Create: `pkg/testbank/manager.go`
- Create: `pkg/testbank/schema.go`
- Create: `pkg/testbank/loader.go`
- Test: `pkg/testbank/manager_test.go`

Loads YAML test bank files and converts them to Challenges format. Reuses `digital.vasic.challenges/pkg/bank` for schema validation.

```go
type Manager struct {
    banks    map[string]*bank.Bank  // reuse Challenges bank
    logger   logging.Logger
}

func NewManager(logger logging.Logger) *Manager
func (m *Manager) LoadBanks(paths []string) error
func (m *Manager) GetChallengesForPlatform(platform string) []challenge.Definition
func (m *Manager) GetAllChallenges() []challenge.Definition
```

---

## Phase 3: Crash/ANR Detection & Step Validation

### Task 3.1: Real-Time Crash Detector

**Files:**
- Create: `pkg/detector/detector.go`
- Create: `pkg/detector/android.go`
- Create: `pkg/detector/web.go`
- Create: `pkg/detector/desktop.go`
- Test: `pkg/detector/detector_test.go`
- Test: `pkg/detector/android_test.go`

Monitors for crashes/ANRs/errors during test execution:

```go
type Detector struct {
    platform string
    device   string
    logger   logging.Logger
}

type DetectionResult struct {
    HasCrash     bool
    HasANR       bool
    HasError     bool
    StackTrace   string
    LogEntries   []string
    Screenshots  []string
    Timestamp    time.Time
}

func (d *Detector) CheckAndroid(ctx context.Context) (*DetectionResult, error)
// Uses: adb logcat -d | grep "FATAL\|ANR\|died\|crash"
// Uses: adb shell pidof <package> (verify app still running)
// Uses: adb shell screencap (capture current state)

func (d *Detector) CheckWeb(ctx context.Context) (*DetectionResult, error)
// Uses: browser console error collection via Playwright

func (d *Detector) CheckDesktop(ctx context.Context) (*DetectionResult, error)
// Uses: process alive check, stderr monitoring
```

### Task 3.2: Step Validator

**Files:**
- Create: `pkg/validator/validator.go`
- Create: `pkg/validator/screenshot.go`
- Create: `pkg/validator/state.go`
- Test: `pkg/validator/validator_test.go`

Validates every test step actually succeeded (no false positives):

```go
type Validator struct {
    detector *detector.Detector
    logger   logging.Logger
}

type StepResult struct {
    StepName    string
    Passed      bool
    Evidence    Evidence
    CrashCheck  *detector.DetectionResult
}

type Evidence struct {
    Screenshot   string  // path to screenshot
    Video        string  // path to video segment
    LogSnippet   string  // relevant log lines
    Timestamp    time.Time
}

func (v *Validator) ValidateStep(ctx context.Context, step string) (*StepResult, error)
// 1. Take screenshot
// 2. Check for crashes/ANRs
// 3. Verify app is still responsive
// 4. Return validated result with evidence
```

---

## Phase 4: Reporter & Evidence Collection

### Task 4.1: Evidence Collector

**Files:**
- Create: `pkg/reporter/evidence.go`
- Create: `pkg/reporter/collector.go`
- Test: `pkg/reporter/evidence_test.go`

Collects all evidence during test execution:

```go
type Collector struct {
    outputDir string
    platform  string
    logger    logging.Logger
}

func (c *Collector) CaptureScreenshot(ctx context.Context, name string) (string, error)
func (c *Collector) StartRecording(ctx context.Context, name string) error
func (c *Collector) StopRecording(ctx context.Context) (string, error)
func (c *Collector) CaptureLogcat(ctx context.Context, lines int) (string, error)
func (c *Collector) CaptureStackTrace(ctx context.Context) (string, error)
```

### Task 4.2: Report Generator

**Files:**
- Create: `pkg/reporter/reporter.go`
- Create: `pkg/reporter/markdown.go`
- Create: `pkg/reporter/html.go`
- Create: `pkg/reporter/json_report.go`
- Test: `pkg/reporter/reporter_test.go`

Reuses `digital.vasic.challenges/pkg/report` for base formatting, extends with QA-specific sections:

```go
type Reporter struct {
    format   string // "markdown", "html", "json"
    logger   logging.Logger
}

type QAReport struct {
    Summary      Summary
    Platforms    []PlatformResult
    Crashes      []CrashReport
    Screenshots  []string
    Videos       []string
    Duration     time.Duration
    Timestamp    time.Time
}

func (r *Reporter) Generate(result *orchestrator.Result) (*QAReport, error)
func (r *Reporter) WriteMarkdown(report *QAReport, path string) error
func (r *Reporter) WriteHTML(report *QAReport, path string) error
```

---

## Phase 5: CLI Entry Point

### Task 5.1: CLI Command

**Files:**
- Create: `cmd/helixqa/main.go`
- Test: `cmd/helixqa/main_test.go`

```go
func main() {
    // Flags:
    // --banks <paths>       Test bank directories
    // --platform <name>     Target platform (android|web|desktop|all)
    // --device <id>         Device/emulator ID
    // --output <dir>        Output directory for reports/recordings
    // --speed <mode>        Speed mode (slow|normal|fast)
    // --report <format>     Report format (markdown|html|json)
    // --validate            Enable step validation (default: true)
    // --record              Enable video recording (default: true)
    // --verbose             Verbose logging
}
```

---

## Phase 6: Comprehensive Tests

### Task 6.1: Unit Tests (per package)
- `pkg/orchestrator/orchestrator_test.go` — 30+ tests
- `pkg/config/config_test.go` — 20+ tests
- `pkg/testbank/manager_test.go` — 25+ tests
- `pkg/detector/detector_test.go` — 30+ tests (per platform)
- `pkg/validator/validator_test.go` — 25+ tests
- `pkg/reporter/reporter_test.go` — 20+ tests

### Task 6.2: Integration Tests
- `tests/integration/orchestrator_integration_test.go` — end-to-end flow
- `tests/integration/challenges_bridge_test.go` — verify Challenges module integration

### Task 6.3: Stress Tests
- `tests/stress/concurrent_orchestration_test.go` — concurrent execution

### Task 6.4: Benchmark Tests
- `tests/benchmark/detection_benchmark_test.go` — crash detection speed

### Task 6.5: Security Tests
- `tests/security/input_validation_test.go` — malicious inputs

### Task 6.6: E2E Tests
- `tests/e2e/full_qa_run_test.go` — complete QA pipeline

---

## Phase 7: Documentation & Video Course

### Task 7.1: Documentation
- Create: `README.md` — overview, installation, usage
- Create: `docs/ARCHITECTURE.md` — system design
- Create: `docs/USER_GUIDE.md` — step-by-step usage
- Create: `docs/API_REFERENCE.md` — package documentation
- Create: `CLAUDE.md` — AI agent instructions
- Create: `AGENTS.md` — development guide
- Create: `CONTRIBUTING.md` — contribution guidelines
- Create: `CHANGELOG.md` — version history

### Task 7.2: Challenge Banks for HelixQA Self-Testing
- Create: `banks/helixqa-unit.json`
- Create: `banks/helixqa-integration.json`
- Create: `banks/helixqa-security.json`

---

## Phase 8: Yole Integration

### Task 8.1: Wire HelixQA into Yole's Test Infrastructure

**Files in Yole repo:**
- Modify: `Makefile` — add `make helixqa` target
- Modify: `docker-compose.yml` — add HelixQA service
- Create: `helixqa.yml` — HelixQA configuration for Yole

### Task 8.2: Create Yole-Specific Test Banks (in Yole repo, NOT HelixQA)

**Files in Yole repo** (`Challenges/banks/yole/`):
- Update existing 27 challenge banks with HelixQA orchestration metadata
- Ensure all 17 format tests have step validation
- Ensure all platform tests have crash detection
- Ensure all automation tests have video recording

### Task 8.3: Gradle Integration

**Files in Yole repo:**
- Modify: `build.gradle.kts` — add `runHelixQA` task
- Create: `.github/workflows/helixqa.yml` — CI workflow

### Task 8.4: Replace Direct Challenge Execution

Wire HelixQA as the orchestrator for all test execution:
- `./gradlew runChallenges` → delegates to HelixQA
- `./gradlew runHelixQA` → full QA run with validation
- `make helixqa` → convenience target

---

## Phase 9: Final Verification

### Task 9.1: Run All HelixQA Tests
```bash
cd HelixQA && go test ./... -race -count=1
```

### Task 9.2: Run Yole Tests via HelixQA
```bash
cd Yole && ./gradlew runHelixQA
```

### Task 9.3: Verify No Dead Code
- Audit all imports across Challenges, Containers, HelixQA
- Verify no duplicate implementations
- Verify HelixQA reuses (not reimplements) existing modules

### Task 9.4: Push Everything
```bash
# HelixQA
cd HelixQA && git push origin main && git push gitlab main

# Challenges
cd Challenges && git push origin main

# Containers
cd Containers && git push origin main

# Yole
cd Yole && git push origin master
```

---

## Dependency Graph

```
Phase 1 (Repo Setup) ──────────────────────────────────────┐
                                                             │
Phase 2 (Orchestrator) ─── depends on Phase 1 ──────────────┤
Phase 3 (Detector/Validator) ─── depends on Phase 1 ────────┤
Phase 4 (Reporter) ─── depends on Phase 1 ──────────────────┤
                                                             │
Phase 5 (CLI) ─── depends on 2, 3, 4 ──────────────────────┤
Phase 6 (Tests) ─── depends on 2, 3, 4, 5 ─────────────────┤
Phase 7 (Docs) ─── depends on all above ────────────────────┤
Phase 8 (Yole Integration) ─── depends on 5, 6 ────────────┤
Phase 9 (Final) ─── depends on ALL ─────────────────────────┘
```

## Key Constraints

1. **Reuse, don't duplicate**: HelixQA imports Challenges and Containers — never reimplements their functionality
2. **No false positives**: Every test step must be validated with crash detection + screenshot evidence
3. **All recordings mandatory**: Video + screenshots at every step
4. **Test banks in Yole, not HelixQA**: HelixQA is generic; Yole-specific tests stay in Yole repo
5. **Both GitHub and GitLab**: Push to both remotes always
6. **Go 1.24+**: Match existing module Go version
7. **Comprehensive tests**: Unit, integration, E2E, stress, benchmark, security, challenges
