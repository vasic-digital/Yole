/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * Desktop-only Compose drag-and-drop modifier for import file drops.
 * Uses Modifier.dragAndDropTarget (Compose 1.7+).
 * Replaces the legacy AWT DropTargetAdapter wired in Main.kt for
 * the importer path (the AWT fallback in Main.kt remains for general
 * file-open drops; this modifier handles importer-specific surfaces).
 *
 *########################################################*/

package digital.vasic.yole.desktop.ui.import_

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import java.io.File
import java.net.URI

/**
 * Attaches a Compose-native drag-and-drop target to any Composable that
 * should accept file drops for the importer workflow.
 *
 * Behaviour:
 * - Accepts events where the dragged data contains at least one local file.
 * - On drop, reads the first file's bytes with [File.readBytes] and invokes
 *   [onFileDropped] with the raw bytes and the file's name.
 * - Silently ignores events with no file data (non-file drags, empty lists).
 * - All file I/O happens on the calling coroutine/thread; callers are
 *   responsible for dispatching to an appropriate scope if needed.
 *
 * Example usage:
 * ```kotlin
 * Box(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .acceptImportFileDrops { bytes, name ->
 *             scope.launch { handleImport(bytes, name) }
 *         }
 * )
 * ```
 *
 * @param onFileDropped Callback invoked with (bytes, fileName) of the first
 *   dropped file. Invoked on the UI thread — dispatch heavy work to a coroutine.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun Modifier.acceptImportFileDrops(
    onFileDropped: (bytes: ByteArray, fileName: String) -> Unit
): Modifier {
    val target = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val filesList = event.dragData() as? DragData.FilesList ?: return false
                val uris = filesList.readFiles()
                val firstUri = uris.firstOrNull() ?: return false
                return try {
                    val file = File(URI(firstUri))
                    if (file.isFile && file.canRead()) {
                        onFileDropped(file.readBytes(), file.name)
                        true
                    } else {
                        false
                    }
                } catch (_: Exception) {
                    false
                }
            }
        }
    }
    return dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            event.dragData() is DragData.FilesList
        },
        target = target
    )
}

/**
 * Pure-logic helper extracted for unit-testability (no Compose runtime needed).
 *
 * Given a list of file URI strings (as returned by [DragData.FilesList.readFiles]),
 * returns the [File] corresponding to the first valid readable local file,
 * or null if the list is empty / the first entry cannot be resolved.
 *
 * This function is intentionally free of Compose, AWT, and coroutines so that
 * it can be exercised in plain [desktopTest] JUnit tests.
 */
fun resolveFirstDroppedFile(uris: List<String>): File? {
    val firstUri = uris.firstOrNull() ?: return null
    return try {
        val file = File(URI(firstUri))
        if (file.isFile && file.canRead()) file else null
    } catch (_: Exception) {
        null
    }
}
