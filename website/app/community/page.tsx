// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

const contributionTypes = [
  {
    title: "Code Contributions",
    description: "Fix bugs, add features, improve performance, or contribute new format parsers. All code contributions are welcome.",
    steps: [
      "Fork the repository on GitHub",
      "Create a feature or bugfix branch",
      "Write clean, tested code following Kotlin coding conventions",
      "Submit a pull request with a clear description",
      "Respond to code review feedback",
    ],
  },
  {
    title: "Bug Reports",
    description: "Found a bug? Help us fix it by filing a detailed report with reproduction steps.",
    steps: [
      "Search existing issues to avoid duplicates",
      "Use the bug report template on GitHub",
      "Include device info, OS version, and steps to reproduce",
      "Provide screenshots or logs if applicable",
      "Be responsive to follow-up questions",
    ],
  },
  {
    title: "Feature Requests",
    description: "Have an idea for a new feature or improvement? Open a discussion or feature request.",
    steps: [
      "Search existing discussions and issues first",
      "Use the feature request template",
      "Describe the use case and expected behavior",
      "Consider if it fits the project scope",
      "Engage with community feedback",
    ],
  },
  {
    title: "Documentation",
    description: "Improve existing documentation, write tutorials, add examples, or fix typos and unclear sections.",
    steps: [
      "All public APIs should have KDoc documentation",
      "Follow conventional commit format for docs changes",
      "Add usage examples for complex APIs",
      "Update README and guides when features change",
      "Review documentation for accuracy",
    ],
  },
  {
    title: "Translations",
    description: "Help make Yole available in more languages by contributing translations via Crowdin.",
    steps: [
      "Join the Yole project on Crowdin",
      "Translate strings to your language",
      "Review existing translations for quality",
      "Report translation issues on GitHub",
      "Help maintain language consistency",
    ],
  },
  {
    title: "Testing",
    description: "Run tests, report test failures, write new tests, or help improve test coverage.",
    steps: [
      "Run the full test suite with ./gradlew test",
      "Write unit tests for new features",
      "Test on different platforms and devices",
      "Report flaky or failing tests",
      "Contribute stress and edge case tests",
    ],
  },
];

const resources = [
  {
    title: "GitHub Repository",
    description: "Source code, issues, pull requests, and releases.",
    href: "https://github.com/vasic-digital/Yole",
    label: "View Repository",
  },
  {
    title: "GitHub Issues",
    description: "Report bugs, request features, and track project progress.",
    href: "https://github.com/vasic-digital/Yole/issues",
    label: "Browse Issues",
  },
  {
    title: "GitHub Discussions",
    description: "Ask questions, share ideas, and connect with other users and developers.",
    href: "https://github.com/vasic-digital/Yole/discussions",
    label: "Join Discussions",
  },
  {
    title: "Crowdin Translations",
    description: "Help translate Yole into your language. Contribute translations via Crowdin.",
    href: "https://crowdin.com/project/markor",
    label: "Translate on Crowdin",
  },
  {
    title: "F-Droid",
    description: "Install Yole from the F-Droid open source app store.",
    href: "https://f-droid.org/repository/browse/?fdid=digital.vasic.yole",
    label: "View on F-Droid",
  },
  {
    title: "Architecture Guide",
    description: "Understand the technical architecture and module structure of Yole.",
    href: "/architecture",
    label: "Read Architecture Guide",
  },
];

const codeOfConduct = [
  "Be respectful and constructive in all interactions",
  "Welcome newcomers and help them learn the codebase",
  "Focus on what is best for the project and community",
  "Show empathy towards other community members",
  "Provide specific, actionable feedback in code reviews",
  "Acknowledge good work and improvements",
  "Be patient with different skill levels and perspectives",
  "Keep discussions focused and professional",
];

