/*#######################################################
 *
 * SPDX-FileCopyrightText: 2026 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iter-55: IDE editor surface that keeps the line-number gutter and
 * text body locked to a single shared ScrollState, eliminating the
 * horizontal desync that occurred when both surfaces previously held
 * independent rememberScrollState() instances.
 *
 * Anti-bluff covenant (CONST-035): the test
 * EditorScrollSyncRobolectricTest scrolls the editor and asserts the
 * gutter's scroll offset matches within 1px. Reverting the shared
 * ScrollState to two independent instances MUST cause the test to fail.
 *
 *########################################################*/
package digital.vasic.yole.android.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SyncedScrollEditor(
    textState: MutableState<String>,
    showLineNumbers: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onTextChanged: (String) -> Unit = {},
    semanticsLabel: String? = null,
    placeholder: String? = null,
    textStyle: TextStyle = TextStyle(
        color = if (isDarkTheme) Color(0xFFD4D4D4) else Color(0xFF1E1E1E),
        fontSize = 14.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 20.sp,
    ),
) {
    val sharedScroll = rememberScrollState()

    Row(modifier = modifier.fillMaxSize()) {
        if (showLineNumbers) {
            val lines = textState.value.lines()
            val gutterWidth = when {
                lines.size >= 1000 -> 48.dp
                lines.size >= 100 -> 40.dp
                else -> 32.dp
            }
            val gutterBg = if (isDarkTheme) Color(0xFF1E1E1E) else Color(0xFFF8F8F8)
            val gutterFg = if (isDarkTheme) Color(0xFF858585) else Color(0xFF999999)

            Column(
                modifier = Modifier
                    .testTag("syncedScrollEditor.gutter")
                    .width(gutterWidth)
                    .fillMaxHeight()
                    .background(gutterBg)
                    .verticalScroll(sharedScroll)
                    .padding(top = 8.dp, end = 4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                lines.forEachIndexed { idx, _ ->
                    Text(
                        text = "${idx + 1}",
                        color = gutterFg,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp,
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            BasicTextField(
                value = textState.value,
                onValueChange = { newValue ->
                    textState.value = newValue
                    onTextChanged(newValue)
                },
                modifier = Modifier
                    .testTag("syncedScrollEditor.editor")
                    .fillMaxSize()
                    .verticalScroll(sharedScroll)
                    .padding(8.dp)
                    .let { m ->
                        if (semanticsLabel != null) {
                            m.semantics { contentDescription = semanticsLabel }
                        } else {
                            m
                        }
                    },
                textStyle = textStyle,
            )
            if (placeholder != null && textState.value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = if (isDarkTheme) Color(0xFF666666) else Color(0xFF999999),
                    fontSize = textStyle.fontSize,
                    fontFamily = textStyle.fontFamily,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
