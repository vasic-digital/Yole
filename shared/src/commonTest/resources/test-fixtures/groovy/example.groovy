// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: Groovy.

class Greeter {
    String name

    Greeter(String name) {
        this.name = name
    }

    String greet() {
        return "Hello, ${name}!"
    }
}

def g = new Greeter("Yole")
println g.greet()