export default function CommunityPage() {
  return (
    <div className="container-page py-16">
      <h1 className="text-4xl font-bold tracking-tight mb-4">Community</h1>
      <p className="text-lg text-[var(--color-text-secondary)] mb-12 max-w-3xl">
        Yole is an open source project built by the community. Whether you are fixing
        bugs, writing documentation, translating the app, or sharing ideas, every
        contribution matters.
      </p>

      {/* Quick Links */}
      <section className="mb-16">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <a
            href="https://github.com/vasic-digital/Yole"
            target="_blank"
            rel="noopener noreferrer"
            className="card text-center group"
          >
            <div className="text-2xl font-bold text-primary-500 mb-1 group-hover:scale-110 transition-transform">
              GH
            </div>
            <div className="text-sm font-semibold">GitHub</div>
          </a>
          <a
            href="https://github.com/vasic-digital/Yole/issues"
            target="_blank"
            rel="noopener noreferrer"
            className="card text-center group"
          >
            <div className="text-2xl font-bold text-primary-500 mb-1 group-hover:scale-110 transition-transform">
              IS
            </div>
            <div className="text-sm font-semibold">Issues</div>
          </a>
          <a
            href="https://github.com/vasic-digital/Yole/discussions"
            target="_blank"
            rel="noopener noreferrer"
            className="card text-center group"
          >
            <div className="text-2xl font-bold text-primary-500 mb-1 group-hover:scale-110 transition-transform">
              DI
            </div>
            <div className="text-sm font-semibold">Discussions</div>
          </a>
          <a
            href="https://crowdin.com/project/markor"
            target="_blank"
            rel="noopener noreferrer"
            className="card text-center group"
          >
            <div className="text-2xl font-bold text-primary-500 mb-1 group-hover:scale-110 transition-transform">
              TR
            </div>
            <div className="text-sm font-semibold">Translations</div>
          </a>
        </div>
      </section>

      {/* Contributing Guidelines */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-6">How to Contribute</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {contributionTypes.map((type) => (
            <div key={type.title} className="card">
              <h3 className="font-semibold text-lg mb-2">{type.title}</h3>
              <p className="text-sm text-[var(--color-text-secondary)] mb-4">{type.description}</p>
              <ol className="space-y-1.5">
                {type.steps.map((step, i) => (
                  <li key={i} className="text-xs text-[var(--color-text-secondary)] flex items-start gap-2">
                    <span className="text-primary-500 font-bold flex-shrink-0">{i + 1}.</span>
                    {step}
                  </li>
                ))}
              </ol>
            </div>
          ))}
        </div>
      </section>

      {/* Development Setup */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Development Setup</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] mb-4">
            Get started with Yole development in a few steps. All builds and tests should
            be run inside Docker/Podman containers for consistency.
          </p>
          <pre className="text-sm bg-[var(--color-bg-secondary)] rounded-lg p-4 overflow-x-auto border border-[var(--color-border)]"><code>{`# Clone the repository
git clone https://github.com/vasic-digital/Yole.git
cd Yole

# Initialize submodules
git submodule update --init --recursive

# Build container
docker compose build build

# Run all tests in container
docker compose run --rm build ./docker/scripts/test-all.sh

# Or build and run directly with Gradle
./gradlew :desktopApp:run         # Desktop
./gradlew :androidApp:assembleDebug  # Android
./gradlew :webApp:wasmJsBrowserRun   # Web

# Run tests
./gradlew test

# Tests with coverage report
./gradlew test koverHtmlReport`}</code></pre>
        </div>
      </section>

      {/* Commit Convention */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Commit Convention</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] mb-4">
            Yole follows the conventional commit format. Use structured commit messages
            with type, scope, and description.
          </p>
          <pre className="text-sm bg-[var(--color-bg-secondary)] rounded-lg p-4 overflow-x-auto border border-[var(--color-border)]"><code>{`feat(markdown): add support for footnotes
fix(todotxt): correct priority parsing for tasks without dates
docs(api): add KDoc to FormatRegistry methods
test(csv): add edge case tests for quoted fields
refactor(network): extract protocol abstraction
chore(deps): update Kotlin to 2.0.20`}</code></pre>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-4">
            {[
              { type: "feat", label: "New feature" },
              { type: "fix", label: "Bug fix" },
              { type: "docs", label: "Documentation" },
              { type: "test", label: "Tests" },
              { type: "refactor", label: "Refactoring" },
              { type: "style", label: "Formatting" },
              { type: "chore", label: "Build/deps" },
              { type: "perf", label: "Performance" },
            ].map((item) => (
              <div key={item.type} className="text-sm">
                <code className="text-primary-500 font-bold">{item.type}</code>
                <span className="text-[var(--color-text-secondary)] ml-2">{item.label}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Code of Conduct */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-4">Code of Conduct</h2>
        <div className="card">
          <p className="text-[var(--color-text-secondary)] mb-4">
            We are committed to providing a welcoming and inclusive community. All
            participants are expected to follow these guidelines. See our full{" "}
            <a
              href="https://github.com/vasic-digital/Yole/blob/master/CODE_OF_CONDUCT.md"
              target="_blank"
              rel="noopener noreferrer"
              className="text-primary-500 hover:text-primary-600"
            >
              Code of Conduct
            </a>{" "}
            for details.
          </p>
          <ul className="space-y-2">
            {codeOfConduct.map((item) => (
              <li
                key={item}
                className="text-sm text-[var(--color-text-secondary)] flex items-start gap-2"
              >
                <span className="text-primary-500 mt-0.5 flex-shrink-0">-</span>
                {item}
              </li>
            ))}
          </ul>
        </div>
      </section>

      {/* Resources */}
      <section className="mb-16">
        <h2 className="text-3xl font-bold tracking-tight mb-6">Resources</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {resources.map((resource) => (
            <a
              key={resource.title}
              href={resource.href}
              target={resource.href.startsWith("http") ? "_blank" : undefined}
              rel={resource.href.startsWith("http") ? "noopener noreferrer" : undefined}
              className="card group"
            >
              <h3 className="font-semibold text-lg mb-1 group-hover:text-primary-500 transition-colors">
                {resource.title}
              </h3>
              <p className="text-sm text-[var(--color-text-secondary)] mb-3">{resource.description}</p>
              <span className="text-sm font-medium text-primary-500">
                {resource.label} &rarr;
              </span>
            </a>
          ))}
        </div>
      </section>

      {/* License */}
      <section className="text-center">
        <h2 className="section-heading">License</h2>
        <p className="text-lg text-[var(--color-text-secondary)] max-w-2xl mx-auto mb-8">
          Yole is licensed under Apache-2.0 for code and CC0-1.0 for translations and samples.
          By contributing, you agree that your contributions will be licensed under the same terms.
        </p>
        <a
          href="https://github.com/vasic-digital/Yole/blob/master/LICENSE.txt"
          target="_blank"
          rel="noopener noreferrer"
          className="btn-primary"
        >
          View License
        </a>
      </section>
    </div>
  );
}
