package com.fleeksoft.ksoup.kotlinx.ported.io

import com.fleeksoft.ksoup.ported.io.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.decode
import io.ktor.utils.io.charsets.forName
import io.ktor.utils.io.charsets.name
import io.ktor.utils.io.core.toByteArray
import kotlinx.io.Buffer


class CharsetImpl(override val name: String) : Charset {
    private var charset: io.ktor.utils.io.charsets.Charset = Charsets.forName(name)

    constructor(charset: io.ktor.utils.io.charsets.Charset) : this(charset.name) {
        this.charset = charset
    }

    override fun decode(stringBuilder: StringBuilder, byteArray: ByteArray, start: Int, end: Int): Int {
        if (end <= 0) return 0
        val buffer = Buffer().apply { write(byteArray, start, end) }
        val size = buffer.size
        stringBuilder.append(this.charset.newDecoder().decode(buffer))
        return (size - buffer.size).toInt()
    }

    override fun toByteArray(value: String): ByteArray {
        return value.toByteArray(charset)
    }
}