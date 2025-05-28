package com.fleeksoft.ksoup.network

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*

actual fun provideHttpClientEngine(): HttpClientEngineFactory<HttpClientEngineConfig> {
    return OkHttp
}