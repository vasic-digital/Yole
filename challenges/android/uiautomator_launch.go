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

// UIAutomatorLaunchChallenge tests app launch on a real
// device/emulator.
type UIAutomatorLaunchChallenge struct {
	challenge.BaseChallenge
	adb    *adapters.ADBAdapter
	gradle *adapters.GradleAdapter
}

// NewUIAutomatorLaunchChallenge creates a challenge that installs
// and launches the Android app on a connected device or emulator.
func NewUIAutomatorLaunchChallenge(
	projectRoot string, useDocker bool,
) *UIAutomatorLaunchChallenge {
	return &UIAutomatorLaunchChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			"android-uiautomator-launch",
			"Android App Launch (Device)",
			"Installs and launches the Android app on a "+
				"real device or emulator via ADB",
			"android",
			[]challenge.ID{"infra-gradle-build"},
		),
		adb: adapters.NewADBAdapter(),
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
}

// Validate checks that an Android device or emulator is
// available.
func (c *UIAutomatorLaunchChallenge) Validate(
	ctx context.Context,
) error {
	available, err := c.adb.IsDeviceAvailable(ctx)
	if err != nil || !available {
		return fmt.Errorf(
			"no Android device or emulator available " +
				"— skipping device tests",
		)
	}
	return nil
}

// Execute builds, installs, and launches the app on the device.
func (c *UIAutomatorLaunchChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult

	// Build debug APK
	c.ReportProgress("Building debug APK...", nil)
	buildRes, err := c.gradle.RunTask(
		ctx, ":androidApp:assembleDebug",
	)
	if err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "apk_build",
				Expected: "APK builds",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "APK build failed",
			},
		)
		return c.CreateResult(
			challenge.StatusFailed, start, assertions,
			metrics, outputs,
			fmt.Sprintf("APK build failed: %v", err),
		), nil
	}
	assertions = append(assertions,
		challenge.AssertionResult{
			Type:     "not_empty",
			Target:   "apk_build",
			Expected: "APK builds",
			Actual:   "APK builds",
			Passed:   true,
			Message: fmt.Sprintf(
				"APK built in %v", buildRes.Duration,
			),
		},
	)

	// Install APK
	c.ReportProgress("Installing APK...", nil)
	apkPath := c.gradle.ProjectRoot +
		"/androidApp/build/outputs/apk/debug/" +
		"androidApp-debug.apk"
	if err := c.adb.InstallAPK(ctx, apkPath); err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "apk_install",
				Expected: "APK installs",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "APK install failed",
			},
		)
		return c.CreateResult(
			challenge.StatusFailed, start, assertions,
			metrics, outputs,
			fmt.Sprintf("APK install failed: %v", err),
		), nil
	}
	assertions = append(assertions,
		challenge.AssertionResult{
			Type:     "not_empty",
			Target:   "apk_install",
			Expected: "APK installs",
			Actual:   "APK installs",
			Passed:   true,
			Message:  "APK installed successfully",
		},
	)

	// Launch app
	c.ReportProgress("Launching app...", nil)
	if err := c.adb.LaunchApp(ctx); err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "app_launch",
				Expected: "app launches",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "App launch failed",
			},
		)
		return c.CreateResult(
			challenge.StatusFailed, start, assertions,
			metrics, outputs,
			fmt.Sprintf("App launch failed: %v", err),
		), nil
	}

	// Wait for app to be running
	if err := c.adb.WaitForApp(ctx, 15*time.Second); err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "app_running",
				Expected: "app is running",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "App did not start",
			},
		)
		return c.CreateResult(
			challenge.StatusFailed, start, assertions,
			metrics, outputs,
			fmt.Sprintf("App did not start: %v", err),
		), nil
	}

	// Wait a bit and check it's still running (not crashed)
	time.Sleep(3 * time.Second)
	running, _ := c.adb.IsAppRunning(ctx)
	if running {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "app_stable",
				Expected: "app running after 3s",
				Actual:   "app running after 3s",
				Passed:   true,
				Message:  "App launched and is running " +
					"without crash",
			},
		)
	} else {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "app_stable",
				Expected: "app running after 3s",
				Actual:   "app crashed",
				Passed:   false,
				Message:  "App crashed after launch",
			},
		)
	}

	// Take screenshot as evidence
	screenshot, err := c.adb.TakeScreenshot(ctx)
	if err == nil && len(screenshot) > 0 {
		outputs["screenshot_size"] = fmt.Sprintf(
			"%d bytes", len(screenshot),
		)
	}

	// Cleanup
	_ = c.adb.StopApp(ctx)

	status := challenge.StatusPassed
	if !running {
		status = challenge.StatusFailed
	}

	return c.CreateResult(
		status, start, assertions, metrics, outputs, "",
	), nil
}
