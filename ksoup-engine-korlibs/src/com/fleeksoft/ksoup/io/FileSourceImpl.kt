package com.fleeksoft.ksoup.io

import com.fleeksoft.ksoup.openStream
import korlibs.io.file.VfsFile
import korlibs.io.file.fullName
import korlibs.io.file.std.uniVfs

class FileSourceImpl : FileSource {
    val file: VfsFile

    constructor(file: VfsFile) {
        this.file = file
    }

    constructor(filePath: String) {
        this.file = filePath.uniVfs
    }

    override suspend fun toSourceReader(): SourceReader = this.file.openStream()

    override fun getPath(): String {
        return this.file.absolutePath
    }

    override fun getFullName(): String {
        return this.file.fullName
    }
}