# Yole Getting Started Guide

Welcome to Yole, a cross-platform text editor supporting 17+ formats!

## Quick Start

### Installation

**Android**
1. Download from F-Droid or GitHub Releases
2. Install the APK
3. Open Yole

**Desktop**
```bash
./gradlew :desktopApp:run
```

**Web**
```bash
./gradlew :webApp:wasmJsBrowserRun
```

## Your First Document

### Creating a Document

1. Tap the **+** button to create a new document
2. Select a format (Markdown, Plain Text, etc.)
3. Start typing!

### Editing

- Use the toolbar for formatting shortcuts
- Enable **Preview** to see rendered output
- Use **Save** to persist changes

## Supported Formats

| Format | Extension | Use Case |
|--------|-----------|----------|
| Markdown | .md | Documentation, notes |
| Todo.txt | .txt | Task management |
| CSV | .csv | Spreadsheet data |
| LaTeX | .tex | Scientific papers |
| Org Mode | .org | Planning, notes |

## Keyboard Shortcuts

### Desktop
- `Ctrl+N` - New document
- `Ctrl+O` - Open
- `Ctrl+S` - Save
- `Ctrl+F` - Find
- `Ctrl+,` - Settings

### Android
- Long-press for context menu
- Swipe for format selection

## Features

### Format Detection
Yole automatically detects format from file extension and content.

### Cloud Storage
Connect to:
 Google Drive
-- Dropbox
- OneDrive
- FTP/SFTP
- WebDAV

### Security
- AES-256 encryption
- Secure credential storage
- No telemetry

## Tips

### Markdown Tips
- Use `#` for headers
- `**bold**` for bold
- `*italic*` for italic
- `[link](url)` for links

### Task Management
- `(A)` for priority
- `@context` for contexts
- `+project` for projects

## Next Steps

- Read [Format Guides](../formats/) for detailed syntax
- Check [Contributing Guide](../CONTRIBUTING.md) to contribute
- Explore advanced features in Settings

---

Need help? Open an issue on GitHub!
