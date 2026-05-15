// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: Swift.

import Foundation

struct Greeter {
    let name: String

    func greet() -> String {
        return "Hello, \(name)!"
    }
}

let g = Greeter(name: "Yole")
print(g.greet())
