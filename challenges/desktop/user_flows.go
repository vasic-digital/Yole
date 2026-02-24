// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package desktop

import (
	"context"
	"fmt"
	"time"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// DesktopUserFlowsChallenge runs desktop unit tests via Gradle.
type DesktopUserFlowsChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

// NewDesktopUserFlowsChallenge creates a challenge that runs
// all desktop-specific tests.
func NewDesktopUserFlowsChallenge(
	projectRoot string,
) *DesktopUserFlowsChallenge {
	return &DesktopUserFlowsChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			"desktop-user-flows",
			"Desktop User Flows",
			"Runs all desktop-specific tests: "+
				"integration, parser, settings, UI, "+
				"file manager, window manager",
			"desktop",
			[]challenge.ID{"desktop-launch"},
		),
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
		},
	}
}

// Execute runs the desktop test suite.
func (c *DesktopUserFlowsChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult
	var errMsg string

	c.ReportProgress("Running desktop tests...", nil)
	res, err := c.gradle.RunTests(ctx, ":desktopApp:test", "")

	if err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "all_pass",
				Target:   "desktop_tests",
				Expected: "all tests pass",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "Desktop tests failed",
			},
		)
		errMsg = fmt.Sprintf("Desktop tests failed: %v", err)
	} else {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "all_pass",
				Target:   "desktop_tests",
				Expected: "all tests pass",
				Actual:   "all tests pass",
				Passed:   true,
				Message: fmt.Sprintf(
					"Desktop tests passed in %v",
					res.Duration,
				),
			},
		)
		metrics["test_duration"] = challenge.MetricValue{
			Name:  "test_duration",
			Value: res.Duration.Seconds(),
			Unit:  "seconds",
		}
	}

	if res != nil {
		for _, suite := range res.Suites {
			outputs[suite.Name] = fmt.Sprintf(
				"tests=%d failures=%d time=%.2fs",
				suite.Tests, suite.Failures, suite.Time,
			)
		}
	}

	status := challenge.StatusPassed
	if err != nil {
		status = challenge.StatusFailed
	}

	return c.CreateResult(
		status, start, assertions, metrics, outputs, errMsg,
	), nil
}
