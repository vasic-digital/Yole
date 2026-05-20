#!/usr/bin/env bash
# SPDX-License-Identifier: CC0-1.0
#
# codegraph_integration_challenge.sh
#
# Anti-bluff verification (CONST-039) that the CodeGraph integration genuinely
# works for end users — not merely that config files exist. RUNTIME layer:
# it queries the real index DB and performs a real MCP JSON-RPC handshake.
#
# Each check prints PASS/FAIL; a per-run log artefact path is printed at the
# end; exit code 0 only when every check passes.
set -u

# --- locate repo root (this script lives at yole-challenges/scripts/) --------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

LOG_DIR="$REPO_ROOT/yole-challenges/logs"
mkdir -p "$LOG_DIR"
LOG="$LOG_DIR/codegraph_integration_$(date +%Y%m%d_%H%M%S).log"

PASS=0
FAIL=0
log()  { echo "$@" | tee -a "$LOG"; }
pass() { PASS=$((PASS+1)); log "PASS: $1"; }
fail() { FAIL=$((FAIL+1)); log "FAIL: $1"; }

# bounded run helper (codegraph serve --mcp is a long-lived stdio server)
run_bounded() {
  local secs="$1"; shift
  if command -v timeout  >/dev/null 2>&1; then timeout  "$secs" "$@"; return $?; fi
  if command -v gtimeout >/dev/null 2>&1; then gtimeout "$secs" "$@"; return $?; fi
  perl -e 'alarm shift; exec @ARGV' "$secs" "$@"
}

log "=== CodeGraph Integration Challenge — $(date -u +%Y-%m-%dT%H:%M:%SZ) ==="
log "repo: $REPO_ROOT"

# --- 1. codegraph binary present --------------------------------------------
if command -v codegraph >/dev/null 2>&1; then
  pass "codegraph binary on PATH ($(codegraph --version 2>/dev/null | head -1))"
else
  fail "codegraph binary not on PATH"
fi

# --- 2. index DB exists ------------------------------------------------------
if [ -f "$REPO_ROOT/.codegraph/codegraph.db" ]; then
  pass ".codegraph/codegraph.db exists ($(du -h "$REPO_ROOT/.codegraph/codegraph.db" | cut -f1))"
else
  fail ".codegraph/codegraph.db missing — run: make codegraph-index"
fi

# --- 3. index holds a real symbol count (anti-bluff: empty DB ~= 0) ---------
STATUS_OUT="$(codegraph status 2>/dev/null || true)"
echo "$STATUS_OUT" >> "$LOG"
NODES="$(echo "$STATUS_OUT" | grep -iE 'Nodes:' | grep -oE '[0-9,]+' | tr -d ',' | head -1)"
NODES="${NODES:-0}"
if [ "$NODES" -ge 5000 ]; then
  pass "index has $NODES symbol nodes (>= 5000 threshold — real scan)"
else
  fail "index has only $NODES symbol nodes (< 5000 — index not built or empty)"
fi

# --- 4. query returns a real Yole symbol with a real file:line --------------
Q_OUT="$(codegraph query FormatRegistry --limit 5 2>/dev/null || true)"
echo "$Q_OUT" >> "$LOG"
if echo "$Q_OUT" | grep -qE 'digital/vasic/yole/.+\.(kt|java):[0-9]+'; then
  HIT="$(echo "$Q_OUT" | grep -oE 'digital/vasic/yole/[^ ]+\.(kt|java):[0-9]+' | head -1)"
  pass "codegraph query 'FormatRegistry' returned a real symbol — $HIT"
else
  fail "codegraph query 'FormatRegistry' returned no real Yole file:line"
fi

# --- 5. MCP server actually starts + answers a tools/list handshake ---------
MCP_OUT="$(printf '%s\n%s\n%s\n' \
  '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"challenge","version":"1.0"}}}' \
  '{"jsonrpc":"2.0","method":"notifications/initialized"}' \
  '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
  | run_bounded 25 codegraph serve --mcp 2>/dev/null || true)"
echo "$MCP_OUT" >> "$LOG"
TOOLS="$(echo "$MCP_OUT" | tr ',' '\n' | grep -oE '"name":"codegraph_[a-z]+"' | sort -u | wc -l | tr -d ' ')"
if [ "${TOOLS:-0}" -ge 6 ]; then
  pass "codegraph MCP server started + answered tools/list with $TOOLS codegraph_* tools"
else
  fail "codegraph MCP server did not return >=6 tools (got ${TOOLS:-0})"
fi

# --- 6-10. each of the 5 CLI agents has a codegraph MCP entry ---------------
check_json_has_codegraph() {  # <label> <file> <node-expression-yielding-truthy>
  local label="$1" file="$2" expr="$3"
  if [ ! -f "$file" ]; then fail "$label: config file missing ($file)"; return; fi
  if node -e "const c=require('$file'); process.exit(($expr)?0:1)" >/dev/null 2>&1; then
    pass "$label: codegraph MCP entry present in $file"
  else
    fail "$label: codegraph MCP entry NOT found in $file"
  fi
}
check_json_has_codegraph "Claude Code" "$REPO_ROOT/.mcp.json"          "c.mcpServers&&c.mcpServers.codegraph"
check_json_has_codegraph "Qwen Code"   "$REPO_ROOT/.qwen/settings.json" "c.mcpServers&&c.mcpServers.codegraph"
check_json_has_codegraph "OpenCode"    "$HOME/.config/opencode/opencode.json" "c.mcp&&c.mcp.codegraph"
check_json_has_codegraph "Crush"       "$HOME/.config/crush/crush.json"        "c.mcp&&c.mcp.codegraph"
check_json_has_codegraph "Kimi CLI"    "$HOME/.kimi/mcp.json"                  "(c.mcpServers&&c.mcpServers.codegraph)||c.codegraph"

# --- summary ----------------------------------------------------------------
log ""
log "=== RESULT: $PASS passed, $FAIL failed ==="
log "log artefact: $LOG"
[ "$FAIL" -eq 0 ] && { log "CHALLENGE PASS"; exit 0; } || { log "CHALLENGE FAIL"; exit 1; }
