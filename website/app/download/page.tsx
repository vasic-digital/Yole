// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const downloads = [
  {
    platform: "Android",
    status: "production" as const,
    description: "Full-featured editor available on Google Play and F-Droid. Supports all 17 text formats with Material Design UI. All 8 cloud protocols with circuit breaker resilience.",
    requirements: "Android 7.0 (API 24) or later",
    links: [
      { label: "Google Play", href: "#", note: "Coming soon", comingSoon: true },
      { label: "F-Droid", href: "#", note: "Coming soon", comingSoon: true },
      { label: "APK Download", href: "https://github.com/user/Yole/releases/latest", note: "Direct install", comingSoon: false },
    ],
  },
  {
    platform: "Desktop",
    status: "beta" as const,
    description: "Cross-platform desktop editor for Windows, macOS, and Linux. Built with Compose for Desktop (JVM).",
    requirements: "Java 11+ runtime, Windows 10 / macOS 12 / Linux with X11 or Wayland",
    links: [
      { label: "Windows (.msi)", href: "https://github.com/user/Yole/releases/latest", note: "", comingSoon: false },
      { label: "macOS (.dmg)", href: "https://github.com/user/Yole/releases/latest", note: "", comingSoon: false },
      { label: "Linux (.deb)", href: "https://github.com/user/Yole/releases/latest", note: "", comingSoon: false },
      { label: "JAR (Universal)", href: "https://github.com/user/Yole/releases/latest", note: "Requires Java 11+", comingSoon: false },
    ],
  },
  {
    platform: "iOS",
    status: "development" as const,
    description: "Native iOS editor built with Compose Multiplatform for iPhone and iPad. Currently in development.",
    requirements: "iOS 16 or later",
    links: [
      { label: "App Store", href: "#", note: "Coming soon", comingSoon: true },
      { label: "TestFlight", href: "#", note: "Coming soon", comingSoon: true },
    ],
  },
  {
    platform: "Web",
    status: "development" as const,
    description: "Browser-based editor powered by Kotlin/Wasm. Works offline as a Progressive Web App.",
    requirements: "Modern browser with WebAssembly support (Chrome 119+, Firefox 120+, Safari 17.4+)",
    links: [
      { label: "Launch Web App", href: "#", note: "Coming soon", comingSoon: true },
    ],
  },
];

const statusColors: Record<string, string> = {
  production: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  beta: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
  development: "bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400",
};

const statusLabels: Record<string, string> = {
  production: "Production",
  beta: "Beta",
  development: "In Development",
};

export default function DownloadPage() {
  return (
    <div className="container-page py-16">
      <h1 className="text-4xl font-bold tracking-tight mb-4">Download Yole</h1>
      <p className="text-lg text-[var(--color-text-secondary)] mb-12 max-w-3xl">
        Yole runs on Android, Desktop, iOS, and Web. Choose your platform below.
      </p>

      <div className="space-y-8">
        {downloads.map((dl) => (
          <section key={dl.platform} className="card">
            <div className="flex items-center gap-3 mb-4">
              <h2 className="text-2xl font-bold">{dl.platform}</h2>
              <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${statusColors[dl.status]}`}>
                {statusLabels[dl.status]}
              </span>
            </div>
            <p className="text-[var(--color-text-secondary)] mb-2">{dl.description}</p>
            <p className="text-sm text-[var(--color-text-secondary)] mb-6">
              <span className="font-medium">Requirements:</span> {dl.requirements}
            </p>
            <div className="flex flex-wrap gap-3">
              {dl.links.map((link) => (
                link.comingSoon ? (
                  <span
                    key={link.label}
                    className="inline-flex items-center px-4 py-2 rounded-lg text-sm font-medium bg-gray-100 text-gray-400 dark:bg-gray-800 dark:text-gray-500 cursor-not-allowed select-none border border-gray-200 dark:border-gray-700"
                  >
                    {link.label}
                    <span className="ml-2 text-xs opacity-75">(Coming soon)</span>
                  </span>
                ) : (
                  <a
                    key={link.label}
                    href={link.href}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={dl.status === "production" ? "btn-primary" : "btn-secondary"}
                  >
                    {link.label}
                    {link.note && (
                      <span className="ml-2 text-xs opacity-75">({link.note})</span>
                    )}
                  </a>
                )
              ))}
            </div>
          </section>
        ))}
      </div>

      <section className="mt-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Build from Source</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] mb-4">
            Yole is open source under the Apache-2.0 license. You can build it from source
            for any supported platform.
          </p>
          <pre className="text-sm bg-[var(--color-bg-secondary)] rounded-lg p-4 overflow-x-auto border border-[var(--color-border)]"><code>{`# Clone the repository
git clone https://github.com/vasic-digital/Yole.git
cd Yole

# Build for Android
./gradlew :androidApp:assembleDebug

# Build and run Desktop
./gradlew :desktopApp:run

# Build Web (Wasm)
./gradlew :webApp:wasmJsBrowserRun

# Run all tests
./gradlew test`}</code></pre>
        </div>
      </section>
    </div>
  );
}
