<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Yole Android User Manual

## Installation

### From F-Droid
1. Install the [F-Droid](https://f-droid.org/) app store on your Android device
2. Search for "Yole" in F-Droid
3. Tap **Install** and grant any requested permissions
4. Open Yole from your app drawer

### From GitHub Releases
1. Visit [Yole Releases](https://github.com/vasic-digital/Yole/releases/latest)
2. Download the `.apk` file for the latest version
3. On your device, enable "Install from unknown sources" in Settings > Security
4. Open the downloaded APK and tap **Install**
5. Launch Yole from your app drawer

### Requirements
- Android 7.0 (API 24) or higher
- Approximately 20 MB storage for the app
- No internet connection required (offline-first)

## First Launch

When you first open Yole, you will see three main sections accessible by swiping:

1. **Notebook** -- Your file browser and document list
2. **ToDo** -- Your main todo.txt task list
3. **QuickNote** -- A fast-access Markdown note

### Setting Up Your Notebook
1. By default, Yole uses the internal "Documents" directory
2. To change it: go to **Settings > General > Notebook root folder**
3. Select any accessible directory on your device or SD card

## Working with Formats

Yole supports 17 text formats. The format is automatically detected based on file extension.

### Selecting a Format
- When creating a new file, choose the format from the **New File** dialog
- Yole remembers your last used format
- For `.txt` files, Yole detects whether it is plain text or todo.txt format

### Supported Formats
| Format | Extensions | Best For |
|--------|-----------|----------|
| Markdown | `.md`, `.markdown` | Notes, documentation, blogging |
| Todo.txt | `.txt` (detected) | Task management, to-do lists |
| Plain Text | `.txt`, `.text`, `.log` | General text, logs |
| CSV | `.csv` | Data tables, spreadsheets |
| LaTeX | `.tex`, `.latex` | Academic papers, math equations |
| Org Mode | `.org` | Emacs-style notes and tasks |
| AsciiDoc | `.adoc`, `.asciidoc` | Technical documentation |
| WikiText | `.wiki`, `.mediawiki` | Wiki-style documents |
| Key-Value | `.properties`, `.ini`, `.env` | Configuration files |
| TaskPaper | `.taskpaper` | Project-based task management |
| And 7 more... | Various | See format guides |

### Editing Documents
1. Tap any file in the Notebook to open it
2. The editor opens with syntax highlighting for the detected format
3. Use the **action buttons** at the bottom for format-specific shortcuts:
   - Markdown: bold, italic, headers, links, images
   - Todo.txt: priority, project, context, due date
   - All formats: date/time insertion, special characters

### Previewing Documents
1. While editing, tap the **eye icon** or swipe to switch to preview mode
2. Preview renders the document as formatted HTML
3. In preview mode:
   - Links are clickable
   - Images are displayed inline
   - Tables are rendered
   - Code blocks have syntax highlighting

## Cloud Storage Integration

Yole supports 8 cloud and network storage protocols for syncing files.

### Supported Protocols
- **Dropbox** -- OAuth2 cloud sync
- **Google Drive** -- OAuth2 cloud sync
- **OneDrive** -- OAuth2 cloud sync
- **WebDAV** -- NextCloud, OwnCloud, and other WebDAV servers
- **SFTP** -- Secure file transfer to any SSH server
- **FTP** -- Standard file transfer protocol
- **SMB** -- Windows network shares
- **Git** -- Git repository sync

### Setting Up Cloud Storage
1. Go to **Settings > Network Storage**
2. Select your protocol
3. Enter connection details (server, credentials, or OAuth2 login)
4. Choose a local sync directory
5. Yole will sync files automatically when connected

### Offline Mode
- Yole caches files locally for offline access
- Changes made offline are synced when connectivity is restored
- Conflict resolution uses last-write-wins with local backup

## Todo.txt Guide

### Creating Tasks
Each line in a todo.txt file is a separate task. Format:

```
(A) 2026-03-19 Call dentist +health @phone due:2026-03-25
```

Components:
- `(A)` -- Priority (A-Z, A is highest)
- `2026-03-19` -- Creation date
- `+health` -- Project tag
- `@phone` -- Context tag
- `due:2026-03-25` -- Due date

### Completing Tasks
- Tap a task and use the **Done** action button
- The task is prefixed with `x` and a completion date
- Optionally archive completed tasks to a done.txt file

### Filtering and Searching
- Use the **search** button to filter tasks
- Filter by project: tap a `+project` tag
- Filter by context: tap an `@context` tag
- Advanced query syntax supports AND/OR/NOT operators

### Sorting
- Long-press the sort button for sort options
- Sort by priority, date, project, context, or due date
- Sort order is remembered per file

## Settings

### Theme
- **Light / Dark / System** -- Choose your preferred theme
- Dark theme reduces battery usage on OLED screens
- System follows your device's dark mode setting

### Editor Settings
- **Font size** -- Adjust text size (per-file or global)
- **Font family** -- Choose from bundled fonts or load custom fonts from `Notebook/.app/fonts/`
- **Line numbers** -- Show/hide line numbers in editor and preview
- **Auto-save** -- Enable/disable automatic saving when leaving the app
- **Line wrapping** -- Configure wrap mode

### Animation Settings
- Enable/disable smooth transitions between screens
- Configurable for performance optimization on older devices

### File Encryption
1. Set a master password in **Settings > General > File encryption password**
2. Toggle "Encrypt file content" when creating a new note
3. Files are encrypted with AES-256
4. Only files encrypted with the current password auto-decrypt

## Tips and Tricks

### Keyboard Shortcuts (with external keyboard)
- **Tab** -- Insert indent
- **Shift+Tab** -- Remove indent

### Sharing
- Share text from any app into Yole (creates a new note or appends to existing)
- Share from Yole to other apps via the share button
- Export documents as HTML or PDF from the preview mode

### Widgets
- Add Yole widgets to your home screen for quick access to Notebook, ToDo, or QuickNote
- Requires enabling "Launcher (Special Documents)" in Settings

### File Sync
Yole is offline-first but works with third-party sync apps:
- **Syncthing** (recommended) -- See the [Syncthing guide](../doc/2020-04-04-syncthing-file-sync-setup-how-to-use-with-markor.md)
- **Dropbox**, **NextCloud**, **FolderSync**, and others

## Troubleshooting

### Files not showing in Notebook
- Ensure the Notebook root folder is set correctly in Settings
- Check file permissions if using an SD card
- Try the pull-down refresh gesture in the file browser

### Preview not rendering
- Ensure the file has the correct extension for its format
- Check that the format parser is available in Settings

### SD Card Access
- On Android 7+, SD card access requires SAF (Storage Access Framework)
- Follow the on-screen dialog to grant folder access
- Files on unwritable paths are shown in red

### Battery Usage
- Yole is designed for minimal battery usage
- No background services run when the app is closed
- No internet connections are made unless you reference external resources
