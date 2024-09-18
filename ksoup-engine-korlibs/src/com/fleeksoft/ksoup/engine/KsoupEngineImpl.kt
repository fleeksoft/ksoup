package com.fleeksoft.ksoup.engine

import com.fleeksoft.ksoup.io.*
import korlibs.io.lang.Charsets

object KsoupEngineImpl : KsoupEngine {

    override fun getUtf8Charset(): Charset {
        return CharsetImpl(Charsets.UTF8)
    }

    override fun charsetForName(name: String): Charset {
        return CharsetImpl(name)
    }

    override fun pathToFileSource(path: String): FileSource {
        return FileSource.from(path)
    }
}