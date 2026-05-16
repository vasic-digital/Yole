/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Kotlin Multiplatform Shared Module
 * Contains platform-agnostic code shared across all platforms
 *
 *########################################################*/

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlinx.benchmark") version "0.4.11"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.1.0"
    id("org.jetbrains.dokka") version "2.0.0"
    id("org.jetbrains.kotlinx.kover") version "0.8.3"
    id("io.gitlab.arturbosch.detekt") version "1.23.7"
}

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    // JVM Desktop target
    jvm("desktop") {
        val mainCompilation = compilations.getByName("main")

        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }

        // Add benchmark compilation
        compilations.create("benchmark") {
            associateWith(mainCompilation)
        }
    }

    // iOS targets - Re-enabled for production
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Web target (Wasm)
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "yole-shared"
        browser {
            commonWebpackConfig {
                outputFileName = "yole-shared.js"
            }
        }
    }

    sourceSets {
        // Common code for all platforms
        val commonMain by getting {
            dependencies {
                // Extracted KMP modules (composite builds)
                implementation("digital.vasic.ratelimiter:RateLimiter-KMP")
                implementation("digital.vasic.concurrency:Concurrency-KMP")
                implementation("digital.vasic.uicomponents:UI-Components-KMP")
                implementation("digital.vasic.auth:Auth-KMP")
                implementation("digital.vasic.security:Security-KMP")
                implementation("digital.vasic.document:Document-KMP")
                implementation("digital.vasic.config:Config-KMP")
                implementation("digital.vasic.database:Database-KMP")
                implementation("digital.vasic.storage:Storage-KMP")
                implementation("digital.vasic.formatters:Formatters-KMP")

                // Kotlin Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Kotlinx Serialization
                implementation(libs.kotlinx.serialization.json)

                // DateTime
                implementation(libs.kotlinx.datetime)

                // Okio for file system
                implementation(libs.okio)

                 // Ktor Client for network operations
                 implementation(libs.ktor.client.core)
                 implementation(libs.ktor.client.content.negotiation)
                 implementation(libs.ktor.serialization.kotlinx.json)

                 // SQLite database for metadata - platform specific due to WASM incompatibility
                 // implementation("app.cash.sqldelight:runtime:2.0.2")
                 // implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")

                // Compose runtime (if using Compose Multiplatform)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotest.assertions.core)
                implementation(libs.ktor.client.mock)
                // MockK is JVM-only, not compatible with WASM
                // implementation(libs.mockk)
                // kotlinx-coroutines-test doesn't have WASM variant
                // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        // Android-specific code
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
                implementation("app.cash.sqldelight:android-driver:2.0.2")
                // Room dependencies for Android database implementation
                implementation("androidx.room:room-runtime:2.6.1")
                implementation("androidx.room:room-ktx:2.6.1")
                // SQLite JDBC for desktop
                implementation("org.xerial:sqlite-jdbc:3.44.1.0")
                implementation(libs.ktor.client.okhttp)
                // CIO is JVM-only, so we use OkHttp for Android
                implementation(libs.ktor.client.cio)
                implementation(libs.sshj)   // SFTP via SSH
                implementation(libs.smbj)   // SMB/CIFS
                implementation(libs.lsp4j)  // iter-61 Phase 4: Eclipse LSP4J JSON-RPC
                implementation(libs.flexmark.core)  // iter-62 Phase 4: HoverMarkdownRenderer Flexmark walker

                // Tree-Sitter (iter-57 Phase 5 — Android NDK fix landed
                // post-Phase 13, ticket #android-tree-sitter-ndk-so-missing):
                //
                // The upstream bonede JAR ships native binaries only for 5
                // desktop OS+arch combos (x86_64/aarch64 linux-gnu/macos +
                // x86_64-windows) — no Android NDK binaries. To make
                // syntax highlighting work on Android devices, the
                // `repackageBonedeJarsForAndroid` Gradle task (defined below)
                // resolves the bonede JARs, replaces their `lib/aarch64-linux-gnu-*.so`
                // and `lib/x86_64-linux-gnu-*.so` resources with our own
                // Android-NDK-built shared libraries (sourced from
                // `shared/native/android-tree-sitter/<abi>/`, committed to the
                // repo), and produces patched JARs under
                // `build/repackaged-libs/`. The Android source set depends on
                // those repackaged JARs instead of the upstream Maven
                // coordinates. Bonede's NativeUtils.loadLib then transparently
                // extracts our Android-compatible bytes at runtime.
                //
                // Architecture support:
                //   arm64-v8a (aarch64) — primary target, modern Android
                //   x86_64                — Android emulator + Chrome OS
                //   armeabi-v7a (armv7l) — NOT supported by bonede's
                //                         NativeUtils OS-arch detection
                //                         (it only knows amd64/x86_64/aarch64).
                //                         32-bit ARM dropped — Android 13+
                //                         and Play Store require 64-bit
                //                         per Google policy. Files retained
                //                         in shared/native/android-tree-sitter/
                //                         for future custom-loader use.
                // Wired below — declared via dependencies { ... } after the
                // repackage tasks are registered so AGP sees the explicit
                // task-output relationship without an implicit-dep warning.
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }

        // Desktop-specific code
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                implementation("app.cash.sqldelight:sqlite-driver:2.0.2")
                // SQLite JDBC for desktop
                implementation("org.xerial:sqlite-jdbc:3.44.1.0")
                implementation(libs.ktor.client.okhttp)
                // CIO is JVM-only, so we use OkHttp for Desktop
                implementation(libs.ktor.client.cio)
                implementation(libs.sshj)   // SFTP via SSH
                implementation(libs.smbj)   // SMB/CIFS
                implementation(libs.lsp4j)  // iter-61 Phase 4: Eclipse LSP4J JSON-RPC
                implementation(libs.flexmark.core)  // iter-62 Phase 4: HoverMarkdownRenderer Flexmark walker

                // Tree-Sitter (iter-57 Phase 5). JAR bundles native binaries
                // for x86_64-linux, aarch64-linux, x86_64-macos, aarch64-macos,
                // x86_64-windows (verified by inspecting the JAR). JDK 11
                // bytecode — compatible with Yole desktop jvmTarget=11.
                implementation(libs.tree.sitter)
                implementation(libs.tree.sitter.markdown)

                // iter-58 F2 Phase 7 — 47 additional bonede grammar JARs.
                // Each ships the same 5 Desktop ABI bundle as tree-sitter-
                // markdown above. The bonede NativeUtils extracts at runtime;
                // the Yole replacement NativeUtils landed in iter-57 handles
                // both Android (System.loadLibrary) and Desktop (classpath
                // extract + System.load) paths identically.
                // Versions snapshot: 2026-05-15. Adding/removing entries
                // here MUST be mirrored in:
                //   - tools/build-language-grammars.sh BONEDE_ARTIFACT map
                //   - GrammarLoader.desktop.kt / .android.kt class-name map
                // Sum of these 47 JARs + the 2 above = ~115 MB Gradle cache.
                // Each end-user platform pulls ONE arch out of the JAR, so
                // shipped artifact bloat is ~22-25 MB per platform.
                implementation(libs.ts.kotlin)
                implementation(libs.ts.java)
                implementation(libs.ts.python)
                implementation(libs.ts.javascript)
                implementation(libs.ts.typescript)
                implementation(libs.ts.go)
                implementation(libs.ts.rust)
                implementation(libs.ts.c)
                implementation(libs.ts.cpp)
                implementation(libs.ts.html)
                implementation(libs.ts.css)
                implementation(libs.ts.sql)
                implementation(libs.ts.json)
                implementation(libs.ts.tsx)
                implementation(libs.ts.yaml)
                implementation(libs.ts.toml)
                implementation(libs.ts.bash)
                implementation(libs.ts.ruby)
                implementation(libs.ts.php)
                implementation(libs.ts.swift)
                implementation(libs.ts.scala)
                implementation(libs.ts.dart)
                implementation(libs.ts.lua)
                implementation(libs.ts.perl)
                implementation(libs.ts.haskell)
                implementation(libs.ts.ocaml)
                implementation(libs.ts.julia)
                implementation(libs.ts.r)
                implementation(libs.ts.elixir)
                implementation(libs.ts.erlang)
                implementation(libs.ts.fortran)
                implementation(libs.ts.dockerfile)
                implementation(libs.ts.make)
                implementation(libs.ts.hcl)
                implementation(libs.ts.regex)
                implementation(libs.ts.vue)
                implementation(libs.ts.graphql)
                implementation(libs.ts.csharp)
                implementation(libs.ts.scss)
                implementation(libs.ts.nix)
                implementation(libs.ts.zig)
                implementation(libs.ts.elm)
                implementation(libs.ts.clojure)
                // nim deliberately excluded — KNOWN_DEFECTS#f2-phase-7-nim-grammar-broken.
                implementation(libs.ts.objc)
                implementation(libs.ts.latex)
                implementation(libs.ts.proto)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libs.assertj.core)
                implementation(libs.mockk)
                implementation(libs.junit)
            }
        }

        // iOS-specific code - Re-enabled for production
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)

            dependencies {
                // iOS-specific dependencies (inherited from commonMain)
                implementation(libs.kotlinx.coroutines.core)
                // Native SQLite for iOS - Using platform-specific implementation
                // implementation("co.touchlab:sqliter:1.3.1")
            }
        }

        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }

        // Web-specific code (Wasm)
        val wasmJsMain by getting {
            dependencies {
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                // SQLDelight not available for WASM
                // implementation("app.cash.sqldelight:sqljs-driver:2.0.2")
                implementation(libs.ktor.client.js)
                // WASM uses JS client, no CIO needed

                // iter-57 Phase 6: vscode-textmate JS interop tokenizer engine.
                // vscode-textmate 9.x ships ESM + CJS bundles; consumed via
                // @JsModule("vscode-textmate"). vscode-oniguruma 2.x provides the
                // WebAssembly regex engine required by vscode-textmate.
                // Both packages are MIT-licensed (Microsoft) and compatible with
                // Yole's SPDX policy. Versions pinned conservatively against the
                // upstream majors documented in research-report.md §3.
                implementation(npm("vscode-textmate", "9.2.0"))
                implementation(npm("vscode-oniguruma", "2.0.1"))
            }
        }

        val wasmJsTest by getting {
            dependencies {
                implementation(kotlin("test-wasm-js"))
                // kotlinx-coroutines-test doesn't have WASM variant
                // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                // MockK is JVM-only, not available for WASM
                // implementation(libs.mockk)
            }
        }

        // Benchmark source set (created automatically by benchmark compilation)
        val desktopBenchmark by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.11")
            }
        }
    }
}

