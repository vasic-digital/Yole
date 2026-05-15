#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: Bash.
set -euo pipefail

greet() {
  local name="$1"
  echo "Hello, ${name}!"
}

main() {
  for target in android desktop ios web; do
    greet "$target"
  done
}

main "$@"
