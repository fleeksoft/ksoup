package com.fleeksoft.ksoup

import com.fleeksoft.ksoup.ported.io.SourceReader
import com.fleeksoft.ksoup.ported.openSourceReader
import korlibs.io.compression.deflate.GZIP
import korlibs.io.compression.uncompress
import korlibs.io.file.VfsFile
import korlibs.io.file.readAsSyncStream
import korlibs.io.stream.readAll

public suspend fun readGzipFile(file: VfsFile): SourceReader {
    return file.readAsSyncStream().readAll().uncompress(GZIP).openSourceReader()
}

public suspend fun readFile(file: VfsFile): SourceReader {
    return file.openStream()
}