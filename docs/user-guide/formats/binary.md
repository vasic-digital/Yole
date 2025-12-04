# Binary File Detection

**Format**: Binary File Detection
**Extensions**: All non-text files
**Purpose**: Prevent editing binary files as text
**Yole Support**: ✅ Detection and warning

---

## Overview

Binary file detection is Yole's ability to identify files that contain non-text (binary) data and prevent accidental editing as plain text. This protects file integrity and provides a better user experience.

### Why Binary Detection?

- **Data Protection**: Prevent corruption of binary files
- **User Experience**: Clear warning instead of garbled display
- **File Safety**: Avoid accidental modifications
- **Performance**: Skip rendering binary data
- **Helpful Guidance**: Suggest appropriate tools

---

## What Are Binary Files?

### Binary vs. Text Files

**Text Files**:
- Contain human-readable characters (UTF-8, ASCII, etc.)
- Editable in text editors
- Line-based organization
- Examples: `.txt`, `.md`, `.json`, `.xml`

**Binary Files**:
- Contain machine-readable data
- Not human-readable
- Byte-oriented structure
- Examples: Images, executables, databases, archives

### Common Binary File Types

**Images**:
```
.jpg, .jpeg  - JPEG images
.png         - PNG images
.gif         - GIF animations
.bmp         - Bitmap images
.svg         - SVG (actually XML, but often binary data)
.ico         - Icon files
.webp        - WebP images
```

**Documents**:
```
.pdf         - PDF documents
.docx        - Microsoft Word
.xlsx        - Microsoft Excel
.pptx        - Microsoft PowerPoint
.odt         - OpenDocument Text
```

**Media**:
```
.mp3, .wav, .flac  - Audio files
.mp4, .avi, .mkv   - Video files
```

**Archives**:
```
.zip         - ZIP archives
.tar         - TAR archives
.gz          - Gzip compression
.7z          - 7-Zip archives
.rar         - RAR archives
```

**Executables**:
```
.exe         - Windows executable
.dll         - Windows library
.so          - Linux shared object
.app         - macOS application
.apk         - Android package
```

**Databases**:
```
.db, .sqlite - SQLite databases
.mdb         - Access database
```

**Other**:
```
.class       - Java bytecode
.pyc         - Python bytecode
.bin         - Generic binary
.dat         - Data file
```

---

## How Yole Detects Binary Files

### Detection Methods

**1. File Extension**:
```
Known binary extensions in blacklist:
.jpg, .png, .gif, .pdf, .zip, .exe, etc.
```

**2. Content Analysis**:
```
- Scan first N bytes (typically 512-8192)
- Count non-text characters
- Check for null bytes (\0)
- Detect encoding issues
- Look for magic numbers
```

**3. Magic Number Detection**:
```
PDF:  %PDF-
PNG:  89 50 4E 47
JPEG: FF D8 FF
ZIP:  50 4B 03 04
EXE:  4D 5A
```

### Detection Algorithm

**Simplified logic**:
```kotlin
fun isBinary(file: File): Boolean {
    // 1. Check extension
    if (file.extension in BINARY_EXTENSIONS) {
        return true
    }

    // 2. Read sample bytes
    val sample = file.readBytes(8192)

    // 3. Check for null bytes
    if (sample.contains(0x00)) {
        return true
    }

    // 4. Count non-printable characters
    val nonPrintable = sample.count { it < 0x20 && it !in listOf(0x09, 0x0A, 0x0D) }
    val ratio = nonPrintable.toFloat() / sample.size

    // 5. If >30% non-printable, likely binary
    return ratio > 0.3
}
```

---

## Yole Behavior

### When Binary File Detected

**Warning Display**:
```
⚠️ Binary File Detected

This file appears to be binary and cannot be edited as text.

File: image.jpg
Type: JPEG Image
Size: 2.4 MB

Options:
• Open with system app
• View file info
• Close
```

### What Yole Does

✅ **Displays warning**: Clear message about binary content
✅ **Shows file info**: Type, size, location
✅ **Prevents editing**: No text editor opened
✅ **Suggests alternatives**: Open with appropriate app
✅ **Safe handling**: No risk of corruption

### What Yole Doesn't Do

❌ **Display binary data**: No garbled text shown
❌ **Allow editing**: Can't modify binary files
❌ **Convert files**: No format conversion
❌ **Execute code**: Won't run executables

---

## User Options

### When You Open a Binary File

**Option 1: Open with System App**
- Yole calls system default handler
- Image opens in image viewer
- PDF opens in PDF reader
- Archive opens in archive manager

**Option 2: View File Info**
- File path
- File size
- File type (if detected)
- Last modified date
- Permissions (on desktop)

**Option 3: Close**
- Return to file browser
- No changes made

---

## False Positives

