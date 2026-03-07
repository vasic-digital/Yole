<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Security Scanning Guide

This document describes how to run security scans on the Yole codebase, including static analysis, dependency vulnerability scanning, secret detection, and code quality analysis.

---

## Overview

Yole uses several security scanning tools, all runnable via Docker Compose profiles or standalone commands:

| Tool | Purpose | Docker Compose Service |
|------|---------|----------------------|
| **SonarQube** | Code quality, bugs, vulnerabilities, code smells | `sonarqube` |
| **Snyk** | Dependency vulnerability scanning | `snyk` |
| **Detekt** | Kotlin static analysis | `detekt` |
| **Gitleaks** | Secret/credential detection in Git history | Standalone |
| **OWASP Dependency Check** | CVE database scanning for dependencies | Gradle plugin |
| **CodeQL** | Semantic code analysis (GitHub Actions) | GitHub Actions |

---

## Quick Start

Run all security scanning tools at once using the `security` Docker Compose profile:

```bash
# Start all security services
docker compose --profile security up -d

# Or start the full stack (build + security)
docker compose --profile full up -d
```

---

## SonarQube Setup

SonarQube provides comprehensive code quality analysis including bugs, vulnerabilities, code smells, duplications, and test coverage.

### Starting SonarQube

```bash
# Start SonarQube
docker compose --profile security up -d sonarqube

# Wait for it to start (takes 1-2 minutes)
# Check health at http://localhost:9000
```

### First-Time Configuration

1. Open `http://localhost:9000` in a browser
2. Log in with default credentials: `admin` / `admin`
3. Change the admin password when prompted
4. Create a new project:
   - Click **Create Project** > **Manually**
   - Project key: `yole`
   - Display name: `Yole`
5. Generate an authentication token:
   - Go to **My Account** > **Security** > **Generate Tokens**
   - Name: `yole-scanner`
   - Type: **Project Analysis Token**
   - Copy the generated token

### Running the Scan

```bash
# Inside the build container
docker compose run --rm build ./gradlew sonar \
    -Dsonar.host.url=http://sonarqube:9000 \
    -Dsonar.token=YOUR_TOKEN_HERE \
    -Dsonar.projectKey=yole \
    -Dsonar.projectName=Yole
```

Or configure the token in `gradle.properties`:
```properties
systemProp.sonar.host.url=http://localhost:9000
systemProp.sonar.token=YOUR_TOKEN_HERE
```

Then run:
```bash
docker compose run --rm build ./gradlew sonar
```

### Interpreting Results

Access the dashboard at `http://localhost:9000/dashboard?id=yole`:

| Metric | Description | Target |
|--------|-------------|--------|
| **Bugs** | Code that is demonstrably wrong | 0 |
| **Vulnerabilities** | Security-sensitive code | 0 |
| **Code Smells** | Maintainability issues | Minimize |
| **Duplications** | Duplicated code blocks | < 3% |
| **Coverage** | Test coverage percentage | > 60% |
| **Security Hotspots** | Code requiring security review | Review all |

### SonarQube Configuration

Key settings in `docker-compose.yml`:
```yaml
sonarqube:
  image: docker.io/library/sonarqube:community
  ports:
    - "9000:9000"
  environment:
    - SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true
  volumes:
    - sonarqube_data:/opt/sonarqube/data
    - sonarqube_logs:/opt/sonarqube/logs
    - sonarqube_extensions:/opt/sonarqube/extensions
```

Data is persisted in Docker volumes (`sonarqube_data`, `sonarqube_logs`, `sonarqube_extensions`).

---

## Snyk Scanning

Snyk scans Gradle dependencies for known vulnerabilities and suggests fixes.

### Prerequisites

