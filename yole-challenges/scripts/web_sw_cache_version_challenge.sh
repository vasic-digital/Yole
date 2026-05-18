#!/bin/bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# iter-89 anti-bluff challenge — web_sw_cache_version.
#
# Catches the operator-reported defect class: live URL spins endlessly
# on the splash because the deployed Service Worker's CACHE_VERSION
# string doesn't match the current build's versionName, so users who
# previously visited the site continue to be served the old (broken)
# bundle from the SW's HTTP cache forever.
#
# Forensic anchor: through v2.0.0 / v2.0.1 / v2.0.2 / v2.0.3 the SW
# shipped with a hardcoded `CACHE_NAME = 'yole-cache-v1'` that was
# never bumped. Every deploy installed a new SW (Cache-Control:no-store
# on /service-worker.js ensured fresh fetch), but the new SW saw an
# existing cache named `yole-cache-v1`, skipped re-installation of the
# app shell, and served the FOUR-VERSION-OLD broken bundle to returning
# users indefinitely. Every gate up to iter-88 PASSed because fresh
# Puppeteer sessions don't have a prior SW cache — they always got the
# new bundle on first visit. The operator's browser, which had been
# visiting since v2.0.0, was stuck.
#
# Layers:
#   STATIC: confirms webApp/src/wasmJsMain/resources/service-worker.js
#     declares CACHE_VERSION matching androidApp/build.gradle.kts
#     versionName (canonical version per iter-81). A CACHE_VERSION pin
#     that doesn't move with versionName is the bug class.
#   RUNTIME: fetches https://yole-app.web.app/service-worker.js (or
#     YOLE_WEB_URL override) and asserts the deployed SW JS contains
#     the same CACHE_VERSION string. If a redeploy was skipped or
#     Firebase Hosting served a cached SW, this catches it.
#
# Exit codes:
#   0 = static + runtime both PASS
#   1 = static drift — SW CACHE_VERSION doesn't match versionName
#   2 = runtime FAIL — deployed SW serves a stale CACHE_VERSION
#   3 = environmental (curl missing) — SKIP-OK
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$REPO_ROOT"

SW_SOURCE="webApp/src/wasmJsMain/resources/service-worker.js"
TARGET_URL="${YOLE_WEB_URL:-https://yole-app.web.app}"

# ---------------- STATIC LAYER ----------------

echo "=== [web_sw_cache_version_challenge] static layer ==="

if [[ ! -f "$SW_SOURCE" ]]; then
    echo "[FAIL] $SW_SOURCE missing"
    exit 1
fi

ANDROID_VERSION=$(grep -E '^\s*versionName\s*=' androidApp/build.gradle.kts \
    | head -1 | sed -E 's/.*"([^"]+)".*/\1/' | tr -d ' \t')
if [[ -z "$ANDROID_VERSION" ]]; then
    echo "[FAIL] could not extract versionName from androidApp/build.gradle.kts"
    exit 1
fi
echo "[OK] canonical version: $ANDROID_VERSION"

# Extract CACHE_VERSION from SW source
SW_VERSION=$(grep -E "^const CACHE_VERSION = " "$SW_SOURCE" \
    | head -1 | sed -E "s/.*'([^']+)'.*/\1/")
if [[ -z "$SW_VERSION" ]]; then
    echo "[FAIL] could not extract CACHE_VERSION from $SW_SOURCE"
    echo "       SW must declare \`const CACHE_VERSION = '<version>';\`"
    exit 1
fi
echo "[INFO] SW source CACHE_VERSION: $SW_VERSION"

if [[ "$SW_VERSION" != "$ANDROID_VERSION" ]]; then
    echo "[FAIL] SW CACHE_VERSION '$SW_VERSION' != canonical versionName '$ANDROID_VERSION'"
    echo
    echo "Per the iter-89 forensic anchor, the SW CACHE_VERSION MUST be"
    echo "bumped to the current versionName on every release. Otherwise,"
    echo "users who previously visited the site will be served the old"
    echo "bundle from their SW cache forever — every release ships a new"
    echo "Wasm bundle but the cached one is returned by the SW's"
    echo "cache-first (or network-first-but-cache-still-stale) strategy."
    exit 1
fi
echo "[OK] SW CACHE_VERSION matches canonical versionName: $SW_VERSION"

# ---------------- RUNTIME LAYER ----------------

echo
echo "=== [web_sw_cache_version_challenge] runtime layer ==="

if ! command -v curl >/dev/null 2>&1; then
    echo "[SKIP-OK] curl not installed; runtime SW-version verification deferred"
    exit 3
fi

echo "[INFO] fetching $TARGET_URL/service-worker.js"
DEPLOYED_SW=$(curl -s --max-time 30 "$TARGET_URL/service-worker.js" 2>&1)
if [[ -z "$DEPLOYED_SW" ]]; then
    echo "[SKIP-OK] could not fetch deployed SW (network/target unreachable)"
    exit 3
fi

# pipefail-safe extraction: grep returns 1 on no-match which would kill us
# silently under set -euo pipefail. The `|| true` guards keep us alive so
# the explicit empty-check below produces a real FAIL with a useful message.
DEPLOYED_VERSION=$(echo "$DEPLOYED_SW" | grep -E "^const CACHE_VERSION = " 2>/dev/null \
    | head -1 | sed -E "s/.*'([^']+)'.*/\1/" 2>/dev/null || true)
if [[ -z "$DEPLOYED_VERSION" ]]; then
    echo "[FAIL] deployed SW at $TARGET_URL/service-worker.js does not declare CACHE_VERSION"
    echo "       The deployed SW may pre-date iter-89's version-aware cache strategy."
    exit 2
fi
echo "[INFO] deployed SW CACHE_VERSION: $DEPLOYED_VERSION"

if [[ "$DEPLOYED_VERSION" != "$ANDROID_VERSION" ]]; then
    echo "[FAIL] deployed SW CACHE_VERSION '$DEPLOYED_VERSION' != canonical versionName '$ANDROID_VERSION'"
    echo
    echo "The deployed Service Worker is for an older version than the"
    echo "current source. Either the deploy was skipped, or Firebase"
    echo "Hosting cached an older SW. Returning users will be served the"
    echo "stale bundle until the SW deploys correctly."
    exit 2
fi
echo "[OK] deployed SW CACHE_VERSION matches canonical: $DEPLOYED_VERSION"

# Also verify the SW uses network-first or has a per-build cache name.
# Cache-first with a static name = the iter-89 root cause.
if echo "$DEPLOYED_SW" | grep -q "networkFirst"; then
    echo "[OK] deployed SW uses network-first strategy (fresh content wins)"
else
    echo "[WARN] deployed SW may use cache-first — fresh content may not displace cached content"
fi

echo
echo "PASS: web_sw_cache_version_challenge — SW source + deployed both at $ANDROID_VERSION."
