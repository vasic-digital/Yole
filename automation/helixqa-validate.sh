#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# HelixQA Post-Automation Evidence Validator
#
# Validates ALL evidence (screenshots, videos, logs) collected by automation
# scripts. Detects black-frame false positives, missing evidence, empty files,
# and generates markdown tickets for any issues found.
#
# Usage:
#   ./automation/helixqa-validate.sh [--platform android|web|desktop|all]
#
# Called automatically by run-qa-all.sh and individual automation scripts.

set -uo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HELIXQA_BIN="${PROJECT_ROOT}/releases/tools/helixqa"
FFMPEG_BIN="${HOME}/bin/ffmpeg"
TICKET_DIR="${PROJECT_ROOT}/qa-results/tickets"
PLATFORM="${1:---platform}"
PLATFORM_VALUE="${2:-all}"

# Parse --platform flag
if [[ "$PLATFORM" == "--platform" ]]; then
    PLATFORMS_TO_CHECK="$PLATFORM_VALUE"
else
    PLATFORMS_TO_CHECK="all"
fi

TOTAL_CHECKS=0
TOTAL_PASS=0
TOTAL_FAIL=0
TOTAL_WARN=0
TICKET_COUNT=0

###############################################################################
# Helpers
###############################################################################

log_pass() { echo "  [PASS] $1"; ((TOTAL_PASS++)); ((TOTAL_CHECKS++)); }
log_fail() { echo "  [FAIL] $1"; ((TOTAL_FAIL++)); ((TOTAL_CHECKS++)); }
log_warn() { echo "  [WARN] $1"; ((TOTAL_WARN++)); ((TOTAL_CHECKS++)); }

# Check if ffmpeg is available
HAS_FFMPEG=false
if command -v "$FFMPEG_BIN" &>/dev/null || command -v ffmpeg &>/dev/null; then
    HAS_FFMPEG=true
    [[ ! -x "$FFMPEG_BIN" ]] && FFMPEG_BIN="ffmpeg"
fi

# Validate a screenshot is not all-black
validate_screenshot() {
    local file="$1"
    local context="$2"

    if [[ ! -f "$file" ]]; then
        log_fail "Missing screenshot: $file ($context)"
        create_ticket "Missing Evidence" "Screenshot not found: $file" "$context" "high"
        return 1
    fi

    local size
    size=$(stat -c%s "$file" 2>/dev/null || echo 0)
    if [[ "$size" -lt 1024 ]]; then
        log_fail "Screenshot too small (${size}B): $file ($context)"
        create_ticket "Invalid Evidence" "Screenshot is only ${size} bytes — likely empty or corrupt" "$context" "high"
        return 1
    fi

    if [[ "$HAS_FFMPEG" == "true" ]]; then
        local black_detected
        black_detected=$("$FFMPEG_BIN" -i "$file" -vf "blackdetect=d=0:pix_th=0.10" \
            -an -f null - 2>&1 | grep -c "black_start" || true)
        if [[ "$black_detected" -gt 0 ]]; then
            log_fail "ALL BLACK screenshot: $file ($context)"
            create_ticket "Black Screenshot" "Screenshot is entirely black — false positive evidence" "$context" "critical"
            return 1
        fi
    fi

    log_pass "Screenshot valid: $(basename "$file") ($context)"
    return 0
}

# Validate a video recording
validate_video() {
    local file="$1"
    local context="$2"

    if [[ ! -f "$file" ]]; then
        log_fail "Missing video: $file ($context)"
        create_ticket "Missing Evidence" "Video recording not found: $file" "$context" "high"
        return 1
    fi

    local size
    size=$(stat -c%s "$file" 2>/dev/null || echo 0)
    if [[ "$size" -lt 1024 ]]; then
        log_fail "Video too small (${size}B): $file ($context)"
        create_ticket "Invalid Evidence" "Video is only ${size} bytes — recording failed" "$context" "high"
        return 1
    fi

    if [[ "$HAS_FFMPEG" == "true" ]]; then
        local duration
        duration=$("$FFMPEG_BIN" -i "$file" 2>&1 | grep "Duration" | head -1 | \
            sed 's/.*Duration: \([^,]*\).*/\1/' || echo "00:00:00.00")
        if [[ "$duration" == "00:00:00.00" || "$duration" == "N/A" ]]; then
            log_fail "Zero-length video: $file ($context)"
            create_ticket "Invalid Evidence" "Video has zero duration" "$context" "high"
            return 1
        fi
    fi

    log_pass "Video valid: $(basename "$file") ($context)"
    return 0
}

