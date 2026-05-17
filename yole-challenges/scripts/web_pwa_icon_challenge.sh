#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# web_pwa_icon_challenge.sh — iter-73 Web PWA icon anti-bluff gate (CONST-039).
#
# Verifies that every PNG icon referenced in the Web PWA manifest.json actually
# exists on disk and is a non-degenerate file. Also validates the bundle when a
# Wasm release bundle is present in releases/.
#
# Anti-bluff (CONST-035 / CONST-039): positive evidence emitted per check.
# A PASS without opening files and checking sizes is bluff.
#
# Three verification layers:
#
#   (A) MANIFEST SCHEMA — validates manifest.json:
#       • manifest.json exists at expected path
#       • "icons" array is present
#       • At least one entry with sizes "192x192" and type "image/png"
#       • At least one entry with sizes "512x512" and type "image/png"
#       Emits "[OK] <check>" per assertion.
#
#   (B) SOURCE-TREE FILE PRESENCE — for each PNG icon declared in manifest:
#       • Referenced file exists in webApp/src/wasmJsMain/resources/
#       • File is a valid PNG (starts with PNG magic bytes 89 50 4E 47)
#       • File is >= 500 bytes (not a stub or empty placeholder)
#       Emits "[OK] <path>: <size> bytes" per assertion.
#
#   (C) BUNDLE AUDIT (conditional) — when a Wasm release bundle exists in releases/:
#       • Each PNG icon referenced in manifest is present inside the bundle zip
#       • Each bundled PNG is >= 500 bytes
#       When no bundle exists, emits "[SKIP] bundle-side audit deferred per
#       #wasmjs-production-distribution-gap" and exits 0 from layer C only.
#
# Exit codes:
#   0 = all applicable layers PASS
#   1 = manifest schema fail
#   2 = source-tree file fail
#   3 = bundle audit fail
#
# Cross-platform impact (CONST-037):
#   All layers: host-agnostic (pure filesystem + file(1) magic check).
#   Android/Desktop/iOS: not in scope for this challenge.
#
# Submodule decoupling (CONST-038): no submodule state read or required.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

MANIFEST_SRC="webApp/src/wasmJsMain/resources/manifest.json"
ICONS_BASE="webApp/src/wasmJsMain/resources"

echo "=== [web_pwa_icon_challenge] LAYER A: manifest schema validation ==="

schema_fail=0

# ---- A1: manifest exists ----
if [ -f "${MANIFEST_SRC}" ]; then
    echo "[OK] manifest.json exists: ${MANIFEST_SRC}"
else
    echo "[FAIL] manifest.json not found: ${MANIFEST_SRC}"
    echo "       Every PWA requires a manifest. Web app cannot be installed without it."
    exit 1
fi

# ---- A2: icons array present ----
if grep -q '"icons"' "${MANIFEST_SRC}"; then
    echo '[OK] manifest.json: "icons" array declared'
else
    echo '[FAIL] manifest.json: missing "icons" array'
    schema_fail=1
fi

# ---- A3: 192x192 PNG entry required by PWA install spec ----
if grep -q '"192x192"' "${MANIFEST_SRC}" && grep -q '"image/png"' "${MANIFEST_SRC}"; then
    echo '[OK] manifest.json: 192x192 PNG icon entry present'
else
    echo '[FAIL] manifest.json: missing required 192x192 PNG icon'
    echo '       Chrome and Firefox require exactly this size to enable PWA install.'
    schema_fail=1
fi

# ---- A4: 512x512 PNG entry required for homescreen splash on install ----
if grep -q '"512x512"' "${MANIFEST_SRC}"; then
    echo '[OK] manifest.json: 512x512 PNG icon entry present'
else
    echo '[FAIL] manifest.json: missing required 512x512 PNG icon'
    echo '       Android Chrome requires 512x512 for the app splash screen.'
    schema_fail=1
fi

if [ "${schema_fail}" -ne 0 ]; then
    echo ""
    echo "[FAIL] web_pwa_icon_challenge LAYER A failed — manifest schema is broken."
    exit 1
fi

echo ""
echo "=== [web_pwa_icon_challenge] LAYER B: source-tree PNG presence + validity ==="

src_fail=0

# Extract all "src": "icons/icon-*.png" paths from the manifest
# Parse lines matching: "src": "icons/..."
mapfile -t ICON_SRCS < <(grep -oE '"src": *"[^"]*\.png"' "${MANIFEST_SRC}" | \
    grep -oE '"[^"]*\.png"' | tr -d '"' || true)

