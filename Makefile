# SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
# SPDX-FileCopyrightText: 2025-2026 Milos Vasic
# SPDX-License-Identifier: CC0-1.0
#
# Yole - Cross-platform text editor
# Build automation for Android, Desktop, Web, and container-based workflows
#
# Usage:
#   make help              - Show all available targets
#   make build             - Build Android APK (requires ANDROID_SDK_ROOT)
#   make desktop           - Run desktop application
#   make web               - Run web application in browser
#   make test-shared       - Run shared module tests (desktop JVM)
#   make container-test    - Run all tests inside a container
#
.PHONY: $(shell sed -n -e '/^$$/ { n ; /^[^ .\#][^ ]*:/ { s/:.*$$// ; p ; } ; }' $(MAKEFILE_LIST))
.NOTPARALLEL: clean
.DEFAULT_GOAL := help

####################################################################################
# Configuration
####################################################################################

DIST_DIR = dist
FLAVOR := $(or ${FLAVOR},${FLAVOR},Atest)
ANDROID_BUILD_TOOLS := $(shell test -n "$$ANDROID_SDK_ROOT" && find "$${ANDROID_SDK_ROOT}/build-tools" -iname "aapt" 2>/dev/null | sort -r | head -n1 | xargs dirname 2>/dev/null)
TOOL_SPELLCHECKING_ISPELL := $(shell command -v ispell 2> /dev/null)

# Container runtime: prefer podman, fall back to docker
CONTAINER_RUNTIME := $(shell command -v podman 2>/dev/null || command -v docker 2>/dev/null)
COMPOSE_CMD := $(shell command -v podman-compose 2>/dev/null || echo "$(CONTAINER_RUNTIME) compose")

####################################################################################
# Help
####################################################################################

# Show all available targets with descriptions
help:
	@echo "Yole Build System"
	@echo "================="
	@echo ""
	@echo "Cross-Platform Targets:"
	@echo "  make build              Build Android APK (requires ANDROID_SDK_ROOT)"
	@echo "  make desktop            Run desktop (JVM) application"
	@echo "  make web                Run web (Wasm) application in browser"
	@echo ""
	@echo "Testing Targets:"
	@echo "  make test-shared        Run shared module tests (desktop JVM, no Android SDK needed)"
	@echo "  make test-android       Run Android unit tests (requires ANDROID_SDK_ROOT)"
	@echo "  make test               Legacy: Run Android flavor tests (requires ANDROID_SDK_ROOT)"
	@echo "  make test-all           Run all tests across all modules"
	@echo "  make coverage           Run tests with Kover HTML coverage report"
	@echo "  make test-coverage      Alias for coverage"
	@echo ""
	@echo "Container Targets (recommended for CI):"
	@echo "  make container-build    Build the container image"
	@echo "  make container-test     Run all tests inside a container"
	@echo "  make container-release  Build release artifacts inside a container"
	@echo "  make container-shell    Open an interactive shell in the build container"
	@echo ""
	@echo "Security Targets:"
	@echo "  make security           Start SonarQube for code quality analysis"
	@echo "  make security-full      Start full security stack (SonarQube + Snyk + Detekt)"
	@echo "  make security-scan      Run Detekt static analysis"
	@echo "  make detekt             Alias for security-scan"
	@echo "  make security-stop      Stop all security services"
	@echo ""
	@echo "Code Quality Targets:"
	@echo "  make lint               Run Android lint (requires ANDROID_SDK_ROOT)"
	@echo "  make spellcheck         Spellcheck strings.xml with ispell"
	@echo "  make docs               Generate Dokka HTML API documentation"
	@echo "  make dokka              Alias for docs"
	@echo ""
	@echo "Android Device Targets (require ANDROID_SDK_ROOT):"
	@echo "  make install            Install APK on connected device"
	@echo "  make run                Launch app on connected device"
	@echo ""
	@echo "Submodule Targets:"
	@echo "  make challenge          Run Challenges test suite (Go)"
	@echo "  make helixqa            Run HelixQA orchestrated QA (all platforms)"
	@echo "  make helixqa-test       Run HelixQA unit/integration tests"
	@echo "  make qa-all             Run all QA: shared + challenges + HelixQA"
	@echo ""
	@echo "Utility Targets:"
	@echo "  make clean              Clean build artifacts and caches"
	@echo "  make clean-deep         Deep clean including Gradle caches"
	@echo "  make submodules         Initialize and update git submodules"
	@echo "  make bootstrap          One-time setup for fresh clone (installs anti-bluff pre-commit hook + initialises submodules)"
	@echo ""
	@echo "Environment Variables:"
	@echo "  ANDROID_SDK_ROOT        Path to Android SDK (required for Android targets)"
	@echo "  FLAVOR                  Build flavor (default: Atest)"
	@echo ""