// iOS framework configuration - enabled when building on macOS
kotlin {
    // Configure frameworks for iOS targets when available
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        if (targetName.contains("ios", ignoreCase = true)) {
            binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework> {
                baseName = "YoleShared"
                isStatic = true
                
                // Export the shared module for iOS consumption
                export(project(":shared"))
            }
        }
    }
}

// Dokka configuration for API documentation
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    // Basic module information
    moduleName = "Yole Shared Module"
    
    // Configure output directory
    outputDirectory = file("${layout.buildDirectory.get()}/dokka")
}

// Configure allopen for benchmark annotations
allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

// Benchmark configuration
benchmark {
    targets {
        register("desktop") {
            this as kotlinx.benchmark.gradle.JvmBenchmarkTarget
            jmhVersion = "1.37"
        }
    }

    configurations {
        named("main") {
            warmups = 3
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
        }
    }
}

// Custom task to run simple benchmarks (workaround for kotlinx.benchmark KMP issues)
tasks.register<JavaExec>("runSimpleBenchmarks") {
    group = "verification"
    description = "Run simple performance benchmarks (KMP workaround)"

    dependsOn("compileBenchmarkKotlinDesktop", "compileKotlinDesktop")

    // Configuration cache compatible classpath setup
    val desktopTarget = kotlin.targets.getByName("desktop")
    val benchmarkCompilation = desktopTarget.compilations.getByName("benchmark")
    val mainCompilation = desktopTarget.compilations.getByName("main")

    classpath = files(
        mainCompilation.output.allOutputs,
        benchmarkCompilation.output.allOutputs,
        benchmarkCompilation.runtimeDependencyFiles
    )

    mainClass.set("digital.vasic.yole.format.benchmark.SimpleBenchmarkRunner")
}

