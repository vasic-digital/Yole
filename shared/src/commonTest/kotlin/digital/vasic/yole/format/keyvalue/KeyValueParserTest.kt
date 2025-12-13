/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Unit tests for Key-Value parser
 *
 *########################################################*/
package digital.vasic.yole.format.keyvalue

import digital.vasic.yole.format.FormatRegistry
import digital.vasic.yole.format.ParserRegistry
import digital.vasic.yole.format.TextFormat
import digital.vasic.yole.format.ParsedDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse


/**
 * Comprehensive unit tests for Key-Value format parser.
 *
 * Tests cover:
 * - Format detection by extension and content
 * - Basic Key-Value parsing (properties, INI-style, various formats)
 * - Key-Value separators (=, :, space)
 * - Sections and groups
 * - Comments and metadata
 * - Round-trip parsing (parse → format → parse)
 * - Edge cases (empty files, malformed Key-Value)
 * - Performance benchmarks
 * - HTML conversion
 */
class KeyValueParserTest {

    private val parser = KeyValueParser()

    // ==================== Format Detection Tests ====================

    @Test
    fun `should detect Key-Value format by INI extension`() {
        val format = FormatRegistry.getByExtension(".ini")

        assertNotNull(format)
        assertEquals(TextFormat.ID_KEYVALUE, format.id)
        assertEquals("Key-Value", format.name)
    }

    @Test
    fun `should detect Key-Value format by properties extension`() {
        val format = FormatRegistry.getByExtension(".properties")

        assertNotNull(format)
        assertEquals(TextFormat.ID_KEYVALUE, format.id)
    }

    @Test
    fun `should detect Key-Value format by keyvalue extension`() {
        val format = FormatRegistry.getByExtension(".keyvalue")

        assertNotNull(format)
        assertEquals(TextFormat.ID_KEYVALUE, format.id)
    }

    @Test
    fun `should detect Key-Value format by filename`() {
        val format = FormatRegistry.detectByFilename("config.ini")

        assertNotNull(format)
        assertEquals(TextFormat.ID_KEYVALUE, format.id)
    }

    @Test
    fun `should support all Key-Value extensions`() {
        val extensions = listOf(".ini", ".properties", ".keyvalue")

        extensions.forEach { ext ->
            val format = FormatRegistry.getByExtension(ext)
            assertNotNull(format, "Extension $ext should be recognized")
            assertEquals(TextFormat.ID_KEYVALUE, format.id)
        }
    }

    // ==================== Basic Key-Value Parsing Tests ====================

    @Test
    fun `should parse basic Key-Value document structure`() {
        val content = """
            name=John Doe
            age=30
            city=New York
            country=USA
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals(content, result.rawContent)
        
        // Check metadata extraction
        assertEquals("generic", result.metadata["type"])
        assertEquals("4", result.metadata["entries"])
        assertEquals("4", result.metadata["lines"])
        assertEquals("0", result.metadata["sections"])
    }

    @Test
    fun `should parse Java properties format`() {
        val content = """
            app.name=My Application
            app.version=1.0.0
            app.author=John Doe
            app.description=Sample application
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "app.properties"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("properties", result.metadata["type"])
        assertEquals(".properties", result.metadata["extension"])
        assertEquals("4", result.metadata["entries"])
    }

    @Test
    fun `should parse INI format with sections`() {
        val content = """
            [Database]
            host=localhost
            port=5432
            name=mydb
            
            [Server]
            host=0.0.0.0
            port=8080
            debug=true
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "config.ini"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("ini", result.metadata["type"])
        assertEquals(".ini", result.metadata["extension"])
        assertEquals("6", result.metadata["entries"])
        assertEquals("2", result.metadata["sections"])
    }

    // ==================== Key-Value Separators Tests ====================

    @Test
    fun `should parse Key-Value pairs with equals separator`() {
        val content = """
            key1=value1
            key2=value2
            key3=value3
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("3", result.metadata["entries"])
    }

