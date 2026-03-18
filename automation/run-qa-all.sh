#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Yole Master QA Orchestration Script
#
# Runs the COMPLETE QA pipeline across ALL platforms:
#   1. Unit tests (shared desktop, Go submodules)
#   2. Static analysis (Detekt)
#   3. Build all platforms (Android debug+release, Desktop JAR, Web WASM)
#   4. Platform automation (Android emulator, Web Playwright, Desktop Compose)
#   5. HelixQA evidence validation (screenshots, videos, logs)
#   6. Ticket generation for failures
#   7. Unified QA report
#
# Usage:
#   ./automation/run-qa-all.sh [--skip-build] [--skip-unit] [--platform all]
#
# Exit codes:
#   0  All checks passed
#   1  Test or evidence failures detected

set -uo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
AUTOMATION_DIR="$PROJECT_ROOT/automation"
QA_RESULTS_DIR="$PROJECT_ROOT/qa-results"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
REPORT_FILE="$QA_RESULTS_DIR/qa-report-${TIMESTAMP}.md"

SKIP_BUILD=false
SKIP_UNIT=false
PLATFORM="all"

# Parse flags
while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-build) SKIP_BUILD=true; shift ;;
        --skip-unit)  SKIP_UNIT=true; shift ;;
        --platform)   PLATFORM="$2"; shift 2 ;;
        *) echo "Unknown flag: $1"; exit 1 ;;
    esac
done

TOTAL_STAGES=0
PASSED_STAGES=0
FAILED_STAGES=0
STAGE_RESULTS=()

###############################################################################
# Helpers
###############################################################################

run_stage() {
    local name="$1"
    shift
    ((TOTAL_STAGES++))

    echo ""
    echo "================================================================"
    echo " Stage $TOTAL_STAGES: $name"
    echo "================================================================"

    if "$@"; then
        echo " -> PASS: $name"
        ((PASSED_STAGES++))
        STAGE_RESULTS+=("PASS: $name")
    else
        echo " -> FAIL: $name"
        ((FAILED_STAGES++))
        STAGE_RESULTS+=("FAIL: $name")
    fi
}

###############################################################################
# Stage implementations
###############################################################################

stage_unit_tests() {
    echo "Running shared desktop tests..."
    cd "$PROJECT_ROOT"
    ./gradlew :shared:desktopTest 2>&1 | tail -5
    local exit_code=${PIPESTATUS[0]}

    echo "Running Go Challenges tests..."
    cd "$PROJECT_ROOT/Challenges"
    go test ./... -race -count=1 2>&1 | tail -5
    local go_exit=${PIPESTATUS[0]}

    echo "Running Go HelixQA tests..."
    cd "$PROJECT_ROOT/HelixQA"
    go test ./... -race -count=1 2>&1 | tail -5
    local hqa_exit=${PIPESTATUS[0]}

    echo "Running Detekt..."
    cd "$PROJECT_ROOT"
    ./gradlew detekt 2>&1 | tail -3
    local detekt_exit=${PIPESTATUS[0]}

    [[ $exit_code -eq 0 && $go_exit -eq 0 && $hqa_exit -eq 0 && $detekt_exit -eq 0 ]]
}

stage_build_all() {
    cd "$PROJECT_ROOT"

    echo "Building Android APKs..."
    ./gradlew :androidApp:assembleDebug :androidApp:assembleRelease 2>&1 | tail -3
    local android_exit=${PIPESTATUS[0]}

    echo "Copying Android builds to releases/..."
    mkdir -p releases/android
    cp androidApp/build/outputs/apk/debug/androidApp-debug.apk \
        releases/android/Yole-Android-1.0.0-Debug-0.0.0.0.1.apk
    cp androidApp/build/outputs/apk/release/androidApp-release.apk \
        releases/android/Yole-Android-1.0.0-Release-0.0.0.0.1.apk

    echo "Building Desktop JAR..."
    ./gradlew :desktopApp:packageUberJarForCurrentOS 2>&1 | tail -3
    local desktop_exit=${PIPESTATUS[0]}

    mkdir -p releases/desktop
    cp desktopApp/build/compose/jars/Yole-linux-x64-1.0.0.jar \
        releases/desktop/Yole-Desktop-linux-x64-1.0.0-Release-0.0.0.0.1.jar

    echo "Building Web WASM..."
    ./gradlew :webApp:wasmJsJar :webApp:compileKotlinWasmJs 2>&1 | tail -3
    local web_exit=${PIPESTATUS[0]}

    mkdir -p releases/web
    rsync -a webApp/build/processedResources/wasmJs/main/ releases/web/ 2>/dev/null || true

    echo "Building Go tools..."
    cd "$PROJECT_ROOT/HelixQA"
    go build -o "$PROJECT_ROOT/releases/tools/helixqa" ./cmd/helixqa/ 2>&1
    local hqa_build=${PIPESTATUS[0]}

    [[ $android_exit -eq 0 && $desktop_exit -eq 0 && $web_exit -eq 0 && $hqa_build -eq 0 ]]
}

stage_android_automation() {
    cd "$PROJECT_ROOT"
    echo "Installing APK on emulator..."
    adb -s emulator-5554 install -r \
        releases/android/Yole-Android-1.0.0-Debug-0.0.0.0.1.apk 2>&1 || true

    echo "Running Android all-formats automation..."
    bash "$AUTOMATION_DIR/android-all-formats-automation.sh" --device emulator-5554 2>&1 | tail -20
    return ${PIPESTATUS[0]}
}

