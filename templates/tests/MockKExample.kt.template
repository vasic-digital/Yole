/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * MockK example tests - demonstrating mocking patterns
 *
 *########################################################*/
package digital.vasic.yole.examples

import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.assertNotNull

/**
 * Example tests using MockK for mocking dependencies.
 *
 * MockK is a powerful mocking library for Kotlin that provides:
 * - DSL for creating mocks
 * - Verification of calls
 * - Stubbing return values
 * - Spy on real objects
 * - Relaxed mocks
 */
class MockKExampleTest {

    // Mock object that will be reset before each test
    private lateinit var mockFileSystem: MockFileSystem

    @Before
    fun setup() {
        // Clear all mocks before each test
        clearAllMocks()

        // Create mock
        mockFileSystem = mockk<MockFileSystem>()
    }

    @After
    fun tearDown() {
        // Verify all mock interactions and clean up
        confirmVerified(mockFileSystem)
    }

    // ==================== Basic Mocking ====================

    @Test
    fun `should mock file read operation`() {
        // Arrange: Set up mock behavior
        every { mockFileSystem.readFile("test.txt") } returns "File content"

        // Act: Call method under test
        val content = mockFileSystem.readFile("test.txt")

        // Assert: Verify result
        assertThat(content).isEqualTo("File content")

        // Verify: Check method was called
        verify(exactly = 1) { mockFileSystem.readFile("test.txt") }
    }

    @Test
    fun `should mock file write operation`() {
        // Arrange: Mock returns nothing (Unit)
        every { mockFileSystem.writeFile("test.txt", "content") } just Runs

        // Act
        mockFileSystem.writeFile("test.txt", "content")

        // Verify
        verify { mockFileSystem.writeFile("test.txt", "content") }
    }

    @Test
    fun `should mock with multiple calls`() {
        // Arrange: Different return values for different calls
        every { mockFileSystem.readFile("test.txt") } returnsMany listOf(
            "First read",
            "Second read",
            "Third read"
        )

        // Act & Assert
        assertThat(mockFileSystem.readFile("test.txt")).isEqualTo("First read")
        assertThat(mockFileSystem.readFile("test.txt")).isEqualTo("Second read")
        assertThat(mockFileSystem.readFile("test.txt")).isEqualTo("Third read")

        verify(exactly = 3) { mockFileSystem.readFile("test.txt") }
    }

    // ==================== Argument Matching ====================

    @Test
    fun `should match any argument`() {
        // Arrange: Match any string argument
        every { mockFileSystem.readFile(any()) } returns "Generic content"

        // Act
        val content1 = mockFileSystem.readFile("file1.txt")
        val content2 = mockFileSystem.readFile("file2.txt")

        // Assert
        assertThat(content1).isEqualTo("Generic content")
        assertThat(content2).isEqualTo("Generic content")

        verify(exactly = 2) { mockFileSystem.readFile(any()) }
    }

    @Test
    fun `should match with predicate`() {
        // Arrange: Match files with .txt extension
        every {
            mockFileSystem.readFile(match { it.endsWith(".txt") })
        } returns "Text file content"

        every {
            mockFileSystem.readFile(match { it.endsWith(".md") })
        } returns "Markdown content"

        // Act
        val txtContent = mockFileSystem.readFile("document.txt")
        val mdContent = mockFileSystem.readFile("readme.md")

        // Assert
        assertThat(txtContent).isEqualTo("Text file content")
        assertThat(mdContent).isEqualTo("Markdown content")
    }

    // ==================== Exception Mocking ====================

    @Test(expected = java.io.IOException::class)
    fun `should throw exception when mocked`() {
        // Arrange: Mock throws exception
        every { mockFileSystem.readFile("missing.txt") } throws java.io.IOException("File not found")

        // Act: This should throw
        mockFileSystem.readFile("missing.txt")
    }

    @Test
    fun `should handle exceptions gracefully`() {
        // Arrange
        every { mockFileSystem.readFile("error.txt") } throws java.io.IOException("Read error")

        // Act: Catch exception and verify behavior
        val result = try {
            mockFileSystem.readFile("error.txt")
            "No error"
        } catch (e: java.io.IOException) {
            "Error caught: ${e.message}"
        }

        // Assert
        assertThat(result).isEqualTo("Error caught: Read error")
        verify { mockFileSystem.readFile("error.txt") }
    }

    // ==================== Spy Examples ====================

    @Test
    fun `should spy on real object`() {
        val realFileSystem = RealFileSystem()

        // Create spy - calls real methods unless mocked
        val spy = spyk(realFileSystem)

        // Mock only specific method
        every { spy.readFile("mocked.txt") } returns "Mocked content"

        // Act
        val mockedContent = spy.readFile("mocked.txt")
        val realContent = spy.readFile("real.txt") // Calls real method

        // Assert
        assertThat(mockedContent).isEqualTo("Mocked content")
        assertNotNull(realContent) // Real method executed

        verify { spy.readFile("mocked.txt") }
        verify { spy.readFile("real.txt") }
    }

    // ==================== Relaxed Mocks ====================

    @Test
    fun `should use relaxed mock`() {
        // Relaxed mock returns default values without explicit stubbing
        val relaxedMock = mockk<MockFileSystem>(relaxed = true)

        // Act: Call without stubbing
        val content = relaxedMock.readFile("any.txt")
        relaxedMock.writeFile("any.txt", "content")

        // Assert: Default return value (empty string for String)
        assertNotNull(content)

        // Verify calls were made
        verify { relaxedMock.readFile("any.txt") }
        verify { relaxedMock.writeFile("any.txt", "content") }

        confirmVerified(relaxedMock)
    }

    // ==================== Capture Arguments ====================

    @Test
    fun `should capture arguments`() {
        // Arrange
        val slot = slot<String>()
        every { mockFileSystem.writeFile("test.txt", capture(slot)) } just Runs

        // Act
        mockFileSystem.writeFile("test.txt", "captured content")

        // Assert: Verify captured argument
        assertThat(slot.captured).isEqualTo("captured content")
        verify { mockFileSystem.writeFile("test.txt", any()) }
    }

    @Test
    fun `should capture multiple arguments`() {
        // Arrange
        val contentSlots = mutableListOf<String>()
        every { mockFileSystem.writeFile("test.txt", capture(contentSlots)) } just Runs

        // Act
        mockFileSystem.writeFile("test.txt", "First write")
        mockFileSystem.writeFile("test.txt", "Second write")
        mockFileSystem.writeFile("test.txt", "Third write")

        // Assert
        assertThat(contentSlots).containsExactly(
            "First write",
            "Second write",
            "Third write"
        )
    }

    // ==================== Helper Classes ====================

    interface MockFileSystem {
        fun readFile(path: String): String
        fun writeFile(path: String, content: String)
        fun exists(path: String): Boolean
    }

    class RealFileSystem : MockFileSystem {
        override fun readFile(path: String): String = "Real content from $path"
        override fun writeFile(path: String, content: String) = Unit
        override fun exists(path: String): Boolean = true
    }
}
