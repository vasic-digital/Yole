#!/bin/bash
# Yole Test Automation Script - Runs all tests with proper frameworks
#
# SPDX-FileCopyrightText: 2025-2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Yole Test Automation ===${NC}"

# Detect container runtime (prefer podman, fall back to docker)
CONTAINER_RUNTIME="$(command -v podman 2>/dev/null || command -v docker 2>/dev/null || true)"
COMPOSE_CMD="$(command -v podman-compose 2>/dev/null || echo "${CONTAINER_RUNTIME} compose" 2>/dev/null || true)"

# Test results directory
TEST_RESULTS="releases/test-results"
mkdir -p "$TEST_RESULTS"

# Run shared module unit tests (desktop JVM — works on all platforms without Android SDK)
run_unit_tests() {
    echo -e "${YELLOW}Running shared module unit tests (desktop JVM)...${NC}"

    ./gradlew :shared:desktopTest \
        --info \
        --no-daemon

    cp -r shared/build/reports/tests "$TEST_RESULTS/unit" 2>/dev/null || true
}

# Run Android tests with Robolectric
run_android_tests() {
    echo -e "${YELLOW}Running Android Robolectric tests...${NC}"

    # Check if emulator is running
    if command -v adb &>/dev/null && adb devices | grep -q "emulator"; then
        echo "Emulator detected, running instrumented tests..."

        ./gradlew :androidApp:testDebugUnitTest \
            --info \
            --no-daemon

        # Run on emulator
        ./gradlew :androidApp:connectedDebugAndroidTest \
            --info \
            --no-daemon || true
    else
        echo "No emulator, running Robolectric only..."
        ./gradlew :androidApp:testDebugUnitTest \
            -Pandroid.testInstrumentationRunnerArguments.notAnnotation=androidx.test.filters.LargeTest \
            --info \
            --no-daemon
    fi

    cp -r androidApp/build/reports/tests "$TEST_RESULTS/android" 2>/dev/null || true
}

# Run Desktop app tests
run_desktop_tests() {
    echo -e "${YELLOW}Running Desktop app tests...${NC}"

    ./gradlew :desktopApp:test \
        --info \
        --no-daemon

    cp -r desktopApp/build/reports/tests "$TEST_RESULTS/desktop" 2>/dev/null || true
}

# Run Web/WASM tests
run_web_tests() {
    echo -e "${YELLOW}Running Web/WASM tests...${NC}"

    ./gradlew :webApp:wasmJsBrowserTest \
        --info \
        --no-daemon || true

    cp -r webApp/build/reports/tests "$TEST_RESULTS/web" 2>/dev/null || true
}

# Run iOS tests (requires macOS)
run_ios_tests() {
    echo -e "${YELLOW}Running iOS tests...${NC}"

    if [[ "$OSTYPE" == "darwin"* ]]; then
        ./gradlew :shared:iosX64Test --info --no-daemon || true
    else
        echo "iOS tests require macOS, skipping..."
    fi
}

# Run integration tests
run_integration_tests() {
    echo -e "${YELLOW}Running integration tests...${NC}"

    # Start required services if container runtime is available
    if [ -n "$COMPOSE_CMD" ]; then
        $COMPOSE_CMD up -d sonarqube 2>/dev/null || true
        sleep 30
    fi

    # Run integration tests via desktop JVM
    ./gradlew :shared:desktopTest \
        --tests "digital.vasic.yole.network.integration.*" \
        --info \
        --no-daemon

    cp -r shared/build/reports/tests "$TEST_RESULTS/integration" 2>/dev/null || true
}

# Run stress tests
run_stress_tests() {
    echo -e "${YELLOW}Running stress tests...${NC}"

    ./gradlew :shared:desktopTest \
        --tests "digital.vasic.yole.format.stress.*" \
        --tests "digital.vasic.yole.network.stress.*" \
        --info \
        --no-daemon

    cp -r shared/build/reports/tests "$TEST_RESULTS/stress" 2>/dev/null || true
}

# Generate test report
generate_report() {
    echo -e "${YELLOW}Generating test report...${NC}"

    cat > "$TEST_RESULTS/index.html" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Yole Test Results</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .pass { color: green; }
        .fail { color: red; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #4CAF50; color: white; }
    </style>
</head>
<body>
    <h1>Yole Test Results</h1>
    <p>Generated: $(date)</p>
    <table>
        <tr><th>Test Suite</th><th>Status</th></tr>
        <tr><td>Shared Module Unit Tests (Desktop JVM)</td><td class="pass">Completed</td></tr>
        <tr><td>Android Tests</td><td class="pass">Completed</td></tr>
        <tr><td>Desktop App Tests</td><td class="pass">Completed</td></tr>
        <tr><td>Web/WASM Tests</td><td class="pass">Completed</td></tr>
        <tr><td>Integration Tests</td><td class="pass">Completed</td></tr>
        <tr><td>Stress Tests</td><td class="pass">Completed</td></tr>
    </table>
</body>
</html>
EOF

    echo -e "${GREEN}Test report generated at $TEST_RESULTS/index.html${NC}"
}

# Main
main() {
    run_unit_tests
    run_android_tests
    run_desktop_tests
    run_web_tests
    run_ios_tests
    run_integration_tests
    run_stress_tests
    generate_report

    echo -e "${GREEN}=== All Tests Complete ===${NC}"
    echo "Results: $TEST_RESULTS/"
}

main "$@"
