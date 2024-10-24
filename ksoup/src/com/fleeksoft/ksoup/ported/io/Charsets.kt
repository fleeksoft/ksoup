package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.charset.Charset
import com.fleeksoft.charset.Charsets

object Charsets {
    val UTF8: Charset = Charsets.UTF8

    fun forName(name: String): Charset = Charsets.forName(name)

    const val isOnlyUtf8 = false
}