####################################################################################
# Internal helpers
####################################################################################

env-%:
	@: $(if ${${*}},,$(error Environment variable $* not set))

# One-time setup for a fresh clone. Idempotent — safe to re-run.
#
# Two-tier submodule strategy: top-level submodules (Challenges,
# Containers, HelixQA) are initialised non-recursively so we always
# get the project's anti-bluff infrastructure even when deeply nested
# transitive submodules in HelixQA's tools/opensource/ tree have
# broken pointer SHAs (which they do — see HelixQA's vendoring of
# external open-source tools). Recursive init is then attempted but
# its failure does not abort the bootstrap.
.PHONY: bootstrap
bootstrap:
	@echo "[bootstrap] Initialising top-level git submodules (non-recursive)…"
	@git submodule update --init Challenges Containers HelixQA
	@echo "[bootstrap] Attempting recursive submodule init (failures non-fatal)…"
	@git submodule update --init --recursive 2>&1 | tail -3 || \
		echo "[bootstrap] Recursive init had errors — top-level submodules are present, continuing."
	@echo "[bootstrap] Installing anti-bluff pre-commit hook in main repo (CONST-035)…"
	@bash scripts/anti-bluff/install-hooks.sh
	@for sub in Challenges Containers HelixQA; do \
		if [ -x "$$sub/scripts/anti-bluff/install-hooks.sh" ]; then \
			echo "[bootstrap] Installing pre-commit hook in $$sub…"; \
			(cd "$$sub" && bash scripts/anti-bluff/install-hooks.sh); \
		else \
			echo "[bootstrap] WARNING: $$sub/scripts/anti-bluff/install-hooks.sh missing or not executable; skipping."; \
		fi; \
	done
	@echo "[bootstrap] Done. Run 'make qa-all' to verify the gates work."

$(DIST_DIR):
	mkdir -p ${DIST_DIR}

.NOTPARALLEL: gradle gradle-analyze-log

# Run a Gradle command with logging (requires ANDROID_SDK_ROOT)
gradle: env-ANDROID_SDK_ROOT
	mkdir -p $(DIST_DIR)/log/
	chmod +x gradlew
	./gradlew --no-daemon --parallel --stacktrace $A  2>&1 | tee "$(DIST_DIR)/log/gradle.log"
	@echo "-----------------------------------------------------------------------------------"

# Run a Gradle command without requiring ANDROID_SDK_ROOT
gradle-no-sdk:
	mkdir -p $(DIST_DIR)/log/
	chmod +x gradlew
	./gradlew --no-daemon --parallel --stacktrace $A  2>&1 | tee "$(DIST_DIR)/log/gradle.log"
	@echo "-----------------------------------------------------------------------------------"

# Verify the last Gradle run was successful
gradle-analyze-log:
	mv  "$(DIST_DIR)/log/gradle.log" "$(DIST_DIR)/log/gradle$A.log"
	cat "$(DIST_DIR)/log/gradle$A.log" | grep "BUILD " | tail -n1 | grep -q "BUILD SUCCESSFUL in"

# Run adb command (requires ANDROID_SDK_ROOT)
adb: env-ANDROID_SDK_ROOT
	"${ANDROID_SDK_ROOT}/platform-tools/adb" $A 2>&1 | tee "$(DIST_DIR)/log/adb-$L.log"

# Run aapt command (requires ANDROID_SDK_ROOT)
aapt: env-ANDROID_SDK_ROOT
	"${ANDROID_BUILD_TOOLS}/aapt" $A 2>&1 | grep -v 'application-label-' | tee "$(DIST_DIR)/log/aapt$L.log"

####################################################################################
# Cross-Platform Targets
####################################################################################

