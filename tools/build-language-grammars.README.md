<!--
SPDX-FileCopyrightText: 2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# `tools/build-language-grammars.sh`

Per-language Tree-Sitter native grammar acquisition pipeline introduced
in iter-58 F2 Phase 7. Closes (per-platform) the gap documented in
`docs/KNOWN_DEFECTS.md#f2-phase-6-grammar-bundling-gap`.

## What it does

For each of the 55 Yole languages declared in
`shared/src/commonMain/kotlin/digital/vasic/yole/language/LanguageMetadata.kt`,
this script acquires the platform-native Tree-Sitter shared library
(`.so` / `.dylib` / `.dll` / `.a`) the `TokenizerEngine` needs to
actually parse user code on that platform.

Two acquisition paths:

1. **bonede-extract** — download `io.github.bonede:tree-sitter-<lang>`
   from Maven Central and unpack the bundled native binaries. Covers
   48 of 55 Yole languages × 5 desktop targets (aarch64-linux-gnu,
   aarch64-macos, x86_64-linux-gnu, x86_64-macos, x86_64-windows).
   This is the canonical Desktop path — Yole consumes the JARs via
   standard Gradle `implementation()` dependencies; the bonede
   `NativeUtils` extracts at runtime. The script's `extract`
   subcommand is purely an operator-side offline verification tool.

2. **build-from-source** — clone `tree-sitter-<lang>` upstream and
   `cc -fPIC -shared` against the chosen toolchain. Used for:
   - Android NDK ABIs (bonede ships only Linux-glibc/macOS/Windows
     binaries; Android needs bionic-compatible NDK builds).
   - The 7 Yole languages with no bonede artifact (jsx, xml, vim,
     less, crystal, groovy, bibtex).
   - iOS arm64 static libraries (when Xcode + iOS SDK present).

## Prerequisites

| Subcommand | Requires |
|------------|----------|
| `inventory` | nothing |
| `extract`   | curl, unzip — works on any Unix host |
| `android`   | Android NDK (`ANDROID_HOME` or `ANDROID_NDK_HOME` set), git, clang |
| `ios`       | Xcode + iOS SDK (`xcrun --sdk iphoneos --show-sdk-path` must work), git |
| `verify`    | `file(1)` utility |

The script's `detect-env` subcommand prints a one-shot summary of
which toolchains are available on the current host.

## Usage

```bash
# Print the Yole-lang × bonede-artifact × upstream-repo matrix.
tools/build-language-grammars.sh inventory

# Verify operator-side that all 48 bonede artifacts are still
# resolvable on Maven Central (no network deletions).
tools/build-language-grammars.sh extract

# Build Android NDK arm64-v8a shared libs for a specific tier of langs.
tools/build-language-grammars.sh android markdown kotlin python java

# Build NDK shared libs for all 3 Android ABIs (opt-in via env var).
NDK_ABIS="arm64-v8a armeabi-v7a x86_64" \
  tools/build-language-grammars.sh android markdown

# Verify every binary under shared/native/ and build/lang-grammars-scratch/.
tools/build-language-grammars.sh verify

# All-in-one (inventory + extract + android arm64-v8a + verify).
tools/build-language-grammars.sh all
```

## Output layout

```
build/lang-grammars-scratch/         (NOT committed)
  <lang>-src/                          upstream repo clone
  <lang>-<abi>.log                     NDK build log on failure
  extracted/<lang>/desktop/            bonede-extracted Desktop binaries
                                       (used for offline verification only)

shared/native/                       (committed when present)
  android-tree-sitter/                 iter-57 markdown + tree-sitter core
                                       (existing, untouched by Phase 7)
  <lang>/
    android-arm64-v8a/lib*.so          NDK build output
    android-armeabi-v7a/lib*.so        (opt-in)
    android-x86_64/lib*.so             (opt-in)
    ios-arm64-device/lib*.a            (when Xcode present)
    ios-arm64-sim/lib*.a               (when Xcode present)
```

## Anti-bluff guarantees (CONST-035)

- Every output binary is `file`-checked. Non-ELF / non-Mach-O /
  non-PE outputs cause a hard exit (no silent zero-byte placeholders).
- Languages with no acquisition path on the current host are logged
  as SKIP with the documented reason; the operator sees the gap
  explicitly rather than getting a fake binary.
- The bonede JARs are immutable Maven Central artifacts pinned by
  exact version in this script — operator can independently verify
  the SHAs against `repo1.maven.org`.

## Phase 7 deliverable status (2026-05-15 snapshot)

| Platform | Coverage | Source |
|----------|----------|--------|
| macOS-arm64 Desktop | 48/55 langs | bonede JARs (Gradle deps) |
| macOS-x64 Desktop   | 48/55 langs | bonede JARs (Gradle deps) |
| Linux-x64 Desktop   | 48/55 langs | bonede JARs (Gradle deps) |
| Linux-aarch64 Desktop | 48/55 langs | bonede JARs (Gradle deps) |
| Windows-x64 Desktop | 48/55 langs | bonede JARs (Gradle deps) |
| Android arm64-v8a   | 1/55 (markdown only) | iter-57 NDK build |
| Android armeabi-v7a | 1/55 (markdown only) | iter-57 NDK build |
| Android x86_64      | 1/55 (markdown only) | iter-57 NDK build |
| iOS arm64           | BLOCKED  | no Xcode in build env |
| Wasm                | OUT OF SCOPE | textmate path per Feature 1 Phase 6 |

The 7 langs without bonede artifacts (jsx, xml, vim, less, crystal,
groovy, bibtex) are tracked as KNOWN_DEFECTS#f2-phase-7-no-bonede-artifact.

Android coverage beyond markdown is tracked as
KNOWN_DEFECTS#f2-phase-7-android-ndk-bulk-build-pending — the build
script is ready; running it for all 48 langs × 3 ABIs is a separate
operator-time investment (~5-15 minutes wall clock).

iOS coverage is tracked as
KNOWN_DEFECTS#f2-phase-7-ios-xcode-required.
