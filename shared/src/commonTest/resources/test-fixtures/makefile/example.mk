# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: Makefile.

.PHONY: all clean test

PLATFORMS := android desktop ios web

all: $(PLATFORMS)

android:
	@echo "Building android..."

desktop:
	@echo "Building desktop..."

ios:
	@echo "Building ios..."

web:
	@echo "Building web..."

test:
	./gradlew :shared:desktopTest

clean:
	./gradlew clean