// Task to run Go-based Challenge framework tests
tasks.register<Exec>("runChallenges") {
    group = "verification"
    description = "Build and test the Challenges Go submodule (requires Go 1.24+)"

    val challengesDir = file("${rootDir}/Challenges")
    val containersDir = file("${rootDir}/Containers")

    doFirst {
        // Check Go is available
        val goCheck = try {
            val proc = ProcessBuilder("go", "version")
                .redirectErrorStream(true)
                .start()
            val output = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            if (proc.exitValue() != 0) {
                throw GradleException("Go returned non-zero exit code")
            }
            logger.lifecycle("Found: $output")
            true
        } catch (e: Exception) {
            throw GradleException(
                "Go is not available on PATH. Install Go 1.24+ to run challenges. " +
                "See https://go.dev/doc/install"
            )
        }

        if (!challengesDir.exists()) {
            throw GradleException(
                "Challenges/ directory not found. " +
                "Run 'git submodule update --init --recursive' first."
            )
        }
        if (!containersDir.exists()) {
            throw GradleException(
                "Containers/ directory not found. " +
                "Run 'git submodule update --init --recursive' first."
            )
        }
    }

    workingDir = challengesDir
    commandLine("go", "test", "./...", "-race", "-count=1")
}

// Task to run HelixQA orchestrated testing
tasks.register<Exec>("runHelixQA") {
    group = "verification"
    description = "Run HelixQA orchestrated QA across all platforms (requires Go 1.24+)"

    val helixqaDir = file("${rootDir}/HelixQA")
    val banksDir = file("${rootDir}/Challenges/banks/yole")

    doFirst {
        if (!helixqaDir.exists()) {
            throw GradleException(
                "HelixQA/ directory not found. " +
                "Run 'git submodule update --init --recursive' first."
            )
        }
    }

    workingDir = helixqaDir
    commandLine("go", "test", "./...", "-race", "-count=1")
}

