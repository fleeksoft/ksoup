package com.fleeksoft.ksoup.kotlinx.ported.io

import com.fleeksoft.ksoup.ported.io.Charset
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import kotlinx.io.Buffer
import kotlin.math.max


class CharsetImpl(override val name: String) : Charset {
    private var charset: io.ktor.utils.io.charsets.Charset = Charsets.forName(name)
    private val charsetDecoder by lazy { this.charset.newDecoder() }

    constructor(charset: io.ktor.utils.io.charsets.Charset) : this(charset.name) {
        this.charset = charset
    }

    override fun decode(stringBuilder: StringBuilder, byteArray: ByteArray, start: Int, end: Int): Int {
        if (end <= 0) return 0
//        val strSizeBeforeDecode = stringBuilder.length
        var incompleteByteIndex = -1

        if (charset.name.lowercase() == "utf-8") {
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

        val buffer = Buffer().apply { write(byteArray, start, toDecodeSize) }
        val size = buffer.size
        stringBuilder.append(charsetDecoder.decode(buffer))
        val decodedBytes = (size - buffer.size).toInt()
        var invalidBytes = 0
        /*if (stringBuilder.lastOrNull()?.code == 65533) {
            stringBuilder.setLength(stringBuilder.length - 1)
            val validDecodedStrBytesSize = stringBuilder.substring(strSizeBeforeDecode).toByteArray(charset).size
            invalidBytes = decodedBytes - validDecodedStrBytesSize
        }
        return decodedBytes - invalidBytes*/

        return decodedBytes
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
        return value.toByteArray(charset)
    }
}