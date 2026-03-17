#!/bin/bash
#########################################################
#
# SPDX-FileCopyrightText: 2025 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Desktop UI Automation Runner for Yole
#
# Runs the FullUIAutomationTest at all 3 speed modes with
# screen recording via ffmpeg. Captures video of the entire
# test run and takes screenshots before/after each speed.
#
#########################################################

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RECORDINGS_DIR="$PROJECT_DIR/recordings/desktop"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
FFMPEG_BIN="${HOME}/bin/ffmpeg"
DISPLAY_TARGET="${DISPLAY:-:0}"

# Parse arguments
SPEED="${1:-all}"       # slow, normal, fast, or all
RESOLUTION="${2:-1920x1080}"

echo "============================================="
echo " Yole Desktop UI Automation Runner"
echo "============================================="
echo " Project:    $PROJECT_DIR"
echo " Speed:      $SPEED"
echo " Recording:  always (ffmpeg)"
echo " Resolution: $RESOLUTION"
echo " Timestamp:  $TIMESTAMP"
echo "============================================="

# Determine which speed modes to run
case "$SPEED" in
    slow)   SPEEDS=("Slow") ;;
    normal) SPEEDS=("Normal") ;;
    fast)   SPEEDS=("Fast") ;;
    all)    SPEEDS=("Slow" "Normal" "Fast") ;;
    *)
        echo "ERROR: Unknown speed mode '$SPEED'. Use: slow, normal, fast, or all"
        exit 1
        ;;
esac

# Create output directories
for speed_label in "${SPEEDS[@]}"; do
    speed_lower=$(echo "$speed_label" | tr '[:upper:]' '[:lower:]')
    mkdir -p "$RECORDINGS_DIR/$speed_lower"
done

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

# Track overall results
TOTAL_PASS=0
TOTAL_FAIL=0
RESULTS=()

# Take a screenshot using ffmpeg's x11grab (single frame)
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

run_speed_test() {
    local speed_label="$1"
    local speed_lower
    speed_lower=$(echo "$speed_label" | tr '[:upper:]' '[:lower:]')
    local output_dir="$RECORDINGS_DIR/$speed_lower"
    local log_file="$output_dir/test-output-${TIMESTAMP}.log"
    local video_file="$output_dir/automation-${TIMESTAMP}.mp4"
    local screenshot_before="$output_dir/before-${TIMESTAMP}.png"
    local screenshot_after="$output_dir/after-${TIMESTAMP}.png"
    local ffmpeg_pid=""

    echo ""
    echo "---------------------------------------------"
    echo " Running: fullAutomation${speed_label}"
    echo "---------------------------------------------"

    # Take screenshot BEFORE test run
    echo "INFO: Taking pre-test screenshot..."
    take_screenshot "$screenshot_before"

    # Start screen recording
    if [[ "$HAS_FFMPEG" == "true" ]]; then
        echo "INFO: Starting screen recording -> $video_file"
        "$FFMPEG_BIN" -y -f x11grab -video_size "$RESOLUTION" -framerate 10 -i "$DISPLAY_TARGET" \
            -c:v libx264 -preset ultrafast -pix_fmt yuv420p \
            "$video_file" </dev/null >/dev/null 2>&1 &
        ffmpeg_pid=$!
        echo "INFO: ffmpeg PID=$ffmpeg_pid"
        sleep 1
    fi

    # Run the Gradle test
    local test_start
    test_start=$(date +%s)
    local exit_code=0

    cd "$PROJECT_DIR"
    ./gradlew :desktopApp:test \
        --tests "digital.vasic.yole.desktop.FullUIAutomationTest.fullAutomation${speed_label}" \
        --no-daemon \
        2>&1 | tee "$log_file" || exit_code=$?

    local test_end
    test_end=$(date +%s)
    local duration=$((test_end - test_start))

    # Stop screen recording
    if [[ -n "$ffmpeg_pid" ]]; then
        echo "INFO: Stopping screen recording (PID=$ffmpeg_pid)"
        kill "$ffmpeg_pid" 2>/dev/null || true
        wait "$ffmpeg_pid" 2>/dev/null || true
        if [[ -f "$video_file" ]]; then
            local video_size
            video_size=$(du -h "$video_file" | cut -f1)
            echo "INFO: Recording saved: $video_file ($video_size)"
        else
            echo "WARNING: Recording file not found: $video_file"
        fi
    fi

    # Take screenshot AFTER test run
    echo "INFO: Taking post-test screenshot..."
    take_screenshot "$screenshot_after"

    # Report result
    if [[ $exit_code -eq 0 ]]; then
        echo "PASS: fullAutomation${speed_label} (${duration}s)"
        RESULTS+=("PASS: fullAutomation${speed_label} (${duration}s)")
        TOTAL_PASS=$((TOTAL_PASS + 1))
    else
        echo "FAIL: fullAutomation${speed_label} (${duration}s, exit=$exit_code)"
        RESULTS+=("FAIL: fullAutomation${speed_label} (${duration}s, exit=$exit_code)")
        TOTAL_FAIL=$((TOTAL_FAIL + 1))
    fi

    echo "  Log:              $log_file"
    echo "  Video:            $video_file"
    echo "  Screenshot before: $screenshot_before"
    echo "  Screenshot after:  $screenshot_after"
}

# Run tests for each speed mode
for speed_label in "${SPEEDS[@]}"; do
    run_speed_test "$speed_label"
done

# Summary
echo ""
echo "============================================="
echo " Automation Summary"
echo "============================================="
for result in "${RESULTS[@]}"; do
    echo "  $result"
done
echo "---------------------------------------------"
echo " Total: $((TOTAL_PASS + TOTAL_FAIL)) | Pass: $TOTAL_PASS | Fail: $TOTAL_FAIL"
echo " Output: $RECORDINGS_DIR"
echo "============================================="

# List all produced artifacts
echo ""
echo " Artifacts:"
for speed_label in "${SPEEDS[@]}"; do
    speed_lower=$(echo "$speed_label" | tr '[:upper:]' '[:lower:]')
    echo "  $speed_lower/:"
    ls -lh "$RECORDINGS_DIR/$speed_lower/"*"${TIMESTAMP}"* 2>/dev/null | while read -r line; do
        echo "    $line"
    done
done
echo "============================================="

# Exit with failure if any test failed
if [[ $TOTAL_FAIL -gt 0 ]]; then
    exit 1
fi
