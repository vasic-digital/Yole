# Frequently Asked Questions (FAQ)

**Last Updated**: November 11, 2025
**Yole Version**: 2.15.1+

---

## General Questions

### What is Yole?

Yole is a versatile, cross-platform text editor that supports 18+ markup formats including Markdown, Todo.txt, CSV, LaTeX, and more. It's designed for writers, developers, students, and anyone who works with text files.

### Is Yole free?

Yes! Yole is completely free and open source, released under the Apache 2.0 license. No ads, no tracking, no premium features locked behind paywalls.

### Which platforms are supported?

- ✅ **Android**: Fully supported (Android 4.3+, minSdk 18, targetSdk 35)
- ✅ **Desktop**: Beta support (Windows, macOS, Linux via JVM)
- 🚧 **iOS**: In development
- 🚧 **Web (PWA)**: Planned for Q3 2026

### Is Yole offline-first?

Yes! Yole works completely offline. No internet connection required. All your files are stored locally on your device.

### Does Yole sync my files?

No, Yole doesn't include built-in sync. However, since all files are plain text, you can use any sync solution:
- **Recommended**: Syncthing (P2P, open source)
- **Cloud**: Dropbox, Google Drive, OneDrive, Nextcloud
- **Version Control**: Git, for tech-savvy users

---

## Installation & Setup

### How do I install Yole on Android?

