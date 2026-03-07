<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# Google Drive Setup Guide

Connect Yole to Google Drive to access, edit, and synchronize your text documents through Google's cloud storage.

---

## Prerequisites

- A Google account (personal or Google Workspace)
- An internet connection for the initial setup and OAuth flow
- Yole installed on your device

---

## Step 1: Create a Google Cloud Project

To use your own OAuth credentials (recommended for organizations or advanced users), you need a Google Cloud project. If Yole ships with built-in credentials for your platform, you can skip to [Step 4](#step-4-configure-in-yole).

1. Go to the [Google Cloud Console](https://console.cloud.google.com/)
2. Click the project dropdown at the top and select **New Project**
3. Enter a project name (for example, `yole-drive`) and click **Create**
4. Select the new project from the project dropdown

---

## Step 2: Configure the OAuth Consent Screen

1. In the Google Cloud Console, go to **APIs & Services** > **OAuth consent screen**
2. Choose **External** user type (or **Internal** if you are in a Google Workspace organization and want to restrict to your domain)
3. Click **Create**
4. Fill in the required fields:
   - **App name**: `Yole`
   - **User support email**: your email address
   - **Developer contact information**: your email address
5. Click **Save and Continue**
6. On the **Scopes** screen, click **Add or Remove Scopes** and add:
   - `https://www.googleapis.com/auth/drive.file` -- View and manage files created by Yole
   - `https://www.googleapis.com/auth/drive.readonly` -- View files in Google Drive
7. Click **Update** and then **Save and Continue**
8. On the **Test users** screen, add your Google account email for testing
9. Click **Save and Continue**, then **Back to Dashboard**

### Publishing Status

- **Testing**: Only test users you added can authorize the app (up to 100 users)
- **In production**: Anyone with a Google account can authorize (requires Google's verification process for sensitive scopes)

For personal use, **Testing** status is sufficient.

---

## Step 3: Create OAuth 2.0 Credentials

1. Go to **APIs & Services** > **Credentials**
2. Click **Create Credentials** > **OAuth client ID**
3. Choose the **Application type**:
   - **Android**: Enter your app's package name (`digital.vasic.yole`) and SHA-1 fingerprint
   - **iOS**: Enter your bundle ID
   - **Web application**: Add authorized redirect URIs (`http://localhost:8080/callback`)
   - **Desktop app**: For the desktop client
4. Click **Create**
5. Note the **Client ID** and **Client Secret** displayed in the dialog

### Enable the Google Drive API

1. Go to **APIs & Services** > **Library**
2. Search for `Google Drive API`
3. Click on it and then click **Enable**

### Configuration Fields

The `GoogleDriveConfig` in Yole accepts these parameters:

| Field | Required | Description | Default |
|-------|----------|-------------|---------|
| `name` | Yes | Human-readable name for this connection | -- |
| `clientId` | Yes | OAuth Client ID (from Cloud Console) | -- |
| `clientSecret` | Yes | OAuth Client Secret (from Cloud Console) | -- |
| `accessToken` | Auto | OAuth access token (obtained during sign-in) | -- |
| `refreshToken` | Auto | OAuth refresh token (obtained during sign-in) | -- |
| `rootFolderId` | No | Google Drive folder ID to use as root | `null` (My Drive root) |
| `teamDriveId` | No | Shared Drive (Team Drive) ID | `null` (personal drive) |

---

## Step 4: Configure in Yole

### Using the Settings UI

1. Open Yole and go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **Google Drive**
3. Enter a **Name** for this connection (for example, "Work Drive")
4. If using custom credentials, enter the **Client ID** and **Client Secret**
5. Tap **Sign in with Google**
6. A browser window opens -- log in to your Google account
7. Review the permissions and tap **Allow**
8. The browser redirects back to Yole with the authorization code
9. Yole exchanges the code for access and refresh tokens automatically
10. Optionally set a **Root Folder ID** to limit Yole to a specific folder
11. Optionally set a **Team Drive ID** if you want to connect to a Shared Drive
12. Tap **Test Connection** to verify
13. Tap **Save**

### Finding a Folder ID

To get a Google Drive folder ID:

1. Open [Google Drive](https://drive.google.com/) in a browser
2. Navigate to the folder you want to use as root
3. Look at the URL -- it ends with the folder ID:
   `https://drive.google.com/drive/folders/1ABCdefGHIjklMNOpqrSTUvwxYZ`
4. Copy the ID string after `/folders/`

### What Happens During OAuth

Yole uses `GoogleDriveOAuth2Flow` to handle authentication:

1. Yole opens a browser to `https://accounts.google.com/o/oauth2/v2/auth`
2. You authorize the application
3. Google redirects back with an authorization code
4. Yole exchanges the code via `https://oauth2.googleapis.com/token`
5. Tokens are stored securely using `AuthTokenManager`
6. The access token is automatically refreshed when it expires (typically every hour)

---

## Step 5: Test the Connection

After saving, tap **Test Connection**. Yole will:

1. Verify the access token is valid (refresh if needed)
2. Call `drive/v3/about` to fetch your drive information
3. Verify the root folder (if set) is accessible
4. Report success or the specific error encountered

---

## Supported Operations

| Operation | Google Drive API Endpoint | Notes |
|-----------|--------------------------|-------|
| Connect | `drive/v3/about` | Validates token, fetches drive info |
| List files | `drive/v3/files` | With parent query, parses JSON response |
| Download | `drive/v3/files/{id}?alt=media` | Fetches metadata first, then content |
| Upload | `drive/v3/files` (multipart) | Creates or updates files |
| Delete | `drive/v3/files/{id}` | Moves to trash (or permanently deletes) |
| Create folder | `drive/v3/files` | With `mimeType=application/vnd.google-apps.folder` |
| Move | `drive/v3/files/{id}` (PATCH) | Updates parent references |
| Copy | `drive/v3/files/{id}/copy` | Server-side copy |
| Get info | `drive/v3/files/{id}` | Returns file metadata |
| Search | `drive/v3/files` with `q` param | Full-text search |
| Quota | `drive/v3/about` | Returns storage quota info |

---

## Troubleshooting

### "Access blocked: This app's request is invalid" error

- **Cause**: The OAuth consent screen is not configured or the redirect URI does not match
- **Fix**: Verify the consent screen is set up (Step 2) and the redirect URI in your OAuth client matches what Yole sends.

### "Error 403: access_denied" during sign-in

- **Cause**: Your Google account is not listed as a test user
- **Fix**: Add your email address to the test users list in the OAuth consent screen configuration (APIs & Services > OAuth consent screen > Test users).

### "Quota exceeded" error

- **Cause**: Google Drive API has a per-project limit of 12,000 requests per minute
- **Fix**: This is very unlikely to hit with normal usage. If you do, wait a minute and try again. Yole's built-in rate limiter should prevent this.

### "File not found" after moving a file in Google Drive

- **Cause**: Google Drive uses unique file IDs, not paths. If you move a file in the Google Drive web interface, Yole's cached metadata may be stale.
- **Fix**: Use the **Refresh** button in the file browser to reload the file listing from Google Drive.

### OAuth token keeps expiring

- **Cause**: Google access tokens expire every hour. If the refresh token is missing or revoked, Yole cannot automatically renew the access token.
- **Fix**: Re-authenticate by going to **Settings** > **Cloud Storage**, selecting the connection, and tapping **Re-authenticate**. Ensure `offline` access was granted during the OAuth flow.

### Cannot access Shared Drives

- **Cause**: The `teamDriveId` is not set, or the Shared Drive has restricted access
- **Fix**: Set the `teamDriveId` field to the Shared Drive's ID. Verify your account has permission to access the Shared Drive.

---

## Revoking Access

To disconnect Yole from your Google account:

1. In Yole: Go to **Settings** > **Cloud Storage**, select the connection, and tap **Remove**
2. In Google: Go to [Google Account Permissions](https://myaccount.google.com/permissions) and remove Yole

---

## Related Documentation

- [Cloud Storage Overview](README.md) -- All providers at a glance
- [Dropbox Setup](dropbox-setup.md) -- Alternative cloud provider
- [OneDrive Setup](onedrive-setup.md) -- Microsoft cloud alternative
- [Troubleshooting](../../TROUBLESHOOTING.md) -- General troubleshooting

---

*Last updated: March 7, 2026*
