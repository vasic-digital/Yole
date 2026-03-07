// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const protocols = [
  {
    id: "dropbox",
    name: "Dropbox",
    icon: "D",
    color: "bg-blue-500/10 text-blue-600 dark:text-blue-400",
    description: "Full integration with Dropbox cloud storage. Browse, read, write, and sync files directly from your Dropbox account using OAuth2 authentication.",
    features: ["OAuth2 authentication", "File browsing and search", "Read and write operations", "File metadata access", "Automatic sync", "Conflict resolution"],
    auth: "OAuth2",
    encryption: "TLS 1.2+",
    platforms: { android: true, desktop: true, ios: false, web: false },
  },
  {
    id: "googledrive",
    name: "Google Drive",
    icon: "G",
    color: "bg-green-500/10 text-green-600 dark:text-green-400",
    description: "Access files stored in Google Drive. Supports browsing folders, reading and writing text files, and real-time synchronization.",
    features: ["OAuth2 authentication", "Folder browsing", "File read/write", "Shared drives support", "Export to text formats", "Revision history"],
    auth: "OAuth2",
    encryption: "TLS 1.2+",
    platforms: { android: true, desktop: true, ios: false, web: false },
  },
  {
    id: "onedrive",
    name: "OneDrive",
    icon: "O",
    color: "bg-sky-500/10 text-sky-600 dark:text-sky-400",
    description: "Connect to Microsoft OneDrive and OneDrive for Business. Browse, edit, and sync text files across your Microsoft ecosystem.",
    features: ["OAuth2 authentication", "Personal and Business accounts", "File browsing", "Read and write operations", "SharePoint integration", "Metadata access"],
    auth: "OAuth2",
    encryption: "TLS 1.2+",
    platforms: { android: true, desktop: true, ios: false, web: false },
  },
  {
    id: "webdav",
    name: "WebDAV",
    icon: "W",
    color: "bg-purple-500/10 text-purple-600 dark:text-purple-400",
    description: "Connect to any WebDAV-compatible server including Nextcloud, ownCloud, and other self-hosted solutions. Full CRUD operations with directory listing.",
    features: ["Basic and Digest authentication", "Directory listing (PROPFIND)", "File upload and download", "File creation and deletion", "Custom server URLs", "Nextcloud/ownCloud compatible"],
    auth: "Basic / Digest",
    encryption: "TLS (configurable)",
    platforms: { android: true, desktop: true, ios: false, web: false },
  },
  {
    id: "ftp",
    name: "FTP",
    icon: "F",
    color: "bg-amber-500/10 text-amber-600 dark:text-amber-400",
    description: "Connect to FTP servers for file browsing, reading, and writing. Supports both active and passive mode connections.",
    features: ["Username/password authentication", "Directory listing", "File upload and download", "Active and passive modes", "Binary and ASCII transfers", "Custom port configuration"],
    auth: "Username / Password",
    encryption: "None (use SFTP for encrypted)",
    platforms: { android: true, desktop: true, ios: false, web: false },
  },
  {
    id: "sftp",
    name: "SFTP",
    icon: "S",
    color: "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400",
    description: "Secure file transfer over SSH. All data is encrypted in transit. Supports key-based and password authentication.",
    features: ["SSH key authentication", "Password authentication", "Encrypted file transfer", "Directory browsing", "File permissions", "Custom port and host"],
    auth: "SSH Key / Password",
    encryption: "SSH (AES-256)",
    platforms: { android: true, desktop: true, ios: false, web: false },
  },
  {
    id: "git",
    name: "Git",
    icon: "G",
    color: "bg-orange-500/10 text-orange-600 dark:text-orange-400",
    description: "Clone and sync Git repositories. Access text files from GitHub, GitLab, Bitbucket, or any Git hosting provider with version history.",
    features: ["HTTPS and SSH protocols", "Repository cloning", "File browsing", "Commit history", "Branch selection", "Pull and push operations"],
    auth: "HTTPS / SSH",
    encryption: "TLS / SSH",
    platforms: { android: true, desktop: true, ios: false, web: false },
  },
  {
    id: "s3",
    name: "S3-Compatible",
    icon: "3",
    color: "bg-rose-500/10 text-rose-600 dark:text-rose-400",
    description: "Connect to Amazon S3 or any S3-compatible storage (MinIO, DigitalOcean Spaces, Backblaze B2). Access text files stored in cloud object storage.",
    features: ["Access key authentication", "Bucket browsing", "Object read/write", "Custom endpoints", "Region selection", "S3-compatible providers"],
    auth: "Access Key / Secret Key",
    encryption: "TLS 1.2+",
    platforms: { android: false, desktop: false, ios: false, web: false },
  },
];

