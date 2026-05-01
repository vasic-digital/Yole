package fixtures
import kotlin.test.Test
class BluffK004Assume {
    @Test fun unconditionallySkipped() {
        // assumeTrue(false) is an unconditional skip — bluff
        assumeTrue(false)
    }
}
private fun assumeTrue(condition: Boolean) { if (!condition) throw AssertionError() }