1. Create a free account at [snyk.io](https://snyk.io/)
2. Get your API token from [Snyk Account Settings](https://app.snyk.io/account)

### Running the Scan

```bash
# Set your Snyk token
export SNYK_TOKEN=your-token-here

# Run Snyk via Docker Compose
docker compose --profile security run --rm snyk snyk test --gradle-sub-project=shared

# Or scan all sub-projects
docker compose --profile security run --rm snyk snyk test --all-sub-projects
```

### Monitoring Mode

```bash
# Continuously monitor for new vulnerabilities
docker compose --profile security run --rm snyk snyk monitor --all-sub-projects
```

This uploads your dependency tree to Snyk's dashboard, which sends alerts when new vulnerabilities are disclosed.

### Interpreting Results

Snyk reports vulnerabilities by severity:

| Severity | Description | Action |
|----------|-------------|--------|
| **Critical** | Actively exploited, easy to exploit | Fix immediately |
| **High** | Likely exploitable, significant impact | Fix within 1 week |
| **Medium** | Harder to exploit or lower impact | Fix within 1 month |
| **Low** | Unlikely to be exploitable | Fix at convenience |

For each vulnerability, Snyk provides:
- **CVE identifier** and description
- **Affected dependency** and version
- **Fix recommendation** (upgrade path or patch)
- **Exploit maturity** (proof of concept, functional, etc.)

---

## Detekt (Kotlin Static Analysis)

Detekt performs static code analysis specifically for Kotlin, detecting code smells, complexity issues, and potential bugs.

### Running Detekt

```bash
# Via Docker Compose
docker compose --profile security run --rm detekt

# Or directly via Gradle
docker compose run --rm build ./gradlew detekt
```

### Configuration

Detekt is configured via `detekt.yml` (if present) or uses sensible defaults. Key rules:

| Rule Set | Description |
|----------|-------------|
| **complexity** | Cyclomatic complexity, long methods, large classes |
| **coroutines** | Coroutine-specific issues (GlobalScope, runBlocking) |
| **exceptions** | Empty catch blocks, swallowed exceptions, generic exceptions |
| **naming** | Naming conventions for classes, functions, variables |
| **performance** | Unnecessary allocations, inefficient patterns |
| **style** | Code style issues, unused imports, magic numbers |

### Interpreting Results

Detekt outputs results to the terminal and generates reports in `build/reports/detekt/`. Each finding has a severity:

- **error** -- Must be fixed (blocks merge)
- **warning** -- Should be fixed
- **info** -- Nice to fix

---

## Gitleaks (Secret Detection)

Gitleaks scans the entire Git history for accidentally committed secrets (API keys, tokens, passwords, private keys).

### Running Gitleaks

```bash
# Install gitleaks
# On Linux: download from https://github.com/gitleaks/gitleaks/releases
# On macOS: brew install gitleaks

# Scan the current repository
gitleaks detect --source /run/media/milosvasic/DATA4TB/Projects/Yole --verbose

# Scan only staged changes (pre-commit hook)
gitleaks protect --staged --verbose

# Generate a JSON report
gitleaks detect --source . --report-format json --report-path gitleaks-report.json
```

### Pre-Commit Hook

Add Gitleaks as a pre-commit hook to prevent secrets from being committed:

```bash
# .git/hooks/pre-commit
#!/bin/bash
gitleaks protect --staged --verbose
```

Make it executable: `chmod +x .git/hooks/pre-commit`

### False Positives

If Gitleaks reports a false positive, add it to `.gitleaksignore`:

```
# .gitleaksignore
# Ignore test fixtures that look like secrets
shared/src/commonTest/kotlin/digital/vasic/yole/network/auth/TestTokens.kt
```

Or use the `[gitleaks:allow]` inline comment for specific lines.

---

## OWASP Dependency Check

The OWASP Dependency Check plugin scans project dependencies against the National Vulnerability Database (NVD).

### Setup

Add the plugin to `build.gradle.kts`:

```kotlin
plugins {
    id("org.owasp.dependencycheck") version "9.0.9"
}

dependencyCheck {
    failBuildOnCVSS = 7.0f // Fail on High/Critical
    formats = listOf("HTML", "JSON")
    suppressionFile = "owasp-suppressions.xml"
}
```

### Running the Check

```bash
# Inside the build container
docker compose run --rm build ./gradlew dependencyCheckAnalyze
```

The report is generated at `build/reports/dependency-check-report.html`.

### Interpreting Results

| CVSS Score | Severity | Action |
|-----------|----------|--------|
| 9.0 - 10.0 | Critical | Fix immediately |
| 7.0 - 8.9 | High | Fix within 1 week |
| 4.0 - 6.9 | Medium | Fix within 1 month |
| 0.1 - 3.9 | Low | Fix at convenience |
| 0.0 | Info | No action needed |

### Suppressing False Positives

Create `owasp-suppressions.xml` for known false positives:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <suppress>
        <notes>This CVE does not apply because we do not use the affected feature</notes>
        <cve>CVE-2024-XXXXX</cve>
    </suppress>
</suppressions>
```

---

## CodeQL Analysis (GitHub Actions)

CodeQL is GitHub's semantic code analysis engine. It runs automatically on pull requests and scheduled scans when configured in GitHub Actions.

### Setup

Create `.github/workflows/codeql.yml`:

```yaml
name: CodeQL Analysis

on:
  push:
    branches: [master]
  pull_request:
    branches: [master]
  schedule:
    - cron: '0 6 * * 1'  # Weekly on Monday at 6 AM

jobs:
  analyze:
    runs-on: ubuntu-latest
    permissions:
      security-events: write
    strategy:
      matrix:
        language: [java-kotlin]
    steps:
      - uses: actions/checkout@v4
      - uses: github/codeql-action/init@v3
        with:
          languages: ${{ matrix.language }}
      - uses: github/codeql-action/autobuild@v3
      - uses: github/codeql-action/analyze@v3
```

### Interpreting Results

CodeQL results appear in the **Security** tab of your GitHub repository under **Code scanning alerts**. Each alert includes:

- **Rule ID and description** -- what was detected
- **Severity** -- error, warning, or note
- **Location** -- file and line number
- **Remediation** -- how to fix the issue

---

## Scan Schedule Recommendations

| Scan | Frequency | Trigger |
|------|-----------|---------|
| **Detekt** | Every commit | Pre-commit hook or CI |
| **Gitleaks** | Every commit | Pre-commit hook |
| **Snyk** | Every PR + weekly monitor | CI + Snyk monitor |
| **SonarQube** | Every PR | CI |
| **OWASP Dependency Check** | Weekly | Scheduled CI job |
| **CodeQL** | Every PR + weekly | GitHub Actions |

---

## Summary of Commands

```bash
# Run all security scans via Docker Compose
docker compose --profile security up -d

# Individual scans
docker compose run --rm build ./gradlew detekt                    # Kotlin static analysis
docker compose run --rm build ./gradlew sonar                     # SonarQube analysis
docker compose --profile security run --rm snyk snyk test         # Snyk dependency scan
docker compose run --rm build ./gradlew dependencyCheckAnalyze    # OWASP dependency check
gitleaks detect --source . --verbose                              # Secret detection
```

---

## Related Documentation

- [Build System Guide](BUILD_SYSTEM.md) -- Build commands and CI/CD setup
- [CI Setup Guide](CI_SETUP_GUIDE.md) -- Continuous integration configuration
- [Troubleshooting](TROUBLESHOOTING.md) -- Common issues and solutions
- [Architecture Guide](../ARCHITECTURE.md) -- System design overview

---

*Last updated: March 7, 2026*
