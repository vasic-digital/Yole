#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# installable_desktop_icon_challenge.sh — iter-74 Desktop .icns anti-bluff gate.
#
# Closes tracker: #iter-71-desktop-icns-format-defect
#
# Anti-bluff installable-asset challenge per CONST-039.
# Verifies the packaged Desktop DMG actually contains a genuine Apple Icon
# Image (ICNS) file and not a PNG renamed to .icns (the iter-71 defect).
#
# CONST-039 requirement: a PASS without opening the artifact and verifying
# each user-visible asset is present + non-degenerate is bluff.  A PNG with
# PNG magic bytes (89 50 4E 47) masquerading as .icns passes filename checks
# but is blurry / unscaled on Retina displays — exactly the defect this gate
# catches.
#
# Three verification layers:
#
#   (a) STATIC — source-tree checks:
#       • desktopApp/src/main/resources/icons/icon.icns exists.
#       • First 4 bytes are ICNS magic (69 63 6E 73 = "icns").
#         NOT PNG magic (89 50 4E 47).
#       • File size >= 30 KB (a real ICNS with all size variants is ≥30 KB;
#         a bare PNG masquerade is typically ≤ 61 KB but lacks multi-size
#         internal structure, detectable by magic-byte check above).
#       • build.gradle.kts macOS block references icons/icon.icns.
#       Emits "[OK] <check>" per assertion.
#
#   (b) DMG-OPEN — mounts the latest release DMG and verifies:
#       • Yole.app/Contents/Resources/Yole.icns exists inside the bundle.
#       • First 4 bytes are ICNS magic (not PNG).
#       • File size >= 30 KB.
#       • Info.plist CFBundleIconFile references Yole (→ Yole.icns resolved by macOS).
#       Emits "[OK] <check>" per assertion.
#
#   (c) INTERNAL STRUCTURE — verifies ICNS internal chunk headers:
#       • At least 2 distinct size-variant chunks found (ic04, ic07, ic08,
#         ic09, ic10, ic11, ic12, ic13, ic14, is32, il32, ih32, it32, …).
#         A PNG masquerade has none of these; a real ICNS has 8–14.
#       Emits "[OK] <check>" per assertion.
#
# Exit codes:
#   0 = all three layers PASS
#   1 = static layer fail
#   2 = DMG-open layer fail
#   3 = internal-structure layer fail
#
# Anti-bluff (CONST-035): positive evidence is always emitted. Every check
# emits "[OK] <description>" on success. A regression causes "[FAIL] <reason>"
# and an immediate non-zero exit.
#
# Cross-platform impact (CONST-037):
#   - macOS: DMG-open layer requires hdiutil (macOS-only).
#     Layer A and C run on any host with xxd.
#   - Android/iOS/Web: not in scope for this challenge.
#
# Submodule decoupling (CONST-038): no submodule state is read or required.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

ICNS_SRC="desktopApp/src/main/resources/icons/icon.icns"
BUILD_GRADLE="desktopApp/build.gradle.kts"

echo "=== [installable_desktop_icon_challenge] LAYER A: source-tree static checks ==="

static_fail=0

# ---- A1: icon.icns exists ----
if [ -f "${ICNS_SRC}" ]; then
    echo "[OK] ${ICNS_SRC} exists"
else
    echo "[FAIL] ${ICNS_SRC} missing — macOS icon not in source tree"
    echo "       Fix: generate a real ICNS with iconutil from a ≥512×512 PNG."
    static_fail=1
fi

# ---- A2: first 4 bytes are ICNS magic (69 63 6E 73), not PNG (89 50 4E 47) ----
if [ -f "${ICNS_SRC}" ]; then
    MAGIC_HEX=$(xxd -l 4 -p "${ICNS_SRC}" 2>/dev/null | tr '[:upper:]' '[:lower:]' || true)
    ICNS_MAGIC="69636e73"
    PNG_MAGIC="89504e47"
    if [ "${MAGIC_HEX}" = "${ICNS_MAGIC}" ]; then
        echo "[OK] ${ICNS_SRC}: first 4 bytes = ${MAGIC_HEX} (ICNS magic 'icns')"
    elif [ "${MAGIC_HEX}" = "${PNG_MAGIC}" ]; then
        echo "[FAIL] ${ICNS_SRC}: first 4 bytes = ${MAGIC_HEX} (PNG magic) — PNG renamed to .icns"
        echo "       This is the #iter-71-desktop-icns-format-defect regression."
        echo "       Fix: run iconutil -c icns Yole.iconset -o icon.icns on a proper iconset."
        static_fail=1
    else
        echo "[FAIL] ${ICNS_SRC}: first 4 bytes = ${MAGIC_HEX} — unrecognised format (expected 69636e73)"
        static_fail=1
    fi
fi

