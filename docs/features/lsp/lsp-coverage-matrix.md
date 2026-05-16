<!-- SPDX-FileCopyrightText: 2026 Milos Vasic -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# LSP Coverage Matrix

> Generated 2026-05-16 from source tree (`shared/src/commonMain/resources/lsp-servers/*/server.json`) + Phase 7 acquisition state (`.lsp-binary-cache/<langId>/macos-arm64/`).
> Re-generate after Phase 8b lands Android binaries.

## Server matrix

| LangId | Server executable | Runtime | Project markers | Bundled on Desktop macOS-arm64? | Bundled on Android? |
|---|---|---|---|---|---|
| `bash` | `bash-language-server` | Node.js | `.shellcheckrc`, `.bash_profile`, `bashrc` | No — runtime DFM pending | No |
| `c` | `clangd` | Native | `compile_commands.json`, `CMakeLists.txt`, `Makefile` | **Yes** — `clangd` (shared with `cpp`) | No |
| `cpp` | `clangd` | Native | `compile_commands.json`, `CMakeLists.txt`, `Makefile` | **Yes** — `clangd` (shared with `c`) | No |
| `elixir` | `elixir-ls` | Elixir/Erlang OTP | `mix.exs` | No — runtime not bundled | No |
| `go` | `gopls` | Native | `go.mod`, `go.work` | No — binary not staged (Phase 7 gap) | No |
| `haskell` | `haskell-language-server-wrapper` | Native | `*.cabal`, `stack.yaml`, `cabal.project`, `hie.yaml` | **Yes** — `haskell-language-server-wrapper` | No |
| `java` | `jdtls` | JVM | `pom.xml`, `build.gradle`, `build.gradle.kts`, `.classpath` | **Yes** — full jdtls bundle (bin + config_mac_arm + features + plugins) | No |
| `kotlin` | `kotlin-language-server` | JVM | `build.gradle.kts`, `build.gradle`, `settings.gradle.kts` | **Yes** — `kotlin-language-server` server dir | No |
| `lua` | `lua-language-server` | Native | `.luarc.json`, `.luarc.jsonc` | **Yes** — `lua-language-server` | No |
| `markdown` | `marksman` | Native | `.marksman.toml`, `README.md` | **Yes** — `marksman` | No |
| `python` | `pyright-langserver` | Node.js | `pyrightconfig.json`, `pyproject.toml`, `setup.py` | No — runtime DFM pending | No |
| `rust` | `rust-analyzer` | Native | `Cargo.toml` | **Yes** — `rust-analyzer` | No |
| `typescript` | `typescript-language-server` | Node.js | `tsconfig.json`, `package.json` | No — runtime DFM pending | No |
| `yaml` | `yaml-language-server` | Node.js | `.yamllint`, `.yamllint.yml`, `.yamllint.yaml` | No — runtime DFM pending | No |
| `zig` | `zls` | Native | `build.zig`, `build.zig.zon` | **Yes** — `zls` | No |

## Bundled summary

**Desktop macOS-arm64 (Phase 7 state): 8 of 15 servers staged**

Staged: `c`/`cpp` (clangd), `haskell`, `java` (jdtls), `kotlin`, `lua`, `markdown` (marksman), `rust`, `zig`

Not staged: `bash`, `elixir`, `go`, `python`, `typescript`, `yaml`

> Note: `c` and `cpp` share the `clangd` binary; both are listed as separate rows because they have separate `server.json` entries and separate staging directories. The physical binary is one file.

**Android: 0 of 15 servers staged**

All Android entries show "Not available on Android (v1)" in the Settings → Language Servers screen. Tracked under `#crossbuild-android-ndk-lsp` (native servers) and Phase 8b planning (JVM/Node runtime DFMs).

## Runtime classification

| Runtime | Servers | Desktop macOS-arm64 staged? |
|---|---|---|
| Native (self-contained binary) | clangd, haskell-language-server-wrapper, lua-language-server, marksman, rust-analyzer, zls | All 6 staged |
| JVM (requires JRE) | jdtls, kotlin-language-server | Both staged (bundled JRE via jdtls tarball) |
| Node.js (requires Node runtime DFM) | bash-language-server, pyright-langserver, typescript-language-server, yaml-language-server | None staged — runtime DFM pending |
| Elixir/OTP (requires Elixir runtime) | elixir-ls | Not staged — runtime not bundled |

## Notes

- **`go` gap:** `gopls` was not staged in Phase 7 despite being a native binary with no external runtime dependency. The acquisition script had a download step for `gopls` that was not executed. This is a gap, not an architectural limitation — `gopls` can be staged the same way as `rust-analyzer` or `marksman` in a follow-up.
- **`elixir-ls`** depends on a full Elixir/OTP runtime. Bundling is non-trivial; deferred beyond Phase 8b.
- **jdtls first-open gap:** On first project open, jdtls downloads ~150 MB of Maven/Gradle build dependencies from the internet. Tracked as `#iter-61-jdtls-project-build-deps-online`.
- **Node DFM servers:** `bash-language-server`, `pyright-langserver`, `typescript-language-server`, and `yaml-language-server` require a Node.js runtime. Desktop macOS-arm64 ships without a bundled Node; these servers require the user to have Node installed. The "runtime DFM pending" label means a bundled Node DFM is planned for Desktop and Android in Phase 8b.
