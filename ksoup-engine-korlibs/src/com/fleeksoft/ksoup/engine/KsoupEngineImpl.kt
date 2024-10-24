package com.fleeksoft.ksoup.engine

import com.fleeksoft.ksoup.io.*

object KsoupEngineImpl : KsoupEngine {

    override fun pathToFileSource(path: String): FileSource {
        return FileSource.from(path)
    }
}