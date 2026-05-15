#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Yole contributors
# SPDX-License-Identifier: Apache-2.0
#
# acquire-lsp-binaries.sh — Phase 7: Download and stage LSP server binaries
#
# SCOPE (v1): Desktop macos-arm64 only.  Android/Linux/Windows ABIs deferred.
# See docs/features/lsp/binary-acquisition-matrix.md for the full 84-cell matrix.
#
# Storage policy: option (c) — Gradle download task calls this script.
# Binaries land in .lsp-binary-cache/ (gitignored).
# The Gradle lspBinaries task then copies them into build/processedResources/.
#
# Usage:
#   bash scripts/acquire-lsp-binaries.sh [--abi macos-arm64] [--force]
#
# Options:
#   --abi <abi>   Target ABI to acquire (default: macos-arm64).
#                 Valid: macos-arm64  (linux-x64 and win-x64 deferred).
#   --force       Re-download even if the cached binary already exists.
#   --dry-run     Print what would be downloaded without fetching.
#   --verify      After download, verify SHA256 where upstream publishes one.
#
# Exit codes:
#   0  All targeted downloads succeeded.
#   1  One or more downloads failed (non-fatal; script continues, reports at end).

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CACHE_DIR="${REPO_ROOT}/.lsp-binary-cache"
ABI="macos-arm64"
FORCE=0
DRY_RUN=0
VERIFY=0

# ── argument parsing ──────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --abi)     ABI="$2"; shift 2 ;;
    --force)   FORCE=1; shift ;;
    --dry-run) DRY_RUN=1; shift ;;
    --verify)  VERIFY=1; shift ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

if [[ "$ABI" != "macos-arm64" ]]; then
  echo "ERROR: ABI '$ABI' is not implemented in v1.  Only macos-arm64 is supported." >&2
  echo "       See docs/features/lsp/binary-acquisition-matrix.md for future ABIs." >&2
  exit 1
fi

# ── helpers ───────────────────────────────────────────────────────────────────
FAILURES=0

log()  { echo "[lsp-acquire] $*"; }
warn() { echo "[lsp-acquire] WARN: $*" >&2; }
fail() { echo "[lsp-acquire] FAIL: $*" >&2; FAILURES=$((FAILURES + 1)); }

require_cmd() {
  if ! command -v "$1" &>/dev/null; then
    echo "ERROR: required command '$1' not found on PATH." >&2
    exit 1
  fi
}

require_cmd curl
require_cmd unzip

mkdir -p "${CACHE_DIR}"

# download <url> <dest-file>
download() {
  local url="$1" dest="$2"
  if [[ -f "$dest" && "$FORCE" -eq 0 ]]; then
    log "  cached   $(basename "$dest")"
    return 0
  fi
  if [[ "$DRY_RUN" -eq 1 ]]; then
    log "  dry-run  curl -fsSL '$url' → '$dest'"
    return 0
  fi
  log "  fetch    $url"
  if ! curl -fsSL --retry 3 --retry-delay 2 -o "$dest" "$url"; then
    fail "Download failed: $url"
    return 1
  fi
  log "  ok       $(basename "$dest") ($(du -sh "$dest" | cut -f1))"
}

# verify_sha256 <file> <expected-sha256>
verify_sha256() {
  local file="$1" expected="$2"
  if [[ "$VERIFY" -eq 0 ]]; then return 0; fi
  local actual
  actual="$(shasum -a 256 "$file" | awk '{print $1}')"
  if [[ "$actual" != "$expected" ]]; then
    fail "SHA256 mismatch for $(basename "$file"): expected=$expected actual=$actual"
    return 1
  fi
  log "  sha256 ok $(basename "$file")"
}

