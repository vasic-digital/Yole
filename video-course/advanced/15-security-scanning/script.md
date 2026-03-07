# Module 15: Security Scanning (7 videos)

## Video 15.1: Security Scanning Overview (15 min)

### Timestamps
- 0:00 Introduction: why security scanning matters for a text editor
- 1:30 Attack surface: file parsing, network protocols, OAuth2 tokens, HTML preview
- 3:00 Defense in depth: multiple scanners catching different vulnerability classes
- 5:00 Yole's security scanning toolkit: SonarQube, Snyk, Gitleaks, CodeQL, OWASP, Detekt
- 7:00 Scanning categories: SAST (static), SCA (dependencies), secrets detection
- 9:00 Integration points: local development, CI/CD, pre-commit hooks
- 11:00 The scanning workflow: scan, triage, fix, verify, document
- 13:00 Severity classification: critical, high, medium, low, informational
- 14:30 Summary

### Key Concepts

Security scanning for Yole covers six categories:

| Scanner | Category | What It Finds |
|---------|----------|---------------|
| SonarQube | SAST | Bugs, vulnerabilities, code smells |
| Snyk | SCA | Dependency vulnerabilities |
| Gitleaks | Secrets | Hardcoded credentials, API keys |
| CodeQL | SAST | Semantic code vulnerabilities |
| OWASP DC | SCA | Known CVEs in dependencies |
| Detekt | SAST | Kotlin-specific code issues |

### Exercises
1. **Threat model** -- Create a threat model for Yole listing the top 5 attack vectors (e.g., malicious file content, MITM on cloud sync, XSS in HTML preview).
2. **Scanner selection** -- For each threat in your model, identify which scanner(s) would detect the vulnerability.

---

## Video 15.2: SonarQube Analysis (20 min)

### Timestamps
- 0:00 SonarQube Community Edition: features and limitations
- 2:00 Starting SonarQube in Docker: `docker compose --profile security up sonarqube`
- 4:00 Project setup: creating a project, generating a token
- 6:00 Configuring the Gradle Sonar plugin in `build.gradle.kts`
- 8:00 Running the analysis: `./gradlew sonar`
- 10:00 Dashboard walkthrough: reliability, security, maintainability ratings
- 12:00 Bugs vs. vulnerabilities vs. code smells: understanding the difference
- 14:00 Security hotspots: areas that need manual review
- 16:00 Coverage integration: importing Kover reports into SonarQube
- 18:00 Quality profiles: customizing rules for Kotlin
- 19:30 Summary

### Code References
- `docker-compose.yml` -- SonarQube service definition with persistent volumes
- `shared/build.gradle.kts` -- Kover configuration for coverage data

### Key Commands

```bash
# Start SonarQube (first run takes ~60s to initialize)
docker compose --profile security up -d sonarqube

# Run analysis with coverage data
docker compose run --rm build bash -c \
  "./gradlew test koverXmlReport sonar \
    -Dsonar.host.url=http://sonarqube:9000 \
    -Dsonar.token=YOUR_TOKEN \
    -Dsonar.coverage.jacoco.xmlReportPaths=shared/build/reports/kover/xml/report.xml"
```

### Exercises
1. **First analysis** -- Run a SonarQube analysis and identify the top 5 issues by severity.
2. **Custom quality gate** -- Create a quality gate requiring: zero critical/blocker bugs, at least 60% coverage on new code, and less than 3% code duplication.
3. **Track trends** -- Run the analysis on two different commits and compare the trend graphs in the SonarQube dashboard.

---

## Video 15.3: Snyk Vulnerability Scanning (15 min)

### Timestamps
- 0:00 What Snyk detects: known CVEs in dependencies
- 1:30 Setting up a Snyk account and obtaining `SNYK_TOKEN`
- 3:00 Running Snyk in container: `docker compose --profile security run snyk`
- 5:00 Interpreting results: vulnerability severity, exploit maturity, fix availability
- 7:00 Dependency tree analysis: understanding transitive dependencies
- 9:00 Fix strategies: upgrade, patch, or replace vulnerable dependencies
- 11:00 Snyk monitoring: continuous watching for new vulnerabilities
- 13:00 Integration with `gradle/libs.versions.toml` version catalog
- 14:30 Summary

### Code References
- `docker-compose.yml` -- Snyk service definition with `SNYK_TOKEN` environment variable
- `gradle/libs.versions.toml` -- Centralized dependency versions

### Key Commands

```bash
# Run Snyk vulnerability scan
SNYK_TOKEN=your-token docker compose --profile security run --rm snyk \
  test --all-projects --severity-threshold=high

# Monitor for new vulnerabilities
SNYK_TOKEN=your-token docker compose --profile security run --rm snyk \
  monitor --all-projects

# Show dependency tree
SNYK_TOKEN=your-token docker compose --profile security run --rm snyk \
  test --all-projects --print-deps
```

### Exercises
1. **Dependency audit** -- Run Snyk and create a spreadsheet of all findings with columns: dependency, version, CVE, severity, fix version.
2. **Fix a vulnerability** -- Pick a finding with an available fix, update the version in `libs.versions.toml`, re-run Snyk, and verify the fix.

---

## Video 15.4: Gitleaks Secret Detection (12 min)

### Timestamps
- 0:00 Why secret detection matters: leaked API keys, tokens, passwords
- 1:30 Gitleaks: scanning git history for secrets
- 3:00 Installing Gitleaks: container or binary
- 5:00 Running against the Yole repository: `gitleaks detect --source .`
- 7:00 Interpreting results: rule ID, secret type, commit, file, line
- 8:30 Custom rules: adding patterns for Yole-specific secrets
- 10:00 Pre-commit hooks: preventing secrets from being committed
- 11:30 Summary

