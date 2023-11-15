package com.fleeksoft.ksoup.ported

import de.cketti.codepoints.deluxe.CodePoint

internal class Character {
    companion object {
        fun isDigit(codePoint: CodePoint): Boolean {
            return codePoint.value.toChar().isDigit()
        }

        val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x010000
    }
}
