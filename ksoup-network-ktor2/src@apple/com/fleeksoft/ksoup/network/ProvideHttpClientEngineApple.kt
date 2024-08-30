package com.fleeksoft.ksoup.network

import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.Darwin

actual fun provideHttpClientEngine(): HttpClientEngine {
    return Darwin.create()
}