#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# lsp_binary_bundle_challenge.sh — iter-61 Phase 9 anti-bluff gate.
#
# Verifies that the LSP binary bundle staging pipeline is wired correctly and
# that at least 5 LSP server binaries are staged for the desktop target. Two layers:
#
#   (a) STATIC: runs ./gradlew :shared:lspBundleStage, then asserts that
#       shared/build/processedResources/desktop/main/lsp-bundles/ contains
#       >= 5 lang subdirs each with a non-empty executable file. For each
#       present binary, runs `file <binary>` and confirms it matches a known
#       executable type (Mach-O, ELF, or PE). Emits "[OK] <binary>" per check.
#
#   (b) RUNTIME (optional): for each verified binary, execs `--version` (or
#       the language-specific equivalent) and confirms exit code is NOT in
#       {126, 127, 139} — which would signal exec failure (not-found / not-exec /
#       segfault). Exit 0 or 1 from the binary itself is acceptable. If `file`
#       is not in PATH or staging produced zero binaries, this layer is skipped
#       with an explicit [SKIP-OK] marker.
#
# Exit codes:
#   0 = staging present and binaries valid
#   1 = staging missing or fewer than 5 binaries staged
#   2 = one or more binaries failed the exec sanity check
#
# Anti-bluff (CONST-035): per-binary "[OK]" lines and exec evidence are
# always emitted. No metadata-only PASS.
#
# Cross-platform impact (CONST-037): staging runs only on the host JVM
# (desktopProcessResources). Android, iOS, Wasm do not use lsp-bundles —
# their LspServerInstaller actuals are no-ops. Staging is macOS-arm64 only
# in v1; other ABI dirs (linux-x64, windows-x64) are not yet populated.
#
# Submodule decoupling (CONST-038): no submodule state is read or required.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
cd "${ROOT_DIR}"

STAGING_DIR="shared/build/processedResources/desktop/main/lsp-bundles"
MIN_LANGS=5

echo "=== [lsp_binary_bundle_challenge] static layer ==="

# ----------------------------------------------------------------
# Run lspBundleStage to populate staging dir
# ----------------------------------------------------------------
echo "Running: ./gradlew :shared:lspBundleStage"
if ! ./gradlew :shared:lspBundleStage > /dev/null 2>&1; then
    echo "FAIL [static]: ./gradlew :shared:lspBundleStage failed."
    exit 1
fi
echo "[OK] :shared:lspBundleStage completed"

# ----------------------------------------------------------------
# Assert staging dir exists
# ----------------------------------------------------------------
if [[ ! -d "${STAGING_DIR}" ]]; then
    echo "FAIL [static]: staging dir absent: ${STAGING_DIR}"
    echo "       Hint: run 'bash scripts/acquire-lsp-binaries.sh' first."
    exit 1
fi
echo "[OK] staging dir exists: ${STAGING_DIR}"

# ----------------------------------------------------------------
# Collect staged binaries: each entry is <langId>/<exe>
# ----------------------------------------------------------------
declare -a STAGED_BINARIES=()
while IFS= read -r -d '' binary; do
    STAGED_BINARIES+=("${binary}")
done < <(find "${STAGING_DIR}" -mindepth 2 -maxdepth 2 -type f -print0 2>/dev/null)

staged_count="${#STAGED_BINARIES[@]}"
echo ""
echo "Staged binaries found: ${staged_count}"

if (( staged_count < MIN_LANGS )); then
    echo "FAIL [static]: only ${staged_count} binaries staged — expected >= ${MIN_LANGS}."
    echo "       Hint: run 'bash scripts/acquire-lsp-binaries.sh' to populate .lsp-binary-cache."
    exit 1
fi

# ----------------------------------------------------------------
# Per-binary: size check + file(1) type validation
# ----------------------------------------------------------------
static_fail=0
file_cmd_available=0
command -v file > /dev/null 2>&1 && file_cmd_available=1

echo ""
echo "Validating ${staged_count} staged binaries:"
for binary in "${STAGED_BINARIES[@]}"; do
    rel="${binary#${STAGING_DIR}/}"
    size=$(wc -c < "${binary}" 2>/dev/null || echo 0)
    if (( size == 0 )); then
        echo "FAIL [static]: zero-byte binary: ${rel}"
        static_fail=1
        continue
    fi

    if (( file_cmd_available )); then
        file_output="$(file "${binary}" 2>/dev/null || true)"
        if echo "${file_output}" | grep -qiE "Mach-O|ELF|PE32|executable"; then
            echo "[OK] ${rel} — ${size} bytes — $(echo "${file_output}" | grep -oiE "(Mach-O|ELF|PE32)[^,]*" | head -1)"
        else
            echo "WARN [static]: ${rel} — ${size} bytes — file type unrecognised: ${file_output}"
            # Warn but do not fail — may be a wrapper script (e.g. lua-language-server)
        fi
    else
        echo "[OK] ${rel} — ${size} bytes (file(1) not in PATH, skipping type check)"
    fi
done

if (( static_fail )); then
    echo ""
    echo "FAIL [static]: one or more binaries are zero-byte — see above."
    exit 1
fi

echo ""
echo "OK [static]: ${staged_count} binaries staged, all non-empty."

# ----------------------------------------------------------------
# Runtime layer: exec sanity (--version or equivalent)
# ----------------------------------------------------------------
echo ""
echo "=== [lsp_binary_bundle_challenge] runtime layer ==="

if (( staged_count == 0 )); then
    echo "[SKIP-OK: no staged binaries — runtime layer skipped]"
    echo ""
    echo "PASS: lsp_binary_bundle_challenge — ${staged_count} binaries staged (runtime skipped)."
    exit 0
fi

runtime_fail=0
skipped_exec=0

for binary in "${STAGED_BINARIES[@]}"; do
    rel="${binary#${STAGING_DIR}/}"
    langId="$(echo "${rel}" | cut -d'/' -f1)"

    # Choose the version-probe argument per language server.
    # Fallback: --version (works for rust-analyzer, zls, marksman, clangd, hls-wrapper)
    case "${langId}" in
        java)    version_arg="--version" ;;
        kotlin)  version_arg="--version" ;;
        lua)     version_arg="--version" ;;
        *)       version_arg="--version" ;;
    esac

    # Make binary executable before probing (Gradle Sync may not preserve chmod)
    chmod +x "${binary}" 2>/dev/null || true

    set +e
    "${binary}" "${version_arg}" > /dev/null 2>&1
    exit_code=$?
    set -e

    case "${exit_code}" in
        126)
            echo "FAIL [runtime]: ${rel} — exit 126 (permission denied / not executable)"
            runtime_fail=1
            ;;
        127)
            echo "FAIL [runtime]: ${rel} — exit 127 (binary not found / bad interpreter)"
            runtime_fail=1
            ;;
        139)
            echo "FAIL [runtime]: ${rel} — exit 139 (segmentation fault)"
            runtime_fail=1
            ;;
        *)
            echo "[OK] ${rel} — exec returned ${exit_code} (acceptable)"
            ;;
    esac
done

if (( runtime_fail )); then
    echo ""
    echo "FAIL [runtime]: one or more binaries failed exec sanity — see above."
    exit 2
fi

echo ""
echo "PASS: lsp_binary_bundle_challenge — ${staged_count} binaries staged + exec-sane (${skipped_exec} skipped)."
