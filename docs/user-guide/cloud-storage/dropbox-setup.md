<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Dropbox Setup Guide

Connect Yole to your Dropbox account to access, edit, and synchronize your text documents in the cloud.

---

## Prerequisites

- A Dropbox account (free or paid)
- An internet connection for the initial setup and OAuth flow
- Yole installed on your device

---

## Step 1: Create a Dropbox App

To connect Yole to Dropbox, you need OAuth 2.0 credentials. If you are using Yole's built-in Dropbox integration (which ships with pre-configured credentials), you can skip to [Step 3](#step-3-configure-in-yole). If you want to use your own Dropbox app (recommended for advanced users or organizations), follow these steps:

1. Go to the [Dropbox App Console](https://www.dropbox.com/developers/apps)
2. Click **Create app**
3. Choose **Scoped access** as the API type
4. Choose **Full Dropbox** or **App folder** access:
   - **Full Dropbox** -- Yole can access all files and folders in your Dropbox
   - **App folder** -- Yole can only access files in its own dedicated folder (`/Apps/YoleName/`)
5. Give your app a unique name (for example, `yole-my-name`)
6. Click **Create app**

---

## Step 2: Get OAuth 2.0 Credentials

After creating the app, you land on the app's **Settings** page:

1. Note the **App key** (this is the `clientId` / `appKey`)
2. Note the **App secret** (this is the `clientSecret` / `appSecret`) -- click **Show** to reveal it
3. Under **OAuth 2**, set the redirect URI:
   - For Android: `yole://oauth/callback`
   - For Desktop: `http://localhost:8080/callback`
4. Under **Permissions** tab, enable at minimum:
   - `files.metadata.read`
   - `files.metadata.write`
   - `files.content.read`
   - `files.content.write`
   - `account_info.read`
5. Click **Submit** on the Permissions tab to save

### Configuration Fields

The `DropboxConfig` in Yole accepts these parameters:

| Field | Required | Description | Default |
|-------|----------|-------------|---------|
| `name` | Yes | Human-readable name for this connection | -- |
| `appKey` | Yes | Dropbox App Key (from App Console) | -- |
| `appSecret` | Yes | Dropbox App Secret (from App Console) | -- |
| `accessToken` | Auto | OAuth access token (obtained during sign-in) | -- |
| `refreshToken` | Auto | OAuth refresh token (obtained during sign-in) | -- |
| `rootPath` | No | Folder path to use as root (e.g., `/Notes`) | `""` (entire Dropbox) |

---

## Step 3: Configure in Yole

### Using the Settings UI

1. Open Yole and go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **Dropbox**
3. Enter a **Name** for this connection (for example, "My Dropbox")
4. If using custom credentials, enter the **App Key** and **App Secret**
5. Tap **Sign in with Dropbox**
6. A browser window opens -- log in to your Dropbox account
7. Review the permissions Yole is requesting and tap **Allow**
8. The browser redirects back to Yole with the authorization code
9. Yole exchanges the code for access and refresh tokens automatically
10. Optionally set a **Root Path** to limit Yole to a specific folder (e.g., `/Notes`)
11. Tap **Test Connection** to verify
12. Tap **Save**

### What Happens During OAuth

Yole uses `DropboxOAuth2Flow` to handle authentication:

1. Yole opens a browser to `https://www.dropbox.com/oauth2/authorize` with your App Key
2. You authorize the application
3. Dropbox redirects back with an authorization code
4. Yole exchanges the code for an access token and refresh token via `https://api.dropboxapi.com/oauth2/token`
5. Tokens are stored securely using `AuthTokenManager` and platform-specific secure storage
6. When the access token expires, Yole automatically refreshes it using the refresh token

---

## Step 4: Test the Connection

After saving, tap **Test Connection**. Yole will:

1. Verify the access token is valid
2. Call `2/users/get_current_account` to fetch your account information
3. Confirm the root path (if set) is accessible
4. Report success or the specific error encountered

A successful test means you are ready to browse, upload, and download files.

---

## Supported Operations

| Operation | Dropbox API Endpoint | Notes |
|-----------|---------------------|-------|
| Connect | `2/users/get_current_account` | Validates token, fetches account info |
| List files | `2/files/list_folder` | Supports pagination via `list_folder/continue` |
| Download | `2/files/download` | Via content endpoint with progress tracking |
| Upload | `2/files/upload` | Supports overwrite, add, or update modes |
| Delete | `2/files/delete_v2` | Deletes files and folders |
| Create folder | `2/files/create_folder_v2` | Creates nested folders |
| Move | `2/files/move_v2` | Moves files between folders |
| Copy | `2/files/copy_v2` | Server-side copy |
| Rename | `2/files/move_v2` | Rename is a move to the same parent with a new name |
| Get info | `2/files/get_metadata` | Returns file metadata |
| Search | `2/files/search_v2` | Full-text search across your Dropbox |
| Quota | `2/users/get_space_usage` | Returns total and used space |

---

## Troubleshooting

### "Invalid access token" error

- **Cause**: Your access token has expired and automatic refresh failed
- **Fix**: Go to **Settings** > **Cloud Storage**, select the Dropbox connection, and tap **Re-authenticate**. This starts a new OAuth flow.

### "App not approved" or "developer mode" warning

- **Cause**: Your custom Dropbox app is in development mode (limited to 500 users)
- **Fix**: This is normal for personal use. If you need more than 500 users, submit your app for Dropbox's production approval process.

### "Permission denied" when accessing a folder

- **Cause**: If your app was created with **App folder** access, it can only see files in `/Apps/YourAppName/`
- **Fix**: Either move your files into the app folder, or create a new app with **Full Dropbox** access.

### Connection times out

- **Cause**: Network connectivity issue or firewall blocking Dropbox API endpoints
- **Fix**: Verify you can reach `api.dropboxapi.com` and `content.dropboxapi.com` in a browser. Check your firewall or proxy settings.

### "Rate limit exceeded" error

- **Cause**: Too many API requests in a short time (Dropbox limits to ~100 requests per minute for development apps)
- **Fix**: Yole includes built-in rate limiting via `RateLimiter` and `AdaptiveRateLimiter`. If you still hit limits, wait a few minutes and try again. Reduce the sync frequency if the issue persists.

### Files not appearing after upload

- **Cause**: The file was uploaded to a different path than expected
- **Fix**: Check the root path setting. If `rootPath` is set to `/Notes`, uploads go to `/Notes/<filename>`, not the Dropbox root.

---

## Revoking Access

To disconnect Yole from your Dropbox account:

1. In Yole: Go to **Settings** > **Cloud Storage**, select the Dropbox connection, and tap **Remove**
2. In Dropbox: Go to [Dropbox Account Settings > Connected Apps](https://www.dropbox.com/account/connected_apps) and click **Disconnect** next to Yole

---

## Related Documentation

- [Cloud Storage Overview](README.md) -- All providers at a glance
- [Google Drive Setup](google-drive-setup.md) -- Alternative cloud provider
- [Troubleshooting](../../TROUBLESHOOTING.md) -- General troubleshooting

---

*Last updated: March 7, 2026*
