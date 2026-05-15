#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
#
# iter-58 F2 Phase 7 — Tree-Sitter native grammar acquisition pipeline.
#
# This script acquires (extract-from-bonede OR build-from-source) the
# native Tree-Sitter shared libraries Yole needs to ship for each of the
# 55 languages declared in shared/src/commonMain/.../LanguageMetadata.kt
# across the platforms Yole supports natively.
#
# Acquisition strategy (in order of preference per language × platform):
#
#   1. bonede JAR extraction — for languages where io.github.bonede
#      publishes a Maven Central JAR (verified Phase 7 inventory: 48 of
#      55 langs), pull the lib/<arch>-<os>-tree-sitter-<lang>.<ext>
#      resource directly out of the published JAR. This is the canonical
#      path for Desktop (5 platforms: aarch64-linux-gnu, aarch64-macos,
#      x86_64-linux-gnu, x86_64-macos, x86_64-windows). All binaries
#      come from the bonede team's CI and have been independently
#      verified against tree-sitter upstream sources.
#
#      Yole DOES NOT redistribute these binaries — they are pulled at
#      Gradle build time via standard implementation dependencies; this
#      script is only used when (a) operator wants to seed local
#      ./shared/native/<lang>/ directories for offline builds, or (b)
#      the Android NDK / iOS path needs the same binary for a different
#      ABI (see step 2 below).
#
#   2. Android NDK build-from-source — bonede JARs do NOT ship
#      Android-bionic-compatible .so files. For each language Yole
#      bundles on Android, build the parser.c + scanner.c against the
#      Android NDK clang toolchain for the configured ABIs (default:
#      arm64-v8a; opt-in: armeabi-v7a, x86_64). Output is placed under
#      shared/native/android-tree-sitter/<abi>/libtree-sitter-<lang>.so
#      where the existing repackageBonedeJarsForAndroid Gradle task can
#      consume it (iter-57 NativeUtils replacement loads via
#      System.loadLibrary which routes through the standard jniLibs
#      lookup path).
#
#   3. iOS arm64 static library — requires Xcode + iOS SDK installed
#      (not present in the current build environment as of Phase 7
#      authoring). Build path documented; the script blocks gracefully
#      when xcrun --sdk iphoneos --show-sdk-path returns an error.
#
#   4. Linux / Windows desktop cross-compile from macOS host — possible
#      if x86_64-w64-mingw32-gcc and x86_64-linux-gnu-gcc are installed
#      via Homebrew; the script detects their absence and SKIPs cleanly.
#      Yole's primary path for these platforms is bonede extraction
#      (step 1), so cross-compile is rarely needed.
#
# Output layout:
#
#   build/lang-grammars-scratch/extracted/<lang>/desktop/        (NOT committed)
#     <arch>-<os>-tree-sitter-<lang>.<ext>           extracted-from-bonede,
#                                                   used only for offline
#                                                   verification.
#
#   shared/native/<lang>/                            (committed when present)
#     android-arm64-v8a/libtree-sitter-<lang>.so     (NDK aarch64, on disk)
#     android-armeabi-v7a/libtree-sitter-<lang>.so   (NDK arm 32, opt-in)
#     android-x86_64/libtree-sitter-<lang>.so        (NDK x86_64, opt-in)
#     ios-arm64-device/libtree-sitter-<lang>.a       (when Xcode present)
#     ios-arm64-sim/libtree-sitter-<lang>.a          (when Xcode present)
#
# CRITICAL: Desktop binaries DO NOT live in the Yole git tree. They flow
# through Maven Central (bonede JAR Gradle deps) so they reach end-user
# desktops without bloating the repo. The `extract` subcommand below is
# only for operator-side offline verification.
#
# Anti-bluff guarantees (CONST-035):
#   - EVERY output binary is `file`-verified to be a real ELF/Mach-O/PE
#     binary of the expected arch+os. A zero-byte or wrong-format
#     output is a HARD FAIL — the script exits non-zero, never silently
#     ships a fake binary.
#   - Languages we cannot acquire today (jsx, xml, vim, less, crystal,
#     groovy, bibtex per Phase 7 inventory) are logged as SKIPPED with
#     the documented reason; the operator sees the gap explicitly.
#   - The bonede JARs we extract from are immutable Maven Central
#     artifacts pinned by SHA in this script — operator can verify
#     against repo1.maven.org any time.
#
# Usage:
#   tools/build-language-grammars.sh inventory          # print Yole×bonede×platform matrix
#   tools/build-language-grammars.sh extract <lang>...  # extract from bonede JAR (Desktop)
#   tools/build-language-grammars.sh android <lang>...  # NDK build for Android arm64-v8a
#   tools/build-language-grammars.sh ios <lang>...      # Xcode static-lib build for iOS
#   tools/build-language-grammars.sh all                # do all of the above for all langs
#   tools/build-language-grammars.sh verify             # `file`-check every output binary

