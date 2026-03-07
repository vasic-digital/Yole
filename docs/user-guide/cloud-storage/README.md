<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Cloud Storage Setup Guides

Yole supports eight cloud and network storage protocols, giving you flexibility to store your documents wherever works best for you. This directory contains detailed, step-by-step setup guides for each provider.

---

## Supported Providers

| Provider | Protocol | Authentication | Guide |
|----------|----------|----------------|-------|
| **Dropbox** | Dropbox API v2 | OAuth 2.0 | [dropbox-setup.md](dropbox-setup.md) |
| **Google Drive** | Google Drive API v3 | OAuth 2.0 | [google-drive-setup.md](google-drive-setup.md) |
| **OneDrive** | Microsoft Graph API v1.0 | OAuth 2.0 | [onedrive-setup.md](onedrive-setup.md) |
| **WebDAV** | HTTP/WebDAV (PROPFIND, GET, PUT) | Basic, Digest, OAuth, None | [webdav-setup.md](webdav-setup.md) |
| **FTP / SFTP** | FTP over TCP / SSH File Transfer | Username/Password, SSH Keys | [ftp-sftp-setup.md](ftp-sftp-setup.md) |
| **Git** | Git Smart HTTP / Platform REST APIs | Personal Access Token, SSH Keys | [git-setup.md](git-setup.md) |
| **SMB/CIFS** | SMB2/SMB3 | NTLM Username/Password | [smb-setup.md](smb-setup.md) |

---

## Choosing a Provider

### For Personal Use

- **Dropbox** -- Simple setup, 2 GB free storage, works well for small document collections
- **Google Drive** -- 15 GB free (shared with Gmail/Photos), integrates with Google Workspace
- **OneDrive** -- 5 GB free, deep integration with Microsoft 365

### For Self-Hosting

- **WebDAV** -- Best choice for Nextcloud, ownCloud, or any WebDAV-compatible server
- **SFTP** -- Secure access to any Linux/Unix server with SSH
- **Git** -- Version-controlled document storage with full history

### For Enterprise / Local Network

- **SMB/CIFS** -- Access Windows file shares and NAS devices on your local network
- **FTP** -- Legacy server access (use SFTP when possible for better security)

---

## How Cloud Storage Works in Yole

Yole is an **offline-first** text editor. Cloud storage extends the local experience:

1. **Connect** -- Add a storage provider with your credentials
2. **Browse** -- Navigate remote files and folders
3. **Download** -- Pull files to your device for local editing
4. **Upload** -- Push edited files back to the cloud
5. **Sync** -- Keep local and remote copies in sync automatically
6. **Work Offline** -- Cached files remain available without an internet connection

All storage providers implement the same `NetworkStorageService` interface, so the user experience is consistent regardless of which provider you choose.

---

## Common Operations

### Adding a Provider

1. Open **Settings** > **Cloud Storage** > **Add Provider**
2. Select the provider type
3. Enter connection details (see individual guides)
4. Tap **Test Connection** to verify
5. Tap **Save**

### Testing a Connection

Every provider supports a **Test Connection** button that verifies:
- Network reachability
- Credential validity
- Permission sufficiency

### Viewing Storage Quota

Providers that support quota information (Dropbox, Google Drive, OneDrive, WebDAV, SMB) display:
- Total space
- Used space
- Available space
- Usage percentage

### Synchronization

- **Sync a single file**: Long-press a file and select **Sync**
- **Sync all files**: Use the **Sync All** button in the storage browser
- **Force sync**: Overrides the cache and re-downloads the latest version

---

## Security Notes

- OAuth 2.0 providers (Dropbox, Google Drive, OneDrive) never expose your password to Yole
- Tokens are stored using platform-specific secure storage (Android Keystore, macOS Keychain, etc.)
- Always use HTTPS/SSL when connecting over the internet
- SSH-based protocols (SFTP, Git over SSH) encrypt all traffic by default
- FTP transmits credentials in plaintext -- use only on trusted networks or switch to SFTP

---

## Related Documentation

- [Cloud Storage Overview](../cloud-storage.md) -- High-level feature description
- [Getting Started](../getting-started.md) -- First steps with Yole
- [Troubleshooting](../../TROUBLESHOOTING.md) -- Common issues and solutions

---

*Last updated: March 7, 2026*
