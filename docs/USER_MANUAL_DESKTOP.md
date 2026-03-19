<!--
SPDX-FileCopyrightText: 2025-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Yole Desktop User Manual

## Installation

### From Source (Recommended)
1. Clone the repository:
   ```bash
   git clone --recursive https://github.com/vasic-digital/Yole.git
   cd Yole
   ```
2. Ensure JDK 21 is installed and available
3. Ensure the 10 KMP sibling modules are cloned (see CONTRIBUTING.md)
4. Run the desktop application:
   ```bash
   ./gradlew :desktopApp:run
   ```
   Or use the Makefile:
   ```bash
   make desktop
   ```

### From Release Builds
1. Download the appropriate JAR from the releases directory:
   - `Yole-Desktop-linux-x64-{version}-Release-{code}.jar` for Linux
   - `Yole-Desktop-windows-x64-{version}-Release-{code}.jar` for Windows
   - `Yole-Desktop-macos-arm64-{version}-Release-{code}.jar` for macOS
2. Run with:
   ```bash
   java -jar Yole-Desktop-*.jar
   ```

### System Requirements
- **OS**: Windows 10+, macOS 11+, Linux (any modern distribution)
- **JDK**: Java 21 or higher
- **RAM**: 512 MB minimum, 1 GB recommended
- **Disk**: 100 MB for application and dependencies

## User Interface

Yole Desktop uses a Compose Desktop UI with a modern IDE-style layout.

### Main Window Layout
```
+--------------------------------------------------+
|  Menu Bar (File, Edit, View, Format, Help)       |
+--------------------------------------------------+
|  Tab Bar (open documents)                         |
+----------+---------------------------------------+
|          |                                       |
| File     |  Editor / Preview Area                |
| Browser  |                                       |
| (left    |  - Syntax highlighted editor          |
|  panel)  |  - Format-specific action bar         |
|          |  - Line numbers (toggleable)           |
|          |                                       |
+----------+---------------------------------------+
|  Status Bar (format, line:col, encoding)         |
+--------------------------------------------------+
```

### File Browser Panel
- Located on the left side of the window
- Shows your Notebook directory tree
- Double-click a file to open it in a new tab
- Right-click for context menu (rename, delete, copy, move)
- Drag-and-drop files to reorder or move between folders

### Tab Management
- Each open document appears as a tab in the tab bar
- Click a tab to switch to that document
- Middle-click or click the X on a tab to close it
- Drag tabs to reorder them
- Modified (unsaved) documents show a dot indicator on the tab

## Keyboard Shortcuts

### File Operations
| Shortcut | Action |
|----------|--------|
| `Ctrl+N` | New file |
| `Ctrl+O` | Open file |
| `Ctrl+S` | Save current file |
| `Ctrl+Shift+S` | Save as |
| `Ctrl+W` | Close current tab |
| `Ctrl+Shift+W` | Close all tabs |

### Editing
| Shortcut | Action |
|----------|--------|
| `Ctrl+Z` | Undo |
| `Ctrl+Shift+Z` | Redo |
| `Ctrl+X` | Cut |
| `Ctrl+C` | Copy |
| `Ctrl+V` | Paste |
| `Ctrl+A` | Select all |
| `Ctrl+D` | Duplicate line |
| `Ctrl+F` | Find in file |
| `Ctrl+H` | Find and replace |
| `Tab` | Indent selection |
| `Shift+Tab` | Unindent selection |
| `Alt+Up` | Move line up |
| `Alt+Down` | Move line down |

### View
| Shortcut | Action |
|----------|--------|
| `Ctrl+P` | Toggle preview mode |
| `Ctrl+L` | Toggle line numbers |
| `Ctrl+Plus` | Increase font size |
| `Ctrl+Minus` | Decrease font size |
| `Ctrl+0` | Reset font size |
| `F11` | Toggle fullscreen |
| `Ctrl+B` | Toggle file browser panel |

