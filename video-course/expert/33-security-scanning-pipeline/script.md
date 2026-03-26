<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Module 33: Security Scanning Pipeline (8 videos)

## Prerequisites

- Module 14: Container-Based Development
- Module 15: Security Scanning
- Module 23: Security Scanning Deep Dive

## Learning Objectives

- Run the full 6-tool security scanning pipeline in Yole
- Interpret and triage findings from SonarQube, Snyk, Detekt, and OWASP Dependency Check
- Parameterize secrets (keystore passwords, API tokens) so they never appear in source
- Configure SonarQube project versioning for accurate historical tracking
- Achieve and maintain zero Detekt violations across a large KMP codebase

---

## Video 33.1: The 6-Tool Security Pipeline Overview (12 min)

### Timestamps
- 0:00 Introduction: defense in depth through layered scanning
- 2:00 Tool 1: Detekt -- Kotlin static analysis, enforced at compile time via Gradle
- 4:00 Tool 2: SonarQube -- code quality, bug detection, security hotspots (runs in Docker)
- 6:00 Tool 3: Snyk -- dependency vulnerability scanning against CVE databases
- 8:00 Tool 4: CodeQL -- semantic analysis for Java/Kotlin (finds logic-level vulnerabilities)
- 9:00 Tool 5: Gitleaks -- secret detection in git history and working tree
- 10:00 Tool 6: OWASP Dependency Check -- CVE scanning with SBOM generation
- 11:30 Summary and when to run each tool

### Code References
- `Makefile` -- `make security`, `make security-full`, `make security-scan` targets
- `docker-compose.yml` -- SonarQube and Snyk service definitions
- `config/detekt/detekt.yml` -- Detekt rule configuration

---

## Video 33.2: Detekt — Zero Violations at Scale (14 min)

### Timestamps
- 0:00 Why zero violations matters: each suppression is a gap in the safety net
- 2:00 Key rules in config/detekt/detekt.yml: GlobalCoroutineUsage, SwallowedException, SleepInsteadOfDelay
- 4:00 Complexity limits: cyclomatic complexity 30, nested block depth 5, method length 100 lines
- 6:00 Running Detekt: `./gradlew detekt` and interpreting the HTML report
- 8:00 Fixing violations: restructuring complex functions, extracting helper methods
- 10:00 The FunctionNaming pattern: allowing PascalCase for test methods while enforcing camelCase elsewhere
- 12:00 Integrating Detekt into the pre-merge verification workflow
- 13:30 Summary

### Code References
- `config/detekt/detekt.yml` -- Full rule configuration
- `shared/build.gradle.kts` -- Detekt plugin configuration

---

## Video 33.3: SonarQube Setup and Project Versioning (14 min)

### Timestamps
- 0:00 SonarQube in Docker: starting the server with docker compose
- 2:00 First-time setup: creating the Yole project, generating a project token
- 4:00 Running a scan: `make security` and what the scanner sends to the server
- 6:00 Interpreting the dashboard: bugs, vulnerabilities, code smells, security hotspots, coverage
- 8:00 Project versioning: why setting sonar.projectVersion matters for historical comparison
- 10:00 Updating the project version to 2.19.0 in the scanner configuration
- 12:00 Quality gate configuration: what thresholds block a release
- 13:30 Summary

### Code References
- `docker-compose.yml` -- SonarQube service definition
- `scripts/run_security_scan.sh` -- Scanner invocation script

---

## Video 33.4: Snyk Dependency Vulnerability Scanning (12 min)

### Timestamps
- 0:00 Why dependency scanning: your code is only as secure as your libraries
- 2:00 Snyk in Docker: running `make security-full` to invoke the Snyk container
- 4:00 Reading the Snyk report: severity levels (critical/high/medium/low), CVSS scores
- 6:00 Triage strategy: fix critical and high; accept or ignore medium and low with justification
- 8:00 Updating vulnerable dependencies: version catalog (`gradle/libs.versions.toml`) as the single source of truth
- 10:00 Monitoring: re-running Snyk after each dependency update
- 11:30 Summary