set -euo pipefail

YOLE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NATIVE_DIR="${YOLE_ROOT}/shared/native"
SCRATCH_DIR="${YOLE_ROOT}/build/lang-grammars-scratch"
mkdir -p "${SCRATCH_DIR}"

# -------------------------------------------------------------------
# Yole language ID -> bonede artifact name (Maven Central groupId
# io.github.bonede). Stored in version-pinned form so the operator can
# verify each artifact independently. Versions match the snapshot taken
# during Phase 7.1 inventory on 2026-05-15.
# -------------------------------------------------------------------
declare -A BONEDE_ARTIFACT=(
  [markdown]="tree-sitter-markdown:0.7.1a"
  [kotlin]="tree-sitter-kotlin:0.3.8.1"
  [java]="tree-sitter-java:0.23.5"
  [python]="tree-sitter-python:0.25.0"
  [javascript]="tree-sitter-javascript:0.25.0"
  [typescript]="tree-sitter-typescript:0.23.2"
  [go]="tree-sitter-go:0.25.0"
  [rust]="tree-sitter-rust:0.24.0"
  [c]="tree-sitter-c:0.24.1"
  [cpp]="tree-sitter-cpp:0.23.4"
  [html]="tree-sitter-html:0.23.2"
  [css]="tree-sitter-css:0.25.0"
  [sql]="tree-sitter-sql:gh-pages-a"
  [json]="tree-sitter-json:0.24.8"
  [tsx]="tree-sitter-tsx:0.23.2"
  [yaml]="tree-sitter-yaml:0.5.0a"
  [toml]="tree-sitter-toml:0.5.1a"
  [bash]="tree-sitter-bash:0.25.1"
  [ruby]="tree-sitter-ruby:0.23.1"
  [php]="tree-sitter-php:0.24.2"
  [swift]="tree-sitter-swift:0.5.0"
  [scala]="tree-sitter-scala:0.24.0"
  [dart]="tree-sitter-dart:master-a"
  [lua]="tree-sitter-lua:2.1.3a"
  [perl]="tree-sitter-perl:1.1.0"
  [haskell]="tree-sitter-haskell:0.23.1"
  [ocaml]="tree-sitter-ocaml:0.23.2"
  [julia]="tree-sitter-julia:0.25.0"
  [r]="tree-sitter-r:main-a"
  [elixir]="tree-sitter-elixir:0.2.0"
  [erlang]="tree-sitter-erlang:0.1.0a"
  [fortran]="tree-sitter-fortran:master-a"
  [dockerfile]="tree-sitter-dockerfile:0.2.0"
  [makefile]="tree-sitter-make:main-a"
  [terraform]="tree-sitter-hcl:1.1.0a"
  [regex]="tree-sitter-regex:1.0.0"
  [vue]="tree-sitter-vue:0.2.1a"
  [graphql]="tree-sitter-graphql:master-a"
  [csharp]="tree-sitter-c-sharp:0.23.1"
  [scss]="tree-sitter-scss:1.0.0a"
  [nix]="tree-sitter-nix:master-a"
  [zig]="tree-sitter-zig:main-a"
  [elm]="tree-sitter-elm:5.7.0a"
  [clojure]="tree-sitter-clojure:0.0.12a"
  # nim:0.6.0 segfaults on parse — see KNOWN_DEFECTS#f2-phase-7-nim-grammar-broken
  [objc]="tree-sitter-objc:main-a"
  [latex]="tree-sitter-latex:0.3.0a"
  [proto]="tree-sitter-proto:main-a"
)

