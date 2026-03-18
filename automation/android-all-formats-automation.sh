#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Yole Android All-Formats Automation — Tests ALL 17 text formats with ALL operations
# on a real physical device with video recording (frame-capture + ffmpeg assembly)
# and per-step screenshots.
#
# Usage:  ./automation/android-all-formats-automation.sh [--device SERIAL]
#
# Requirements: adb in PATH, physical Android device connected, APK installed,
#               ~/bin/ffmpeg available for video assembly.

set -uo pipefail
# Note: we intentionally do NOT use 'set -e' because many ADB commands
# return non-zero exit codes in normal operation (e.g., pidof when process
# is not running, pm clear after force-stop, screencap display warnings).

###############################################################################
# Configuration
###############################################################################

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK_PATH="${PROJECT_ROOT}/releases/android/androidApp-debug.apk"
PACKAGE="digital.vasic.yole.android"
ACTIVITY="${PACKAGE}.MainActivity"
RECORDINGS_ROOT="${PROJECT_ROOT}/recordings/android/formats"

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
# Speed: normal only (400ms tap delay, 50ms type delay)
###############################################################################

TAP_DELAY=400
TYPE_DELAY=50
NAV_PAUSE=1000
SWIPE_DUR=300
SETTLE=800
SCREEN_WAIT=1500

###############################################################################
# Screen geometry — resolved at runtime via `wm size`
###############################################################################

SCREEN_W=0
SCREEN_H=0

# Computed UI element positions (set by compute_positions)
TOPBAR_Y=0
BOTTOM_NAV_Y=0
BOTTOM_NAV_FILES_X=0
BOTTOM_NAV_TODO_X=0
BOTTOM_NAV_QUICKNOTE_X=0
BOTTOM_NAV_MORE_X=0
TOPBAR_ACTION1_X=0
TOPBAR_ACTION2_X=0
TOPBAR_ACTION3_X=0
TOPBAR_NAV_BACK_X=0
CONTENT_CENTER_X=0
CONTENT_TOP_Y=0
CONTENT_MID_Y=0
FAB_X=0
FAB_Y=0
EDITOR_FIELD_X=0
EDITOR_FIELD_Y=0
FILE_ITEM1_X=0
FILE_ITEM1_Y=0
FILE_ITEM2_Y=0
FILE_ITEM3_Y=0
MORE_CARD1_Y=0
MORE_CARD2_Y=0
SETTINGS_RADIO_X=0
SETTINGS_RADIO_SYSTEM_Y=0
SETTINGS_RADIO_LIGHT_Y=0
SETTINGS_RADIO_DARK_Y=0

# New Document Dialog positions
DIALOG_FORMAT_X=0
DIALOG_FORMAT_START_Y=0
DIALOG_FORMAT_ROW_H=0
DIALOG_CREATE_BTN_X=0
DIALOG_CREATE_BTN_Y=0

compute_positions() {
    local w=$SCREEN_W
    local h=$SCREEN_H

    TOPBAR_Y=$(( h * 7 / 100 ))
    # Bottom nav icons are at ~91.5% of screen height (verified via uiautomator)
    BOTTOM_NAV_Y=$(( h * 915 / 1000 ))
    BOTTOM_NAV_FILES_X=$(( w * 1 / 8 ))
    BOTTOM_NAV_TODO_X=$(( w * 3 / 8 ))
    BOTTOM_NAV_QUICKNOTE_X=$(( w * 5 / 8 ))
    BOTTOM_NAV_MORE_X=$(( w * 7 / 8 ))

    local icon_spacing=$(( w * 12 / 100 ))
    TOPBAR_ACTION1_X=$(( w - icon_spacing / 2 ))
    TOPBAR_ACTION2_X=$(( w - icon_spacing - icon_spacing / 2 ))
    TOPBAR_ACTION3_X=$(( w - 2 * icon_spacing - icon_spacing / 2 ))
    TOPBAR_NAV_BACK_X=$(( icon_spacing / 2 ))

    CONTENT_CENTER_X=$(( w / 2 ))
    CONTENT_TOP_Y=$(( h * 18 / 100 ))
    CONTENT_MID_Y=$(( h / 2 ))

    FAB_X=$(( w - w * 10 / 100 ))
    FAB_Y=$(( h - h * 14 / 100 ))

    EDITOR_FIELD_X=$(( w / 2 ))
    EDITOR_FIELD_Y=$(( h * 45 / 100 ))

    FILE_ITEM1_X=$(( w / 2 ))
    FILE_ITEM1_Y=$(( h * 32 / 100 ))
    FILE_ITEM2_Y=$(( h * 38 / 100 ))
    FILE_ITEM3_Y=$(( h * 44 / 100 ))

    local card_start=$(( h * 18 / 100 ))
    local card_h=$(( h * 10 / 100 ))
    MORE_CARD1_Y=$(( card_start ))
    MORE_CARD2_Y=$(( card_start + card_h ))

    # Settings theme radio buttons
    SETTINGS_RADIO_X=$(( w * 6 / 100 ))
    local settings_start=$(( h * 25 / 100 ))
    local row_h=$(( h * 5 / 100 ))
    SETTINGS_RADIO_SYSTEM_Y=$(( settings_start ))
    SETTINGS_RADIO_LIGHT_Y=$(( settings_start + row_h ))
    SETTINGS_RADIO_DARK_Y=$(( settings_start + 2 * row_h ))

    # New Document Dialog — AlertDialog is centered on screen
    # Radio list starts about 35% from top, each row ~48dp (~7% of height)
    DIALOG_FORMAT_X=$(( w * 30 / 100 ))
    DIALOG_FORMAT_START_Y=$(( h * 35 / 100 ))
    DIALOG_FORMAT_ROW_H=$(( h * 5 / 100 ))
    # "Create" button is bottom-right of dialog
    DIALOG_CREATE_BTN_X=$(( w * 72 / 100 ))
    DIALOG_CREATE_BTN_Y=$(( h * 68 / 100 ))
}

