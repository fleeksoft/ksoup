package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.ksoup.KsoupEngineInstance

object Charsets {
    val UTF8: Charset = KsoupEngineInstance.ksoupEngine.getUtf8Charset()

    fun forName(name: String): Charset = KsoupEngineInstance.ksoupEngine.charsetForName(name)
}