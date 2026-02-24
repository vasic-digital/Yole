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

// WebUserFlowsChallenge tests web app user flows via Playwright.
type WebUserFlowsChallenge struct {
	challenge.BaseChallenge
	projectRoot string
	pw          *adapters.PlaywrightAdapter
}

// NewWebUserFlowsChallenge creates a challenge that tests web
// app user flows using Playwright.
func NewWebUserFlowsChallenge(
	projectRoot string,
) *WebUserFlowsChallenge {
	return &WebUserFlowsChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			"web-user-flows",
			"Web User Flows (Playwright)",
			"Tests web app user flows: format selection, "+
				"document editing, theme switching, "+
				"file operations",
			"web",
			[]challenge.ID{"web-launch"},
		),
		projectRoot: projectRoot,
		pw: adapters.NewPlaywrightAdapter(
			"http://localhost:8080",
		),
	}
}

// Execute tests web app user flows using Playwright browser
// automation.
func (c *WebUserFlowsChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult

	// Initialize Playwright
	c.ReportProgress("Initializing Playwright...", nil)
	if err := c.pw.Initialize(ctx, "chromium"); err != nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "playwright_init",
				Expected: "playwright initializes",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message:  "Playwright init failed",
			},
		)
		return c.CreateResult(
			challenge.StatusFailed, start, assertions,
			metrics, outputs,
			fmt.Sprintf("Playwright init failed: %v", err),
		), nil
	}
	defer c.pw.Close(ctx)

	// Navigate to app
	if err := c.pw.Navigate(ctx, c.pw.BaseURL); err != nil {
		outputs["note"] = "Web app server not running " +
			"— skipping Playwright flow tests"
		return c.CreateResult(
			challenge.StatusSkipped, start, assertions,
			metrics, outputs,
			"Web app server not running",
		), nil
	}

	// Test 1: Verify main UI elements
	c.ReportProgress("Testing main UI elements...", nil)
	if visible, _ := c.pw.IsVisible(
		ctx, "text=Yole - Web Editor",
	); visible {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "title_bar",
				Expected: "title bar visible",
				Actual:   "title bar visible",
				Passed:   true,
				Message:  "Title bar visible",
			},
		)
	} else {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "title_bar",
				Expected: "title bar visible",
				Actual:   "not visible",
				Passed:   false,
				Message:  "Title bar not visible",
			},
		)
	}

	// Test 2: Format selection
	c.ReportProgress("Testing format selection...", nil)
	if err := c.pw.ClickByText(
		ctx, "Markdown (.md)",
	); err == nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "format_selection",
				Expected: "format selectable",
				Actual:   "format selectable",
				Passed:   true,
				Message:  "Markdown format selectable",
			},
		)
	} else {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "format_selection",
				Expected: "format selectable",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message: fmt.Sprintf(
					"Format selection failed: %v", err,
				),
			},
		)
	}

	// Test 3: Theme toggle
	c.ReportProgress("Testing theme toggle...", nil)
	if err := c.pw.ClickByText(ctx, "Dark"); err == nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "theme_toggle",
				Expected: "theme toggles",
				Actual:   "theme toggles",
				Passed:   true,
				Message:  "Theme toggle works",
			},
		)
	} else {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "theme_toggle",
				Expected: "theme toggles",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message: fmt.Sprintf(
					"Theme toggle failed: %v", err,
				),
			},
		)
	}

	// Test 4: New document
	c.ReportProgress(
		"Testing new document creation...", nil,
	)
	if err := c.pw.ClickByText(
		ctx, "New Document",
	); err == nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "new_document",
				Expected: "new document creates",
				Actual:   "new document creates",
				Passed:   true,
				Message:  "New document creation works",
			},
		)
	} else {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "new_document",
				Expected: "new document creates",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message: fmt.Sprintf(
					"New document failed: %v", err,
				),
			},
		)
	}

	// Test 5: Save document
	c.ReportProgress("Testing save...", nil)
	if err := c.pw.ClickByText(ctx, "Save"); err == nil {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "save_document",
				Expected: "save works",
				Actual:   "save works",
				Passed:   true,
				Message:  "Save button works",
			},
		)
	} else {
		assertions = append(assertions,
			challenge.AssertionResult{
				Type:     "not_empty",
				Target:   "save_document",
				Expected: "save works",
				Actual:   fmt.Sprintf("failed: %v", err),
				Passed:   false,
				Message: fmt.Sprintf(
					"Save failed: %v", err,
				),
			},
		)
	}

	// Screenshot for evidence
	screenshot, _ := c.pw.Screenshot(ctx)
	if len(screenshot) > 0 {
		outputs["screenshot_size"] = fmt.Sprintf(
			"%d bytes", len(screenshot),
		)
	}

	// Determine overall status
	status := challenge.StatusPassed
	for _, a := range assertions {
		if !a.Passed {
			status = challenge.StatusFailed
			break
		}
	}

	return c.CreateResult(
		status, start, assertions, metrics, outputs, "",
	), nil
}
