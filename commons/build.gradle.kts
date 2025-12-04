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
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "net.gsantner.opoc"

    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 21
        targetSdk = 35

        buildConfigField("boolean", "IS_TEST_BUILD", "false")
        buildConfigField("boolean", "IS_GPLAY_BUILD", "false")
        buildConfigField("String", "BUILD_DATE", "\"2025-12-04\"")
        buildConfigField("String", "GITHASH", "\"unknown\"")
        buildConfigField("String", "GITMSG", "\"unknown\"")

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

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.preference.ktx)

    // Material Design
    implementation(libs.material)

    // UI libs
    implementation(libs.epub.parser)

    // Utilities
    implementation(libs.commons.io)
    implementation(libs.gson)
}
