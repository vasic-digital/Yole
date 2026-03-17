# Yole Desktop User Manual

**Version**: 1.0
**Date**: 2026-03-17
**Platforms**: Windows, macOS, Linux
**Status**: Beta

---

## Installation

### Windows

1. Download `yole-windows.msi` from the [GitHub Releases](https://github.com/vasic-digital/Yole/releases) page
2. Run the installer
3. Follow the installation wizard
4. Launch Yole from the Start menu

### macOS

1. Download `yole-macos.dmg` from the [GitHub Releases](https://github.com/vasic-digital/Yole/releases) page
2. Open the DMG file
3. Drag Yole to your Applications folder
4. Launch from Applications (you may need to allow it in **System Preferences > Security & Privacy**)

### Linux

**Debian/Ubuntu**:
```bash
sudo dpkg -i yole-linux.deb
```

**Fedora/RHEL**:
```bash
sudo rpm -i yole-linux.rpm
```

**Run from source**:
```bash
git clone https://github.com/vasic-digital/Yole.git
cd Yole
./gradlew :desktopApp:run
```

### Build Distributables

```bash
# Package for your current OS
./gradlew :desktopApp:packageDistributionForCurrentOS

# Platform-specific packaging
./gradlew :desktopApp:packageMsi     # Windows
./gradlew :desktopApp:packageDmg     # macOS
./gradlew :desktopApp:packageDeb     # Linux (Debian)
```

---

## Getting Started

### Main Window

The Yole desktop application uses Compose Desktop for a native look and feel. The main window consists of:

- **Menu Bar** -- File, Edit, View, Format, Tools, Help menus
- **Toolbar** -- Quick access to common actions
- **File Tree** (left panel) -- Navigate your file system
- **Editor** (center) -- Edit documents with syntax highlighting
- **Preview** (right panel, toggleable) -- Live preview for supported formats

### Creating a New Document

1. **File > New** or press `Ctrl+N` (Windows/Linux) / `Cmd+N` (macOS)
2. Choose a format from the format selector
3. Start typing in the editor
4. **File > Save** or `Ctrl+S` / `Cmd+S` to save

### Opening a File

1. **File > Open** or `Ctrl+O` / `Cmd+O`
2. Navigate to the file in the file dialog
3. Select the file and click **Open**
4. Format is detected automatically by extension

---

## Keyboard Shortcuts

### General

| Action | Windows/Linux | macOS |
|--------|--------------|-------|
| New file | `Ctrl+N` | `Cmd+N` |
| Open file | `Ctrl+O` | `Cmd+O` |
| Save | `Ctrl+S` | `Cmd+S` |
| Save As | `Ctrl+Shift+S` | `Cmd+Shift+S` |
| Close file | `Ctrl+W` | `Cmd+W` |
| Quit | `Ctrl+Q` | `Cmd+Q` |
| Preferences | `Ctrl+,` | `Cmd+,` |

### Editing

| Action | Windows/Linux | macOS |
|--------|--------------|-------|
| Undo | `Ctrl+Z` | `Cmd+Z` |
| Redo | `Ctrl+Shift+Z` | `Cmd+Shift+Z` |
| Cut | `Ctrl+X` | `Cmd+X` |
| Copy | `Ctrl+C` | `Cmd+C` |
| Paste | `Ctrl+V` | `Cmd+V` |
| Select All | `Ctrl+A` | `Cmd+A` |
| Find | `Ctrl+F` | `Cmd+F` |
| Find and Replace | `Ctrl+H` | `Cmd+H` |
| Go to Line | `Ctrl+G` | `Cmd+G` |

### View

| Action | Windows/Linux | macOS |
|--------|--------------|-------|
| Toggle Preview | `Ctrl+P` | `Cmd+P` |
| Toggle File Tree | `Ctrl+B` | `Cmd+B` |
| Zoom In | `Ctrl++` | `Cmd++` |
| Zoom Out | `Ctrl+-` | `Cmd+-` |
| Reset Zoom | `Ctrl+0` | `Cmd+0` |
| Full Screen | `F11` | `Cmd+Ctrl+F` |

### Formatting (Markdown mode)

| Action | Windows/Linux | macOS |
|--------|--------------|-------|
| Bold | `Ctrl+B` | `Cmd+B` |
| Italic | `Ctrl+I` | `Cmd+I` |
| Code | `Ctrl+`` ` | `Cmd+`` ` |
| Link | `Ctrl+K` | `Cmd+K` |
| Heading | `Ctrl+1..6` | `Cmd+1..6` |

---

## Format Support

All 17 text formats are supported with full parsing and HTML preview:

- **Markdown** (.md) -- Full CommonMark + GFM with live preview
- **Todo.txt** (.txt) -- Task management with priorities, projects, contexts
- **CSV** (.csv) -- Table view with column highlighting
- **Plain Text** (.txt, .log) -- Universal text editing
- **LaTeX** (.tex) -- Math equations and document structure
- **Org Mode** (.org) -- Headings, TODO states, tables
- **WikiText** (.wiki) -- MediaWiki-style markup
- **AsciiDoc** (.adoc) -- Technical documentation
- **reStructuredText** (.rst) -- Python documentation standard
- **Key-Value** (.properties, .ini, .env) -- Configuration files
- **TaskPaper** (.taskpaper) -- Project-based task management
- **Textile** (.textile) -- Lightweight markup
- **Creole** (.creole) -- Wiki markup standard
- **TiddlyWiki** (.tid) -- Personal wiki format
- **Jupyter** (.ipynb) -- Notebook JSON viewing
- **R Markdown** (.Rmd) -- R code chunks with markdown
- **Binary** -- Detection and display of binary file info

---

## Cloud and Network Storage

Desktop supports all 8 storage protocols:

- **Dropbox** -- OAuth2 authentication via browser popup
- **Google Drive** -- OAuth2 authentication via browser popup
- **OneDrive** -- OAuth2 authentication via browser popup
- **WebDAV** -- Username/password or certificate authentication
- **FTP** -- Standard FTP with optional TLS
- **SFTP** -- SSH-based file transfer
- **SMB** -- Windows network shares
- **Git** -- Git repository sync with SSH or HTTPS

Configure via **Tools > Storage Settings**.

---

## Theme Customization

### Built-in Themes

- **Light** -- Light background, dark text (default)
- **Dark** -- Dark background, light text
- **System** -- Follow OS theme setting

### Custom Colors

Access **Preferences > Appearance** to customize:
- Editor background and foreground colors
- Syntax highlighting colors per token type
- Preview CSS themes

### Font Settings

- **Editor Font** -- Choose any installed monospace font
- **Font Size** -- Adjustable from 8pt to 48pt
- **Line Height** -- Configure line spacing

---

## Troubleshooting

### Application Does Not Start
- Ensure JDK 21+ is installed (check with `java -version`)
- On macOS, allow the app in **System Preferences > Security & Privacy**
- Check available disk space
- Run from terminal to see error output: `./gradlew :desktopApp:run`

### Slow Performance
- Increase JVM heap size:
  ```bash
  ./gradlew :desktopApp:run -Dorg.gradle.jvmargs="-Xmx2g"
  ```
- Close unused files
- Disable live preview for very large documents

### File Encoding Issues
- Yole defaults to UTF-8
- Check the file encoding in the status bar
- Use **File > Reopen with Encoding** to change encoding

---

**Last Updated**: 2026-03-17
