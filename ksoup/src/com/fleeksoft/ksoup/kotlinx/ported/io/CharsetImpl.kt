package com.fleeksoft.ksoup.kotlinx.ported.io

import com.fleeksoft.ksoup.ported.io.Charset
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import kotlinx.io.Buffer


class CharsetImpl(override val name: String) : Charset {
    private var charset: io.ktor.utils.io.charsets.Charset = Charsets.forName(name)
    private val charsetDecoder by lazy { this.charset.newDecoder() }

    constructor(charset: io.ktor.utils.io.charsets.Charset) : this(charset.name) {
        this.charset = charset
    }

    override fun decode(stringBuilder: StringBuilder, byteArray: ByteArray, start: Int, end: Int): Int {
        if (end <= 0) return 0
        val buffer = Buffer().apply { write(byteArray, start, end) }
        val size = buffer.size
        val strSizeBeforeDecode = stringBuilder.length
        charsetDecoder.decode(buffer, stringBuilder, buffer.size.toInt())
        val decodedBytes = (size - buffer.size).toInt()
        var invalidBytes = 0
        if (stringBuilder.lastOrNull()?.code == 65533) {
            stringBuilder.setLength(stringBuilder.length - 1)
            val validDecodedStrBytesSize = stringBuilder.substring(strSizeBeforeDecode).toByteArray(charset).size
            invalidBytes = decodedBytes - validDecodedStrBytesSize
        }
        return decodedBytes - invalidBytes
    }

    override fun toByteArray(value: String): ByteArray {
        return value.toByteArray(charset)
    }
}