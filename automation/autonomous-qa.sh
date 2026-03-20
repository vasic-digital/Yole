#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# Yole Autonomous QA Session Runner
#
# Leverages HelixQA's LLM-powered autonomous testing capabilities:
#   - 4-phase autonomous session (Setup → Doc-Driven → Curiosity-Driven → Report)
#   - LLM-powered bug detection (visual, UX, accessibility, functional)
#   - Video evidence with timeline annotations
#   - Auto-generated tickets with LLM-suggested fixes
#
# Prerequisites:
#   - Go 1.24+
#   - LLM API keys in .env (ANTHROPIC_API_KEY, OPENAI_API_KEY, etc.)
#   - Built Yole binaries for target platforms
#
# Usage:
#   ./automation/autonomous-qa.sh --platforms android,desktop,web --timeout 2h

set -uo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HELIXQA_DIR="$PROJECT_ROOT/HelixQA"
OUTPUT_DIR="$PROJECT_ROOT/qa-results/autonomous"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

PLATFORMS="android,desktop,web"
TIMEOUT="2h"
COVERAGE_TARGET="0.9"
REPORT_FORMATS="markdown,html,json"
ENV_FILE="$PROJECT_ROOT/.env"

print_usage() {
    cat <<EOF
Yole Autonomous QA Session Runner

Usage: $0 [OPTIONS]

Options:
    --platforms LIST     Comma-separated platforms (default: android,desktop,web)
    --timeout DURATION   Session timeout (default: 2h)
    --coverage TARGET    Coverage target 0.0-1.0 (default: 0.9)
    --report FORMATS     Report formats (default: markdown,html,json)
    --env FILE           Environment file (default: .env)
    --help               Show this help

Examples:
    $0 --platforms android --timeout 1h
    $0 --platforms desktop,web --coverage 0.95

Phase Overview:
    1. Setup        - LLM selection, feature map building, agent spawning
    2. Doc-Driven   - Verify all documented features against running app
    3. Curiosity    - Explore undiscovered areas, edge cases, empty inputs
    4. Report       - Aggregate coverage, tickets, navigation maps

EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --platforms)   PLATFORMS="$2"; shift 2 ;;
        --timeout)     TIMEOUT="$2"; shift 2 ;;
        --coverage)    COVERAGE_TARGET="$2"; shift 2 ;;
        --report)      REPORT_FORMATS="$2"; shift 2 ;;
        --env)         ENV_FILE="$2"; shift 2 ;;
        --help)        print_usage; exit 0 ;;
        *)             echo "Unknown option: $1"; print_usage; exit 1 ;;
    esac
done

echo "============================================================"
echo " Yole Autonomous QA Session"
echo "============================================================"
echo " Platforms:      $PLATFORMS"
echo " Timeout:        $TIMEOUT"
echo " Coverage:       $COVERAGE_TARGET"
echo " Report formats: $REPORT_FORMATS"
echo " Output:         $OUTPUT_DIR"
echo "============================================================"
echo ""

if [[ ! -d "$HELIXQA_DIR" ]]; then
    echo "ERROR: HelixQA directory not found at $HELIXQA_DIR"
    echo "Run: git submodule update --init --recursive"
    exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
    echo "WARNING: No .env file found at $ENV_FILE"
    echo "LLM features may not work without API keys."
    echo ""
fi

mkdir -p "$OUTPUT_DIR"

cd "$HELIXQA_DIR"

echo "Building HelixQA..."
go build -o "$PROJECT_ROOT/releases/tools/helixqa" ./cmd/helixqa/
if [[ $? -ne 0 ]]; then
    echo "ERROR: Failed to build HelixQA"
    exit 1
fi

echo ""
echo "Starting autonomous QA session..."
echo "This will run 4 phases: Setup → Doc-Driven → Curiosity → Report"
echo ""

cd "$PROJECT_ROOT"

"$PROJECT_ROOT/releases/tools/helixqa" autonomous \
    --project "$PROJECT_ROOT" \
    --platforms "$PLATFORMS" \
    --env "$ENV_FILE" \
    --timeout "$TIMEOUT" \
    --coverage-target "$COVERAGE_TARGET" \
    --output "$OUTPUT_DIR/session-$TIMESTAMP" \
    --report "$REPORT_FORMATS" \
    --verbose

EXIT_CODE=$?

echo ""
echo "============================================================"
if [[ $EXIT_CODE -eq 0 ]]; then
    echo " Autonomous QA session completed successfully"
else
    echo " Autonomous QA session completed with issues (exit: $EXIT_CODE)"
fi
echo "============================================================"
echo ""
echo "Results available at:"
echo "  $OUTPUT_DIR/session-$TIMESTAMP/"
echo ""
echo "Key outputs:"
echo "  - qa-report.md          Summary report"
echo "  - qa-report.html        Visual report with navigation"
echo "  - coverage.json         Feature coverage metrics"
echo "  - tickets/               Auto-generated issue tickets"
echo "  - evidence/              Screenshots and video recordings"
echo "  - navigation-map.json    App navigation graph"
echo ""

exit $EXIT_CODE
