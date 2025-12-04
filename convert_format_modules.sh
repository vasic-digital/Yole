#!/bin/bash

# Script to convert all format module build.gradle files to Kotlin DSL
# This automates the conversion of 18 format modules

set -e

FORMAT_MODULES=(
    "markdown"
    "todotxt"
    "csv"
    "wikitext"
    "keyvalue"
    "asciidoc"
    "orgmode"
    "plaintext"
    "latex"
    "restructuredtext"
    "taskpaper"
    "textile"
    "creole"
    "tiddlywiki"
    "jupyter"
    "rmarkdown"
)

create_build_gradle_kts() {
    local format_name=$1
    local module_dir="format-${format_name}"
    local build_file="${module_dir}/build.gradle.kts"

    cat > "$build_file" << 'EOF'
/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   Maintained 2025 by Milos Vasic
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 #########################################################*/

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "digital.vasic.yole.format.FORMAT_NAME"

    compileSdk = rootProject.extra["version_compileSdk"] as Int
    buildToolsVersion = rootProject.extra["version_buildTools"] as String

    defaultConfig {
        minSdk = rootProject.extra["version_minSdk"] as Int
        targetSdk = rootProject.extra["version_targetSdk"] as Int

        buildConfigField("boolean", "IS_TEST_BUILD", "false")
        buildConfigField("boolean", "IS_GPLAY_BUILD", "false")
        buildConfigField("String", "BUILD_DATE", "\"${rootProject.extra["getBuildDate"] as () -> String}\"")
        buildConfigField("String", "GITHASH", "\"${rootProject.extra["getGitHash"] as () -> String}\"")
        buildConfigField("String", "GITMSG", "\"${rootProject.extra["getGitLastCommitMessage"] as () -> String}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn"
        )
    }

    lint {
        abortOnError = false
        disable += listOf(
            "MissingTranslation",
            "InvalidPackage",
            "ObsoleteLintCustomCheck",
            "DefaultLocale",
            "UnusedAttribute",
            "VectorRaster",
            "InflateParams",
            "IconLocation",
            "UnusedResources",
            "TypographyEllipsis"
        )
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Module dependencies
    implementation(project(":commons"))
    implementation(project(":core"))

    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.assertj.core)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // Format-specific dependencies
FORMAT_DEPENDENCIES
}
EOF

    # Replace FORMAT_NAME placeholder
    sed -i "s/FORMAT_NAME/${format_name}/g" "$build_file"

    # Add format-specific dependencies
    local deps=""
    case "$format_name" in
        "markdown")
            deps="    // Markdown converter (Flexmark)\n    implementation(libs.bundles.flexmark)"
            ;;
        "csv")
            deps="    // CSV processing\n    implementation(libs.opencsv)"
            ;;
        *)
            deps="    // No additional dependencies for this format"
            ;;
    esac

    sed -i "s|FORMAT_DEPENDENCIES|${deps}|g" "$build_file"

    echo "Created $build_file"
}

echo "Converting format module build files to Kotlin DSL..."

for format in "${FORMAT_MODULES[@]}"; do
    echo "Processing format-${format}..."
    create_build_gradle_kts "$format"
done

echo "✓ All format module build.gradle files converted to Kotlin DSL"
