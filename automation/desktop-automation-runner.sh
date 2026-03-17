#!/bin/bash
#########################################################
#
# SPDX-FileCopyrightText: 2025 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Desktop UI Automation Runner for Yole
#
# Runs the FullUIAutomationTest at all 3 speed modes with
# optional screen recording via ffmpeg. Collects test output
# and screenshots into recordings/desktop/.
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
RECORD="${2:-false}"    # true to enable screen recording
RESOLUTION="${3:-1920x1080}"

echo "============================================="
echo " Yole Desktop UI Automation Runner"
echo "============================================="
echo " Project:    $PROJECT_DIR"
echo " Speed:      $SPEED"
echo " Recording:  $RECORD"
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

# Check for ffmpeg if recording is requested
HAS_FFMPEG=false
if [[ "$RECORD" == "true" ]]; then
    if command -v "$FFMPEG_BIN" &>/dev/null; then
        HAS_FFMPEG=true
        echo "INFO: ffmpeg found at $FFMPEG_BIN"
    elif command -v ffmpeg &>/dev/null; then
        FFMPEG_BIN="ffmpeg"
        HAS_FFMPEG=true
        echo "INFO: ffmpeg found in PATH"
    else
        echo "WARNING: ffmpeg not found. Screen recording disabled."
        echo "  Checked: $FFMPEG_BIN and PATH"
    fi
fi

# Track overall results
TOTAL_PASS=0
TOTAL_FAIL=0
RESULTS=()

run_speed_test() {
    local speed_label="$1"
    local speed_lower
    speed_lower=$(echo "$speed_label" | tr '[:upper:]' '[:lower:]')
    local output_dir="$RECORDINGS_DIR/$speed_lower"
    local log_file="$output_dir/test-output-${TIMESTAMP}.log"
    local video_file="$output_dir/automation-${TIMESTAMP}.mp4"
    local ffmpeg_pid=""

    echo ""
    echo "---------------------------------------------"
    echo " Running: fullAutomation${speed_label}"
    echo "---------------------------------------------"

    # Start screen recording if enabled
    if [[ "$HAS_FFMPEG" == "true" && "$RECORD" == "true" ]]; then
        echo "INFO: Starting screen recording -> $video_file"
        "$FFMPEG_BIN" -y -f x11grab -video_size "$RESOLUTION" -i "$DISPLAY_TARGET" \
            -framerate 10 -c:v libx264 -preset ultrafast -pix_fmt yuv420p \
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
        fi
    fi

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

    echo "  Log: $log_file"
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

# Exit with failure if any test failed
if [[ $TOTAL_FAIL -gt 0 ]]; then
    exit 1
fi
