package net.trajano.intellij.actions

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import javax.swing.Icon
import javax.swing.Timer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Abstract base class for defining chord-based IntelliJ actions
 * that support a primary action, a long-press variant, and optional
 * secondary actions when another key is pressed while Ctrl is held.
 */
abstract class AbstractDelayChordAction(
    private val primaryActionId: String,
    private val longPressActionId: String,
    shortPressDuration: Duration = 250.milliseconds,
    longPressDuration: Duration = 500.milliseconds,
    private val secondaryActionMap: Map<Int, String> = emptyMap(),
    icon: Icon? = null
) : AnAction(icon) {

    private var isRunning = false
    private var isTriggered = false

    private var charKeyCode: Int = KeyEvent.VK_UNDEFINED
    private var secondaryCharKeyCode: Int = KeyEvent.VK_UNDEFINED

    private var charWasReleased = false
    private var ctrlWasReleased = false
    private var secondaryCharPressedWithCtrl = false

    private var dispatcher: KeyEventDispatcher? = null
    private var shortTimer: Timer? = null
    private var longTimer: Timer? = null

    private val shortPressMillis = shortPressDuration.inWholeMilliseconds.toInt()
    private val longPressMillis = (longPressDuration - shortPressDuration).inWholeMilliseconds.toInt()

    override fun actionPerformed(e: AnActionEvent) {
        if (isRunning) return

        isRunning = true
        isTriggered = false
        charKeyCode = guessOriginKey(e) ?: return

        assert(e.inputEvent?.isControlDown == true)
        resetKeyFlags()

        dispatcher = KeyEventDispatcher { event -> handleKeyEvent(event, e) }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher)
    }

    /**
     * Detects the keyCode of the originating key press event.
     */
    private fun guessOriginKey(e: AnActionEvent): Int? {
        return (e.inputEvent as? KeyEvent)?.keyCode
    }

    /**
     * Triggers the given action by its ID, using the original event context.
     */
    private fun triggerAction(actionId: String, originalEvent: AnActionEvent) {
        ApplicationManager.getApplication().invokeLater {
            val action = ActionManager.getInstance().getAction(actionId)
            val event = AnActionEvent.createEvent(
                action,
                originalEvent.dataContext,
                null,
                ActionPlaces.KEYBOARD_SHORTCUT,
                ActionUiKind.NONE,
                null
            )
            action.actionPerformed(event)
        }
        isTriggered = true
    }

    /**
     * Cleans up the key event dispatcher and stops any running timers.
     */
    private fun cleanupDispatcher() {
        dispatcher?.let {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(it)
        }
        shortTimer?.stop()
        longTimer?.stop()
        shortTimer = null
        longTimer = null
    }

    /**
     * Resets all the key state tracking variables.
     */
    private fun resetKeyFlags() {
        charWasReleased = false
        ctrlWasReleased = false
        secondaryCharPressedWithCtrl = false
        secondaryCharKeyCode = KeyEvent.VK_UNDEFINED
    }

    /**
     * Handles the key event lifecycle and timer logic.
     */
    private fun handleKeyEvent(event: KeyEvent, originalEvent: AnActionEvent): Boolean {
        when (event.id) {
            KeyEvent.KEY_RELEASED -> {
                when (event.keyCode) {
                    charKeyCode -> charWasReleased = true
                    KeyEvent.VK_CONTROL -> ctrlWasReleased = true
                }
            }
            KeyEvent.KEY_PRESSED -> {
                if (event.keyCode == charKeyCode && !charWasReleased) return true
                if (!ctrlWasReleased) {
                    secondaryCharPressedWithCtrl = true
                    secondaryCharKeyCode = event.keyCode
                }
            }
        }

        if (ctrlWasReleased) {
            cleanupDispatcher()
            if (!isTriggered) triggerAction(primaryActionId, originalEvent)
            isRunning = false
            return true
        }

        if (isTriggered) return true

        if (shortTimer?.isRunning != true) {
            shortTimer = Timer(shortPressMillis) {
                if (charWasReleased && !secondaryCharPressedWithCtrl) {
                    cleanupDispatcher()
                    isRunning = false
                    triggerAction(primaryActionId, originalEvent)
                } else {
                    longTimer = Timer(longPressMillis) {
                        if (!charWasReleased && !secondaryCharPressedWithCtrl) {
                            triggerAction(longPressActionId, originalEvent)
                        }
                    }.apply {
                        isRepeats = false
                        start()
                    }
                }
            }.apply {
                isRepeats = false
                start()
            }
        }

        if (secondaryCharPressedWithCtrl) {
            secondaryActionMap[event.keyCode]?.let { actionId ->
                triggerAction(actionId, originalEvent)
            } ?: run { isTriggered = true }
        }

        return false
    }
}