### Text Files Detected as Binary

**Common causes**:
- Very long lines (no line breaks)
- Unusual encoding (UTF-16, etc.)
- Embedded control characters
- Mixed binary/text content

**What to do**:
1. Check file in another editor
2. Verify encoding
3. Report issue if consistently wrong

### Overriding Detection

**Force open as text** (if available):
```
Settings → Advanced → Allow binary as text (⚠️ Use with caution)
```

**Risks**:
- May display garbled characters
- Could corrupt file if saved
- Performance issues with large files

---

## Common Scenarios

### 1. Accidentally Opening an Image

**What happens**:
```
1. Tap image.jpg in file browser
2. Yole detects JPEG magic number
3. Warning dialog appears
4. Choose "Open with system app"
5. Image viewer opens image
```

**Result**: Image safely opened in appropriate app.

### 2. Opening a PDF

**What happens**:
```
1. Select document.pdf
2. Yole detects PDF magic number (%PDF-)
3. Warning shown
4. Open in system PDF reader
```

**Result**: PDF viewed correctly.

### 3. Opening a ZIP Archive

**What happens**:
```
1. Try to open archive.zip
2. Binary detection triggers
3. Warning displayed
4. Open with archive manager
```

**Result**: Archive opened for extraction/viewing.

### 4. Mixed Content Files

**Example**: HTML with embedded base64 images
```html
<img src="data:image/png;base64,iVBORw0KGgoAAAANS...">
```

**Handling**:
- Usually detected as text (valid HTML)
- Base64 is text-representable
- Opens normally in editor

---

## Best Practices

### File Organization

**Separate binary and text files**:
```
project/
├── docs/           # Text files (.md, .txt)
├── assets/         # Binary files (images, etc.)
├── src/            # Source code (text)
└── build/          # Compiled binaries
```

### Avoid Common Mistakes

❌ **Don't**:
- Try to edit images as text
- Open executables in text editor
- Modify binary databases as text
- Edit PDF files directly

✅ **Do**:
- Use appropriate apps for each file type
- Keep backups before any operations
- Understand file types before opening
- Use Yole for text files only

---

## Supported Binary Extensions

**Images**:
`jpg, jpeg, png, gif, bmp, ico, webp, svg, tiff`

**Documents**:
`pdf, doc, docx, xls, xlsx, ppt, pptx, odt, ods, odp`

**Archives**:
`zip, tar, gz, bz2, xz, 7z, rar`

**Media**:
`mp3, mp4, wav, avi, mkv, flac, ogg, webm`

**Executables**:
`exe, dll, so, dylib, app, apk, dex`

**Databases**:
`db, sqlite, sqlite3, mdb`

**Fonts**:
`ttf, otf, woff, woff2`

**Other**:
`bin, dat, class, pyc, o, a, lib`

---

## Technical Details

### Magic Number Database

**Common magic numbers**:
```
PDF:    25 50 44 46      (%PDF)
PNG:    89 50 4E 47      (.PNG)
JPEG:   FF D8 FF         (...)
GIF:    47 49 46 38      (GIF8)
ZIP:    50 4B 03 04      (PK..)
EXE:    4D 5A            (MZ)
```

### Detection Threshold

**Default settings**:
- Sample size: 8192 bytes
- Null byte threshold: 1 occurrence
- Non-printable threshold: 30%
- Encoding detection: UTF-8, ASCII

---

## Troubleshooting

### File Wrongly Detected as Binary

**Check**:
1. File encoding (should be UTF-8)
2. Line endings (CR, LF, CRLF)
3. Hidden control characters
4. File extension

**Solutions**:
- Convert to UTF-8 encoding
- Remove control characters
- Use standard line endings
- Rename with correct extension

### Binary File Not Detected

**Check**:
1. File extension matches type
2. File actually contains binary data
3. Magic number present

**Report**:
- File type
- Sample of hex dump
- Expected behavior

---

## External Resources

### Tools

**File Type Detection**:
- `file` command (Unix/Linux)
- `Get-Content` (PowerShell)
- `xxd` or `hexdump` (hex viewers)

**Hex Editors**:
- HxD (Windows)
- Hex Fiend (macOS)
- xxd (command-line)

### References

- [List of File Signatures](https://en.wikipedia.org/wiki/List_of_file_signatures)
- [Magic Number Database](https://www.garykessler.net/library/file_sigs.html)
- [File Format Wiki](http://justsolve.archiveteam.org/wiki/Main_Page)

---

## Next Steps

- **[Plain Text Format →](./plaintext.md)** - Editing text files
- **[All Formats →](./README.md)** - Browse all format guides
- **[Back to Getting Started →](../getting-started.md)**

---

*Last updated: November 11, 2025*
*Yole version: 2.15.1+*
