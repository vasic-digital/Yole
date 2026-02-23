<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Cloud Storage Integration

**Feature**: Cloud Storage and Network File Access
**Supported Providers**: Dropbox, Google Drive, OneDrive, WebDAV, FTP, SFTP, Git, SMB/CIFS
**Yole Support**: All platforms (Android, Desktop, iOS, Web)

---

## Overview

Yole is designed as an offline-first text editor -- your files live on your device and are always accessible, even without an internet connection. Cloud storage integration extends this by letting you connect to remote storage services to upload, download, and synchronize your documents.

Whether you keep your notes in Dropbox, store documentation on a company WebDAV server, manage files through Git, or access shared drives over your local network with SMB, Yole provides a unified interface for all of these providers.

### Why Use Cloud Storage?

- **Access your files from multiple devices**: Edit a Markdown document on your phone, then continue on your desktop
- **Backup your documents**: Keep copies of important files on a remote server
- **Collaborate with others**: Share files through Google Drive, OneDrive, or a Git repository
- **Self-host your data**: Use WebDAV (Nextcloud, ownCloud) or SFTP to maintain full control
- **Work offline**: Yole caches files locally so you can keep working without a connection

### How It Works

1. You add a storage provider in **Settings** and provide your credentials
2. Yole connects to the remote storage and lets you browse files
3. You can download files for local editing or upload local files to the cloud
4. Synchronization keeps your local and remote copies in sync
5. If you go offline, cached files remain available for editing

---

## Quick Start

### Adding a Storage Provider

1. Open Yole and go to **More** (bottom navigation) or **Settings** (desktop menu)
2. Select **Cloud Storage** or **Network Storage**
3. Tap **Add Storage Provider**
4. Choose your provider (Dropbox, Google Drive, OneDrive, WebDAV, FTP, SFTP, Git, or SMB)
5. Enter your connection details and credentials
6. Tap **Test Connection** to verify everything works
7. Tap **Save** to add the provider

### Browsing Remote Files

1. Go to **Files** tab
2. Tap the storage provider name in the sidebar or file browser header
3. Browse folders and files just like your local Notebook
4. Tap a file to download and open it for editing

### Uploading a Local File

1. Open the file you want to upload
2. Tap **Menu** and select **Upload to Cloud**
3. Choose the storage provider and destination folder
4. The file is uploaded and synchronization begins

---

## Supported Storage Providers

### Provider Comparison

| Feature | Dropbox | Google Drive | OneDrive | WebDAV | FTP | SFTP | Git | SMB/CIFS |
|---------|---------|-------------|----------|--------|-----|------|-----|----------|
| Folders | Yes | Yes | Yes | Yes | Limited | Yes | Yes | Yes |
| Encryption | Yes | Yes | Yes | Yes | No | Yes | Yes | Yes |
| Search | Yes | Yes | Yes | No | No | No | No | No |
| Quota info | Yes | Yes | Yes | Yes | No | No | No | Yes |
| Authentication | OAuth 2.0 | OAuth 2.0 | OAuth 2.0 | Username/Password | Username/Password | SSH Key or Password | SSH Key, Token, or Password | Username/Password |
| Best for | Personal files | Google Workspace | Microsoft 365 | Self-hosted | Legacy servers | Secure servers | Version control | Local network |

---

## Dropbox

### What You Need

- A Dropbox account (free or paid)
- An internet connection for the initial setup

### Setup

1. Go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **Dropbox**
3. Give the connection a name (for example, "My Dropbox")
4. Tap **Sign in with Dropbox**
5. A browser window opens asking you to log in to your Dropbox account
6. Review the permissions Yole is requesting and tap **Allow**
7. The browser redirects back to Yole and the connection is established
8. Optionally set a **Root Folder** to limit Yole to a specific Dropbox folder (for example, `/Notes`)

### OAuth Authentication Flow

Yole uses the industry-standard OAuth 2.0 protocol to connect to Dropbox. This means:

- Yole never sees or stores your Dropbox password
- Yole receives a limited-access token that only allows file operations
- You can revoke access at any time from your Dropbox account settings
- Tokens refresh automatically so you stay connected

If your token expires or becomes invalid, Yole will prompt you to sign in again.

