<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Git Repository Setup Guide

Connect Yole to a Git repository to access your text documents with full version control. Yole supports GitHub, GitLab, Bitbucket, and any Git server that supports the Smart HTTP protocol.

---

## Prerequisites

- A Git repository (hosted on GitHub, GitLab, Bitbucket, or a self-hosted server)
- Authentication credentials: personal access token, username/password, or SSH key
- An internet connection (or local network access for self-hosted servers)

---

## Step 1: Choose a Hosting Platform

Yole supports Git access through two mechanisms:

1. **Platform REST APIs** (recommended for GitHub, GitLab, Bitbucket) -- Uses the platform's native API for file browsing, metadata, and content retrieval
2. **Git Smart HTTP** -- Direct access via the Git protocol's `info/refs` endpoint; works with any Git server

### Platform Comparison

| Platform | REST API | Smart HTTP | Authentication |
|----------|----------|------------|----------------|
| **GitHub** | GitHub Contents API | Yes | Personal Access Token |
| **GitLab** | GitLab Repository Files API | Yes | Personal Access Token |
| **Bitbucket** | Bitbucket Source API | Yes | App Password |
| **Self-hosted** | Depends on software | Yes | Username/Password or Token |

---

## Step 2: Create Authentication Credentials

### GitHub Personal Access Token

