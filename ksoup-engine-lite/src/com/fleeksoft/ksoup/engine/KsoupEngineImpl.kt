package com.fleeksoft.ksoup.engine

import com.fleeksoft.ksoup.io.*

object KsoupEngineImpl : KsoupEngine {

    override fun getUtf8Charset(): Charset {
        return CharsetImpl("UTF-8")
    }

    override fun charsetForName(name: String): Charset {
        return CharsetImpl(name)
    }

    override fun pathToFileSource(path: String): FileSource {
        TODO("File Source not supported in lite")
    }
}