package com.innoo.chinesepairplugin

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.application.ApplicationManager

/**
 * 智能退格Handler：若光标在成对符号正中间，一次退格可同时删除两边符号。
 * 其它情况交给原始handler，支持普通字符/符号删除。
 */
class SmartBackspaceHandler(
    private val originalHandler: EditorActionHandler?
) : EditorActionHandler() {

    override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
        val caretModel = editor.caretModel
        val document = editor.document
        val offset = caretModel.offset

        var deleted = false // 标记是否已执行成对符号删除

        // 检查光标是否正好在配对符号中间
        if (offset > 0 && offset < document.textLength) {
            val charBefore = document.charsSequence[offset - 1]
            val charAfter = document.charsSequence[offset]
            if (
                ChineseQuotePairs.lefts.contains(charBefore)
                && ChineseQuotePairs.rights.contains(charAfter)
                && ChineseQuotePairs.pairs[charBefore] == charAfter
            ) {
                // 用writeAction安全删除成对符号
                ApplicationManager.getApplication().runWriteAction {
                    document.deleteString(offset - 1, offset + 1)
                }
                caretModel.moveToOffset(offset - 1)
                deleted = true
            }
        }
//
        // 如果不是成对符号，执行原生退格（支持只删除右边符号/普通字符）
        if (!deleted) {
            originalHandler?.execute(editor, caret, dataContext)
        }
    }
}