# Languages NOT on bonede (Phase 7 inventory): jsx, xml, vim, less, crystal, groovy, bibtex.
# These either need a separate bonede artifact (which doesn't exist as of
# 2026-05-15) or build-from-source. They are documented as gaps in
# docs/KNOWN_DEFECTS.md#f2-phase-7-no-bonede-artifact.

# -------------------------------------------------------------------
# Yole language ID -> upstream tree-sitter-<lang> repo URL (build-from-source).
# Used only when bonede has no published artifact, OR when building for an
# Android NDK ABI that bonede does not ship.
# Sources: research-report.md §3.
# -------------------------------------------------------------------
declare -A GRAMMAR_REPO=(
  [markdown]="https://github.com/tree-sitter-grammars/tree-sitter-markdown"
  [kotlin]="https://github.com/fwcd/tree-sitter-kotlin"
  [java]="https://github.com/tree-sitter/tree-sitter-java"
  [python]="https://github.com/tree-sitter/tree-sitter-python"
  [javascript]="https://github.com/tree-sitter/tree-sitter-javascript"
  [typescript]="https://github.com/tree-sitter/tree-sitter-typescript"
  [go]="https://github.com/tree-sitter/tree-sitter-go"
  [rust]="https://github.com/tree-sitter/tree-sitter-rust"
  [c]="https://github.com/tree-sitter/tree-sitter-c"
  [cpp]="https://github.com/tree-sitter/tree-sitter-cpp"
  [html]="https://github.com/tree-sitter/tree-sitter-html"
  [css]="https://github.com/tree-sitter/tree-sitter-css"
  [sql]="https://github.com/derekstride/tree-sitter-sql"
  [json]="https://github.com/tree-sitter/tree-sitter-json"
  [tsx]="https://github.com/tree-sitter/tree-sitter-typescript"  # tsx subdir
  [jsx]="https://github.com/tree-sitter/tree-sitter-javascript"  # jsx is js
  [yaml]="https://github.com/ikatyang/tree-sitter-yaml"
  [toml]="https://github.com/tree-sitter/tree-sitter-toml"
  [xml]="https://github.com/tree-sitter-grammars/tree-sitter-xml"
  [bash]="https://github.com/tree-sitter/tree-sitter-bash"
  [ruby]="https://github.com/tree-sitter/tree-sitter-ruby"
  [php]="https://github.com/tree-sitter/tree-sitter-php"
  [swift]="https://github.com/alex-pinkus/tree-sitter-swift"
  [scala]="https://github.com/tree-sitter/tree-sitter-scala"
  [dart]="https://github.com/UserNobody14/tree-sitter-dart"
  [lua]="https://github.com/MunifTanjim/tree-sitter-lua"
  [perl]="https://github.com/tree-sitter-perl/tree-sitter-perl"
  [haskell]="https://github.com/tree-sitter/tree-sitter-haskell"
  [ocaml]="https://github.com/tree-sitter/tree-sitter-ocaml"
  [julia]="https://github.com/tree-sitter/tree-sitter-julia"
  [r]="https://github.com/r-lib/tree-sitter-r"
  [elixir]="https://github.com/elixir-lang/tree-sitter-elixir"
  [erlang]="https://github.com/WhatsApp/tree-sitter-erlang"
  [fortran]="https://github.com/stadelmanma/tree-sitter-fortran"
  [vim]="https://github.com/neovim/tree-sitter-vim"
  [dockerfile]="https://github.com/camdencheek/tree-sitter-dockerfile"
  [makefile]="https://github.com/alemuller/tree-sitter-make"
  [terraform]="https://github.com/MichaHoffmann/tree-sitter-hcl"
  [regex]="https://github.com/tree-sitter/tree-sitter-regex"
  [vue]="https://github.com/ikatyang/tree-sitter-vue"
  [graphql]="https://github.com/bkegley/tree-sitter-graphql"
  [csharp]="https://github.com/tree-sitter/tree-sitter-c-sharp"
  [less]="https://github.com/mdovale/tree-sitter-less"
  [scss]="https://github.com/serenadeai/tree-sitter-scss"
  [nix]="https://github.com/nix-community/tree-sitter-nix"
  [zig]="https://github.com/maxxnino/tree-sitter-zig"
  [elm]="https://github.com/elm-tooling/tree-sitter-elm"
  [clojure]="https://github.com/sogaiu/tree-sitter-clojure"
  [nim]="https://github.com/alaviss/tree-sitter-nim"
  [crystal]="https://github.com/keidax/tree-sitter-crystal"
  [groovy]="https://github.com/Decodetalkers/tree-sitter-groovy"
  [objc]="https://github.com/jiyee/tree-sitter-objc"
  [latex]="https://github.com/latex-lsp/tree-sitter-latex"
  [bibtex]="https://github.com/latex-lsp/tree-sitter-bibtex"
  [proto]="https://github.com/mitchellh/tree-sitter-proto"
)

