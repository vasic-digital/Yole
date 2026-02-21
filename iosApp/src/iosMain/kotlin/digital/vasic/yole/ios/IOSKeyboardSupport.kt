/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS Keyboard Support
 * Native keyboard handling and shortcuts
 *
 *########################################################*/
package digital.vasic.yole.ios

import platform.UIKit.UIInputViewController
import platform.UIKit.UIKeyCommand
import platform.UIKit.UIKeyCommandInput
import platform.UIKit.UIMenu
import platform.UIKit.UIMenuElement

/**
 * iOS Keyboard Commands
 */
object YoleKeyboardCommands {
    
    /**
     * Available keyboard shortcuts
     */
    enum class KeyboardShortcut(val key: String, val modifiers: Int = 0) {
        SAVE("s", 1),           // Command+S
        NEW("n", 1),             // Command+N
        OPEN("o", 1),            // Command+O
        CLOSE("w", 1),           // Command+W
        FIND("f", 1),            // Command+F
        REPLACE("r", 1),         // Command+R
        BOLD("b", 1),            // Command+B
        ITALIC("i", 1),          // Command+I
        UNDERLINE("u", 1),       // Command+U
        UNDO("z", 1),            // Command+Z
        REDO("z", 1),            // Command+Shift+Z
        CUT("x", 1),             // Command+X
        COPY("c", 1),            // Command+C
        PASTE("v", 1),           // Command+V
        SELECT_ALL("a", 1),      // Command+A
        NEXT_WORD("right", 2),   // Option+Right
        PREV_WORD("left", 2),   // Option+Left
        LINE_START("left", 4),   // Command+Left
        LINE_END("right", 4)     // Command+Right
    }
    
    /**
     * Modifier flags
     */
    object Modifiers {
        const val COMMAND = 1
        const val SHIFT = 2
        const val OPTION = 4
        const val CONTROL = 8
    }
    
    /**
     * Create key command for shortcut
     */
    fun createKeyCommand(shortcut: KeyboardShortcut): UIKeyCommand {
        return UIKeyCommand(
            input = shortcut.key,
            modifierFlags = shortcut.modifiers.toUInt(),
            action = NSSelectorFromString("handleKeyCommand:")
        )
    }
    
    /**
     * Get all keyboard commands as menu
     */
    fun getMenu(): UIMenu {
        val commands = listOf(
            createKeyCommand(KeyboardShortcut.NEW),
            createKeyCommand(KeyboardShortcut.OPEN),
            createKeyCommand(KeyboardShortcut.SAVE),
            createKeyCommand(KeyboardShortcut.CLOSE)
        )
        return UIMenu.create(title = "File", children = commands)
    }
}

/**
 * Custom Text Input View Controller
 */
class YoleTextInputViewController : UIInputViewController() {
    
    /**
     * Handle keyboard input
     */
    override fun textWillChange(textInput: platform.UIKit.UITextInput?) {
        super.textWillChange(textInput)
    }
    
    /**
     * Handle text change
     */
    override fun textDidChange(textInput: platform.UIKit.UITextInput?) {
        super.textDidChange(textInput)
    }
    
    /**
     * Get available key commands
     */
    override fun keyCommands(): List<UIKeyCommand> {
        return listOf(
            YoleKeyboardCommands.createKeyCommand(YoleKeyboardCommands.KeyboardShortcut.BOLD),
            YoleKeyboardCommands.createKeyCommand(YoleKeyboardCommands.KeyboardShortcut.ITALIC),
            YoleKeyboardCommands.createKeyCommand(YoleKeyboardCommands.KeyboardShortcut.UNDERLINE)
        )
    }
}