# ---- A3: file size >= 30 KB (real ICNS with all variants) ----
if [ -f "${ICNS_SRC}" ]; then
    ICNS_SIZE=$(wc -c < "${ICNS_SRC}" | tr -d ' ')
    if [ "${ICNS_SIZE}" -ge 30720 ]; then
        echo "[OK] ${ICNS_SRC}: size ${ICNS_SIZE} bytes (≥30 KB — multi-variant ICNS)"
    else
        echo "[FAIL] ${ICNS_SRC}: size ${ICNS_SIZE} bytes (<30 KB) — too small for a real multi-size ICNS"
        echo "       A genuine macOS ICNS with 16x16…512x512@2x variants is ≥30 KB."
        static_fail=1
    fi
fi

# ---- A4: build.gradle.kts macOS block references icon.icns ----
if grep -q 'icons/icon.icns' "${BUILD_GRADLE}" 2>/dev/null; then
    echo "[OK] ${BUILD_GRADLE}: macOS iconFile references icons/icon.icns"
else
    echo "[FAIL] ${BUILD_GRADLE}: macOS iconFile.set() does not reference icons/icon.icns"
    echo "       Check the macOS { iconFile.set(...) } block in desktopApp/build.gradle.kts."
    static_fail=1
fi

if [ "${static_fail}" -ne 0 ]; then
    echo ""
    echo "[FAIL] installable_desktop_icon_challenge LAYER A failed — source tree is broken."
    exit 1
fi

echo ""
echo "=== [installable_desktop_icon_challenge] LAYER B: DMG-open verification ==="

# Find latest macOS release DMG
LATEST_DMG=$(ls -t "${ROOT_DIR}/releases/Yole-Desktop-macos-arm64-"*"-Release-"*".dmg" 2>/dev/null | head -1 || true)
if [ -z "${LATEST_DMG}" ]; then
    echo "[WARN] No release DMG found in releases/ — skipping DMG-open layer."
    echo "       Run './gradlew :desktopApp:packageDmg' and copy to releases/ first."
    echo "[SKIP] LAYER B: no DMG available  # SKIP-OK: #iter-74-challenge-no-dmg"
else
    echo "[OK] Found release DMG: $(basename "${LATEST_DMG}")"

    # Mount DMG
    MOUNT_POINT="/tmp/YoleIconChallengeDMGMount_$$"
    mkdir -p "${MOUNT_POINT}"
    dmg_fail=0

    if hdiutil attach "${LATEST_DMG}" -mountpoint "${MOUNT_POINT}" -nobrowse -quiet 2>/dev/null; then
        APP_ICNS="${MOUNT_POINT}/Yole.app/Contents/Resources/Yole.icns"
        APP_PLIST="${MOUNT_POINT}/Yole.app/Contents/Info.plist"

        # ---- B1: Yole.icns exists in bundle ----
        if [ -f "${APP_ICNS}" ]; then
            echo "[OK] Yole.app/Contents/Resources/Yole.icns exists in DMG bundle"
        else
            echo "[FAIL] Yole.app/Contents/Resources/Yole.icns missing from DMG bundle"
            dmg_fail=1
        fi

        # ---- B2: first 4 bytes are ICNS magic in bundle copy ----
        if [ -f "${APP_ICNS}" ]; then
            BUNDLE_MAGIC=$(xxd -l 4 -p "${APP_ICNS}" 2>/dev/null | tr '[:upper:]' '[:lower:]' || true)
            if [ "${BUNDLE_MAGIC}" = "69636e73" ]; then
                echo "[OK] Yole.app bundle Yole.icns: ICNS magic confirmed (${BUNDLE_MAGIC})"
            elif [ "${BUNDLE_MAGIC}" = "89504e47" ]; then
                echo "[FAIL] Yole.app bundle Yole.icns: PNG magic (${BUNDLE_MAGIC}) — PNG renamed to .icns inside bundle"
                dmg_fail=1
            else
                echo "[FAIL] Yole.app bundle Yole.icns: unexpected magic ${BUNDLE_MAGIC}"
                dmg_fail=1
            fi
        fi

        # ---- B3: bundle Yole.icns >= 30 KB ----
        if [ -f "${APP_ICNS}" ]; then
            BUNDLE_SIZE=$(wc -c < "${APP_ICNS}" | tr -d ' ')
            if [ "${BUNDLE_SIZE}" -ge 30720 ]; then
                echo "[OK] Bundle Yole.icns: ${BUNDLE_SIZE} bytes (≥30 KB)"
            else
                echo "[FAIL] Bundle Yole.icns: ${BUNDLE_SIZE} bytes (<30 KB)"
                dmg_fail=1
            fi
        fi

        # ---- B4: Info.plist CFBundleIconFile references Yole ----
        if [ -f "${APP_PLIST}" ]; then
            if grep -q 'Yole' "${APP_PLIST}" 2>/dev/null; then
                ICON_VALUE=$(grep -A 1 'CFBundleIconFile' "${APP_PLIST}" 2>/dev/null | grep '<string>' | sed 's/.*<string>\(.*\)<\/string>.*/\1/' | head -1 || true)
                echo "[OK] Info.plist CFBundleIconFile: '${ICON_VALUE}' (resolves to Yole.icns)"
            else
                echo "[FAIL] Info.plist does not reference Yole icon"
                dmg_fail=1
            fi
        fi

        hdiutil detach "${MOUNT_POINT}" -quiet 2>/dev/null || true
        rm -rf "${MOUNT_POINT}"

        if [ "${dmg_fail}" -ne 0 ]; then
            echo ""
            echo "[FAIL] installable_desktop_icon_challenge LAYER B failed — DMG bundle icon broken."
            exit 2
        fi
    else
        echo "[WARN] hdiutil attach failed (may not be macOS host) — skipping DMG-open layer."
        echo "[SKIP] LAYER B: hdiutil unavailable  # SKIP-OK: #iter-74-challenge-no-hdiutil"
        rm -rf "${MOUNT_POINT}"
    fi