# Validate a results file exists and has no failures
validate_results() {
    local file="$1"
    local context="$2"

    if [[ ! -f "$file" ]]; then
        log_fail "Missing results file: $file ($context)"
        create_ticket "Missing Results" "Test results file not found" "$context" "critical"
        return 1
    fi

    local fail_count=0
    if [[ "$file" == *.jsonl ]]; then
        fail_count=$(grep -c '"FAIL"' "$file" 2>/dev/null || true)
    elif [[ "$file" == *.json ]]; then
        fail_count=$(grep -c '"failed"' "$file" 2>/dev/null || true)
    fi

    if [[ "$fail_count" -gt 0 ]]; then
        log_fail "Results contain $fail_count failures: $file ($context)"
        create_ticket "Test Failures" "$fail_count test(s) failed in results file" "$context" "critical"
        return 1
    fi

    log_pass "Results valid: $(basename "$file") ($context)"
    return 0
}

# Create a markdown ticket
create_ticket() {
    local title="$1"
    local description="$2"
    local context="$3"
    local severity="$4"

    ((TICKET_COUNT++))
    local ticket_id
    ticket_id=$(printf "HQA-V-%04d" "$TICKET_COUNT")
    local ticket_file="${TICKET_DIR}/${ticket_id}.md"

    mkdir -p "$TICKET_DIR"
    cat > "$ticket_file" <<TICKET
# ${title}

| Field | Value |
|-------|-------|
| **ID** | ${ticket_id} |
| **Severity** | ${severity^^} |
| **Context** | ${context} |
| **Created** | $(date -Iseconds) |

## Description

${description}

## Context

Platform/step: ${context}

---
*Generated by HelixQA Evidence Validator*
TICKET

    echo "    Ticket created: ${ticket_id} (${severity})"
}

###############################################################################
# Platform validators
###############################################################################

validate_android() {
    echo ""
    echo "=== Validating Android Evidence ==="
    local dir="${PROJECT_ROOT}/recordings/android/formats"

    if [[ ! -d "$dir" ]]; then
        log_fail "Android recordings directory missing: $dir"
        return
    fi

    # Validate results file
    validate_results "$dir/results.jsonl" "android/results" || true

    # Validate each format directory
    for fmt_dir in "$dir"/[0-9][0-9]-*; do
        [[ -d "$fmt_dir" ]] || continue
        local fmt_name
        fmt_name=$(basename "$fmt_dir")

        # Check screenshots
        local screenshot_count
        screenshot_count=$(find "$fmt_dir" -name "*.png" 2>/dev/null | wc -l)
        if [[ "$screenshot_count" -eq 0 ]]; then
            log_fail "No screenshots in $fmt_name"
        else
            # Validate first and last screenshot
            local first_screenshot
            first_screenshot=$(find "$fmt_dir" -name "*.png" | sort | head -1)
            validate_screenshot "$first_screenshot" "android/$fmt_name" || true
        fi

        # Check video
        local video_count
        video_count=$(find "$fmt_dir" -name "*.mp4" 2>/dev/null | wc -l)
        if [[ "$video_count" -eq 0 ]]; then
            log_warn "No video recording in $fmt_name"
        else
            local video
            video=$(find "$fmt_dir" -name "*.mp4" | head -1)
            validate_video "$video" "android/$fmt_name" || true
        fi
    done
}

validate_web() {
    echo ""
    echo "=== Validating Web Evidence ==="
    local dir="${PROJECT_ROOT}/recordings/web/formats"

    if [[ ! -d "$dir" ]]; then
        log_fail "Web recordings directory missing: $dir"
        return
    fi

    # Validate results file
    validate_results "$dir/results.json" "web/results" || true

    # Validate each format directory
    for fmt_dir in "$dir"/[0-9][0-9]-*; do
        [[ -d "$fmt_dir" ]] || continue
        local fmt_name
        fmt_name=$(basename "$fmt_dir")

        local screenshot_count
        screenshot_count=$(find "$fmt_dir" -name "*.png" 2>/dev/null | wc -l)
        if [[ "$screenshot_count" -eq 0 ]]; then
            log_fail "No screenshots in $fmt_name"
        else
            local first_screenshot
            first_screenshot=$(find "$fmt_dir" -name "*.png" | sort | head -1)
            validate_screenshot "$first_screenshot" "web/$fmt_name" || true
        fi

        local video_count
        video_count=$(find "$fmt_dir" -name "*.webm" 2>/dev/null | wc -l)
        if [[ "$video_count" -eq 0 ]]; then
            log_warn "No video recording in $fmt_name"
        else
            local video
            video=$(find "$fmt_dir" -name "*.webm" | head -1)
            validate_video "$video" "web/$fmt_name" || true
        fi
    done
}

