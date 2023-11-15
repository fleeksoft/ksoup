package com.fleeksoft.ksoup

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import okio.*

internal actual fun readGzipFile(file: Path): BufferedSource {
    val fileSource = FileSystem.SYSTEM.source(file)
    return GzipSource(source = fileSource).buffer()
}


internal actual fun readFile(file: Path): BufferedSource {
    return FileSystem.SYSTEM.source(file).buffer()
}

internal actual fun provideHttpClientEngine(): HttpClientEngine {
    return OkHttp.create {
        config {
            followRedirects(true)
        }
    }
}