const featureComparison = [
  { feature: "Browse files", dropbox: true, googledrive: true, onedrive: true, webdav: true, ftp: true, sftp: true, git: true, s3: true },
  { feature: "Read files", dropbox: true, googledrive: true, onedrive: true, webdav: true, ftp: true, sftp: true, git: true, s3: true },
  { feature: "Write files", dropbox: true, googledrive: true, onedrive: true, webdav: true, ftp: true, sftp: true, git: true, s3: true },
  { feature: "Delete files", dropbox: true, googledrive: true, onedrive: true, webdav: true, ftp: true, sftp: true, git: false, s3: true },
  { feature: "Create directories", dropbox: true, googledrive: true, onedrive: true, webdav: true, ftp: true, sftp: true, git: false, s3: true },
  { feature: "OAuth2 authentication", dropbox: true, googledrive: true, onedrive: true, webdav: false, ftp: false, sftp: false, git: false, s3: false },
  { feature: "SSH key authentication", dropbox: false, googledrive: false, onedrive: false, webdav: false, ftp: false, sftp: true, git: true, s3: false },
  { feature: "Encrypted transfer", dropbox: true, googledrive: true, onedrive: true, webdav: true, ftp: false, sftp: true, git: true, s3: true },
  { feature: "Self-hosted option", dropbox: false, googledrive: false, onedrive: false, webdav: true, ftp: true, sftp: true, git: true, s3: true },
  { feature: "Version history", dropbox: true, googledrive: true, onedrive: true, webdav: false, ftp: false, sftp: false, git: true, s3: false },
  { feature: "Rate limiting", dropbox: true, googledrive: true, onedrive: true, webdav: false, ftp: false, sftp: false, git: false, s3: true },
];

const protocolKeys = ["dropbox", "googledrive", "onedrive", "webdav", "ftp", "sftp", "git", "s3"] as const;
const protocolLabels: Record<string, string> = {
  dropbox: "Dropbox",
  googledrive: "Google Drive",
  onedrive: "OneDrive",
  webdav: "WebDAV",
  ftp: "FTP",
  sftp: "SFTP",
  git: "Git",
  s3: "S3",
};