# Build Android debug APK
build: $(DIST_DIR)
	rm -f $(DIST_DIR)/*.apk
	$(MAKE) A="clean :androidApp:assembleDebug -x lint" gradle
	find androidApp -type f -newermt '-300 seconds' -iname '*.apk' -not -iname '*unsigned.apk' | xargs cp -R -t $(DIST_DIR)/ 2>/dev/null || true
	@echo "Build artifacts in $(DIST_DIR)/"
	@echo "-----------------------------------------------------------------------------------"

# Run the desktop (JVM) application
desktop:
	chmod +x gradlew
	./gradlew --no-daemon :desktopApp:run

# Run the web (Wasm) application in the browser
web:
	chmod +x gradlew
	./gradlew --no-daemon :webApp:wasmJsBrowserRun

####################################################################################
# Testing Targets
####################################################################################

# Run shared module tests on desktop JVM (does not require Android SDK)
test-shared:
	chmod +x gradlew
	./gradlew --no-daemon :shared:desktopTest
	@echo "-----------------------------------------------------------------------------------"

# Run Android unit tests (requires ANDROID_SDK_ROOT)
test-android:
	$(MAKE) A=":androidApp:testDebugUnitTest -x lint" gradle
	@echo "-----------------------------------------------------------------------------------"

# Run all tests (requires ANDROID_SDK_ROOT) - legacy Android flavor tests
test: $(DIST_DIR)
	rm -Rf $(DIST_DIR)/tests
	$(MAKE) A="test -x lint" gradle
	@echo "-----------------------------------------------------------------------------------"

# Run all tests across all modules
test-all:
	chmod +x gradlew
	./gradlew --no-daemon test
	@echo "-----------------------------------------------------------------------------------"

# Run tests with Kover HTML coverage report
coverage:
	chmod +x gradlew
	./gradlew --no-daemon test koverHtmlReport
	@echo "Coverage report: shared/build/reports/kover/html/index.html"
	@echo "-----------------------------------------------------------------------------------"

# Alias: test-coverage -> coverage
test-coverage: coverage

####################################################################################
# Container Targets
####################################################################################

# Build the container image for the build environment
container-build:
	$(COMPOSE_CMD) build build

# Run all tests inside a container (consistent environment)
container-test:
	$(COMPOSE_CMD) run --rm build ./docker/scripts/test-all.sh

# Build release artifacts inside a container
container-release:
	$(COMPOSE_CMD) run --rm build ./docker/scripts/build.sh

# Run Robolectric Compose UI tests in a dedicated container, isolated from the
# main build pipeline. Enforces the anti-bluff covenant (CONST-035): the
# Robolectric tests must confirm that the Android UI actually renders and
# navigates as expected for an end user.
container-robolectric-test:
	$(COMPOSE_CMD) run --rm robolectric-test

# Open an interactive shell in the build container
container-shell:
	$(COMPOSE_CMD) run --rm build bash

####################################################################################
# Security Targets
####################################################################################

# Start SonarQube for code quality analysis (http://localhost:9000)
security:
	$(COMPOSE_CMD) --profile security up -d sonarqube
	@echo "SonarQube starting at http://localhost:9000 (default: admin/admin)"

# Start full security stack (SonarQube + Snyk + Detekt)
security-full:
	$(COMPOSE_CMD) --profile full up -d
	@echo "Full security stack started"

# Run Detekt static analysis
security-scan:
	chmod +x gradlew
	./gradlew --no-daemon detekt
	@echo "-----------------------------------------------------------------------------------"

# Alias: detekt -> security-scan
detekt: security-scan

# Stop all security services
security-stop:
	$(COMPOSE_CMD) --profile full down

####################################################################################
# Code Quality Targets
####################################################################################

# Run Android lint checks (requires ANDROID_SDK_ROOT)
lint: $(DIST_DIR)
	rm -Rf $(DIST_DIR)/lint
	mkdir -p $(DIST_DIR)/lint/
	$(MAKE) A="lintFlavorDefaultDebug" gradle
	find androidApp -type f -iname 'lint-results-*' | grep -v 'intermediates' | xargs cp -R -t $(DIST_DIR)/lint 2>/dev/null || true
	@echo "-----------------------------------------------------------------------------------"

# Spellcheck strings.xml using ispell
spellcheck: $(DIST_DIR)
	mkdir -p "$(DIST_DIR)/lint/"
ifndef TOOL_SPELLCHECKING_ISPELL
	@echo "Tool ispell (spellcheck) not found in PATH. Spellcheck skipped." > "$(DIST_DIR)/lint/stringsxml-spellcheck.txt"
else
	@echo "Use ispell for spellchecking the original values/strings.xml"
	find . -iname "strings.xml" -path "*/main*/values/*" | head -n1 | xargs cat \
	   | grep "<string name=" | sed 's@.*">@@' | sed 's@</string>@@' | sed 's@\\n@  @g' | sed 's@\\@@g'  \
	   | ispell -W3 -a | grep ^\& | sed 's@[0-9]@@g' | sort | uniq | cut -d, -f1-4 \
	   | sed 's@^..@- @' | column -t -s: \
	   > "$(DIST_DIR)/lint/stringsxml-spellcheck.txt"
	@echo "\nPotential words with bad spelling:"
endif
	@cat "$(DIST_DIR)/lint/stringsxml-spellcheck.txt"
	@echo "-----------------------------------------------------------------------------------"

# Generate API documentation with Dokka
docs:
	chmod +x gradlew
	./gradlew --no-daemon :shared:dokkaHtml
	@echo "API docs: shared/build/dokka/html/index.html"
	@echo "-----------------------------------------------------------------------------------"

# Alias: dokka -> docs
dokka: docs

####################################################################################
# Android Device Targets
####################################################################################

# Install APK on connected device via adb
install:
	$(MAKE) A="install -r $(DIST_DIR)/*.apk" L="install" adb

# Launch app on connected device via adb
run:
	$(MAKE) A="shell monkey -p $$(aapt dump badging $(DIST_DIR)/*.apk | grep package: | sed 's@.* name=@@' | sed 's@ .*@@' | xargs | head -n1) -c android.intent.category.LAUNCHER 1" L="run" adb

####################################################################################
# Submodule Targets
####################################################################################

# Run Challenges test suite (Go-based cross-platform challenges)
challenge:
	cd Challenges && go test ./... -race -count=1
	@echo "-----------------------------------------------------------------------------------"

# Run HelixQA orchestrated QA (main QA brain)
helixqa:
	cd HelixQA && go run ./cmd/helixqa/ run \
		--banks ../Challenges/banks/yole/,../HelixQA/banks/ \
		--platform all \
		--device emulator-5554 \
		--package digital.vasic.yole.android \
		--browser-url http://localhost:8080 \
		--desktop-process java \
		--output ../qa-results \
		--report markdown \
		--validate \
		--record \
		--tickets \
		--verbose \
		--timeout 45m
	@echo "-----------------------------------------------------------------------------------"

# Run HelixQA unit tests
helixqa-test:
	cd HelixQA && go test ./... -race -count=1
	@echo "-----------------------------------------------------------------------------------"

# Validate evidence from automation runs via HelixQA
helixqa-validate:
	bash automation/helixqa-validate.sh --platform all
	@echo "-----------------------------------------------------------------------------------"

# === CONST-035 anti-bluff gates ===
.PHONY: anti-bluff anti-bluff-scan anti-bluff-anchors anti-bluff-mutation anti-bluff-mutation-changed update-baseline

anti-bluff-scan:
	@bash scripts/anti-bluff/bluff-scanner.sh --mode all

anti-bluff-anchors:
	@bash challenges/scripts/anchor_manifest_challenge.sh

anti-bluff-mutation:
	@bash challenges/scripts/mutation_ratchet_challenge.sh

anti-bluff-mutation-changed:
	@bash challenges/scripts/mutation_ratchet_challenge.sh

anti-bluff: anti-bluff-scan anti-bluff-anchors anti-bluff-mutation

update-baseline:
	@echo "Manual baseline update — see docs/ANTI_BLUFF.md"
	@echo "1. Run scanner: bash scripts/anti-bluff/bluff-scanner.sh --mode all"
	@echo "2. Run mutation: bash challenges/scripts/mutation_ratchet_challenge.sh"
	@echo "3. Edit challenges/baselines/bluff-baseline.txt to reflect new state."

# Run full QA pipeline: unit tests + Go tests + automation + evidence validation + anti-bluff gates
qa-all: test-shared challenge helixqa-test anti-bluff
	bash automation/run-qa-all.sh --skip-unit --skip-build
	@echo "-----------------------------------------------------------------------------------"

# Run full QA pipeline including builds
qa-full:
	bash automation/run-qa-all.sh
	@echo "-----------------------------------------------------------------------------------"

####################################################################################
# Utility Targets
####################################################################################

# Clean build artifacts
clean:
	chmod +x gradlew
	./gradlew --no-daemon clean 2>/dev/null || true
	rm -Rf $(DIST_DIR) app/build androidApp/build shared/build desktopApp/build webApp/build .idea dist
	find . -type f -iname "*.iml" -delete
	mkdir -p $(DIST_DIR)
	@echo "-----------------------------------------------------------------------------------"

# Deep clean including Gradle caches
clean-deep: clean
	rm -Rf .gradle build
	@echo "Deep clean complete (Gradle caches removed)"
	@echo "-----------------------------------------------------------------------------------"

# Initialize and update git submodules
submodules:
	git submodule update --init --recursive
	@echo "Submodules initialized"
	@echo "-----------------------------------------------------------------------------------"
