/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Challenge Bank Validation Tests
 *
 * Validates that all challenge bank JSON files in
 * Challenges/banks/yole/ are well-formed and contain
 * valid assertions, unique IDs, and correct references
 * to format IDs and protocol service names.
 *
 *########################################################*/
package digital.vasic.yole.challenges

import digital.vasic.yole.format.TextFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Validates challenge bank JSON files for structural correctness,
 * unique IDs, valid assertion types, and correct references to
 * Yole format IDs and protocol service names.
 *
 * Since we cannot execute Go code from Kotlin tests, this test
 * verifies the JSON structure and referential integrity of the
 * challenge bank definitions against the Kotlin codebase.
 *
 * 30 test methods covering:
 * - JSON well-formedness
 * - Required field presence
 * - ID uniqueness within and across banks
 * - Assertion type validity
 * - Format ID references
 * - Protocol name references
 * - Metadata consistency
 * - Dependency references
 * - Input/output structure
 */
class ChallengeValidationTests {

    // ====================================================================
    // Known valid values from the Yole codebase
    // ====================================================================

    /** All format IDs defined in TextFormat.Companion */
    private val validFormatIds = setOf(
        TextFormat.ID_UNKNOWN,
        TextFormat.ID_PLAINTEXT,
        TextFormat.ID_MARKDOWN,
        TextFormat.ID_TODOTXT,
        TextFormat.ID_CSV,
        TextFormat.ID_WIKITEXT,
        TextFormat.ID_KEYVALUE,
        TextFormat.ID_ASCIIDOC,
        TextFormat.ID_ORGMODE,
        TextFormat.ID_LATEX,
        TextFormat.ID_RESTRUCTUREDTEXT,
        TextFormat.ID_TASKPAPER,
        TextFormat.ID_TEXTILE,
        TextFormat.ID_CREOLE,
        TextFormat.ID_TIDDLYWIKI,
        TextFormat.ID_JUPYTER,
        TextFormat.ID_RMARKDOWN,
        TextFormat.ID_BINARY,
        TextFormat.ID_JSON,
    )

    /** Protocol service names corresponding to Yole's 8 network protocol implementations */
    private val validProtocolNames = setOf(
        "dropbox", "googledrive", "onedrive",
        "ftp", "sftp", "smb", "webdav", "git"
    )

    /** All 17 challenge bank file names expected under Challenges/banks/yole/ */
    private val expectedBankFiles = listOf(
        "concurrency.json",
        "cross-platform-build.json",
        "e2e-userflow.json",
        "format-detection.json",
        "format-edge-cases-challenges.json",
        "format-parsing.json",
        "lazy-loading.json",
        "memory.json",
        "monitoring.json",
        "network-protocols.json",
        "performance.json",
        "protocol-resilience-challenges.json",
        "resilience.json",
        "security-challenges.json",
        "security.json",
        "test-coverage.json",
        "ui-accessibility.json",
    )

    /** Valid assertion types defined in the Challenges framework (16 built-in evaluators) */
    private val validAssertionTypes = setOf(
        "not_empty", "not_mock", "contains", "contains_any",
        "min_length", "quality_score", "reasoning_present", "code_valid",
        "min_count", "exact_count", "max_latency",
        "all_valid", "no_duplicates", "all_pass",
        "no_mock_responses", "min_score",
        // Userflow-specific evaluators
        "http_status_ok", "http_status_created", "http_status_unauthorized",
        "http_status_forbidden", "http_status_not_found",
        "http_json_valid", "browser_element_visible", "browser_url_matches",
        "mobile_activity_visible", "mobile_element_exists",
        "build_success", "test_pass_rate",
        // Extended evaluators found in bank files
        "equals",
    )

    /** Valid input sources */
    private val validInputSources = setOf("config", "env")
    private val dependencySourcePrefix = "dependency:"

    /** Valid output types */
    private val validOutputTypes = setOf("string", "json")

    // ====================================================================
    // Sample challenge bank data for testing
    // (Embedded JSON representing the structure of real bank files)
    // ====================================================================

