<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# FTP / SFTP Setup Guide

Connect Yole to FTP or SFTP servers to access, edit, and synchronize your text documents. SFTP (SSH File Transfer Protocol) is the recommended choice for security; FTP is supported for legacy servers.

---

## FTP vs. SFTP

| Feature | FTP | SFTP |
|---------|-----|------|
| **Protocol** | File Transfer Protocol over TCP | SSH File Transfer Protocol |
| **Default Port** | 21 | 22 |
| **Encryption** | None (plaintext) | Full SSH encryption |
| **Authentication** | Username/Password only | Password or SSH key |
| **Folder Support** | Limited | Full |
| **Server-side Copy** | Not supported | Not supported |
| **Recommended** | Legacy systems only | Yes, for all new setups |

**Use SFTP whenever possible.** FTP sends credentials and data in plaintext, making it unsuitable for use over the internet.

---

## FTP Setup

### Prerequisites

- An FTP server address (hostname or IP)
- Your username and password
- The FTP port (default: 21)

### Configuration Fields

The `FtpConfig` in Yole accepts these parameters:

| Field | Required | Description | Default |
|-------|----------|-------------|---------|
| `name` | Yes | Human-readable name for this connection | -- |
| `host` | Yes | FTP server hostname or IP address | -- |
| `port` | No | FTP server port | `21` |
| `username` | Yes | FTP username | -- |
| `password` | Yes | FTP password | -- |
| `rootPath` | No | Starting directory path | `/` |
| `passiveMode` | No | Use PASV mode for data connections | `true` |
| `secureFtp` | No | Use FTPS (FTP over TLS) | `false` |
| `encoding` | No | Character encoding for filenames | `UTF-8` |
| `connectionTimeout` | No | Connection timeout in milliseconds | `30000` |

### Step-by-Step Setup

1. Open Yole and go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **FTP**
3. Enter a **Name** (for example, "Office FTP Server")
4. Enter the **Host** (for example, `ftp.example.com` or `192.168.1.100`)
5. Enter the **Port** (default: 21)
6. Enter your **Username** and **Password**
7. Set the **Root Path** to a specific directory (for example, `/home/user/documents`)
8. Enable **Passive Mode** (recommended; required for most firewalls and NAT)
9. Enable **Secure FTP (FTPS)** if your server supports it
10. Tap **Test Connection** to verify
11. Tap **Save**

### FTP Operations

| Operation | FTP Command | Notes |
|-----------|------------|-------|
| Connect | `USER` / `PASS` | Control channel connection with authentication |
| List files | `LIST` | Parses Unix and DOS listing formats |
| Download | `RETR` | Over PASV data connection with progress tracking |
| Upload | `STOR` | Over PASV data connection with progress tracking |
| Delete file | `DELE` | For files |
| Delete folder | `RMD` | For directories |
| Create folder | `MKD` | Creates a new directory |
| Rename | `RNFR` / `RNTO` | Rename from/to pair |
| Move | `RNFR` / `RNTO` | Move is rename with a different path |
| Get info | `SIZE` + `MDTM` | File size and modification time |
| Check exists | `SIZE` or `LIST` | Probes via SIZE, falls back to LIST |

### FTP Limitations

- **No server-side copy** -- FTP has no COPY command; copy operations require download and re-upload
- **No encryption** (unless FTPS is enabled) -- credentials and data are sent in plaintext
- **Limited folder support** -- basic FTP does not have reliable folder metadata
- **No search** -- FTP has no server-side search capability

---

## SFTP Setup

### Prerequisites

- An SFTP/SSH server address (hostname or IP)
- Authentication credentials: either a username/password or an SSH private key
- The SSH port (default: 22)

### Configuration Fields

The `SftpConfig` in Yole accepts these parameters:

| Field | Required | Description | Default |
|-------|----------|-------------|---------|
| `name` | Yes | Human-readable name for this connection | -- |
| `host` | Yes | SSH server hostname or IP address | -- |
| `port` | No | SSH server port | `22` |
| `username` | Depends | SSH username (required for password auth) | `null` |
| `password` | Depends | SSH password (if using password auth) | `null` |
| `privateKeyPath` | Depends | Path to SSH private key file | `null` |
| `privateKeyPassphrase` | No | Passphrase for the private key (if encrypted) | `null` |
| `knownHostsPath` | No | Path to known_hosts file for host verification | `null` |
| `strictHostKeyChecking` | No | Verify server host key against known_hosts | `true` |
| `rootPath` | No | Starting directory path | `/` |
| `useSsl` | No | Use SSL/TLS layer | `true` |
| `connectionTimeout` | No | Connection timeout in milliseconds | `30000` |

### Authentication Methods

SFTP supports two authentication methods:

#### Method 1: Password Authentication

1. Enter your **Username** and **Password**
2. This is the simplest method but less secure than key-based authentication

#### Method 2: SSH Key Authentication (Recommended)

