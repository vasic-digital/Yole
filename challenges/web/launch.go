// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package web

import (
	"context"
	"fmt"
	"time"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// WebLaunchChallenge builds and tests the Wasm web app.
type WebLaunchChallenge struct {
	challenge.BaseChallenge
	projectRoot string
	gradle      *adapters.GradleAdapter
	pw          *adapters.PlaywrightAdapter
}

// NewWebLaunchChallenge creates a challenge that builds the web
// app and tests it with Playwright.
func NewWebLaunchChallenge(
	projectRoot string,
) *WebLaunchChallenge {
	return &WebLaunchChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			"web-launch",
			"Web App Launch (Playwright)",
			"Builds the Wasm web app, launches it in "+
				"a browser, and verifies it loads",
			"web",
			[]challenge.ID{"infra-gradle-build"},
		),
		projectRoot: projectRoot,
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
		},
		pw: adapters.NewPlaywrightAdapter(
			"http://localhost:8080",
		),
	}
}

// Execute runs web tests via Gradle and optionally launches
// Playwright.
func (c *WebLaunchChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult
	var errMsg string
	allPassed := true

	// Run web tests via Gradle (uses Karma + Chrome Headless)
	c.ReportProgress("Running web tests...", nil)
	res, err := c.gradle.RunTests(
		ctx, ":webApp:wasmJsBrowserTest", "",
	)

	if err != nil {
		allPassed = false
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "all_pass",
				Target:   "web_tests",
				Expected: "web tests pass",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "Web tests failed",
			},
		)
		errMsg = fmt.Sprintf("Web tests failed: %v", err)
	} else {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "all_pass",
				Target:   "web_tests",
				Expected: "web tests pass",
				Actual:   "web tests pass",
				Passed:   true,
				Message: fmt.Sprintf(
					"Web tests passed in %v", res.Duration,
				),
			},
		)
	}

	if res != nil {
		for _, suite := range res.Suites {
			outputs[suite.Name] = fmt.Sprintf(
				"tests=%d failures=%d time=%.2fs",
				suite.Tests, suite.Failures, suite.Time,
			)
		}
	}

	// Playwright browser test
	c.ReportProgress("Initializing Playwright...", nil)
	if err := c.pw.Initialize(ctx, "chromium"); err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:   "not_empty",
				Target: "playwright_init",
				Expected: "playwright initializes",
				Actual: fmt.Sprintf("failed: %v", err),
				Passed: false,
				Message: "Playwright init failed " +
					"— install with: " +
					"npx playwright install",
			},
		)
	} else {
		defer c.pw.Close(ctx)

		c.ReportProgress(
			"Navigating to web app...", nil,
		)
		if err := c.pw.Navigate(
			ctx, c.pw.BaseURL,
		); err != nil {
			outputs["note"] = "Web app server not running " +
				"— Playwright launch test skipped. " +
				"Start with: " +
				"./gradlew :webApp:wasmJsBrowserRun"
		} else {
			screenshot, _ := c.pw.Screenshot(ctx)
			if len(screenshot) > 0 {
				outputs["screenshot_size"] = fmt.Sprintf(
					"%d bytes", len(screenshot),
				)
			}
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:     "not_empty",
					Target:   "playwright_load",
					Expected: "web app loads in browser",
					Actual:   "web app loads in browser",
					Passed:   true,
					Message: "Web app loaded in " +
						"Playwright browser",
				},
			)
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
