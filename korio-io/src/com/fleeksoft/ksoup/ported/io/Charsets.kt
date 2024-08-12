package com.fleeksoft.ksoup.ported.io

import korlibs.io.lang.Charsets

object Charsets {
    val UTF8 : Charset = CharsetImpl(Charsets.UTF8)

    fun forName(name: String): Charset = CharsetImpl(name)
}