package com.fleeksoft.ksoup.engine

import com.fleeksoft.ksoup.io.Charset
import com.fleeksoft.ksoup.io.FileSource

interface KsoupEngine {

    fun getUtf8Charset(): Charset

    fun charsetForName(name: String): Charset

    fun pathToFileSource(path: String): FileSource
}