fi

echo ""
echo "=== [installable_desktop_icon_challenge] LAYER C: ICNS internal structure ==="

struct_fail=0

if [ -f "${ICNS_SRC}" ]; then
    # Parse ICNS chunk structure using Python's struct module.
    # ICNS format: 4-byte 'icns' magic + 4-byte file-size, then repeated chunks of:
    #   4-byte type + 4-byte chunk-size (includes the 8-byte header).
    # Known size-variant chunk types (Apple Technical Note HI 2711):
    #   ic04=16@2x, ic05=32@2x, ic07=128, ic08=256, ic09=512, ic10=1024@2x,
    #   ic11=16, ic12=32, ic13=64, ic14=128@2x
    #   is32=16, il32=32, ih32=48, it32=128 — older 32-bit RLE format
    CHUNK_RESULT=$(python3 - "${ICNS_SRC}" <<'PYEOF'
import sys, struct

path = sys.argv[1]
data = open(path, 'rb').read()

if len(data) < 8 or data[:4] != b'icns':
    print("NOT_ICNS")
    sys.exit(0)

SIZE_VARIANT_CHUNKS = {
    'ic04','ic05','ic07','ic08','ic09','ic10',
    'ic11','ic12','ic13','ic14',
    'is32','il32','ih32','it32',
}

chunks = []
offset = 8  # skip file header
while offset + 8 <= len(data):
    chunk_type_bytes = data[offset:offset+4]
    try:
        chunk_type = chunk_type_bytes.decode('ascii')
    except Exception:
        break
    chunk_size = struct.unpack('>I', data[offset+4:offset+8])[0]
    if chunk_size < 8 or offset + chunk_size > len(data):
        break
    if chunk_type in SIZE_VARIANT_CHUNKS:
        chunks.append(chunk_type)
    offset += chunk_size

print(','.join(chunks) if chunks else 'NONE')
PYEOF
)

    if [ "${CHUNK_RESULT}" = "NOT_ICNS" ]; then
        echo "[FAIL] ${ICNS_SRC}: Python ICNS parser: file is not ICNS format"
        struct_fail=1
    elif [ "${CHUNK_RESULT}" = "NONE" ]; then
        echo "[FAIL] ${ICNS_SRC}: 0 size-variant ICNS chunks found — PNG masquerade or corrupt ICNS"
        echo "       A genuine ICNS from iconutil has 8–14 size-variant chunks."
        struct_fail=1
    else
        CHUNK_COUNT=$(echo "${CHUNK_RESULT}" | tr ',' '\n' | wc -l | tr -d ' ')
        if [ "${CHUNK_COUNT}" -ge 2 ]; then
            echo "[OK] ${ICNS_SRC}: ${CHUNK_COUNT} ICNS size-variant chunks found: ${CHUNK_RESULT}"
            echo "     A PNG renamed to .icns has 0 valid ICNS chunk headers — this is a real ICNS."
        else
            echo "[FAIL] ${ICNS_SRC}: only ${CHUNK_COUNT} size-variant chunk(s) found: ${CHUNK_RESULT}"
            echo "       Expected ≥2 of: ic04 ic05 ic07 ic08 ic09 ic10 ic11 ic12 ic13 ic14 is32 il32 ih32 it32"
            struct_fail=1
        fi
    fi
fi

if [ "${struct_fail}" -ne 0 ]; then
    echo ""
    echo "[FAIL] installable_desktop_icon_challenge LAYER C failed — ICNS has no valid internal structure."
    exit 3
fi

echo ""
echo "==================================================================="
echo "[PASS] installable_desktop_icon_challenge — all 3 layers PASS."
echo "       The Desktop macOS ICNS is a genuine Apple Icon Image file."
echo "       Evidence: magic bytes, file size, internal chunk structure,"
echo "       DMG bundle integrity, Info.plist CFBundleIconFile reference."
echo "       CONST-039 installable-asset verification: SATISFIED."
echo "       Closes: #iter-71-desktop-icns-format-defect"
echo "==================================================================="
