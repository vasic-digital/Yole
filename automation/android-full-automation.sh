#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Yole Android UI Automation — Real device interaction with ADB
# Comprehensive test of all app screens, navigation, editing, and settings
# with screenrecord video capture and per-step screenshots.
#
# Usage:  ./automation/android-full-automation.sh [--device SERIAL] [--speed slow|normal|fast|all]
#
# Requirements: adb in PATH, physical Android device connected, APK built.

set -euo pipefail

###############################################################################
# Configuration
###############################################################################

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK_PATH="${PROJECT_ROOT}/releases/android/androidApp-debug.apk"
PACKAGE="digital.vasic.yole.android"
ACTIVITY="${PACKAGE}.MainActivity"
RECORDINGS_ROOT="${PROJECT_ROOT}/recordings/android"

ADB="${ADB:-adb}"

# Default device — first connected device
DEFAULT_DEVICE="19bbb528a1dbbc4d"

# Parse arguments
DEVICE_SERIAL=""
SPEED_MODE="all"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --device) DEVICE_SERIAL="$2"; shift 2 ;;
        --speed)  SPEED_MODE="$2";    shift 2 ;;
        *)        echo "Unknown arg: $1"; exit 1 ;;
    esac
done

###############################################################################
# Speed mode definitions (milliseconds)
###############################################################################

declare -A SLOW_SPEED=(
    [tap_delay]=1000
    [type_delay]=100
    [nav_pause]=2000
    [swipe_dur]=500
    [settle]=1500
    [screen_wait]=3000
)

declare -A NORMAL_SPEED=(
    [tap_delay]=400
    [type_delay]=50
    [nav_pause]=1000
    [swipe_dur]=300
    [settle]=800
    [screen_wait]=1500
)

declare -A FAST_SPEED=(
    [tap_delay]=100
    [type_delay]=20
    [nav_pause]=300
    [swipe_dur]=100
    [settle]=300
    [screen_wait]=600
)

# Active speed vars — set by select_speed()
TAP_DELAY=0
TYPE_DELAY=0
NAV_PAUSE=0
SWIPE_DUR=0
SETTLE=0
SCREEN_WAIT=0

select_speed() {
    local mode="$1"
    case "$mode" in
        slow)
            TAP_DELAY=${SLOW_SPEED[tap_delay]}
            TYPE_DELAY=${SLOW_SPEED[type_delay]}
            NAV_PAUSE=${SLOW_SPEED[nav_pause]}
            SWIPE_DUR=${SLOW_SPEED[swipe_dur]}
            SETTLE=${SLOW_SPEED[settle]}
            SCREEN_WAIT=${SLOW_SPEED[screen_wait]}
            ;;
        normal)
            TAP_DELAY=${NORMAL_SPEED[tap_delay]}
            TYPE_DELAY=${NORMAL_SPEED[type_delay]}
            NAV_PAUSE=${NORMAL_SPEED[nav_pause]}
            SWIPE_DUR=${NORMAL_SPEED[swipe_dur]}
            SETTLE=${NORMAL_SPEED[settle]}
            SCREEN_WAIT=${NORMAL_SPEED[screen_wait]}
            ;;
        fast)
            TAP_DELAY=${FAST_SPEED[tap_delay]}
            TYPE_DELAY=${FAST_SPEED[type_delay]}
            NAV_PAUSE=${FAST_SPEED[nav_pause]}
            SWIPE_DUR=${FAST_SPEED[swipe_dur]}
            SETTLE=${FAST_SPEED[settle]}
            SCREEN_WAIT=${FAST_SPEED[screen_wait]}
            ;;
    esac
}

###############################################################################
# Screen geometry — resolved at runtime via `wm size`
###############################################################################

SCREEN_W=0
SCREEN_H=0

# Computed UI element positions (set by compute_positions)
# Top bar area
TOPBAR_Y=0
# Bottom navigation bar items (4 items: Files, To-Do, QuickNote, More)
BOTTOM_NAV_Y=0
BOTTOM_NAV_FILES_X=0
BOTTOM_NAV_TODO_X=0
BOTTOM_NAV_QUICKNOTE_X=0
BOTTOM_NAV_MORE_X=0
# Top bar action icons (right side)
TOPBAR_ACTION1_X=0   # first action from right
TOPBAR_ACTION2_X=0   # second action from right
TOPBAR_ACTION3_X=0   # third action from right
TOPBAR_NAV_BACK_X=0  # navigation back icon (left side)
# Content area
CONTENT_CENTER_X=0
CONTENT_TOP_Y=0
CONTENT_MID_Y=0
# FAB
FAB_X=0
FAB_Y=0
# Settings screen positions
SETTINGS_RADIO_X=0
SETTINGS_RADIO_SYSTEM_Y=0
SETTINGS_RADIO_LIGHT_Y=0
SETTINGS_RADIO_DARK_Y=0
SETTINGS_SWITCH_X=0
SETTINGS_SWITCH_LINENUMS_Y=0
SETTINGS_SWITCH_AUTOSAVE_Y=0
SETTINGS_SWITCH_ANIMATIONS_Y=0
# Editor text field area
EDITOR_FIELD_X=0
EDITOR_FIELD_Y=0
# File list first item
FILE_ITEM1_X=0
FILE_ITEM1_Y=0
FILE_ITEM2_Y=0
FILE_ITEM3_Y=0
# More screen card positions
MORE_CARD1_Y=0
MORE_CARD2_Y=0
MORE_CARD3_Y=0
MORE_CARD4_Y=0
MORE_CARD5_Y=0
# Todo add field
TODO_FIELD_X=0
TODO_FIELD_Y=0
TODO_ADD_BTN_X=0

