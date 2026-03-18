#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Yole Android Tab Navigation Automation Test
#
# Tests comprehensive bottom navigation cycling (Files, To-Do, QuickNote, More)
# with video recording (frame-capture + ffmpeg assembly), per-step screenshots,
# crash detection, and foreground verification after EVERY switch.
#
# Usage:  ./automation/android-navigation-test.sh [--device SERIAL]
#
# Requirements: adb in PATH, Android device/emulator connected, APK installed,
#               ~/bin/ffmpeg available for video assembly.

set -uo pipefail
# Note: we intentionally do NOT use 'set -e' because many ADB commands
# return non-zero exit codes in normal operation.

###############################################################################
# Configuration
###############################################################################

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK_PATH="${PROJECT_ROOT}/releases/android/androidApp-debug.apk"
PACKAGE="digital.vasic.yole.android"
ACTIVITY="${PACKAGE}.MainActivity"
RECORDINGS_ROOT="${PROJECT_ROOT}/recordings/android/navigation"

ADB="${ADB:-adb}"
FFMPEG_BIN="${HOME}/bin/ffmpeg"

# Default device
DEFAULT_DEVICE="19bbb528a1dbbc4d"

# Parse arguments
DEVICE_SERIAL=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --device) DEVICE_SERIAL="$2"; shift 2 ;;
        *)        echo "Unknown arg: $1"; exit 1 ;;
    esac
done

###############################################################################
# Speed: normal (400ms tap delay)
###############################################################################

TAP_DELAY=400
NAV_PAUSE=1000
SETTLE=800
SCREEN_WAIT=1500

###############################################################################
# Screen geometry — resolved at runtime via `wm size`
###############################################################################

SCREEN_W=0
SCREEN_H=0

# Bottom navigation positions (computed by compute_positions)
BOTTOM_NAV_Y=0
BOTTOM_NAV_FILES_X=0
BOTTOM_NAV_TODO_X=0
BOTTOM_NAV_QUICKNOTE_X=0
BOTTOM_NAV_MORE_X=0
CONTENT_CENTER_X=0

compute_positions() {
    local w=$SCREEN_W
    local h=$SCREEN_H

    BOTTOM_NAV_Y=$(( h - h * 4 / 100 ))
    BOTTOM_NAV_FILES_X=$(( w * 1 / 8 ))
    BOTTOM_NAV_TODO_X=$(( w * 3 / 8 ))
    BOTTOM_NAV_QUICKNOTE_X=$(( w * 5 / 8 ))
    BOTTOM_NAV_MORE_X=$(( w * 7 / 8 ))
    CONTENT_CENTER_X=$(( w / 2 ))
}

###############################################################################
# Test result tracking
###############################################################################

TESTS_PASSED=0
TESTS_FAILED=0
TESTS_CRASHED=0

