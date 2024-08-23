package com.fleeksoft.ksoup.io

import korlibs.io.lang.toByteArray

class CharsetImpl(override val name: String) : Charset {

    private var charset: korlibs.io.lang.Charset = korlibs.io.lang.Charset.forName(name)

    fun getInternalCharset() = charset

    constructor(charset: korlibs.io.lang.Charset) : this(charset.name) {
        this.charset = charset
    }

    override fun decode(stringBuilder: StringBuilder, byteArray: ByteArray, start: Int, end: Int): Int {
        return this.charset.decode(stringBuilder, byteArray, start, end)
    }

    override fun toByteArray(value: String): ByteArray {
        return value.toByteArray(charset)
    }
}