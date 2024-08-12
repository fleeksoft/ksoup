package com.fleeksoft.ksoup.ported.io

import korlibs.io.lang.Charset
import korlibs.io.lang.toByteArray
import korlibs.io.lang.toString

class CharsetImpl(override val name: String) : com.fleeksoft.ksoup.ported.io.Charset {

    private var charset: Charset = Charset.forName(name)
    
    constructor(charset: Charset) : this(charset.name) {
        this.charset = charset
    }

    override fun decode(stringBuilder: StringBuilder, byteArray: ByteArray, start: Int, end: Int): Int {
        return this.charset.decode(stringBuilder, byteArray, start, end)
    }

    override fun toByteArray(value: String): ByteArray {
        return value.toByteArray(charset)
    }

    override fun toString(byteArray: ByteArray, start: Int, end: Int): String {
        return byteArray.toString(charset, start, end)
    }
}