# extract_and_stage <archive> <extract-dir> <src-path-inside-archive> <dest-binary>
extract_and_stage() {
  local archive="$1" extract_dir="$2" src_path="$3" dest="$4"
  if [[ "$DRY_RUN" -eq 1 ]]; then
    log "  dry-run  extract $archive → $dest"
    return 0
  fi
  rm -rf "$extract_dir"
  mkdir -p "$extract_dir"
  case "$archive" in
    *.tar.gz)  tar -xzf "$archive"  -C "$extract_dir" ;;
    *.tar.xz)  tar -xJf "$archive"  -C "$extract_dir" ;;
    *.tar.bz2) tar -xjf "$archive"  -C "$extract_dir" ;;
    *.zip)     unzip -q  "$archive"  -d "$extract_dir" ;;
    *)
      fail "Unknown archive type: $archive"
      return 1
      ;;
  esac
  local src="${extract_dir}/${src_path}"
  if [[ ! -f "$src" ]]; then
    # Try to find it (handle slightly different layout)
    src="$(find "$extract_dir" -name "$(basename "$src_path")" -type f | head -1)"
    if [[ -z "$src" ]]; then
      fail "Expected binary '$(basename "$src_path")' not found inside $archive"
      return 1
    fi
  fi
  mkdir -p "$(dirname "$dest")"
  cp "$src" "$dest"
  chmod +x "$dest"
  log "  staged   $dest"
}

# stage_raw <downloaded-file> <dest-binary>
stage_raw() {
  local src="$1" dest="$2"
  if [[ "$DRY_RUN" -eq 1 ]]; then
    log "  dry-run  stage $src → $dest"
    return 0
  fi
  mkdir -p "$(dirname "$dest")"
  cp "$src" "$dest"
  chmod +x "$dest"
  log "  staged   $dest"
}

# Stage directory: .lsp-binary-cache/<langId>/<abi>/
stage_dir() {
  local lang_id="$1" abi="$2"
  echo "${CACHE_DIR}/${lang_id}/${abi}"
}

# ── download definitions — macos-arm64 ───────────────────────────────────────
log "=== LSP binary acquisition — ABI: ${ABI} ==="

# Version pins (update here when bumping)
RA_VERSION="2026-05-11"
CLANGD_VERSION="22.1.0"
MARKSMAN_VERSION="2026-02-08"
LLS_VERSION="3.18.2"
ZLS_VERSION="0.16.0"
HLS_VERSION="2.14.0.0"
JDTLS_VERSION="1.58.0"
JDTLS_BUILD="202604151538"
KLS_VERSION="1.3.13"   # fwcd/kotlin-language-server (JVM-bundle; JetBrains .sit deferred)

# ── 1. rust-analyzer ─────────────────────────────────────────────────────────
log "--- rust-analyzer ${RA_VERSION} ---"
STAGE_DIR="$(stage_dir "rust" "${ABI}")"
mkdir -p "${STAGE_DIR}"
ARCHIVE="${CACHE_DIR}/rust-analyzer-${ABI}.gz"
download \
  "https://github.com/rust-lang/rust-analyzer/releases/download/${RA_VERSION}/rust-analyzer-aarch64-apple-darwin.gz" \
  "${ARCHIVE}"
if [[ -f "$ARCHIVE" && "$DRY_RUN" -eq 0 ]]; then
  gunzip -fk "${ARCHIVE}" 2>/dev/null || true
  RAW="${ARCHIVE%.gz}"
  stage_raw "${RAW}" "${STAGE_DIR}/rust-analyzer"
  rm -f "${RAW}"
fi

# ── 2. clangd (shared: c and cpp lang IDs) ───────────────────────────────────
log "--- clangd ${CLANGD_VERSION} ---"
CLANGD_ARCHIVE="${CACHE_DIR}/clangd-mac-${CLANGD_VERSION}.zip"
CLANGD_EXTRACT="${CACHE_DIR}/clangd-mac-extract"
download \
  "https://github.com/clangd/clangd/releases/download/${CLANGD_VERSION}/clangd-mac-${CLANGD_VERSION}.zip" \
  "${CLANGD_ARCHIVE}"
if [[ -f "$CLANGD_ARCHIVE" ]]; then
  # clangd zip layout: clangd_<ver>/bin/clangd
  extract_and_stage \
    "${CLANGD_ARCHIVE}" \
    "${CLANGD_EXTRACT}" \
    "clangd_${CLANGD_VERSION}/bin/clangd" \
    "$(stage_dir "c" "${ABI}")/clangd"
  # cpp shares the same binary — symlink via copy
  mkdir -p "$(stage_dir "cpp" "${ABI}")"
  if [[ "$DRY_RUN" -eq 0 && -f "$(stage_dir "c" "${ABI}")/clangd" ]]; then
    cp "$(stage_dir "c" "${ABI}")/clangd" "$(stage_dir "cpp" "${ABI}")/clangd"
    log "  staged   $(stage_dir "cpp" "${ABI}")/clangd (copy of c/clangd)"
  fi
