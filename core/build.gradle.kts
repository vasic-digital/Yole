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
    namespace = "digital.vasic.yole.core"

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

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java", "src/main/kotlin")
        }
    }
}

dependencies {
    // Module dependencies
    implementation(project(":commons"))

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

    // Material Design
    implementation(libs.material)
}