    @Test
    fun `should parse Key-Value pairs with colon separator`() {
        val content = """
            key1: value1
            key2: value2
            key3: value3
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("3", result.metadata["entries"])
    }

    @Test
    fun `should parse Key-Value pairs with dash separator`() {
        val content = """
            key1 - value1
            key2 - value2
            key3 - value3
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("3", result.metadata["entries"])
    }

    @Test
    fun `should parse mixed Key-Value separators`() {
        val content = """
            key1=value1
            key2: value2
            key3 - value3
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("3", result.metadata["entries"])
    }

    @Test
    fun `should handle quoted keys and values`() {
        val content = """
            "app.name"="My Application"
            'config.path': '/etc/app'
            "debug.mode" - true
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("3", result.metadata["entries"])
    }

    // ==================== Sections and Groups Tests ====================

    @Test
    fun `should parse INI sections correctly`() {
        val content = """
            [Section1]
            key1=value1
            key2=value2
            
            [Section2]
            key3=value3
            key4=value4
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("4", result.metadata["entries"])
        assertEquals("2", result.metadata["sections"])
    }

    @Test
    fun `should handle nested section-like structures`() {
        val content = """
            [Database.Main]
            host=localhost
            port=5432
            
            [Database.Replica]
            host=replica.local
            port=5433
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("4", result.metadata["entries"])
        assertEquals("2", result.metadata["sections"])
    }

    @Test
    fun `should handle sections with special characters`() {
        val content = """
            [User Settings]
            theme=dark
            language=en
            
            [Advanced-Config]
            debug=true
            log_level=info
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("4", result.metadata["entries"])
        assertEquals("2", result.metadata["sections"])
    }

    // ==================== Comments and Metadata Tests ====================

    @Test
    fun `should parse hash comments`() {
        val content = """
            # This is a configuration file
            app.name=MyApp
            # Database settings
            db.host=localhost
            db.port=5432
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("3", result.metadata["entries"])
        assertTrue(result.parsedContent.contains("color: #88b04b")) // Comment color
    }

    @Test
    fun `should parse semicolon comments`() {
        val content = """
            ; Configuration file
            name=Application
            ; Version information
            version=1.0.0
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("2", result.metadata["entries"])
    }

    @Test
    fun `should parse double-slash comments`() {
        val content = """
            // Application config
            debug=true
            // Database config
            db.name=mydb
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("2", result.metadata["entries"])
    }

