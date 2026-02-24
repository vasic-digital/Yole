// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package common

import (
	"context"
	"fmt"
	"time"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// AppLaunchChallenge is a platform-agnostic challenge that
// verifies an app launches via a PlatformAdapter.
type AppLaunchChallenge struct {
	challenge.BaseChallenge
	adapter adapters.PlatformAdapter
}

// NewAppLaunchChallenge creates a reusable launch challenge for
// any platform that implements PlatformAdapter.
func NewAppLaunchChallenge(
	adapter adapters.PlatformAdapter,
) *AppLaunchChallenge {
	return &AppLaunchChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			challenge.ID(fmt.Sprintf(
				"common-launch-%s", adapter.Name(),
			)),
			fmt.Sprintf("App Launch (%s)", adapter.Name()),
			fmt.Sprintf(
				"Verifies the %s app launches "+
					"without errors", adapter.Name(),
			),
			"common",
			nil,
		),
		adapter: adapter,
	}
}

// Execute builds, launches, and verifies the app is running.
func (c *AppLaunchChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult

	// Build
	c.ReportProgress("Building...", nil)
	if err := c.adapter.Build(ctx); err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "build",
				Expected: "build succeeds",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "Build failed",
			},
		)
		return c.CreateResult(
			challenge.StatusFailed, start, assertions,
			metrics, outputs,
			fmt.Sprintf("Build failed: %v", err),
		), nil
	}
	assertions = append(assertions,
		challenge.AssertionResult{
			Type:     "not_empty",
			Target:   "build",
			Expected: "build succeeds",
			Actual:   "build succeeds",
			Passed:   true,
			Message:  "Build succeeded",
		},
	)

	// Launch
	c.ReportProgress("Launching...", nil)
	if err := c.adapter.Launch(ctx); err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "launch",
				Expected: "app launches",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "Launch failed",
			},
		)
		return c.CreateResult(
			challenge.StatusFailed, start, assertions,
			metrics, outputs,
			fmt.Sprintf("Launch failed: %v", err),
		), nil
	}
	defer c.adapter.Close(ctx)

	// Verify running
	running, err := c.adapter.IsRunning(ctx)
	if err != nil || !running {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "running",
				Expected: "app is running",
				Actual:   "app not running",
				Passed:   false,
				Message:  "App not running after launch",
			},
		)
		return c.CreateResult(
			challenge.StatusFailed, start, assertions,
			metrics, outputs,
			"App not running after launch",
		), nil
	}
	assertions = append(assertions,
		challenge.AssertionResult{
			Type:     "not_empty",
			Target:   "running",
			Expected: "app is running",
			Actual:   "app is running",
			Passed:   true,
			Message:  "App is running",
		},
	)

	// Take screenshot
	screenshot, err := c.adapter.TakeScreenshot(ctx)
	if err == nil && len(screenshot) > 0 {
		outputs["screenshot_size"] = fmt.Sprintf(
			"%d bytes", len(screenshot),
		)
	}

	return c.CreateResult(
		challenge.StatusPassed, start, assertions,
		metrics, outputs, "",
	), nil
}
