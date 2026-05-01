package fixtures
import kotlin.test.Test
import kotlin.test.assertEquals
class Clean {
    @Test fun adds() {
        val got = 1 + 1
        assertEquals(2, got, "1+1 should be 2")
    }
}
