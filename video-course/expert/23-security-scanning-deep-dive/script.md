<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 23: Security Scanning Deep Dive (7 videos)

## Video 23.1: Security Architecture Overview (15 min)

### Timestamps
- 0:00 Introduction: security as a first-class concern in Yole
- 2:00 The 6 security scanning tools: SonarQube, Snyk, CodeQL, Gitleaks, Detekt, OWASP
- 4:00 Defense in depth: multiple overlapping tools catching different vulnerability classes
- 6:00 Security patterns in Yole's code: normalizePath(), CancellationException rethrow, SecureStorage
- 8:00 The CVSS 7.0 threshold: build fails on High/Critical vulnerabilities
- 10:00 SBOM generation with CycloneDX for compliance and monitoring
- 12:00 Security scanning in CI/CD: which tools run when
- 14:00 Summary

### Code References
- `docs/SECURITY_SCANNING.md`
- `docker-compose.yml` -- Security service definitions
- `.github/workflows/security.yml` -- CI security scanning
- `shared/src/commonMain/kotlin/digital/vasic/yole/network/common/PathUtils.kt` -- normalizePath()

---

## Video 23.2: SonarQube Deep Dive (20 min)

### Timestamps
- 0:00 What SonarQube detects: bugs, vulnerabilities, code smells, duplications
- 2:00 Setting up SonarQube via Docker Compose
- 4:00 First-time configuration: project creation, token generation
- 6:00 Running analysis: `./gradlew sonar` with proper configuration
- 8:00 Interpreting the dashboard: quality gates, ratings (A-E), issue severity
- 10:00 Security hotspots: code requiring manual security review
- 12:00 Fixing common findings: SQL injection, XSS, path traversal, hard-coded credentials
- 14:00 Quality profiles: customizing which rules are active
- 16:00 Pull request decoration: SonarQube comments on GitHub PRs
- 18:00 Data persistence with Docker volumes
- 19:30 Summary

### Exercises
1. **Run a full SonarQube scan** on the Yole project and identify the top 5 issues by severity.
2. **Create a custom quality gate** that requires 0 bugs, 0 vulnerabilities, and >60% coverage.

---

## Video 23.3: Snyk and Dependency Vulnerability Management (18 min)

### Timestamps
- 0:00 Why dependency scanning matters: transitive vulnerabilities
- 2:00 Setting up Snyk: free account, API token, CLI installation
- 4:00 Running `snyk test --all-sub-projects`: understanding the output
- 6:00 Snyk monitoring mode: continuous alerts for new vulnerabilities
- 8:00 Interpreting findings: CVE identifiers, exploit maturity, fix recommendations
- 10:00 Upgrading vulnerable dependencies: Snyk's auto-fix PRs
- 12:00 Ignoring false positives: `.snyk` policy file
- 14:00 Snyk in CI: blocking PRs with Critical/High vulnerabilities
- 16:00 Comparing Snyk with OWASP Dependency Check
- 17:30 Summary

---

## Video 23.4: CodeQL Semantic Analysis (18 min)

### Timestamps
- 0:00 What makes CodeQL special: semantic analysis vs. pattern matching
- 2:00 How CodeQL works: building a database from your code, running queries
- 4:00 GitHub Actions setup: the codeql.yml workflow
- 6:00 Supported languages: java-kotlin analysis for Yole
- 8:00 Security queries: injection, authentication, cryptography
- 10:00 Custom query suites for project-specific patterns
- 12:00 Interpreting alerts in the GitHub Security tab
- 14:00 Dismissing alerts with justification
- 16:00 CodeQL performance: build time impact and optimization
- 17:30 Summary

### Code References
- `.github/workflows/codeql.yml`

---

## Video 23.5: Secret Detection with Gitleaks (15 min)

### Timestamps
- 0:00 The risk of committed secrets: API keys, tokens, passwords in git history
- 2:00 Installing Gitleaks and basic scanning
- 4:00 Full history scan: `gitleaks detect --source . --verbose`
- 6:00 Pre-commit hook: preventing secrets before they are committed
- 8:00 Handling false positives: `.gitleaksignore` and inline `[gitleaks:allow]`
- 10:00 CI integration: scanning on every push and PR
- 12:00 Remediation: if a secret was committed, rotate it immediately
- 14:00 Summary

---

## Video 23.6: OWASP Dependency Check and SBOM (18 min)

### Timestamps
- 0:00 OWASP Dependency Check: scanning against the NVD (National Vulnerability Database)
- 2:00 Gradle plugin setup: `org.owasp.dependencycheck`
- 4:00 Running the check: `./gradlew dependencyCheckAnalyze`
- 6:00 Interpreting the HTML report: CVSS scores, affected dependencies
- 8:00 Suppressing false positives: `owasp-suppressions.xml`
- 10:00 The CVSS 7.0 threshold: `failBuildOnCVSS = 7.0f`
- 12:00 CycloneDX SBOM generation: `./gradlew cyclonedxBom`
- 14:00 SBOM use cases: compliance, Dependency-Track integration, license auditing
- 16:00 Scheduling weekly OWASP scans in CI
- 17:30 Summary

---

## Video 23.7: Building a Security-First Development Workflow (15 min)

### Timestamps
- 0:00 Putting it all together: the complete security scanning workflow
- 2:00 Pre-commit: Gitleaks protect --staged
- 4:00 Local development: Detekt for Kotlin-specific issues
- 6:00 Pull request: CodeQL + Snyk + OWASP automatically
- 8:00 Merge to master: SonarQube quality gate check
- 10:00 Weekly: OWASP + Snyk monitor for newly disclosed vulnerabilities
- 12:00 The security scanning pipeline diagram (Mermaid)
- 14:00 Summary and best practices

### Code References
- `docs/diagrams/security-scanning-pipeline.mmd`

### Exercises
1. **Set up the full security scanning pipeline** for a fresh project following Yole's model.
2. **Create a custom Detekt rule** that flags any catch block that does not rethrow CancellationException.
3. **Generate an SBOM** and analyze it for license compliance issues.
