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

// GradleBuildChallenge verifies all Gradle build tasks succeed.
type GradleBuildChallenge struct {
	challenge.BaseChallenge
	gradle *adapters.GradleAdapter
}

// NewGradleBuildChallenge creates a challenge that compiles all
// application modules.
func NewGradleBuildChallenge(
	projectRoot string, useDocker bool,
) *GradleBuildChallenge {
	return &GradleBuildChallenge{
		BaseChallenge: challenge.NewBaseChallenge(
			"infra-gradle-build",
			"Gradle Build Verification",
			"Verifies all application modules compile successfully",
			"infrastructure",
			nil,
		),
		gradle: &adapters.GradleAdapter{
			ProjectRoot: projectRoot,
			UseDocker:   useDocker,
		},
	}
}

// Execute runs the Gradle build tasks for all platform modules.
func (c *GradleBuildChallenge) Execute(
	ctx context.Context,
) (*challenge.Result, error) {
	start := time.Now()
	outputs := make(map[string]string)
	metrics := make(map[string]challenge.MetricValue)
	var assertions []challenge.AssertionResult
	var errMsg string

	builds := []struct {
		name string
		task string
	}{
		{"Android Debug", ":androidApp:assembleDebug"},
		{"Desktop JAR", ":desktopApp:jar"},
		{"Shared Library", ":shared:compileKotlinJvm"},
	}

	allPassed := true
	for _, build := range builds {
		c.ReportProgress(
			fmt.Sprintf("Building %s...", build.name), nil,
		)
		res, err := c.gradle.RunTask(ctx, build.task)
		if err != nil {
			allPassed = false
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:     "not_empty",
					Target:   build.name,
					Expected: "build success",
					Actual:   fmt.Sprintf("failed: %v", err),
					Passed:   false,
					Message: fmt.Sprintf(
						"%s build failed", build.name,
					),
				},
			)
			if res != nil {
				outputs[build.name+"_output"] = res.Output
			}
			errMsg = fmt.Sprintf(
				"%s build failed: %v", build.name, err,
			)
		} else {
			assertions = append(assertions,
				challenge.AssertionResult{
					Type:     "not_empty",
					Target:   build.name,
					Expected: "build success",
					Actual:   "build success",
					Passed:   true,
					Message: fmt.Sprintf(
						"%s built in %v",
						build.name, res.Duration,
					),
				},
			)
			metrics[build.name] = challenge.MetricValue{
				Name:  build.name + "_duration",
				Value: res.Duration.Seconds(),
				Unit:  "seconds",
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
