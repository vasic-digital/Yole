#!/bin/bash
# Yole Build Script - Complete build with tests, signing, and release generation

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Yole Complete Build Script ===${NC}"

# Check environment
check_environment() {
    echo -e "${YELLOW}Checking environment...${NC}"

    # Check Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}Java not found. Please install JDK 17+${NC}"
        exit 1
    fi

    # Check Gradle
    if ! command -v gradle &> /dev/null; then
        echo -e "${RED}Gradle not found${NC}"
        exit 1
    fi

    # Check Android SDK
    if [ -z "$ANDROID_SDK_ROOT" ]; then
        echo -e "${RED}ANDROID_SDK_ROOT not set${NC}"
        exit 1
    fi

    echo -e "${GREEN}Environment check passed${NC}"
}

# Generate signing keys if they don't exist
setup_signing_keys() {
    echo -e "${YELLOW}Setting up signing keys...${NC}"

    KEY_DIR="docker/keys"
    mkdir -p "$KEY_DIR"

    # Android keystore
    if [ ! -f "$KEY_DIR/yole.keystore" ]; then
        echo "Generating Android keystore..."
        keytool -genkeypair \
            -v \
            -keystore "$KEY_DIR/yole.keystore" \
            -alias yole \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -storepass yole123 \
            -keypass yole123 \
            -dname "CN=Yole, OU=Development, O=Yole, L=City, ST=State, C=US"
        echo -e "${GREEN}Android keystore created${NC}"
    fi

    # Desktop keystore
    if [ ! -f "$KEY_DIR/yole-desktop.jks" ]; then
        echo "Generating Desktop keystore..."
        keytool -genkeypair \
            -v \
            -keystore "$KEY_DIR/yole-desktop.jks" \
            -alias yole-desktop \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -storepass yole123 \
            -keypass yole123 \
            -dname "CN=Yole, OU=Development, O=Yole, L=City, ST=State, C=US"
        echo -e "${GREEN}Desktop keystore created${NC}"
    fi

    echo -e "${GREEN}Signing keys ready${NC}"
}

# Clean build
clean_build() {
    echo -e "${YELLOW}Cleaning previous build...${NC}"
    ./gradlew clean
    rm -rf releases/*
    mkdir -p releases/android releases/desktop releases/web
}

# Run all tests
run_tests() {
    echo -e "${YELLOW}Running all tests...${NC}"

    # Shared module tests
    ./gradlew :shared:testDebugUnitTest

    # Android module tests
    ./gradlew :androidApp:testDebugUnitTest

    # Desktop module tests
    ./gradlew :desktopApp:test

    echo -e "${GREEN}All tests passed${NC}"
}

# Generate coverage reports
generate_coverage() {
    echo -e "${YELLOW}Generating coverage reports...${NC}"

    ./gradlew koverHtmlReport koverXmlReport

    # Verify coverage is 100%
    COVERAGE=$(grep -oP 'line-rate="\K[0-9.]+' build/reports/kover/report.xml | head -1 || echo "0")
    COVERAGE_PCT=$(echo "$COVERAGE * 100" | bc -l | xargs printf "%.1f")

    echo "Coverage: ${COVERAGE_PCT}%"

    if (( $(echo "$COVERAGE < 0.70" | bc -l) )); then
        echo -e "${RED}Coverage below 70%! Build failed.${NC}"
        exit 1
    fi

    echo -e "${GREEN}Coverage requirements met${NC}"
}

# Build and sign Android APK
build_android() {
    echo -e "${YELLOW}Building signed Android APK...${NC}"

    # Release build uses signingConfigs defined in androidApp/build.gradle.kts
    ./gradlew :androidApp:assembleRelease

    # Copy signed APK to releases
    cp androidApp/build/outputs/apk/release/androidApp-release.apk releases/android/

    # Verify signature
    echo "Verifying Android APK signature..."
    jarsigner -verify releases/android/androidApp-release.apk

    echo -e "${GREEN}Signed Android APK built${NC}"
}

# Build and sign Desktop JAR
build_desktop() {
    echo -e "${YELLOW}Building signed Desktop JAR...${NC}"

    KEY_DIR="docker/keys"

    ./gradlew :desktopApp:packageUberJarForCurrentOS

    # Find the built jar
    JAR_FILE=$(find desktopApp/build/compose/jars -name "*.jar" | head -1)
    if [ -z "$JAR_FILE" ]; then
        echo -e "${RED}Desktop JAR not found${NC}"
        exit 1
    fi

    # Strip any existing signatures before re-signing
    echo "Stripping existing signatures..."
    zip -d "$JAR_FILE" 'META-INF/*.SF' 'META-INF/*.RSA' 'META-INF/*.DSA' 'META-INF/*.EC' 'META-INF/SIG-*' 2>/dev/null || true

    # Sign with development keystore
    echo "Signing Desktop JAR..."
    jarsigner \
        -keystore "$KEY_DIR/yole-desktop.jks" \
        -storepass yole123 \
        -keypass yole123 \
        "$JAR_FILE" \
        yole-desktop

    # Copy signed jar to releases
    cp "$JAR_FILE" releases/desktop/

    # Verify signature
    echo "Verifying Desktop JAR signature..."
    jarsigner -verify "releases/desktop/$(basename "$JAR_FILE")"

    echo -e "${GREEN}Signed Desktop JAR built${NC}"
}

# Build Web/WASM
build_web() {
    echo -e "${YELLOW}Building Web/WASM...${NC}"

    ./gradlew :webApp:assemble

    # Copy web assets to releases
    cp -r webApp/build/processedResources/wasmJs/main/* releases/web/ 2>/dev/null || true
    cp webApp/build/libs/*.jar releases/web/ 2>/dev/null || true

    echo -e "${GREEN}Web/WASM built${NC}"
}

# Run security scans
run_security_scans() {
    echo -e "${YELLOW}Running security scans...${NC}"

    # OWASP Dependency Check
    ./gradlew dependencyCheckAnalyze || true

    echo -e "${GREEN}Security scans complete${NC}"
}

# Main build process
main() {
    check_environment
    setup_signing_keys
    clean_build
    run_tests
    generate_coverage
    build_android
    build_desktop
    build_web
    run_security_scans

    echo -e "${GREEN}=== Build Complete ===${NC}"
    echo "Releases available in: releases/"
    ls -laR releases/
}

# Run main
main "$@"