record_result() {
    local step="$1" status="$2"
    echo "{\"step\":\"$step\",\"status\":\"$status\"}" >> "${RECORDINGS_ROOT}/results.jsonl"
    if [ "$status" = "PASS" ]; then
        TESTS_PASSED=$((TESTS_PASSED + 1))
    elif [ "$status" = "CRASH" ]; then
        TESTS_CRASHED=$((TESTS_CRASHED + 1))
    else
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

###############################################################################
# Utility functions
###############################################################################

log() {
    echo "[$(date '+%H:%M:%S')] $*"
}

warn() {
    echo "[$(date '+%H:%M:%S')] WARNING: $*" >&2
}

error() {
    echo "[$(date '+%H:%M:%S')] ERROR: $*" >&2
}

fail() {
    echo "[$(date '+%H:%M:%S')] FATAL: $*" >&2
    exit 1
}

adb_cmd() {
    "$ADB" -s "$DEVICE_SERIAL" "$@"
}

adb_shell() {
    adb_cmd shell "$@"
}

ms_sleep() {
    local ms="$1"
    if (( ms > 0 )); then
        sleep "$(awk "BEGIN{printf \"%.3f\", $ms/1000}")"
    fi
}

tap() {
    local x="$1" y="$2"
    adb_shell input tap "$x" "$y"
    capture_frame
    ms_sleep "$TAP_DELAY"
}

###############################################################################
# Screenshot & Recording helpers
###############################################################################

SCREENSHOT_DIR=""
SCREENSHOT_COUNTER=0
FRAME_DIR=""
FRAME_COUNTER=0
FRAME_CAPTURE_ACTIVE=0

screenshot() {
    local label="$1"
    SCREENSHOT_COUNTER=$(( SCREENSHOT_COUNTER + 1 ))
    local fname
    fname="$(printf '%03d' $SCREENSHOT_COUNTER)-${label}.png"
    local device_path="/sdcard/yole-screenshot.png"
    local local_path="${SCREENSHOT_DIR}/${fname}"

    adb_shell screencap -p "$device_path"
    adb_cmd pull "$device_path" "$local_path" > /dev/null 2>&1
    adb_shell rm -f "$device_path"
    log "    Screenshot: ${fname}"

    # Copy to frame directory for video assembly
    if [[ "$FRAME_CAPTURE_ACTIVE" -eq 1 && -n "$FRAME_DIR" ]]; then
        FRAME_COUNTER=$(( FRAME_COUNTER + 1 ))
        cp "$local_path" "${FRAME_DIR}/$(printf 'frame_%05d.png' $FRAME_COUNTER)"
    fi
}

capture_frame() {
    if [[ "$FRAME_CAPTURE_ACTIVE" -eq 1 && -n "$FRAME_DIR" ]]; then
        FRAME_COUNTER=$(( FRAME_COUNTER + 1 ))
        local frame_fname
        frame_fname="$(printf 'frame_%05d.png' $FRAME_COUNTER)"
        local device_path="/sdcard/yole-frame.png"
        adb_shell screencap -p "$device_path" 2>/dev/null || true
        adb_cmd pull "$device_path" "${FRAME_DIR}/${frame_fname}" > /dev/null 2>&1 || true
        adb_shell rm -f "$device_path" 2>/dev/null || true
    fi
}

start_recording() {
    local video_name="$1"
    FRAME_DIR=$(mktemp -d "/tmp/yole-nav-frames-XXXXXX")
    FRAME_COUNTER=0
    FRAME_CAPTURE_ACTIVE=1
    log "    Recording started (frame capture): ${video_name}"
}

stop_recording() {
    local video_name="$1"
    local output_path="${SCREENSHOT_DIR}/${video_name}.mp4"

    FRAME_CAPTURE_ACTIVE=0

    if [[ -n "$FRAME_DIR" && -d "$FRAME_DIR" ]]; then
        local frame_count
        frame_count=$(find "$FRAME_DIR" -name "frame_*.png" 2>/dev/null | wc -l)

        if [[ "$frame_count" -gt 0 && -x "$FFMPEG_BIN" ]]; then
            "$FFMPEG_BIN" -y -framerate 3 \
                -i "${FRAME_DIR}/frame_%05d.png" \
                -c:v libx264 -pix_fmt yuv420p -crf 23 \
                -vf "scale=trunc(iw/2)*2:trunc(ih/2)*2" \
                "$output_path" \
                > /dev/null 2>&1

            if [[ -s "$output_path" ]]; then
                log "    Recording saved (${frame_count} frames): ${video_name}.mp4"
            else
                warn "ffmpeg failed to create video from ${frame_count} frames"
            fi
        elif [[ "$frame_count" -gt 0 ]]; then
            warn "ffmpeg not found at ${FFMPEG_BIN} — ${frame_count} frames captured but no video"
        else
            warn "No frames captured for ${video_name}"
        fi

        rm -rf "$FRAME_DIR"
        FRAME_DIR=""
    fi
}

###############################################################################
# Device setup
###############################################################################

resolve_device() {
    if [[ -n "$DEVICE_SERIAL" ]]; then
        if ! "$ADB" devices | grep -q "$DEVICE_SERIAL"; then
            fail "Device ${DEVICE_SERIAL} not found"
        fi
        return
    fi
    if "$ADB" devices | grep -q "$DEFAULT_DEVICE"; then
        DEVICE_SERIAL="$DEFAULT_DEVICE"
        return
    fi
    DEVICE_SERIAL=$("$ADB" devices | grep -w 'device$' | head -1 | awk '{print $1}')
    if [[ -z "$DEVICE_SERIAL" ]]; then
        fail "No Android device connected"
    fi
}

get_screen_size() {
    local size
    size=$(adb_shell wm size | grep -oP 'Physical size: \K\d+x\d+' || \
           adb_shell wm size | grep -oP 'Override size: \K\d+x\d+' || \
           adb_shell wm size | tail -1 | grep -oP '\d+x\d+')
    SCREEN_W="${size%x*}"
    SCREEN_H="${size#*x}"
    if [[ -z "$SCREEN_W" || -z "$SCREEN_H" ]]; then
        fail "Could not determine screen size"
    fi
    log "Screen resolution: ${SCREEN_W}x${SCREEN_H}"
}

wake_screen() {
    local screen_state
    screen_state=$(adb_shell dumpsys power | grep "mWakefulness=" | head -1 || true)
    if [[ "$screen_state" != *"Awake"* ]]; then
        adb_shell input keyevent KEYCODE_WAKEUP
        ms_sleep 500
    fi
    adb_shell input keyevent "KEYCODE_MENU" 2>/dev/null || true
    ms_sleep 300
}

launch_app() {
    adb_shell am force-stop "$PACKAGE"
    ms_sleep 1000
    # Dismiss Google overlays
    adb_shell am force-stop com.google.android.gms 2>/dev/null || true
    adb_shell am force-stop com.google.android.gms.ui 2>/dev/null || true
    adb_shell input keyevent KEYCODE_BACK 2>/dev/null || true
    ms_sleep 500
    # Launch with -W to wait for activity
    adb_shell am start -W -n "${PACKAGE}/${ACTIVITY}" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
    ms_sleep 3000
    log "  App launched"
}

###############################################################################
# Crash & foreground verification (run after EVERY navigation)
###############################################################################

verify_foreground() {
    local fg
    fg=$(adb_shell "dumpsys window | grep mCurrentFocus" 2>/dev/null | head -1)
    if echo "$fg" | grep -q "digital.vasic.yole"; then
        return 0
    else
        warn "YOLE NOT IN FOREGROUND: $fg"
        adb_shell input keyevent KEYCODE_BACK 2>/dev/null || true
        ms_sleep 500
        adb_shell am force-stop com.google.android.gms 2>/dev/null || true
        adb_shell am force-stop com.google.android.gms.ui 2>/dev/null || true
        launch_app
        fg=$(adb_shell "dumpsys window | grep mCurrentFocus" 2>/dev/null | head -1)
        if echo "$fg" | grep -q "digital.vasic.yole"; then
            return 0
        else
            error "FATAL: Cannot bring Yole to foreground after recovery. Focus: $fg"
            return 1
        fi
    fi
}

verify_no_crash() {
    local pid
    pid=$(adb_shell "pidof ${PACKAGE}" 2>/dev/null || true)
    if [[ -z "$pid" ]]; then
        error "APP CRASHED! PID not found."
        if [[ -n "$SCREENSHOT_DIR" && -d "$SCREENSHOT_DIR" ]]; then
            adb_shell "logcat -d -s AndroidRuntime:E" > "${SCREENSHOT_DIR}/crash-stacktrace-$(date +%s).txt" 2>/dev/null || true
            adb_shell "logcat -d | grep -i 'digital.vasic.yole'" > "${SCREENSHOT_DIR}/crash-logcat-$(date +%s).txt" 2>/dev/null || true
            screenshot "crash-detected" 2>/dev/null || true
        fi
        return 1
    fi
    return 0
}

verify_app_state() {
    local context="${1:-unknown-step}"
    if ! verify_no_crash; then
        error "Crash detected at: $context"
        record_result "$context" "CRASH"
        return 1
    fi
    if ! verify_foreground; then
        error "Lost foreground at: $context"
        record_result "$context" "FAIL"
        return 1
    fi
    return 0
}

###############################################################################
# Navigation helpers
###############################################################################

nav_to_files() {
    log "    Nav -> Files"
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$SETTLE"
}

nav_to_todo() {
    log "    Nav -> To-Do"
    tap "$BOTTOM_NAV_TODO_X" "$BOTTOM_NAV_Y"
    ms_sleep "$SETTLE"
}

nav_to_quicknote() {
    log "    Nav -> QuickNote"
    tap "$BOTTOM_NAV_QUICKNOTE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$SETTLE"
}

nav_to_more() {
    log "    Nav -> More"
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$SETTLE"
}

###############################################################################
# Navigation + Verify step
###############################################################################

nav_step() {
    local step_name="$1"
    local nav_fn="$2"

    $nav_fn
    screenshot "$step_name"

    if verify_app_state "$step_name"; then
        record_result "$step_name" "PASS"
        log "    $step_name: PASS"
    else
        log "    $step_name: FAIL (crash or lost foreground)"
        # Attempt recovery
        launch_app
        ms_sleep "$SCREEN_WAIT"
    fi
}

###############################################################################
# Main Test Suites
###############################################################################

test_forward_navigation() {
    log "=== Phase 1: Forward Navigation (Files -> To-Do -> QuickNote -> More) ==="
    nav_step "forward-01-files" nav_to_files
    nav_step "forward-02-todo" nav_to_todo
    nav_step "forward-03-quicknote" nav_to_quicknote
    nav_step "forward-04-more" nav_to_more
}

test_backward_navigation() {
    log "=== Phase 2: Backward Navigation (More -> QuickNote -> To-Do -> Files) ==="
    nav_step "backward-01-more" nav_to_more
    nav_step "backward-02-quicknote" nav_to_quicknote
    nav_step "backward-03-todo" nav_to_todo
    nav_step "backward-04-files" nav_to_files
}

test_random_jumps() {
    log "=== Phase 3: Random Jumps ==="
    nav_step "random-01-more" nav_to_more
    nav_step "random-02-files" nav_to_files
    nav_step "random-03-quicknote" nav_to_quicknote
    nav_step "random-04-todo" nav_to_todo
    nav_step "random-05-more" nav_to_more
    nav_step "random-06-todo" nav_to_todo
    nav_step "random-07-files" nav_to_files
    nav_step "random-08-quicknote" nav_to_quicknote
    nav_step "random-09-more" nav_to_more
    nav_step "random-10-files" nav_to_files
}

test_rapid_switching() {
    log "=== Phase 4: Rapid Switching (20 tab changes) ==="

    local tabs=("nav_to_files" "nav_to_todo" "nav_to_quicknote" "nav_to_more")
    local tab_names=("files" "todo" "quicknote" "more")

    for i in $(seq 1 20); do
        local idx=$(( (i - 1) % 4 ))
        local fn="${tabs[$idx]}"
        local name="${tab_names[$idx]}"
        local step_label
        step_label="$(printf 'rapid-%02d-%s' "$i" "$name")"

        nav_step "$step_label" "$fn"
    done
}

test_alternating_pairs() {
    log "=== Phase 5: Alternating Pairs (Files <-> More) x10 ==="
    for i in $(seq 1 10); do
        nav_step "alt-files-more-$(printf '%02d' $((i * 2 - 1)))-files" nav_to_files
        nav_step "alt-files-more-$(printf '%02d' $((i * 2)))-more" nav_to_more
    done

    log "=== Phase 5b: Alternating Pairs (To-Do <-> QuickNote) x10 ==="
    for i in $(seq 1 10); do
        nav_step "alt-todo-qn-$(printf '%02d' $((i * 2 - 1)))-todo" nav_to_todo
        nav_step "alt-todo-qn-$(printf '%02d' $((i * 2)))-quicknote" nav_to_quicknote
    done
}

###############################################################################
# Main
###############################################################################

main() {
    log "============================================"
    log "  Yole Android Tab Navigation Test"
    log "============================================"

    resolve_device
    log "Device: ${DEVICE_SERIAL}"

    get_screen_size
    compute_positions

    log "Bottom nav Y: ${BOTTOM_NAV_Y}"
    log "Bottom nav X positions: Files=${BOTTOM_NAV_FILES_X} Todo=${BOTTOM_NAV_TODO_X} QuickNote=${BOTTOM_NAV_QUICKNOTE_X} More=${BOTTOM_NAV_MORE_X}"

    # Setup output directory
    SCREENSHOT_DIR="${RECORDINGS_ROOT}/$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$SCREENSHOT_DIR"
    SCREENSHOT_COUNTER=0

    # Clear old results
    rm -f "${RECORDINGS_ROOT}/results.jsonl"

    # Wake screen and launch app
    wake_screen
    launch_app

    # Initial state screenshot
    ms_sleep "$SCREEN_WAIT"
    screenshot "00-initial-state"
    verify_app_state "initial-launch" || fail "App failed to launch"

    # Start video recording
    start_recording "tab-navigation"

    # Run all test phases
    test_forward_navigation
    test_backward_navigation
    test_random_jumps
    test_rapid_switching
    test_alternating_pairs

    # Stop recording
    stop_recording "tab-navigation"

    # Final state
    screenshot "99-final-state"
    verify_app_state "final-state"

    # Report
    log ""
    log "============================================"
    log "  Navigation Test Results"
    log "============================================"
    log "  Passed:   ${TESTS_PASSED}"
    log "  Failed:   ${TESTS_FAILED}"
    log "  Crashed:  ${TESTS_CRASHED}"
    log "  Total:    $(( TESTS_PASSED + TESTS_FAILED + TESTS_CRASHED ))"
    log "  Dir:      ${SCREENSHOT_DIR}"
    log "============================================"

    if [[ "$TESTS_CRASHED" -gt 0 ]]; then
        error "CRASHES DETECTED — see ${SCREENSHOT_DIR} for evidence"
        exit 2
    fi
    if [[ "$TESTS_FAILED" -gt 0 ]]; then
        error "FAILURES DETECTED — see ${SCREENSHOT_DIR} for screenshots"
        exit 1
    fi

    log "ALL NAVIGATION TESTS PASSED"
    exit 0
}

main
