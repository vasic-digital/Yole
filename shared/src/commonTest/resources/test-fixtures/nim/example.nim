# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: Nim.

type
  Greeter = object
    name: string

proc greet(g: Greeter): string =
  "Hello, " & g.name & "!"

when isMainModule:
  let g = Greeter(name: "Yole")
  echo greet(g)
