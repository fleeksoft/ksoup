package com.fleeksoft.ksoup.ported

import de.cketti.codepoints.deluxe.CodePoint

internal class Character {
    companion object {
        fun isDigit(codePoint: CodePoint): Boolean {
            return codePoint.value.toChar().isDigit()
        }

        val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x010000
        const val MIN_HIGH_SURROGATE: Char = '\uD800'
        const val MAX_LOW_SURROGATE: Char = '\uDFFF'
        const val MIN_SURROGATE: Char = MIN_HIGH_SURROGATE
        val MAX_SURROGATE: Char = MAX_LOW_SURROGATE

    }
}
