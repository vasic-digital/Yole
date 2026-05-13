# Yole 1.0.1 — Release Notes (versionCode 101 = `0.0.0.1.1`)

**Distribution date:** 2026-05-13 (iter-54)

This is the canonical per-version release-notes snapshot mandated by
the project's release governance (mirrors the §6.P pattern from
sibling projects). When Firebase App Distribution is triggered, this
text is what `--release-notes` references.

## Highlights

- **Anti-bluff hardening across the model authority surface (iter-53).**
  The LLMProvider submodule's `pkg/discovery` Tier 3 (hardcoded
  FallbackModels) was DEPRECATED per CONST-036, with the per-provider
  httptest-fixture sweep tracked as
  `#fallback-tier-removed-needs-httptest-fixture` (75 latent bluffs
  surfaced + counted in the raw strip log). Ollama + Venice
  `TestGetCapabilities` already rewritten to use controlled httptest
  fixtures so they no longer drift when upstream catalogues change.

- **New `pkg/apikeys` central authority (iter-53).** Reads
  `ApiKey_<Provider>` env vars from `~/api_keys.sh`. Matches the
  convention used by LLMsVerifier so all three surfaces share one
  source of credential truth.

- **New `apikeys_live_discovery_challenge.sh` (iter-53).** Real-stack
  Challenge that sources `~/api_keys.sh` and invokes the live
  HuggingFace `/api/models` endpoint. Operator's run captured 5 real
  models including `SulphurAI/Sulphur-2-base` — positive runtime
  evidence per CONST-035 §11.4.

- **Governance covenant cascade (iter-52).** The verbatim
  end-user-quality covenant ("We had been in position that all
  tests do execute with success and all Challenges as well, but in
  reality the most of the features does not work and can't be
  used! …") was propagated to 48 governance files across the Yole
  superproject, LLMProvider submodule, and all 10 sibling KMP
  repos' CONSTITUTION/CLAUDE/AGENTS triples.

- **Cross-submodule test fixes (iter-52).** macOS portability gaps
  in Challenges (`#!/bin/sh` trailing-newline) + Containers
  (symlink resolution + Linux-only test skip guards) so the full
  multi-submodule suite runs green on the macOS audit host.

- **KNOWN_DEFECTS pruned (iter-52).** SMB stub-no-negotiation and
  WebDAV always-online-stub tickets moved OPEN → CLOSED with
  forensic anchor to commit `1f6472c9` (2026-05-07).

## Pre-build verification gates (all PASS)

- `./gradlew :shared:desktopTest` — BUILD SUCCESSFUL in 59s (host JVM).
- `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL in 42s.
- `./gradlew :androidApp:assembleRelease` — BUILD SUCCESSFUL in 2m 13s
  (signed with release keystore SHA-256 8E:67:AB:AC:E5:61:52:1D:CE:
  B0:E3:76:5B:27:D6:9F:30:15:41:CA:0F:C6:43:99:3D:8B:1D:FC:27:0E:01:AD
  per iter-31 record).
- LLMProvider full Go test suite — 50 packages OK / 0 FAIL (iter-53).
- HuggingFace live-discovery Challenge — PASS (5 real models captured).
- Yole `scripts/anti-bluff/bluff-scanner.sh --mode all` — PASS clean.
- Yole `yole-challenges/scripts/anchor_manifest_challenge.sh` — PASS
  (55 capability rows).
- Yole `yole-challenges/scripts/mutation_ratchet_challenge.sh` — PASS
  (stub; full ratchet deferred to sub-project 4).

## Build artifacts produced (locally — see "Operator distribution steps" below)

Path: `releases/` (gitignored — large binaries).

| Platform | Variant | Filename | Size | Build status |
|----------|---------|----------|------|--------------|
| Android | Debug | `Yole-Android-1.0.1-Debug-0.0.0.1.1.apk` | 31 MB | PASS (`assembleDebug`) |
| Android | Release | `Yole-Android-1.0.1-Release-0.0.0.1.1.apk` | 24 MB | PASS (`assembleRelease`, release-keystore-signed) |
| Desktop macOS-arm64 | Debug | `Yole-Desktop-macos-arm64-1.0.1-Debug-0.0.0.1.1.dmg` | 130 MB | PASS (`packageDmg`) |
| Desktop macOS-arm64 | Release | `Yole-Desktop-macos-arm64-1.0.1-Release-0.0.0.1.1.dmg` | 130 MB | PASS (`packageReleaseDmg`) |
| Desktop linux-x64 | Debug + Release | — | — | **DEFERRED** — Compose Desktop only produces native packages on the matching host. Build .deb on a Linux host. |
| Desktop windows-x64 | Debug + Release | — | — | **DEFERRED** — same reason: build .msi on a Windows host. |
| Web Wasm PWA | — | — | — | **DEFERRED** — `:webApp` doesn't currently have `BrowserDistribution` task wired (pre-existing config gap; tracked as a webApp module owed item). |
| iOS | — | — | — | **DEFERRED** — in development per `CLAUDE.md` platform-status table; not part of this iter. |

## Operator distribution steps (Firebase)

Firebase CLI cached token has expired on the audit host (`firebase
projects:list` reports auth failure). The operator must complete the
distribution step interactively:

```bash
# 1. Authenticate (interactive — only the operator can run this):
firebase login

# 2. Distribute the release APK to the 3 testers (per iter-31 record):
firebase appdistribution:distribute releases/Yole-Android-1.0.1-Release-0.0.0.1.1.apk \
  --app <app-id-from-iter-31> \
  --release-notes-file docs/releases/1.0.1/release-notes.md \
  --testers milos85vasic@gmail.com,milos85vasic.2nd@gmail.com,milos85vasic.3rd@gmail.com

# 3. Distribute the debug APK to the same testers:
firebase appdistribution:distribute releases/Yole-Android-1.0.1-Debug-0.0.0.1.1.apk \
  --app <app-id-from-iter-31> \
  --release-notes-file docs/releases/1.0.1/release-notes.md \
  --testers milos85vasic@gmail.com,milos85vasic.2nd@gmail.com,milos85vasic.3rd@gmail.com

# 4. Confirm both distributions landed:
firebase appdistribution:testers:list
```

(Desktop dmgs + Web Wasm PWA don't go through Firebase App
Distribution — that surface is mobile-only. They remain in
`releases/` as legacy artifacts for the operator's manual upload
elsewhere.)

## Known carry-over (tracked in `docs/KNOWN_DEFECTS.md`)

- `#fallback-tier-removed-needs-httptest-fixture` — 75 LLMProvider
  test assertions still consult the deprecated Tier 3 path;
  multi-iteration httptest-fixture sweep owed before the runtime
  path can be removed.
- `#robolectric-compose-ui-tests-brittle` — long-running migration to
  HelixQA on-device automation; mitigated by dedicated container.
- `#helixqa-missing-sibling-repos` — environment bootstrap gap for
  31 HelixQA packages when sibling repos absent.
- **iter-54 desktop cross-platform** — Linux .deb + Windows .msi
  require those hosts to build; both deferred. Web Wasm
  `:webApp:BrowserDistribution` config gap owed.
- **iter-54 Firebase distribution** — operator must run interactive
  `firebase login` then the distribute commands above. I cannot run
  the interactive auth step.
