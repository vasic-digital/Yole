# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: R.

greeter <- function(name) {
  list(
    name = name,
    greet = function() paste0("Hello, ", name, "!")
  )
}

main <- function() {
  g <- greeter("Yole")
  print(g$greet())
}

main()
