# Autonomous QA Session Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend HelixQA with LLM-powered autonomous testing across 5 modules — LLMsVerifier Strategy, LLMOrchestrator, VisionEngine, DocProcessor, and HelixQA Autonomous Session.

**Architecture:** Coordinator + Worker Agents pattern. SessionCoordinator runs 4 phases (Setup → Doc-Driven → Curiosity → Report), delegating to parallel PlatformWorkers. 3 new standalone Go modules communicate via injected interfaces. LLMsVerifier selects optimal LLMs via pluggable Strategy pattern.

**Tech Stack:** Go 1.24, GoCV (gocv.io/x/gocv), ffmpeg, adb, Playwright, xdotool, testify, yaml.v3

**Spec:** `docs/superpowers/specs/2026-03-19-autonomous-qa-session-design.md`

---

## Phase Overview

| Phase | Module | Dependencies | Est. Tasks | Est. Tests |
|-------|--------|-------------|------------|------------|
| 1 | LLMsVerifier Strategy Extension | None | 30 | ~120 |
| 2 | DocProcessor (new module) | None | 28 | ~150 |
| 3 | LLMOrchestrator (new module) | None | 35 | ~200 |
| 4 | VisionEngine (new module) | None | 32 | ~180 |
| 5 | HelixQA New Packages | Phases 1-4 | 40 | ~300 |
| 6 | HelixQA Enhanced Packages | Phase 5 | 20 | ~150 |
| 7 | Documentation & Video Courses | Phase 6 | 15 | — |
| 8 | GitHub/GitLab Project Management | Phase 1 | 10 | — |

**Phases 1-4 are independent** and can run in parallel. Phases 5-6 depend on 1-4. Phase 8 starts first (create project board), then runs alongside all others.

### Note on Plan Granularity

Phase 1 provides full code samples for every step. Phases 2-4 define interfaces, file structures, and test scenarios but defer exact implementations to the spec document (`docs/superpowers/specs/2026-03-19-autonomous-qa-session-design.md`), which contains all type definitions, method signatures, and architectural decisions. **Implementing agents MUST read the spec for each module before executing its phase.** Each step in Phases 2-4 should still follow TDD: write failing test → run to confirm failure → implement minimal code → run to confirm pass → commit.

### Note on Go Module Paths

The existing LLMsVerifier uses module path `llm-verifier` (with `replace llm-verifier => ./llm-verifier` in root go.mod). The spec proposes migrating to `digital.vasic.llmsverifier`. **This plan uses the existing paths** and defers the migration to a separate task after all functionality is implemented. New modules (DocProcessor, LLMOrchestrator, VisionEngine) use `digital.vasic.*` convention from the start.

---

## Phase 1: LLMsVerifier Strategy Extension

**Module:** Existing repo at `/run/media/milosvasic/DATA4TB/Projects/LLMsVerifier`
**Go module:** `llm-verifier` (local path, replace directive in root go.mod)
**Key files to understand first:**
- `llm-verifier/llmverifier/verifier.go` — Verifier struct (line 17), CalculateScores (line 2371)
- `llm-verifier/llmverifier/models.go` — PerformanceScore (line 226), VerificationResult (line 5)
- `llm-verifier/config/` — Config struct

### Task 1.1: Strategy Interface & Types

**Files:**
- Create: `llm-verifier/llmverifier/strategy.go`
- Test: `llm-verifier/llmverifier/strategy_test.go`

- [ ] **Step 1: Write failing test for ScoringStrategy interface**

```go
// strategy_test.go
package llmverifier

import (
    "context"
    "testing"
    "time"

    "github.com/stretchr/testify/assert"
    "github.com/stretchr/testify/require"
)

func TestWeightConfig_Validate_SumsToOne(t *testing.T) {
    wc := WeightConfig{
        Responsiveness:       0.30,
        CodeCapability:       0.25,
        FeatureRichness:      0.25,
        Reliability:          0.20,
        VisionCapability:     0.00,
        InstructionFollowing: 0.00,
    }
    assert.NoError(t, wc.Validate())
}

func TestWeightConfig_Validate_FailsWhenNotSumToOne(t *testing.T) {
    wc := WeightConfig{
        Responsiveness: 0.50,
        CodeCapability: 0.30,
        // Sum = 0.80, not 1.0
    }
    assert.Error(t, wc.Validate())
}

func TestStrategyScore_ToPerformanceScore(t *testing.T) {
    ss := StrategyScore{
        Overall: 0.85,
        Breakdown: map[string]float64{
            "responsiveness":  0.90,
            "code_capability": 0.80,
            "feature_richness": 0.85,
            "reliability":     0.82,
        },
        Passed: true,
    }
    ps := ss.ToPerformanceScore()
    assert.Equal(t, 0.85, ps.OverallScore)
    assert.Equal(t, 0.90, ps.Responsiveness)
    assert.Equal(t, 0.80, ps.CodeCapability)
}

func TestThresholds_Check(t *testing.T) {
    th := Thresholds{
        MinOverallScore:      0.6,
        MaxLatency:           10 * time.Second,
        MinContextWindow:     50000,
        RequiredCapabilities: []string{"vision"},
    }
    assert.True(t, th.Check(0.7, 5*time.Second, 100000, []string{"vision", "streaming"}))
    assert.False(t, th.Check(0.5, 5*time.Second, 100000, []string{"vision"}))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /run/media/milosvasic/DATA4TB/Projects/LLMsVerifier && go test ./llm-verifier/llmverifier/ -run "TestWeightConfig|TestStrategyScore|TestThresholds" -v`
Expected: FAIL — types not defined

- [ ] **Step 3: Implement strategy types**

