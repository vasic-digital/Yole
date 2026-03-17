# Yole Android User Manual

**Version**: 1.0
**Date**: 2026-03-17
**Platform**: Android 5.0+ (API 18+)

---

## Installation

### Google Play Store

1. Open the Google Play Store on your Android device
2. Search for "Yole Text Editor"
3. Tap **Install**
4. Wait for the download and installation to complete
5. Tap **Open** to launch Yole

### APK Sideload

1. Download the APK from the [GitHub Releases](https://github.com/vasic-digital/Yole/releases) page
2. On your device, go to **Settings > Security > Unknown Sources** and enable it (or grant permission to the browser/file manager)
3. Open the downloaded APK file
4. Tap **Install**
5. Once installed, tap **Open**

### Build from Source

```bash
git clone https://github.com/vasic-digital/Yole.git
cd Yole
./gradlew :androidApp:assembleFlavorDefaultDebug
adb install androidApp/build/outputs/apk/flavorDefault/debug/androidApp-flavorDefault-debug.apk
```

---

## Getting Started

### First Launch

When you first open Yole, you will see the main file browser. Yole stores documents in its internal storage by default.

### Creating a New Document

1. Tap the **+** (New Document) button
2. Choose a format from the list (Markdown, Todo.txt, Plain Text, etc.)
3. Enter a filename
4. Tap **Create**
5. Start editing in the built-in editor

### Opening an Existing File

1. Navigate to the file using the file browser
2. Tap the file to open it
3. Yole automatically detects the format based on file extension
4. The editor loads with format-specific syntax highlighting

### Saving Documents

- Documents save automatically as you type
- Tap the save icon in the toolbar to force-save
- Long-press the save icon for save options (save as, export)

---

## Format-by-Format Usage

### Markdown (.md, .markdown)

**Features**: Headers, bold, italic, links, images, code blocks, tables, task lists, math (KaTeX)

**Toolbar Actions**:
- **B** -- Bold (`**text**`)
- **I** -- Italic (`*text*`)
- **H** -- Insert heading (H1-H6)
- **Link** -- Insert link (`[text](url)`)
- **Image** -- Insert image (`![alt](url)`)
- **Code** -- Insert code block
- **Table** -- Insert table
- **List** -- Insert list (ordered/unordered)
- **Task** -- Insert task list (`- [ ] task`)

**Preview**: Tap the preview icon to see rendered HTML output with CSS styling.

### Todo.txt (.txt with todo.txt format)

**Features**: Priorities, projects, contexts, due dates, completion tracking

**Toolbar Actions**:
- **Priority** -- Set task priority (A-Z)
- **Complete** -- Toggle task completion
- **Context** -- Add @context
- **Project** -- Add +project
- **Due** -- Set due date
- **Sort** -- Sort tasks by priority, date, or project
- **Filter** -- Filter by priority, context, or project
- **Archive** -- Move completed tasks to done.txt

**Query Syntax**: Use the search bar with advanced query operators:
- `+project` -- Filter by project
- `@context` -- Filter by context
- `(A)` -- Filter by priority
- `due:2026-04-01` -- Filter by due date

### CSV (.csv)

**Features**: Table view, column highlighting, header detection

**Toolbar Actions**:
- **Table View** -- Toggle between raw text and table preview
- **Sort** -- Sort by any column
- **Filter** -- Filter rows by content

### LaTeX (.tex, .latex)

**Features**: Math equations, document structure, environments

**Toolbar Actions**:
- **Math** -- Insert math delimiters (`$...$` or `$$...$$`)
- **Section** -- Insert section command
- **Environment** -- Insert `\begin{}`/`\end{}` pair

### Org Mode (.org)

**Features**: Headings, TODO states, tags, timestamps, tables, code blocks

**Toolbar Actions**:
- **TODO** -- Cycle through TODO states
- **Tag** -- Add tags
- **Time** -- Insert timestamp
- **Table** -- Insert org-mode table

### WikiText (.wiki)

**Features**: MediaWiki-style headings, links, tables, formatting

### AsciiDoc (.adoc)

**Features**: Sections, admonitions, code blocks, cross-references

### reStructuredText (.rst)

**Features**: Section headers, directives, roles, tables

### TaskPaper (.taskpaper)

**Features**: Projects, tasks, tags, notes

### Textile (.textile)

**Features**: Text formatting, headings, tables, links

### Creole (.creole)

**Features**: Wiki links, headings, formatting, tables

### TiddlyWiki (.tid)

**Features**: Tiddler metadata, tags, formatting

### Jupyter (.ipynb)

**Features**: JSON-based notebook viewing, cell structure display

### R Markdown (.Rmd)

**Features**: YAML front matter, R code chunks, markdown text

### Key-Value (.properties, .ini, .env, .conf)

**Features**: Configuration file editing, section support, comment handling

### Plain Text (.txt, .text, .log)

**Features**: Basic text editing, universal format support

---

## Cloud Storage Setup

### Dropbox

1. Go to **Settings > Cloud Storage > Add Storage**
2. Select **Dropbox**
3. Sign in with your Dropbox account
4. Authorize Yole to access your files
5. Choose a sync folder
6. Tap **Connect**

### Google Drive

1. Go to **Settings > Cloud Storage > Add Storage**
2. Select **Google Drive**
3. Sign in with your Google account
4. Authorize Yole
5. Choose a sync folder
6. Tap **Connect**

### OneDrive

1. Go to **Settings > Cloud Storage > Add Storage**
2. Select **OneDrive**
3. Sign in with your Microsoft account
4. Authorize Yole
5. Choose a sync folder
6. Tap **Connect**

### WebDAV

1. Go to **Settings > Cloud Storage > Add Storage**
2. Select **WebDAV**
3. Enter server URL (e.g., `https://cloud.example.com/remote.php/webdav/`)
4. Enter username and password
5. Tap **Connect**

### Git

1. Go to **Settings > Cloud Storage > Add Storage**
2. Select **Git**
3. Enter repository URL
4. Configure authentication (SSH key or HTTPS credentials)
5. Choose local sync directory
6. Tap **Connect**

---

## Network Storage (FTP, SFTP, SMB)

### FTP

1. Go to **Settings > Network Storage > Add**
2. Select **FTP**
3. Enter server address and port (default: 21)
4. Enter username and password
5. Optionally set root directory
6. Tap **Connect**

### SFTP

1. Go to **Settings > Network Storage > Add**
2. Select **SFTP**
3. Enter server address and port (default: 22)
4. Choose authentication: password or SSH key
5. Enter credentials
6. Tap **Connect**

### SMB

1. Go to **Settings > Network Storage > Add**
2. Select **SMB/CIFS**
3. Enter server address and share name
4. Enter domain, username, and password
5. Tap **Connect**

---

## Settings and Customization

### Editor Settings

- **Font Size** -- Adjust editor font size (8-48pt)
- **Font Family** -- Choose from monospace, serif, or sans-serif
- **Line Numbers** -- Show/hide line numbers
- **Word Wrap** -- Enable/disable word wrapping
- **Auto-Save** -- Configure auto-save interval

### Theme

- **Light Mode** -- Light background with dark text
- **Dark Mode** -- Dark background with light text
- **System Default** -- Follow device theme setting
- **Custom Theme** -- Configure custom colors

### File Management

- **Default Directory** -- Set the default file storage location
- **Recent Files** -- Configure number of recent files to show
- **File Encoding** -- Set default file encoding (UTF-8 recommended)

### Sync Settings

- **Auto-Sync** -- Enable/disable automatic synchronization
- **Sync Interval** -- Set sync frequency (5 min, 15 min, 30 min, 1 hour)
- **Wi-Fi Only** -- Sync only on Wi-Fi to save mobile data
- **Conflict Resolution** -- Choose default conflict resolution strategy

---

## Troubleshooting

### App Crashes on Launch
- Clear app data: **Settings > Apps > Yole > Clear Data**
- Reinstall the app
- Check for updates on Google Play

### Files Not Opening
- Check file permissions (Android 11+ requires Storage Access Framework)
- Verify the file is not corrupted
- Try opening with a different format manually

### Sync Not Working
- Check internet connectivity
- Re-authenticate cloud storage accounts
- Check available storage space on device
- Review error messages in **Settings > Cloud Storage > Status**

### Slow Performance on Large Files
- Files over 10,000 lines may take longer to parse
- Enable lazy loading in settings
- Consider splitting large files

### Preview Not Rendering
- Ensure the correct format is selected
- Check for syntax errors in the document
- Clear the app cache and try again

---

**Last Updated**: 2026-03-17
