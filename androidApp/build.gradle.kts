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
}

android {
    namespace = "digital.vasic.yole.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "digital.vasic.yole.android"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("docker/keys/yole.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = "yole123"
                keyAlias = "yole"
                keyPassword = "yole123"
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
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
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
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":commons"))

    // Compose
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.documentfile)

    // Material Design 3
    implementation(libs.compose.material3)

    // PDF Export
    implementation("com.itextpdf:itextpdf:5.5.13.3")
    
    // Backup/Restore
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    
    // File operations
    implementation(libs.commons.io)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.compose.ui)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test)

    // Robolectric for JVM-based Android UI testing
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.androidx.test.compose.ui)
}