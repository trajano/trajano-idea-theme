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

abstract class AbstractDelayChordAction(
    private val primaryActionId: String,
    private val longPressActionId: String,
    shortPressDuration: Duration = 250.milliseconds,
    longPressDuration: Duration = 500.milliseconds,
    private val secondaryActionMap: Map<Int, String> = emptyMap(),
    icon: Icon? = null
) : AnAction(icon) {

    private var isRunning = false;
    private var charKeyCode: Int = KeyEvent.VK_UNDEFINED
    private var charWasReleased = false
    private var ctrlWasReleased = false
    private var secondaryCharWasPressedWhileCtrlWasHeld = false
    private var secondaryCharKeyCode: Int = KeyEvent.VK_UNDEFINED
    private var dispatcher: KeyEventDispatcher? = null
    private var shortTimer: Timer? = null
    private var longTimer: Timer? = null
    private var isTriggered = false
    private var shortPressMillis = (shortPressDuration).inWholeMilliseconds.toInt()
    private var longPressMillis = (longPressDuration - shortPressDuration).inWholeMilliseconds.toInt()

    override fun actionPerformed(e: AnActionEvent) {
        if (isRunning) {
            return
        }
        isRunning = true
        isTriggered = false
        charKeyCode = guessOriginKey(e) ?: return

        assert(e.inputEvent?.isControlDown == true)
        ctrlWasReleased = false
        charWasReleased = false
        secondaryCharWasPressedWhileCtrlWasHeld = false
        secondaryCharKeyCode = KeyEvent.VK_UNDEFINED
        dispatcher = KeyEventDispatcher { event ->
            when (event.id) {
                KeyEvent.KEY_RELEASED -> {
                    when (event.keyCode) {
                        charKeyCode -> charWasReleased = true
                        KeyEvent.VK_CONTROL -> ctrlWasReleased = true
                    }
                }

                KeyEvent.KEY_PRESSED -> {
                    if (event.keyCode == charKeyCode && !charWasReleased) {
                        // Do nothing when the key is still held
                        return@KeyEventDispatcher true
                    }
                    if (!ctrlWasReleased) {
                        secondaryCharWasPressedWhileCtrlWasHeld = true
                        secondaryCharKeyCode = event.keyCode
                    }
                }
            }
            if (ctrlWasReleased) {
                // if ctrl released then don't do anything else anymore
                cleanupDispatcher()
                if (!isTriggered) {
                    triggerAction(primaryActionId, e)
                }
                // running is stopped once the Ctrl key is released
                isRunning = false
                return@KeyEventDispatcher true
            }
            if (isTriggered) {
                return@KeyEventDispatcher true
            }
            if (!(shortTimer?.isRunning ?: false)) {
                shortTimer = Timer(shortPressMillis) {
                    if (charWasReleased && !secondaryCharWasPressedWhileCtrlWasHeld) {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher)
                        isRunning = false
                        triggerAction(primaryActionId, e)
                        return@Timer
                    }
                    longTimer = Timer(longPressMillis) {
                        if (!charWasReleased && !secondaryCharWasPressedWhileCtrlWasHeld) {
                            triggerAction(longPressActionId, e)
                            return@Timer
                        }
                    }
                    longTimer!!.isRepeats = false
                    longTimer!!.start()
                }
                shortTimer!!.isRepeats = false
                shortTimer!!.start()
            }
            if (secondaryCharWasPressedWhileCtrlWasHeld) {
                secondaryActionMap[event.keyCode]?.let { actionId ->
                    triggerAction(actionId, e)
                } ?: run {
                    isTriggered = true
                }
            }
            false
        }

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher)

    }

    private fun guessOriginKey(e: AnActionEvent): Int? {
        val keyEvent = e.inputEvent as? KeyEvent
        return keyEvent?.keyCode
    }

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

    private fun cleanupDispatcher() {
        dispatcher.let { KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(it) }
        shortTimer?.stop()
        longTimer?.stop()
        shortTimer = null
        longTimer = null
    }
}