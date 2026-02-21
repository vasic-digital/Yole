# Yole Build Environment
# Complete build environment with Android SDK, Java, Gradle, and all dependencies

FROM ubuntu:22.04

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV GRADLE_HOME=/opt/gradle
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Install system dependencies
RUN apt-get update && apt-get install -y \
    # Java 17
    openjdk-17-jdk \
    # Build tools
    wget \
    curl \
    unzip \
    git \
    # Android dependencies
    libc6:i386 \
    libncurses5:i386 \
    libstdc++6:i386 \
    lib32gcc-s1 \
    lib32stdc++6 \
    # Desktop dependencies
    libxcb1 \
    libxcb-xkb1 \
    libxkbcommon-x11-0 \
    libgl1-mesa-glx \
    libglib2.0-0 \
    # Additional
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# Install Gradle
RUN wget -q https://services.gradle.org/distributions/gradle-8.11.1-bin.zip -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    ln -s /opt/gradle-8.11.1/bin/gradle /usr/local/bin/gradle && \
    rm /tmp/gradle.zip

# Install Android SDK
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    cd $ANDROID_SDK_ROOT/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip && \
    unzip -q cmdline-tools.zip && \
    mv cmdline-tools latest && \
    rm cmdline-tools.zip

# Accept licenses and install SDK components
RUN yes | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses || true
RUN $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --install \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0" \
    "emulator" \
    "system-images;android-35;google_apis;x86_64"

# Create Android emulator
RUN echo "no" | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager create avd --name yole_emulator \
    --package "system-images;android-35;google_apis;x86_64" \
    --device "pixel_5" || true

# Configure Android SDK environment
ENV PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator"

# Set working directory
WORKDIR /workspace

# Create release directory
RUN mkdir -p /workspace/releases

# Default command
CMD ["/bin/bash"]
