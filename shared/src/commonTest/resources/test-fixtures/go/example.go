// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: Go.

package main

import "fmt"

type Greeter struct {
	Name string
}

func (g Greeter) Greet() string {
	return fmt.Sprintf("Hello, %s!", g.Name)
}

func main() {
	g := Greeter{Name: "Yole"}
	fmt.Println(g.Greet())
}