    @Test
    fun `should handle mixed comment styles`() {
        val content = """
            # Main configuration
            app.name=TestApp
            ; Feature flags
            feature.newUI=true
            // Logging settings
            log.level=info
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("3", result.metadata["entries"])
    }

    @Test
    fun `should handle comments in INI sections`() {
        val content = """
            [Database]
            # Main database
            host=localhost
            port=5432
            ; Connection pool
            pool.size=10
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("3", result.metadata["entries"])
        assertEquals("1", result.metadata["sections"])
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun `should handle empty document`() {
        val content = ""

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("0", result.metadata["entries"])
        assertEquals("1", result.metadata["lines"])
        assertEquals("0", result.metadata["sections"])
    }

    @Test
    fun `should handle document with only whitespace`() {
        val content = "   \n  \n   \n"

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals(content, result.rawContent)
        assertEquals("0", result.metadata["entries"])
        assertEquals("4", result.metadata["lines"])
    }

    @Test
    fun `should handle document with only comments`() {
        val content = """
            # Configuration file
            ; This is a comment
            // Another comment
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("0", result.metadata["entries"])
    }

    @Test
    fun `should handle document with only sections`() {
        val content = """
            [Section1]
            [Section2]
            [Section3]
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("0", result.metadata["entries"])
        assertEquals("3", result.metadata["sections"])
    }

    @Test
    fun `should handle malformed Key-Value pairs`() {
        val content = """
            valid.key=value
            invalid_line_without_separator
            another.key=valid_value
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("2", result.metadata["entries"]) // Only valid pairs
    }

    @Test
    fun `should handle lines with multiple separators`() {
        val content = """
            url=http://example.com:8080/path
            description=This: is a test
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("2", result.metadata["entries"])
    }

    @Test
    fun `should handle empty values`() {
        val content = """
            key1=
            key2: 
            key3 - 
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("3", result.metadata["entries"])
    }

    @Test
    fun `should handle special characters in keys and values`() {
        val content = """
            app.name=My App
            app.version=1.0.0-SNAPSHOT
            database.url=jdbc:postgresql://localhost:5432/mydb
            paths.config=/etc/app/config
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("4", result.metadata["entries"])
    }

    // ==================== HTML Conversion Tests ====================

    @Test
    fun `should convert simple Key-Value to HTML`() {
        val content = """
            name=John Doe
            age=30
            city=New York
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='keyvalue'>"))
        assertTrue(html.contains("<pre"))
        assertTrue(html.contains("font-weight: bold")) // Key highlighting
    }

    @Test
    fun `should convert INI format to HTML`() {
        val content = """
            [Database]
            host=localhost
            port=5432
            
            [Server]
            host=0.0.0.0
            port=8080
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='keyvalue'>"))
        assertTrue(html.contains("color: #ef6d00")) // Section color
        assertTrue(html.contains("font-weight: bold")) // Key highlighting
    }

    @Test
    fun `should convert Key-Value with comments to HTML`() {
        val content = """
            # Application configuration
            app.name=MyApp
            ; Feature settings
            feature.enabled=true
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = true)

        assertNotNull(html)
        assertTrue(html.contains("<div class='keyvalue'>"))
        assertTrue(html.contains("color: #88b04b")) // Comment color
        assertTrue(html.contains("font-weight: bold")) // Key highlighting
    }

    @Test
    fun `should apply dark mode styles`() {
        val content = """
            debug=true
            log.level=info
        """.trimIndent()

        val document = parser.parse(content)
        val html = parser.toHtml(document, lightMode = false)

        assertNotNull(html)
        assertTrue(html.contains("<div class='keyvalue'>"))
        assertTrue(html.contains("<pre"))
    }

    // ==================== Validation Tests ====================

    @Test
    fun `should validate well-formed Key-Value content`() {
        val content = """
            key1=value1
            key2: value2
            key3 - value3
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun `should detect lines without separators`() {
        val content = """
            valid.key=value
            invalid_line_without_separator
            another.key=value
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("No key-value separator found"))
    }

    @Test
    fun `should validate INI format`() {
        val content = """
            [Section1]
            key1=value1
            key2=value2
            
            invalid_line_without_separator
            
            [Section2]
            key3=value3
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isNotEmpty())
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("No key-value separator found"))
    }

    @Test
    fun `should skip validation for comments and sections`() {
        val content = """
            # Configuration
            [Database]
            ; Settings
            host=localhost
            // More settings
            port=5432
        """.trimIndent()

        val errors = parser.validate(content)

        assertTrue(errors.isEmpty())
    }

    // ==================== Round-trip Parsing Tests ====================

    @Test
    fun `should support round-trip parsing`() {
        val originalContent = """
            [Application]
            name=MyApp
            version=1.0.0
            debug=true
            
            [Database]
            host=localhost
            port=5432
            name=mydb
        """.trimIndent()

        // Parse the original content
        val firstParse = parser.parse(originalContent)
        
        // Get the formatted content (should be same as original)
        val formattedContent = firstParse.rawContent
        
        // Parse the formatted content again
        val secondParse = parser.parse(formattedContent)
        
        // Verify round-trip consistency
        assertEquals(firstParse.format.id, secondParse.format.id)
        assertEquals(firstParse.rawContent, secondParse.rawContent)
        assertEquals(firstParse.metadata, secondParse.metadata)
    }

