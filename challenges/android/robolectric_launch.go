// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package android

import (
	"context"
	"fmt"
	"time"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// RobolectricLaunchChallenge runs Robolectric app launch tests.
type RobolectricLaunchChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

// NewRobolectricLaunchChallenge creates a challenge that verifies
// the Android app launches without crash using Robolectric.
func NewRobolectricLaunchChallenge(
	projectRoot string, useDocker bool,
) *RobolectricLaunchChallenge {
	return &RobolectricLaunchChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			"android-robolectric-launch",
			"Android App Launch (Robolectric)",
			"Verifies the Android app launches without "+
				"crash using Robolectric",
			"android",
			[]challenge.ID{"infra-gradle-build"},
		),
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
}

// Execute runs the Robolectric app launch test class.
func (c *RobolectricLaunchChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult
	var errMsg string

	c.ReportProgress(
		"Running Robolectric app launch tests...", nil,
	)
	res, err := c.gradle.RunTests(
		ctx,
		":androidApp:testDebugUnitTest",
		"digital.vasic.yole.android.robolectric."+
			"AppLaunchRobolectricTest",
	)

	if err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "all_pass",
				Target:   "robolectric_launch",
				Expected: "app launches without crash",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "Robolectric launch tests failed",
			},
		)
		errMsg = fmt.Sprintf(
			"Robolectric launch tests failed: %v", err,
		)
		if res != nil {
			outputs["output"] = res.Output
		}
	} else {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "all_pass",
				Target:   "robolectric_launch",
				Expected: "app launches without crash",
				Actual:   "app launches without crash",
				Passed:   true,
				Message: fmt.Sprintf(
					"App launch tests passed in %v",
					res.Duration,
				),
			},
		)
		for _, suite := range res.Suites {
			outputs[suite.Name] = fmt.Sprintf(
				"tests=%d failures=%d time=%.2fs",
				suite.Tests, suite.Failures, suite.Time,
			)
		}
		metrics["launch_test_duration"] = challenge.MetricValue{
			Name:  "launch_test_duration",
			Value: res.Duration.Seconds(),
			Unit:  "seconds",
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