```go
// strategy.go
package llmverifier

import (
    "context"
    "fmt"
    "math"
    "time"
)

// TestCategory classifies verification tests
type TestCategory string

const (
    TestCategoryExistence      TestCategory = "existence"
    TestCategoryResponsiveness TestCategory = "responsiveness"
    TestCategoryLatency        TestCategory = "latency"
    TestCategoryFeature        TestCategory = "feature"
    TestCategoryCode           TestCategory = "code"
    TestCategoryVision         TestCategory = "vision"
    TestCategoryInstruction    TestCategory = "instruction"
    TestCategoryStability      TestCategory = "stability"
)

// WeightConfig defines scoring dimension weights
type WeightConfig struct {
    Responsiveness       float64
    CodeCapability       float64
    FeatureRichness      float64
    Reliability          float64
    VisionCapability     float64
    InstructionFollowing float64
}

// Validate ensures weights sum to 1.0 within tolerance
func (wc WeightConfig) Validate() error {
    sum := wc.Responsiveness + wc.CodeCapability + wc.FeatureRichness +
        wc.Reliability + wc.VisionCapability + wc.InstructionFollowing
    if math.Abs(sum-1.0) > 0.001 {
        return fmt.Errorf("weights must sum to 1.0, got %f", sum)
    }
    return nil
}

// TestResult is the outcome of a single VerificationTest run
type TestResult struct {
    TestID    string
    Category  TestCategory
    Passed    bool
    Score     float64
    Latency   time.Duration
    Error     error
    Details   map[string]any
    Timestamp time.Time
}

// StrategyScore is returned by ScoringStrategy.ScoreModel()
type StrategyScore struct {
    Overall   float64
    Breakdown map[string]float64
    Passed    bool
    Details   map[string]any
}

// ToPerformanceScore converts to existing PerformanceScore for backward compat
func (ss StrategyScore) ToPerformanceScore() PerformanceScore {
    return PerformanceScore{
        OverallScore:     ss.Overall,
        Responsiveness:   ss.Breakdown["responsiveness"],
        CodeCapability:   ss.Breakdown["code_capability"],
        FeatureRichness:  ss.Breakdown["feature_richness"],
        Reliability:      ss.Breakdown["reliability"],
        ValueProposition: ss.Breakdown["value_proposition"],
    }
}

// Thresholds defines minimum requirements for model viability
type Thresholds struct {
    MinOverallScore      float64
    MaxLatency           time.Duration
    MinContextWindow     int
    RequiredCapabilities []string
}

// Check verifies if a model meets thresholds
func (th Thresholds) Check(score float64, latency time.Duration, contextWindow int, capabilities []string) bool {
    if score < th.MinOverallScore {
        return false
    }
    if th.MaxLatency > 0 && latency > th.MaxLatency {
        return false
    }
    if contextWindow < th.MinContextWindow {
        return false
    }
    capSet := make(map[string]bool, len(capabilities))
    for _, c := range capabilities {
        capSet[c] = true
    }
    for _, req := range th.RequiredCapabilities {
        if !capSet[req] {
            return false
        }
    }
    return true
}

// ScoringStrategy is the pluggable scoring mechanism interface
type ScoringStrategy interface {
    Name() string
    Description() string
    WeightConfig() WeightConfig
    CustomTests() []VerificationTest
    ScoreModel(ctx context.Context, model ModelInfo, results []TestResult) (StrategyScore, error)
    FilterModels(models []ModelInfo) []ModelInfo
    MinimumThresholds() Thresholds
}

// VerificationTest is an individual test that a strategy can inject
type VerificationTest interface {
    ID() string
    Name() string
    Category() TestCategory
    Run(ctx context.Context, client *LLMClient) (TestResult, error)
    Weight() float64
    Required() bool
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd /run/media/milosvasic/DATA4TB/Projects/LLMsVerifier && go test ./llm-verifier/llmverifier/ -run "TestWeightConfig|TestStrategyScore|TestThresholds" -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/LLMsVerifier
git add llm-verifier/llmverifier/strategy.go llm-verifier/llmverifier/strategy_test.go
git commit -m "feat: add ScoringStrategy interface and supporting types"
```

### Task 1.2: DefaultStrategy Implementation

**Files:**
- Create: `llm-verifier/llmverifier/strategy_default.go`
- Add to: `llm-verifier/llmverifier/strategy_test.go`

- [ ] **Step 1: Write failing tests for DefaultStrategy**

```go
// Add to strategy_test.go
func TestDefaultStrategy_Name(t *testing.T) {
    ds := NewDefaultStrategy()
    assert.Equal(t, "default", ds.Name())
}

func TestDefaultStrategy_WeightConfig(t *testing.T) {
    ds := NewDefaultStrategy()
    wc := ds.WeightConfig()
    assert.NoError(t, wc.Validate())
    assert.Equal(t, 0.30, wc.Responsiveness)
    assert.Equal(t, 0.25, wc.CodeCapability)
    assert.Equal(t, 0.25, wc.FeatureRichness)
    assert.Equal(t, 0.20, wc.Reliability)
    assert.Equal(t, 0.0, wc.VisionCapability)
    assert.Equal(t, 0.0, wc.InstructionFollowing)
}

func TestDefaultStrategy_CustomTests_Empty(t *testing.T) {
    ds := NewDefaultStrategy()
    assert.Empty(t, ds.CustomTests())
}

func TestDefaultStrategy_FilterModels_PassesAll(t *testing.T) {
    ds := NewDefaultStrategy()
    models := []ModelInfo{{ID: "m1"}, {ID: "m2"}}
    filtered := ds.FilterModels(models)
    assert.Len(t, filtered, 2)
}

func TestDefaultStrategy_MinimumThresholds_Zero(t *testing.T) {
    ds := NewDefaultStrategy()
    th := ds.MinimumThresholds()
    assert.Equal(t, 0.0, th.MinOverallScore)
}

func TestDefaultStrategy_ScoreModel(t *testing.T) {
    ds := NewDefaultStrategy()
    model := ModelInfo{ID: "test-model"}
    results := []TestResult{
        {Category: TestCategoryResponsiveness, Score: 0.90, Passed: true},
        {Category: TestCategoryCode, Score: 0.80, Passed: true},
        {Category: TestCategoryFeature, Score: 0.85, Passed: true},
        {Category: TestCategoryExistence, Score: 0.82, Passed: true},
    }
    score, err := ds.ScoreModel(context.Background(), model, results)
    require.NoError(t, err)
    assert.True(t, score.Overall > 0)
    assert.True(t, score.Passed)
}
```

- [ ] **Step 2: Run tests — expect FAIL**

Run: `cd /run/media/milosvasic/DATA4TB/Projects/LLMsVerifier && go test ./llm-verifier/llmverifier/ -run "TestDefaultStrategy" -v`

- [ ] **Step 3: Implement DefaultStrategy**

```go
// strategy_default.go
package llmverifier

import "context"

// defaultStrategy wraps the existing scoring behavior
type defaultStrategy struct{}

// NewDefaultStrategy creates the default scoring strategy matching existing CalculateScores behavior
func NewDefaultStrategy() ScoringStrategy {
    return &defaultStrategy{}
}

func (ds *defaultStrategy) Name() string        { return "default" }
func (ds *defaultStrategy) Description() string { return "Default scoring strategy matching existing behavior" }

func (ds *defaultStrategy) WeightConfig() WeightConfig {
    return WeightConfig{
        Responsiveness:       0.30,
        CodeCapability:       0.25,
        FeatureRichness:      0.25,
        Reliability:          0.20,
        VisionCapability:     0.00,
        InstructionFollowing: 0.00,
    }
}

func (ds *defaultStrategy) CustomTests() []VerificationTest { return nil }

func (ds *defaultStrategy) FilterModels(models []ModelInfo) []ModelInfo { return models }

func (ds *defaultStrategy) MinimumThresholds() Thresholds { return Thresholds{} }

func (ds *defaultStrategy) ScoreModel(ctx context.Context, model ModelInfo, results []TestResult) (StrategyScore, error) {
    wc := ds.WeightConfig()
    scores := map[string]float64{
        "responsiveness":  0,
        "code_capability": 0,
        "feature_richness": 0,
        "reliability":     0,
    }
    counts := map[string]int{}

    for _, r := range results {
        switch r.Category {
        case TestCategoryResponsiveness, TestCategoryLatency:
            scores["responsiveness"] += r.Score
            counts["responsiveness"]++
        case TestCategoryCode:
            scores["code_capability"] += r.Score
            counts["code_capability"]++
        case TestCategoryFeature:
            scores["feature_richness"] += r.Score
            counts["feature_richness"]++
        case TestCategoryExistence:
            scores["reliability"] += r.Score
            counts["reliability"]++
        }
    }

    // Average per dimension
    for k, c := range counts {
        if c > 0 {
            scores[k] /= float64(c)
        }
    }

    overall := scores["responsiveness"]*wc.Responsiveness +
        scores["code_capability"]*wc.CodeCapability +
        scores["feature_richness"]*wc.FeatureRichness +
        scores["reliability"]*wc.Reliability

    return StrategyScore{
        Overall:   overall,
        Breakdown: scores,
        Passed:    true,
    }, nil
}
```

