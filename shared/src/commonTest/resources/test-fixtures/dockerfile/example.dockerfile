# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: Dockerfile.

FROM eclipse-temurin:17-jdk

LABEL org.opencontainers.image.title="Yole"
LABEL org.opencontainers.image.version="1.0.1"

WORKDIR /opt/yole
COPY . /opt/yole
RUN ./gradlew :shared:desktopTest --no-daemon

ENTRYPOINT ["./gradlew", ":desktopApp:run"]
CMD []
