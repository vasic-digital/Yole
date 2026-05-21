// SPDX-License-Identifier: CC0-1.0
// Anti-bluff self-test fixture (Phase 5B). NOT compiled into the build —
// the scanner fixtures directory is excluded from every source set.
//
// It documents the canonical shape of a test that leaves an obvious
// mutant alive: `add` could be mutated to subtraction, multiplication,
// or a hardcoded constant and `testAdd` would still pass, because
// 0 + 0 == 0 - 0 == 0 * 0 == 0. A real mutation pass over this code
// would report a SURVIVED arithmetic mutant — which is exactly the
// signal `mutation_ratchet_challenge.sh` ratchets against.
package fixtures

object SurvivingMutantBluff {
    fun add(a: Int, b: Int): Int = a + b
}

class SurvivingMutantBluffTest {
    // BLUFF: never distinguishes + from -, *, or a constant return.
    // A non-bluff test would assert add(2, 3) == 5.
    fun testAdd() {
        check(SurvivingMutantBluff.add(0, 0) == 0)
    }
}
