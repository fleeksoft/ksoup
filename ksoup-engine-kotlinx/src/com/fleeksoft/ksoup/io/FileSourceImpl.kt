package com.fleeksoft.ksoup.io

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class FileSourceImpl : FileSource {
    val path: Path

    private val sourceBuffered by lazy { buffered() }

    constructor(file: Path) {
        this.path = file
    }

    constructor(filePath: String) {
        this.path = Path(filePath)
    }

    override suspend fun toSourceReader(): SourceReader = SourceReaderImpl(sourceBuffered)

    private fun FileSourceImpl.buffered() = SystemFileSystem.source(path).buffered()

    override fun getPath(): String {
        return this.path.name
    }

    override fun getFullName(): String {
        return this.path.name
    }
}