compute_positions() {
    # Material3 TopAppBar is typically 64dp; on modern phones at ~2.75 density that is ~176px.
    # We keep positions as proportions of screen size to work on any resolution.

    local w=$SCREEN_W
    local h=$SCREEN_H

    # TopAppBar centre-line (Material3 TopAppBar ~ 64dp from top, ~176px on xxhdpi)
    TOPBAR_Y=$(( h * 7 / 100 ))           # ~7% from top

    # Bottom NavigationBar centre-line (~80dp tall, positioned at very bottom)
    BOTTOM_NAV_Y=$(( h - h * 4 / 100 ))   # ~4% from bottom edge
    # 4 equally spaced items across width
    BOTTOM_NAV_FILES_X=$(( w * 1 / 8 ))
    BOTTOM_NAV_TODO_X=$(( w * 3 / 8 ))
    BOTTOM_NAV_QUICKNOTE_X=$(( w * 5 / 8 ))
    BOTTOM_NAV_MORE_X=$(( w * 7 / 8 ))

    # TopBar action icons (right-aligned, each ~48dp = ~132px apart on xxhdpi)
    local icon_spacing=$(( w * 12 / 100 ))
    TOPBAR_ACTION1_X=$(( w - icon_spacing / 2 ))        # rightmost
    TOPBAR_ACTION2_X=$(( w - icon_spacing - icon_spacing / 2 ))
    TOPBAR_ACTION3_X=$(( w - 2 * icon_spacing - icon_spacing / 2 ))
    TOPBAR_NAV_BACK_X=$(( icon_spacing / 2 ))

    # Content area
    CONTENT_CENTER_X=$(( w / 2 ))
    CONTENT_TOP_Y=$(( h * 18 / 100 ))     # below top bar + some padding
    CONTENT_MID_Y=$(( h / 2 ))

    # FAB — bottom-right, above bottom nav
    FAB_X=$(( w - w * 10 / 100 ))
    FAB_Y=$(( h - h * 14 / 100 ))

    # Settings screen — the layout starts after the "Settings" headline + "Appearance" section
    # Radio buttons are left-aligned with padding (16dp ~ 44px)
    SETTINGS_RADIO_X=$(( w * 6 / 100 ))
    # Vertical positions (each row ~48dp)
    local settings_start=$(( h * 25 / 100 ))
    local row_h=$(( h * 5 / 100 ))
    SETTINGS_RADIO_SYSTEM_Y=$(( settings_start ))
    SETTINGS_RADIO_LIGHT_Y=$(( settings_start + row_h ))
    SETTINGS_RADIO_DARK_Y=$(( settings_start + 2 * row_h ))

    # Switches are right-aligned
    SETTINGS_SWITCH_X=$(( w - w * 8 / 100 ))
    local switch_start=$(( settings_start + 5 * row_h ))
    SETTINGS_SWITCH_LINENUMS_Y=$(( switch_start ))
    SETTINGS_SWITCH_AUTOSAVE_Y=$(( switch_start + row_h ))
    local anim_start=$(( switch_start + 4 * row_h ))
    SETTINGS_SWITCH_ANIMATIONS_Y=$(( anim_start ))

    # Editor text field — occupies most of screen below toolbar
    EDITOR_FIELD_X=$(( w / 2 ))
    EDITOR_FIELD_Y=$(( h * 45 / 100 ))

    # File list items — after "File Browser" headline + path, ~25% from top
    FILE_ITEM1_X=$(( w / 2 ))
    FILE_ITEM1_Y=$(( h * 32 / 100 ))
    FILE_ITEM2_Y=$(( h * 38 / 100 ))
    FILE_ITEM3_Y=$(( h * 44 / 100 ))

    # More screen cards — stacked cards starting around 18% from top
    local card_start=$(( h * 18 / 100 ))
    local card_h=$(( h * 10 / 100 ))
    MORE_CARD1_Y=$(( card_start ))                # Settings
    MORE_CARD2_Y=$(( card_start + card_h ))        # File Browser
    MORE_CARD3_Y=$(( card_start + 2 * card_h ))    # Search
    MORE_CARD4_Y=$(( card_start + 3 * card_h ))    # Backup
    MORE_CARD5_Y=$(( card_start + 4 * card_h ))    # About

    # Todo screen — add field is below header, about 25% from top
    TODO_FIELD_X=$(( w * 40 / 100 ))
    TODO_FIELD_Y=$(( h * 24 / 100 ))
    TODO_ADD_BTN_X=$(( w - w * 12 / 100 ))
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
    # sleep for N milliseconds
    local ms="$1"
    if (( ms > 0 )); then
        sleep "$(awk "BEGIN{printf \"%.3f\", $ms/1000}")"
    fi
}

