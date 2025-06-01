package com.innoo.chinesesymbolpair

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.application.ApplicationManager

/**
 * 中文符号自动配对和包裹 Handler。
 * 支持双引号“”、单引号‘’、《》等任意成对符号。
 * - 输入左/右符号都会智能配对成一对，光标置中
 * - 选区包裹时，如两端已包裹则只跳出，不再二次包裹
 * - 右符号跳过时自动取消选区
 * - 其它字符交给原始 IDEA 处理
 */
class ChinesePairTypedHandler(
    private val originalHandler: TypedActionHandler?
) : TypedActionHandler {

    /**
     * 输入法触发字符时的主处理函数
     */
    override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
        val caretModel = editor.caretModel
        val document = editor.document
        val selectionModel = editor.selectionModel
        val offset = caretModel.offset

        // 1. 判断当前输入是否为配对符号（如“、‘、《等）
        //    分别处理双引号、单引号、其它 pairs 里定义的符号
        val (isPair, left, right) = when {
            ChineseQuotePairs.doubleQuoteSet.contains(charTyped) -> Triple(true, '“', '”')
            ChineseQuotePairs.singleQuoteSet.contains(charTyped) -> Triple(true, '‘', '’')
            ChineseQuotePairs.pairs.containsKey(charTyped) -> {
                val rightChar = ChineseQuotePairs.pairs[charTyped]!!
                Triple(true, charTyped, rightChar)
            }
            else -> Triple(false, null, null)
        }

        // 2. 是配对符号，则进入智能配对/包裹逻辑
        if (isPair && left != null && right != null) {
            handlePairSymbol(
                document, selectionModel, caretModel, offset,
                left, right, charTyped
            )
            return
        }

        // 3. 右符号跳过逻辑：光标后已有右符号，则直接跳过，不插入新符号
        //    并取消当前选区（避免高亮残留）
        if (ChineseQuotePairs.rights.contains(charTyped)) {
            val textAfter = if (offset < document.textLength) document.charsSequence[offset] else null
            if (textAfter == charTyped) {
                if (selectionModel.hasSelection()) {
                    selectionModel.removeSelection()
                }
                caretModel.moveToOffset(offset + 1)
                return
            }
        }

        // 4. 其它字符直接交由编辑器原始处理
        originalHandler?.execute(editor, charTyped, dataContext)
    }

    /**
     * 统一处理所有成对符号（含双引号、单引号、《》等）的包裹、插入、跳过等逻辑
     */
    private fun handlePairSymbol(
        document: com.intellij.openapi.editor.Document,
        selectionModel: com.intellij.openapi.editor.SelectionModel,
        caretModel: com.intellij.openapi.editor.CaretModel,
        offset: Int,
        left: Char,   // 当前要配对/包裹的左符号
        right: Char,  // 对应的右符号
        charTyped: Char // 用户实际输入的字符
    ) {
        if (selectionModel.hasSelection()) {
            // 有选区时，优先判断是否已经包裹
            val selStart = selectionModel.selectionStart
            val selEnd = selectionModel.selectionEnd

            // 如果选区两端已经被成对符号包裹（如“你好”），
            // 说明用户此时只想跳出包裹而非二次包裹
            if (isSurroundedByPair(document, selStart, selEnd, left, right)) {
                selectionModel.removeSelection()
                caretModel.moveToOffset(selEnd + 1) // 跳到包裹的右符号后面
                return
            }

            // 否则正常包裹：将选区内容用左右符号包裹
            val selectedText = document.getText(TextRange(selStart, selEnd))
            ApplicationManager.getApplication().runWriteAction {
                document.replaceString(selStart, selEnd, "$left$selectedText$right")
            }
            // 包裹后，取消选区，光标移到新内容后
            selectionModel.removeSelection()
            caretModel.moveToOffset(selStart + selectedText.length + 2)
            return
        }

        // 无选区时：如果光标后已有右符号，直接跳过，不再插入配对
        val textAfter = if (offset < document.textLength) document.charsSequence[offset] else null
        if (textAfter == right) {
            caretModel.moveToOffset(offset + 1)
            return
        }
        // 普通配对插入：“”/‘’/《》
        ApplicationManager.getApplication().runWriteAction {
            document.insertString(offset, "$left$right")
        }
        caretModel.moveToOffset(offset + 1) // 光标置于两符号之间
    }

    /**
     * 判断选区两端是否已被某组符号包裹（如“选中内容”）
     */
    private fun isSurroundedByPair(
        document: com.intellij.openapi.editor.Document,
        selStart: Int,
        selEnd: Int,
        left: Char,
        right: Char
    ): Boolean {
        val textLen = document.textLength
        val leftChar = if (selStart > 0) document.charsSequence[selStart - 1] else null
        val rightChar = if (selEnd < textLen) document.charsSequence[selEnd] else null
        return leftChar == left && rightChar == right
    }
}
