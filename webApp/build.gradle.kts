 /*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Web Application Module for Yole
 * Progressive Web App with Kotlin/Wasm
 *
 *########################################################*/

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        // iter-82: `moduleName` removed in KGP 2.3.x — use outputModuleName + Provider API.
        outputModuleName.set("yole-web")
        // iter-82: binaries.executable() was blocked by KGP 2.0.20 bug (KT-XXXXX).
        // Fixed in KGP 2.3.21 — production bundle now enabled.
        binaries.executable()
        browser {
            commonWebpackConfig {
                // iter-82: `outputFileName` removed — kept for webpack output name only (not runTask).
                outputFileName = "yole-web.js"
            }
        }
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)

                // Web-specific dependencies
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
                // iter-82: pinned to 0.6.1 to match composite-build submodule binary compat constraint
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }

        val wasmJsTest by getting {
            dependencies {
                implementation(kotlin("test-wasm-js"))
                // kotlinx-coroutines-test doesn't have WASM variant
                // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

// Dev server configuration
@OptIn(ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
            runTask {
                // iter-82: `outputFileName` removed in KGP 2.3.x — use mainOutputFileName.
                mainOutputFileName.set("yole-web.js")
            }
        }
    }
}

// tasks.named("wasmJsBrowserDistribution") {
//     dependsOn("copyWebResources")
// }

// Copy Logo.png to resources
tasks.register<Copy>("copyLogo") {
    from("../../Assets/Logo.png")
    into("src/wasmJsMain/resources")
}

tasks.named("compileKotlinWasmJs") {
    dependsOn("copyLogo")
}

tasks.named("wasmJsProcessResources") {
    dependsOn("copyLogo")
}