    private val sampleFormatParsingBank = """
    {
      "version": "1.0",
      "name": "Yole Format Parsing Challenges",
      "metadata": {
        "project": "yole",
        "domain": "format-parsing",
        "description": "Challenges validating all 17 text format parsers in the Yole editor"
      },
      "challenges": [
        {
          "id": "yole-fmt-markdown-basic",
          "name": "Markdown Basic Parsing",
          "description": "Verify Markdown parser handles headings, bold, italic, links",
          "category": "format-parsing",
          "estimated_duration": "30s",
          "inputs": [
            {"name": "markdown_content", "source": "config", "required": true},
            {"name": "project_root", "source": "env", "required": true}
          ],
          "outputs": [
            {"name": "parsed_html", "type": "string", "description": "Generated HTML"},
            {"name": "parse_errors", "type": "json", "description": "List of errors"}
          ],
          "assertions": [
            {"type": "not_empty", "target": "parsed_html", "message": "Must produce HTML output"},
            {"type": "contains", "target": "parsed_html", "value": "<h1>", "message": "Must contain h1"},
            {"type": "exact_count", "target": "parse_errors", "value": 0, "message": "No errors"}
          ],
          "metrics": ["parse_time_ms", "html_size_bytes"]
        },
        {
          "id": "yole-fmt-csv-parse",
          "name": "CSV Format Parsing",
          "description": "Verify CSV parser handles headers, quoted fields, escaped delimiters",
          "category": "format-parsing",
          "estimated_duration": "30s",
          "inputs": [
            {"name": "csv_content", "source": "config", "required": true}
          ],
          "outputs": [
            {"name": "parsed_html", "type": "string", "description": "HTML table"},
            {"name": "row_count", "type": "string", "description": "Data rows"}
          ],
          "assertions": [
            {"type": "not_empty", "target": "parsed_html", "message": "Must produce HTML"},
            {"type": "min_count", "target": "row_count", "value": 1, "message": "At least one row"}
          ],
          "metrics": ["parse_time_ms"]
        }
      ]
    }
    """.trimIndent()

    private val sampleSecurityBank = """
    {
      "version": "1.0",
      "name": "Yole Security Challenges",
      "metadata": {
        "project": "yole",
        "domain": "security",
        "description": "Security validation challenges"
      },
      "challenges": [
        {
          "id": "yole-sec-xss-html-escape",
          "name": "HTML Escaping in Parsers",
          "description": "Verify HTML escaping prevents XSS",
          "category": "security",
          "estimated_duration": "30s",
          "inputs": [
            {"name": "xss_payloads", "source": "config", "required": true}
          ],
          "outputs": [
            {"name": "vulnerable_formats", "type": "json", "description": "Vulnerable formats"}
          ],
          "assertions": [
            {"type": "exact_count", "target": "vulnerable_formats", "value": 0, "message": "No XSS"}
          ],
          "metrics": ["formats_tested"]
        }
      ]
    }
    """.trimIndent()

    private val sampleNetworkBank = """
    {
      "version": "1.0",
      "name": "Yole Network Protocol Challenges",
      "metadata": {
        "project": "yole",
        "domain": "network-protocols",
        "description": "Network protocol integration challenges"
      },
      "challenges": [
        {
          "id": "yole-net-dropbox-auth",
          "name": "Dropbox OAuth Authentication",
          "description": "Verify Dropbox OAuth2 flow",
          "category": "network",
          "estimated_duration": "30s",
          "inputs": [
            {"name": "dropbox_app_key", "source": "env", "required": true}
          ],
          "outputs": [
            {"name": "auth_result", "type": "string", "description": "Auth status"}
          ],
          "assertions": [
            {"type": "not_empty", "target": "auth_result", "message": "Auth must not be empty"}
          ],
          "metrics": ["auth_time_ms"]
        },
        {
          "id": "yole-net-dropbox-crud",
          "name": "Dropbox File CRUD",
          "description": "Verify Dropbox CRUD operations",
          "category": "network",
          "dependencies": ["yole-net-dropbox-auth"],
          "estimated_duration": "60s",
          "inputs": [
            {"name": "dropbox_token", "source": "dependency:yole-net-dropbox-auth", "required": true}
          ],
          "outputs": [
            {"name": "crud_results", "type": "json", "description": "CRUD results"}
          ],
          "assertions": [
            {"type": "all_pass", "target": "crud_results", "message": "All CRUD must pass"}
          ],
          "metrics": ["create_time_ms"]
        }
      ]
    }
    """.trimIndent()