### Key Commands

```bash
# Scan current state
docker run --rm -v $(pwd):/repo zricethezav/gitleaks:latest detect --source /repo

# Scan full git history
docker run --rm -v $(pwd):/repo zricethezav/gitleaks:latest detect \
  --source /repo --log-opts="--all"

# Generate report
docker run --rm -v $(pwd):/repo zricethezav/gitleaks:latest detect \
  --source /repo --report-format json --report-path /repo/gitleaks-report.json

# Install as pre-commit hook
# .pre-commit-config.yaml:
# - repo: https://github.com/gitleaks/gitleaks
#   rev: v8.18.0
#   hooks:
#     - id: gitleaks
```

### Exercises
1. **History scan** -- Run Gitleaks against the full Yole git history and review any findings.
2. **Pre-commit hook** -- Set up a Gitleaks pre-commit hook, then try committing a file with a fake API key (`FAKE_API_KEY=sk-1234567890abcdef`) and verify the hook blocks the commit.

---

## Video 15.5: CodeQL Analysis (15 min)

### Timestamps
- 0:00 What is CodeQL: GitHub's semantic code analysis engine
- 1:30 CodeQL vs. pattern-matching SAST: understanding data flow analysis
- 3:00 Setting up CodeQL for Kotlin/Java projects
- 5:00 Creating a CodeQL database from the Yole codebase
- 7:00 Running built-in query suites: security-and-quality, security-extended
- 9:00 Interpreting results: data flow paths, source-to-sink analysis
- 11:00 Custom queries: writing CodeQL for Yole-specific patterns
- 12:30 GitHub Actions integration for automated CodeQL scans
- 14:00 Summary

### Key Commands

```bash
# Create CodeQL database
codeql database create yole-db --language=java \
  --command="./gradlew :shared:compileKotlinDesktop"

# Run security queries
codeql database analyze yole-db \
  codeql/java-queries:codeql-suites/java-security-and-quality.qls \
  --format=sarif-latest --output=results.sarif

# Run in GitHub Actions (workflow example)
# .github/workflows/codeql.yml
```

### Exercises
1. **First CodeQL scan** -- Create a CodeQL database for Yole and run the security-and-quality suite. Compare findings with SonarQube results.
2. **Data flow analysis** -- Find a CodeQL result with a multi-step data flow path and trace it through the source code to understand the vulnerability.

---

## Video 15.6: OWASP Dependency Check (12 min)

### Timestamps
- 0:00 OWASP Dependency Check: scanning for known CVEs in dependencies
- 1:30 How it works: National Vulnerability Database (NVD) matching
- 3:00 Gradle plugin setup: `org.owasp.dependencycheck`
- 5:00 Running the check: `./gradlew dependencyCheckAnalyze`
- 7:00 Interpreting the HTML report: CVE details, CVSS scores, affected components
- 9:00 Suppressing false positives with a suppression XML file
- 10:30 Comparing with Snyk: overlap and differences
- 11:30 Summary

### Key Commands

```bash
# Run OWASP Dependency Check
docker compose run --rm build ./gradlew dependencyCheckAnalyze

# Generate HTML report
# Output: shared/build/reports/dependency-check-report.html

# With suppression file
docker compose run --rm build ./gradlew dependencyCheckAnalyze \
  -Pdependency.check.suppression=owasp-suppressions.xml
```

### Exercises
1. **NVD analysis** -- Run OWASP Dependency Check and compare its findings with Snyk's results. Identify any CVEs found by one tool but not the other.
2. **Suppression file** -- Create a suppression file for any confirmed false positive and verify it is excluded from subsequent runs.

---

## Video 15.7: Detekt Static Analysis and Fixing Findings (18 min)

### Timestamps
- 0:00 What Detekt detects: Kotlin-specific code smells and style issues
- 2:00 Running Detekt in container: `docker compose --profile security run detekt`
- 4:00 Default rule sets: complexity, coroutines, empty-blocks, exceptions, naming, performance, style
- 6:00 Configuring Detekt: `detekt.yml` rule customization
- 8:00 Suppressing rules with `@Suppress` annotations
- 10:00 Interpreting the report: issue count by category, file-level breakdown
- 12:00 Fixing common findings: long methods, complex conditions, magic numbers
- 14:00 Combining all scanner results: creating an actionable remediation plan
- 15:30 Prioritization: critical security fixes first, then high, then code quality
- 17:00 Summary

### Code References
- `docker-compose.yml` -- Detekt service definition

### Key Commands

```bash
# Run Detekt
docker compose --profile security run --rm detekt

# Run with custom config
docker compose run --rm build ./gradlew detekt \
  --config shared/detekt.yml

# Generate baseline (to track only new issues)
docker compose run --rm build ./gradlew detektBaseline
```

### Key Concept: Remediation Priority Matrix

| Priority | Scanner | Action |
|----------|---------|--------|
| P0 | Gitleaks | Rotate any leaked secrets immediately |
| P1 | Snyk/OWASP | Upgrade dependencies with critical CVEs |
| P1 | CodeQL | Fix security vulnerabilities with data flow evidence |
| P2 | SonarQube | Fix bugs and security hotspots |
| P3 | Detekt | Improve code quality and maintainability |

### Exercises
1. **Detekt analysis** -- Run Detekt and categorize findings by rule set. Identify the rule set with the most violations.
2. **Fix and verify** -- Pick 3 Detekt findings, fix them in the source code, and re-run Detekt to verify they are resolved.
3. **Full security report** -- Run all 6 scanners against Yole and produce a combined remediation report with prioritized findings from each tool.