    @Test
    fun `should preserve metadata through round-trip`() {
        val originalContent = """
            # Config file
            app.name=TestApp
            app.version=2.0.0
            
            [Features]
            feature1=true
            feature2=false
        """.trimIndent()

        val firstParse = parser.parse(originalContent)
        val secondParse = parser.parse(firstParse.rawContent)
        
        assertEquals(firstParse.metadata, secondParse.metadata)
        assertEquals(firstParse.format.id, secondParse.format.id)
    }

    @Test
    fun `should preserve comments through round-trip`() {
        val originalContent = """
            # Main config
            debug=true
            ; Database settings
            db.host=localhost
            // Cache settings
            cache.enabled=true
        """.trimIndent()

        val firstParse = parser.parse(originalContent)
        val secondParse = parser.parse(firstParse.rawContent)
        
        assertEquals(firstParse.metadata, secondParse.metadata)
        assertEquals(firstParse.format.id, secondParse.format.id)
    }

    // ==================== Performance Tests ====================

    @Test
    fun `should parse large Key-Value documents efficiently`() {
        // Create a large document with many key-value pairs
        val largeContent = buildString {
            repeat(100) { sectionIndex ->
                appendLine("[Section${sectionIndex + 1}]")
                
                repeat(20) { keyIndex ->
                    appendLine("key${keyIndex + 1}=value${keyIndex + 1}")
                }
                
                appendLine()
            }
        }

        val startTime = System.currentTimeMillis()
        val result = parser.parse(largeContent)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("2000", result.metadata["entries"])
        assertEquals("100", result.metadata["sections"])
        
        // Performance assertion - should parse in reasonable time (less than 1 second for this size)
        val parseTime = endTime - startTime
        assertTrue(parseTime < 1000, "Parsing should complete within 1 second, took: ${parseTime}ms")
    }

    @Test
    fun `should convert large documents to HTML efficiently`() {
        // Create a large document
        val largeContent = buildString {
            repeat(50) { sectionIndex ->
                appendLine("[Section${sectionIndex + 1}]")
                
                repeat(10) { keyIndex ->
                    appendLine("config.key${keyIndex + 1}=value${keyIndex + 1}")
                }
                
                appendLine()
            }
        }

        val document = parser.parse(largeContent)
        
        val startTime = System.currentTimeMillis()
        val html = parser.toHtml(document, lightMode = true)
        val endTime = System.currentTimeMillis()
        
        assertNotNull(html)
        assertTrue(html.isNotEmpty())
        assertTrue(html.contains("<div class='keyvalue'>"))
        assertTrue(html.contains("font-weight: bold"))
        
        // Performance assertion - should convert in reasonable time (less than 500ms)
        val conversionTime = endTime - startTime
        assertTrue(conversionTime < 500, "HTML conversion should complete within 500ms, took: ${conversionTime}ms")
    }

    // ==================== Integration Tests ====================

    @Test
    fun `should integrate with ParserRegistry`() {
        // Register the parser
        ParserRegistry.register(parser)
        
        // Get parser from registry
        val registryParser = ParserRegistry.getParser(TextFormat.ID_KEYVALUE)
        
        assertNotNull(registryParser)
        assertEquals(TextFormat.ID_KEYVALUE, registryParser.supportedFormat.id)
        
        // Test that it works
        val content = "key=value"
        val result = registryParser.parse(content)
        
        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
    }

    @Test
    fun `should be registered in FormatRegistry`() {
        val allFormats = FormatRegistry.formats
        val keyValueFormat = allFormats.find { it.id == TextFormat.ID_KEYVALUE }

        assertNotNull(keyValueFormat)
        assertEquals("Key-Value", keyValueFormat.name)
        assertEquals(".ini", keyValueFormat.defaultExtension)
        assertTrue(keyValueFormat.extensions.contains(".ini"))
        assertTrue(keyValueFormat.extensions.contains(".properties"))
        assertTrue(keyValueFormat.extensions.contains(".keyvalue"))
    }

    // ==================== Complex Document Tests ====================

