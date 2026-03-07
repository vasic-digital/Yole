<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Migration from Markor to Yole

Yole is the spiritual successor to Markor, rebuilt from the ground up as a Kotlin Multiplatform application. This guide covers what has changed, what is the same, and how to migrate your existing setup.

---

## Overview

| Aspect | Markor | Yole |
|--------|--------|------|
| **Platform** | Android only | Android, Desktop, iOS (in development), Web (in development) |
| **Language** | Java + Kotlin (Android) | Kotlin Multiplatform |
| **UI Framework** | Android Views | Compose Multiplatform |
| **Package** | `net.gsantner.opoc.*` | `digital.vasic.yole.*` |
| **Text Formats** | Markdown, Todo.txt, Plain Text | 17 formats (Markdown, Todo.txt, LaTeX, Org Mode, CSV, and more) |
| **Cloud Storage** | None (external sync only) | 8 built-in providers (Dropbox, Google Drive, OneDrive, WebDAV, FTP, SFTP, Git, SMB) |
| **Offline** | Yes (local only) | Yes (offline-first with optional cloud sync) |

---

## File Compatibility

### 100% Backward Compatible

All your existing Markor files work in Yole without any changes:

- **Markdown files** (`.md`, `.markdown`) -- Yole uses Flexmark (same as Markor) with 16+ extensions
- **Todo.txt files** (`.txt`) -- Full todo.txt format support with the same query syntax
- **Plain text files** (`.txt`) -- Plain text is always supported

Yole reads the same file formats, in the same directories, using the same plain-text storage approach. You do not need to convert or modify any files.

### New Formats Available

After migrating, you can create documents in any of the 17 supported formats:

| Format | Extensions | Description |
|--------|-----------|-------------|
| Markdown | `.md`, `.markdown` | CommonMark + GFM (same as Markor) |
| Todo.txt | `.txt` (auto-detected) | Task management (same as Markor) |
| Plain Text | `.txt` | Simple text with syntax highlighting |
| LaTeX | `.tex`, `.latex` | Mathematical and scientific documents |
| Org Mode | `.org` | Emacs-style organizational documents |
| CSV | `.csv` | Tabular data with column highlighting |
| WikiText | `.wiki` | MediaWiki-compatible markup |
| AsciiDoc | `.adoc`, `.asciidoc` | Technical documentation |
| reStructuredText | `.rst` | Python documentation standard |
| R Markdown | `.rmd` | R statistical documents |
| TaskPaper | `.taskpaper` | Alternative task management format |
| Textile | `.textile` | Lightweight markup |
| Creole | `.creole` | Standardized wiki markup |
| TiddlyWiki | `.tid` | Non-linear personal wiki |
| Jupyter | `.ipynb` | Interactive notebooks (view-only) |
| Key-Value | `.ini`, `.properties`, `.env` | Configuration files |
| Binary | Various | Binary file detection and handling |

---

## What's Different

### Architecture

| Feature | Markor | Yole |
|---------|--------|------|
| Module structure | Single Android module | 10 extracted KMP modules + platform apps |
| Build system | Gradle (Android) | Gradle (KMP) with composite builds |
| Testing | Android instrumentation | Common tests + platform tests (4,750+ tests) |
| Dependencies | Android-specific | Multiplatform (Ktor, kotlinx, Compose) |

### User Interface

| Feature | Markor | Yole |
|---------|--------|------|
| UI toolkit | Android Views (XML layouts) | Compose Multiplatform |
| Navigation | Bottom tabs | Bottom tabs (Android), sidebar (Desktop), tab bar (iOS/Web) |
| Theme | Material Design (Android) | Custom theme system with light/dark/system modes |
| Animations | Standard Android animations | Custom animation system with accessibility support |
| Accessibility | Android accessibility | Cross-platform accessibility utilities |

### Editor

| Feature | Markor | Yole |
|---------|--------|------|
| Format detection | Extension-based | Extension + content pattern detection |
| Markdown parser | Flexmark | Flexmark (same, with more extensions) |
| Preview | WebView (Android) | Compose-based rendering |
| Code highlighting | Highlight.js | Platform-specific rendering |
| Line numbers | Optional | Optional |
| Word wrap | Optional | Optional |

### Storage

| Feature | Markor | Yole |
|---------|--------|------|
| Local storage | Android file system | Cross-platform (Okio) |
| Cloud storage | None (use external sync apps) | 8 built-in providers |
| Sync | External (Syncthing, etc.) | Built-in + external |
| Offline support | Always offline | Offline-first with optional online |

---

## Migration Steps

### Step 1: Install Yole

Install Yole alongside Markor. Both apps can coexist on the same Android device.

