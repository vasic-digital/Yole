/*#######################################################
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 * iter-63 Phase 3: CodeActionMappingTest — unit tests for
 * the mapLspCodeAction internal helper.
 *
 * LSP `textDocument/codeAction` returns `List<Either<Command, CodeAction>>`.
 * Three cases must be covered per Phase 0 §1 research:
 *   1. Left side (Command) → CodeAction with title + command; edit = null.
 *   2. Right side (CodeAction) with edit → CodeAction with mapped WorkspaceEdit.
 *   3. Right side (CodeAction) with both edit and command.
 *
 * Mutation procedure (CONST-035):
 *   1. Stub mapLspCodeAction to always return CodeAction("STUB",null,null,null).
 *      → commandEither_mapsToCodeAction FAILS (title != "Fix unused import").
 *      → codeActionEither_withEdit_mapsWorkspaceEdit FAILS (edit != null expected).
 *   2. Revert; confirm all 3 PASS.
 *
 * Cross-platform impact (CONST-037):
 *   - Desktop (desktopTest): JVM actual helper under test; lives here.
 *   - Android: mapLspCodeActionAndroid mirrors logic; covered by androidUnitTest.
 *   - iOS/Wasm: codeActions() returns emptyList(); no mapping occurs.
 *
 * Submodules: not touched (CONST-038).
 *#######################################################*/
package digital.vasic.yole.lsp

import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.TextEdit as LspTextEdit
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.jsonrpc.messages.Either
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [mapLspCodeAction].
 *
 * Tests call the production helper directly (internal visibility, same package).
 * No mock server needed — LSP4J data classes are constructed inline.
 */
class CodeActionMappingTest {

    /**
     * When the Either is left (Command), mapLspCodeAction must produce a
     * [CodeAction] with the command's title, the command identifier, kind = null,
     * and edit = null.
     *
     * Mutation: return CodeAction("STUB",...) → title check FAILS.
     */
    @Test
    fun commandEither_mapsToCodeAction() {
        val cmd = Command("Fix unused import", "fix.unusedImport", listOf("com.example.Foo"))
        val either: Either<Command, org.eclipse.lsp4j.CodeAction> = Either.forLeft(cmd)

        val result = mapLspCodeAction(either)

        assertEquals("Fix unused import", result.title, "title must come from Command.title")
        assertEquals("fix.unusedImport", result.command, "command must come from Command.command")
        assertNull(result.kind, "kind must be null for Command-based actions")
        assertNull(result.edit, "edit must be null for Command-based actions")
    }

    /**
     * When the Either is right (CodeAction) and the action carries a WorkspaceEdit,
     * the mapper must produce a [CodeAction] with a non-null edit whose changes map
     * is correctly populated.
     *
     * Mutation: return CodeAction with edit = null → assertNotNull FAILS.
     */
    @Test
    fun codeActionEither_withEdit_mapsWorkspaceEdit() {
        val lspAction = org.eclipse.lsp4j.CodeAction("Rename symbol").also { action ->
            action.kind = "refactor.rename"
            // Build a workspace edit using the legacy changes map.
            val wsEdit = org.eclipse.lsp4j.WorkspaceEdit()
            wsEdit.changes = mapOf(
                "file:///project/Foo.kt" to listOf(
                    LspTextEdit(Range(Position(1, 4), Position(1, 7)), "newFoo"),
                ),
            )
            action.edit = wsEdit
        }
        val either: Either<Command, org.eclipse.lsp4j.CodeAction> = Either.forRight(lspAction)

        val result = mapLspCodeAction(either)

        assertEquals("Rename symbol", result.title)
        assertEquals("refactor.rename", result.kind)
        assertNull(result.command, "command should be null when CodeAction has no command field")
        assertNotNull(result.edit, "edit must not be null")
        assertTrue(result.edit!!.changes.containsKey("file:///project/Foo.kt"), "URI must be in mapped changes")
        assertEquals("newFoo", result.edit!!.changes["file:///project/Foo.kt"]!![0].newText)
    }

    /**
     * When the Either is right (CodeAction) with BOTH edit and command, the mapper
     * must preserve both — edit mapped to WorkspaceEdit and command extracted from
     * the nested Command object.
     *
     * Mutation: drop command → command field is null → FAILS.
     */
    @Test
    fun codeActionEither_withEditAndCommand_mapsBoth() {
        val lspAction = org.eclipse.lsp4j.CodeAction("Organize imports").also { action ->
            action.kind = "source.organizeImports"
            val wsEdit = org.eclipse.lsp4j.WorkspaceEdit()
            wsEdit.changes = mapOf(
                "file:///project/Main.kt" to listOf(
                    LspTextEdit(Range(Position(0, 0), Position(0, 0)), "import org.example.Foo\n"),
                ),
            )
            action.edit = wsEdit
            action.command = Command("Run post-action", "run.postAction", emptyList())
        }
        val either: Either<Command, org.eclipse.lsp4j.CodeAction> = Either.forRight(lspAction)

        val result = mapLspCodeAction(either)

        assertEquals("Organize imports", result.title)
        assertEquals("source.organizeImports", result.kind)
        assertNotNull(result.edit, "edit must be non-null")
        assertEquals("run.postAction", result.command, "command must be extracted from nested Command")
    }
}
