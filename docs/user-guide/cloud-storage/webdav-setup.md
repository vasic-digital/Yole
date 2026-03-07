<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# WebDAV Setup Guide

Connect Yole to any WebDAV-compatible server to access, edit, and synchronize your text documents. WebDAV is an open standard supported by Nextcloud, ownCloud, Seafile, Fastmail, Box, and many other services.

---

## Prerequisites

- A WebDAV server URL
- Your username and password (or other credentials depending on the authentication type)
- An internet connection or local network access to the server

---

## Step 1: Find Your WebDAV Server URL

The WebDAV URL varies by service. Here are the most common ones:

| Service | WebDAV URL Format |
|---------|-------------------|
| **Nextcloud** | `https://your-server.com/remote.php/dav/files/USERNAME/` |
| **ownCloud** | `https://your-server.com/remote.php/webdav/` |
| **Seafile** | `https://your-server.com/seafdav/` |
| **Fastmail** | `https://myfiles.fastmail.com/` |
| **Box** | `https://dav.box.com/dav/` |
| **Apache (mod_dav)** | `https://your-server.com/webdav/` |
| **Nginx (dav module)** | Depends on your configuration |

Replace `your-server.com` and `USERNAME` with your actual server address and username.

---

## Step 2: Choose an Authentication Type

Yole supports four WebDAV authentication methods:

| Type | Description | When to Use |
|------|-------------|------------|
| **Basic** | Username and password sent with each request (Base64-encoded) | Most common; use with HTTPS |
| **Digest** | Challenge-response authentication (password never sent in cleartext) | More secure than Basic over HTTP; rare in practice |
| **OAuth** | Token-based authentication via OAuth 2.0 | Services that support it (some enterprise setups) |
| **None** | No authentication | Public/anonymous WebDAV servers only |

**Recommendation**: Use **Basic** authentication over **HTTPS**. This is the standard for Nextcloud, ownCloud, and most other services.

---

## Step 3: Configure in Yole

### Using the Settings UI

1. Open Yole and go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **WebDAV**
3. Enter a **Name** for this connection (for example, "My Nextcloud")
4. Enter the **Server URL** (from Step 1)
5. Enter your **Username** and **Password**
6. Choose the **Authentication Type** (default: Basic)
7. Configure SSL options:
   - **SSL Enabled**: On by default (recommended)
   - **Verify Certificate**: On by default; disable only for self-signed certificates
8. Optionally adjust timeouts:
   - **Connection Timeout**: 30,000 ms (30 seconds) by default
   - **Read Timeout**: 60,000 ms (60 seconds) by default
9. Tap **Test Connection** to verify
10. Tap **Save**

### Configuration Fields

The `WebDavConfig` in Yole accepts these parameters:

| Field | Required | Description | Default |
|-------|----------|-------------|---------|
| `name` | Yes | Human-readable name for this connection | -- |
| `url` | Yes | Full WebDAV server URL | -- |
| `username` | Yes | Authentication username | -- |
| `password` | Yes | Authentication password | -- |
| `authenticationType` | No | `BASIC`, `DIGEST`, `OAUTH`, or `NONE` | `BASIC` |
| `sslEnabled` | No | Whether to use HTTPS | `true` |
| `verifyCertificate` | No | Whether to verify SSL certificates | `true` |
| `connectionTimeout` | No | Connection timeout in milliseconds | `30000` |
| `readTimeout` | No | Read timeout in milliseconds | `60000` |

---

## Step 4: Test the Connection

After saving, tap **Test Connection**. Yole will:

1. Send an HTTP `OPTIONS` request to the server URL to verify WebDAV capability
2. Check that the server responds with DAV-related headers
3. Verify authentication credentials are accepted
4. Report success or the specific error encountered

---

## Supported Operations

| Operation | WebDAV Method | Notes |
|-----------|--------------|-------|
| Connect | `OPTIONS` | Verifies WebDAV capability |
| List files | `PROPFIND` (Depth: 1) | Parses multistatus XML response |
| Download | `GET` | With progress tracking |
| Upload | `PUT` | With progress tracking |
| Delete | `DELETE` | Deletes files and folders |
| Create folder | `MKCOL` | Creates a new collection (folder) |
| Move | `MOVE` | With `Destination` header |
| Copy | `COPY` | With `Destination` header (server-side) |
| Rename | `MOVE` | Move to same parent with new name |
| Get info | `PROPFIND` (Depth: 0) | Returns properties for a single resource |

---

## Service-Specific Tips

### Nextcloud

- Use an **App Password** instead of your main password for better security:
  1. Log in to Nextcloud in a browser
  2. Go to **Settings** > **Security** > **Devices & sessions**
  3. Enter a name (e.g., "Yole") and click **Create new app password**
  4. Use the generated password in Yole
- WebDAV URL: `https://your-nextcloud.com/remote.php/dav/files/USERNAME/`
- To access a specific folder, append the path: `https://your-nextcloud.com/remote.php/dav/files/USERNAME/Documents/Notes/`

### ownCloud

- Similar to Nextcloud. WebDAV URL: `https://your-owncloud.com/remote.php/webdav/`
- App passwords are available under **Settings** > **Security**

### Fastmail

- WebDAV URL: `https://myfiles.fastmail.com/`
- Use your Fastmail username and an app-specific password
- Generate app passwords at **Settings** > **Password & Security** > **Third-party apps**

### Self-Hosted (Apache/Nginx)

- Ensure the WebDAV module is enabled and properly configured
- Test with a command-line tool first:
  ```bash
  curl -u username:password -X PROPFIND https://your-server.com/webdav/ -H "Depth: 1"
  ```
- If you see a valid XML response, the server is ready for Yole

---

## Troubleshooting

### "Connection refused" or "Unable to connect"

- **Cause**: The server is unreachable, the URL is wrong, or a firewall is blocking the connection
- **Fix**: Verify the URL in a browser. Check that the port is open (443 for HTTPS, 80 for HTTP). Test with `curl` from the command line.

### "SSL certificate error" or "Certificate not trusted"

- **Cause**: The server uses a self-signed certificate or a certificate from an untrusted CA
- **Fix**: Either install the CA certificate on your device, or disable **Verify Certificate** in Yole's WebDAV settings. Disabling verification reduces security -- use only on trusted networks.

### "401 Unauthorized" or "Authentication failed"

- **Cause**: Wrong username, wrong password, or wrong authentication type
- **Fix**: Double-check credentials. If using Nextcloud/ownCloud, try an app password. Verify the authentication type matches what the server expects.

### "405 Method Not Allowed" on PROPFIND

- **Cause**: The server does not have WebDAV enabled at the specified URL
- **Fix**: Verify the WebDAV URL is correct. For Apache, ensure `mod_dav` is enabled. For Nginx, ensure the `dav_methods` directive includes `PROPFIND`.

### Listing shows parent directory or duplicates

- **Cause**: Some WebDAV servers include the current directory in PROPFIND responses
- **Fix**: Yole filters out the parent directory automatically. If you still see duplicates, check that the server URL does not have a trailing slash issue (try both with and without `/`).

### Slow file listing

- **Cause**: The WebDAV server is responding slowly or the directory contains many files
- **Fix**: Increase the **Read Timeout** in Yole's settings. Consider using a root URL that points to a smaller subdirectory.

---

## Related Documentation

- [Cloud Storage Overview](README.md) -- All providers at a glance
- [FTP/SFTP Setup](ftp-sftp-setup.md) -- Alternative self-hosted protocols
- [Troubleshooting](../../TROUBLESHOOTING.md) -- General troubleshooting

---

*Last updated: March 7, 2026*