- **F-Droid**: [f-droid.org/packages/digital.vasic.yole](https://f-droid.org/packages/digital.vasic.yole/)
- **GitHub Releases**: [github.com/vasic-digital/Yole/releases](https://github.com/vasic-digital/Yole/releases)

### Step 2: Point Yole to Your Notebook

1. Open Yole
2. Go to **Settings** > **General** > **Notebook folder**
3. Select the same directory Markor uses as its Notebook folder
   - Default Markor location: `/storage/emulated/0/Documents/markor/`
   - Or wherever you configured Markor's Notebook

Yole will immediately see all your existing files. No copying or conversion needed.

### Step 3: Verify Your Files

1. Open the **Files** tab in Yole
2. Browse through your documents and verify they open correctly
3. Check Markdown rendering in Preview mode
4. Check Todo.txt files in the To-Do tab

### Step 4: Configure Cloud Storage (Optional)

If you previously used external sync apps (Syncthing, Nextcloud app, Dropbox app) with Markor, you can now set up Yole's built-in cloud storage:

1. Go to **Settings** > **Cloud Storage** > **Add Provider**
2. Choose your provider (see [Cloud Storage Setup Guides](user-guide/cloud-storage/README.md))
3. Configure and test the connection
4. Your files will sync through Yole directly

You can continue using external sync apps alongside Yole's built-in sync if you prefer.

### Step 5: Configure Settings

Review and configure Yole's settings to match your Markor preferences:

| Markor Setting | Yole Equivalent |
|---------------|-----------------|
| Font size | Settings > Editor > Font size |
| Font family | Settings > Editor > Font family |
| Line numbering | Settings > Editor > Show line numbers |
| Auto-save | Settings > Editor > Auto-save |
| Theme | Settings > Appearance > Theme |
| Render links | Settings > Editor > Link highlighting |
| QuickNote file | Settings > General > QuickNote path |
| Todo.txt file | Settings > General > Todo.txt path |

### Step 6: Uninstall Markor (Optional)

Once you are satisfied that Yole works correctly with all your files, you can optionally uninstall Markor. Since both apps use the same plain-text files on disk, there is no data loss risk.

---

## Settings Migration

### Manually Migrated Settings

Yole does not automatically import Markor settings. You need to configure these manually:

- **Editor preferences**: font, size, line numbers, word wrap, auto-save
- **Theme**: light, dark, or system
- **Notebook folder path**: must be set explicitly
- **QuickNote and Todo.txt paths**: must be set explicitly
- **Keyboard shortcuts**: Yole uses a different (but similar) shortcut scheme

### Settings Not Applicable

Some Markor settings do not have Yole equivalents because the features work differently:

- **Render mode** (WebView vs. HTML) -- Yole uses Compose-based rendering
- **Android-specific permissions** -- handled through Android system settings
- **Share intent filters** -- Yole registers its own intent filters

---

## Feature Comparison

### Features in Both Markor and Yole

- Markdown editing with live preview
- Todo.txt task management with projects and contexts
- QuickNote for quick access
- File browser with folder navigation
- Search within files
- Dark mode / light mode / system theme
- Line numbers
- Export to PDF / HTML
- Share files
- Auto-save
- Keyboard shortcuts

### Features New in Yole

- **17 text formats** instead of 3
- **Cross-platform**: use the same app on Android, Desktop, iOS, and Web
- **Built-in cloud storage**: 8 providers without needing external apps
- **Offline-first sync**: download files for local editing, sync when online
- **Format auto-detection**: by extension and content analysis
- **Advanced Markdown**: 16+ Flexmark extensions (footnotes, math, abbreviations, etc.)
- **Circuit breaker and rate limiting**: resilient network operations
- **Modular architecture**: 10 independent KMP modules

### Features Being Ported

The following Markor features are being ported to Yole (in development):

- Advanced toolbar actions (Markdown-specific formatting buttons)
- File encryption (encrypt/decrypt documents at rest)
- Calendar integration (view todos by date)
- Attachment handling (embed images in Markdown)

---

## FAQ

### Can I use both Markor and Yole at the same time?

Yes. Both apps work with plain text files and can share the same Notebook folder. However, avoid editing the same file in both apps simultaneously to prevent conflicts.

### Will Yole overwrite or modify my Markor files?

No. Yole reads and writes standard plain text files. It does not add metadata, headers, or other artifacts to your files. A file saved by Yole is identical in format to a file saved by Markor.

### Do I need to change file extensions?

No. Yole supports all the same extensions Markor uses (`.md`, `.txt`, `.markdown`). It also supports many additional extensions for new formats.

### What about my Markor widgets (Android)?

Yole's Android widgets are in development. In the meantime, you can keep Markor installed solely for its widgets while using Yole as your primary editor.

### Is there a way to sync settings between Markor and Yole?

No automated sync exists. Settings must be configured manually in Yole. This is a one-time setup.

---

## Related Documentation

- [Getting Started](user-guide/getting-started.md) -- New user guide
- [Cloud Storage Setup](user-guide/cloud-storage/README.md) -- Set up cloud providers
- [Format Guides](user-guide/formats/README.md) -- Learn about all 17 formats
- [Troubleshooting](TROUBLESHOOTING.md) -- Common issues and solutions

---

*Last updated: March 7, 2026*
