// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package common

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.yole/challenges/adapters"
)

// FormatRenderingChallenge tests opening and rendering files of
// various formats through the PlatformAdapter interface.
type FormatRenderingChallenge struct {
	challenge.BaseChallenge
	adapter     adapters.PlatformAdapter
	testdataDir string
}

// NewFormatRenderingChallenge creates a challenge that opens
// sample files in each supported format and verifies rendering.
func NewFormatRenderingChallenge(
	adapter adapters.PlatformAdapter,
	testdataDir string,
) *FormatRenderingChallenge {
	return &FormatRenderingChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			challenge.ID(fmt.Sprintf(
				"common-format-rendering-%s",
				adapter.Name(),
			)),
			fmt.Sprintf(
				"Format Rendering (%s)", adapter.Name(),
			),
			fmt.Sprintf(
				"Opens and renders sample files in "+
					"multiple formats on %s",
				adapter.Name(),
			),
			"common",
			[]challenge.ID{challenge.ID(fmt.Sprintf(
				"common-launch-%s", adapter.Name(),
			))},
		),
		adapter:     adapter,
		testdataDir: testdataDir,
	}
}

// Execute opens each sample file and verifies content is
// displayed.
func (c *FormatRenderingChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult
	allPassed := true

	sampleFiles := []struct {
		name string
		file string
	}{
		{"Markdown", "sample.md"},
		{"Plain Text", "sample.txt"},
		{"CSV", "sample.csv"},
		{"Org Mode", "sample.org"},
		{"LaTeX", "sample.tex"},
		{"AsciiDoc", "sample.adoc"},
	}

	for _, sf := range sampleFiles {
		c.ReportProgress(
			fmt.Sprintf("Opening %s...", sf.name), nil,
		)

		filePath := filepath.Join(c.testdataDir, sf.file)
		if _, err := os.Stat(filePath); err != nil {
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:   "not_empty",
					Target: sf.name,
					Expected: fmt.Sprintf(
						"%s file exists", sf.name,
					),
					Actual: "file not found",
					Passed: false,
					Message: fmt.Sprintf(
						"%s sample file not found",
						sf.name,
					),
				},
			)
			allPassed = false
			continue
		}

		if err := c.adapter.OpenFile(
			ctx, filePath,
		); err != nil {
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:   "not_empty",
					Target: sf.name,
					Expected: fmt.Sprintf(
						"%s file opens", sf.name,
					),
					Actual: fmt.Sprintf(
						"failed: %v", err,
					),
					Passed: false,
					Message: fmt.Sprintf(
						"Failed to open %s file", sf.name,
					),
				},
			)
			allPassed = false
			continue
		}

		content, err := c.adapter.GetDisplayedContent(ctx)
		if err != nil {
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:   "not_empty",
					Target: sf.name,
					Expected: fmt.Sprintf(
						"%s content displayed", sf.name,
					),
					Actual: fmt.Sprintf(
						"failed: %v", err,
					),
					Passed: false,
					Message: fmt.Sprintf(
						"Failed to get %s content",
						sf.name,
					),
				},
			)
			allPassed = false
			continue
		}

		if len(content) > 0 {
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:   "not_empty",
					Target: sf.name,
					Expected: fmt.Sprintf(
						"%s content displayed", sf.name,
					),
					Actual: fmt.Sprintf(
						"%d chars", len(content),
					),
					Passed: true,
					Message: fmt.Sprintf(
						"%s rendered successfully "+
							"(%d chars)",
						sf.name, len(content),
					),
				},
			)
			outputs[sf.name+"_length"] = fmt.Sprintf(
				"%d", len(content),
			)
		} else {
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:   "not_empty",
					Target: sf.name,
					Expected: fmt.Sprintf(
						"%s content displayed", sf.name,
					),
					Actual:  "empty content",
					Passed:  false,
					Message: fmt.Sprintf(
						"%s rendered empty content",
						sf.name,
					),
				},
			)
			allPassed = false
		}
	}

	metrics["formats_tested"] = challenge.MetricValue{
		Name:  "formats_tested",
		Value: float64(len(sampleFiles)),
		Unit:  "count",
	}

	status := challenge.StatusPassed
	if !allPassed {
		status = challenge.StatusFailed
	}

	return c.CreateResult(
		status, start, assertions, metrics, outputs, "",
	), nil
}