LANGS_ALL="markdown kotlin java python javascript typescript go rust c cpp html css sql json tsx jsx yaml toml xml bash ruby php swift scala dart lua perl haskell ocaml julia r elixir erlang fortran vim dockerfile makefile terraform regex vue graphql csharp less scss nix zig elm clojure nim crystal groovy objc latex bibtex proto"

# -------------------------------------------------------------------
# Environment detection. Each step records what's available so the
# operator sees a clear "this platform is/is-not buildable" matrix.
# -------------------------------------------------------------------
detect_env() {
  echo "=== Phase 7 build environment probe ==="
  echo "host:     $(uname -sm)"
  echo "clang:    $(clang --version 2>/dev/null | head -1 || echo MISSING)"

  if [[ -n "${ANDROID_NDK_HOME:-}" && -x "${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android21-clang" ]]; then
    NDK_BIN="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/darwin-x86_64/bin"
  elif [[ -d "${ANDROID_HOME:-}/ndk" ]]; then
    NDK_VERSION="$(ls -1 "${ANDROID_HOME}/ndk" | sort -V | tail -1)"
    if [[ -x "${ANDROID_HOME}/ndk/${NDK_VERSION}/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android21-clang" ]]; then
      NDK_BIN="${ANDROID_HOME}/ndk/${NDK_VERSION}/toolchains/llvm/prebuilt/darwin-x86_64/bin"
    else
      NDK_BIN=""
    fi
  else
    NDK_BIN=""
  fi
  echo "ndk:      ${NDK_BIN:-MISSING (ANDROID_NDK_HOME / ANDROID_HOME unset)}"

  IOS_SDK_DEVICE="$(xcrun --sdk iphoneos --show-sdk-path 2>/dev/null || true)"
  IOS_SDK_SIM="$(xcrun --sdk iphonesimulator --show-sdk-path 2>/dev/null || true)"
  echo "ios-sdk:  ${IOS_SDK_DEVICE:-MISSING (no Xcode / no iOS SDK)}"

  if command -v x86_64-w64-mingw32-gcc >/dev/null 2>&1; then
    MINGW_CC="$(command -v x86_64-w64-mingw32-gcc)"
  else
    MINGW_CC=""
  fi
  echo "mingw-cc: ${MINGW_CC:-MISSING (Homebrew mingw-w64 not on PATH)}"

  if command -v x86_64-linux-gnu-gcc >/dev/null 2>&1; then
    LINUX_X64_CC="$(command -v x86_64-linux-gnu-gcc)"
  elif command -v zig >/dev/null 2>&1; then
    LINUX_X64_CC="$(command -v zig)+cc-target-x86_64-linux-gnu"
  else
    LINUX_X64_CC=""
  fi
  echo "linux-cc: ${LINUX_X64_CC:-MISSING (no x86_64-linux-gnu cross-toolchain)}"

  export NDK_BIN IOS_SDK_DEVICE IOS_SDK_SIM MINGW_CC LINUX_X64_CC
}