### Features

- **Browse folders and files** in your Dropbox
- **Upload and download** documents
- **Create folders** to organize your files
- **Move, rename, and delete** files remotely
- **Check storage quota** to see how much space you have
- **Sync individual files or entire folders** between local and cloud

### Tips

- Set a root folder path (like `/Apps/Yole`) to keep your Yole documents separate from other Dropbox files
- Dropbox free accounts include 2 GB of storage; paid plans offer 2 TB or more
- If you use Dropbox on multiple devices, be careful about editing the same file simultaneously

---

## Google Drive

### What You Need

- A Google account
- An internet connection for the initial setup

### Setup

1. Go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **Google Drive**
3. Give the connection a name (for example, "Work Drive")
4. Tap **Sign in with Google**
5. A browser window opens asking you to log in to your Google account
6. Review the permissions and tap **Allow**
7. The browser redirects back to Yole and the connection is established
8. Optionally set a **Root Folder** to limit Yole to a specific Google Drive folder
9. If you have access to a Shared Drive (Team Drive), you can enter its ID to connect to it instead of your personal drive

### OAuth Authentication Flow

Yole uses Google's OAuth 2.0 to authenticate. The process requests these permissions:

- **View and manage files created by Yole** (drive.file scope)
- **View files in your Google Drive** (drive.readonly scope)

Yole does not request full access to your entire Google Drive. Only files created by or opened through Yole are accessible, unless you explicitly grant broader access.