fi

# ── 3. marksman ──────────────────────────────────────────────────────────────
log "--- marksman ${MARKSMAN_VERSION} ---"
MARKSMAN_RAW="${CACHE_DIR}/marksman-macos"
download \
  "https://github.com/artempyanykh/marksman/releases/download/${MARKSMAN_VERSION}/marksman-macos" \
  "${MARKSMAN_RAW}"
if [[ -f "$MARKSMAN_RAW" ]]; then
  stage_raw "${MARKSMAN_RAW}" "$(stage_dir "markdown" "${ABI}")/marksman"
fi

# ── 4. lua-language-server ───────────────────────────────────────────────────
log "--- lua-language-server ${LLS_VERSION} ---"
LLS_ARCHIVE="${CACHE_DIR}/lua-language-server-${LLS_VERSION}-darwin-arm64.tar.gz"
LLS_EXTRACT="${CACHE_DIR}/lua-language-server-extract"
download \
  "https://github.com/LuaLS/lua-language-server/releases/download/${LLS_VERSION}/lua-language-server-${LLS_VERSION}-darwin-arm64.tar.gz" \
  "${LLS_ARCHIVE}"
if [[ -f "$LLS_ARCHIVE" ]]; then
  # LLS layout: bin/lua-language-server
  extract_and_stage \
    "${LLS_ARCHIVE}" \
    "${LLS_EXTRACT}" \
    "bin/lua-language-server" \
    "$(stage_dir "lua" "${ABI}")/lua-language-server"
fi

# ── 5. zls (Zig) ─────────────────────────────────────────────────────────────
log "--- zls ${ZLS_VERSION} ---"
ZLS_ARCHIVE="${CACHE_DIR}/zls-aarch64-macos.tar.xz"
ZLS_EXTRACT="${CACHE_DIR}/zls-extract"
download \
  "https://github.com/zigtools/zls/releases/download/${ZLS_VERSION}/zls-aarch64-macos.tar.xz" \
  "${ZLS_ARCHIVE}"
if [[ -f "$ZLS_ARCHIVE" ]]; then
  extract_and_stage \
    "${ZLS_ARCHIVE}" \
    "${ZLS_EXTRACT}" \
    "zls" \
    "$(stage_dir "zig" "${ABI}")/zls"
fi

# ── 6. haskell-language-server ───────────────────────────────────────────────
log "--- haskell-language-server ${HLS_VERSION} ---"
HLS_ARCHIVE="${CACHE_DIR}/hls-${HLS_VERSION}-aarch64-apple-darwin.tar.xz"
HLS_EXTRACT="${CACHE_DIR}/hls-extract"
download \
  "https://github.com/haskell/haskell-language-server/releases/download/${HLS_VERSION}/haskell-language-server-${HLS_VERSION}-aarch64-apple-darwin.tar.xz" \
  "${HLS_ARCHIVE}"
if [[ -f "$HLS_ARCHIVE" ]]; then
  # HLS ships multiple GHC-versioned wrappers; the wrapper named
  # haskell-language-server-wrapper dispatches to the right one.
  extract_and_stage \
    "${HLS_ARCHIVE}" \
    "${HLS_EXTRACT}" \
    "haskell-language-server-wrapper" \
    "$(stage_dir "haskell" "${ABI}")/haskell-language-server-wrapper"
fi

# ── 7. jdtls (JVM-bundle, platform-neutral) ──────────────────────────────────
log "--- jdtls ${JDTLS_VERSION} ---"
JDTLS_ARCHIVE="${CACHE_DIR}/jdt-language-server-${JDTLS_VERSION}-${JDTLS_BUILD}.tar.gz"
JDTLS_EXTRACT="${CACHE_DIR}/jdtls-extract"
JDTLS_SHA256_URL="https://download.eclipse.org/jdtls/milestones/${JDTLS_VERSION}/jdt-language-server-${JDTLS_VERSION}-${JDTLS_BUILD}.tar.gz.sha256"
download \
  "https://download.eclipse.org/jdtls/milestones/${JDTLS_VERSION}/jdt-language-server-${JDTLS_VERSION}-${JDTLS_BUILD}.tar.gz" \
  "${JDTLS_ARCHIVE}"