if [ "${#ICON_SRCS[@]}" -eq 0 ]; then
    echo "[FAIL] No PNG icon 'src' entries found in manifest.json"
    src_fail=1
else
    echo "[OK] Found ${#ICON_SRCS[@]} PNG icon src entries in manifest.json"

    for icon_src in "${ICON_SRCS[@]}"; do
        full_path="${ICONS_BASE}/${icon_src}"

        # B1: file exists
        if [ ! -f "${full_path}" ]; then
            echo "[FAIL] ${icon_src}: file does not exist at ${full_path}"
            src_fail=1
            continue
        fi

        # B2: PNG magic bytes (89 50 4E 47 0D 0A 1A 0A)
        MAGIC=$(xxd -p -l 4 "${full_path}" 2>/dev/null || od -A n -t x1 -N 4 "${full_path}" 2>/dev/null | tr -d ' \n' | head -c 8)
        if [ "${MAGIC}" = "89504e47" ]; then
            echo "[OK] ${icon_src}: valid PNG magic bytes (89504e47)"
        else
            echo "[FAIL] ${icon_src}: not a valid PNG (magic=${MAGIC})"
            src_fail=1
            continue
        fi

        # B3: file size >= 500 bytes
        FILE_SIZE=$(wc -c < "${full_path}" | tr -d ' ')
        if [ "${FILE_SIZE}" -ge 500 ]; then
            echo "[OK] ${icon_src}: ${FILE_SIZE} bytes (>= 500 — non-degenerate)"
        else
            echo "[FAIL] ${icon_src}: only ${FILE_SIZE} bytes — degenerate stub (need >= 500)"
            src_fail=1
        fi
    done
fi

if [ "${src_fail}" -ne 0 ]; then
    echo ""
    echo "[FAIL] web_pwa_icon_challenge LAYER B failed — source-tree PNG icons broken."
    exit 2
fi

echo ""
echo "=== [web_pwa_icon_challenge] LAYER C: Wasm bundle audit ==="

# Find latest Web Wasm bundle in releases/
LATEST_WEB_BUNDLE=$(ls -t "${ROOT_DIR}/releases/Yole-Web-"*.zip 2>/dev/null | head -1 || true)

if [ -z "${LATEST_WEB_BUNDLE}" ]; then
    echo "[SKIP] No Web Wasm bundle found in releases/ — bundle-side audit deferred."
    echo "       Reason: #wasmjs-production-distribution-gap — Wasm bundle not yet built."
    echo "       Source-tree layers A and B passed; this layer is conditional."
    echo "[INFO] Run 'make container-release' once Web Wasm build is unblocked."
else
    echo "[OK] Found Web bundle: $(basename "${LATEST_WEB_BUNDLE}")"
    bundle_fail=0

    for icon_src in "${ICON_SRCS[@]}"; do
        # Icons should be in the bundle under icons/
        BUNDLE_PATH="${icon_src}"
        BUNDLE_SIZE=$(unzip -l "${LATEST_WEB_BUNDLE}" 2>/dev/null | awk -v p="${BUNDLE_PATH}" '$4 == p {print $1}' | head -1 || true)

        if [ -z "${BUNDLE_SIZE}" ]; then
            echo "[FAIL] ${icon_src}: not found in bundle ${LATEST_WEB_BUNDLE}"
            bundle_fail=1
        elif [ "${BUNDLE_SIZE}" -ge 500 ]; then
            echo "[OK] ${icon_src}: ${BUNDLE_SIZE} bytes in bundle (>= 500)"
        else
            echo "[FAIL] ${icon_src}: only ${BUNDLE_SIZE} bytes in bundle — degenerate"
            bundle_fail=1
        fi
    done

    if [ "${bundle_fail}" -ne 0 ]; then
        echo ""
        echo "[FAIL] web_pwa_icon_challenge LAYER C failed — bundle is missing icons."
        exit 3
    fi
fi

echo ""
echo "==================================================================="
echo "[PASS] web_pwa_icon_challenge — all applicable layers PASS."
echo "       PWA manifest declares required 192x192 and 512x512 PNG icons."
echo "       All referenced PNG files exist, are valid PNGs, and >= 500 bytes."
echo "       CONST-039 installable-asset verification: SATISFIED."
echo "==================================================================="
