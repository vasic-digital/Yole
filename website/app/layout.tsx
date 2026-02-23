// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Yole — Cross-Platform Text Editor",
  description:
    "A powerful cross-platform text editor supporting 17+ text formats. Offline-first with optional cloud storage. Available on Android, Desktop, iOS, and Web.",
  keywords: [
    "text editor",
    "markdown",
    "todo.txt",
    "cross-platform",
    "kotlin multiplatform",
    "android",
    "desktop",
    "offline-first",
  ],
  icons: {
    icon: "/favicon.svg",
  },
};

const navLinks = [
  { href: "/", label: "Home" },
  { href: "/docs", label: "Documentation" },
  { href: "/download", label: "Download" },
  { href: "/about", label: "About" },
];

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="min-h-screen flex flex-col">
        {/* Header */}
        <header className="sticky top-0 z-50 border-b border-[var(--color-border)] bg-[var(--color-bg)]/95 backdrop-blur supports-[backdrop-filter]:bg-[var(--color-bg)]/80">
          <div className="container-page flex items-center justify-between h-16">
            <a href="/" className="flex items-center gap-2 font-bold text-xl">
              <span className="text-primary-500">Yole</span>
            </a>

            <nav className="hidden md:flex items-center gap-6">
              {navLinks.map((link) => (
                <a
                  key={link.href}
                  href={link.href}
                  className="text-sm font-medium text-[var(--color-text-secondary)] hover:text-primary-500 transition-colors"
                >
                  {link.label}
                </a>
              ))}
              <a
                href="https://github.com/vasic-digital/Yole"
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm font-medium text-[var(--color-text-secondary)] hover:text-primary-500 transition-colors"
              >
                GitHub
              </a>
            </nav>
          </div>
        </header>

        {/* Main content */}
        <main className="flex-1">{children}</main>

        {/* Footer */}
        <footer className="border-t border-[var(--color-border)] bg-[var(--color-bg-secondary)]">
          <div className="container-page py-12">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
              <div>
                <h3 className="font-bold text-lg mb-3">Yole</h3>
                <p className="text-sm text-[var(--color-text-secondary)]">
                  Cross-platform text editor supporting 17+ formats.
                  Offline-first with optional cloud storage.
                </p>
              </div>
              <div>
                <h4 className="font-semibold mb-3">Product</h4>
                <ul className="space-y-2 text-sm text-[var(--color-text-secondary)]">
                  <li><a href="/download" className="hover:text-primary-500">Download</a></li>
                  <li><a href="/docs" className="hover:text-primary-500">Documentation</a></li>
                  <li><a href="/about" className="hover:text-primary-500">About</a></li>
                </ul>
              </div>
              <div>
                <h4 className="font-semibold mb-3">Formats</h4>
                <ul className="space-y-2 text-sm text-[var(--color-text-secondary)]">
                  <li><a href="/docs#markdown" className="hover:text-primary-500">Markdown</a></li>
                  <li><a href="/docs#todotxt" className="hover:text-primary-500">todo.txt</a></li>
                  <li><a href="/docs#csv" className="hover:text-primary-500">CSV</a></li>
                  <li><a href="/docs#latex" className="hover:text-primary-500">LaTeX</a></li>
                </ul>
              </div>
              <div>
                <h4 className="font-semibold mb-3">Community</h4>
                <ul className="space-y-2 text-sm text-[var(--color-text-secondary)]">
                  <li>
                    <a href="https://github.com/vasic-digital/Yole" className="hover:text-primary-500" target="_blank" rel="noopener noreferrer">
                      GitHub
                    </a>
                  </li>
                  <li>
                    <a href="https://github.com/vasic-digital/Yole/issues" className="hover:text-primary-500" target="_blank" rel="noopener noreferrer">
                      Issues
                    </a>
                  </li>
                  <li>
                    <a href="https://github.com/vasic-digital/Yole/blob/master/LICENSE.txt" className="hover:text-primary-500" target="_blank" rel="noopener noreferrer">
                      License
                    </a>
                  </li>
                </ul>
              </div>
            </div>
            <div className="mt-8 pt-8 border-t border-[var(--color-border)] text-center text-sm text-[var(--color-text-secondary)]">
              <p>Copyright 2025–2026 Milos Vasic. Licensed under Apache-2.0.</p>
            </div>
          </div>
        </footer>
      </body>
    </html>
  );
}
