/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS Application Module for Yole
 * Native iOS app with Compose Multiplatform
 *
 *########################################################*/

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    // iOS targets - Re-enabled for production
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        val iosMain by creating {
            dependsOn(commonMain.get())
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)

            dependencies {
                implementation(project(":shared"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)

                // iOS-specific dependencies - Updated to match version catalog
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            }
        }

        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting

        val iosTest by creating {
            dependsOn(commonTest.get())
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)

            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
            }
        }
    }
}

// iOS-specific configurations - Re-enabled for production development
// Framework tasks will be configured when building on macOS with Xcode

// Configure iOS frameworks when targets are available
kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        if (targetName.contains("ios", ignoreCase = true)) {
            binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.Framework> {
                baseName = "YoleIOS"
                isStatic = true
                
                // Export dependencies for iOS app framework
                export(project(":shared"))
            }
        }
    }
}