1. Download from [F-Droid](https://f-droid.org/) (recommended)
2. Or download APK from [GitHub Releases](https://github.com/vasic-digital/Yole/releases)
3. Install and grant storage permissions
4. Choose your Notebook folder

### How do I install Yole on Desktop?

```bash
# Clone repository
git clone https://github.com/vasic-digital/Yole.git
cd Yole

# Build and run
./gradlew :desktopApp:run
```

Or download pre-built binaries from GitHub Releases (when available).

### What is a "Notebook" folder?

Your Notebook is the root folder where Yole stores all your documents. Think of it as your personal library of text files. You can organize files into subfolders as you like.

### Can I change my Notebook location?

Yes! Go to **Settings** → **General** → **Notebook folder** and choose a new location.

### Why does Yole need storage permissions?

Yole needs permission to read and write your text files. On Android, this is required to access files in your chosen Notebook folder.

---

## File Formats

### What formats does Yole support?

Yole supports 18+ formats:
- **Core**: Markdown, Plain Text, Todo.txt, CSV
- **Wiki**: WikiText, Org Mode, Creole, TiddlyWiki
- **Technical**: LaTeX, AsciiDoc, reStructuredText
- **Specialized**: Key-Value, TaskPaper, Textile
- **Data Science**: Jupyter, R Markdown
- **Binary**: Binary file detection

[See complete format list →](./formats/README.md)

### How does Yole detect file formats?

Yole uses three methods:
1. **File extension** (e.g., `.md` → Markdown)
2. **File content** (analyzing syntax patterns)
3. **Manual selection** (override via Format menu)

### Can I edit multiple formats in one app?

Yes! That's Yole's superpower. Switch between Markdown, Todo.txt, LaTeX, and more - all in one app.

### Does Yole convert between formats?

Not yet, but format conversion is planned for a future release. For now, files remain in their original format.

### What happens to my files?

Your files remain plain text. Yole never locks you in - you can open your files in any text editor, now or 50 years from now.

---

## Features

### Does Yole have syntax highlighting?

Yes! Real-time syntax highlighting for all supported formats, including:
- Markdown elements (headers, bold, italic, links)
- Todo.txt priorities and contexts
- LaTeX commands
- Code blocks (40+ programming languages)

### Can I preview formatted documents?

Yes! Toggle between **Edit** and **Preview** modes:
- **Edit**: See raw markup with syntax highlighting
- **Preview**: See rendered output (Markdown, LaTeX, etc.)

### Does Yole have dark mode?

Yes! Choose from:
- **System**: Follow device theme
- **Light**: Always light theme
- **Dark**: Always dark theme

Go to **Settings** → **Appearance** → **Theme**

### Can I export documents?

Yes! Export options include:
- **PDF**: For sharing and printing
- **HTML**: For web publishing
- **Plain Text**: Strip formatting

(Feature availability varies by format)

### Does Yole have auto-save?

Yes! Enable in **Settings** → **Editor** → **Auto-save**. Saves every 30 seconds and on app switch.

### Can I use keyboard shortcuts?

Yes, on desktop! Common shortcuts:
- `Ctrl+N`: New file
- `Ctrl+S`: Save
- `Ctrl+P`: Preview toggle
- `Ctrl+F`: Find
- [See full list →](./keyboard-shortcuts.md)

### Does Yole support encryption?

Yes! Yole includes AES-256 encryption for sensitive files. Use responsibly and remember your password (it cannot be recovered).

---

## Markdown-Specific

### What Markdown flavor does Yole use?

CommonMark + GitHub Flavored Markdown (GFM), including:
- Standard Markdown syntax
- Tables
- Task lists
- Strikethrough
- Footnotes (where supported)

### Why don't my Markdown tables render?

Make sure you're using correct GFM table syntax:
```markdown
| Column 1 | Column 2 |
|----------|----------|
| Data 1   | Data 2   |
```

### How do I add images to Markdown?

```markdown
![Alt text](path/to/image.jpg)
```

Use relative paths for local images in your Notebook folder.

### Can I use math in Markdown?

Math rendering support varies. For complex equations, use LaTeX format instead.

---

## Todo.txt-Specific

### What is Todo.txt format?

Todo.txt is a simple, text-based task management format. [Learn more →](http://todotxt.org/)

### How do I set task priority?

Start line with priority in parentheses:
```
(A) High priority task
(B) Medium priority task
(C) Low priority task
```

### How do I mark tasks complete?

In the To-Do screen, tap the checkbox. Or manually add `x` and completion date:
```
x 2025-11-11 Completed task
```

### How do I organize tasks by project?

Use `+` prefix:
```
(A) Update website +WebsiteProject
Write blog post +Blog +Marketing
```

### How do I filter tasks by context?

Use `@` prefix:
```
(B) Call dentist @phone
Fix printer @office
```

Then search for `@phone` or `@office` to filter.

---

## Troubleshooting

### Files not showing in file browser?

**Check**:
1. Notebook folder path in Settings
2. Storage permissions granted
3. Files actually exist in the folder
4. Pull down to refresh file list

### App crashes on startup?

**Try**:
1. Restart app
2. Clear app cache (Settings → Apps → Yole → Clear Cache)
3. Check Android version (minSdk 18, Android 4.3+)
4. Report issue on GitHub with crash log

### Format not detected correctly?

**Solutions**:
1. Check file extension matches format
2. Try manual format selection (Format menu)
3. Verify file content matches format syntax
4. Update to latest version

### Preview not working?

**Check**:
1. Format supports preview (not all do)
2. File has valid syntax
3. Try Edit mode and fix syntax errors
4. Restart app if needed

### Can't save files?

**Check**:
1. Storage permissions granted
2. Notebook folder exists and is writable
3. Sufficient storage space
4. File not open in another app

### Syntax highlighting not working?

**Solutions**:
1. Check format is correctly detected
2. Verify syntax is valid
3. Try closing and reopening file
4. Update to latest version

### Performance issues with large files?

**Tips**:
1. Split large files into smaller ones
2. Use PlainText format for logs (faster)
3. Close unused files
4. Restart app to clear memory

[More troubleshooting →](./troubleshooting.md)

---

## Data & Privacy

### Where are my files stored?

On your device, in the Notebook folder you chose. Yole has no cloud storage.

### Does Yole collect my data?

No. Yole is completely offline and collects no data. No analytics, no tracking, no telemetry.

### Can I backup my files?

Yes! Since all files are plain text:
1. Use any backup solution (cloud, local, both)
2. Copy Notebook folder to backup location
3. Use sync apps (Syncthing, Nextcloud)
4. Commit to Git repository

### What if I lose my device?

If you've set up backups/sync, restore from backup. Otherwise, files are lost. **Always backup important files!**

### Are encrypted files recoverable without password?

No. AES-256 encryption is strong. **Lost password = lost file**. Use encryption responsibly.

---

## Advanced Usage

### Can I use Yole with Git?

Absolutely! All files are plain text, perfect for version control:
```bash
cd /path/to/notebook
git init
git add .
git commit -m "Initial commit"
```

### Can I edit files from other apps?

Yes! Since files are plain text, edit in any app. Changes sync via file system.

### Can I use external keyboards?

Yes! Desktop shortcuts work with external keyboards on Android/iOS.

### Can I customize syntax highlighting?

Not yet, but theme customization is planned for a future release.

### Can I write plugins/extensions?

Not currently. Plugin system is on the roadmap. Follow GitHub for updates.

### Can I self-host sync?

Yes! Use Nextcloud, Syncthing, or any self-hosted solution. Yole just reads/writes files.

---

## Development & Contributing

### Is Yole open source?

Yes! Apache 2.0 license. Source code: https://github.com/vasic-digital/Yole

### Can I contribute code?

Yes! Contributions welcome. See [CONTRIBUTING.md](../../CONTRIBUTING.md) for guidelines.

### How do I report bugs?

Open an issue on [GitHub Issues](https://github.com/vasic-digital/Yole/issues) with:
- Device info (OS, version)
- Steps to reproduce
- Expected vs actual behavior
- Screenshots if applicable

### Can I suggest features?

Yes! Open a feature request on GitHub Issues. Check existing requests first.

### How do I build from source?

```bash
git clone https://github.com/vasic-digital/Yole.git
cd Yole
./gradlew assembleFlavorDefaultDebug  # Android
./gradlew :desktopApp:run              # Desktop
```

### What's the development roadmap?

See [GitHub Projects](https://github.com/vasic-digital/Yole/projects) for roadmap and milestones.

---

## Comparison with Other Apps

### Yole vs. Markor?

**Yole**:
- 18+ formats (Markdown, Todo.txt, LaTeX, CSV, more)
- Cross-platform (Android, Desktop, iOS*, Web*)
- KMP architecture

**Markor**:
- Primarily Markdown and Todo.txt
- Android only
- More mature (longer development history)

Both are excellent open-source choices!

### Yole vs. Obsidian?

**Yole**:
- Simple, straightforward editor
- Multiple markup formats
- Completely free and open source
- Offline-first, plain text

**Obsidian**:
- Knowledge management system
- Graph view, linking, plugins
- Freemium (sync costs money)
- Primarily Markdown

Different tools for different needs!

### Yole vs. Notion?

**Yole**:
- Plain text files
- Offline-first
- Open source
- Multiple formats

**Notion**:
- Cloud-based
- Collaborative
- Rich databases and views
- Proprietary format

Notion is for teams/collaboration, Yole is for personal text editing.

---

## Future Plans

### When will iOS version be ready?

iOS support is in active development. Expected: Q2 2026. Follow GitHub for updates.

### When will Web (PWA) version launch?

Web PWA is planned for Q3 2026 after iOS stabilization.

### Will you add format X?

Maybe! Open a feature request on GitHub. Popular formats get priority.

### Will you add cloud sync?

Not built-in (goes against offline-first philosophy), but we may add integration helpers for popular sync services.

### Will Yole stay free?

Yes, forever. Open source is our commitment.

---

## Getting Help

### Where can I get support?

1. **This FAQ**: Check common questions
2. **Documentation**: [User Guide](./getting-started.md)
3. **GitHub Issues**: Bug reports and questions
4. **Community**: GitHub Discussions
5. **Email**: support@vasic.digital (for sensitive issues)

### How can I help Yole?

1. **Use it**: Try Yole and provide feedback
2. **Star on GitHub**: Help others discover it
3. **Report bugs**: Help improve quality
4. **Contribute code**: See CONTRIBUTING.md
5. **Write docs**: Documentation always needs help
6. **Spread the word**: Tell others about Yole

---

**Still have questions?** Open an issue on [GitHub](https://github.com/vasic-digital/Yole/issues) or check our [full documentation](./README.md).

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
