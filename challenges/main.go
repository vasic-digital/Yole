// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"time"

	"digital.vasic.challenges/pkg/challenge"
	"digital.vasic.challenges/pkg/registry"
	"digital.vasic.challenges/pkg/report"
	"digital.vasic.challenges/pkg/runner"

	androidch "digital.vasic.yole/challenges/android"
	"digital.vasic.yole/challenges/desktop"
	"digital.vasic.yole/challenges/infra"
	"digital.vasic.yole/challenges/web"
)

func main() {
	var (
		platform  = flag.String("platform", "all",
			"Platform to test: android, desktop, web, all")
		reportFmt = flag.String("report", "markdown",
			"Report format: markdown, json, html")
		outputDir = flag.String("output", "reports",
			"Output directory for reports")
		useDocker = flag.Bool("docker", false,
			"Run Gradle tasks in Docker containers")
		timeout = flag.Duration("timeout", 30*time.Minute,
			"Global timeout for all challenges")
	)
	flag.Parse()

	// Determine project root (parent of challenges/ directory)
	projectRoot := findProjectRoot()

	fmt.Println("Yole Challenges Runner")
	fmt.Printf("Project root: %s\n", projectRoot)
	fmt.Printf("Platform: %s\n", *platform)
	fmt.Printf("Docker: %v\n", *useDocker)
	fmt.Printf("Timeout: %v\n", *timeout)
	fmt.Println()

	ctx, cancel := context.WithTimeout(context.Background(), *timeout)
	defer cancel()

	// Create registry
	reg := registry.NewRegistry()

	// Register challenges based on platform
	registerInfraChallenges(reg, projectRoot, *useDocker)
	if *platform == "all" || *platform == "android" {
		registerAndroidChallenges(reg, projectRoot, *useDocker)
	}
	if *platform == "all" || *platform == "desktop" {
		registerDesktopChallenges(reg, projectRoot)
	}
	if *platform == "all" || *platform == "web" {
		registerWebChallenges(reg, projectRoot)
	}

	fmt.Printf("Registered %d challenges\n\n", reg.Count())

	// Create runner
	r := runner.NewRunner(
		runner.WithRegistry(reg),
		runner.WithTimeout(*timeout),
		runner.WithResultsDir(*outputDir),
	)

	// Create config
	cfg := challenge.NewConfig("yole-challenges")

	// Run all challenges
	results, err := r.RunAll(ctx, cfg)
	if err != nil {
		log.Printf("Runner error: %v\n", err)
	}

	// Generate reports
	if mkErr := os.MkdirAll(*outputDir, 0755); mkErr != nil {
		log.Printf("Failed to create output dir: %v\n", mkErr)
	}

	switch *reportFmt {
	case "json":
		reporter := report.NewJSONReporter(*outputDir, true)
		data, genErr := reporter.GenerateMasterSummary(results)
		if genErr == nil {
			_ = os.WriteFile(
				filepath.Join(*outputDir, "results.json"),
				data, 0644,
			)
		}
	case "html":
		reporter := report.NewHTMLReporter(*outputDir)
		data, genErr := reporter.GenerateMasterSummary(results)
		if genErr == nil {
			_ = os.WriteFile(
				filepath.Join(*outputDir, "results.html"),
				data, 0644,
			)
		}
	default:
		reporter := report.NewMarkdownReporter(*outputDir)
		_ = reporter.SaveMasterSummary(results, "results.md")
	}

	fmt.Printf("\nReport written to %s/\n", *outputDir)

	// Exit with non-zero if any challenge failed
	for _, r := range results {
		if r.Status == challenge.StatusFailed ||
			r.Status == challenge.StatusError {
			os.Exit(1)
		}
	}
}

func findProjectRoot() string {
	// Try relative to current directory
	cwd, _ := os.Getwd()
	parent := filepath.Dir(cwd)
	if _, err := os.Stat(
		filepath.Join(parent, "settings.gradle.kts"),
	); err == nil {
		return parent
	}
	// Fallback: assume we're in the project root
	if _, err := os.Stat(
		filepath.Join(cwd, "settings.gradle.kts"),
	); err == nil {
		return cwd
	}
	return parent
}

func registerInfraChallenges(
	reg *registry.DefaultRegistry,
	projectRoot string, useDocker bool,
) {
	_ = reg.Register(
		infra.NewGradleBuildChallenge(projectRoot, useDocker),
	)
	_ = reg.Register(
		infra.NewGradleTestsChallenge(projectRoot, useDocker),
	)
	_ = reg.Register(
		infra.NewLintChallenge(projectRoot, useDocker),
	)
}

func registerAndroidChallenges(
	reg *registry.DefaultRegistry,
	projectRoot string, useDocker bool,
) {
	_ = reg.Register(
		androidch.NewRobolectricLaunchChallenge(
			projectRoot, useDocker,
		),
	)
	_ = reg.Register(
		androidch.NewRobolectricFlowsChallenge(
			projectRoot, useDocker,
		),
	)
	_ = reg.Register(
		androidch.NewUIAutomatorLaunchChallenge(
			projectRoot, useDocker,
		),
	)
}

func registerDesktopChallenges(
	reg *registry.DefaultRegistry, projectRoot string,
) {
	_ = reg.Register(
		desktop.NewDesktopLaunchChallenge(projectRoot),
	)
	_ = reg.Register(
		desktop.NewDesktopUserFlowsChallenge(projectRoot),
	)
}

func registerWebChallenges(
	reg *registry.DefaultRegistry, projectRoot string,
) {
	_ = reg.Register(
		web.NewWebLaunchChallenge(projectRoot),
	)
	_ = reg.Register(
		web.NewWebUserFlowsChallenge(projectRoot),
	)
}
