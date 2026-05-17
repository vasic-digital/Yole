# T7 External SSD Migration — iter-79 (2026-05-17)

Goal: migrate all Yole + Lava build dependencies from `/Volumes/T7` to `$HOME`
so the T7 Samsung Portable SSD can be safely detached.

---

## Pre-migration disk state

| Filesystem | Total | Used | Free |
|-----------|-------|------|------|
| Home (`/dev/disk3s5`) | 460 GiB | 399 GiB | **15 GiB** |
| T7 (`/dev/disk4s1`) | 1.8 TiB | 456 GiB | 1.4 TiB |

---

## Phase A: Home cleanup (freed ~30 GB)

All items below are **reproducible caches** — deleted without data loss:

| Item | Freed |
|------|-------|
| `~/Library/Caches/Google/AndroidStudio2024.3` | 2.1 G |
| `~/Library/Caches/Google/AndroidStudio2024.3.2` | 1.3 G |
| `~/Library/Caches/Google/AndroidStudio2025.1.1` | 1.6 G |
| `~/Library/Caches/Google/AndroidStudio2025.1.2` | 3.0 G |
| `~/Library/Caches/Yarn` | 2.8 G |
| `~/Library/Caches/Homebrew` | 2.2 G |
| `~/Library/Caches/com.seriflabs.affinitydesigner` | 2.9 G |
| `~/Library/Caches/Cypress` | 1.2 G |
| `~/Library/Caches/ms-playwright` | 1.0 G |
| `~/Library/Caches/JetBrains` (IDE indexes/caches) | 3.1 G |
| `~/Library/Caches/pip` | 357 M |
| `~/Library/Caches/typescript` | 1.6 G |
| `~/Library/Caches/goimports` | 628 M |
| `~/Library/Caches/vscode-cpptools` | 697 M |
| Dangling Podman images | ~2.0 G |
| Go build cache (`go clean -cache`) | 3.1 G |
| Unavailable iOS simulator devices (`xcrun simctl delete unavailable`) | ~353 M |
| **Total freed** | **~30 GB** |

---

## Phase B: T7 pruning (reduced T7 Gradle 23G → 16G)

Deleted old Gradle version caches (unused by any project):

| Deleted from T7 | Size |
|----------------|------|
| `/Volumes/T7/Gradle/caches/8.14` | 2.7 G |
| `/Volumes/T7/Gradle/caches/8.14.2` | 569 M |
| `/Volumes/T7/Gradle/caches/8.14.3` | 1.4 G |
| `/Volumes/T7/Gradle/caches/8.13` | 1.3 G |
| `/Volumes/T7/Gradle/caches/9.0-milestone-1` | 282 M |
| `/Volumes/T7/Gradle/daemon` (build logs) | 1.0 G |
| **Total pruned from T7** | **~7.3 GB** |

Kept: `8.11.1` (Yole), `8.9` (Lava), `modules-2`, `build-cache-1`, `wrapper`.

---

## Phase C: Migration results

### C.1 Android SDK — COMPLETE
- Source: `/Volumes/T7/Android/SDK/` (26 G)
- Destination: `~/Library/Android/sdk/` (27.3 G transferred)
- Verified: `platforms/`, `build-tools/`, `ndk/` (4 versions), `system-images/` all present

### C.2 Gradle cache — COMPLETE
- Source: `/Volumes/T7/Gradle/` (16 G after pruning)
- Destination: `~/.gradle/` (merged, now 17 G total)
- Verified: `caches/8.11.1` (6.8 G), `caches/8.9` (1.8 G), `caches/modules-2` (2.7 G)

### C.3 JetBrains IDEs — PARTIAL (space constraint)
- T7 total: 60 G (.app bundles for 9 IDEs, both versioned and unversioned copies)
- Home free after C.1+C.2: ~8 G — insufficient for 60 G of IDEs
- **IntelliJ IDEA Ultimate 2025.2.4.app** migrated → `~/Applications/` (4.7 G)
- **GoLand, CLion, Rider, WebStorm, DataGrip, DataSpell, PyCharm, RubyMine** — remain on T7
- **Action required by operator:** Use JetBrains Toolbox to reinstall remaining IDEs to
  `/Applications/` from JetBrains servers, then delete from T7. Each IDE is 2–6 G.
  Total home expansion needed: ~55 G (or delete other large items first).
  Alternative: buy a new SSD and expand home partition.

### C.4 Ollama — COMPLETE
- Source: `/Volumes/T7/Ollama/` (3.6 G — model: `deepseek-coder/6.7b`)
- Destination: `~/.ollama/` (3.6 G)
- Verified: `blobs/` + `manifests/` present

