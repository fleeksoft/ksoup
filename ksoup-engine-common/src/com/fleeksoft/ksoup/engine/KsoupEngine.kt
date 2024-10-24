package com.fleeksoft.ksoup.engine

import com.fleeksoft.ksoup.io.FileSource

interface KsoupEngine {

    fun pathToFileSource(path: String): FileSource
}