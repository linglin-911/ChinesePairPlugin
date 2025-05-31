package com.innoo.chinesepairplugin

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.TextRange

/**
 * 中文符号自动配对和包裹核心 Handler。
 * 只处理符号集合里的配对，其他符号交由IDE原生处理。
 */
class ChinesePairTypedHandler(
    private val originalHandler: TypedActionHandler?
) : TypedActionHandler {

    override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
        val caretModel = editor.caretModel
        val document = editor.document
        val selectionModel = editor.selectionModel
        val offset = caretModel.offset

        // 1. 中文双引号特殊处理
        if (charTyped == '“' || charTyped == '”') {
            val textAfter = if (offset < document.textLength) document.charsSequence[offset] else null
            // 如果光标后已是右引号，则跳过
            if (textAfter == '”') {
                caretModel.moveToOffset(offset + 1)
                return
            }
            // 选区包裹
            if (selectionModel.hasSelection()) {
                val selStart = selectionModel.selectionStart
                val selEnd = selectionModel.selectionEnd
                val selectedText = document.getText(TextRange(selStart, selEnd))
                document.replaceString(selStart, selEnd, "“$selectedText”")
                selectionModel.setSelection(selStart + 1, selStart + 1 + selectedText.length)
                caretModel.moveToOffset(selStart + 1 + selectedText.length)
            } else {
                // 无选区，直接插入“”
                document.insertString(offset, "“”")
                caretModel.moveToOffset(offset + 1)
            }
            return
        }

        // 2. 其他符号：左符号配对，右符号跳过
        if (ChineseQuotePairs.pairs.containsKey(charTyped)) {
            val rightChar = ChineseQuotePairs.pairs[charTyped]!!
            if (selectionModel.hasSelection()) {
                val selStart = selectionModel.selectionStart
                val selEnd = selectionModel.selectionEnd
                val selectedText = document.getText(TextRange(selStart, selEnd))
                document.replaceString(selStart, selEnd, "$charTyped$selectedText$rightChar")
                selectionModel.setSelection(selStart + 1, selStart + 1 + selectedText.length)
                caretModel.moveToOffset(selStart + 1 + selectedText.length)
            } else {
                document.insertString(offset, "$charTyped$rightChar")
                caretModel.moveToOffset(offset + 1)
            }
            return
        }

        // 3. 右符号跳过
        if (ChineseQuotePairs.rights.contains(charTyped)) {
            val textAfter = if (offset < document.textLength) document.charsSequence[offset] else null
            if (textAfter == charTyped) {
                caretModel.moveToOffset(offset + 1)
                return
            }
        }

        // 4. 其它符号：走原始逻辑
        originalHandler?.execute(editor, charTyped, dataContext)
    }

}
