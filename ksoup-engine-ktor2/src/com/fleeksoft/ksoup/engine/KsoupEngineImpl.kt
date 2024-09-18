package com.fleeksoft.ksoup.engine

import com.fleeksoft.ksoup.io.*
import com.fleeksoft.ksoup.io.Charset
import io.ktor.utils.io.charsets.*

object KsoupEngineImpl : KsoupEngine {

    override fun getUtf8Charset(): Charset {
        return CharsetImpl(Charsets.UTF_8)
    }

    override fun charsetForName(name: String): Charset {
        return CharsetImpl(name)
    }

    override fun pathToFileSource(path: String): FileSource {
        return FileSource.from(path)
    }
}