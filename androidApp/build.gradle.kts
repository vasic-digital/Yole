/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Android Application Module for Yole
 * Modern Android app with Compose Multiplatform
 *
 *########################################################*/

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// iter-64 Phase 15: pdfbox-android pulls in org.bouncycastle:bcprov-jdk15to18:1.72
// while the SSH/SFTP stack depends on org.bouncycastle:bcprov-jdk18on:1.75 —
// the two are identical in API but different artifact coordinates, causing
// checkDuplicateClasses to fail. Force resolution to the newer jdk18on family
// so only one set of BouncyCastle classes lands in the APK.
configurations.all {
    resolutionStrategy {
        eachDependency {
            if (requested.group == "org.bouncycastle") {
                when (requested.name) {
                    "bcprov-jdk15to18" -> useTarget("org.bouncycastle:bcprov-jdk18on:1.75")
                    "bcpkix-jdk15to18" -> useTarget("org.bouncycastle:bcpkix-jdk18on:1.75")
                    "bcutil-jdk15to18" -> useTarget("org.bouncycastle:bcutil-jdk18on:1.75")
                }
            }
        }
    }
}

android {
    namespace = "digital.vasic.yole.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "digital.vasic.yole.android"
        // iter-64 Phase 15: Apache POI 5.x uses MethodHandle.invoke (Java 9+ bytecode)
        // which D8 rejects below API 26. POI requires Android 8+ (API 26) per upstream.
        // Raised from 24 to 26; API 26 covers ~92%+ of active Android devices (Dec 2025).
        minSdk = 26
        targetSdk = 35
        versionCode = 206
        versionName = "2.0.6"

        // iter-64 Phase 3: Apache POI pushes the method count past the 64k
        // dex limit. multiDexEnabled = true activates AndroidX MultiDex so
        // the APK is split across multiple dex files at build time.
        // androidx.multidex:multidex is already declared in libs.versions.toml
        // and wired into shared/build.gradle.kts androidMain dependencies.
        multiDexEnabled = true

        // Custom runner grants MANAGE_EXTERNAL_STORAGE before any test
        // launches MainActivity. Resolves Bucket A of the iter-34 finding
        // documented in docs/qa/iter-34/known-issues.md.
        testInstrumentationRunner = "digital.vasic.yole.android.test.YoleTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("docker/keys/yole.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("YOLE_KEYSTORE_PASSWORD") ?: "yole123"
                keyAlias = System.getenv("YOLE_KEY_ALIAS") ?: "yole"
                keyPassword = System.getenv("YOLE_KEY_PASSWORD") ?: "yole123"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release variant MUST be signed with the project's release
            // keystore. If `docker/keys/yole.keystore` is absent on this
            // host, the build will fail at `packageRelease` with a clear
            // "storeFile missing" error. Run `scripts/generate-keystore.sh`
            // to create one (idempotent), or transfer the production
            // keystore from the dev host. NEVER fall back to debug-signing
            // for release distributions — that violates the user mandate
            // "properly signed with the key" recorded in iter 30.
            signingConfig = signingConfigs.getByName("release")
            // iter-59: production label (no DEV suffix).
            manifestPlaceholders["appLabel"] = "Yole"
        }
        debug {
            // iter-59: distinguish dev builds from production at every visible
            // surface so the operator (and testers) never confuse the two.
            applicationIdSuffix = ".dev"
            versionNameSuffix = " DEV"
            // Override the launcher icon + label per-variant — see
            // androidApp/src/debug/res/ for the tinted resources.
            manifestPlaceholders["appLabel"] = "Yole DEV"
            isDebuggable = true
            // No keystore signing for debug — AGP defaults (debug keystore) apply.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // iter-64 Phase 15: Enable desugaring for Java 8+ API backport (e.g. java.time).
        isCoreLibraryDesugaringEnabled = true
    }

    // KGP 2.3+ requires compilerOptions DSL; jvmTarget must match compileOptions (JVM 11)
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // AppCompatResource is a false positive - we do use appcompat library
        disable += setOf("AppCompatResource")
        // Treat warnings as informational only
        warningsAsErrors = false
        abortOnError = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // iter-60 Phase 9 fix (#iter-60-robolectric-snippet-classpath):
    // KMP commonMain/resources are NOT placed on the Android unit-test
    // classpath automatically — they end up only in the desktop
    // processedResources and the production AAR assets, never in the
    // JVM classpath that Robolectric's SandboxClassLoader interrogates
    // via getResourceAsStream(). Adding the shared commonMain/resources
    // directory as a test Java resource source set makes the snippet
    // JSON files visible to all three classloader probes in
    // SnippetRegistry.android.kt (Thread context + anonymous object +
    // system classloader) under Robolectric.
    sourceSets {
        getByName("test") {
            resources.srcDirs(
                "src/test/resources",
                "../shared/src/commonMain/resources",
            )
        }
    }
}

