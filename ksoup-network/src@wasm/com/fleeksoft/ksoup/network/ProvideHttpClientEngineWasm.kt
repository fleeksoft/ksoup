package com.fleeksoft.ksoup.network

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*

actual fun provideHttpClientEngine(): HttpClientEngineFactory<HttpClientEngineConfig> {
    return Js
}