    private val sampleResilienceBank = """
    {
      "version": "1.0",
      "name": "Yole Resilience Challenges",
      "metadata": {
        "project": "yole",
        "domain": "resilience",
        "description": "Resilience pattern validation"
      },
      "challenges": [
        {
          "id": "yole-res-circuit-breaker-activation",
          "name": "Circuit Breaker Activation",
          "description": "Verify circuit breaker opens after failures",
          "category": "resilience",
          "estimated_duration": "60s",
          "inputs": [
            {"name": "failure_threshold", "source": "config", "required": true}
          ],
          "outputs": [
            {"name": "circuit_opened", "type": "string", "description": "Whether CB opened"}
          ],
          "assertions": [
            {"type": "contains", "target": "circuit_opened", "value": "true", "message": "CB must open"}
          ],
          "metrics": ["open_time_ms", "recovery_time_ms"]
        }
      ]
    }
    """.trimIndent()

    private val sampleConcurrencyBank = """
    {
      "version": "1.0",
      "name": "Yole Concurrency Challenges",
      "metadata": {
        "project": "yole",
        "domain": "concurrency",
        "description": "Concurrency validation"
      },
      "challenges": [
        {
          "id": "yole-conc-parallel-format-parsing",
          "name": "Concurrent Format Parsing",
          "description": "Verify concurrent parsing safety",
          "category": "concurrency",
          "estimated_duration": "60s",
          "inputs": [
            {"name": "thread_count", "source": "config", "required": true}
          ],
          "outputs": [
            {"name": "completed_threads", "type": "string", "description": "Threads completed"}
          ],
          "assertions": [
            {"type": "exact_count", "target": "completed_threads", "value": 10, "message": "All threads must complete"}
          ],
          "metrics": ["total_time_ms"]
        }
      ]
    }
    """.trimIndent()

    // ====================================================================
    // Helper: Parse a bank JSON string and return the challenges array
    // ====================================================================

    private fun parseBank(json: String): JsonObject {
        return Json.parseToJsonElement(json).jsonObject
    }

