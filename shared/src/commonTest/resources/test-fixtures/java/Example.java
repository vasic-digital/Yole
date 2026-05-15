// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: Java.

package digital.vasic.yole.fixtures;

public class Example {
    private final String name;

    public Example(String name) {
        this.name = name;
    }

    public String greet() {
        return "Hello, " + name + "!";
    }

    public static void main(String[] args) {
        System.out.println(new Example("Yole").greet());
    }
}
