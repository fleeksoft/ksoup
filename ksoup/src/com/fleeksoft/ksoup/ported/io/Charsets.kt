package com.fleeksoft.ksoup.ported.io

object Charsets {
    val UTF8 get(): Charset = Charset(korlibs.io.lang.UTF8)
    val LATIN1 get(): Charset = Charset(korlibs.io.lang.LATIN1)
    val UTF16_LE get(): Charset = Charset(korlibs.io.lang.UTF16_LE)
    val UTF16_BE get(): Charset = Charset(korlibs.io.lang.UTF16_BE)
    val ASCII get(): Charset = Charset(korlibs.io.lang.ASCII)
}