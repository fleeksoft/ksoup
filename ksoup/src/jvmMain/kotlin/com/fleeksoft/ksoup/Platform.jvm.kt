package com.fleeksoft.ksoup

import okio.BufferedSource
import okio.FileSystem
import okio.GzipSource
import okio.Path
import okio.buffer

actual fun readGzipFile(file: Path): BufferedSource {
    val fileSource = FileSystem.SYSTEM.source(file)
    return GzipSource(source = fileSource).buffer()
}


actual fun readFile(file: Path): BufferedSource {
    return FileSystem.SYSTEM.source(file).buffer()
}