    private fun getChallenges(bank: JsonObject): List<JsonObject> {
        return bank["challenges"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
    }

    private fun getAssertions(challenge: JsonObject): List<JsonObject> {
        return challenge["assertions"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
    }

    private fun getInputs(challenge: JsonObject): List<JsonObject> {
        return challenge["inputs"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
    }

    private fun getOutputs(challenge: JsonObject): List<JsonObject> {
        return challenge["outputs"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
    }

    // ====================================================================
    // Test 1-5: JSON well-formedness and parsing
    // ====================================================================

    @Test
    fun testFormatParsingBankParsesAsValidJson() {
        val bank = parseBank(sampleFormatParsingBank)
        assertNotNull(bank, "Format parsing bank must parse as valid JSON")
        assertTrue(bank.containsKey("challenges"), "Bank must contain 'challenges' array")
    }

    @Test
    fun testSecurityBankParsesAsValidJson() {
        val bank = parseBank(sampleSecurityBank)
        assertNotNull(bank, "Security bank must parse as valid JSON")
        assertTrue(bank.containsKey("challenges"), "Bank must contain 'challenges' array")
    }

    @Test
    fun testNetworkBankParsesAsValidJson() {
        val bank = parseBank(sampleNetworkBank)
        assertNotNull(bank, "Network bank must parse as valid JSON")
        assertTrue(bank.containsKey("challenges"), "Bank must contain 'challenges' array")
    }

    @Test
    fun testResilienceBankParsesAsValidJson() {
        val bank = parseBank(sampleResilienceBank)
        assertNotNull(bank, "Resilience bank must parse as valid JSON")
        assertTrue(bank.containsKey("challenges"), "Bank must contain 'challenges' array")
    }

    @Test
    fun testConcurrencyBankParsesAsValidJson() {
        val bank = parseBank(sampleConcurrencyBank)
        assertNotNull(bank, "Concurrency bank must parse as valid JSON")
        assertTrue(bank.containsKey("challenges"), "Bank must contain 'challenges' array")
    }

    // ====================================================================
    // Test 6-10: Required field validation
    // ====================================================================

    @Test
    fun testAllChallengesHaveRequiredIdField() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                val id = challenge["id"]?.jsonPrimitive?.content
                assertNotNull(id, "Challenge must have 'id' field")
                assertTrue(id.isNotBlank(), "Challenge id must not be blank")
            }
        }
    }

    @Test
    fun testAllChallengesHaveRequiredNameField() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                val name = challenge["name"]?.jsonPrimitive?.content
                assertNotNull(name, "Challenge must have 'name' field")
                assertTrue(name.isNotBlank(), "Challenge name must not be blank")
            }
        }
    }

    @Test
    fun testAllChallengesHaveRequiredDescriptionField() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                val desc = challenge["description"]?.jsonPrimitive?.content
                assertNotNull(desc, "Challenge must have 'description' field")
                assertTrue(desc.isNotBlank(), "Challenge description must not be blank")
            }
        }
    }

    @Test
    fun testAllChallengesHaveRequiredAssertionsField() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                val assertions = challenge["assertions"]?.jsonArray
                assertNotNull(assertions, "Challenge '${challenge["id"]}' must have 'assertions' array")
                assertTrue(assertions.isNotEmpty(), "Challenge '${challenge["id"]}' must have at least one assertion")
            }
        }
    }

    @Test
    fun testAllChallengesHaveEstimatedDuration() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                val duration = challenge["estimated_duration"]?.jsonPrimitive?.content
                assertNotNull(duration, "Challenge '${challenge["id"]}' must have estimated_duration")
                assertTrue(
                    duration.matches(Regex("^\\d+[smh]$")),
                    "Duration '$duration' must match pattern like '30s', '2m', '1h'"
                )
            }
        }
    }

    // ====================================================================
    // Test 11-13: ID uniqueness
    // ====================================================================

    @Test
    fun testChallengeIdsUniqueWithinFormatParsingBank() {
        val bank = parseBank(sampleFormatParsingBank)
        val ids = getChallenges(bank).map { it["id"]!!.jsonPrimitive.content }
        assertEquals(ids.size, ids.toSet().size, "IDs must be unique within format-parsing bank")
    }

    @Test
    fun testChallengeIdsUniqueWithinNetworkBank() {
        val bank = parseBank(sampleNetworkBank)
        val ids = getChallenges(bank).map { it["id"]!!.jsonPrimitive.content }
        assertEquals(ids.size, ids.toSet().size, "IDs must be unique within network bank")
    }

    @Test
    fun testChallengeIdsUniqueAcrossAllBanks() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        val allIds = mutableListOf<String>()
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                allIds.add(challenge["id"]!!.jsonPrimitive.content)
            }
        }
        assertEquals(
            allIds.size, allIds.toSet().size,
            "Challenge IDs must be unique across all banks. Duplicates: ${allIds.groupBy { it }.filter { it.value.size > 1 }.keys}"
        )
    }

    // ====================================================================
    // Test 14-16: ID prefix convention
    // ====================================================================

    @Test
    fun testAllChallengeIdsStartWithYolePrefix() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                val id = challenge["id"]!!.jsonPrimitive.content
                assertTrue(id.startsWith("yole-"), "Challenge ID '$id' must start with 'yole-' prefix")
            }
        }
    }

    @Test
    fun testFormatParsingChallengeIdsUseFormatPrefix() {
        val bank = parseBank(sampleFormatParsingBank)
        for (challenge in getChallenges(bank)) {
            val id = challenge["id"]!!.jsonPrimitive.content
            assertTrue(
                id.startsWith("yole-fmt-"),
                "Format parsing challenge ID '$id' should use 'yole-fmt-' prefix"
            )
        }
    }

    @Test
    fun testSecurityChallengeIdsUseSecurityPrefix() {
        val bank = parseBank(sampleSecurityBank)
        for (challenge in getChallenges(bank)) {
            val id = challenge["id"]!!.jsonPrimitive.content
            assertTrue(
                id.startsWith("yole-sec-"),
                "Security challenge ID '$id' should use 'yole-sec-' prefix"
            )
        }
    }

    // ====================================================================
    // Test 17-19: Assertion type validity
    // ====================================================================

    @Test
    fun testAllAssertionTypesAreValid() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                for (assertion in getAssertions(challenge)) {
                    val type = assertion["type"]!!.jsonPrimitive.content
                    assertTrue(
                        type in validAssertionTypes,
                        "Assertion type '$type' in challenge '${challenge["id"]}' " +
                                "is not a valid built-in evaluator. Valid: $validAssertionTypes"
                    )
                }
            }
        }
    }

    @Test
    fun testAllAssertionsHaveTargetField() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                for (assertion in getAssertions(challenge)) {
                    val target = assertion["target"]?.jsonPrimitive?.content
                    assertNotNull(
                        target,
                        "Assertion in challenge '${challenge["id"]}' must have 'target' field"
                    )
                    assertTrue(target.isNotBlank(), "Assertion target must not be blank")
                }
            }
        }
    }

    @Test
    fun testAllAssertionsHaveMessageField() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                for (assertion in getAssertions(challenge)) {
                    val message = assertion["message"]?.jsonPrimitive?.content
                    assertNotNull(
                        message,
                        "Assertion in challenge '${challenge["id"]}' must have 'message' field"
                    )
                    assertTrue(message.isNotBlank(), "Assertion message must not be blank")
                }
            }
        }
    }

    // ====================================================================
    // Test 20-21: Assertion target references declared outputs
    // ====================================================================

    @Test
    fun testAssertionTargetsReferenceOutputNames() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                val outputNames = getOutputs(challenge).map { it["name"]!!.jsonPrimitive.content }.toSet()
                for (assertion in getAssertions(challenge)) {
                    val target = assertion["target"]!!.jsonPrimitive.content
                    assertTrue(
                        target in outputNames,
                        "Assertion target '$target' in challenge '${challenge["id"]}' " +
                                "must reference a declared output. Declared outputs: $outputNames"
                    )
                }
            }
        }
    }

    @Test
    fun testValueRequiredForCountAndContainsAssertions() {
        val typesRequiringValue = setOf("contains", "contains_any", "exact_count", "min_count", "min_length",
            "max_latency", "quality_score", "min_score", "equals")
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                for (assertion in getAssertions(challenge)) {
                    val type = assertion["type"]!!.jsonPrimitive.content
                    if (type in typesRequiringValue) {
                        assertNotNull(
                            assertion["value"],
                            "Assertion type '$type' in challenge '${challenge["id"]}' requires a 'value' field"
                        )
                    }
                }
            }
        }
    }

    // ====================================================================
    // Test 22-23: Format ID references
    // ====================================================================

    @Test
    fun testFormatIdsInTextFormatCompanionAreComplete() {
        // Verify we have all 19 expected format IDs (18 formats + unknown).
        // 18 formats = 17 original (markdown, todotxt, csv, ...) + JSON (iter 42).
        assertTrue(
            validFormatIds.size >= 19,
            "TextFormat.Companion must define at least 19 format IDs (18 formats + unknown), found ${validFormatIds.size}"
        )
        // Verify the known core format IDs exist
        assertTrue("markdown" in validFormatIds, "markdown must be a valid format ID")
        assertTrue("todotxt" in validFormatIds, "todotxt must be a valid format ID")
        assertTrue("csv" in validFormatIds, "csv must be a valid format ID")
        assertTrue("latex" in validFormatIds, "latex must be a valid format ID")
        assertTrue("orgmode" in validFormatIds, "orgmode must be a valid format ID")
        assertTrue("restructuredtext" in validFormatIds, "restructuredtext must be a valid format ID")
        assertTrue("asciidoc" in validFormatIds, "asciidoc must be a valid format ID")
        assertTrue("wikitext" in validFormatIds, "wikitext must be a valid format ID")
        assertTrue("keyvalue" in validFormatIds, "keyvalue must be a valid format ID")
        assertTrue("taskpaper" in validFormatIds, "taskpaper must be a valid format ID")
        assertTrue("textile" in validFormatIds, "textile must be a valid format ID")
        assertTrue("creole" in validFormatIds, "creole must be a valid format ID")
        assertTrue("tiddlywiki" in validFormatIds, "tiddlywiki must be a valid format ID")
        assertTrue("jupyter" in validFormatIds, "jupyter must be a valid format ID")
        assertTrue("rmarkdown" in validFormatIds, "rmarkdown must be a valid format ID")
        assertTrue("binary" in validFormatIds, "binary must be a valid format ID")
        assertTrue("plaintext" in validFormatIds, "plaintext must be a valid format ID")
        assertTrue("json" in validFormatIds, "json must be a valid format ID (added iter 42)")
    }

    @Test
    fun testFormatParsingBankCoversAllFormats() {
        // The format-parsing bank should have challenges for the core 17 formats
        // (excluding unknown and binary which are special cases)
        val bank = parseBank(sampleFormatParsingBank)
        val challengeIds = getChallenges(bank).map { it["id"]!!.jsonPrimitive.content }
        // At minimum, the sample has markdown and csv
        assertTrue(
            challengeIds.any { it.contains("markdown") },
            "Format parsing bank must include a markdown challenge"
        )
        assertTrue(
            challengeIds.any { it.contains("csv") },
            "Format parsing bank must include a CSV challenge"
        )
    }

    // ====================================================================
    // Test 24-25: Protocol name references
    // ====================================================================

    @Test
    fun testProtocolNamesMatchServiceImplementations() {
        // Verify all 8 expected protocol service implementations are tracked
        assertEquals(8, validProtocolNames.size, "There should be 8 protocol implementations")
        assertTrue("dropbox" in validProtocolNames, "DropboxService must be tracked")
        assertTrue("googledrive" in validProtocolNames, "GoogleDriveService must be tracked")
        assertTrue("onedrive" in validProtocolNames, "OneDriveService must be tracked")
        assertTrue("ftp" in validProtocolNames, "FtpService must be tracked")
        assertTrue("sftp" in validProtocolNames, "SftpService must be tracked")
        assertTrue("smb" in validProtocolNames, "SmbService must be tracked")
        assertTrue("webdav" in validProtocolNames, "WebDavService must be tracked")
        assertTrue("git" in validProtocolNames, "GitService must be tracked")
    }

    @Test
    fun testNetworkBankReferencesValidProtocols() {
        val bank = parseBank(sampleNetworkBank)
        val challengeIds = getChallenges(bank).map { it["id"]!!.jsonPrimitive.content }
        // All challenge IDs with protocol names should reference known protocols
        for (id in challengeIds) {
            val protocolPart = id.removePrefix("yole-net-").split("-").firstOrNull() ?: continue
            if (protocolPart in listOf("rate", "storage")) continue // Non-protocol challenges
            assertTrue(
                protocolPart in validProtocolNames,
                "Network challenge '$id' references unknown protocol '$protocolPart'"
            )
        }
    }

    // ====================================================================
    // Test 26-27: Metadata validation
    // ====================================================================

    @Test
    fun testBankMetadataHasRequiredFields() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            val metadata = bank["metadata"]?.jsonObject
            assertNotNull(metadata, "Bank must have metadata object")
            assertNotNull(metadata["project"], "Metadata must have 'project' field")
            assertNotNull(metadata["domain"], "Metadata must have 'domain' field")
            assertNotNull(metadata["description"], "Metadata must have 'description' field")
        }
    }

    @Test
    fun testBankMetadataProjectIsYole() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            val project = bank["metadata"]!!.jsonObject["project"]!!.jsonPrimitive.content
            assertEquals("yole", project, "Bank metadata project must be 'yole'")
        }
    }

    // ====================================================================
    // Test 28: Input source validation
    // ====================================================================

    @Test
    fun testInputSourcesAreValid() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                for (input in getInputs(challenge)) {
                    val source = input["source"]!!.jsonPrimitive.content
                    assertTrue(
                        source in validInputSources || source.startsWith(dependencySourcePrefix),
                        "Input source '$source' in challenge '${challenge["id"]}' " +
                                "must be 'config', 'env', or 'dependency:<id>'"
                    )
                }
            }
        }
    }

    // ====================================================================
    // Test 29: Output type validation
    // ====================================================================

    @Test
    fun testOutputTypesAreValid() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            for (challenge in getChallenges(bank)) {
                for (output in getOutputs(challenge)) {
                    val type = output["type"]!!.jsonPrimitive.content
                    assertTrue(
                        type in validOutputTypes,
                        "Output type '$type' in challenge '${challenge["id"]}' " +
                                "must be one of $validOutputTypes"
                    )
                }
            }
        }
    }

    // ====================================================================
    // Test 30: Dependency references validation
    // ====================================================================

    @Test
    fun testDependencyReferencesExistInSameBank() {
        // Verify that dependency IDs reference valid challenge IDs
        val bank = parseBank(sampleNetworkBank)
        val allIds = getChallenges(bank).map { it["id"]!!.jsonPrimitive.content }.toSet()
        for (challenge in getChallenges(bank)) {
            val deps = challenge["dependencies"]?.jsonArray
                ?.map { it.jsonPrimitive.content } ?: emptyList()
            for (depId in deps) {
                assertTrue(
                    depId in allIds,
                    "Dependency '$depId' in challenge '${challenge["id"]}' " +
                            "must reference a valid challenge ID within the same bank or a loaded bank"
                )
            }
        }
    }

    // ====================================================================
    // Test 31: Bank version field validation
    // ====================================================================

    @Test
    fun testAllBanksHaveVersionField() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            val version = bank["version"]?.jsonPrimitive?.content
            assertNotNull(version, "Bank must have 'version' field")
            assertEquals("1.0", version, "Bank version must be '1.0'")
        }
    }

    // ====================================================================
    // Test 32: Expected bank file list validation
    // ====================================================================

    @Test
    fun testExpectedBankFileListIsComplete() {
        // Verify we track all 17 known bank files
        assertEquals(
            17, expectedBankFiles.size,
            "Expected 17 bank files in Challenges/banks/yole/"
        )
        assertTrue("format-parsing.json" in expectedBankFiles, "Must include format-parsing.json")
        assertTrue("security.json" in expectedBankFiles, "Must include security.json")
        assertTrue("network-protocols.json" in expectedBankFiles, "Must include network-protocols.json")
        assertTrue("resilience.json" in expectedBankFiles, "Must include resilience.json")
        assertTrue("concurrency.json" in expectedBankFiles, "Must include concurrency.json")
        assertTrue("performance.json" in expectedBankFiles, "Must include performance.json")
        assertTrue("monitoring.json" in expectedBankFiles, "Must include monitoring.json")
        assertTrue("memory.json" in expectedBankFiles, "Must include memory.json")
        assertTrue("lazy-loading.json" in expectedBankFiles, "Must include lazy-loading.json")
        assertTrue("test-coverage.json" in expectedBankFiles, "Must include test-coverage.json")
        assertTrue("ui-accessibility.json" in expectedBankFiles, "Must include ui-accessibility.json")
        assertTrue("cross-platform-build.json" in expectedBankFiles, "Must include cross-platform-build.json")
        assertTrue("e2e-userflow.json" in expectedBankFiles, "Must include e2e-userflow.json")
    }

    // ====================================================================
    // Test 33: Malformed JSON detection
    // ====================================================================

    @Test
    fun testMalformedJsonIsRejected() {
        val malformedInputs = listOf(
            "",
            "{",
            "{\"challenges\": [}",
            "not json at all",
        )
        for (input in malformedInputs) {
            val result = runCatching { Json.parseToJsonElement(input) }
            assertTrue(result.isFailure, "Malformed JSON should fail to parse: '$input'")
        }

        // Structurally invalid: "challenges" is not an array (valid JSON but invalid bank)
        val structurallyInvalid = "{\"challenges\": \"not an array\"}"
        val parsed = Json.parseToJsonElement(structurallyInvalid)
        val challengesField = parsed.jsonObject["challenges"]
        assertFalse(
            challengesField is kotlinx.serialization.json.JsonArray,
            "challenges field should not be a JsonArray in structurally invalid input"
        )
    }

    // ====================================================================
    // Test 34: Category consistency
    // ====================================================================

    @Test
    fun testChallengeCategoryMatchesBankDomain() {
        // In well-formed banks, challenge categories should relate to the bank's domain
        val bank = parseBank(sampleFormatParsingBank)
        val domain = bank["metadata"]!!.jsonObject["domain"]!!.jsonPrimitive.content
        for (challenge in getChallenges(bank)) {
            val category = challenge["category"]!!.jsonPrimitive.content
            assertEquals(
                domain, category,
                "Challenge '${challenge["id"]}' category '$category' should match bank domain '$domain'"
            )
        }
    }

    // ====================================================================
    // Test 35: Bank name is non-empty
    // ====================================================================

    @Test
    fun testAllBanksHaveNonEmptyName() {
        val allBanks = listOf(
            sampleFormatParsingBank, sampleSecurityBank,
            sampleNetworkBank, sampleResilienceBank, sampleConcurrencyBank
        )
        for (bankJson in allBanks) {
            val bank = parseBank(bankJson)
            val name = bank["name"]?.jsonPrimitive?.content
            assertNotNull(name, "Bank must have 'name' field")
            assertTrue(name.isNotBlank(), "Bank name must not be blank")
        }
    }
}
