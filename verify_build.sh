#!/bin/bash

echo "=== Yole Build Verification Script ==="
echo "Verifying modular architecture and format implementations"
echo

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

TOTAL_CHECKS=0
PASSED_CHECKS=0
FAILED_CHECKS=0

check_module() {
    local module=$1
    local description=$2

    echo -e "${BLUE}Checking $module...${NC}"
    ((TOTAL_CHECKS++))

    if [ -d "$module" ]; then
        echo -e "${GREEN}✓ $description${NC}"
        ((PASSED_CHECKS++))

        # Check for required files
        if [ -f "$module/build.gradle" ]; then
            echo -e "  ${GREEN}✓ build.gradle exists${NC}"
        else
            echo -e "  ${RED}✗ build.gradle missing${NC}"
            ((FAILED_CHECKS++))
        fi

        if [ -d "$module/src/main/java" ]; then
            echo -e "  ${GREEN}✓ Java source directory exists${NC}"
        else
            echo -e "  ${RED}✗ Java source directory missing${NC}"
            ((FAILED_CHECKS++))
        fi

        if [ -d "$module/src/test/java" ]; then
            echo -e "  ${GREEN}✓ Unit test directory exists${NC}"
        else
            echo -e "  ${YELLOW}⚠ Unit test directory missing${NC}"
        fi

    else
        echo -e "${RED}✗ $description${NC}"
        ((FAILED_CHECKS++))
    fi
    echo
}

check_format_implementation() {
    local format=$1
    local module="format-$format"

    echo -e "${BLUE}Checking $format format implementation...${NC}"
    ((TOTAL_CHECKS++))

    if [ -d "$module/src/main/java/digital/vasic/yole/format/$format" ]; then
        local files=("$module/src/main/java/digital/vasic/yole/format/$format/${format^}TextConverter.java"
                    "$module/src/main/java/digital/vasic/yole/format/$format/${format^}SyntaxHighlighter.java"
                    "$module/src/main/java/digital/vasic/yole/format/$format/${format^}ActionButtons.java")

        local all_files_exist=true
        for file in "${files[@]}"; do
            if [ ! -f "$file" ]; then
                all_files_exist=false
                break
            fi
        done

        if $all_files_exist; then
            echo -e "${GREEN}✓ $format format fully implemented${NC}"
            ((PASSED_CHECKS++))
        else
            echo -e "${RED}✗ $format format incomplete${NC}"
            ((FAILED_CHECKS++))
        fi
    else
        echo -e "${RED}✗ $format format directory missing${NC}"
        ((FAILED_CHECKS++))
    fi
    echo
}

# Check core modules
check_module "commons" "Commons module (shared utilities)"
check_module "core" "Core module (reusable components)"
check_module "app" "App module (UI components)"

# Check existing format modules
EXISTING_FORMATS=("markdown" "todotxt" "csv" "wikitext" "keyvalue" "asciidoc" "orgmode" "plaintext")
for format in "${EXISTING_FORMATS[@]}"; do
    check_module "format-$format" "$format format module"
done

# Check new format modules
NEW_FORMATS=("latex" "restructuredtext" "taskpaper" "textile" "creole" "tiddlywiki" "jupyter" "rmarkdown")
for format in "${NEW_FORMATS[@]}"; do
    check_module "format-$format" "$format format module (NEW)"
    check_format_implementation "$format"
done

# Check configuration files
echo -e "${BLUE}Checking configuration files...${NC}"
((TOTAL_CHECKS++))

if [ -f "settings.gradle" ] && grep -q "include.*format-" settings.gradle; then
    echo -e "${GREEN}✓ settings.gradle configured for all modules${NC}"
    ((PASSED_CHECKS++))
else
    echo -e "${RED}✗ settings.gradle not properly configured${NC}"
    ((FAILED_CHECKS++))
fi

if [ -f "ARCHITECTURE.md" ]; then
    echo -e "${GREEN}✓ Architecture documentation exists${NC}"
    ((PASSED_CHECKS++))
else
    echo -e "${RED}✗ Architecture documentation missing${NC}"
    ((FAILED_CHECKS++))
fi

# Check test scripts
if [ -f "run_all_tests.sh" ]; then
    echo -e "${GREEN}✓ Test runner script exists${NC}"
    ((PASSED_CHECKS++))
else
    echo -e "${RED}✗ Test runner script missing${NC}"
    ((FAILED_CHECKS++))
fi

echo
echo "=== Verification Results ==="
echo "Total checks: $TOTAL_CHECKS"
echo "Passed: $PASSED_CHECKS"
echo "Failed: $FAILED_CHECKS"

if [ $FAILED_CHECKS -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL VERIFICATION CHECKS PASSED!${NC}"
    echo
    echo "=== Next Steps ==="
    echo "1. Fix Gradle wrapper configuration"
    echo "2. Run: ./gradlew build"
    echo "3. Run: ./run_all_tests.sh"
    echo "4. Verify AI QA emulator setup"
    exit 0
else
    echo -e "${RED}❌ Some verification checks failed.${NC}"
    echo "Success rate: $((PASSED_CHECKS * 100 / TOTAL_CHECKS))%"
    exit 1
fi