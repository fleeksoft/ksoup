package com.fleeksoft.ksoup.ported.io

import com.fleeksoft.ksoup.KsoupEngineInstance
import com.fleeksoft.ksoup.io.Charset

object Charsets {
    val UTF8: Charset = KsoupEngineInstance.ksoupEngine.getUtf8Charset()

    fun forName(name: String): Charset = KsoupEngineInstance.ksoupEngine.charsetForName(name)
}