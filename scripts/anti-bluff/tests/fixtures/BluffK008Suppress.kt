package fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
class BluffK008Suppress {
    @Test
    @Suppress("BLUFF-K-002")
    fun suppressedBluff() {
        // @Suppress is for the Kotlin compiler, not for the bluff scanner.
        // The fixture's job is to verify that BLUFF-K-008 fires when a
        // @Suppress("BLUFF-...") annotation has no exempt-marker
        // justification on the line above. The body is otherwise clean
        // (no BLUFF-K-002 trigger) so this fixture isolates BLUFF-K-008.
        val a = 1 + 1
        assertEquals(2, a)
    }
}