// Robolectric Compose UI tests run in a DEDICATED container per the user
// mandate ("Make sure they are ran in separate individual container").
// Wired via `make container-robolectric-test` and the `robolectric-test`
// service in docker-compose.yml. The default `:androidApp:testDebugUnitTest`
// task excludes them so the main release build pipeline (which runs in
// `make container-release`) doesn't get gated on UI-test stability — the
// dedicated container is the single source of truth for Robolectric pass/fail.
//
// To run Robolectric tests:
//     make container-robolectric-test
//
// To temporarily include them in the default test task (e.g., for debugging):
//     ./gradlew :androidApp:testDebugUnitTest -PincludeRobolectric=true
//
// CONST-035 anti-bluff: Robolectric tests must still pass green in their
// dedicated container before any release ships. Skipping them is not the
// same as exempting them — a SKIP-OK exemption only suppresses scanner
// detection, never substitutes for actually running the test.
//
// Iter 38 fix (#yole-android-gradle-utp-single-class-filter): scope the
// filter to JVM unit-test tasks only. `DeviceProviderInstrumentTestTask`
// (the type of `connectedDebugAndroidTest`) extends `Test` in AGP 8.x, so
// withType<Test>().configureEach { filter {...} } previously narrowed the
// connected-test execution to a single class via UTP's `class` arg_map,
// producing 26 PASS / 8 SKIP-OK reports instead of the true 49 PASS /
// 27 SKIP-OK. Limiting application to *UnitTest tasks (which is where
// Robolectric tests live anyway) avoids the collateral damage while
// preserving the original intent.
tasks.withType<Test>().configureEach {
    val isJvmUnitTest = name.endsWith("UnitTest")
    if (isJvmUnitTest && !project.hasProperty("includeRobolectric")) {
        filter {
            excludeTestsMatching("*.robolectric.*")
            isFailOnNoMatchingTests = false
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":commons"))

    // iter-64 Phase 15: core library desugaring for Apache POI MethodHandle.invoke API.
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // Compose (CMP artifacts for UI/Foundation/Runtime; material3 uses AndroidX for Android target)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    // iter-83: CMP 1.11.0 material3 is a KMP-only artifact; Android target uses androidx.compose.material3
    implementation(libs.androidx.compose.material3)
    // Material Icons needed for Icons.* references in YoleApp.kt
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.documentfile)

    // Material Design 3 (AndroidX alias, same as above — explicit for clarity)
    implementation(libs.androidx.compose.material3)

    // PDF Export
    implementation("com.itextpdf:itextpdf:5.5.13.3")
    
    // Backup/Restore
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    
    // File operations
    implementation(libs.commons.io)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics.lib)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.config)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.compose.ui)
    androidTestImplementation(libs.androidx.test.rules)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test)

    // Robolectric for JVM-based Android UI testing
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.test.compose.ui)
}