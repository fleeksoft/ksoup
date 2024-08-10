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