android {
    namespace = "digital.vasic.yole.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // iter-57 #android-tree-sitter-ndk-so-missing — surface the prebuilt
    // Android NDK libtree-sitter[-markdown].so files via standard
    // jniLibs convention so AGP packages them at <apk>/lib/<abi>/lib*.so.
    // Our Yole-replacement NativeUtils on Android then locates them via
    // System.loadLibrary("tree-sitter") + System.loadLibrary("tree-sitter-markdown").
    sourceSets.getByName("main").jniLibs.srcDirs(
        layout.projectDirectory.dir("native/android-tree-sitter")
    )
}

// Detekt configuration for static analysis
detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

// Kover configuration for code coverage
kover {
    reports {
        // Configure verification rules - 70% minimum coverage
        verify {
            rule {
                minBound(70) // Minimum 70% coverage target
            }
        }

        // Configure filters for coverage
        filters {
            excludes {
                // Exclude benchmark code from coverage
                packages("digital.vasic.yole.benchmark")
                packages("digital.vasic.yole.format.benchmark")

                // Exclude generated code
                annotatedBy("*Generated*")

                // Exclude test code from production coverage
                packages("digital.vasic.yole.*Test*")
                packages("digital.vasic.yole.*Tests*")
            }
        }
    }
}

// =========================================================================
// iter-57 Android NDK fix — #android-tree-sitter-ndk-so-missing
// =========================================================================
//
// Resolves the long-standing gap that the bonede tree-sitter JARs do not
// bundle Android NDK shared libraries. Two-step solution:
//
// 1. The .so files are built once via the Android NDK r29 clang
//    toolchain against tree-sitter 0.22.6 + ikatyang's tree-sitter-markdown
//    0.7.1 grammar (matching the bonede JAR versions pinned in
//    libs.versions.toml). They are committed at
//    shared/native/android-tree-sitter/<abi>/lib{tree-sitter,
//    tree-sitter-markdown}.so and consumed via the standard Android
//    jniLibs convention — androidApp.sourceSets.main.jniLibs.srcDirs
//    points at that directory, so the APK ships them at
//    <apk>/lib/<abi>/libtree-sitter.so etc.
//
// 2. The bonede `org.treesitter.utils.NativeUtils` class extracts JAR-
//    embedded linux-gnu .so files at runtime — on Android, that flow
//    fails (linux-gnu binaries are not loadable on bionic; ${user.home}
//    is unwritable in app sandboxes; the CRC-overwrite logic destroys
//    any operator-placed Android .so file). We therefore swap the
//    bonede NativeUtils.class for a Yole-written drop-in replacement
//    (source at shared/native/android-tree-sitter/java/...) that:
//      - on Android (detected via java.vm.vendor / Dalvik / ART),
//        loads the library via System.loadLibrary, which routes through
//        the Android linker and picks up the jniLibs-placed .so file;
//      - on Desktop / Server JVMs, preserves the bonede 0.22.6 extract-
//        then-System.load flow byte-for-byte so :shared:desktopTest
//        continues to pass.
//
// Architecture coverage: arm64-v8a + x86_64. armeabi-v7a is NOT included
// because the upstream bonede NativeUtils we replace on Android already
// would have rejected it (os.arch="armv7l" not in its supported set).
// Google Play 64-bit-only policy + Android 13+ device shipping practice
// make 32-bit ARM obsolete for new releases anyway.
// =========================================================================
val androidTreeSitterNativeDir =
    layout.projectDirectory.dir("native/android-tree-sitter")
