package com.fleeksoft.ksoup

import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.buffer

internal actual fun readGzipFile(file: Path): BufferedSource {
    TODO("Not yet supported")
}

internal actual fun readFile(file: Path): BufferedSource {
    FileSystem.SYSTEM.source(file).buffer()
    return FileSystem.SYSTEM.source(file).buffer()
}