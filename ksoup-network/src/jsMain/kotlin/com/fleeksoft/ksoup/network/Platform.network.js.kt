package com.fleeksoft.ksoup.network

import io.ktor.client.engine.*
import io.ktor.client.engine.js.*

internal actual fun provideHttpClientEngine(): HttpClientEngine {
    return Js.create()
}