val androidTreeSitterJavaDir =
    layout.projectDirectory.dir("native/android-tree-sitter/java")
val repackagedLibsDir = layout.buildDirectory.dir("repackaged-libs")
val yoleAndroidNativeUtilsClassesDir = layout.buildDirectory.dir("yole-native-utils-classes")

// Configuration that resolves only the upstream bonede JARs so we can
// pull their bytes without polluting the Android compile classpath.
val bonedeJarsToRepackage by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    bonedeJarsToRepackage(libs.tree.sitter)
    bonedeJarsToRepackage(libs.tree.sitter.markdown)
}

// Compile our Yole replacement org.treesitter.utils.NativeUtils against
// JDK 11 bytecode (matching bonede's distribution). The output class
// file is then patched into the repackaged JAR (replacing bonede's
// original NativeUtils.class) by RepackageBonedeJarTask.
val compileYoleNativeUtils = tasks.register<JavaCompile>(
    "compileYoleAndroidNativeUtils"
) {
    group = "build"
    description = "Compile Yole replacement org.treesitter.utils.NativeUtils for Android"
    source = fileTree(androidTreeSitterJavaDir) { include("**/*.java") }
    classpath = files()
    destinationDirectory.set(yoleAndroidNativeUtilsClassesDir)
    options.release.set(11)
}

abstract class RepackageBonedeJarTask : DefaultTask() {
    @get:InputFile abstract val inputJar: RegularFileProperty
    @get:InputDirectory abstract val compiledNativeUtilsDir: DirectoryProperty
    @get:OutputFile abstract val outputJar: RegularFileProperty

    @TaskAction
    fun repackage() {
        val srcJar = inputJar.get().asFile
        val dstJar = outputJar.get().asFile
        dstJar.parentFile.mkdirs()
        val nativeUtilsClass = compiledNativeUtilsDir.get().asFile
            .resolve("org/treesitter/utils/NativeUtils.class")
        require(nativeUtilsClass.exists()) {
            "Missing compiled NativeUtils.class at $nativeUtilsClass — " +
                "verify compileYoleAndroidNativeUtils task ran."
        }
        val replacements = mapOf(
            "org/treesitter/utils/NativeUtils.class" to nativeUtilsClass,
        )
        var matched = 0
        ZipInputStream(srcJar.inputStream()).use { zin ->
            ZipOutputStream(dstJar.outputStream()).use { zout ->
                var entry = zin.nextEntry
                while (entry != null) {
                    val name = entry.name
                    val replacement = replacements[name]
                    if (replacement != null) {
                        matched++
                        val outEntry = ZipEntry(name)
                        outEntry.time = entry.time
                        zout.putNextEntry(outEntry)
                        replacement.inputStream().use { it.copyTo(zout) }
                        zout.closeEntry()
                    } else {
                        val outEntry = ZipEntry(name)
                        outEntry.time = entry.time
                        zout.putNextEntry(outEntry)
                        if (!entry.isDirectory) zin.copyTo(zout)
                        zout.closeEntry()
                    }
                    entry = zin.nextEntry
                }
            }
        }
        // bonede tree-sitter JAR has NativeUtils.class; tree-sitter-markdown
        // does NOT (it depends transitively on tree-sitter for it). Both
        // call paths are fine — matched==1 for tree-sitter, matched==0 for
        // tree-sitter-markdown.
        logger.lifecycle(
            "Repackaged ${srcJar.name} → ${dstJar.name} (NativeUtils swaps=$matched)"
        )
    }
}

