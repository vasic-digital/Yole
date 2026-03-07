<!--
SPDX-FileCopyrightText: 2024-2026 Milos Vasic
SPDX-License-Identifier: Apache-2.0
-->

# OneDrive Setup Guide

Connect Yole to Microsoft OneDrive to access, edit, and synchronize your text documents through Microsoft's cloud storage. Yole supports personal OneDrive, OneDrive for Business, SharePoint document libraries, and Microsoft 365 group drives.

---

## Prerequisites

- A Microsoft account (personal, business, or school)
- An internet connection for the initial setup and OAuth flow
- Yole installed on your device

---

## Step 1: Register an Application in Azure AD

To use your own OAuth credentials (recommended for organizations), you need to register an application in Azure Active Directory. If Yole ships with built-in credentials for your platform, you can skip to [Step 3](#step-3-configure-in-yole).

1. Go to the [Azure Portal - App Registrations](https://portal.azure.com/#blade/Microsoft_AAD_RegisteredApps/ApplicationsListBlade)
2. Click **New registration**
3. Fill in the fields:
   - **Name**: `Yole`
   - **Supported account types**: Choose based on your needs:
     - **Accounts in any organizational directory and personal Microsoft accounts** -- broadest access
     - **Accounts in this organizational directory only** -- restricted to your organization
     - **Personal Microsoft accounts only** -- restricted to personal accounts
   - **Redirect URI**: Select **Public client/native (mobile & desktop)** and enter `yole://oauth/callback`
4. Click **Register**
5. Note the **Application (client) ID** -- this is your `clientId`

---

## Step 2: Configure API Permissions and Client Secret

### API Permissions

1. In your app registration, go to **API permissions**
2. Click **Add a permission** > **Microsoft Graph** > **Delegated permissions**
3. Add the following permissions:
   - `Files.ReadWrite` -- Read and write your files
   - `Files.ReadWrite.All` -- Read and write all files you have access to
   - `User.Read` -- Read your basic profile
   - `offline_access` -- Maintain access for token refresh
4. Click **Add permissions**
5. If you are an admin, click **Grant admin consent** for your organization

### Client Secret

1. Go to **Certificates & secrets**
2. Click **New client secret**
3. Enter a description (for example, `yole-secret`) and choose an expiration period
4. Click **Add**
5. Copy the **Value** immediately -- this is your `clientSecret` (it will not be shown again)

### Configuration Fields

The `OneDriveConfig` in Yole accepts these parameters:

| Field | Required | Description | Default |
|-------|----------|-------------|---------|
| `name` | Yes | Human-readable name for this connection | -- |
| `clientId` | Yes | Application (client) ID from Azure AD | -- |
| `clientSecret` | Yes | Client secret value from Azure AD | -- |
| `accessToken` | Auto | OAuth access token (obtained during sign-in) | -- |
| `refreshToken` | Auto | OAuth refresh token (obtained during sign-in) | -- |
| `driveType` | No | Drive type: `ME`, `BUSINESS`, `SHAREPOINT`, `GROUP` | `ME` |
| `driveId` | No | Specific drive ID (for business/SharePoint/group) | `null` |
| `rootFolderId` | No | Folder ID to use as root | `null` (drive root) |

### Drive Types

| Type | Description | Microsoft Graph Endpoint |
|------|-------------|------------------------|
| `ME` | Personal OneDrive | `v1.0/me/drive` |
| `BUSINESS` | OneDrive for Business | `v1.0/me/drive` (same endpoint, different storage) |
| `SHAREPOINT` | SharePoint document library | `v1.0/sites/{siteId}/drive` |
| `GROUP` | Microsoft 365 group files | `v1.0/groups/{groupId}/drive` |

---

## Step 3: Configure in Yole

### Using the Settings UI

1. Open Yole and go to **Settings** > **Cloud Storage** > **Add Provider**
2. Select **OneDrive**
3. Enter a **Name** (for example, "Personal OneDrive")
4. Choose the **Drive Type**:
   - **Personal** for your personal OneDrive
   - **Business** for OneDrive for Business
   - **SharePoint** for a SharePoint document library (requires site/drive ID)
   - **Group** for a Microsoft 365 group (requires group/drive ID)
5. If using custom credentials, enter the **Client ID** and **Client Secret**
6. Tap **Sign in with Microsoft**
7. A browser window opens -- log in to your Microsoft account
8. Review the permissions and tap **Accept**
9. The browser redirects back to Yole with the authorization code
10. Yole exchanges the code for access and refresh tokens
11. Optionally set a **Root Folder ID** to limit access to a specific folder
12. Tap **Test Connection** to verify
13. Tap **Save**

### What Happens During OAuth

Yole uses `OneDriveOAuth2Flow` to handle authentication:

1. Yole opens a browser to `https://login.microsoftonline.com/common/oauth2/v2.0/authorize`
2. You authorize the application
3. Microsoft redirects back with an authorization code
4. Yole exchanges the code via `https://login.microsoftonline.com/common/oauth2/v2.0/token`
5. Tokens are stored securely using `AuthTokenManager`
6. Access tokens are refreshed automatically (Microsoft tokens expire after 60-90 minutes)

---

## Step 4: Test the Connection

After saving, tap **Test Connection**. Yole will:

1. Verify the access token is valid (refresh if needed)
2. Call the appropriate drive endpoint based on drive type (e.g., `v1.0/me/drive`)
3. Verify the root folder (if set) is accessible
4. Report success or the specific error encountered

---

## Supported Operations

| Operation | Microsoft Graph Endpoint | Notes |
|-----------|-------------------------|-------|
| Connect | `v1.0/{driveType}/drive` | Validates token, fetches drive info |
| List files | `v1.0/me/drive/root/children` or by folder ID | Supports pagination with `@odata.nextLink` |
| Download | `v1.0/me/drive/items/{id}/content` | Downloads file content with progress |
| Upload | `v1.0/me/drive/items/{parent}:/{name}:/content` | PUT for small files, upload sessions for large files |
| Delete | `v1.0/me/drive/items/{id}` | Moves to recycle bin |
| Create folder | `v1.0/me/drive/items/{parent}/children` | Creates folder with specified name |
| Move | `v1.0/me/drive/items/{id}` (PATCH) | Updates parentReference |
| Copy | `v1.0/me/drive/items/{id}/copy` | Asynchronous server-side copy |
| Get info | `v1.0/me/drive/items/{id}` | Returns full item metadata |
| Search | `v1.0/me/drive/root/search(q='{query}')` | Full-text search across drive |
| Quota | `v1.0/me/drive` | Returns total and used quota |

---

## Troubleshooting

### "AADSTS700016: Application not found" error

- **Cause**: The client ID is incorrect or the app registration was deleted
- **Fix**: Verify the Application (client) ID in the Azure Portal matches what you entered in Yole.

### "AADSTS65001: The user or administrator has not consented"

- **Cause**: Required permissions have not been granted
- **Fix**: Go to the Azure Portal > App Registration > API permissions and click **Grant admin consent**. If you are not an admin, ask your organization's admin to consent.

### "Insufficient privileges" when accessing SharePoint

- **Cause**: Your account does not have access to the specified SharePoint site or document library
- **Fix**: Verify your account has at least **Read** access to the SharePoint site. Contact your SharePoint administrator if needed.

### Client secret expired

- **Cause**: Azure AD client secrets have a maximum lifetime (recommended: 6-24 months)
- **Fix**: Go to Azure Portal > App Registration > Certificates & secrets, create a new secret, and update the secret in Yole's settings.

### "Tenant does not allow this operation" for personal accounts

- **Cause**: The app registration is configured for organizational accounts only
- **Fix**: In Azure Portal > App Registration > Authentication, change **Supported account types** to include personal Microsoft accounts.

### Large file uploads fail

- **Cause**: OneDrive has a 4 MB limit for simple PUT uploads; larger files need upload sessions
- **Fix**: Yole handles this automatically. If uploads still fail, check your network stability. OneDrive upload sessions can resume after interruptions.

---

## Revoking Access

To disconnect Yole from your Microsoft account:

1. In Yole: Go to **Settings** > **Cloud Storage**, select the connection, and tap **Remove**
2. In Microsoft: Go to [Microsoft Account App Permissions](https://account.microsoft.com/privacy/app-access) and remove Yole

---

## Related Documentation

- [Cloud Storage Overview](README.md) -- All providers at a glance
- [Dropbox Setup](dropbox-setup.md) -- Alternative cloud provider
- [Google Drive Setup](google-drive-setup.md) -- Google cloud alternative
- [Troubleshooting](../../TROUBLESHOOTING.md) -- General troubleshooting

---

*Last updated: March 7, 2026*
