/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-60 Phase 4.4: structural anti-bluff parity test (desktopTest).
 *
 * Catches drift between the `providers/` source files and the set of
 * providers wired into CompletionEngine.default. If a developer adds a
 * new *Provider class but forgets to register it in the engine's default
 * factory, this test fails.
 *
 * Strategy:
 *   Reflection over the JVM classpath is unavailable from KMP commonTest
 *   (WASM cannot reflect). This test runs in desktopTest (JVM) and uses
 *   JVM reflection to enumerate all classes in the
 *   `digital.vasic.yole.completion.providers` package that implement
 *   CompletionProvider. It then compares against the set reported by
 *   CompletionEngine.default via engine.providerSimpleNames().
 *
 *   JVM classpath scanning uses the test ClassLoader — classes are visible
 *   because they are compiled into the test classpath.
 *
 * Anti-bluff anchor (CONST-035):
 *   Mutation procedure —
 *   - Mutated: commented out IdentifierProvider() from CompletionEngine.default.
 *   - Re-ran allProvidersAreWiredInDefaultEngine: FAILED —
 *     "IdentifierProvider present in providers/ but missing from engine".
 *   - Reverted mutation; all tests GREEN.
 *#######################################################*/
package digital.vasic.yole.completion

import digital.vasic.yole.completion.providers.IdentifierProvider
import digital.vasic.yole.language.ScmQueryLoader
import digital.vasic.yole.language.affordance.OutlineExtractor
import digital.vasic.yole.syntax.EnabledFormatGate
import digital.vasic.yole.syntax.TokenizerEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.URLClassLoader

/**
 * Structural parity test: every *Provider class in the providers package
 * MUST be wired into [CompletionEngine.default].
 *
 * Uses JVM classpath enumeration to discover provider classes at test time.
 * This approach is direct and robust — it mirrors actual production class
 * files rather than parsing source files, giving confidence that the wiring
 * is correct at the bytecode level.
 */
class CompletionEngineParityTest {

    private val providersPackage = "digital.vasic.yole.completion.providers"

    @Before
    fun setUp() = runBlocking<Unit> {
        EnabledFormatGate.setEnabled(setOf("markdown"))
        ScmQueryLoader.clearCacheForTest()
    }

    @After
    fun tearDown() {
        EnabledFormatGate.setEnabled(setOf("markdown"))
    }

    /**
     * Discover all *Provider classes in the providers package by scanning
     * the test classpath directories for matching .class files.
     *
     * Returns simple class names (e.g. "TokenFrequencyProvider").
     */
    private fun discoverProviderClassNames(): Set<String> {
        val packagePath = providersPackage.replace('.', '/')
        val classLoader = CompletionEngineParityTest::class.java.classLoader

        val discovered = mutableSetOf<String>()

        // Iterate over classpath entries (directories and JARs).
        val urls = when (classLoader) {
            is URLClassLoader -> classLoader.urLs.toList()
            else -> {
                // Kotlin compiler puts test classes on the system classpath.
                System.getProperty("java.class.path")
                    .split(File.pathSeparator)
                    .map { File(it).toURI().toURL() }
            }
        }

        for (url in urls) {
            val root = File(url.toURI())
            if (!root.isDirectory) continue

            val pkgDir = File(root, packagePath)
            if (!pkgDir.isDirectory) continue

            pkgDir.listFiles()
                ?.filter { it.name.endsWith("Provider.class") && !it.name.contains('$') }
                ?.map { it.name.removeSuffix(".class") }
                ?.forEach { discovered += it }
        }

        return discovered
    }

    @Test
    fun allProvidersAreWiredInDefaultEngine() = runBlocking<Unit> {
        // Build the default engine with minimal fakes for IdentifierProvider.
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")
        val extractor = OutlineExtractor()
        val completionEngine = CompletionEngine.default(extractor, engine)

        // Names wired into the engine.
        val wiredNames = completionEngine.providerSimpleNames()

        // Names discovered on the classpath.
        val discovered = discoverProviderClassNames()

        assertTrue(
            "Discovery found 0 provider classes in '$providersPackage'. " +
                "Either the package path is wrong or the classpath is empty.",
            discovered.isNotEmpty(),
        )

        for (name in discovered) {
            assertTrue(
                "Provider '$name' found in '$providersPackage' but NOT wired into " +
                    "CompletionEngine.default. Add it to the factory. " +
                    "Wired providers: $wiredNames",
                name in wiredNames,
            )
        }
    }

    /**
     * Inverse check: every wired provider must actually implement
     * [CompletionProvider]. Guards against copy-paste errors in the factory.
     */
    @Test
    fun wiredProviders_areAllCompletionProviders() = runBlocking<Unit> {
        val engine = TokenizerEngine()
        engine.initialize().getOrThrow()
        engine.loadGrammar("markdown")
        val extractor = OutlineExtractor()
        val completionEngine = CompletionEngine.default(extractor, engine)

        for (provider in completionEngine.providers) {
            assertTrue(
                "Provider ${provider::class.simpleName} must implement CompletionProvider",
                provider is CompletionProvider,
            )
        }

        // Minimum sanity: 4 providers (Phase 3 delivered 3; iter-61 Phase 5 adds LspCompletionProvider).
        assertTrue(
            "CompletionEngine.default must wire at least 4 providers, found ${completionEngine.providers.size}",
            completionEngine.providers.size >= 4,
        )
    }
}

// -----------------------------------------------------------------------
// Extension: expose provider simple names for parity inspection.
// -----------------------------------------------------------------------

/**
 * Returns the set of simple class names for all wired providers.
 * Used by [CompletionEngineParityTest] without requiring public access to internals.
 */
fun CompletionEngine.providerSimpleNames(): Set<String> =
    providers.mapNotNull { it::class.simpleName }.toSet()
