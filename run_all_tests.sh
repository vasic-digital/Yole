#!/bin/bash

echo "=== Yole Comprehensive Test Suite ==="
echo "Kotlin Multiplatform Architecture"
echo "Running all tests with 100% success requirement"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

run_gradle_test() {
    local module=$1
    local test_type=$2
    local description=$3

    echo -e "${BLUE}Running $description...${NC}"

    if GRADLE_USER_HOME=.gradle ./gradlew :$module:$test_type 2>/dev/null; then
        echo -e "${GREEN}✓ $description PASSED${NC}"
        ((PASSED_TESTS++))
    else
        echo -e "${RED}✗ $description FAILED${NC}"
        ((FAILED_TESTS++))
    fi
    ((TOTAL_TESTS++))
    echo ""
}

echo "=== Kotlin Multiplatform Shared Module Tests ==="
run_gradle_test "shared" "test" "Shared module tests (all platforms)"

echo ""
echo "=== Android App Tests ==="
run_gradle_test "androidApp" "testDebugUnitTest" "Android app unit tests"
run_gradle_test "androidApp" "connectedAndroidTest" "Android instrumented tests"

echo ""
echo "=== Desktop App Tests ==="
run_gradle_test "desktopApp" "test" "Desktop app tests"

echo ""
echo "=== Web App Tests ==="
run_gradle_test "webApp" "wasmJsTest" "Web app tests (Wasm)"

echo ""
echo "=== iOS App Tests ==="
# iOS tests currently disabled due to Kotlin 2.1.0 issue
echo -e "${YELLOW}⚠ iOS tests SKIPPED (see IOS_COMPILATION_ISSUE.md)${NC}"
echo ""

echo ""
echo "=== Legacy Module Tests ==="
run_gradle_test "commons" "test" "Commons module tests"
run_gradle_test "core" "test" "Core module tests"

echo ""
echo "=================================================="
echo "=== Test Results Summary ==="
echo "=================================================="
echo "Total test suites: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"
echo "Skipped: 1 (iOS - blocked)"

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL TESTS PASSED! (100% success rate)${NC}"
    echo ""
    echo "Next steps:"
    echo "  - Run: ./gradlew koverHtmlReport"
    echo "  - View coverage: open build/reports/kover/html/index.html"
    exit 0
else
    SUCCESS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    echo -e "${RED}❌ Some tests failed. Success rate: ${SUCCESS_RATE}%${NC}"
    echo ""
    echo "To see detailed failures, run:"
    echo "  ./gradlew test --info"
    exit 1
fi
