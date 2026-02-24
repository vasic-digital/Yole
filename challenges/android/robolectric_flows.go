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

// RobolectricFlowsChallenge runs all Robolectric user flow
// tests.
type RobolectricFlowsChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

// NewRobolectricFlowsChallenge creates a challenge that runs all
// Robolectric user flow test classes.
func NewRobolectricFlowsChallenge(
	projectRoot string, useDocker bool,
) *RobolectricFlowsChallenge {
	return &RobolectricFlowsChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			"android-robolectric-flows",
			"Android User Flows (Robolectric)",
			"Runs all Robolectric user flow tests: "+
				"navigation, theme, settings, editing, "+
				"todo, quicknote, backup, accessibility",
			"android",
			[]challenge.ID{"android-robolectric-launch"},
		),
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
}

// Execute runs each Robolectric test class and collects results.
func (c *RobolectricFlowsChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult
	var errMsg string
	allPassed := true

	testClasses := []struct {
		name  string
		class string
	}{
		{
			"Theme",
			"digital.vasic.yole.android.robolectric." +
				"ThemeRobolectricTest",
		},
		{
			"Navigation",
			"digital.vasic.yole.android.robolectric." +
				"NavigationRobolectricTest",
		},
		{
			"Settings",
			"digital.vasic.yole.android.robolectric." +
				"SettingsRobolectricTest",
		},
		{
			"File Editing",
			"digital.vasic.yole.android.robolectric." +
				"FileEditingRobolectricTest",
		},
		{
			"Format Detection",
			"digital.vasic.yole.android.robolectric." +
				"FormatDetectionRobolectricTest",
		},
		{
			"Todo Workflow",
			"digital.vasic.yole.android.robolectric." +
				"TodoWorkflowRobolectricTest",
		},
		{
			"QuickNote",
			"digital.vasic.yole.android.robolectric." +
				"QuickNoteRobolectricTest",
		},
		{
			"Backup/Restore",
			"digital.vasic.yole.android.robolectric." +
				"BackupRestoreRobolectricTest",
		},
		{
			"Accessibility",
			"digital.vasic.yole.android.robolectric." +
				"AccessibilityRobolectricTest",
		},
	}

	for _, tc := range testClasses {
		c.ReportProgress(
			fmt.Sprintf("Running %s tests...", tc.name), nil,
		)
		res, err := c.gradle.RunTests(
			ctx, ":androidApp:testDebugUnitTest", tc.class,
		)

		if err != nil {
			allPassed = false
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:     "all_pass",
					Target:   tc.name,
					Expected: "tests pass",
					Actual: fmt.Sprintf(
						"failed: %v", err,
					),
					Passed: false,
					Message: fmt.Sprintf(
						"%s tests failed", tc.name,
					),
				},
			)
			errMsg = fmt.Sprintf(
				"%s tests failed: %v", tc.name, err,
			)
		} else {
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:     "all_pass",
					Target:   tc.name,
					Expected: "tests pass",
					Actual:   "tests pass",
					Passed:   true,
					Message: fmt.Sprintf(
						"%s tests passed in %v",
						tc.name, res.Duration,
					),
				},
			)
		}

		if res != nil {
			for _, suite := range res.Suites {
				outputs[fmt.Sprintf(
					"%s_%s", tc.name, suite.Name,
				)] = fmt.Sprintf(
					"tests=%d failures=%d time=%.2fs",
					suite.Tests, suite.Failures, suite.Time,
				)
			}
		}
	}

	status := challenge.StatusPassed
	if !allPassed {
		status = challenge.StatusFailed
	}

	return c.CreateResult(
		status, start, assertions, metrics, outputs, errMsg,
	), nil
}
