#!/bin/bash

# List of all format modules to create
FORMATS=(
    "format-todotxt"
    "format-csv"
    "format-wikitext"
    "format-keyvalue"
    "format-asciidoc"
    "format-orgmode"
    "format-plaintext"
    "format-latex"
    "format-restructuredtext"
    "format-taskpaper"
    "format-textile"
    "format-creole"
    "format-tiddlywiki"
    "format-jupyter"
    "format-rmarkdown"
)

# Create directory structure for each format
for format in "${FORMATS[@]}"; do
    echo "Creating $format module..."
    mkdir -p "$format/src/main/java/digital/vasic/yole/format/${format#format-}"
    mkdir -p "$format/src/test/java"
    mkdir -p "$format/src/androidTest/java"

    # Create build.gradle for each format
    cat > "$format/build.gradle" << EOF
/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 #########################################################*/
apply plugin: 'com.android.library'

android {
    buildToolsVersion rootProject.ext.version_buildTools
    compileSdkVersion rootProject.ext.version_compileSdk

    defaultConfig {
        minSdkVersion rootProject.ext.version_minSdk
        targetSdkVersion rootProject.ext.version_compileSdk
        buildConfigField "boolean", "IS_TEST_BUILD", "false"
        buildConfigField "boolean", "IS_GPLAY_BUILD", "false"
        buildConfigField "String", "BUILD_DATE", "\"\${getBuildDate()}\""
        buildConfigField "String", "GITHASH", "\"\${getGitHash}\""
        buildConfigField "String", "GITMSG", "\"\${getGitLastCommitMessage}\""
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        encoding = 'UTF-8'
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    namespace 'digital.vasic.yole.format.${format#format-}'

    lint {
        abortOnError false
        disable 'MissingTranslation', 'InvalidPackage', 'ObsoleteLintCustomCheck', 'DefaultLocale', 'UnusedAttribute', 'VectorRaster', 'InflateParams', 'IconLocation', 'UnusedResources', 'TypographyEllipsis'
    }
}

dependencies {
    implementation project(':commons')
    implementation project(':core')

    // Testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.assertj:assertj-core:3.13.1'

    // Android standard libs
    implementation 'androidx.appcompat:appcompat:1.4.2'
    implementation 'com.google.android.material:material:1.6.1'
}
EOF
done

echo "All format modules created successfully!"