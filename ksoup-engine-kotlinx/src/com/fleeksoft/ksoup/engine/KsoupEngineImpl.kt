package com.fleeksoft.ksoup.engine

import com.fleeksoft.ksoup.io.FileSource
import com.fleeksoft.ksoup.io.from

object KsoupEngineImpl : KsoupEngine {

    override fun pathToFileSource(path: String): FileSource {
        return FileSource.from(path)
    }
}