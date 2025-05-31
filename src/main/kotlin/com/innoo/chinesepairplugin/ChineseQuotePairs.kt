package com.innoo.chinesepairplugin

object ChineseQuotePairs {
    // 配对符号集合（左->右）
    val pairs: Map<Char, Char> = mapOf(
        // 只放中文配对符号，避免和英文符号冲突
        '“' to '”',
        '‘' to '’',
        '【' to '】',
        '（' to '）',
        '『' to '』',
        '〈' to '〉',
        '「' to '」',
        '〔' to '〕',
        '《' to '》'
        // 不要添加 (, {, [, ", ', <, >, 这些英文符号
    )

    // 左符号集合
    val lefts: Set<Char> = pairs.keys
    // 右符号集合
    val rights: Set<Char> = pairs.values.toSet()
    // 右->左的反向查找
    val rightToLeft: Map<Char, Char> = pairs.entries.associate { it.value to it.key }
    /** 特殊：双引号、单引号都视为左引号 */
    val doubleQuoteSet = setOf('“', '”')
    val singleQuoteSet = setOf('‘', '’')
}
