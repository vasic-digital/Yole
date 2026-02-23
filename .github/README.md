<!--
SPDX-FileCopyrightText: 2025 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# CI/CD Setup Guide

This document describes the GitHub Actions CI/CD workflows for the Yole project,
including configuration requirements and instructions for enabling automatic triggers.

## Workflow Overview

The project has four workflow files located in `.github/workflows/`:

| Workflow | File | Purpose |
|----------|------|---------|
| **CI** | `ci.yml` | Core build and test pipeline. Runs shared module unit tests (Android and Desktop), builds the Android debug APK and Desktop JAR, runs Android lint, generates Kover coverage reports, and uploads results to Codecov. Uploads test results, lint results, and coverage reports as artifacts (retained 14 days). |
| **Security** | `security.yml` | Security scanning pipeline with four parallel jobs: secret scanning via Gitleaks, dependency auditing via OWASP Dependency Check, vulnerability scanning via Snyk, and static code analysis via CodeQL (java-kotlin). |
| **SonarQube** | `sonar.yml` | Code quality and coverage analysis. Runs the full test suite with Kover XML report generation, then submits results to a SonarQube or SonarCloud instance for analysis. |
| **Release** | `release.yml` | Release artifact builder. Builds Android release APK (falls back to debug if signing is unavailable), builds Desktop JARs, collects all artifacts, and creates a GitHub Release with auto-generated release notes. Pre-releases are detected from `-alpha`, `-beta`, or `-rc` suffixes in the tag. |

## Current Status

**All four workflows are currently disabled.** Each workflow's `on:` trigger is set
to `workflow_dispatch:` only, meaning they will not run automatically on pushes,
pull requests, or tags. They can only be triggered manually from the GitHub Actions
UI (the "Run workflow" button) or via the GitHub API.

This is indicated by the comment at the top of each workflow file:

```yaml
# DISABLED: All automatic triggers removed. Use workflow_dispatch to run manually.
on:
  workflow_dispatch:
```

## Required Secrets

Each workflow requires specific GitHub repository secrets to function correctly.
Configure these under **Settings > Secrets and variables > Actions** in the GitHub
repository.

### ci.yml

| Secret | Required | Description |
|--------|----------|-------------|
| `CODECOV_TOKEN` | Optional | Authentication token for Codecov coverage uploads. The workflow will not fail if this is missing (`fail_ci_if_error: false`), but coverage data will not be reported to Codecov. |

No other secrets are needed. The `GITHUB_TOKEN` used elsewhere is provided
automatically by GitHub Actions.

### security.yml

| Secret | Required | Description |
|--------|----------|-------------|
| `SNYK_TOKEN` | Yes (for Snyk job) | Authentication token for the Snyk vulnerability scanner. Obtain from [snyk.io](https://snyk.io) account settings. The Snyk job uses `continue-on-error: true`, so it will not block other jobs if the token is missing, but no scan results will be produced. |

The Gitleaks and CodeQL jobs use the automatic `GITHUB_TOKEN` and do not require
additional secrets. The OWASP Dependency Check job currently only lists dependencies
(the Gradle plugin is not yet configured).

### sonar.yml

| Secret | Required | Description |
|--------|----------|-------------|
| `SONAR_TOKEN` | Yes | Authentication token for SonarQube or SonarCloud. Generate from your SonarQube instance under **My Account > Security > Tokens**. |
| `SONAR_HOST_URL` | Yes | The base URL of your SonarQube server (e.g., `https://sonarcloud.io` or `https://sonar.example.com`). |

Both secrets are passed as environment variables to the `SonarSource/sonarqube-scan-action`.
The job has `continue-on-error: true`, so it will not fail the entire workflow if
SonarQube is unreachable or misconfigured, but no analysis will be produced.

### release.yml

| Secret | Required | Description |
|--------|----------|-------------|
| `GITHUB_TOKEN` | Automatic | Provided automatically by GitHub Actions. Used to create GitHub Releases via the `softprops/action-gh-release` action. No manual configuration needed. |

## How to Re-Enable Automatic Triggers

To restore automatic triggers for any workflow, replace the disabled `on:` block
with the appropriate trigger configuration shown below. In each workflow file, find
this block:

```yaml
# DISABLED: All automatic triggers removed. Use workflow_dispatch to run manually.
on:
  workflow_dispatch:
```

And replace it with the trigger block specific to that workflow, as documented in
the sections below. You may also keep `workflow_dispatch:` alongside the automatic
triggers to retain the ability to run workflows manually.

### ci.yml -- Restore Automatic CI Triggers

Replace the disabled trigger block with:

```yaml
on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]
  workflow_dispatch:
```

This runs the CI pipeline on every push to `master` and on every pull request
targeting `master`. The `workflow_dispatch` trigger is retained for manual runs.

**File:** `.github/workflows/ci.yml`, lines 9-11.

### security.yml -- Restore Automatic Security Triggers

Replace the disabled trigger block with:

```yaml
on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]
  schedule:
    - cron: '0 6 * * 1'
  workflow_dispatch:
```

This runs security scans on pushes and pull requests to `master`, and on a weekly
schedule (every Monday at 06:00 UTC) to catch newly disclosed vulnerabilities in
dependencies. The `workflow_dispatch` trigger is retained for manual runs.

**File:** `.github/workflows/security.yml`, lines 9-11.

### sonar.yml -- Restore Automatic SonarQube Triggers

Replace the disabled trigger block with:

```yaml
on:
  push:
    branches: [ master ]
  pull_request:
    branches: [ master ]
  workflow_dispatch:
```

This runs SonarQube analysis on every push to `master` and on pull requests
targeting `master`. The `workflow_dispatch` trigger is retained for manual runs.

**File:** `.github/workflows/sonar.yml`, lines 9-11.

### release.yml -- Restore Automatic Release Triggers

Replace the disabled trigger block with:

```yaml
on:
  push:
    tags:
      - 'v*'
  workflow_dispatch:
```

This triggers the release workflow when a version tag is pushed (e.g., `v1.0.0`,
`v2.1.0-beta`). Tags with `-alpha`, `-beta`, or `-rc` suffixes will automatically
be marked as pre-releases. The `workflow_dispatch` trigger is retained for manual
runs.

**File:** `.github/workflows/release.yml`, lines 9-11.

## Quick Reference

Summary of all secrets across all workflows:

| Secret | Workflows | Required | Source |
|--------|-----------|----------|--------|
| `GITHUB_TOKEN` | security.yml, release.yml | Automatic | Provided by GitHub Actions |
| `CODECOV_TOKEN` | ci.yml | Optional | [codecov.io](https://codecov.io) |
| `SNYK_TOKEN` | security.yml | Yes (for Snyk job) | [snyk.io](https://snyk.io) |
| `SONAR_TOKEN` | sonar.yml | Yes | SonarQube/SonarCloud instance |
| `SONAR_HOST_URL` | sonar.yml | Yes | SonarQube/SonarCloud URL |
