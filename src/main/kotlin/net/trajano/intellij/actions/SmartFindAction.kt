package net.trajano.intellij.actions

import com.intellij.openapi.actionSystem.IdeActions
import java.awt.event.KeyEvent

class SmartFindAction : AbstractDelayChordAction(
    IdeActions.ACTION_FIND,
    IdeActions.ACTION_FIND_IN_PATH,
    secondaryActionMap = mapOf(
        KeyEvent.VK_F to IdeActions.ACTION_FIND_IN_PATH,
        KeyEvent.VK_R to IdeActions.ACTION_REPLACE,
    )
)