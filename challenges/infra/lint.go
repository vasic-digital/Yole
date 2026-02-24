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

// LintChallenge runs lint and static analysis checks.
type LintChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

// NewLintChallenge creates a challenge that runs Android lint
// and Detekt.
func NewLintChallenge(
	projectRoot string, useDocker bool,
) *LintChallenge {
	return &LintChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			"infra-lint",
			"Lint and Static Analysis",
			"Runs Android lint and Detekt static analysis",
			"infrastructure",
			[]challenge.ID{"infra-gradle-build"},
		),
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
}

// Execute runs lint and static analysis tasks.
func (c *LintChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult
	var errMsg string
	allPassed := true

	// Android Lint
	c.ReportProgress("Running Android lint...", nil)
	lintRes, err := c.gradle.RunTask(
		ctx, ":androidApp:lintDebug",
	)
	if err != nil {
		allPassed = false
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "all_pass",
				Target:   "android_lint",
				Expected: "lint passes",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "Android lint failed",
			},
		)
		errMsg = fmt.Sprintf("Android lint failed: %v", err)
	} else {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "all_pass",
				Target:   "android_lint",
				Expected: "lint passes",
				Actual:   "lint passes",
				Passed:   true,
				Message: fmt.Sprintf(
					"Android lint passed in %v",
					lintRes.Duration,
				),
			},
		)
		metrics["lint_duration"] = challenge.MetricValue{
			Name:  "lint_duration",
			Value: lintRes.Duration.Seconds(),
			Unit:  "seconds",
		}
	}

	// Detekt
	c.ReportProgress("Running Detekt...", nil)
	detektRes, err := c.gradle.RunTask(ctx, "detekt")
	if err != nil {
		allPassed = false
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "all_pass",
				Target:   "detekt",
				Expected: "detekt passes",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "Detekt failed",
			},
		)
		if errMsg == "" {
			errMsg = fmt.Sprintf("Detekt failed: %v", err)
		}
	} else {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "all_pass",
				Target:   "detekt",
				Expected: "detekt passes",
				Actual:   "detekt passes",
				Passed:   true,
				Message: fmt.Sprintf(
					"Detekt passed in %v",
					detektRes.Duration,
				),
			},
		)
		metrics["detekt_duration"] = challenge.MetricValue{
			Name:  "detekt_duration",
			Value: detektRes.Duration.Seconds(),
			Unit:  "seconds",
		}
	}

	_ = outputs // no extra outputs needed

	status := challenge.StatusPassed
	if !allPassed {
		status = challenge.StatusFailed
	}

	return c.CreateResult(
		status, start, assertions, metrics, outputs, errMsg,
	), nil
}
