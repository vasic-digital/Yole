// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package desktop

import (
	"context"
	"fmt"
	"path/filepath"
	"time"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// DesktopLaunchChallenge launches the desktop app and verifies
// it starts.
type DesktopLaunchChallenge struct {
	challenge.BaseChallenge
	projectRoot string
	process     *adapters.ProcessAdapter
	gradle      *adapters.GradleAdapter
}

// NewDesktopLaunchChallenge creates a challenge that builds and
// launches the desktop JVM application.
func NewDesktopLaunchChallenge(
	projectRoot string,
) *DesktopLaunchChallenge {
	return &DesktopLaunchChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			"desktop-launch",
			"Desktop App Launch",
			"Builds and launches the desktop JVM "+
				"application, verifies it starts "+
				"without crash",
			"desktop",
			[]challenge.ID{"infra-gradle-build"},
		),
		projectRoot: projectRoot,
		process:     &adapters.ProcessAdapter{},
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
		},
	}
}

// Execute builds the desktop JAR and launches it.
func (c *DesktopLaunchChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult

	// Build desktop JAR
	c.ReportProgress("Building desktop JAR...", nil)
	buildRes, err := c.gradle.RunTask(ctx, ":desktopApp:jar")
	if err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "desktop_build",
				Expected: "JAR builds",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "Desktop JAR build failed",
			},
		)
		return c.CreateResult(
			challenge.StatusFailed, start, assertions,
			metrics, outputs,
			fmt.Sprintf("Desktop JAR build failed: %v", err),
		), nil
	}
	assertions = append(assertions,
		challenge.AssertionResult{
			Type:     "not_empty",
			Target:   "desktop_build",
			Expected: "JAR builds",
			Actual:   "JAR builds",
			Passed:   true,
			Message: fmt.Sprintf(
				"Desktop JAR built in %v", buildRes.Duration,
			),
		},
	)
	metrics["build_duration"] = challenge.MetricValue{
		Name:  "build_duration",
		Value: buildRes.Duration.Seconds(),
		Unit:  "seconds",
	}

	// Find the JAR file
	jarPath := filepath.Join(
		c.projectRoot, "desktopApp", "build",
		"libs", "desktopApp.jar",
	)

	// Launch the desktop app
	c.ReportProgress("Launching desktop app...", nil)
	if err := c.process.LaunchJVM(ctx, jarPath); err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "desktop_launch",
				Expected: "app launches",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "Desktop app launch failed",
			},
		)
		return c.CreateResult(
			challenge.StatusFailed, start, assertions,
			metrics, outputs,
			fmt.Sprintf("Desktop app launch failed: %v", err),
		), nil
	}
	defer c.process.Stop()

	// Wait for the app to start
	if err := c.process.WaitForReady(
		ctx, 15*time.Second,
	); err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "desktop_ready",
				Expected: "app is ready",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "Desktop app did not start",
			},
		)
		return c.CreateResult(
			challenge.StatusFailed, start, assertions,
			metrics, outputs,
			fmt.Sprintf("Desktop app did not start: %v", err),
		), nil
	}

	// Wait a bit and check it's still running (not crashed)
	time.Sleep(5 * time.Second)
	status := challenge.StatusPassed
	if c.process.IsRunning() {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "desktop_stable",
				Expected: "app running after 5s",
				Actual:   "app running after 5s",
				Passed:   true,
				Message:  "Desktop app launched and is " +
					"running without crash",
			},
		)
	} else {
		status = challenge.StatusFailed
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "desktop_stable",
				Expected: "app running after 5s",
				Actual:   "app crashed",
				Passed:   false,
				Message:  "Desktop app crashed after launch",
			},
		)
	}

	return c.CreateResult(
		status, start, assertions, metrics, outputs, "",
	), nil
}
