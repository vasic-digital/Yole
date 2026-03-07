#!/bin/bash
#######################################################
#
# SPDX-FileCopyrightText: 2025 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Local Security Scanning Script for Yole
# Runs all available security scans locally
#
#######################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Yole Security Scanning Suite${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

cd "$PROJECT_ROOT"

# Track results
FAILED=0
WARNINGS=0

# Function to run a check
run_check() {
    local name="$1"
    local command="$2"
    local critical="${3:-false}"

    echo -e "${YELLOW}Running: $name...${NC}"

    if eval "$command"; then
        echo -e "${GREEN}[PASS] $name${NC}"
        echo ""
    else
        if [ "$critical" = "true" ]; then
            echo -e "${RED}[FAIL] $name (CRITICAL)${NC}"
            FAILED=$((FAILED + 1))
        else
            echo -e "${YELLOW}[WARN] $name${NC}"
            WARNINGS=$((WARNINGS + 1))
        fi
        echo ""
    fi
}

# 1. Gitleaks - Secret scanning
echo -e "${BLUE}[1/6] Secret Scanning${NC}"
if command -v gitleaks &> /dev/null; then
    run_check "Gitleaks Secret Scan" "gitleaks detect --source . --config .gitleaks.toml --verbose" "true"
else
    echo -e "${YELLOW}[SKIP] Gitleaks not installed. Install with: brew install gitleaks${NC}"
    echo ""
fi

# 2. OWASP Dependency Check
echo -e "${BLUE}[2/6] Dependency Vulnerability Scan${NC}"
run_check "OWASP Dependency Check" "./gradlew dependencyCheckAnalyze --info 2>&1 | tail -20" "false"

# 3. Detekt - Static Analysis
echo -e "${BLUE}[3/6] Static Analysis (Detekt)${NC}"
run_check "Detekt Static Analysis" "./gradlew detekt --info 2>&1 | tail -20" "false"

# 4. Detekt - Additional Static Analysis Pass (stricter rules)
echo -e "${BLUE}[4/6] Static Analysis (Detekt - strict)${NC}"
run_check "Detekt Strict Analysis" "./gradlew detekt --info 2>&1 | tail -20" "false"

# 5. Android Lint
echo -e "${BLUE}[5/6] Android Lint${NC}"
run_check "Android Lint" "./gradlew :androidApp:lintDebug --info 2>&1 | tail -20" "false"

# 6. Check for hardcoded secrets in code
echo -e "${BLUE}[6/6] Manual Secret Patterns${NC}"
echo "Checking for common secret patterns..."

SECRET_PATTERNS=(
    'password\s*=\s*"[^"]*"'
    'api[_-]?key\s*=\s*"[^"]*"'
    'secret\s*=\s*"[^"]*"'
    'token\s*=\s*"[^"]*"'
    'BEGIN RSA PRIVATE KEY'
    'BEGIN DSA PRIVATE KEY'
    'BEGIN EC PRIVATE KEY'
    'BEGIN OPENSSH PRIVATE KEY'
)

FOUND_SECRETS=false
for pattern in "${SECRET_PATTERNS[@]}"; do
    if grep -rn --include="*.kt" --include="*.java" -E "$pattern" . \
        --exclude-dir=build \
        --exclude-dir=.gradle \
        --exclude-dir=.git \
        --exclude="*Test*" \
        --exclude="*test*" \
        --exclude="*Mock*" \
        --exclude="*Fake*" 2>/dev/null | head -5; then
        FOUND_SECRETS=true
    fi
done

if [ "$FOUND_SECRETS" = "true" ]; then
    echo -e "${YELLOW}[WARN] Potential secrets found - please review${NC}"
    WARNINGS=$((WARNINGS + 1))
else
    echo -e "${GREEN}[PASS] No obvious secret patterns found${NC}"
fi
echo ""

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Security Scan Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

if [ $FAILED -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}All security checks passed!${NC}"
    exit 0
elif [ $FAILED -eq 0 ]; then
    echo -e "${YELLOW}Security scan completed with $WARNINGS warning(s)${NC}"
    echo "Review warnings above and fix if necessary."
    exit 0
else
    echo -e "${RED}Security scan FAILED with $FAILED critical issue(s) and $WARNINGS warning(s)${NC}"
    echo "Fix critical issues before committing!"
    exit 1
fi
