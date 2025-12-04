/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Root build file - Kotlin Multiplatform configuration
 *#########################################################*/

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    // Apply plugins using version catalog
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kover) apply false
}

// Build configuration
buildscript {
    extra.apply {
        set("version_compileSdk", 35)
        set("version_buildTools", "35.0.0")
        set("version_minSdk", 21) // Updated for Compose Multiplatform support
        set("version_targetSdk", 35)
    }
}

// Configure all projects
allprojects {
    // Set extra properties for all projects
    extra.apply {
        set("version_compileSdk", 35)
        set("version_buildTools", "35.0.0")
        set("version_minSdk", 21) // Updated for Compose Multiplatform support
        set("version_targetSdk", 35)
    }
    // Copy repo files task (for README, LICENSE, etc.)
    tasks.register<Copy>("copyRepoFiles") {
        from(rootProject.files(ROOT_TO_RAW_COPYFILES))
        into("app/src/main/res/raw")
        rename { fileName -> fileName.lowercase() }
    }

    // Ensure resource generation depends on copyRepoFiles
    tasks.matching { task ->
        task.name.matches(Regex("(.*generate.*Resources)|(.*map.*SourceSetPaths)"))
    }.configureEach {
        dependsOn("copyRepoFiles")
    }

    // Configure test output for all test tasks
    tasks.withType<Test>().configureEach {
        testLogging {
            events("passed", "skipped", "failed", "standardOut", "standardError")
            showStandardStreams = true
        }
        reports {
            junitXml.required.set(true)
            html.required.set(true)
        }
    }
}

// Clean task
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// Helper functions and constants
val ROOT_TO_RAW_COPYFILES = arrayOf(
    "README.md",
    "CHANGELOG.md",
    "CONTRIBUTORS.md",
    "LICENSE.txt",
    "LICENSE.md",
    "LICENSE"
)

/**
 * Returns used android languages as a buildConfig array: {'de', 'it', ..}
 */
fun findUsedAndroidLocales(): String {
    val langs = mutableSetOf<String>()

    file(".").walkTopDown()
        .filter { it.isDirectory }
        .filter { it.name.contains("values-") }
        .filter { !it.absolutePath.contains("build${File.separator}intermediates") }
        .filter { !it.name.matches(Regex(".*values-((.*[0-9])|(land)|(port)).*")) }
        .forEach { dir ->
            dir.walkTopDown()
                .filter { it.isFile }
                .filter { it.extension == "xml" }
                .filter { it.readText().contains("<string") }
                .forEach {
                    langs.add(dir.name.replace("values-", ""))
                }
        }

    return langs.sorted().joinToString(prefix = "{", postfix = "}", separator = ",") { "\"$it\"" }
}

/**
 * Returns the build date in RFC3339 compatible format. TZ is always converted to UTC
 */
fun getBuildDate(): String {
    val rfc3339Like = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    rfc3339Like.timeZone = TimeZone.getTimeZone("UTC")
    return rfc3339Like.format(Date())
}

/**
 * Get Git commit hash
 */
fun getGitHash(): String {
    return try {
        val process = ProcessBuilder("git", "rev-parse", "HEAD")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        process.inputStream.bufferedReader().readText().trim()
    } catch (e: Exception) {
        "unknown"
    }
}

/**
 * Get Git last commit message
 */
fun getGitLastCommitMessage(): String {
    return try {
        val process = ProcessBuilder("git", "log", "--oneline", "-1", "--format=%s")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        process.inputStream.bufferedReader().readText().trim()
            .replace("\"", "")
            .replace("\\", "")
    } catch (e: Exception) {
        "unknown"
    }
}

// Export helper functions to be used in subprojects
ext {
    set("findUsedAndroidLocales", ::findUsedAndroidLocales)
    set("getBuildDate", ::getBuildDate)
    set("getGitHash", ::getGitHash)
    set("getGitLastCommitMessage", ::getGitLastCommitMessage)
}

// Configure Dokka for API documentation
// Note: Dokka plugin must be applied to modules to generate documentation
// Run: ./gradlew dokkaHtml (for single module) or ./gradlew :shared:dokkaHtml
// Output: build/dokka/html/

tasks.register("generateApiDocs") {
    group = "documentation"
    description = "Generate API documentation for all modules"
    doLast {
        println("Generating API documentation...")
        println("Run: ./gradlew :shared:dokkaHtml")
        println("Output will be in: shared/build/dokka/html/")
        println("")
        println("To copy to docs/api/ for publishing:")
        println("mkdir -p docs/api")
        println("cp -r shared/build/dokka/html/* docs/api/")
    }
}

// Configure Kover for code coverage
// Note: Kover plugin must be applied to generate reports
// Run: ./gradlew koverHtmlReport or ./gradlew koverXmlReport
// Reports will be in: build/reports/kover/

// Placeholder for merged report - actual Kover tasks are generated by the plugin
tasks.register("koverMergedReport") {
    group = "verification"
    description = "Generate merged code coverage report (use koverHtmlReport instead)"
    doLast {
        println("Note: Use './gradlew koverHtmlReport' to generate HTML coverage report")
        println("Or use './gradlew koverXmlReport' for XML format")
        println("Reports location: build/reports/kover/")
    }
}