if [[ "$VERIFY" -eq 1 && -f "${JDTLS_ARCHIVE}" ]]; then
  JDTLS_SHA256_FILE="${CACHE_DIR}/jdtls.sha256"
  download "${JDTLS_SHA256_URL}" "${JDTLS_SHA256_FILE}"
  if [[ -f "$JDTLS_SHA256_FILE" ]]; then
    EXPECTED_SHA="$(cat "${JDTLS_SHA256_FILE}" | awk '{print $1}')"
    verify_sha256 "${JDTLS_ARCHIVE}" "${EXPECTED_SHA}"
  fi
fi
if [[ -f "$JDTLS_ARCHIVE" ]]; then
  # jdtls uses a launcher script at bin/jdtls; stage the entire bundle tree
  JDTLS_STAGE="$(stage_dir "java" "${ABI}")"
  if [[ "$DRY_RUN" -eq 0 ]]; then
    rm -rf "${JDTLS_STAGE}"
    mkdir -p "${JDTLS_STAGE}"
    tar -xzf "${JDTLS_ARCHIVE}" -C "${JDTLS_STAGE}"
    chmod +x "${JDTLS_STAGE}/bin/jdtls" 2>/dev/null || true
    log "  staged   ${JDTLS_STAGE}/bin/jdtls (full bundle)"
  else
    log "  dry-run  extract ${JDTLS_ARCHIVE} → ${JDTLS_STAGE}/"
  fi
fi

# ── 8. kotlin-language-server (fwcd JVM-bundle fallback) ─────────────────────
# JetBrains Kotlin/kotlin-lsp .sit archive requires `unstuff` which is not
# universally available. Using fwcd/kotlin-language-server server.zip instead —
# a platform-neutral JVM bundle that runs on any JRE 11+.
log "--- kotlin-language-server ${KLS_VERSION} (fwcd JVM-bundle) ---"
KLS_ARCHIVE="${CACHE_DIR}/kotlin-language-server-${KLS_VERSION}.zip"
KLS_EXTRACT="${CACHE_DIR}/kls-extract"
download \
  "https://github.com/fwcd/kotlin-language-server/releases/download/${KLS_VERSION}/server.zip" \
  "${KLS_ARCHIVE}"
if [[ -f "$KLS_ARCHIVE" ]]; then
  KLS_STAGE="$(stage_dir "kotlin" "${ABI}")"
  if [[ "$DRY_RUN" -eq 0 ]]; then
    rm -rf "${KLS_STAGE}"
    mkdir -p "${KLS_STAGE}"
    unzip -q "${KLS_ARCHIVE}" -d "${KLS_STAGE}"
    chmod +x "${KLS_STAGE}/server/bin/kotlin-language-server" 2>/dev/null || true
    log "  staged   ${KLS_STAGE}/server/bin/kotlin-language-server (full bundle)"
  else
    log "  dry-run  extract ${KLS_ARCHIVE} → ${KLS_STAGE}/"
  fi
fi

# ── SKIP summary ──────────────────────────────────────────────────────────────
log ""
log "=== SKIP summary (no action taken) ==="
log "  gopls         — no upstream pre-built binary; needs 'go install'; see #lsp-gopls-binary"
log "  pyright       — Node.js bundle; deferred to lsp-node-runtime DFM (Phase 8)"
log "  typescript-ls — Node.js bundle; deferred to lsp-node-runtime DFM (Phase 8)"
log "  bash-ls       — Node.js bundle; deferred to lsp-node-runtime DFM (Phase 8)"
log "  yaml-ls       — Node.js bundle; deferred to lsp-node-runtime DFM (Phase 8)"
log "  elixir-ls     — requires Erlang/OTP runtime; not self-contained; SKIP all ABIs"
log ""
log "  Android arm64 native servers (rust-analyzer, clangd, marksman, lua-ls, zls, hls)"
log "  — cross-build deferred; tracker: #crossbuild-android-ndk-lsp"

# ── result ────────────────────────────────────────────────────────────────────
log ""
if [[ $FAILURES -gt 0 ]]; then
  echo "[lsp-acquire] DONE with ${FAILURES} failure(s). Check log above." >&2
  exit 1
else
  log "=== DONE — all targeted downloads succeeded ==="
fi