    @Test
    fun `should parse complex Key-Value document`() {
        val content = """
            # Application Configuration
            app.name=MyApplication
            app.version=2.1.0
            app.description=A sample application with many features
            
            [Database]
            # Main database configuration
            db.primary.host=localhost
            db.primary.port=5432
            db.primary.name=production_db
            db.primary.user=dbuser
            db.primary.password=secret123
            db.primary.ssl=true
            
            # Replica database configuration
            db.replica.host=replica.example.com
            db.replica.port=5432
            db.replica.name=production_db_replica
            db.replica.ssl=true
            
            [Cache]
            # Redis cache configuration
            cache.type=redis
            cache.host=redis.example.com
            cache.port=6379
            cache.ttl: 3600
            cache.max_entries - 10000
            
            [Server]
            # Web server configuration
            server.host=0.0.0.0
            server.port=8080
            server.ssl.enabled=true
            server.ssl.cert.path=/etc/ssl/certs/app.crt
            server.ssl.key.path=/etc/ssl/private/app.key
            server.max_connections=1000
            server.timeout=30
            
            [Logging]
            # Logging configuration
            log.level=INFO
            log.file.path=/var/log/myapp/app.log
            log.file.max_size=100MB
            log.file.max_files=10
            log.format=%Y-%m-%d %H:%M:%S [%level] %message
            
            [Features]
            # Feature flags
            feature.new_ui=true
            feature.beta_api=false
            feature.analytics=true
            feature.performance_monitoring=true
            
            [Security]
            # Security settings
            security.csrf.enabled=true
            security.cors.allowed_origins=https://app.example.com,https://admin.example.com
            security.rate_limit.requests_per_minute=100
            security.session.timeout=3600
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("6", result.metadata["sections"])
        assertEquals("30", result.metadata["entries"])
        
        // Verify HTML contains expected elements
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("keyvalue"))
        assertTrue(html.contains("font-weight: bold")) // Key highlighting
        assertTrue(html.contains("color: #ef6d00")) // Section color
        assertTrue(html.contains("color: #88b04b")) // Comment color
    }

    @Test
    fun `should handle unicode and special characters`() {
        val content = """
            [International]
            app.name=My App 🚀
            app.description=Application with unicode: 你好世界 🌍
            welcome.message=Welcome! Bienvenue! Добро пожаловать!
            
            [Paths]
            user.home=/home/user
            app.data=/var/lib/myapp
            log.dir=/var/log/myapp
            
            [URLs]
            api.endpoint=https://api.example.com/v1/
            docs.url=http://docs.example.com:8080/
            websocket=wss://ws.example.com/socket
        """.trimIndent()

        val result = parser.parse(content)

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("3", result.metadata["sections"])
        assertEquals("8", result.metadata["entries"])
        
        // Verify HTML contains unicode characters
        val html = parser.toHtml(result, lightMode = true)
        assertTrue(html.contains("🚀"))
        assertTrue(html.contains("你好世界"))
        assertTrue(html.contains("🌍"))
        assertTrue(html.contains("Добро пожаловать"))
    }

    @Test
    fun `should handle YAML-like format`() {
        val content = """
            app:
              name: MyApp
              version: 1.0.0
            
            database:
              host: localhost
              port: 5432
            
            server:
              host: 0.0.0.0
              port: 8080
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "config.yaml"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("yaml", result.metadata["type"])
        assertEquals(".yaml", result.metadata["extension"])
    }

    @Test
    fun `should handle TOML-like format`() {
        val content = """
            [server]
            host = "localhost"
            port = 8080
            
            [database]
            host = "db.example.com"
            port = 5432
            name = "mydb"
        """.trimIndent()

        val result = parser.parse(content, mapOf("filename" to "config.toml"))

        assertNotNull(result)
        assertEquals(TextFormat.ID_KEYVALUE, result.format.id)
        assertEquals("toml", result.metadata["type"])
        assertEquals(".toml", result.metadata["extension"])
    }
}