package com.fleeksoft.ksoup.io

import com.fleeksoft.io.InputStream
import com.fleeksoft.ksoup.inputStream
import korlibs.io.file.VfsFile
import korlibs.io.file.fullName
import korlibs.io.file.std.uniVfs

internal class FileSourceImpl : FileSource {
    private val file: VfsFile

    constructor(file: VfsFile) {
        this.file = file
    }

    constructor(filePath: String) {
        this.file = filePath.uniVfs
    }

    override suspend fun asInputStream(): InputStream = this.file.inputStream()

    override fun getPath(): String {
        return this.file.absolutePath
    }

    override fun getFullName(): String {
        return this.file.fullName
    }
}