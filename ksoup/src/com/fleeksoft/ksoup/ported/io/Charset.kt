package com.fleeksoft.ksoup.ported.io

import korlibs.io.lang.Charset
import korlibs.memory.ByteArrayBuilder

class Charset : Charset {

    private val charset: Charset

    constructor(name: String) : super(name) {
        this.charset = Charset.forName(name)
    }

    constructor(charset: Charset) : super(charset.name) {
        this.charset = charset
    }

    // TODO: handle it better
    // some charsets can read but not encode; switch to an encodable charset and update the meta el
    fun canEncode(): Boolean = true

    fun canEncode(c: Char): Boolean {
        return this.canEncode("$c")
    }

    fun canEncode(s: String): Boolean {
//    return true
        // TODO: check this
        return kotlin.runCatching { this.encode(ByteArrayBuilder(s.length * 8), s) }
            .onFailure {
                println("encodingError: $this")
                it.printStackTrace()
            }.isSuccess
    }

    override fun decode(out: StringBuilder, src: ByteArray, start: Int, end: Int): Int {
        return charset.decode(out, src, start, end)
    }

    override fun encode(out: ByteArrayBuilder, src: CharSequence, start: Int, end: Int) {
        return charset.encode(out, src, start, end)
    }

    companion object {
        fun forName(name: String): com.fleeksoft.ksoup.ported.io.Charset {
            return com.fleeksoft.ksoup.ported.io.Charset(name)
        }
    }
}