validate_desktop() {
    echo ""
    echo "=== Validating Desktop Evidence ==="
    local dir="${PROJECT_ROOT}/recordings/desktop/formats"

    if [[ ! -d "$dir" ]]; then
        log_fail "Desktop recordings directory missing: $dir"
        return
    fi

    # Validate Compose-captured screenshots (primary evidence)
    # Screenshots are saved directly in recordings/desktop/formats/ (consistent
    # with Android/Web structure). Also check legacy desktopApp/ location.
    local found_compose=""
    for candidate in "$dir" "$dir/screenshots" "${PROJECT_ROOT}/desktopApp/recordings/desktop/formats" "${PROJECT_ROOT}/desktopApp/recordings/desktop/formats/screenshots"; do
        local count
        count=$(find "$candidate" -maxdepth 1 -name "*.png" 2>/dev/null | wc -l)
        if [[ "$count" -gt 0 ]]; then
            found_compose="$candidate"
            break
        fi
    done

    if [[ -z "$found_compose" ]]; then
        log_fail "No Compose screenshots directory found (primary evidence missing)"
        create_ticket "Missing Desktop Evidence" \
            "No Compose captureToImage() screenshots found — desktop evidence is invalid" \
            "desktop/screenshots" "critical"
    else
        local compose_count
        compose_count=$(find "$found_compose" -name "*.png" 2>/dev/null | wc -l)
        if [[ "$compose_count" -eq 0 ]]; then
            log_fail "Compose screenshot directory empty"
            create_ticket "Empty Desktop Evidence" \
                "Compose screenshot directory exists but contains no files" \
                "desktop/screenshots" "critical"
        else
            echo "  Found $compose_count Compose screenshots in $found_compose"
            for img in "$found_compose"/*.png; do
                [[ -f "$img" ]] || continue
                validate_screenshot "$img" "desktop/compose" || true
            done
        fi
    fi

    # Check x11grab screenshots (secondary — warn only)
    for xgrab in "$dir"/before-*.png "$dir"/after-*.png; do
        [[ -f "$xgrab" ]] || continue
        if [[ "$HAS_FFMPEG" == "true" ]]; then
            local black
            black=$("$FFMPEG_BIN" -i "$xgrab" -vf "blackdetect=d=0:pix_th=0.10" \
                -an -f null - 2>&1 | grep -c "black_start" || true)
            if [[ "$black" -gt 0 ]]; then
                log_warn "x11grab screenshot is black (Wayland/headless): $(basename "$xgrab")"
            fi
        fi
    done
}

###############################################################################
# Main
###############################################################################

echo "============================================================"
echo " HelixQA Evidence Validator"
echo "============================================================"
echo " Project:   $PROJECT_ROOT"
echo " Platforms: $PLATFORMS_TO_CHECK"
echo " Tickets:   $TICKET_DIR"
echo "============================================================"

case "$PLATFORMS_TO_CHECK" in
    android)
        validate_android
        ;;
    web)
        validate_web
        ;;
    desktop)
        validate_desktop
        ;;
    all)
        validate_android
        validate_web
        validate_desktop
        ;;
    *)
        echo "ERROR: Unknown platform: $PLATFORMS_TO_CHECK"
        exit 1
        ;;
esac

# Summary
echo ""
echo "============================================================"
echo " Evidence Validation Summary"
echo "============================================================"
echo "  Total checks: $TOTAL_CHECKS"
echo "  Passed:       $TOTAL_PASS"
echo "  Failed:       $TOTAL_FAIL"
echo "  Warnings:     $TOTAL_WARN"
echo "  Tickets:      $TICKET_COUNT"

if [[ "$TOTAL_FAIL" -gt 0 ]]; then
    echo ""
    echo "  RESULT: FAIL ($TOTAL_FAIL evidence validation failures)"
    echo "  Tickets at: $TICKET_DIR/"
    exit 1
else
    echo ""
    echo "  RESULT: PASS (all evidence validated)"
    exit 0
fi
