# SPDX-FileCopyrightText: 2026 Milos Vasic
# SPDX-License-Identifier: Apache-2.0
# iter-58 F2 Phase 6 fixture: Python.

class Greeter:
    """A small greeter for fixture purposes."""

    def __init__(self, name: str) -> None:
        self.name = name

    def greet(self) -> str:
        return f"Hello, {self.name}!"


def main() -> int:
    g = Greeter("Yole")
    print(g.greet())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
