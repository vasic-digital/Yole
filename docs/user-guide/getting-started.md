# Getting Started with Yole

Welcome to Yole - a powerful, cross-platform text editor supporting 18+ markup formats!

## Quick Start (5 minutes)

### 1. Installation

#### Android
- Download from [F-Droid](https://f-droid.org/packages/digital.vasic.yole/) (recommended)
- Or download APK from [GitHub Releases](https://github.com/vasic-digital/Yole/releases)
- Install and grant storage permissions

#### Desktop (Windows, macOS, Linux)
```bash
# Clone the repository
git clone https://github.com/vasic-digital/Yole.git
cd Yole

# Build and run
./gradlew :desktopApp:run
```

#### iOS (Coming Soon)
iOS support is in development. Stay tuned for updates!

#### Web (Coming Soon)
Progressive Web App support is planned for Q3 2026.

### 2. First Launch

When you first launch Yole:

1. **Grant Permissions** (Android): Allow file access to read/write documents
2. **Choose Notebook Location**: Select or create a folder for your documents
3. **Explore the Interface**: Familiarize yourself with the main screens

### 3. Create Your First Document

#### Quick Note
1. Tap the **QuickNote** tab (bottom navigation)
2. Start typing your note
3. Tap **Save** when done
4. Your note is automatically saved as `quicknote.md`

#### New File
1. Go to **Files** tab
2. Tap the **+ (Add)** button
3. Start typing your content
4. Save with a filename of your choice

### 4. Your First Markdown Document

Try this sample content:
```markdown
# My First Yole Document

Welcome to **Yole**! This is a *markdown* document.

## Features I'm Excited About

- [x] Beautiful markdown rendering
- [x] Syntax highlighting
- [ ] Learning all the formats
- [ ] Becoming a power user

## Quick Tips

1. Use `#` for headers
2. Use `**text**` for bold
3. Use `*text*` for italic
4. Use `` `code` `` for inline code

### Code Example

\```python
def hello_yole():
    print("Hello from Yole!")
\```

> **Tip**: Switch to Preview mode to see rendered output!
```

---

## Main Interface

### Bottom Navigation (Android)

- **Files** 📁 - Browse and manage documents
- **To-Do** ✓ - Task management (todo.txt)
- **QuickNote** ✏️ - Quick access notepad
- **More** ⋯ - Settings and additional options

### Top Bar

- **Document Title** - Shows current file name
- **Format Indicator** - Shows detected format
- **Action Menu** - Save, Share, Export options

---

## Key Concepts

### 1. Format Auto-Detection

Yole automatically detects document format based on:
- **File Extension**: `.md` → Markdown, `.tex` → LaTeX
- **File Content**: Headers, syntax patterns
- **Manual Selection**: Override via Format menu

### 2. Notebook Folder

Your **Notebook** is the root folder for all Yole documents:
- Organize files in subfolders
- All files remain plain text
- Compatible with any text editor
- Easy to sync (Syncthing, Nextcloud, etc.)

### 3. Edit vs. Preview

- **Edit Mode**: Type and edit raw markup
- **Preview Mode**: See rendered output (Markdown, LaTeX, etc.)
- Switch easily with toolbar button

### 4. Syntax Highlighting

Real-time syntax highlighting for:
- Markdown elements
- Code blocks (40+ languages)
- Todo.txt priorities and contexts
- LaTeX commands
- And more!

---

## Essential Workflows

### Writing a Blog Post (Markdown)

1. Create new file: `my-blog-post.md`
2. Write in Markdown format
3. Preview to see formatting
4. Export to HTML when ready
5. Copy/paste to your blog platform

### Managing Tasks (Todo.txt)

1. Go to **To-Do** tab
2. Add tasks with priorities:
   ```
   (A) High priority task +Project @Context
   (B) Medium priority task
   Regular task without priority
   ```
3. Mark complete by tapping checkbox
4. Filter by project or context

### Taking Meeting Notes

1. Use **QuickNote** for instant access
2. Use Markdown for formatting:
   ```markdown
   # Meeting: Project Review
   **Date**: 2025-11-11
   **Attendees**: Alice, Bob, Charlie

   ## Agenda
   1. Project status
   2. Next milestones
   3. Blockers

   ## Action Items
   - [ ] Alice: Update documentation
   - [ ] Bob: Fix performance issue
   - [ ] Charlie: Prepare demo
   ```
3. Convert to proper document later if needed

### Writing Documentation (reStructuredText)

1. Create file with `.rst` extension
2. Use reStructuredText syntax
3. Preview rendered output
4. Export to PDF or HTML

---

## Supported Formats (18+)

### Core Formats
- **Markdown** (.md) - CommonMark + GFM
- **Plain Text** (.txt) - Simple text with highlighting
- **Todo.txt** (.txt) - Task management format
- **CSV** (.csv) - Comma-separated values

### Wiki Formats
- **WikiText** (.wiki) - MediaWiki markup
- **Org Mode** (.org) - Emacs Org mode
- **Creole** (.creole) - Wiki Creole
- **TiddlyWiki** (.tid) - TiddlyWiki markup

### Technical Formats
- **LaTeX** (.tex) - Typesetting system
- **AsciiDoc** (.adoc) - Documentation format
- **reStructuredText** (.rst) - Python docs format

### Specialized Formats
- **Key-Value** (.ini, .properties) - Configuration files
- **TaskPaper** (.taskpaper) - Task management
- **Textile** (.textile) - Textile markup

### Data Science
- **Jupyter** (.ipynb) - Jupyter Notebooks (view-only)
- **R Markdown** (.rmd) - R Markdown documents

[See detailed format guides →](./formats/README.md)

---

## Common Tasks

### Changing Themes
1. Go to **More** → **Settings**
2. Select **Theme**
3. Choose: System, Light, or Dark

### Enabling Line Numbers
1. **Settings** → **Editor**
2. Toggle **Show line numbers**

### Setting Default Notebook
1. **Settings** → **General**
2. Tap **Notebook folder**
3. Choose directory

### Exporting Documents
1. Open document
2. Tap **Menu** (⋯)
3. Select **Export**
4. Choose format (PDF, HTML, etc.)

### Sharing Files
1. Open document
2. Tap **Share** icon
3. Choose app to share with

---

## Keyboard Shortcuts (Desktop)

| Shortcut | Action |
|----------|--------|
| `Ctrl+N` | New file |
| `Ctrl+O` | Open file |
| `Ctrl+S` | Save file |
| `Ctrl+Shift+S` | Save as |
| `Ctrl+P` | Preview toggle |
| `Ctrl+F` | Find |
| `Ctrl+H` | Replace |
| `Ctrl+/` | Comment/uncomment |
| `Ctrl++` | Zoom in |
| `Ctrl+-` | Zoom out |
| `Ctrl+0` | Reset zoom |

---

## Tips & Tricks

### 🎯 Pro Tips

1. **Use QuickNote for scratch**:
   QuickNote is perfect for temporary thoughts, clipboard storage, or quick calculations.

2. **Organize with folders**:
   Create subject-based folders (Work, Personal, Projects) in your Notebook.

3. **Leverage format auto-detection**:
   Name files with proper extensions to get automatic syntax highlighting.

4. **Master markdown shortcuts**:
   Learn common markdown syntax to write faster.

5. **Enable auto-save**:
   Never lose work - turn on auto-save in Settings.

### 🚀 Power User Features

- **Cross-linking**: Link between documents using file paths
- **Templates**: Save frequently used structures as template files
- **Batch operations**: Rename or move multiple files at once
- **Format conversion**: Convert between compatible formats
- **Backup**: Set up automatic backups with sync apps

---

## Troubleshooting

### Files not showing up?
- Check Notebook folder path in Settings
- Grant storage permissions (Android)
- Refresh file list (pull down to refresh)

### Format not detected?
- Check file extension matches format
- Try manual format selection via menu
- Update to latest version

### Preview not working?
- Some formats don't support preview
- Check format is supported (see list above)
- Clear app cache if needed

### Can't save files?
- Check storage permissions
- Ensure Notebook folder exists
- Check available storage space

### Sync issues?
- Yole doesn't sync - use external sync apps
- Recommended: Syncthing, Nextcloud, Dropbox
- Avoid editing same file on multiple devices simultaneously

[More troubleshooting →](./troubleshooting.md)

---

## Next Steps

### Learn More
- [Format-Specific Guides](./formats/README.md) - Deep dive into each format
- [Feature Documentation](./features/README.md) - Detailed feature explanations
- [Keyboard Shortcuts](./keyboard-shortcuts.md) - Complete shortcut reference

### Get Help
- [FAQ](./faq.md) - Frequently asked questions
- [Troubleshooting](./troubleshooting.md) - Common issues and solutions
- [GitHub Issues](https://github.com/vasic-digital/Yole/issues) - Report bugs

### Contribute
- [Contributing Guide](../../CONTRIBUTING.md) - Help improve Yole
- [Developer Docs](../developer-guide/README.md) - For developers
- [Translations](../../translations/README.md) - Help translate

---

## Welcome to the Yole Community!

You're now ready to use Yole productively. Happy writing!

**Questions?** Join our community:
- GitHub Discussions
- Matrix chat
- Reddit: r/YoleEditor

*Last updated: November 11, 2025*
