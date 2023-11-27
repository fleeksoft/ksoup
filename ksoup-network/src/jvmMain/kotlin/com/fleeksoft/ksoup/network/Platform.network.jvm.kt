package com.fleeksoft.ksoup.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun provideHttpClientEngine(): HttpClientEngine {
    return OkHttp.create()
}