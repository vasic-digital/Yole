// SPDX-FileCopyrightText: 2026 Milos Vasic
// SPDX-License-Identifier: Apache-2.0
// iter-58 F2 Phase 6 fixture: Scala.

package digital.vasic.yole.fixtures

case class Greeter(name: String) {
  def greet: String = s"Hello, $name!"
}

object Example {
  def main(args: Array[String]): Unit = {
    println(Greeter("Yole").greet)
  }
}