1. Generate an SSH key pair if you do not have one:
   ```bash
   ssh-keygen -t ed25519 -f ~/.ssh/yole_key -C "yole@device"
   ```
2. Copy the public key to your server:
   ```bash
   ssh-copy-id -i ~/.ssh/yole_key.pub user@your-server.com
   ```
3. In Yole, set the **Private Key Path** to the private key file:
   - Android: copy the key to `/storage/emulated/0/Documents/.ssh/yole_key`
   - Desktop: use the full path, e.g., `/home/user/.ssh/yole_key`
4. If your key is encrypted with a passphrase, enter it in the **Private Key Passphrase** field

### Step-by-Step Setup

1. Open Yole and go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **SFTP**
3. Enter a **Name** (for example, "Home Server")
4. Enter the **Host** (for example, `ssh.example.com` or `192.168.1.100`)
5. Enter the **Port** (default: 22)
6. Choose your authentication method:
   - For password: enter **Username** and **Password**
   - For SSH key: enter **Username**, set **Private Key Path**, and optionally set **Private Key Passphrase**
7. Set the **Root Path** (for example, `/home/user/notes`)
8. Configure host key verification:
   - **Strict Host Key Checking**: On by default; disable only for first-time connections to unknown servers
   - **Known Hosts Path**: Leave empty to use the system default, or specify a custom path
9. Tap **Test Connection** to verify
10. Tap **Save**

### SFTP Operations

| Operation | Description | Notes |
|-----------|------------|-------|
| Connect | SSH handshake + SFTP subsystem | Validates config, authenticates |
| List files | Directory listing | Returns entries from remote directory |
| Download | Chunked encrypted transfer | With progress tracking |
| Upload | Chunked encrypted transfer | With progress tracking |
| Delete | Remove file or directory | Recursive for directories |
| Create folder | Make directory | With proper permissions |
| Rename | Rename/move | POSIX rename semantics |
| Move | Rename with different path | Same as rename |
| Get info | Stat file | Returns permissions, size, timestamps |
| Check exists | Stat probe | Returns true/false |

---

## Troubleshooting

### FTP: "Connection refused" on port 21

- **Cause**: FTP server is not running or firewall blocks port 21
- **Fix**: Verify the server is running. Check firewall rules. Try connecting with a standalone FTP client (e.g., `ftp` command or FileZilla).

### FTP: "Passive mode refused" or data connection timeout

- **Cause**: The server's PASV response contains an internal IP address that is not reachable from your network
- **Fix**: If you control the server, configure the FTP server's passive port range and external IP. If not, try disabling passive mode (but this rarely works behind NAT).

### SFTP: "Host key verification failed"

- **Cause**: The server's host key is not in your known_hosts file
- **Fix**: Connect to the server once using `ssh user@host` from a terminal and accept the host key. Alternatively, temporarily disable **Strict Host Key Checking** in Yole (less secure).

### SFTP: "Permission denied (publickey)"

- **Cause**: The server does not accept password authentication, or the SSH key is not authorized
- **Fix**: Verify the public key is in `~/.ssh/authorized_keys` on the server. Check file permissions (`chmod 600 ~/.ssh/authorized_keys`). Ensure the private key path in Yole is correct.

### SFTP: "Private key passphrase incorrect"

- **Cause**: The passphrase entered does not match the key's encryption
- **Fix**: Verify the passphrase. Test locally: `ssh-keygen -y -f /path/to/key` will prompt for the passphrase and print the public key on success.

### Both: "Connection timed out"

- **Cause**: Network issue, wrong hostname/IP, or firewall blocking the port
- **Fix**: Verify the hostname resolves correctly (`ping hostname`). Check the port is open (`nc -zv hostname port`). Increase the connection timeout in Yole if the server is slow to respond.

### Both: File encoding issues (garbled filenames)

- **Cause**: The server uses a different character encoding than UTF-8
- **Fix**: For FTP, change the **Encoding** field to match the server (e.g., `ISO-8859-1` or `Windows-1252`). SFTP always uses UTF-8.

---

## Security Recommendations

1. **Always prefer SFTP over FTP** -- all traffic is encrypted
2. **Use SSH key authentication** instead of passwords when possible
3. **Use FTPS** if you must use FTP -- enable the `secureFtp` option
4. **Never use plain FTP over the internet** -- only on trusted local networks
5. **Use a dedicated user account** with restricted permissions for Yole
6. **Set a specific root path** to limit Yole's access to only the directories it needs

---

## Related Documentation

- [Cloud Storage Overview](README.md) -- All providers at a glance
- [WebDAV Setup](webdav-setup.md) -- Alternative self-hosted protocol
- [Git Setup](git-setup.md) -- Version-controlled alternative
- [Troubleshooting](../../TROUBLESHOOTING.md) -- General troubleshooting

---

*Last updated: March 7, 2026*
