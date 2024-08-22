package com.fleeksoft.ksoup.engine

import com.fleeksoft.ksoup.io.Charset
import com.fleeksoft.ksoup.io.FileSource
import com.fleeksoft.ksoup.io.SourceReader

interface KsoupEngine {
    fun urlResolveOrNull(base: String, relUrl: String): String?

    fun openSourceReader(content: String, charset: Charset? = null): SourceReader

    fun openSourceReader(byteArray: ByteArray): SourceReader

    fun getUtf8Charset(): Charset

    fun charsetForName(name: String): Charset

    fun pathToFileSource(path: String): FileSource
}