###############################################################################
# Test result tracking (Fix 7)
###############################################################################

TESTS_PASSED=0
TESTS_FAILED=0
TESTS_CRASHED=0
EVIDENCE_DIR=""

record_result() {
    local format="$1" step="$2" status="$3" evidence="${4:-}"
    echo "{\"format\":\"$format\",\"step\":\"$step\",\"status\":\"$status\",\"evidence\":\"$evidence\"}" >> "${RECORDINGS_ROOT}/results.jsonl"
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

long_press() {
    local x="$1" y="$2" dur="${3:-800}"
    adb_shell input swipe "$x" "$y" "$x" "$y" "$dur"
    ms_sleep "$TAP_DELAY"
}

swipe() {
    local x1="$1" y1="$2" x2="$3" y2="$4"
    adb_shell input swipe "$x1" "$y1" "$x2" "$y2" "$SWIPE_DUR"
    capture_frame
    ms_sleep "$TAP_DELAY"
}

swipe_up() {
    swipe "$CONTENT_CENTER_X" "$(( SCREEN_H * 70 / 100 ))" "$CONTENT_CENTER_X" "$(( SCREEN_H * 30 / 100 ))"
}

swipe_down() {
    swipe "$CONTENT_CENTER_X" "$(( SCREEN_H * 30 / 100 ))" "$CONTENT_CENTER_X" "$(( SCREEN_H * 70 / 100 ))"
}

type_text() {
    # ADB input text: spaces are %s
    local text="$1"
    local escaped="${text// /%s}"
    adb_shell input text "'${escaped}'"
    ms_sleep "$TYPE_DELAY"
}

press_key() {
    local keycode="$1"
    adb_shell input keyevent "$keycode"
    ms_sleep "$TAP_DELAY"
}

press_back() {
    press_key "KEYCODE_BACK"
}

press_enter() {
    press_key "KEYCODE_ENTER"
}

clear_field() {
    # Triple-tap to select all, then delete
    local x="$1" y="$2"
    tap "$x" "$y"
    ms_sleep 200
    tap "$x" "$y"
    ms_sleep 100
    tap "$x" "$y"
    ms_sleep 200
    # Ctrl+A
    adb_shell "input keyevent 29 --meta 4096" 2>/dev/null || true
    ms_sleep 200
    long_press "$x" "$y" 1200
    ms_sleep 500
    press_key "KEYCODE_DEL"
    ms_sleep 200
    for _ in {1..30}; do
        adb_shell input keyevent "KEYCODE_DEL"
    done
    ms_sleep 200
}

# Push content to device file and then input via clipboard
push_content_to_editor() {
    local content="$1"
    # Write content to device temp file
    echo "$content" | adb_cmd shell "cat > /sdcard/yole-test-input.txt"
    ms_sleep 200
    # Use am broadcast to set clipboard (Android 10+ restriction may apply)
    # Fallback: type line by line
    local IFS=$'\n'
    local first=1
    for line in $content; do
        if [[ "$first" -eq 1 ]]; then
            first=0
        else
            press_enter
        fi
        if [[ -n "$line" ]]; then
            type_text "$line"
        fi
    done
}

###############################################################################
# Screenshot & Recording helpers
###############################################################################

SCREENSHOT_DIR=""
SCREENSHOT_COUNTER=0
FRAME_DIR=""
FRAME_COUNTER=0
FRAME_CAPTURE_ACTIVE=0

init_format_dir() {
    local dir_name="$1"
    SCREENSHOT_DIR="${RECORDINGS_ROOT}/${dir_name}"
    mkdir -p "$SCREENSHOT_DIR"
    SCREENSHOT_COUNTER=0
}

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
    FRAME_DIR=$(mktemp -d "/tmp/yole-frames-XXXXXX")
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
        press_key "KEYCODE_WAKEUP"
        ms_sleep 500
    fi
    adb_shell input keyevent "KEYCODE_MENU" 2>/dev/null || true
    ms_sleep 300
}

