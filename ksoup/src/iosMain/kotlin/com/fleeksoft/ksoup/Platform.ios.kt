package com.fleeksoft.ksoup

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.*
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Path
import okio.buffer

internal actual fun readGzipFile(file: Path): BufferedSource {
    return Buffer().apply {
        write(decompressGzip(FileSystem.SYSTEM.source(file).buffer().readByteArray()))
    }
}

internal actual fun readFile(file: okio.Path): BufferedSource {
    return FileSystem.SYSTEM.source(file).buffer()
}

internal actual fun provideHttpClientEngine(): HttpClientEngine {
    return Darwin.create()
}