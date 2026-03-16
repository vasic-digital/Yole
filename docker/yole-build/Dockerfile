# Yole Build Environment - Fast Build Version
# Uses pre-built Android SDK image for faster builds

FROM ubuntu:22.04

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV GRADLE_HOME=/opt/gradle
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Install system dependencies
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    curl \
    unzip \
    git \
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# Install Gradle
RUN wget -q https://services.gradle.org/distributions/gradle-8.11.1-bin.zip -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    ln -s /opt/gradle-8.11.1/bin/gradle /usr/local/bin/gradle && \
    rm /tmp/gradle.zip

# Download and extract official Android command-line tools
RUN mkdir -p $ANDROID_SDK_ROOT && \
    cd /tmp && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip && \
    unzip -q cmdline-tools.zip && \
    mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    mv cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && \
    rm cmdline-tools.zip

# Accept licenses
RUN yes | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses || true

# Install minimal SDK components
RUN $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --install \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0" || true

ENV PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"

WORKDIR /workspace

RUN mkdir -p /workspace/releases

CMD ["/bin/bash"]