launch_app() {
    adb_shell am force-stop "$PACKAGE"
    ms_sleep 1000
    # Dismiss any Google overlays that may steal focus
    adb_shell am force-stop com.google.android.gms 2>/dev/null || true
    adb_shell am force-stop com.google.android.gms.ui 2>/dev/null || true
    adb_shell input keyevent KEYCODE_BACK 2>/dev/null || true
    ms_sleep 500
    # Launch with -W to wait for activity to fully display
    adb_shell am start -W -n "${PACKAGE}/${ACTIVITY}" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
    ms_sleep 3000  # Additional settle time for Compose initialization
    log "  App launched"
}

ensure_app_running() {
    local running
    running=$(adb_shell "pidof ${PACKAGE}" 2>/dev/null || true)
    if [[ -z "$running" ]]; then
        warn "App not running — relaunching"
        launch_app
    fi
}

# Verify Yole is the foreground app; recover if not (Fix 2)
verify_foreground() {
    local fg
    fg=$(adb_shell "dumpsys window | grep mCurrentFocus" 2>/dev/null | head -1)
    if echo "$fg" | grep -q "digital.vasic.yole"; then
        return 0  # Yole is in foreground
    else
        warn "YOLE NOT IN FOREGROUND: $fg"
        # Try to recover: dismiss overlays and relaunch
        adb_shell input keyevent KEYCODE_BACK 2>/dev/null || true
        ms_sleep 500
        adb_shell am force-stop com.google.android.gms 2>/dev/null || true
        adb_shell am force-stop com.google.android.gms.ui 2>/dev/null || true
        launch_app
        # Check again
        fg=$(adb_shell "dumpsys window | grep mCurrentFocus" 2>/dev/null | head -1)
        if echo "$fg" | grep -q "digital.vasic.yole"; then
            return 0
        else
            error "FATAL: Cannot bring Yole to foreground after recovery. Focus: $fg"
            return 1
        fi
    fi
}

# Verify the app process has not crashed (Fix 3)
verify_no_crash() {
    local pid
    pid=$(adb_shell "pidof ${PACKAGE}" 2>/dev/null || true)
    if [[ -z "$pid" ]]; then
        error "APP CRASHED! PID not found."
        # Collect crash evidence
        if [[ -n "$EVIDENCE_DIR" && -d "$EVIDENCE_DIR" ]]; then
            adb_shell "logcat -d -s AndroidRuntime:E" > "${EVIDENCE_DIR}/crash-stacktrace-$(date +%s).txt" 2>/dev/null || true
            adb_shell "logcat -d | grep -i 'digital.vasic.yole'" > "${EVIDENCE_DIR}/crash-logcat-$(date +%s).txt" 2>/dev/null || true
            screenshot "crash-detected" 2>/dev/null || true
        fi
        return 1
    fi
    return 0
}

# Combined check: verify app alive and in foreground (Fix 4)
verify_app_state() {
    local context="${1:-unknown-step}"
    if ! verify_no_crash; then
        error "Crash detected at: $context"
        return 1
    fi
    if ! verify_foreground; then
        error "Lost foreground at: $context"
        return 1
    fi
    return 0
}

# Setup emulator for clean automation (Fix 5)
setup_emulator() {
    log "Setting up emulator for automation..."
    # Dismiss Google setup wizard / MinuteMaidActivity
    adb_shell am force-stop com.google.android.gms 2>/dev/null || true
    adb_shell am force-stop com.google.android.gms.ui 2>/dev/null || true
    adb_shell pm disable-user --user 0 com.google.android.gms/.auth.uiflows.minutemaid.MinuteMaidActivity 2>/dev/null || true
    # Dismiss any dialogs
    adb_shell input keyevent KEYCODE_HOME
    ms_sleep 1000
    # Set screen always-on for testing
    adb_shell svc power stayon true 2>/dev/null || true
    adb_shell settings put system screen_off_timeout 1800000 2>/dev/null || true
    # Clear logcat for clean crash detection
    adb_shell logcat -c 2>/dev/null || true
    log "  Emulator setup complete"
}

# Navigate to Files tab from any state
go_to_files() {
    # If in a sub-screen, press back first
    press_back
    ms_sleep "$NAV_PAUSE"
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
}

# Create a new document via the New Document dialog.
# format_index: 0=markdown, 1=plaintext, 2=todotxt, 3=csv, 4=latex, 5=orgmode
create_new_doc_dialog() {
    local format_index="$1"

    # Tap FAB to open New Document dialog
    tap "$FAB_X" "$FAB_Y"
    ms_sleep "$SETTLE"

    # Tap the format radio button at the given index
    local format_y=$(( DIALOG_FORMAT_START_Y + format_index * DIALOG_FORMAT_ROW_H ))
    tap "$DIALOG_FORMAT_X" "$format_y"
    ms_sleep 300

    # Tap Create button
    tap "$DIALOG_CREATE_BTN_X" "$DIALOG_CREATE_BTN_Y"
    ms_sleep "$NAV_PAUSE"
}

# Push a file to the device's app Documents directory and refresh file list
push_format_file() {
    local filename="$1"
    local content="$2"
    # The app reads from /sdcard/Android/data/digital.vasic.yole.android/Documents/
    local device_dir="/sdcard/Android/data/${PACKAGE}/Documents"
    adb_shell "mkdir -p '${device_dir}'"
    echo "$content" | adb_cmd shell "cat > '${device_dir}/${filename}'"
    ms_sleep 300
    log "    Pushed ${filename} to device"
}

