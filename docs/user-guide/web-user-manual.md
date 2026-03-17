# Yole Web (Wasm PWA) User Manual

**Version**: 1.0
**Date**: 2026-03-17
**Platform**: Modern web browsers (Chrome, Firefox, Edge, Safari)
**Status**: In Development

---

## PWA Installation

### Chrome / Edge (Desktop)

1. Navigate to the Yole web app URL
2. Click the install icon in the address bar (or look for the "Install" prompt)
3. Click **Install**
4. Yole will appear as a standalone app in your applications

### Chrome / Edge (Mobile)

1. Navigate to the Yole web app URL
2. Tap the browser menu (three dots)
3. Tap **Install app** or **Add to Home Screen**
4. Tap **Install**

### Firefox (Desktop)

Firefox does not support PWA installation natively. Use Yole directly in the browser tab.

### Safari (macOS/iOS)

1. Navigate to the Yole web app URL
2. Tap the share button
3. Tap **Add to Home Screen**
4. Tap **Add**

---

## Getting Started

### First Use

When you open Yole in your browser, you will see the editor interface. The web version uses Kotlin/Wasm (WebAssembly) for near-native performance.

### Creating a Document

1. Click **New** in the toolbar
2. Select a format
3. Start editing
4. Use **Ctrl+S** / **Cmd+S** to save to browser storage

### Opening a File

1. Click **Open** in the toolbar
2. Select a file from your device using the file picker
3. The file loads into the editor with automatic format detection

### Saving Files

- **Browser Storage**: Files are saved to the browser's local storage (IndexedDB) automatically
- **Download**: Use **File > Download** to save a file to your device
- **File System Access API**: On supported browsers (Chrome, Edge), Yole can save directly to your file system

---

## Platform Limitations

### Browser Sandbox

The web version runs inside the browser sandbox, which imposes certain limitations:

| Feature | Status | Notes |
|---------|--------|-------|
| File editing | Supported | All 17 formats |
| Local storage | Supported | IndexedDB for persistence |
| Direct file save | Partial | Requires File System Access API (Chrome/Edge only) |
| FTP/SFTP/SMB | Not available | Browser cannot open raw TCP sockets |
| Cloud storage (OAuth2) | Supported | Dropbox, Google Drive, OneDrive via popup windows |
| WebDAV | Partial | Requires CORS-compatible server or proxy |
| Git | Not available | Requires server-side proxy |
| Keyboard shortcuts | Supported | May conflict with browser shortcuts |
| System clipboard | Supported | Requires clipboard permission |

### CORS Restrictions

Web requests are subject to Cross-Origin Resource Sharing (CORS) restrictions. Cloud storage APIs (Dropbox, Google Drive, OneDrive) include proper CORS headers. Self-hosted protocols (WebDAV, FTP, SFTP, SMB) require a CORS proxy server when accessed from the browser.

### Memory Limitations

WebAssembly runs in a fixed memory region (default: 256 MB, expandable). Very large documents (>100,000 lines) may exceed the Wasm memory limit. Monitor memory usage via the browser's developer tools (Memory tab).

---

## Offline Usage

### Service Worker

Yole's PWA includes a service worker that caches:
- The Wasm binary and JavaScript glue code
- Static assets (CSS, fonts, icons)
- Recently opened documents (in IndexedDB)

### Working Offline

1. Open Yole at least once with an internet connection (to cache the app)
2. Subsequent visits work fully offline
3. Documents saved to browser storage are available offline
4. Cloud sync operations queue and execute when connectivity is restored

### Clearing Offline Data

To clear all cached data:
1. Open browser settings
2. Navigate to **Site Settings** or **Storage**
3. Find the Yole web app URL
4. Clear stored data

---

## Supported Formats

All 17 formats are supported in the web version with the same parsing engine as Android and Desktop:

- Markdown, Todo.txt, CSV, Plain Text, LaTeX, Org Mode
- WikiText, Creole, TiddlyWiki, AsciiDoc, reStructuredText
- Key-Value, TaskPaper, Textile, Jupyter, R Markdown
- Binary detection

---

## Troubleshooting

### App Not Loading
- Ensure your browser supports WebAssembly (all modern browsers do)
- Check that JavaScript is enabled
- Try clearing browser cache and reloading
- Check the browser console for error messages

### Slow Initial Load
- The first load downloads the Wasm binary (several MB)
- Subsequent loads use the cached version and are fast
- Ensure you have a stable internet connection for the first load

### Files Not Saving
- Check if IndexedDB is enabled in your browser
- Ensure the browser is not in private/incognito mode (some browsers restrict storage)
- Check available storage quota

### Cloud Storage Not Connecting
- Ensure popups are allowed for the Yole domain
- Check that cookies are enabled (required for OAuth2 flows)
- Try clearing site data and re-authenticating

---

**Last Updated**: 2026-03-17
