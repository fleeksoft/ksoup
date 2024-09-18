package com.fleeksoft.ksoup.io

import kotlin.math.max


class CharsetImpl(override val name: String) : Charset {
    init {
        require(name.lowercase() == "utf8" || name.lowercase() == "utf-8" || name.lowercase() == "iso-8859-1" || name.lowercase() == "ascii" || name.lowercase() == "us-ascii") {
            "Charset $name not supported"
        }
    }

    override fun onlyUtf8(): Boolean = true

    override fun decode(stringBuilder: StringBuilder, byteArray: ByteArray, start: Int, end: Int): Int {
        if (end <= 0) return 0
        var incompleteByteIndex = -1

        val isUtf8 = name.lowercase() == "utf-8" || name.lowercase() == "utf8"
        if (isUtf8) {
//            TODO:// may be we can use this for other charsets
            val startIndex = if (end > 4) end - 4 else 0
            var i = startIndex
            while (i < end) {
                val byteLength = guessByteSequenceLength(byteArray[i])
                if (byteLength > 1 && (i + byteLength) > end) {
                    incompleteByteIndex = i
                    break
                } else {
                    i += max(byteLength, 1)
                }
            }
        }
        val toDecodeSize = if (incompleteByteIndex > 0) {
            incompleteByteIndex
        } else {
            end
        }

        stringBuilder.append(byteArray.sliceArray(start until toDecodeSize).decodeToString())
        return toDecodeSize - start
    }

    private fun guessByteSequenceLength(byte: Byte): Int {
        return when ((byte.toInt() and 0xFF) shr 4) {
            in 0b0000..0b0111 -> 1
            in 0b1100..0b1101 -> 2
            0b1110 -> 3
            0b1111 -> 4
            else -> 0
        }
    }

    override fun toByteArray(value: String): ByteArray {
        return value.encodeToByteArray()
    }
}