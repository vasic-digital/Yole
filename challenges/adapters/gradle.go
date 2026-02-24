// SPDX-FileCopyrightText: 2025 Milos Vasic
// SPDX-License-Identifier: Apache-2.0

package adapters

import (
	"context"
	"encoding/xml"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"
)

// GradleAdapter executes Gradle tasks and parses JUnit XML results.
type GradleAdapter struct {
	ProjectRoot string
	UseDocker   bool
}

// JUnitTestSuites represents the top-level JUnit XML structure.
type JUnitTestSuites struct {
	XMLName    xml.Name         `xml:"testsuites"`
	TestSuites []JUnitTestSuite `xml:"testsuite"`
}

// JUnitTestSuite represents a single test suite in JUnit XML.
type JUnitTestSuite struct {
	XMLName   xml.Name        `xml:"testsuite"`
	Name      string          `xml:"name,attr"`
	Tests     int             `xml:"tests,attr"`
	Failures  int             `xml:"failures,attr"`
	Errors    int             `xml:"errors,attr"`
	Skipped   int             `xml:"skipped,attr"`
	Time      float64         `xml:"time,attr"`
	TestCases []JUnitTestCase `xml:"testcase"`
}

// JUnitTestCase represents a single test case in JUnit XML.
type JUnitTestCase struct {
	XMLName   xml.Name      `xml:"testcase"`
	Name      string        `xml:"name,attr"`
	ClassName string        `xml:"classname,attr"`
	Time      float64       `xml:"time,attr"`
	Failure   *JUnitFailure `xml:"failure,omitempty"`
	Error     *JUnitError   `xml:"error,omitempty"`
}

// JUnitFailure represents a test failure.
type JUnitFailure struct {
	Message string `xml:"message,attr"`
	Type    string `xml:"type,attr"`
	Content string `xml:",chardata"`
}

// JUnitError represents a test error.
type JUnitError struct {
	Message string `xml:"message,attr"`
	Type    string `xml:"type,attr"`
	Content string `xml:",chardata"`
}

// GradleResult holds the result of a Gradle task execution.
type GradleResult struct {
	Task     string
	Success  bool
	Duration time.Duration
	Output   string
	Suites   []JUnitTestSuite
}

// RunTask executes a Gradle task and returns the result.
func (g *GradleAdapter) RunTask(
	ctx context.Context, task string, args ...string,
) (*GradleResult, error) {
	start := time.Now()

	cmdArgs := []string{task}
	cmdArgs = append(cmdArgs, args...)

	var cmd *exec.Cmd
	if g.UseDocker {
		dockerArgs := []string{
			"compose", "run", "--rm", "build", "./gradlew",
		}
		dockerArgs = append(dockerArgs, cmdArgs...)
		cmd = exec.CommandContext(ctx, "docker", dockerArgs...)
	} else {
		cmd = exec.CommandContext(
			ctx,
			filepath.Join(g.ProjectRoot, "gradlew"),
			cmdArgs...,
		)
	}
	cmd.Dir = g.ProjectRoot

	output, err := cmd.CombinedOutput()
	duration := time.Since(start)

	result := &GradleResult{
		Task:     task,
		Success:  err == nil,
		Duration: duration,
		Output:   string(output),
	}

	return result, err
}

// RunTests executes a Gradle test task and parses JUnit XML results.
func (g *GradleAdapter) RunTests(
	ctx context.Context, task string, testFilter string,
) (*GradleResult, error) {
	args := []string{}
	if testFilter != "" {
		args = append(args, "--tests", testFilter)
	}

	result, err := g.RunTask(ctx, task, args...)
	if err != nil && result == nil {
		return nil, fmt.Errorf("gradle task failed: %w", err)
	}

	// Parse JUnit XML results
	suites, parseErr := g.ParseJUnitResults()
	if parseErr == nil {
		result.Suites = suites
	}

	return result, err
}

// ParseJUnitResults finds and parses JUnit XML files from test output.
func (g *GradleAdapter) ParseJUnitResults() ([]JUnitTestSuite, error) {
	var allSuites []JUnitTestSuite

	searchPaths := []string{
		filepath.Join(g.ProjectRoot, "shared", "build", "test-results"),
		filepath.Join(
			g.ProjectRoot, "androidApp", "build", "test-results",
		),
		filepath.Join(
			g.ProjectRoot, "desktopApp", "build", "test-results",
		),
		filepath.Join(
			g.ProjectRoot, "webApp", "build", "test-results",
		),
	}

	for _, searchPath := range searchPaths {
		_ = filepath.Walk(
			searchPath,
			func(path string, info os.FileInfo, err error) error {
				if err != nil {
					return nil
				}
				if !info.IsDir() && strings.HasSuffix(path, ".xml") {
					suites, parseErr := parseJUnitXML(path)
					if parseErr == nil {
						allSuites = append(allSuites, suites...)
					}
				}
				return nil
			},
		)
	}

	return allSuites, nil
}

func parseJUnitXML(path string) ([]JUnitTestSuite, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}

	// Try parsing as testsuites (multiple suites)
	var suites JUnitTestSuites
	if err := xml.Unmarshal(data, &suites); err == nil &&
		len(suites.TestSuites) > 0 {
		return suites.TestSuites, nil
	}

	// Try parsing as single testsuite
	var suite JUnitTestSuite
	if err := xml.Unmarshal(data, &suite); err == nil {
		return []JUnitTestSuite{suite}, nil
	}

	return nil, fmt.Errorf("unable to parse JUnit XML: %s", path)
}
