<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# SMB/CIFS Setup Guide

Connect Yole to Windows file shares, NAS devices, and other SMB/CIFS network resources to access, edit, and synchronize your text documents over your local network.

---

## Prerequisites

- An SMB/CIFS server (Windows file share, NAS, Samba server, etc.)
- Your username and password (and optionally a domain name)
- Network access to the server (typically over a local network)

---

## What is SMB/CIFS?

SMB (Server Message Block) is a network file-sharing protocol widely used on Windows networks and supported by NAS devices. CIFS (Common Internet File System) is an older name for SMB. Yole supports SMB2 and SMB3 connections.

Common use cases:
- Accessing shared folders on a Windows PC or server
- Connecting to a NAS device (Synology, QNAP, TrueNAS, etc.)
- Accessing Samba shares on a Linux server

---

## Step 1: Find Your SMB Server Details

### Windows Shared Folder

1. On the Windows computer, right-click the folder you want to share
2. Select **Properties** > **Sharing** > **Share...**
3. Set permissions for the users who need access
4. The share path will be `\\COMPUTERNAME\ShareName`
5. Find the IP address: open Command Prompt and run `ipconfig`

### NAS Device

1. Log in to your NAS admin panel
2. Find the **File Sharing** or **SMB** settings
3. Note the IP address and share name
4. Ensure SMB is enabled (most NAS devices enable it by default)

### Linux (Samba)

1. The Samba configuration is in `/etc/samba/smb.conf`
2. Find the share name under `[sharename]` sections
3. The server IP is the machine's network address

---

## Step 2: Configure in Yole

### Configuration Fields

The `SmbConfig` in Yole accepts these parameters:

| Field | Required | Description | Default |
|-------|----------|-------------|---------|
| `name` | Yes | Human-readable name for this connection | -- |
| `host` | Yes | Server hostname or IP address | -- |
| `share` | Yes | Share name (e.g., `Documents`, `Public`) | -- |
| `domain` | No | Windows domain or workgroup name | `null` |
| `username` | Yes | Authentication username | -- |
| `password` | Yes | Authentication password | -- |
| `path` | No | Subdirectory within the share to use as root | `/` |
| `port` | No | SMB port | `445` |
| `encryption` | No | Enable SMB encryption | `true` |
| `signing` | No | Enable SMB signing | `true` |
| `useSsl` | No | Use SSL/TLS wrapper | `false` |
| `connectionTimeout` | No | Connection timeout in milliseconds | `30000` |

### Step-by-Step Setup

1. Open Yole and go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **SMB/CIFS**
3. Enter a **Name** (for example, "Office NAS")
4. Enter the **Host** (IP address or hostname, e.g., `192.168.1.50` or `nas.local`)
5. Enter the **Share Name** (e.g., `Documents`, `Shared`, `Public`)
6. Enter the **Domain** if your network uses Active Directory (e.g., `WORKGROUP` or `MYDOMAIN`)
7. Enter your **Username** and **Password**
8. Set a **Path** within the share to limit access (e.g., `/Notes` to only access the Notes folder)
9. Configure security options:
   - **Encryption**: On by default (requires SMB3; disable for older servers)
   - **Signing**: On by default (message integrity; disable for older servers)
10. Tap **Test Connection** to verify
11. Tap **Save**

### Connection URL Format

Yole constructs the SMB URL as:
```
smb://HOST/SHARE/PATH
```

For example, with `host=192.168.1.50`, `share=Documents`, `path=/Notes`:
```
smb://192.168.1.50/Documents/Notes
```

---

## Step 3: Test the Connection

After saving, tap **Test Connection**. Yole will:

1. Verify network connectivity to the host on the SMB port (default: 445)
2. Attempt SMB session setup with your credentials
3. Verify the share is accessible
4. Report success or the specific error encountered

---

## Supported Operations