val repackageTreeSitterJar = tasks.register<RepackageBonedeJarTask>(
    "repackageTreeSitterJarForAndroid"
) {
    group = "build"
    description = "Patch bonede tree-sitter JAR with Yole replacement NativeUtils for Android"
    dependsOn(compileYoleNativeUtils)
    compiledNativeUtilsDir.set(yoleAndroidNativeUtilsClassesDir)
    inputJar.fileProvider(provider {
        bonedeJarsToRepackage.resolvedConfiguration.resolvedArtifacts
            .single { it.moduleVersion.id.name == "tree-sitter" }
            .file
    })
    outputJar.set(repackagedLibsDir.map { it.file("tree-sitter-android.jar") })
}

val repackageTreeSitterMarkdownJar = tasks.register<RepackageBonedeJarTask>(
    "repackageTreeSitterMarkdownJarForAndroid"
) {
    group = "build"
    description = "Pass-through copy of bonede tree-sitter-markdown JAR for the Android source set"
    dependsOn(compileYoleNativeUtils)
    compiledNativeUtilsDir.set(yoleAndroidNativeUtilsClassesDir)
    inputJar.fileProvider(provider {
        bonedeJarsToRepackage.resolvedConfiguration.resolvedArtifacts
            .single { it.moduleVersion.id.name == "tree-sitter-markdown" }
            .file
    })
    outputJar.set(repackagedLibsDir.map { it.file("tree-sitter-markdown-android.jar") })
}

// Re-bind the Android source set to the patched JARs produced by the
// repackage tasks. Using `files(taskProvider)` carries the explicit
// task-output dependency so AGP's strict validation does not flag an
// implicit dependency (Gradle 8.11 error).
dependencies {
    val patchedTreeSitter = files(repackageTreeSitterJar.map { it.outputJar.get().asFile })
    val patchedTreeSitterMarkdown = files(repackageTreeSitterMarkdownJar.map { it.outputJar.get().asFile })
    add("androidMainImplementation", patchedTreeSitter)
    add("androidMainImplementation", patchedTreeSitterMarkdown)
}

// jniLibs srcDirs is now configured inside the android { } block above.

// ─── LSP Binary acquisition task (Phase 7) ────────────────────────────────
// Downloads pre-built LSP server binaries into .lsp-binary-cache/ and stages
// them for use by LspServerInstaller at runtime.  Binaries are NOT committed
// to git; the cache directory is gitignored.  Storage policy: option (c).
//
// Run explicitly:   ./gradlew :shared:lspBinaries
// Run with force:   ./gradlew :shared:lspBinaries --rerun-tasks
//
// The task is intentionally NOT wired into processResources — it performs
// network I/O and must remain opt-in so offline builds do not regress.
tasks.register<Exec>("lspBinaries") {
    group = "build"
    description = "Download and stage LSP server binaries for Desktop macos-arm64 (Phase 7 v1)"

    val scriptFile = file("${rootDir}/scripts/acquire-lsp-binaries.sh")
    inputs.file(scriptFile)
    outputs.dir(file("${rootDir}/.lsp-binary-cache"))

    commandLine("bash", scriptFile.absolutePath, "--abi", "macos-arm64")

    doFirst {
        if (!scriptFile.exists()) {
            throw GradleException(
                "LSP acquire script not found: ${scriptFile.absolutePath}\n" +
                "Run from the repo root: bash scripts/acquire-lsp-binaries.sh"
            )
        }
        logger.lifecycle("Acquiring LSP binaries → ${rootDir}/.lsp-binary-cache/")
    }
}

