// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: Rust.

struct Greeter {
    name: String,
}

impl Greeter {
    fn new(name: &str) -> Self {
        Greeter { name: name.to_string() }
    }

    fn greet(&self) -> String {
        format!("Hello, {}!", self.name)
    }
}

fn main() {
    let g = Greeter::new("Yole");
    println!("{}", g.greet());
}
