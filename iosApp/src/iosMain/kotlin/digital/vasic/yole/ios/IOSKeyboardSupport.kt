/*#######################################################
 *
 * SPDX-FileCopyrightText: 2025 Milos Vasic
 * SPDX-License-Identifier: Apache-2.0
 *
 * iOS Keyboard Support
 * Native keyboard handling and shortcuts
 *
 * K/N API notes (iter-75 fixes):
 *   - UIKeyCommandInput was removed; use top-level UIKeyInput* getter properties
 *     (UIKeyInputLeftArrow, UIKeyInputRightArrow, etc.) from platform.UIKit.
 *   - UIKeyCommand is created via the class method
 *     keyCommandWithInput:modifierFlags:action: → K/N:
 *     UIKeyCommand.keyCommandWithInput(input, modifierFlags, action).
 *   - NSSelectorFromString is in platform.Foundation.
 *   - UIKeyModifierCommand / UIKeyModifierAlternate are top-level Long constants.
 *   - UIMenu.menuWithTitle(title,image,identifier,options,children) is the factory.
 *   - textWillChange / textDidChange take UITextInputProtocol? (not UITextInput).
 *   - keyCommands() is a fun override returning List<UIKeyCommand>? in K/N.
 *
 *########################################################*/
package digital.vasic.yole.ios

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIInputViewController
import platform.UIKit.UIKeyCommand
import platform.UIKit.UIKeyInputDownArrow
import platform.UIKit.UIKeyInputLeftArrow
import platform.UIKit.UIKeyInputRightArrow
import platform.UIKit.UIKeyInputUpArrow
import platform.UIKit.UIKeyModifierAlternate
import platform.UIKit.UIKeyModifierCommand
import platform.UIKit.UIMenu
import platform.UIKit.UITextInputProtocol

/**
 * iOS Keyboard Commands
 */
@OptIn(ExperimentalForeignApi::class)
object YoleKeyboardCommands {

    /**
     * Available keyboard shortcuts.
     * modifiers uses UIKeyModifier* Long constants.
     */
    enum class KeyboardShortcut(val key: String, val modifiers: Long) {
        SAVE("s", UIKeyModifierCommand),
        NEW("n", UIKeyModifierCommand),
        OPEN("o", UIKeyModifierCommand),
        CLOSE("w", UIKeyModifierCommand),
        FIND("f", UIKeyModifierCommand),
        REPLACE("r", UIKeyModifierCommand),
        BOLD("b", UIKeyModifierCommand),
        ITALIC("i", UIKeyModifierCommand),
        UNDERLINE("u", UIKeyModifierCommand),
        UNDO("z", UIKeyModifierCommand),
        REDO("z", UIKeyModifierCommand),
        CUT("x", UIKeyModifierCommand),
        COPY("c", UIKeyModifierCommand),
        PASTE("v", UIKeyModifierCommand),
        SELECT_ALL("a", UIKeyModifierCommand),
        NEXT_WORD(UIKeyInputRightArrow, UIKeyModifierAlternate),
        PREV_WORD(UIKeyInputLeftArrow, UIKeyModifierAlternate),
        LINE_START(UIKeyInputLeftArrow, UIKeyModifierCommand),
        LINE_END(UIKeyInputRightArrow, UIKeyModifierCommand)
    }

    /**
     * Create a UIKeyCommand for the given shortcut.
     * The action selector "handleKeyCommand:" must be implemented by the
     * responder that receives the command.
     */
    fun createKeyCommand(shortcut: KeyboardShortcut): UIKeyCommand {
        return UIKeyCommand.keyCommandWithInput(
            input = shortcut.key,
            modifierFlags = shortcut.modifiers,
            action = NSSelectorFromString("handleKeyCommand:")
        )
    }

    /**
     * Get a UIMenu containing the primary file commands.
     */
    fun getMenu(): UIMenu {
        val commands = listOf(
            createKeyCommand(KeyboardShortcut.NEW),
            createKeyCommand(KeyboardShortcut.OPEN),
            createKeyCommand(KeyboardShortcut.SAVE),
            createKeyCommand(KeyboardShortcut.CLOSE)
        )
        return UIMenu.menuWithTitle(
            title = "File",
            image = null,
            identifier = null,
            options = 0u,
            children = commands
        )
    }
}

/**
 * Custom Text Input View Controller
 *
 * Provides keyboard command support for the editor text field.
 */
@OptIn(ExperimentalForeignApi::class)
class YoleTextInputViewController : UIInputViewController(nibName = null, bundle = null) {

    /**
     * Handle keyboard input — called before a character is inserted.
     */
    override fun textWillChange(textInput: UITextInputProtocol?) {
        super.textWillChange(textInput)
    }

    /**
     * Handle text change — called after a character is inserted.
     */
    override fun textDidChange(textInput: UITextInputProtocol?) {
        super.textDidChange(textInput)
    }

    /**
     * UIKeyCommand list for this text input view controller.
     *
     * K/N 2.0.20: 'keyCommands' is exposed as a final extension val on UIResponder
     * (not overridable from Kotlin). The Swift/ObjC layer must call
     * getKeyCommandList() and return the result from its own keyCommands override.
     */
    fun getKeyCommandList(): List<UIKeyCommand> {
        return listOf(
            YoleKeyboardCommands.createKeyCommand(YoleKeyboardCommands.KeyboardShortcut.BOLD),
            YoleKeyboardCommands.createKeyCommand(YoleKeyboardCommands.KeyboardShortcut.ITALIC),
            YoleKeyboardCommands.createKeyCommand(YoleKeyboardCommands.KeyboardShortcut.UNDERLINE)
        )
    }
}
