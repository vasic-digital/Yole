// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: C++.

#include <iostream>
#include <string>

class Greeter {
public:
    explicit Greeter(std::string name) : name_(std::move(name)) {}

    std::string greet() const {
        return "Hello, " + name_ + "!";
    }

private:
    std::string name_;
};

int main() {
    Greeter g("Yole");
    std::cout << g.greet() << std::endl;
    return 0;
}