### C.5 HelixCode — N/A
- `/Volumes/T7/Projects/` was **empty** (0 bytes). HelixCode was never on T7.
- `~/Projects/HelixCode` does not exist. PATH entry is a dead ref (harmless).

### C.6 Anaconda — N/A
- Zshrc note confirms: "Anaconda was not on the external drive."
- Already pointed to `$HOME/anaconda3` in `~/.zshrc`.

### C.7 LLamaCpp — N/A
- `/Volumes/T7/LLamaCpp/` was 8K (empty Models dir).
- `LLAMACPP_MODELS_PATH` already set to `$HOME/.local/share/llama.cpp/models` in `~/.zshrc`.

---

## Phase D: Config file updates

### `~/.zshrc` — Already migrated (no T7 refs found)
All env vars already used `$HOME` paths:
```
GRADLE_USER_HOME=$HOME/.gradle
ANDROID_HOME="$HOME/Library/Android/sdk"
ANDROID_SDK_ROOT="$ANDROID_HOME"
ANDROID_NDK_ROOT="$ANDROID_HOME/ndk/29.0.13599879"
STATIC_ASSETS="$HOME/Projects/StaticAssets"
LLAMACPP_MODELS_PATH="$HOME/.local/share/llama.cpp/models"
```
Conda block uses `$HOME/anaconda3`.

### `~/.bash_profile` — Updated
- Was referencing `/Volumes/DATA1/Anaconda/anaconda3` (a different old volume).
- Updated to `$HOME/anaconda3`. Backup: `~/.bash_profile.t7-migration-bak`.

### `~/Projects/Yole/local.properties` — Updated
- Was: `sdk.dir=/opt/homebrew/share/android-commandlinetools`
- Now: `sdk.dir=/Users/milosvasic/Library/Android/sdk`
- Backup: `~/Projects/Yole/local.properties.t7-migration-bak`

### `~/Projects/Lava/local.properties` — N/A (file doesn't exist)
Lava uses `ANDROID_HOME` env var, which is already home-based.

### `~/Projects/StaticAssets` — Created as empty placeholder
Was referenced by `STATIC_ASSETS` env var but didn't exist on either home or T7.

---

## Phase E: Build validation

| Project | Command | Result |
|---------|---------|--------|
| Yole | `./gradlew :shared:desktopTest` | **BUILD SUCCESSFUL** (22s, 33 tasks up-to-date) |
| Lava | `./gradlew tasks` | **SUCCESS** (Gradle 8.9 from `~/.gradle`) |

---

## Post-migration disk state

| Filesystem | Total | Used | Free |
|-----------|-------|------|------|
| Home (`/dev/disk3s5`) | 460 GiB | ~411 GiB | **~8 GiB** |

---

## Remaining T7 dependency

T7 **CANNOT** be safely detached yet because:

1. **JetBrains IDEs** (GoLand, CLion, Rider, WebStorm, DataGrip, DataSpell, PyCharm, RubyMine)
   are still on `/Volumes/T7/JetBrains/` — 8 apps × ~3-6 G = ~55 G remaining.

### Recommended operator actions to complete migration:

**Option A (preferred): JetBrains Toolbox reinstall**
1. Download JetBrains Toolbox from jetbrains.com
2. Configure install location to `/Applications` (home volume)
3. Install each IDE you use: GoLand, CLion, Rider, WebStorm, DataGrip
4. Once verified working, delete the T7 copies
5. This downloads from JetBrains servers, no T7 data loss

**Option B: Direct copy (requires ~55G free on home)**
1. Free ~55G more from home (old simulator runtimes, Docker images, etc.)
2. `rsync -avh /Volumes/T7/JetBrains/*.app ~/Applications/`
3. Verify each IDE launches before deleting from T7

**Option C: External SSD replacement**
1. Buy a second portable SSD
2. Migrate JetBrains IDEs there
3. Update Spotlight/Launchpad paths

---

## T7 cleanup (DO NOT DO UNTIL IDEs ARE MIGRATED)

After all IDEs are confirmed working from home, these T7 items are safe to delete:

- `/Volumes/T7/Android/` — migrated to `~/Library/Android/sdk`
- `/Volumes/T7/Gradle/` — merged into `~/.gradle`
- `/Volumes/T7/Ollama/` — migrated to `~/.ollama`
- `/Volumes/T7/LLamaCpp/` — was empty
- `/Volumes/T7/Projects/` — was empty
- `/Volumes/T7/tmp/` — was empty
- `/Volumes/T7/Yandex` — was empty
- `/Volumes/T7/JetBrains/*.app` — ONLY after reinstalling via Toolbox

DO NOT delete: `Yandex.Disk.localized` (322G personal), `Music` (12G), `Downloads` (1.6G).
