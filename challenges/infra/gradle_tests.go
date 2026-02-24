// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package infra

import (
	"context"
	"fmt"
	"time"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// GradleTestsChallenge runs all existing Gradle tests and
// collects results.
type GradleTestsChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

// NewGradleTestsChallenge creates a challenge that executes all
// unit and integration tests.
func NewGradleTestsChallenge(
	projectRoot string, useDocker bool,
) *GradleTestsChallenge {
	return &GradleTestsChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			"infra-gradle-tests",
			"Gradle Test Execution",
			"Runs all existing unit and integration tests "+
				"across all modules",
			"infrastructure",
			[]challenge.ID{"infra-gradle-build"},
		),
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
}

// Execute runs each test task and collects JUnit results.
func (c *GradleTestsChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult
	var errMsg string

	testTasks := []struct {
		name string
		task string
	}{
		{"Shared Unit Tests", ":shared:testDebugUnitTest"},
		{"Shared Desktop Tests", ":shared:desktopTest"},
		{
			"Android Robolectric Tests",
			":androidApp:testDebugUnitTest",
		},
	}

	totalTests := 0
	totalFailures := 0
	allPassed := true

	for _, tt := range testTasks {
		c.ReportProgress(
			fmt.Sprintf("Running %s...", tt.name), nil,
		)
		res, err := c.gradle.RunTests(ctx, tt.task, "")

		if res != nil {
			for _, suite := range res.Suites {
				totalTests += suite.Tests
				totalFailures += suite.Failures + suite.Errors
				outputs[fmt.Sprintf(
					"%s_%s", tt.name, suite.Name,
				)] = fmt.Sprintf(
					"tests=%d failures=%d errors=%d "+
						"time=%.2fs",
					suite.Tests, suite.Failures,
					suite.Errors, suite.Time,
				)
			}
		}

		if err != nil {
			allPassed = false
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:     "all_pass",
					Target:   tt.name,
					Expected: "all tests pass",
					Actual:   fmt.Sprintf("failed: %v", err),
					Passed:   false,
					Message: fmt.Sprintf(
						"%s failed", tt.name,
					),
				},
			)
			errMsg = fmt.Sprintf("%s failed: %v", tt.name, err)
		} else {
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:     "all_pass",
					Target:   tt.name,
					Expected: "all tests pass",
					Actual:   "all tests pass",
					Passed:   true,
					Message: fmt.Sprintf(
						"%s passed in %v",
						tt.name, res.Duration,
					),
				},
			)
		}
	}

	outputs["total_tests"] = fmt.Sprintf("%d", totalTests)
	outputs["total_failures"] = fmt.Sprintf("%d", totalFailures)
	metrics["total_tests"] = challenge.MetricValue{
		Name: "total_tests", Value: float64(totalTests),
		Unit: "count",
	}
	metrics["total_failures"] = challenge.MetricValue{
		Name: "total_failures", Value: float64(totalFailures),
		Unit: "count",
	}

	status := challenge.StatusPassed
	if !allPassed {
		status = challenge.StatusFailed
	}

	return c.CreateResult(
		status, start, assertions, metrics, outputs, errMsg,
	), nil
}