### Code References
- `gradle/libs.versions.toml` -- Centralized dependency versions
- `docker-compose.yml` -- Snyk service definition

---

## Video 33.5: Credential Management and Secret Parameterization (14 min)

### Timestamps
- 0:00 The problem: hardcoded passwords in build scripts are a critical security risk
- 2:00 Case study: keystore passwords hardcoded in build.sh -- what went wrong
- 4:00 The fix: reading passwords from environment variables at build time
- 6:00 Environment variable pattern: YOLE_KEYSTORE_PASSWORD, YOLE_KEY_ALIAS, YOLE_KEY_PASSWORD
- 8:00 Makefile integration: passing env vars into container builds
- 10:00 Gitleaks: detecting secrets already committed to git history
- 12:00 Pre-commit hooks: preventing future accidental credential commits
- 13:30 Summary

### Code References
- `Makefile` -- Container build targets with env var forwarding
- `docker-compose.yml` -- Environment variable injection

---

## Video 33.6: OWASP Dependency Check and SBOM Generation (12 min)

### Timestamps
- 0:00 OWASP Dependency Check vs Snyk: complementary tools with different databases
- 2:00 Running OWASP Dependency Check manually and interpreting the HTML report
- 3:00 CVSS score thresholds: what score triggers a block vs a warning
- 5:00 Software Bill of Materials (SBOM): what it is and why enterprise customers require it
- 7:00 Generating an SBOM with CycloneDX Gradle plugin
- 9:00 SBOM formats: CycloneDX JSON vs SPDX
- 10:00 Including the SBOM in release artifacts
- 11:30 Summary

### Code References
- `docs/SBOM_GUIDE.md` -- SBOM generation and usage guide
- `gradle/libs.versions.toml` -- Dependency inventory for SBOM

---

## Video 33.7: CodeQL and Gitleaks (12 min)

### Timestamps
- 0:00 CodeQL: semantic analysis that finds vulnerabilities Detekt and SonarQube miss
- 2:00 Setting up CodeQL for Kotlin: initializing the database, running queries
- 4:00 Key CodeQL query suites for Kotlin: security-extended, security-and-quality
- 6:00 Gitleaks: scanning git history for leaked secrets (API keys, passwords, tokens)
- 8:00 Running Gitleaks: `gitleaks detect --source .` and interpreting findings
- 10:00 Remediating git history secrets: rotating credentials, using git-filter-repo
- 11:30 Summary

---

## Video 33.8: Building a Security-First Development Workflow (14 min)

### Timestamps
- 0:00 Shifting left: catching security issues before code review, not after deployment
- 2:00 Developer workflow: Detekt on every build, Gitleaks as a pre-commit hook
- 4:00 Weekly cadence: SonarQube and Snyk scans on the latest main branch
- 6:00 Release cadence: full 6-tool scan before tagging a release
- 8:00 SecurityEventLogger: structured audit trail for security-relevant operations at runtime
- 10:00 Triage SLA: critical within 24h, high within 7 days, medium within 30 days
- 12:00 Documentation: SECURITY_SCANNING.md, SBOM_GUIDE.md, SECURITY_EVENT_LOGGING.md
- 13:30 Summary

### Exercises
1. **Detekt run**: Run `./gradlew detekt` and verify zero violations. If violations exist, fix the first one and re-run.
2. **SonarQube scan**: Start the SonarQube Docker container and run `make security`. Identify one security hotspot and document your triage decision.
3. **Secret audit**: Run Gitleaks on the repository. Document any findings and the remediation plan.
4. **Credential parameterization**: Identify any hardcoded credentials in build scripts. Replace them with environment variable references and update the Makefile accordingly.
5. **SBOM generation**: Generate a CycloneDX SBOM for the project. Count the number of direct and transitive dependencies listed.
