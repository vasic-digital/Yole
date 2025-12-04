/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Kotlin Multiplatform Settings
 *#########################################################*/

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://jitpack.io")
        // Node.js distributions for Kotlin/JS and Kotlin/Wasm
        ivy("https://nodejs.org/dist") {
            patternLayout {
                artifact("v[revision]/[artifact]-v[revision]-[classifier].[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
        }
        // Yarn distributions for Kotlin/JS and Kotlin/Wasm
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            patternLayout {
                artifact("v[revision]/[artifact]-v[revision].tar.gz")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("com.yarnpkg", "yarn")
            }
        }
    }
}

rootProject.name = "Yole"

// Enable Gradle version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Existing Android modules
include(":core")
include(":commons")

// Format modules removed as they were for legacy app

// Kotlin Multiplatform modules
include(":shared")

// Platform-specific apps
include(":androidApp")
include(":desktopApp")
include(":webApp")
include(":iosApp")
