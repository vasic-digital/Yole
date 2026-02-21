# Binary Format Detection Guide

## Overview

Yole includes binary file detection to identify and handle non-text files appropriately.

## Detection

The Binary parser detects:
- Executable files (ELF, PE, Mach-O)
- Image files (PNG, JPEG, GIF, etc.)
- Audio/Video files
- Compressed archives
- Database files
- Office documents

## Behavior

When a binary file is detected:
1. File is marked as binary
2. Content is not parsed as text
3. Hex preview may be shown
4. User is notified of binary nature

## Supported Binary Types

| Category | Extensions |
|----------|------------|
| Images | .png, .jpg, .gif, .bmp, .webp |
| Audio | .mp3, .wav, .ogg, .flac |
| Video | .mp4, .avi, .mkv, .mov |
| Archives | .zip, .tar, .gz, .7z |
| Executables | .exe, .dll, .so, .dylib |
| Documents | .pdf, .docx, .xlsx |

## Detection Methods

1. **Magic Bytes**: File signature analysis
2. **Extension Mapping**: Known extensions
3. **Content Analysis**: Binary character ratio

## See Also

- [File Signature Database](https://www.filesignatures.net)
