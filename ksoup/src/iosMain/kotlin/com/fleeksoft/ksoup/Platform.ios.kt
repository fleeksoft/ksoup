package com.fleeksoft.ksoup

import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.buffer

internal actual fun readGzipFile(file: Path): BufferedSource {
    // TODO: optimize for BufferedSource without reading all bytes
    return Buffer().apply {
        write(decompressGzip(readFile(file).buffer().readByteArray()))
    }
}

internal actual fun readFile(file: Path): BufferedSource {
    return FileSystem.SYSTEM.source(file).buffer()
}