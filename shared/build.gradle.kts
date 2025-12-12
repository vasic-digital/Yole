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
                // Kotlin Coroutines - Updated to match version catalog
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

                // Kotlinx Serialization - Updated to match version catalog
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

                // DateTime - Updated to match version catalog
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

                // Okio for file system - Updated to match version catalog
                implementation("com.squareup.okio:okio:3.9.1")

                 // Ktor Client for network operations
                 implementation(libs.ktor.client.core)
                 implementation("io.ktor:ktor-client-content-negotiation:3.0.2")
                 implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")

                 // SQLite database for metadata - platform specific due to WASM incompatibility
                 // implementation("app.cash.sqldelight:runtime:2.0.2")
                 // implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
                 
                 // Database dependencies for cross-platform implementation
                 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

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
                implementation("io.ktor:ktor-client-cio:3.0.2")
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
                implementation("io.ktor:ktor-client-cio:3.0.2")
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                // SQLDelight not available for WASM
                // implementation("app.cash.sqldelight:sqljs-driver:2.0.2")
                implementation(libs.ktor.client.js)
                // WASM uses JS client, no CIO needed
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
}

// Kover configuration for code coverage
kover {
    reports {
        // Configure verification rules
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

                // Exclude generated code
                annotatedBy("*Generated*")
            }
        }
    }
}