### Navigation
| Shortcut | Action |
|----------|--------|
| `Ctrl+Tab` | Next tab |
| `Ctrl+Shift+Tab` | Previous tab |
| `Ctrl+G` | Go to line |
| `Ctrl+1..9` | Switch to tab 1-9 |

## Working with Documents

### Creating Files
1. Use `Ctrl+N` or **File > New**
2. Choose a format from the dialog
3. Enter a filename
4. The file is created in the current directory and opened in a new tab

### Editing
- The editor provides syntax highlighting for all 17 supported formats
- Auto-save is enabled by default (configurable in Settings)
- Line numbers are shown by default (toggle with `Ctrl+L`)
- The status bar shows the current format, cursor position, and encoding

### Preview
- Toggle preview with `Ctrl+P` or the preview button in the toolbar
- Preview renders the document as formatted HTML
- Split view option shows editor and preview side by side
- Preview automatically updates as you type

### Format-Specific Features
- **Markdown**: Live preview with table rendering, math (KaTeX), mermaid diagrams
- **Todo.txt**: Task sorting, filtering by project/context, completion tracking
- **CSV**: Column-colored syntax highlighting, table preview
- **LaTeX**: Math rendering in preview mode
- **All formats**: Date/time insertion, special character palette

## Themes

### Available Themes
- **Light** -- Standard light theme for bright environments
- **Dark** -- Dark theme for reduced eye strain
- **System** -- Follows your OS theme setting

### Changing Themes
1. Go to **View > Theme** in the menu bar
2. Select your preferred theme
3. The theme applies immediately and persists across sessions

### Custom Fonts
- Place `.ttf` or `.otf` font files in your Notebook's `.app/fonts/` directory
- Select custom fonts in **Settings > Editor > Font family**

## Window Management

### Multiple Windows
- Yole supports multiple editor windows
- Each window maintains its own set of tabs
- Drag a tab out of the tab bar to open it in a new window

### Resizable Panels
- The file browser panel can be resized by dragging its border
- The preview split can be resized when in split-view mode
- Panel sizes are remembered across sessions

## Settings

Access settings via **File > Settings** or `Ctrl+,`.

### Editor Settings
- **Font family**: Choose from system fonts or custom fonts
- **Font size**: Adjust text size (8-72pt)
- **Tab width**: Number of spaces for tab indentation (2, 4, 8)
- **Line wrapping**: Enable/disable soft line wrapping
- **Auto-save**: Automatic saving interval
- **Line numbers**: Show/hide line numbers

### Appearance
- **Theme**: Light / Dark / System
- **Animations**: Enable/disable transition animations
- **Sidebar**: Default visibility and width

### File Handling
- **Default format**: Format for new files
- **Encoding**: Default text encoding (UTF-8 recommended)
- **Line endings**: LF (Unix) or CRLF (Windows)

## Cloud Storage

The desktop version supports the same 8 cloud and network protocols as Android:

- Dropbox, Google Drive, OneDrive (OAuth2)
- WebDAV, SFTP, FTP, SMB, Git

Configure in **Settings > Network Storage**. Credentials are stored securely using the OS keychain (Linux Secret Service, macOS Keychain, Windows Credential Manager).

## Troubleshooting

### Application does not start
- Verify Java 21 is installed: `java -version`
- Ensure the JAR file is not corrupted (re-download if needed)
- Check system memory availability

### Build from source fails
- Ensure all 10 KMP sibling modules are cloned alongside the Yole directory
- Run `git submodule update --init --recursive`
- Check that JDK 21 is the active Java version

### Fonts not rendering correctly
- Install the required fonts on your system
- Try a different font in Settings
- On Linux, ensure fontconfig is installed

### High DPI display issues
- Yole uses Compose Desktop which handles HiDPI automatically
- If scaling looks wrong, check your OS display scaling settings
- Set the `GDK_SCALE` environment variable on Linux if needed
