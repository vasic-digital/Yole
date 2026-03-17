#!/bin/bash
#########################################################
#
# SPDX-FileCopyrightText: 2025 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Desktop All-Formats UI Automation Runner for Yole
#
# Runs the AllFormatsAutomationTest with screen recording
# via ffmpeg. Captures video of the entire test run and
# takes before/after screenshots.
#
# Usage:
#   ./automation/desktop-all-formats-runner.sh [test-filter]
#
# Examples:
#   ./automation/desktop-all-formats-runner.sh              # Run all tests
#   ./automation/desktop-all-formats-runner.sh testMarkdown  # Single test
#
#########################################################

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RECORDINGS_DIR="$PROJECT_DIR/recordings/desktop/formats"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
FFMPEG_BIN="${HOME}/bin/ffmpeg"
DISPLAY_TARGET="${DISPLAY:-:0}"
RESOLUTION="${RESOLUTION:-1920x1080}"
TEST_FILTER="${1:-}"
TEST_CLASS="digital.vasic.yole.desktop.AllFormatsAutomationTest"

echo "============================================="
echo " Yole All-Formats UI Automation Runner"
echo "============================================="
echo " Project:    $PROJECT_DIR"
echo " Test class: $TEST_CLASS"
echo " Filter:     ${TEST_FILTER:-all tests}"
echo " Recording:  $RECORDINGS_DIR"
echo " Resolution: $RESOLUTION"
echo " Timestamp:  $TIMESTAMP"
echo "============================================="

# Create output directory
mkdir -p "$RECORDINGS_DIR"

# Resolve ffmpeg binary
HAS_FFMPEG=false
if [[ -x "$FFMPEG_BIN" ]] || command -v "$FFMPEG_BIN" &>/dev/null; then
    HAS_FFMPEG=true
    echo "INFO: ffmpeg found at $FFMPEG_BIN"
elif command -v ffmpeg &>/dev/null; then
    FFMPEG_BIN="ffmpeg"
    HAS_FFMPEG=true
    echo "INFO: ffmpeg found in PATH"
else
    echo "WARNING: ffmpeg not found. Screen recording and screenshots disabled."
    echo "  Checked: $FFMPEG_BIN and PATH"
fi

# File paths
LOG_FILE="$RECORDINGS_DIR/test-output-${TIMESTAMP}.log"
VIDEO_FILE="$RECORDINGS_DIR/all-formats-test-${TIMESTAMP}.mp4"
SCREENSHOT_BEFORE="$RECORDINGS_DIR/before-${TIMESTAMP}.png"
SCREENSHOT_AFTER="$RECORDINGS_DIR/after-${TIMESTAMP}.png"

# Take a screenshot using ffmpeg x11grab (single frame)
take_screenshot() {
    local output_file="$1"
    if [[ "$HAS_FFMPEG" == "true" ]]; then
        "$FFMPEG_BIN" -y -f x11grab -video_size "$RESOLUTION" -i "$DISPLAY_TARGET" \
            -frames:v 1 -update 1 \
            "$output_file" </dev/null >/dev/null 2>&1 || true
        if [[ -f "$output_file" ]]; then
            echo "INFO: Screenshot saved: $output_file"
        else
            echo "WARNING: Screenshot failed: $output_file"
        fi
    fi
}

# Take screenshot BEFORE test run
echo ""
echo "INFO: Taking pre-test screenshot..."
take_screenshot "$SCREENSHOT_BEFORE"

# Start screen recording
FFMPEG_PID=""
if [[ "$HAS_FFMPEG" == "true" ]]; then
    echo "INFO: Starting screen recording -> $VIDEO_FILE"
    "$FFMPEG_BIN" -y -f x11grab -video_size "$RESOLUTION" -framerate 10 -i "$DISPLAY_TARGET" \
        -c:v libx264 -preset ultrafast -pix_fmt yuv420p \
        "$VIDEO_FILE" </dev/null >/dev/null 2>&1 &
    FFMPEG_PID=$!
    echo "INFO: ffmpeg PID=$FFMPEG_PID"
    sleep 1
fi

# Build the Gradle test command
TEST_START=$(date +%s)
EXIT_CODE=0

if [[ -n "$TEST_FILTER" ]]; then
    TEST_TARGET="$TEST_CLASS.$TEST_FILTER"
else
    TEST_TARGET="$TEST_CLASS"
fi

echo ""
echo "---------------------------------------------"
echo " Running: $TEST_TARGET"
echo "---------------------------------------------"

cd "$PROJECT_DIR"
./gradlew :desktopApp:test \
    --tests "$TEST_TARGET" \
    --no-daemon \
    2>&1 | tee "$LOG_FILE" || EXIT_CODE=$?

TEST_END=$(date +%s)
DURATION=$((TEST_END - TEST_START))

# Stop screen recording
if [[ -n "$FFMPEG_PID" ]]; then
    echo ""
    echo "INFO: Stopping screen recording (PID=$FFMPEG_PID)"
    kill "$FFMPEG_PID" 2>/dev/null || true
    wait "$FFMPEG_PID" 2>/dev/null || true
    if [[ -f "$VIDEO_FILE" ]]; then
        VIDEO_SIZE=$(du -h "$VIDEO_FILE" | cut -f1)
        echo "INFO: Recording saved: $VIDEO_FILE ($VIDEO_SIZE)"
    else
        echo "WARNING: Recording file not found: $VIDEO_FILE"
    fi
fi

# Take screenshot AFTER test run
echo "INFO: Taking post-test screenshot..."
take_screenshot "$SCREENSHOT_AFTER"

# Summary
echo ""
echo "============================================="
echo " All-Formats Automation Summary"
echo "============================================="
if [[ $EXIT_CODE -eq 0 ]]; then
    echo " Result:   PASS"
else
    echo " Result:   FAIL (exit=$EXIT_CODE)"
fi
echo " Duration: ${DURATION}s"
echo " Log:      $LOG_FILE"
echo " Video:    $VIDEO_FILE"
echo " Before:   $SCREENSHOT_BEFORE"
echo " After:    $SCREENSHOT_AFTER"
echo "============================================="

# List all artifacts
echo ""
echo " Artifacts in $RECORDINGS_DIR:"
ls -lh "$RECORDINGS_DIR/"*"${TIMESTAMP}"* 2>/dev/null | while read -r line; do
    echo "   $line"
done
echo "============================================="

exit $EXIT_CODE
