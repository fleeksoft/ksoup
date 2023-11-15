package com.fleeksoft.ksoup

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import okio.*

actual fun readGzipFile(file: Path): BufferedSource {
    val fileSource = FileSystem.SYSTEM.source(file)
    return GzipSource(source = fileSource).buffer()
}


actual fun readFile(file: Path): BufferedSource {
    return FileSystem.SYSTEM.source(file).buffer()
}

actual fun provideHttpClientEngine(): HttpClientEngine {
    return OkHttp.create {
        config {
            followRedirects(true)
        }
    }
}