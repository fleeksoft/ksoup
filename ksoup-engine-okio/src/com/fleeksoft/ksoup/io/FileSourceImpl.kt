package com.fleeksoft.ksoup.io

import okio.BufferedSource
import okio.Path
import okio.Path.Companion.toPath
import okio.Source

class FileSourceImpl : FileSource {
    val path: Path

    private val sourceBuffered by lazy { buffered() }

    constructor(file: Path) {
        this.path = file
    }

    constructor(filePath: String) {
        this.path = filePath.toPath()
    }

    override suspend fun toSourceReader(): SourceReader = SourceReader.from(sourceBuffered)

    private fun FileSourceImpl.buffered(): Source = readFile(path)

    override fun getPath(): String {
        return this.path.name
    }

    override fun getFullName(): String {
        return this.path.name
    }
}