// Convenience task: acquire + verify SHA256 checksums where available
tasks.register<Exec>("lspBinariesVerify") {
    group = "verification"
    description = "Download LSP binaries and verify SHA256 checksums"
    val scriptFile = file("${rootDir}/scripts/acquire-lsp-binaries.sh")
    commandLine("bash", scriptFile.absolutePath, "--abi", "macos-arm64", "--verify")
}

// ── iter-61 Phase 8: Gradle binary bridge ──────────────────────────────────
//
// Stages cached LSP server binaries from .lsp-binary-cache/<langId>/macos-arm64/
// into the JVM processedResources directory (lsp-bundles/<langId>/<executable>).
// This makes binaries available on the desktopTest classpath so
// LspServerInstaller.desktop.kt can extract them at test time without network I/O.
//
// Layout mapping:
//   .lsp-binary-cache/<langId>/macos-arm64/<exe>
//   → shared/build/processedResources/desktop/main/lsp-bundles/<langId>/<exe>
//
// Also staged into the test resource dir so desktopTest classpath resolves them.
//
// The task is incremental via Gradle's Sync semantics (only copies changed files).
// Run explicitly: ./gradlew :shared:lspBundleStage
// Wired to:       desktopProcessResources + desktopTestProcessResources

val lspBundleStage = tasks.register<Sync>("lspBundleStage") {
    description = "Stages cached LSP server binaries from .lsp-binary-cache into JVM lsp-bundles resources."
    group = "build"

    val cacheDir = rootProject.projectDir.resolve(".lsp-binary-cache")

    // Include only the <langId>/macos-arm64/<anything> subtree.
    // Strip the ABI dir ("macos-arm64") so the resource lands at lsp-bundles/<langId>/<exe>.
    from(cacheDir) {
        include("*/macos-arm64/**")
        // eachFile iterates files (not dirs). Segments: [<langId>, "macos-arm64", <rest...>]
        // After stripping index 1 ("macos-arm64") → [<langId>, <rest...>].
        eachFile {
            val segs = relativePath.segments.toMutableList()
            if (segs.size >= 2 && segs[1] == "macos-arm64") {
                segs.removeAt(1)
                relativePath = RelativePath(true, *segs.toTypedArray())
            }
        }
        includeEmptyDirs = false
    }

    into(layout.buildDirectory.dir("processedResources/desktop/main/lsp-bundles"))

    doFirst {
        if (!cacheDir.exists() || cacheDir.listFiles().isNullOrEmpty()) {
            logger.warn(
                "lspBundleStage: .lsp-binary-cache is absent or empty. " +
                "Run `bash scripts/acquire-lsp-binaries.sh` first. " +
                "Staging nothing — desktopTest smoke tests will SKIP (not FAIL)."
            )
        }
    }
}

// Also stage into the desktop test resource dir so desktopTest classpath picks them up.
val lspBundleStageTest = tasks.register<Sync>("lspBundleStageTest") {
    description = "Stages cached LSP server binaries into the desktop TEST resource dir for desktopTest classpath."
    group = "build"

    val cacheDir = rootProject.projectDir.resolve(".lsp-binary-cache")

    from(cacheDir) {
        include("*/macos-arm64/**")
        eachFile {
            val segs = relativePath.segments.toMutableList()
            if (segs.size >= 2 && segs[1] == "macos-arm64") {
                segs.removeAt(1)
                relativePath = RelativePath(true, *segs.toTypedArray())
            }
        }
        includeEmptyDirs = false
    }

    into(layout.buildDirectory.dir("processedResources/desktop/test/lsp-bundles"))
}

// Wire into the standard Gradle resource-processing lifecycle so that
// `./gradlew :shared:desktopProcessResources` and desktopTest both include
// the staged binaries without any additional developer action.
tasks.named("desktopProcessResources") {
    dependsOn(lspBundleStage)
}
tasks.named("desktopTestProcessResources") {
    dependsOn(lspBundleStage, lspBundleStageTest)
}