tap() {
    local x="$1" y="$2"
    adb_shell input tap "$x" "$y"
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
    ms_sleep "$TAP_DELAY"
}

swipe_up() {
    swipe "$CONTENT_CENTER_X" "$(( SCREEN_H * 70 / 100 ))" "$CONTENT_CENTER_X" "$(( SCREEN_H * 30 / 100 ))"
}

swipe_down() {
    swipe "$CONTENT_CENTER_X" "$(( SCREEN_H * 30 / 100 ))" "$CONTENT_CENTER_X" "$(( SCREEN_H * 70 / 100 ))"
}

type_text() {
    # ADB input text does not handle spaces or special chars well.
    # We escape spaces as %s and send text in chunks.
    local text="$1"
    # Replace spaces with %s for adb
    local escaped="${text// /%s}"
    adb_shell input text "'${escaped}'"
    ms_sleep "$TYPE_DELAY"
}

type_text_slow() {
    # Type character by character for more realistic feel
    local text="$1"
    local i
    for (( i=0; i<${#text}; i++ )); do
        local ch="${text:$i:1}"
        case "$ch" in
            " ") adb_shell input text '%s' ;;
            *)   adb_shell input text "$ch" ;;
        esac
        ms_sleep "$(( TYPE_DELAY / 2 ))"
    done
}

press_key() {
    local keycode="$1"
    adb_shell input keyevent "$keycode"
    ms_sleep "$TAP_DELAY"
}

press_back() {
    press_key "KEYCODE_BACK"
}

press_home() {
    press_key "KEYCODE_HOME"
}

press_enter() {
    press_key "KEYCODE_ENTER"
}

select_all() {
    # Ctrl+A equivalent via keyevent
    adb_shell input keyevent "KEYCODE_MOVE_HOME"
    ms_sleep 100
    adb_shell input keyevent --longpress "KEYCODE_SHIFT_LEFT" "KEYCODE_MOVE_END"
    ms_sleep 100
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
    # Select all via Ctrl+A
    adb_shell "input keyevent 29 --meta 4096" 2>/dev/null || true  # Ctrl+A
    ms_sleep 200
    # Try long press to trigger selection menu, then select all
    long_press "$x" "$y" 1200
    ms_sleep 500
    press_key "KEYCODE_DEL"
    ms_sleep 200
    # Additional safety: send multiple deletes
    for _ in {1..20}; do
        adb_shell input keyevent "KEYCODE_DEL"
    done
    ms_sleep 200
}

###############################################################################
# Screenshot & Recording helpers
###############################################################################

SCREENSHOT_DIR=""
SCREENSHOT_COUNTER=0
RECORDING_PID=""
CURRENT_RECORDING_FILE=""

init_output_dirs() {
    local speed="$1"
    SCREENSHOT_DIR="${RECORDINGS_ROOT}/${speed}/screenshots"
    mkdir -p "$SCREENSHOT_DIR"
    mkdir -p "${RECORDINGS_ROOT}/${speed}"
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
    log "  Screenshot: ${fname}"
}

start_recording() {
    local speed="$1" phase="$2"
    local device_path="/sdcard/yole-test-${speed}-${phase}.mp4"
    local local_dir="${RECORDINGS_ROOT}/${speed}"
    CURRENT_RECORDING_FILE="${local_dir}/yole-test-${speed}-${phase}.mp4"

    # Kill any existing screenrecord
    adb_shell "killall screenrecord 2>/dev/null || true"
    ms_sleep 300

    # Start recording in background (max 180s)
    adb_shell "screenrecord --time-limit 180 ${device_path}" &
    RECORDING_PID=$!
    ms_sleep 500
    log "  Recording started: ${device_path} (PID ${RECORDING_PID})"
}

stop_recording() {
    local speed="$1" phase="$2"
    local device_path="/sdcard/yole-test-${speed}-${phase}.mp4"

    # Send SIGINT to screenrecord on device
    adb_shell "killall -2 screenrecord 2>/dev/null || true"
    ms_sleep 1500

    # Wait for background process
    if [[ -n "$RECORDING_PID" ]]; then
        wait "$RECORDING_PID" 2>/dev/null || true
        RECORDING_PID=""
    fi

    # Pull the recording
    if adb_shell "ls ${device_path}" > /dev/null 2>&1; then
        adb_cmd pull "$device_path" "$CURRENT_RECORDING_FILE" > /dev/null 2>&1
        adb_shell rm -f "$device_path"
        log "  Recording saved: ${CURRENT_RECORDING_FILE}"
    else
        warn "Recording file not found on device: ${device_path}"
    fi
}

###############################################################################
# Device setup
###############################################################################

resolve_device() {
    if [[ -n "$DEVICE_SERIAL" ]]; then
        # Verify device is connected
        if ! "$ADB" devices | grep -q "$DEVICE_SERIAL"; then
            fail "Device ${DEVICE_SERIAL} not found. Connected devices:"
            "$ADB" devices
        fi
        return
    fi

    # Try default device
    if "$ADB" devices | grep -q "$DEFAULT_DEVICE"; then
        DEVICE_SERIAL="$DEFAULT_DEVICE"
        return
    fi

    # Fall back to first available device
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

install_apk() {
    if [[ ! -f "$APK_PATH" ]]; then
        fail "APK not found at ${APK_PATH}"
    fi

    # Check if already installed with same version
    if adb_shell pm list packages | grep -q "$PACKAGE"; then
        log "Package ${PACKAGE} already installed — reinstalling"
        adb_cmd install -r -g "$APK_PATH" > /dev/null 2>&1 || \
            adb_cmd install -r "$APK_PATH" > /dev/null 2>&1
    else
        log "Installing ${APK_PATH}"
        adb_cmd install -g "$APK_PATH" > /dev/null 2>&1 || \
            adb_cmd install "$APK_PATH" > /dev/null 2>&1
    fi
    log "APK installed successfully"
}

launch_app() {
    # Force stop first to get clean state
    adb_shell am force-stop "$PACKAGE"
    ms_sleep 500
    adb_shell am start -n "${PACKAGE}/${ACTIVITY}" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
    ms_sleep "$SCREEN_WAIT"
    log "App launched"
}

ensure_app_running() {
    # Check if the app process is alive
    if ! adb_shell "pidof ${PACKAGE}" > /dev/null 2>&1; then
        warn "App not running — relaunching"
        launch_app
    fi
}

wake_screen() {
    # Ensure screen is on and unlocked
    local screen_state
    screen_state=$(adb_shell dumpsys power | grep "mWakefulness=" | head -1 || true)
    if [[ "$screen_state" != *"Awake"* ]]; then
        press_key "KEYCODE_WAKEUP"
        ms_sleep 500
    fi
    # Dismiss keyguard
    adb_shell input keyevent "KEYCODE_MENU" 2>/dev/null || true
    ms_sleep 300
}

###############################################################################
# Test Phases
###############################################################################

phase_01_app_launch() {
    local speed="$1"
    log "Phase 1: App Launch (${speed})"
    start_recording "$speed" "01-launch"

    install_apk
    wake_screen
    launch_app

    ms_sleep "$SCREEN_WAIT"
    screenshot "01-app-launched"

    # Verify we see the Files screen (default)
    ms_sleep "$SETTLE"
    screenshot "01-files-default-screen"

    stop_recording "$speed" "01-launch"
    log "Phase 1 complete"
}

phase_02_navigation() {
    local speed="$1"
    log "Phase 2: Bottom Navigation (${speed})"
    start_recording "$speed" "02-navigation"
    ensure_app_running

    # Start at Files (should already be there)
    screenshot "02-nav-start-files"

    # Tap To-Do in bottom nav
    log "  Navigating to To-Do"
    tap "$BOTTOM_NAV_TODO_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "02-nav-todo"

    # Tap QuickNote in bottom nav
    log "  Navigating to QuickNote"
    tap "$BOTTOM_NAV_QUICKNOTE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "02-nav-quicknote"

    # Tap More in bottom nav
    log "  Navigating to More"
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "02-nav-more"

    # Back to Files
    log "  Navigating back to Files"
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "02-nav-back-to-files"

    stop_recording "$speed" "02-navigation"
    log "Phase 2 complete"
}

phase_03_file_browser() {
    local speed="$1"
    log "Phase 3: File Browser (${speed})"
    start_recording "$speed" "03-file-browser"
    ensure_app_running

    # Make sure we're on Files tab
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "03-file-browser-initial"

    # Tap the FAB to create a new file (this navigates to editor with untitled.txt)
    log "  Creating new file via FAB"
    tap "$FAB_X" "$FAB_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "03-new-file-editor"

    # Go back to Files
    log "  Going back to Files"
    # The editor has a back button in the top-left (navigation icon)
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "03-back-to-files"

    # Try scrolling file list
    log "  Scrolling file list"
    swipe_up
    ms_sleep "$SETTLE"
    screenshot "03-files-scrolled-down"

    swipe_down
    ms_sleep "$SETTLE"
    screenshot "03-files-scrolled-up"

    # Tap search icon in top bar (first icon from left in actions)
    log "  Opening search"
    tap "$TOPBAR_ACTION3_X" "$TOPBAR_Y"
    ms_sleep "$SETTLE"
    screenshot "03-search-opened"

    # Close search by tapping again
    tap "$TOPBAR_ACTION3_X" "$TOPBAR_Y"
    ms_sleep "$SETTLE"

    # Tap sort icon (second action)
    log "  Cycling sort mode"
    tap "$TOPBAR_ACTION2_X" "$TOPBAR_Y"
    ms_sleep "$SETTLE"
    screenshot "03-sort-changed"

    # Tap more/settings icon (rightmost action)
    log "  Opening settings from top bar"
    tap "$TOPBAR_ACTION1_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "03-settings-from-files"

    # Go back
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"

    stop_recording "$speed" "03-file-browser"
    log "Phase 3 complete"
}

phase_04_editor_markdown() {
    local speed="$1"
    log "Phase 4: Editor - Markdown (${speed})"
    start_recording "$speed" "04-editor-markdown"
    ensure_app_running

    # Navigate to Files
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"

    # Create new file via FAB
    tap "$FAB_X" "$FAB_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "04-editor-empty"

    # Tap on the text field to focus it
    log "  Focusing editor field"
    tap "$EDITOR_FIELD_X" "$EDITOR_FIELD_Y"
    ms_sleep "$SETTLE"

    # Clear any existing content
    clear_field "$EDITOR_FIELD_X" "$EDITOR_FIELD_Y"
    ms_sleep 300

    # Type Markdown content line by line
    log "  Typing Markdown content"
    type_text "#%sTest%sDocument"
    press_enter
    type_text "##%sCreated%sby%sAutomation"
    press_enter
    type_text "This%sis%s**bold**%sand%s*italic*%stext."
    press_enter
    type_text "-%sItem%s1"
    press_enter
    type_text "-%sItem%s2"
    press_enter
    type_text "-%sItem%s3"
    press_enter
    press_enter
    type_text "A%sparagraph%swith%smore%scontent."

    ms_sleep "$SETTLE"
    screenshot "04-markdown-typed"

    # Tap Save in toolbar (icon in top bar actions — check icon is first action)
    log "  Saving file"
    tap "$TOPBAR_ACTION2_X" "$TOPBAR_Y"
    ms_sleep "$SETTLE"
    screenshot "04-markdown-saved"

    # Tap Preview icon (second action in editor top bar)
    log "  Opening preview from editor top bar"
    tap "$TOPBAR_ACTION1_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "04-markdown-preview"

    # Go back from preview
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"

    stop_recording "$speed" "04-editor-markdown"
    log "Phase 4 complete"
}

phase_05_editor_todotxt() {
    local speed="$1"
    log "Phase 5: Editor - Todo.txt (${speed})"
    start_recording "$speed" "05-editor-todotxt"
    ensure_app_running

    # Navigate to To-Do screen
    tap "$BOTTOM_NAV_TODO_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "05-todo-screen"

    # Tap the add-todo text field
    log "  Focusing todo input field"
    tap "$TODO_FIELD_X" "$TODO_FIELD_Y"
    ms_sleep "$SETTLE"

    # Type a todo item
    log "  Adding todo items"
    type_text "Complete%sautomation%stest%s@yole%s+testing"
    ms_sleep 300
    screenshot "05-todo-typed"

    # Tap Add button
    tap "$TODO_ADD_BTN_X" "$TODO_FIELD_Y"
    ms_sleep "$SETTLE"
    screenshot "05-todo-item-added"

    # Add another todo
    tap "$TODO_FIELD_X" "$TODO_FIELD_Y"
    ms_sleep 300
    type_text "Review%srecordings%s@yole%s+qa"
    tap "$TODO_ADD_BTN_X" "$TODO_FIELD_Y"
    ms_sleep "$SETTLE"
    screenshot "05-todo-second-added"

    # Add a third todo
    tap "$TODO_FIELD_X" "$TODO_FIELD_Y"
    ms_sleep 300
    type_text "Write%sdocumentation%s@yole%s+docs"
    tap "$TODO_ADD_BTN_X" "$TODO_FIELD_Y"
    ms_sleep "$SETTLE"
    screenshot "05-todo-third-added"

    # Tap the filter toggle
    log "  Cycling todo filter"
    # The "Show Active" / filter button is in the To-Do header area, right side
    local filter_btn_x=$(( SCREEN_W * 80 / 100 ))
    local filter_btn_y=$(( SCREEN_H * 17 / 100 ))
    tap "$filter_btn_x" "$filter_btn_y"
    ms_sleep "$SETTLE"
    screenshot "05-todo-filter-active"

    tap "$filter_btn_x" "$filter_btn_y"
    ms_sleep "$SETTLE"
    screenshot "05-todo-filter-completed"

    tap "$filter_btn_x" "$filter_btn_y"
    ms_sleep "$SETTLE"
    screenshot "05-todo-filter-all"

    stop_recording "$speed" "05-editor-todotxt"
    log "Phase 5 complete"
}

phase_06_preview() {
    local speed="$1"
    log "Phase 6: Preview (${speed})"
    start_recording "$speed" "06-preview"
    ensure_app_running

    # Navigate to QuickNote which has a preview toggle
    tap "$BOTTOM_NAV_QUICKNOTE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "06-quicknote-start"

    # Type some markdown content in QuickNote
    log "  Typing in QuickNote"
    # QuickNote has a text field filling most of the screen
    local qn_field_y=$(( SCREEN_H * 45 / 100 ))
    tap "$CONTENT_CENTER_X" "$qn_field_y"
    ms_sleep "$SETTLE"

    type_text "#%sQuickNote%sPreview%sTest"
    press_enter
    type_text "This%sis%s**bold**%sand%s*italic*."
    press_enter
    type_text "-%sList%sitem%s1"
    press_enter
    type_text "-%sList%sitem%s2"

    ms_sleep "$SETTLE"
    screenshot "06-quicknote-content-typed"

    # Tap Preview button (in the QuickNote header area)
    # The "Preview" text button is at the top-right of the content area
    local preview_btn_x=$(( SCREEN_W * 65 / 100 ))
    local preview_btn_y=$(( SCREEN_H * 12 / 100 ))
    tap "$preview_btn_x" "$preview_btn_y"
    ms_sleep "$NAV_PAUSE"
    screenshot "06-quicknote-preview-mode"

    # Switch back to Edit
    tap "$preview_btn_x" "$preview_btn_y"
    ms_sleep "$NAV_PAUSE"
    screenshot "06-quicknote-edit-mode"

    # Also test: go to Files, create file, then use Preview via editor top bar
    log "  Testing editor preview via top bar"
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"

    tap "$FAB_X" "$FAB_Y"
    ms_sleep "$NAV_PAUSE"

    # Type content
    tap "$EDITOR_FIELD_X" "$EDITOR_FIELD_Y"
    ms_sleep 300
    type_text "#%sPreview%sTest"
    press_enter
    type_text "Content%sfor%spreview."
    ms_sleep "$SETTLE"

    # Tap Preview icon in editor top bar
    tap "$TOPBAR_ACTION1_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "06-editor-preview-screen"

    # Go back
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"

    stop_recording "$speed" "06-preview"
    log "Phase 6 complete"
}

phase_07_settings() {
    local speed="$1"
    log "Phase 7: Settings (${speed})"
    start_recording "$speed" "07-settings"
    ensure_app_running

    # Navigate to More > Settings
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "07-more-screen"

    # Tap Settings card (first card in More screen)
    tap "$CONTENT_CENTER_X" "$MORE_CARD1_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "07-settings-initial"

    # Toggle theme to Light
    log "  Setting Light theme"
    tap "$SETTINGS_RADIO_X" "$SETTINGS_RADIO_LIGHT_Y"
    ms_sleep "$SETTLE"
    screenshot "07-settings-light-theme"

    # Toggle theme to Dark
    log "  Setting Dark theme"
    tap "$SETTINGS_RADIO_X" "$SETTINGS_RADIO_DARK_Y"
    ms_sleep "$SETTLE"
    screenshot "07-settings-dark-theme"

    # Toggle Line Numbers switch
    log "  Toggling Line Numbers"
    tap "$SETTINGS_SWITCH_X" "$SETTINGS_SWITCH_LINENUMS_Y"
    ms_sleep "$SETTLE"
    screenshot "07-settings-linenums-toggled"

    # Toggle Auto-save switch
    log "  Toggling Auto-save"
    tap "$SETTINGS_SWITCH_X" "$SETTINGS_SWITCH_AUTOSAVE_Y"
    ms_sleep "$SETTLE"
    screenshot "07-settings-autosave-toggled"

    # Toggle Animations switch
    log "  Toggling Animations"
    tap "$SETTINGS_SWITCH_X" "$SETTINGS_SWITCH_ANIMATIONS_Y"
    ms_sleep "$SETTLE"
    screenshot "07-settings-animations-toggled"

    # Scroll down to see format info
    log "  Scrolling settings to see formats"
    swipe_up
    ms_sleep "$SETTLE"
    screenshot "07-settings-formats-visible"

    # Scroll back up
    swipe_down
    ms_sleep "$SETTLE"

    # Reset theme to System
    log "  Resetting to System theme"
    tap "$SETTINGS_RADIO_X" "$SETTINGS_RADIO_SYSTEM_Y"
    ms_sleep "$SETTLE"
    screenshot "07-settings-system-theme"

    # Toggle switches back
    tap "$SETTINGS_SWITCH_X" "$SETTINGS_SWITCH_LINENUMS_Y"
    ms_sleep 200
    tap "$SETTINGS_SWITCH_X" "$SETTINGS_SWITCH_AUTOSAVE_Y"
    ms_sleep 200
    tap "$SETTINGS_SWITCH_X" "$SETTINGS_SWITCH_ANIMATIONS_Y"
    ms_sleep "$SETTLE"
    screenshot "07-settings-restored"

    # Go back
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"

    stop_recording "$speed" "07-settings"
    log "Phase 7 complete"
}

phase_08_theme_verification() {
    local speed="$1"
    log "Phase 8: Theme Verification (${speed})"
    start_recording "$speed" "08-theme"
    ensure_app_running

    # Go to More > Settings
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$CONTENT_CENTER_X" "$MORE_CARD1_Y"
    ms_sleep "$NAV_PAUSE"

    # Set Light theme
    log "  Setting Light theme for verification"
    tap "$SETTINGS_RADIO_X" "$SETTINGS_RADIO_LIGHT_Y"
    ms_sleep "$SETTLE"

    # Go back to main, then to Files
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "08-files-light-theme"

    # Create new file to see editor in light mode
    tap "$FAB_X" "$FAB_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "08-editor-light-theme"

    # Type some content for visual reference
    tap "$EDITOR_FIELD_X" "$EDITOR_FIELD_Y"
    ms_sleep 300
    type_text "Light%stheme%seditor%stest"
    ms_sleep "$SETTLE"
    screenshot "08-editor-light-with-content"

    # Go back
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"

    # Navigate to settings and switch to Dark
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$CONTENT_CENTER_X" "$MORE_CARD1_Y"
    ms_sleep "$NAV_PAUSE"

    log "  Setting Dark theme for verification"
    tap "$SETTINGS_RADIO_X" "$SETTINGS_RADIO_DARK_Y"
    ms_sleep "$SETTLE"

    # Go back and check Files
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "08-files-dark-theme"

    # Open editor in dark mode
    tap "$FAB_X" "$FAB_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "08-editor-dark-theme"

    tap "$EDITOR_FIELD_X" "$EDITOR_FIELD_Y"
    ms_sleep 300
    type_text "Dark%stheme%seditor%stest"
    ms_sleep "$SETTLE"
    screenshot "08-editor-dark-with-content"

    # Go back and reset to system theme
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$CONTENT_CENTER_X" "$MORE_CARD1_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$SETTINGS_RADIO_X" "$SETTINGS_RADIO_SYSTEM_Y"
    ms_sleep "$SETTLE"
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"

    stop_recording "$speed" "08-theme"
    log "Phase 8 complete"
}

phase_09_edge_cases() {
    local speed="$1"
    log "Phase 9: Edge Cases (${speed})"
    start_recording "$speed" "09-edge-cases"
    ensure_app_running

    # Navigate to Files
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"

    # Create new file via FAB
    log "  Creating file with long content"
    tap "$FAB_X" "$FAB_Y"
    ms_sleep "$NAV_PAUSE"

    # Focus editor
    tap "$EDITOR_FIELD_X" "$EDITOR_FIELD_Y"
    ms_sleep 300

    # Type a very long block of content (500+ characters)
    local long_line="Lorem%sipsum%sdolor%ssit%samet%sconsectetur%sadipiscing%selit."
    for i in {1..10}; do
        type_text "${long_line}"
        press_enter
        type_text "Line%s${i}:%sThe%squick%sbrown%sfox%sjumps%sover%sthe%slazy%sdog."
        press_enter
    done
    ms_sleep "$SETTLE"
    screenshot "09-long-content-typed"

    # Scroll through the long content
    log "  Scrolling through long content"
    swipe_up
    ms_sleep "$SETTLE"
    screenshot "09-long-content-scrolled-down"

    swipe_up
    ms_sleep "$SETTLE"
    screenshot "09-long-content-scrolled-more"

    swipe_down
    ms_sleep "$SETTLE"
    screenshot "09-long-content-scrolled-back"

    # Test empty QuickNote
    log "  Testing empty QuickNote"
    tap "$TOPBAR_NAV_BACK_X" "$TOPBAR_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$BOTTOM_NAV_QUICKNOTE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"

    # Clear QuickNote content by selecting all and deleting
    local qn_field_y=$(( SCREEN_H * 45 / 100 ))
    tap "$CONTENT_CENTER_X" "$qn_field_y"
    ms_sleep 300
    clear_field "$CONTENT_CENTER_X" "$qn_field_y"
    ms_sleep "$SETTLE"
    screenshot "09-quicknote-empty"

    # Test rapid navigation
    log "  Testing rapid navigation"
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep 200
    tap "$BOTTOM_NAV_TODO_X" "$BOTTOM_NAV_Y"
    ms_sleep 200
    tap "$BOTTOM_NAV_QUICKNOTE_X" "$BOTTOM_NAV_Y"
    ms_sleep 200
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep 200
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$SETTLE"
    screenshot "09-rapid-nav-complete"

    stop_recording "$speed" "09-edge-cases"
    log "Phase 9 complete"
}

phase_10_back_button() {
    local speed="$1"
    log "Phase 10: Back Button & Home (${speed})"
    start_recording "$speed" "10-back-button"
    ensure_app_running

    # Start from Files
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "10-start-files"

    # Open editor via FAB
    tap "$FAB_X" "$FAB_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "10-editor-opened"

    # Press hardware back button — should go back to Files
    log "  Hardware back from editor"
    press_back
    ms_sleep "$NAV_PAUSE"
    screenshot "10-back-from-editor"

    # Navigate to More > Settings
    tap "$BOTTOM_NAV_MORE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"
    tap "$CONTENT_CENTER_X" "$MORE_CARD1_Y"
    ms_sleep "$NAV_PAUSE"
    screenshot "10-settings-entered"

    # Press back from Settings
    log "  Hardware back from settings"
    press_back
    ms_sleep "$NAV_PAUSE"
    screenshot "10-back-from-settings"

    # Navigate to QuickNote
    tap "$BOTTOM_NAV_QUICKNOTE_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"

    # Press back from QuickNote (may close app or go to previous tab)
    log "  Hardware back from QuickNote"
    press_back
    ms_sleep "$NAV_PAUSE"
    screenshot "10-back-from-quicknote"

    # Test Home button and resume
    log "  Testing Home and resume"
    ensure_app_running
    tap "$BOTTOM_NAV_FILES_X" "$BOTTOM_NAV_Y"
    ms_sleep "$NAV_PAUSE"

    press_home
    ms_sleep "$SETTLE"
    screenshot "10-at-home-screen"

    # Resume app via recent apps or relaunch
    log "  Resuming app"
    adb_shell am start -n "${PACKAGE}/${ACTIVITY}" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
    ms_sleep "$SCREEN_WAIT"
    screenshot "10-app-resumed"

    # Verify state is preserved
    screenshot "10-state-preserved"

    stop_recording "$speed" "10-back-button"
    log "Phase 10 complete"
}

###############################################################################
# Run all phases for a given speed mode
###############################################################################

run_all_phases() {
    local speed="$1"
    log "============================================"
    log "Starting test run: speed=${speed}"
    log "============================================"

    select_speed "$speed"
    init_output_dirs "$speed"

    # Clean app state for fresh run
    adb_shell am force-stop "$PACKAGE" 2>/dev/null || true
    adb_shell pm clear "$PACKAGE" 2>/dev/null || true
    ms_sleep 500

    local phase_start phase_end elapsed
    phase_start=$(date +%s)

    phase_01_app_launch "$speed"
    phase_02_navigation "$speed"
    phase_03_file_browser "$speed"
    phase_04_editor_markdown "$speed"
    phase_05_editor_todotxt "$speed"
    phase_06_preview "$speed"
    phase_07_settings "$speed"
    phase_08_theme_verification "$speed"
    phase_09_edge_cases "$speed"
    phase_10_back_button "$speed"

    phase_end=$(date +%s)
    elapsed=$(( phase_end - phase_start ))

    log "============================================"
    log "Test run complete: speed=${speed} (${elapsed}s)"
    log "  Screenshots: ${SCREENSHOT_DIR}/"
    log "  Videos:      ${RECORDINGS_ROOT}/${speed}/"
    log "============================================"
}

###############################################################################
# Main
###############################################################################

main() {
    log "========================================================"
    log "Yole Android UI Automation"
    log "========================================================"

    resolve_device
    log "Using device: ${DEVICE_SERIAL}"

    get_screen_size
    compute_positions

    log "Computed positions:"
    log "  TopBar Y:    ${TOPBAR_Y}"
    log "  BottomNav Y: ${BOTTOM_NAV_Y}"
    log "  BottomNav:   Files=${BOTTOM_NAV_FILES_X} Todo=${BOTTOM_NAV_TODO_X} QN=${BOTTOM_NAV_QUICKNOTE_X} More=${BOTTOM_NAV_MORE_X}"
    log "  FAB:         (${FAB_X}, ${FAB_Y})"
    log "  Content:     center=(${CONTENT_CENTER_X}, ${CONTENT_MID_Y})"

    # Create output directories
    mkdir -p "${RECORDINGS_ROOT}/slow/screenshots"
    mkdir -p "${RECORDINGS_ROOT}/normal/screenshots"
    mkdir -p "${RECORDINGS_ROOT}/fast/screenshots"

    local total_start total_end total_elapsed
    total_start=$(date +%s)

    case "$SPEED_MODE" in
        slow)
            run_all_phases "slow"
            ;;
        normal)
            run_all_phases "normal"
            ;;
        fast)
            run_all_phases "fast"
            ;;
        all)
            run_all_phases "slow"
            run_all_phases "normal"
            run_all_phases "fast"
            ;;
        *)
            fail "Unknown speed mode: ${SPEED_MODE}. Use slow, normal, fast, or all."
            ;;
    esac

    total_end=$(date +%s)
    total_elapsed=$(( total_end - total_start ))

    log ""
    log "========================================================"
    log "ALL TESTS COMPLETE"
    log "Total time: ${total_elapsed}s"
    log "========================================================"
    log ""

    # Print summary
    local total_screenshots total_videos
    total_screenshots=$(find "${RECORDINGS_ROOT}" -name "*.png" 2>/dev/null | wc -l)
    total_videos=$(find "${RECORDINGS_ROOT}" -name "*.mp4" 2>/dev/null | wc -l)

    log "Results:"
    log "  Screenshots: ${total_screenshots} files"
    log "  Videos:      ${total_videos} files"
    log "  Location:    ${RECORDINGS_ROOT}/"
    log ""

    for spd in slow normal fast; do
        if [[ -d "${RECORDINGS_ROOT}/${spd}/screenshots" ]]; then
            local sc vc
            sc=$(find "${RECORDINGS_ROOT}/${spd}/screenshots" -name "*.png" 2>/dev/null | wc -l)
            vc=$(find "${RECORDINGS_ROOT}/${spd}" -maxdepth 1 -name "*.mp4" 2>/dev/null | wc -l)
            log "  ${spd}: ${sc} screenshots, ${vc} videos"
        fi
    done
}

main "$@"
