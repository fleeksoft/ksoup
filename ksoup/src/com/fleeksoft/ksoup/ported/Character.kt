package com.fleeksoft.ksoup.ported

internal object Character {
    val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x010000
    const val MIN_HIGH_SURROGATE: Char = '\uD800'
    const val MIN_LOW_SURROGATE: Char = '\uDC00'
    const val MAX_LOW_SURROGATE: Char = '\uDFFF'
    const val MIN_SURROGATE: Char = MIN_HIGH_SURROGATE
    val MAX_SURROGATE: Char = MAX_LOW_SURROGATE
    const val MAX_CODE_POINT: Int = 0X10FFFF


    fun toCodePoint(high: Char, low: Char): Int {
        return ((high.code shl 10) + low.code) + (MIN_SUPPLEMENTARY_CODE_POINT - (MIN_HIGH_SURROGATE.code shl 10) - MIN_LOW_SURROGATE.code)
    }

    fun isDigit(codePoint: CodePoint): Boolean {
        return codePoint.value.toChar().isDigit()
    }

    fun isDigit(codePointValue: Int): Boolean {
        return codePointValue.toChar().isDigit()
    }

    fun isValidCodePoint(codePoint: Int): Boolean {
        val plane = codePoint ushr 16
        return plane < ((MAX_CODE_POINT + 1) ushr 16)
    }

    fun isBmpCodePoint(codePoint: Int): Boolean {
        return codePoint ushr 16 == 0
    }

    fun highSurrogate(codePoint: Int): Char {
        return ((codePoint ushr 10) + (MIN_HIGH_SURROGATE.code - (MIN_SUPPLEMENTARY_CODE_POINT ushr 10))).toChar()
    }

    fun lowSurrogate(codePoint: Int): Char {
        return ((codePoint and 0x3ff) + MIN_LOW_SURROGATE.code).toChar()
    }

    fun toSurrogates(codePoint: Int, dst: CharArray, index: Int) {
        // We write elements "backwards" to guarantee all-or-nothing
        dst[index + 1] = lowSurrogate(codePoint)
        dst[index] = highSurrogate(codePoint)
    }

    fun toChars(codePoint: Int): CharArray {
        return when {
            isBmpCodePoint(codePoint) -> {
                charArrayOf(codePoint.toChar())
            }

            isValidCodePoint(codePoint) -> {
                val result = CharArray(2)
                toSurrogates(codePoint, result, 0)
                result
            }

            else -> throw IllegalArgumentException(
                "Not a valid Unicode code point: 0x${codePoint.toString(16).uppercase()}"
            )
        }
    }

    fun toChars(codePoint: Int, dst: CharArray, dstIndex: Int): Int {
        return when {
            isBmpCodePoint(codePoint) -> {
                dst[dstIndex] = codePoint.toChar()
                1
            }

            isValidCodePoint(codePoint) -> {
                toSurrogates(codePoint, dst, dstIndex)
                2
            }

            else -> throw IllegalArgumentException(
                "Not a valid Unicode code point: 0x${codePoint.toString(16).uppercase()}"
            )
        }
    }

}

fun CharSequence.codePointValueAt(index: Int): Int {
    if (index !in indices) throw IndexOutOfBoundsException()

    val firstChar = this[index]
    if (firstChar.isHighSurrogate() && index + 1 < length) {
        val nextChar = this[index + 1]
        if (nextChar.isLowSurrogate()) {
            return Character.toCodePoint(firstChar, nextChar)
        }
    }

    return firstChar.code
}

fun CharSequence.codePointAt(index: Int): CodePoint {
    return this.codePointValueAt(index).toCodePoint()
}

fun <T : Appendable> T.appendCodePoint(codePoint: Int): T = apply {
    if (Character.isBmpCodePoint(codePoint)) {
        append(codePoint.toChar())
    } else {
        append(Character.highSurrogate(codePoint))
        append(Character.lowSurrogate(codePoint))
    }
}