// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: TypeScript.

interface Greetable {
  greet(): string;
}

class Greeter implements Greetable {
  constructor(private readonly name: string) {}

  greet(): string {
    return `Hello, ${this.name}!`;
  }
}

function main(): void {
  const g: Greetable = new Greeter("Yole");
  console.log(g.greet());
}

main();
