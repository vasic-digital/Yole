<!-- SPDX-License-Identifier: CC0-1.0 -->
# CodeGraph Integration

[CodeGraph](https://github.com/colbymchenry/codegraph) is a local, pre-indexed
code-knowledge-graph + MCP server. It gives AI coding agents semantic answers
about Yole's code structure (symbols, call graphs, impact radius) without
re-scanning the filesystem on every query.

- **Index:** `.codegraph/codegraph.db` — local SQLite, **git-ignored**, ~33 MB.
- **Config:** `.codegraph/config.json` — **committed**.
- **Scope:** Yole's own code only (`shared/`, `*App/`, `webApp/`, `tools/`,
  `yole-challenges/`, `archive/`). The 7 git submodules and
  `HelixQA/tools/opensource/` are excluded (they are separate projects).
- Current index: **911 files / ~21,964 symbols** (849 Kotlin, 18 Java, plus
  TS/JS/Go/Swift/Python).

## Wired CLI agents (5)

Every agent runs the same MCP server: `codegraph serve --mcp` (stdio).

| Agent | Config file | Scope |
|---|---|---|
| **Claude Code** (primary) | `.mcp.json` (repo root) | project — committed |
| **Qwen Code** | `.qwen/settings.json` (repo root) | project — committed |
| **OpenCode** | `~/.config/opencode/opencode.json` | user-global |
| **Crush** | `~/.config/crush/crush.json` | user-global |
| **Kimi CLI** | `~/.kimi/mcp.json` | user-global |

MCP tools exposed: `codegraph_search`, `codegraph_context`, `codegraph_callers`,
`codegraph_callees`, `codegraph_impact`, `codegraph_node`, `codegraph_files`,
`codegraph_status`.

## ⚠️ Host-specific runtime constraints

Two issues were found on this host and worked around — **reproduce these when
re-installing**:

1. **Node 23+ OOMs the indexer.** `codegraph index` parses with tree-sitter
   WASM grammars; on Node 25 V8's turboshaft WASM compiler aborts with
   `Fatal process out of memory: Zone` partway through a large index. **Indexing
   must run on Node 18–22.** `scripts/codegraph-index.sh` auto-selects a
   Node 18–22 runtime. The MCP server (`serve --mcp`) does no heavy parsing and
   runs fine on any Node.
2. **SQLite backend split.** codegraph's *native* `better-sqlite3` opens the DB
   in WAL mode, which its *WASM* SQLite fallback cannot reopen — so a DB touched
   by Node 25 could not be re-indexed by Node 18. Fixed by forcing the WASM
   SQLite backend everywhere: the native module is renamed aside
   (`node_modules/better-sqlite3` → `better-sqlite3.disabled`) inside the
   codegraph install. One backend → no WAL conflict.

### Reinstall / bootstrap steps

```bash
npm install -g @colbymchenry/codegraph
# force the WASM SQLite backend (one consistent backend across Node versions):
CGPKG=$(npm root -g)/@colbymchenry/codegraph
mv "$CGPKG/node_modules/better-sqlite3" "$CGPKG/node_modules/better-sqlite3.disabled"
# wire the 5 agents:
cd /path/to/Yole
claude mcp add -s project codegraph -- codegraph serve --mcp
qwen   mcp add -s project codegraph codegraph serve --mcp
kimi   mcp add codegraph -- codegraph serve --mcp
#   opencode + crush: add a "mcp" entry to their JSON configs (see this repo's
#   committed .mcp.json for the shape).
make codegraph-index          # init + full index, on Node 18–22
```

## Make targets

```bash
make codegraph-index     # full (re)index of the Yole codebase  (Node 18–22)
make codegraph-sync      # incremental update after edits        (Node 18–22)
make codegraph-status    # index statistics
make codegraph-query Q=FormatRegistry   # symbol search
make codegraph-verify    # run the anti-bluff integration Challenge
```

## Direct CLI

```bash
codegraph status
codegraph query <symbol> [--kind class|method|function] [--limit N] [--json]
codegraph context "<task description>"        # build AI-ready context
codegraph callers <symbol> / codegraph callees <symbol>
codegraph affected <file...>                  # impact analysis
bash scripts/codegraph-index.sh index --force # re-index (Node-version-safe)
```

## Verification (anti-bluff)

`yole-challenges/scripts/codegraph_integration_challenge.sh` proves the
integration genuinely works — not just that config files exist:

- the index DB holds a real symbol count above threshold;
- `codegraph query` returns a real Yole symbol with a real `file:line`;
- `codegraph serve --mcp` actually starts and answers a JSON-RPC `tools/list`
  handshake with the 8 codegraph tools;
- every one of the 5 agents has a codegraph MCP entry in its config.

Run it with `make codegraph-verify`. It is wired into `make qa-all`.
