package com.fleeksoft.ksoup.network

import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*

internal actual fun provideHttpClientEngine(): HttpClientEngine {
    return CIO.create()
}