- [ ] **Step 4: Run tests — expect PASS**
- [ ] **Step 5: Commit**

```bash
git add llm-verifier/llmverifier/strategy_default.go llm-verifier/llmverifier/strategy_test.go
git commit -m "feat: implement DefaultStrategy wrapping existing scoring behavior"
```

### Task 1.3: StrategyBuilder with Fluent API

**Files:**
- Create: `llm-verifier/llmverifier/strategy_builder.go`
- Create: `llm-verifier/llmverifier/strategy_builder_test.go`

- [ ] **Step 1: Write failing tests for StrategyBuilder**

```go
// strategy_builder_test.go
package llmverifier

import (
    "testing"
    "time"

    "github.com/stretchr/testify/assert"
    "github.com/stretchr/testify/require"
)

func TestStrategyBuilder_Build_MinimalValid(t *testing.T) {
    s, err := NewStrategyBuilder("test").
        WithDescription("Test strategy").
        WithWeights(WeightConfig{
            Responsiveness: 0.5,
            Reliability:    0.5,
        }).
        Build()
    require.NoError(t, err)
    assert.Equal(t, "test", s.Name())
    assert.Equal(t, "Test strategy", s.Description())
}

func TestStrategyBuilder_Build_InvalidWeights(t *testing.T) {
    _, err := NewStrategyBuilder("bad").
        WithWeights(WeightConfig{Responsiveness: 0.5}).
        Build()
    assert.Error(t, err)
}

func TestStrategyBuilder_WithMinScore(t *testing.T) {
    s, err := NewStrategyBuilder("test").
        WithWeights(WeightConfig{Responsiveness: 1.0}).
        MinScore(0.7).
        Build()
    require.NoError(t, err)
    th := s.MinimumThresholds()
    assert.Equal(t, 0.7, th.MinOverallScore)
}

func TestStrategyBuilder_WithMaxLatency(t *testing.T) {
    s, err := NewStrategyBuilder("test").
        WithWeights(WeightConfig{Responsiveness: 1.0}).
        MaxLatency(5 * time.Second).
        Build()
    require.NoError(t, err)
    assert.Equal(t, 5*time.Second, s.MinimumThresholds().MaxLatency)
}

func TestStrategyBuilder_RequireCapability(t *testing.T) {
    s, err := NewStrategyBuilder("test").
        WithWeights(WeightConfig{Responsiveness: 1.0}).
        RequireCapability("vision").
        RequireCapability("streaming").
        Build()
    require.NoError(t, err)
    th := s.MinimumThresholds()
    assert.Contains(t, th.RequiredCapabilities, "vision")
    assert.Contains(t, th.RequiredCapabilities, "streaming")
}

func TestStrategyBuilder_FilterModels_ByCapability(t *testing.T) {
    s, err := NewStrategyBuilder("test").
        WithWeights(WeightConfig{Responsiveness: 1.0}).
        RequireCapability("vision").
        Build()
    require.NoError(t, err)
    models := []ModelInfo{
        {ID: "m1", SupportsVision: true},
        {ID: "m2", SupportsVision: false},
    }
    filtered := s.FilterModels(models)
    assert.Len(t, filtered, 1)
    assert.Equal(t, "m1", filtered[0].ID)
}
```

- [ ] **Step 2: Run tests — expect FAIL**
- [ ] **Step 3: Implement StrategyBuilder**

Create `strategy_builder.go` implementing `NewStrategyBuilder()` that returns a `*StrategyBuilder` with fluent methods: `WithDescription()`, `WithWeights()`, `WithRecipe()`, `WithTest()`, `RequireCapability()`, `MinContextWindow()`, `MinScore()`, `MaxLatency()`, `Build()`. The `Build()` method validates weights and returns a `*builtStrategy` that implements `ScoringStrategy`.

- [ ] **Step 4: Run tests — expect PASS**
- [ ] **Step 5: Commit**

### Task 1.4: Recipe System

**Files:**
- Create: `llm-verifier/llmverifier/recipes/recipe.go`
- Create: `llm-verifier/llmverifier/recipes/vision.go`
- Create: `llm-verifier/llmverifier/recipes/instruction.go`
- Create: `llm-verifier/llmverifier/recipes/context_window.go`
- Create: `llm-verifier/llmverifier/recipes/streaming.go`
- Create: `llm-verifier/llmverifier/recipes/long_session.go`
- Create: `llm-verifier/llmverifier/recipes/recipes_test.go`

- [ ] **Step 1: Write failing tests for RecipeStep interface and built-in recipes**

Test that each recipe: (a) implements RecipeStep, (b) adds the correct VerificationTest to the builder, (c) the test has correct ID/Name/Category/Weight/Required values.

- [ ] **Step 2: Run tests — expect FAIL**
- [ ] **Step 3: Implement RecipeStep interface and 5 built-in recipes**

Each recipe implements `Apply(builder *StrategyBuilder) *StrategyBuilder` which calls `builder.WithTest()` to add its VerificationTest. Each VerificationTest's `Run()` method sends appropriate prompts to the LLMClient and evaluates responses.

- [ ] **Step 4: Run tests — expect PASS**
- [ ] **Step 5: Commit**

### Task 1.5: VerifyWithStrategy Entry Point

