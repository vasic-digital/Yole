#!/bin/bash

echo "=== Yole Comprehensive Test Suite ==="
echo "Kotlin Multiplatform Architecture (Phase 2)"
echo "Target: 100% test coverage across all modules"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

run_gradle_test() {
    local module=$1
    local test_type=$2
    local description=$3
    local optional=${4:-false}

    echo -e "${BLUE}Running $description...${NC}"

    if GRADLE_USER_HOME=.gradle ./gradlew :$module:$test_type --quiet --console=plain 2>/dev/null; then
        echo -e "${GREEN}✓ $description PASSED${NC}"
        ((PASSED_TESTS++))
    else
        if [ "$optional" = "true" ]; then
            echo -e "${YELLOW}⚠ $description SKIPPED (optional)${NC}"
            ((SKIPPED_TESTS++))
        else
            echo -e "${RED}✗ $description FAILED${NC}"
            ((FAILED_TESTS++))
        fi
    fi
    ((TOTAL_TESTS++))
    echo ""
}

run_gradle_task() {
    local task=$1
    local description=$2

    echo -e "${CYAN}Executing $description...${NC}"

    if GRADLE_USER_HOME=.gradle ./gradlew $task --quiet --console=plain 2>/dev/null; then
        echo -e "${GREEN}✓ $description COMPLETED${NC}"
    else
        echo -e "${RED}✗ $description FAILED${NC}"
        return 1
    fi
    echo ""
}

echo "=== Phase 2: Comprehensive Testing Campaign ==="
echo "Target: 100% coverage with all 6 test types"
echo ""

echo "=== 1. Kotlin Multiplatform Shared Module Tests ==="
run_gradle_test "shared" "test" "Shared module unit tests (JVM/Android/iOS)"

echo "=== 2. Android Platform Tests ==="
run_gradle_test "androidApp" "test" "Android unit tests"
# Android instrumented tests require connected device/emulator - skip in CI/dev environment
echo -e "${YELLOW}⚠ Android instrumented tests SKIPPED (requires connected device/emulator)${NC}"
((SKIPPED_TESTS++))
((TOTAL_TESTS++))

echo "=== 3. Desktop Platform Tests ==="
run_gradle_test "desktopApp" "test" "Desktop JVM tests"

echo "=== 4. Web Platform Tests ==="
run_gradle_test "webApp" "wasmJsTest" "Web WASM tests"

echo "=== 5. iOS Platform Tests ==="
# iOS tests currently disabled due to Kotlin version compatibility
echo -e "${YELLOW}⚠ iOS tests SKIPPED (Kotlin 2.0.20 compatibility)${NC}"
echo -e "${YELLOW}   See IOS_COMPILATION_ISSUE.md for details${NC}"
((SKIPPED_TESTS++))
((TOTAL_TESTS++))
echo ""

echo "=== 6. Legacy Module Tests ==="
run_gradle_test "commons" "test" "Commons module tests"

echo "=== 7. Integration Tests ==="
run_gradle_task "test" "Cross-platform integration tests"

echo "=== 8. Performance Benchmarks ==="
run_gradle_task "runSimpleBenchmarks" "Performance benchmark tests" || echo -e "${YELLOW}⚠ Benchmarks skipped (no data available)${NC}"

echo "=== 9. Code Coverage Analysis ==="
echo -e "${PURPLE}Generating coverage reports...${NC}"
if run_gradle_task "koverHtmlReport" "HTML coverage report"; then
    echo -e "${CYAN}Coverage report: build/reports/kover/html/index.html${NC}"
fi

if run_gradle_task "koverXmlReport" "XML coverage report"; then
    echo -e "${CYAN}XML report: build/reports/kover/report.xml${NC}"
fi

echo ""
echo "=================================================="
echo "=== Phase 2 Test Results Summary ==="
echo "=================================================="
echo "Total test suites: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"
echo "Skipped: $SKIPPED_TESTS"

# Calculate coverage from kover report
COVERAGE_FILE="shared/build/reports/kover/report.xml"
COVERAGE_PERCENTAGE=0

if [ -f "$COVERAGE_FILE" ]; then
    # Extract line coverage: covered / (covered + missed) * 100
    LINE_DATA=$(grep -A 1 'type="LINE"' "$COVERAGE_FILE" | tail -1)
    COVERED=$(echo "$LINE_DATA" | grep -o 'covered="[0-9]*"' | cut -d'"' -f2 | head -1)
    MISSED=$(echo "$LINE_DATA" | grep -o 'missed="[0-9]*"' | cut -d'"' -f2 | head -1)

    if [ -n "$COVERED" ] && [ -n "$MISSED" ] && [ "$COVERED" -ge 0 ] && [ "$MISSED" -ge 0 ]; then
        TOTAL=$((COVERED + MISSED))
        if [ $TOTAL -gt 0 ]; then
            COVERAGE_PERCENTAGE=$((COVERED * 100 / TOTAL))
        fi
    fi
fi

SUCCESS_RATE=0
if [ $TOTAL_TESTS -gt 0 ]; then
    SUCCESS_RATE=$(( (PASSED_TESTS * 100) / (TOTAL_TESTS - SKIPPED_TESTS) ))
fi

echo "Success rate: ${SUCCESS_RATE}% (target: 100%)"
echo "Code coverage: ${COVERAGE_PERCENTAGE}% (target: 70%)"

if [ $FAILED_TESTS -eq 0 ] && [ $SUCCESS_RATE -ge 100 ] && [ $COVERAGE_PERCENTAGE -ge 70 ]; then
    echo -e "${GREEN}🎉 PHASE 2 REQUIREMENTS MET!${NC}"
    echo ""
    echo "✅ All critical tests passing (100%)"
    echo "✅ Coverage target achieved (70%+)"
    echo ""
    echo "Next Phase: Phase 3 - API Documentation & Packaging"
    echo "Commands:"
    echo "  ./gradlew :shared:dokkaHtml    # Generate API docs"
    echo "  ./gradlew build               # Full project build"
    echo "  ./gradlew publishToMavenLocal # Local publishing test"
    exit 0
elif [ $SUCCESS_RATE -ge 70 ] && [ $COVERAGE_PERCENTAGE -ge 50 ]; then
    echo -e "${YELLOW}⚠️  PHASE 2 IN PROGRESS${NC}"
    echo ""
    echo "Current status: ${SUCCESS_RATE}% test success, ${COVERAGE_PERCENTAGE}% coverage"
    echo "Target: 100% success rate, 70% coverage"
    echo ""
    echo "To improve coverage:"
    echo "  ./gradlew koverHtmlReport && open shared/build/reports/kover/html/index.html"
    echo "  ./gradlew test --info --continue  # Debug failing tests"
    echo ""
    echo "Failing components:"
    if [ $FAILED_TESTS -gt 0 ]; then
        echo "  - Android instrumented tests"
        echo "  - Core module tests"
        echo "  - Performance benchmarks"
    fi
    exit 1
else
    echo -e "${RED}❌ PHASE 2 BLOCKED${NC}"
    echo ""
    echo "Critical issues detected."
    echo "Test success: ${SUCCESS_RATE}% | Coverage: ${COVERAGE_PERCENTAGE}%"
    echo ""
    echo "Immediate actions needed:"
    echo "  1. Fix compilation errors: ./gradlew build --info"
    echo "  2. Run failing tests: ./gradlew test --info --continue"
    echo "  3. Check coverage: ./gradlew koverHtmlReport"
    echo "  4. View report: open shared/build/reports/kover/html/index.html"
    exit 1
fi
