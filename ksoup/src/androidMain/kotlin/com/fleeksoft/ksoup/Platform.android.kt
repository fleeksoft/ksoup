package com.fleeksoft.ksoup

import okio.BufferedSource
import okio.FileSystem
import okio.GzipSource
import okio.Path
import okio.buffer

internal actual fun readGzipFile(file: Path): BufferedSource {
    val fileSource = FileSystem.SYSTEM.source(file)
    return GzipSource(source = fileSource).buffer()
}


internal actual fun readFile(file: Path): BufferedSource {
    return FileSystem.SYSTEM.source(file).buffer()
}