package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.ported.io.BufferReader
import com.fleeksoft.ksoup.ported.openBufferReader
import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.uncompress
import korlibs.io.file.VfsFile
import korlibs.io.file.readAsSyncStream
import korlibs.io.stream.readAll

public suspend fun readGzipFile(file: VfsFile): BufferReader {
    return file.readAsSyncStream().readAll().uncompress(GZIP).openBufferReader()
}

public suspend fun readFile(file: VfsFile): BufferReader {
    return file.openStream()
}