# Open file from file list by tapping on the first item
# (after pushing, we re-launch app or navigate to Files to see it)
open_first_file() {
    tap "$FILE_ITEM1_X" "$FILE_ITEM1_Y"
    ms_sleep "$NAV_PAUSE"
}

open_second_file() {
    tap "$FILE_ITEM1_X" "$FILE_ITEM2_Y"
    ms_sleep "$NAV_PAUSE"
}

open_third_file() {
    tap "$FILE_ITEM1_X" "$FILE_ITEM3_Y"
    ms_sleep "$NAV_PAUSE"
}

###############################################################################
# Per-format test function
# Arguments: dir_name, format_label, create_method, content_lines...
#
# create_method:
#   "dialog:N"    — use New Document dialog, format index N
#   "file:name"   — push file to device, open from file browser
###############################################################################

test_format() {
    local dir_name="$1"
    local format_label="$2"
    local create_method="$3"
    shift 3
    # Remaining args are content lines to type (if dialog) or full content (if file)
    local content="$*"
    local format_failed=0

    log ""
    log "  === ${format_label} ==="
    init_format_dir "$dir_name"
    EVIDENCE_DIR="$SCREENSHOT_DIR"
    start_recording "recording"

    # Ensure app is running and in foreground before starting (Fix 4)
    ensure_app_running
    if ! verify_app_state "pre-${format_label}"; then
        # Try one more relaunch
        launch_app
        if ! verify_app_state "pre-${format_label}-retry"; then
            error "Cannot start test for ${format_label} — app not available"
            record_result "$format_label" "launch" "CRASH" ""
            stop_recording "recording"
            return
        fi
    fi

    if [[ "$create_method" == dialog:* ]]; then
        local idx="${create_method#dialog:}"

        # Navigate to Files tab (Fix 6: navigate, don't force-stop)
        go_to_files
        verify_app_state "go_to_files-${format_label}" || { record_result "$format_label" "navigate" "CRASH" ""; stop_recording "recording"; launch_app; return; }
        screenshot "01-files-before-create"

        # Create via dialog
        create_new_doc_dialog "$idx"
        verify_app_state "create_dialog-${format_label}" || { record_result "$format_label" "create-dialog" "CRASH" ""; stop_recording "recording"; launch_app; return; }
        screenshot "02-editor-opened"

        # The editor is now open with template content
        # Tap editor field to focus
        tap "$EDITOR_FIELD_X" "$EDITOR_FIELD_Y"
        ms_sleep "$SETTLE"

        # Clear template and type our content
        clear_field "$EDITOR_FIELD_X" "$EDITOR_FIELD_Y"
        ms_sleep 300
        verify_app_state "clear_field-${format_label}" || { record_result "$format_label" "clear-field" "CRASH" ""; stop_recording "recording"; launch_app; return; }

        # Type content line by line
        local IFS=$'\n'
        local first=1
        for line in $content; do
            if [[ "$first" -eq 1 ]]; then
                first=0
            else
                press_enter
            fi
            if [[ -n "$line" ]]; then
                type_text "$line"
            fi
        done

    elif [[ "$create_method" == file:* ]]; then
        local filename="${create_method#file:}"

        # Push file to device
        push_format_file "$filename" "$content"

        # (Fix 6) Navigate to Files tab to pick up the new file instead of force-stop
        go_to_files
        # Swipe down to refresh the file list
        swipe_down
        ms_sleep "$SETTLE"
        verify_app_state "refresh-files-${format_label}" || {
            # If navigate fails, try a full relaunch as fallback
            launch_app
            tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
            ms_sleep "$NAV_PAUSE"
        }
        screenshot "01-files-with-pushed-file"

        # Open the first file (should be the one we pushed, or scroll to find it)
        open_first_file
        verify_app_state "open_file-${format_label}" || { record_result "$format_label" "open-file" "CRASH" ""; stop_recording "recording"; launch_app; return; }
        screenshot "02-editor-opened"

        # The file content is already loaded; tap to focus
        tap "$EDITOR_FIELD_X" "$EDITOR_FIELD_Y"
        ms_sleep "$SETTLE"
    fi

    ms_sleep "$SETTLE"
    verify_app_state "content-created-${format_label}" || { record_result "$format_label" "content-create" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "03-content-created"

    # Save via toolbar (action2 = save icon)
    log "    Saving..."
    tap "$TOPBAR_ACTION2_X" "$TOPBAR_Y"
    ms_sleep "$SETTLE"
    verify_app_state "save-${format_label}" || { record_result "$format_label" "save" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "04-saved"

    # Navigate to Preview (action1 = preview icon)
    log "    Opening preview..."
    tap "$TOPBAR_ACTION1_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "preview-${format_label}" || { record_result "$format_label" "preview" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "05-preview"

    # Scroll preview to see more content
    swipe_up
    ms_sleep "$SETTLE"
    screenshot "06-preview-scrolled"

    # Go back to editor
    log "    Back to editor..."
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "back-to-editor-${format_label}" || { record_result "$format_label" "back-to-editor" "CRASH" ""; stop_recording "recording"; launch_app; return; }

    # Wait for editor to load
    ms_sleep "$SETTLE"

    # Edit: add more content at end
    log "    Editing (appending content)..."
    tap "$EDITOR_FIELD_X" "$EDITOR_FIELD_Y"
    ms_sleep 300

    # Move to end of content
    adb_shell input keyevent "KEYCODE_MOVE_END" 2>/dev/null || true
    ms_sleep 200

    press_enter
    type_text "---"
    press_enter
    type_text "Edited%sby%sautomation%stest"

    ms_sleep "$SETTLE"
    verify_app_state "edit-${format_label}" || { record_result "$format_label" "edit" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "07-edited"

    # Save again
    tap "$TOPBAR_ACTION2_X" "$TOPBAR_Y"
    ms_sleep "$SETTLE"
    verify_app_state "save-after-edit-${format_label}" || { record_result "$format_label" "save-after-edit" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "08-saved-after-edit"

    # Preview again
    tap "$TOPBAR_ACTION1_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "preview-after-edit-${format_label}" || { record_result "$format_label" "preview-after-edit" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "09-preview-after-edit"

    # Back to Files
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "10-final"

    # Go back to main files list (Fix 6: stay in app, just navigate back)
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"

    stop_recording "recording"
    record_result "$format_label" "complete" "PASS" ""
    log "  === ${format_label} complete ==="
}

###############################################################################
# Format content definitions
###############################################################################

MARKDOWN_CONTENT="# Yole Automation Test
## Introduction
This is **bold** and *italic* text.
- Item 1
- Item 2
- Item 3

> A blockquote for testing.

\`\`\`kotlin
fun main() = println(\"Hello Yole!\")
\`\`\`"

PLAINTEXT_CONTENT="Yole Automation Test - Plain Text
==================================
This is a plain text document.
No special formatting applied.
Line 1: The quick brown fox.
Line 2: Jumps over the lazy dog.
Line 3: Testing all characters."

TODOTXT_CONTENT="(A) Complete automation testing @yole +testing due:2026-03-17
(B) Review test recordings @yole +qa
(C) Update documentation @yole +docs
x 2026-03-16 Fix concurrency issues @yole +bugfix
(A) Run full test suite @yole +ci due:2026-03-18"

CSV_CONTENT="Name,Email,Department,Status
John Doe,john@company.com,Engineering,Active
Jane Smith,jane@company.com,Marketing,Active
Bob Johnson,bob@company.com,Sales,On Leave
Alice Brown,alice@company.com,Design,Active"

LATEX_CONTENT="\\documentclass{article}
\\title{Yole Automation Test}
\\author{Test Suite}
\\begin{document}
\\maketitle
\\section{Introduction}
This is a LaTeX document for testing.
\\subsection{Details}
Formula: \$E = mc^2\$
\\end{document}"

ORGMODE_CONTENT="#+TITLE: Yole Automation Test
#+AUTHOR: Test Suite

* Introduction
This is an Org Mode document.

** Tasks
- [ ] Task 1: Run automation
- [X] Task 2: Write tests
- [ ] Task 3: Review results

** Notes
Some notes about the test.
*** Sub-section
More details here."

WIKITEXT_CONTENT="== Yole Automation Test ==
=== Introduction ===
This is a '''WikiText''' document.

* Item 1
* Item 2
* Item 3

[[Link Target|Display Text]]

{| class=\"wikitable\"
|-
! Header 1 !! Header 2
|-
| Cell 1 || Cell 2
|}"

KEYVALUE_CONTENT="[application]
name = Yole
version = 2.0.0
platform = Android

[editor]
theme = dark
font_size = 14
line_numbers = true
auto_save = true

[formats]
default = markdown
supported = 17"

ASCIIDOC_CONTENT="= Yole Automation Test
Test Suite <test@yole.dev>
:toc:

== Introduction

This is an *AsciiDoc* document created for testing.

=== Features

* Feature 1: Format detection
* Feature 2: Live preview
* Feature 3: Theme support

[source,kotlin]
----
fun main() {
    println(\"Hello from Yole!\")
}
----

NOTE: This is a note admonition."

RESTRUCTUREDTEXT_CONTENT="=========================
Yole Automation Test
=========================

Introduction
------------
This is a reStructuredText document.

Features
~~~~~~~~
- Feature 1
- Feature 2
- Feature 3

Code Example
~~~~~~~~~~~~
.. code-block:: kotlin

   fun main() = println(\"Hello!\")

.. note::
   This is a note directive."

TASKPAPER_CONTENT="Yole Testing Project:
	- Run all format tests @tag(automation)
	- Verify screenshots @tag(qa) @due(2026-03-17)
	- Review video recordings @tag(review)
	- Update documentation @tag(docs) @done
	- Deploy release build @tag(deploy)
Housekeeping:
	- Clean up temp files @tag(cleanup)
	- Archive old logs @tag(maintenance)"

TEXTILE_CONTENT="h1. Yole Automation Test

h2. Introduction

This is a *Textile* document with _emphasis_.

h3. Features

* Feature 1
* Feature 2
* Feature 3

|_. Name |_. Status |
| Markdown | Active |
| Textile | Active |

bq. This is a blockquote in Textile format."

CREOLE_CONTENT="= Yole Automation Test =

== Introduction ==

This is a **Creole** markup document.
It has //italic// text too.

=== Features ===

* Feature 1
* Feature 2
** Sub-feature 2.1
** Sub-feature 2.2
* Feature 3

{{{
fun main() = println(\"Hello!\")
}}}"

TIDDLYWIKI_CONTENT="title: Yole Automation Test
tags: testing automation

! Introduction

This is a ''TiddlyWiki'' tiddler.

!! Features

* Feature 1
* Feature 2
* Feature 3

!!! Code Example

\`\`\`
fun main() = println(\"Hello!\")
\`\`\`

<<tag testing>>"

JUPYTER_CONTENT='{
  "nbformat": 4,
  "nbformat_minor": 5,
  "metadata": {
    "kernelspec": {
      "display_name": "Python 3",
      "language": "python",
      "name": "python3"
    }
  },
  "cells": [
    {
      "cell_type": "markdown",
      "metadata": {},
      "source": ["# Yole Automation Test\n", "Testing Jupyter format."]
    },
    {
      "cell_type": "code",
      "metadata": {},
      "source": ["print(\"Hello from Yole!\")"],
      "outputs": []
    }
  ]
}'

RMARKDOWN_CONTENT='---
title: "Yole Automation Test"
author: "Test Suite"
output: html_document
---

```{r setup, include=FALSE}
knitr::opts_chunk$set(echo = TRUE)
```

## Introduction

This is an **R Markdown** document.

```{r}
summary(cars)
```

## Plot

```{r pressure, echo=FALSE}
plot(pressure)
```'

BINARY_CONTENT="00000000  48 65 6c 6c 6f 20 59 6f  6c 65 21 0a 42 69 6e 61
00000010  72 79 20 66 6f 72 6d 61  74 20 74 65 73 74 0a 00
00000020  ff fe fd fc fb fa f9 f8  f7 f6 f5 f4 f3 f2 f1 f0
00000030  de ad be ef ca fe ba be  00 11 22 33 44 55 66 77"

###############################################################################
# Navigation & Settings flows
###############################################################################

test_navigation() {
    local dir_name="18-navigation"
    log ""
    log "  === Navigation Flow ==="
    init_format_dir "$dir_name"
    EVIDENCE_DIR="$SCREENSHOT_DIR"
    start_recording "recording"

    ensure_app_running
    verify_app_state "pre-navigation" || { launch_app; }

    # Files tab
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "files-tab" || { record_result "Navigation" "files-tab" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "01-files-tab"

    # To-Do tab
    tap "$BOTTOM_NAV_TODO_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "todo-tab" || { record_result "Navigation" "todo-tab" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "02-todo-tab"

    # QuickNote tab
    tap "$BOTTOM_NAV_QUICKNOTE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "quicknote-tab" || { record_result "Navigation" "quicknote-tab" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "03-quicknote-tab"

    # More tab
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "more-tab" || { record_result "Navigation" "more-tab" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "04-more-tab"

    # Back to Files
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "back-to-files" || { record_result "Navigation" "back-to-files" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "05-back-to-files"

    # Open FAB menu
    tap "$FAB_X" "$FAB_Y"
    ms_sleep "$SETTLE"
    screenshot "06-fab-dialog"

    # Dismiss dialog
    press_back
    ms_sleep "$NAV_PAUSE"

    # Scroll file list
    swipe_up
    ms_sleep "$SETTLE"
    screenshot "07-files-scrolled"

    swipe_down
    ms_sleep "$SETTLE"
    screenshot "08-files-top"

    # Hardware back from various screens
    tap "$BOTTOM_NAV_TODO_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    press_back
    ms_sleep "$NAV_PAUSE"
    verify_app_state "back-from-todo" || { record_result "Navigation" "back-from-todo" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "09-back-from-todo"

    stop_recording "recording"
    record_result "Navigation" "complete" "PASS" ""
    log "  === Navigation complete ==="
}

test_settings() {
    local dir_name="19-settings"
    log ""
    log "  === Settings & Theme Flow ==="
    init_format_dir "$dir_name"
    EVIDENCE_DIR="$SCREENSHOT_DIR"
    start_recording "recording"

    ensure_app_running
    verify_app_state "pre-settings" || { launch_app; }

    # Navigate to More > Settings
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "more-tab-settings" || { record_result "Settings" "more-tab" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "01-more-screen"

    tap "$CONTENT_CENTER_X" "$MORE_CARD1_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "settings-screen" || { record_result "Settings" "settings-screen" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "02-settings-initial"

    # Switch to Light theme
    log "    Setting Light theme"
    tap "$SETTINGS_RADIO_X" "$SETTINGS_RADIO_LIGHT_Y"
    ms_sleep "$SETTLE"
    verify_app_state "light-theme" || { record_result "Settings" "light-theme" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "03-light-theme"

    # Go back to see the app in light theme
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "files-light" || { record_result "Settings" "files-light" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "04-files-light"

    # Open editor in light theme
    tap "$FAB_X" "$FAB_Y"
    ms_sleep "$SETTLE"
    # Tap Create with default format (markdown)
    tap "$DIALOG_CREATE_BTN_X" "$DIALOG_CREATE_BTN_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "editor-light" || { record_result "Settings" "editor-light" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "05-editor-light"

    # Go back
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"

    # Switch to Dark theme
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$CONTENT_CENTER_X" "$MORE_CARD1_Y"
    ms_sleep "$NAV_PAUSE"

    log "    Setting Dark theme"
    tap "$SETTINGS_RADIO_X" "$SETTINGS_RADIO_DARK_Y"
    ms_sleep "$SETTLE"
    verify_app_state "dark-theme" || { record_result "Settings" "dark-theme" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "06-dark-theme"

    # Go back to see the app in dark theme
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "files-dark" || { record_result "Settings" "files-dark" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "07-files-dark"

    # Open editor in dark theme
    tap "$FAB_X" "$FAB_Y"
    ms_sleep "$SETTLE"
    tap "$DIALOG_CREATE_BTN_X" "$DIALOG_CREATE_BTN_Y"
    ms_sleep "$NAV_PAUSE"
    verify_app_state "editor-dark" || { record_result "Settings" "editor-dark" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "08-editor-dark"

    # Go back
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"

    # Reset to System theme
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$CONTENT_CENTER_X" "$MORE_CARD1_Y"
    ms_sleep "$NAV_PAUSE"

    log "    Resetting to System theme"
    tap "$SETTINGS_RADIO_X" "$SETTINGS_RADIO_SYSTEM_Y"
    ms_sleep "$SETTLE"
    verify_app_state "system-theme-restored" || { record_result "Settings" "system-theme" "CRASH" ""; stop_recording "recording"; launch_app; return; }
    screenshot "09-system-theme-restored"

    # Scroll settings
    swipe_up
    ms_sleep "$SETTLE"
    screenshot "10-settings-scrolled"

    swipe_down
    ms_sleep "$SETTLE"

    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"

    stop_recording "recording"
    record_result "Settings" "complete" "PASS" ""
    log "  === Settings complete ==="
}

###############################################################################
# Main execution
###############################################################################

main() {
    log "========================================================"
    log "Yole Android All-Formats Automation"
    log "Testing ALL 17 formats + navigation + settings"
    log "========================================================"

    resolve_device
    log "Using device: ${DEVICE_SERIAL}"

    get_screen_size
    compute_positions

    log "Positions:"
    log "  TopBar Y=${TOPBAR_Y}  BottomNav Y=${BOTTOM_NAV_Y}"
    log "  BottomNav: Files=${BOTTOM_NAV_FILES_X} Todo=${BOTTOM_NAV_TODO_X} QN=${BOTTOM_NAV_QUICKNOTE_X} More=${BOTTOM_NAV_MORE_X}"
    log "  FAB=(${FAB_X}, ${FAB_Y})  Editor=(${EDITOR_FIELD_X}, ${EDITOR_FIELD_Y})"
    log "  Dialog format start Y=${DIALOG_FORMAT_START_Y}  Create btn=(${DIALOG_CREATE_BTN_X}, ${DIALOG_CREATE_BTN_Y})"

    # Create output directory
    mkdir -p "$RECORDINGS_ROOT"

    # Initialize results file (Fix 7)
    : > "${RECORDINGS_ROOT}/results.jsonl"

    local total_start
    total_start=$(date +%s)

    # Setup emulator for clean automation (Fix 5)
    wake_screen
    setup_emulator

    # Ensure clean state
    adb_shell "am force-stop ${PACKAGE}" 2>/dev/null || true
    adb_shell "pm clear ${PACKAGE}" 2>/dev/null || true
    ms_sleep 500

    # Install and launch
    if [[ -f "$APK_PATH" ]]; then
        log "Installing APK..."
        adb_cmd install -r -g "$APK_PATH" > /dev/null 2>&1 || \
            adb_cmd install -r "$APK_PATH" > /dev/null 2>&1 || true
        log "APK installed"
    fi

    launch_app

    # =========================================================================
    # FORMAT 1: Markdown (via dialog, index 0)
    # =========================================================================
    test_format "01-markdown" "Markdown" "dialog:0" "$MARKDOWN_CONTENT"

    # =========================================================================
    # FORMAT 2: Plain Text (via dialog, index 1)
    # =========================================================================
    test_format "02-plaintext" "Plain Text" "dialog:1" "$PLAINTEXT_CONTENT"

    # =========================================================================
    # FORMAT 3: Todo.txt (via dialog, index 2)
    # =========================================================================
    test_format "03-todotxt" "Todo.txt" "dialog:2" "$TODOTXT_CONTENT"

    # =========================================================================
    # FORMAT 4: CSV (via dialog, index 3)
    # =========================================================================
    test_format "04-csv" "CSV" "dialog:3" "$CSV_CONTENT"

    # =========================================================================
    # FORMAT 5: LaTeX (via dialog, index 4)
    # =========================================================================
    test_format "05-latex" "LaTeX" "dialog:4" "$LATEX_CONTENT"

    # =========================================================================
    # FORMAT 6: Org Mode (via dialog, index 5)
    # =========================================================================
    test_format "06-orgmode" "Org Mode" "dialog:5" "$ORGMODE_CONTENT"

    # =========================================================================
    # FORMAT 7: WikiText (push file, open from browser)
    # =========================================================================
    test_format "07-wikitext" "WikiText" "file:test-document.wiki" "$WIKITEXT_CONTENT"

    # =========================================================================
    # FORMAT 8: Key-Value / INI (push file)
    # =========================================================================
    test_format "08-keyvalue" "Key-Value" "file:test-config.ini" "$KEYVALUE_CONTENT"

    # =========================================================================
    # FORMAT 9: AsciiDoc (push file)
    # =========================================================================
    test_format "09-asciidoc" "AsciiDoc" "file:test-document.adoc" "$ASCIIDOC_CONTENT"

    # =========================================================================
    # FORMAT 10: reStructuredText (push file)
    # =========================================================================
    test_format "10-restructuredtext" "reStructuredText" "file:test-document.rst" "$RESTRUCTUREDTEXT_CONTENT"

    # =========================================================================
    # FORMAT 11: TaskPaper (push file)
    # =========================================================================
    test_format "11-taskpaper" "TaskPaper" "file:test-document.taskpaper" "$TASKPAPER_CONTENT"

    # =========================================================================
    # FORMAT 12: Textile (push file)
    # =========================================================================
    test_format "12-textile" "Textile" "file:test-document.textile" "$TEXTILE_CONTENT"

    # =========================================================================
    # FORMAT 13: Creole (push file)
    # =========================================================================
    test_format "13-creole" "Creole" "file:test-document.creole" "$CREOLE_CONTENT"

    # =========================================================================
    # FORMAT 14: TiddlyWiki (push file)
    # =========================================================================
    test_format "14-tiddlywiki" "TiddlyWiki" "file:test-document.tid" "$TIDDLYWIKI_CONTENT"

    # =========================================================================
    # FORMAT 15: Jupyter Notebook (push file)
    # =========================================================================
    test_format "15-jupyter" "Jupyter Notebook" "file:test-notebook.ipynb" "$JUPYTER_CONTENT"

    # =========================================================================
    # FORMAT 16: R Markdown (push file)
    # =========================================================================
    test_format "16-rmarkdown" "R Markdown" "file:test-document.rmd" "$RMARKDOWN_CONTENT"

    # =========================================================================
    # FORMAT 17: Binary (push file)
    # =========================================================================
    test_format "17-binary" "Binary" "file:test-data.bin" "$BINARY_CONTENT"

    # =========================================================================
    # ADDITIONAL FLOW 18: Navigation
    # =========================================================================
    test_navigation

    # =========================================================================
    # ADDITIONAL FLOW 19: Settings & Theme
    # =========================================================================
    test_settings

    # =========================================================================
    # Cleanup device temp files
    # =========================================================================
    adb_shell "rm -f /sdcard/yole-test-input.txt /sdcard/yole-screenshot.png /sdcard/yole-frame.png" 2>/dev/null || true

    local total_end
    total_end=$(date +%s)
    local total_elapsed=$(( total_end - total_start ))

    log ""
    log "========================================================"
    log "ALL FORMAT TESTS COMPLETE"
    log "Total time: ${total_elapsed}s ($(( total_elapsed / 60 ))m $(( total_elapsed % 60 ))s)"
    log "========================================================"
    log ""

    # Print test results summary (Fix 7)
    log "Test Results:"
    log "  PASSED:  ${TESTS_PASSED}"
    log "  FAILED:  ${TESTS_FAILED}"
    log "  CRASHED: ${TESTS_CRASHED}"
    log ""

    # Print per-directory summary
    local total_screenshots=0
    local total_videos=0
    local dir_count=0

    for dir in "${RECORDINGS_ROOT}"/*/; do
        if [[ -d "$dir" ]]; then
            dir_count=$(( dir_count + 1 ))
            local sc vc dir_name
            dir_name=$(basename "$dir")
            sc=$(find "$dir" -maxdepth 1 -name "*.png" 2>/dev/null | wc -l)
            vc=$(find "$dir" -maxdepth 1 -name "*.mp4" -size +0c 2>/dev/null | wc -l)
            total_screenshots=$(( total_screenshots + sc ))
            total_videos=$(( total_videos + vc ))
            log "  ${dir_name}: ${sc} screenshots, ${vc} videos"
        fi
    done

    log ""
    log "Totals: ${total_screenshots} screenshots, ${total_videos} videos across ${dir_count} test directories"
    log "Results file: ${RECORDINGS_ROOT}/results.jsonl"
    log "Location: ${RECORDINGS_ROOT}/"

    # Return non-zero if any tests crashed or failed
    if [[ "$TESTS_CRASHED" -gt 0 || "$TESTS_FAILED" -gt 0 ]]; then
        return 1
    fi
}

main "$@"