**Files:**
- Modify: `llm-verifier/llmverifier/verifier.go` (add new method, don't touch existing)
- Create: `llm-verifier/llmverifier/strategy_integration_test.go`

- [ ] **Step 1: Write failing integration test**

```go
// strategy_integration_test.go
package llmverifier

import (
    "context"
    "testing"

    "llm-verifier/config"
    "github.com/stretchr/testify/assert"
    "github.com/stretchr/testify/require"
)

func TestVerifyWithStrategy_DefaultStrategy_EmptyModels(t *testing.T) {
    cfg := &config.Config{
        Concurrency: 1,
        Timeout:     10 * time.Second,
    }
    v := New(cfg)
    strategy := NewDefaultStrategy()
    // With empty models, should return empty results
    results, scores, err := v.VerifyWithStrategy(context.Background(), strategy, []ModelInfo{})
    assert.NoError(t, err)
    assert.Empty(t, results)
    assert.Empty(t, scores)
}

func TestCalculateScoresWithStrategy(t *testing.T) {
    cfg := &config.Config{}
    v := New(cfg)
    vr := VerificationResult{
        Availability: AvailabilityResult{Exists: true, Responsive: true},
    }
    strategy := NewDefaultStrategy()
    score, err := v.CalculateScoresWithStrategy(vr, strategy)
    require.NoError(t, err)
    assert.True(t, score.Overall >= 0)
}
```

- [ ] **Step 2: Run tests — expect FAIL**
- [ ] **Step 3: Add VerifyWithStrategy and CalculateScoresWithStrategy to verifier.go**

Add at end of file (after line 2815), never modifying existing functions.

**Note:** The existing `verifySingleModel` has signature `func (v *Verifier) verifySingleModel(client *LLMClient, modelName, endpoint string) (VerificationResult, error)`. The new method creates clients per-model using the existing `NewLLMClient()` constructor.

```go
// VerifyWithStrategy runs verification using a custom scoring strategy
func (v *Verifier) VerifyWithStrategy(ctx context.Context, strategy ScoringStrategy, models []ModelInfo) ([]VerificationResult, []StrategyScore, error) {
    filtered := strategy.FilterModels(models)
    var results []VerificationResult
    var scores []StrategyScore
    for _, model := range filtered {
        // Create client using model's endpoint and API key from config
        client := NewLLMClient(model.Endpoint, v.cfg.GetAPIKeyForProvider(model.Organization), nil)
        vr, err := v.verifySingleModel(client, model.ID, model.Endpoint)
        if err != nil {
            vr = VerificationResult{ModelInfo: model, Error: err.Error()}
        }
        results = append(results, vr)
        // Run custom tests from strategy
        var testResults []TestResult
        for _, test := range strategy.CustomTests() {
            tr, testErr := test.Run(ctx, client)
            if testErr != nil && test.Required() {
                return results, scores, fmt.Errorf("required test %s failed: %w", test.ID(), testErr)
            }
            testResults = append(testResults, tr)
        }
        score, scoreErr := strategy.ScoreModel(ctx, model, testResults)
        if scoreErr != nil {
            return results, scores, scoreErr
        }
        scores = append(scores, score)
    }
    return results, scores, nil
}

// CalculateScoresWithStrategy scores a result using a custom strategy
func (v *Verifier) CalculateScoresWithStrategy(result VerificationResult, strategy ScoringStrategy) (StrategyScore, error) {
    return strategy.ScoreModel(context.Background(), result.ModelInfo, nil)
}
```

**Important:** If `GetAPIKeyForProvider()` does not exist on Config, add a helper method that looks up the API key from the config's LLM entries by provider name. Check the existing `config.Config` struct for how API keys are stored.

- [ ] **Step 4: Run all tests — expect ALL PASS (existing + new)**

Run: `cd /run/media/milosvasic/DATA4TB/Projects/LLMsVerifier && go test ./llm-verifier/llmverifier/ -v -count=1`

- [ ] **Step 5: Commit**

### Task 1.6: Security, Stress, E2E, and Automation Tests

**Files:**
- Create: `llm-verifier/llmverifier/strategy_security_test.go`
- Create: `llm-verifier/llmverifier/strategy_stress_test.go`
- Create: `llm-verifier/llmverifier/strategy_e2e_test.go`
- Create: `llm-verifier/llmverifier/strategy_automation_test.go`
- Create: `llm-verifier/llmverifier/recipes/recipes_stress_test.go`
- Create: `llm-verifier/llmverifier/recipes/recipes_security_test.go`

- [ ] **Step 1: Write security tests** — API key masking in strategy output, WeightConfig injection, malicious test names
- [ ] **Step 2: Write stress tests** — concurrent ScoreModel calls, large model lists (1000+), rapid builder creation
- [ ] **Step 3: Write E2E tests** — full VerifyWithStrategy pipeline with mock models
- [ ] **Step 4: Write automation tests** — build validation, `go vet`, race detection
- [ ] **Step 5: Run all tests with race detection**

Run: `go test ./llm-verifier/llmverifier/... -race -count=1 -v`

- [ ] **Step 6: Commit**

### Task 1.7: Documentation

**Files:**
- Create: `llm-verifier/llmverifier/recipes/README.md` — Recipe authoring guide
- Modify: `README.md` — Add Strategy section
- Create: `docs/STRATEGY_GUIDE.md` — Full strategy authoring guide
- Create: `docs/VIDEO_COURSE_STRATEGY.md` — 5 episode scripts

- [ ] **Step 1: Write recipe README**
- [ ] **Step 2: Update main README with Strategy section**
- [ ] **Step 3: Write strategy guide**
- [ ] **Step 4: Write video course scripts**
- [ ] **Step 5: Commit**

### Task 1.8: Push to All Remotes

- [ ] **Step 1: Run full test suite**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/LLMsVerifier
go test ./... -race -count=1
```

- [ ] **Step 2: Push to all remotes**

```bash
git push origin main
git push gitlab main
# (HelixDevelopment remotes if configured)
```

---

## Phase 2: DocProcessor Module (New)

**Location:** `/run/media/milosvasic/DATA4TB/Projects/DocProcessor`
**Go module:** `digital.vasic.docprocessor`

### Task 2.1: Initialize Module Repository

- [ ] **Step 1: Create repos on GitHub and GitLab**

```bash
gh repo create vasic-digital/DocProcessor --public --description "Documentation processing and feature map extraction for QA automation"
gh repo create HelixDevelopment/DocProcessor --public --description "Documentation processing and feature map extraction for QA automation"
# GitLab equivalents via glab CLI
glab repo create DocProcessor --group vasic-digital --public --description "Documentation processing and feature map extraction for QA automation"
glab repo create DocProcessor --group HelixDevelopment --public --description "Documentation processing and feature map extraction for QA automation"
```

- [ ] **Step 2: Initialize local project**

```bash
mkdir -p /run/media/milosvasic/DATA4TB/Projects/DocProcessor
cd /run/media/milosvasic/DATA4TB/Projects/DocProcessor
git init
go mod init digital.vasic.docprocessor
```

- [ ] **Step 3: Create directory structure**

```bash
mkdir -p cmd/docprocessor pkg/{loader,feature,coverage,docgraph,llm,config} Upstreams
```

- [ ] **Step 4: Create go.mod with dependencies**

```
module digital.vasic.docprocessor

go 1.24.0

require (
    github.com/stretchr/testify v1.11.1
    gopkg.in/yaml.v3 v3.0.1
)
```

- [ ] **Step 5: Create LICENSE (Apache-2.0), .gitignore, Makefile, .env.example**
- [ ] **Step 6: Create Upstreams sync scripts (4 remotes)**
- [ ] **Step 7: Commit initial structure**

### Task 2.2: Loader Package — Types & Interface

**Files:**
- Create: `pkg/loader/loader.go` — Loader interface, Document struct
- Create: `pkg/loader/scanner.go` — recursive project tree scanner
- Create: `pkg/loader/markdown.go` — Markdown parser (headings, links, sections)
- Create: `pkg/loader/yaml.go` — YAML document parser
- Test: `pkg/loader/loader_test.go`, `pkg/loader/scanner_test.go`, `pkg/loader/markdown_test.go`, `pkg/loader/yaml_test.go`

- [ ] **Step 1: Write failing tests for Document type and Loader interface**
- [ ] **Step 2: Implement loader.go with types**
- [ ] **Step 3: Write failing tests for scanner (auto-discovery patterns)**
- [ ] **Step 4: Implement scanner.go**
- [ ] **Step 5: Write failing tests for markdown parser**
- [ ] **Step 6: Implement markdown.go**
- [ ] **Step 7: Write failing tests for YAML parser**
- [ ] **Step 8: Implement yaml.go**
- [ ] **Step 9: Run all tests — expect PASS**
- [ ] **Step 10: Commit**

### Task 2.3: Feature Package — Types & Builder

**Files:**
- Create: `pkg/feature/feature.go` — Feature, FeatureMap, Workflow types
- Create: `pkg/feature/category.go` — FeatureCategory enum
- Create: `pkg/feature/screen.go` — ExpectedScreen type
- Create: `pkg/feature/builder.go` — FeatureMapBuilder interface + impl
- Tests: `pkg/feature/*_test.go`

- [ ] **Step 1-8: TDD cycle for feature types, categories, screens, builder**
- [ ] **Step 9: Commit**

### Task 2.4: LLM Package — Agent Interface & Prompts

**Files:**
- Create: `pkg/llm/agent.go` — LLMAgent interface
- Create: `pkg/llm/prompts.go` — Prompt templates for feature extraction
- Tests: `pkg/llm/agent_test.go`, `pkg/llm/prompts_test.go`

- [ ] **Step 1: Write tests for LLMAgent mock implementation**
- [ ] **Step 2: Implement LLMAgent interface**
- [ ] **Step 3: Write tests for prompt templates (structured JSON output)**
- [ ] **Step 4: Implement prompts.go**
- [ ] **Step 5: Commit**

### Task 2.5: Coverage Package — Tracker

**Files:**
- Create: `pkg/coverage/tracker.go` — CoverageTracker interface + thread-safe impl
- Create: `pkg/coverage/report.go` — CoverageReport generation
- Tests: `pkg/coverage/tracker_test.go`, `pkg/coverage/tracker_stress_test.go`

- [ ] **Step 1: Write failing tests for CoverageTracker (mark verified/failed/skipped)**
- [ ] **Step 2: Implement with sync.RWMutex**
- [ ] **Step 3: Write stress tests (concurrent marks from multiple goroutines)**
- [ ] **Step 4: Run with -race flag**
- [ ] **Step 5: Commit**

### Task 2.6: DocGraph Package

**Files:**
- Create: `pkg/docgraph/graph.go` — inter-document link graph
- Create: `pkg/docgraph/export.go` — JSON, Mermaid export
- Tests: `pkg/docgraph/graph_test.go`

- [ ] **Step 1-4: TDD cycle**
- [ ] **Step 5: Commit**

### Task 2.7: Config Package

**Files:**
- Create: `pkg/config/config.go` — configuration from .env
- Test: `pkg/config/config_test.go`

- [ ] **Step 1-4: TDD cycle**
- [ ] **Step 5: Commit**

### Task 2.8: Integration, E2E, Security, Stress, Automation Tests

**Files:**
- Create: `pkg/loader/loader_integration_test.go`
- Create: `pkg/feature/builder_integration_test.go`
- Create: `pkg/*/..._security_test.go`
- Create: `pkg/*/..._stress_test.go`
- Create: `pkg/*/..._e2e_test.go`
- Create: `pkg/*/..._automation_test.go`

- [ ] **Step 1: Write integration tests (loader → builder → coverage pipeline)**
- [ ] **Step 2: Write E2E tests (full doc processing with mock LLM)**
- [ ] **Step 3: Write security tests (path traversal, large files)**
- [ ] **Step 4: Write stress tests (1000 docs, concurrent coverage)**
- [ ] **Step 5: Write automation tests (build, vet, race)**
- [ ] **Step 6: Run all: `go test ./... -race -count=1`**
- [ ] **Step 7: Commit**

### Task 2.9: Documentation & Push

- [ ] **Step 1: Write README.md, ARCHITECTURE.md, API_REFERENCE.md**
- [ ] **Step 2: Write USER_GUIDE.md, CONTRIBUTING.md, CHANGELOG.md**
- [ ] **Step 3: Write CLAUDE.md, AGENTS.md**
- [ ] **Step 4: Write VIDEO_COURSE.md (5 episodes)**
- [ ] **Step 5: Add Mermaid diagrams to ARCHITECTURE.md**
- [ ] **Step 6: Run full test suite**
- [ ] **Step 7: Commit and push to all 4 remotes**

---

## Phase 3: LLMOrchestrator Module (New)

**Location:** `/run/media/milosvasic/DATA4TB/Projects/LLMOrchestrator`
**Go module:** `digital.vasic.llmorchestrator`

### Task 3.1: Initialize Module Repository

Same pattern as Task 2.1. Create repos on GitHub/GitLab (vasic-digital + HelixDevelopment), init Go module, create directory structure:

```
pkg/{agent,adapter,protocol,parser,config}
cmd/orchestrator
Upstreams/
```

- [ ] **Steps 1-7: Init, structure, go.mod, LICENSE, Makefile, Upstreams, commit**

### Task 3.2: Protocol Package — Message Types & Transports

**Files:**
- Create: `pkg/protocol/message.go` — Message types (prompt, response), Attachment, StreamChunk
- Create: `pkg/protocol/pipe.go` — PipeTransport (stdin/stdout JSON-lines)
- Create: `pkg/protocol/file.go` — FileTransport (inbox/outbox/shared)
- Tests: `pkg/protocol/*_test.go`, `pkg/protocol/protocol_stress_test.go`

- [ ] **Step 1-10: TDD cycle for message types, pipe transport, file transport**
- [ ] **Step 11: Stress tests for concurrent pipe/file operations**
- [ ] **Step 12: Commit**

### Task 3.3: Agent Package — Interface & Pool

**Files:**
- Create: `pkg/agent/agent.go` — Agent interface, Response, AgentCapabilities, ModelInfo types
- Create: `pkg/agent/pool.go` — AgentPool with mutex-safe acquire/release
- Create: `pkg/agent/health.go` — HealthMonitor, circuit breaker per agent
- Tests: `pkg/agent/*_test.go`

- [ ] **Step 1-12: TDD cycle for Agent types, pool acquire/release, health monitoring**
- [ ] **Step 13: Commit**

### Task 3.4: Parser Package — Response Extraction

**Files:**
- Create: `pkg/parser/parser.go` — ResponseParser interface + impl
- Create: `pkg/parser/action.go` — Action types (click, type, scroll, navigate)
- Tests: `pkg/parser/parser_test.go`, `pkg/parser/parser_fuzz_test.go`

- [ ] **Step 1-8: TDD cycle for JSON extraction, action parsing, issue extraction**
- [ ] **Step 9: Fuzz tests for malformed LLM output**
- [ ] **Step 10: Commit**

### Task 3.5: Adapter Package — 5 CLI Adapters

**Files:**
- Create: `pkg/adapter/base.go` — BaseAdapter (shared process management)
- Create: `pkg/adapter/opencode.go` — OpenCode headless adapter
- Create: `pkg/adapter/claudecode.go` — Claude Code adapter
- Create: `pkg/adapter/gemini.go` — Gemini CLI adapter
- Create: `pkg/adapter/junie.go` — Junie adapter
- Create: `pkg/adapter/qwencode.go` — Qwen Code adapter
- Tests: `pkg/adapter/*_test.go`, `pkg/adapter/adapter_integration_test.go`

- [ ] **Step 1: Write failing tests for BaseAdapter** (process spawn, pipe setup, graceful shutdown, timeout)
- [ ] **Step 2: Implement BaseAdapter**
- [ ] **Step 3-12: Implement each adapter (2 steps each: test + implement)**

Each adapter overrides: `buildCommand() *exec.Cmd`, `parseResponse(raw []byte) (*Response, error)`, `formatPrompt(prompt string) []byte`

- [ ] **Step 13: Integration test with mock subprocess**
- [ ] **Step 14: Commit**

### Task 3.6: Config Package

- [ ] **Steps 1-4: TDD cycle for .env loading, agent path resolution**
- [ ] **Step 5: Commit**

### Task 3.7: All Test Types + Documentation + Push

Same pattern as Tasks 2.8 and 2.9.

- [ ] **Step 1-6: Security, stress, E2E, automation tests**
- [ ] **Step 7-12: All documentation files**
- [ ] **Step 13: Full test suite with race detection**
- [ ] **Step 14: Commit and push to all 4 remotes**

---

## Phase 4: VisionEngine Module (New)

**Location:** `/run/media/milosvasic/DATA4TB/Projects/VisionEngine`
**Go module:** `digital.vasic.visionengine`
**Special dependency:** `gocv.io/x/gocv` (requires OpenCV 4.x system libraries)

### Task 4.1: Initialize Module Repository

Same pattern. Create repos, init, structure:

```
pkg/{analyzer,opencv,llmvision,graph,config}
```

**go.mod:**
```
module digital.vasic.visionengine

go 1.24.0

require (
    gocv.io/x/gocv v0.37.0
    github.com/stretchr/testify v1.11.1
)
```

- [ ] **Steps 1-7: Init, structure, go.mod, commit**

### Task 4.2: Analyzer Package — Interface & Types

**Files:**
- Create: `pkg/analyzer/analyzer.go` — Analyzer interface + composite impl
- Create: `pkg/analyzer/screen.go` — ScreenAnalysis, ScreenIdentity, ScreenDiff
- Create: `pkg/analyzer/element.go` — UIElement, TextRegion, VisualIssue, Rect
- Tests: `pkg/analyzer/*_test.go`

- [ ] **Step 1-8: TDD cycle for all types and Analyzer interface**
- [ ] **Step 9: Commit**

### Task 4.3: OpenCV Package (build-tagged)

**Files (all with `//go:build vision`):**
- Create: `pkg/opencv/differ.go` — SSIM, pixel diff
- Create: `pkg/opencv/detector.go` — edge detection, contour detection
- Create: `pkg/opencv/color.go` — dominant colors, contrast ratio
- Create: `pkg/opencv/video.go` — frame extraction, scene changes
- Create: `pkg/opencv/stub.go` (without build tag) — stub returning "OpenCV not available"
- Tests: `pkg/opencv/*_test.go` (with `//go:build vision`)

- [ ] **Step 1-10: TDD cycle for each OpenCV operation**
- [ ] **Step 11: Stub implementations for non-vision builds**
- [ ] **Step 12: Commit**

### Task 4.4: LLM Vision Package

**Files:**
- Create: `pkg/llmvision/provider.go` — VisionProvider interface
- Create: `pkg/llmvision/openai.go` — GPT-4o adapter (HTTP)
- Create: `pkg/llmvision/anthropic.go` — Claude adapter (HTTP)
- Create: `pkg/llmvision/gemini.go` — Gemini adapter (HTTP)
- Create: `pkg/llmvision/qwen.go` — Qwen-VL adapter (HTTP)
- Tests: `pkg/llmvision/*_test.go`

- [ ] **Step 1-10: TDD cycle for VisionProvider and each HTTP adapter**
- [ ] **Step 11: Commit**

### Task 4.5: Graph Package — NavigationGraph

**Files:**
- Create: `pkg/graph/graph.go` — NavigationGraph interface + impl
- Create: `pkg/graph/pathfinder.go` — BFS pathfinding
- Create: `pkg/graph/export.go` — DOT, JSON, Mermaid export
- Tests: `pkg/graph/*_test.go`, `pkg/graph/graph_stress_test.go`

- [ ] **Step 1-10: TDD cycle for graph operations, pathfinding, export**
- [ ] **Step 11: Stress tests (500+ nodes, concurrent add/query)**
- [ ] **Step 12: Commit**

### Task 4.6: Config + All Test Types + Documentation + Push

Same pattern as previous phases.

---

## Phase 5: HelixQA New Packages

**Location:** `/run/media/milosvasic/DATA4TB/Projects/Yole/HelixQA`
**Go module:** `digital.vasic.helixqa`

### Task 5.0: Update go.mod with New Dependencies

**File:** `go.mod`

- [ ] **Step 1: Add module dependencies**

```
require (
    digital.vasic.challenges v0.0.0
    digital.vasic.docprocessor v0.0.0
    digital.vasic.llmorchestrator v0.0.0
    digital.vasic.visionengine v0.0.0
    github.com/stretchr/testify v1.11.1
    gopkg.in/yaml.v3 v3.0.1
)

replace (
    digital.vasic.challenges => ../Challenges
    digital.vasic.containers => ../Containers
    digital.vasic.docprocessor => ../DocProcessor
    digital.vasic.llmorchestrator => ../LLMOrchestrator
    digital.vasic.visionengine => ../VisionEngine
)
```

- [ ] **Step 2: Run `go mod tidy`**
- [ ] **Step 3: Verify existing tests still pass: `go test ./... -race -count=1`**
- [ ] **Step 4: Commit**

### Task 5.1: pkg/session — Recording & Timeline

**Files:**
- Create: `pkg/session/recorder.go` — SessionRecorder struct
- Create: `pkg/session/timeline.go` — TimelineEvent, Timeline
- Create: `pkg/session/video.go` — VideoManager (ffmpeg wrapper)
- Tests: `pkg/session/recorder_test.go`, `pkg/session/timeline_test.go`, `pkg/session/video_test.go`, `pkg/session/session_stress_test.go`

- [ ] **Step 1-12: TDD cycle for Timeline events, SessionRecorder, VideoManager**
- [ ] **Step 13: Stress tests (concurrent events, multiple platform videos)**
- [ ] **Step 14: Commit**

### Task 5.2: pkg/navigator — Navigation Engine

**Files:**
- Create: `pkg/navigator/engine.go` — NavigationEngine struct
- Create: `pkg/navigator/executor.go` — ActionExecutor interface
- Create: `pkg/navigator/adb.go` — ADBExecutor
- Create: `pkg/navigator/playwright.go` — PlaywrightExecutor
- Create: `pkg/navigator/x11.go` — X11Executor
- Create: `pkg/navigator/state.go` — StateTracker
- Tests: `pkg/navigator/*_test.go`, `pkg/navigator/navigator_stress_test.go`

- [ ] **Step 1: Write failing tests for ActionExecutor interface and mock impl**
- [ ] **Step 2: Implement executor.go with interface**
- [ ] **Step 3-8: TDD for ADB, Playwright, X11 executors (mock CommandRunner)**
- [ ] **Step 9-10: TDD for StateTracker**
- [ ] **Step 11-12: TDD for NavigationEngine (NavigateTo, ExploreUnknown, PerformAction)**
- [ ] **Step 13: Stress tests**
- [ ] **Step 14: Commit**

### Task 5.3: pkg/issuedetector — LLM-Powered Bug Detection

**Files:**
- Create: `pkg/issuedetector/detector.go` — IssueDetector struct
- Create: `pkg/issuedetector/categories.go` — issue categories & severity
- Create: `pkg/issuedetector/prompts.go` — LLM prompts for issue analysis
- Tests: `pkg/issuedetector/*_test.go`

- [ ] **Step 1-8: TDD cycle for issue detection, categorization, ticket creation**
- [ ] **Step 9: Commit**

### Task 5.4: pkg/autonomous — Session Coordinator

**Files:**
- Create: `pkg/autonomous/coordinator.go` — SessionCoordinator
- Create: `pkg/autonomous/worker.go` — PlatformWorker
- Create: `pkg/autonomous/phase.go` — PhaseManager
- Create: `pkg/autonomous/result.go` — SessionResult, SessionStatus
- Create: `pkg/autonomous/adapters.go` — agentLLMAdapter bridge
- Tests: `pkg/autonomous/*_test.go`

- [ ] **Step 1-4: TDD for PhaseManager state machine**
- [ ] **Step 5-6: TDD for agentLLMAdapter bridge**
- [ ] **Step 7-10: TDD for PlatformWorker (RunDocDriven, RunCuriosityDriven)**
- [ ] **Step 11-14: TDD for SessionCoordinator (Run, Pause, Resume, Cancel)**
- [ ] **Step 15: Integration test — full session lifecycle with mocks**
- [ ] **Step 16: Commit**

### Task 5.4b: Resilience Layer

**Files:**
- Create: `pkg/autonomous/retry.go` — retry wrapper with exponential backoff
- Create: `pkg/autonomous/fallback.go` — vision provider fallback chain, graceful degradation
- Create: `pkg/autonomous/sanitize.go` — prompt injection protection, response sanitization
- Tests: `pkg/autonomous/retry_test.go`, `pkg/autonomous/fallback_test.go`, `pkg/autonomous/sanitize_test.go`

Implements spec Section 10 (Resilience & Error Handling):
1. Retry policy: configurable retries with exponential backoff (1s, 2s, 4s)
2. Malformed JSON fallback: regex extraction → re-prompt → skip after 3 failures
3. Vision provider fallback chain: score-ranked providers, GoCV-only fallback
4. Agent circuit breaker: 3 failures → 60s open → pool replacement
5. Graceful degradation: full → degraded vision → degraded navigation → abort
6. Prompt injection: sanitize paths, shell metacharacters, excessive length

- [ ] **Step 1: Write failing tests for retry wrapper**
- [ ] **Step 2: Implement retry.go**
- [ ] **Step 3: Write failing tests for fallback chain**
- [ ] **Step 4: Implement fallback.go**
- [ ] **Step 5: Write failing tests for sanitization (path traversal, shell injection)**
- [ ] **Step 6: Implement sanitize.go**
- [ ] **Step 7: Run tests: `go test ./pkg/autonomous/ -run "Retry|Fallback|Sanitize" -v`**
- [ ] **Step 8: Commit**

### Task 5.4c: Build Tag Conditional Compilation

**Files:**
- Create: `pkg/autonomous/vision_opencv.go` — with `//go:build vision` tag, imports VisionEngine's OpenCV layer
- Create: `pkg/autonomous/vision_stub.go` — without build tag, stub returning "OpenCV not available"

- [ ] **Step 1: Write vision_stub.go** with fallback implementations
- [ ] **Step 2: Write vision_opencv.go** with real GoCV integrations (build-tagged)
- [ ] **Step 3: Verify build without vision tag: `go build ./...`**
- [ ] **Step 4: Verify build with vision tag: `go build -tags vision ./...`**
- [ ] **Step 5: Commit**

### Task 5.5: Update Config Package

**File:** `pkg/config/config.go`

- [ ] **Step 1: Write failing tests for AutonomousConfig fields**

Add `Autonomous AutonomousConfig` field to existing Config struct (line 54 of config.go):
```go
type AutonomousConfig struct {
    Enabled          bool
    Platforms        []Platform
    Timeout          time.Duration
    CoverageTarget   float64
    CuriosityEnabled bool
    CuriosityTimeout time.Duration
    EnvFile          string
    ProjectRoot      string
}
```

- [ ] **Step 2: Implement — add to Config, update Validate(), update DefaultConfig()**
- [ ] **Step 3: Verify existing config tests still pass**
- [ ] **Step 4: Commit**

### Task 5.6: CLI — Add "autonomous" Subcommand

**File:** `cmd/helixqa/main.go`

- [ ] **Step 1: Add `cmdAutonomous()` function** (same pattern as cmdRun)
- [ ] **Step 2: Add "autonomous" case to main() switch**
- [ ] **Step 3: Wire flags: --project, --platforms, --env, --timeout, --coverage-target, --output, --report**
- [ ] **Step 4: Build and test CLI: `go build ./cmd/helixqa && ./bin/helixqa autonomous --help`**
- [ ] **Step 5: Commit**

### Task 5.6b: Create Comprehensive .env.example

**File:** `.env.example`

- [ ] **Step 1: Create .env.example** with all ~50 environment variables from spec Section 8, organized by section (Master Switch, LLMsVerifier, API Keys, CLI Agents, Vision Engine, Doc Processor, Recording, Platform-specific, Output)
- [ ] **Step 2: Commit**

### Task 5.6c: Modify Existing Orchestrator Package

**Files:**
- Modify: `pkg/orchestrator/orchestrator.go` — add autonomous session type support
- Test: `pkg/orchestrator/orchestrator_autonomous_test.go`

The existing orchestrator needs to recognize the "autonomous" test type and delegate to SessionCoordinator. Add a `RunAutonomous(ctx) (*Result, error)` method alongside existing `Run()`.

- [ ] **Step 1: Write failing test for RunAutonomous**
- [ ] **Step 2: Implement RunAutonomous** — creates SessionCoordinator, calls Run, converts SessionResult to existing Result type
- [ ] **Step 3: Verify existing orchestrator tests still pass**
- [ ] **Step 4: Commit**

### Task 5.7: All Test Types

- [ ] **Step 1: E2E tests — full session lifecycle via CLI mock**
- [ ] **Step 2: Security tests — .env key masking, path traversal in evidence dirs**
- [ ] **Step 3: Stress tests — concurrent workers, large feature maps**
- [ ] **Step 4: Automation tests — build, vet, race detection**
- [ ] **Step 5: Run all: `go test ./... -race -count=1`**
- [ ] **Step 6: Verify existing 235 tests still pass**
- [ ] **Step 7: Commit**

---

## Phase 6: HelixQA Enhanced Packages

### Task 6.1: Enhanced Detector (LLM Crash Analysis)

**Files:**
- Modify: `pkg/detector/detector.go` — add LLMCrashAnalyzer interface, EnhancedDetectionResult
- Create: `pkg/detector/llm_analyzer.go` — LLM-powered crash analysis
- Test: `pkg/detector/llm_analyzer_test.go`

- [ ] **Step 1: Write failing tests for LLMCrashAnalyzer**
- [ ] **Step 2: Implement — new interface, does NOT change existing Detector behavior**
- [ ] **Step 3: Verify existing detector tests pass**
- [ ] **Step 4: Commit**

### Task 6.2: Enhanced Validator (Semantic Evaluation)

**Files:**
- Modify: `pkg/validator/validator.go` — add LLMValidator interface, SemanticValidation field
- Create: `pkg/validator/llm_validator.go`
- Test: `pkg/validator/llm_validator_test.go`

- [ ] **Steps 1-4: Same pattern as 6.1**

### Task 6.3: Enhanced Evidence (LLM-Annotated Screenshots)

**Files:**
- Modify: `pkg/evidence/collector.go` — add AnnotateScreenshot(), AnnotatedItem type
- Create: `pkg/evidence/annotator.go`
- Test: `pkg/evidence/annotator_test.go`

- [ ] **Steps 1-4: Same pattern**

### Task 6.4: Enhanced Ticket (Video References, Suggested Fixes)

**Files:**
- Modify: `pkg/ticket/ticket.go` — add VideoReference, LLMSuggestedFix fields
- Test: `pkg/ticket/ticket_enhanced_test.go`

- [ ] **Steps 1-4: Same pattern**

### Task 6.5: Enhanced Reporter (Executive Summary, Nav Maps)

**Files:**
- Modify: `pkg/reporter/reporter.go` — add ExecutiveSummary, NavigationMapEmbed
- Test: `pkg/reporter/reporter_enhanced_test.go`

- [ ] **Steps 1-4: Same pattern**

### Task 6.6: Enhanced TestBank (LLM-Generated Test Cases)

**Files:**
- Modify: `pkg/testbank/manager.go` — add GenerateFromFeatureMap(), ExpandEdgeCases()
- Test: `pkg/testbank/manager_enhanced_test.go`

- [ ] **Steps 1-4: Same pattern**

### Task 6.7: Full Test Suite Verification

- [ ] **Step 1: Run ALL tests (existing 235 + all new)**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole/HelixQA
go test ./... -race -count=1 -v
```

- [ ] **Step 2: Verify 0 failures**
- [ ] **Step 3: Commit and push to all 4 remotes**

---

## Phase 7: Documentation & Video Courses

### Task 7.1: DocProcessor Documentation

- [ ] **Step 1: README.md** — overview, quick start, examples
- [ ] **Step 2: ARCHITECTURE.md** — 5 Mermaid diagrams (component, sequence, class, state, flowchart)
- [ ] **Step 3: API_REFERENCE.md** — all interfaces, types, methods
- [ ] **Step 4: USER_GUIDE.md** — tutorial-style walkthrough
- [ ] **Step 5: CONTRIBUTING.md, CHANGELOG.md, CLAUDE.md, AGENTS.md**
- [ ] **Step 6: VIDEO_COURSE.md** — 5 episode scripts
- [ ] **Step 7: Commit and push**

### Task 7.2: LLMOrchestrator Documentation

- [ ] **Steps 1-7: Same pattern as 7.1**

### Task 7.3: VisionEngine Documentation

- [ ] **Steps 1-7: Same pattern**

### Task 7.4: HelixQA Autonomous Session Documentation

- [ ] **Step 1: Update README.md** — add Autonomous QA Session section
- [ ] **Step 2: Update ARCHITECTURE.md** — add new packages diagram
- [ ] **Step 3: Update API_REFERENCE.md** — new interfaces and types
- [ ] **Step 4: Create USER_GUIDE_AUTONOMOUS.md** — full autonomous session guide
- [ ] **Step 5: Create VIDEO_COURSE_AUTONOMOUS.md** — 8 episode scripts
- [ ] **Step 6: Commit and push**

---

## Phase 8: GitHub/GitLab Project Management

### Task 8.1: Create GitHub Project

- [ ] **Step 1: Create GitHub project board**

```bash
gh project create --owner vasic-digital --title "Autonomous QA Session" --body "HelixQA Autonomous QA Session implementation tracking"
```

- [ ] **Step 2: Create milestone for each phase**

```bash
for repo in LLMsVerifier LLMOrchestrator VisionEngine DocProcessor HelixQA; do
    gh api repos/vasic-digital/$repo/milestones -f title="Autonomous QA Session" -f description="Phase implementation" -f state="open"
done
```

- [ ] **Step 3: Create issues for each task**

Create one issue per Task (1.1 through 8.1) with:
- Title matching task name
- Body with all steps as checkboxes
- Labels: phase, module, priority
- Milestone assignment

- [ ] **Step 4: Add issues to project board**

### Task 8.2: Create GitLab Equivalent

- [ ] **Step 1: Create GitLab group milestones**
- [ ] **Step 2: Create GitLab issues mirroring GitHub**
- [ ] **Step 3: Create GitLab board**

### Task 8.3: Sync Protocol

- [ ] **Step 1: Create sync script** `scripts/sync-trackers.sh` that:
  - Lists all GitHub issues with their states
  - Updates corresponding GitLab issues to match
  - Reports any discrepancies
- [ ] **Step 2: Document sync process in CONTRIBUTING.md**
- [ ] **Step 3: Commit**

---

## Integration: Yole Submodule Updates

After all phases complete:

### Task I.1: Add New Submodules to Yole

- [ ] **Step 1: Add Git submodules**

```bash
cd /run/media/milosvasic/DATA4TB/Projects/Yole
git submodule add git@github.com:vasic-digital/DocProcessor.git DocProcessor
git submodule add git@github.com:vasic-digital/LLMOrchestrator.git LLMOrchestrator
git submodule add git@github.com:vasic-digital/VisionEngine.git VisionEngine
```

- [ ] **Step 2: Update .gitmodules**
- [ ] **Step 3: Update Makefile** — add targets for new submodules
- [ ] **Step 4: Run Yole desktop tests**

```bash
./gradlew :shared:desktopTest
```

- [ ] **Step 5: Run HelixQA tests**

```bash
cd HelixQA && go test ./... -race -count=1
```

- [ ] **Step 6: Commit Yole with updated submodules**

---

## Verification Checklist

Before declaring complete:

- [ ] All LLMsVerifier tests pass (existing + ~120 new)
- [ ] All DocProcessor tests pass (~150 new)
- [ ] All LLMOrchestrator tests pass (~200 new)
- [ ] All VisionEngine tests pass (~180 new, vision-tagged)
- [ ] All HelixQA tests pass (existing 235 + ~450 new)
- [ ] Total: ~1,100+ new tests, all passing with `-race -count=1`
- [ ] All 5 modules pushed to 4 remotes each
- [ ] GitHub project board with all tasks tracked
- [ ] GitLab equivalent 100% in sync
- [ ] 50 documentation files created
- [ ] 28 video course scripts written
- [ ] 25 Mermaid diagrams in ARCHITECTURE.md files
- [ ] Yole submodules updated and desktop tests passing