1. Go to [GitHub Settings > Developer Settings > Personal Access Tokens > Fine-grained tokens](https://github.com/settings/tokens?type=beta)
2. Click **Generate new token**
3. Enter a token name (for example, `yole-access`)
4. Set an expiration date
5. Under **Repository access**, select the repositories Yole should access
6. Under **Permissions** > **Repository permissions**, grant:
   - **Contents**: Read and write
   - **Metadata**: Read-only
7. Click **Generate token** and copy the token immediately

### GitLab Personal Access Token

1. Go to [GitLab > User Settings > Access Tokens](https://gitlab.com/-/user_settings/personal_access_tokens)
2. Enter a token name (for example, `yole-access`)
3. Set an expiration date
4. Select scopes:
   - `read_repository`
   - `write_repository` (if you want to push changes)
5. Click **Create personal access token** and copy the token

### Bitbucket App Password

1. Go to [Bitbucket > Personal Settings > App passwords](https://bitbucket.org/account/settings/app-passwords/)
2. Click **Create app password**
3. Enter a label (for example, `yole-access`)
4. Select permissions:
   - **Repositories**: Read, Write
5. Click **Create** and copy the password

### SSH Key (Advanced)

If your Git server supports SSH:

1. Generate an SSH key pair:
   ```bash
   ssh-keygen -t ed25519 -f ~/.ssh/yole_git_key -C "yole@device"
   ```
2. Add the public key to your Git hosting platform:
   - GitHub: Settings > SSH and GPG keys > New SSH key
   - GitLab: User Settings > SSH Keys
   - Bitbucket: Personal Settings > SSH keys
3. Use the private key path in Yole's configuration

---

## Step 3: Configure in Yole

### Configuration Fields

The `GitConfig` in Yole accepts these parameters:

| Field | Required | Description | Default |
|-------|----------|-------------|---------|
| `name` | Yes | Human-readable name for this connection | -- |
| `repositoryUrl` | Yes | Git repository HTTPS URL | -- |
| `branch` | No | Branch to track | `main` |
| `username` | Depends | Git username (for HTTPS auth) | `null` |
| `password` | Depends | Git password (for HTTPS auth) | `null` |
| `personalAccessToken` | Depends | Personal access token (preferred over password) | `null` |
| `privateKeyPath` | Depends | Path to SSH private key | `null` |
| `privateKeyPassphrase` | No | Passphrase for the private key | `null` |
| `localCachePath` | Yes | Local directory to cache repository files | -- |
| `autoSync` | No | Automatically sync changes | `true` |
| `commitAuthorName` | No | Git commit author name | `Yole` |
| `commitAuthorEmail` | No | Git commit author email | `yole@example.com` |
| `connectionTimeout` | No | Connection timeout in milliseconds | `30000` |

### Repository URL Format

| Platform | URL Format |
|----------|-----------|
| GitHub | `https://github.com/owner/repo.git` |
| GitLab | `https://gitlab.com/owner/repo.git` |
| Bitbucket | `https://bitbucket.org/owner/repo.git` |
| Self-hosted | `https://your-server.com/owner/repo.git` |

### Step-by-Step Setup

1. Open Yole and go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **Git**
3. Enter a **Name** (for example, "My Notes Repo")
4. Enter the **Repository URL** (HTTPS URL)
5. Enter the **Branch** to use (default: `main`)
6. Enter your authentication credentials:
   - **Preferred**: Set the **Personal Access Token** field
   - **Alternative**: Set **Username** and **Password**
   - **SSH**: Set **Private Key Path** (and passphrase if needed)
7. Set the **Local Cache Path** where repository files will be stored locally
8. Configure commit settings:
   - **Author Name**: Your name (used in Git commits)
   - **Author Email**: Your email (used in Git commits)
9. Enable or disable **Auto Sync**
10. Tap **Test Connection** to verify
11. Tap **Save**

---

## Step 4: Test the Connection

After saving, tap **Test Connection**. Yole will:

1. Fetch the `info/refs` endpoint to verify Smart HTTP access
2. Attempt to use the platform REST API (GitHub/GitLab) to list repository contents
3. Verify authentication credentials are accepted
4. Report success or the specific error encountered

---

## Supported Operations

| Operation | Method | Notes |
|-----------|--------|-------|
| Connect | `GET info/refs` | Verifies Smart HTTP access |
| List files | Platform Contents API | GitHub/GitLab API, falls back to local cache |
| Download | Platform raw content URL | Fetches file content via platform-specific URLs |
| Upload | Local tracking | Changes tracked locally as pending |
| Delete | Local tracking | Tracked locally; not pushed automatically |
| Create folder | Local tracking | Git does not track empty folders |
| Get info | Cache + HTTP HEAD | Checks local cache, then probes remote |
| Exists | Cache + HTTP HEAD | Checks local cache, then probes remote |

### Current Limitations

- **Write operations are local-only**: Uploads, deletes, and renames are tracked locally as pending changes but are **not pushed** to the remote repository automatically. Push support is planned for a future release.
- **No server-side search**: Search is limited to the local cache.
- **No merge conflict resolution**: If the remote branch has diverged, manual resolution is required.

---

## Recommended Repository Structure

For the best experience with Yole, organize your repository like this:

```
my-notes/
  README.md
  journal/
    2026-03-07.md
    2026-03-06.md
  projects/
    project-alpha.md
    project-beta.md
  reference/
    cheatsheet.md
    links.md
```

- Use Markdown (`.md`) or other supported text formats
- Keep files small for fast sync
- Avoid large binary files (images, PDFs) -- Git is not optimized for them

---

## Troubleshooting

### "Authentication failed" error

- **Cause**: Invalid token, expired token, or wrong username/password
- **Fix**: Verify your personal access token is valid and has not expired. For GitHub, check that the token has the `repo` or `contents:read` scope.

### "Repository not found" error

- **Cause**: The repository URL is wrong, or you do not have access
- **Fix**: Verify the URL. For private repositories, ensure your token has access to the specific repository.

### "Branch not found" error

- **Cause**: The specified branch does not exist
- **Fix**: Check the branch name. Common names are `main`, `master`, or `develop`. Verify in your Git hosting platform's UI.

### "info/refs endpoint not accessible"

- **Cause**: The server does not support Git Smart HTTP, or the URL is incorrect
- **Fix**: Append `/info/refs?service=git-upload-pack` to your repository URL in a browser. If you see a valid response, the server supports Smart HTTP. If not, check the URL or server configuration.

### Changes not appearing on remote

- **Cause**: Yole currently tracks write operations locally but does not push them
- **Fix**: This is a known limitation. Use `git push` from a terminal or another Git client to push changes to the remote.

### Slow file browsing

- **Cause**: Large repositories with many files take longer to list
- **Fix**: Use a dedicated repository for your Yole documents rather than a large project repository.

---

## Related Documentation

- [Cloud Storage Overview](README.md) -- All providers at a glance
- [FTP/SFTP Setup](ftp-sftp-setup.md) -- Alternative for direct server access
- [WebDAV Setup](webdav-setup.md) -- Alternative for self-hosted storage
- [Troubleshooting](../../TROUBLESHOOTING.md) -- General troubleshooting

---

*Last updated: March 7, 2026*
