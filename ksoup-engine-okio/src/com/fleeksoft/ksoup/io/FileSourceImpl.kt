package com.fleeksoft.ksoup.io

import com.fleeksoft.io.InputStream
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import okio.buffer

class FileSourceImpl : FileSource {
    val path: Path

    private val sourceBuffer by lazy { buffered().buffer() }

    constructor(file: Path) {
        this.path = file
    }

    constructor(filePath: String) {
        this.path = filePath.toPath()
    }

    private fun FileSourceImpl.buffered(): Source = readFile(path)
    override suspend fun asInputStream(): InputStream {
        return OkioSourceInputStream(sourceBuffer)
    }

    override fun getPath(): String {
        return this.path.name
    }

    override fun getFullName(): String {
        return this.path.name
    }
}