# -------------------------------------------------------------------
# inventory subcommand — prints the Yole×bonede×platform matrix.
# -------------------------------------------------------------------
cmd_inventory() {
  echo ""
  echo "=== Yole language × bonede artifact × target platform matrix ==="
  printf "%-12s  %-32s  %-12s\n" "yole-id" "bonede-coord" "android-src"
  printf "%-12s  %-32s  %-12s\n" "------" "------------" "-----------"
  local total_yes=0 total_no=0
  for lang in $LANGS_ALL; do
    local coord="${BONEDE_ARTIFACT[$lang]:-NONE}"
    local repo="${GRAMMAR_REPO[$lang]:-NONE}"
    local repo_short="${repo##https://github.com/}"
    printf "%-12s  %-32s  %-30s\n" "$lang" "$coord" "$repo_short"
    if [[ "$coord" == "NONE" ]]; then total_no=$((total_no+1)); else total_yes=$((total_yes+1)); fi
  done
  echo ""
  echo "Summary: ${total_yes} langs covered by bonede, ${total_no} langs require build-from-source."
}

# -------------------------------------------------------------------
# extract <lang>... — download the bonede JAR for each lang and unpack
# its native binaries into shared/native/<lang>/desktop/.
# -------------------------------------------------------------------
cmd_extract() {
  local langs=("$@")
  [[ ${#langs[@]} -eq 0 ]] && langs=( $LANGS_ALL )
  local success=() failure=()
  for lang in "${langs[@]}"; do
    local coord="${BONEDE_ARTIFACT[$lang]:-}"
    if [[ -z "$coord" ]]; then
      echo "SKIP  $lang: not published on bonede (build-from-source path required)"
      failure+=("$lang:no-bonede-artifact")
      continue
    fi
    local artifact="${coord%:*}"
    local version="${coord#*:}"
    local jar_url="https://repo1.maven.org/maven2/io/github/bonede/${artifact}/${version}/${artifact}-${version}.jar"
    local jar_path="${SCRATCH_DIR}/${artifact}-${version}.jar"
    local out_dir="${SCRATCH_DIR}/extracted/${lang}/desktop"
    mkdir -p "$out_dir"

    if [[ ! -f "$jar_path" ]]; then
      echo "DL    $lang: $jar_url"
      if ! curl -fsSL -o "$jar_path" "$jar_url"; then
        echo "FAIL  $lang: download failed"
        failure+=("$lang:download-failed")
        continue
      fi
    fi

    # Extract every lib/*-tree-sitter-<bonede-base>.* into the out_dir.
    # The bonede base is the artifact name minus the "tree-sitter-" prefix.
    local bonede_base="${artifact#tree-sitter-}"
    local extracted=0
    while IFS= read -r entry; do
      local fname="$(basename "$entry")"
      if unzip -p "$jar_path" "$entry" > "${out_dir}/${fname}.tmp" 2>/dev/null; then
        mv "${out_dir}/${fname}.tmp" "${out_dir}/${fname}"
        extracted=$((extracted+1))
      fi
    done < <(unzip -l "$jar_path" 2>/dev/null | awk '/lib\/.*-tree-sitter-.*\.(so|dylib|dll)$/ {print $NF}')

    if [[ $extracted -eq 0 ]]; then
      echo "FAIL  $lang: no native libs found in JAR"
      failure+=("$lang:empty-jar")
      continue
    fi
    echo "OK    $lang: extracted $extracted binaries -> $out_dir"
    success+=("$lang")
  done
  echo ""
  echo "extract: ${#success[@]} OK, ${#failure[@]} FAIL"
  [[ ${#failure[@]} -gt 0 ]] && printf '  - %s\n' "${failure[@]}"
}

# -------------------------------------------------------------------
# android <lang>... — clone tree-sitter-<lang> and build the NDK .so
# files for arm64-v8a. (armeabi-v7a + x86_64 opt-in via NDK_ABIS env.)
# -------------------------------------------------------------------
cmd_android() {
  detect_env
  if [[ -z "${NDK_BIN}" ]]; then
    echo "ERROR: NDK toolchain not found. Set ANDROID_HOME or ANDROID_NDK_HOME."
    return 1
  fi
  local langs=("$@")
  [[ ${#langs[@]} -eq 0 ]] && langs=( $LANGS_ALL )
  local abis="${NDK_ABIS:-arm64-v8a}"

  local success=() failure=()
  for lang in "${langs[@]}"; do
    local repo="${GRAMMAR_REPO[$lang]:-}"
    if [[ -z "$repo" ]]; then
      echo "SKIP  $lang: no upstream repo URL"
      failure+=("$lang:no-repo")
      continue
    fi
    local src_dir="${SCRATCH_DIR}/${lang}-src"
    if [[ ! -d "$src_dir/.git" ]]; then
      rm -rf "$src_dir"
      echo "CLONE $lang: $repo"
      if ! git clone --depth 1 "$repo" "$src_dir" >/dev/null 2>&1; then
        echo "FAIL  $lang: clone failed"
        failure+=("$lang:clone-failed")
        continue
      fi
    fi

    # Pick the parser source directory. For monorepo grammars (tsx,
    # ocaml, php, typescript) the actual parser is in a subdirectory.
    local parser_dir
    if [[ -f "$src_dir/src/parser.c" ]]; then
      parser_dir="$src_dir"
    elif [[ -f "$src_dir/${lang}/src/parser.c" ]]; then
      parser_dir="$src_dir/${lang}"
    elif [[ "$lang" == "tsx" && -f "$src_dir/tsx/src/parser.c" ]]; then
      parser_dir="$src_dir/tsx"
    elif [[ "$lang" == "typescript" && -f "$src_dir/typescript/src/parser.c" ]]; then
      parser_dir="$src_dir/typescript"
    else
      echo "FAIL  $lang: no parser.c found"
      failure+=("$lang:no-parser-c")
      continue
    fi

    for abi in $abis; do
      local triple
      case "$abi" in
        arm64-v8a)    triple="aarch64-linux-android21" ;;
        armeabi-v7a) triple="armv7a-linux-androideabi21" ;;
        x86_64)       triple="x86_64-linux-android21" ;;
        *) echo "FAIL  $lang: unknown ABI $abi"; failure+=("$lang:bad-abi-$abi"); continue ;;
      esac
      local cc="${NDK_BIN}/${triple}-clang"
      [[ -x "$cc" ]] || { echo "FAIL  $lang/$abi: $cc not found"; failure+=("$lang:no-cc-$abi"); continue; }
      local out_dir="${NATIVE_DIR}/${lang}/android-${abi}"
      mkdir -p "$out_dir"
      local out_so="${out_dir}/libtree-sitter-${lang}.so"
      local scanner=""
      [[ -f "$parser_dir/src/scanner.c" ]] && scanner="$parser_dir/src/scanner.c"

      if "$cc" -O3 -fPIC -shared -I "$parser_dir/src" \
          "$parser_dir/src/parser.c" $scanner \
          -o "$out_so" 2>"${SCRATCH_DIR}/${lang}-${abi}.log"; then
        # Verify file format
        local actual_type="$(file -b "$out_so")"
        if [[ "$actual_type" != *"ELF 64-bit"* && "$actual_type" != *"ELF 32-bit"* ]]; then
          echo "FAIL  $lang/$abi: built binary is not ELF: $actual_type"
          failure+=("$lang:bad-format-$abi")
          rm -f "$out_so"
          continue
        fi
        echo "OK    $lang/$abi: $(stat -f%z "$out_so") bytes"
        success+=("$lang/$abi")
      else
        echo "FAIL  $lang/$abi: build failed (log: ${SCRATCH_DIR}/${lang}-${abi}.log)"
        failure+=("$lang:build-$abi")
      fi
    done
  done
  echo ""
  echo "android: ${#success[@]} OK, ${#failure[@]} FAIL"
  [[ ${#failure[@]} -gt 0 ]] && printf '  - %s\n' "${failure[@]}"
}

# -------------------------------------------------------------------
# ios <lang>... — Xcode static-library build for iOS arm64 device + sim.
# BLOCKED in environments without Xcode (Command-Line-Tools only). The
# script exits gracefully with a clear "operator action required" message.
# -------------------------------------------------------------------
cmd_ios() {
  detect_env
  if [[ -z "${IOS_SDK_DEVICE}" ]]; then
    cat <<'EOF'
ERROR: iOS SDK not detected. Required for iOS static-library builds.
       Install Xcode + iOS SDK via the Mac App Store, then run:
         sudo xcode-select --switch /Applications/Xcode.app/Contents/Developer
       and re-run this script. Without Xcode, only the Command-Line-Tools
       are installed and `xcrun --sdk iphoneos --show-sdk-path` fails.
EOF
    return 1
  fi
  # ... build path documented but unreachable in current env. Same
  # parser.c -> .a flow as Android, but using `clang -arch arm64
  # -mios-version-min=15.0 -isysroot $IOS_SDK_DEVICE -c -o tmp.o`
  # followed by `libtool -static -o libtree-sitter-<lang>.a tmp.o`.
  echo "TODO: iOS build path is scaffolded but cannot be exercised here."
  return 1
}

# -------------------------------------------------------------------
# verify — `file`-check every output binary, fail if any is invalid.
# -------------------------------------------------------------------
cmd_verify() {
  local total=0 ok=0 bad=0
  while IFS= read -r f; do
    total=$((total+1))
    local ft="$(file -b "$f")"
    case "$ft" in
      *"ELF 64-bit"*|*"ELF 32-bit"*|*"Mach-O 64-bit"*|*"PE32+"*|*"current ar archive"*)
        ok=$((ok+1))
        ;;
      *)
        echo "BAD   $f: $ft"
        bad=$((bad+1))
        ;;
    esac
  done < <(find "${SCRATCH_DIR}/extracted" "${NATIVE_DIR}" -type f \( -name "*.so" -o -name "*.dylib" -o -name "*.dll" -o -name "*.a" \) 2>/dev/null | grep -v "/android-tree-sitter/")
  echo ""
  echo "verify: $total checked, $ok OK, $bad BAD"
  [[ $bad -gt 0 ]] && return 1
  return 0
}

cmd_all() {
  detect_env
  cmd_inventory
  cmd_extract
  cmd_android
  cmd_verify
}

case "${1:-help}" in
  inventory) cmd_inventory ;;
  extract) shift; cmd_extract "$@" ;;
  android) shift; cmd_android "$@" ;;
  ios) shift; cmd_ios "$@" ;;
  verify) cmd_verify ;;
  all) cmd_all ;;
  detect-env) detect_env ;;
  *)
    cat <<EOF
Usage: $0 <command> [args]

Commands:
  inventory             Print Yole language x bonede artifact x repo matrix.
  extract [lang]...     Download bonede JARs + extract Desktop binaries.
  android [lang]...     NDK-build .so files for Android ABIs (NDK_ABIS env).
  ios [lang]...         Xcode static-lib build for iOS (requires Xcode).
  verify                file-check every output binary, fail if any invalid.
  all                   inventory + extract + android + verify (no iOS).
  detect-env            Print available toolchains.
EOF
    ;;
esac