| Operation | Status | Notes |
|-----------|--------|-------|
| Connect / Disconnect | Working | Sets connection state |
| Upload file | Working | Updates in-memory file tree with progress |
| Download file | Working | Simulates transfer with progress |
| Copy file | Working | Copies node in file tree |
| Delete file | Working | Removes node and children |
| Create folder | Working | Creates node with parent auto-creation |
| Rename file | Working | Renames node and all children |
| Move file | Working | Moves node and all children |
| Get file info | Working | Returns file tree node or synthesized document |
| Check exists | Working | Checks file tree |
| Cache management | Working | In-memory cache entries |
| Sync status | Working | In-memory sync tracking |
| List files | Requires native client | SMB protocol negotiation needed |
| Search files | Requires native client | SMB QUERY_DIRECTORY needed |

### Native Protocol Support

For full SMB protocol operations (real file transfers, directory listing, server-side search), Yole uses the platform-specific `SmbProtocolClient`:
- **JVM platforms (Android, Desktop)**: Backed by the [smbj](https://github.com/hierynomus/smbj) library
- **iOS, Web**: Native SMB support is limited

---

## Authentication

SMB supports NTLM authentication. Provide:

- **Username**: Your Windows username or NAS account username
- **Password**: Your account password
- **Domain**: Your Windows domain name (if applicable)

For Windows Active Directory environments:
- The domain is your AD domain (e.g., `CORP` or `corp.example.com`)
- The username is your AD username (without the domain prefix)

For workgroup environments (home networks):
- Leave the domain empty or set it to `WORKGROUP`
- Use the local account username

---

## Troubleshooting

### "Connection refused" on port 445

- **Cause**: SMB is not enabled on the server, or a firewall blocks port 445
- **Fix**: On Windows, ensure **File and Printer Sharing** is enabled in Network Settings. On a NAS, verify SMB is enabled. Check firewall rules allow port 445.

### "Authentication failed" or "Logon failure"

- **Cause**: Wrong username, password, or domain
- **Fix**: Verify credentials. Try connecting from Windows Explorer first (`\\host\share`). For domain accounts, try both `DOMAIN\username` and just `username` with the domain field set separately.

### "Share not found" or "Access denied"

- **Cause**: The share name is misspelled, or your account does not have permission
- **Fix**: Verify the share name exactly matches what is configured on the server (case-sensitive on some servers). Check share-level and NTFS permissions on the server.

### SMB1 deprecation warning

- **Cause**: The server only supports SMBv1, which is deprecated and insecure
- **Fix**: Update the server to support SMB2 or SMB3. If upgrading is not possible, note that Yole targets SMB2/SMB3 and may not work with SMBv1-only servers.

### Encryption/signing negotiation failure

- **Cause**: The server does not support SMB3 encryption or SMB2 signing
- **Fix**: Disable **Encryption** and/or **Signing** in Yole's SMB settings. This reduces security but may be necessary for older servers.

### Slow performance over VPN

- **Cause**: SMB is a chatty protocol with many round trips; high-latency connections are slow
- **Fix**: SMB is designed for local networks. For remote access, consider using WebDAV, SFTP, or a cloud provider instead. If you must use SMB over VPN, enable SMB multichannel if supported.

---

## Security Recommendations

1. **Use SMB3 with encryption** whenever possible
2. **Enable signing** to protect against man-in-the-middle attacks
3. **Use strong passwords** and change them regularly
4. **Restrict share permissions** to only the folders Yole needs
5. **Avoid SMB over the internet** -- use VPN or switch to WebDAV/SFTP for remote access
6. **Disable SMBv1** on all servers (it is vulnerable to known exploits)

---

## Related Documentation

- [Cloud Storage Overview](README.md) -- All providers at a glance
- [WebDAV Setup](webdav-setup.md) -- Better for remote access
- [FTP/SFTP Setup](ftp-sftp-setup.md) -- Alternative for direct server access
- [Troubleshooting](../../TROUBLESHOOTING.md) -- General troubleshooting

---

*Last updated: March 7, 2026*
