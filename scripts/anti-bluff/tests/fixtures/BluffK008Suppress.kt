package fixtures
import kotlin.test.Test
import kotlin.test.assertTrue
class BluffK008Suppress {
    @Test
    @Suppress("BLUFF-K-002")
    fun suppressedBluff() {
        assertTrue(true)
    }
}
