package com.innoo.chinesepairplugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.editor.actionSystem.EditorActionManager

class ChinesePairStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        val actionManager = EditorActionManager.getInstance()
        val oldTypedHandler = actionManager.typedAction.handler
        actionManager.typedAction.setupHandler(ChinesePairTypedHandler(oldTypedHandler))
        val oldBackspaceHandler = actionManager.getActionHandler("EditorBackSpace")
        actionManager.setActionHandler("EditorBackSpace", SmartBackspaceHandler(oldBackspaceHandler))
    }
}