To revoke access later, visit [Google Account Permissions](https://myaccount.google.com/permissions) and remove Yole.

### Features

- **Browse folders and files** in your Google Drive
- **Upload and download** documents
- **Create folders** for organization
- **Move, rename, and delete** files
- **View storage quota** (free accounts get 15 GB shared across Google services)
- **Access Shared Drives** (Team Drives) if your organization uses Google Workspace

### Tips

- Google Drive's 15 GB of free storage is shared with Gmail and Google Photos
- Text files are small, so you can store thousands of documents even on a free account
- Google Drive supports file search, which Yole can use to find documents quickly

---

## OneDrive

### What You Need

- A Microsoft account (personal, business, or school)
- An internet connection for the initial setup

### Setup

1. Go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **OneDrive**
3. Give the connection a name (for example, "Personal OneDrive")
4. Choose your **Drive Type**:
   - **Personal** -- Your personal OneDrive storage
   - **Business** -- OneDrive for Business (Microsoft 365)
   - **SharePoint** -- A SharePoint document library
   - **Group** -- A Microsoft 365 group's shared files
5. Tap **Sign in with Microsoft**
6. A browser window opens asking you to log in to your Microsoft account
7. Review the permissions and tap **Accept**
8. The browser redirects back to Yole and the connection is established
9. Optionally set a **Root Folder** to limit access to a specific folder

### OAuth Authentication Flow

Yole authenticates through Microsoft's identity platform using OAuth 2.0. The requested permissions include:

- **Read and write your files** (Files.ReadWrite)
- **Read and write all files you have access to** (Files.ReadWrite.All)
- **Maintain access to data you have given it access to** (offline_access, for token refresh)

To revoke access later, visit [Microsoft Account App Permissions](https://account.microsoft.com/privacy/app-access) and remove Yole.

### Features

- **Browse folders and files** in your OneDrive
- **Upload and download** documents
- **Create folders** for organization
- **Move, rename, and delete** files
- **Access different drive types**: personal, business, SharePoint, and group drives
- **View storage quota** information

### Tips

- OneDrive personal accounts include 5 GB of free storage (upgradeable with Microsoft 365)
- Microsoft 365 Business plans often include 1 TB per user
- If your organization uses SharePoint, you can connect directly to document libraries

---

## WebDAV

WebDAV (Web Distributed Authoring and Versioning) is an open standard protocol supported by many cloud and self-hosted services. If you use Nextcloud, ownCloud, or any other WebDAV-compatible server, this is the provider for you.

### What You Need

- A WebDAV server URL
- Your username and password (or other credentials)
- An internet connection (or local network access)

### Setup

1. Go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **WebDAV**
3. Give the connection a name (for example, "My Nextcloud")
4. Enter the **Server URL**:
   - **Nextcloud**: `https://your-server.com/remote.php/dav/files/USERNAME/`
   - **ownCloud**: `https://your-server.com/remote.php/webdav/`
   - **Generic WebDAV**: The URL provided by your service
5. Enter your **Username** and **Password**
6. Choose the **Authentication Type**:
   - **Basic** (most common)
   - **Digest** (more secure, if supported by the server)
   - **OAuth** (for services that support it)
   - **None** (for public/anonymous access)
7. Configure SSL options:
   - **SSL Enabled**: Enabled by default (recommended)
   - **Verify Certificate**: Enabled by default; disable only if using a self-signed certificate
8. Tap **Test Connection** to verify
9. Tap **Save**

### Compatible Services

WebDAV works with many popular self-hosted and cloud services:

- **Nextcloud** -- Popular self-hosted cloud platform
- **ownCloud** -- Self-hosted file sharing platform
- **Seafile** -- Via WebDAV interface
- **Fastmail** -- File storage via WebDAV
- **Box** -- Via WebDAV endpoint
- **Apache HTTP Server** -- With mod_dav enabled
- **Nginx** -- With WebDAV module
- **Any WebDAV-compliant server**

### Features

- **Browse folders and files** on any WebDAV server
- **Upload and download** documents
- **Create, move, rename, and delete** files and folders
- **SSL/TLS encryption** for secure connections
- **Configurable timeouts** (connection: 30 seconds, read: 60 seconds by default)

### Tips

- Always use HTTPS when connecting over the internet
- If you see certificate errors with a self-hosted server using a self-signed certificate, you can disable certificate verification, but understand this reduces security
- Nextcloud users: use an App Password instead of your main password for better security (go to Nextcloud Settings > Security > App Passwords)
- Connection and read timeouts can be adjusted if you have a slow connection

---

## FTP

FTP (File Transfer Protocol) is one of the oldest file transfer protocols. While not as secure as modern alternatives, it remains useful for accessing legacy servers.

### What You Need

- An FTP server address (hostname or IP)
- Your username and password
- The port number (default: 21)

### Setup

1. Go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **FTP**
3. Give the connection a name (for example, "Office FTP Server")
4. Enter the **Host** address (for example, `ftp.example.com` or `192.168.1.100`)
5. Enter the **Port** (default: 21)
6. Enter your **Username** and **Password**
7. Set the **Root Path** to start in a specific directory (default: `/`)
8. Configure connection options:
   - **Passive Mode**: Enabled by default (recommended for most networks and firewalls)
   - **Secure FTP (FTPS)**: Enable for FTP over TLS encryption
   - **Encoding**: UTF-8 by default
9. Tap **Test Connection** to verify
10. Tap **Save**

### Features

- **Browse files** on an FTP server
- **Upload and download** documents
- **Delete and rename** files
- **Configurable passive/active mode** for firewall compatibility
- **Optional FTPS** (FTP over TLS) for encrypted transfers

### Limitations

FTP has several limitations compared to other protocols:

- **No encryption by default**: Standard FTP sends passwords and data in plain text; enable FTPS when available
- **Limited folder support**: Some FTP servers do not reliably support folder creation
- **No copy operation**: FTP does not support server-side file copying
- **No search**: FTP does not support searching for files by name or content
- **No quota information**: FTP does not report available storage space
- **No metadata support**: Limited file information (size and date only)

### Tips

- Use SFTP instead of FTP whenever possible for better security
- Enable Passive Mode if you are behind a firewall or NAT router
- Enable FTPS (Secure FTP) if your server supports it to encrypt transfers
- FTP is best suited for simple file uploads and downloads on trusted networks

---

## SFTP

SFTP (SSH File Transfer Protocol) provides secure, encrypted file transfer over SSH. It is the recommended choice for connecting to remote servers.

### What You Need

- An SFTP server address (hostname or IP)
- Your username
- Either a password or an SSH private key
- The port number (default: 22)

### Setup with Password Authentication

1. Go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **SFTP**
3. Give the connection a name (for example, "Home Server")
4. Enter the **Host** address (for example, `server.example.com`)
5. Enter the **Port** (default: 22)
6. Enter your **Username** and **Password**
7. Set the **Root Path** to start in a specific directory (default: `/`)
8. Tap **Test Connection** to verify
9. Tap **Save**

### Setup with SSH Key Authentication

SSH key authentication is more secure than passwords and is recommended for most use cases.

1. Go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **SFTP**
3. Give the connection a name
4. Enter the **Host** and **Port**
5. Enter your **Username**
6. Leave the **Password** field empty
7. Tap **SSH Private Key** and select your private key file (for example, `id_rsa` or `id_ed25519`)
8. If your private key is protected by a passphrase, enter it in the **Key Passphrase** field
9. Optionally, provide a **Known Hosts** file for server verification
10. Tap **Test Connection** to verify
11. Tap **Save**

### Host Key Verification

SFTP supports strict host key checking to prevent man-in-the-middle attacks:

- **Strict Host Key Checking** (enabled by default): Requires a `known_hosts` file to verify the server's identity
- When disabled, Yole will connect to any server without verifying its identity (less secure, but easier to set up)
- On first connection, you may be asked to accept the server's host key

### Features

- **Browse folders and files** securely over SSH
- **Upload and download** documents with encryption
- **Create, move, rename, and delete** files and folders
- **Copy files** on the server
- **SSH key authentication** for password-free access
- **Configurable host key verification** for security
- **Full folder support** with proper permissions
- **Encrypted transfers** using SSH2

### Tips

- Use SSH key authentication instead of passwords when possible
- Generate an Ed25519 key for the best combination of security and performance
- Keep your private key file secure and never share it
- If you manage your own server, disable password authentication and allow only key-based login
- SFTP is ideal for accessing Linux servers, NAS devices, and VPS instances

---

## Git

Git integration lets you use a Git repository as a storage backend. This is ideal for version-controlled documents, technical documentation, and any workflow where you want full change history.

### What You Need

- A Git repository URL (HTTPS or SSH)
- Authentication credentials (username/password, personal access token, or SSH key)
- An internet connection (or access to a local Git server)

### Setup

1. Go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **Git**
3. Give the connection a name (for example, "My Notes Repo")
4. Enter the **Repository URL**:
   - **HTTPS**: `https://github.com/username/my-notes.git`
   - **SSH**: `git@github.com:username/my-notes.git`
5. Enter the **Branch** to use (default: `main`)
6. Choose your authentication method:
   - **Personal Access Token** (recommended for GitHub, GitLab, Bitbucket): Enter the token in the provided field
   - **Username and Password**: For servers that support password authentication
   - **SSH Key**: Select your private key file and optionally provide the passphrase
7. Set the **Local Cache Path** -- this is where Yole will store the local clone of the repository
8. Configure sync options:
   - **Auto Sync**: Enabled by default; Yole will automatically pull changes and push your edits
   - **Commit Author Name**: The name used in Git commits (default: "Yole")
   - **Commit Author Email**: The email used in Git commits
9. Tap **Test Connection** to verify
10. Tap **Save**

### How Git Sync Works

Unlike other storage providers, Git tracks the full history of your changes:

1. Yole clones the repository to a local cache folder
2. When you edit a file, changes are saved locally first
3. If Auto Sync is enabled, Yole periodically commits your changes and pushes them to the remote
4. When syncing, Yole pulls remote changes and merges them with your local edits
5. Each edit creates a Git commit, giving you a complete revision history

### Features

- **Full version history** for all your documents
- **Branch support**: Work on different branches
- **Multiple authentication methods**: HTTPS with token, SSH key, or username/password
- **Auto sync**: Automatic commit and push on save
- **Customizable commit identity**: Set your own author name and email
- **Browse and manage files** in the repository

### Tips

- Use a Personal Access Token (PAT) instead of your password for GitHub, GitLab, and Bitbucket
- Set meaningful commit author details so you can identify Yole's commits in the history
- Consider creating a dedicated repository for your Yole notes rather than mixing them with code
- Git works especially well for Markdown files, todo.txt, and other plain-text formats
- If you use Git with others, enable Auto Sync to stay up to date with remote changes

---

## SMB/CIFS

SMB (Server Message Block), also known as CIFS, is the standard file sharing protocol used on Windows networks and many NAS devices. Use this to access shared folders on your local network.

### What You Need

- The hostname or IP address of the file server
- The name of the shared folder
- Your username, password, and optionally a domain name
- Network access to the server (typically on the same local network)

### Setup

1. Go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **SMB/CIFS**
3. Give the connection a name (for example, "Office NAS")
4. Enter the **Host** address (for example, `192.168.1.50` or `nas.local`)
5. Enter the **Share Name** (for example, `Documents` or `Public`)
6. Enter your **Username** and **Password**
7. If your network uses a Windows domain, enter the **Domain** name
8. Set the **Path** within the share (default: `/`)
9. Enter the **Port** (default: 445)
10. Configure security options:
    - **Encryption**: Enabled by default (SMB3 encryption)
    - **Signing**: Enabled by default (message integrity)
11. Tap **Test Connection** to verify
12. Tap **Save**

### Features

- **Browse folders and files** on Windows shares and NAS devices
- **Upload and download** documents
- **Create, move, rename, and delete** files and folders
- **Copy files** on the server
- **SMB3 encryption and signing** for secure transfers
- **Domain authentication** for corporate networks

### Tips

- SMB is ideal for accessing a NAS (Synology, QNAP, etc.) or a Windows file server on your local network
- Make sure you are on the same network as the file server, or have VPN access
- Use the IP address instead of the hostname if name resolution is unreliable
- Enable encryption and signing for security, especially on shared networks
- If you cannot connect, check that the SMB service is running on the server and that the firewall allows port 445

---

## Syncing Files Between Local and Cloud

### How Synchronization Works

Yole uses a bidirectional sync model to keep your local and remote files in sync:

1. **Download**: Remote files are downloaded to your device for local editing
2. **Edit**: You edit the local copy in Yole, which works entirely offline
3. **Upload**: Changes are uploaded back to the remote storage
4. **Monitoring**: Yole tracks the sync status of each file

### Sync Statuses

Each file has a sync status indicator:

| Status | Meaning |
|--------|---------|
| **Synced** | The local and remote copies are identical and up to date |
| **Pending Upload** | You have made local changes that have not yet been uploaded |
| **Pending Download** | The remote copy has changed and needs to be downloaded |
| **Syncing** | A sync operation is currently in progress |
| **Uploading** | The file is currently being uploaded to the remote |
| **Downloading** | The file is currently being downloaded from the remote |
| **Queued** | The file is waiting for its turn to sync |
| **Sync Error** | The last sync attempt failed (check error details) |
| **Conflict** | Both the local and remote copies have changed since the last sync |
| **Not Synced** | The file is not set up for synchronization |

### Manual Sync

To manually sync a file:

1. Open the file or select it in the file browser
2. Tap **Menu** > **Sync**
3. The file is synced immediately

To sync all files for a storage provider:

1. Go to the storage provider in **Settings** > **Cloud Storage**
2. Tap **Sync All**
3. All files are synchronized

### Force Sync

If a file appears up to date but you suspect it is not, you can force a sync:

1. Open the file
2. Tap **Menu** > **Force Sync**
3. Yole re-downloads the remote version and compares it with your local copy

---

## Conflict Resolution

Conflicts occur when both the local and remote copies of a file have been modified since the last sync. This can happen when you edit a file on your phone while someone else edits it on another device.

### How Yole Detects Conflicts

Yole compares the last-known sync timestamp with the modification times of both the local and remote files. If both have changed, the file is marked as **Conflict**.

### Resolution Strategies

When a conflict is detected, Yole offers several ways to resolve it:

| Strategy | What Happens |
|----------|-------------|
| **Local Wins** | Your local version overwrites the remote version |
| **Remote Wins** | The remote version overwrites your local version |
| **Keep Both** | Both versions are saved -- the remote version is downloaded with a modified filename (for example, `notes-conflict-2026-02-23.md`) |
| **Manual** | You review both versions side by side and choose which content to keep |
| **Skip** | The conflict is ignored until you decide to resolve it later |

### Resolving a Conflict

1. When a conflict is detected, Yole shows a notification
2. Tap the notification or open the file
3. Choose a resolution strategy from the options above
4. If you choose **Manual**, both versions are displayed so you can compare them
5. After resolving, the file is synced with the chosen version

### Preventing Conflicts

- Avoid editing the same file on multiple devices at the same time
- Sync frequently so changes are pushed and pulled promptly
- Use Git storage for documents that need collaborative editing -- Git's merge capabilities handle many conflicts automatically

---

## Offline Mode

### How Offline Mode Works

Yole is offline-first, which means your local files are always available regardless of your internet connection. Cloud storage integration extends this with smart caching:

1. **Files you have opened** are cached locally for offline access
2. **Edits made offline** are saved locally and queued for upload
3. **When connectivity returns**, queued changes are automatically synced

### Caching Files for Offline Access

To ensure specific files are available offline:

1. Browse to the file in your cloud storage
2. Tap and hold the file (or tap the menu icon next to it)
3. Select **Make Available Offline**
4. The file is downloaded and cached

To remove a file from the offline cache:

1. Go to **Settings** > **Cloud Storage** > select the provider
2. Tap **Manage Offline Files**
3. Remove files you no longer need offline

### Clearing the Cache

To free up local storage:

1. Go to **Settings** > **Cloud Storage** > select the provider
2. Tap **Clear Cache**
3. Cached files are removed (they remain on the remote server)

### What Happens When You Go Offline

- Files already downloaded continue to work normally
- Edits are saved locally and marked as **Pending Upload**
- Browsing remote folders that are not cached will show an offline message
- When you reconnect, pending uploads are processed automatically

---

## Security Considerations

### Credentials Storage

Yole stores your credentials securely using platform-specific secure storage:

- **Android**: Android Keystore
- **Desktop**: OS-level credential store
- **iOS**: Keychain Services

Credentials are encrypted at rest and never stored in plain text.

### OAuth 2.0 Tokens (Dropbox, Google Drive, OneDrive)

- Yole stores OAuth access tokens and refresh tokens securely
- Access tokens expire automatically (typically after one hour) and are refreshed using the refresh token
- You can revoke access at any time from the respective provider's account settings
- Yole never sees or stores your account password

### Encryption in Transit

| Protocol | Encryption |
|----------|-----------|
| Dropbox | HTTPS (TLS 1.2+) |
| Google Drive | HTTPS (TLS 1.2+) |
| OneDrive | HTTPS (TLS 1.2+) |
| WebDAV | HTTPS (TLS, configurable) |
| FTP | None by default; FTPS available |
| SFTP | SSH2 (always encrypted) |
| Git | HTTPS or SSH (both encrypted) |
| SMB | SMB3 encryption (configurable) |

### Recommendations

1. **Always use encrypted protocols**: Prefer SFTP over FTP, HTTPS over HTTP, enable SMB encryption
2. **Use SSH keys** instead of passwords for SFTP and Git when possible
3. **Use Personal Access Tokens** instead of passwords for Git hosting services
4. **Use App Passwords** for WebDAV services like Nextcloud instead of your main account password
5. **Enable two-factor authentication** on your Dropbox, Google, and Microsoft accounts
6. **Revoke unused connections**: If you stop using a provider, remove it from Yole and revoke access from the provider's account settings
7. **Verify SSL certificates**: Only disable certificate verification for self-signed certificates on trusted servers
8. **Keep Yole updated**: Updates include security patches for network libraries

---

## Troubleshooting

### Connection Issues

#### "Authentication failed" error

**For OAuth providers (Dropbox, Google Drive, OneDrive)**:
- Your token may have expired. Tap **Reconnect** or remove and re-add the provider
- Check that you have not revoked access from the provider's account settings
- Ensure your account is in good standing (not locked or suspended)

**For username/password providers (WebDAV, FTP, SFTP, SMB)**:
- Double-check your username and password
- Ensure you are using the correct server address and port
- For WebDAV with Nextcloud/ownCloud, try using an App Password instead of your main password
- For SFTP with SSH keys, make sure the key file is accessible and the passphrase is correct

#### "Connection timed out" error

- Check your internet connection
- Verify the server address is correct
- Confirm the server is running and accessible
- For local network services (SMB, FTP), make sure you are on the same network
- Try increasing the connection timeout in the provider settings (default: 30 seconds)

#### "SSL certificate error"

- The server's SSL certificate may be expired, self-signed, or invalid
- If the server uses a self-signed certificate and you trust it, you can disable **Verify Certificate** in the WebDAV settings
- For production servers, contact the server administrator to fix the certificate
- Do not disable certificate verification for servers on the public internet

#### "Host key verification failed" (SFTP)

- The server's SSH host key has changed, which could indicate a security issue
- If the server was recently reinstalled or its keys were regenerated, update your `known_hosts` file
- If you are connecting for the first time, accept the new host key when prompted
- Alternatively, disable **Strict Host Key Checking** (less secure)

### Sync Issues

#### Files not syncing

- Check that the storage provider shows as **Connected** in Settings
- Tap **Test Connection** to verify the connection is working
- Try a manual sync: select the file and tap **Sync**
- Check for error messages in the sync status
- Ensure the remote file has not been deleted or moved

#### Slow sync speed

- Large files take longer to transfer, especially on mobile connections
- Consider syncing only the files you need rather than entire folders
- WebDAV and FTP may be slower than cloud APIs (Dropbox, Google Drive, OneDrive)
- SFTP performance depends on the SSH connection and server speed

#### Sync stuck at "Pending Upload"

- Check your internet connection
- The file may be too large for the provider (check provider limits)
- Try disconnecting and reconnecting the provider
- Force sync the specific file

#### Duplicate files appearing

- This can happen after a conflict resolution with **Keep Both**
- Conflict copies are named with a suffix (for example, `-conflict-2026-02-23`)
- Review both copies, keep the one you want, and delete the other

### Provider-Specific Issues

#### Dropbox: "Rate limit exceeded"

- Dropbox limits API requests. Wait a few minutes and try again
- Avoid syncing too many files at once

#### Google Drive: "Quota exceeded"

- Your Google account storage is full (shared across Gmail, Drive, and Photos)
- Free up space by deleting files from Google Drive or emptying Trash
- Consider upgrading to a Google One plan for more storage

#### OneDrive: "Item not found"

- The file may have been moved or deleted from another device
- Refresh the file list by pulling down
- Re-sync the folder

#### WebDAV: "405 Method Not Allowed"

- The WebDAV server may not support the requested operation
- Verify the WebDAV URL is correct (common mistake: wrong path for Nextcloud/ownCloud)
- Check that your user account has write permissions on the server

#### FTP: "Passive mode failed"

- Your network or firewall may be blocking passive mode data connections
- Try disabling Passive Mode in the FTP settings (use Active Mode instead)
- If behind a corporate firewall, contact your network administrator

#### Git: "Permission denied"

- Check that your Personal Access Token has the required scopes (usually `repo` access)
- For SSH, verify that your public key is added to your Git hosting account
- Make sure the repository URL is correct and you have push access

#### SMB: "Unable to connect"

- Verify you are on the same network as the file server
- Check that the SMB service is running on the server
- Ensure port 445 is not blocked by a firewall
- Try using the server's IP address instead of its hostname

---

## Managing Multiple Providers

You can add multiple storage providers and switch between them freely:

1. Each provider appears as a separate entry in the cloud storage list
2. You can enable or disable providers without removing them
3. Set a **Priority** for each provider to control display order (lower number = higher priority)
4. Use the **Name** field to label providers clearly (for example, "Work Nextcloud" vs. "Personal Dropbox")

### Best Practices

- **Keep it simple**: Most users only need one or two providers
- **Name providers clearly**: Use descriptive names so you know which is which
- **Set root folders**: Limit each provider to a specific folder to avoid clutter
- **Disable unused providers**: Turn off providers you are not actively using to reduce background activity
- **Test connections periodically**: Tokens expire, passwords change, and servers move; test connections when issues arise

---

## Next Steps

- **[Getting Started](./getting-started.md)** -- Set up Yole for the first time
- **[Format Guides](./formats/README.md)** -- Learn about supported text formats
- **[FAQ](./faq.md)** -- Frequently asked questions
- **[Back to Getting Started](./getting-started.md)**

---

*Last updated: February 23, 2026*
*Yole version: 2.15.1+*
