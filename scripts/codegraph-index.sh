#!/usr/bin/env bash
# SPDX-License-Identifier: CC0-1.0
#
# codegraph-index.sh — run `codegraph index` / `codegraph sync` under a Node
# runtime that does not crash.
#
# WHY: codegraph parses source with tree-sitter WASM grammars. On Node 23+
# (observed on Node 25.9.0) V8's turboshaft WASM compiler exhausts its Zone
# allocator and aborts with "Fatal process out of memory: Zone" partway
# through a large index. Node 18–22 use the older WASM pipeline and index the
# full Yole codebase cleanly. The MCP server (`codegraph serve --mcp`) does no
# heavy parsing and runs fine on any Node — only index/sync need this wrapper.
#
# Usage:
#   scripts/codegraph-index.sh [index|sync|status|query|context] [args...]
# Examples:
#   scripts/codegraph-index.sh index --force
#   scripts/codegraph-index.sh sync
set -euo pipefail

SUB="${1:-index}"
[ "$#" -gt 0 ] && shift || true

# --- locate the codegraph CLI entry point -----------------------------------
CG_BIN="$(command -v codegraph || true)"
if [ -z "$CG_BIN" ]; then
  echo "ERROR: codegraph is not on PATH. Install it with:" >&2
  echo "       npm install -g @colbymchenry/codegraph" >&2
  exit 1
fi
# Resolve the bin symlink to the real .js file (codegraph's bin is a symlink
# into the npm package). `node -p fs.realpathSync` works on macOS + Linux.
CG_JS="$(node -p "require('fs').realpathSync(process.argv[1])" "$CG_BIN" 2>/dev/null || echo "$CG_BIN")"

# --- find a Node runtime in the 18–22 range ---------------------------------
NODE=""
for cand in \
  /opt/homebrew/opt/node@18/bin/node \
  /opt/homebrew/opt/node@20/bin/node \
  /opt/homebrew/opt/node@22/bin/node \
  /usr/local/opt/node@18/bin/node \
  /usr/local/opt/node@20/bin/node \
  /usr/local/opt/node@22/bin/node ; do
  [ -x "$cand" ] || continue
  major="$("$cand" --version 2>/dev/null | sed 's/^v//; s/\..*//')"
  case "$major" in
    18|19|20|21|22) NODE="$cand"; break ;;
  esac
done

if [ -z "$NODE" ]; then
  echo "ERROR: no Node 18–22 runtime found." >&2
  echo "       codegraph index OOMs on Node 23+ (V8 turboshaft WASM Zone)." >&2
  echo "       Install one, e.g.:  brew install node@22" >&2
  exit 1
fi

echo "[codegraph-index] $SUB  (runtime: $("$NODE" --version))"
exec env PATH="$(dirname "$NODE"):$PATH" "$NODE" "$CG_JS" "$SUB" "$@"