export default function CloudStoragePage() {
  return (
    <div className="container-page py-16">
      <h1 className="text-4xl font-bold tracking-tight mb-4">Cloud Storage</h1>
      <p className="text-lg text-[var(--color-text-secondary)] mb-4 max-w-3xl">
        Yole is offline-first but integrates with 8 cloud storage protocols for seamless
        file access across devices. Your files stay on your device by default -- cloud sync
        is entirely optional.
      </p>
      <p className="text-[var(--color-text-secondary)] mb-12 max-w-3xl">
        All protocols support browsing, reading, and writing text files in any of
        Yole&apos;s 17 supported formats.
      </p>

      {/* Protocol Cards */}
      <section className="mb-16">
        <h2 className="section-heading">Supported Protocols</h2>
        <p className="section-subheading">
          From major cloud providers to self-hosted solutions.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {protocols.map((protocol) => (
            <div key={protocol.id} id={protocol.id} className="card scroll-mt-24">
              <div className="flex items-center gap-3 mb-4">
                <div className={`flex-shrink-0 w-10 h-10 rounded-lg ${protocol.color} flex items-center justify-center font-bold text-lg`}>
                  {protocol.icon}
                </div>
                <div>
                  <h3 className="text-xl font-bold">{protocol.name}</h3>
                  <div className="flex items-center gap-2 mt-0.5">
                    <span className="text-xs px-2 py-0.5 rounded-full bg-[var(--color-bg-secondary)] border border-[var(--color-border)] text-[var(--color-text-secondary)]">
                      {protocol.auth}
                    </span>
                    <span className="text-xs px-2 py-0.5 rounded-full bg-[var(--color-bg-secondary)] border border-[var(--color-border)] text-[var(--color-text-secondary)]">
                      {protocol.encryption}
                    </span>
                  </div>
                </div>
              </div>
              <p className="text-sm text-[var(--color-text-secondary)] mb-4">{protocol.description}</p>
              <div className="grid grid-cols-2 gap-2">
                {protocol.features.map((feature) => (
                  <div key={feature} className="text-xs text-[var(--color-text-secondary)] flex items-start gap-1.5">
                    <span className="text-primary-500 mt-0.5 flex-shrink-0">-</span>
                    {feature}
                  </div>
                ))}
              </div>
              <div className="flex items-center gap-2 mt-4 pt-4 border-t border-[var(--color-border)]">
                <span className="text-xs text-[var(--color-text-secondary)]">Available on:</span>
                {(["android", "desktop", "ios", "web"] as const).map((platform) => (
                  <span
                    key={platform}
                    className={`text-xs px-2 py-0.5 rounded-full ${
                      protocol.platforms[platform]
                        ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                        : "bg-gray-100 text-gray-400 dark:bg-gray-800 dark:text-gray-500"
                    }`}
                  >
                    {platform.charAt(0).toUpperCase() + platform.slice(1)}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Feature Comparison Table */}
      <section className="mb-16">
        <h2 className="section-heading">Feature Comparison</h2>
        <p className="section-subheading">
          See which features are available across each protocol.
        </p>
        <div className="card overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--color-border)]">
                <th className="text-left py-3 pr-4 font-semibold">Feature</th>
                {protocolKeys.map((key) => (
                  <th key={key} className="text-center py-3 px-2 font-semibold whitespace-nowrap">
                    {protocolLabels[key]}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {featureComparison.map((row) => (
                <tr key={row.feature} className="border-b border-[var(--color-border)] last:border-0">
                  <td className="py-2.5 pr-4 text-[var(--color-text-secondary)]">{row.feature}</td>
                  {protocolKeys.map((key) => (
                    <td key={key} className="text-center py-2.5 px-2">
                      {row[key] ? (
                        <span className="text-green-600 dark:text-green-400 font-bold">Y</span>
                      ) : (
                        <span className="text-gray-300 dark:text-gray-600">-</span>
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Platform Availability Matrix */}
      <section className="mb-16">
        <h2 className="section-heading">Platform Availability</h2>
        <p className="section-subheading">
          Protocol support across all Yole platforms.
        </p>
        <div className="card overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-[var(--color-border)]">
                <th className="text-left py-3 pr-4 font-semibold">Protocol</th>
                <th className="text-center py-3 px-4 font-semibold">Android</th>
                <th className="text-center py-3 px-4 font-semibold">Desktop</th>
                <th className="text-center py-3 px-4 font-semibold">iOS</th>
                <th className="text-center py-3 px-4 font-semibold">Web</th>
              </tr>
            </thead>
            <tbody>
              {protocols.map((protocol) => (
                <tr key={protocol.id} className="border-b border-[var(--color-border)] last:border-0">
                  <td className="py-2.5 pr-4 font-medium">{protocol.name}</td>
                  {(["android", "desktop", "ios", "web"] as const).map((platform) => (
                    <td key={platform} className="text-center py-2.5 px-4">
                      {protocol.platforms[platform] ? (
                        <span className="text-green-600 dark:text-green-400 font-bold">Available</span>
                      ) : (
                        <span className="text-gray-400 dark:text-gray-500">Planned</span>
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {/* Setup Guide */}
      <section className="mb-16">
        <h2 className="section-heading">Getting Started</h2>
        <p className="section-subheading">
          Setting up cloud storage in Yole takes just a few steps.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="card text-center">
            <div className="w-12 h-12 rounded-full bg-primary-500/10 text-primary-500 flex items-center justify-center mx-auto mb-4 font-bold text-lg">
              1
            </div>
            <h3 className="font-semibold text-lg mb-2">Choose a Protocol</h3>
            <p className="text-sm text-[var(--color-text-secondary)]">
              Open Yole settings and navigate to Cloud Storage. Select the protocol
              that matches your cloud provider or server.
            </p>
          </div>
          <div className="card text-center">
            <div className="w-12 h-12 rounded-full bg-primary-500/10 text-primary-500 flex items-center justify-center mx-auto mb-4 font-bold text-lg">
              2
            </div>
            <h3 className="font-semibold text-lg mb-2">Authenticate</h3>
            <p className="text-sm text-[var(--color-text-secondary)]">
              Sign in with OAuth2 for cloud providers, or enter your credentials
              for self-hosted servers (FTP, SFTP, WebDAV).
            </p>
          </div>
          <div className="card text-center">
            <div className="w-12 h-12 rounded-full bg-primary-500/10 text-primary-500 flex items-center justify-center mx-auto mb-4 font-bold text-lg">
              3
            </div>
            <h3 className="font-semibold text-lg mb-2">Browse and Edit</h3>
            <p className="text-sm text-[var(--color-text-secondary)]">
              Browse your remote files, open them in any of Yole&apos;s 17 formats,
              edit offline, and sync changes when ready.
            </p>
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="text-center">
        <h2 className="section-heading">Offline First</h2>
        <p className="text-lg text-[var(--color-text-secondary)] max-w-2xl mx-auto mb-8">
          Cloud storage is always optional. Yole works fully offline and stores all data
          locally on your device. Cloud sync is there when you need it.
        </p>
        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <a href="/download" className="btn-primary">
            Download Yole
          </a>
          <a href="/docs" className="btn-secondary">
            Read the Docs
          </a>
        </div>
      </section>
    </div>
  );
}
