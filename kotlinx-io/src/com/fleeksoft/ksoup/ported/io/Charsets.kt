package com.fleeksoft.ksoup.ported.io

object Charsets {
    val UTF8 : Charset = TODO()

    fun forName(name: String): Charset = CharsetImpl(name)
}