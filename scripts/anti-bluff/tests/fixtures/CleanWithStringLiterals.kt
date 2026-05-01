package fixtures
import kotlin.test.Test
import kotlin.test.assertEquals

// Negative regression fixture: this test contains the literal substring
// `assertTrue(true)` and `@Ignore` and `assumeTrue(false)` INSIDE STRING
// LITERALS but never executes any of them. The scanner must NOT flag
// this file — those substrings are documentation/log content, not
// real bluff.
class CleanWithStringLiterals {
    @Test
    fun describesBluffPatternsInLogsButDoesNotExecuteThem() {
        val description = "this test does not call assertTrue(true) or @Ignore"
        val moreDescription = """
            assumeTrue(false) is mentioned here but never invoked.
            Same with @Ignore — only as a string literal.
        """.trimIndent()
        // Real assertion exercising the SUT (string concat).
        assertEquals(
            "this test does not call assertTrue(true) or @Ignore",
            description,
            "constructed description must match"
        )
        assertEquals(true, moreDescription.contains("assumeTrue"), "moreDescription must contain the keyword string")
    }
}
