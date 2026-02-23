# Module 1: Getting Started (5 videos)

## Video 1.1: Introduction to Yole & Kotlin Multiplatform (10 min)

### Script Outline

**[0:00-1:30] Welcome**
- Welcome to the Yole video course
- What you will learn: building a cross-platform text editor with KMP
- Prerequisites: basic Kotlin knowledge

**[1:30-3:00] What is Yole?**
- Demonstrate Yole on Android (production), Desktop (beta), Web (Wasm)
- Show the same document rendered identically across platforms
- Highlight: 17+ text formats, offline-first, cloud storage optional

**[3:00-5:00] Kotlin Multiplatform**
- Explain expect/actual mechanism with diagram
- Show shared module structure: commonMain, androidMain, desktopMain, iosMain, wasmJsMain
- Compare with other cross-platform approaches (Flutter, React Native)

**[5:00-7:00] Live Demo**
- Open a Markdown file on Android and Desktop side-by-side
- Switch to todo.txt format, show parsing works identically
- Edit on one platform, sync to another via cloud storage

**[7:00-9:00] Project Architecture**
- Walk through the repository structure
- Explain: shared/ (business logic), androidApp/, desktopApp/, iosApp/, webApp/
- Show FormatRegistry, TextParser, TextFormat

**[9:00-10:00] Course Roadmap**
- Beginner: Getting started, Markdown editor, todo.txt manager, note app
- Advanced: Custom formats, performance, network storage, UI
- Expert: Architecture, deployment, testing, community

---

## Video 1.2: Development Environment Setup (12 min)

### Script Outline

**[0:00-1:00] Prerequisites**
- JDK 11+ (recommend JDK 17)
- Android Studio or IntelliJ IDEA
- Git

**[1:00-3:00] Java Installation**
- Install SDKMAN or direct download
- Verify: `java -version`, `javac -version`
- Set JAVA_HOME

**[3:00-5:00] Android Studio**
- Download and install
- Install KMP plugin
- Configure SDK (API 24+)

**[5:00-7:00] Xcode (macOS only)**
- Install from App Store
- Accept license agreement
- Verify command line tools

**[7:00-9:00] Clone and Build**
- `git clone https://github.com/vasic-digital/Yole.git`
- `./gradlew :shared:compileKotlinDesktop` — verify shared module
- `./gradlew :desktopApp:run` — launch desktop app

**[9:00-11:00] Run First Test**
- `./gradlew test --tests "digital.vasic.yole.format.markdown.*"`
- Explain test output
- Show test count (2,427+)

**[11:00-12:00] Troubleshooting**
- Common issues: wrong JDK version, missing Android SDK, Gradle daemon
- `./gradlew --version` for diagnostics

---

## Video 1.3: Your First Cross-Platform App (15 min)

### Script Outline

- Create a minimal KMP project from scratch
- Add a shared `Greeter` class with expect/actual
- Build for Android and Desktop
- Run tests on both targets

---

## Video 1.4: Understanding the Build System (10 min)

### Script Outline

- Walk through `build.gradle.kts` files
- Explain `kotlin { }` multiplatform block
- Show `libs.versions.toml` dependency catalog
- Explain source sets: commonMain, commonTest, platformMain

---

## Video 1.5: Debugging Cross-Platform Code (8 min)

### Script Outline

- Set breakpoints in shared code
- Debug from Android Studio (Android target)
- Debug from IntelliJ (Desktop target)
- Use `println` and Napier logging library