stage_web_automation() {
    cd "$PROJECT_ROOT"
    echo "Running Web all-formats automation..."
    node "$AUTOMATION_DIR/web-all-formats-automation.mjs" 2>&1 | tail -30
    return ${PIPESTATUS[0]}
}

stage_desktop_automation() {
    cd "$PROJECT_ROOT"
    echo "Running Desktop all-formats automation..."
    bash "$AUTOMATION_DIR/desktop-all-formats-runner.sh" 2>&1 | tail -20
    return ${PIPESTATUS[0]}
}

stage_evidence_validation() {
    cd "$PROJECT_ROOT"
    echo "Running HelixQA evidence validation..."
    bash "$AUTOMATION_DIR/helixqa-validate.sh" --platform all 2>&1
    return ${PIPESTATUS[0]}
}

stage_generate_report() {
    mkdir -p "$QA_RESULTS_DIR"

    cat > "$REPORT_FILE" <<REPORT
# Yole QA Report — $(date -Iseconds)

## Pipeline Summary

| Stage | Result |
|-------|--------|
REPORT

    for result in "${STAGE_RESULTS[@]}"; do
        local status="${result%%:*}"
        local name="${result#*: }"
        if [[ "$status" == "PASS" ]]; then
            echo "| $name | PASS |" >> "$REPORT_FILE"
        else
            echo "| $name | **FAIL** |" >> "$REPORT_FILE"
        fi
    done

    cat >> "$REPORT_FILE" <<REPORT

## Totals

- **Stages:** $TOTAL_STAGES
- **Passed:** $PASSED_STAGES
- **Failed:** $FAILED_STAGES

## Evidence

| Platform | Screenshots | Videos |
|----------|-------------|--------|
REPORT

    local android_screenshots web_screenshots desktop_screenshots
    android_screenshots=$(find recordings/android/formats -name "*.png" 2>/dev/null | wc -l)
    web_screenshots=$(find recordings/web/formats -name "*.png" 2>/dev/null | wc -l)
    desktop_screenshots=$(find recordings/desktop/formats/screenshots -name "*.png" 2>/dev/null; \
        find desktopApp/recordings/desktop/formats/screenshots -name "*.png" 2>/dev/null | wc -l)
    local android_videos web_videos desktop_videos
    android_videos=$(find recordings/android/formats -name "*.mp4" 2>/dev/null | wc -l)
    web_videos=$(find recordings/web/formats -name "*.webm" 2>/dev/null | wc -l)
    desktop_videos=$(find recordings/desktop/formats -name "*.mp4" 2>/dev/null | wc -l)

    echo "| Android | $android_screenshots | $android_videos |" >> "$REPORT_FILE"
    echo "| Web | $web_screenshots | $web_videos |" >> "$REPORT_FILE"
    echo "| Desktop | $desktop_screenshots | $desktop_videos |" >> "$REPORT_FILE"

    local ticket_count
    ticket_count=$(find "$QA_RESULTS_DIR/tickets" -name "*.md" 2>/dev/null | wc -l)
    if [[ "$ticket_count" -gt 0 ]]; then
        echo "" >> "$REPORT_FILE"
        echo "## Tickets ($ticket_count)" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        for ticket in "$QA_RESULTS_DIR/tickets"/*.md; do
            [[ -f "$ticket" ]] || continue
            local ticket_title
            ticket_title=$(head -1 "$ticket" | sed 's/^# //')
            echo "- [$(basename "$ticket")] $ticket_title" >> "$REPORT_FILE"
        done
    fi

    echo "" >> "$REPORT_FILE"
    echo "---" >> "$REPORT_FILE"
    echo "*Generated by HelixQA Pipeline*" >> "$REPORT_FILE"

    echo "Report written to: $REPORT_FILE"
    return 0
}

###############################################################################
# Main pipeline
###############################################################################

echo "============================================================"
echo " Yole Master QA Pipeline"
echo "============================================================"
echo " Project:   $PROJECT_ROOT"
echo " Platform:  $PLATFORM"
echo " Timestamp: $TIMESTAMP"
echo " Results:   $QA_RESULTS_DIR"
echo "============================================================"

# Stage 1: Unit tests
if [[ "$SKIP_UNIT" == "false" ]]; then
    run_stage "Unit Tests + Static Analysis" stage_unit_tests
fi

# Stage 2: Build all platforms
if [[ "$SKIP_BUILD" == "false" ]]; then
    run_stage "Build All Platforms" stage_build_all
fi

# Stage 3-5: Platform automation
if [[ "$PLATFORM" == "all" || "$PLATFORM" == "android" ]]; then
    run_stage "Android Automation (17 formats)" stage_android_automation
fi
if [[ "$PLATFORM" == "all" || "$PLATFORM" == "web" ]]; then
    run_stage "Web Automation (17 formats)" stage_web_automation
fi
if [[ "$PLATFORM" == "all" || "$PLATFORM" == "desktop" ]]; then
    run_stage "Desktop Automation (17 formats)" stage_desktop_automation
fi

# Stage 6: HelixQA evidence validation
run_stage "HelixQA Evidence Validation" stage_evidence_validation

# Stage 7: Generate report
run_stage "Generate QA Report" stage_generate_report

# Final summary
echo ""
echo "============================================================"
echo " Yole QA Pipeline Complete"
echo "============================================================"
echo ""
for result in "${STAGE_RESULTS[@]}"; do
    echo "  $result"
done
echo ""
echo "  Total: $TOTAL_STAGES stages, $PASSED_STAGES passed, $FAILED_STAGES failed"
echo "  Report: $REPORT_FILE"
echo "============================================================"

if [[ "$FAILED_STAGES" -gt 0 ]]; then
    exit 1
fi
exit 0
